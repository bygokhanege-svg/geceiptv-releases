/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import app.opentv.R
import app.opentv.core.findActivity
import app.opentv.core.StatusBus
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Dvr
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FiberManualRecord
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.opentv.core.AppSettings
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.opentv.data.model.Channel
import app.opentv.data.model.Movie
import app.opentv.data.model.Recording
import app.opentv.data.model.Series
import app.opentv.ui.channels.HomeScreen
import app.opentv.ui.recordings.RecordingsScreen
import app.opentv.ui.remote.RemoteScreen
import app.opentv.ui.vod.MoviesScreen
import app.opentv.ui.vod.SeriesScreen
import app.opentv.ui.DashboardScreen
import app.opentv.ui.theme.GecePalette

/**
 * Modern TV Navigation Shell:
 * - Constant-width content container: The content viewport has a fixed left margin (70dp) and never
 *   undergoes layout reflows or resizing when the side menu opens or closes.
 * - Floating Glassmorphic Drawer: When focused, the side menu floats smoothly OVER the screen as an
 *   elevated acrylic glass panel with an obsidian gradient and soft drop shadow.
 * - One-Click Remote Back Key Quick Access: Pressing the remote's BACK key from anywhere in the content
 *   immediately pops open and focuses the side menu.
 * - Direct Sub-menu Entry on Click: Clicking any tab immediately selects it and passes focus directly
 *   into the sub-menu / content without having to manually press directional keys.
 */
enum class Tab(val labelRes: Int, val icon: ImageVector) {
    HOME(R.string.nav_home, Icons.Outlined.Home),
    LIVE(R.string.nav_live_tv, Icons.Outlined.LiveTv),
    MOVIES(R.string.nav_movies, Icons.Outlined.Movie),
    SHOWS(R.string.nav_shows, Icons.Outlined.VideoLibrary),
    RECORDINGS(R.string.nav_recordings, Icons.AutoMirrored.Outlined.Dvr),
    REMOTE(R.string.nav_remote, Icons.Filled.Tv),
}

private val RAIL_COLLAPSED = 70.dp
private val RAIL_EXPANDED = 236.dp

