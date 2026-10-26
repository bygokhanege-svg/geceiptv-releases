/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.vod

import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.ui.AspectRatioFrameLayout
import app.opentv.data.model.Episode
import app.opentv.player.OnlineSubtitle
import app.opentv.ui.player.PlayerClockBadge
import app.opentv.ui.player.StreamStatsDialog
import app.opentv.ui.theme.GecePalette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import app.opentv.R
import app.opentv.core.ServiceLocator
import app.opentv.core.SleepTimer
import app.opentv.core.findActivity
import app.opentv.player.PlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AspectRatioMode(val title: String, val shortLabel: String) {
    FIT("Sayfaya Sığdır (Orijinal)", "Sığdır"),
    RATIO_16_9("16:9 Geniş Ekran", "16:9"),
    RATIO_4_3("4:3 Klasik TV", "4:3"),
    ZOOM("Full Ekran (Zoom)", "Full Ekran"),
    STRETCH("Ekrana Yay (Uzat)", "Uzat"),
}

data class ExternalAppInfo(
    val name: String,
    val packageName: String,
    val activityName: String?,
    val icon: Drawable?,
)

private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is BitmapDrawable && drawable.bitmap != null) {
        return drawable.bitmap
    }
    return try {
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    } catch (_: Exception) {
        null
    }
}

private fun detectExternalPlayers(context: Context): List<ExternalAppInfo> {
    val pm = context.packageManager
    val players = mutableListOf<ExternalAppInfo>()
    val seenPackages = mutableSetOf<String>()

    val viewIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(Uri.parse("http://example.com/video.mp4"), "video/*")
    }
    val resolveList = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(viewIntent, PackageManager.ResolveInfoFlags.of(0L))
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(viewIntent, 0)
    }

    for (resolve in resolveList) {
        val pkg = resolve.activityInfo.packageName
        if (pkg != context.packageName && seenPackages.add(pkg)) {
            val label = resolve.loadLabel(pm).toString()
            val icon = resolve.loadIcon(pm)
            players.add(ExternalAppInfo(label, pkg, resolve.activityInfo.name, icon))
        }
    }

    val wellKnown = listOf(
        "org.videolan.vlc" to "VLC for Android",
        "com.mxtech.videoplayer.ad" to "MX Player",
        "com.mxtech.videoplayer.pro" to "MX Player Pro",
        "com.brouken.player" to "Just Player",
        "org.xbmc.kodi" to "Kodi",
        "is.xyz.mpv" to "mpv-android",
        "org.courville.nova" to "Nova Video Player",
        "com.kmplayer" to "KMPlayer",
        "com.inshot.xplayer" to "XPlayer",
        "com.bsplayer.bspandroid.free" to "BSPlayer",
    )

    for ((pkg, defaultName) in wellKnown) {
        if (!seenPackages.contains(pkg)) {
            try {
                val appInfo = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0L))
                } else {
                    @Suppress("DEPRECATION")
                    pm.getApplicationInfo(pkg, 0)
                }
                if (appInfo.enabled) {
                    seenPackages.add(pkg)
                    val label = pm.getApplicationLabel(appInfo).toString().ifBlank { defaultName }
                    val icon = pm.getApplicationIcon(appInfo)
                    players.add(ExternalAppInfo(label, pkg, null, icon))
                }
            } catch (_: Exception) {}
        }
    }

    return players
}

/**
 * Plays a movie or episode: a single non-live stream, with resume and a proper transport bar.
 *
 * The bar is drawn in Compose rather than left to the Media3 view's own controller, because that
 * controller needs the embedded view to hold d-pad focus to be summoned — which it doesn't, on a
 * TV inside Compose, so it reads as "no controls". This is the same self-drawn, auto-hiding bar as
 * the live player, with a seek bar added since a film needs scrubbing.
 */
