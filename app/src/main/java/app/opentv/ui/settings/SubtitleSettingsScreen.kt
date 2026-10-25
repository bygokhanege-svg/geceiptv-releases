/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.opentv.core.AppSettings

/**
 * Dedicated Subtitle Control screen.
 * Consolidates all subtitle settings, live preview, appearance options,
 * and online subtitle provider preferences.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }

    val subtitlesEnabled by settings.subtitlesEnabled.collectAsState()
    val subtitleLang by settings.subtitleLanguage.collectAsState()
    val subtitleSize by settings.subtitleTextSize.collectAsState()
    val subtitleColor by settings.subtitleTextColor.collectAsState()
    val subtitleBg by settings.subtitleBackground.collectAsState()
    val subtitleAutoSearch by settings.subtitleAutoSearch.collectAsState()
    val subtitleEncoding by settings.subtitleEncoding.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Filled.Subtitles,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Altyazı Ayarları",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            HubTextButton("Tamam", onBack)
        }

        Spacer(Modifier.height(18.dp))

        // Live Subtitle Preview Box
        SubtitleLivePreview(
            sizeMultiplier = subtitleSize,
            textColorTag = subtitleColor,
            bgStyleTag = subtitleBg,
        )

        Spacer(Modifier.height(24.dp))

        // Section 1: General Subtitle Preferences
        SubtitleSection(title = "Genel Altyazı Tercihleri") {
            SubtitleToggleRow(
                title = "Varsayılan Olarak Altyazıları Aç",
                subtitle = "Yayın veya video başladığında mevcut altyazıları otomatik olarak gösterir.",
                checked = subtitlesEnabled,
                onToggle = settings::setSubtitlesEnabled,
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = "Tercih Edilen Altyazı Dili",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "İçerik açıldığında ilk olarak bu dildeki altyazı parçası aranır.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val languages = listOf(
                    "tr" to "Türkçe",
                    "auto" to "Otomatik (Sistem)",
                    "en" to "English",
                    "de" to "Deutsch",
                    "fr" to "Français",
                    "es" to "Español",
                    "ar" to "العربية",
                    "ru" to "Русский",
                )
                languages.forEach { (tag, label) ->
                    SubtitleOptionChip(
                        label = label,
                        selected = subtitleLang == tag,
                        onSelect = { settings.setSubtitleLanguage(tag) },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Section 2: Appearance & Styling
        SubtitleSection(title = "Metin ve Görünüm Stili") {
            // Text Size
            Text(
                text = "Yazı Boyutu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val sizes = listOf(
                    0.8f to "Küçük (%75)",
                    1.0f to "Normal (%100)",
                    1.25f to "Büyük (%125)",
                    1.5f to "Çok Büyük (%150)",
                )
                sizes.forEach { (size, label) ->
                    SubtitleOptionChip(
                        label = label,
                        selected = subtitleSize == size,
                        onSelect = { settings.setSubtitleTextSize(size) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Text Color
            Text(
                text = "Yazı Rengi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val colors = listOf(
                    "white" to "Klasik Beyaz",
                    "yellow" to "Sinema Sarısı",
                    "cyan" to "Buz Mavisi",
                    "green" to "Canlı Yeşil",
                )
                colors.forEach { (tag, label) ->
                    SubtitleOptionChip(
                        label = label,
                        selected = subtitleColor == tag,
                        onSelect = { settings.setSubtitleTextColor(tag) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Background & Border Style
            Text(
                text = "Arka Plan & Kontur Stili",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val bgStyles = listOf(
                    "translucent" to "Yarı Saydam Kutu",
                    "outline" to "Sadece Kontur / Gölge",
                    "black" to "Tam Siyah Kutu",
                    "none" to "Arka Plansız",
                )
                bgStyles.forEach { (tag, label) ->
                    SubtitleOptionChip(
                        label = label,
                        selected = subtitleBg == tag,
                        onSelect = { settings.setSubtitleBackground(tag) },
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Section 3: Advanced & Online Subtitles
        SubtitleSection(title = "Gelişmiş & Çevrimiçi Altyazı") {
            SubtitleToggleRow(
                title = "OpenSubtitles Otomatik Arama",
                subtitle = "VOD film ve dizilerde yerleşik altyazı yoksa OpenSubtitles üzerinden otomatik arama yapar.",
                checked = subtitleAutoSearch,
                onToggle = settings::setSubtitleAutoSearch,
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Çoklu Çevrim İçi Altyazı Kaynakları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Arama yapıldığında etkin olan tüm kaynaklar aynı anda taranır ve listelenir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            val provOpenSub by settings.providerOpenSubtitles.collectAsState()
            val provSubDL by settings.providerSubDL.collectAsState()
            val provCommunity by settings.providerCommunity.collectAsState()

            SubtitleToggleRow(
                title = "OpenSubtitles v3",
                subtitle = "Dünyanın en geniş film ve dizi altyazı veritabanı (Türkçe ve çoklu dil).",
                checked = provOpenSub,
                onToggle = settings::setProviderOpenSubtitles,
            )
            SubtitleToggleRow(
                title = "TheSubtitleDB & SubDL",
                subtitle = "Geniş topluluk ve Türkçe destekli popüler altyazı kaynağı.",
                checked = provSubDL,
                onToggle = settings::setProviderSubDL,
            )
            SubtitleToggleRow(
                title = "Top Community",
                subtitle = "Doğrulanmış ve alternatif yedek altyazı havuzu.",
                checked = provCommunity,
                onToggle = settings::setProviderCommunity,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Section 4: Smart Synchronization & Timing
        val syncStepMs by settings.subtitleSyncStepMs.collectAsState()

        SubtitleSection(title = "Akıllı Senkronizasyon ve Zamanlama") {
            Text(
                text = "Varsayılan Senkronizasyon Adımı",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "Oynatıcı ekranında ileri/geri tuşlarına her basıldığında uygulanacak zaman farkı.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val steps = listOf(
                    250L to "0.25 saniye (Hassas İnce Ayar)",
                    500L to "0.5 saniye (Standart Önerilen)",
                    1000L to "1.0 saniye (Hızlı Atlama)",
                )
                steps.forEach { (step, label) ->
                    SubtitleOptionChip(
                        label = label,
                        selected = syncStepMs == step,
                        onSelect = { settings.setSubtitleSyncStepMs(step) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .padding(14.dp),
            ) {
                Column {
                    Text(
                        text = "⚡ Oynatıcı İçi Akıllı Senkronizasyon Özellikleri",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Film veya dizi oynatılırken Altyazı menüsünü açtığınızda canlı olarak milisaniye bazında altyazı gecikmesini ayarlayabilir, 23.976 <-> 25 FPS PAL framerate kaymasını tek tıkla düzeltebilir veya başlangıç jenerik farkını otomatik telafi edebilirsiniz.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

/**
 * Interactive preview of the subtitle text on top of a simulated cinematic video background.
 */
