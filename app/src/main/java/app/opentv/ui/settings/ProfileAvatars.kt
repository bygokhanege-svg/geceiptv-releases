/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class ProfileAvatar(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val gradient: Brush,
    val primaryColor: Color,
)

object ProfileAvatars {
    val ALL = listOf(
        ProfileAvatar(
            id = "avatar_1",
            name = "Kırmızı Kahraman",
            icon = Icons.Filled.Face,
            gradient = Brush.linearGradient(listOf(Color(0xFFE50914), Color(0xFF7A070E))),
            primaryColor = Color(0xFFE50914),
        ),
        ProfileAvatar(
            id = "avatar_2",
            name = "Gece Mavisi",
            icon = Icons.Filled.Movie,
            gradient = Brush.linearGradient(listOf(Color(0xFF0072D2), Color(0xFF002952))),
            primaryColor = Color(0xFF0072D2),
        ),
        ProfileAvatar(
            id = "avatar_3",
            name = "Altın Yıldız",
            icon = Icons.Filled.Star,
            gradient = Brush.linearGradient(listOf(Color(0xFFFFB800), Color(0xFF8C6400))),
            primaryColor = Color(0xFFFFB800),
        ),
        ProfileAvatar(
            id = "avatar_4",
            name = "Zümrüt Yeşil",
            icon = Icons.Filled.EmojiEmotions,
            gradient = Brush.linearGradient(listOf(Color(0xFF00B14F), Color(0xFF00441E))),
            primaryColor = Color(0xFF00B14F),
        ),
        ProfileAvatar(
            id = "avatar_5",
            name = "Neon Mor",
            icon = Icons.Filled.VideogameAsset,
            gradient = Brush.linearGradient(listOf(Color(0xFF9C27B0), Color(0xFF430E52))),
            primaryColor = Color(0xFF9C27B0),
        ),
        ProfileAvatar(
            id = "avatar_6",
            name = "Çocuk Roket",
            icon = Icons.Filled.RocketLaunch,
            gradient = Brush.linearGradient(listOf(Color(0xFFFF5722), Color(0xFF942907))),
            primaryColor = Color(0xFFFF5722),
        ),
        ProfileAvatar(
            id = "avatar_7",
            name = "Sevimli Dost",
            icon = Icons.Filled.Pets,
            gradient = Brush.linearGradient(listOf(Color(0xFF00BCD4), Color(0xFF005662))),
            primaryColor = Color(0xFF00BCD4),
        ),
        ProfileAvatar(
            id = "avatar_8",
            name = "Minik Bebek",
            icon = Icons.Filled.ChildCare,
            gradient = Brush.linearGradient(listOf(Color(0xFFE91E63), Color(0xFF6B0B2E))),
            primaryColor = Color(0xFFE91E63),
        ),
    )

    fun get(id: String?): ProfileAvatar = ALL.find { it.id == id } ?: ALL.first()
}
