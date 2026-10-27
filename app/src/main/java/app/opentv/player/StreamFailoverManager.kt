/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.player

import android.content.Context
import android.util.Log
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import app.opentv.core.AppSettings
import app.opentv.data.model.Channel
import app.opentv.data.repo.CatalogRepository
import app.opentv.data.repo.SourceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.ArrayDeque

/**
 * Representation of an available stream route / candidate for a channel.
 */
data class StreamCandidate(
    val id: String,
    val title: String,
    val subtitle: String,
    val channel: Channel?,
    val streamUrl: String,
    val userAgent: String,
    val isCurrent: Boolean = false,
)

/**
 * Notification payload emitted when an automatic or manual stream failover occurs.
 */
data class FailoverNotice(
    val title: String,
    val details: String,
    val candidate: StreamCandidate,
    val reason: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Intelligent Server Load Balancer & Anti-Lag Engine (Akıllı Sunucu Yük Dengeleyici & Donma Kalkanı).
 *
 * Automatically monitors live stream health:
 * 1. Freeze Watchdog: If a live stream stays in [Player.STATE_BUFFERING] continuously for longer
 *    than the configured threshold (default 3.5s), it triggers seamless failover to a backup stream.
 * 2. Frequent Stutter / Severe Lag Detector: If 3 or more buffering events occur in 15 seconds,
 *    it proactively steps over to a more stable or alternate server/stream.
 * 3. Immediate Error Interceptor: Recovers instantly from dead stream links (404, 500, network loss)
 *    by failing over to secondary lines or alternative providers before the user sees an error box.
 * 4. Multi-Tier Stream Discovery: Assembles backup lines from:
 *    - Same provider's quality tiers (HD, FHD, HEVC, RAW) & server variants (YEDEK, BACKUP)
 *    - Cross-provider matching channels (Provider B carrying the same channel)
 *    - Dynamic container format fallbacks (HLS .m3u8 <-> MPEG-TS .ts)
 */
class StreamFailoverManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val catalogRepository: CatalogRepository,
    private val sourceRepository: SourceRepository,
    private val settings: AppSettings,
    private val onSwitchRequest: (PlayerController.Request, StreamCandidate) -> Unit,
) {
    companion object {
        private const val TAG = "StreamFailoverManager"
        private const val STALL_WINDOW_MS = 15_000L
        private const val STALL_THRESHOLD_COUNT = 3
        private const val NOTICE_DURATION_MS = 4_500L
        private const val STABLE_RESET_DELAY_MS = 12_000L
    }

    private val _candidates = MutableStateFlow<List<StreamCandidate>>(emptyList())
    val candidates: StateFlow<List<StreamCandidate>> = _candidates.asStateFlow()

    private val _activeCandidate = MutableStateFlow<StreamCandidate?>(null)
    val activeCandidate: StateFlow<StreamCandidate?> = _activeCandidate.asStateFlow()

    private val _notice = MutableStateFlow<FailoverNotice?>(null)
    val notice: StateFlow<FailoverNotice?> = _notice.asStateFlow()

    private var currentChannel: Channel? = null
    private var currentUrl: String = ""
    private var currentUserAgent: String = ""

    private var stallWatchdogJob: Job? = null
    private var dismissNoticeJob: Job? = null
    private var failoverAttempts = 0
    private val recentStalls = ArrayDeque<Long>()

    private val freezeTimeoutMs: Long
        get() = (settings.autoFailoverTimeoutSec.value * 1000L).coerceIn(2000L, 10000L)

    /**
     * Called when a channel is tuned or changed by the user.
     */
    fun onChannelTuned(channel: Channel, url: String, userAgent: String) {
        currentChannel = channel
        currentUrl = url
        currentUserAgent = userAgent
        failoverAttempts = 0
        recentStalls.clear()
        stallWatchdogJob?.cancel()
        stallWatchdogJob = null

        scope.launch {
            loadCandidates(channel, url, userAgent)
        }
    }

    /**
     * Discovers all available alternative lines for the current channel across providers.
     */
    private suspend fun loadCandidates(channel: Channel, activeUrl: String, defaultUa: String) {
        val list = mutableListOf<StreamCandidate>()
        val currentSource = sourceRepository.byId(channel.sourceId)
        val currentSourceName = currentSource?.name ?: "Aktif Sağlayıcı"

        // 1. Current active stream
        val activeCandidate = StreamCandidate(
            id = "cur_${channel.id}",
            title = "${channel.displayName} (${channel.qualityLabel.ifEmpty { "Standart" }})",
            subtitle = "$currentSourceName • Aktif Hat",
            channel = channel,
            streamUrl = activeUrl,
            userAgent = defaultUa,
            isCurrent = true,
        )
        list.add(activeCandidate)
        _activeCandidate.value = activeCandidate

        // 2. Discover backup channels from same or secondary sources
        val backups = runCatching { catalogRepository.findBackupChannels(channel) }.getOrDefault(emptyList())
        for (backup in backups) {
            val source = if (backup.sourceId == channel.sourceId) currentSource else sourceRepository.byId(backup.sourceId)
            val sourceName = source?.name ?: "Yedek Sağlayıcı"
            val backupUrl = catalogRepository.resolvePlaybackUrl(backup, source)
            if (backupUrl.isNotBlank() && backupUrl != activeUrl && list.none { it.streamUrl == backupUrl }) {
                val qLabel = backup.qualityLabel.ifEmpty {
                    if (backup.name.contains("YEDEK", true) || backup.name.contains("BACKUP", true)) "Yedek"
                    else "Alternatif"
                }
                val isSameSource = backup.sourceId == channel.sourceId
                list.add(
                    StreamCandidate(
                        id = "chan_${backup.id}",
                        title = "${backup.displayName} ($qLabel)",
                        subtitle = if (isSameSource) "Yedek Hat • $sourceName" else "Alternatif Sağlayıcı • $sourceName",
                        channel = backup,
                        streamUrl = backupUrl,
                        userAgent = source?.userAgent ?: defaultUa,
                        isCurrent = false,
                    )
                )
            }
        }

        // 3. Dynamic format alternative (.m3u8 <-> .ts)
        val altUrl = PlayerController.alternateStreamUrl(activeUrl)
        if (altUrl != null && list.none { it.streamUrl == altUrl }) {
            val isM3u8 = altUrl.contains(".m3u8", ignoreCase = true)
            val fmtLabel = if (isM3u8) "HLS (.m3u8)" else "MPEG-TS (.ts)"
            list.add(
                StreamCandidate(
                    id = "alt_fmt",
                    title = "${channel.displayName} ($fmtLabel)",
                    subtitle = "$currentSourceName • Format Alternatifi",
                    channel = channel,
                    streamUrl = altUrl,
                    userAgent = defaultUa,
                    isCurrent = false,
                )
            )
        }

        _candidates.value = list
        Log.i(TAG, "Discovered ${list.size} stream routes for ${channel.displayName}")
    }

    /**
     * Called whenever ExoPlayer enters or changes playbackState.
     */
    fun onPlaybackStateChanged(state: Int, isLive: Boolean) {
        if (!isLive) return

        when (state) {
            Player.STATE_BUFFERING -> {
                recordStallEvent()
                startStallWatchdog()
            }
            Player.STATE_READY -> {
                stallWatchdogJob?.cancel()
                stallWatchdogJob = null
                // If playback remains stable for 12 seconds, reset failover counter
                scope.launch {
                    delay(STABLE_RESET_DELAY_MS)
                    failoverAttempts = 0
                }
            }
            Player.STATE_IDLE, Player.STATE_ENDED -> {
                stallWatchdogJob?.cancel()
                stallWatchdogJob = null
            }
        }
    }

    /**
     * Tracks frequency of buffering events (severe stutter / lag detection).
     * If 3 stalls occur within 15 seconds, trigger failover.
     */
    private fun recordStallEvent() {
        val now = System.currentTimeMillis()
        recentStalls.addLast(now)
        while (recentStalls.isNotEmpty() && now - recentStalls.first() > STALL_WINDOW_MS) {
            recentStalls.removeFirst()
        }
        if (recentStalls.size >= STALL_THRESHOLD_COUNT) {
            Log.w(TAG, "Severe lag detected (3 stalls in 15s). Triggering auto-failover.")
            triggerFailover("Sık takılma ve donma algılandı")
            recentStalls.clear()
        }
    }

    private fun startStallWatchdog() {
        stallWatchdogJob?.cancel()
        stallWatchdogJob = scope.launch {
            val timeout = freezeTimeoutMs
            delay(timeout)
            Log.w(TAG, "Stream stalled for ${timeout}ms. Triggering auto-failover.")
            triggerFailover("Yayın $timeout ms boyunca dondu")
        }
    }

    /**
     * Called when ExoPlayer throws a fatal error.
     * Returns true if failover handled the error by switching to a backup stream.
     */
    fun onPlaybackError(error: PlaybackException): Boolean {
        if (!settings.autoFailoverEnabled.value) return false
        val available = _candidates.value.filter { !it.isCurrent }
        if (available.isEmpty()) return false
        Log.w(TAG, "Stream playback error (${error.errorCodeName}). Attempting failover.")
        return triggerFailover("Bağlantı hatası (${error.errorCodeName})")
    }

    /**
     * Executes automatic failover to the best backup stream candidate.
     */
    fun triggerFailover(reason: String): Boolean {
        if (!settings.autoFailoverEnabled.value) return false
        val all = _candidates.value
        val backups = all.filter { !it.isCurrent }
        if (backups.isEmpty()) {
            Log.d(TAG, "No backup stream candidate available for failover.")
            return false
        }

        // Pick next candidate based on attempt index
        val target = backups[failoverAttempts % backups.size]
        failoverAttempts++

        Log.i(TAG, "Executing auto-failover: '${target.title}' (Reason: $reason)")
        switchCandidate(target, autoTriggered = true, reason = reason)
        return true
    }

    /**
     * Switches playback to a specific candidate (can be auto or manual).
     */
    fun switchCandidate(candidate: StreamCandidate, autoTriggered: Boolean = false, reason: String = "") {
        currentUrl = candidate.streamUrl
        _activeCandidate.value = candidate

        // Update candidate list to mark current
        _candidates.value = _candidates.value.map {
            it.copy(isCurrent = it.id == candidate.id)
        }

        // Emit notice for HUD
        val notice = FailoverNotice(
            title = if (autoTriggered) "🛡️ Donma Kalkanı Devrede" else "Yedek Hata Geçildi",
            details = "${candidate.title} yayınına bağlandı",
            candidate = candidate,
            reason = reason,
        )
        _notice.value = notice

        dismissNoticeJob?.cancel()
        dismissNoticeJob = scope.launch {
            delay(NOTICE_DURATION_MS)
            _notice.value = null
        }

        // Request player controller to play new stream
        onSwitchRequest(
            PlayerController.Request(
                url = candidate.streamUrl,
                title = candidate.title,
                userAgent = candidate.userAgent,
                isLive = true,
            ),
            candidate,
        )
    }

    fun dismissNotice() {
        _notice.value = null
    }

    fun release() {
        stallWatchdogJob?.cancel()
        stallWatchdogJob = null
        dismissNoticeJob?.cancel()
        dismissNoticeJob = null
    }
}
