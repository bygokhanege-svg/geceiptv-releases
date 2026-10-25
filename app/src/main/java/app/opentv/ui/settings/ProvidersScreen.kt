/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.data.model.LiveStreamFormat
import app.opentv.data.model.Source
import app.opentv.data.model.SourceKind
import app.opentv.ui.SourcesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The provider list: what's connected, with the plumbing to add another, remove one,
 * and perform live connection speed and diagnostic tests.
 */
@Composable
fun ProvidersScreen(
    onAddSource: () -> Unit,
    onEditSource: (Source) -> Unit,
    onBack: () -> Unit,
    viewModel: SourcesViewModel = viewModel(),
) {
    val ui by viewModel.ui.collectAsState()
    val speedTestState by viewModel.speedTest.collectAsState()

    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = if (isPhone) 16.dp else 32.dp, vertical = if (isPhone) 16.dp else 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.settings_providers_title),
                style = if (isPhone) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onAddSource) { Text(stringResource(R.string.providers_add)) }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
        }

        Spacer(Modifier.height(16.dp))

        if (ui.sources.isEmpty()) {
            Text(
                stringResource(R.string.providers_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@Column
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.widthIn(max = 840.dp),
        ) {
            items(ui.sources, key = { it.id }) { source ->
                ProviderRow(
                    source = source,
                    onEdit = { onEditSource(source) },
                    onRemove = { viewModel.delete(source) },
                    onSetLiveFormat = { viewModel.setLiveFormat(source, it) },
                    onRunSpeedTest = { viewModel.runSpeedTest(source) },
                )
            }

            // Diagnostic Results / Progress Panel
            item {
                when (val state = speedTestState) {
                    is SourcesViewModel.SpeedTestState.Testing -> {
                        SpeedTestingCard()
                    }
                    is SourcesViewModel.SpeedTestState.Success -> {
                        SpeedTestResultCard(
                            result = state.result,
                            onDismiss = { viewModel.resetSpeedTest() },
                            onRetest = {
                                val src = ui.sources.find { it.id == state.result.sourceId } ?: ui.sources.firstOrNull()
                                src?.let { viewModel.runSpeedTest(it) }
                            },
                        )
                    }
                    is SourcesViewModel.SpeedTestState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "⚠️ Bağlantı Testi Başarısız: ${state.message}",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Spacer(Modifier.height(8.dp))
                                Row {
                                    val src = ui.sources.find { it.id == state.sourceId } ?: ui.sources.firstOrNull()
                                    if (src != null) {
                                        Button(onClick = { viewModel.runSpeedTest(src) }) { Text(stringResource(R.string.common_try_again)) }
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    OutlinedButton(onClick = { viewModel.resetSpeedTest() }) { Text(stringResource(R.string.common_done)) }
                                }
                            }
                        }
                    }
                    is SourcesViewModel.SpeedTestState.Idle -> {
                        // Global Speed Test Button at the bottom
                        if (ui.sources.isNotEmpty()) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            stringResource(R.string.provider_speed_test_banner_title),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            stringResource(R.string.provider_speed_test_banner_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Button(
                                        onClick = { viewModel.runSpeedTest(ui.sources.first()) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = app.opentv.ui.theme.GecePalette.Accent,
                                            contentColor = app.opentv.ui.theme.GecePalette.OnAccent,
                                        ),
                                    ) {
                                        Text(stringResource(R.string.provider_speed_test_start_btn), fontWeight = FontWeight.Bold)
                                    }
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
private fun ProviderRow(
    source: Source,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onSetLiveFormat: (LiveStreamFormat) -> Unit,
    onRunSpeedTest: () -> Unit,
) {
    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isPhone) 14.dp else 18.dp, vertical = 14.dp),
        ) {
            if (isPhone) {
                // Mobile layout: Title & Host on top
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(source.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            source.kind.name,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    hostOf(source.url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // 1. Birincil / Kolay Ulaşılabilir Konum: Otomatik, HLS, MPEG-TS format butonları
                if (source.kind == SourceKind.XTREAM) {
                    Spacer(Modifier.height(12.dp))
                    StreamFormatSelector(
                        selected = source.liveFormat,
                        onSelect = onSetLiveFormat,
                        showHint = false,
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 2. İkincil Konum: Hız Testi, Düzenle, Kaldır butonları
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = onRunSpeedTest) {
                        Text(stringResource(R.string.provider_speed_test_btn))
                    }
                    TextButton(onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text(stringResource(R.string.common_remove))
                    }
                }

                if (source.kind == SourceKind.XTREAM) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.provider_stream_format_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                // TV layout:
                // Üst Satır (Birincil): Sağlayıcı Adı & Host solda, Otomatik / HLS / MPEG-TS butonları sağda
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(source.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    source.kind.name,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            hostOf(source.url),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    if (source.kind == SourceKind.XTREAM) {
                        // Xtream için birincil kolay ulaşılabilir yerde: Otomatik, HLS, MPEG-TS
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            FormatSegment(
                                label = stringResource(R.string.provider_stream_format_auto),
                                selected = source.liveFormat == LiveStreamFormat.AUTO,
                                onClick = { onSetLiveFormat(LiveStreamFormat.AUTO) },
                            )
                            FormatSegment(
                                label = stringResource(R.string.provider_stream_format_hls),
                                selected = source.liveFormat == LiveStreamFormat.HLS,
                                onClick = { onSetLiveFormat(LiveStreamFormat.HLS) },
                            )
                            FormatSegment(
                                label = stringResource(R.string.provider_stream_format_ts),
                                selected = source.liveFormat == LiveStreamFormat.MPEG_TS,
                                onClick = { onSetLiveFormat(LiveStreamFormat.MPEG_TS) },
                            )
                        }
                    } else {
                        // Xtream olmayanlar için Hız Testi, Düzenle, Kaldır
                        FilledTonalButton(onClick = onRunSpeedTest) {
                            Text(stringResource(R.string.provider_speed_test_btn))
                        }
                        Spacer(Modifier.width(6.dp))
                        TextButton(onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
                        Spacer(Modifier.width(4.dp))
                        TextButton(
                            onClick = onRemove,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        ) {
                            Text(stringResource(R.string.common_remove))
                        }
                    }
                }

                // Alt Satır (İkincil): Xtream için Hız Testi, Düzenle, Kaldır butonları ve format ipucu
                if (source.kind == SourceKind.XTREAM) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            stringResource(R.string.provider_stream_format_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(end = 16.dp),
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(onClick = onRunSpeedTest) {
                                Text(stringResource(R.string.provider_speed_test_btn))
                            }
                            Spacer(Modifier.width(6.dp))
                            TextButton(onClick = onEdit) { Text(stringResource(R.string.common_edit)) }
                            Spacer(Modifier.width(4.dp))
                            TextButton(
                                onClick = onRemove,
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Text(stringResource(R.string.common_remove))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedTestingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(app.opentv.ui.theme.GecePalette.Accent),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.provider_speed_test_running_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.provider_speed_test_running_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = app.opentv.ui.theme.GecePalette.Accent,
            )
        }
    }
}

@Composable
private fun SpeedTestResultCard(
    result: SourcesViewModel.SpeedTestResult,
    onDismiss: () -> Unit,
    onRetest: () -> Unit,
) {
    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val unspecifiedStr = stringResource(R.string.provider_date_unspecified)
    val createdStr = remember(result.createdAtMillis, result.createdAtRaw, unspecifiedStr) {
        when {
            result.createdAtMillis != null && result.createdAtMillis > 0 -> dateFormatter.format(Date(result.createdAtMillis))
            !result.createdAtRaw.isNullOrBlank() -> result.createdAtRaw
            else -> unspecifiedStr
        }
    }
    val unlimitedStr = stringResource(R.string.provider_date_unlimited)
    val expiryStr = remember(result.expiryMillis, unlimitedStr) {
        when {
            result.expiryMillis != null && result.expiryMillis > 0 -> dateFormatter.format(Date(result.expiryMillis))
            else -> unlimitedStr
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, app.opentv.ui.theme.GecePalette.CardBorder, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = app.opentv.ui.theme.GecePalette.CardSurface),
    ) {
        Column(Modifier.padding(20.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡", fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            stringResource(R.string.provider_speed_test_report_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            "${result.sourceName} · ${result.serverHost}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = onRetest) { Text(stringResource(R.string.provider_speed_test_retest)) }
                    OutlinedButton(onClick = onDismiss) { Text(stringResource(R.string.common_done)) }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Stats Grid (Ping & Speed & Formats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Ping Box
                MetricBox(
                    title = stringResource(R.string.provider_metric_ping),
                    value = "${result.pingMs} ms",
                    subtext = when {
                        result.pingMs < 60 -> stringResource(R.string.provider_ping_excellent)
                        result.pingMs < 130 -> stringResource(R.string.provider_ping_good)
                        else -> stringResource(R.string.provider_ping_normal)
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF22C55E),
                )

                // Speed Box
                MetricBox(
                    title = stringResource(R.string.provider_metric_speed),
                    value = "${result.speedMbps} Mbps",
                    subtext = when {
                        result.speedMbps >= 30.0 -> stringResource(R.string.provider_speed_4k)
                        result.speedMbps >= 12.0 -> stringResource(R.string.provider_speed_fhd)
                        else -> stringResource(R.string.provider_speed_sd)
                    },
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF38BDF8),
                )

                // Account Dates Box
                MetricBox(
                    title = stringResource(R.string.provider_metric_dates),
                    value = expiryStr,
                    subtext = stringResource(R.string.provider_date_created, createdStr),
                    modifier = Modifier.weight(1.2f),
                    accentColor = Color(0xFFA855F7),
                )
            }

            Spacer(Modifier.height(12.dp))

            // Secondary Details (Stream Formats & Active Connections)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Stream Formats Box
                MetricBox(
                    title = stringResource(R.string.provider_metric_format),
                    value = result.streamFormats.joinToString(", ").ifEmpty { "HLS, TS" },
                    subtext = stringResource(R.string.provider_active_choice, result.activeLiveFormat.name),
                    modifier = Modifier.weight(1.4f),
                    accentColor = Color(0xFFF59E0B),
                )

                // Connections & Status Box
                MetricBox(
                    title = stringResource(R.string.provider_metric_status),
                    value = stringResource(R.string.provider_conns_active, result.activeConnections ?: 1, result.maxConnections ?: 1),
                    subtext = stringResource(R.string.provider_status_online, result.status),
                    modifier = Modifier.weight(1f),
                    accentColor = Color(0xFF10B981),
                )
            }
        }
    }
}

@Composable
private fun MetricBox(
    title: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    accentColor: Color,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(app.opentv.ui.theme.GecePalette.CardElevated)
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(12.dp),
    ) {
        Column {
            Text(
                title,
                color = accentColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                subtext,
                color = Color(0xFF94A3B8),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * The per-source HLS / MPEG-TS picker. A compact two-option segmented control (the selected
 * container is a filled button, the other outlined) with a one-line hint on when to reach for it.
 */
@Composable
private fun StreamFormatSelector(
    selected: LiveStreamFormat,
    onSelect: (LiveStreamFormat) -> Unit,
    showHint: Boolean = true,
) {
    Column {
        Text(
            stringResource(R.string.provider_stream_format),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FormatSegment(
                label = stringResource(R.string.provider_stream_format_auto),
                selected = selected == LiveStreamFormat.AUTO,
                onClick = { onSelect(LiveStreamFormat.AUTO) },
            )
            FormatSegment(
                label = stringResource(R.string.provider_stream_format_hls),
                selected = selected == LiveStreamFormat.HLS,
                onClick = { onSelect(LiveStreamFormat.HLS) },
            )
            FormatSegment(
                label = stringResource(R.string.provider_stream_format_ts),
                selected = selected == LiveStreamFormat.MPEG_TS,
                onClick = { onSelect(LiveStreamFormat.MPEG_TS) },
            )
        }
        if (showHint) {
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.provider_stream_format_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One option of the stream-format control: filled when chosen, outlined otherwise. */
@Composable
private fun FormatSegment(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

/** Host only — the full URL carries the login and has no place on a settings list. */
private fun hostOf(url: String): String =
    runCatching { java.net.URI(url).host ?: url }.getOrDefault(url)
