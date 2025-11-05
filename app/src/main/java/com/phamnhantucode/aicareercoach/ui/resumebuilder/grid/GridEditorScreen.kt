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
import androidx.compose.runtime.key
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val gridResume by viewModel.gridResume.collectAsState()
    val selectedElement by viewModel.selectedElement.collectAsState()
    val draggedElement by viewModel.draggedElement.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val pdfExportState by viewModel.pdfExportState.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()

    var showPropertyPanel by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showElementPicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GridEditorTopBar(
                resumeName = gridResume.name,
                isSaving = isSaving,
                onNavigateBack = onNavigateBack,
                onSave = { viewModel.save() },
                onExport = { showExportDialog = true },
                onPreview = onNavigateToPreview,
                onSwitchMode = onSwitchToFormEditor,
                onShowTemplates = { showTemplateDialog = true }
            )
        },
        bottomBar = {
            GridEditorBottomBar(
                gridConfig = gridResume.gridConfig,
                zoomLevel = zoomLevel,
                onToggleGrid = { viewModel.toggleGrid() },
                onToggleSnap = { viewModel.toggleSnap() },
                onZoomIn = { viewModel.zoomIn() },
                onZoomOut = { viewModel.zoomOut() },
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
                    zoomLevel = zoomLevel,
                    onElementSelect = { viewModel.selectElement(it) },
                    onElementDeselect = { viewModel.deselectElement() },
                    onDragStart = { viewModel.startDrag(it) },
                    onDrag = { element, position ->
                        viewModel.updateDragPosition(position)
                    },
                    onDragEnd = { element, position ->
                        viewModel.endDrag(position)
                    },
                    onResize = { element, newPosition ->
                        // Clamp position to ensure it stays within bounds
                        val clampedPosition = GridUtils.clampPosition(newPosition, gridResume.gridConfig)

                        // Update element with new position (size)
                        val updatedElement = when (element) {
                            is ResumeElement.TextElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ImageElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ShapeElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ChartElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ContainerElement -> element.copy(position = clampedPosition)
                            is ResumeElement.IconElement -> element.copy(position = clampedPosition)
                        }
                        viewModel.updateElement(updatedElement)
                    },
                    onOpenProperties = { showPropertyPanel = true }
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

    // PDF Export dialog
    if (showExportDialog) {
        PdfExportDialog(
            exportState = pdfExportState,
            onDismiss = {
                showExportDialog = false
                viewModel.resetExportState()
            },
            onExport = {
                viewModel.exportToPdf()
            },
            onShare = { uri ->
                // Share the PDF using Android share sheet
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share Resume PDF"))
            }
        )
    }

    // Close property panel when element is deselected
    LaunchedEffect(selectedElement) {
        if (selectedElement == null) {
            showPropertyPanel = false
        }
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
    onExport: () -> Unit,
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

            // Export PDF
            IconButton(onClick = onExport) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export PDF")
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
    zoomLevel: Float,
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
                Text("${(zoomLevel * 100).toInt()}%", fontSize = 14.sp)
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
    zoomLevel: Float,
    onElementSelect: (ResumeElement) -> Unit,
    onElementDeselect: () -> Unit,
    onDragStart: (ResumeElement) -> Unit,
    onDrag: (ResumeElement, GridPosition) -> Unit,
    onDragEnd: (ResumeElement, GridPosition) -> Unit,
    onResize: (ResumeElement, GridPosition) -> Unit,
    onOpenProperties: () -> Unit
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
            .horizontalScroll(scrollStateHorizontal),
        contentAlignment = Alignment.Center
    ) {
        // Resume page (A4-like container)
        Box(
            modifier = Modifier
                .size(
                    width = GridUtils.pxToDp(gridWidthPx, density) * zoomLevel,
                    height = GridUtils.pxToDp(gridHeightPx, density) * zoomLevel
                )
                .background(Color.White)
                .padding(0.dp)
        ) {
            // Grid background
            GridBackground(
                gridConfig = gridResume.gridConfig,
                zoomLevel = zoomLevel
            )

            // Render all elements from first page
            gridResume.pages.firstOrNull()?.let { page ->
                page.elementsByZIndex().forEach { element ->
                    key(element.id) {
                        val isSelected = selectedElement?.id == element.id
                        val isDragging = draggedElement?.element?.id == element.id

                        DraggableElement(
                            element = element,
                            gridConfig = gridResume.gridConfig,
                            zoomLevel = zoomLevel,
                            isSelected = isSelected,
                            isDragging = isDragging,
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onResize = onResize,
                            onSelect = onElementSelect,
                            onDeselect = onElementDeselect,
                            onOpenProperties = { _ -> onOpenProperties() }
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
            }

            // Show drag ghost if dragging
            draggedElement?.let { drag ->
                DragGhost(
                    element = drag.element,
                    position = drag.currentPosition,
                    gridConfig = gridResume.gridConfig,
                    zoomLevel = zoomLevel,
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
 * PDF Export Dialog
 */
@Composable
private fun PdfExportDialog(
    exportState: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onShare: (android.net.Uri) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Export PDF")
        },
        text = {
            when (exportState) {
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.Idle -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Export your resume as a PDF file.")
                        Text(
                            "The PDF will be saved to Downloads/AI Career Coach",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.PreparingImages -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Preparing images...")
                    }
                }
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.RenderingPage -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Rendering page ${exportState.page} of ${exportState.total}...")
                    }
                }
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.SavingFile -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Saving PDF...")
                    }
                }
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.Success -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✓ PDF exported successfully!")
                        Text(
                            "Size: ${exportState.fileSizeBytes / 1024} KB",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Location: Downloads/AI Career Coach",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.Error -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("❌ Export failed")
                        Text(
                            exportState.message,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (exportState) {
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.Idle -> {
                    Button(onClick = onExport) {
                        Text("Export")
                    }
                }
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.Success -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onShare(exportState.uri) }) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.Error -> {
                    Button(onClick = onDismiss) {
                        Text("Close")
                    }
                }
                else -> {
                    // Hide button during export
                }
            }
        },
        dismissButton = {
            when (exportState) {
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState.Idle -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
                else -> { /* No dismiss button during/after export */ }
            }
        }
    )
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
