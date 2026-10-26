/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.onboarding

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.opentv.R
import app.opentv.core.AppSettings
import app.opentv.data.model.Source
import app.opentv.data.model.SourceKind
import app.opentv.ui.LegalDisclaimerAcceptanceCard
import app.opentv.ui.LegalDisclaimerModal
import app.opentv.ui.SourcesViewModel
import app.opentv.ui.channels.readClipboardText
import app.opentv.ui.theme.GecePalette

/**
 * GECE İPTV Player - Profesyonel & Glassmorphism Sağlayıcı Ekleme/Düzenleme Menüsü.
 * Televizyon, Tablet ve Telefon cihazlarında aynı lüks, modern ve birleşik arayüzü sunar.
 */
@Composable
fun AddSourceScreen(
    viewModel: SourcesViewModel,
    onFinished: () -> Unit,
    editingSourceId: Long? = null,
) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val disclaimerAccepted by settings.disclaimerAccepted.collectAsState()
    var showLegalModal by remember { mutableStateOf(false) }

    // Birleşik menü yaklaşımı: TV, tablet ve telefon doğrudan aynı profesyonel menüyle açılır.
    // Kullanıcı dilerse "Telefondan QR ile Kur" butonuna basarak eşleme ekranına geçebilir.
    var usePhone by remember { mutableStateOf(false) }

    if (usePhone) {
        PhonePairingScreen(
            onReceived = { draft -> viewModel.saveAndSync(draft) { onFinished() } },
            onCancel = { usePhone = false },
        )
        return
    }

    val ui by viewModel.ui.collectAsState()

    // Düzenleme modunda mevcut verileri doldur
    val existing = remember(editingSourceId, ui.sources) {
        editingSourceId?.let { id -> ui.sources.firstOrNull { it.id == id } }
    }

    fun extractCleanMac(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val trimmed = raw.trim()
        val match = Regex("""([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})""").find(trimmed)
        return match?.value?.uppercase() ?: trimmed.substringBefore(";").substringBefore("|").trim()
    }

    fun extractParam(raw: String?, vararg keys: String): String {
        if (raw.isNullOrBlank()) return ""
        val tokens = raw.trim().split(";", "|")
        for (token in tokens) {
            val eqIdx = token.indexOf('=')
            if (eqIdx > 0) {
                val k = token.substring(0, eqIdx).trim().lowercase()
                val v = token.substring(eqIdx + 1).trim()
                if (keys.any { it.equals(k, ignoreCase = true) }) {
                    return v
                }
            }
        }
        return ""
    }

    fun buildMacPayload(cleanMac: String, snVal: String, dev1Val: String, dev2Val: String): String? {
        val trimmedMac = cleanMac.trim()
        if (trimmedMac.isBlank()) return null
        val parts = mutableListOf<String>()
        parts.add(trimmedMac)
        if (snVal.isNotBlank()) parts.add("sn=${snVal.trim()}")
        if (dev1Val.isNotBlank()) parts.add("device_id=${dev1Val.trim()}")
        if (dev2Val.isNotBlank()) parts.add("device_id2=${dev2Val.trim()}")
        return parts.joinToString(";")
    }

    var kind by remember(existing?.id) { mutableStateOf(existing?.kind ?: SourceKind.XTREAM) }
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
    var url by remember(existing?.id) { mutableStateOf(existing?.url ?: "") }
    var username by remember(existing?.id) { mutableStateOf(existing?.username ?: "") }
    var password by remember(existing?.id) { mutableStateOf(existing?.password ?: "") }
    var showPassword by remember { mutableStateOf(false) }

    var mac by remember(existing?.id) { mutableStateOf(extractCleanMac(existing?.macAddress)) }
    var sn by remember(existing?.id) { mutableStateOf(extractParam(existing?.macAddress, "sn", "serial", "sncut")) }
    var deviceId1 by remember(existing?.id) { mutableStateOf(extractParam(existing?.macAddress, "device_id", "device_id1", "deviceid", "deviceid1")) }
    var deviceId2 by remember(existing?.id) { mutableStateOf(extractParam(existing?.macAddress, "device_id2", "deviceid2")) }
    var showMagIdentities by remember { mutableStateOf(sn.isNotBlank() || deviceId1.isNotBlank() || deviceId2.isNotBlank()) }

    var epgUrl by remember(existing?.id) { mutableStateOf(existing?.epgUrl ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }
    var userAgent by remember(existing?.id) { mutableStateOf(existing?.userAgent ?: Source.DEFAULT_USER_AGENT) }

    fun draft(): Source = (existing ?: Source(name = "", kind = kind, url = "")).copy(
        name = name.ifBlank {
            if (kind == SourceKind.M3U) context.getString(R.string.onboarding_default_playlist_name)
            else context.getString(R.string.onboarding_default_provider_name)
        },
        kind = kind,
        url = url,
        username = username.takeIf { it.isNotBlank() },
        password = password.takeIf { it.isNotBlank() },
        macAddress = if (kind == SourceKind.STALKER) buildMacPayload(mac, sn, deviceId1, deviceId2) else null,
        epgUrl = epgUrl.takeIf { it.isNotBlank() },
        userAgent = userAgent.ifBlank { Source.DEFAULT_USER_AGENT },
    )

    val canSubmit = url.isNotBlank() && when (kind) {
        SourceKind.XTREAM -> username.isNotBlank() && password.isNotBlank()
        SourceKind.M3U -> true
        SourceKind.STALKER -> mac.isNotBlank()
    }

    // Akıllı URL yapıştırma analizi: kullanıcı tam bir Xtream m3u linki yapıştırırsa
    // kullanıcı adı, şifre ve sunucu adresini anında ayıklar ve ilgili alanlara doldurur.
    val onUrlChange: (String) -> Unit = { input ->
        val trimmed = input.trim()
        if (trimmed.contains("username=") && trimmed.contains("password=")) {
            try {
                val uri = android.net.Uri.parse(trimmed)
                val u = uri.getQueryParameter("username")
                val p = uri.getQueryParameter("password")
                if (!u.isNullOrBlank() && !p.isNullOrBlank()) {
                    kind = SourceKind.XTREAM
                    username = u
                    password = p
                    val portPart = if (uri.port != -1) ":${uri.port}" else ""
                    val scheme = uri.scheme ?: "http"
                    val host = uri.host ?: uri.authority ?: ""
                    url = if (host.isNotBlank()) "$scheme://$host$portPart" else trimmed
                } else {
                    url = trimmed
                }
            } catch (e: Exception) {
                url = trimmed
            }
        } else {
            url = trimmed
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWideScreen = screenWidthDp >= 580

    // TextField renk paleti: Glassmorphism koyu cam zemin ve altın vurgu
    val glassTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = Color(0x800E121D),
        unfocusedContainerColor = Color(0x4D0E121D),
        focusedBorderColor = GecePalette.Accent,
        unfocusedBorderColor = GecePalette.CardBorder,
        focusedLabelColor = GecePalette.Accent,
        unfocusedLabelColor = GecePalette.TextSecondary,
        focusedTextColor = GecePalette.TextPrimary,
        unfocusedTextColor = GecePalette.TextPrimary,
        cursorColor = GecePalette.Accent,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GecePalette.CanvasBg),
    ) {
        // 1. Sinematik Arka Plan Altın Ortam Işıması (Radial Glows)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Üst merkez altın ambiyansı
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x28F59E0B),
                        Color(0x0CF59E0B),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.12f),
                    radius = size.maxDimension * 0.55f,
                ),
            )
            // Sağ alt hafif parıltı
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x15F59E0B),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.88f),
                    radius = size.maxDimension * 0.40f,
                ),
            )
        }

        // 2. Ana Kaydırılabilir İçerik
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = if (screenWidthDp >= 600) 36.dp else 16.dp,
                    vertical = if (screenWidthDp >= 600) 28.dp else 18.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp),
            ) {
                // LÜKS GLASSMORPHISM KARTI (Tüm Formu Çevreleyen Ana Cam Gövde)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xE6141926),
                                    Color(0xD90E121D),
                                ),
                            ),
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0x66F59E0B),
                                    Color(0x26FFFFFF),
                                    Color(0x3322283A),
                                ),
                            ),
                            RoundedCornerShape(24.dp),
                        )
                        .padding(if (screenWidthDp >= 600) 32.dp else 20.dp),
                ) {
                    Column {
                        // 1. Başlık & Marka Bölümü
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GecePalette.CardSurface)
                                    .border(
                                        1.5.dp,
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFFFFDF70),
                                                GecePalette.Accent,
                                            ),
                                        ),
                                        RoundedCornerShape(14.dp),
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.ic_opentv_logo),
                                    contentDescription = "GECE İPTV",
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }

                            Spacer(Modifier.width(14.dp))

                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "GECE",
                                        color = GecePalette.Accent,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "İPTV",
                                        color = GecePalette.TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(GecePalette.Accent.copy(alpha = 0.2f))
                                            .border(1.dp, GecePalette.Accent.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            text = "SAĞLAYICI MERKEZİ",
                                            color = GecePalette.Accent,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                        )
                                    }
                                }

                                Text(
                                    text = if (existing != null) "Sağlayıcı Ayarlarını Düzenle" else "Yeni IPTV Sağlayıcısı Ekle",
                                    color = GecePalette.TextPrimary,
                                    fontSize = if (screenWidthDp >= 600) 22.sp else 19.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "Xtream API, M3U çalma listesi veya Stalker Portal hesap bilgilerinizi girerek canlı TV ve VOD arşivinizi anında eşitleyin.",
                            color = GecePalette.TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                        )

                        Spacer(Modifier.height(14.dp))

                        // Özellik Rozetleri (Glass Badges)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            GlassBadge(text = "⚡ 4K / FHD Yüksek Hız")
                            GlassBadge(text = "🎬 Canlı TV & VOD")
                            GlassBadge(text = "🔒 Cihazda Güvenli")
                        }

                        Spacer(Modifier.height(24.dp))

                        // 2. Telefondan QR ile Kolay Kurulum Kartı (Tüm Cihazlar İçin Erişilebilir)
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(GecePalette.Accent.copy(alpha = 0.15f))
                                        .border(1.dp, GecePalette.Accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Filled.QrCodeScanner,
                                        contentDescription = null,
                                        tint = GecePalette.Accent,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Telefondan Kolay Kurulum (QR Kod)",
                                        color = GecePalette.TextPrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                    )
                                    Text(
                                        text = "Uzun sunucu adresi ve şifreleri yazmak yerine kamerayla QR okutarak anında doldurun.",
                                        color = GecePalette.TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp,
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { usePhone = true },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = GecePalette.CardElevated,
                                        contentColor = GecePalette.Accent,
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, GecePalette.Accent.copy(alpha = 0.45f)),
                                    modifier = Modifier.height(36.dp),
                                ) {
                                    Text("QR'ı Aç", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        // 3. Sağlayıcı Türü Seçimi (Segmented Glass Cards)
                        Text(
                            text = "Sağlayıcı Protokolü Seçin",
                            color = GecePalette.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                        )
                        Spacer(Modifier.height(10.dp))

                        if (isWideScreen) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                ProviderKindGlassCard(
                                    title = "Xtream Codes",
                                    subtitle = "API Girişi",
                                    badge = "ÖNERİLEN",
                                    icon = Icons.Filled.Tv,
                                    selected = kind == SourceKind.XTREAM,
                                    onClick = { kind = SourceKind.XTREAM },
                                    modifier = Modifier.weight(1f),
                                )
                                ProviderKindGlassCard(
                                    title = "M3U Listesi",
                                    subtitle = "URL / Bağlantı",
                                    badge = "M3U_PLUS",
                                    icon = Icons.Filled.VideoLibrary,
                                    selected = kind == SourceKind.M3U,
                                    onClick = { kind = SourceKind.M3U },
                                    modifier = Modifier.weight(1f),
                                )
                                ProviderKindGlassCard(
                                    title = "Stalker Portal",
                                    subtitle = "MAG / MAC",
                                    badge = "PORTAL",
                                    icon = Icons.Filled.Tv,
                                    selected = kind == SourceKind.STALKER,
                                    onClick = { kind = SourceKind.STALKER },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                ProviderKindGlassCard(
                                    title = "Xtream Codes API",
                                    subtitle = "Sunucu, kullanıcı adı ve şifre ile tam arşiv",
                                    badge = "ÖNERİLEN • EN HIZLI",
                                    icon = Icons.Filled.Tv,
                                    selected = kind == SourceKind.XTREAM,
                                    onClick = { kind = SourceKind.XTREAM },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                ProviderKindGlassCard(
                                    title = "M3U Çalma Listesi",
                                    subtitle = "M3U / M3U8 veya dosya bağlantısı",
                                    badge = "M3U_PLUS",
                                    icon = Icons.Filled.VideoLibrary,
                                    selected = kind == SourceKind.M3U,
                                    onClick = { kind = SourceKind.M3U },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                ProviderKindGlassCard(
                                    title = "Stalker Portal",
                                    subtitle = "MAG cihazı ve MAC adresi yetkilendirmesi",
                                    badge = "MAG / PORTAL",
                                    icon = Icons.Filled.Tv,
                                    selected = kind == SourceKind.STALKER,
                                    onClick = { kind = SourceKind.STALKER },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        // 4. Form Giriş Alanları
                        // Sağlayıcı Adı (İsteğe Bağlı)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text(stringResource(R.string.epg_name_optional)) },
                            placeholder = { Text("Örn: Ana Sağlayıcı, VIP Spor vb.") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = glassTextFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(14.dp))

                        // Sunucu / URL Alanı
                        OutlinedTextField(
                            value = url,
                            onValueChange = onUrlChange,
                            label = {
                                Text(
                                    when (kind) {
                                        SourceKind.XTREAM -> stringResource(R.string.onboarding_server_address)
                                        SourceKind.M3U -> stringResource(R.string.onboarding_playlist_url)
                                        SourceKind.STALKER -> stringResource(R.string.onboarding_portal_url)
                                    },
                                )
                            },
                            placeholder = {
                                Text(
                                    when (kind) {
                                        SourceKind.XTREAM -> "http://sunucu-adresi.com:8080"
                                        SourceKind.M3U -> "http://sunucu-adresi.com/get.php?..."
                                        SourceKind.STALKER -> "http://portal-adresi.com/c/"
                                    },
                                )
                            },
                            supportingText = {
                                Text(
                                    when (kind) {
                                        SourceKind.XTREAM -> stringResource(R.string.onboarding_server_help)
                                        SourceKind.M3U -> stringResource(R.string.onboarding_playlist_help)
                                        SourceKind.STALKER -> stringResource(R.string.onboarding_portal_help)
                                    },
                                    color = GecePalette.TextSecondary,
                                )
                            },
                            trailingIcon = {
                                PasteButton { onUrlChange(it) }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            shape = RoundedCornerShape(12.dp),
                            colors = glassTextFieldColors,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // XTREAM ÖZEL ALANLARI: Kullanıcı Adı & Şifre
                        if (kind == SourceKind.XTREAM) {
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text(stringResource(R.string.recset_field_username)) },
                                placeholder = { Text("Kullanıcı adınız") },
                                trailingIcon = { PasteButton { username = it } },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = glassTextFieldColors,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(14.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text(stringResource(R.string.recset_field_password)) },
                                placeholder = { Text("Şifreniz") },
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = if (showPassword) "Şifreyi Gizle" else "Şifreyi Göster",
                                                tint = if (showPassword) GecePalette.Accent else GecePalette.TextSecondary,
                                                modifier = Modifier.size(20.dp),
                                            )
                                        }
                                        PasteButton { password = it }
                                    }
                                },
                                singleLine = true,
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                shape = RoundedCornerShape(12.dp),
                                colors = glassTextFieldColors,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                        // STALKER ÖZEL ALANLARI: MAC Adresi & MAG Kimlikleri
                        if (kind == SourceKind.STALKER) {
                            Spacer(Modifier.height(14.dp))
                            OutlinedTextField(
                                value = mac,
                                onValueChange = { mac = it.trim() },
                                label = { Text(stringResource(R.string.onboarding_mac_address)) },
                                placeholder = { Text("00:1A:79:XX:XX:XX") },
                                supportingText = {
                                    Text(
                                        stringResource(R.string.onboarding_mac_help),
                                        color = GecePalette.TextSecondary,
                                    )
                                },
                                trailingIcon = {
                                    PasteButton { input ->
                                        mac = extractCleanMac(input).ifBlank { input.trim() }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = glassTextFieldColors,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(10.dp))

                            // MAG Gelişmiş Kimlikleri (Açılır Kapanır Cam Kart)
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Column(Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            stringResource(R.string.stalker_mag_identities_title),
                                            color = GecePalette.TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                        )
                                        TextButton(onClick = { showMagIdentities = !showMagIdentities }) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    if (showMagIdentities) stringResource(R.string.onboarding_hide_advanced)
                                                    else stringResource(R.string.onboarding_advanced),
                                                    color = GecePalette.Accent,
                                                    fontSize = 12.sp,
                                                )
                                                Icon(
                                                    if (showMagIdentities) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                                    contentDescription = null,
                                                    tint = GecePalette.Accent,
                                                    modifier = Modifier.size(16.dp),
                                                )
                                            }
                                        }
                                    }

                                    if (showMagIdentities) {
                                        Spacer(Modifier.height(6.dp))
                                        Text(
                                            stringResource(R.string.stalker_mag_identities_desc),
                                            color = GecePalette.TextSecondary,
                                            fontSize = 11.5.sp,
                                            lineHeight = 15.sp,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        OutlinedTextField(
                                            value = sn,
                                            onValueChange = { sn = it.trim() },
                                            label = { Text(stringResource(R.string.stalker_field_sn)) },
                                            placeholder = { Text("Örn: 8E3CB62D32AD7...") },
                                            trailingIcon = { PasteButton { sn = it.trim() } },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = glassTextFieldColors,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = deviceId1,
                                            onValueChange = { deviceId1 = it.trim() },
                                            label = { Text(stringResource(R.string.stalker_field_device_id1)) },
                                            placeholder = { Text("Örn: 675E33773C6F0B96...") },
                                            trailingIcon = { PasteButton { deviceId1 = it.trim() } },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = glassTextFieldColors,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = deviceId2,
                                            onValueChange = { deviceId2 = it.trim() },
                                            label = { Text(stringResource(R.string.stalker_field_device_id2)) },
                                            placeholder = { Text("Örn: 4A9A4970252EA3BD...") },
                                            trailingIcon = { PasteButton { deviceId2 = it.trim() } },
                                            singleLine = true,
                                            shape = RoundedCornerShape(10.dp),
                                            colors = glassTextFieldColors,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        // Gelişmiş Ayarlar (EPG & User-Agent) Butonu
                        OutlinedButton(
                            onClick = { showAdvanced = !showAdvanced },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, GecePalette.CardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0x33141926),
                                contentColor = GecePalette.TextSecondary,
                            ),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    if (showAdvanced) stringResource(R.string.onboarding_hide_advanced)
                                    else "Gelişmiş Seçenekler (EPG & User-Agent)",
                                    fontSize = 12.5.sp,
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    if (showAdvanced) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }

                        if (showAdvanced) {
                            Spacer(Modifier.height(12.dp))
                            GlassCard(modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(14.dp)) {
                                    OutlinedTextField(
                                        value = epgUrl,
                                        onValueChange = { epgUrl = it },
                                        label = { Text(stringResource(R.string.onboarding_guide_url_optional)) },
                                        placeholder = { Text("http://epg-adresi.xml.gz") },
                                        supportingText = {
                                            Text(
                                                stringResource(R.string.onboarding_guide_blank_help),
                                                color = GecePalette.TextSecondary,
                                            )
                                        },
                                        trailingIcon = { PasteButton { epgUrl = it } },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = glassTextFieldColors,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = userAgent,
                                        onValueChange = { userAgent = it },
                                        label = { Text("Özel User-Agent") },
                                        supportingText = {
                                            Text(
                                                stringResource(R.string.onboarding_user_agent_help),
                                                color = GecePalette.TextSecondary,
                                            )
                                        },
                                        trailingIcon = { PasteButton { userAgent = it } },
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = glassTextFieldColors,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Test ve Eşitleme Durum Bildirimleri (Glass Banners)
                        ui.testResult?.let { message ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x2810B981))
                                    .border(1.dp, Color(0xFF10B981), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = message,
                                        color = Color(0xFFE2E8F0),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        ui.testError?.let { message ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x28EF4444))
                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = message,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        ui.syncMessage?.let { message ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(GecePalette.AccentSubtle)
                                    .border(1.dp, GecePalette.Accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(14.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = GecePalette.Accent,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = message,
                                        color = GecePalette.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // Yasal Sorumluluk Reddi Kartı
                        LegalDisclaimerAcceptanceCard(
                            accepted = disclaimerAccepted,
                            onAcceptedChange = { settings.setDisclaimerAccepted(it) },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(20.dp))

                        // 5. Birincil Aksiyon Butonları (Test Et & Kaydet/Başlat)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Bağlantıyı Test Et Butonu
                            OutlinedButton(
                                onClick = { viewModel.test(draft()) },
                                enabled = canSubmit && !ui.testing && !ui.syncing,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color(0x331A1F2E),
                                    contentColor = GecePalette.Accent,
                                    disabledContainerColor = Color(0x1A1A1F2E),
                                    disabledContentColor = GecePalette.TextSecondary.copy(0.4f),
                                ),
                                border = BorderStroke(
                                    1.5.dp,
                                    if (canSubmit && !ui.testing) GecePalette.Accent.copy(0.7f) else GecePalette.CardBorder,
                                ),
                            ) {
                                if (ui.testing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = GecePalette.Accent,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Test Ediliyor…", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.recset_test_connection), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Kaydet ve Yayınları Başlat Butonu (Altın Vurgu ve Simsiyah Yüksek Kontrast Metin)
                            Button(
                                onClick = {
                                    if (!disclaimerAccepted) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.legal_disclaimer_must_accept),
                                            Toast.LENGTH_LONG,
                                        ).show()
                                        showLegalModal = true
                                        return@Button
                                    }
                                    viewModel.saveAndSync(draft()) { ok -> if (ok) onFinished() }
                                },
                                enabled = canSubmit && !ui.testing && !ui.syncing,
                                modifier = Modifier
                                    .weight(1.35f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GecePalette.Accent,
                                    contentColor = GecePalette.OnAccent,
                                    disabledContainerColor = GecePalette.CardElevated,
                                    disabledContentColor = GecePalette.TextSecondary.copy(0.4f),
                                ),
                            ) {
                                if (ui.syncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = GecePalette.OnAccent,
                                        strokeWidth = 2.dp,
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.onboarding_working), fontSize = 13.5.sp, fontWeight = FontWeight.Black)
                                } else {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.onboarding_save_load),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp,
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        // Güvenlik & Gizlilik Notu
                        Text(
                            text = stringResource(R.string.onboarding_privacy_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = GecePalette.TextSecondary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

    if (showLegalModal) {
        LegalDisclaimerModal(
            onAccept = {
                settings.setDisclaimerAccepted(true)
                showLegalModal = false
            },
            onDismiss = { showLegalModal = false },
        )
    }
}

/**
 * Yeniden kullanılabilir Glassmorphism Kart Taşıyıcısı
 */
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xB3181E2E),
                        Color(0x99111522),
                    ),
                ),
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color(0x40F59E0B),
                        Color(0x1AFFFFFF),
                        Color(0x22283A),
                    ),
                ),
                RoundedCornerShape(16.dp),
            ),
    ) {
        content()
    }
}

