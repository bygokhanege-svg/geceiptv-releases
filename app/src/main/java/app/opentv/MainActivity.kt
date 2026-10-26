/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv

import android.app.PictureInPictureParams
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.opentv.core.AppSettings
import app.opentv.core.LicenseManager
import app.opentv.core.ServiceLocator
import app.opentv.data.model.shownName
import app.opentv.data.parser.displayTitle
import app.opentv.ui.MainScreen
import app.opentv.ui.ProfilesViewModel
import app.opentv.ui.SourcesViewModel
import app.opentv.ui.channels.ChannelManagerScreen
import app.opentv.ui.channels.SearchScreen
import app.opentv.ui.channels.SearchScope
import app.opentv.ui.VodViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import app.opentv.ui.onboarding.AddSourceScreen
import app.opentv.ui.player.MultiViewScreen
import app.opentv.ui.player.PlayerScreen
import app.opentv.ui.settings.AboutScreen
import app.opentv.ui.settings.AppSettingsScreen
import app.opentv.ui.settings.EpgSettingsScreen
import app.opentv.ui.settings.ParentalControlsScreen
import app.opentv.ui.settings.ProfilesScreen
import app.opentv.ui.settings.ProvidersScreen
import app.opentv.ui.settings.StremioAddonsScreen
import app.opentv.ui.settings.RecordingSettingsScreen
import app.opentv.ui.settings.LanguageSettingsScreen
import app.opentv.ui.settings.SleepTimerScreen
import app.opentv.ui.settings.SubtitleSettingsScreen
import app.opentv.ui.settings.SyncScreen
import app.opentv.ui.settings.AdminPanelScreen
import app.opentv.ui.settings.DeviceLicenseScreen
import app.opentv.ui.settings.SettingsHubScreen
import app.opentv.ui.settings.WebManagerScreen
import app.opentv.ui.theme.OpenTvTheme
import app.opentv.ui.LegalDisclaimerModal
import app.opentv.ui.SplashScreen
import app.opentv.ui.vod.MovieDetailScreen
import app.opentv.ui.vod.PersonScreen
import app.opentv.ui.vod.SeriesDetailScreen
import app.opentv.ui.vod.VodPlayerScreen
import app.opentv.update.UpdateGate

class MainActivity : ComponentActivity() {

    private var isTelevisionDevice = false

