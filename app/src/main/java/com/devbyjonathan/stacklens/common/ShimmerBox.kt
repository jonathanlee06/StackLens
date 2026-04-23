package com.devbyjonathan.stacklens.common

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.ui.geometry.CornerRadius
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
    val base = scheme.onSurface.copy(alpha = 0.07f)
    val highlight = scheme.onSurface.copy(alpha = 0.18f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateColor(
        initialValue = base,
        targetValue = highlight,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shimmer-progress",
    )
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRoundRect(
                    color = progress,
                    cornerRadius = CornerRadius(x = 4f, y = 4f),
                )
            },
    )
}