/**
 * Sağlayıcı Protokol Seçim Kartı (Xtream, M3U, Stalker)
 */
@Composable
private fun ProviderKindGlassCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val cardBorder = when {
        isFocused -> GecePalette.Accent
        selected -> GecePalette.Accent
        else -> GecePalette.CardBorder
    }

    val cardBackground = when {
        isFocused -> Brush.verticalGradient(listOf(Color(0x4DF59E0B), Color(0x28F59E0B), Color(0x33141926)))
        selected -> Brush.verticalGradient(listOf(Color(0x3DF59E0B), Color(0x1AF59E0B), Color(0x26141926)))
        else -> Brush.verticalGradient(listOf(Color(0x66141926), Color(0x4D0E121D)))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(
                if (selected || isFocused) 2.dp else 1.dp,
                cardBorder,
                RoundedCornerShape(14.dp),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .focusable(interactionSource = interactionSource)
            .padding(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) GecePalette.Accent else GecePalette.CardElevated),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) GecePalette.OnAccent else GecePalette.Accent,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (selected) GecePalette.Accent.copy(alpha = 0.2f)
                            else Color(0x2622283A),
                        )
                        .border(
                            1.dp,
                            if (selected) GecePalette.Accent.copy(alpha = 0.6f)
                            else Color(0x33FFFFFF),
                            RoundedCornerShape(4.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = badge,
                        color = if (selected) GecePalette.Accent else GecePalette.TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = title,
                color = GecePalette.TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = subtitle,
                color = GecePalette.TextSecondary,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Üst Başlık Altındaki Cam Rozetler
 */
@Composable
private fun GlassBadge(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33141926))
            .border(1.dp, Color(0x33F59E0B), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = GecePalette.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

/**
 * Hızlı Pano Yapıştırma Butonu (Glass Format)
 */
@Composable
private fun PasteButton(onPaste: (String) -> Unit) {
    val context = LocalContext.current
    IconButton(
        onClick = {
            val pasted = readClipboardText(context)
            if (pasted != null) onPaste(pasted)
            else Toast.makeText(context, context.getString(R.string.kbd_clipboard_empty), Toast.LENGTH_SHORT).show()
        },
    ) {
        Icon(
            imageVector = Icons.Filled.ContentPaste,
            contentDescription = stringResource(R.string.kbd_paste),
            tint = GecePalette.Accent,
            modifier = Modifier.size(18.dp),
        )
    }
}