@OptIn(UnstableApi::class)
@Composable
fun VodPlayerScreen(
    mediaKey: String,
    streamUrl: String,
    title: String,
    userAgent: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isTelevision = remember(context) { app.opentv.isRunningOnTelevision(context) }
    DisposableEffect(isTelevision) {
        val act = context.findActivity()
        if (!isTelevision) {
            act?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
        onDispose {
            if (!isTelevision) {
                act?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
            }
        }
    }
    val graph = remember { ServiceLocator.get(context) }
    val settings = remember { graph.settings }
    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    var currentMediaKey by remember(mediaKey) { mutableStateOf(mediaKey) }
    var currentStreamUrl by remember(streamUrl) { mutableStateOf(streamUrl) }
    var currentTitle by remember(title) { mutableStateOf(title) }
    var nextEpisode by remember { mutableStateOf<Episode?>(null) }

    // A recording that's still being written plays through the tail-following source. It behaves a
    // little differently in the transport: the "length" is how much has been recorded so far, which
    // keeps growing, and seeking is bounded to that recorded extent.
    val growingRec = remember(currentStreamUrl) { currentStreamUrl.startsWith("optvrec://") }
    // The SMB source lets a recording stored on a NAS play and seek in-app; harmless for the
    // http/file URLs of ordinary VOD.
    val controller = remember {
        PlayerController(
            context, scope, graph.streamingHttpClient, subtitlesEnabled = false,
            smbDataSourceFactory = app.opentv.player.SmbDataSource.Factory(graph.settings),
            // Lets an `optvrec://<id>` recording play while it's still being written.
            growingDataSourceFactory =
                app.opentv.player.GrowingRecordingDataSource.Factory(context.applicationContext),
            liveRecording = growingRec,
            isVod = !growingRec,
        ).apply {
            onFormatResolved = { _, workingUrl ->
                currentStreamUrl = workingUrl
            }
        }
    }
    var positionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedMs by remember { mutableLongStateOf(0L) }
    var skipOverlayText by remember { mutableStateOf<String?>(null) }
    val playerClockEnabled by settings.playerClockEnabled.collectAsState()
    var showStatsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(skipOverlayText) {
        if (skipOverlayText != null) {
            delay(1000)
            skipOverlayText = null
        }
    }

    var pendingSeekTargetMs by remember { mutableStateOf<Long?>(null) }
    var seekDebounceJob by remember { mutableStateOf<Job?>(null) }
    var effectiveUserAgent by remember(userAgent) { mutableStateOf(userAgent) }

    // Skip forward/back with smooth accumulation and debounce to prevent sending multiple
    // rapid Range requests that trigger provider concurrent connection limits ("401 check username").
    fun seekRelative(deltaMs: Long) {
        val p = controller.player
        val ceiling = if (growingRec) p.bufferedPosition.coerceAtLeast(0L)
        else (p.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)

        val basePos = pendingSeekTargetMs ?: p.currentPosition
        val target = (basePos + deltaMs).coerceIn(0L, ceiling)
        pendingSeekTargetMs = target
        positionMs = target

        val diffMs = target - p.currentPosition
        val diffSec = diffMs / 1000
        skipOverlayText = if (diffSec >= 0) "+$diffSec sn" else "$diffSec sn"

        seekDebounceJob?.cancel()
        seekDebounceJob = scope.launch {
            delay(400)
            val finalTarget = pendingSeekTargetMs ?: target
            pendingSeekTargetMs = null
            p.seekTo(finalTarget)
        }
    }
    val state by controller.state.collectAsState()
    val tracks by controller.tracks.collectAsState()
    val nextEpisodePromptEnabled by settings.nextEpisodePromptEnabled.collectAsState()
    val inPip by app.opentv.core.PipState.inPip.collectAsState()

    var paused by remember { mutableStateOf(false) }
    var vodPanel by remember { mutableStateOf(VodPanel.NONE) }
    val panelFocus = remember { FocusRequester() }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val barFocus = remember { FocusRequester() }
    val rootFocus = remember { FocusRequester() }
    val episodesDrawerFocus = remember { FocusRequester() }

    var posterUrl by remember { mutableStateOf<String?>(null) }
    var plot by remember { mutableStateOf<String?>(null) }
    var rating by remember { mutableStateOf<Double?>(null) }
    var seasonNumber by remember { mutableStateOf<Int?>(null) }
    var episodeNumber by remember { mutableStateOf<Int?>(null) }
    var movieImdbId by remember { mutableStateOf<String?>(null) }
    var parentSeriesTitle by remember { mutableStateOf<String?>(null) }
    var movieName by remember { mutableStateOf<String?>(null) }
    var onlineSubtitles by remember { mutableStateOf<List<OnlineSubtitle>>(emptyList()) }
    var isSearchingSubtitles by remember { mutableStateOf(false) }
    var subtitleSearchMessage by remember { mutableStateOf<String?>(null) }
    var selectedOnlineSubId by remember { mutableStateOf<String?>(null) }

    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var aspectMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var seriesEpisodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
    var currentEpisodeId by remember { mutableStateOf<Long?>(null) }
    var showEpisodesDrawer by remember { mutableStateOf(false) }
    var selectedSeasonTab by remember { mutableStateOf(1) }
    var showExternalChooser by remember { mutableStateOf(false) }
    val detectedPlayers = remember { detectExternalPlayers(context) }

    LaunchedEffect(currentMediaKey, currentStreamUrl) {
        withContext(Dispatchers.IO) {
            val parts = currentMediaKey.split(":", limit = 2)
            val id = parts.getOrNull(1)?.toLongOrNull()
            var currentEp: Episode? = null
            var currentMovie: app.opentv.data.model.Movie? = null
            if (parts[0] == "ep" && id != null) {
                currentEp = graph.catalogRepository.episode(id)
            }
            if (currentEp == null && parts[0] == "ep") {
                currentEp = graph.catalogRepository.episodeByStreamUrl(currentStreamUrl)
            }

            if (currentEp != null) {
                posterUrl = currentEp.stillUrl
                plot = currentEp.plot
                rating = null
                seasonNumber = currentEp.season
                episodeNumber = currentEp.episodeNumber
                movieName = null
                currentEpisodeId = currentEp.id
                selectedSeasonTab = currentEp.season

                val series = graph.catalogRepository.seriesBySeriesId(currentEp.sourceId, currentEp.seriesId)
                parentSeriesTitle = series?.name
                movieImdbId = series?.let { s -> graph.catalogRepository.imdbIdForSeries(s) }

                val allEps = graph.catalogRepository.episodesForSeries(currentEp.sourceId, currentEp.seriesId)
                val sorted = allEps.sortedWith(compareBy({ it.season }, { it.episodeNumber }))
                seriesEpisodes = sorted
                val curIdx = sorted.indexOfFirst { it.id == currentEp.id }
                nextEpisode = if (curIdx != -1 && curIdx + 1 < sorted.size) {
                    sorted[curIdx + 1]
                } else {
                    null
                }
            } else if (parts[0] == "movie" && id != null) {
                val m = graph.catalogRepository.movie(id)
                currentMovie = m
                posterUrl = m?.posterUrl
                plot = m?.plot
                rating = m?.rating
                movieName = m?.name
                movieImdbId = m?.let { graph.catalogRepository.imdbIdFor(it) }
                parentSeriesTitle = null
                seasonNumber = null
                episodeNumber = null
                nextEpisode = null
                seriesEpisodes = emptyList()
                currentEpisodeId = null
            } else {
                movieName = null
                parentSeriesTitle = null
                nextEpisode = null
                seriesEpisodes = emptyList()
                currentEpisodeId = null
            }

            val srcId = currentEp?.sourceId ?: currentMovie?.sourceId
            if (srcId != null) {
                val src = graph.sourceRepository.byId(srcId)
                if (src != null && src.userAgent.isNotBlank() && src.userAgent != app.opentv.data.model.Source.DEFAULT_USER_AGENT) {
                    effectiveUserAgent = src.userAgent
                }
            }
        }
    }

    fun reveal() { controlsVisible = true; interaction++ }

    suspend fun savePosition() {
        val player = controller.player
        val pos = player.currentPosition
        val dur = player.duration.takeIf { it > 0 } ?: return
        if (pos > 5_000) {
            graph.playbackPositions.upsert(
                app.opentv.data.model.PlaybackPosition(
                    profileId = settings.activeProfileId.value,
                    mediaKey = currentMediaKey,
                    positionMillis = pos,
                    durationMillis = dur,
                    updatedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun playEpisode(ep: Episode) {
        scope.launch {
            savePosition()
            currentMediaKey = "ep:${ep.id}"
            currentStreamUrl = ep.streamUrl
            currentTitle = "S${ep.season}E${ep.episodeNumber} · ${ep.title}"
            currentEpisodeId = ep.id
            selectedSeasonTab = ep.season
            paused = false
            onlineSubtitles = emptyList()
            selectedOnlineSubId = null
            subtitleSearchMessage = null
            showEpisodesDrawer = false
            reveal()
        }
    }

    fun playNextEpisode() {
        val next = nextEpisode ?: return
        playEpisode(next)
    }

    fun launchPlayer(app: ExternalAppInfo) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(currentStreamUrl), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("title", currentTitle)
                putExtra("secure_uri", true)
                if (app.activityName != null) {
                    setClassName(app.packageName, app.activityName)
                } else {
                    setPackage(app.packageName)
                }
                if (effectiveUserAgent.isNotBlank()) {
                    putExtra("headers", arrayOf("User-Agent", effectiveUserAgent))
                }
            }
            paused = true
            controller.player.playWhenReady = false
            controller.player.pause()
            context.startActivity(intent)
        }
    }

    fun launchExternalPlayer() {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(currentStreamUrl), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("title", currentTitle)
                putExtra("secure_uri", true)
                if (effectiveUserAgent.isNotBlank()) {
                    putExtra("headers", arrayOf("User-Agent", effectiveUserAgent))
                }
            }
            val chooser = Intent.createChooser(intent, "Harici Oynatıcı Seçin (VLC / MX)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            paused = true
            controller.player.playWhenReady = false
            controller.player.pause()
            context.startActivity(chooser)
        }
    }

    fun searchOnlineSubtitles() {
        if (isSearchingSubtitles) return
        isSearchingSubtitles = true
        val searchTitle = if (seasonNumber != null && episodeNumber != null) {
            parentSeriesTitle ?: currentTitle
        } else {
            movieName ?: currentTitle
        }
        subtitleSearchMessage = context.getString(R.string.vod_sub_searching)
        scope.launch {
            val enabledProviders = mutableListOf<String>()
            if (graph.settings.providerOpenSubtitles.value) enabledProviders += "OpenSubtitles v3"
            if (graph.settings.providerSubDL.value) enabledProviders += "TheSubtitleDB"
            if (graph.settings.providerCommunity.value) enabledProviders += "Top Community"

            val results = withContext(Dispatchers.IO) {
                graph.subtitleService.searchSubtitles(
                    title = searchTitle,
                    season = seasonNumber,
                    episode = episodeNumber,
                    imdbId = movieImdbId,
                    enabledProviders = enabledProviders,
                )
            }
            onlineSubtitles = results
            isSearchingSubtitles = false
            subtitleSearchMessage = if (results.isEmpty()) {
                context.getString(R.string.vod_sub_not_found, searchTitle)
            } else {
                context.getString(R.string.vod_sub_found, results.size)
            }
        }
    }

    // Keep the screen awake during playback — see the note in PlayerScreen; a film is exactly when
    // the screensaver must not fire. Window flag is the reliable path; keepScreenOn is a backstop.
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view.keepScreenOn = true
        onDispose {
            if (!isTelevision) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                view.keepScreenOn = false
            }
            scope.launch { savePosition() }
            controller.release()
            scope.cancel()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    val act = context.findActivity()
                    val inPip = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N &&
                        (act?.isInPictureInPictureMode == true)
                    if (!inPip) {
                        paused = true
                        controller.player.playWhenReady = false
                        controller.player.pause()
                        scope.launch { savePosition() }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(currentMediaKey, effectiveUserAgent) {
        val resumeFrom = graph.playbackPositions.get(settings.activeProfileId.value, currentMediaKey)
            ?.takeIf { !it.isFinished }?.positionMillis ?: 0L
        controller.play(
            PlayerController.Request(
                url = currentStreamUrl,
                title = currentTitle,
                userAgent = effectiveUserAgent,
                startPositionMillis = resumeFrom,
                isLive = false,
            ),
            debounce = false,
        )
        while (isActive) {
            delay(15_000)
            savePosition()
        }
    }

    // Sleep timer: leave the film when the armed deadline passes (position is saved on dispose).
    val sleepDeadline by SleepTimer.deadline.collectAsState()
    LaunchedEffect(sleepDeadline) {
        val d = sleepDeadline ?: return@LaunchedEffect
        val wait = d - System.currentTimeMillis()
        if (wait > 0) delay(wait)
        SleepTimer.clear()
        onBack()
    }

    // Poll position/duration for the seek bar while the film plays.
    LaunchedEffect(Unit) {
        while (isActive) {
            if (!scrubbing && pendingSeekTargetMs == null) {
                positionMs = controller.player.currentPosition.coerceAtLeast(0)
                // A growing recording has no fixed length; the recorded-so-far extent (how far you
                // can skip ahead) is what's been read into the buffer.
                durationMs = if (growingRec) controller.player.bufferedPosition.coerceAtLeast(0)
                else controller.player.duration.takeIf { it > 0 } ?: 0
                bufferedMs = controller.player.bufferedPosition.coerceAtLeast(0)
            }
            delay(500)
        }
    }

    // Auto-hide when playing and not paused/scrubbing and no picker open. A growing recording dips in
    // and out of Buffering as it rides the write head, so for that case Buffering counts as "playing"
    // here — otherwise a single hiccup would pin the transport bar on screen for the rest of the watch.
    LaunchedEffect(controlsVisible, interaction, state, paused, scrubbing, vodPanel, showEpisodesDrawer, showExternalChooser) {
        val activelyPlaying = state is PlayerController.State.Playing ||
            (growingRec && state is PlayerController.State.Buffering)
        if (controlsVisible && !paused && !scrubbing && vodPanel == VodPanel.NONE && !showEpisodesDrawer && !showExternalChooser && activelyPlaying) {
            delay(5_000)
            controlsVisible = false
        }
    }

    LaunchedEffect(controlsVisible, vodPanel, showEpisodesDrawer) {
        if (showEpisodesDrawer) {
            delay(60)
            runCatching { episodesDrawerFocus.requestFocus() }
        } else if (controlsVisible) {
            delay(40)
            runCatching { if (vodPanel != VodPanel.NONE) panelFocus.requestFocus() else barFocus.requestFocus() }
        } else {
            runCatching { rootFocus.requestFocus() }
        }
    }

    BackHandler {
        val activelyPlaying = state is PlayerController.State.Playing ||
            (growingRec && state is PlayerController.State.Buffering)
        when {
            showStatsDialog -> showStatsDialog = false
            showExternalChooser -> showExternalChooser = false
            showEpisodesDrawer -> showEpisodesDrawer = false
            vodPanel != VodPanel.NONE -> vodPanel = VodPanel.NONE
            controlsVisible && activelyPlaying && !paused -> controlsVisible = false
            else -> onBack()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                when {
                    event.key == Key.Back || event.key == Key.Escape -> false
                    event.type == KeyEventType.KeyDown && !controlsVisible -> {
                        if (event.key == Key.DirectionRight || event.key == Key.DirectionLeft) {
                            val sign = if (event.key == Key.DirectionRight) 1 else -1
                            val delta = 10_000L * sign
                            seekRelative(delta)
                            skipOverlayText = if (sign > 0) "+10 sn" else "-10 sn"
                            true
                        } else {
                            reveal()
                            true
                        }
                    }
                    event.type == KeyEventType.KeyDown -> { interaction++; false }
                    else -> false
                }
            }
            .focusRequester(rootFocus)
            .focusable()
            .pointerInput(Unit) {
                detectTapGestures { if (controlsVisible) controlsVisible = false else reveal() }
            },
    ) {
        val aspectModifier = when (aspectMode) {
            AspectRatioMode.RATIO_16_9 -> Modifier.aspectRatio(16f / 9f)
            AspectRatioMode.RATIO_4_3 -> Modifier.aspectRatio(4f / 3f)
            else -> Modifier.fillMaxSize()
        }
        val exoResizeMode = when (aspectMode) {
            AspectRatioMode.ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            AspectRatioMode.RATIO_16_9, AspectRatioMode.RATIO_4_3 -> AspectRatioFrameLayout.RESIZE_MODE_FILL
            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AndroidView(
                modifier = aspectModifier,
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = controller.player
                        useController = false
                        keepScreenOn = true
                        setKeepContentOnPlayerReset(true)
                        this.resizeMode = exoResizeMode
                        app.opentv.ui.settings.SubtitleStyleHelper.applySubtitlePreferences(subtitleView, graph.settings)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { it.resizeMode = exoResizeMode },
            )
        }

        // Top-Right Live Clock Badge
        if (playerClockEnabled && !inPip) {
            PlayerClockBadge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 28.dp, end = 32.dp),
                onClick = { showStatsDialog = true },
            )
        }

        when (val current = state) {
            is PlayerController.State.Buffering ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(current.title, color = Color.White)
                    }
                }

            is PlayerController.State.Error ->
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(48.dp),
                    ) {
                        Text(current.title, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Spacer(Modifier.height(12.dp))
                        Text(current.message, color = Color.White.copy(alpha = 0.85f), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(24.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(onClick = { controller.retry() }) { Text(stringResource(R.string.common_try_again)) }
                            Spacer(Modifier.width(16.dp))
                            Button(onClick = { launchExternalPlayer() }) { Text("Harici Oynatıcı ile Aç (VLC / MX)") }
                        }
                    }
                }

            else -> Unit
        }

        if (skipOverlayText != null) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(24.dp)
            ) {
                Text(skipOverlayText!!, color = Color.White, style = MaterialTheme.typography.displaySmall)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.92f))))
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                // Info OSD (Poster and Plot)
                if (posterUrl != null || plot != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        if (posterUrl != null) {
                            AsyncImage(
                                model = posterUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .height(130.dp)
                                    .width(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.width(14.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            if (rating != null && rating!! > 0.0) {
                                Text(
                                    "IMDB: ${rating}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFFFC107),
                                    modifier = Modifier.padding(bottom = 3.dp)
                                )
                            }
                            if (plot != null) {
                                Text(
                                    plot!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 3,
                                )
                            }
                        }
                    }
                }

                if (vodPanel != VodPanel.NONE) {
                    val currentOffsetMs by graph.subtitleSyncManager.currentOffsetMs.collectAsState()
                    TrackPanel(
                        panel = vodPanel,
                        controller = controller,
                        tracks = tracks,
                        firstFocus = panelFocus,
                        playbackSpeed = playbackSpeed,
                        onSelectSpeed = { spd ->
                            playbackSpeed = spd
                            controller.setPlaybackSpeed(spd)
                        },
                        aspectMode = aspectMode,
                        onSelectAspect = { mode ->
                            aspectMode = mode
                        },
                        onlineSubtitles = onlineSubtitles,
                        isSearchingSubtitles = isSearchingSubtitles,
                        subtitleSearchMessage = subtitleSearchMessage,
                        selectedOnlineSubId = selectedOnlineSubId,
                        currentOffsetMs = currentOffsetMs,
                        onAdjustSyncOffset = { delta ->
                            scope.launch {
                                val activeSub = graph.subtitleSyncManager.activeSubtitle.value
                                val shiftedFile = graph.subtitleSyncManager.adjustOffset(context, delta)
                                if (shiftedFile != null && activeSub != null) {
                                    controller.injectSubtitle("file://${shiftedFile.absolutePath}", activeSub.lang, activeSub.langLabel)
                                }
                            }
                        },
                        onResetSyncOffset = {
                            scope.launch {
                                val activeSub = graph.subtitleSyncManager.activeSubtitle.value
                                val resetFile = graph.subtitleSyncManager.resetOffset(context)
                                if (resetFile != null && activeSub != null) {
                                    controller.injectSubtitle("file://${resetFile.absolutePath}", activeSub.lang, activeSub.langLabel)
                                }
                            }
                        },
                        onApplyFpsCorrection = { from23to25 ->
                            scope.launch {
                                val activeSub = graph.subtitleSyncManager.activeSubtitle.value
                                val correctedFile = graph.subtitleSyncManager.applyFpsCorrection(context, from23to25)
                                if (correctedFile != null && activeSub != null) {
                                    controller.injectSubtitle("file://${correctedFile.absolutePath}", activeSub.lang, activeSub.langLabel)
                                }
                            }
                        },
                        onSearchOnlineSubtitles = { searchOnlineSubtitles() },
                        onSelectOnlineSubtitle = { sub ->
                            selectedOnlineSubId = sub.id
                            scope.launch {
                                val preparedFile = graph.subtitleSyncManager.downloadAndPrepare(context, sub)
                                if (preparedFile != null) {
                                    controller.injectSubtitle("file://${preparedFile.absolutePath}", sub.lang, sub.langLabel)
                                } else {
                                    controller.injectSubtitle(sub.url, sub.lang, sub.langLabel)
                                }
                            }
                        },
                        onClearOnlineSubtitle = {
                            selectedOnlineSubId = null
                            graph.subtitleSyncManager.clear()
                        },
                        onDone = { vodPanel = VodPanel.NONE; interaction++ },
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (growingRec) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.FiberManualRecord,
                            contentDescription = null,
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(R.string.rec_watching_live_badge),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE53935),
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                }
                Text(currentTitle, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                Spacer(Modifier.height(6.dp))

                // Seek bar with times either side. Left/right on the remote scrubs.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDuration(if (scrubbing) scrubValue.toLong() else positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        // YouTube-style background buffer track (shows forward buffered extent ahead of playback)
                        if (durationMs > 0 && bufferedMs > 0) {
                            val bufferFraction = (bufferedMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(bufferFraction)
                                    .height(4.dp)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.35f))
                            )
                        }
                        Slider(
                            value = if (scrubbing) scrubValue else (pendingSeekTargetMs?.toFloat() ?: positionMs.toFloat()),
                            valueRange = 0f..(durationMs.takeIf { it > 0 }?.toFloat() ?: 1f),
                            onValueChange = { scrubbing = true; scrubValue = it; interaction++ },
                            onValueChangeFinished = {
                                val target = scrubValue.toLong()
                                positionMs = target
                                scrubbing = false
                                seekDebounceJob?.cancel()
                                seekDebounceJob = scope.launch {
                                    delay(300)
                                    controller.player.seekTo(target)
                                }
                                interaction++
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        formatDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    RoundMediaButton(
                        icon = Icons.Filled.FastRewind,
                        contentDescription = stringResource(R.string.player_rewind),
                        size = 38.dp,
                        iconSize = 20.dp,
                        onClick = {
                            seekRelative(-15_000)
                            interaction++
                        },
                    )
                    RoundMediaButton(
                        icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = if (paused) stringResource(R.string.common_play) else stringResource(R.string.player_pause),
                        size = 46.dp,
                        iconSize = 26.dp,
                        isPrimary = true,
                        focusRequester = barFocus,
                        onClick = {
                            paused = !paused
                            controller.player.playWhenReady = !paused
                            interaction++
                        },
                    )
                    RoundMediaButton(
                        icon = Icons.Filled.FastForward,
                        contentDescription = stringResource(R.string.player_forward),
                        size = 38.dp,
                        iconSize = 20.dp,
                        onClick = {
                            seekRelative(15_000)
                            interaction++
                        },
                    )

                    Spacer(Modifier.width(10.dp))

                    VodChip(
                        icon = Icons.Filled.ClosedCaption,
                        label = stringResource(R.string.player_subtitles),
                        onClick = {
                            vodPanel = if (vodPanel == VodPanel.SUBTITLES) VodPanel.NONE else VodPanel.SUBTITLES
                            interaction++
                        },
                    )
                    VodChip(
                        icon = Icons.Filled.Audiotrack,
                        label = stringResource(R.string.player_audio),
                        onClick = {
                            vodPanel = if (vodPanel == VodPanel.AUDIO) VodPanel.NONE else VodPanel.AUDIO
                            interaction++
                        },
                    )
                    VodChip(
                        icon = Icons.Filled.Speed,
                        label = if (playbackSpeed == 1.0f) stringResource(R.string.player_speed) else "${playbackSpeed}x",
                        onClick = {
                            vodPanel = if (vodPanel == VodPanel.SPEED) VodPanel.NONE else VodPanel.SPEED
                            interaction++
                        },
                    )
                    VodChip(
                        icon = Icons.Filled.AspectRatio,
                        label = aspectMode.shortLabel,
                        onClick = {
                            vodPanel = if (vodPanel == VodPanel.ASPECT) VodPanel.NONE else VodPanel.ASPECT
                            interaction++
                        },
                    )
                    VodChip(
                        icon = Icons.Filled.OpenInNew,
                        label = stringResource(R.string.vod_external_player),
                        onClick = {
                            showExternalChooser = true
                            interaction++
                        },
                    )
                    VodChip(
                        icon = Icons.Filled.Analytics,
                        label = "Info",
                        onClick = {
                            showStatsDialog = true
                            interaction++
                        },
                    )
                    if (seriesEpisodes.isNotEmpty()) {
                        VodChip(
                            icon = Icons.Filled.VideoLibrary,
                            label = stringResource(R.string.player_episodes),
                            selected = showEpisodesDrawer,
                            onClick = {
                                showEpisodesDrawer = !showEpisodesDrawer
                                interaction++
                                if (showEpisodesDrawer) {
                                    scope.launch {
                                        delay(80)
                                        runCatching { episodesDrawerFocus.requestFocus() }
                                    }
                                }
                            },
                        )
                    }
                    if (nextEpisodePromptEnabled && nextEpisode != null) {
                        VodChip(
                            icon = Icons.Filled.SkipNext,
                            label = stringResource(R.string.vod_next_episode_tag, nextEpisode!!.season, nextEpisode!!.episodeNumber),
                            onClick = {
                                playNextEpisode()
                                interaction++
                            },
                        )
                    }
                }
            }
        }

        // Netflix-style Floating Next Episode Card in Bottom-Right
        val remainingMs = if (durationMs > 0 && positionMs > 0) durationMs - positionMs else Long.MAX_VALUE
        val isNearEnd = remainingMs in 1..40_000L
        val isFinished = state is PlayerController.State.Idle && positionMs > 10_000L
        val showNextEpisodeCard = nextEpisodePromptEnabled && nextEpisode != null && (isNearEnd || isFinished)

        val targetBottomPadding by animateDpAsState(
            targetValue = if (controlsVisible) 160.dp else 42.dp,
            label = "nextEpBottomPadding",
        )

        AnimatedVisibility(
            visible = showNextEpisodeCard && nextEpisode != null,
            enter = fadeIn(tween(300)) + slideInHorizontally(tween(350)) { it / 2 },
            exit = fadeOut(tween(250)) + slideOutHorizontally(tween(250)) { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = targetBottomPadding),
        ) {
            val ep = nextEpisode
            if (ep != null) {
                NextEpisodeFloatingCard(
                    episode = ep,
                    onClick = { playNextEpisode() },
                )
            }
        }

        if (showStatsDialog) {
            StreamStatsDialog(
                controller = controller,
                settings = settings,
                onDismiss = { showStatsDialog = false },
            )
        }

        AnimatedVisibility(
            visible = showEpisodesDrawer && seriesEpisodes.isNotEmpty(),
            enter = fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it },
            exit = fadeOut(tween(200)) + slideOutHorizontally(tween(250)) { it },
            modifier = Modifier.align(Alignment.CenterEnd),
        ) {
            SeriesEpisodesDrawer(
                episodes = seriesEpisodes,
                currentEpisodeId = currentEpisodeId,
                selectedSeason = selectedSeasonTab,
                onSelectSeason = { selectedSeasonTab = it },
                onSelectEpisode = { ep ->
                    playEpisode(ep)
                    showEpisodesDrawer = false
                    interaction++
                },
                onClose = {
                    showEpisodesDrawer = false
                    interaction++
                },
                firstFocus = episodesDrawerFocus,
            )
        }

        if (showExternalChooser) {
            ExternalPlayerChooserDialog(
                players = detectedPlayers,
                onSelectPlayer = { app ->
                    launchPlayer(app)
                    showExternalChooser = false
                },
                onOpenSystemChooser = {
                    launchExternalPlayer()
                    showExternalChooser = false
                },
                onDismiss = {
                    showExternalChooser = false
                },
            )
        }
    }
}

