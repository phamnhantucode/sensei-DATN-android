package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.*

/**
 * HSV color data class
 */
data class HSV(
    val hue: Float,        // 0-360
    val saturation: Float, // 0-1
    val value: Float,      // 0-1
    val alpha: Float = 1f  // 0-1
)

/**
 * Color picker configuration
 */
data class ColorPickerConfig(
    val showAlpha: Boolean = true,
    val showColorHistory: Boolean = true,
    val animateColorChanges: Boolean = true
)

/**
 * Professional advanced color picker dialog with tabs
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    config: ColorPickerConfig = ColorPickerConfig()
) {
    val context = LocalContext.current
    val colorHistory = remember { ColorHistory(context) }

    var selectedColor by remember { mutableStateOf(initialColor) }
    var activeTab by remember { mutableStateOf(0) }

    val animatedColor by animateColorAsState(
        targetValue = if (config.animateColorChanges) selectedColor else selectedColor,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "color"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Color Picker",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Tab selector
                TabRow(
                    selectedTabIndex = activeTab,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Wheel") }
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Sliders") }
                    )
                    Tab(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        text = { Text("Palette") }
                    )
                    Tab(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        text = { Text("Recent") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Content based on selected tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                ) {
                    when (activeTab) {
                        0 -> {
                            // HSV Color Wheel
                            OptimizedHsvColorWheel(
                                modifier = Modifier.fillMaxSize(),
                                currentColor = selectedColor,
                                onColorChanged = { selectedColor = it }
                            )
                        }
                        1 -> {
                            // RGB/HSV Sliders
                            ColorSlidersPanel(
                                color = selectedColor,
                                onColorChanged = { selectedColor = it },
                                showAlpha = config.showAlpha
                            )
                        }
                        2 -> {
                            // Color Palette
                            ColorPalettePanel(
                                onColorSelected = { selectedColor = it }
                            )
                        }
                        3 -> {
                            // Recent Colors
                            if (config.showColorHistory) {
                                ColorHistoryTab(
                                    colorHistory = colorHistory,
                                    onColorSelected = { selectedColor = it }
                                )
                            }
                        }
                    }
                }

                // Color preview and info
                ColorInfoPanel(
                    color = animatedColor,
                    showAlpha = config.showAlpha,
                    onColorChanged = { selectedColor = it }
                )

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            colorHistory.addColor(selectedColor)
                            onColorSelected(selectedColor)
                            onDismiss()
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply")
                    }
                }
            }
        }
    }
}

/**
 * Optimized HSV Color Wheel with bitmap caching
 */
