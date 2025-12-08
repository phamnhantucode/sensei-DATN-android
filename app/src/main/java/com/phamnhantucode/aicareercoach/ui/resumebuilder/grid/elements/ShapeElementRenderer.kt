package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.DividerOrientation
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.Padding
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ShapeType

/**
 * Renders a shape element on the resume
 * Supports rectangles, circles, lines, and dividers
 */
@Composable
fun ShapeElementRenderer(
    element: ResumeElement.ShapeElement,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    // Determine background color
    // For DIVIDER: if selected, use selection color (Blue), otherwise use element color with borderColor fallback
    val backgroundColor = if (element.shapeType == ShapeType.DIVIDER && isSelected) {
        Color(0xFF2196F3) // Selection Blue
    } else {
        val colorValue = element.style.backgroundColor ?: element.style.borderColor
        colorValue?.let { Color(it) } ?: Color.Transparent
    }
    
    val borderColor = element.style.borderColor?.let { Color(it) }
    val shape = element.shapeType.toShape(element.cornerRadius)

    Box(
        modifier = modifier
            .then(
                when (element.shapeType) {
                    ShapeType.LINE, ShapeType.DIVIDER -> {
                        // Lines and dividers support both horizontal and vertical orientation
                        when (element.orientation) {
                            DividerOrientation.HORIZONTAL -> {
                                // Horizontal: customHeightDp = thickness, customWidthDp = width (or fill if null)
                                val thickness = element.customHeightDp?.dp ?: 2.dp
                                if (element.customWidthDp != null) {
                                    Modifier.width(element.customWidthDp.dp).height(thickness)
                                } else {
                                    Modifier.fillMaxWidth().height(thickness)
                                }
                            }
                            DividerOrientation.VERTICAL -> {
                                // Vertical: customWidthDp = thickness, customHeightDp = height (or fill if null)
                                val thickness = element.customWidthDp?.dp ?: 2.dp
                                if (element.customHeightDp != null) {
                                    Modifier.width(thickness).height(element.customHeightDp.dp)
                                } else {
                                    Modifier.fillMaxHeight().width(thickness)
                                }
                            }
                        }
                    }
                    ShapeType.CIRCLE -> {
                        // Circles should maintain 1:1 aspect ratio
                        // Use the smaller dimension to ensure a perfect circle
                        Modifier.fillMaxSize()
                            .aspectRatio(1f, matchHeightConstraintsFirst = true)
                    }
                    else -> {
                        // Other shapes fill the entire space
                        Modifier.fillMaxSize()
                    }
                }
            )
            .padding(
                start = (element.padding?.left ?: 0f).dp,
                top = (element.padding?.top ?: 0f).dp,
                end = (element.padding?.right ?: 0f).dp,
                bottom = (element.padding?.bottom ?: 0f).dp
            )
            .then(
                if (element.style.shadowBlur > 0) {
                    Modifier.shadow(
                        elevation = element.style.shadowBlur.dp,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
            .background(
                color = backgroundColor,
                shape = shape
            )
            .then(
                if (borderColor != null && element.style.borderWidth > 0) {
                    Modifier.border(
                        width = element.style.borderWidth.dp,
                        color = borderColor,
                        shape = shape
                    )
                } else {
                    Modifier
                }
            )
    )
}

/**
 * Converts shape type to Compose Shape
 */
private fun ShapeType.toShape(cornerRadius: Float): Shape {
    return when (this) {
        ShapeType.RECTANGLE -> {
            if (cornerRadius > 0) {
                RoundedCornerShape(cornerRadius.dp)
            } else {
                RoundedCornerShape(0.dp)
            }
        }
        ShapeType.CIRCLE -> CircleShape
        ShapeType.LINE, ShapeType.DIVIDER -> RoundedCornerShape(0.dp)
    }
}
