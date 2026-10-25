/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.update

import android.app.Application
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.BuildConfig
import app.opentv.core.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * The one-line entry point: drop [UpdateGate] into the top-level layout and a sideloaded
 * install will notice and offer its own updates. It renders nothing until there is something
 * to say, so it is safe to place unconditionally over the whole app.
 */
@Composable
fun UpdateGate(viewModel: UpdateViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    when (val s = state) {
        UpdateUiState.Idle -> Unit

        is UpdateUiState.Available -> {
            val updateBtnFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(100)
                runCatching { updateBtnFocusRequester.requestFocus() }
            }

            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFF0F172A),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0284C7).copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Yeni Güncelleme Mevcut",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            text = "GECE İPTV Player ${s.update.versionName} sürümü yayınlandı.",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8),
                        )
                        Text(
                            text = "Mevcut sürümünüz: ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.5f),
                        )

                        if (s.update.notes.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E293B).copy(alpha = 0.6f))
                                    .padding(10.dp),
                            ) {
                                Text(
                                    text = s.update.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 10,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    var isFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = viewModel::install,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFocused) Color(0xFF0284C7) else Color(0xFF0369A1),
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .focusRequester(updateBtnFocusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .then(
                                if (isFocused) Modifier.border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                    ) {
                        Text("Şimdi Güncelle", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    var isFocused by remember { mutableStateOf(false) }
                    OutlinedButton(
                        onClick = viewModel::dismiss,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isFocused) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.2f),
                        ),
                        modifier = Modifier
                            .onFocusChanged { isFocused = it.isFocused }
                            .then(
                                if (isFocused) Modifier.background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                    ) {
                        Text("Daha Sonra", color = if (isFocused) Color.White else Color.White.copy(alpha = 0.7f))
                    }
                },
            )
        }

        is UpdateUiState.Downloading -> {
            AlertDialog(
                onDismissRequest = {},
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFF0F172A),
                confirmButton = {},
                title = {
                    Text(
                        text = "Güncelleme İndiriliyor…",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        if (s.fraction >= 0f) {
                            LinearProgressIndicator(
                                progress = { s.fraction },
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF1E293B),
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = "İndiriliyor: %${(s.fraction * 100).toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Lütfen bekleyin...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.5f),
                                )
                            }
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                color = Color(0xFF38BDF8),
                                trackColor = Color(0xFF1E293B),
                            )
                        }
                    }
                },
            )
        }

        is UpdateUiState.Failed -> {
            val retryFocusRequester = remember { FocusRequester() }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(100)
                runCatching { retryFocusRequester.requestFocus() }
            }

            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                shape = RoundedCornerShape(16.dp),
                containerColor = Color(0xFF0F172A),
                title = {
                    Text(
                        text = "İndirme Başarısız Oldu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                    )
                },
                text = {
                    Text(
                        text = "Güncelleme dosyası indirilemedi. İnternet bağlantınızı kontrol edip tekrar deneyin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                },
                confirmButton = {
                    var isFocused by remember { mutableStateOf(false) }
                    Button(
                        onClick = viewModel::install,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .focusRequester(retryFocusRequester)
                            .onFocusChanged { isFocused = it.isFocused }
                            .then(
                                if (isFocused) Modifier.border(2.dp, Color(0xFF38BDF8), RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                    ) {
                        Text("Tekrar Dene", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = viewModel::dismiss,
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text("Kapat", color = Color.White.copy(alpha = 0.7f))
                    }
                },
            )
        }
    }
}

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data class Available(val update: UpdateChecker.Update) : UpdateUiState
    data class Downloading(val update: UpdateChecker.Update, val fraction: Float) : UpdateUiState
    data class Failed(val update: UpdateChecker.Update) : UpdateUiState
}

/**
 * One shared update state so the manual "check for updates" (About) and the automatic gate speak
 * with one voice. A manual check that finds a build flips this, and [UpdateGate] — mounted over the
 * whole app — shows the dialog wherever the user is standing. Previously the two had separate state
 * (and the gate was throttled to a 6-hour check), so a manual find never actually prompted.
 */
object UpdateHub {
    val state = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
}

class UpdateViewModel(app: Application) : AndroidViewModel(app) {
    private val graph = ServiceLocator.get(app)
    private val checker = UpdateChecker(graph.httpClient, BuildConfig.VERSION_NAME)
    private val installer = ApkInstaller(graph.httpClient)

    // Backed by the shared hub so a manual check from About lights up this same gate.
    private val _state = UpdateHub.state
    val state = _state.asStateFlow()

    init { checkOnLaunch() }

    /**
     * Checks for updates on app launch. Runs asynchronously in the background so it never blocks
     * startup or live TV playback. If a newer release is found, prompts the user via UpdateGate.
     */
    fun checkOnLaunch() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1200)
            val update = checker.check()
            if (update != null) {
                _state.value = UpdateUiState.Available(update)
            }
        }
    }

    fun install() {
        val update = when (val s = _state.value) {
            is UpdateUiState.Available -> s.update
            is UpdateUiState.Failed -> s.update
            is UpdateUiState.Downloading -> s.update
            UpdateUiState.Idle -> return
        }
        viewModelScope.launch {
            _state.value = UpdateUiState.Downloading(update, 0f)
            runCatching {
                installer.downloadAndInstall(
                    context = getApplication(),
                    url = update.apkUrl,
                    expectedBytes = update.apkSizeBytes,
                ) { fraction -> _state.value = UpdateUiState.Downloading(update, fraction) }
            }.onSuccess {
                // The system installer is now front-and-centre; step our dialog aside.
                _state.value = UpdateUiState.Idle
            }.onFailure {
                _state.value = UpdateUiState.Failed(update)
            }
        }
    }

    fun dismiss() { _state.value = UpdateUiState.Idle }

    private companion object {
        const val KEY_LAST_CHECK = "last_update_check"
        const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L // 6 hours
    }
}
