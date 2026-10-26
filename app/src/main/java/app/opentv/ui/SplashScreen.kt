/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.opentv.R
import app.opentv.ui.theme.GecePalette
import kotlinx.coroutines.delay

/**
 * GECE İPTV Player - Sinematik ve Lüks Açılış (Splash) Ekranı.
 * Televizyon, Telefon ve Tablet ekranlarına tam duyarlı (responsive),
 * 3-renkli kurumsal palet (Obsidyen Siyah, Grafit, Asil Kehribar Altın) ile tasarlanmıştır.
 */
@Composable
fun SplashScreen(
    isTelevision: Boolean,
    isLoaded: Boolean,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isTablet = screenWidthDp >= 600 && !isTelevision
    val isTv = isTelevision || screenWidthDp >= 960

    // Responsif boyutlandırmalar
    val logoSize = when {
        isTv -> 116.dp
        isTablet -> 100.dp
        else -> 84.dp
    }
    val logoCorner = when {
        isTv -> 24.dp
        isTablet -> 20.dp
        else -> 18.dp
    }
    val titleSize = when {
        isTv -> 38.sp
        isTablet -> 32.sp
        else -> 28.sp
    }
    val subtitleSize = when {
        isTv -> 13.5.sp
        isTablet -> 12.sp
        else -> 11.sp
    }
    val progressWidth = when {
        isTv -> 280.dp
        isTablet -> 240.dp
        else -> 200.dp
    }

    // Animasyon durumları
    val entranceAlpha = remember { Animatable(0f) }
    val logoScale = remember { Animatable(0.75f) }
    val contentAlpha = remember { Animatable(0f) }
    val progressAnim = remember { Animatable(0f) }

    // Sürekli ortam ışığı ve nabız animasyonları
    val infiniteTransition = rememberInfiniteTransition(label = "SplashAmbient")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowPulse",
    )
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "haloScale",
    )

    var statusText by remember { mutableStateOf("Sistem motoru hazırlanıyor…") }
    var canDismissEarly by remember { mutableStateOf(false) }

    // Giriş animasyonları
    LaunchedEffect(Unit) {
        entranceAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(250)
        contentAlpha.animateTo(1f, animationSpec = tween(600, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        delay(800)
        canDismissEarly = true
    }

    // Yükleme ve ilerleme mantığı
    LaunchedEffect(isLoaded) {
        statusText = "Donanım hızlandırma ve ses motoru başlatılıyor…"
        progressAnim.animateTo(0.35f, animationSpec = tween(600, easing = FastOutSlowInEasing))

        statusText = "Kanal listeleri ve medya veritabanı yükleniyor…"
        progressAnim.animateTo(0.85f, animationSpec = tween(900, easing = FastOutSlowInEasing))

        // Veritabanı ve kaynakların tam yüklenmesini bekle
        while (!isLoaded) {
            delay(50)
        }

        statusText = "Arayüz hazırlandı, hoş geldiniz…"
        progressAnim.animateTo(1.0f, animationSpec = tween(400, easing = FastOutSlowInEasing))
        delay(200)
        onFinish()
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GecePalette.CanvasBg)
            .alpha(entranceAlpha.value)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (canDismissEarly && isLoaded) onFinish()
                },
            )
            .focusable(interactionSource = interactionSource)
            .onKeyEvent {
                if ((it.key == Key.DirectionCenter || it.key == Key.Enter) && canDismissEarly && isLoaded) {
                    onFinish()
                    true
                } else false
            },
        contentAlignment = Alignment.Center,
    ) {
        // 1. Arka Plan Sinematik Altın Ortam Işıması (Radial Glow)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.42f)
            val radius = size.maxDimension * 0.45f * haloScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GecePalette.Accent.copy(alpha = 0.24f * glowPulse),
                        GecePalette.Accent.copy(alpha = 0.08f * glowPulse),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = radius,
                ),
                center = center,
                radius = radius,
            )
        }

        // 2. Ana İçerik Bloğu (Logo, Marka, Rozet, İlerleme Çubuğu)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .alpha(contentAlpha.value),
        ) {
            // Logo & Işıma Halkası
            Box(contentAlignment = Alignment.Center) {
                // Nefes alan dış hale halkası
                Box(
                    modifier = Modifier
                        .size(logoSize + 28.dp)
                        .scale(haloScale)
                        .clip(CircleShape)
                        .background(GecePalette.Accent.copy(alpha = 0.07f * glowPulse))
                        .border(
                            1.dp,
                            GecePalette.Accent.copy(alpha = 0.22f * glowPulse),
                            CircleShape,
                        ),
                )

                // Lüks Logo Taşıyıcısı
                Box(
                    modifier = Modifier
                        .size(logoSize)
                        .scale(logoScale.value)
                        .clip(RoundedCornerShape(logoCorner))
                        .background(
                            Brush.verticalGradient(
                                listOf(GecePalette.CardElevated, GecePalette.CardSurface),
                            ),
                        )
                        .border(
                            2.dp,
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFFFFDF70),
                                    GecePalette.Accent,
                                    Color(0xFFB45309),
                                ),
                            ),
                            RoundedCornerShape(logoCorner),
                        )
                        .padding(if (isTv) 14.dp else 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_opentv_logo),
                        contentDescription = "GECE İPTV",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(logoCorner - 4.dp)),
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTv) 22.dp else 16.dp))

            // Marka Başlığı: GECE İPTV [PRO]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "GECE",
                    color = GecePalette.Accent,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "İPTV",
                    color = GecePalette.TextPrimary,
                    fontSize = titleSize,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                )
                Spacer(modifier = Modifier.width(8.dp))
                // PRO Vurgu Rozeti
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GecePalette.Accent)
                        .padding(horizontal = 7.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "ULTRA",
                        color = GecePalette.OnAccent,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Alt Başlık / Slogan
            Text(
                text = "PREMIUM MEDYA & YAYIN MERKEZİ",
                color = GecePalette.TextSecondary,
                fontSize = subtitleSize,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.5.sp,
            )

            Spacer(modifier = Modifier.height(if (isTv) 16.dp else 12.dp))

            // Cihaz Sürümü Rozeti (TV, Tablet, Mobil)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GecePalette.CardSurface)
                    .border(1.dp, GecePalette.CardBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    Icon(
                        imageVector = if (isTv) Icons.Default.Tv else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = GecePalette.Accent,
                        modifier = Modifier.size(15.dp),
                    )
                    Text(
                        text = when {
                            isTv -> "ANDROİD TV • 4K ULTRA HD"
                            isTablet -> "TABLET EDİSYONU • ÇOKLU EKRAN"
                            else -> "MOBİL EDİSYON • ULTRA HIZLI"
                        },
                        color = GecePalette.Accent,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (isTv) 36.dp else 28.dp))

            // İlerleme Çubuğu (Gold Progress Bar)
            Box(
                modifier = Modifier
                    .width(progressWidth)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GecePalette.CardElevated),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.value)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFFF59E0B),
                                    Color(0xFFFFDF70),
                                    Color(0xFFF59E0B),
                                ),
                            ),
                        ),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dinamik Yükleme Durum Metni
            Text(
                text = statusText,
                color = GecePalette.TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
            )
        }

        // 3. Alt Bilgi (Footer)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isTv) 26.dp else 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "GECE İPTV v0.12.21 • YÜKSEK HIZLI & GÜVENLİ AKIŞ",
                color = GecePalette.TextSecondary.copy(alpha = 0.5f),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
            )
        }
    }
}