@Composable
private fun VodChip(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val container = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    val content = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White
    Row(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(7.dp))
            .background(container)
            .then(
                if (focused) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(7.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .focusable()
            .padding(horizontal = 7.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = content,
            maxLines = 1,
            fontWeight = FontWeight.Medium,
        )
    }
}

private enum class VodPanel { NONE, SUBTITLES, AUDIO, QUALITY, SPEED, ASPECT }

@OptIn(UnstableApi::class)
@Composable
private fun TrackPanel(
    panel: VodPanel,
    controller: PlayerController,
    tracks: Tracks,
    firstFocus: FocusRequester,
    playbackSpeed: Float = 1.0f,
    onSelectSpeed: (Float) -> Unit = {},
    aspectMode: AspectRatioMode = AspectRatioMode.FIT,
    onSelectAspect: (AspectRatioMode) -> Unit = {},
    onlineSubtitles: List<OnlineSubtitle> = emptyList(),
    isSearchingSubtitles: Boolean = false,
    subtitleSearchMessage: String? = null,
    selectedOnlineSubId: String? = null,
    currentOffsetMs: Long = 0L,
    onAdjustSyncOffset: (Long) -> Unit = {},
    onResetSyncOffset: () -> Unit = {},
    onApplyFpsCorrection: (Boolean) -> Unit = {},
    onSearchOnlineSubtitles: () -> Unit = {},
    onSelectOnlineSubtitle: (OnlineSubtitle) -> Unit = {},
    onClearOnlineSubtitle: () -> Unit = {},
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var subTab by remember { mutableStateOf(0) } // 0 = Tracks & Search, 1 = Smart Sync
    val options = mutableListOf<Triple<String, Boolean, () -> Unit>>()

    when (panel) {
        VodPanel.SPEED -> {
            val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
            speeds.forEach { spd ->
                val label = if (spd == 1.0f) "1.0x (${stringResource(R.string.player_quality_standard)})" else "${spd}x"
                val isSelected = (playbackSpeed == spd)
                options += Triple(label, isSelected) {
                    onSelectSpeed(spd)
                    onDone()
                }
            }
        }
        VodPanel.ASPECT -> {
            val aspectOptions = listOf(
                Pair(AspectRatioMode.FIT, stringResource(R.string.aspect_fit)),
                Pair(AspectRatioMode.RATIO_16_9, stringResource(R.string.aspect_16_9)),
                Pair(AspectRatioMode.RATIO_4_3, stringResource(R.string.aspect_4_3)),
                Pair(AspectRatioMode.ZOOM, stringResource(R.string.aspect_zoom)),
                Pair(AspectRatioMode.STRETCH, stringResource(R.string.aspect_stretch)),
            )
            aspectOptions.forEach { (mode, label) ->
                val isSelected = (aspectMode == mode)
                options += Triple(label, isSelected) {
                    onSelectAspect(mode)
                    onDone()
                }
            }
        }
        VodPanel.SUBTITLES -> {
            val textGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_TEXT }
            val anyEmbeddedSelected = textGroups.any { g -> (0 until g.length).any { g.isTrackSelected(it) } }
            val isOff = !anyEmbeddedSelected && selectedOnlineSubId == null
            options += Triple(stringResource(R.string.player_subtitles_off), isOff) {
                controller.disableText()
                onClearOnlineSubtitle()
                onDone()
            }
            textGroups.forEach { group ->
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    val isSelected = selectedOnlineSubId == null && group.isTrackSelected(i)
                    options += Triple(vodTextTrackLabel(context, format.label, format.language, options.size), isSelected) {
                        onClearOnlineSubtitle()
                        controller.selectTrack(group, i)
                        onDone()
                    }
                }
            }

            // Online subtitle search option
            val searchBtnLabel = if (isSearchingSubtitles) stringResource(R.string.vod_sub_searching) else stringResource(R.string.vod_sub_search_btn)
            options += Triple(searchBtnLabel, false) {
                onSearchOnlineSubtitles()
            }

            // Fetched online subtitles with source tag
            onlineSubtitles.forEach { sub ->
                val flag = when {
                    app.opentv.player.OnlineSubtitleService.isTurkish(sub) -> "🇹🇷 "
                    app.opentv.player.OnlineSubtitleService.isEnglish(sub) -> "🇬🇧 "
                    else -> "🌐 "
                }
                val label = "$flag[${sub.langLabel}] ${sub.name} • [${sub.source}]"
                val isSelected = selectedOnlineSubId == sub.id
                options += Triple(label, isSelected) {
                    onSelectOnlineSubtitle(sub)
                    onDone()
                }
            }
        }
        VodPanel.AUDIO -> {
            val audioGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
            audioGroups.forEach { group ->
                for (i in 0 until group.length) {
                    val format = group.getTrackFormat(i)
                    options += Triple(vodAudioTrackLabel(context, format, options.size), group.isTrackSelected(i)) {
                        controller.selectTrack(group, i); onDone()
                    }
                }
            }
        }
        VodPanel.QUALITY -> {
            val autoLabel = stringResource(R.string.player_quality_auto)
            val serverLabel = stringResource(R.string.player_quality_server)
            controller.getAvailableQualityOptions(
                tracks = tracks,
                autoLabel = autoLabel,
                serverStreamLabel = serverLabel,
                onSelected = onDone,
            ).forEach { opt ->
                options += Triple(opt.label, opt.selected, opt.onClick)
            }
        }
        VodPanel.NONE -> {}
    }

    val title = when (panel) {
        VodPanel.SUBTITLES -> stringResource(R.string.player_subtitles)
        VodPanel.AUDIO -> stringResource(R.string.player_audio)
        VodPanel.QUALITY -> stringResource(R.string.player_quality)
        VodPanel.SPEED -> stringResource(R.string.player_speed)
        VodPanel.ASPECT -> stringResource(R.string.player_aspect)
        VodPanel.NONE -> ""
    }

    Column(
        Modifier
            .widthIn(max = 480.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black.copy(alpha = 0.94f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (isSearchingSubtitles) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        // Subtitle Sub-Tabs (Tracks vs Smart Sync)
        if (panel == VodPanel.SUBTITLES) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubPanelTabButton(
                    label = stringResource(R.string.vod_sub_tab_selection),
                    selected = subTab == 0,
                    onClick = { subTab = 0 },
                    modifier = Modifier.weight(1f),
                )
                SubPanelTabButton(
                    label = stringResource(R.string.vod_sub_tab_sync),
                    selected = subTab == 1,
                    onClick = { subTab = 1 },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (subtitleSearchMessage != null && panel == VodPanel.SUBTITLES && subTab == 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitleSearchMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.height(8.dp))

        if (panel == VodPanel.SUBTITLES && subTab == 1) {
            // Smart Sync Controls HUD
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                // Current Offset Status Display
                val offsetSec = currentOffsetMs / 1000f
                val statusText = if (currentOffsetMs == 0L) {
                    stringResource(R.string.vod_sub_sync_exact)
                } else if (currentOffsetMs > 0L) {
                    stringResource(R.string.vod_sub_sync_delayed, offsetSec)
                } else {
                    stringResource(R.string.vod_sub_sync_advanced, offsetSec)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (currentOffsetMs == 0L) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        )
                        .border(
                            1.dp,
                            if (currentOffsetMs == 0L) Color.White.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(10.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.vod_sub_sync_diff),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentOffsetMs == 0L) Color.White else MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.vod_sub_quick_adjust),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SyncActionButton("-1.0s", modifier = Modifier.weight(1f)) { onAdjustSyncOffset(-1000L) }
                    SyncActionButton("-0.25s", modifier = Modifier.weight(1f)) { onAdjustSyncOffset(-250L) }
                    SyncActionButton(stringResource(R.string.vod_sub_reset_sync), modifier = Modifier.weight(1.3f)) { onResetSyncOffset() }
                    SyncActionButton("+0.25s", modifier = Modifier.weight(1f)) { onAdjustSyncOffset(250L) }
                    SyncActionButton("+1.0s", modifier = Modifier.weight(1f)) { onAdjustSyncOffset(1000L) }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.vod_sub_smart_presets),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SyncActionButton("⚡ 23.976 ➔ 25 FPS", modifier = Modifier.weight(1f)) { onApplyFpsCorrection(true) }
                    SyncActionButton("⚡ 25 ➔ 23.976 FPS", modifier = Modifier.weight(1f)) { onApplyFpsCorrection(false) }
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    SyncActionButton("⚡ +2.0s Jenerik", modifier = Modifier.weight(1f)) { onAdjustSyncOffset(2000L) }
                    SyncActionButton("⚡ -2.0s Jenerik", modifier = Modifier.weight(1f)) { onAdjustSyncOffset(-2000L) }
                }
            }
        } else {
            Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                if (options.isEmpty()) {
                    Text(
                        stringResource(R.string.player_none_available),
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(8.dp),
                    )
                }
                options.forEachIndexed { index, (label, selected, onClick) ->
                    TrackRow(label, selected, onClick, if (index == 0) firstFocus else null)
                }
            }
        }
    }
}