    // Apply the chosen UI language before any view or resource is resolved. A language change in
    // settings calls recreate(), which re-runs this with the new tag.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(app.opentv.core.LocaleUtils.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyImmersiveFullscreen()
        handlePlayIntent(intent)
        val isTelevision = isRunningOnTelevision(this)
        isTelevisionDevice = isTelevision
        requestedOrientation = if (isTelevision) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT
        }

        // On TV devices, keep the screen awake permanently while the app is in the foreground.
        // This prevents Android TV / Google TV / Fire TV screensavers and Daydream ambient mode
        // from triggering while using GECE İPTV Player.
        ServiceLocator.get(this).virtualMouseServer.attachActivity(this)
        if (isTelevision) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.decorView.keepScreenOn = true
            // Start local TV remote manager server so paired phones can connect automatically
            ServiceLocator.get(this).managerServer.start()
            ServiceLocator.get(this).virtualMouseServer.start()
        }

        setContent {
            val settings = remember { ServiceLocator.get(this).settings }
            val themeMode by settings.themeMode.collectAsState()
            val colorTheme by settings.colorTheme.collectAsState()
            val darkTheme = when (themeMode) {
                AppSettings.ThemeMode.DARK -> true
                AppSettings.ThemeMode.LIGHT -> false
                // A living-room screen defaults to dark; a phone/tablet follows the system.
                AppSettings.ThemeMode.SYSTEM -> isTelevision || isSystemInDarkTheme()
            }
            OpenTvTheme(darkTheme = darkTheme, colorTheme = colorTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GeceIptvApp(isTelevision = isTelevision)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyImmersiveFullscreen()
        if (isTelevisionDevice) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            window.decorView.keepScreenOn = true
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveFullscreen()
            if (isTelevisionDevice) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                window.decorView.keepScreenOn = true
            }
        }
    }

    fun applyImmersiveFullscreen() {
        runCatching {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                window.attributes.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
    }

    // singleTask: a reminder tapped while the app is already running arrives here, not a fresh
    // onCreate. Either way the requested channel is handed to the nav graph via [PlayRequests].
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePlayIntent(intent)
    }

    private fun handlePlayIntent(intent: Intent?) {
        val id = intent?.getLongExtra(EXTRA_PLAY_CHANNEL, 0L) ?: 0L
        if (id != 0L) app.opentv.core.PlayRequests.request(id)
        val recId = intent?.getLongExtra(EXTRA_WATCH_RECORDING, 0L) ?: 0L
        if (recId != 0L) {
            app.opentv.core.RecordingSignals.requestWatch(recId, System.currentTimeMillis())
        }
    }

    // ---- Picture-in-picture ------------------------------------------------------------------

    /** Home pressed while a programme is playing → shrink to a floating window instead of stopping. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        maybeEnterPip()
    }

    private fun supportsPip(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    fun maybeEnterPip() {
        if (isTelevisionDevice) return // On TV, switching away must not try background PiP audio
        if (!app.opentv.core.PipState.eligible || !app.opentv.core.PipState.isPlaying) return
        enterPipNow()
    }

    /** Explicit request from the player's PiP button — no eligibility guard, the user asked. */
    fun enterPipNow() {
        if (!supportsPip()) return
        runCatching {
            enterPictureInPictureMode(
                PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build(),
            )
        }
    }

    fun pipSupported(): Boolean = supportsPip()

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration,
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        app.opentv.core.PipState.setInPip(isInPictureInPictureMode)
    }

    override fun onStop() {
        super.onStop()
        val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
        if (!inPip) {
            // Cut off background audio and playback immediately when leaving the app
            app.opentv.player.PlayerController.pauseAll()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        app.opentv.player.PlayerController.stopAll()
        ServiceLocator.get(this).virtualMouseServer.detachActivity()
        if (isTelevisionDevice) {
            ServiceLocator.get(this).managerServer.stop()
            ServiceLocator.get(this).virtualMouseServer.stop()
        }
    }

    companion object {
        /** A reminder notification carries the channel to tune to in this extra. */
        const val EXTRA_PLAY_CHANNEL = "opentv.play_channel"

        /** A recording auto-switch notification carries the recording to watch in this extra. */
        const val EXTRA_WATCH_RECORDING = "opentv.watch_recording"
    }
}

object Routes {
    const val HOME = "home"
    const val ADD_SOURCE = "add-source"
    const val PLAYER = "player/{channelId}"
    const val SEARCH = "search?q={q}&scope={scope}"
    const val EPG_SETTINGS = "epg-settings"
    const val APP_SETTINGS = "app-settings"
    const val LANGUAGE = "language"
    const val SLEEP_TIMER = "sleep-timer"
    const val SUBTITLES = "subtitles"
    const val SETTINGS_HUB = "settings"
    const val PROVIDERS = "providers"
    const val ADDONS = "addons"
    const val CHANNELS = "channels"
    const val WEB_MANAGER = "web-manager"
    const val PROFILES = "profiles"
    const val PARENTAL = "parental"
    const val SYNC = "sync"
    const val REC_SETTINGS = "recording-settings"
    const val DEVICE_LICENSE = "device-license"
    const val ABOUT = "about"
    const val SERIES_DETAIL = "series/{seriesId}"
    const val MOVIE_DETAIL = "movie/{movieId}"
    const val EDIT_SOURCE = "edit-source/{sourceId}"

    // A person's name goes in a query arg, URL-encoded, so spaces and punctuation survive the round
    // trip — the same inline-encode/decode approach as the VOD player below.
    const val PERSON = "person?name={name}"

    // VOD plays carry the stream inline; a movie/episode is a one-off URL, not a stored id
    // the player can look up the way a channel is.
    const val VOD_PLAYER = "vod?key={key}&url={url}&title={title}&ua={ua}"
    const val MULTIVIEW = "multiview?channelId={channelId}"
    const val ADMIN_PANEL = "admin-panel"

