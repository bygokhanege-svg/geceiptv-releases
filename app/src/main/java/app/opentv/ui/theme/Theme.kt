/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import app.opentv.core.AppSettings

/**
 * GECE İPTV Player 3-Renkli Resmi Tasarım Paleti.
 * 1. Zemin (Canvas): Derin Obsidyen Siyah
 * 2. Kart/Yüzey (Surface): Grafit Yüzey & Mikro Çerçeve
 * 3. Vurgu (Accent): Asil Kehribar Altın
 * Zıt Renkler: Koyu üzerinde kar beyazı; altın butonlarda simsiyah metin.
 */
object GecePalette {
    val CanvasBg = Color(0xFF090A0F)        // 1. Zemin (Obsidyen Gece Siyahı)
    val CardSurface = Color(0xFF131722)     // 2. Yüzey/Kart (Grafit)
    val CardBorder = Color(0xFF22283A)      // 2. Alt Çerçeve
    val CardElevated = Color(0xFF1A1F2E)    // 2. Alt Kabarık Yüzey
    val Accent = Color(0xFFF59E0B)          // 3. Vurgu (Asil Kehribar Altın)
    val AccentSubtle = Color(0x26F59E0B)    // 3. Alt Hafif Vurgu (%15)
    val TextPrimary = Color(0xFFFFFFFF)     // Kar Beyazı Kristal Metin
    val TextSecondary = Color(0xFF94A3B8)   // Gümüş/Arduvaz İkincil Metin
    val OnAccent = Color(0xFF000000)        // Altın Üzerine Simsiyah Zıt Metin
}

/**
 * Builds high-contrast, modern ColorScheme for TV and Mobile.
 * Preserves video stabilization by dynamically changing color tokens without recreating Activity.
 */