@Composable
private fun SubPanelTabButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primaryContainer
                else if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else Color.White.copy(alpha = 0.08f),
            )
            .border(
                1.dp,
                if (focused) MaterialTheme.colorScheme.primary
                else if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else Color.Transparent,
                RoundedCornerShape(8.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (focused) MaterialTheme.colorScheme.onPrimaryContainer
            else if (selected) MaterialTheme.colorScheme.primary
            else Color.White.copy(alpha = 0.85f),
        )
    }
}

@Composable
private fun SyncActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.10f),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White,
            maxLines = 1,
        )
    }
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit, focusRequester: FocusRequester?) {
    var focused by remember { mutableStateOf(false) }
    val bg = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f)
    val fg = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, color = fg, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.common_selected), tint = fg)
    }
}

private fun vodTextTrackLabel(context: android.content.Context, label: String?, language: String?, index: Int): String {
    if (!label.isNullOrBlank()) return label
    if (!language.isNullOrBlank() && language != "und") {
        return runCatching { java.util.Locale(language).displayLanguage.ifBlank { language } }.getOrDefault(language)
    }
    return context.getString(R.string.vod_track_num, index + 1)
}

private fun vodAudioTrackLabel(context: android.content.Context, format: androidx.media3.common.Format, index: Int): String {
    val lang = if (!format.language.isNullOrBlank() && format.language != "und") {
        runCatching { java.util.Locale(format.language!!).displayLanguage.ifBlank { format.language!! } }.getOrDefault(format.language!!)
    } else if (!format.label.isNullOrBlank()) {
        format.label!!
    } else {
        context.getString(R.string.vod_audio_num, index + 1)
    }

    val codec = when {
        format.sampleMimeType?.contains("ac3", ignoreCase = true) == true -> "Dolby Digital"
        format.sampleMimeType?.contains("eac3", ignoreCase = true) == true -> "Dolby Digital Plus"
        format.sampleMimeType?.contains("dts", ignoreCase = true) == true -> "DTS"
        format.sampleMimeType?.contains("mp4a", ignoreCase = true) == true -> "AAC"
        format.sampleMimeType?.contains("mpeg", ignoreCase = true) == true -> "MP3"
        else -> null
    }

    val channels = when (format.channelCount) {
        1 -> "Mono"
        2 -> "Stereo"
        6 -> "5.1"
        8 -> "7.1"
        else -> if (format.channelCount > 0) "${format.channelCount}ch" else null
    }

    val details = listOfNotNull(codec, channels).joinToString(" • ")
    return if (details.isNotEmpty()) "$lang ($details)" else lang
}

