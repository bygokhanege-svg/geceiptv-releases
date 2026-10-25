/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.player

import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ViewQuilt
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import app.opentv.R
import app.opentv.ui.theme.GecePalette
import app.opentv.core.ServiceLocator
import app.opentv.core.findActivity
import app.opentv.data.model.Category
import app.opentv.data.model.Channel
import app.opentv.data.model.StreamKind
import app.opentv.data.model.shownName
import app.opentv.player.PlaybackQueue
import app.opentv.player.PlayerController
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Multi-View Layout Configurations:
 * - DUAL_SPLIT: 2 screens side-by-side (50% / 50%)
 * - TRIPLE_PIP: 1 main large screen on left + 2 stacked small screens on right
 * - QUAD_GRID: 4 screens in a 2x2 grid
 */
enum class MultiViewLayout(val slotCount: Int, val labelRes: Int, val icon: ImageVector) {
    DUAL_SPLIT(2, R.string.multiview_dual, Icons.Filled.ViewStream),
    TRIPLE_PIP(3, R.string.multiview_triple, Icons.AutoMirrored.Filled.ViewQuilt),
    QUAD_GRID(4, R.string.multiview_quad, Icons.Filled.GridView),
}

/**
 * High-performance, stable Multi-View (Çoklu Ekran) screen.
 *
 * Algorithm Highlights:
 * 1. Memory & Decoder Efficiency: Instances use `preview = true` (3-8s shallow buffer, no DVR back-buffer)
 *    and decoder fallback enabled so budget TV boxes can run 2-4 concurrent video decoders without OOM or stalls.
 * 2. Strict Exclusive Audio Dispatcher: Only the currently focused/active slot outputs audio (`volume = 1f`).
 *    All other slots are completely muted (`volume = 0f`), eliminating echo or audio mixing.
 * 3. Fast D-Pad Navigation & Slot Switching: Directional key navigation allows instant focus movement with
 *    luminous active borders and instant channel switching.
 * 4. In-flight Slot Management: When dropping from 4 to 2 slots, unused players are paused immediately
 *    to preserve network bandwidth and hardware decoding slots.
 */
