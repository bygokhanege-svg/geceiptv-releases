/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.channels

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import app.opentv.R
import app.opentv.data.model.Programme
import app.opentv.data.model.shownName
import app.opentv.ui.ChannelsViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The Preview + Guide Side Pane:
 * - Top: Small Preview Video Player card for the currently highlighted channel.
 * - Underneath: Full Programme Guide (Yayın Akışı) for the highlighted channel.
 *
 * This design avoids rendering massive horizontal EPG timelines across hundreds of channels,
 * preventing stutter, frame drops, and buffering on TV boxes.
 */
@OptIn(UnstableApi::class)
@Composable
fun GuidePreview(
    row: ChannelsViewModel.Row?,
    nowMillis: Long,
    onWatch: () -> Unit,
    onRefresh: () -> Unit,
    onAddSource: () -> Unit,
    previewPlayer: ExoPlayer?,
    isRecording: Boolean = false,
    onRecord: () -> Unit = {},
    dayLabel: String = "",
    canGoPrevDay: Boolean = false,
    onPrevDay: () -> Unit = {},
    onNextDay: () -> Unit = {},
    onProgrammeClick: (Programme) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
    ) {
        // ---- 1. Küçük Ön İzleme Penceresi (Preview Player Card) ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(205.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(app.opentv.ui.theme.GecePalette.CanvasBg)
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .clickable(onClick = onWatch),
            contentAlignment = Alignment.Center,
        ) {
            // Channel logo fallback / shutter
            if (row != null) {
                AsyncImage(
                    model = row.primary.logoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
            }

            // Live preview player
            if (previewPlayer != null && row != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            useController = false
                            keepScreenOn = true
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                            setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            player = previewPlayer
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                        }
                    },
                    update = { it.player = previewPlayer },
                )
            }

            // Top-Right Quick Action Buttons
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (row != null) {
                    IconButton(onClick = onRecord, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                            contentDescription = if (isRecording) stringResource(R.string.guide_cd_stop_recording) else stringResource(R.string.guide_cd_record_now),
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                IconButton(onClick = onRefresh, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.guide_cd_refresh),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
                IconButton(onClick = onAddSource, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.guide_cd_add_source),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // Bottom-Left Overlay: "Tam Ekran İzle"
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                    .border(1.dp, app.opentv.ui.theme.GecePalette.Accent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = app.opentv.ui.theme.GecePalette.Accent,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.guide_watch_label),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ---- 2. Ön İzleme Penceresinin Altındaki Rehber Menüsü (Yayın Akışı) ----
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            if (row == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.guide_highlight_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                ) {
                    // Header: Channel Name + Day Navigation (‹ Bugün ›)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = row.primary.shownName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (row.variants.size > 1) {
                                Text(
                                    stringResource(R.string.guide_qualities_count, row.variants.size),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        // Day Navigation
                        if (dayLabel.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 4.dp),
                            ) {
                                IconButton(
                                    onClick = onPrevDay,
                                    enabled = canGoPrevDay,
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                        contentDescription = stringResource(R.string.guide_prev_day),
                                        tint = if (canGoPrevDay) MaterialTheme.colorScheme.onSurface
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                Text(
                                    dayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                                IconButton(
                                    onClick = onNextDay,
                                    modifier = Modifier.size(28.dp),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(R.string.guide_next_day),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    val nowProg = row.now
                    val allProgrammes = row.programmes
                    val upcoming = remember(allProgrammes, nowProg, nowMillis) {
                        allProgrammes.filter { it.id != nowProg?.id && it.endUtcMillis > nowMillis }
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        // NOW PLAYING CARD
                        if (nowProg != null) {
                            item(key = "now-card-${nowProg.id}") {
                                NowPlayingScheduleCard(
                                    programme = nowProg,
                                    nowMillis = nowMillis,
                                    onClick = { onProgrammeClick(nowProg) },
                                )
                            }
                        }

                        // UPCOMING PROGRAMMES LIST
                        if (upcoming.isNotEmpty()) {
                            item(key = "upcoming-header") {
                                Text(
                                    text = "Yayın Akışı",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                                )
                            }

                            items(upcoming, key = { it.id }) { prog ->
                                ProgrammeScheduleItem(
                                    programme = prog,
                                    isCatchup = row.primary.tvArchive && prog.endUtcMillis <= nowMillis,
                                    onClick = { onProgrammeClick(prog) },
                                )
                            }
                        } else if (nowProg == null && allProgrammes.isEmpty()) {
                            item(key = "empty-epg") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = stringResource(R.string.guide_no_info_channel),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NowPlayingScheduleCard(
    programme: Programme,
    nowMillis: Long,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            )
            .border(
                width = if (isFocused) 2.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(app.opentv.ui.theme.GecePalette.Accent.copy(alpha = 0.25f))
                    .border(1.dp, app.opentv.ui.theme.GecePalette.Accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "● ŞİMDİ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = app.opentv.ui.theme.GecePalette.Accent,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${formatTime(programme.startUtcMillis)} – ${formatTime(programme.endUtcMillis)}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = programme.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { programme.progressAt(nowMillis) },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
        )

        programme.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ProgrammeScheduleItem(
    programme: Programme,
    isCatchup: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            )
            .border(
                width = if (isFocused) 1.5.dp else 1.dp,
                color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatTime(programme.startUtcMillis),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = programme.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isFocused) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (isCatchup) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(app.opentv.ui.theme.GecePalette.Accent.copy(alpha = 0.2f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text(
                    text = "Geri Al",
                    style = MaterialTheme.typography.labelSmall,
                    color = app.opentv.ui.theme.GecePalette.Accent,
                )
            }
        }
    }
}

private val previewTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatTime(utcMillis: Long): String = previewTimeFormat.format(Date(utcMillis))