@Composable
private fun SubtitleLivePreview(
    sizeMultiplier: Float,
    textColorTag: String,
    bgStyleTag: String,
) {
    val textColor = when (textColorTag) {
        "yellow" -> Color(0xFFFFEB3B)
        "cyan" -> Color(0xFF4DD0E1)
        "green" -> Color(0xFF81C784)
        else -> Color.White
    }

    val boxBg = when (bgStyleTag) {
        "outline" -> Color.Transparent
        "black" -> Color.Black
        "none" -> Color.Transparent
        else -> Color.Black.copy(alpha = 0.65f)
    }

    val fontSize = (16 * sizeMultiplier).sp
    val lineHeight = (22 * sizeMultiplier).sp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(145.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1E2638),
                            Color(0xFF0F131D),
                        ),
                    ),
                )
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .padding(16.dp),
        ) {
            // Preview header badge
            Text(
                text = "CANLI ALTYAZI ÖNİZLEMESİ",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )

            // Simulated Subtitle Box
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(8.dp))
                    .background(boxBg)
                    .then(
                        if (bgStyleTag == "outline") {
                            Modifier.border(1.5.dp, Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "— GECE İPTV Player: Harika bir film başlıyor! (01:23:45)",
                    color = textColor,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "— Türkçe altyazı senkronizasyonu aktif.",
                    color = textColor,
                    fontSize = fontSize,
                    lineHeight = lineHeight,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun SubtitleSection(title: String, content: @Composable () -> Unit) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(8.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SubtitleOptionChip(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primaryContainer
                else if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant,
            )
            .border(
                width = if (focused) 2.dp else if (selected) 1.5.dp else 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary
                else if (selected) MaterialTheme.colorScheme.primary
                else Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(10.dp),
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = if (focused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (focused) MaterialTheme.colorScheme.onPrimaryContainer
            else if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SubtitleToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (focused) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else Color.Transparent,
            )
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).widthIn(max = 680.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (focused) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = if (focused) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
        )
    }
}