    fun search(query: String = "", scope: String? = null): String {
        val params = mutableListOf<String>()
        if (query.isNotBlank()) {
            val cleanQuery = query.replace("+", " ").trim()
            params.add("q=${android.net.Uri.encode(cleanQuery)}")
        }
        if (!scope.isNullOrBlank()) {
            params.add("scope=${android.net.Uri.encode(scope)}")
        }
        return if (params.isNotEmpty()) "search?${params.joinToString("&")}" else "search"
    }
    fun player(channelId: Long) = "player/$channelId"
    fun multiView(channelId: Long? = null): String =
        if (channelId != null) "multiview?channelId=$channelId" else "multiview"
    fun seriesDetail(seriesId: Long) = "series/$seriesId"
    fun movieDetail(movieId: Long) = "movie/$movieId"
    fun editSource(sourceId: Long) = "edit-source/$sourceId"
    fun person(name: String) = "person?name=${java.net.URLEncoder.encode(name, "UTF-8")}"
    fun vodPlayer(key: String, url: String, title: String, ua: String): String {
        fun e(v: String) = java.net.URLEncoder.encode(v, "UTF-8")
        return "vod?key=${e(key)}&url=${e(url)}&title=${e(title)}&ua=${e(ua)}"
    }
}

@Composable
private fun GeceIptvApp(isTelevision: Boolean) {
    val context = LocalContext.current
    val licenseManager = remember { ServiceLocator.get(context).licenseManager }
    val navController = rememberNavController()
    DisposableEffect(navController) {
        val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
            val isSearch = destination.route?.startsWith("search") == true
            ServiceLocator.get(context).managerServer.isSearchActive = isSearch
            ServiceLocator.get(context).virtualMouseServer.isSearchActiveOnTv = isSearch
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
    val sourcesViewModel: SourcesViewModel = viewModel()
    val vodViewModel: VodViewModel = viewModel()
    val profilesViewModel: ProfilesViewModel = viewModel()
    val sourcesUi by sourcesViewModel.ui.collectAsState()
    val profiles by profilesViewModel.profiles.collectAsState()
    val activeProfileId by profilesViewModel.activeProfileId.collectAsState()
    val activeProfile = profiles.firstOrNull { it.id == activeProfileId }
    val activeProfileName = activeProfile?.name ?: "Varsayılan"

    // Background cloud license & remote playlists check with Supabase on app startup
    LaunchedEffect(Unit) {
        val graph = ServiceLocator.get(context)
        graph.licenseManager.syncWithCloud(graph.licenseCloudClient)
        if (isTelevision) {
            runCatching { graph.managerServer.start() }
        }
    }

    var splashActive by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        if (sourcesUi.loaded) {

    // First run goes straight to setup — an empty channel list with no explanation is the
    // worst possible first impression.
    val start = if (sourcesUi.sources.isEmpty()) Routes.ADD_SOURCE else Routes.HOME

    // Boot to last channel: if enabled and we have one, jump straight into the player on launch.
    // Runs once; backing out returns to the guide and doesn't re-trigger.
    val bootContext = androidx.compose.ui.platform.LocalContext.current
    val bootSettings = remember { ServiceLocator.get(bootContext).settings }
    LaunchedEffect(start) {
        if (start == Routes.HOME && bootSettings.resumeLastChannel.value && bootSettings.lastChannelId != 0L) {
            navController.navigate(Routes.player(bootSettings.lastChannelId))
        }
    }

    // A tapped reminder notification asks for a specific channel. Consume it so it fires once and
    // never re-triggers on a later launch.
    val playRequest by app.opentv.core.PlayRequests.channelId.collectAsState()
    LaunchedEffect(playRequest) {
        val id = playRequest
        if (id != null && id != 0L) {
            app.opentv.core.PlayRequests.consume()
            navController.navigate(Routes.player(id))
        }
    }

    // Auto-switch: a scheduled recording started, so move the live view onto the recording file
    // (single-connection safe). Ignore a stale request a long-backgrounded app only now sees.
    val watchRequest by app.opentv.core.RecordingSignals.watch.collectAsState()
    LaunchedEffect(watchRequest) {
        val req = watchRequest ?: return@LaunchedEffect
        app.opentv.core.RecordingSignals.consumeWatch()
        val fresh = System.currentTimeMillis() - req.requestedAtMillis < 10 * 60_000L
        if (req.recordingId != 0L && fresh) {
            val rec = runCatching {
                ServiceLocator.get(bootContext).recordingRepository.byId(req.recordingId)
            }.getOrNull() ?: return@LaunchedEffect
            runCatching {
                navController.navigate(
                    Routes.vodPlayer(
                        key = "rec:${rec.id}",
                        url = "optvrec://${rec.id}",
                        title = rec.title,
                        ua = rec.userAgent,
                    ),
                ) {
                    launchSingleTop = true
                    // Drop the live channel we switched away from off the back stack, so Back from
                    // the recording returns to the home screen — never to a live stream. On a single-
                    // connection line, returning to live TV while still recording would open a second
                    // stream and risk the provider banning the account.
                    popUpTo(Routes.PLAYER) { inclusive = true }
                }
            }
        }
    }

    // Handle remote control commands from phone/web companion
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? android.media.AudioManager }
    val componentActivity = context as? ComponentActivity
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(Unit) {
        app.opentv.core.RemoteControlSignals.commands.collect { cmd ->
            when (cmd) {
                is app.opentv.core.RemoteControlSignals.RemoteCommand.PlayChannel -> {
                    navController.navigate(Routes.player(cmd.channelId)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.CastMedia -> {
                    navController.navigate(Routes.vodPlayer("cast", cmd.url, cmd.title, "OpenTV")) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.Home -> {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.Back -> {
                    componentActivity?.onBackPressedDispatcher?.onBackPressed()
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.SearchText -> {
                    val cleanQuery = cmd.query.replace("+", " ")
                    navController.navigate(Routes.search(cleanQuery)) {
                        launchSingleTop = true
                    }
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.VolumeUp -> {
                    audioManager?.adjustStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        android.media.AudioManager.ADJUST_RAISE,
                        android.media.AudioManager.FLAG_SHOW_UI,
                    )
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.VolumeDown -> {
                    audioManager?.adjustStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        android.media.AudioManager.ADJUST_LOWER,
                        android.media.AudioManager.FLAG_SHOW_UI,
                    )
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.Mute -> {
                    audioManager?.adjustStreamVolume(
                        android.media.AudioManager.STREAM_MUSIC,
                        android.media.AudioManager.ADJUST_TOGGLE_MUTE,
                        android.media.AudioManager.FLAG_SHOW_UI,
                    )
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.MouseClick -> {
                    ServiceLocator.get(context).virtualMouseServer.triggerClick()
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.MouseBack -> {
                    ServiceLocator.get(context).virtualMouseServer.triggerBack()
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.MouseActivate -> {
                    ServiceLocator.get(context).virtualMouseServer.activatePointer()
                }
                is app.opentv.core.RemoteControlSignals.RemoteCommand.MouseHide -> {
                    ServiceLocator.get(context).virtualMouseServer.hidePointer()
                }
                else -> {
                    val keyCode = when (cmd) {
                        app.opentv.core.RemoteControlSignals.RemoteCommand.Up -> android.view.KeyEvent.KEYCODE_DPAD_UP
                        app.opentv.core.RemoteControlSignals.RemoteCommand.Down -> android.view.KeyEvent.KEYCODE_DPAD_DOWN
                        app.opentv.core.RemoteControlSignals.RemoteCommand.Left -> android.view.KeyEvent.KEYCODE_DPAD_LEFT
                        app.opentv.core.RemoteControlSignals.RemoteCommand.Right -> android.view.KeyEvent.KEYCODE_DPAD_RIGHT
                        app.opentv.core.RemoteControlSignals.RemoteCommand.Select -> android.view.KeyEvent.KEYCODE_DPAD_CENTER
                        app.opentv.core.RemoteControlSignals.RemoteCommand.PlayPause -> android.view.KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        app.opentv.core.RemoteControlSignals.RemoteCommand.ChannelUp -> android.view.KeyEvent.KEYCODE_CHANNEL_UP
                        app.opentv.core.RemoteControlSignals.RemoteCommand.ChannelDown -> android.view.KeyEvent.KEYCODE_CHANNEL_DOWN
                        is app.opentv.core.RemoteControlSignals.RemoteCommand.Number -> android.view.KeyEvent.KEYCODE_0 + cmd.digit.coerceIn(0, 9)
                        else -> null
                    }
                    if (keyCode != null && componentActivity != null) {
                        val now = android.os.SystemClock.uptimeMillis()
                        val down = android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN, keyCode, 0)
                        val up = android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_UP, keyCode, 0)

                        // 1. Dispatch through Activity
                        val dHandled = componentActivity.dispatchKeyEvent(down)
                        val uHandled = componentActivity.dispatchKeyEvent(up)
                        val handled = dHandled || uHandled

                        // 2. Direct fallback navigation via Compose focus manager if unhandled by window
                        if (!handled) {
                            when (cmd) {
                                app.opentv.core.RemoteControlSignals.RemoteCommand.Up ->
                                    focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Up)
                                app.opentv.core.RemoteControlSignals.RemoteCommand.Down ->
                                    focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                                app.opentv.core.RemoteControlSignals.RemoteCommand.Left ->
                                    focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Left)
                                app.opentv.core.RemoteControlSignals.RemoteCommand.Right ->
                                    focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right)
                                app.opentv.core.RemoteControlSignals.RemoteCommand.Select -> {
                                    val enterDown = android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER, 0)
                                    val enterUp = android.view.KeyEvent(now, now, android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER, 0)
                                    val target = componentActivity.currentFocus ?: componentActivity.window.decorView
                                    target.dispatchKeyEvent(enterDown)
                                    target.dispatchKeyEvent(enterUp)
                                }
                                else -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = start) {
            composable(Routes.ADD_SOURCE) {
                AddSourceScreen(
                    viewModel = sourcesViewModel,
                    onFinished = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ADD_SOURCE) { inclusive = true }
                        }
                    },
                )
            }

            composable(Routes.EDIT_SOURCE) { entry ->
                val sourceId = entry.arguments?.getString("sourceId")?.toLongOrNull() ?: return@composable
                AddSourceScreen(
                    viewModel = sourcesViewModel,
                    editingSourceId = sourceId,
                    onFinished = { navController.popBackStack() },
                )
            }

            composable(Routes.HOME) {
                MainScreen(
                    isTelevision = isTelevision,
                    hasSources = sourcesUi.sources.isNotEmpty(),
                    isSyncing = sourcesUi.syncing,
                    onPlayChannel = { channel ->
                        if (licenseManager.licenseStatus.value is LicenseManager.LicenseStatus.Expired) {
                            navController.navigate(Routes.DEVICE_LICENSE)
                        } else {
                            navController.navigate(Routes.player(channel.id))
                        }
                    },
                    onOpenMovie = { movie ->
                        if (licenseManager.licenseStatus.value is LicenseManager.LicenseStatus.Expired) {
                            navController.navigate(Routes.DEVICE_LICENSE)
                        } else {
                            navController.navigate(Routes.movieDetail(movie.id))
                        }
                    },
                    onOpenSeries = { series ->
                        if (licenseManager.licenseStatus.value is LicenseManager.LicenseStatus.Expired) {
                            navController.navigate(Routes.DEVICE_LICENSE)
                        } else {
                            navController.navigate(Routes.seriesDetail(series.id))
                        }
                    },
                    onResume = { key, url, title ->
                        if (licenseManager.licenseStatus.value is LicenseManager.LicenseStatus.Expired) {
                            navController.navigate(Routes.DEVICE_LICENSE)
                        } else {
                            navController.navigate(
                                Routes.vodPlayer(key, url, title, "OpenTV/0.1 (Android)"),
                            )
                        }
                    },
                    onAddSource = { navController.navigate(Routes.ADD_SOURCE) },
                    onRefresh = sourcesViewModel::refreshAll,
                    onOpenSearch = { scope -> navController.navigate(Routes.search(scope = scope)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS_HUB) },
                    onOpenLicense = { navController.navigate(Routes.DEVICE_LICENSE) },
                    onOpenProfiles = { navController.navigate(Routes.PROFILES) },
                    onPlayRecording = { rec ->
                        // A NAS recording plays straight off its smb:// locator and a USB one off
                        // its content:// document URI (Media3 reads both); an internal one through
                        // a file:// uri. An internal recording that's still capturing plays through
                        // the tail-following optvrec:// source, so it can be watched as it records
                        // with no second connection to the provider. All go through the VOD player.
                        val stillRecording = rec.status == app.opentv.data.model.RecordingStatus.RECORDING
                        // Internal and NAS in-progress recordings are both tail-followable via the
                        // growing source; USB (content://) isn't.
                        val growable = !app.opentv.recording.RecordingStorage.isContent(rec.filePath) &&
                            !app.opentv.recording.RecordingStorage.isUsbPlaceholder(rec.filePath)
                        val url = when {
                            stillRecording && growable -> "optvrec://${rec.id}"
                            app.opentv.recording.SmbClient.isSmb(rec.filePath) -> rec.filePath
                            app.opentv.recording.RecordingStorage.isContent(rec.filePath) -> rec.filePath
                            else -> android.net.Uri.fromFile(java.io.File(rec.filePath)).toString()
                        }
                        navController.navigate(
                            Routes.vodPlayer(
                                key = "rec:${rec.id}",
                                url = url,
                                title = rec.title,
                                ua = rec.userAgent,
                            ),
                        )
                    },
                    onPlayCatchup = { key, url, title, ua ->
                        // Catch-up is a seekable archive stream — plays through the VOD player.
                        navController.navigate(Routes.vodPlayer(key, url, title, ua))
                    },
                    activeProfileName = activeProfileName,
                    activeProfile = activeProfile,
                    onOpenMultiView = { navController.navigate(Routes.multiView()) },
                    onOpenWebManager = { navController.navigate(Routes.WEB_MANAGER) },
                )
            }

            composable(Routes.PROFILES) {
                ProfilesScreen(
                    isTelevision = isTelevision,
                    viewModel = profilesViewModel,
                    onBack = { navController.popBackStack() },
                )
            }

            composable(
                route = Routes.SEARCH,
                arguments = listOf(
                    navArgument("q") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("scope") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val rawQ = entry.arguments?.getString("q")
                val decodedQ = rawQ?.let {
                    runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it)
                }?.replace("+", " ")
                val initialQuery = if (decodedQ == null || decodedQ == "{q}" || decodedQ == "(q)" || decodedQ.contains("{q}")) "" else decodedQ
                val rawScope = entry.arguments?.getString("scope")
                val initialScope = when (rawScope?.uppercase(java.util.Locale.ROOT)) {
                    "MOVIES" -> SearchScope.MOVIES
                    "SERIES" -> SearchScope.SERIES
                    "CHANNELS" -> SearchScope.CHANNELS
                    else -> SearchScope.ALL
                }
                SearchScreen(
                    initialQuery = initialQuery,
                    initialScope = initialScope,
                    onPlayChannel = { channel ->
                        if (licenseManager.licenseStatus.value is LicenseManager.LicenseStatus.Expired) {
                            navController.navigate(Routes.DEVICE_LICENSE)
                        } else {
                            navController.navigate(Routes.player(channel.id))
                        }
                    },
                    onPlayMovie = { movie ->
                        if (licenseManager.licenseStatus.value is LicenseManager.LicenseStatus.Expired) {
                            navController.navigate(Routes.DEVICE_LICENSE)
                        } else {
                            navController.navigate(Routes.movieDetail(movie.id))
                        }
                    },
                    onOpenSeries = { series ->
                        if (licenseManager.licenseStatus.value is LicenseManager.LicenseStatus.Expired) {
                            navController.navigate(Routes.DEVICE_LICENSE)
                        } else {
                            navController.navigate(Routes.seriesDetail(series.id))
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SETTINGS_HUB) {
                SettingsHubScreen(
                    onOpenProviders = { navController.navigate(Routes.PROVIDERS) },
                    onOpenAddons = { navController.navigate(Routes.ADDONS) },
                    onOpenGuide = { navController.navigate(Routes.EPG_SETTINGS) },
                    onOpenChannels = { navController.navigate(Routes.CHANNELS) },
                    onOpenWebManager = { navController.navigate(Routes.WEB_MANAGER) },
                    onOpenDisplay = { navController.navigate(Routes.APP_SETTINGS) },
                    onOpenSubtitles = { navController.navigate(Routes.SUBTITLES) },
                    onOpenLanguage = { navController.navigate(Routes.LANGUAGE) },
                    onOpenSleepTimer = { navController.navigate(Routes.SLEEP_TIMER) },
                    onOpenParental = { navController.navigate(Routes.PARENTAL) },
                    onOpenSync = { navController.navigate(Routes.SYNC) },
                    onOpenRecordings = { navController.navigate(Routes.REC_SETTINGS) },
                    onOpenProfiles = { navController.navigate(Routes.PROFILES) },
                    onOpenSearch = { navController.navigate(Routes.search()) },
                    onOpenLicense = { navController.navigate(Routes.DEVICE_LICENSE) },
                    onOpenAbout = { navController.navigate(Routes.ABOUT) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.DEVICE_LICENSE) {
                DeviceLicenseScreen(
                    onBack = { navController.popBackStack() },
                    onOpenAdminPanel = { navController.navigate(Routes.ADMIN_PANEL) },
                )
            }

            composable(Routes.ADMIN_PANEL) {
                AdminPanelScreen(
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.SUBTITLES) {
                SubtitleSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SYNC) {
                SyncScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.REC_SETTINGS) {
                RecordingSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.CHANNELS) {
                ChannelManagerScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.WEB_MANAGER) {
                WebManagerScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PARENTAL) {
                ParentalControlsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PROVIDERS) {
                ProvidersScreen(
                    viewModel = sourcesViewModel,
                    onAddSource = { navController.navigate(Routes.ADD_SOURCE) },
                    onEditSource = { src -> navController.navigate(Routes.editSource(src.id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.ADDONS) {
                StremioAddonsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.ABOUT) {
                AboutScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.EPG_SETTINGS) {
                EpgSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.APP_SETTINGS) {
                AppSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.LANGUAGE) {
                LanguageSettingsScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.SLEEP_TIMER) {
                SleepTimerScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.PLAYER) { entry ->
                val channelId = entry.arguments?.getString("channelId")?.toLongOrNull()
                PlayerScreen(
                    channelId = channelId,
                    onBack = { navController.popBackStack() },
                    onOpenMultiView = { id -> navController.navigate(Routes.multiView(id)) },
                )
            }

            composable(
                route = Routes.MULTIVIEW,
                arguments = listOf(
                    navArgument("channelId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val channelId = entry.arguments?.getString("channelId")?.toLongOrNull()
                MultiViewScreen(
                    initialChannelId = channelId,
                    onBack = { navController.popBackStack() },
                    onPlayFullscreen = { channel ->
                        app.opentv.player.PlaybackQueue.items = listOf(
                            app.opentv.player.PlaybackQueue.Item(
                                id = channel.id,
                                name = channel.shownName,
                                logoUrl = channel.logoUrl,
                                number = channel.number,
                            ),
                        )
                        navController.navigate(Routes.player(channel.id))
                    },
                )
            }

            composable(Routes.SERIES_DETAIL) { entry ->
                val seriesId = entry.arguments?.getString("seriesId")?.toLongOrNull() ?: return@composable
                SeriesDetailScreen(
                    seriesId = seriesId,
                    viewModel = vodViewModel,
                    onPlayEpisode = { key, url, title ->
                        navController.navigate(
                            Routes.vodPlayer(key, url, title, "OpenTV/0.1 (Android)"),
                        )
                    },
                    onOpenSeries = { series -> navController.navigate(Routes.seriesDetail(series.id)) },
                    onOpenPerson = { name -> navController.navigate(Routes.person(name)) },
                )
            }

            composable(Routes.MOVIE_DETAIL) { entry ->
                val movieId = entry.arguments?.getString("movieId")?.toLongOrNull() ?: return@composable
                MovieDetailScreen(
                    movieId = movieId,
                    viewModel = vodViewModel,
                    onPlay = { movie ->
                        navController.navigate(
                            Routes.vodPlayer(
                                key = "movie:${movie.id}",
                                url = movie.streamUrl,
                                title = movie.displayTitle,
                                ua = "OpenTV/0.1 (Android)",
                            ),
                        )
                    },
                    onPlayUrl = { key, url, title ->
                        navController.navigate(
                            Routes.vodPlayer(key = key, url = url, title = title, ua = "OpenTV/0.1 (Android)"),
                        )
                    },
                    onOpenMovie = { movie -> navController.navigate(Routes.movieDetail(movie.id)) },
                    onOpenPerson = { name -> navController.navigate(Routes.person(name)) },
                )
            }

            composable(Routes.PERSON) { entry ->
                val name = entry.arguments?.getString("name")
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }.orEmpty()
                PersonScreen(
                    name = name,
                    viewModel = vodViewModel,
                    onOpenMovie = { movie -> navController.navigate(Routes.movieDetail(movie.id)) },
                    onOpenSeries = { series -> navController.navigate(Routes.seriesDetail(series.id)) },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(Routes.VOD_PLAYER) { entry ->
                fun arg(name: String) = entry.arguments?.getString(name)
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }.orEmpty()
                VodPlayerScreen(
                    mediaKey = arg("key"),
                    streamUrl = arg("url"),
                    title = arg("title"),
                    userAgent = arg("ua").ifEmpty { "OpenTV/0.1 (Android)" },
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Enforce mandatory Legal Disclaimer & User Agreement on home screen if unaccepted
        val appSettings = remember { ServiceLocator.get(context).settings }
        val disclaimerAccepted by appSettings.disclaimerAccepted.collectAsState()
        if (!disclaimerAccepted && start == Routes.HOME) {
            LegalDisclaimerModal(
                onAccept = {
                    appSettings.setDisclaimerAccepted(true)
                },
                onDismiss = {
                    // Cannot be dismissed without accepting
                },
            )
        }

        // Virtual Mouse pointer overlay (hardware-accelerated GPU render)
        app.opentv.ui.remote.VirtualMouseOverlay(server = ServiceLocator.get(context).virtualMouseServer)

        // Sits above the whole nav graph so a found update can prompt from any screen.
        UpdateGate()

        // "About to switch to a recording" banner — shows 30s before an auto-switch fires.
        RecordingSwitchBanner()
        }
    }

        // Luxury Responsive Splash Screen for TV, Tablet & Mobile
        AnimatedVisibility(
            visible = splashActive || !sourcesUi.loaded,
            enter = fadeIn(),
            exit = fadeOut(
                animationSpec = tween(
                    durationMillis = 600,
                    easing = FastOutSlowInEasing,
                ),
            ),
        ) {
            SplashScreen(
                isTelevision = isTelevision,
                isLoaded = sourcesUi.loaded,
                onFinish = {
                    splashActive = false
                },
            )
        }
    }
}

/**
 * A slim banner shown 30 seconds before a scheduled recording takes over the screen (single-
 * connection auto-switch). It gives the viewer a heads-up and a way out: "Keep watching" suppresses
 * the switch for that booking. It clears itself once the switch would have happened.
 */
@Composable
private fun RecordingSwitchBanner() {
    val imminent by app.opentv.core.RecordingSignals.imminent.collectAsState()
    val current = imminent ?: return

    // Auto-clear a little after the switch instant, in case the switch itself never lands.
    LaunchedEffect(current.recordingId) {
        val wait = (current.startAtMillis + 5_000L) - System.currentTimeMillis()
        kotlinx.coroutines.delay(wait.coerceIn(5_000L, 60_000L))
        if (app.opentv.core.RecordingSignals.imminent.value?.recordingId == current.recordingId) {
            app.opentv.core.RecordingSignals.clearImminent()
        }
    }

    Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.TopCenter) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.FiberManualRecord,
                contentDescription = null,
                tint = Color(0xFFE53935),
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                stringResource(R.string.rec_switch_banner, current.title, current.channelName),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.width(14.dp))
            var focused by remember { mutableStateOf(false) }
            Text(
                stringResource(R.string.rec_switch_keep_watching),
                style = MaterialTheme.typography.labelLarge,
                color = if (focused) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (focused) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .onFocusChanged { focused = it.isFocused }
                    .focusable()
                    .clickable { app.opentv.core.RecordingSignals.suppress(current.recordingId) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Detects whether the device is a TV (Smart TV, Android TV, Fire TV, generic TV Box).
 *
 * Checked at runtime rather than shipping separate APKs:
 * - Certified Android TV / Google TV via UiModeManager or Leanback / Television feature.
 * - Amazon Fire TV via amazon.hardware.fire_tv.
 * - Generic Chinese TV Boxes (running tablet ROMs) via lack of touchscreen + cellular SIM.
 */
fun isRunningOnTelevision(context: Context): Boolean {
    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
    if (uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) return true

    val packageManager = context.packageManager
    if (packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
        packageManager.hasSystemFeature("android.hardware.type.television") ||
        packageManager.hasSystemFeature("amazon.hardware.fire_tv") ||
        packageManager.hasSystemFeature("android.software.live_tv")
    ) {
        return true
    }

    // Generic Chinese TV Box detection:
    // Many budget TV boxes (X96, Tanix, Rockchip, Amlogic) run tablet firmware.
    // However, they lack a touchscreen and cellular telephony (SIM).
    val hasTouchscreen = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
    val hasTelephony = packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)
    if (!hasTouchscreen && !hasTelephony) {
        return true
    }

    // Model, hardware, or device string markers common across TV and TV box vendors
    val specs = (Build.MODEL + " " + Build.DEVICE + " " + Build.PRODUCT + " " + Build.BRAND + " " + Build.MANUFACTURER).lowercase()
    if (specs.contains("android tv") || specs.contains("google tv") || specs.contains("fire tv") ||
        specs.contains("firetv") || specs.contains("aft") || specs.contains("bravia") ||
        specs.contains("mibox") || specs.contains("mitv") || specs.contains("shield") ||
        specs.contains("chromecast")
    ) {
        return true
    }

    return false
}
