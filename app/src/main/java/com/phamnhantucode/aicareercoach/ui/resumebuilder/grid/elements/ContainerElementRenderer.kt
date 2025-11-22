package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.GridUtils
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*

@Composable
fun ContainerElementRenderer(
    element: ResumeElement.ContainerElement,
    modifier: Modifier = Modifier,
    hoverProgress: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundColor = element.style.backgroundColor?.let { Color(it) } ?: Color.Transparent
    val borderColor = element.style.borderColor?.let { Color(it) }
    
    val baseModifier = modifier
        .fillMaxSize()
        .then(
            if (backgroundColor != Color.Transparent) {
                Modifier.background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(element.style.borderRadius.dp)
                )
            } else {
                Modifier
            }
        )
        .then(
            if (borderColor != null && element.style.borderWidth > 0) {
                Modifier.border(
                    width = element.style.borderWidth.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(element.style.borderRadius.dp)
                )
            } else {
                Modifier
            }
        )
        .then(
            if (element.clipContent) {
                Modifier.clip(RoundedCornerShape(element.style.borderRadius.dp))
            } else {
                Modifier
            }
        )
    
    Box(modifier = baseModifier) {
        // Render content (children)
        content()

        // Hover progress overlay - shows when dragging element over this container
        if (hoverProgress > 0f) {
            HoverProgressOverlay(
                progress = hoverProgress,
                cornerRadius = element.style.borderRadius,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * Container renderer with vertical layout mode support
 */
@Composable
fun ContainerElementRendererWithLayout(
    element: ResumeElement.ContainerElement,
    modifier: Modifier = Modifier,
    hoverProgress: Float = 0f,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = element.style.backgroundColor?.let { Color(it) } ?: Color.Transparent
    val borderColor = element.style.borderColor?.let { Color(it) }
    
    val baseModifier = modifier
        .fillMaxSize()
        .then(
            if (backgroundColor != Color.Transparent) {
                Modifier.background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(element.style.borderRadius.dp)
                )
            } else {
                Modifier
            }
        )
        .then(
            if (borderColor != null && element.style.borderWidth > 0) {
                Modifier.border(
                    width = element.style.borderWidth.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(element.style.borderRadius.dp)
                )
            } else {
                Modifier
            }
        )
        .then(
            if (element.clipContent) {
                Modifier.clip(RoundedCornerShape(element.style.borderRadius.dp))
            } else {
                Modifier
            }
        )
    
    Box(modifier = baseModifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(element.padding.toDp()),
            verticalArrangement = Arrangement.Top
        ) {
            content()
        }

        // Hover progress overlay - shows when dragging element over this container
        if (hoverProgress > 0f) {
            HoverProgressOverlay(
                progress = hoverProgress,
                cornerRadius = element.style.borderRadius,
                modifier = Modifier.matchParentSize()
            )
        }
    }
}

/**
 * Convert Padding to Compose Dp values
 */
private fun Padding.toDp(): androidx.compose.foundation.layout.PaddingValues {
    return androidx.compose.foundation.layout.PaddingValues(
        start = this.left.dp,
        top = this.top.dp,
        end = this.right.dp,
        bottom = this.bottom.dp
    )
}

/**
 * Animated border overlay that shows progress towards auto-add to container
 */
@Composable
private fun HoverProgressOverlay(
    progress: Float,
    cornerRadius: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 4.dp.toPx()

        // Animated border color - increases brightness and opacity with progress
        // Start: Subtle blue (30% opacity)
        // Middle: Brighter blue (50% opacity)
        // End: Full brightness (90% opacity) with pulsing glow
        val baseAlpha = 0.3f + (progress * 0.6f)
        val borderColor = Color(0xFF2196F3).copy(alpha = baseAlpha)

        // Draw main animated border
        drawRoundRect(
            color = borderColor,
            cornerRadius = CornerRadius(cornerRadius * 2, cornerRadius * 2),
            style = Stroke(width = strokeWidth)
        )

        // Add inner glow effect at high progress (70-100%)
        if (progress > 0.7f) {
            val glowProgress = (progress - 0.7f) / 0.3f  // 0 to 1 in the 70-100% range
            val glowAlpha = glowProgress * 0.25f

            // Pulsing effect at completion
            val pulseAlpha = if (progress >= 0.95f) {
                glowAlpha + ((progress - 0.95f) / 0.05f) * 0.15f
            } else {
                glowAlpha
            }

            drawRoundRect(
                color = Color(0xFF2196F3).copy(alpha = pulseAlpha),
                cornerRadius = CornerRadius(cornerRadius * 2, cornerRadius * 2),
                style = Stroke(width = strokeWidth * 2.5f)
            )
        }

        // Background tint overlay at high progress
        if (progress > 0.5f) {
            val tintAlpha = ((progress - 0.5f) / 0.5f) * 0.05f
            drawRoundRect(
                color = Color(0xFF2196F3).copy(alpha = tintAlpha),
                cornerRadius = CornerRadius(cornerRadius * 2, cornerRadius * 2)
            )
        }
    }
}
