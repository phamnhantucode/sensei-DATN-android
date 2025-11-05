package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.*

/**
 * Professional color picker dialog with hue ring and saturation/brightness triangle
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    val hsv = remember(initialColor) {
        val hsvArray = FloatArray(3)
        // Convert Compose Color to Android Color int (ARGB)
        val colorInt = android.graphics.Color.argb(
            (initialColor.alpha * 255).toInt(),
            (initialColor.red * 255).toInt(),
            (initialColor.green * 255).toInt(),
            (initialColor.blue * 255).toInt()
        )
        android.graphics.Color.colorToHSV(colorInt, hsvArray)
        mutableStateOf(HSV(hsvArray[0], hsvArray[1], hsvArray[2]))
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Pick a Color",
                    style = MaterialTheme.typography.titleLarge
                )

                // Color picker wheel with triangle
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .padding(8.dp)
                ) {
                    HueRingWithTriangle(
                        hsv = hsv.value,
                        onHsvChange = { newHsv ->
                            hsv.value = newHsv
                            selectedColor = hsvToColor(newHsv)
                        }
                    )
                }

                // Color preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Preview:")
                    Surface(
                        modifier = Modifier
                            .size(60.dp, 40.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = selectedColor,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
                    ) {}
                }

                // RGB values display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val rgb = colorToRgb(selectedColor)
                    Text(
                        text = "RGB: ${rgb.red}, ${rgb.green}, ${rgb.blue}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    val hexString = String.format("%02X%02X%02X", rgb.red, rgb.green, rgb.blue)
                    Text(
                        text = "Hex: #$hexString",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onColorSelected(selectedColor)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Select")
                    }
                }
            }
        }
    }
}

/**
 * Hue ring with saturation/brightness triangle inside
 */
@Composable
private fun HueRingWithTriangle(
    hsv: HSV,
    onHsvChange: (HSV) -> Unit
) {
    var isDraggingHue by remember { mutableStateOf(false) }
    var isDraggingSV by remember { mutableStateOf(false) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val distance = sqrt((offset.x - center.x).pow(2) + (offset.y - center.y).pow(2))
                        val outerRadius = size.width / 2f
                        val innerRadius = outerRadius * 0.75f

                        if (distance >= innerRadius && distance <= outerRadius) {
                            isDraggingHue = true
                        } else if (distance < innerRadius) {
                            isDraggingSV = true
                        }
                    },
                    onDrag = { change, _ ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val offset = change.position

                        if (isDraggingHue) {
                            // Update hue based on angle
                            val angle = atan2(offset.y - center.y, offset.x - center.x)
                            val hue = ((angle * 180f / PI.toFloat() + 360f) % 360f)
                            onHsvChange(hsv.copy(hue = hue))
                        } else if (isDraggingSV) {
                            // Update saturation and value based on position in triangle
                            val newHsv = updateSVFromTrianglePosition(
                                offset,
                                center,
                                size.width / 2f * 0.75f,
                                hsv
                            )
                            onHsvChange(newHsv)
                        }
                    },
                    onDragEnd = {
                        isDraggingHue = false
                        isDraggingSV = false
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val distance = sqrt((offset.x - center.x).pow(2) + (offset.y - center.y).pow(2))
                    val outerRadius = size.width / 2f
                    val innerRadius = outerRadius * 0.75f

                    if (distance >= innerRadius && distance <= outerRadius) {
                        // Tap on hue ring
                        val angle = atan2(offset.y - center.y, offset.x - center.x)
                        val hue = ((angle * 180f / PI.toFloat() + 360f) % 360f)
                        onHsvChange(hsv.copy(hue = hue))
                    } else if (distance < innerRadius) {
                        // Tap on SV triangle
                        val newHsv = updateSVFromTrianglePosition(
                            offset,
                            center,
                            innerRadius,
                            hsv
                        )
                        onHsvChange(newHsv)
                    }
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.width / 2f
        val innerRadius = outerRadius * 0.75f

        // Draw hue ring
        drawHueRing(center, innerRadius, outerRadius)

        // Draw saturation/brightness triangle
        drawSVTriangle(center, innerRadius, hsv.hue)

        // Draw hue indicator
        val hueAngle = hsv.hue * PI.toFloat() / 180f
        val hueIndicatorRadius = (innerRadius + outerRadius) / 2f
        val hueIndicatorPos = Offset(
            center.x + hueIndicatorRadius * cos(hueAngle),
            center.y + hueIndicatorRadius * sin(hueAngle)
        )
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = hueIndicatorPos,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color.Black,
            radius = 8f,
            center = hueIndicatorPos,
            style = Stroke(width = 1f)
        )

        // Draw SV indicator
        val svPos = getSVIndicatorPosition(center, innerRadius, hsv)
        drawCircle(
            color = Color.White,
            radius = 7f,
            center = svPos,
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color.Black,
            radius = 7f,
            center = svPos,
            style = Stroke(width = 1f)
        )
    }
}