@OptIn(UnstableApi::class)
@Composable
fun MultiViewScreen(
    initialChannelId: Long?,
    onBack: () -> Unit,
    onPlayFullscreen: (Channel) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val isTelevision = remember(context) { app.opentv.isRunningOnTelevision(context) }

    // Lock to landscape during Multi-View playback for optimal widescreen presentation
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
    val scope = rememberCoroutineScope()

    // 4 Slots for Channels
    val slots = remember { mutableStateListOf<Channel?>(null, null, null, null) }
    var layout by remember { mutableStateOf(MultiViewLayout.DUAL_SPLIT) }
    var focusedSlotIndex by remember { mutableIntStateOf(0) }
    var showTopBar by remember { mutableStateOf(true) }
    var channelPickerSlotIndex by remember { mutableStateOf<Int?>(null) }
    var slotActionMenuIndex by remember { mutableStateOf<Int?>(null) }

    // 4 Lightweight Player Controllers with disabled AudioFocus handling to prevent freezing
    val controllers = remember {
        List(4) { index ->
            PlayerController(
                context = context,
                scope = scope,
                httpClient = graph.streamingHttpClient,
                preview = true, // Shallow buffer, quick tune, no DVR memory hog
                subtitlesEnabled = false,
                dvr = false,
                handleAudioFocus = false, // Critical: Disables ExoPlayer AudioFocusManager so concurrent players never pause each other
            ).apply {
                player.volume = if (index == 0) 1f else 0f
            }
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    controllers.forEach { ctrl ->
                        runCatching {
                            ctrl.player.playWhenReady = false
                            ctrl.player.pause()
                        }
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

    DisposableEffect(Unit) {
        onDispose {
            controllers.forEach { it.release() }
        }
    }

    /**
     * Strict Exclusive Audio Dispatcher:
     * Only the slot with active D-pad focus outputs sound (volume = 1f).
     * All background slots are immediately and completely muted (volume = 0f).
     * Runs synchronously on focus change for zero-latency audio switching without crackle or frame drops.
     */
    val applyExclusiveAudio: (Int) -> Unit = { targetIndex ->
        controllers.forEachIndexed { index, ctrl ->
            val hasAudio = (index == targetIndex && index < layout.slotCount && slots[index] != null)
            val desiredVolume = if (hasAudio) 1f else 0f
            if (ctrl.player.volume != desiredVolume) {
                ctrl.player.volume = desiredVolume
            }
        }
    }

    val onSlotFocused: (Int) -> Unit = { slotIdx ->
        focusedSlotIndex = slotIdx
        applyExclusiveAudio(slotIdx)
    }

    val slotFocusRequesters = remember { List(4) { FocusRequester() } }
    val topBarFocusRequester = remember { FocusRequester() }

    val activeSourceId by graph.settings.activeSourceId.collectAsState()
    val effectiveSourceId = remember(activeSourceId) { if (activeSourceId > 0) activeSourceId else null }

    val liveCategoriesRaw by graph.catalogRepository.observeCategories(StreamKind.LIVE).collectAsState(initial = emptyList())
    val allChannels by graph.catalogRepository.observeChannels(sourceId = effectiveSourceId).collectAsState(initial = emptyList())

    val liveCategories = remember(liveCategoriesRaw, effectiveSourceId) {
        val filtered = if (effectiveSourceId != null) {
            liveCategoriesRaw.filter { it.sourceId == effectiveSourceId }
        } else {
            liveCategoriesRaw
        }
        filtered.distinctBy { it.id }
    }

    val onSelectChannelForSlot: (Int, Channel) -> Unit = { slotIdx, selected ->
        runCatching {
            slots[slotIdx] = selected
            onSlotFocused(slotIdx)
            tuneSlot(slotIdx, selected, controllers, graph, scope, isAudioActive = true)
            channelPickerSlotIndex = null
            scope.launch {
                delay(80)
                runCatching { slotFocusRequesters.getOrNull(slotIdx)?.requestFocus() }
            }
        }.onFailure { e ->
            android.util.Log.e("MultiViewScreen", "Error selecting channel for slot $slotIdx", e)
        }
    }

    val onCloseChannelPickerForSlot: (Int) -> Unit = { slotIdx ->
        channelPickerSlotIndex = null
        scope.launch {
            delay(60)
            runCatching { slotFocusRequesters.getOrNull(slotIdx)?.requestFocus() }
        }
    }

    val onCloseActionMenuForSlot: (Int) -> Unit = { slotIdx ->
        slotActionMenuIndex = null
        scope.launch {
            delay(60)
            runCatching { slotFocusRequesters[slotIdx].requestFocus() }
        }
    }

    val onPlayFullscreenForSlot: (Int) -> Unit = { slotIdx ->
        val ch = slots[slotIdx]
        slotActionMenuIndex = null
        if (ch != null) {
            controllers.forEach { it.stop() }
            PlaybackQueue.items = listOf(
                PlaybackQueue.Item(
                    id = ch.id,
                    name = ch.shownName,
                    logoUrl = ch.logoUrl,
                    number = ch.number,
                ),
            )
            onPlayFullscreen(ch)
        }
    }

    val onChangeChannelForSlot: (Int) -> Unit = { slotIdx ->
        slotActionMenuIndex = null
        channelPickerSlotIndex = slotIdx
    }

    val onRemoveChannelForSlot: (Int) -> Unit = { slotIdx ->
        slotActionMenuIndex = null
        slots[slotIdx] = null
        controllers[slotIdx].stop()
        val fallbackSlot = slots.indices.firstOrNull { it < layout.slotCount && slots[it] != null } ?: 0
        onSlotFocused(fallbackSlot)
        scope.launch {
            delay(60)
            runCatching { slotFocusRequesters[fallbackSlot].requestFocus() }
        }
    }

    // Keep screen awake during multi-view playback
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        view.keepScreenOn = true
        onDispose {
            if (!isTelevision) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                view.keepScreenOn = false
            }
        }
    }

    // Initialize initial channel on slot 0
    LaunchedEffect(initialChannelId) {
        if (initialChannelId != null && slots[0] == null) {
            val chan = graph.catalogRepository.channel(initialChannelId)
            if (chan != null) {
                slots[0] = chan
                tuneSlot(0, chan, controllers, graph, scope, isAudioActive = (focusedSlotIndex == 0))
            }
        }
        // Auto-suggest next channel in queue for slot 1 if empty
        if (slots[1] == null) {
            val queueItems = PlaybackQueue.items
            val nextItem = queueItems.firstOrNull { it.id != initialChannelId }
            if (nextItem != null) {
                val nextChan = graph.catalogRepository.channel(nextItem.id)
                if (nextChan != null) {
                    slots[1] = nextChan
                    tuneSlot(1, nextChan, controllers, graph, scope, isAudioActive = (focusedSlotIndex == 1))
                }
            }
        }
    }

    // Exclusive Audio Synchronizer: guarantees volume state whenever focus, layout, or slot channels change
    LaunchedEffect(focusedSlotIndex, layout, slots.map { it?.id }) {
        applyExclusiveAudio(focusedSlotIndex)
    }

    // When layout shrinks (e.g., from 4 to 2), stop non-visible slots to free codecs & network
    LaunchedEffect(layout) {
        controllers.forEachIndexed { index, ctrl ->
            if (index >= layout.slotCount) {
                ctrl.stop()
            } else {
                val ch = slots[index]
                if (ch != null && ctrl.state.value is PlayerController.State.Idle) {
                    tuneSlot(index, ch, controllers, graph, scope, isAudioActive = (index == focusedSlotIndex))
                }
            }
        }
        if (focusedSlotIndex >= layout.slotCount) {
            onSlotFocused(0)
        } else {
            applyExclusiveAudio(focusedSlotIndex)
        }
    }

    // Auto-hide Top Bar after inactivity
    LaunchedEffect(showTopBar) {
        if (showTopBar && channelPickerSlotIndex == null) {
            delay(5000)
            showTopBar = false
        }
    }

    // Remote Back Handler
    BackHandler {
        when {
            channelPickerSlotIndex != null -> {
                val closedSlot = channelPickerSlotIndex
                channelPickerSlotIndex = null
                if (closedSlot != null) {
                    scope.launch {
                        delay(60)
                        runCatching { slotFocusRequesters[closedSlot].requestFocus() }
                    }
                }
            }
            slotActionMenuIndex != null -> {
                val closedSlot = slotActionMenuIndex
                slotActionMenuIndex = null
                if (closedSlot != null) {
                    scope.launch {
                        delay(60)
                        runCatching { slotFocusRequesters[closedSlot].requestFocus() }
                    }
                }
            }
            showTopBar -> showTopBar = false
            else -> {
                controllers.forEach { it.stop() }
                onBack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.key == Key.Back || event.key == Key.Escape -> false
                    event.key == Key.Menu -> {
                        showTopBar = !showTopBar
                        if (showTopBar) scope.launch { delay(50); topBarFocusRequester.requestFocus() }
                        true
                    }
                    else -> false
                }
            },
    ) {
        // Multi-View Main Video Grid
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (showTopBar) 54.dp else 0.dp),
        ) {
            @Composable
            fun SlotTile(slotIdx: Int, tileModifier: Modifier) {
                MultiViewSlotTile(
                    slotIndex = slotIdx,
                    channel = slots[slotIdx],
                    controller = controllers[slotIdx],
                    isFocused = focusedSlotIndex == slotIdx,
                    isAudioActive = focusedSlotIndex == slotIdx,
                    isPickingChannel = channelPickerSlotIndex == slotIdx,
                    isActionMenuOpen = slotActionMenuIndex == slotIdx,
                    categories = liveCategories,
                    allChannels = allChannels,
                    modifier = tileModifier,
                    onFocused = { onSlotFocused(slotIdx) },
                    onSelect = {
                        onSlotFocused(slotIdx)
                        if (slots[slotIdx] == null) channelPickerSlotIndex = slotIdx
                        else slotActionMenuIndex = slotIdx
                    },
                    onAddChannel = {
                        onSlotFocused(slotIdx)
                        channelPickerSlotIndex = slotIdx
                    },
                    onCloseChannelPicker = { onCloseChannelPickerForSlot(slotIdx) },
                    onChannelSelected = { selected -> onSelectChannelForSlot(slotIdx, selected) },
                    onCloseActionMenu = { onCloseActionMenuForSlot(slotIdx) },
                    onPlayFullscreen = { onPlayFullscreenForSlot(slotIdx) },
                    onChangeChannel = { onChangeChannelForSlot(slotIdx) },
                    onRemoveChannel = { onRemoveChannelForSlot(slotIdx) },
                )
            }

            when (layout) {
                MultiViewLayout.DUAL_SPLIT -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        SlotTile(
                            slotIdx = 0,
                            tileModifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .focusRequester(slotFocusRequesters[0])
                                .focusProperties {
                                    right = slotFocusRequesters[1]
                                    up = topBarFocusRequester
                                },
                        )
                        SlotTile(
                            slotIdx = 1,
                            tileModifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .focusRequester(slotFocusRequesters[1])
                                .focusProperties {
                                    left = slotFocusRequesters[0]
                                    up = topBarFocusRequester
                                },
                        )
                    }
                }

                MultiViewLayout.TRIPLE_PIP -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Main Big Screen (Left)
                        SlotTile(
                            slotIdx = 0,
                            tileModifier = Modifier
                                .weight(0.64f)
                                .fillMaxHeight()
                                .focusRequester(slotFocusRequesters[0])
                                .focusProperties {
                                    right = slotFocusRequesters[1]
                                    up = topBarFocusRequester
                                },
                        )

                        // 2 Stacked Screens (Right)
                        Column(
                            modifier = Modifier
                                .weight(0.36f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            SlotTile(
                                slotIdx = 1,
                                tileModifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .focusRequester(slotFocusRequesters[1])
                                    .focusProperties {
                                        left = slotFocusRequesters[0]
                                        down = slotFocusRequesters[2]
                                        up = topBarFocusRequester
                                    },
                            )
                            SlotTile(
                                slotIdx = 2,
                                tileModifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .focusRequester(slotFocusRequesters[2])
                                    .focusProperties {
                                        left = slotFocusRequesters[0]
                                        up = slotFocusRequesters[1]
                                    },
                            )
                        }
                    }
                }

                MultiViewLayout.QUAD_GRID -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        // Top Row (Slots 0 & 1)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            SlotTile(
                                slotIdx = 0,
                                tileModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .focusRequester(slotFocusRequesters[0])
                                    .focusProperties {
                                        right = slotFocusRequesters[1]
                                        down = slotFocusRequesters[2]
                                        up = topBarFocusRequester
                                    },
                            )
                            SlotTile(
                                slotIdx = 1,
                                tileModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .focusRequester(slotFocusRequesters[1])
                                    .focusProperties {
                                        left = slotFocusRequesters[0]
                                        down = slotFocusRequesters[3]
                                        up = topBarFocusRequester
                                    },
                            )
                        }

                        // Bottom Row (Slots 2 & 3)
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            SlotTile(
                                slotIdx = 2,
                                tileModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .focusRequester(slotFocusRequesters[2])
                                    .focusProperties {
                                        right = slotFocusRequesters[3]
                                        up = slotFocusRequesters[0]
                                    },
                            )
                            SlotTile(
                                slotIdx = 3,
                                tileModifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .focusRequester(slotFocusRequesters[3])
                                    .focusProperties {
                                        left = slotFocusRequesters[2]
                                        up = slotFocusRequesters[1]
                                    },
                            )
                        }
                    }
                }
            }
        }

        // Top Header Control Bar (Layout Switcher & Controls)
        AnimatedVisibility(
            visible = showTopBar,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)),
                color = GecePalette.CanvasBg.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Back / Exit Button
                    TopBarIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = stringResource(R.string.common_back),
                        focusRequester = topBarFocusRequester,
                        onClick = {
                            controllers.forEach { it.stop() }
                            onBack()
                        },
                    )

                    Spacer(Modifier.width(14.dp))

                    Text(
                        text = stringResource(R.string.multiview_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )

                    Spacer(Modifier.width(8.dp))

                    // Audio active hint badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GecePalette.AccentSubtle)
                            .border(1.dp, GecePalette.Accent.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = GecePalette.Accent,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.multiview_slot_label, focusedSlotIndex + 1),
                                style = MaterialTheme.typography.labelSmall,
                                color = GecePalette.Accent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Layout Mode Switchers (2 screens, 3 screens, 4 screens)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MultiViewLayout.entries.forEach { mode ->
                            val isSelected = layout == mode
                            TopBarLayoutChip(
                                mode = mode,
                                isSelected = isSelected,
                                onClick = { layout = mode },
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tunes a specific multi-view slot to the selected channel with optimal low-buffer params.
 * Dispatches stream URL resolution to IO thread to prevent UI micro-stutters.
 */
@OptIn(UnstableApi::class)
private fun tuneSlot(
    slotIndex: Int,
    channel: Channel,
    controllers: List<PlayerController>,
    graph: ServiceLocator.Graph,
    scope: kotlinx.coroutines.CoroutineScope,
    isAudioActive: Boolean,
) {
    scope.launch {
        runCatching {
            val (source, url) = kotlinx.coroutines.withContext(Dispatchers.IO) {
                val src = graph.sourceRepository.byId(channel.sourceId)
                val playbackUrl = graph.catalogRepository.resolvePlaybackUrl(channel, src)
                src to playbackUrl
            }
            val ctrl = controllers.getOrNull(slotIndex) ?: return@launch
            ctrl.player.volume = if (isAudioActive) 1f else 0f
            ctrl.play(
                PlayerController.Request(
                    url = url,
                    title = channel.shownName,
                    userAgent = source?.userAgent ?: "GECEiptv/1.0 (Android)",
                    isLive = true,
                ),
                debounce = false,
            )
            ctrl.player.volume = if (isAudioActive) 1f else 0f
        }.onFailure { e ->
            android.util.Log.e("MultiViewScreen", "Failed to tune slot $slotIndex for ${channel.name}", e)
        }
    }
}

/**
 * Individual Multi-View Slot Tile:
 * - Renders PlayerView
 * - Handles D-Pad focus & active glowing cyan border
 * - Shows Audio status & Channel Badge
 * - Shows "+ Kanal Ekle" button when empty
 */
@OptIn(UnstableApi::class)
@Composable
private fun MultiViewSlotTile(
    slotIndex: Int,
    channel: Channel?,
    controller: PlayerController,
    isFocused: Boolean,
    isAudioActive: Boolean,
    isPickingChannel: Boolean,
    isActionMenuOpen: Boolean,
    categories: List<Category>,
    allChannels: List<Channel>,
    modifier: Modifier = Modifier,
    onFocused: () -> Unit,
    onSelect: () -> Unit,
    onAddChannel: () -> Unit,
    onCloseChannelPicker: () -> Unit,
    onChannelSelected: (Channel) -> Unit,
    onCloseActionMenu: () -> Unit,
    onPlayFullscreen: () -> Unit,
    onChangeChannel: () -> Unit,
    onRemoveChannel: () -> Unit,
) {
    if (isPickingChannel) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(GecePalette.CanvasBg)
                .border(
                    width = 2.dp,
                    brush = Brush.horizontalGradient(listOf(GecePalette.Accent, GecePalette.Accent)),
                    shape = RoundedCornerShape(12.dp),
                ),
        ) {
            InSlotChannelPicker(
                slotIndex = slotIndex,
                categories = categories,
                allChannels = allChannels,
                onClose = onCloseChannelPicker,
                onChannelSelected = onChannelSelected,
            )
        }
        return
    }

    val state by controller.state.collectAsState()

    val borderModifier = if (isFocused || isActionMenuOpen) {
        Modifier.border(
            width = 3.dp,
            brush = Brush.horizontalGradient(listOf(GecePalette.Accent, GecePalette.Accent)),
            shape = RoundedCornerShape(12.dp),
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.15f),
            shape = RoundedCornerShape(12.dp),
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GecePalette.CardSurface)
            .then(borderModifier)
            .onFocusChanged { if (it.isFocused) onFocused() }
            .focusable(!isActionMenuOpen)
            .clickable(enabled = !isActionMenuOpen) { onSelect() },
    ) {
        if (channel != null) {
            // Live Video Stream Layer
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = controller.player
                        useController = false
                        keepScreenOn = true
                        setKeepContentOnPlayerReset(true)
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        )
                    }
                },
                update = { playerView ->
                    if (playerView.player != controller.player) {
                        playerView.player = controller.player
                    }
                },
            )

            // Buffering Indicator
            if (state is PlayerController.State.Buffering) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        color = GecePalette.Accent,
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                    )
                }
            }

            // Error State
            if (state is PlayerController.State.Error) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (state as PlayerController.State.Error).message,
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(GecePalette.Accent)
                                .clickable { controller.retry() }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(stringResource(R.string.common_try_again), color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }

            // Top-Left Badge: Channel Logo, Number, Name & Audio State
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(
                        1.dp,
                        if (isAudioActive) GecePalette.Accent else Color.White.copy(alpha = 0.15f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Audio Wave / Muted Icon
                    Icon(
                        imageVector = if (isAudioActive) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeMute,
                        contentDescription = null,
                        tint = if (isAudioActive) GecePalette.Accent else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(6.dp))

                    // Channel Logo
                    if (!channel.logoUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = channel.logoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(3.dp)),
                        )
                        Spacer(Modifier.width(6.dp))
                    }

                    // Channel Name
                    Text(
                        text = channel.shownName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // In-Slot Action Menu Overlay
            if (isActionMenuOpen) {
                InSlotActionMenu(
                    channel = channel,
                    slotIndex = slotIndex,
                    onClose = onCloseActionMenu,
                    onPlayFullscreen = onPlayFullscreen,
                    onChangeChannel = onChangeChannel,
                    onRemoveChannel = onRemoveChannel,
                )
            }
        } else {
            // Empty Slot Tile: "+ Kanal Ekle" Prompt
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAddChannel() },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (isFocused) GecePalette.AccentSubtle
                            else GecePalette.CardBorder.copy(alpha = 0.6f),
                        )
                        .border(
                            1.5.dp,
                            if (isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.2f),
                            CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = null,
                        tint = if (isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.multiview_add_channel),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.7f),
                )
                Text(
                    text = stringResource(R.string.multiview_slot_label, slotIndex + 1),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
        }
    }
}

