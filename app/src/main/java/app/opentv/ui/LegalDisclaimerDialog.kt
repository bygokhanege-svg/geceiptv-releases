/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.zIndex
import app.opentv.R

/**
 * Full-screen / large modal presenting the 7 detailed clauses of the Legal Disclaimer & Terms of Service.
 * TV remote and mobile friendly with high-contrast text and focusable action buttons.
 */
@Composable
fun LegalDisclaimerModal(
    onAccept: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    val acceptFocus = remember { FocusRequester() }

    androidx.activity.compose.BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(200f)
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .widthIn(max = 680.dp)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
            ),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Gavel,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.legal_disclaimer_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = stringResource(R.string.legal_disclaimer_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable clauses
                Column(
                    modifier = Modifier
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(end = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    LegalClauseItem(
                        title = stringResource(R.string.legal_clause_1_title),
                        body = stringResource(R.string.legal_clause_1_body),
                    )
                    LegalClauseItem(
                        title = stringResource(R.string.legal_clause_2_title),
                        body = stringResource(R.string.legal_clause_2_body),
                    )
                    LegalClauseItem(
                        title = stringResource(R.string.legal_clause_3_title),
                        body = stringResource(R.string.legal_clause_3_body),
                    )
                    LegalClauseItem(
                        title = stringResource(R.string.legal_clause_4_title),
                        body = stringResource(R.string.legal_clause_4_body),
                    )
                    LegalClauseItem(
                        title = stringResource(R.string.legal_clause_5_title),
                        body = stringResource(R.string.legal_clause_5_body),
                    )
                    LegalClauseItem(
                        title = stringResource(R.string.legal_clause_6_title),
                        body = stringResource(R.string.legal_clause_6_body),
                    )
                    LegalClauseItem(
                        title = stringResource(R.string.legal_clause_7_title),
                        body = stringResource(R.string.legal_clause_7_body),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(stringResource(R.string.legal_disclaimer_close))
                    }
                    if (onAccept != null) {
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = {
                                onAccept()
                                onDismiss()
                            },
                            modifier = Modifier.focusRequester(acceptFocus),
                        ) {
                            Text(
                                stringResource(R.string.legal_disclaimer_accept_btn),
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
            }
        }
    }

    if (onAccept != null) {
        LaunchedEffect(Unit) {
            runCatching { acceptFocus.requestFocus() }
        }
    }
}

@Composable
private fun LegalClauseItem(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp,
            )
        }
    }
}

/**
 * Compact, interactive legal confirmation card with a checkbox and "Review Terms" button.
 * Designed to fit directly beneath the QR code menu or above setup forms.
 */
@Composable
fun LegalDisclaimerAcceptanceCard(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showModal by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val borderColor = when {
        isFocused -> MaterialTheme.colorScheme.primary
        accepted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
            .onFocusChanged { isFocused = it.isFocused },
        shape = RoundedCornerShape(14.dp),
        color = if (accepted) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onAcceptedChange(!accepted) },
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = onAcceptedChange,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.legal_disclaimer_short_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.legal_disclaimer_checkbox),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { showModal = true }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.legal_disclaimer_read_btn),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }

    if (showModal) {
        LegalDisclaimerModal(
            onAccept = { onAcceptedChange(true) },
            onDismiss = { showModal = false },
        )
    }
}
