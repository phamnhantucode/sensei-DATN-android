package com.phamnhantucode.colorpicker.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.phamnhantucode.colorpicker.models.HSV
import com.phamnhantucode.colorpicker.utils.toColor
import com.phamnhantucode.colorpicker.utils.toHsv
import kotlin.math.*

/**
 * Optimized HSV Color Wheel component with hue ring and saturation/value triangle.
 *
 * @param modifier Modifier for the composable
 * @param currentColor The currently selected color
 * @param onColorChanged Callback invoked when the color is changed
 */
@Composable
fun OptimizedHsvColorWheel(
    modifier: Modifier = Modifier,
    currentColor: Color,
    onColorChanged: (Color) -> Unit
) {
    val hsv = remember(currentColor) { currentColor.toHsv() }
    var currentHsv by remember { mutableStateOf(hsv) }

    var isDraggingHue by remember { mutableStateOf(false) }
    var isDraggingSV by remember { mutableStateOf(false) }

    // Track if we're actively dragging to avoid external updates during drag
    val isDragging = isDraggingHue || isDraggingSV

    // Update when color changes externally, but preserve hue if not dragging
    // and the color change isn't from our own drag operation
    LaunchedEffect(currentColor, isDragging) {
        if (!isDragging) {
            val newHsv = currentColor.toHsv()
            // Preserve hue if saturation or value is very low (hue becomes meaningless)
            val shouldPreserveHue = newHsv.saturation < 0.01f || newHsv.value < 0.01f
            currentHsv = if (shouldPreserveHue) {
                newHsv.copy(hue = currentHsv.hue)
            } else {
                newHsv
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val distance =
                            sqrt((offset.x - center.x).pow(2) + (offset.y - center.y).pow(2))
                        val outerRadius = size.width / 2f
                        val innerRadius = outerRadius * 0.75f

                        if (distance >= innerRadius * 0.9f && distance <= outerRadius) {
                            isDraggingHue = true
                            isDraggingSV = false

                            val angle = atan2(offset.y - center.y, offset.x - center.x)
                            val hue = ((angle * 180f / PI.toFloat() + 360f) % 360f)
                            currentHsv = currentHsv.copy(hue = hue)
                            onColorChanged(currentHsv.toColor())
                        } else if (distance < innerRadius) {
                            isDraggingSV = true
                            isDraggingHue = false

                            updateSVFromPosition(
                                offset,
                                center,
                                innerRadius,
                                currentHsv
                            )?.let { newHsv ->
                                currentHsv = newHsv
                                onColorChanged(newHsv.toColor())
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val offset = change.position

                        if (isDraggingHue) {
                            val angle = atan2(offset.y - center.y, offset.x - center.x)
                            val hue = ((angle * 180f / PI.toFloat() + 360f) % 360f)
                            currentHsv = currentHsv.copy(hue = hue)
                            onColorChanged(currentHsv.toColor())
                        } else if (isDraggingSV) {
                            val innerRadius = size.width / 2f * 0.75f
                            updateSVFromPosition(
                                offset,
                                center,
                                innerRadius,
                                currentHsv
                            )?.let { newHsv ->
                                currentHsv = newHsv
                                onColorChanged(newHsv.toColor())
                            }
                        }
                    },
                    onDragEnd = {
                        isDraggingHue = false
                        isDraggingSV = false
                    }
                )
            }
    ) {
        val center = this.center
        val outerRadius = size.width / 2f
        val innerRadius = outerRadius * 0.75f

        // Draw hue ring (optimized)
        drawOptimizedHueRing(center, innerRadius, outerRadius)

        // Draw SV triangle
        drawSVTriangle(center, innerRadius, currentHsv.hue)

        // Draw hue indicator
        val hueAngle = currentHsv.hue * PI.toFloat() / 180f
        val hueIndicatorRadius = (innerRadius + outerRadius) / 2f
        val hueIndicatorPos = Offset(
            center.x + hueIndicatorRadius * cos(hueAngle),
            center.y + hueIndicatorRadius * sin(hueAngle)
        )
        drawCircle(
            color = Color.White,
            radius = 10f,
            center = hueIndicatorPos,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color.Black,
            radius = 10f,
            center = hueIndicatorPos,
            style = Stroke(width = 1f)
        )

        // Draw SV indicator
        val svPos = getSVIndicatorPosition(center, innerRadius, currentHsv)
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = svPos,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color.Black,
            radius = 8f,
            center = svPos,
            style = Stroke(width = 1f)
        )
    }
}