@Composable
fun MainScreen(
    isTelevision: Boolean,
    hasSources: Boolean,
    isSyncing: Boolean,
    onPlayChannel: (Channel) -> Unit,
    onOpenMovie: (Movie) -> Unit,
    onOpenSeries: (Series) -> Unit,
    onResume: (mediaKey: String, url: String, title: String) -> Unit,
    onAddSource: () -> Unit,
    onRefresh: () -> Unit,
    onOpenSearch: (scope: String?) -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenLicense: () -> Unit = onOpenSettings,
    onOpenProfiles: () -> Unit,
    onPlayRecording: (Recording) -> Unit,
    onPlayCatchup: (mediaKey: String, url: String, title: String, ua: String) -> Unit,
    activeProfileName: String,
    onOpenMultiView: () -> Unit = {},
    onOpenWebManager: () -> Unit = {},
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()
    val contentFocusRequester = remember { FocusRequester() }

    val settings = remember { AppSettings.get(context) }
    val liveEnabled by settings.liveEnabled.collectAsState()
    val moviesEnabled by settings.moviesEnabled.collectAsState()
    val seriesEnabled by settings.seriesEnabled.collectAsState()
    val visibleTabs = remember(isTelevision, liveEnabled, moviesEnabled, seriesEnabled) {
        buildList {
            add(Tab.HOME)
            if (liveEnabled) add(Tab.LIVE)
            if (moviesEnabled) add(Tab.MOVIES)
            if (seriesEnabled) add(Tab.SHOWS)
            if (isTelevision) {
                add(Tab.RECORDINGS)
            } else {
                add(Tab.REMOTE)
            }
        }
    }
    val homeTab = visibleTabs.first()
    var tab by remember { mutableStateOf(homeTab) }

    LaunchedEffect(visibleTabs) {
        if (tab !in visibleTabs) tab = homeTab
    }

    LaunchedEffect(Unit) {
        app.opentv.core.RemoteControlSignals.commands.collect { cmd ->
            when (cmd) {
                is app.opentv.core.RemoteControlSignals.RemoteCommand.NavLive -> tab = Tab.LIVE
                is app.opentv.core.RemoteControlSignals.RemoteCommand.NavMovies -> tab = Tab.MOVIES
                is app.opentv.core.RemoteControlSignals.RemoteCommand.NavShows -> tab = Tab.SHOWS
                is app.opentv.core.RemoteControlSignals.RemoteCommand.Home -> tab = homeTab
                else -> Unit
            }
        }
    }

    val tabFocusRequesters = remember {
        Tab.entries.associateWith { FocusRequester() }
    }

    var showExit by remember { mutableStateOf(false) }

    // When on any non-home tab, Back key switches back to HOME
    BackHandler(enabled = tab != homeTab) {
        tab = homeTab
    }

    // When on HOME tab, Back key prompts exit dialog
    BackHandler(enabled = tab == homeTab) {
        showExit = true
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            title = { Text(stringResource(R.string.exit_title)) },
            text = { Text(stringResource(R.string.exit_body)) },
            confirmButton = {
                TextButton(onClick = { showExit = false; context.findActivity()?.finish() }) {
                    Text(stringResource(R.string.exit_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExit = false }) { Text(stringResource(R.string.exit_cancel)) }
            },
        )
    }

    Column(Modifier.fillMaxSize()) {
        if (!isTelevision && tab != Tab.HOME) {
            // Modern Phone Top App Bar (Only shown on other tabs; on HOME it is positioned below the subscription card)
            PhoneTopBar(
                title = "GECE İPTV Player",
                activeProfileName = activeProfileName,
                onOpenSearch = {
                    val scope = when (tab) {
                        Tab.MOVIES -> "MOVIES"
                        Tab.SHOWS -> "SERIES"
                        Tab.LIVE -> "CHANNELS"
                        else -> null
                    }
                    onOpenSearch(scope)
                },
                onOpenMultiView = onOpenMultiView,
                onOpenSettings = onOpenSettings,
                onOpenProfiles = onOpenProfiles,
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            // CONTENT AREA:
            // Fixed full width (0.dp) on both TV and phones for a stable, clean layout.
            Box(
                Modifier
                    .fillMaxSize()
                    .focusRequester(contentFocusRequester)
                    .focusGroup()
            ) {
                when (tab) {
                    Tab.HOME -> DashboardScreen(
                        isTelevision = isTelevision,
                        onOpenMovie = onOpenMovie,
                        onOpenSeries = onOpenSeries,
                        onResume = onResume,
                        onOpenLicense = onOpenLicense,
                        onOpenSearch = { onOpenSearch(null) },
                        onOpenSettings = onOpenSettings,
                        onOpenMultiView = onOpenMultiView,
                        onAddSource = onAddSource,
                        activeProfileName = activeProfileName,
                        onOpenProfiles = onOpenProfiles,
                        onNavigateTab = { tab = it },
                        onOpenWebManager = onOpenWebManager,
                    )
                    Tab.LIVE -> HomeScreen(
                        isTelevision = isTelevision,
                        hasSources = hasSources,
                        isSyncing = isSyncing,
                        onPlayChannel = onPlayChannel,
                        onAddSource = onAddSource,
                        onRefresh = onRefresh,
                        onPlayCatchup = onPlayCatchup,
                    )
                    Tab.MOVIES -> MoviesScreen(
                        onOpenMovie = onOpenMovie,
                        onResume = onResume,
                        onOpenSearch = { onOpenSearch("MOVIES") },
                        hasSources = hasSources,
                        isSyncing = isSyncing,
                    )
                    Tab.SHOWS -> SeriesScreen(
                        onOpenSeries = onOpenSeries,
                        onResume = onResume,
                        onOpenSearch = { onOpenSearch("SERIES") },
                        hasSources = hasSources,
                        isSyncing = isSyncing,
                    )
                    Tab.RECORDINGS -> RecordingsScreen(onPlay = onPlayRecording)
                    Tab.REMOTE -> RemoteScreen()
                }
            }
        }

        StatusBar()

        if (!isTelevision) {
            // Modern Phone Bottom Navigation Bar
            PhoneBottomBar(
                tabs = visibleTabs,
                current = tab,
                onSelect = { tab = it },
            )
        }
    }
}

/**
 * Top App Bar optimized for mobile smartphones.
 */
@Composable
private fun PhoneTopBar(
    title: String,
    activeProfileName: String,
    onOpenSearch: () -> Unit,
    onOpenMultiView: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenProfiles: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(54.dp)
            .background(GecePalette.CanvasBg)
            .border(width = 0.5.dp, color = GecePalette.CardBorder, shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // App Logo & Brand Title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = onOpenProfiles),
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(GecePalette.CardSurface)
                    .border(1.dp, GecePalette.Accent.copy(alpha = 0.45f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_opentv_logo),
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        color = GecePalette.TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(GecePalette.Accent),
                    )
                }
                if (activeProfileName.isNotBlank() && activeProfileName != "Varsayılan") {
                    Text(
                        text = activeProfileName,
                        color = GecePalette.Accent,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        // Action Icons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(
                onClick = onOpenSearch,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = stringResource(R.string.nav_search),
                    tint = GecePalette.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onOpenMultiView,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = stringResource(R.string.multiview_title),
                    tint = GecePalette.Accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = stringResource(R.string.nav_settings),
                    tint = GecePalette.TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/**
 * Bottom Navigation Bar optimized for mobile smartphones.
 */
@Composable
private fun PhoneBottomBar(
    tabs: List<Tab>,
    current: Tab,
    onSelect: (Tab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(60.dp)
            .background(Color(0xF90A0F1D))
            .border(width = 0.5.dp, color = Color(0x22FFFFFF), shape = RoundedCornerShape(0.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        tabs.forEach { tab ->
            val isSelected = tab == current
            val activeColor = MaterialTheme.colorScheme.primary
            val inactiveColor = Color.White.copy(alpha = 0.55f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelect(tab) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) activeColor.copy(alpha = 0.18f) else Color.Transparent)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = stringResource(tab.labelRes),
                        tint = if (isSelected) activeColor else inactiveColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(tab.labelRes),
                    color = if (isSelected) activeColor else inactiveColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

/**
 * A slim line along the bottom that says what the app is doing in the background — loading
 * channels, building the guide, loading movies — so a slow moment on a big provider reads as work
 * in progress, not a frozen screen. Invisible when there's nothing to report.
 */
@Composable
private fun StatusBar() {
    val message by StatusBus.message.collectAsState()
    val progress by StatusBus.progress.collectAsState()
    val text = message ?: return
    val p = progress
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (p == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    "${(p * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (p != null) {
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { p },
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun NavRail(
    tabs: List<Tab>,
    current: Tab,
    expanded: Boolean,
    onExpandedChanged: (Boolean) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSelect: (Tab) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLicense: () -> Unit,
    onOpenProfiles: () -> Unit,
    onOpenMultiView: () -> Unit,
    onOpenWebManager: () -> Unit,
    activeProfileName: String,
    isTelevision: Boolean,
    tabFocusRequesters: Map<Tab, FocusRequester>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val themeMode by settings.themeMode.collectAsState()
    val isDark = when (themeMode) {
        AppSettings.ThemeMode.DARK -> true
        AppSettings.ThemeMode.LIGHT -> false
        AppSettings.ThemeMode.SYSTEM -> isTelevision || isSystemInDarkTheme()
    }

    val width by animateDpAsState(
        targetValue = if (expanded) RAIL_EXPANDED else RAIL_COLLAPSED,
        animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing),
        label = "railWidth",
    )

    Column(
        modifier = modifier
            .width(width)
            .fillMaxHeight()
            .shadow(
                elevation = if (expanded) 24.dp else 0.dp,
                shape = if (expanded) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(0.dp),
            )
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        GecePalette.CanvasBg,
                        GecePalette.CardSurface,
                    )
                ),
                shape = if (expanded) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(0.dp),
            )
            .border(
                width = 1.dp,
                color = if (expanded) GecePalette.Accent.copy(alpha = 0.35f) else GecePalette.CardBorder,
                shape = if (expanded) RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp) else RoundedCornerShape(0.dp),
            )
            .focusGroup()
            .onFocusChanged {
                val has = it.hasFocus
                onFocusChanged(has)
                onExpandedChanged(has)
            }
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Brand Header (Centered in 54dp box)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(width = 54.dp, height = 36.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(GecePalette.CardSurface)
                        .border(1.dp, GecePalette.Accent.copy(alpha = 0.55f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_opentv_logo),
                        contentDescription = "GECE İPTV Player",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(100)),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            "GECE İPTV Player",
                            style = MaterialTheme.typography.titleMedium,
                            color = GecePalette.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "Canlı & Sinema",
                            style = MaterialTheme.typography.labelSmall,
                            color = GecePalette.Accent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(2.dp))

        tabs.forEach { t ->
            RailItem(
                icon = t.icon,
                label = stringResource(t.labelRes),
                expanded = expanded,
                selected = current == t,
                focusRequester = tabFocusRequesters[t],
                onClick = { onSelect(t) },
            )
        }

        Spacer(Modifier.height(6.dp))

        RailItem(
            icon = Icons.Outlined.Search,
            label = stringResource(R.string.nav_search),
            expanded = expanded,
            selected = false,
            onClick = onOpenSearch,
        )
        RailItem(
            icon = Icons.Outlined.AccountCircle,
            label = activeProfileName,
            expanded = expanded,
            selected = false,
            onClick = onOpenProfiles,
        )
        RailItem(
            icon = Icons.Filled.WorkspacePremium,
            label = "Cihaz & VIP",
            expanded = expanded,
            selected = false,
            onClick = onOpenLicense,
        )
        RailItem(
            icon = Icons.Filled.GridView,
            label = stringResource(R.string.multiview_title),
            expanded = expanded,
            selected = false,
            onClick = onOpenMultiView,
        )
        RailItem(
            icon = Icons.Filled.QrCode2,
            label = "Telefon Kumandası",
            expanded = expanded,
            selected = false,
            onClick = onOpenWebManager,
        )
        RailItem(
            icon = Icons.Outlined.Settings,
            label = stringResource(R.string.nav_settings),
            expanded = expanded,
            selected = false,
            onClick = onOpenSettings,
        )
        ThemeToggleRailItem(
            isDark = isDark,
            expanded = expanded,
            onToggle = {
                val nextMode = if (isDark) AppSettings.ThemeMode.LIGHT else AppSettings.ThemeMode.DARK
                settings.setThemeMode(nextMode)
            },
        )
    }
}

@Composable
private fun RailItem(
    icon: ImageVector,
    label: String,
    expanded: Boolean,
    selected: Boolean,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val bg = when {
        focused -> Brush.horizontalGradient(
            listOf(
                GecePalette.Accent,
                GecePalette.Accent.copy(alpha = 0.90f),
            )
        )
        selected -> Brush.horizontalGradient(
            listOf(
                GecePalette.AccentSubtle,
                GecePalette.AccentSubtle.copy(alpha = 0.50f),
            )
        )
        else -> Brush.horizontalGradient(
            listOf(Color.Transparent, Color.Transparent)
        )
    }

    val tint = when {
        focused -> GecePalette.OnAccent
        selected -> GecePalette.Accent
        else -> GecePalette.TextSecondary
    }

    val borderColor = when {
        focused -> GecePalette.Accent
        selected -> GecePalette.Accent.copy(alpha = 0.45f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(
                width = if (focused) 1.5.dp else if (selected) 1.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .padding(vertical = 5.5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // High-end glowing left accent pill on active/focused item
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(
                    if (focused) GecePalette.OnAccent
                    else if (selected) GecePalette.Accent
                    else Color.Transparent
                )
        )

        Spacer(Modifier.width(3.dp))

        Box(
            modifier = Modifier.size(width = 48.dp, height = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(21.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(100)),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    color = tint,
                    fontWeight = if (focused) FontWeight.ExtraBold else if (selected) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ThemeToggleRailItem(
    isDark: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    val activeAccent = GecePalette.Accent

    val containerBg = when {
        focused -> GecePalette.Accent
        isDark -> GecePalette.CardSurface
        else -> Color(0xFFFEF3C7)
    }
    val containerBorderColor = when {
        focused -> GecePalette.Accent
        isDark -> GecePalette.CardBorder
        else -> GecePalette.Accent.copy(alpha = 0.65f)
    }

    val icon = if (isDark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode
    val label = if (isDark) stringResource(R.string.nav_theme_dark) else stringResource(R.string.nav_theme_light)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
            .border(
                width = if (focused) 1.5.dp else 1.dp,
                color = containerBorderColor,
                shape = RoundedCornerShape(10.dp),
            )
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (!expanded) {
            // Collapsed: A prominent, centered mini-switch capsule (44dp x 24dp)
            Box(
                modifier = Modifier.size(width = 50.dp, height = 26.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 44.dp, height = 24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (focused) GecePalette.OnAccent.copy(alpha = 0.2f)
                            else if (isDark) GecePalette.CardElevated
                            else Color(0xFFFDE68A),
                        )
                        .border(
                            1.dp,
                            if (focused) GecePalette.OnAccent.copy(alpha = 0.4f)
                            else GecePalette.CardBorder,
                            RoundedCornerShape(12.dp),
                        ),
                    contentAlignment = if (isDark) Alignment.CenterEnd else Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(19.dp)
                            .clip(RoundedCornerShape(9.5.dp))
                            .background(
                                if (focused) GecePalette.OnAccent
                                else activeAccent,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = if (focused) GecePalette.Accent else if (isDark) GecePalette.OnAccent else Color.White,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
            }
        } else {
            // Expanded: Left icon badge + Center titles + Right prominent switch
            Box(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (focused) GecePalette.OnAccent.copy(alpha = 0.2f)
                        else GecePalette.AccentSubtle
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (focused) GecePalette.OnAccent else activeAccent,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "TEMA",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (focused) GecePalette.OnAccent else activeAccent,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (focused) GecePalette.OnAccent else GecePalette.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // High-contrast Physical Switch Pill
            Box(
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(width = 42.dp, height = 22.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(
                        if (focused) GecePalette.OnAccent.copy(alpha = 0.25f)
                        else GecePalette.Accent,
                    )
                    .border(
                        1.dp,
                        if (focused) GecePalette.OnAccent.copy(alpha = 0.5f) else GecePalette.CardBorder,
                        RoundedCornerShape(11.dp),
                    ),
                contentAlignment = if (isDark) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(17.dp)
                        .clip(RoundedCornerShape(8.5.dp))
                        .background(if (focused) GecePalette.OnAccent else Color.White),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (focused) GecePalette.Accent else activeAccent),
                    )
                }
            }
        }
    }
}
