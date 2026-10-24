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
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import app.opentv.data.model.Episode
import app.opentv.player.OnlineSubtitle
import app.opentv.ui.player.PlayerClockBadge
import app.opentv.ui.player.StreamStatsDialog
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

                val series = graph.catalogRepository.seriesBySeriesId(currentEp.sourceId, currentEp.seriesId)
                parentSeriesTitle = series?.name
                movieImdbId = series?.let { s -> graph.catalogRepository.imdbIdForSeries(s) }

                val allEps = graph.catalogRepository.episodesForSeries(currentEp.sourceId, currentEp.seriesId)
                val sorted = allEps.sortedWith(compareBy({ it.season }, { it.episodeNumber }))
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
            } else {
                movieName = null
                parentSeriesTitle = null
                nextEpisode = null
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

    fun playNextEpisode() {
        val next = nextEpisode ?: return
        scope.launch {
            savePosition()
            currentMediaKey = "ep:${next.id}"
            currentStreamUrl = next.streamUrl
            currentTitle = "S${next.season}E${next.episodeNumber} · ${next.title}"
            paused = false
            onlineSubtitles = emptyList()
            selectedOnlineSubId = null
            subtitleSearchMessage = null
            reveal()
        }
    }

    fun launchExternalPlayer() {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(currentStreamUrl), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("title", currentTitle)
                putExtra("secure_uri", true)
            }
            val chooser = Intent.createChooser(intent, "Harici Oynatıcı Seçin (VLC / MX)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
    LaunchedEffect(controlsVisible, interaction, state, paused, scrubbing, vodPanel) {
        val activelyPlaying = state is PlayerController.State.Playing ||
            (growingRec && state is PlayerController.State.Buffering)
        if (controlsVisible && !paused && !scrubbing && vodPanel == VodPanel.NONE && activelyPlaying) {
            delay(5_000)
            controlsVisible = false
        }
    }

    LaunchedEffect(controlsVisible, vodPanel) {
        if (controlsVisible) {
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = controller.player
                    useController = false
                    keepScreenOn = true
                    setKeepContentOnPlayerReset(true)
                    app.opentv.ui.settings.SubtitleStyleHelper.applySubtitlePreferences(subtitleView, graph.settings)
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                }
            },
        )

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
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VodChip(Icons.Filled.FastRewind, stringResource(R.string.player_rewind)) {
                        seekRelative(-15_000)
                        interaction++
                    }
                    VodChip(
                        icon = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        label = if (paused) stringResource(R.string.common_play) else stringResource(R.string.player_pause),
                        focusRequester = barFocus,
                    ) {
                        paused = !paused
                        controller.player.playWhenReady = !paused
                        interaction++
                    }
                    VodChip(Icons.Filled.FastForward, stringResource(R.string.player_forward)) {
                        seekRelative(15_000)
                        interaction++
                    }
                    VodChip(Icons.Filled.ClosedCaption, stringResource(R.string.player_subtitles)) {
                        vodPanel = if (vodPanel == VodPanel.SUBTITLES) VodPanel.NONE else VodPanel.SUBTITLES
                        interaction++
                    }
                    VodChip(Icons.Filled.Audiotrack, stringResource(R.string.player_audio)) {
                        vodPanel = if (vodPanel == VodPanel.AUDIO) VodPanel.NONE else VodPanel.AUDIO
                        interaction++
                    }
                    VodChip(Icons.Filled.HighQuality, stringResource(R.string.player_quality)) {
                        vodPanel = if (vodPanel == VodPanel.QUALITY) VodPanel.NONE else VodPanel.QUALITY
                        interaction++
                    }
                    VodChip(Icons.Filled.Analytics, stringResource(R.string.player_stats)) {
                        showStatsDialog = true
                        interaction++
                    }
                    VodChip(Icons.Filled.Tv, stringResource(R.string.vod_external_player)) {
                        launchExternalPlayer()
                        interaction++
                    }
                    if (nextEpisodePromptEnabled && nextEpisode != null) {
                        VodChip(
                            icon = Icons.Filled.SkipNext,
                            label = stringResource(R.string.vod_next_episode_tag, nextEpisode!!.season, nextEpisode!!.episodeNumber),
                        ) {
                            playNextEpisode()
                            interaction++
                        }
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
    }
}

@Composable
private fun VodChip(
    icon: ImageVector,
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val container = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f)
    val content = if (focused) MaterialTheme.colorScheme.onPrimary else Color.White
    Row(
        modifier = Modifier
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(container)
            .then(
                if (focused) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = label, tint = content, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
    }
}

private enum class VodPanel { NONE, SUBTITLES, AUDIO, QUALITY }

@OptIn(UnstableApi::class)
@Composable
private fun TrackPanel(
    panel: VodPanel,
    controller: PlayerController,
    tracks: Tracks,
    firstFocus: FocusRequester,
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
                val flag = if (sub.lang.lowercase() in listOf("tur", "tr")) "🇹🇷 " else "🌐 "
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
                        Color(0xF51A2238),
                        Color(0xF8243050),
                    ) else listOf(
                        Color(0xE6101420),
                        Color(0xEE161C2C),
                    ),
                ),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                brush = if (isFocused) Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.primary, Color(0xFF7C93FF)),
                ) else Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f)),
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
                    .background(Color(0xFF090D16))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
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
