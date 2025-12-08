package com.phamnhantucode.colorpicker

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.phamnhantucode.colorpicker.components.*

/**
 * A professional color picker dialog for Jetpack Compose with multiple selection modes.
 *
 * Features:
 * - HSV color wheel for intuitive color selection
 * - RGB/HSV sliders for precise control
 * - Material Design color palette
 * - Recent colors history (requires Context)
 * - Hex and RGB color input
 * - Configurable alpha channel support
 *
 * @param initialColor The starting color to display in the picker
 * @param onDismiss Callback invoked when the dialog is dismissed without selection
 * @param onColorSelected Callback invoked when a color is selected via the Apply button
 * @param config Configuration options for the color picker behavior and appearance
 * @param context Optional Android Context for color history persistence. If null, history is disabled.
 *
 * @sample
 * ```kotlin
 * var showColorPicker by remember { mutableStateOf(false) }
 * var selectedColor by remember { mutableStateOf(Color.Blue) }
 *
 * if (showColorPicker) {
 *     ColorPickerDialog(
 *         initialColor = selectedColor,
 *         onDismiss = { showColorPicker = false },
 *         onColorSelected = { color ->
 *             selectedColor = color
 *         },
 *         context = LocalContext.current
 *     )
 * }
 * ```
 */
@Composable
fun ColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit,
    config: ColorPickerConfig = ColorPickerConfig(),
    context: Context? = null
) {
    // Only create color history if context is provided and history is enabled
    val colorHistory = remember(context) {
        if (context != null && config.showColorHistory) {
            ColorHistory(context, config)
        } else {
            null
        }
    }

    var selectedColor by remember { mutableStateOf(initialColor) }
    var activeTab by remember { mutableStateOf(0) }

    // Determine if we should show the Recent tab
    val showRecentTab = colorHistory != null && config.showColorHistory

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
                    if (showRecentTab) {
                        Tab(
                            selected = activeTab == 3,
                            onClick = { activeTab = 3 },
                            text = { Text("Recent") }
                        )
                    }
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
                            // Recent Colors (only shown if colorHistory is not null)
                            if (showRecentTab && colorHistory != null) {
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
                            // Add to history only if color history is available
                            colorHistory?.addColor(selectedColor)
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
