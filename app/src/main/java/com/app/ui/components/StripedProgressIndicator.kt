package com.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun StripedProgressIndicator(
    progress: Float,
    color: Color,
    trackColor: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Canvas(modifier = modifier.clipToBounds()) {
        val width = size.width
        val height = size.height
        val progressWidth = width * progress

        // Draw track
        drawRoundRect(
            color = trackColor,
            size = Size(width, height),
            cornerRadius = CornerRadius(height / 2, height / 2)
        )

        // Draw progress
        if (progress > 0) {
            clipPath(
                Path().apply {
                    addRoundRect(
                        RoundRect(
                            0f, 0f, progressWidth, height,
                            CornerRadius(height / 2, height / 2)
                        )
                    )
                }
            ) {
                // Draw base progress color
                drawRoundRect(
                    color = color,
                    size = Size(progressWidth, height),
                    cornerRadius = CornerRadius(height / 2, height / 2)
                )

                // Draw stripes
                val stripeWidth = height * 1.5f
                val gap = height * 1.5f
                val totalStripe = stripeWidth + gap
                // Start drawing stripes from left to right
                val startOffset = -offset * totalStripe

                var x = startOffset
                while (x < progressWidth + height * 2) {
                    val path = Path().apply {
                        moveTo(x, height)
                        lineTo(x + stripeWidth, height)
                        lineTo(x + stripeWidth + height, 0f)
                        lineTo(x + height, 0f)
                        close()
                    }
                    drawPath(path, Color.White.copy(alpha = 0.3f))
                    x += totalStripe
                }
            }
        }
    }
}
