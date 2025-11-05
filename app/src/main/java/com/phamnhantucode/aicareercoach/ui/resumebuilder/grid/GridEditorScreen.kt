package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.TextElementRenderer

/**
 * Main grid-based resume editor screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridEditorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onSwitchToFormEditor: () -> Unit,
    viewModel: GridEditorViewModel = viewModel()
) {
    val gridResume by viewModel.gridResume.collectAsState()
    val selectedElement by viewModel.selectedElement.collectAsState()
    val draggedElement by viewModel.draggedElement.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    var showPropertyPanel by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showElementPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GridEditorTopBar(
                resumeName = gridResume.name,
                isSaving = isSaving,
                onNavigateBack = onNavigateBack,
                onSave = { viewModel.save() },
                onPreview = onNavigateToPreview,
                onSwitchMode = onSwitchToFormEditor,
                onShowTemplates = { showTemplateDialog = true }
            )
        },
        bottomBar = {
            GridEditorBottomBar(
                gridConfig = gridResume.gridConfig,
                onToggleGrid = { viewModel.toggleGrid() },
                onToggleSnap = { viewModel.toggleSnap() },
                onZoomIn = { /* TODO */ },
                onZoomOut = { /* TODO */ },
                onUndo = { viewModel.undo() },
                onRedo = { viewModel.redo() }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main canvas area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                GridCanvas(
                    gridResume = gridResume,
                    selectedElement = selectedElement,
                    draggedElement = draggedElement,
                    onElementSelect = { viewModel.selectElement(it) },
                    onElementDeselect = { viewModel.deselectElement() },
                    onDragStart = { viewModel.startDrag(it) },
                    onDrag = { element, position ->
                        viewModel.updateDragPosition(position)
                    },
                    onDragEnd = { element, position ->
                        viewModel.endDrag(position)
                    }
                )

                // Floating action button to add elements
                FloatingActionButton(
                    onClick = { showElementPicker = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Element")
                }
            }

            // Property panel (right side) - shown when element is selected
            if (showPropertyPanel && selectedElement != null) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    tonalElevation = 2.dp
                ) {
                    PropertyPanel(
                        element = selectedElement!!,
                        onUpdateElement = { viewModel.updateElement(it) },
                        onClose = { showPropertyPanel = false }
                    )
                }
            }
        }
    }

    // Element picker dialog
    if (showElementPicker) {
        ElementPickerDialog(
            onDismiss = { showElementPicker = false },
            onElementTypeSelected = { type ->
                viewModel.addElement(type)
                showElementPicker = false
            }
        )
    }

    // Template picker dialog
    if (showTemplateDialog) {
        TemplatePickerDialog(
            onDismiss = { showTemplateDialog = false },
            onTemplateSelected = { template ->
                viewModel.applyTemplate(template)
                showTemplateDialog = false
            }
        )
    }

    // Auto-show property panel when element is selected
    LaunchedEffect(selectedElement) {
        showPropertyPanel = selectedElement != null
    }
}

/**
 * Top app bar for grid editor
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridEditorTopBar(
    resumeName: String,
    isSaving: Boolean,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onPreview: () -> Unit,
    onSwitchMode: () -> Unit,
    onShowTemplates: () -> Unit
) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = resumeName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Grid Editor",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            // Switch to form editor
            IconButton(onClick = onSwitchMode) {
                Icon(Icons.Default.Edit, contentDescription = "Switch to Form Editor")
            }

            // Templates
            IconButton(onClick = onShowTemplates) {
                Icon(Icons.Default.Dashboard, contentDescription = "Templates")
            }

            // Preview
            IconButton(onClick = onPreview) {
                Icon(Icons.Default.Visibility, contentDescription = "Preview")
            }

            // Save
            IconButton(
                onClick = onSave,
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = "Save")
                }
            }
        }
    )
}

/**
 * Bottom toolbar with grid controls and actions
 */
@Composable
private fun GridEditorBottomBar(
    gridConfig: GridConfig,
    onToggleGrid: () -> Unit,
    onToggleSnap: () -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - Grid controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Show/Hide Grid
                FilterChip(
                    selected = gridConfig.showGrid,
                    onClick = onToggleGrid,
                    label = { Text("Grid") },
                    leadingIcon = {
                        Icon(
                            IconAliases.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                // Snap to Grid
                FilterChip(
                    selected = gridConfig.snapToGrid,
                    onClick = onToggleSnap,
                    label = { Text("Snap") },
                    leadingIcon = {
                        Icon(
                            IconAliases.GridOn,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            // Center - Zoom controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onZoomOut) {
                    Icon(IconAliases.ZoomOut, contentDescription = "Zoom Out")
                }
                Text("100%", fontSize = 14.sp)
                IconButton(onClick = onZoomIn) {
                    Icon(IconAliases.ZoomIn, contentDescription = "Zoom In")
                }
            }

            // Right side - Undo/Redo
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = onUndo) {
                    Icon(IconAliases.Undo, contentDescription = "Undo")
                }
                IconButton(onClick = onRedo) {
                    Icon(IconAliases.Redo, contentDescription = "Redo")
                }
            }
        }
    }
}