/**
 * Top Bar Layout Chip (2 Ekran, 3 Ekran, 4 Ekran)
 */
@Composable
private fun TopBarLayoutChip(
    mode: MultiViewLayout,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> GecePalette.AccentSubtle
                    isSelected -> GecePalette.AccentSubtle
                    else -> Color.Transparent
                },
            )
            .border(
                1.dp,
                when {
                    isFocused -> GecePalette.Accent
                    isSelected -> GecePalette.Accent
                    else -> Color.White.copy(alpha = 0.2f)
                },
                RoundedCornerShape(8.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                mode.icon,
                contentDescription = null,
                tint = if (isSelected || isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(mode.labelRes),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected || isFocused) Color.White else Color.White.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TopBarIconButton(
    icon: ImageVector,
    label: String,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) GecePalette.AccentSubtle else Color.Transparent)
            .border(
                1.dp,
                if (isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp),
            )
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/**
 * In-Slot Action Menu:
 * Rendered directly inside the active channel tile as a sleek dark overlay.
 * Replaces the external modal so TV remote D-Pad focus is immediate,
 * fully controllable, and visually bound to the active channel's square.
 */
@Composable
private fun InSlotActionMenu(
    channel: Channel,
    slotIndex: Int,
    onClose: () -> Unit,
    onPlayFullscreen: () -> Unit,
    onChangeChannel: () -> Unit,
    onRemoveChannel: () -> Unit,
) {
    val firstActionFocusRequester = remember { FocusRequester() }

    // Automatically focus the first action ("Tam Ekran") as soon as the menu opens
    LaunchedEffect(Unit) {
        delay(60)
        runCatching { firstActionFocusRequester.requestFocus() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GecePalette.CanvasBg.copy(alpha = 0.95f))
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && (event.key == Key.Back || event.key == Key.Escape)) {
                    onClose()
                    true
                } else {
                    false
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Channel Header (Badge & Title)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (!channel.logoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = channel.logoUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = channel.shownName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }

            // 1. Tam Ekran (Fullscreen)
            InSlotActionRow(
                icon = Icons.Filled.Fullscreen,
                label = stringResource(R.string.multiview_fullscreen),
                iconColor = GecePalette.Accent,
                focusRequester = firstActionFocusRequester,
                onClick = onPlayFullscreen,
            )

            // 2. Kanal Değiştir (Change Channel)
            InSlotActionRow(
                icon = Icons.Filled.SwapHoriz,
                label = stringResource(R.string.multiview_change_channel),
                iconColor = GecePalette.Accent,
                onClick = onChangeChannel,
            )

            // 3. Kaldır (Remove from Slot)
            InSlotActionRow(
                icon = Icons.Filled.Close,
                label = stringResource(R.string.multiview_remove),
                iconColor = Color(0xFFEF4444),
                onClick = onRemoveChannel,
            )

            // 4. Tamam (Done / Close)
            InSlotActionRow(
                icon = Icons.Filled.Check,
                label = stringResource(R.string.common_done),
                iconColor = Color(0xFF10B981),
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun InSlotActionRow(
    icon: ImageVector,
    label: String,
    iconColor: Color = GecePalette.Accent,
    focusRequester: FocusRequester? = null,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) GecePalette.AccentSubtle else GecePalette.CardSurface.copy(alpha = 0.6f),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(8.dp),
            )
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                    if (event.type == KeyEventType.KeyUp) {
                        onClick()
                    }
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isFocused) GecePalette.Accent else iconColor,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * In-Slot Channel Picker:
 * Rendered directly inside the clicked slot tile for TV remote stability.
 * Focus immediately lands on the first channel so D-Pad Up/Down works instantly.
 * Left/Right on category chips filters the channels on the fly.
 */
@Composable
private fun InSlotChannelPicker(
    slotIndex: Int,
    categories: List<Category>,
    allChannels: List<Channel>,
    onClose: () -> Unit,
    onChannelSelected: (Channel) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    val filteredChannels = remember(allChannels, selectedCategoryId) {
        val list = if (selectedCategoryId == null) {
            allChannels
        } else {
            allChannels.filter { it.categoryId == selectedCategoryId }
        }
        // Memory guard for low-spec TV boxes: limit initial display to 200 items when "All" is active
        if (selectedCategoryId == null && list.size > 200) {
            list.take(200)
        } else {
            list
        }
    }

    val firstChannelFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()

    // Request initial focus onto the first channel so remote D-Pad works immediately
    LaunchedEffect(filteredChannels.size) {
        if (filteredChannels.isNotEmpty()) {
            delay(60)
            runCatching { firstChannelFocusRequester.requestFocus() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GecePalette.CanvasBg)
            .padding(8.dp),
    ) {
        // In-Slot Header: Slot badge + Close button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Glowing Indicator & Slot Title
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(GecePalette.AccentSubtle)
                    .border(1.dp, GecePalette.Accent.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LiveTv,
                        contentDescription = null,
                        tint = GecePalette.Accent,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.multiview_slot_label, slotIndex + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = GecePalette.Accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.multiview_select_channel),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // Close (X) button with TV focus support
            var isCloseFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (isCloseFocused) Color(0xFFEF4444).copy(alpha = 0.35f) else GecePalette.CardElevated)
                    .border(
                        1.dp,
                        if (isCloseFocused) Color(0xFFEF4444) else Color.White.copy(alpha = 0.15f),
                        CircleShape,
                    )
                    .onFocusChanged { isCloseFocused = it.isFocused }
                    .onPreviewKeyEvent { event ->
                        if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                            if (event.type == KeyEventType.KeyUp) {
                                onClose()
                            }
                            true
                        } else false
                    }
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.common_back),
                    tint = if (isCloseFocused) Color(0xFFEF4444) else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        // Horizontal Category Filter Chips
        if (categories.isNotEmpty()) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item(key = "all_channels_chip") {
                    InSlotCategoryChip(
                        label = stringResource(R.string.guide_all_channels),
                        isSelected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                    )
                }
                itemsIndexed(
                    items = categories,
                    key = { index, cat -> "${cat.sourceId}_${cat.id}_$index" },
                ) { _, cat ->
                    InSlotCategoryChip(
                        label = cat.name,
                        isSelected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // Channel List
        if (filteredChannels.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Kanal bulunamadı",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.5f),
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                itemsIndexed(
                    items = filteredChannels,
                    key = { index, ch -> "${ch.sourceId}_${ch.id}_$index" },
                ) { index, channel ->
                    InSlotChannelRow(
                        channel = channel,
                        focusRequester = if (index == 0) firstChannelFocusRequester else null,
                        onClick = { onChannelSelected(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun InSlotCategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                when {
                    isFocused -> GecePalette.Accent.copy(alpha = 0.4f)
                    isSelected -> GecePalette.Accent
                    else -> GecePalette.CardSurface
                },
            )
            .border(
                1.dp,
                if (isFocused || isSelected) GecePalette.Accent else Color.White.copy(alpha = 0.1f),
                RoundedCornerShape(12.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                    if (event.type == KeyEventType.KeyUp) {
                        onClick()
                    }
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected || isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected || isFocused) Color.White else Color.White.copy(alpha = 0.7f),
            maxLines = 1,
        )
    }
}

@Composable
private fun InSlotChannelRow(
    channel: Channel,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isFocused -> Brush.horizontalGradient(
                        listOf(GecePalette.AccentSubtle, GecePalette.CardSurface),
                    )
                    else -> Brush.horizontalGradient(
                        listOf(GecePalette.CardSurface.copy(alpha = 0.7f), GecePalette.CardElevated.copy(alpha = 0.7f)),
                    )
                },
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(8.dp),
            )
            .let { if (focusRequester != null) it.focusRequester(focusRequester) else it }
            .onFocusChanged { isFocused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter) {
                    if (event.type == KeyEventType.KeyUp) {
                        onClick()
                    }
                    true
                } else false
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Channel Logo or Fallback Play Icon
        if (!channel.logoUrl.isNullOrEmpty()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GecePalette.CardElevated),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = if (isFocused) GecePalette.Accent else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
        }

        // Channel Name
        Text(
            text = channel.shownName,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Medium,
            color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        // Focused Arrow Indicator
        if (isFocused) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = GecePalette.Accent,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
