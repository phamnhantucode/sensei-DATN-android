package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*

/**
 * Template picker dialog
 * Allows users to select from predefined resume templates
 */
@Composable
fun TemplatePickerDialog(
    onDismiss: () -> Unit,
    onTemplateSelected: (GridTemplateType) -> Unit
) {
    var selectedTemplate by remember { mutableStateOf<GridTemplateType?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Choose Template",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                Text(
                    text = "Select a template to get started. You can customize it later.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(GridTemplateType.values()) { template ->
                        TemplateCard(
                            template = template,
                            isSelected = selectedTemplate == template,
                            onClick = { selectedTemplate = template }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedTemplate?.let { onTemplateSelected(it) }
                },
                enabled = selectedTemplate != null
            ) {
                Text("Apply Template")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Individual template card
 */
@Composable
private fun TemplateCard(
    template: GridTemplateType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val templateInfo = getTemplateInfo(template)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 2.dp
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .border(
                            width = 1.dp,
                            color = Color.LightGray,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(8.dp)
                ) {
                    TemplatePreview(template)
                }

                Spacer(modifier = Modifier.height(8.dp))


                Text(
                    text = templateInfo.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Text(
                    text = templateInfo.description,
                    fontSize = 11.sp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    lineHeight = 14.sp
                )
            }


            if (isSelected) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(24.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Simplified template preview visualization
 */
@Composable
private fun BoxScope.TemplatePreview(template: GridTemplateType) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        when (template) {
            GridTemplateType.PROFESSIONAL -> {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(16.dp)
                        .background(Color(0xFF2196F3), RoundedCornerShape(2.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(0.35f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .background(Color.LightGray, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(0.65f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(4) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color.Gray, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            GridTemplateType.MODERN -> {

                Row {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .fillMaxHeight()
                            .background(Color(0xFF4CAF50))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .background(Color.LightGray, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }

            GridTemplateType.MINIMAL -> {

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            GridTemplateType.CREATIVE -> {

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(0.6f)
                                .height(16.dp)
                                .background(Color(0xFFFF9800), RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .weight(0.4f)
                                .height(16.dp)
                                .background(Color.LightGray, RoundedCornerShape(2.dp))
                        )
                    }
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(Color.Gray, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            GridTemplateType.ACADEMIC -> {

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .background(Color(0xFF673AB7), RoundedCornerShape(2.dp))
                    )
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(Color.LightGray, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }

            GridTemplateType.TECHNICAL -> {

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(Color(0xFF009688), RoundedCornerShape(2.dp))
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .background(Color.LightGray, RoundedCornerShape(2.dp))
                        )
                    }
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .background(Color.Gray, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Template information
 */
internal data class TemplateInfo(
    val name: String,
    val description: String
)

/**
 * Gets information for a template type
 */
internal fun getTemplateInfo(template: GridTemplateType): TemplateInfo {
    return when (template) {
        GridTemplateType.PROFESSIONAL -> TemplateInfo(
            name = "Professional",
            description = "Classic two-column layout for business professionals"
        )
        GridTemplateType.MODERN -> TemplateInfo(
            name = "Modern",
            description = "Contemporary design with accent colors and clean lines"
        )
        GridTemplateType.MINIMAL -> TemplateInfo(
            name = "Minimal",
            description = "Simple, elegant layout focused on content"
        )
        GridTemplateType.CREATIVE -> TemplateInfo(
            name = "Creative",
            description = "Bold, asymmetric design for creative fields"
        )
        GridTemplateType.ACADEMIC -> TemplateInfo(
            name = "Academic",
            description = "Traditional format suitable for academic positions"
        )
        GridTemplateType.TECHNICAL -> TemplateInfo(
            name = "Technical",
            description = "Grid-based layout optimized for technical roles"
        )
    }
}

/**
 * Creates a GridResume from a template type
 * This is a simplified implementation - full templates would be more detailed
 */
fun createResumeFromTemplate(template: GridTemplateType): GridResume {
    val elements = when (template) {
        GridTemplateType.PROFESSIONAL -> createProfessionalTemplate()
        GridTemplateType.MODERN -> createModernTemplate()
        GridTemplateType.MINIMAL -> createMinimalTemplate()
        GridTemplateType.CREATIVE -> createCreativeTemplate()
        GridTemplateType.ACADEMIC -> createAcademicTemplate()
        GridTemplateType.TECHNICAL -> createTechnicalTemplate()
    }

    return GridResume(
        name = "Resume - ${template.name} Template",
        pages = listOf(
            ResumePage(elements = elements)
        ),
        gridConfig = GridConfig(
            columns = 48, // Templates are defined in 48x68, will be migrated
            rows = 68,
            cellSizeDp = GridConfig.CELL_SIZE_FOR_A4 * 2 // Double cell size for 48x68
        )
    )
}


private fun createProfessionalTemplate(): List<ResumeElement> {
    return listOf(

        ResumeElement.TextElement(
            position = GridPosition(0, 0, 2, 12),
            content = "Your Name",
            textStyle = TextStyle(
                fontSize = 28f,
                fontWeight = FontWeight.Bold,
                color = 0xFF000000
            ),
            alignment = TextAlignment.CENTER
        ),
        ResumeElement.TextElement(
            position = GridPosition(2, 0, 1, 12),
            content = "email@example.com | (123) 456-7890 | City, State",
            textStyle = TextStyle(fontSize = 12f, color = 0xFF666666),
            alignment = TextAlignment.CENTER
        ),

        ResumeElement.ShapeElement(
            position = GridPosition(3, 0, 1, 12),
            shapeType = ShapeType.DIVIDER,
            style = ElementStyle(backgroundColor = 0xFF2196F3, borderWidth = 2f),
            customHeightDp = 3f // Slightly thicker colored divider
        )
    )
}

private fun createModernTemplate(): List<ResumeElement> {
    return listOf(
        ResumeElement.TextElement(
            position = GridPosition(0, 0, 2, 12),
            content = "Your Name",
            textStyle = TextStyle(fontSize = 32f, fontWeight = FontWeight.Bold),
            alignment = TextAlignment.LEFT
        )
    )
}

private fun createMinimalTemplate(): List<ResumeElement> {
    return listOf(
        ResumeElement.TextElement(
            position = GridPosition(0, 0, 1, 12),
            content = "YOUR NAME",
            textStyle = TextStyle(fontSize = 24f, fontWeight = FontWeight.SemiBold),
            alignment = TextAlignment.CENTER
        )
    )
}

private fun createCreativeTemplate(): List<ResumeElement> {
    return listOf(
        ResumeElement.TextElement(
            position = GridPosition(0, 0, 3, 7),
            content = "Your Name",
            textStyle = TextStyle(fontSize = 36f, fontWeight = FontWeight.Bold),
            alignment = TextAlignment.LEFT
        )
    )
}

private fun createAcademicTemplate(): List<ResumeElement> {
    return listOf(
        ResumeElement.TextElement(
            position = GridPosition(0, 0, 2, 12),
            content = "Your Name",
            textStyle = TextStyle(fontSize = 26f, fontWeight = FontWeight.Bold),
            alignment = TextAlignment.CENTER
        )
    )
}

private fun createTechnicalTemplate(): List<ResumeElement> {
    return listOf(
        ResumeElement.TextElement(
            position = GridPosition(0, 0, 2, 8),
            content = "Your Name",
            textStyle = TextStyle(fontSize = 30f, fontWeight = FontWeight.Bold),
            alignment = TextAlignment.LEFT
        )
    )
}
