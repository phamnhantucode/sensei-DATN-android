package com.phamnhantucode.colorpicker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phamnhantucode.colorpicker.utils.toHsv

/**
 * RGB/HSV Sliders Panel for precise color adjustment.
 */
@Composable
internal fun ColorSlidersPanel(
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
 * Individual color slider item.
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