/**
 * Main grid canvas where elements are placed
 */
@Composable
private fun GridCanvas(
    gridResume: GridResume,
    selectedElement: ResumeElement?,
    draggedElement: DragState?,
    onElementSelect: (ResumeElement) -> Unit,
    onElementDeselect: () -> Unit,
    onDragStart: (ResumeElement) -> Unit,
    onDrag: (ResumeElement, GridPosition) -> Unit,
    onDragEnd: (ResumeElement, GridPosition) -> Unit
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridResume.gridConfig.cellSizeDp * density
    val (gridWidthPx, gridHeightPx) = GridUtils.getGridSizePx(
        gridResume.gridConfig,
        cellSizePx
    )

    val scrollStateVertical = rememberScrollState()
    val scrollStateHorizontal = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(scrollStateVertical)
            .horizontalScroll(scrollStateHorizontal)
            .padding(32.dp)
    ) {
        // Resume page (A4-like container)
        Box(
            modifier = Modifier
                .size(
                    width = GridUtils.pxToDp(gridWidthPx, density),
                    height = GridUtils.pxToDp(gridHeightPx, density)
                )
                .background(Color.White)
                .padding(0.dp)
        ) {
            // Grid background
            GridBackground(
                gridConfig = gridResume.gridConfig
            )

            // Render all elements from first page
            gridResume.pages.firstOrNull()?.let { page ->
                page.elementsByZIndex().forEach { element ->
                    val isSelected = selectedElement?.id == element.id
                    val isDragging = draggedElement?.element?.id == element.id

                    DraggableElement(
                        element = element,
                        gridConfig = gridResume.gridConfig,
                        isSelected = isSelected,
                        isDragging = isDragging,
                        onDragStart = onDragStart,
                        onDrag = onDrag,
                        onDragEnd = onDragEnd,
                        onSelect = onElementSelect,
                        onDeselect = onElementDeselect
                    ) {
                        // Render element content based on type
                        when (element) {
                            is ResumeElement.TextElement -> {
                                TextElementRenderer(
                                    element = element,
                                    isEditing = false
                                )
                            }
                            is ResumeElement.ImageElement -> {
                                // TODO: Implement ImageElementRenderer
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray)
                                )
                            }
                            is ResumeElement.ShapeElement -> {
                                // TODO: Implement ShapeElementRenderer
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(element.style.backgroundColor ?: 0xFF000000))
                                )
                            }
                            else -> {
                                // Placeholder for other element types
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray)
                                )
                            }
                        }
                    }
                }
            }

            // Show drag ghost if dragging
            draggedElement?.let { drag ->
                DragGhost(
                    element = drag.element,
                    position = drag.currentPosition,
                    gridConfig = gridResume.gridConfig,
                    isValid = drag.isValidPosition
                ) {
                    // Ghost content (simplified)
                }
            }
        }
    }
}

/**
 * Element picker dialog
 */
@Composable
private fun ElementPickerDialog(
    onDismiss: () -> Unit,
    onElementTypeSelected: (ElementType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Element") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ElementTypeButton(
                    icon = IconAliases.TextFields,
                    label = "Text",
                    onClick = { onElementTypeSelected(ElementType.TEXT) }
                )
                ElementTypeButton(
                    icon = Icons.Default.Image,
                    label = "Image",
                    onClick = { onElementTypeSelected(ElementType.IMAGE) }
                )
                ElementTypeButton(
                    icon = IconAliases.RectangleFilled,
                    label = "Shape",
                    onClick = { onElementTypeSelected(ElementType.SHAPE) }
                )
                ElementTypeButton(
                    icon = Icons.Default.HorizontalRule,
                    label = "Divider",
                    onClick = { onElementTypeSelected(ElementType.DIVIDER) }
                )
                ElementTypeButton(
                    icon = IconAliases.BarChart,
                    label = "Chart",
                    onClick = { onElementTypeSelected(ElementType.CHART) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Element type button in picker
 */
@Composable
private fun ElementTypeButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Text(label, fontSize = 16.sp)
        }
    }
}

/**
 * Element type enum for adding new elements
 */
enum class ElementType {
    TEXT,
    IMAGE,
    SHAPE,
    DIVIDER,
    CHART,
    CONTAINER,
    ICON
}

// Icon aliases for missing icons (using available Material Icons)
private object IconAliases {
    val RectangleFilled = Icons.Default.CropSquare
    val Rectangle = Icons.Default.CropSquare
    val Undo = Icons.Default.ArrowBack
    val Redo = Icons.Default.ArrowForward
    val ZoomIn = Icons.Default.Add
    val ZoomOut = Icons.Default.Remove
    val GridOn = Icons.Default.Dashboard
    val TextFields = Icons.Default.TextFormat
    val BarChart = Icons.Default.BarChart
}