fun buildColorScheme(darkTheme: Boolean, colorTheme: AppSettings.AppColorTheme): ColorScheme {
    return if (darkTheme) {
        when (colorTheme) {
            AppSettings.AppColorTheme.OBSIDIAN -> darkColorScheme(
                primary = GecePalette.Accent,
                onPrimary = GecePalette.OnAccent,
                primaryContainer = GecePalette.AccentSubtle,
                onPrimaryContainer = GecePalette.Accent,
                secondary = GecePalette.Accent,
                onSecondary = GecePalette.OnAccent,
                background = GecePalette.CanvasBg,
                onBackground = GecePalette.TextPrimary,
                surface = GecePalette.CardSurface,
                onSurface = GecePalette.TextPrimary,
                surfaceVariant = GecePalette.CardElevated,
                onSurfaceVariant = GecePalette.TextSecondary,
                outline = GecePalette.CardBorder,
                error = Color(0xFFEF4444),
                onError = Color(0xFF1E0A0A),
            )
            AppSettings.AppColorTheme.OCEAN -> darkColorScheme(
                primary = Color(0xFF06B6D4),
                onPrimary = Color(0xFF04212C),
                primaryContainer = Color(0xFF0E7490),
                onPrimaryContainer = Color(0xFFCFFAFE),
                secondary = Color(0xFF22D3EE),
                onSecondary = Color(0xFF083344),
                background = Color(0xFF07131D),
                onBackground = Color(0xFFF0FDF4),
                surface = Color(0xFF0E2033),
                onSurface = Color(0xFFF0FDF4),
                surfaceVariant = Color(0xFF162C46),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF1E3A5F),
                error = Color(0xFFEF4444),
                onError = Color(0xFF1E0A0A),
            )
            AppSettings.AppColorTheme.EMERALD -> darkColorScheme(
                primary = Color(0xFF10B981),
                onPrimary = Color(0xFF022C22),
                primaryContainer = Color(0xFF059669),
                onPrimaryContainer = Color(0xFFD1FAE5),
                secondary = Color(0xFF34D399),
                onSecondary = Color(0xFF064E3B),
                background = Color(0xFF06150F),
                onBackground = Color(0xFFF2FBF7),
                surface = Color(0xFF0C241B),
                onSurface = Color(0xFFF2FBF7),
                surfaceVariant = Color(0xFF143528),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF1B4736),
                error = Color(0xFFEF4444),
                onError = Color(0xFF1E0A0A),
            )
            AppSettings.AppColorTheme.AMETHYST -> darkColorScheme(
                primary = Color(0xFFA855F7),
                onPrimary = Color(0xFF2E0854),
                primaryContainer = Color(0xFF7E22CE),
                onPrimaryContainer = Color(0xFFF3E8FF),
                secondary = Color(0xFFC084FC),
                onSecondary = Color(0xFF3B0764),
                background = Color(0xFF100A1C),
                onBackground = Color(0xFFFAF5FF),
                surface = Color(0xFF1C122F),
                onSurface = Color(0xFFFAF5FF),
                surfaceVariant = Color(0xFF281B43),
                onSurfaceVariant = Color(0xFFA8A29E),
                outline = Color(0xFF3B2860),
                error = Color(0xFFEF4444),
                onError = Color(0xFF1E0A0A),
            )
            AppSettings.AppColorTheme.AMBER -> darkColorScheme(
                primary = Color(0xFFF59E0B),
                onPrimary = Color(0xFF331F00),
                primaryContainer = Color(0xFFB45309),
                onPrimaryContainer = Color(0xFFFEF3C7),
                secondary = Color(0xFFFBBF24),
                onSecondary = Color(0xFF451A03),
                background = Color(0xFF161008),
                onBackground = Color(0xFFFFFBEB),
                surface = Color(0xFF251A0E),
                onSurface = Color(0xFFFFFBEB),
                surfaceVariant = Color(0xFF352615),
                onSurfaceVariant = Color(0xFFA8A29E),
                outline = Color(0xFF4B361E),
                error = Color(0xFFEF4444),
                onError = Color(0xFF1E0A0A),
            )
            AppSettings.AppColorTheme.STEALTH -> darkColorScheme(
                primary = Color(0xFF94A3B8),
                onPrimary = Color(0xFF020617),
                primaryContainer = Color(0xFF475569),
                onPrimaryContainer = Color(0xFFF1F5F9),
                secondary = Color(0xFFCBD5E1),
                onSecondary = Color(0xFF0F172A),
                background = Color(0xFF000000),
                onBackground = Color(0xFFF8FAFC),
                surface = Color(0xFF121316),
                onSurface = Color(0xFFF8FAFC),
                surfaceVariant = Color(0xFF1C1E24),
                onSurfaceVariant = Color(0xFF94A3B8),
                outline = Color(0xFF2E323B),
                error = Color(0xFFEF4444),
                onError = Color(0xFF1E0A0A),
            )
        }
    } else {
        when (colorTheme) {
            AppSettings.AppColorTheme.OBSIDIAN -> lightColorScheme(
                primary = Color(0xFF4F46E5),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEEF2FF),
                onPrimaryContainer = Color(0xFF312E81),
                secondary = Color(0xFF4F46E5),
                onSecondary = Color.White,
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFF1F5F9),
                onSurfaceVariant = Color(0xFF64748B),
                outline = Color(0xFFE2E8F0),
                error = Color(0xFFDC2626),
                onError = Color.White,
            )
            AppSettings.AppColorTheme.OCEAN -> lightColorScheme(
                primary = Color(0xFF0891B2),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFECFEFF),
                onPrimaryContainer = Color(0xFF164E63),
                secondary = Color(0xFF06B6D4),
                onSecondary = Color.White,
                background = Color(0xFFF0F9FF),
                onBackground = Color(0xFF082F49),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF082F49),
                surfaceVariant = Color(0xFFE0F2FE),
                onSurfaceVariant = Color(0xFF475569),
                outline = Color(0xFFBAE6FD),
                error = Color(0xFFDC2626),
                onError = Color.White,
            )
            AppSettings.AppColorTheme.EMERALD -> lightColorScheme(
                primary = Color(0xFF059669),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFECFDF5),
                onPrimaryContainer = Color(0xFF064E3B),
                secondary = Color(0xFF10B981),
                onSecondary = Color.White,
                background = Color(0xFFF0FDF4),
                onBackground = Color(0xFF062D1F),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF062D1F),
                surfaceVariant = Color(0xFFDCFCE7),
                onSurfaceVariant = Color(0xFF475569),
                outline = Color(0xFFA7F3D0),
                error = Color(0xFFDC2626),
                onError = Color.White,
            )
            AppSettings.AppColorTheme.AMETHYST -> lightColorScheme(
                primary = Color(0xFF9333EA),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFAF5FF),
                onPrimaryContainer = Color(0xFF581C87),
                secondary = Color(0xFFA855F7),
                onSecondary = Color.White,
                background = Color(0xFFFAF5FF),
                onBackground = Color(0xFF3B0764),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF3B0764),
                surfaceVariant = Color(0xFFF3E8FF),
                onSurfaceVariant = Color(0xFF6B7280),
                outline = Color(0xFFE9D5FF),
                error = Color(0xFFDC2626),
                onError = Color.White,
            )
            AppSettings.AppColorTheme.AMBER -> lightColorScheme(
                primary = Color(0xFFD97706),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFFFFBEB),
                onPrimaryContainer = Color(0xFF78350F),
                secondary = Color(0xFFF59E0B),
                onSecondary = Color.White,
                background = Color(0xFFFFFDF5),
                onBackground = Color(0xFF451A03),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF451A03),
                surfaceVariant = Color(0xFFFEF3C7),
                onSurfaceVariant = Color(0xFF78716C),
                outline = Color(0xFFFDE68A),
                error = Color(0xFFDC2626),
                onError = Color.White,
            )
            AppSettings.AppColorTheme.STEALTH -> lightColorScheme(
                primary = Color(0xFF475569),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFF1F5F9),
                onPrimaryContainer = Color(0xFF0F172A),
                secondary = Color(0xFF64748B),
                onSecondary = Color.White,
                background = Color(0xFFF8FAFC),
                onBackground = Color(0xFF0F172A),
                surface = Color(0xFFFFFFFF),
                onSurface = Color(0xFF0F172A),
                surfaceVariant = Color(0xFFE2E8F0),
                onSurfaceVariant = Color(0xFF64748B),
                outline = Color(0xFFCBD5E1),
                error = Color(0xFFDC2626),
                onError = Color.White,
            )
        }
    }
}

