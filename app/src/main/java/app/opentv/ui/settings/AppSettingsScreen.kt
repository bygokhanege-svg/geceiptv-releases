/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.content.Context
import android.widget.Toast
import app.opentv.R
import app.opentv.core.AppSettings
import app.opentv.core.DeviceOptimizationManager
import app.opentv.data.work.SyncWorker
import app.opentv.ui.theme.GecePalette
import kotlinx.coroutines.delay

/**
 * Display & playback preferences: the app-behaviour settings, kept apart from the guide/data
 * settings so neither screen becomes a junk drawer. Everything here writes straight to
 * [AppSettings] and takes effect immediately.
 */
@Composable
fun AppSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val themeMode by settings.themeMode.collectAsState()
    val colorTheme by settings.colorTheme.collectAsState()
    val channelLayout by settings.channelLayout.collectAsState()
    val previewVideo by settings.guidePreviewVideo.collectAsState()
    val previewSound by settings.guidePreviewSound.collectAsState()
    val resumeLast by settings.resumeLastChannel.collectAsState()
    val nextEpisodePrompt by settings.nextEpisodePromptEnabled.collectAsState()
    val tunnelingEnabled by settings.tunnelingEnabled.collectAsState()
    val dohEnabled by settings.dohEnabled.collectAsState()
    val playerClockEnabled by settings.playerClockEnabled.collectAsState()
    val liveEnabled by settings.liveEnabled.collectAsState()
    val moviesEnabled by settings.moviesEnabled.collectAsState()
    val seriesEnabled by settings.seriesEnabled.collectAsState()

    // A refresh next to each content toggle kicks a full catalogue re-sync (which now honours the
    // toggles, so a type just switched on is fetched). Runs as background work so it isn't cut short
    // if you leave this screen; a quick Toast confirms it started.
    val refreshingMessage = stringResource(R.string.settings_content_refreshing)
    val onRefreshContent: () -> Unit = {
        SyncWorker.refreshNow(context)
        Toast.makeText(context, refreshingMessage, Toast.LENGTH_SHORT).show()
    }

    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (isPhone) 16.dp else 32.dp, vertical = if (isPhone) 16.dp else 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(R.string.settings_display_title),
                style = if (isPhone) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
        }

        Spacer(Modifier.height(20.dp))

        DeviceOptimizerSection(settings)

        Spacer(Modifier.height(16.dp))

        SettingsSection(stringResource(R.string.settings_appearance)) {
            ThemeOption(stringResource(R.string.settings_theme_system), themeMode == AppSettings.ThemeMode.SYSTEM) {
                settings.setThemeMode(AppSettings.ThemeMode.SYSTEM)
            }
            ThemeOption(stringResource(R.string.settings_theme_dark), themeMode == AppSettings.ThemeMode.DARK) {
                settings.setThemeMode(AppSettings.ThemeMode.DARK)
            }
            ThemeOption(stringResource(R.string.settings_theme_light), themeMode == AppSettings.ThemeMode.LIGHT) {
                settings.setThemeMode(AppSettings.ThemeMode.LIGHT)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.settings_theme_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(stringResource(R.string.settings_color_theme)) {
            Text(
                stringResource(R.string.settings_color_theme_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            AppSettings.AppColorTheme.values().forEach { theme ->
                ColorThemeOption(
                    theme = theme,
                    selected = colorTheme == theme,
                    onSelect = { settings.setColorTheme(theme) },
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(stringResource(R.string.settings_section_content)) {
            Text(
                stringResource(R.string.settings_content_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            ContentToggleRow(
                title = stringResource(R.string.nav_live_tv),
                subtitle = stringResource(R.string.settings_content_live_subtitle),
                checked = liveEnabled,
                onToggle = settings::setLiveEnabled,
                onRefresh = onRefreshContent,
            )
            ContentToggleRow(
                title = stringResource(R.string.nav_movies),
                subtitle = stringResource(R.string.settings_content_movies_subtitle),
                checked = moviesEnabled,
                onToggle = settings::setMoviesEnabled,
                onRefresh = onRefreshContent,
            )
            ContentToggleRow(
                title = stringResource(R.string.nav_shows),
                subtitle = stringResource(R.string.settings_content_series_subtitle),
                checked = seriesEnabled,
                onToggle = settings::setSeriesEnabled,
                onRefresh = onRefreshContent,
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(stringResource(R.string.settings_section_guide)) {
            Text(
                stringResource(R.string.settings_channel_layout_title),
                style = MaterialTheme.typography.titleMedium,
            )
            ThemeOption(
                stringResource(R.string.settings_channel_layout_grid),
                channelLayout == AppSettings.ChannelLayout.GRID,
            ) { settings.setChannelLayout(AppSettings.ChannelLayout.GRID) }
            ThemeOption(
                stringResource(R.string.settings_channel_layout_list),
                channelLayout == AppSettings.ChannelLayout.LIST,
            ) { settings.setChannelLayout(AppSettings.ChannelLayout.LIST) }
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                title = stringResource(R.string.settings_live_preview_title),
                subtitle = stringResource(R.string.settings_live_preview_subtitle),
                checked = previewVideo,
                onToggle = settings::setGuidePreviewVideo,
            )
            ToggleRow(
                title = stringResource(R.string.settings_preview_sound_title),
                subtitle = stringResource(R.string.settings_preview_sound_subtitle),
                checked = previewSound,
                onToggle = settings::setGuidePreviewSound,
            )
        }

        Spacer(Modifier.height(16.dp))

        SettingsSection(stringResource(R.string.settings_section_playback)) {
            ToggleRow(
                title = stringResource(R.string.settings_resume_title),
                subtitle = stringResource(R.string.settings_resume_subtitle),
                checked = resumeLast,
                onToggle = settings::setResumeLastChannel,
            )
            ToggleRow(
                title = stringResource(R.string.settings_next_episode_title),
                subtitle = stringResource(R.string.settings_next_episode_subtitle),
                checked = nextEpisodePrompt,
                onToggle = settings::setNextEpisodePromptEnabled,
            )
            ToggleRow(
                title = stringResource(R.string.settings_tunneling_title),
                subtitle = stringResource(R.string.settings_tunneling_subtitle),
                checked = tunnelingEnabled,
                onToggle = settings::setTunnelingEnabled,
            )
            ToggleRow(
                title = stringResource(R.string.settings_doh_title),
                subtitle = stringResource(R.string.settings_doh_subtitle),
                checked = dohEnabled,
                onToggle = settings::setDohEnabled,
            )
            ToggleRow(
                title = stringResource(R.string.settings_player_clock_title),
                subtitle = stringResource(R.string.settings_player_clock_subtitle),
                checked = playerClockEnabled,
                onToggle = settings::setPlayerClockEnabled,
            )
        }

        VodBufferingSection(settings)

        TmdbKeySection(settings)
    }
}

@Composable
private fun VodBufferingSection(settings: AppSettings) {
    val currentMode by settings.vodBufferMode.collectAsState()

    SettingsSection("Film ve Dizi Akıllı Tamponlama (YouTube Modu)") {
        Text(
            "Film ve dizilerde donma ve takılmaları önlemek için arka planda ileriye doğru akıllı indirme yapar. LRU disk kalkanı sayesinde cihaz hafızası asla şişmez (Maks. 250 MB).",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        val options = listOf(
            Triple("FAST", "Hızlı Başlatma (30 sn)", "Düşük RAM'li cihazlar veya hızlı film atlama için ideal."),
            Triple("BALANCED", "Dengeli - YouTube Modu (90 sn - Önerilen)", "Arka planda 1.5 dakika önceden indirir; takılmaları ve dalgalanmaları tamamen önler."),
            Triple("ULTRA", "Süper Kararlı (120 sn)", "Zayıf Wi-Fi ve yavaş internet bağlantıları için 2 dakikalık derin tampon."),
        )

        options.forEach { (mode, label, desc) ->
            var isFocused by remember { mutableStateOf(false) }
            val isSelected = currentMode == mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        else if (isSelected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        else Color.Transparent,
                    )
                    .border(
                        if (isFocused) 2.dp else if (isSelected) 1.dp else 0.dp,
                        if (isFocused) MaterialTheme.colorScheme.primary else if (isSelected) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else Color.Transparent,
                        RoundedCornerShape(10.dp),
                    )
                    .clickable { settings.setVodBufferMode(mode) }
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = isSelected,
                    onClick = { settings.setVodBufferMode(mode) },
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

/**
 * Optional TMDB back-fill. The user pastes their own free key; it is stored on-device in
 * [AppSettings] and used to fill posters/backdrops/synopsis/cast a provider left blank. Empty by
 * default, so the feature is off until opted into.
 */
@Composable
private fun TmdbKeySection(settings: AppSettings) {
    val context = LocalContext.current
    val savedKey by settings.tmdbApiKey.collectAsState()
    var field by remember(savedKey) { mutableStateOf(savedKey) }
    val savedMessage = stringResource(R.string.settings_tmdb_saved)

    SettingsSection(stringResource(R.string.settings_section_metadata)) {
        Text(
            stringResource(R.string.settings_tmdb_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = field,
            onValueChange = { field = it.trim() },
            singleLine = true,
            label = { Text(stringResource(R.string.settings_tmdb_key_label)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                settings.setTmdbApiKey(field)
                Toast.makeText(context, savedMessage, Toast.LENGTH_SHORT).show()
            }) { Text(stringResource(R.string.settings_tmdb_save)) }
            if (savedKey.isNotBlank()) {
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.settings_tmdb_active),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
    Card {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
    }
}

@Composable
private fun ThemeOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ColorThemeOption(
    theme: AppSettings.AppColorTheme,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val primaryColor = Color(theme.primaryColorHex)
    val surfaceColor = Color(theme.surfaceColorHex)

    val backgroundModifier = when {
        isFocused -> Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        selected -> Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .border(1.5.dp, primaryColor, RoundedCornerShape(12.dp))
        else -> Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(surfaceColor, CircleShape)
                .border(2.dp, primaryColor, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(primaryColor, CircleShape)
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(theme.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = if (selected || isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).widthIn(max = 640.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

/**
 * A [ToggleRow] with a refresh button in front of the switch: toggle the content type on/off, or
 * tap refresh to re-sync it now. Only the label column toggles on tap, so the refresh button and
 * switch stay independently focusable for d-pad users.
 */
@Composable
private fun ContentToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .widthIn(max = 640.dp)
                .clickable { onToggle(!checked) },
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRefresh) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = stringResource(R.string.settings_content_refresh),
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onToggle)
    }
}

@Composable
private fun DeviceOptimizerSection(settings: AppSettings) {
    val context = LocalContext.current
    val hardwareInfo = remember { DeviceOptimizationManager.analyzeHardware(context) }
    val recommended = remember(hardwareInfo) { DeviceOptimizationManager.recommendProfile(hardwareInfo) }
    val activeProfileName by settings.hardwareProfile.collectAsState()
    val activeProfile = remember(activeProfileName) {
        DeviceOptimizationManager.HardwareProfile.values().find { it.name == activeProfileName }
            ?: DeviceOptimizationManager.HardwareProfile.BALANCED
    }

    var showDialog by remember { mutableStateOf(false) }

    SettingsSection(stringResource(R.string.settings_device_optimizer_title)) {
        Text(
            stringResource(R.string.settings_device_optimizer_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // Device Specs Summary Card
        val isPhone = LocalConfiguration.current.screenWidthDp < 600
        if (isPhone) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tv,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "${hardwareInfo.brand} ${hardwareInfo.model}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    // Current Active Badge
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(
                                Icons.Filled.Bolt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                stringResource(activeProfile.titleRes),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }

                Text(
                    text = "${hardwareInfo.socOrHardware} (${hardwareInfo.cpuCores} ${stringResource(R.string.settings_device_hw_cpu)}) • RAM: ${hardwareInfo.formattedRam}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Tv,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${hardwareInfo.brand} ${hardwareInfo.model}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${hardwareInfo.socOrHardware} (${hardwareInfo.cpuCores} ${stringResource(R.string.settings_device_hw_cpu)}) • RAM: ${hardwareInfo.formattedRam}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Current Active Badge
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            stringResource(activeProfile.titleRes),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Big Optimize Button
        var isOptimizeBtnFocused by remember { mutableStateOf(false) }
        Button(
            onClick = { showDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isOptimizeBtnFocused = it.isFocused }
                .border(
                    if (isOptimizeBtnFocused) 2.5.dp else 0.dp,
                    if (isOptimizeBtnFocused) GecePalette.Accent else Color.Transparent,
                    RoundedCornerShape(12.dp)
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOptimizeBtnFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 4.dp),
            ) {
                Icon(Icons.Filled.Speed, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.settings_device_optimizer_btn),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Manual Profiles Selection
        Text(
            stringResource(R.string.settings_device_active_profile),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(4.dp))

        DeviceOptimizationManager.HardwareProfile.values().forEach { profile ->
            HardwareProfileCard(
                profile = profile,
                selected = activeProfile == profile,
                isRecommended = recommended == profile,
                onSelect = {
                    DeviceOptimizationManager.applyProfile(context, profile)
                    Toast.makeText(
                        context,
                        context.getString(R.string.settings_device_optimizer_applied_toast),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showDialog) {
        DeviceOptimizationDialog(
            hardwareInfo = hardwareInfo,
            recommendedProfile = recommended,
            onDismiss = { showDialog = false },
            onApply = {
                DeviceOptimizationManager.applyProfile(context, recommended)
                showDialog = false
                Toast.makeText(
                    context,
                    context.getString(R.string.settings_device_optimizer_applied_toast),
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun HardwareProfileCard(
    profile: DeviceOptimizationManager.HardwareProfile,
    selected: Boolean,
    isRecommended: Boolean,
    onSelect: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val backgroundModifier = when {
        isFocused -> Modifier
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        selected -> Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
        else -> Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(backgroundModifier)
            .onFocusChanged { isFocused = it.isFocused }
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(profile.titleRes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected || isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isRecommended) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(GecePalette.AccentSubtle, RoundedCornerShape(6.dp))
                            .border(1.dp, GecePalette.Accent, RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.settings_device_recommended_badge),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = GecePalette.Accent,
                        )
                    }
                }
            }

            Spacer(Modifier.height(2.dp))

            Text(
                text = stringResource(profile.descRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(4.dp))

            // Tech specs badges
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "⏱ ${(profile.minBufferMs / 1000)}s - ${(profile.maxBufferMs / 1000)}s Buffer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "⚡ ${(profile.playbackStartBufferMs)}ms Start",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "🖥 ${profile.maxMultiViewSlots} Ekran",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                if (profile.hwTunneling) {
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "⚡ A/V Tunneling",
                        style = MaterialTheme.typography.labelSmall,
                        color = GecePalette.Accent,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceOptimizationDialog(
    hardwareInfo: DeviceOptimizationManager.DeviceHardwareInfo,
    recommendedProfile: DeviceOptimizationManager.HardwareProfile,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
) {
    var scanStep by remember { mutableIntStateOf(1) }
    var isScanning by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(450)
        scanStep = 2
        delay(450)
        scanStep = 3
        delay(450)
        scanStep = 4
        delay(350)
        isScanning = false
    }

    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f))
                .padding(if (isPhone) 12.dp else 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .widthIn(max = 680.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(if (isPhone) 16.dp else 24.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    if (isScanning) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    stringResource(R.string.settings_device_optimizer_scanning),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    stringResource(R.string.settings_device_optimizer_scan_chip),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        LinearProgressIndicator(
                            progress = { scanStep / 4f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        Spacer(Modifier.height(18.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ScanStepRow(
                                title = stringResource(R.string.settings_device_scan_step_1),
                                isDone = scanStep > 1,
                                isActive = scanStep == 1,
                            )
                            ScanStepRow(
                                title = stringResource(R.string.settings_device_scan_step_2),
                                isDone = scanStep > 2,
                                isActive = scanStep == 2,
                            )
                            ScanStepRow(
                                title = stringResource(R.string.settings_device_scan_step_3),
                                isDone = scanStep > 3,
                                isActive = scanStep == 3,
                            )
                            ScanStepRow(
                                title = stringResource(R.string.settings_device_scan_step_4),
                                isDone = scanStep >= 4,
                                isActive = scanStep == 4,
                            )
                        }
                    } else {
                        // Scan Completed UI
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(GecePalette.AccentSubtle, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = GecePalette.Accent,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    stringResource(R.string.settings_device_scan_completed),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${hardwareInfo.brand} ${hardwareInfo.model} • ${hardwareInfo.formattedRam} RAM",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        // Hardware Grid
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (isPhone) {
                                DialogSpecItem(
                                    label = stringResource(R.string.settings_device_hw_brand_model),
                                    value = "${hardwareInfo.brand} ${hardwareInfo.model}",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                DialogSpecItem(
                                    label = stringResource(R.string.settings_device_hw_ram),
                                    value = "${hardwareInfo.formattedRam} (${hardwareInfo.availableRamMb} MB boş)",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                DialogSpecItem(
                                    label = stringResource(R.string.settings_device_hw_cpu),
                                    value = "${hardwareInfo.socOrHardware} (${hardwareInfo.cpuCores} Çekirdek)",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                DialogSpecItem(
                                    label = stringResource(R.string.settings_device_hw_codec),
                                    value = if (hardwareInfo.hasHardwareHevc) "HEVC / 4K Donanım Aktif" else "Standart Çözücü",
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            } else {
                                Row(Modifier.fillMaxWidth()) {
                                    DialogSpecItem(
                                        label = stringResource(R.string.settings_device_hw_brand_model),
                                        value = "${hardwareInfo.brand} ${hardwareInfo.model}",
                                        modifier = Modifier.weight(1f),
                                    )
                                    DialogSpecItem(
                                        label = stringResource(R.string.settings_device_hw_ram),
                                        value = "${hardwareInfo.formattedRam} (${hardwareInfo.availableRamMb} MB boş)",
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                Row(Modifier.fillMaxWidth()) {
                                    DialogSpecItem(
                                        label = stringResource(R.string.settings_device_hw_cpu),
                                        value = "${hardwareInfo.socOrHardware} (${hardwareInfo.cpuCores} Çekirdek)",
                                        modifier = Modifier.weight(1f),
                                    )
                                    DialogSpecItem(
                                        label = stringResource(R.string.settings_device_hw_codec),
                                        value = if (hardwareInfo.hasHardwareHevc) "HEVC / 4K Donanım Aktif" else "Standart Çözücü",
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Recommended Profile Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
                                .padding(14.dp),
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(GecePalette.Accent, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            stringResource(R.string.settings_device_recommended_badge),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = GecePalette.OnAccent,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(recommendedProfile.titleRes),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }

                                Spacer(Modifier.height(6.dp))

                                Text(
                                    stringResource(recommendedProfile.descRes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                Spacer(Modifier.height(10.dp))

                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    OptimizationCheckItem("Tampon Süresi: ${(recommendedProfile.minBufferMs / 1000)}s - ${(recommendedProfile.maxBufferMs / 1000)}s (RAM ve donma optimizasyonu)")
                                    OptimizationCheckItem("Hızlı Başlatma: ${recommendedProfile.playbackStartBufferMs}ms anında yayın akışı")
                                    OptimizationCheckItem("Ön İzleme: ${if (recommendedProfile.previewVideoEnabled) "Canlı video (Akıcı)" else "Hafif Mod (Düşük bellek koruması)"}")
                                    OptimizationCheckItem("Çoklu Ekran Sınırı: ${recommendedProfile.maxMultiViewSlots} eşzamanlı video çözücü")
                                    OptimizationCheckItem("OutOfMemory & Çökme Koruması: Aktif")
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedButton(onClick = onDismiss) {
                                Text(stringResource(R.string.common_cancel))
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(
                                onClick = onApply,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(10.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        stringResource(R.string.settings_device_optimizer_apply_btn),
                                        fontWeight = FontWeight.Bold,
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
private fun ScanStepRow(title: String, isDone: Boolean, isActive: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDone) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = GecePalette.Accent,
                modifier = Modifier.size(18.dp),
            )
        } else if (isActive) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isActive || isDone) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun DialogSpecItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun OptimizationCheckItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Filled.Check,
            contentDescription = null,
            tint = GecePalette.Accent,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
