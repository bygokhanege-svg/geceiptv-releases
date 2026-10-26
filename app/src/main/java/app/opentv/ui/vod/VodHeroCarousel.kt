/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.vod

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Common data model representing a featured hero item in the top carousel.
 */
data class VodHeroItem(
    val id: Long,
    val title: String,
    val backdropUrl: String?,
    val posterUrl: String?,
    val rating: Double?,
    val year: Int?,
    val genre: String?,
    val plot: String?,
    val isSeries: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * Cinema-grade featured Hero Carousel / Slider for Movies and Series.
 * Displays large backdrop images with smooth gradient shading, rating, plot summary,
 * action buttons, auto-rotation timer, and manual touch/D-pad navigation.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VodHeroCarousel(
    items: List<VodHeroItem>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return

    val isPhone = LocalConfiguration.current.screenWidthDp < 600
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    var isUserInteracting by remember { mutableStateOf(false) }

    // Auto-advance every 6 seconds unless user is dragging or interacting
    LaunchedEffect(isDragged, isUserInteracting, items.size) {
        if (!isDragged && !isUserInteracting && items.size > 1) {
            while (true) {
                delay(6000)
                val nextPage = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(
                    nextPage,
                    animationSpec = tween(durationMillis = 650),
                )
            }
        }
    }

    val bannerHeight = if (isPhone) 240.dp else 340.dp
    val bannerShape = RoundedCornerShape(if (isPhone) 16.dp else 22.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(bannerHeight)
            .padding(horizontal = if (isPhone) 4.dp else 8.dp, vertical = 4.dp),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .clip(bannerShape),
        ) { page ->
            val item = items[page]
            VodHeroSlide(
                item = item,
                isPhone = isPhone,
                onFocusActive = { isUserInteracting = it },
            )
        }

        // Navigation Arrows (Manual click for convenience)
        if (items.size > 1 && !isPhone) {
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val prev = if (pagerState.currentPage > 0) pagerState.currentPage - 1 else items.size - 1
                        pagerState.animateScrollToPage(prev)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Önceki",
                    tint = Color.White,
                )
            }

            IconButton(
                onClick = {
                    coroutineScope.launch {
                        val next = (pagerState.currentPage + 1) % items.size
                        pagerState.animateScrollToPage(next)
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .size(38.dp)
                    .background(Color.Black.copy(alpha = 0.45f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Sonraki",
                    tint = Color.White,
                )
            }
        }

        // Dot indicators at bottom
        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .align(if (isPhone) Alignment.BottomCenter else Alignment.BottomEnd)
                    .padding(end = if (isPhone) 0.dp else 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue = if (isSelected) 22.dp else 7.dp,
                        animationSpec = tween(300),
                        label = "dotWidth",
                    )
                    val dotColor by animateColorAsState(
                        targetValue = if (isSelected) Color(0xFFFFB800) else Color.White.copy(alpha = 0.35f),
                        animationSpec = tween(300),
                        label = "dotColor",
                    )

                    Box(
                        modifier = Modifier
                            .height(6.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(dotColor)
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun VodHeroSlide(
    item: VodHeroItem,
    isPhone: Boolean,
    onFocusActive: (Boolean) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val imageUrl = item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C1017))
            .onFocusChanged {
                isFocused = it.isFocused
                onFocusActive(it.isFocused)
            }
            .border(
                width = if (isFocused) 2.5.dp else 1.dp,
                color = if (isFocused) Color(0xFFFFB800) else Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(if (isPhone) 16.dp else 22.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = item.onClick,
            ),
    ) {
        // High-res Background Image
        AsyncImage(
            model = imageUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Luxury Multi-Stop Gradient Scrim (Bottom & Left)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x3006090E),
                            Color(0xB006090E),
                            Color(0xFA06090E),
                        ),
                        startY = 0f,
                    ),
                ),
        )

        if (!isPhone) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xF206090E),
                                Color(0xBB06090E),
                                Color(0x4006090E),
                                Color.Transparent,
                            ),
                            startX = 0f,
                            endX = 900f,
                        ),
                    ),
            )
        }

        // Text & Action Overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isPhone) 14.dp else 28.dp,
                    end = if (isPhone) 14.dp else 180.dp,
                    top = if (isPhone) 12.dp else 20.dp,
                    bottom = if (isPhone) 14.dp else 20.dp,
                ),
            verticalArrangement = Arrangement.Bottom,
        ) {
            // Badges row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // "Öne Çıkan" tag badge
                Surface(
                    color = Color(0xFFFFB800).copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFB800).copy(alpha = 0.5f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = Color(0xFFFFB800),
                            modifier = Modifier.size(11.dp),
                        )
                        Text(
                            text = if (item.isSeries) "ÖNE ÇIKAN DİZİ" else "ÖNE ÇIKAN FİLM",
                            color = Color(0xFFFFB800),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                        )
                    }
                }

                // Rating badge
                if (item.rating != null && item.rating > 0.0) {
                    Surface(
                        color = Color(0xFF1E2638).copy(alpha = 0.85f),
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(1.dp, Color(0xFF384666)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = "⭐",
                                fontSize = 10.sp,
                            )
                            Text(
                                text = String.format(Locale.US, "%.1f", item.rating),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                // Year
                if (item.year != null && item.year > 0) {
                    Text(
                        text = item.year.toString(),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Genre
                if (!item.genre.isNullOrBlank()) {
                    Text(
                        text = "•",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 11.sp,
                    )
                    Text(
                        text = item.genre.split(",", "|", "/").firstOrNull()?.trim() ?: item.genre,
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Title
            Text(
                text = item.title,
                color = Color.White,
                fontSize = if (isPhone) 18.sp else 24.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = if (isPhone) 1 else 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = if (isPhone) 22.sp else 28.sp,
            )

            // Plot / Summary (Özet)
            val plotText = item.plot?.trim().takeIf { !it.isNullOrBlank() }
                ?: if (item.isSeries) "Sezonları ve bölümleri keşfetmek için tıklayın."
                else "Detayları görüntülemek ve izlemek için tıklayın."

            Spacer(Modifier.height(4.dp))
            Text(
                text = plotText,
                color = Color.White.copy(alpha = 0.82f),
                fontSize = if (isPhone) 11.5.sp else 13.sp,
                lineHeight = if (isPhone) 15.sp else 18.sp,
                maxLines = if (isPhone) 2 else 3,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(if (isPhone) 8.dp else 12.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = item.onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB800),
                        contentColor = Color.Black,
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(
                        horizontal = if (isPhone) 12.dp else 18.dp,
                        vertical = if (isPhone) 6.dp else 9.dp,
                    ),
                    modifier = Modifier.height(if (isPhone) 32.dp else 38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(if (isPhone) 15.dp else 18.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (item.isSeries) "Bölümleri Aç" else "Hemen İzle",
                        fontWeight = FontWeight.Bold,
                        fontSize = if (isPhone) 11.5.sp else 13.sp,
                        maxLines = 1,
                    )
                }

                OutlinedButton(
                    onClick = item.onClick,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(
                        horizontal = if (isPhone) 10.dp else 16.dp,
                        vertical = if (isPhone) 6.dp else 9.dp,
                    ),
                    modifier = Modifier.height(if (isPhone) 32.dp else 38.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(if (isPhone) 14.dp else 16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Detay",
                        fontWeight = FontWeight.Medium,
                        fontSize = if (isPhone) 11.5.sp else 13.sp,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