/**
 * Draw optimized hue ring using sweep gradient.
 */
private fun DrawScope.drawOptimizedHueRing(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float
) {
    // Create hue spectrum colors
    val hueColors = (0..360 step 10).map { hue ->
        Color.hsv(hue.toFloat(), 1f, 1f)
    }

    // Draw with sweep gradient for smooth rendering
    val path = Path().apply {
        addOval(
            androidx.compose.ui.geometry.Rect(
                left = center.x - outerRadius,
                top = center.y - outerRadius,
                right = center.x + outerRadius,
                bottom = center.y + outerRadius
            )
        )
    }

    drawPath(
        path = path,
        brush = Brush.sweepGradient(
            colors = hueColors,
            center = center
        )
    )

    // Cut out inner circle
    drawCircle(
        color = Color.White,
        radius = innerRadius,
        center = center
    )

    // Draw borders
    drawCircle(
        color = Color.Gray.copy(alpha = 0.3f),
        radius = outerRadius,
        center = center,
        style = Stroke(width = 1f)
    )
    drawCircle(
        color = Color.Gray.copy(alpha = 0.3f),
        radius = innerRadius,
        center = center,
        style = Stroke(width = 1f)
    )
}

/**
 * Draw SV triangle with correct coordinate mapping.
 */
private fun DrawScope.drawSVTriangle(
    center: Offset,
    radius: Float,
    hue: Float
) {
    val hueAngleRad = hue * PI.toFloat() / 180f

    // Calculate triangle vertices
    // Top vertex - represents pure hue color (S=1, V=1)
    val top = Offset(
        center.x + radius * cos(hueAngleRad),
        center.y + radius * sin(hueAngleRad)
    )

    // Bottom left - represents black (S=any, V=0)
    val bottomLeft = Offset(
        center.x + radius * cos(hueAngleRad + 2 * PI.toFloat() / 3),
        center.y + radius * sin(hueAngleRad + 2 * PI.toFloat() / 3)
    )

    // Bottom right - represents white (S=0, V=1)
    val bottomRight = Offset(
        center.x + radius * cos(hueAngleRad - 2 * PI.toFloat() / 3),
        center.y + radius * sin(hueAngleRad - 2 * PI.toFloat() / 3)
    )

    // Create triangle path
    val trianglePath = Path().apply {
        moveTo(top.x, top.y)
        lineTo(bottomLeft.x, bottomLeft.y)
        lineTo(bottomRight.x, bottomRight.y)
        close()
    }

    // Draw the triangle with proper gradient
    // We'll use a mesh approach for smooth gradients
    val steps = 20
    for (i in 0..steps) {
        for (j in 0..(steps - i)) {
            val k = steps - i - j
            if (k >= 0) {
                // Barycentric coordinates
                val u = i.toFloat() / steps
                val v = j.toFloat() / steps
                val w = k.toFloat() / steps

                // Position in triangle
                val x = u * top.x + v * bottomLeft.x + w * bottomRight.x
                val y = u * top.y + v * bottomLeft.y + w * bottomRight.y

                // Calculate color based on position
                // u corresponds to pure hue (top vertex)
                // v corresponds to black (bottom left)
                // w corresponds to white (bottom right)

                // Saturation decreases as we move toward white (bottomRight)
                val saturation = 1f - w
                // Value decreases as we move toward black (bottomLeft)
                val value = 1f - v

                val color = Color.hsv(hue, saturation, value)

                drawCircle(
                    color = color,
                    radius = radius / steps * 1.5f,
                    center = Offset(x, y)
                )
            }
        }
    }

    // Draw border
    drawPath(
        path = trianglePath,
        color = Color.Gray.copy(alpha = 0.5f),
        style = Stroke(width = 1.5f)
    )
}

/**
 * Get SV indicator position in triangle.
 */
