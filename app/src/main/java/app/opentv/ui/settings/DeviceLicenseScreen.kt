/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import app.opentv.ui.theme.GecePalette
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.opentv.R
import app.opentv.core.LicenseManager
import app.opentv.core.ServiceLocator
import app.opentv.pairing.QrCodes
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeviceLicenseScreen(
    onBack: () -> Unit,
    onOpenAdminPanel: () -> Unit = {},
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val graph = remember { ServiceLocator.get(context) }
    val licenseManager = graph.licenseManager
    val cloudClient = graph.licenseCloudClient
    val scope = rememberCoroutineScope()

    val licenseStatus by licenseManager.licenseStatus.collectAsState()
    val mac = licenseManager.formattedMac
    val key = licenseManager.deviceKey
    val qrUrl = licenseManager.webActivationUrl

    val qrBitmap = remember(qrUrl) { QrCodes.render(qrUrl, 420) }

    var inputCode by remember { mutableStateOf("") }
    var codeError by remember { mutableStateOf<String?>(null) }
    var codeSuccess by remember { mutableStateOf(false) }
    var isCloudSyncing by remember { mutableStateOf(false) }

    // Auto-check with Supabase when screen opens
    LaunchedEffect(Unit) {
        licenseManager.syncWithCloud(cloudClient)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.background,
                    ),
                    radius = 1600f,
                ),
            ),
    ) {
        val isPhone = LocalConfiguration.current.screenWidthDp < 600
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isPhone) 16.dp else 24.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.common_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.settings_license_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(22.dp),
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_license_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // Cloud Refresh / Sync Button
                IconButton(
                    onClick = {
                        if (!isCloudSyncing) {
                            scope.launch {
                                isCloudSyncing = true
                                val res = licenseManager.syncWithCloud(cloudClient)
                                isCloudSyncing = false
                                val msg = if (res.success) {
                                    if (res.newPlaylistsAdded > 0) {
                                        "Bulut ile eşitlendi: ${res.newPlaylistsAdded} yeni liste eklendi!"
                                    } else {
                                        "Bulut ile eşitlendi: Lisansınız güncel."
                                    }
                                } else {
                                    res.errorMessage ?: "Sunucuya bağlanılamadı"
                                }
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape),
                ) {
                    if (isCloudSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = "Bulut ile Eşitle",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            val submitCodeAction = {
                val trimmed = inputCode.trim()
                if (trimmed.equals("egesmart2026", ignoreCase = true)) {
                    inputCode = ""
                    codeError = null
                    codeSuccess = false
                    onOpenAdminPanel()
                } else {
                    val ok = licenseManager.activateWithCode(inputCode)
                    if (ok) {
                        codeSuccess = true
                        codeError = null
                        inputCode = ""
                    } else {
                        codeSuccess = false
                        codeError = context.getString(R.string.license_code_invalid)
                    }
                }
            }

            if (isPhone) {
                // Phone Layout: Single vertical scrolling column with full-width cards
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    LicenseStatusCard(licenseStatus)

                    DeviceCredentialsCard(
                        mac = mac,
                        key = key,
                        onCopyMac = {
                            clipboard.setText(AnnotatedString(mac))
                            Toast.makeText(context, context.getString(R.string.license_copied), Toast.LENGTH_SHORT).show()
                        },
                        onCopyKey = {
                            clipboard.setText(AnnotatedString(key))
                            Toast.makeText(context, context.getString(R.string.license_copied), Toast.LENGTH_SHORT).show()
                        },
                    )

                    LicenseCodeInputCard(
                        inputCode = inputCode,
                        onInputCodeChange = {
                            inputCode = it.uppercase()
                            codeError = null
                            codeSuccess = false
                        },
                        codeSuccess = codeSuccess,
                        codeError = codeError,
                        onSubmit = submitCodeAction,
                    )

                    WebActivationCard(qrBitmap = qrBitmap)

                    SupportContactCard()

                    Spacer(modifier = Modifier.height(16.dp))
                }
            } else {
                // TV / Large Tablet Layout: 2-Column Split
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    Column(
                        modifier = Modifier.weight(1.15f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LicenseStatusCard(licenseStatus)

                        DeviceCredentialsCard(
                            mac = mac,
                            key = key,
                            onCopyMac = {
                                clipboard.setText(AnnotatedString(mac))
                                Toast.makeText(context, context.getString(R.string.license_copied), Toast.LENGTH_SHORT).show()
                            },
                            onCopyKey = {
                                clipboard.setText(AnnotatedString(key))
                                Toast.makeText(context, context.getString(R.string.license_copied), Toast.LENGTH_SHORT).show()
                            },
                        )

                        LicenseCodeInputCard(
                            inputCode = inputCode,
                            onInputCodeChange = {
                                inputCode = it.uppercase()
                                codeError = null
                                codeSuccess = false
                            },
                            codeSuccess = codeSuccess,
                            codeError = codeError,
                            onSubmit = submitCodeAction,
                        )
                    }

                    Column(
                        modifier = Modifier.weight(0.85f),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        WebActivationCard(qrBitmap = qrBitmap)
                        SupportContactCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseCodeInputCard(
    inputCode: String,
    onInputCodeChange: (String) -> Unit,
    codeSuccess: Boolean,
    codeError: String?,
    onSubmit: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.license_code_input_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = onInputCodeChange,
                    placeholder = {
                        Text(
                            stringResource(R.string.license_code_hint),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onSubmit() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                )

                Button(
                    onClick = onSubmit,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.height(52.dp),
                ) {
                    Text(
                        text = stringResource(R.string.license_code_submit),
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            if (codeSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.license_code_success),
                    color = GecePalette.Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (codeError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = codeError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun WebActivationCard(qrBitmap: android.graphics.Bitmap?) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCode2,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.license_web_activation_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.license_web_activation_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(14.dp))

            // QR Image Box
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "gecetv.app/activate",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Kamera ile okutarak telefonunuzdan anında liste yükleyin veya lisansınızı yenileyin.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun SupportContactCard() {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "PREMİUM AKTİVASYON & DESTEK",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✉️ E-posta: ",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "geceiptv@gmail.com",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun LicenseStatusCard(status: LicenseManager.LicenseStatus) {
    val (badgeColor, title, subtitle, icon) = when (status) {
        is LicenseManager.LicenseStatus.Trial -> {
            Tuple4(
                Color(0xFFF59E0B),
                stringResource(R.string.license_trial_badge),
                stringResource(R.string.license_trial_remaining, status.daysRemaining),
                Icons.Filled.HourglassTop,
            )
        }
        is LicenseManager.LicenseStatus.Premium -> {
            val dateStr = if (status.isLifetime) {
                stringResource(R.string.license_premium_lifetime)
            } else {
                val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                "Bitiş: " + fmt.format(Date(status.expiryMillis))
            }
            Tuple4(
                GecePalette.Accent,
                stringResource(R.string.license_premium_active),
                dateStr,
                Icons.Filled.CheckCircle,
            )
        }
        is LicenseManager.LicenseStatus.Expired -> {
            Tuple4(
                MaterialTheme.colorScheme.error,
                stringResource(R.string.license_expired),
                stringResource(R.string.license_expired_desc),
                Icons.Filled.Warning,
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = badgeColor.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, badgeColor.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeColor,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DeviceCredentialsCard(
    mac: String,
    key: String,
    onCopyMac: () -> Unit,
    onCopyKey: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // MAC Address
            CredentialItem(
                label = stringResource(R.string.license_device_id),
                value = mac,
                onCopy = onCopyMac,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
            )

            // Device Key
            CredentialItem(
                label = stringResource(R.string.license_device_key),
                value = key,
                onCopy = onCopyKey,
            )
        }
    }
}

@Composable
private fun CredentialItem(
    label: String,
    value: String,
    onCopy: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onCopy)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.2.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Filled.ContentCopy,
                contentDescription = stringResource(R.string.license_copied),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
