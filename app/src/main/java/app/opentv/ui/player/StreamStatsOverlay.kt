/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.opentv.R
import app.opentv.core.AppSettings
import app.opentv.player.PlayerController
import app.opentv.ui.theme.GecePalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import java.util.Locale

/**
 * Minimalist digital clock in the top-right corner of the video player.
 * Frameless, iconless, with 30% reduced opacity (~0.65f) and shadow so it stays legible without distracting.
 */
@Composable
fun PlayerClockBadge(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var currentTime by remember { mutableStateOf(getFormattedTime()) }

    LaunchedEffect(Unit) {
        while (isActive) {
            currentTime = getFormattedTime()
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ) else Modifier
            )
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = currentTime,
            color = Color.White.copy(alpha = 0.65f),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            style = TextStyle(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(1.5f, 1.5f),
                    blurRadius = 4f,
                )
            ),
        )
    }
}

private fun getFormattedTime(): String {
    val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    return formatter.format(Date())
}

/**
 * Detailed Stream & Hardware Analytics Modal (Nerd Stats)
 */
@Composable
fun StreamStatsDialog(
    controller: PlayerController,
    settings: AppSettings,
    onDismiss: () -> Unit,
) {
    var stats by remember { mutableStateOf(controller.getStreamStats()) }
    var clockEnabled by remember { mutableStateOf(settings.playerClockEnabled.value) }

    // Live update stats every second while modal is visible
    LaunchedEffect(Unit) {
        while (isActive) {
            stats = controller.getStreamStats()
            delay(1000)
        }
    }

    val closeFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(60)
        runCatching { closeFocus.requestFocus() }
    }

    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.78f))
                .padding(if (isPhone) 12.dp else 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GecePalette.Accent.copy(alpha = 0.5f)),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(if (isPhone) 16.dp else 22.dp),
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(GecePalette.AccentSubtle, RoundedCornerShape(10.dp))
                                    .border(1.dp, GecePalette.Accent, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Analytics,
                                    contentDescription = null,
                                    tint = GecePalette.Accent,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.player_stats_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                                Text(
                                    text = stats.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.focusRequester(closeFocus),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White.copy(alpha = 0.8f))
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // 1. Connection & Bandwidth Card
                    StatsSectionCard(
                        icon = Icons.Filled.Speed,
                        title = stringResource(R.string.player_stats_connection),
                        accentColor = GecePalette.Accent,
                    ) {
                        StatsRowItem("İndirme / Okuma Hızı", stats.connectionSpeed, highlight = true)
                        StatsRowItem("Video Akış Bitrate", stats.videoBitrate)
                        StatsRowItem("Taşıyıcı / Protokol", stats.containerFormat)
                    }

                    Spacer(Modifier.height(10.dp))

                    // 2. Video & Resolution Card
                    StatsSectionCard(
                        icon = Icons.Filled.HighQuality,
                        title = stringResource(R.string.player_stats_video_specs),
                        accentColor = GecePalette.Accent,
                    ) {
                        StatsRowItem("Çözünürlük & Ebat", stats.resolution, highlight = true)
                        StatsRowItem("Kare Hızı (FPS)", stats.fps)
                        StatsRowItem("Video Kod Çözücü", stats.videoCodec)
                        StatsRowItem(
                            "Donanım Tünelleme",
                            if (stats.tunnelingActive) "Aktif (A/V Tunneling) ⚡" else "Standart Donanım",
                            highlight = stats.tunnelingActive,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // 3. Audio System Card
                    StatsSectionCard(
                        icon = Icons.Filled.VolumeUp,
                        title = stringResource(R.string.player_stats_audio),
                        accentColor = GecePalette.Accent,
                    ) {
                        StatsRowItem("Ses Formatı", stats.audioCodec)
                        StatsRowItem("Kanal Düzeni", stats.audioChannels)
                        StatsRowItem("Örnekleme Hızı", stats.audioSampleRate)
                    }

                    Spacer(Modifier.height(10.dp))

                    // 4. Buffer & Stream Health Card
                    StatsSectionCard(
                        icon = Icons.Filled.Tune,
                        title = stringResource(R.string.player_stats_buffer),
                        accentColor = GecePalette.Accent,
                    ) {
                        StatsRowItem("Tampon Süresi", stats.bufferDuration)
                        StatsRowItem("Tampon Doluluk Oranı", "%${stats.bufferPercentage}")
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { stats.bufferPercentage / 100f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = GecePalette.Accent,
                            trackColor = Color.White.copy(alpha = 0.1f),
                        )
                        if (stats.estimatedFileSize != null) {
                            Spacer(Modifier.height(6.dp))
                            StatsRowItem(stringResource(R.string.player_stats_file_size), stats.estimatedFileSize!!, highlight = true)
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 5. Quick Clock Toggle Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .clickable {
                                clockEnabled = !clockEnabled
                                settings.setPlayerClockEnabled(clockEnabled)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Schedule, contentDescription = null, tint = GecePalette.Accent, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.player_stats_clock_toggle),
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Switch(
                            checked = clockEnabled,
                            onCheckedChange = {
                                clockEnabled = it
                                settings.setPlayerClockEnabled(it)
                            },
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // Close Button
                    var isBtnFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isBtnFocused = it.isFocused }
                            .border(
                                if (isBtnFocused) 2.dp else 0.dp,
                                if (isBtnFocused) GecePalette.Accent else Color.Transparent,
                                RoundedCornerShape(10.dp),
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isBtnFocused) GecePalette.Accent else Color.White.copy(alpha = 0.12f),
                        ),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.common_done),
                            color = if (isBtnFocused) GecePalette.OnAccent else Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsSectionCard(
    icon: ImageVector,
    title: String,
    accentColor: Color,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
        content()
    }
}

@Composable
private fun StatsRowItem(
    label: String,
    value: String,
    highlight: Boolean = false,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.65f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) GecePalette.Accent else Color.White,
            fontFamily = FontFamily.Monospace,
        )
    }
}