@Composable
private fun OptimizedHsvColorWheel(
    modifier: Modifier = Modifier,
    currentColor: Color,
    onColorChanged: (Color) -> Unit
) {
    val hsv = remember(currentColor) { currentColor.toHsv() }
    var currentHsv by remember { mutableStateOf(hsv) }

    // Update when color changes externally
    LaunchedEffect(currentColor) {
        currentHsv = currentColor.toHsv()
    }

    var isDraggingHue by remember { mutableStateOf(false) }
    var isDraggingSV by remember { mutableStateOf(false) }

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
 * Draw optimized hue ring using sweep gradient
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
 * Draw SV triangle with improved gradient rendering
 */
/**
 * Fixed SV Triangle calculation functions
 * Replace these functions in your ColorPicker.kt file
 */

/**
 * Draw SV triangle with correct coordinate mapping
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
 * Get SV indicator position in triangle (fixed version)
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
 * Update SV from triangle position (fixed version)
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
    if (kotlin.math.abs(denom) < 0.0001f) return null

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
 * Helper function for dot product of two offsets
 */
private fun dotProduct(a: Offset, b: Offset): Float {
    return a.x * b.x + a.y * b.y
}

/**
 * Helper operator for offset subtraction
 */
private operator fun Offset.minus(other: Offset): Offset {
    return Offset(this.x - other.x, this.y - other.y)
}
/**
 * RGB/HSV Sliders Panel
 */
@Composable
private fun ColorSlidersPanel(
    color: Color,
    onColorChanged: (Color) -> Unit,
    showAlpha: Boolean
) {
    var useHsv by remember { mutableStateOf(false) }
    val hsv = remember(color) { color.toHsv() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FilterChip(
                selected = !useHsv,
                onClick = { useHsv = false },
                label = { Text("RGB") },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
            FilterChip(
                selected = useHsv,
                onClick = { useHsv = true },
                label = { Text("HSV") },
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (useHsv) {
            // HSV Sliders
            ColorSliderItem(
                label = "Hue",
                value = hsv.hue,
                onValueChange = { h ->
                    onColorChanged(Color.hsv(h, hsv.saturation, hsv.value, color.alpha))
                },
                valueRange = 0f..360f,
                gradient = (0..360 step 10).map { Color.hsv(it.toFloat(), 1f, 1f) }
            )

            ColorSliderItem(
                label = "Saturation",
                value = hsv.saturation * 100,
                onValueChange = { s ->
                    onColorChanged(Color.hsv(hsv.hue, s / 100, hsv.value, color.alpha))
                },
                valueRange = 0f..100f,
                gradient = listOf(
                    Color.hsv(hsv.hue, 0f, hsv.value),
                    Color.hsv(hsv.hue, 1f, hsv.value)
                )
            )

            ColorSliderItem(
                label = "Value",
                value = hsv.value * 100,
                onValueChange = { v ->
                    onColorChanged(Color.hsv(hsv.hue, hsv.saturation, v / 100, color.alpha))
                },
                valueRange = 0f..100f,
                gradient = listOf(
                    Color.Black,
                    Color.hsv(hsv.hue, hsv.saturation, 1f)
                )
            )
        } else {
            // RGB Sliders
            ColorSliderItem(
                label = "Red",
                value = color.red * 255,
                onValueChange = { r ->
                    onColorChanged(color.copy(red = r / 255))
                },
                valueRange = 0f..255f,
                gradient = listOf(
                    color.copy(red = 0f),
                    color.copy(red = 1f)
                )
            )

            ColorSliderItem(
                label = "Green",
                value = color.green * 255,
                onValueChange = { g ->
                    onColorChanged(color.copy(green = g / 255))
                },
                valueRange = 0f..255f,
                gradient = listOf(
                    color.copy(green = 0f),
                    color.copy(green = 1f)
                )
            )

            ColorSliderItem(
                label = "Blue",
                value = color.blue * 255,
                onValueChange = { b ->
                    onColorChanged(color.copy(blue = b / 255))
                },
                valueRange = 0f..255f,
                gradient = listOf(
                    color.copy(blue = 0f),
                    color.copy(blue = 1f)
                )
            )
        }

        if (showAlpha) {
            ColorSliderItem(
                label = "Alpha",
                value = color.alpha * 100,
                onValueChange = { a ->
                    onColorChanged(color.copy(alpha = a / 100))
                },
                valueRange = 0f..100f,
                gradient = listOf(
                    color.copy(alpha = 0f),
                    color.copy(alpha = 1f)
                )
            )
        }
    }
}

/**
 * Individual color slider item
 */
@Composable
private fun ColorSliderItem(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    gradient: List<Color>
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value.toInt().toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(gradient)
                )
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }
    }
}

/**
 * Color Palette Panel with preset colors
 */
