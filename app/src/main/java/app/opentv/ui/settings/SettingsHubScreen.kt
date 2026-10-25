/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Subtitles
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewTimeline
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.opentv.R
import app.opentv.isRunningOnTelevision

/**
 * Modern ultra-premium glassmorphism settings hub.
 *
 * Configured as a 3-column layout on phones and tablets with interactive 3D perspective
 * tilt physics, specular highlight refraction borders, embossed 3D glass badges,
 * and dynamic ambient light mesh glow.
 *
 * On Android TV, maintains a 5-column square grid optimized for D-Pad navigation.
 */
@Composable
fun SettingsHubScreen(
    onOpenProviders: () -> Unit,
    onOpenAddons: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenChannels: () -> Unit,
    onOpenWebManager: () -> Unit,
    onOpenDisplay: () -> Unit,
    onOpenSubtitles: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenParental: () -> Unit,
    onOpenSync: () -> Unit,
    onOpenRecordings: () -> Unit,
    onOpenProfiles: () -> Unit = {},
    onOpenSearch: () -> Unit = {},
    onOpenLicense: () -> Unit = {},
    onOpenAbout: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val isTv = remember { isRunningOnTelevision(context) }
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 600

    var showingOtherSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showingOtherSettings) {
        showingOtherSettings = false
    }

    // Grid columns: 2 columns for portrait phones (prevents text clipping), 4 for TV in main menu (4x2 = 8), 5 for TV in other menu (5+4=9), 3 or 4 for tablets
    val columns = when {
        isTv -> if (showingOtherSettings) 5 else 4
        isWideScreen -> if (showingOtherSettings) 3 else 4
        else -> 2
    }

    // Card aspect ratio: 1.05f for TV, 1.15f for tablets/phones
    val tileAspectRatio = when {
        isTv -> 1.05f
        isWideScreen -> 1.15f
        else -> 1.15f
    }

    val mainEntries = listOf(
        HubEntry(
            icon = Icons.Filled.WorkspacePremium,
            title = stringResource(R.string.settings_license_title),
            subtitle = stringResource(R.string.settings_license_subtitle),
            accentColor = Color(0xFFF59E0B), // Royal Amber / Gold
            onClick = onOpenLicense,
        ),
        HubEntry(
            icon = Icons.AutoMirrored.Filled.PlaylistPlay,
            title = stringResource(R.string.settings_providers_title),
            subtitle = stringResource(R.string.settings_providers_subtitle),
            accentColor = Color(0xFF06B6D4), // Electric Cyan
            onClick = onOpenProviders,
        ),
        HubEntry(
            icon = Icons.Filled.LiveTv,
            title = stringResource(R.string.common_channels),
            subtitle = stringResource(R.string.settings_channels_subtitle),
            accentColor = Color(0xFF3B82F6), // Royal Blue
            onClick = onOpenChannels,
        ),
        HubEntry(
            icon = Icons.Filled.QrCode2,
            title = stringResource(R.string.settings_webmanager_title),
            subtitle = stringResource(R.string.settings_webmanager_subtitle),
            accentColor = Color(0xFFF97316), // Sunset Orange
            onClick = onOpenWebManager,
        ),
        HubEntry(
            icon = Icons.Filled.DisplaySettings,
            title = stringResource(R.string.settings_display_title),
            subtitle = stringResource(R.string.settings_display_subtitle),
            accentColor = Color(0xFF6366F1), // Neon Indigo
            onClick = onOpenDisplay,
        ),
        HubEntry(
            icon = Icons.Filled.Subtitles,
            title = stringResource(R.string.settings_subtitles_hub_title),
            subtitle = stringResource(R.string.settings_subtitles_hub_subtitle),
            accentColor = Color(0xFFEC4899), // Hot Pink / Magenta
            onClick = onOpenSubtitles,
        ),
        HubEntry(
            icon = Icons.Filled.Tune,
            title = stringResource(R.string.settings_other_title),
            subtitle = stringResource(R.string.settings_other_subtitle),
            accentColor = Color(0xFF10B981), // Emerald Green
            onClick = { showingOtherSettings = true },
        ),
        HubEntry(
            icon = Icons.Filled.Info,
            title = stringResource(R.string.settings_about_title),
            subtitle = stringResource(R.string.settings_about_subtitle),
            accentColor = Color(0xFF94A3B8), // Diamond Platinum
            onClick = onOpenAbout,
        ),
    )

    val otherEntries = listOf(
        HubEntry(
            icon = Icons.Filled.Extension,
            title = stringResource(R.string.settings_addons_title),
            subtitle = stringResource(R.string.settings_addons_subtitle),
            accentColor = Color(0xFF8B5CF6), // Cyber Violet
            onClick = onOpenAddons,
        ),
        HubEntry(
            icon = Icons.Filled.ViewTimeline,
            title = stringResource(R.string.settings_guide_title),
            subtitle = stringResource(R.string.settings_guide_subtitle),
            accentColor = Color(0xFF10B981), // Emerald Green
            onClick = onOpenGuide,
        ),
        HubEntry(
            icon = Icons.Filled.Translate,
            title = stringResource(R.string.settings_language_title),
            subtitle = stringResource(R.string.settings_language_subtitle),
            accentColor = Color(0xFF14B8A6), // Mint Teal
            onClick = onOpenLanguage,
        ),
        HubEntry(
            icon = Icons.Filled.Bedtime,
            title = stringResource(R.string.settings_sleep_timer),
            subtitle = stringResource(R.string.settings_sleep_subtitle),
            accentColor = Color(0xFF818CF8), // Moonlit Indigo
            onClick = onOpenSleepTimer,
        ),
        HubEntry(
            icon = Icons.Filled.VideoLibrary,
            title = stringResource(R.string.settings_recording_title),
            subtitle = stringResource(R.string.settings_recording_subtitle),
            accentColor = Color(0xFFEF4444), // Ruby Crimson
            onClick = onOpenRecordings,
        ),
        HubEntry(
            icon = Icons.Filled.Security,
            title = stringResource(R.string.settings_parental_title),
            subtitle = stringResource(R.string.settings_parental_subtitle),
            accentColor = Color(0xFFE11D48), // Rose Shield
            onClick = onOpenParental,
        ),
        HubEntry(
            icon = Icons.Filled.CloudSync,
            title = stringResource(R.string.settings_sync_title),
            subtitle = stringResource(R.string.settings_sync_subtitle),
            accentColor = Color(0xFF0EA5E9), // Sky Blue
            onClick = onOpenSync,
        ),
        HubEntry(
            icon = Icons.Outlined.AccountCircle,
            title = stringResource(R.string.settings_profiles_hub_title),
            subtitle = stringResource(R.string.settings_profiles_hub_subtitle),
            accentColor = Color(0xFFA855F7), // Lavender Purple
            onClick = onOpenProfiles,
        ),
        HubEntry(
            icon = Icons.Filled.Search,
            title = stringResource(R.string.settings_search_hub_title),
            subtitle = stringResource(R.string.settings_search_hub_subtitle),
            accentColor = Color(0xFFEAB308), // Sunbeam Gold
            onClick = onOpenSearch,
        ),
    )

    val entries = if (showingOtherSettings) otherEntries else mainEntries

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080D17)),
    ) {
        // 3D Animated Ambient Light Canvas in background (gives true glass refraction depth)
        GlassAmbientBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (isWideScreen) 28.dp else 16.dp,
                    vertical = if (isWideScreen) 20.dp else 14.dp,
                ),
        ) {
            // Glassmorphic Top Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (isWideScreen) 18.dp else 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isWideScreen) 46.dp else 38.dp)
                        .clip(RoundedCornerShape(if (isWideScreen) 14.dp else 11.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.04f),
                                )
                            )
                        )
                        .border(
                            BorderStroke(
                                1.dp,
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.40f),
                                        Color.White.copy(alpha = 0.08f),
                                    )
                                )
                            ),
                            RoundedCornerShape(if (isWideScreen) 14.dp else 11.dp)
                        )
                        .then(
                            if (showingOtherSettings) {
                                Modifier.clickable { showingOtherSettings = false }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (showingOtherSettings) Icons.Filled.Tune else Icons.Filled.Tv,
                        contentDescription = null,
                        tint = if (showingOtherSettings) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isWideScreen) 26.dp else 22.dp),
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = if (showingOtherSettings) stringResource(R.string.settings_other_title) else stringResource(R.string.nav_settings),
                        style = if (isWideScreen) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = if (showingOtherSettings) {
                            stringResource(R.string.settings_other_subtitle)
                        } else {
                            if (isTv) "GECE IPTV • TV Kontrol Merkezi" else "GECE IPTV • Modern Ayarlar"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = if (isWideScreen) 12.sp else 10.5.sp,
                        color = Color.White.copy(alpha = 0.50f),
                    )
                }

                Spacer(Modifier.weight(1f))

                if (showingOtherSettings) {
                    HubTextButton("← " + stringResource(R.string.common_back)) {
                        showingOtherSettings = false
                    }
                } else {
                    HubTextButton(stringResource(R.string.common_done), onBack)
                }
            }

            // Grid of Settings Tiles (2 columns on phone, 4 on TV for main menu, 5 on TV for other menu)
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isWideScreen) 16.dp else 10.dp),
                verticalArrangement = Arrangement.spacedBy(if (isWideScreen) 16.dp else 10.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(entries, key = { it.title }) { entry ->
                    HubTile(
                        entry = entry,
                        isTv = isTv,
                        isWideScreen = isWideScreen,
                        aspectRatio = tileAspectRatio,
                    )
                }
            }
        }
    }
}

