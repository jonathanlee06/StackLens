package com.devbyjonathan.stacklens.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.devbyjonathan.uikit.theme.scheme

/**
 * Neutral shimmering placeholder block used by skeleton-loading screens.
 * Sweeps a linear-gradient highlight across a dimmed base fill to suggest
 * content is coming. Sized and shaped by the caller.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-progress",
    )
    val base = scheme.onSurface.copy(alpha = 0.07f)
    val highlight = scheme.onSurface.copy(alpha = 0.18f)
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(base)
                val w = size.width
                if (w <= 0f) return@drawBehind
                val sweep = w * 0.6f
                val startX = progress * (w + sweep) - sweep
                val brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, highlight, Color.Transparent),
                    start = Offset(startX, 0f),
                    end = Offset(startX + sweep, 0f),
                )
                drawRect(brush)
            },
    )
}