@Composable
private fun ColorPalettePanel(
    onColorSelected: (Color) -> Unit
) {
    val materialColors = listOf(
        // Reds
        Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A), Color(0xFFE57373),
        Color(0xFFEF5350), Color(0xFFF44336), Color(0xFFE53935), Color(0xFFD32F2F),
        // Pinks
        Color(0xFFFCE4EC), Color(0xFFF8BBD0), Color(0xFFF48FB1), Color(0xFFF06292),
        Color(0xFFEC407A), Color(0xFFE91E63), Color(0xFFD81B60), Color(0xFFC2185B),
        // Purples
        Color(0xFFF3E5F5), Color(0xFFE1BEE7), Color(0xFFCE93D8), Color(0xFFBA68C8),
        Color(0xFFAB47BC), Color(0xFF9C27B0), Color(0xFF8E24AA), Color(0xFF7B1FA2),
        // Deep Purples
        Color(0xFFEDE7F6), Color(0xFFD1C4E9), Color(0xFFB39DDB), Color(0xFF9575CD),
        Color(0xFF7E57C2), Color(0xFF673AB7), Color(0xFF5E35B1), Color(0xFF512DA8),
        // Indigos
        Color(0xFFE8EAF6), Color(0xFFC5CAE9), Color(0xFF9FA8DA), Color(0xFF7986CB),
        Color(0xFF5C6BC0), Color(0xFF3F51B5), Color(0xFF3949AB), Color(0xFF303F9F),
        // Blues
        Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF64B5F6),
        Color(0xFF42A5F5), Color(0xFF2196F3), Color(0xFF1E88E5), Color(0xFF1976D2),
        // Light Blues
        Color(0xFFE1F5FE), Color(0xFFB3E5FC), Color(0xFF81D4FA), Color(0xFF4FC3F7),
        Color(0xFF29B6F6), Color(0xFF03A9F4), Color(0xFF039BE5), Color(0xFF0288D1),
        // Cyans
        Color(0xFFE0F7FA), Color(0xFFB2EBF2), Color(0xFF80DEEA), Color(0xFF4DD0E1),
        Color(0xFF26C6DA), Color(0xFF00BCD4), Color(0xFF00ACC1), Color(0xFF0097A7),
        // Teals
        Color(0xFFE0F2F1), Color(0xFFB2DFDB), Color(0xFF80CBC4), Color(0xFF4DB6AC),
        Color(0xFF26A69A), Color(0xFF009688), Color(0xFF00897B), Color(0xFF00796B),
        // Greens
        Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7), Color(0xFF81C784),
        Color(0xFF66BB6A), Color(0xFF4CAF50), Color(0xFF43A047), Color(0xFF388E3C),
        // Light Greens
        Color(0xFFF1F8E9), Color(0xFFDCEDC8), Color(0xFFC5E1A5), Color(0xFFAED581),
        Color(0xFF9CCC65), Color(0xFF8BC34A), Color(0xFF7CB342), Color(0xFF689F38),
        // Yellows
        Color(0xFFFFFDE7), Color(0xFFFFF9C4), Color(0xFFFFF59D), Color(0xFFFFF176),
        Color(0xFFFFEE58), Color(0xFFFFEB3B), Color(0xFFFDD835), Color(0xFFFBC02D),
        // Oranges
        Color(0xFFFFF3E0), Color(0xFFFFE0B2), Color(0xFFFFCC80), Color(0xFFFFB74D),
        Color(0xFFFFA726), Color(0xFFFF9800), Color(0xFFFB8C00), Color(0xFFF57C00),
        // Browns
        Color(0xFFEFEBE9), Color(0xFFD7CCC8), Color(0xFFBCAAA4), Color(0xFFA1887F),
        Color(0xFF8D6E63), Color(0xFF795548), Color(0xFF6D4C41), Color(0xFF5D4037),
        // Grays
        Color(0xFFFAFAFA), Color(0xFFF5F5F5), Color(0xFFEEEEEE), Color(0xFFE0E0E0),
        Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575), Color(0xFF616161),
        Color(0xFF424242), Color(0xFF212121), Color(0xFF000000), Color(0xFFFFFFFF)
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(42.dp),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(materialColors.size) { index ->
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(materialColors[index])
                    .border(
                        width = 1.dp,
                        color = Color.Gray.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        onColorSelected(materialColors[index])
                    }
            )
        }
    }
}

/**
 * Color history tab - displays recent colors in a grid layout
 */
