/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.opentv.core.AdminCloudClient
import app.opentv.core.LicenseManager
import app.opentv.core.ServiceLocator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Secret In-App Admin Console (Gizli Yönetici Paneli).
 * Accessible exclusively by submitting the master code 'egesmart2026' in the License Activation field.
 */
@Composable
fun AdminPanelScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val graph = remember { ServiceLocator.get(context) }
    val adminClient = graph.adminCloudClient
    val licenseManager = graph.licenseManager
    val scope = rememberCoroutineScope()

    val localStatus by licenseManager.licenseStatus.collectAsState()
    val localMac = licenseManager.formattedMac
    val localKey = licenseManager.deviceKey

    var devices by remember { mutableStateOf<List<AdminCloudClient.DeviceAdminRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var filterTab by remember { mutableStateOf("all") } // all, premium, trial, expired

    // License Code Generator state
    var genMac by remember { mutableStateOf("") }
    var genKey by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }

    fun refreshDevices() {
        scope.launch {
            isLoading = true
            devices = adminClient.fetchAllDevices()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshDevices()
    }

    BackHandler { onBack() }

    val filteredDevices = remember(devices, searchQuery, filterTab) {
        devices.filter { dev ->
            val matchesSearch = searchQuery.isBlank() ||
                dev.mac.contains(searchQuery, ignoreCase = true) ||
                dev.deviceKey.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterTab) {
                "premium" -> dev.licenseStatus == "active" || dev.planType == "lifetime" || dev.planType == "1_year"
                "trial" -> dev.licenseStatus == "trial"
                "expired" -> dev.licenseStatus == "expired"
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    val totalCount = devices.size
    val premiumCount = devices.count { it.licenseStatus == "active" || it.planType == "lifetime" || it.planType == "1_year" }
    val trialCount = devices.count { it.licenseStatus == "trial" }
    val expiredCount = devices.count { it.licenseStatus == "expired" }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        app.opentv.ui.theme.GecePalette.CanvasBg,
                        Color(0xFF040508),
                    ),
                    radius = 1800f,
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AdminIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                    )
                    Spacer(Modifier.width(14.dp))
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(app.opentv.ui.theme.GecePalette.AccentSubtle)
                            .border(1.5.dp, app.opentv.ui.theme.GecePalette.Accent, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = app.opentv.ui.theme.GecePalette.Accent,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "GECE İPTV PLAYER • YÖNETİCİ PANELİ",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Spacer(Modifier.width(10.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Text("GİZLİ KONSOL", color = Color(0xFF4ADE80), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            "Bulut Cihaz Takip, Lisanslama & Kullanıcı Yönetim Merkezi",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                        )
                    }

                    // Refresh Button
                    AdminActionButton(
                        icon = Icons.Filled.Refresh,
                        label = if (isLoading) "Yenileniyor…" else "Bulutu Yenile",
                        isLoading = isLoading,
                        onClick = { refreshDevices() },
                    )
                }
            }

            // Overview Stats (İstatistik Kartları)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    AdminStatCard(
                        title = "TOPLAM CİHAZ",
                        value = totalCount.toString(),
                        accentColor = app.opentv.ui.theme.GecePalette.Accent,
                        modifier = Modifier.weight(1f),
                    )
                    AdminStatCard(
                        title = "AKTİF PREMİUM",
                        value = premiumCount.toString(),
                        accentColor = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f),
                    )
                    AdminStatCard(
                        title = "DENEME SÜRÜMÜ",
                        value = trialCount.toString(),
                        accentColor = Color(0xFFF59E0B),
                        modifier = Modifier.weight(1f),
                    )
                    AdminStatCard(
                        title = "SÜRESİ DOLANLAR",
                        value = expiredCount.toString(),
                        accentColor = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Current Local TV Device Card (Bu Cihazın Kontrolü)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = app.opentv.ui.theme.GecePalette.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, app.opentv.ui.theme.GecePalette.CardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(app.opentv.ui.theme.GecePalette.CardElevated),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Filled.Tv, contentDescription = null, tint = app.opentv.ui.theme.GecePalette.Accent, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "ŞU AN KULLANDIĞINIZ TELEVİZYON",
                                style = MaterialTheme.typography.labelSmall,
                                color = app.opentv.ui.theme.GecePalette.Accent,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "MAC: $localMac  •  PIN: $localKey",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                            )
                            val statusLabel = when (localStatus) {
                                is LicenseManager.LicenseStatus.Premium -> "Ömür Boyu VIP Aktif"
                                is LicenseManager.LicenseStatus.Trial -> "${(localStatus as LicenseManager.LicenseStatus.Trial).daysRemaining} Gün Deneme"
                                is LicenseManager.LicenseStatus.Expired -> "Süresi Dolmuş"
                            }
                            Text(
                                "Mevcut Durum: $statusLabel",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (localStatus is LicenseManager.LicenseStatus.Premium) Color(0xFF4ADE80) else Color(0xFFF59E0B),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }

                        // Local Fast VIP Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AdminActionButton(
                                icon = Icons.Filled.WorkspacePremium,
                                label = "Bu Cihazı VIP Yap",
                                accentColor = Color(0xFF22C55E),
                                onClick = {
                                    licenseManager.setPremium(isLifetime = true)
                                    scope.launch {
                                        adminClient.setLifetimeVip(localMac)
                                        Toast.makeText(context, "Bu cihaz ömür boyu VIP yapıldı!", Toast.LENGTH_SHORT).show()
                                        refreshDevices()
                                    }
                                },
                            )
                            AdminActionButton(
                                icon = Icons.Filled.Refresh,
                                label = "Sıfırla / Deneme",
                                onClick = {
                                    licenseManager.resetTrial()
                                    scope.launch {
                                        adminClient.resetTrial(localMac)
                                        Toast.makeText(context, "Bu cihaz deneme moduna alındı", Toast.LENGTH_SHORT).show()
                                        refreshDevices()
                                    }
                                },
                            )
                        }
                    }
                }
            }

            // Fast License Code Generator Box (Müşteriye Kod Üretme Kutusu)
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = app.opentv.ui.theme.GecePalette.CardSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VpnKey, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "MÜŞTERİ LİSANS KODU ÜRETİCİ & AKTİVATÖR",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                        }
                        Text(
                            "Müşterinin e-postadan (geceiptv@gmail.com) ilettiği MAC adresi ve Cihaz Kodunu buraya girip tek tıkla lisans kodunu oluşturun:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = genMac,
                                onValueChange = { genMac = it.uppercase() },
                                label = { Text("Müşteri MAC (örn: AA:BB:CC:...)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1.3f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                ),
                            )

                            OutlinedTextField(
                                value = genKey,
                                onValueChange = { genKey = it },
                                label = { Text("Cihaz PIN (6 hane)", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(0.9f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontFamily = FontFamily.Monospace),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                ),
                            )

                            AdminActionButton(
                                icon = Icons.Filled.Key,
                                label = "KOD ÜRET",
                                accentColor = Color(0xFFF59E0B),
                                onClick = {
                                    val cleanMac = genMac.trim()
                                    val cleanKey = genKey.trim()
                                    if (cleanMac.length >= 12 && cleanKey.length == 6) {
                                        generatedCode = LicenseManager.generateDeviceLicenseCode(cleanMac, cleanKey)
                                    } else {
                                        Toast.makeText(context, "Lütfen geçerli MAC ve 6 haneli PIN girin", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            )
                        }

                        val code = generatedCode
                        if (code != null) {
                            Spacer(Modifier.height(14.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, Color(0xFF22C55E), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Aktivasyon Kodu:", color = Color(0xFF94A3B8), fontSize = 13.sp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = code,
                                    color = Color(0xFF4ADE80),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace,
                                )
                                Spacer(Modifier.weight(1f))
                                AdminActionButton(
                                    icon = Icons.Filled.ContentCopy,
                                    label = "Kopyala",
                                    onClick = {
                                        clipboard.setText(AnnotatedString(code))
                                        Toast.makeText(context, "Lisans kodu kopyalandı!", Toast.LENGTH_SHORT).show()
                                    },
                                )
                                Spacer(Modifier.width(8.dp))
                                AdminActionButton(
                                    icon = Icons.Filled.WorkspacePremium,
                                    label = "Bulutta da VIP Yap",
                                    accentColor = Color(0xFF22C55E),
                                    onClick = {
                                        scope.launch {
                                            val ok = adminClient.setLifetimeVip(genMac.trim())
                                            if (ok) {
                                                Toast.makeText(context, "Cihaz bulutta VIP yapıldı!", Toast.LENGTH_SHORT).show()
                                                refreshDevices()
                                            } else {
                                                Toast.makeText(context, "Bulut kaydı güncellenemedi", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            // User Search & Filter Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "KAYITLI CİHAZLAR & KULLANICILAR (${filteredDevices.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    Spacer(Modifier.weight(1f))

                    // Filter tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AdminFilterChip("Tümü", filterTab == "all") { filterTab = "all" }
                        AdminFilterChip("VIP", filterTab == "premium") { filterTab = "premium" }
                        AdminFilterChip("Deneme", filterTab == "trial") { filterTab = "trial" }
                        AdminFilterChip("Bitenler", filterTab == "expired") { filterTab = "expired" }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("MAC Adresi veya Cihaz Kodu ile Ara…", color = Color.White.copy(alpha = 0.5f)) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF38BDF8)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = app.opentv.ui.theme.GecePalette.Accent,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                    ),
                )
            }

            // Devices List
            if (filteredDevices.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isLoading) "Cihazlar Supabase bulutundan yükleniyor…" else "Arama kriterine uygun cihaz bulunamadı.",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp,
                        )
                    }
                }
            } else {
                items(filteredDevices, key = { it.mac }) { record ->
                    AdminDeviceRowCard(
                        record = record,
                        onSetLifetime = {
                            scope.launch {
                                val ok = adminClient.setLifetimeVip(record.mac)
                                if (ok) {
                                    Toast.makeText(context, "${record.mac} Ömür Boyu VIP yapıldı", Toast.LENGTH_SHORT).show()
                                    refreshDevices()
                                }
                            }
                        },
                        onSetOneYear = {
                            scope.launch {
                                val ok = adminClient.setOneYearVip(record.mac)
                                if (ok) {
                                    Toast.makeText(context, "${record.mac} 1 Yıl VIP yapıldı", Toast.LENGTH_SHORT).show()
                                    refreshDevices()
                                }
                            }
                        },
                        onResetTrial = {
                            scope.launch {
                                val ok = adminClient.resetTrial(record.mac)
                                if (ok) {
                                    Toast.makeText(context, "${record.mac} deneme süresi sıfırlandı", Toast.LENGTH_SHORT).show()
                                    refreshDevices()
                                }
                            }
                        },
                        onExpire = {
                            scope.launch {
                                val ok = adminClient.expireDevice(record.mac)
                                if (ok) {
                                    Toast.makeText(context, "${record.mac} lisansı iptal edildi", Toast.LENGTH_SHORT).show()
                                    refreshDevices()
                                }
                            }
                        },
                    )
                }
            }
        }
    }
}

/**
 * Single Device Row Card in Admin Panel
 */
@Composable
private fun AdminDeviceRowCard(
    record: AdminCloudClient.DeviceAdminRecord,
    onSetLifetime: () -> Unit,
    onSetOneYear: () -> Unit,
    onResetTrial: () -> Unit,
    onExpire: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    val isVip = record.licenseStatus == "active" || record.planType == "lifetime" || record.planType == "1_year"
    val isExpired = record.licenseStatus == "expired"

    val statusBadgeColor = when {
        isVip -> Color(0xFF22C55E)
        isExpired -> Color(0xFFEF4444)
        else -> Color(0xFFF59E0B)
    }

    val statusBadgeText = when {
        record.planType == "lifetime" -> "ÖMÜR BOYU VIP"
        record.planType == "1_year" -> "1 YILLIK VIP"
        isVip -> "AKTİF PREMİUM"
        isExpired -> "SÜRESİ DOLMUŞ"
        else -> "7 GÜNLÜK DENEME"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isFocused) app.opentv.ui.theme.GecePalette.CardElevated else app.opentv.ui.theme.GecePalette.CardSurface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isFocused) app.opentv.ui.theme.GecePalette.Accent else app.opentv.ui.theme.GecePalette.CardBorder,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Device Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(statusBadgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (isVip) Icons.Filled.CheckCircle else if (isExpired) Icons.Filled.Warning else Icons.Filled.HourglassBottom,
                        contentDescription = null,
                        tint = statusBadgeColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))

                // MAC and Key
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.mac,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "PIN: ${record.deviceKey}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        text = "${record.label} (v${record.appVersion})  •  Kayıt: ${formatDate(record.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBadgeColor.copy(alpha = 0.2f))
                        .border(1.dp, statusBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text(statusBadgeText, color = statusBadgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AdminRowButton("👑 Ömür Boyu VIP", Color(0xFF22C55E), onClick = onSetLifetime)
                AdminRowButton("📅 +1 Yıl Ekle", Color(0xFF38BDF8), onClick = onSetOneYear)
                AdminRowButton("⏳ Denemeyi Sıfırla", Color(0xFFF59E0B), onClick = onResetTrial)
                AdminRowButton("🚫 İptal Et", Color(0xFFEF4444), onClick = onExpire)
            }
        }
    }
}

@Composable
private fun AdminRowButton(
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) accentColor.copy(alpha = 0.35f) else Color(0xFF1E293B).copy(alpha = 0.6f))
            .border(
                1.dp,
                if (isFocused) accentColor else Color.White.copy(alpha = 0.12f),
                RoundedCornerShape(6.dp),
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            color = if (isFocused) Color.White else accentColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun AdminStatCard(
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF0F172A),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accentColor, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun AdminFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) Color(0xFF0284C7) else if (isFocused) Color(0xFF1E293B) else Color.Transparent)
            .border(1.dp, if (isSelected || isFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun AdminActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color = Color(0xFF38BDF8),
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) accentColor.copy(alpha = 0.35f) else Color(0xFF1E293B))
            .border(1.dp, if (isFocused) accentColor else Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(enabled = !isLoading, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = accentColor)
            } else {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isFocused) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFF1E293B))
            .border(1.dp, if (isFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
    }
}

private fun formatDate(isoStr: String): String {
    if (isoStr.isBlank()) return "-"
    return runCatching {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = parser.parse(isoStr.take(19)) ?: return "-"
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        formatter.format(date)
    }.getOrDefault(isoStr.take(10))
}