private data class HubEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
    val onClick: () -> Unit,
)

/**
 * Hardware-accelerated 3D ambient radial glow spheres floating in the background.
 * Creates authentic multi-spectral glass refraction when tiles overlay it.
 */
@Composable
private fun GlassAmbientBackdrop() {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_glow")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse1",
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(19000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulse2",
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Orb 1: Upper-left cyan & sapphire glow
        val rad1 = Math.toRadians(pulse1.toDouble())
        val ox1 = (w * 0.22f + Math.cos(rad1).toFloat() * (w * 0.08f))
        val oy1 = (h * 0.28f + Math.sin(rad1).toFloat() * (h * 0.06f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF06B6D4).copy(alpha = 0.18f),
                    Color(0xFF3B82F6).copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                center = Offset(ox1, oy1),
                radius = w * 0.45f,
            ),
            center = Offset(ox1, oy1),
            radius = w * 0.45f,
        )

        // Orb 2: Center-right violet & magenta glow
        val rad2 = Math.toRadians(pulse2.toDouble())
        val ox2 = (w * 0.80f + Math.cos(rad2).toFloat() * (w * 0.06f))
        val oy2 = (h * 0.58f + Math.sin(rad2).toFloat() * (h * 0.08f))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF8B5CF6).copy(alpha = 0.16f),
                    Color(0xFFEC4899).copy(alpha = 0.07f),
                    Color.Transparent,
                ),
                center = Offset(ox2, oy2),
                radius = w * 0.42f,
            ),
            center = Offset(ox2, oy2),
            radius = w * 0.42f,
        )

        // Orb 3: Bottom-left emerald & teal glow
        val ox3 = w * 0.35f
        val oy3 = h * 0.88f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF10B981).copy(alpha = 0.12f),
                    Color.Transparent,
                ),
                center = Offset(ox3, oy3),
                radius = w * 0.40f,
            ),
            center = Offset(ox3, oy3),
            radius = w * 0.40f,
        )
    }
}