@Composable
private fun ColorHistoryTab(
    colorHistory: ColorHistory,
    onColorSelected: (Color) -> Unit
) {
    val colors = colorHistory.getColors()

    if (colors.isNotEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(80.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors.size) { index ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors[index])
                            .border(
                                width = 2.dp,
                                color = Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onColorSelected(colors[index])
                            }
                    )

                    Text(
                        text = String.format("#%06X", colors[index].toArgb() and 0xFFFFFF),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "No recent colors",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Colors you select will appear here",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Color info panel with hex input
 */
@Composable
private fun ColorInfoPanel(
    color: Color,
    showAlpha: Boolean,
    onColorChanged: (Color) -> Unit
) {
    var hexInput by remember(color) {
        val hex = if (showAlpha) {
            String.format(
                "#%08X",
                color.toArgb()
            )
        } else {
            String.format("#%06X", color.toArgb() and 0xFFFFFF)
        }
        mutableStateOf(hex)
    }

    var rgbInputs by remember(color) {
        mutableStateOf(
            Triple(
                (color.red * 255).toInt().toString(),
                (color.green * 255).toInt().toString(),
                (color.blue * 255).toInt().toString()
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Color preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                )

                // Hex input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { newValue ->
                        hexInput = newValue.uppercase()
                        if (newValue.startsWith("#")) {
                            try {
                                val colorInt = android.graphics.Color.parseColor(newValue)
                                onColorChanged(Color(colorInt))
                            } catch (e: Exception) {
                                // Invalid hex color
                            }
                        }
                    },
                    label = { Text("HEX", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    )
                )
            }

            // RGB inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = rgbInputs.first,
                    onValueChange = { newValue ->
                        rgbInputs = rgbInputs.copy(first = newValue)
                        newValue.toIntOrNull()?.let { r ->
                            if (r in 0..255) {
                                onColorChanged(color.copy(red = r / 255f))
                            }
                        }
                    },
                    label = { Text("R", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )

                OutlinedTextField(
                    value = rgbInputs.second,
                    onValueChange = { newValue ->
                        rgbInputs = rgbInputs.copy(second = newValue)
                        newValue.toIntOrNull()?.let { g ->
                            if (g in 0..255) {
                                onColorChanged(color.copy(green = g / 255f))
                            }
                        }
                    },
                    label = { Text("G", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )

                OutlinedTextField(
                    value = rgbInputs.third,
                    onValueChange = { newValue ->
                        rgbInputs = rgbInputs.copy(third = newValue)
                        newValue.toIntOrNull()?.let { b ->
                            if (b in 0..255) {
                                onColorChanged(color.copy(blue = b / 255f))
                            }
                        }
                    },
                    label = { Text("B", fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}

/**
 * Color history manager with SharedPreferences persistence
 */
class ColorHistory(context: Context) {
    private val prefs = context.getSharedPreferences("color_picker_history", Context.MODE_PRIVATE)
    private val maxColors = 8

    fun getColors(): List<Color> {
        val colorStrings = prefs.getString("colors", "") ?: ""
        if (colorStrings.isEmpty()) return emptyList()

        return colorStrings.split(",")
            .mapNotNull { hexString ->
                try {
                    Color(android.graphics.Color.parseColor(hexString))
                } catch (e: Exception) {
                    null
                }
            }
    }

    fun addColor(color: Color) {
        val currentColors = getColors().toMutableList()

        // Remove if already exists
        currentColors.removeAll { it == color }

        // Add to front
        currentColors.add(0, color)

        // Keep only max colors
        val colorsToSave = currentColors.take(maxColors)

        val colorStrings = colorsToSave.joinToString(",") { c ->
            String.format("#%08X", c.toArgb())
        }

        prefs.edit().putString("colors", colorStrings).apply()
    }
}

/**
 * Extension: Convert Color to HSV
 */
fun Color.toHsv(): HSV {
    val hsvArray = FloatArray(3)
    val colorInt = this.toArgb()
    android.graphics.Color.colorToHSV(colorInt, hsvArray)
    return HSV(hsvArray[0], hsvArray[1], hsvArray[2], this.alpha)
}

/**
 * Extension: Convert HSV to Color
 */
fun HSV.toColor(): Color {
    return Color.hsv(hue, saturation, value, alpha)
}