private fun getSVIndicatorPosition(
    center: Offset,
    radius: Float,
    hsv: HSV
): Offset {
    val hueAngleRad = hsv.hue * PI.toFloat() / 180f

    // Triangle vertices
    val top = Offset(
        center.x + radius * cos(hueAngleRad),
        center.y + radius * sin(hueAngleRad)
    )
    val bottomLeft = Offset(
        center.x + radius * cos(hueAngleRad + 2 * PI.toFloat() / 3),
        center.y + radius * sin(hueAngleRad + 2 * PI.toFloat() / 3)
    )
    val bottomRight = Offset(
        center.x + radius * cos(hueAngleRad - 2 * PI.toFloat() / 3),
        center.y + radius * sin(hueAngleRad - 2 * PI.toFloat() / 3)
    )

    // Calculate barycentric coordinates based on HSV values
    // Top vertex (u): pure hue (S=1, V=1)
    // Bottom left (v): black (S=any, V=0)
    // Bottom right (w): white (S=0, V=1)

    val v = 1f - hsv.value  // Weight for black vertex
    val w = (1f - hsv.saturation) * hsv.value  // Weight for white vertex
    val u = 1f - v - w  // Weight for pure hue vertex

    // Calculate position using barycentric coordinates
    return Offset(
        u * top.x + v * bottomLeft.x + w * bottomRight.x,
        u * top.y + v * bottomLeft.y + w * bottomRight.y
    )
}

/**
 * Update SV from triangle position.
 */
private fun updateSVFromPosition(
    position: Offset,
    center: Offset,
    radius: Float,
    currentHsv: HSV
): HSV? {
    val hueAngleRad = currentHsv.hue * PI.toFloat() / 180f

    // Triangle vertices
    val top = Offset(
        center.x + radius * cos(hueAngleRad),
        center.y + radius * sin(hueAngleRad)
    )
    val bottomLeft = Offset(
        center.x + radius * cos(hueAngleRad + 2 * PI.toFloat() / 3),
        center.y + radius * sin(hueAngleRad + 2 * PI.toFloat() / 3)
    )
    val bottomRight = Offset(
        center.x + radius * cos(hueAngleRad - 2 * PI.toFloat() / 3),
        center.y + radius * sin(hueAngleRad - 2 * PI.toFloat() / 3)
    )

    // Calculate barycentric coordinates for the position
    val v1 = top - bottomLeft
    val v2 = bottomRight - bottomLeft
    val vp = position - bottomLeft

    val d00 = dotProduct(v1, v1)
    val d01 = dotProduct(v1, v2)
    val d11 = dotProduct(v2, v2)
    val d20 = dotProduct(vp, v1)
    val d21 = dotProduct(vp, v2)

    val denom = d00 * d11 - d01 * d01
    if (abs(denom) < 0.0001f) return null

    val v = (d11 * d20 - d01 * d21) / denom  // Barycentric coord for top vertex
    val w = (d00 * d21 - d01 * d20) / denom  // Barycentric coord for bottomRight vertex
    val u = 1f - v - w  // Barycentric coord for bottomLeft vertex

    // Check if point is inside triangle
    if (v < -0.01f || w < -0.01f || u < -0.01f) {
        // Point is outside triangle, clamp to nearest edge
        val clampedV = v.coerceIn(0f, 1f)
        val clampedW = w.coerceIn(0f, 1f)
        val clampedU = u.coerceIn(0f, 1f)

        val sum = clampedV + clampedW + clampedU
        if (sum > 0.001f) {
            val normalizedV = clampedV / sum
            val normalizedW = clampedW / sum
            val normalizedU = clampedU / sum

            // Convert from barycentric to HSV
            // v = weight for top (pure hue: S=1, V=1)
            // u = weight for bottomLeft (black: S=any, V=0)
            // w = weight for bottomRight (white: S=0, V=1)

            val value = normalizedV + normalizedW  // V=0 only at black vertex
            val saturation = if (value > 0.001f) {
                normalizedV / value  // S approaches 0 as we move toward white
            } else {
                0f
            }

            return currentHsv.copy(
                saturation = saturation.coerceIn(0f, 1f),
                value = value.coerceIn(0f, 1f)
            )
        }
    }

    // Convert from barycentric to HSV
    // v = weight for top (pure hue: S=1, V=1)
    // u = weight for bottomLeft (black: S=any, V=0)
    // w = weight for bottomRight (white: S=0, V=1)

    val value = v + w  // V=0 only at black vertex
    val saturation = if (value > 0.001f) {
        v / value  // S approaches 0 as we move toward white
    } else {
        0f
    }

    return currentHsv.copy(
        saturation = saturation.coerceIn(0f, 1f),
        value = value.coerceIn(0f, 1f)
    )
}

/**
 * Helper function for dot product of two offsets.
 */
private fun dotProduct(a: Offset, b: Offset): Float {
    return a.x * b.x + a.y * b.y
}

/**
 * Helper operator for offset subtraction.
 */
private operator fun Offset.minus(other: Offset): Offset {
    return Offset(this.x - other.x, this.y - other.y)
}
