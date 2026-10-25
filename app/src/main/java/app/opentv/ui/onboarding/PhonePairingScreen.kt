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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.opentv.R
import app.opentv.core.AppSettings
import app.opentv.data.model.Source
import app.opentv.pairing.PairingServer
import app.opentv.pairing.QrCodes
import app.opentv.ui.LegalDisclaimerAcceptanceCard
import app.opentv.ui.LegalDisclaimerModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * "Set it up from your phone."
 *
 * Shows a QR code pointing at a web server running on this television, plus a six-digit code
 * the phone must echo back. See [PairingServer] for why it works this way rather than through
 * a website.
 */
@Composable
fun PhonePairingScreen(
    onReceived: (Source) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val settings = remember { AppSettings.get(context) }
    val disclaimerAccepted by settings.disclaimerAccepted.collectAsState()
    var showLegalModal by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<Source?>(null) }

    val scope = remember { CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate) }
    val server = remember { PairingServer(scope) }
    val state by server.state.collectAsState()

    DisposableEffect(Unit) {
        server.start()
        onDispose {
            // Never leave a socket listening on someone's home network after they have
            // navigated away.
            server.stop()
            scope.cancel()
        }
    }

    LaunchedEffect(state) {
        (state as? PairingServer.State.Received)?.let {
            if (!settings.disclaimerAccepted.value) {
                pendingSource = it.draft
                showLegalModal = true
            } else {
                onReceived(it.draft)
            }
        }
    }

    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        when (val current = state) {
            is PairingServer.State.Listening -> Listening(
                session = current.session,
                disclaimerAccepted = disclaimerAccepted,
                onAcceptedChange = { settings.setDisclaimerAccepted(it) },
                onShowLegalModal = { showLegalModal = true },
                onCancel = {
                    if (!disclaimerAccepted) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.legal_disclaimer_must_accept),
                            Toast.LENGTH_LONG,
                        ).show()
                        showLegalModal = true
                    } else {
                        onCancel()
                    }
                },
            )

            is PairingServer.State.Failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.pairing_stopped), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                Text(
                    current.reason,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { server.start() }) { Text(stringResource(R.string.common_try_again)) }
                    OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.pairing_type_here)) }
                }
            }

            is PairingServer.State.Received -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text(stringResource(R.string.pairing_got_it))
            }

            PairingServer.State.Idle -> CircularProgressIndicator()
        }
    }

    if (showLegalModal) {
        LegalDisclaimerModal(
            onAccept = {
                settings.setDisclaimerAccepted(true)
                showLegalModal = false
                pendingSource?.let {
                    onReceived(it)
                    pendingSource = null
                }
            },
            onDismiss = { showLegalModal = false },
        )
    }
}

@Composable
private fun Listening(
    session: PairingServer.Session,
    disclaimerAccepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onShowLegalModal: () -> Unit,
    onCancel: () -> Unit,
) {
    // Generated once per session rather than on every recomposition — encoding is not free
    // and the content does not change.
    val qr = remember(session.url) { QrCodes.render(session.url, QR_SIZE_PX) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(320.dp),
        ) {
            if (qr != null) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = stringResource(R.string.pairing_qr_desc),
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(10.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            LegalDisclaimerAcceptanceCard(
                accepted = disclaimerAccepted,
                onAcceptedChange = onAcceptedChange,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(Modifier.widthIn(max = 520.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(Color(0xFF090D16))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(9.dp)),
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
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        "Hızlı QR Eşleştirme",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Text(stringResource(R.string.pairing_setup_title), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.pairing_setup_desc),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.pairing_enter_code),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                // Spaced out because it is being read across a room and typed into a phone.
                text = session.confirmCode.toCharArray().joinToString("  "),
                fontSize = 52.sp,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.pairing_camera_fail),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(session.shortUrl, style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.pairing_type_on_tv)) }
        }
    }
}

private const val QR_SIZE_PX = 600
