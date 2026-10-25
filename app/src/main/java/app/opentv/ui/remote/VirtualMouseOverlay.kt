/*
 * This file is part of OpenTV.
 * Copyright (C) 2026 The OpenTV Contributors
 * Licensed under the GNU General Public License v3.0 or later.
 */
package app.opentv.ui.remote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.opentv.pairing.VirtualMouseServer

/**
 * Hardware-accelerated GPU overlay rendering the TV's virtual mouse pointer.
 * Uses graphicsLayer translationX/Y to achieve 60-120fps motion with zero screen recomposition.
 */
@Composable
fun VirtualMouseOverlay(server: VirtualMouseServer) {
    val isVisible by server.isPointerVisible.collectAsState()
    val pointerX by server.pointerX.collectAsState()
    val pointerY by server.pointerY.collectAsState()
    val isClicking by server.isClicking.collectAsState()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.fillMaxSize().zIndex(99999f),
    ) {
        Box(modifier = Modifier.fillMaxSize().zIndex(99999f)) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationX = pointerX
                        translationY = pointerY
                    }
                    .size(32.dp)
            ) {
                // Click wave animation
                if (isClicking) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(app.opentv.ui.theme.GecePalette.AccentSubtle)
                            .border(2.dp, app.opentv.ui.theme.GecePalette.Accent, CircleShape)
                    )
                }

                // Sharp Canvas Mouse Arrow Cursor
                Canvas(modifier = Modifier.size(26.dp)) {
                    val w = size.width
                    val h = size.height

                    val arrowPath = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w * 0.72f, h * 0.72f)
                        lineTo(w * 0.42f, h * 0.72f)
                        lineTo(w * 0.62f, h * 0.98f)
                        lineTo(w * 0.46f, h * 1.0f)
                        lineTo(w * 0.26f, h * 0.74f)
                        lineTo(0f, h * 0.92f)
                        close()
                    }

                    // Black drop shadow / border
                    drawPath(
                        path = arrowPath,
                        color = Color.Black,
                        style = Stroke(width = 2.5f.dp.toPx())
                    )
                    // Vibrant clean white fill
                    drawPath(
                        path = arrowPath,
                        color = Color.White
                    )
                }
            }
        }
    }
}