/**
 * Draw the hue ring
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHueRing(
    center: Offset,
    innerRadius: Float,
    outerRadius: Float
) {
    val steps = 360
    for (i in 0 until steps) {
        val startAngle = i.toFloat()
        val sweepAngle = 1f
        val hue = i.toFloat()

        val color = Color.hsv(hue, 1f, 1f)

        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(color, color),
                center = center
            ),
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = true,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2)
        )
    }

    // Cut out the inner circle to create a ring
    drawCircle(
        color = Color.White,
        radius = innerRadius,
        center = center
    )
}

/**
 * Draw the saturation/brightness triangle
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSVTriangle(
    center: Offset,
    radius: Float,
    hue: Float
) {
    val triangleRadius = radius * 0.9f

    // Rotate triangle to point toward selected hue
    val hueAngleRad = hue * PI.toFloat() / 180f

    // Triangle vertices (pointing up, then rotated by hue)
    val angle1 = -90f * PI.toFloat() / 180f + hueAngleRad
    val angle2 = 30f * PI.toFloat() / 180f + hueAngleRad
    val angle3 = 150f * PI.toFloat() / 180f + hueAngleRad

    val p1 = Offset(
        center.x + triangleRadius * cos(angle1),
        center.y + triangleRadius * sin(angle1)
    )
    val p2 = Offset(
        center.x + triangleRadius * cos(angle2),
        center.y + triangleRadius * sin(angle2)
    )
    val p3 = Offset(
        center.x + triangleRadius * cos(angle3),
        center.y + triangleRadius * sin(angle3)
    )

    // Draw gradient triangle
    // We'll draw many small triangles from center to edges to create gradients
    val steps = 40
    val baseColor = Color.hsv(hue, 1f, 1f)

    for (i in 0..steps) {
        for (j in 0..steps - i) {
            val t1 = i.toFloat() / steps
            val t2 = j.toFloat() / steps

            if (t1 + t2 <= 1f) {
                val saturation = t1
                val value = 1f - t2

                val color = Color.hsv(hue, saturation, value)

                // Calculate position in triangle
                val x = center.x + (t1 * (p2.x - center.x) + t2 * (p3.x - center.x))
                val y = center.y + (t1 * (p2.y - center.y) + t2 * (p3.y - center.y))

                drawCircle(
                    color = color,
                    radius = triangleRadius / steps * 1.5f,
                    center = Offset(x, y)
                )
            }
        }
    }
}

/**
 * Get the position of SV indicator in the triangle
 */
private fun getSVIndicatorPosition(
    center: Offset,
    radius: Float,
    hsv: HSV
): Offset {
    val triangleRadius = radius * 0.9f

    // Rotate triangle to point toward selected hue
    val hueAngleRad = hsv.hue * PI.toFloat() / 180f

    // Map saturation and value to triangle position
    val t1 = hsv.saturation
    val t2 = 1f - hsv.value

    val angle2 = 30f * PI.toFloat() / 180f + hueAngleRad
    val angle3 = 150f * PI.toFloat() / 180f + hueAngleRad

    val p2 = Offset(
        triangleRadius * cos(angle2),
        triangleRadius * sin(angle2)
    )
    val p3 = Offset(
        triangleRadius * cos(angle3),
        triangleRadius * sin(angle3)
    )

    return Offset(
        center.x + t1 * p2.x + t2 * p3.x,
        center.y + t1 * p2.y + t2 * p3.y
    )
}

/**
 * Update saturation and value based on position in triangle
 */
private fun updateSVFromTrianglePosition(
    position: Offset,
    center: Offset,
    radius: Float,
    currentHsv: HSV
): HSV {
    val triangleRadius = radius * 0.9f

    // Rotate triangle to point toward selected hue
    val hueAngleRad = currentHsv.hue * PI.toFloat() / 180f

    // Convert position to triangle coordinates
    val localX = position.x - center.x
    val localY = position.y - center.y

    val angle2 = 30f * PI.toFloat() / 180f + hueAngleRad
    val angle3 = 150f * PI.toFloat() / 180f + hueAngleRad

    val p2 = Offset(
        triangleRadius * cos(angle2),
        triangleRadius * sin(angle2)
    )
    val p3 = Offset(
        triangleRadius * cos(angle3),
        triangleRadius * sin(angle3)
    )

    // Solve for t1 (saturation) and t2 (1 - value)
    // localX = t1 * p2.x + t2 * p3.x
    // localY = t1 * p2.y + t2 * p3.y

    val det = p2.x * p3.y - p2.y * p3.x
    if (abs(det) < 0.001f) {
        return currentHsv
    }

    val t1 = (localX * p3.y - localY * p3.x) / det
    val t2 = (p2.x * localY - p2.y * localX) / det

    // Clamp to valid triangle region
    val clampedT1 = t1.coerceIn(0f, 1f)
    val clampedT2 = t2.coerceIn(0f, 1f - clampedT1)

    val saturation = clampedT1
    val value = 1f - clampedT2

    return currentHsv.copy(
        saturation = saturation.coerceIn(0f, 1f),
        value = value.coerceIn(0f, 1f)
    )
}

/**
 * HSV color data class
 */
data class HSV(
    val hue: Float,        // 0-360
    val saturation: Float, // 0-1
    val value: Float       // 0-1
)

/**
 * RGB color data class
 */
data class RGB(
    val red: Int,
    val green: Int,
    val blue: Int
)

/**
 * Convert HSV to Color
 */
private fun hsvToColor(hsv: HSV): Color {
    return Color.hsv(
        hsv.hue,
        hsv.saturation,
        hsv.value
    )
}

/**
 * Convert Color to RGB
 */
private fun colorToRgb(color: Color): RGB {
    return RGB(
        red = (color.red * 255).toInt(),
        green = (color.green * 255).toInt(),
        blue = (color.blue * 255).toInt()
    )
}
