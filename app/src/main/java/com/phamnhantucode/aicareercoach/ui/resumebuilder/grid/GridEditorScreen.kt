package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.positionChange
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.CertificationElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ContactElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.EducationElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ImageElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.LanguageElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ProjectElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ShapeElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.SkillElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.TextElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.WorkExperienceElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ContainerElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.utils.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val gridResume by viewModel.gridResume.collectAsState()
    val selectedElement by viewModel.selectedElement.collectAsState()
    val draggedElement by viewModel.draggedElement.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val pdfExportState by viewModel.pdfExportState.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val isMoveMode by viewModel.isMoveMode.collectAsState()

    var showPropertyPanel by remember { mutableStateOf(false) }
    var showLayersPanel by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showElementPicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var applyingTemplateData by remember { mutableStateOf(false) }

    // Load resume data for template application
    val scope = rememberCoroutineScope()
    val repository = remember { ResumeRepository.getInstance(context) }

    // Calculate and set optimal default zoom on first composition
    LaunchedEffect(Unit) {
        val cellSizePx = gridResume.gridConfig.cellSizeDp * density
        val screenWidthPx = configuration.screenWidthDp * density
        val screenHeightPx = configuration.screenHeightDp * density

        // Reserve space for top bar (~64dp), bottom bar (~56dp), and some padding
        val availableHeightPx = screenHeightPx - (120 * density)
        val availableWidthPx = screenWidthPx

        val optimalZoom = GridUtils.calculateOptimalZoom(
            gridConfig = gridResume.gridConfig,
            availableWidthPx = availableWidthPx,
            availableHeightPx = availableHeightPx,
            cellSizePx = cellSizePx,
            padding = 0.85f // 85% of viewport for some breathing room
        )

        viewModel.setZoomLevel(optimalZoom)
    }

    // Save data when leaving the screen (lifecycle-aware save)
    DisposableEffect(viewModel) {
        onDispose {
            // Save immediately and synchronously when screen is disposed
            // This ensures data is persisted even if app is killed during navigation
            viewModel.saveImmediately()
        }
    }

    Scaffold(
        topBar = {
            GridEditorTopBar(
                resumeName = gridResume.name,
                isSaving = isSaving,
                isApplyingData = applyingTemplateData,
                onNavigateBack = onNavigateBack,
                onSave = { viewModel.save() },
                onExport = { showExportDialog = true },
                onPreview = onNavigateToPreview,
                onSwitchMode = onSwitchToFormEditor,
                onShowTemplates = { showTemplateDialog = true },
                onApplyTemplateData = {
                    scope.launch {
                        applyingTemplateData = true
                        try {
                            val result = repository.getLatestResume()
                            result.onSuccess { resume ->
                                resume?.let {
                                    viewModel.applyUserDataToTemplate(it)
                                }
                            }
                        } finally {
                            applyingTemplateData = false
                        }
                    }
                }
            )
        },
        bottomBar = {
            GridEditorBottomBar(
                gridConfig = gridResume.gridConfig,
                zoomLevel = zoomLevel,
                isMoveMode = isMoveMode,
                showLayersPanel = showLayersPanel,
                onToggleGrid = { viewModel.toggleGrid() },
                onToggleMoveMode = { viewModel.toggleMoveMode() },
                onToggleLayers = { showLayersPanel = !showLayersPanel },
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
                    isMoveMode = isMoveMode,
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
                        // IMPORTANT: Use copy() which preserves all properties including shapeType, isCircle, etc.
                        val updatedElement = when (element) {
                            is ResumeElement.TextElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ImageElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ShapeElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ChartElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ContainerElement -> element.copy(position = clampedPosition)
                            is ResumeElement.IconElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ContactElement -> element.copy(position = clampedPosition)
                            is ResumeElement.WorkExperienceElement -> element.copy(position = clampedPosition)
                            is ResumeElement.EducationElement -> element.copy(position = clampedPosition)
                            is ResumeElement.SkillElement -> element.copy(position = clampedPosition)
                            is ResumeElement.ProjectElement -> element.copy(position = clampedPosition)
                            is ResumeElement.CertificationElement -> element.copy(position = clampedPosition)
                            is ResumeElement.LanguageElement -> element.copy(position = clampedPosition)
                        }
                        viewModel.updateElement(updatedElement)
                    },
                    onOpenProperties = { showPropertyPanel = true },
                    onZoomChange = { viewModel.setZoomLevel(it) },
                    onExitMoveMode = { viewModel.toggleMoveMode() }
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

            // Layers Panel (right side) - shown when toggled
            if (showLayersPanel) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    tonalElevation = 2.dp
                ) {
                    LayersPanel(
                        elements = gridResume.pages.firstOrNull()?.elements ?: emptyList(),
                        selectedElementId = selectedElement?.id,
                        onSelectElement = { viewModel.selectElement(it) },
                        onToggleVisibility = { viewModel.toggleElementVisibility(it) },
                        onToggleLock = { viewModel.toggleElementLock(it) },
                        onMoveLayer = { from, to -> viewModel.moveElementLayer(from, to) },
                        onMoveToContainer = { elementId, containerId -> viewModel.moveElementToContainer(elementId, containerId) },
                        onMoveOut = { elementId -> viewModel.moveElementOut(elementId) },
                        onClose = { showLayersPanel = false }
                    )
                }
            }

            // Property panel (right side) - shown when element is selected
            // Only show if Layers Panel is NOT shown (to avoid clutter), or stack them?
            // Let's show Property Panel only if Layers Panel is hidden, or maybe allow side-by-side?
            // For mobile/tablet, side-by-side might be too much. Let's prioritize Layers Panel if open.
            if (showPropertyPanel && selectedElement != null && !showLayersPanel) {
                Surface(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight(),
                    tonalElevation = 2.dp
                ) {
                    PropertyPanel(
                        element = selectedElement!!,
                        onUpdateElement = { viewModel.updateElement(it) },
                        onClose = {
                            showPropertyPanel = false
                            viewModel.deselectElement()
                        },
                        onRemoveElement = {
                            viewModel.removeElement(selectedElement!!.id)
                            showPropertyPanel = false
                        }
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
                viewModel.resetPdfExportState()
            },
            onExport = {
                // Create a temporary file URI for the PDF
                val fileName = "resume_${System.currentTimeMillis()}.pdf"
                val file = java.io.File(context.cacheDir, fileName)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                viewModel.exportToPdf(file, uri)
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
    isApplyingData: Boolean = false,
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    onExport: () -> Unit,
    onPreview: () -> Unit,
    onSwitchMode: () -> Unit,
    onShowTemplates: () -> Unit,
    onApplyTemplateData: () -> Unit = {}
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
            // Save indicator (shows status, manual save on click)
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

            // Preview
            IconButton(onClick = onPreview) {
                Icon(Icons.Default.Visibility, contentDescription = "Preview")
            }

            // More options menu
            var showMenu by remember { mutableStateOf(false) }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    // Apply Template Data (debug only)
                    if (BuildConfig.DEBUG) {
                        DropdownMenuItem(
                            text = { Text("Apply User Data") },
                            onClick = {
                                showMenu = false
                                onApplyTemplateData()
                            },
                            enabled = !isApplyingData,
                            leadingIcon = {
                                if (isApplyingData) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.PersonAdd, null)
                                }
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("Switch to Form Editor") },
                        onClick = {
                            showMenu = false
                            onSwitchMode()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )

                    DropdownMenuItem(
                        text = { Text("Templates") },
                        onClick = {
                            showMenu = false
                            onShowTemplates()
                        },
                        leadingIcon = { Icon(Icons.Default.Dashboard, null) }
                    )

                    DropdownMenuItem(
                        text = { Text("Export as PDF") },
                        onClick = {
                            showMenu = false
                            onExport()
                        },
                        leadingIcon = { Icon(Icons.Default.FileDownload, null) }
                    )
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
    isMoveMode: Boolean,
    showLayersPanel: Boolean,
    onToggleGrid: () -> Unit,
    onToggleMoveMode: () -> Unit,
    onToggleLayers: () -> Unit,
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

                // Move Mode (Pan & Zoom)
                FilterChip(
                    selected = isMoveMode,
                    onClick = onToggleMoveMode,
                    label = { Text("Move") },
                    leadingIcon = {
                        Icon(
                            IconAliases.Move,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                // Layers Toggle
                FilterChip(
                    selected = showLayersPanel,
                    onClick = onToggleLayers,
                    label = { Text("Layers") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Layers,
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
    isMoveMode: Boolean,
    onElementSelect: (ResumeElement) -> Unit,
    onElementDeselect: () -> Unit,
    onDragStart: (ResumeElement) -> Unit,
    onDrag: (ResumeElement, GridPosition) -> Unit,
    onDragEnd: (ResumeElement, GridPosition) -> Unit,
    onResize: (ResumeElement, GridPosition) -> Unit,
    onOpenProperties: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onExitMoveMode: () -> Unit
) {
    val density = LocalDensity.current.density
    val cellSizePx = gridResume.gridConfig.cellSizeDp * density
    val (gridWidthPx, gridHeightPx) = GridUtils.getGridSizePx(
        gridResume.gridConfig,
        cellSizePx
    )

    // Track zoom level
    var currentZoom by remember { mutableFloatStateOf(zoomLevel) }
    
    // Pan offset for move mode
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Scroll states for non-move mode
    val horizontalScrollState = rememberScrollState()
    val verticalScrollState = rememberScrollState()

    // 1. Use BoxWithConstraints to get the REAL canvas size (excluding top/bottom bars)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()
        val viewportWidthDp = maxWidth
        val viewportHeightDp = maxHeight

        // Helper to clamp pan offset
        fun clampPan(panX: Float, panY: Float, zoom: Float): Pair<Float, Float> {
            val pageW = gridWidthPx * zoom
            val pageH = gridHeightPx * zoom
            
            val newX = if (pageW <= viewportWidthPx) {
                (viewportWidthPx - pageW) / 2f
            } else {
                panX.coerceIn(viewportWidthPx - pageW, 0f)
            }
            
            val newY = if (pageH <= viewportHeightPx) {
                (viewportHeightPx - pageH) / 2f
            } else {
                panY.coerceIn(viewportHeightPx - pageH, 0f)
            }
            
            return newX to newY
        }

        // Sync zoom from ViewModel and handle zoom anchor (center of viewport)
        LaunchedEffect(zoomLevel) {
            if (currentZoom != zoomLevel) {
                val oldZoom = currentZoom
                val newZoom = zoomLevel
                val zoomFactor = if (oldZoom > 0) newZoom / oldZoom else 1f
                
                if (isMoveMode) {
                    // Zoom to center of viewport
                    val cx = viewportWidthPx / 2f
                    val cy = viewportHeightPx / 2f
                    val targetPanX = cx - (cx - panOffsetX) * zoomFactor
                    val targetPanY = cy - (cy - panOffsetY) * zoomFactor
                    
                    val (clampedX, clampedY) = clampPan(targetPanX, targetPanY, newZoom)
                    panOffsetX = clampedX
                    panOffsetY = clampedY
                } else {
                    // Adjust scroll to keep center
                    val cx = viewportWidthPx / 2f
                    val cy = viewportHeightPx / 2f
                    
                    val scrollX = horizontalScrollState.value
                    val scrollY = verticalScrollState.value
                    
                    val newScrollX = ((scrollX + cx) * zoomFactor - cx).roundToInt()
                    val newScrollY = ((scrollY + cy) * zoomFactor - cy).roundToInt()
                    
                    horizontalScrollState.scrollTo(newScrollX)
                    verticalScrollState.scrollTo(newScrollY)
                }
                currentZoom = zoomLevel
            }
        }

        // Continuous Sync: Keep the "inactive" state updated so it's ready when we switch modes
        if (isMoveMode) {
            // We are in Move Mode -> Keep ScrollState updated
            LaunchedEffect(panOffsetX, panOffsetY, viewportWidthPx, viewportHeightPx, currentZoom) {
                val pageWidthPx = gridWidthPx * currentZoom
                val pageHeightPx = gridHeightPx * currentZoom
                val innerWidth = maxOf(viewportWidthPx, pageWidthPx)
                val innerHeight = maxOf(viewportHeightPx, pageHeightPx)
                
                val newScrollX = ((innerWidth - pageWidthPx) / 2f - panOffsetX).roundToInt()
                val newScrollY = ((innerHeight - pageHeightPx) / 2f - panOffsetY).roundToInt()
                
                horizontalScrollState.scrollTo(newScrollX)
                verticalScrollState.scrollTo(newScrollY)
            }
        } else {
            // We are in Scroll Mode -> Keep PanOffset updated
            val scrollX = horizontalScrollState.value
            val scrollY = verticalScrollState.value
            
            LaunchedEffect(scrollX, scrollY, viewportWidthPx, viewportHeightPx, currentZoom) {
                val pageWidthPx = gridWidthPx * currentZoom
                val pageHeightPx = gridHeightPx * currentZoom
                val innerWidth = maxOf(viewportWidthPx, pageWidthPx)
                val innerHeight = maxOf(viewportHeightPx, pageHeightPx)
                
                val targetPanX = (innerWidth - pageWidthPx) / 2f - scrollX
                val targetPanY = (innerHeight - pageHeightPx) / 2f - scrollY
                
                // We don't clamp here because ScrollState is already valid/clamped by definition
                panOffsetX = targetPanX
                panOffsetY = targetPanY
            }
        }

        // 2. Gesture Handler
        val gestureModifier = Modifier.pointerInput(isMoveMode) {
            awaitEachGesture {
                val firstDown = awaitFirstDown(requireUnconsumed = false)
                var isZooming = false

                do {
                    val event = awaitPointerEvent()
                    val pointerCount = event.changes.size

                    if (pointerCount >= 2) {
                        val zoom = event.calculateZoom()
                        val centroid = event.calculateCentroid(useCurrent = true)
                        if (zoom != 1f) {
                            isZooming = true
                            val oldZoom = currentZoom
                            val newZoom = (currentZoom * zoom).coerceIn(0.25f, 2f)
                            val zoomFactor = newZoom / oldZoom

                            if (isMoveMode) {
                                val cx = centroid.x
                                val cy = centroid.y
                                val targetPanX = cx - (cx - panOffsetX) * zoomFactor
                                val targetPanY = cy - (cy - panOffsetY) * zoomFactor
                                
                                val (clampedX, clampedY) = clampPan(targetPanX, targetPanY, newZoom)
                                panOffsetX = clampedX
                                panOffsetY = clampedY
                            }
                            
                            currentZoom = newZoom
                            onZoomChange(currentZoom)
                            event.changes.forEach { it.consume() }
                        }
                    } else if (pointerCount == 1 && !isZooming && isMoveMode) {
                        val change = event.changes.first()
                        if (change.positionChanged()) {
                            val targetPanX = panOffsetX + change.positionChange().x
                            val targetPanY = panOffsetY + change.positionChange().y
                            
                            val (clampedX, clampedY) = clampPan(targetPanX, targetPanY, currentZoom)
                            panOffsetX = clampedX
                            panOffsetY = clampedY
                            
                            change.consume()
                        }
                    }
                } while (event.changes.any { it.pressed })
            }
        }

        // 3. The Scrollable Container (Active only when NOT in move mode)
        // We apply scroll here, but we ensure the inner content fills the viewport to allow centering
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(gestureModifier)
                .then(
                    if (!isMoveMode) {
                        Modifier
                            .horizontalScroll(horizontalScrollState, enabled = false)
                            .verticalScroll(verticalScrollState, enabled = false)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    horizontalScrollState.dispatchRawDelta(-dragAmount.x)
                                    verticalScrollState.dispatchRawDelta(-dragAmount.y)
                                }
                            }
                    } else {
                        Modifier
                    }
                ),
            // Crucial: If not in move mode, align content to center of the scroll view
            contentAlignment = if (!isMoveMode) Alignment.Center else Alignment.TopStart
        ) {
            // 4. The Page Wrapper
            // In scroll mode: We force this box to be at least the size of the viewport.
            // This ensures that if the page is small, it sits in the center of the screen.
            // In move mode: We use offset.
            Box(
                modifier = Modifier
                    .then(
                        if (isMoveMode) {
                             // Just wrap content, position is handled by offset
                            Modifier.wrapContentSize(Alignment.TopStart, unbounded = true)
                        } else {
                            // Force minimum size to allow centering within scroll
                            Modifier.defaultMinSize(
                                minWidth = viewportWidthDp, 
                                minHeight = viewportHeightDp
                            )
                        }
                    ),
                contentAlignment = Alignment.Center // Centers the actual resume page inside this wrapper
            ) {
                // 5. The Actual Resume Page
                Box(
                    modifier = Modifier
                        .then(
                            if (isMoveMode) {
                                Modifier.offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        x = panOffsetX.roundToInt(),
                                        y = panOffsetY.roundToInt()
                                    )
                                }
                            } else {
                                Modifier
                            }
                        )
                        .size(
                            width = GridUtils.pxToDp(gridWidthPx, density) * currentZoom,
                            height = GridUtils.pxToDp(gridHeightPx, density) * currentZoom
                        )
                        .background(Color.White)
                        .pointerInput(isMoveMode) {
                            if (isMoveMode) {
                                detectTapGestures(onDoubleTap = { onExitMoveMode() })
                            } else {
                                detectTapGestures(onTap = { onElementDeselect() })
                            }
                        }
                ) {
                    // Grid Lines
                    GridBackground(
                        gridConfig = gridResume.gridConfig,
                        zoomLevel = currentZoom
                    )

                    // Elements
                    gridResume.pages.firstOrNull()?.let { page ->
                        // Identify children to exclude from top-level rendering
                        val childIds = remember(page.elements) {
                            page.elements.filterIsInstance<ResumeElement.ContainerElement>()
                                .flatMap { it.children }
                                .toSet()
                        }

                        page.elementsByZIndex()
                            .filter { it.isVisible && !childIds.contains(it.id) } // Only render visible top-level elements
                            .forEach { element ->
                            key(element.id) {
                                val isSelected = selectedElement?.id == element.id
                                val isDragging = draggedElement?.element?.id == element.id

                                DraggableElement(
                                    element = element,
                                    gridConfig = gridResume.gridConfig,
                                    zoomLevel = currentZoom,
                                    isSelected = isSelected && !isMoveMode,
                                    isDragging = isDragging && !isMoveMode,
                                    enabled = !isMoveMode,
                                    onDragStart = onDragStart,
                                    onDrag = onDrag,
                                    onDragEnd = onDragEnd,
                                    onResize = onResize,
                                    onSelect = onElementSelect,
                                    onDeselect = onElementDeselect,
                                    onOpenProperties = { _ -> onOpenProperties() }
                                ) {
                                    when (element) {
                                        is ResumeElement.TextElement -> {
                                            TextElementRenderer(
                                                element = element,
                                                isEditing = false,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.ImageElement -> {
                                            ImageElementRenderer(
                                                element = element
                                            )
                                        }
                                        is ResumeElement.ShapeElement -> {
                                            ShapeElementRenderer(
                                                element = element
                                            )
                                        }
                                        is ResumeElement.ContactElement -> {
                                            ContactElementRenderer(
                                                element = element,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.WorkExperienceElement -> {
                                            WorkExperienceElementRenderer(
                                                element = element,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.EducationElement -> {
                                            EducationElementRenderer(
                                                element = element,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.SkillElement -> {
                                            SkillElementRenderer(
                                                element = element,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.ProjectElement -> {
                                            ProjectElementRenderer(
                                                element = element,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.CertificationElement -> {
                                            CertificationElementRenderer(
                                                element = element,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.LanguageElement -> {
                                            LanguageElementRenderer(
                                                element = element,
                                                zoomLevel = currentZoom
                                            )
                                        }
                                        is ResumeElement.ContainerElement -> {
                                            // Resolve children
                                            val children = remember(element.children, page.elements) {
                                                element.children.mapNotNull { childId ->
                                                    page.elements.find { it.id == childId }
                                                }
                                            }
                                            
                                            ContainerElementRenderer(
                                                element = element,
                                                children = children,
                                                gridConfig = gridResume.gridConfig,
                                                zoomLevel = currentZoom,
                                                renderChild = { child ->
                                                    // Recursive rendering for children
                                                    // Note: We don't support nested dragging inside canvas yet, only via Layers Panel
                                                    // So we just render the child content
                                                    when (child) {
                                                        is ResumeElement.TextElement -> TextElementRenderer(
                                                            element = child,
                                                            isEditing = false,
                                                            zoomLevel = currentZoom
                                                        )
                                                        is ResumeElement.ImageElement -> ImageElementRenderer(child)
                                                        is ResumeElement.ShapeElement -> ShapeElementRenderer(child)
                                                        is ResumeElement.ContactElement -> ContactElementRenderer(child, currentZoom)
                                                        is ResumeElement.WorkExperienceElement -> WorkExperienceElementRenderer(child, currentZoom)
                                                        is ResumeElement.EducationElement -> EducationElementRenderer(child, currentZoom)
                                                        is ResumeElement.SkillElement -> SkillElementRenderer(child, currentZoom)
                                                        is ResumeElement.ProjectElement -> ProjectElementRenderer(child, currentZoom)
                                                        is ResumeElement.CertificationElement -> CertificationElementRenderer(child, currentZoom)
                                                        is ResumeElement.LanguageElement -> LanguageElementRenderer(child, currentZoom)
                                                        // Handle nested containers if needed (recursion)
                                                        is ResumeElement.ContainerElement -> {
                                                            // Simple recursion for nested containers
                                                            // Note: This might hit recursion depth limits if circular, but ViewModel prevents circular
                                                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray.copy(alpha = 0.5f)))
                                                        }
                                                        else -> Box(modifier = Modifier.fillMaxSize())
                                                    }
                                                }
                                            )
                                        }
                                        else -> {
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

                    // Drag Ghost
                    draggedElement?.let { drag ->
                        DragGhost(
                            element = drag.element,
                            position = drag.currentPosition,
                            gridConfig = gridResume.gridConfig,
                            zoomLevel = currentZoom,
                            isValid = drag.isValidPosition
                        ) {}
                    }
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
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                item {
                    ElementTypeButton(
                        icon = IconAliases.TextFields,
                        label = "Text",
                        onClick = { onElementTypeSelected(ElementType.TEXT) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.Image,
                        label = "Image",
                        onClick = { onElementTypeSelected(ElementType.IMAGE) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = IconAliases.RectangleFilled,
                        label = "Shape",
                        onClick = { onElementTypeSelected(ElementType.SHAPE) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.HorizontalRule,
                        label = "Divider",
                        onClick = { onElementTypeSelected(ElementType.DIVIDER) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = IconAliases.BarChart,
                        label = "Chart",
                        onClick = { onElementTypeSelected(ElementType.CHART) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.Contacts,
                        label = "Contact Info",
                        onClick = { onElementTypeSelected(ElementType.CONTACT) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.Work,
                        label = "Work Experience",
                        onClick = { onElementTypeSelected(ElementType.WORK_EXPERIENCE) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.School,
                        label = "Education",
                        onClick = { onElementTypeSelected(ElementType.EDUCATION) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.Stars,
                        label = "Skills",
                        onClick = { onElementTypeSelected(ElementType.SKILL) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.Code,
                        label = "Projects",
                        onClick = { onElementTypeSelected(ElementType.PROJECT) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.EmojiEvents,
                        label = "Certifications",
                        onClick = { onElementTypeSelected(ElementType.CERTIFICATION) }
                    )
                }
                item {
                    ElementTypeButton(
                        icon = Icons.Default.Translate,
                        label = "Languages",
                        onClick = { onElementTypeSelected(ElementType.LANGUAGE) }
                    )
                }
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
    val Move = Icons.Default.OpenWith
}
