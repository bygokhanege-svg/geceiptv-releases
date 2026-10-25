/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.remote

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import app.opentv.core.TvCastHelper
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import app.opentv.R
import app.opentv.core.ServiceLocator
import app.opentv.pairing.QrCodes
import app.opentv.ui.theme.GecePalette
import androidx.core.content.ContextCompat
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import kotlinx.coroutines.channels.Channel
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

private const val PREFS_NAME = "opentv_remote"
private const val KEY_TV_URL = "last_tv_url"
private const val KEY_TV_TOKEN = "last_tv_token"

/**
 * Mobile and Tablet Remote Control Screen.
 * Allows phones and tablets to connect to the TV via QR code scanning or direct IP,
 * turning the device into an interactive smart TV remote control.
 */
@Composable
fun RemoteScreen() {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val httpClient = remember { ServiceLocator.get(context).httpClient }

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var tvUrl by remember { mutableStateOf(prefs.getString(KEY_TV_URL, "").orEmpty()) }
    var tvToken by remember { mutableStateOf(prefs.getString(KEY_TV_TOKEN, "").orEmpty()) }

    var isConnected by remember { mutableStateOf(false) }
    var isConnecting by remember { mutableStateOf(false) }
    var isSearchingLan by remember { mutableStateOf(false) }
    var showQrScanner by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var showNumpad by remember { mutableStateOf(false) }
    var searchInput by remember { mutableStateOf("") }
    var isMouseMode by remember { mutableStateOf(false) }
    var isTvSearchActive by remember { mutableStateOf(false) }
    var isSendingCmd by remember { mutableStateOf(false) }
    var lastCmdStatus by remember { mutableStateOf<String?>(null) }

    val fastClient = remember {
        httpClient.newBuilder()
            .connectTimeout(600, TimeUnit.MILLISECONDS)
            .readTimeout(600, TimeUnit.MILLISECONDS)
            .build()
    }

    suspend fun pingTv(targetUrl: String, token: String): Boolean = withContext(Dispatchers.IO) {
        if (targetUrl.isBlank()) return@withContext false
        val endpoint = if (targetUrl.endsWith("/")) "${targetUrl}ping?t=$token" else "$targetUrl/ping?t=$token"
        try {
            val req = Request.Builder().url(endpoint).get().build()
            fastClient.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val str = res.body?.string().orEmpty()
                    isTvSearchActive = str.contains("\"isSearch\":true")
                    str.contains("\"tv\":true")
                } else false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun discoverTvOnLan(token: String): String? = withContext(Dispatchers.IO) {
        val prefix = runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            var foundPrefix: String? = null
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                for (addr in intf.inetAddresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: continue
                        if (host.startsWith("192.168.") || host.startsWith("10.") || host.startsWith("172.")) {
                            val lastDot = host.lastIndexOf('.')
                            if (lastDot != -1) {
                                foundPrefix = host.substring(0, lastDot + 1)
                                break
                            }
                        }
                    }
                }
                if (foundPrefix != null) break
            }
            foundPrefix
        }.getOrNull() ?: return@withContext null

        val scanClient = httpClient.newBuilder()
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .build()

        for (batch in (1..254).chunked(25)) {
            val jobs = batch.map { i ->
                async(Dispatchers.IO) {
                    val candidate = "http://${prefix}${i}:8765"
                    val endpoint = "$candidate/ping?t=$token"
                    try {
                        val req = Request.Builder().url(endpoint).get().build()
                        scanClient.newCall(req).execute().use { res ->
                            if (res.isSuccessful) {
                                val body = res.body?.string().orEmpty()
                                if (body.contains("\"tv\":true") && (token.isBlank() || body.contains("\"validToken\":true"))) {
                                    candidate
                                } else null
                            } else null
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            val match = jobs.awaitAll().firstOrNull { it != null }
            if (match != null) return@withContext match
        }
        null
    }

    // Auto-connect loop: If previously paired, automatically establish LAN connection whenever apps are open
    LaunchedEffect(tvToken) {
        if (tvUrl.isBlank() && tvToken.isBlank()) return@LaunchedEffect
        while (isActive) {
            if (!isConnected) {
                isConnecting = true
                var alive = false
                if (tvUrl.isNotBlank()) {
                    alive = pingTv(tvUrl, tvToken)
                }
                if (!alive && tvToken.isNotBlank()) {
                    isSearchingLan = true
                    val found = discoverTvOnLan(tvToken)
                    isSearchingLan = false
                    if (found != null) {
                        tvUrl = found
                        prefs.edit().putString(KEY_TV_URL, found).apply()
                        alive = true
                    }
                }
                isConnected = alive
                isConnecting = false
            } else {
                val alive = pingTv(tvUrl, tvToken)
                if (!alive) {
                    isConnected = false
                }
            }
            delay(if (isConnected) 15_000L else 5_000L)
        }
    }

    val tvHost = remember(tvUrl) {
        val raw = tvUrl.removePrefix("http://").removePrefix("https://")
        raw.substringBefore(':').substringBefore('/').trim()
    }
    var targetAddress by remember { mutableStateOf<InetAddress?>(null) }

    LaunchedEffect(tvHost) {
        if (tvHost.isNotBlank()) {
            withContext(Dispatchers.IO) {
                targetAddress = runCatching { InetAddress.getByName(tvHost) }.getOrNull()
            }
        } else {
            targetAddress = null
        }
    }

    val udpSocket = remember { runCatching { DatagramSocket() }.getOrNull() }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { udpSocket?.close() }
        }
    }

    val mouseChannel = remember { Channel<Pair<Float, Float>>(capacity = Channel.CONFLATED) }

    LaunchedEffect(targetAddress) {
        val addr = targetAddress ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            for ((dx, dy) in mouseChannel) {
                try {
                    val buf = ByteBuffer.allocate(9)
                    buf.put(1.toByte()) // type 1: Move
                    buf.putFloat(dx)
                    buf.putFloat(dy)
                    val packet = DatagramPacket(buf.array(), 9, addr, 8766)
                    udpSocket?.send(packet)
                } catch (_: Exception) {}
            }
        }
    }

    fun vibrateDevice() {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (_: Exception) {}
    }

    fun sendCommand(cmd: String, value: String? = null) {
        if (tvUrl.isBlank()) return
        vibrateDevice()
        isSendingCmd = true

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val endpoint = if (tvUrl.endsWith("/")) "${tvUrl}remote?t=$tvToken" else "$tvUrl/remote?t=$tvToken"
                val body = buildString {
                    append("{\"cmd\":\"").append(cmd).append("\"")
                    if (value != null) {
                        append(",\"value\":\"").append(value.replace("\"", "\\\"")).append("\"")
                    }
                    append("}")
                }
                val req = Request.Builder()
                    .url(endpoint)
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(req).execute().use { res ->
                    withContext(Dispatchers.Main) {
                        isSendingCmd = false
                        if (!res.isSuccessful) {
                            lastCmdStatus = "Hata: HTTP ${res.code}"
                        } else {
                            lastCmdStatus = null
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSendingCmd = false
                    lastCmdStatus = "Bağlantı koptu"
                }
            }
        }
    }

    fun sendMouseClick() {
        vibrateDevice()
        coroutineScope.launch(Dispatchers.IO) {
            val addr = targetAddress
            if (addr != null && udpSocket != null) {
                try {
                    val buf = byteArrayOf(2.toByte()) // type 2: Click
                    val packet = DatagramPacket(buf, 1, addr, 8766)
                    udpSocket.send(packet)
                    return@launch
                } catch (_: Exception) {}
            }
            sendCommand("mouse_click")
        }
    }

    fun sendMouseBack() {
        vibrateDevice()
        coroutineScope.launch(Dispatchers.IO) {
            val addr = targetAddress
            if (addr != null && udpSocket != null) {
                try {
                    val buf = byteArrayOf(4.toByte()) // type 4: Back
                    val packet = DatagramPacket(buf, 1, addr, 8766)
                    udpSocket.send(packet)
                    return@launch
                } catch (_: Exception) {}
            }
            sendCommand("back")
        }
    }

    fun toggleMouseMode() {
        val next = !isMouseMode
        isMouseMode = next
        vibrateDevice()
        coroutineScope.launch(Dispatchers.IO) {
            val addr = targetAddress
            if (addr != null && udpSocket != null) {
                try {
                    val type = if (next) 5.toByte() else 6.toByte()
                    val packet = DatagramPacket(byteArrayOf(type), 1, addr, 8766)
                    udpSocket.send(packet)
                } catch (_: Exception) {}
            }
            sendCommand(if (next) "mouse_activate" else "mouse_hide")
        }
    }

    fun parseAndConnect(rawQr: String) {
        val trimmed = rawQr.trim()
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()

        var hostBase = ""
        var token = ""

        if (uri != null && uri.scheme != null && uri.host != null) {
            val portPart = if (uri.port != -1) ":${uri.port}" else ""
            hostBase = "${uri.scheme}://${uri.host}$portPart"
            token = uri.getQueryParameter("t").orEmpty()
        } else {
            // Raw ip:port or ip:port?t=token
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                val qIdx = trimmed.indexOf("?t=")
                if (qIdx != -1) {
                    hostBase = trimmed.substring(0, qIdx).trimEnd('/')
                    token = trimmed.substring(qIdx + 3)
                } else {
                    hostBase = trimmed.trimEnd('/')
                }
            } else {
                val qIdx = trimmed.indexOf("?t=")
                if (qIdx != -1) {
                    hostBase = "http://" + trimmed.substring(0, qIdx).trimEnd('/')
                    token = trimmed.substring(qIdx + 3)
                } else {
                    hostBase = "http://" + trimmed.trimEnd('/')
                }
            }
        }

        if (hostBase.isNotBlank()) {
            tvUrl = hostBase
            tvToken = token
            prefs.edit().putString(KEY_TV_URL, hostBase).putString(KEY_TV_TOKEN, token).apply()
            isConnected = true
            showQrScanner = false
            showManualDialog = false
            vibrateDevice()
            Toast.makeText(context, context.getString(R.string.remote_toast_connected), Toast.LENGTH_SHORT).show()
            // Not: Sağlayıcılar otomatik senkronize edilmez. Sadece kullanıcı 'Sağlayıcıları Eşitle' butonuna tıklayınca senkronize edilir.
        } else {
            Toast.makeText(context, context.getString(R.string.remote_toast_invalid_qr), Toast.LENGTH_SHORT).show()
        }
    }

    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GecePalette.CanvasBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(GecePalette.Accent, Color(0xFFD97706))
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Tv, contentDescription = null, tint = GecePalette.OnAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            stringResource(R.string.remote_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isConnected -> Color(0xFF10B981)
                                            isSearchingLan || isConnecting -> Color(0xFFF59E0B)
                                            tvToken.isNotBlank() -> Color(0xFFFBBF24)
                                            else -> Color(0xFFEF4444)
                                        }
                                    )
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                when {
                                    isConnected -> {
                                        val displayHost = tvUrl.removePrefix("http://").removePrefix("https://").take(22)
                                        stringResource(R.string.remote_connected, displayHost)
                                    }
                                    isSearchingLan -> stringResource(R.string.remote_scanning)
                                    isConnecting -> stringResource(R.string.remote_connecting_status)
                                    tvToken.isNotBlank() -> stringResource(R.string.remote_waiting_tv)
                                    else -> stringResource(R.string.remote_not_connected)
                                },
                                fontSize = 11.sp,
                                color = when {
                                    isConnected -> Color(0xFF34D399)
                                    isSearchingLan || isConnecting -> Color(0xFFFBBF24)
                                    tvToken.isNotBlank() -> Color(0xFFFCD34D)
                                    else -> Color(0xFFF87171)
                                },
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // QR Scanner Trigger Button
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showQrScanner = true },
                        color = GecePalette.CardSurface,
                        border = BorderStroke(1.dp, GecePalette.CardBorder),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = stringResource(R.string.remote_btn_qr_scan), tint = GecePalette.Accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isConnected) stringResource(R.string.remote_btn_qr_change) else stringResource(R.string.remote_btn_qr_scan), fontSize = 12.sp, color = GecePalette.Accent, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (isConnected) {
                        Spacer(Modifier.width(8.dp))
                        // TV Standby / Power Button
                        IconButton(
                            onClick = { sendCommand("power") },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0x33EF4444))
                                .border(1.dp, Color(0x66EF4444), CircleShape),
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = stringResource(R.string.remote_btn_power), tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            if (!isConnected) {
                // Not Connected State: TV Search & QR Prompt Card
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(16.dp, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, GecePalette.CardBorder),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(GecePalette.AccentSubtle)
                                .border(1.dp, GecePalette.Accent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isConnecting || isSearchingLan) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = GecePalette.Accent,
                                    strokeWidth = 3.dp,
                                )
                            } else {
                                Icon(
                                    if (tvToken.isNotBlank()) Icons.Default.Wifi else Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = GecePalette.Accent,
                                    modifier = Modifier.size(36.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            when {
                                isSearchingLan -> stringResource(R.string.remote_lan_scanning)
                                isConnecting -> stringResource(R.string.remote_connecting)
                                tvToken.isNotBlank() -> stringResource(R.string.remote_waiting_tv)
                                else -> stringResource(R.string.remote_card_title_ready)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            when {
                                tvToken.isNotBlank() -> stringResource(R.string.remote_card_desc_paired)
                                else -> stringResource(R.string.remote_card_desc_unpaired)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF94A3B8),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                        )

                        Spacer(Modifier.height(20.dp))

                        if (tvToken.isNotBlank()) {
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isConnecting = true
                                        var ok = false
                                        if (tvUrl.isNotBlank()) {
                                            ok = pingTv(tvUrl, tvToken)
                                        }
                                        if (!ok && tvToken.isNotBlank()) {
                                            isSearchingLan = true
                                            val found = discoverTvOnLan(tvToken)
                                            isSearchingLan = false
                                            if (found != null) {
                                                tvUrl = found
                                                prefs.edit().putString(KEY_TV_URL, found).apply()
                                                ok = true
                                            }
                                        }
                                        isConnected = ok
                                        isConnecting = false
                                        if (ok) {
                                            Toast.makeText(context, context.getString(R.string.remote_toast_connected), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.remote_toast_not_found), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GecePalette.Accent),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = GecePalette.OnAccent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.remote_reconnect_now), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GecePalette.OnAccent)
                            }

                            Spacer(Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { showQrScanner = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, GecePalette.CardBorder),
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = GecePalette.Accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.remote_scan_different_tv), fontSize = 13.sp, color = GecePalette.TextSecondary)
                            }
                        } else {
                            Button(
                                onClick = { showQrScanner = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GecePalette.Accent),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = GecePalette.OnAccent, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.remote_scan_qr_camera), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GecePalette.OnAccent)
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showManualDialog = true },
                            color = Color.Transparent,
                        ) {
                            Text(
                                stringResource(R.string.remote_manual_ip_link),
                                fontSize = 12.sp,
                                color = GecePalette.Accent,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 10.dp),
                            )
                        }
                    }
                }
            } else {
                // Connected State: Full Interactive Remote Control
                Spacer(Modifier.height(8.dp))

                // Search / Text Input Bar (Send text directly to TV)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, GecePalette.CardBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = GecePalette.TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            placeholder = { Text(stringResource(R.string.remote_send_text_hint), fontSize = 13.sp, color = GecePalette.TextSecondary.copy(alpha = 0.6f)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (searchInput.isNotBlank()) {
                                    sendCommand("search", searchInput.replace("+", " "))
                                    keyboardController?.hide()
                                }
                            }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                if (searchInput.isNotBlank()) {
                                    sendCommand("search", searchInput.replace("+", " "))
                                    keyboardController?.hide()
                                }
                            },
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.remote_send), tint = GecePalette.Accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // TV Shortcuts Row (Canlı TV, Film, Dizi, Anasayfa, Sağlayıcıları Eşitle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TvShortcutChip(
                        icon = Icons.Default.Tv,
                        label = stringResource(R.string.remote_nav_live),
                        color = GecePalette.Accent,
                        onClick = { sendCommand("nav_live") }
                    )
                    TvShortcutChip(
                        icon = Icons.Default.PlayArrow,
                        label = stringResource(R.string.remote_nav_movies),
                        color = GecePalette.Accent,
                        onClick = { sendCommand("nav_movies") }
                    )
                    TvShortcutChip(
                        icon = Icons.Default.Tv,
                        label = stringResource(R.string.remote_nav_shows),
                        color = GecePalette.Accent,
                        onClick = { sendCommand("nav_shows") }
                    )
                    TvShortcutChip(
                        icon = Icons.Default.Home,
                        label = stringResource(R.string.remote_nav_home),
                        color = GecePalette.Accent,
                        onClick = { sendCommand("home") }
                    )
                    TvShortcutChip(
                        icon = Icons.Default.Refresh,
                        label = stringResource(R.string.remote_sync_sources),
                        color = GecePalette.Accent,
                        onClick = {
                            TvCastHelper.syncSourcesToTv(context, coroutineScope, tvUrl, tvToken)
                        }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Quick Navigation & Media Controls Bar (Geri, Oynat/Duraklat, Sessiz, Rehber, Fare, Sayılar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(GecePalette.CardSurface)
                        .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(16.dp))
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RemoteIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        label = stringResource(R.string.remote_btn_back),
                        onClick = { sendCommand("back") },
                    )
                    RemoteIconButton(
                        icon = Icons.Default.PlayArrow,
                        label = stringResource(R.string.remote_btn_play_pause),
                        onClick = { sendCommand("play_pause") },
                    )
                    RemoteIconButton(
                        icon = Icons.AutoMirrored.Filled.VolumeMute,
                        label = stringResource(R.string.remote_btn_mute),
                        onClick = { sendCommand("mute") },
                    )
                    RemoteIconButton(
                        icon = Icons.Default.Menu,
                        label = stringResource(R.string.remote_btn_guide),
                        onClick = { sendCommand("ok") },
                    )
                    RemoteIconButton(
                        customIcon = { MouseIcon(tint = if (isMouseMode) GecePalette.OnAccent else GecePalette.Accent) },
                        label = if (isMouseMode) stringResource(R.string.remote_btn_mouse_on) else stringResource(R.string.remote_btn_mouse),
                        highlighted = isMouseMode,
                        onClick = { toggleMouseMode() },
                    )
                    RemoteIconButton(
                        icon = Icons.Default.Dialpad,
                        label = stringResource(R.string.remote_btn_numpad),
                        highlighted = showNumpad,
                        onClick = { showNumpad = !showNumpad },
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (isMouseMode) {
                    MouseTouchpadAndKeyboardSection(
                        mouseChannel = mouseChannel,
                        onLeftClick = { sendMouseClick() },
                        onRightClick = { sendMouseBack() },
                        onExitMouseMode = { toggleMouseMode() },
                        isTvSearchActive = isTvSearchActive,
                        onSendSearch = { query ->
                            if (query.isNotBlank()) {
                                sendCommand("search", query.replace("+", " "))
                            }
                        },
                    )
                } else {
                    // Ergonomic Controller Row: SOLUNDA SES, AŞŞADA JOYSTICK YUVARLAĞI (SAĞ BAŞ PARMAĞIN ULAŞACAĞI YERDE), SAĞINDA KANAL
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 1. SOLUNDA SES (Volume Pill Rocker)
                        Column(
                            modifier = Modifier
                                .width(62.dp)
                                .clip(RoundedCornerShape(31.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(GecePalette.CardElevated, GecePalette.CardSurface)
                                    )
                                )
                                .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(31.dp))
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = { sendCommand("vol_up") },
                                modifier = Modifier.size(46.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = stringResource(R.string.remote_vol_up), tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Text(stringResource(R.string.remote_vol_title), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = GecePalette.Accent, letterSpacing = 1.sp)
                            IconButton(
                                onClick = { sendCommand("vol_down") },
                                modifier = Modifier.size(46.dp),
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeDown, contentDescription = stringResource(R.string.remote_vol_down), tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }

                        // 2. AŞŞADA JOYSTICK YUVARLAĞI (Center D-Pad Wheel for Natural Right-Hand Thumb Reach)
                        Box(
                            modifier = Modifier
                                .size(208.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(GecePalette.CardElevated, GecePalette.CardSurface)
                                    )
                                )
                                .border(2.dp, GecePalette.Accent, CircleShape)
                                .shadow(16.dp, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            // UP Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp)
                                    .size(70.dp, 56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { sendCommand("up") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = stringResource(R.string.remote_dpad_up), tint = Color.White, modifier = Modifier.size(34.dp))
                            }

                            // DOWN Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 4.dp)
                                    .size(70.dp, 56.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { sendCommand("down") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = stringResource(R.string.remote_dpad_down), tint = Color.White, modifier = Modifier.size(34.dp))
                            }

                            // LEFT Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 4.dp)
                                    .size(56.dp, 70.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { sendCommand("left") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = stringResource(R.string.remote_dpad_left), tint = Color.White, modifier = Modifier.size(34.dp))
                            }

                            // RIGHT Button
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(end = 4.dp)
                                    .size(56.dp, 70.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .clickable { sendCommand("right") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = stringResource(R.string.remote_dpad_right), tint = Color.White, modifier = Modifier.size(34.dp))
                            }

                            // CENTER OK BUTTON (Direct Thumb Action - Gold with High Contrast Black Text!)
                            Box(
                                modifier = Modifier
                                    .size(74.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(GecePalette.Accent, Color(0xFFD97706))
                                        )
                                    )
                                    .border(2.dp, GecePalette.Accent, CircleShape)
                                    .clickable { sendCommand("ok") },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(stringResource(R.string.remote_dpad_ok), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = GecePalette.OnAccent)
                            }
                        }

                        // 3. SAĞINDA KANAL (Channel Pill Rocker)
                        Column(
                            modifier = Modifier
                                .width(62.dp)
                                .clip(RoundedCornerShape(31.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(GecePalette.CardElevated, GecePalette.CardSurface)
                                    )
                                )
                                .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(31.dp))
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            IconButton(
                                onClick = { sendCommand("ch_up") },
                                modifier = Modifier.size(46.dp),
                            ) {
                                Icon(Icons.Default.ArrowUpward, contentDescription = stringResource(R.string.remote_ch_up), tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                            Text(stringResource(R.string.remote_ch_title), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = GecePalette.Accent, letterSpacing = 0.5.sp)
                            IconButton(
                                onClick = { sendCommand("ch_down") },
                                modifier = Modifier.size(46.dp),
                            ) {
                                Icon(Icons.Default.ArrowDownward, contentDescription = stringResource(R.string.remote_ch_down), tint = Color.White, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // Expandable Numeric Keypad (1 - 9, 0)
                AnimatedVisibility(
                    visible = showNumpad,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(GecePalette.CardSurface)
                            .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(20.dp))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(stringResource(R.string.remote_numpad_title), fontSize = 12.sp, color = GecePalette.TextSecondary, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(10.dp))
                        val numbers = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("", "0", ""),
                        )
                        numbers.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                row.forEach { num ->
                                    if (num.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .size(54.dp)
                                                .clip(CircleShape)
                                                .clickable { sendCommand("num", num) },
                                            color = GecePalette.CardElevated,
                                            border = BorderStroke(1.dp, GecePalette.CardBorder),
                                            shape = CircleShape,
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(num, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            }
                                        }
                                    } else {
                                        Spacer(Modifier.size(54.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Fullscreen QR Code Camera Scanner Modal
    if (showQrScanner) {
        CameraScannerDialog(
            onDismiss = { showQrScanner = false },
            onQrCodeDetected = { rawCode ->
                parseAndConnect(rawCode)
            },
        )
    }

    // Manual IP Dialog
    if (showManualDialog) {
        var manualInput by remember { mutableStateOf(tvUrl) }
        Dialog(onDismissRequest = { showManualDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                border = BorderStroke(1.dp, GecePalette.CardBorder),
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(R.string.remote_dialog_manual_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.remote_dialog_manual_desc), fontSize = 12.sp, color = GecePalette.TextSecondary, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    OutlinedTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        placeholder = { Text(stringResource(R.string.remote_dialog_ip_hint), fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = GecePalette.Accent,
                            unfocusedBorderColor = GecePalette.CardBorder,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { showManualDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = GecePalette.CardElevated),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.common_cancel), color = GecePalette.TextSecondary)
                        }
                        Button(
                            onClick = {
                                if (manualInput.isNotBlank()) parseAndConnect(manualInput)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GecePalette.Accent),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(stringResource(R.string.remote_btn_connect), fontWeight = FontWeight.Bold, color = GecePalette.OnAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RemoteIconButton(
    icon: ImageVector? = null,
    customIcon: (@Composable () -> Unit)? = null,
    label: String,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            color = if (highlighted) GecePalette.Accent else GecePalette.CardElevated,
            border = BorderStroke(1.dp, if (highlighted) GecePalette.Accent else GecePalette.CardBorder),
            shape = CircleShape,
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (customIcon != null) {
                    customIcon()
                } else if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = label,
                        tint = if (highlighted) GecePalette.OnAccent else Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            fontSize = 10.sp,
            maxLines = 1,
            color = if (highlighted) GecePalette.Accent else GecePalette.TextSecondary,
        )
    }
}

@Composable
private fun MouseIcon(tint: Color = Color.White, modifier: Modifier = Modifier.size(20.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val r = CornerRadius(w * 0.45f, w * 0.45f)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.15f, h * 0.05f),
            size = Size(w * 0.7f, h * 0.9f),
            cornerRadius = r,
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.2f),
            end = Offset(w * 0.5f, h * 0.42f),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun MouseTouchpadAndKeyboardSection(
    mouseChannel: Channel<Pair<Float, Float>>,
    onLeftClick: () -> Unit,
    onRightClick: () -> Unit,
    onExitMouseMode: () -> Unit,
    isTvSearchActive: Boolean,
    onSendSearch: (String) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    // When TV opens the search screen (e.g. mouse clicked TV search button), automatically focus and pop up phone keyboard
    LaunchedEffect(isTvSearchActive) {
        if (isTvSearchActive) {
            delay(150)
            runCatching {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Mode Header & Switch Back Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GecePalette.Accent)
                )
                Text(
                    stringResource(R.string.remote_mouse_title),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = GecePalette.Accent,
                    letterSpacing = 0.5.sp,
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onExitMouseMode),
                color = GecePalette.AccentSubtle,
                border = BorderStroke(1.dp, GecePalette.Accent),
                shape = RoundedCornerShape(10.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("🎮", fontSize = 12.sp)
                    Text(stringResource(R.string.remote_mouse_return_remote), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Low-Latency Touchpad Surface
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        // 2.2f sensitivity for ergonomic thumb glide across TV resolution
                        val speed = 2.2f
                        mouseChannel.trySend(Pair(dragAmount.x * speed, dragAmount.y * speed))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            onLeftClick()
                        }
                    )
                },
            colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, GecePalette.Accent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(GecePalette.CardElevated, GecePalette.CanvasBg)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                // Subtle central watermark & guidance
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MouseIcon(
                        tint = GecePalette.Accent.copy(alpha = 0.35f),
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        stringResource(R.string.remote_mouse_guide_drag),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        stringResource(R.string.remote_mouse_guide_tap),
                        fontSize = 10.5.sp,
                        color = Color(0xFF475569),
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Left Click (Select) and Right Click (Back) Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Left Click (Primary Select)
            Surface(
                modifier = Modifier
                    .weight(1.2f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onLeftClick),
                color = Color(0xFF2563EB),
                border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.remote_mouse_left_click), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Right Click (Back / Cancel)
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onRightClick),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.remote_mouse_right_click), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFCBD5E1))
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Fast TV Search & Soft Keyboard Section (Beliren Klavye ve Hızlı Arama Alanı)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isTvSearchActive) Color(0xFF0F2338) else Color(0xFF111827)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                if (isTvSearchActive) 1.5.dp else 1.dp,
                if (isTvSearchActive) Color(0xFF38BDF8) else Color(0x22FFFFFF)
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
            ) {
                // Search status bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = if (isTvSearchActive) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (isTvSearchActive) stringResource(R.string.remote_mouse_search_active) else stringResource(R.string.remote_mouse_search_title),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTvSearchActive) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        )
                    }

                    if (isTvSearchActive) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0x330284C7),
                        ) {
                            Text(
                                stringResource(R.string.remote_mouse_search_live_badge),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Text Field with FocusRequester
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            stringResource(R.string.remote_mouse_keyboard_hint),
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                onSendSearch(searchQuery)
                                keyboardController?.hide()
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFF38BDF8),
                    ),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.kbd_clear), tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )

                Spacer(Modifier.height(8.dp))

                // Quick Send Button
                Button(
                    onClick = {
                        if (searchQuery.isNotBlank()) {
                            onSendSearch(searchQuery)
                            keyboardController?.hide()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    enabled = searchQuery.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.remote_mouse_search_send), fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

/**
 * Camera Viewfinder Dialog with Scanner Reticle and Real-Time QR Decoding.
 */
@Composable
fun CameraScannerDialog(
    onDismiss: () -> Unit,
    onQrCodeDetected: (String) -> Unit,
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            if (hasCameraPermission) {
                // Live Camera SurfaceView
                CameraSurfacePreview(onQrCodeDetected = onQrCodeDetected)

                // Laser Scanning Line Animation
                val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
                val laserPosition by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 240f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "laser_y",
                )

                // Scanner Viewfinder Overlay
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(3.dp, Color(0xFF3B82F6), RoundedCornerShape(24.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .offset(y = laserPosition.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, GecePalette.Accent, Color(0xFFFBBF24), Color.Transparent)
                                )
                            )
                    )
                }

                // Top Close Button and Guidance
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(stringResource(R.string.remote_align_qr_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0x66000000)),
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.common_done), tint = Color.White)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xCC0A0F1D),
                        border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    ) {
                        Text(
                            stringResource(R.string.remote_align_qr_desc),
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            } else {
                // Permission Denied View
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFF60A5FA), modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.remote_camera_permission_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.remote_camera_permission_desc), color = Color(0xFF94A3B8), textAlign = TextAlign.Center, fontSize = 13.sp)
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = GecePalette.Accent),
                    ) {
                        Text(stringResource(R.string.remote_camera_permission_allow), fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)) {
                        Text(stringResource(R.string.common_cancel), color = Color(0xFF94A3B8))
                    }
                }
            }
        }
    }
}

