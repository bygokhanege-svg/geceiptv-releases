/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import app.opentv.ui.theme.GecePalette
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.pairing.ManagerServer
import app.opentv.pairing.QrCodes
import app.opentv.ui.WebManagerViewModel

/**
 * "Manage your channels from your phone or laptop."
 *
 * Shows a QR code and a short URL pointing at a web server running on this television.
 * Opening it on a phone gives a virtual remote, channel manager with cast-to-TV, and IPTV installer.
 */
@Composable
fun WebManagerScreen(
    onBack: () -> Unit,
    viewModel: WebManagerViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    Box(
        Modifier
            .fillMaxSize()
            .padding(if (isPhone) 16.dp else 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        when (val current = state) {
            is ManagerServer.State.Listening -> Listening(current.session, onBack)

            is ManagerServer.State.Failed -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.webmanager_stopped),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    current.reason,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { viewModel.start() }) { Text(stringResource(R.string.common_try_again)) }
                    OutlinedButton(onClick = onBack) { Text(stringResource(R.string.common_done)) }
                }
            }

            ManagerServer.State.Idle -> CircularProgressIndicator()
        }
    }
}

@Composable
private fun Listening(session: ManagerServer.Session, onBack: () -> Unit) {
    // Generated once per session — encoding is not free and the content does not change.
    val qr = remember(session.url) { QrCodes.render(session.url, QR_SIZE_PX) }
    val isPhone = LocalConfiguration.current.screenWidthDp < 600

    if (isPhone) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Live Status Indicator Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(GecePalette.AccentSubtle)
                    .border(1.dp, GecePalette.Accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GecePalette.Accent),
                )
                Text(
                    stringResource(R.string.web_manager_badge),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = GecePalette.Accent,
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                stringResource(R.string.web_manager_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                stringResource(R.string.web_manager_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            // QR Code Box
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = stringResource(R.string.webmanager_qr_desc),
                        modifier = Modifier.size(180.dp),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(R.string.web_manager_scan_cam),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                session.url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(16.dp))

            // 3 Instruction Steps Cards
            StepRow(
                step = "1",
                title = stringResource(R.string.web_manager_feature_remote_title),
                desc = stringResource(R.string.web_manager_feature_remote_desc),
            )
            Spacer(Modifier.height(8.dp))
            StepRow(
                step = "2",
                title = stringResource(R.string.web_manager_feature_browse_title),
                desc = stringResource(R.string.web_manager_feature_browse_desc),
            )
            Spacer(Modifier.height(8.dp))
            StepRow(
                step = "3",
                title = stringResource(R.string.web_manager_feature_m3u_title),
                desc = stringResource(R.string.web_manager_feature_paste_desc),
            )

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.common_done))
            }

            Spacer(Modifier.height(6.dp))

            Text(
                stringResource(R.string.web_manager_security_note),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(40.dp, Alignment.CenterHorizontally),
        ) {
            // Left Column: QR Card & Link
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 300.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (qr != null) {
                        Image(
                            bitmap = qr.asImageBitmap(),
                            contentDescription = stringResource(R.string.webmanager_qr_desc),
                            modifier = Modifier.size(240.dp),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.web_manager_scan_cam),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    session.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            // Right Column: Title, Instructions, Features, Buttons
            Column(
                modifier = Modifier
                    .widthIn(max = 580.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                // Live Status Indicator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GecePalette.AccentSubtle)
                        .border(1.dp, GecePalette.Accent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(GecePalette.Accent),
                    )
                    Text(
                        stringResource(R.string.web_manager_badge),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GecePalette.Accent,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    stringResource(R.string.web_manager_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    stringResource(R.string.web_manager_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                // 3 Instruction Steps Cards
                StepRow(
                    step = "1",
                    title = stringResource(R.string.web_manager_feature_remote_title),
                    desc = stringResource(R.string.web_manager_feature_remote_desc),
                )
                Spacer(Modifier.height(8.dp))
                StepRow(
                    step = "2",
                    title = stringResource(R.string.web_manager_feature_browse_title),
                    desc = stringResource(R.string.web_manager_feature_browse_desc),
                )
                Spacer(Modifier.height(8.dp))
                StepRow(
                    step = "3",
                    title = stringResource(R.string.web_manager_feature_m3u_title),
                    desc = stringResource(R.string.web_manager_feature_paste_desc),
                )

                Spacer(Modifier.height(18.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.common_done))
                    }
                    Text(
                        stringResource(R.string.web_manager_security_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(step: String, title: String, desc: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                step,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private const val QR_SIZE_PX = 600
