package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import kotlin.math.roundToInt

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
    onMoveToContainer: (String, String) -> Unit,
    onMoveOut: (String) -> Unit,
    onClose: () -> Unit
) {
    // Helper to build the tree structure
    val (topLevelElements, childMap) = remember(elements) {
        val children = elements.filterIsInstance<ResumeElement.ContainerElement>()
            .flatMap { it.children }
            .toSet()
        
        val topLevel = elements.filter { !children.contains(it.id) }
            .sortedByDescending { it.zIndex }
            
        val map = elements.filterIsInstance<ResumeElement.ContainerElement>()
            .associate { it.id to it.children }
            
        topLevel to map
    }

    val topLevelList = remember(topLevelElements) { topLevelElements.toMutableStateList() }

    // State for nesting drag-and-drop
    var nestingDragElementId by remember { mutableStateOf<String?>(null) }
    var nestingDragPosition by remember { mutableStateOf(Offset.Zero) }
    val itemBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }
    val iconBounds = remember { mutableStateMapOf<String, androidx.compose.ui.geometry.Rect>() }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val item = topLevelList.removeAt(from.index)
        topLevelList.add(to.index, item)
        onMoveLayer(from.index, to.index)
    }

    Box(modifier = Modifier.fillMaxSize()) {
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

            // Layers List
            androidx.compose.foundation.lazy.LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(
                    count = topLevelList.size,
                    key = { index -> topLevelList[index].id }
                ) { index ->
                    ReorderableItem(reorderableLazyListState, key = topLevelList[index].id) { isDragging ->
                        val element = topLevelList[index]
                        
                        LayerItem(
                            element = element,
                            isSelected = element.id == selectedElementId,
                            isDragging = isDragging,
                            onSelect = { onSelectElement(element) },
                            onToggleVisibility = { onToggleVisibility(element.id) },
                            onToggleLock = { onToggleLock(element.id) },
                            dragModifier = Modifier.draggableHandle(),
                            depth = 0,
                            childMap = childMap,
                            allElements = elements,
                            onMoveToContainer = onMoveToContainer,
                            onMoveOut = onMoveOut,
                            onSelectElement = onSelectElement,
                            selectedElementId = selectedElementId,
                            onDragStart = { id, localOffset -> 
                                val bounds = iconBounds[id]
                                if (bounds != null) {
                                    nestingDragElementId = id 
                                    nestingDragPosition = bounds.topLeft + localOffset
                                }
                            },
                            onDrag = { offset -> nestingDragPosition += offset },
                            onDragEnd = { 
                                // Check drop target
                                val targetId = itemBounds.entries.find { (_, rect) ->
                                    rect.contains(nestingDragPosition)
                                }?.key
                                
                                if (targetId != null && targetId != nestingDragElementId) {
                                    // Check if target is a container
                                    val targetElement = elements.find { it.id == targetId }
                                    if (targetElement is ResumeElement.ContainerElement) {
                                        onMoveToContainer(nestingDragElementId!!, targetId)
                                    }
                                }
                                nestingDragElementId = null
                            },
                            onPositioned = { id, rect -> itemBounds[id] = rect },
                            onIconPositioned = { id, rect -> iconBounds[id] = rect }
                        )
                        
                        if (index < topLevelList.size - 1) {
                            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
        
        // Drag Overlay using Popup to ensure it's on top and uses global coordinates
        if (nestingDragElementId != null) {
            val element = elements.find { it.id == nestingDragElementId }
            if (element != null) {
                androidx.compose.ui.window.Popup(
                    offset = IntOffset(
                        nestingDragPosition.x.roundToInt(), 
                        nestingDragPosition.y.roundToInt()
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                            .shadow(4.dp)
                    ) {
                        Text(getElementName(element), style = MaterialTheme.typography.bodySmall)
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
    dragModifier: Modifier = Modifier,
    depth: Int = 0,
    childMap: Map<String, List<String>> = emptyMap(),
    allElements: List<ResumeElement> = emptyList(),
    onMoveToContainer: (String, String) -> Unit = { _, _ -> },
    onMoveOut: (String) -> Unit = {},
    onSelectElement: (ResumeElement) -> Unit = {},
    selectedElementId: String? = null,
    onDragStart: (String, Offset) -> Unit = { _, _ -> },
    onDrag: (Offset) -> Unit = {},
    onDragEnd: () -> Unit = {},
    onPositioned: (String, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> },
    onIconPositioned: (String, androidx.compose.ui.geometry.Rect) -> Unit = { _, _ -> }
) {
    val backgroundColor = when {
        isDragging -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        else -> Color.Transparent
    }

    Column(
        modifier = Modifier.onGloballyPositioned { coordinates ->
            onPositioned(element.id, coordinates.boundsInRoot())
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .clickable(onClick = onSelect)
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .padding(start = (depth * 16).dp), // Indentation
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Element Icon - Draggable for nesting
            Icon(
                imageVector = getElementIcon(element),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .onGloballyPositioned { coordinates ->
                        onIconPositioned(element.id, coordinates.boundsInRoot())
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> onDragStart(element.id, offset) },
                            onDrag = { change, dragAmount -> 
                                change.consume()
                                onDrag(dragAmount) 
                            },
                            onDragEnd = { onDragEnd() },
                            onDragCancel = { onDragEnd() }
                        )
                    }
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
                // Move Out (if depth > 0)
                if (depth > 0) {
                    IconButton(
                        onClick = { onMoveOut(element.id) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = "Move Out",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

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

                // Drag Handle Icon (at the end) - Only for top level for now
                if (depth == 0) {
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
        
        // Render children
        val childrenIds = childMap[element.id]
        if (!childrenIds.isNullOrEmpty()) {
            childrenIds.forEach { childId ->
                val child = allElements.find { it.id == childId }
                if (child != null) {
                    LayerItem(
                        element = child,
                        isSelected = child.id == selectedElementId,
                        isDragging = false, // Children not draggable via Reorderable for now
                        onSelect = { onSelectElement(child) },
                        onToggleVisibility = { /* TODO: Handle child visibility toggle via ViewModel */ },
                        onToggleLock = { /* TODO: Handle child lock toggle via ViewModel */ },
                        dragModifier = Modifier,
                        depth = depth + 1,
                        childMap = childMap,
                        allElements = allElements,
                        onMoveToContainer = onMoveToContainer,
                        onMoveOut = onMoveOut,
                        onSelectElement = onSelectElement,
                        selectedElementId = selectedElementId,
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onPositioned = onPositioned,
                        onIconPositioned = onIconPositioned
                    )
                }
            }
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
