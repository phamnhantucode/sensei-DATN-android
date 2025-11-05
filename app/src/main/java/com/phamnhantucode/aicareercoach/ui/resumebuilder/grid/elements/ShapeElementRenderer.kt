package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ShapeType

/**
 * Renders a shape element on the resume
 * Supports rectangles, circles, lines, and dividers
 */
@Composable
fun ShapeElementRenderer(
    element: ResumeElement.ShapeElement,
    modifier: Modifier = Modifier
) {
    val backgroundColor = element.style.backgroundColor?.let { Color(it) } ?: Color.Black
    val borderColor = element.style.borderColor?.let { Color(it) }
    val shape = element.shapeType.toShape(element.cornerRadius)

    Box(
        modifier = modifier
            .then(
                when (element.shapeType) {
                    ShapeType.LINE, ShapeType.DIVIDER -> {
                        // Lines and dividers should only fill width
                        Modifier.fillMaxWidth()
                    }
                    else -> {
                        // Other shapes fill the entire space
                        Modifier.fillMaxSize()
                    }
                }
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
