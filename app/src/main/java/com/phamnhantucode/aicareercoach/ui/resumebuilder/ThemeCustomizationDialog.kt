package com.phamnhantucode.aicareercoach.ui.resumebuilder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationDialog(
    currentTheme: ResumeTheme,
    onDismiss: () -> Unit,
    onApplyTheme: (ResumeTheme) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var workingTheme by remember { mutableStateOf(currentTheme) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Customize Theme",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                }


                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Templates") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Colors") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Typography") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Layout") }
                    )
                }


                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> TemplatesTab(
                            currentTheme = workingTheme,
                            onSelectTemplate = { workingTheme = it }
                        )
                        1 -> ColorsTab(
                            colorScheme = workingTheme.colorScheme,
                            onUpdateColors = { workingTheme = workingTheme.copy(colorScheme = it) }
                        )
                        2 -> TypographyTab(
                            typography = workingTheme.typography,
                            onUpdateTypography = { workingTheme = workingTheme.copy(typography = it) }
                        )
                        3 -> LayoutTab(
                            layout = workingTheme.layout,
                            onUpdateLayout = { workingTheme = workingTheme.copy(layout = it) }
                        )
                    }
                }


                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                    ) {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onApplyTheme(workingTheme)
                                onDismiss()
                            }
                        ) {
                            Text("Apply Theme")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemplatesTab(
    currentTheme: ResumeTheme,
    onSelectTemplate: (ResumeTheme) -> Unit
) {
    val templates = ResumeTemplates.getAllTemplates()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        templates.forEach { template ->
            TemplateCard(
                template = template,
                isSelected = currentTheme.templateId == template.id,
                onClick = { onSelectTemplate(template.theme) }
            )
        }
    }
}

@Composable
private fun TemplateCard(
    template: TemplateInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = template.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }


            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ColorCircle(Color(template.theme.colorScheme.primaryColor))
                ColorCircle(Color(template.theme.colorScheme.accentColor))
                ColorCircle(Color(template.theme.colorScheme.textColor))
            }
        }
    }
}

@Composable
private fun ColorsTab(
    colorScheme: ColorScheme,
    onUpdateColors: (ColorScheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Color Scheme",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        ColorPicker(
            label = "Primary Color",
            color = Color(colorScheme.primaryColor),
            onColorSelected = { onUpdateColors(colorScheme.copy(primaryColor = it.value.toLong())) }
        )

        ColorPicker(
            label = "Accent Color",
            color = Color(colorScheme.accentColor),
            onColorSelected = { onUpdateColors(colorScheme.copy(accentColor = it.value.toLong())) }
        )

        ColorPicker(
            label = "Text Color",
            color = Color(colorScheme.textColor),
            onColorSelected = { onUpdateColors(colorScheme.copy(textColor = it.value.toLong())) }
        )

        ColorPicker(
            label = "Background Color",
            color = Color(colorScheme.backgroundColor),
            onColorSelected = { onUpdateColors(colorScheme.copy(backgroundColor = it.value.toLong())) }
        )

        ColorPicker(
            label = "Section Header Color",
            color = Color(colorScheme.sectionHeaderColor),
            onColorSelected = { onUpdateColors(colorScheme.copy(sectionHeaderColor = it.value.toLong())) }
        )

        ColorPicker(
            label = "Secondary Text Color",
            color = Color(colorScheme.secondaryTextColor),
            onColorSelected = { onUpdateColors(colorScheme.copy(secondaryTextColor = it.value.toLong())) }
        )
    }
}

