package com.phamnhantucode.aicareercoach.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush

/**
 * A reusable shimmer loading effect component.
 *
 * @param modifier The modifier to apply to the shimmer box
 * @param shimmerProgress The animation progress value (0f to 1f) from an infinite transition
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shimmerProgress: Float
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    Box(
        modifier = modifier
            .background(
                brush = Brush.linearGradient(
                    colors = shimmerColors,
                    start = Offset(shimmerProgress * 1000f - 500f, 0f),
                    end = Offset(shimmerProgress * 1000f, 0f)
                )
            )
    )
}