@Composable
private fun HubTile(
    entry: HubEntry,
    isTv: Boolean,
    isWideScreen: Boolean,
    aspectRatio: Float,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Smooth 3D scale spring physics
    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.94f
            focused -> 1.05f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "tile_scale",
    )

    // 3D perspective rotation tilt
    val rotX by animateFloatAsState(
        targetValue = if (isPressed) -4f else if (focused) -2f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "tile_rotX",
    )

    val cardShape = RoundedCornerShape(if (isWideScreen) 22.dp else 16.dp)

    // Frosted glass background brush with depth
    val backgroundBrush = if (focused || isPressed) {
        Brush.linearGradient(
            colors = listOf(
                entry.accentColor.copy(alpha = 0.32f),
                Color(0xFF1E293B).copy(alpha = 0.88f),
                Color(0xFF0F172A).copy(alpha = 0.94f),
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.10f),
                Color(0xFF161F2E).copy(alpha = 0.65f),
                Color(0xFF0B0F17).copy(alpha = 0.82f),
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
        )
    }

    // Glass specular light refraction edge border (light source from top-left)
    val borderBrush = if (focused || isPressed) {
        Brush.linearGradient(
            colors = listOf(
                entry.accentColor,
                Color.White.copy(alpha = 0.75f),
                entry.accentColor.copy(alpha = 0.45f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.28f), // Specular light highlight at top-left edge
                Color.White.copy(alpha = 0.08f),
                entry.accentColor.copy(alpha = 0.16f), // Subtle tinted glass edge reflection
            )
        )
    }

    val borderWidth = if (focused || isPressed) 2.dp else 1.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = rotX
                cameraDistance = 16f * density
                shadowElevation = if (focused || isPressed) 16.dp.toPx() else 4.dp.toPx()
                shape = cardShape
                clip = false
            }
            .onFocusChanged { focused = it.isFocused }
            .clip(cardShape)
            .background(backgroundBrush)
            .border(width = borderWidth, brush = borderBrush, shape = cardShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = entry.onClick,
            )
            .padding(if (isWideScreen) 14.dp else 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Inner ambient radial reflection for luminous glass depth
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        entry.accentColor.copy(alpha = if (focused || isPressed) 0.35f else 0.12f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.20f),
                    radius = size.width * 0.70f,
                ),
                center = Offset(size.width * 0.82f, size.height * 0.20f),
                radius = size.width * 0.70f,
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            // 3D Embossed Glass Badge
            val badgeSize = if (isWideScreen) 54.dp else 42.dp
            val iconSize = if (isWideScreen) 28.dp else 22.dp
            val badgeShape = RoundedCornerShape(if (isWideScreen) 17.dp else 13.dp)

            val badgeBg = if (focused || isPressed) {
                Brush.radialGradient(
                    colors = listOf(
                        entry.accentColor,
                        entry.accentColor.copy(alpha = 0.70f),
                    ),
                    radius = 80f,
                )
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        entry.accentColor.copy(alpha = 0.18f),
                        Color.Black.copy(alpha = 0.35f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                )
            }

            val badgeBorder = BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (focused || isPressed) 0.90f else 0.45f),
                        entry.accentColor.copy(alpha = 0.35f),
                        Color.Transparent,
                    )
                ),
            )

            Box(
                modifier = Modifier
                    .size(badgeSize)
                    .clip(badgeShape)
                    .background(badgeBg)
                    .border(badgeBorder, shape = badgeShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = entry.icon,
                    contentDescription = null,
                    tint = if (focused || isPressed) Color.White else entry.accentColor,
                    modifier = Modifier.size(iconSize),
                )
            }

            Spacer(Modifier.height(if (isWideScreen) 10.dp else 6.dp))

            // Title
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                fontSize = if (isWideScreen) 14.sp else 13.sp,
                color = if (focused || isPressed) Color.White else MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = if (isWideScreen) 1 else 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(3.dp))

            // Subtitle
            Text(
                text = entry.subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = if (isWideScreen) 11.sp else 10.sp,
                lineHeight = if (isWideScreen) 14.sp else 13.sp,
                color = if (focused || isPressed) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun HubTextButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else if (focused) 1.05f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "btn_scale",
    )

    val shape = RoundedCornerShape(13.dp)
    val bgBrush = if (focused || isPressed) {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.16f),
                Color.White.copy(alpha = 0.05f),
            )
        )
    }

    val border = if (focused || isPressed) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.08f),
                )
            )
        )
    }

    Box(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(bgBrush)
            .border(border, shape = shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (focused || isPressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