@Composable
private fun ColorPicker(
    label: String,
    color: Color,
    onColorSelected: (Color) -> Unit
) {
    // Quick color palette
    val colorPalette = listOf(
        Color(0xFF000000), Color(0xFF212121), Color(0xFF424242), Color(0xFF757575),
        Color(0xFF1976D2), Color(0xFF0288D1), Color(0xFF0097A7), Color(0xFF00796B),
        Color(0xFF388E3C), Color(0xFF689F38), Color(0xFFFBC02D), Color(0xFFFFA000),
        Color(0xFFF57C00), Color(0xFFE64A19), Color(0xFFD32F2F), Color(0xFFC2185B),
        Color(0xFF7B1FA2), Color(0xFF512DA8), Color(0xFF303F9F), Color(0xFF1976D2),
        Color(0xFFFFFFFF), Color(0xFFFAFAFA), Color(0xFFF5F5F5), Color(0xFFEEEEEE)
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorPalette.chunked(8).forEach { row ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { paletteColor ->
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(paletteColor)
                                .border(
                                    width = if (paletteColor == color) 3.dp else 1.dp,
                                    color = if (paletteColor == color)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline,
                                    shape = CircleShape
                                )
                                .clickable { onColorSelected(paletteColor) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TypographyTab(
    typography: TypographyScheme,
    onUpdateTypography: (TypographyScheme) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Typography",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )


        SliderSetting(
            label = "Header Size",
            value = typography.headerSize,
            valueRange = 18f..32f,
            onValueChange = { onUpdateTypography(typography.copy(headerSize = it)) },
            valueDisplay = "${typography.headerSize.toInt()}sp"
        )

        SliderSetting(
            label = "Sub-header Size",
            value = typography.subHeaderSize,
            valueRange = 14f..24f,
            onValueChange = { onUpdateTypography(typography.copy(subHeaderSize = it)) },
            valueDisplay = "${typography.subHeaderSize.toInt()}sp"
        )

        SliderSetting(
            label = "Body Size",
            value = typography.bodySize,
            valueRange = 10f..18f,
            onValueChange = { onUpdateTypography(typography.copy(bodySize = it)) },
            valueDisplay = "${typography.bodySize.toInt()}sp"
        )

        SliderSetting(
            label = "Caption Size",
            value = typography.captionSize,
            valueRange = 8f..14f,
            onValueChange = { onUpdateTypography(typography.copy(captionSize = it)) },
            valueDisplay = "${typography.captionSize.toInt()}sp"
        )

        Divider()


        SliderSetting(
            label = "Header Weight",
            value = typography.headerWeight.toFloat(),
            valueRange = 400f..900f,
            steps = 4,
            onValueChange = { onUpdateTypography(typography.copy(headerWeight = it.toInt())) },
            valueDisplay = when (typography.headerWeight) {
                400 -> "Normal"
                500 -> "Medium"
                600 -> "Semi-Bold"
                700 -> "Bold"
                800 -> "Extra-Bold"
                900 -> "Black"
                else -> typography.headerWeight.toString()
            }
        )

        SliderSetting(
            label = "Body Weight",
            value = typography.bodyWeight.toFloat(),
            valueRange = 300f..600f,
            steps = 2,
            onValueChange = { onUpdateTypography(typography.copy(bodyWeight = it.toInt())) },
            valueDisplay = when (typography.bodyWeight) {
                300 -> "Light"
                400 -> "Normal"
                500 -> "Medium"
                600 -> "Semi-Bold"
                else -> typography.bodyWeight.toString()
            }
        )
    }
}

@Composable
private fun LayoutTab(
    layout: LayoutConfig,
    onUpdateLayout: (LayoutConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Layout Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )


        Text(
            text = "Layout Type",
            style = MaterialTheme.typography.labelLarge
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LayoutType.values().forEach { type ->
                LayoutTypeCard(
                    type = type,
                    isSelected = layout.type == type,
                    onClick = { onUpdateLayout(layout.copy(type = type)) }
                )
            }
        }

        Divider()


        Text(
            text = "Section Style",
            style = MaterialTheme.typography.labelLarge
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionStyle.values().forEach { style ->
                SectionStyleCard(
                    style = style,
                    isSelected = layout.sectionStyle == style,
                    onClick = { onUpdateLayout(layout.copy(sectionStyle = style)) }
                )
            }
        }

        Divider()


        SliderSetting(
            label = "Spacing",
            value = layout.spacing.toFloat(),
            valueRange = 8f..24f,
            onValueChange = { onUpdateLayout(layout.copy(spacing = it.toInt())) },
            valueDisplay = "${layout.spacing}dp"
        )

        SliderSetting(
            label = "Section Spacing",
            value = layout.sectionSpacing.toFloat(),
            valueRange = 12f..36f,
            onValueChange = { onUpdateLayout(layout.copy(sectionSpacing = it.toInt())) },
            valueDisplay = "${layout.sectionSpacing}dp"
        )
    }
}

@Composable
private fun LayoutTypeCard(
    type: LayoutType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SectionStyleCard(
    style: SectionStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isSelected)
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = style.displayName,
                style = MaterialTheme.typography.bodyLarge
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
    valueDisplay: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = valueDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps
        )
    }
}

@Composable
private fun ColorCircle(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
    )
}
