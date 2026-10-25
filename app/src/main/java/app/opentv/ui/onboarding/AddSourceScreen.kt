/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.onboarding

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.opentv.R
import app.opentv.core.AppSettings
import app.opentv.data.model.Source
import app.opentv.data.model.SourceKind
import app.opentv.isRunningOnTelevision
import app.opentv.ui.LegalDisclaimerAcceptanceCard
import app.opentv.ui.LegalDisclaimerModal
import app.opentv.ui.SourcesViewModel
import app.opentv.ui.channels.readClipboardText

/**
 * First-run setup.
 *
 * Two things this screen does that most IPTV players do not:
 *
 * 1. **Test before saving.** The user finds out the password is wrong here, with a sentence
 *    explaining what to change, rather than staring at an empty channel list later.
 * 2. **Says where the credentials go.** People are handing over their provider login. They
 *    are entitled to know it stays on the device, and to be told so without having to go
 *    looking for a privacy policy.
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

    // The QR "set it up from your phone" flow only makes sense on a TV — it serves a setup page
    // FROM this device for a phone to fill in. On a phone that same screen shows a QR meant to be
    // scanned BY a phone, which is nonsensical and traps the user — so a non-TV device defaults
    // straight to the direct entry form. On a TV the QR is still offered first, because typing a
    // server address and password with a d-pad is the worst moment in every app of this kind.
    val isTelevision = remember(context) { isRunningOnTelevision(context) }
    // Editing an existing provider always uses the direct form (the fields are pre-filled, and there
    // is nothing to pair); only a fresh add on a TV offers the phone-pairing QR first.
    var usePhone by remember { mutableStateOf(isTelevision && editingSourceId == null) }

    if (usePhone) {
        PhonePairingScreen(
            // Even on a failed sync, leave the pairing screen: the source is already saved, so the
            // home screen shows either the guide (success) or a recoverable error (failure) rather
            // than stranding the user on an endless "Got it — connecting…" spinner.
            onReceived = { draft -> viewModel.saveAndSync(draft) { onFinished() } },
            onCancel = { usePhone = false },
        )
        return
    }

    val ui by viewModel.ui.collectAsState()

    // When editing, find the source being edited and pre-fill every field from it. Keyed on its id
    // so the fields seed once the list has loaded; a fresh add leaves everything blank.
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
    var mac by remember(existing?.id) { mutableStateOf(extractCleanMac(existing?.macAddress)) }
    var sn by remember(existing?.id) { mutableStateOf(extractParam(existing?.macAddress, "sn", "serial", "sncut")) }
    var deviceId1 by remember(existing?.id) { mutableStateOf(extractParam(existing?.macAddress, "device_id", "device_id1", "deviceid", "deviceid1")) }
    var deviceId2 by remember(existing?.id) { mutableStateOf(extractParam(existing?.macAddress, "device_id2", "deviceid2")) }
    var showMagIdentities by remember { mutableStateOf(sn.isNotBlank() || deviceId1.isNotBlank() || deviceId2.isNotBlank()) }
    var epgUrl by remember(existing?.id) { mutableStateOf(existing?.epgUrl ?: "") }
    var showAdvanced by remember { mutableStateOf(false) }
    var userAgent by remember(existing?.id) { mutableStateOf(existing?.userAgent ?: Source.DEFAULT_USER_AGENT) }

    // Editing copies onto the existing row, so id, enabled state, live format and last-sync stamp
    // are preserved; a fresh add starts from a blank Source (id 0, so save() inserts).
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 640.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_opentv_logo),
                        contentDescription = "GECE İPTV Player",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "GECE İPTV Player",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "Hızlı Kurulum & Yayın Portalı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(
                stringResource(
                    if (existing != null) R.string.settings_edit_provider_title
                    else R.string.onboarding_add_provider_title,
                ),
                style = MaterialTheme.typography.displaySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_add_provider_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilterChip(
                    selected = kind == SourceKind.XTREAM,
                    onClick = { kind = SourceKind.XTREAM },
                    label = { Text(stringResource(R.string.onboarding_xtream_login)) },
                )
                FilterChip(
                    selected = kind == SourceKind.M3U,
                    onClick = { kind = SourceKind.M3U },
                    label = { Text(stringResource(R.string.onboarding_m3u_url)) },
                )
                FilterChip(
                    selected = kind == SourceKind.STALKER,
                    onClick = { kind = SourceKind.STALKER },
                    label = { Text(stringResource(R.string.onboarding_stalker_portal)) },
                )
            }

            Spacer(Modifier.height(20.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.epg_name_optional)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

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
                supportingText = {
                    Text(
                        when (kind) {
                            SourceKind.XTREAM -> stringResource(R.string.onboarding_server_help)
                            SourceKind.M3U -> stringResource(R.string.onboarding_playlist_help)
                            SourceKind.STALKER -> stringResource(R.string.onboarding_portal_help)
                        },
                    )
                },
                trailingIcon = { PasteButton { onUrlChange(it) } },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            if (kind == SourceKind.XTREAM) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.recset_field_username)) },
                    trailingIcon = { PasteButton { username = it } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.recset_field_password)) },
                    trailingIcon = { PasteButton { password = it } },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (kind == SourceKind.STALKER) {
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = mac,
                    onValueChange = { mac = it.trim() },
                    label = { Text(stringResource(R.string.onboarding_mac_address)) },
                    placeholder = { Text("00:1A:79:XX:XX:XX") },
                    supportingText = {
                        Text(stringResource(R.string.onboarding_mac_help))
                    },
                    trailingIcon = {
                        PasteButton { input ->
                            mac = extractCleanMac(input).ifBlank { input.trim() }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.stalker_mag_identities_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = { showMagIdentities = !showMagIdentities }) {
                        Text(
                            if (showMagIdentities) stringResource(R.string.onboarding_hide_advanced)
                            else stringResource(R.string.onboarding_advanced)
                        )
                    }
                }

                if (showMagIdentities) {
                    Text(
                        stringResource(R.string.stalker_mag_identities_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    OutlinedTextField(
                        value = sn,
                        onValueChange = { sn = it.trim() },
                        label = { Text(stringResource(R.string.stalker_field_sn)) },
                        placeholder = { Text("Örn: 8E3CB62D32AD7...") },
                        trailingIcon = { PasteButton { sn = it.trim() } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deviceId1,
                        onValueChange = { deviceId1 = it.trim() },
                        label = { Text(stringResource(R.string.stalker_field_device_id1)) },
                        placeholder = { Text("Örn: 675E33773C6F0B96...") },
                        trailingIcon = { PasteButton { deviceId1 = it.trim() } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = deviceId2,
                        onValueChange = { deviceId2 = it.trim() },
                        label = { Text(stringResource(R.string.stalker_field_device_id2)) },
                        placeholder = { Text("Örn: 4A9A4970252EA3BD...") },
                        trailingIcon = { PasteButton { deviceId2 = it.trim() } },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = { showAdvanced = !showAdvanced }) {
                Text(if (showAdvanced) stringResource(R.string.onboarding_hide_advanced) else stringResource(R.string.onboarding_advanced))
            }

            if (showAdvanced) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = epgUrl,
                    onValueChange = { epgUrl = it },
                    label = { Text(stringResource(R.string.onboarding_guide_url_optional)) },
                    supportingText = {
                        Text(stringResource(R.string.onboarding_guide_blank_help))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = userAgent,
                    onValueChange = { userAgent = it },
                    label = { Text("User-Agent") },
                    supportingText = {
                        Text(stringResource(R.string.onboarding_user_agent_help))
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(20.dp))

            ui.testResult?.let { message ->
                Card(Modifier.fillMaxWidth()) {
                    Text(message, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
            }
            ui.testError?.let { message ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        message,
                        Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
            ui.syncMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(12.dp))
            }

            Spacer(Modifier.height(8.dp))
            LegalDisclaimerAcceptanceCard(
                accepted = disclaimerAccepted,
                onAcceptedChange = { settings.setDisclaimerAccepted(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 640.dp),
            )
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { viewModel.test(draft()) },
                    enabled = canSubmit && !ui.testing && !ui.syncing,
                ) {
                    if (ui.testing) {
                        CircularProgressIndicator(Modifier.height(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.recset_test_connection))
                    }
                }
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
                ) {
                    Text(if (ui.syncing) stringResource(R.string.onboarding_working) else stringResource(R.string.onboarding_save_load))
                }
            }

            // "Set up from your phone" (the QR pairing flow) is offered only on a TV — see the
            // isTelevision note above. On a phone it would just show a QR to nowhere.
            if (isTelevision) {
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = { usePhone = true }) {
                    Text(stringResource(R.string.onboarding_use_phone))
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.onboarding_privacy_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
 * A trailing "paste" affordance for a provider field: reads the system clipboard and appends it
 * via [onPaste]. A long Xtream URL or username is miserable to type on a d-pad, so this is exactly
 * what the reporter asked for. Reads the same clipboard as the on-screen keyboard's Paste key; an
 * empty clipboard just shows a brief message rather than doing nothing silently.
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
        Icon(Icons.Filled.ContentPaste, contentDescription = stringResource(R.string.kbd_paste))
    }
}
