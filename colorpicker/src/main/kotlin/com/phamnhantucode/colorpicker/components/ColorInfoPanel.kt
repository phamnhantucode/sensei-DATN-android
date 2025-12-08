package com.phamnhantucode.colorpicker.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Color info panel with hex and RGB inputs.
 */
@Composable
internal fun ColorInfoPanel(
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