private fun formatDuration(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
private fun NextEpisodeFloatingCard(
    episode: Episode,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isFocused) 1.05f else 1f, label = "cardScale")

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFocused) 16.dp else 8.dp),
        modifier = Modifier
            .widthIn(min = 280.dp, max = 340.dp)
            .scale(scale)
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = if (isFocused) listOf(
                        GecePalette.CardElevated,
                        GecePalette.CardElevated.copy(alpha = 0.9f),
                    ) else listOf(
                        GecePalette.CardSurface,
                        GecePalette.CardSurface.copy(alpha = 0.8f),
                    ),
                ),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) Brush.horizontalGradient(
                    listOf(GecePalette.Accent, GecePalette.Accent.copy(alpha = 0.7f)),
                ) else Brush.horizontalGradient(
                    listOf(GecePalette.CardBorder, GecePalette.CardBorder.copy(alpha = 0.5f)),
                ),
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .focusable(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GecePalette.CanvasBg)
                    .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (!episode.stillUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.stillUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isFocused) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = if (isFocused) Color.Black else Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = stringResource(R.string.vod_next_episode_title),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 10.sp,
                        letterSpacing = 0.6.sp,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "S${episode.season}E${episode.episodeNumber} · ${episode.title}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.width(8.dp))

            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RoundMediaButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp = 44.dp,
    iconSize: Dp = 22.dp,
    isPrimary: Boolean = false,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (focused) 1.15f else 1f, label = "roundBtnScale")
    val bgColor = when {
        focused -> MaterialTheme.colorScheme.primary
        isPrimary -> Color.White.copy(alpha = 0.28f)
        else -> Color.White.copy(alpha = 0.14f)
    }
    val iconColor = when {
        focused -> MaterialTheme.colorScheme.onPrimary
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .scale(scale)
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.25f),
                shape = CircleShape,
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun SeriesEpisodesDrawer(
    episodes: List<Episode>,
    currentEpisodeId: Long?,
    selectedSeason: Int,
    onSelectSeason: (Int) -> Unit,
    onSelectEpisode: (Episode) -> Unit,
    onClose: () -> Unit,
    firstFocus: FocusRequester? = null,
) {
    val seasons = remember(episodes) { episodes.map { it.season }.distinct().sorted() }
    val activeSeason = if (selectedSeason in seasons) selectedSeason else (seasons.firstOrNull() ?: 1)
    val seasonEpisodes = remember(episodes, activeSeason) {
        episodes.filter { it.season == activeSeason }.sortedBy { it.episodeNumber }
    }
    val currentIdx = remember(seasonEpisodes, currentEpisodeId) {
        seasonEpisodes.indexOfFirst { it.id == currentEpisodeId }
    }
    val initialTargetIdx = if (currentIdx >= 0) currentIdx else 0
    val listState = rememberLazyListState()

    LaunchedEffect(activeSeason, currentEpisodeId) {
        if (initialTargetIdx > 0) {
            listState.scrollToItem((initialTargetIdx - 1).coerceAtLeast(0))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = 320.dp, max = 390.dp)
            .background(GecePalette.CanvasBg.copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = GecePalette.CardBorder,
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            )
            .padding(16.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header: Title & Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.VideoLibrary,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.player_episodes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "(${seasonEpisodes.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Season selector row (if more than 1 season)
            if (seasons.size > 1) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(seasons) { s ->
                        val isSelected = (s == activeSeason)
                        var focused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .onFocusChanged { focused = it.isFocused }
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        focused -> MaterialTheme.colorScheme.primary
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                        else -> Color.White.copy(alpha = 0.08f)
                                    },
                                )
                                .border(
                                    1.dp,
                                    if (focused || isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable { onSelectSeason(s) }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Sezon $s",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (focused) MaterialTheme.colorScheme.onPrimary else if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Episode list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(seasonEpisodes) { index, ep ->
                    val isCurrent = (ep.id == currentEpisodeId)
                    val shouldAttachFocus = (index == initialTargetIdx && firstFocus != null)
                    var focused by remember { mutableStateOf(false) }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (shouldAttachFocus) Modifier.focusRequester(firstFocus!!) else Modifier)
                            .onFocusChanged { focused = it.isFocused }
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    isCurrent -> Color(0xFF1E293B)
                                    else -> Color.White.copy(alpha = 0.05f)
                                },
                            )
                            .border(
                                width = if (focused) 2.dp else if (isCurrent) 1.5.dp else 1.dp,
                                color = when {
                                    focused -> MaterialTheme.colorScheme.primary
                                    isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                },
                                shape = RoundedCornerShape(12.dp),
                            )
                            .clickable { onSelectEpisode(ep) }
                            .focusable()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Thumbnail or placeholder
                        Box(
                            modifier = Modifier
                                .size(width = 68.dp, height = 44.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Black.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (!ep.stillUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = ep.stillUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isCurrent) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.6f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "B${ep.episodeNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (isCurrent) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "• Şu an Oynatılıyor",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GecePalette.Accent,
                                        fontSize = 10.sp,
                                    )
                                }
                            }
                            Text(
                                text = ep.title.ifBlank { "Bölüm ${ep.episodeNumber}" },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (!ep.plot.isNullOrBlank()) {
                                Text(
                                    text = ep.plot,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontSize = 10.sp,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExternalPlayerChooserDialog(
    players: List<ExternalAppInfo>,
    onSelectPlayer: (ExternalAppInfo) -> Unit,
    onOpenSystemChooser: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                border = BorderStroke(1.dp, GecePalette.CardBorder),
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 460.dp)
                    .clickable(enabled = false) {}
                    .padding(20.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.vod_external_chooser_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.common_close),
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Videoyu harici bir video oynatıcı uygulamasında izleyin:",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.65f),
                    )

                    Spacer(Modifier.height(14.dp))

                    if (players.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(players) { app ->
                                var focused by remember { mutableStateOf(false) }
                                val bmp = remember(app.icon) { drawableToBitmap(app.icon) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onFocusChanged { focused = it.isFocused }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (focused) MaterialTheme.colorScheme.primary
                                            else Color.White.copy(alpha = 0.08f),
                                        )
                                        .border(
                                            1.dp,
                                            if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                            RoundedCornerShape(12.dp),
                                        )
                                        .clickable { onSelectPlayer(app) }
                                        .focusable()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (bmp != null) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = app.name,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color.White.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(
                                                Icons.Filled.Tv,
                                                contentDescription = null,
                                                tint = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(22.dp),
                                            )
                                        }
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = app.packageName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (focused) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.5f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 10.sp,
                                        )
                                    }

                                    Icon(
                                        Icons.Filled.OpenInNew,
                                        contentDescription = null,
                                        tint = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.05f))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.vod_external_chooser_empty),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // System app chooser button
                    var sysFocused by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { sysFocused = it.isFocused }
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (sysFocused) MaterialTheme.colorScheme.primaryContainer
                                else Color.White.copy(alpha = 0.12f),
                            )
                            .border(
                                1.dp,
                                if (sysFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                RoundedCornerShape(10.dp),
                            )
                            .clickable(onClick = onOpenSystemChooser)
                            .focusable()
                            .padding(vertical = 10.dp, horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            Icons.Filled.Tv,
                            contentDescription = null,
                            tint = if (sysFocused) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.vod_external_system_chooser),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (sysFocused) MaterialTheme.colorScheme.onPrimaryContainer else Color.White,
                        )
                    }
                }
            }
        }
    }
}
