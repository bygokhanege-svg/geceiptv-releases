/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.Locale

/**
 * Owns the single [ExoPlayer] instance and everything about switching what it is playing.
 *
 * ## Why one player, reused
 *
 * The obvious implementation of "change channel" is release the player and build a new one.
 * On a low-end TV box that takes long enough that a user pressing channel-up four times in a
 * row can queue four constructions and four teardowns, and the codec ends up in a state where
 * nothing plays until the app is killed. That is the "changing channels too quickly causes
 * streams to fail" class of bug, and it is entirely self-inflicted.
 *
 * OpenTV keeps exactly one player for the lifetime of the screen and only ever swaps its
 * media item. Requests are debounced, and an in-flight switch is cancelled the moment a newer
 * one arrives, so holding channel-up costs one actual tune — the one the user stopped on.
 */
@OptIn(UnstableApi::class)
class PlayerController(
    context: Context,
    private val scope: CoroutineScope,
    httpClient: OkHttpClient,
    subtitlesEnabled: Boolean = true,
    /**
     * Tunes the player for the guide's muted preview pane: a shallow start buffer so a highlighted
     * channel shows a frame quickly, and a longer switch debounce so scrolling the channel list
     * does not tune to every channel passed over. The full-screen players leave this false and keep
     * the deep buffer that rides out a twitchy IPTV source.
     */
    private val preview: Boolean = false,
    /**
     * Turns the live player into a shallow DVR: a retained back-buffer so the viewer can pause and
     * rewind live TV within the last few minutes. Off by default because holding minutes of video
     * in memory is a real cost on a cheap box; the recording player and preview never set it.
     */
    private val dvr: Boolean = false,
    /**
     * When set, `smb://` media (a recording on a NAS) is read through this source so it plays and
     * seeks in-app. Null for the live/VOD players, which never see an smb URI.
     */
    smbDataSourceFactory: androidx.media3.datasource.DataSource.Factory? = null,
    /**
     * When set, `optvrec://<id>` media (a recording still being written) is read through this
     * tail-following source so it can be watched while it records. Null everywhere except the
     * recording player.
     */
    growingDataSourceFactory: androidx.media3.datasource.DataSource.Factory? = null,
    /**
     * Playing a recording that is still being written (watch-while-recording). Playback is held a
     * cushion *behind* the file's growing edge (see the LIVE_REC_* constants) so the bursty arrival
     * of an IPTV source — worst when the recording is read back over SMB from a NAS, where each read
     * is a network round-trip — rides over the gaps instead of stalling on every one.
     */
    private val liveRecording: Boolean = false,
    /**
     * Controls whether ExoPlayer registers for and responds to Android system audio focus changes.
     * In Multi-View or preview modes, this must be false so concurrent players never pause or freeze
     * each other when another channel starts playing.
     */
    private val handleAudioFocus: Boolean = !preview,
    val isVod: Boolean = false,
) {

    init {
        register(this)
    }

    /** Channel-surf debounce for this controller — longer for the preview so browsing is calm. */
    private val switchDebounceMillis = if (preview) PREVIEW_SWITCH_DEBOUNCE_MILLIS else SWITCH_DEBOUNCE_MILLIS

    sealed interface State {
        data object Idle : State
        data class Buffering(val title: String) : State
        data class Playing(val title: String) : State
        /** Playback stopped and retries are exhausted. [message] is written for humans. */
        data class Error(val title: String, val message: String, val canRetry: Boolean) : State
    }

    data class Request(
        val url: String,
        val title: String,
        val userAgent: String,
        /** Live streams are never resumed; VOD is. */
        val startPositionMillis: Long = 0L,
        val isLive: Boolean = true,
    )

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** The current stream's tracks (audio/text/video), for the in-player pickers. */
    private val _tracks = MutableStateFlow(Tracks.EMPTY)
    val tracks: StateFlow<Tracks> = _tracks.asStateFlow()

    private var switchJob: Job? = null
    private var current: Request? = null
    private var consecutiveFailures = 0

    var onFormatResolved: ((originalUrl: String, workingUrl: String) -> Unit)? = null
    private var initialRequestUrl: String? = null
    private var triedAlternateFormat: Boolean = false

    /**
     * Held so the User-Agent can be swapped per source before each tune.
     *
     * Providers commonly 403 any client whose UA they do not recognise, and the workaround —
     * "try a different User-Agent" — is useless unless the setting actually reaches the
     * player's HTTP layer. Setting it on the factory applies it to data sources created for
     * subsequent loads, which is exactly the granularity we need.
     */
    private val bandwidthMeter = androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.getSingletonInstance(context)

    private val httpFactory = OkHttpDataSource.Factory(httpClient).apply {
        setDefaultRequestProperties(mapOf("User-Agent" to DEFAULT_USER_AGENT))
        setTransferListener(bandwidthMeter)
    }

    private val dataSourceFactory: androidx.media3.datasource.DataSource.Factory =
        DefaultDataSource.Factory(context, httpFactory).let { default ->
            val base = if (isVod) VodCacheManager.buildCacheDataSourceFactory(context, default) else default
            val custom = buildMap<String, androidx.media3.datasource.DataSource.Factory> {
                if (smbDataSourceFactory != null) put("smb", smbDataSourceFactory)
                if (growingDataSourceFactory != null) put("optvrec", growingDataSourceFactory)
            }
            if (custom.isEmpty()) base else RoutingDataSourceFactory(base, custom)
        }

    /**
     * Retry policy tuned for IPTV rather than for CDNs.
     *
     * A provider under load returns 403, 429 or 500 for a few seconds and then serves the
     * stream perfectly well. ExoPlayer's default is to give up almost immediately on those,
     * which surfaces to the user as a channel that "doesn't work" but plays fine on the
     * second try. We retry them with backoff instead. 401 and 404 are not retried — those
     * genuinely will not fix themselves.
     */
    private val loadErrorPolicy = object : DefaultLoadErrorHandlingPolicy() {
        override fun getRetryDelayMsFor(info: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
            val statusCode =
                (info.exception as? HttpDataSource.InvalidResponseCodeException)?.responseCode

            return when (statusCode) {
                404, 410 -> C_TIME_UNSET
                // In IPTV panels (Xtream/Stalker), seeking or rapid socket changes can temporarily hit
                // the server's concurrent connection limit (active_cons >= max_connections) while the
                // previous socket tears down, producing a transient HTTP 401. Retrying with backoff
                // lets the previous socket clear and resumes playback without crashing.
                401 -> if (info.errorCount <= 3) backoffFor(info.errorCount) else C_TIME_UNSET
                403, 405, 429, 500, 502, 503, 504 -> backoffFor(info.errorCount)
                null -> backoffFor(info.errorCount) // network blips
                else -> super.getRetryDelayMsFor(info)
            }
        }

        override fun getMinimumLoadableRetryCount(dataType: Int): Int = MAX_LOAD_RETRIES

        private fun backoffFor(errorCount: Int): Long =
            if (errorCount > MAX_LOAD_RETRIES) C_TIME_UNSET
            else minOf(INITIAL_BACKOFF_MILLIS * (1L shl (errorCount - 1)), MAX_BACKOFF_MILLIS)
    }

    /**
     * Held so captions can be turned on and off at runtime. Prefer the device language when
     * captions are on; [setSubtitlesEnabled] flips the text renderer off entirely.
     */
    /**
     * Use a customized RenderersFactory with decoder fallback enabled so older TV chipsets
     * (Amlogic, Rockchip, cheap MediaTek) can fail over to secondary hardware/software codecs
     * when high-bitrate 1080p profiles stumble on the primary decoder.
     */
    private val renderersFactory = DefaultRenderersFactory(context).apply {
        setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
        setEnableDecoderFallback(true)
    }

    private val appSettings = runCatching { app.opentv.core.AppSettings.get(context) }.getOrNull()
    private val isTunnelingActive: Boolean = !preview && (appSettings?.tunnelingEnabled?.value == true)

    private val defaultTrackSelector = DefaultTrackSelector(context).apply {
        val prefLang = appSettings?.subtitleLanguage?.value?.takeIf { it.isNotBlank() && it != "auto" }
            ?: java.util.Locale.getDefault().language
        parameters = buildUponParameters()
            .setPreferredTextLanguage(prefLang)
            .setPreferredAudioLanguage(java.util.Locale.getDefault().language)
            .setSelectUndeterminedTextLanguage(true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !subtitlesEnabled)
            .setExceedRendererCapabilitiesIfNecessary(true)
            .setTunnelingEnabled(isTunnelingActive)
            .build()
    }

    private val dynamicMinBuffer = when {
        preview -> PREVIEW_MIN_BUFFER_MILLIS
        isVod -> VOD_MIN_BUFFER_MILLIS
        else -> (appSettings?.streamMinBufferMs?.value ?: MIN_BUFFER_MILLIS)
    }
    private val dynamicMaxBuffer = when {
        preview -> PREVIEW_MAX_BUFFER_MILLIS
        isVod -> (appSettings?.vodMaxBufferMs?.value ?: VOD_MAX_BUFFER_MILLIS)
        dvr -> DVR_MAX_BUFFER_MILLIS
        liveRecording -> LIVE_REC_MAX_BUFFER_MILLIS
        else -> (appSettings?.streamMaxBufferMs?.value ?: MAX_BUFFER_MILLIS)
    }
    private val dynamicPlaybackStartBuffer = when {
        preview -> PREVIEW_BUFFER_FOR_PLAYBACK_MILLIS
        isVod -> VOD_BUFFER_FOR_PLAYBACK_MILLIS
        liveRecording -> LIVE_REC_BUFFER_FOR_PLAYBACK_MILLIS
        else -> (appSettings?.streamStartBufferMs?.value ?: BUFFER_FOR_PLAYBACK_MILLIS)
    }

    val player: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        .setBandwidthMeter(bandwidthMeter)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ handleAudioFocus,
        )
        .setSeekBackIncrementMs(SEEK_INCREMENT_MILLIS)
        .setSeekForwardIncrementMs(SEEK_INCREMENT_MILLIS)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(dataSourceFactory)
                .setLoadErrorHandlingPolicy(loadErrorPolicy),
        )
        .setTrackSelector(defaultTrackSelector)
        .setLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    dynamicMinBuffer,
                    dynamicMaxBuffer,
                    dynamicPlaybackStartBuffer,
                    when {
                        preview -> PREVIEW_BUFFER_AFTER_REBUFFER_MILLIS
                        isVod -> VOD_BUFFER_AFTER_REBUFFER_MILLIS
                        liveRecording -> LIVE_REC_BUFFER_AFTER_REBUFFER_MILLIS
                        else -> (dynamicPlaybackStartBuffer * 2).coerceAtLeast(BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MILLIS)
                    },
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                // Retain the back-buffer for instant rewinds in VOD and live pause in DVR
                .apply {
                    if (isVod) setBackBuffer(VOD_BACK_BUFFER_MILLIS, true)
                    else if (dvr) setBackBuffer(DVR_BACK_BUFFER_MILLIS, true)
                }
                .build(),
        )
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    _tracks.value = tracks
                    ensureAudioTrackSelected(tracks)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    val title = current?.title.orEmpty()
                    _state.value = when (playbackState) {
                        Player.STATE_BUFFERING -> State.Buffering(title)
                        Player.STATE_READY -> {
                            consecutiveFailures = 0
                            val workingUrl = current?.url
                            val origUrl = initialRequestUrl
                            if (origUrl != null && workingUrl != null && origUrl != workingUrl) {
                                android.util.Log.i("PlayerController", "Stream auto-format resolved: $origUrl -> $workingUrl")
                                onFormatResolved?.invoke(origUrl, workingUrl)
                            }
                            State.Playing(title)
                        }
                        Player.STATE_IDLE -> _state.value
                        Player.STATE_ENDED -> State.Idle
                        else -> _state.value
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    val request = current
                    val altUrl = request?.url?.let { alternateStreamUrl(it) }

                    // If tunneling was active and playback hit an error, automatically disable tunneling and retry with standard pipeline
                    if (defaultTrackSelector.parameters.tunnelingEnabled) {
                        android.util.Log.w("PlayerController", "Stream error on ${request?.url} with tunneling active (${error.errorCodeName}). Disabling tunneling for this session and retrying.")
                        defaultTrackSelector.parameters = defaultTrackSelector.buildUponParameters()
                            .setTunnelingEnabled(false)
                            .build()
                        scope.launch {
                            delay(250)
                            if (current == request && request != null) {
                                play(request, debounce = false)
                            }
                        }
                        return
                    }

                    // Smart auto-format fallback: if .m3u8 fails (e.g. 404, parser error), automatically try .ts (and vice versa)
                    if (request != null && !triedAlternateFormat && altUrl != null) {
                        triedAlternateFormat = true
                        android.util.Log.i("PlayerController", "Stream error on ${request.url} (${error.errorCodeName}). Auto-fallback to alternative format: $altUrl")
                        scope.launch {
                            delay(250)
                            if (current == request) {
                                play(request.copy(url = altUrl), debounce = false, isFormatFallback = true)
                            }
                        }
                        return
                    }

                    consecutiveFailures++
                    val lastKnownPosition = player.currentPosition.takeIf { it > 0 }
                        ?: request?.startPositionMillis ?: 0L

                    // If we still have auto-restart attempts, don't disorient the user with a fatal error banner.
                    // Keep buffering/reconnecting and resume at the exact position being watched/seeked.
                    if (consecutiveFailures < MAX_AUTO_RESTARTS && request != null) {
                        _state.value = State.Buffering(request.title)
                        scope.launch {
                            delay(AUTO_RESTART_DELAY_MILLIS)
                            if (current == request) {
                                play(
                                    request.copy(startPositionMillis = lastKnownPosition),
                                    debounce = false,
                                    isFormatFallback = triedAlternateFormat,
                                )
                            }
                        }
                    } else {
                        _state.value = State.Error(
                            title = request?.title.orEmpty(),
                            message = PlaybackErrors.describe(error),
                            canRetry = true,
                        )
                    }
                }
            })
        }

    /**
     * Switches playback.
     *
     * @param debounce when true (the default for channel surfing) the switch waits briefly so
     * that rapid presses collapse into a single tune. Pass false for a deliberate selection.
     */
    fun play(request: Request, debounce: Boolean = true, isFormatFallback: Boolean = false) {
        if (!isFormatFallback) {
            initialRequestUrl = request.url
            triedAlternateFormat = false
        }
        // Cancelling here is what makes fast channel-changing safe: the previous switch never
        // reaches the player, so we never stack prepares.
        switchJob?.cancel()
        current = request

        switchJob = scope.launch {
            if (debounce) delay(switchDebounceMillis)

            consecutiveFailures = 0
            httpFactory.setDefaultRequestProperties(mapOf("User-Agent" to request.userAgent))
            _state.value = State.Buffering(request.title)

            val mediaItem = MediaItem.Builder()
                .setUri(request.url)
                .apply {
                    // Only meaningful for live streams; setting it on VOD skews seeking.
                    if (request.isLive) {
                        setLiveConfiguration(
                            MediaItem.LiveConfiguration.Builder()
                                .setTargetOffsetMs(LIVE_TARGET_OFFSET_MILLIS)
                                .build(),
                        )
                    }
                }
                .build()

            with(player) {
                // stop() rather than release(): keeps the codec and the surface alive.
                stop()
                clearMediaItems()
                setMediaItem(mediaItem)
                if (!preview) volume = 1f
                if (!request.isLive && request.startPositionMillis > 0) {
                    seekTo(request.startPositionMillis)
                }
                playWhenReady = true
                prepare()
            }
        }
    }

    fun retry() {
        current?.let { play(it, debounce = false) }
    }

    /**
     * The quick captions toggle. On is *not* merely "allow the text renderer" — that leaves it
     * to the selector to guess a language, which for a lot of IPTV streams guesses nothing and
     * the user sees no captions even though they turned them on. So On explicitly selects the
     * first available subtitle track; Off disables text entirely.
     */
    fun setSubtitlesEnabled(enabled: Boolean) {
        if (!enabled) {
            disableText()
            return
        }
        val firstText = _tracks.value.groups.firstOrNull { it.type == C.TRACK_TYPE_TEXT && it.isSupported }
        if (firstText != null) {
            selectTrack(firstText, firstSupportedIndex(firstText))
        } else {
            // Nothing to select yet — allow the renderer so a track that arrives later can be
            // picked up, and re-assert once tracks change.
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .build()
        }
    }

    /** Force a specific audio or text track on (used by the in-player pickers). */
    fun selectTrack(group: Tracks.Group, trackIndex: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
            .setTrackTypeDisabled(group.type, false)
            .build()
    }

    /** Turn subtitles off entirely. */
    fun disableText() {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    /** Attach an external subtitle (such as from OpenSubtitles) and enable it immediately. */
    fun injectSubtitle(url: String, language: String, label: String) {
        val currentItem = player.currentMediaItem ?: return
        val currentPos = player.currentPosition
        val wasPlaying = player.isPlaying

        val subtitleConfig = MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(url))
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_SUBRIP)
            .setLanguage(language)
            .setLabel(label)
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        val updated = currentItem.buildUpon()
            .setSubtitleConfigurations(listOf(subtitleConfig))
            .build()

        player.setMediaItem(updated, currentPos)
        player.prepare()
        if (wasPlaying) {
            player.play()
        }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage(language)
            .build()
        setSubtitlesEnabled(true)
    }

    fun seekBackward() {
        if (player.isCurrentMediaItemSeekable) player.seekBack()
    }

    fun seekForward() {
        if (player.isCurrentMediaItemSeekable) player.seekForward()
    }

    fun setPlaybackSpeed(speed: Float) {
        player.setPlaybackSpeed(speed)
    }

    val isSeekable: Boolean get() = player.isCurrentMediaItemSeekable

    data class QualityOption(
        val label: String,
        val selected: Boolean,
        val onClick: () -> Unit,
    )

    private val _qualityHeight = MutableStateFlow<Int?>(null) // null = Auto
    val qualityHeight: StateFlow<Int?> = _qualityHeight.asStateFlow()

    fun setQualityAuto() {
        _qualityHeight.value = null
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
            .setMaxVideoBitrate(Int.MAX_VALUE)
            .build()
    }

    fun setQualityMaxHeight(height: Int) {
        _qualityHeight.value = height
        val maxWidth = when (height) {
            1080 -> 1920
            720 -> 1280
            480 -> 854
            360 -> 640
            else -> (height * 16) / 9
        }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setMaxVideoSize(maxWidth, height)
            .build()
    }

    fun selectVideoTrack(group: Tracks.Group, trackIndex: Int) {
        val format = group.getTrackFormat(trackIndex)
        _qualityHeight.value = format.height.takeIf { it > 0 }
        selectTrack(group, trackIndex)
    }

    fun hasMultipleQualities(tracks: Tracks = _tracks.value): Boolean {
        val videoGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        return videoGroups.sumOf { it.length } > 1
    }

    fun getAvailableQualityOptions(
        tracks: Tracks = _tracks.value,
        autoLabel: String = "Otomatik (En Kararlı)",
        serverStreamLabel: String = "Sunucu Yayını",
        onSelected: () -> Unit = {},
    ): List<QualityOption> {
        val list = mutableListOf<QualityOption>()
        val hasVideoOverride = player.trackSelectionParameters.overrides.any { it.key.type == C.TRACK_TYPE_VIDEO }

        val videoGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO }
        val totalVideoTracks = videoGroups.sumOf { it.length }

        if (totalVideoTracks > 1) {
            // 1. Auto option (Adaptive HLS/DASH)
            list += QualityOption(
                label = autoLabel,
                selected = !hasVideoOverride,
                onClick = {
                    setQualityAuto()
                    onSelected()
                },
            )

            // 2. Real server tracks
            videoGroups.forEach { group ->
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val h = format.height
                    val fps = if (format.frameRate > 0) " ${format.frameRate.toInt()}fps" else ""
                    val br = when {
                        format.bitrate >= 1_000_000 -> " • ${String.format(Locale.US, "%.1f", format.bitrate / 1_000_000f)}Mbps"
                        format.bitrate > 0 -> " • ${format.bitrate / 1_000}kbps"
                        else -> ""
                    }
                    val res = if (h > 0) "${h}p" else "Akış ${i + 1}"
                    val label = "$res$fps$br"
                    list += QualityOption(
                        label = label,
                        selected = group.isTrackSelected(i),
                        onClick = {
                            selectVideoTrack(group, i)
                            onSelected()
                        },
                    )
                }
            }
        } else if (totalVideoTracks == 1) {
            // Single stream track directly from the server — no fake downscaling options
            val group = videoGroups.first()
            val format = group.getTrackFormat(0)
            val h = format.height
            val fps = if (format.frameRate > 0) " ${format.frameRate.toInt()}fps" else ""
            val br = when {
                format.bitrate >= 1_000_000 -> " • ${String.format(Locale.US, "%.1f", format.bitrate / 1_000_000f)}Mbps"
                format.bitrate > 0 -> " • ${format.bitrate / 1_000}kbps"
                else -> ""
            }
            val res = if (h > 0) "${h}p$fps$br" else autoLabel
            val label = if (h > 0) "$res ($serverStreamLabel)" else "$autoLabel ($serverStreamLabel)"
            list += QualityOption(
                label = label,
                selected = true,
                onClick = {
                    setQualityAuto()
                    onSelected()
                },
            )
        } else {
            // Video track info not yet received or raw stream
            list += QualityOption(
                label = "$autoLabel ($serverStreamLabel)",
                selected = true,
                onClick = {
                    setQualityAuto()
                    onSelected()
                },
            )
        }

        return list
    }

    private fun ensureAudioTrackSelected(tracks: Tracks) {
        val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
        if (audioGroups.isEmpty()) return
        val anySupportedSelected = audioGroups.any { g ->
            (0 until g.length).any { g.isTrackSelected(it) && g.isTrackSupported(it) }
        }
        if (!anySupportedSelected) {
            val supportedGroup = audioGroups.firstOrNull { g ->
                (0 until g.length).any { g.isTrackSupported(it) }
            }
            if (supportedGroup != null) {
                val targetIdx = (0 until supportedGroup.length).firstOrNull { supportedGroup.isTrackSupported(it) } ?: 0
                selectTrack(supportedGroup, targetIdx)
            }
        }
    }

    private fun firstSupportedIndex(group: Tracks.Group): Int {
        for (i in 0 until group.length) if (group.isTrackSupported(i)) return i
        return 0
    }

    var volume: Float
        get() = player.volume
        set(value) {
            player.volume = value.coerceIn(0f, 1f)
        }

    fun stop() {
        switchJob?.cancel()
        current = null
        player.stop()
        player.clearMediaItems()
        _state.value = State.Idle
    }

    fun release() {
        unregister(this)
        switchJob?.cancel()
        player.release()
    }

    data class StreamStatsInfo(
        val title: String,
        val resolution: String,
        val fps: String,
        val videoCodec: String,
        val videoBitrate: String,
        val audioCodec: String,
        val audioChannels: String,
        val audioSampleRate: String,
        val connectionSpeed: String,
        val bufferDuration: String,
        val bufferPercentage: Int,
        val containerFormat: String,
        val tunnelingActive: Boolean,
        val estimatedFileSize: String?,
        val streamUrl: String,
    )

    fun getStreamStats(): StreamStatsInfo {
        val req = current
        val vFormat = player.videoFormat
        val aFormat = player.audioFormat

        val width = vFormat?.width ?: 0
        val height = vFormat?.height ?: 0
        val resTag = when {
            height >= 2160 -> "4K Ultra HD"
            height >= 1080 -> "1080p Full HD"
            height >= 720 -> "720p HD"
            height >= 480 -> "480p SD"
            height > 0 -> "${height}p"
            else -> "Otomatik"
        }
        val resolution = if (width > 0 && height > 0) "$width x $height ($resTag)" else resTag

        val frameRate = vFormat?.frameRate ?: 0f
        val fps = if (frameRate > 0) String.format(Locale.US, "%.1f fps", frameRate) else "Otomatik"

        val vBitrate = vFormat?.bitrate?.takeIf { it > 0 } ?: (vFormat?.peakBitrate?.takeIf { it > 0 } ?: 0)
        val videoBitrate = when {
            vBitrate >= 1_000_000 -> String.format(Locale.US, "%.1f Mbps", vBitrate / 1_000_000f)
            vBitrate > 0 -> "${vBitrate / 1_000} kbps"
            else -> "Dinamik Akış"
        }

        val vMime = vFormat?.sampleMimeType.orEmpty().lowercase()
        val videoCodec = when {
            vMime.contains("hevc") || vMime.contains("h265") -> "HEVC / H.265 (Donanım)"
            vMime.contains("avc") || vMime.contains("h264") -> "AVC / H.264 (Donanım)"
            vMime.contains("vp9") -> "VP9 (Donanım)"
            vMime.contains("av01") || vMime.contains("av1") -> "AV1 (MediaCodec)"
            vMime.contains("mpeg2") -> "MPEG-2"
            vFormat?.codecs != null -> vFormat.codecs.orEmpty()
            vMime.isNotBlank() -> vMime.removePrefix("video/")
            else -> "Donanım Kod Çözücü"
        }

        val aMime = aFormat?.sampleMimeType.orEmpty().lowercase()
        val audioCodec = when {
            aMime.contains("eac3") -> "Dolby Digital Plus (E-AC-3)"
            aMime.contains("ac3") -> "Dolby Digital (AC-3)"
            aMime.contains("dts") -> "DTS Digital Surround"
            aMime.contains("truehd") -> "Dolby TrueHD"
            aMime.contains("mp4a") || aMime.contains("aac") -> "AAC Stereo/Audio"
            aMime.contains("opus") -> "Opus Audio"
            aMime.contains("flac") -> "FLAC Lossless"
            aMime.contains("vorbis") -> "Vorbis"
            aMime.contains("mpeg") -> "MP3 / MPEG Audio"
            aMime.isNotBlank() -> aMime.removePrefix("audio/")
            else -> "Standart Ses"
        }

        val channels = aFormat?.channelCount ?: 0
        val audioChannels = when (channels) {
            1 -> "Mono (1.0)"
            2 -> "Stereo (2.0)"
            6 -> "5.1 Surround (6 Kanal)"
            8 -> "7.1 Surround (8 Kanal)"
            else -> if (channels > 0) "$channels Kanal" else "Stereo"
        }
        val sampleRate = aFormat?.sampleRate ?: 0
        val audioSampleRate = if (sampleRate > 0) String.format(Locale.US, "%.1f kHz", sampleRate / 1000f) else "48.0 kHz"

        val bw = bandwidthMeter.bitrateEstimate
        val connectionSpeed = when {
            bw > 1_000_000 -> String.format(Locale.US, "%.1f Mbps", bw / 1_000_000f)
            bw > 0 -> "${bw / 1000} kbps"
            else -> "Hesaplanıyor…"
        }

        val totalBufferMs = player.totalBufferedDuration.coerceAtLeast(0L)
        val bufferDuration = String.format(Locale.US, "%.1f sn", totalBufferMs / 1000f)
        val bufferPercentage = player.bufferedPercentage.coerceIn(0, 100)

        val url = req?.url.orEmpty()
        val containerFormat = when {
            url.contains(".m3u8", ignoreCase = true) -> "HLS (.m3u8 Canlı Akış)"
            url.contains(".ts", ignoreCase = true) -> "MPEG-TS (.ts Taşıyıcı)"
            url.contains(".mpd", ignoreCase = true) -> "DASH (.mpd Akış)"
            url.contains(".mp4", ignoreCase = true) -> "MP4 Video Dosyası"
            url.contains(".mkv", ignoreCase = true) -> "MKV Matroska Dosyası"
            url.contains(".avi", ignoreCase = true) -> "AVI Video Dosyası"
            else -> "HTTP Medya Akışı"
        }

        val durMs = player.duration
        val isVodStream = (req != null && !req.isLive) || isVod
        val estimatedFileSize = if (isVodStream && durMs > 0) {
            val totalBr = (if (vBitrate > 0) vBitrate else 4_000_000) + (aFormat?.bitrate?.takeIf { it > 0 } ?: 192_000)
            val bytes = (totalBr.toLong() * (durMs / 1000L)) / 8L
            when {
                bytes >= 1024L * 1024L * 1024L -> String.format(Locale.US, "%.2f GB", bytes / (1024f * 1024f * 1024f))
                bytes > 0 -> String.format(Locale.US, "%d MB", bytes / (1024L * 1024L))
                else -> null
            }
        } else null

        val tunnelingActive = defaultTrackSelector.parameters.tunnelingEnabled

        return StreamStatsInfo(
            title = req?.title?.ifBlank { "Aktif Akış" } ?: "Aktif Akış",
            resolution = resolution,
            fps = fps,
            videoCodec = videoCodec,
            videoBitrate = videoBitrate,
            audioCodec = audioCodec,
            audioChannels = audioChannels,
            audioSampleRate = audioSampleRate,
            connectionSpeed = connectionSpeed,
            bufferDuration = bufferDuration,
            bufferPercentage = bufferPercentage,
            containerFormat = containerFormat,
            tunnelingActive = tunnelingActive,
            estimatedFileSize = estimatedFileSize,
            streamUrl = url,
        )
    }

    companion object {
        const val DEFAULT_USER_AGENT = "GECEiptv/1.0 (Android)"

        private val activeControllers = java.util.Collections.newSetFromMap(java.util.WeakHashMap<PlayerController, Boolean>())

        fun register(controller: PlayerController) {
            synchronized(activeControllers) { activeControllers.add(controller) }
        }

        fun unregister(controller: PlayerController) {
            synchronized(activeControllers) { activeControllers.remove(controller) }
        }

        /**
         * Pauses all active playback instances across the application.
         * Called when the app transitions to the background on TVs or when leaving foreground.
         */
        fun pauseAll() {
            synchronized(activeControllers) {
                for (controller in activeControllers) {
                    runCatching {
                        controller.player.playWhenReady = false
                        controller.player.pause()
                    }
                }
            }
        }

        /**
         * Stops and releases active resources if needed when the activity is destroyed.
         */
        fun stopAll() {
            synchronized(activeControllers) {
                for (controller in activeControllers) {
                    runCatching {
                        controller.stop()
                    }
                }
            }
        }

        /**
         * Computes the alternate stream format URL (m3u8 <-> ts) for Xtream and IPTV live/vod links.
         */
        fun alternateStreamUrl(url: String): String? {
            val m3u8Pattern = Regex("""\.m3u8(\?.*)?$""", RegexOption.IGNORE_CASE)
            val tsPattern = Regex("""\.ts(\?.*)?$""", RegexOption.IGNORE_CASE)
            return when {
                m3u8Pattern.containsMatchIn(url) -> m3u8Pattern.replace(url) { ".ts" + (it.groups[1]?.value ?: "") }
                tsPattern.containsMatchIn(url) -> tsPattern.replace(url) { ".m3u8" + (it.groups[1]?.value ?: "") }
                else -> null
            }
        }

        /** ExoPlayer's "do not retry" sentinel. */
        const val C_TIME_UNSET = androidx.media3.common.C.TIME_UNSET

        const val SWITCH_DEBOUNCE_MILLIS = 350L
        const val SEEK_INCREMENT_MILLIS = 15_000L
        const val INITIAL_BACKOFF_MILLIS = 500L
        const val MAX_BACKOFF_MILLIS = 8_000L
        const val MAX_LOAD_RETRIES = 5
        const val MAX_AUTO_RESTARTS = 3
        const val AUTO_RESTART_DELAY_MILLIS = 1_500L

        /**
         * IPTV stream buffer tuning.
         * Previously was 60 seconds (60,000ms), which caused memory exhaustion and Garbage Collection
         * freezes on low-end Android TV boxes with 1GB/2GB RAM when decoding high-bitrate 1080p streams.
         * Tuned to 15s max buffer with rapid start (1s) to guarantee smooth playback without RAM bloat.
         */
        const val MIN_BUFFER_MILLIS = 8_000
        const val MAX_BUFFER_MILLIS = 15_000
        const val BUFFER_FOR_PLAYBACK_MILLIS = 1_000
        const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MILLIS = 2_000

        /**
         * VOD (Film / Dizi) stream buffer tuning (YouTube-style forward buffering):
         * Starts playback fast (1.5s), keeps 30s min, and pre-buffers up to 90s (user-configurable up to 120s)
         * in the background. If internet drops/stutters, playback stays buttery smooth.
         * Also keeps 30s back-buffer so instant rewinds do not hit the IPTV server.
         */
        const val VOD_MIN_BUFFER_MILLIS = 30_000
        const val VOD_MAX_BUFFER_MILLIS = 90_000
        const val VOD_BUFFER_FOR_PLAYBACK_MILLIS = 1_500
        const val VOD_BUFFER_AFTER_REBUFFER_MILLIS = 3_000
        const val VOD_BACK_BUFFER_MILLIS = 30_000

        // DVR mode (pause & rewind live TV): keep 60s behind live edge to balance seekability with RAM.
        const val DVR_BACK_BUFFER_MILLIS = 60_000
        const val DVR_MAX_BUFFER_MILLIS = 60_000

        // Watch-while-recording: hold playback a moderate cushion behind the file's growing edge.
        const val LIVE_REC_MAX_BUFFER_MILLIS = 30_000
        const val LIVE_REC_BUFFER_FOR_PLAYBACK_MILLIS = 4_000
        const val LIVE_REC_BUFFER_AFTER_REBUFFER_MILLIS = 4_000

        // Guide-preview tuning: start fast and don't hoard buffer.
        const val PREVIEW_MIN_BUFFER_MILLIS = 3_000
        const val PREVIEW_MAX_BUFFER_MILLIS = 8_000
        const val PREVIEW_BUFFER_FOR_PLAYBACK_MILLIS = 800
        const val PREVIEW_BUFFER_AFTER_REBUFFER_MILLIS = 1_200
        const val PREVIEW_SWITCH_DEBOUNCE_MILLIS = 700L

        const val LIVE_TARGET_OFFSET_MILLIS = 8_000L
    }
}
