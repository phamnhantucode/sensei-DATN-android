package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * Panel for managing layers (elements)
 * Allows reordering, toggling visibility, and locking elements
 */
@Composable
fun LayersPanel(
    elements: List<ResumeElement>,
    selectedElementId: String?,
    onSelectElement: (ResumeElement) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onMoveLayer: (Int, Int) -> Unit,
    onClose: () -> Unit
) {
    // Sort elements by Z-index descending (Front to Back) for display
    // This matches the visual stacking order (top of list = top of stack)
    val sortedElements = remember(elements) {
        elements.sortedByDescending { it.zIndex }.toMutableStateList()
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Move item in the local state list
        val item = sortedElements.removeAt(from.index)
        sortedElements.add(to.index, item)
        
        // Notify parent to update the actual data
        onMoveLayer(from.index, to.index)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Layers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        Divider()

        // Layers List with Reorderable
        androidx.compose.foundation.lazy.LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(
                count = sortedElements.size,
                key = { index -> sortedElements[index].id }
            ) { index ->
                ReorderableItem(reorderableLazyListState, key = sortedElements[index].id) { isDragging ->
                    val element = sortedElements[index]
                    
                    LayerItem(
                        element = element,
                        isSelected = element.id == selectedElementId,
                        isDragging = isDragging,
                        onSelect = { onSelectElement(element) },
                        onToggleVisibility = { onToggleVisibility(element.id) },
                        onToggleLock = { onToggleLock(element.id) },
                        dragModifier = Modifier.draggableHandle()
                    )
                    
                    if (index < sortedElements.size - 1) {
                        Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerItem(
    element: ResumeElement,
    isSelected: Boolean,
    isDragging: Boolean,
    onSelect: () -> Unit,
    onToggleVisibility: () -> Unit,
    onToggleLock: () -> Unit,
    dragModifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Element Icon
        Icon(
            imageVector = getElementIcon(element),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Element Name/Description
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = getElementName(element),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            // Optional: Show Z-index or type as subtitle
            Text(
                text = element.javaClass.simpleName.replace("Element", ""),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }

        // Actions
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Visibility
            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (element.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = "Toggle Visibility",
                    modifier = Modifier.size(18.dp),
                    tint = if (element.isVisible) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                )
            }

            // Lock
            IconButton(
                onClick = onToggleLock,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (element.locked) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = "Toggle Lock",
                    modifier = Modifier.size(18.dp),
                    tint = if (element.locked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Drag Handle Icon (at the end)
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = dragModifier
                    .padding(start = 4.dp)
                    .size(20.dp)
            )
        }
    }
}

private fun getElementIcon(element: ResumeElement): ImageVector {
    return when (element) {
        is ResumeElement.TextElement -> Icons.Default.TextFields
        is ResumeElement.ImageElement -> Icons.Default.Image
        is ResumeElement.ShapeElement -> when (element.shapeType) {
            ShapeType.RECTANGLE -> Icons.Default.CheckBoxOutlineBlank
            ShapeType.CIRCLE -> Icons.Default.Circle
            ShapeType.LINE -> Icons.Default.HorizontalRule
            ShapeType.DIVIDER -> Icons.Default.HorizontalRule
        }
        is ResumeElement.ChartElement -> Icons.Default.BarChart
        is ResumeElement.ContainerElement -> Icons.Default.CheckBoxOutlineBlank
        is ResumeElement.IconElement -> Icons.Default.EmojiEmotions
        is ResumeElement.ContactElement -> Icons.Default.ContactPhone
        is ResumeElement.WorkExperienceElement -> Icons.Default.Work
        is ResumeElement.EducationElement -> Icons.Default.School
        is ResumeElement.SkillElement -> Icons.Default.Star
        is ResumeElement.ProjectElement -> Icons.Default.Code
        is ResumeElement.CertificationElement -> Icons.Default.CardMembership
        is ResumeElement.LanguageElement -> Icons.Default.Language
    }
}

private fun getElementName(element: ResumeElement): String {
    return when (element) {
        is ResumeElement.TextElement -> element.content.ifEmpty { "Text" }
        is ResumeElement.ImageElement -> "Image"
        is ResumeElement.ShapeElement -> element.shapeType.name.lowercase().capitalize()
        is ResumeElement.ChartElement -> "Chart"
        is ResumeElement.ContainerElement -> "Container"
        is ResumeElement.IconElement -> "Icon"
        is ResumeElement.ContactElement -> "Contact Info"
        is ResumeElement.WorkExperienceElement -> "Work Experience"
        is ResumeElement.EducationElement -> "Education"
        is ResumeElement.SkillElement -> "Skills"
        is ResumeElement.ProjectElement -> "Projects"
        is ResumeElement.CertificationElement -> "Certifications"
        is ResumeElement.LanguageElement -> "Languages"
    }
}

private fun String.capitalize(): String {
    return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
}