/**
 * Razor-sharp, compact typography tuned for 10-foot and mobile readability.
 * Uses refined font sizes, tight line heights, and negative letter-spacing for headers,
 * plus positive letter-spacing for micro labels so small text remains crisp and readable.
 */
private val OpenTvTypography = Typography(
    displaySmall = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
        lineHeight = 32.sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
        lineHeight = 26.sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
        lineHeight = 22.sp,
    ),
    titleLarge = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
        lineHeight = 20.sp,
    ),
    titleMedium = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 18.sp,
    ),
    titleSmall = TextStyle(
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.1.sp,
        lineHeight = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontSize = 13.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
        lineHeight = 18.sp,
    ),
    bodyMedium = TextStyle(
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.1.sp,
        lineHeight = 16.sp,
    ),
    bodySmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.2.sp,
        lineHeight = 14.sp,
    ),
    labelLarge = TextStyle(
        fontSize = 11.5.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp,
        lineHeight = 16.sp,
    ),
    labelMedium = TextStyle(
        fontSize = 10.5.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.25.sp,
        lineHeight = 14.sp,
    ),
    labelSmall = TextStyle(
        fontSize = 9.5.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.3.sp,
        lineHeight = 13.sp,
    ),
)

@Composable
fun OpenTvTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorTheme: AppSettings.AppColorTheme = AppSettings.AppColorTheme.OBSIDIAN,
    content: @Composable () -> Unit,
) {
    val colorScheme = remember(darkTheme, colorTheme) {
        buildColorScheme(darkTheme, colorTheme)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = OpenTvTypography,
        content = content,
    )
}
