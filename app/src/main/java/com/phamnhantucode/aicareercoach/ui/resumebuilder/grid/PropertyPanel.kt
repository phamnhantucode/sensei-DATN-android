package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Property panel for editing element properties
 * Shows context-sensitive controls based on element type
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertyPanel(
    element: ResumeElement,
    onUpdateElement: (ResumeElement) -> Unit,
    onClose: () -> Unit,
    onRemoveElement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Surface(
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Properties",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = onClose) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        }

        Divider()

        // Scrollable content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Common properties (all elements)
            CommonPropertiesSection(
                element = element,
                onUpdateElement = onUpdateElement
            )

            Divider()

            // Type-specific properties
            when (element) {
                is ResumeElement.TextElement -> {
                    TextElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ImageElement -> {
                    ImageElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ShapeElement -> {
                    ShapeElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                is ResumeElement.ChartElement -> {
                    ChartElementProperties(
                        element = element,
                        onUpdateElement = onUpdateElement
                    )
                }
                else -> {
                    Text("Properties not yet implemented for this element type")
                }
            }

            Divider()

            // Style properties
            StylePropertiesSection(
                element = element,
                onUpdateElement = onUpdateElement
            )

            Divider()

            // Remove element button
            var showDeleteConfirmation by remember { mutableStateOf(false) }

            Button(
                onClick = { showDeleteConfirmation = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Remove Element")
            }

            // Delete confirmation dialog
            if (showDeleteConfirmation) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmation = false },
                    title = { Text("Remove Element?") },
                    text = { Text("Are you sure you want to remove this element? This action cannot be undone.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDeleteConfirmation = false
                                onRemoveElement()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirmation = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * Common properties section (position, size, z-index, lock)
 */
@Composable
private fun CommonPropertiesSection(
    element: ResumeElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Position & Size") {
        // Position
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberField(
                label = "Row",
                value = element.position.row,
                onValueChange = { newRow ->
                    val newPosition = element.position.copy(row = newRow)
                    onUpdateElement(updateElementPosition(element, newPosition))
                },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "Col",
                value = element.position.col,
                onValueChange = { newCol ->
                    val newPosition = element.position.copy(col = newCol)
                    onUpdateElement(updateElementPosition(element, newPosition))
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Size
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            NumberField(
                label = "Height",
                value = element.position.rowSpan,
                onValueChange = { newHeight ->
                    val newPosition = element.position.copy(rowSpan = newHeight.coerceAtLeast(1))
                    onUpdateElement(updateElementPosition(element, newPosition))
                },
                modifier = Modifier.weight(1f)
            )
            NumberField(
                label = "Width",
                value = element.position.colSpan,
                onValueChange = { newWidth ->
                    val newPosition = element.position.copy(colSpan = newWidth.coerceAtLeast(1))
                    onUpdateElement(updateElementPosition(element, newPosition))
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Z-Index
        NumberField(
            label = "Layer (Z-Index)",
            value = element.zIndex,
            onValueChange = { newZIndex ->
                onUpdateElement(updateElementZIndex(element, newZIndex))
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Lock toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lock Element")
            Switch(
                checked = element.locked,
                onCheckedChange = { locked ->
                    onUpdateElement(updateElementLocked(element, locked))
                }
            )
        }
    }
}

/**
 * Text element specific properties
 */
@Composable
private fun TextElementProperties(
    element: ResumeElement.TextElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Text Content") {
        // Content
        OutlinedTextField(
            value = element.content,
            onValueChange = { newContent ->
                onUpdateElement(element.copy(content = newContent))
            },
            label = { Text("Content") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 8
        )

        // Font size
        SliderField(
            label = "Font Size: ${element.textStyle.fontSize.toInt()}sp",
            value = element.textStyle.fontSize,
            valueRange = 8f..72f,
            onValueChange = { newSize ->
                onUpdateElement(
                    element.copy(
                        textStyle = element.textStyle.copy(fontSize = newSize)
                    )
                )
            }
        )

        // Font weight
        var showFontWeightMenu by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = showFontWeightMenu,
            onExpandedChange = { showFontWeightMenu = it }
        ) {
            OutlinedTextField(
                value = element.textStyle.fontWeight.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("Font Weight") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showFontWeightMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showFontWeightMenu,
                onDismissRequest = { showFontWeightMenu = false }
            ) {
                listOf(
                    FontWeight.Thin to "Thin",
                    FontWeight.Light to "Light",
                    FontWeight.Normal to "Normal",
                    FontWeight.Medium to "Medium",
                    FontWeight.SemiBold to "SemiBold",
                    FontWeight.Bold to "Bold",
                    FontWeight.ExtraBold to "ExtraBold"
                ).forEach { (weight, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onUpdateElement(
                                element.copy(
                                    textStyle = element.textStyle.copy(fontWeight = weight)
                                )
                            )
                            showFontWeightMenu = false
                        }
                    )
                }
            }
        }

        // Text alignment
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextAlignment.values().forEach { alignment ->
                FilterChip(
                    selected = element.alignment == alignment,
                    onClick = {
                        onUpdateElement(element.copy(alignment = alignment))
                    },
                    label = { Text(alignment.name) }
                )
            }
        }

        // Text color (simplified color picker)
        ColorPicker(
            label = "Text Color",
            color = Color(element.textStyle.color),
            onColorChange = { newColor ->
                newColor?.let {
                    val colorLong = android.graphics.Color.argb(
                        (it.alpha * 255).toInt(),
                        (it.red * 255).toInt(),
                        (it.green * 255).toInt(),
                        (it.blue * 255).toInt()
                    ).toLong()
                    onUpdateElement(
                        element.copy(
                            textStyle = element.textStyle.copy(color = colorLong)
                        )
                    )
                }
            }
        )
    }
}

/**
 * Image element properties (placeholder)
 */
@Composable
private fun ImageElementProperties(
    element: ResumeElement.ImageElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Image") {
        OutlinedTextField(
            value = element.imageUrl,
            onValueChange = { newUrl ->
                onUpdateElement(element.copy(imageUrl = newUrl))
            },
            label = { Text("Image URL or Path") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { /* TODO: Image picker */ },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Choose Image")
        }
    }
}

/**
 * Shape element properties
 */
@Composable
private fun ShapeElementProperties(
    element: ResumeElement.ShapeElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Shape") {
        // Shape type dropdown
        var showShapeMenu by remember { mutableStateOf(false) }
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = showShapeMenu,
            onExpandedChange = { showShapeMenu = it }
        ) {
            OutlinedTextField(
                value = element.shapeType.name,
                onValueChange = {},
                readOnly = true,
                label = { Text("Shape Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showShapeMenu) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = showShapeMenu,
                onDismissRequest = { showShapeMenu = false }
            ) {
                ShapeType.values().forEach { shapeType ->
                    DropdownMenuItem(
                        text = { Text(shapeType.name) },
                        onClick = {
                            onUpdateElement(element.copy(shapeType = shapeType))
                            showShapeMenu = false
                        }
                    )
                }
            }
        }

        // Corner radius
        SliderField(
            label = "Corner Radius: ${element.cornerRadius.toInt()}dp",
            value = element.cornerRadius,
            valueRange = 0f..32f,
            onValueChange = { newRadius ->
                onUpdateElement(element.copy(cornerRadius = newRadius))
            }
        )

        // Custom height (particularly useful for dividers)
        if (element.shapeType == ShapeType.DIVIDER || element.customHeightDp != null) {
            SliderField(
                label = "Custom Height: ${element.customHeightDp?.toInt() ?: 2}dp",
                value = element.customHeightDp ?: 2f,
                valueRange = 1f..24f,
                onValueChange = { newHeight ->
                    onUpdateElement(element.copy(customHeightDp = newHeight))
                }
            )
        }

        // Custom width (optional, for vertical dividers)
        if (element.shapeType == ShapeType.LINE || element.customWidthDp != null) {
            SliderField(
                label = "Custom Width: ${element.customWidthDp?.toInt() ?: 2}dp",
                value = element.customWidthDp ?: 2f,
                valueRange = 1f..24f,
                onValueChange = { newWidth ->
                    onUpdateElement(element.copy(customWidthDp = newWidth))
                }
            )
        }
    }
}

/**
 * Chart element properties (placeholder)
 */
@Composable
private fun ChartElementProperties(
    element: ResumeElement.ChartElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Chart") {
        Text("Chart properties coming soon...")
    }
}

/**
 * Style properties section (border, background, shadow)
 */
@Composable
private fun StylePropertiesSection(
    element: ResumeElement,
    onUpdateElement: (ResumeElement) -> Unit
) {
    PropertySection(title = "Style") {
        // Background color
        ColorPicker(
            label = "Background",
            color = element.style.backgroundColor?.let { Color(it) },
            onColorChange = { newColor ->
                val colorLong = newColor?.let {
                    android.graphics.Color.argb(
                        (it.alpha * 255).toInt(),
                        (it.red * 255).toInt(),
                        (it.green * 255).toInt(),
                        (it.blue * 255).toInt()
                    ).toLong()
                }
                onUpdateElement(
                    updateElementStyle(
                        element,
                        element.style.copy(backgroundColor = colorLong)
                    )
                )
            },
            nullable = true
        )

        // Border
        ColorPicker(
            label = "Border Color",
            color = element.style.borderColor?.let { Color(it) },
            onColorChange = { newColor ->
                val colorLong = newColor?.let {
                    android.graphics.Color.argb(
                        (it.alpha * 255).toInt(),
                        (it.red * 255).toInt(),
                        (it.green * 255).toInt(),
                        (it.blue * 255).toInt()
                    ).toLong()
                }
                onUpdateElement(
                    updateElementStyle(
                        element,
                        element.style.copy(borderColor = colorLong)
                    )
                )
            },
            nullable = true
        )

        if (element.style.borderColor != null) {
            SliderField(
                label = "Border Width: ${element.style.borderWidth.toInt()}dp",
                value = element.style.borderWidth,
                valueRange = 0f..8f,
                onValueChange = { newWidth ->
                    onUpdateElement(
                        updateElementStyle(
                            element,
                            element.style.copy(borderWidth = newWidth)
                        )
                    )
                }
            )
        }
    }
}

// ============================================================================
// Helper Components
// ============================================================================

@Composable
private fun PropertySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        content()
    }
}

@Composable
private fun NumberField(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value.toString(),
        onValueChange = { newValue ->
            newValue.toIntOrNull()?.let(onValueChange)
        },
        label = { Text(label) },
        singleLine = true,
        modifier = modifier
    )
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, fontSize = 12.sp)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange
        )
    }
}

@Composable
private fun ColorPicker(
    label: String,
    color: Color?,
    onColorChange: (Color?) -> Unit,
    nullable: Boolean = false
) {
    var showColorPickerDialog by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color preview box - click to open dialog
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(color ?: Color.Transparent, MaterialTheme.shapes.small)
                        .border(1.dp, Color.Gray, MaterialTheme.shapes.small)
                        .clickable {
                            showColorPickerDialog = true
                        }
                )

                if (nullable && color != null) {
                    IconButton(
                        onClick = { onColorChange(null) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear color",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Quick color presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                Color.White,
                Color.Black,
                Color.Gray,
                Color.Red,
                Color.Blue,
                Color.Green,
                Color.Yellow
            ).forEach { presetColor ->
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(presetColor, MaterialTheme.shapes.small)
                        .border(
                            width = if (color == presetColor) 2.dp else 1.dp,
                            color = if (color == presetColor) MaterialTheme.colorScheme.primary else Color.Gray,
                            shape = MaterialTheme.shapes.small
                        )
                        .clickable { onColorChange(presetColor) }
                )
            }
        }

        // Button to open advanced color picker
        OutlinedButton(
            onClick = { showColorPickerDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Palette, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Advanced Picker")
        }
    }

    // Show color picker dialog
    if (showColorPickerDialog) {
        ColorPickerDialog(
            initialColor = color ?: Color.Black,
            onDismiss = { showColorPickerDialog = false },
            onColorSelected = { newColor ->
                onColorChange(newColor)
            }
        )
    }
}