/**
 * SurfaceView hosting the camera hardware and streaming frames to ZXing decoder.
 */
@Suppress("DEPRECATION")
@Composable
private fun CameraSurfacePreview(
    onQrCodeDetected: (String) -> Unit,
) {
    var camera by remember { mutableStateOf<Camera?>(null) }
    var hasDecoded by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.setPreviewCallback(null)
                camera?.stopPreview()
                camera?.release()
                camera = null
            } catch (_: Exception) {}
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        try {
                            val cam = Camera.open()
                            camera = cam
                            cam.setDisplayOrientation(90)
                            val params = cam.parameters
                            val focusModes = params.supportedFocusModes
                            if (focusModes?.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) == true) {
                                params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                            }
                            cam.parameters = params
                            cam.setPreviewDisplay(holder)
                            cam.setPreviewCallback { data, c ->
                                if (hasDecoded || data == null) return@setPreviewCallback
                                val size = c.parameters.previewSize ?: return@setPreviewCallback
                                CoroutineScope(Dispatchers.Default).launch {
                                    val qr = QrCodes.decodeYuv(data, size.width, size.height)
                                    if (!qr.isNullOrBlank() && !hasDecoded) {
                                        hasDecoded = true
                                        withContext(Dispatchers.Main) {
                                            onQrCodeDetected(qr)
                                        }
                                    }
                                }
                            }
                            cam.startPreview()
                        } catch (e: Exception) {
                            Log.e("CameraSurfacePreview", "Kamera başlatılamadı", e)
                        }
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        try {
                            camera?.setPreviewCallback(null)
                            camera?.stopPreview()
                            camera?.release()
                            camera = null
                        } catch (_: Exception) {}
                    }
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun TvShortcutChip(
    icon: ImageVector,
    label: String,
    color: Color = GecePalette.Accent,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        color = GecePalette.CardSurface,
        border = BorderStroke(1.dp, GecePalette.CardBorder),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(icon, contentDescription = null, tint = GecePalette.Accent, modifier = Modifier.size(16.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = GecePalette.TextPrimary)
        }
    }
}

