/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import app.opentv.R
import app.opentv.data.model.Profile
import app.opentv.ui.ProfilesViewModel
import app.opentv.ui.channels.OnScreenKeyboard
import app.opentv.ui.theme.GecePalette

/**
 * Netflix-Style Who's Watching Profile Management screen:
 * - Avatar selection with vibrant thematic badges
 * - Profile PIN lock support (4-digit security code)
 * - Kids profile mode
 * - Custom menu toggle per profile (Live TV, Movies, Series, MultiView)
 * - Tailored keyboard support: Native soft keyboard on Phone/Tablet; D-pad OnScreenKeyboard on TV.
 */
@Composable
fun ProfilesScreen(
    onBack: () -> Unit,
    isTelevision: Boolean = false,
    viewModel: ProfilesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val profiles by viewModel.profiles.collectAsState()
    val activeId by viewModel.activeProfileId.collectAsState()
    val configuration = LocalConfiguration.current
    val isPhone = configuration.screenWidthDp < 600 && !isTelevision

    var isManageMode by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf<Profile?>(null) }
    var isCreatingNew by remember { mutableStateOf(false) }
    var pinPromptProfile by remember { mutableStateOf<Profile?>(null) }

    // PIN Unlock Prompt Dialog
    pinPromptProfile?.let { target ->
        ProfilePinPromptDialog(
            profile = target,
            isTelevision = isTelevision,
            onDismiss = { pinPromptProfile = null },
            onSuccess = {
                pinPromptProfile = null
                viewModel.select(target.id)
                Toast.makeText(context, "${target.name} profiline geçildi", Toast.LENGTH_SHORT).show()
                onBack()
            },
        )
    }

    // Profile Editor Screen / Overlay
    if (editingProfile != null || isCreatingNew) {
        val target = editingProfile ?: Profile(
            id = 0,
            name = "",
            createdAtMillis = System.currentTimeMillis(),
            avatarId = "avatar_1",
            pin = null,
            isKids = false,
            liveEnabled = true,
            moviesEnabled = true,
            seriesEnabled = true,
            multiViewEnabled = true,
        )

        ProfileEditScreen(
            profile = target,
            isNew = isCreatingNew,
            isTelevision = isTelevision,
            onCancel = {
                editingProfile = null
                isCreatingNew = false
            },
            onSave = { updated ->
                viewModel.saveProfile(updated)
                editingProfile = null
                isCreatingNew = false
                isManageMode = false
                Toast.makeText(context, "${updated.name} profili kaydedildi", Toast.LENGTH_SHORT).show()
            },
            onDelete = { profileId ->
                viewModel.remove(profileId)
                editingProfile = null
                isCreatingNew = false
                Toast.makeText(context, "Profil silindi", Toast.LENGTH_SHORT).show()
            },
        )
        return
    }

    // MAIN VIEW: "Kim İzliyor?" (Who's Watching?)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GecePalette.CanvasBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isPhone) 16.dp else 40.dp, vertical = if (isPhone) 24.dp else 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Geri",
                        tint = GecePalette.TextSecondary,
                    )
                }

                Button(
                    onClick = { isManageMode = !isManageMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isManageMode) GecePalette.Accent else GecePalette.CardElevated,
                        contentColor = if (isManageMode) GecePalette.OnAccent else GecePalette.TextPrimary,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isManageMode) GecePalette.Accent else GecePalette.CardBorder),
                ) {
                    Icon(
                        imageVector = if (isManageMode) Icons.Filled.Check else Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isManageMode) "Tamamla" else "Profilleri Yönet",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(if (isPhone) 20.dp else 32.dp))

            // Cinematic Netflix-style Title
            Text(
                text = stringResource(R.string.profiles_title),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = GecePalette.TextPrimary,
                fontSize = if (isPhone) 24.sp else 34.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = if (isManageMode) "Düzenlemek istediğiniz profile dokunun" else "İzlemeye devam etmek için profilinizi seçin",
                color = GecePalette.TextSecondary,
                fontSize = if (isPhone) 12.sp else 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(if (isPhone) 28.dp else 44.dp))

            // Responsive Netflix Avatar Grid / Row
            if (isTelevision || !isPhone) {
                // TV / Tablet: Yatay ortalı sıralı avatar listesi
                Row(
                    horizontalArrangement = Arrangement.spacedBy(22.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 12.dp),
                ) {
                    profiles.forEach { profile ->
                        NetflixProfileCard(
                            profile = profile,
                            isActive = profile.id == activeId,
                            isManageMode = isManageMode,
                            isTelevision = isTelevision,
                            onClick = {
                                if (isManageMode) {
                                    editingProfile = profile
                                } else {
                                    if (!profile.pin.isNullOrBlank()) {
                                        pinPromptProfile = profile
                                    } else {
                                        viewModel.select(profile.id)
                                        Toast.makeText(context, "${profile.name} profiline geçildi", Toast.LENGTH_SHORT).show()
                                        onBack()
                                    }
                                }
                            },
                        )
                    }

                    // "+ Profil Ekle" Kartı
                    if (profiles.size < 6) {
                        NetflixAddProfileCard(
                            isTelevision = isTelevision,
                            onClick = { isCreatingNew = true },
                        )
                    }
                }
            } else {
                // Phone: 2'li ızgara (2 Columns)
                val chunked = profiles.chunked(2)
                Column(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    chunked.forEach { rowProfiles ->
                        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            rowProfiles.forEach { profile ->
                                NetflixProfileCard(
                                    profile = profile,
                                    isActive = profile.id == activeId,
                                    isManageMode = isManageMode,
                                    isTelevision = false,
                                    onClick = {
                                        if (isManageMode) {
                                            editingProfile = profile
                                        } else {
                                            if (!profile.pin.isNullOrBlank()) {
                                                pinPromptProfile = profile
                                            } else {
                                                viewModel.select(profile.id)
                                                Toast.makeText(context, "${profile.name} profiline geçildi", Toast.LENGTH_SHORT).show()
                                                onBack()
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }

                    if (profiles.size < 6) {
                        NetflixAddProfileCard(
                            isTelevision = false,
                            onClick = { isCreatingNew = true },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Netflix-style Profile Card with gradient badge, lock badge, kids tag and pencil overlay
 */
@Composable
private fun NetflixProfileCard(
    profile: Profile,
    isActive: Boolean,
    isManageMode: Boolean,
    isTelevision: Boolean,
    onClick: () -> Unit,
) {
    val avatar = remember(profile.avatarId) { ProfileAvatars.get(profile.avatarId) }
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "card_scale")

    val cardSize = if (isTelevision) 130.dp else 115.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .width(cardSize + 16.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(cardSize)
                .clip(RoundedCornerShape(16.dp))
                .background(avatar.gradient)
                .border(
                    width = if (isFocused) 3.dp else if (isActive) 2.5.dp else 1.dp,
                    color = if (isFocused) GecePalette.Accent else if (isActive) GecePalette.Accent else Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp),
                )
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            // Avatar İkonu
            Icon(
                imageVector = avatar.icon,
                contentDescription = profile.name,
                tint = Color.White,
                modifier = Modifier.size(if (isTelevision) 58.dp else 50.dp),
            )

            // Kilit Rozeti (PIN Varsa)
            if (!profile.pin.isNullOrBlank() && !isManageMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Kilitli",
                        tint = GecePalette.Accent,
                        modifier = Modifier.size(13.dp),
                    )
                }
            }

            // Çocuk Rozeti
            if (profile.isKids && !isManageMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GecePalette.Accent)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "ÇOCUK",
                        color = GecePalette.OnAccent,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }

            // Profilleri Yönet Modu: Kalem Katmanı
            if (isManageMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Düzenle",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Profil İsmi
        Text(
            text = profile.name,
            color = if (isActive) GecePalette.Accent else GecePalette.TextPrimary,
            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )

        if (isActive) {
            Text(
                text = "Aktif",
                color = GecePalette.Accent,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Netflix-style Add Profile button card
 */
@Composable
private fun NetflixAddProfileCard(
    isTelevision: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.08f else 1f, label = "add_scale")
    val cardSize = if (isTelevision) 130.dp else 115.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .width(cardSize + 16.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(cardSize)
                .clip(RoundedCornerShape(16.dp))
                .background(GecePalette.CardElevated)
                .border(
                    width = if (isFocused) 2.5.dp else 1.5.dp,
                    color = if (isFocused) GecePalette.Accent else GecePalette.CardBorder,
                    shape = RoundedCornerShape(16.dp),
                )
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Profil Ekle",
                tint = if (isFocused) GecePalette.Accent else GecePalette.TextSecondary,
                modifier = Modifier.size(if (isTelevision) 44.dp else 38.dp),
            )
        }

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Profil Ekle",
            color = if (isFocused) GecePalette.Accent else GecePalette.TextSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * PIN Prompt Dialog with circular dots and TV/touch friendly keypad
 */
@Composable
private fun ProfilePinPromptDialog(
    profile: Profile,
    isTelevision: Boolean,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    var enteredPin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }
    val avatar = remember(profile.avatarId) { ProfileAvatars.get(profile.avatarId) }

    fun addDigit(digit: String) {
        if (enteredPin.length < 4) {
            hasError = false
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == 4) {
                if (newPin == profile.pin) {
                    onSuccess()
                } else {
                    hasError = true
                    enteredPin = ""
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GecePalette.CardSurface)
                .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(20.dp))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Profile Avatar mini
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(avatar.gradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = avatar.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Profil Kilidi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = GecePalette.TextPrimary,
                )

                Text(
                    text = "${profile.name} profiline girmek için 4 haneli PIN girin",
                    color = GecePalette.TextSecondary,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(20.dp))

                // 4 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasError) Color(0xFFEF4444)
                                    else if (isFilled) GecePalette.Accent
                                    else Color.Transparent,
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (hasError) Color(0xFFEF4444)
                                    else if (isFilled) GecePalette.Accent
                                    else GecePalette.TextSecondary.copy(alpha = 0.5f),
                                    shape = CircleShape,
                                ),
                        )
                    }
                }

                if (hasError) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Hatalı PIN kodu!",
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(22.dp))

                // Keypad (3x4 Grid)
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "⌫"),
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { rowKeys ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowKeys.forEach { key ->
                                Box(
                                    modifier = Modifier
                                        .size(if (isTelevision) 64.dp else 56.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(GecePalette.CardElevated)
                                        .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(12.dp))
                                        .clickable {
                                            when (key) {
                                                "C" -> {
                                                    enteredPin = ""
                                                    hasError = false
                                                }
                                                "⌫" -> {
                                                    if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                    hasError = false
                                                }
                                                else -> addDigit(key)
                                            }
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = key,
                                        color = GecePalette.TextPrimary,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text(
                        text = "Vazgeç",
                        color = GecePalette.TextSecondary,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

/**
 * Full-featured Profile Customization Screen:
 * - Profile Name with soft keyboard on phone/tablet, OnScreenKeyboard on TV
 * - Avatar selection gallery
 * - 4-digit PIN setup toggle
 * - Kids mode toggle
 * - Menu customization (Live TV, Movies, Series, MultiView toggles)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileEditScreen(
    profile: Profile,
    isNew: Boolean,
    isTelevision: Boolean,
    onCancel: () -> Unit,
    onSave: (Profile) -> Unit,
    onDelete: (Long) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isPhone = configuration.screenWidthDp < 600 && !isTelevision

    var name by remember { mutableStateOf(profile.name) }
    var selectedAvatarId by remember { mutableStateOf(profile.avatarId) }
    var hasPin by remember { mutableStateOf(!profile.pin.isNullOrBlank()) }
    var pinValue by remember { mutableStateOf(profile.pin ?: "") }
    var isKids by remember { mutableStateOf(profile.isKids) }
    var liveEnabled by remember { mutableStateOf(profile.liveEnabled) }
    var moviesEnabled by remember { mutableStateOf(profile.moviesEnabled) }
    var seriesEnabled by remember { mutableStateOf(profile.seriesEnabled) }
    var multiViewEnabled by remember { mutableStateOf(profile.multiViewEnabled) }

    var showTvKeyboard by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val currentAvatar = remember(selectedAvatarId) { ProfileAvatars.get(selectedAvatarId) }

    // TV Virtual Keyboard Modal
    if (showTvKeyboard && isTelevision) {
        Dialog(onDismissRequest = { showTvKeyboard = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(GecePalette.CardSurface)
                    .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(16.dp))
                    .padding(20.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Profil İsmini Düzenle",
                        style = MaterialTheme.typography.titleMedium,
                        color = GecePalette.TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = name.ifBlank { "(Boş)" },
                        style = MaterialTheme.typography.headlineSmall,
                        color = GecePalette.Accent,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    Spacer(Modifier.height(16.dp))
                    OnScreenKeyboard(
                        onKey = { if (name.length < 20) name += it },
                        onSpace = { if (name.length < 20) name += " " },
                        onBackspace = { name = name.dropLast(1) },
                        onClear = { name = "" },
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showTvKeyboard = false },
                        colors = ButtonDefaults.buttonColors(containerColor = GecePalette.Accent),
                    ) {
                        Text("Tamam", color = GecePalette.OnAccent, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Profili Sil") },
            text = { Text("${profile.name} profili ve izleme geçmişi kalıcı olarak silinecektir. Onaylıyor musunuz?") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete(profile.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                ) {
                    Text("Evet, Sil", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Vazgeç") }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GecePalette.CanvasBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (isPhone) 16.dp else 40.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "İptal",
                        tint = GecePalette.TextSecondary,
                    )
                }

                Text(
                    text = if (isNew) "Yeni Profil" else "Profili Düzenle",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = GecePalette.TextPrimary,
                )

                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Lütfen bir profil adı girin", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (hasPin && pinValue.length != 4) {
                            Toast.makeText(context, "PIN kodu 4 haneli olmalıdır", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val finalPin = if (hasPin) pinValue else null
                        onSave(
                            profile.copy(
                                name = name.trim(),
                                avatarId = selectedAvatarId,
                                pin = finalPin,
                                isKids = isKids,
                                liveEnabled = liveEnabled,
                                moviesEnabled = moviesEnabled,
                                seriesEnabled = seriesEnabled,
                                multiViewEnabled = multiViewEnabled,
                            ),
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GecePalette.Accent),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Text("Kaydet", color = GecePalette.OnAccent, fontWeight = FontWeight.ExtraBold)
                }
            }

            Spacer(Modifier.height(24.dp))

            // Large Avatar Preview
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(currentAvatar.gradient)
                    .border(2.dp, GecePalette.Accent, RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = currentAvatar.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // 1. Profil İsmi (TV vs Phone Klavye Düzeni)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
            ) {
                Text(
                    text = "Profil Adı",
                    color = GecePalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(6.dp))

                if (isTelevision) {
                    // TV: Kumanda ile tıklayıp D-pad sanal klavyeyi açan kutu
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GecePalette.CardElevated)
                            .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(12.dp))
                            .clickable { showTvKeyboard = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = name.ifBlank { "Profil adı belirleyin..." },
                                color = if (name.isBlank()) GecePalette.TextSecondary else GecePalette.TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Düzenle",
                                tint = GecePalette.Accent,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                } else {
                    // Telefon ve Tablet: Doğrudan dokunmatik sistem klavyesini açan OutlinedTextField
                    OutlinedTextField(
                        value = name,
                        onValueChange = { if (it.length <= 20) name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Profil adı girin", color = GecePalette.TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GecePalette.Accent,
                            unfocusedBorderColor = GecePalette.CardBorder,
                            focusedTextColor = GecePalette.TextPrimary,
                            unfocusedTextColor = GecePalette.TextPrimary,
                            cursorColor = GecePalette.Accent,
                        ),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 2. Avatar Galerisi (Netflix Tarzı)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
            ) {
                Text(
                    text = "Avatarını Seç",
                    color = GecePalette.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp),
                ) {
                    items(ProfileAvatars.ALL) { av ->
                        val isSelected = av.id == selectedAvatarId
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(av.gradient)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) GecePalette.Accent else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(14.dp),
                                )
                                .clickable { selectedAvatarId = av.id },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = av.icon,
                                contentDescription = av.name,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp),
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(GecePalette.Accent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = GecePalette.OnAccent,
                                        modifier = Modifier.size(11.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // 3. Profil Kilidi (PIN)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GecePalette.CardBorder),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = GecePalette.Accent,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Profil Kilidi (4 Haneli PIN)",
                                    color = GecePalette.TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                )
                                Text(
                                    text = "Profile girişte şifre sorulsun",
                                    color = GecePalette.TextSecondary,
                                    fontSize = 11.sp,
                                )
                            }
                        }

                        Switch(
                            checked = hasPin,
                            onCheckedChange = {
                                hasPin = it
                                if (!it) pinValue = ""
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GecePalette.OnAccent,
                                checkedTrackColor = GecePalette.Accent,
                            ),
                        )
                    }

                    if (hasPin) {
                        Spacer(Modifier.height(14.dp))
                        OutlinedTextField(
                            value = pinValue,
                            onValueChange = {
                                val filtered = it.filter { ch -> ch.isDigit() }
                                if (filtered.length <= 4) pinValue = filtered
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("4 haneli PIN girin (Örn: 1234)", color = GecePalette.TextSecondary) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GecePalette.Accent,
                                unfocusedBorderColor = GecePalette.CardBorder,
                                focusedTextColor = GecePalette.TextPrimary,
                                unfocusedTextColor = GecePalette.TextPrimary,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // 4. Çocuk Profili (Kids Mode)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GecePalette.CardBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Çocuk Profili",
                            color = GecePalette.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "Sadece aile ve çocuklara uygun içerikler sunulsun",
                            color = GecePalette.TextSecondary,
                            fontSize = 11.sp,
                        )
                    }

                    Switch(
                        checked = isKids,
                        onCheckedChange = { isKids = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GecePalette.OnAccent,
                            checkedTrackColor = GecePalette.Accent,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 5. KENDİ MENÜSÜNÜ DÜZENLEME (NETFLIX TARZI ÖZELLEŞTİRME)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp),
                colors = CardDefaults.cardColors(containerColor = GecePalette.CardSurface),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GecePalette.CardBorder),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Görünecek Menüler (Menüyü Özelleştir)",
                        color = GecePalette.TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = "Bu profil için ana ekranda hangi menülerin açık olacağını belirleyin:",
                        color = GecePalette.TextSecondary,
                        fontSize = 11.5.sp,
                    )

                    Spacer(Modifier.height(14.dp))

                    ProfileMenuToggleRow(
                        title = "Canlı TV",
                        icon = Icons.Default.Tv,
                        enabled = liveEnabled,
                        onToggle = { liveEnabled = it },
                    )

                    ProfileMenuToggleRow(
                        title = "Filmler (VOD)",
                        icon = Icons.Default.Movie,
                        enabled = moviesEnabled,
                        onToggle = { moviesEnabled = it },
                    )

                    ProfileMenuToggleRow(
                        title = "Diziler",
                        icon = Icons.Default.VideoLibrary,
                        enabled = seriesEnabled,
                        onToggle = { seriesEnabled = it },
                    )

                    ProfileMenuToggleRow(
                        title = "Çoklu Ekran",
                        icon = Icons.Default.GridView,
                        enabled = multiViewEnabled,
                        onToggle = { multiViewEnabled = it },
                    )
                }
            }

            // 6. Profili Sil Butonu (Temel profil 1 değilse)
            if (!isNew && profile.id != 1L) {
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x22EF4444)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Profili Sil",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Bu Profili Sil",
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProfileMenuToggleRow(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) GecePalette.Accent else GecePalette.TextSecondary,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = title,
                color = if (enabled) GecePalette.TextPrimary else GecePalette.TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = if (enabled) FontWeight.Bold else FontWeight.Normal,
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = GecePalette.OnAccent,
                checkedTrackColor = GecePalette.Accent,
            ),
        )
    }
}