// Helper functions to update elements immutably
private fun updateElementPosition(element: ResumeElement, newPosition: GridPosition): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(position = newPosition)
        is ResumeElement.ImageElement -> element.copy(position = newPosition)
        is ResumeElement.ShapeElement -> element.copy(position = newPosition)
        is ResumeElement.ChartElement -> element.copy(position = newPosition)
        is ResumeElement.ContainerElement -> element.copy(position = newPosition)
        is ResumeElement.IconElement -> element.copy(position = newPosition)
    }
}

private fun updateElementZIndex(element: ResumeElement, newZIndex: Int): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ImageElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ShapeElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ChartElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.ContainerElement -> element.copy(zIndex = newZIndex)
        is ResumeElement.IconElement -> element.copy(zIndex = newZIndex)
    }
}

private fun updateElementLocked(element: ResumeElement, locked: Boolean): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(locked = locked)
        is ResumeElement.ImageElement -> element.copy(locked = locked)
        is ResumeElement.ShapeElement -> element.copy(locked = locked)
        is ResumeElement.ChartElement -> element.copy(locked = locked)
        is ResumeElement.ContainerElement -> element.copy(locked = locked)
        is ResumeElement.IconElement -> element.copy(locked = locked)
    }
}

private fun updateElementStyle(element: ResumeElement, newStyle: ElementStyle): ResumeElement {
    return when (element) {
        is ResumeElement.TextElement -> element.copy(style = newStyle)
        is ResumeElement.ImageElement -> element.copy(style = newStyle)
        is ResumeElement.ShapeElement -> element.copy(style = newStyle)
        is ResumeElement.ChartElement -> element.copy(style = newStyle)
        is ResumeElement.ContainerElement -> element.copy(style = newStyle)
        is ResumeElement.IconElement -> element.copy(style = newStyle)
    }
}
