package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.CertificationElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ContactElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ContainerElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ContainerElementRendererWithLayout
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.EducationElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ImageElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.LanguageElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ProjectElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.ShapeElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.SkillElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.TextElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.elements.WorkExperienceElementRenderer
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ElementType
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridPosition
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.utils.elementsByZIndex
import kotlinx.coroutines.launch
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState
import kotlin.math.roundToInt

/**
 * Main grid-based resume editor screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridEditorScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: () -> Unit,
    onSwitchToFormEditor: () -> Unit,
    viewModel: GridEditorViewModel = viewModel(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val density = LocalDensity.current.density
    val configuration = LocalConfiguration.current
    val gridResume by viewModel.gridResume.collectAsState()
    val selectedElement by viewModel.selectedElement.collectAsState()
    val draggedElement by viewModel.draggedElement.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val pdfExportState by viewModel.pdfExportState.collectAsState()
    val imageExportState by viewModel.imageExportState.collectAsState()
    val zoomLevel by viewModel.zoomLevel.collectAsState()
    val isMoveMode by viewModel.isMoveMode.collectAsState()

    var showPropertyPanel by remember { mutableStateOf(false) }
    var showLayersPanel by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showElementPicker by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImageExportDialog by remember { mutableStateOf(false) }
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
                onExportImage = { showImageExportDialog = true },
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
                    onDragStart = { element -> 
                        // Calculate originalY for vertical layout to ensure smooth dragging
                        var originalY = 0f
                        val allElements = gridResume.pages.firstOrNull()?.elements ?: emptyList()
                        val parent = allElements.filterIsInstance<ResumeElement.ContainerElement>()
                            .find { it.children.contains(element.id) }
                            
                        if (parent != null && parent.effectiveLayoutMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode.VERTICAL) {
                             val currentIndex = parent.children.indexOf(element.id)
                             for (i in 0 until currentIndex) {
                                val childId = parent.children[i]
                                val childElement = allElements.find { it.id == childId }
                                if (childElement != null) {
                                    val childCellSizePx = gridResume.gridConfig.cellSizeDp * density * zoomLevel
                                    val childHeightPx = if (childElement.position.heightMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.SizeMode.WRAP_CONTENT) {
                                        val cachedHeight = childElement.position.cachedHeightDp
                                        cachedHeight?.let { it * density * zoomLevel } ?: (childElement.position.rowSpan * childCellSizePx)
                                    } else {
                                        childElement.position.rowSpan * childCellSizePx
                                    }
                                    originalY += childHeightPx
                                }
                             }
                        }
                        viewModel.startDrag(element, originalY) 
                    },
                    onDrag = { element: ResumeElement, position: GridPosition, offsetY: Float ->
                        // Pass offsetY and density for accurate vertical container reordering
                        viewModel.updateDragPosition(position, offsetY, density)

                        // Check if dragging over any unlocked container
                        val currentPage = gridResume.pages.firstOrNull()
                        if (currentPage != null) {
                            val hoveredContainer = currentPage.elements
                                .filterIsInstance<ResumeElement.ContainerElement>()
                                .filter { it.locked && it.id != element.id }
                                .firstOrNull { container ->
                                    // Check if drag position overlaps with container position
                                    position.overlaps(container.position)
                                }

                            if (hoveredContainer != null) {
                                viewModel.onDragOverContainer(hoveredContainer.id, context)
                            } else {
                                viewModel.cancelHoverTimer()
                            }
                        }
                    },
                    onDragEnd = { element, position ->
                        viewModel.endDrag(position)
                    },
                    onResize = { element, newPosition ->
                        // Clamp position to ensure it stays within bounds
                        val clampedPosition =
                            GridUtils.clampPosition(newPosition, gridResume.gridConfig)

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

            // Right side panels (Layers and Properties)
            if (showLayersPanel || (showPropertyPanel && selectedElement != null)) {
                Box(
                    modifier = Modifier
                        .width(300.dp)
                        .fillMaxHeight()
                ) {
                    // Layers Panel
                    if (showLayersPanel) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            tonalElevation = 2.dp
                        ) {
                            LayersPanel(
                                elements = gridResume.pages.firstOrNull()?.elements ?: emptyList(),
                                selectedElementId = selectedElement?.id,
                                onSelectElement = { viewModel.selectElement(it) },
                                onToggleVisibility = { viewModel.toggleElementVisibility(it) },
                                onToggleLock = { viewModel.toggleElementLock(it) },
                                onMoveLayer = { from, to -> viewModel.moveElementLayer(from, to) },
                                onMoveToContainer = { elementId, containerId ->
                                    viewModel.moveElementToContainer(
                                        elementId,
                                        containerId
                                    )
                                },
                                onMoveOut = { elementId -> viewModel.moveElementOut(elementId) },
                                onOpenProperties = { showPropertyPanel = true },
                                onClose = { showLayersPanel = false }
                            )
                        }
                    }

                    // Property panel - overlays layers if both are open
                    if (showPropertyPanel && selectedElement != null) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            tonalElevation = 2.dp
                        ) {
                            // Find parent container
                            val allElements = gridResume.pages.firstOrNull()?.elements ?: emptyList()
                            val parentContainer = allElements.filterIsInstance<ResumeElement.ContainerElement>()
                                .find { it.children.contains(selectedElement!!.id) }

                            PropertyPanel(
                                element = selectedElement!!,
                                parentContainer = parentContainer,
                                onUpdateElement = { viewModel.updateElement(it) },
                                onClose = {
                                    showPropertyPanel = false
                                    viewModel.deselectElement()
                                },
                                onRemoveElement = {
                                    viewModel.removeElement(selectedElement!!.id)
                                    showPropertyPanel = false
                                },
                                onUpdateContainerLayoutMode = { container, mode ->
                                    viewModel.updateContainerLayoutMode(container, mode)
                                }
                            )
                        }
                    }
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
                context.startActivity(
                    android.content.Intent.createChooser(
                        intent,
                        "Share Resume PDF"
                    )
                )
            }
        )
    }

    // Image Export dialog
    if (showImageExportDialog) {
        ImageExportDialog(
            exportState = imageExportState,
            onDismiss = {
                showImageExportDialog = false
                viewModel.resetImageExportState()
            },
            onExport = {
                // Create a temporary file URI for the Image
                val fileName = "resume_${System.currentTimeMillis()}.png"
                val file = java.io.File(context.cacheDir, fileName)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                viewModel.exportToImage(file, uri)
            },
            onShare = { uri ->
                // Share the Image using Android share sheet
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    android.content.Intent.createChooser(
                        intent,
                        "Share Resume Image"
                    )
                )
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
    onExportImage: () -> Unit,
    onPreview: () -> Unit,
    onSwitchMode: () -> Unit,
    onShowTemplates: () -> Unit,
    onApplyTemplateData: () -> Unit = {},
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

                    DropdownMenuItem(
                        text = { Text("Export as Image") },
                        onClick = {
                            showMenu = false
                            onExportImage()
                        },
                        leadingIcon = { Icon(Icons.Default.Image, null) }
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
    onRedo: () -> Unit,
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
    onDrag: (ResumeElement, GridPosition, Float) -> Unit,  // Added Float parameter for offsetY
    onDragEnd: (ResumeElement, GridPosition) -> Unit,
    onResize: (ResumeElement, GridPosition) -> Unit,
    onOpenProperties: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onExitMoveMode: () -> Unit,
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
                                                // Center dividers and lines vertically in their container
                                                val modifier = if (element.shapeType == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ShapeType.DIVIDER || 
                                                                  element.shapeType == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ShapeType.LINE) {
                                                    Modifier.align(Alignment.Center)
                                                } else {
                                                    Modifier
                                                }

                                                ShapeElementRenderer(
                                                    element = element,
                                                    modifier = modifier,
                                                    isSelected = isSelected
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
                                                val children =
                                                    remember(element.children, page.elements) {
                                                        element.children.mapNotNull { childId ->
                                                            page.elements.find { it.id == childId }
                                                        }
                                                    }

                                                // Get hover progress if this container is being hovered
                                                val hoverProgress =
                                                    if (draggedElement?.hoveredContainerId == element.id) {
                                                        draggedElement.hoverProgress
                                                    } else {
                                                        0f
                                                    }

                                                // Check if vertical layout mode
                                                val isVerticalLayout = element.effectiveLayoutMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode.VERTICAL

                                                if (isVerticalLayout) {
                                                    // Vertical layout mode - stack children vertically
                                                    ContainerElementRendererWithLayout(
                                                        element = element,
                                                        hoverProgress = hoverProgress
                                                    ) {
                                                        // Check if currently dragging a child of this container
                                                        val isDraggingChild = draggedElement?.let { drag ->
                                                            element.children.contains(drag.element.id)
                                                        } ?: false

                                                        val targetIndex = draggedElement?.targetInsertionIndex
                                                        val draggedChildId = if (isDraggingChild) draggedElement?.element?.id else null

                                                        for ((index, child) in children.withIndex()) {
                                                            val isInteractive = element.locked

                                                            // Calculate animation offset for this child
                                                            val animationOffset = if (isDraggingChild && draggedChildId != null && child.id != draggedChildId && targetIndex != null) {
                                                                // Find original index of dragged element
                                                                val draggedOriginalIndex = element.children.indexOf(draggedChildId)
                                                                val currentChildIndex = element.children.indexOf(child.id)

                                                                // If target is lower (moving down) and this child is between original and target
                                                                if (targetIndex > draggedOriginalIndex && currentChildIndex > draggedOriginalIndex && currentChildIndex <= targetIndex) {
                                                                    // Shift this element up (negative offset)
                                                                    val draggedElement = children.find { it.id == draggedChildId }
                                                                    if (draggedElement != null) {
                                                                        val cellSizePx = gridResume.gridConfig.cellSizeDp * density * currentZoom
                                                                        val draggedHeightPx = if (draggedElement.position.heightMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.SizeMode.WRAP_CONTENT) {
                                                                            draggedElement.position.cachedHeightDp?.let { it * density * currentZoom } ?: (draggedElement.position.rowSpan * cellSizePx)
                                                                        } else {
                                                                            draggedElement.position.rowSpan * cellSizePx
                                                                        }
                                                                        -draggedHeightPx
                                                                    } else 0f
                                                                }
                                                                // If target is higher (moving up) and this child is between target and original
                                                                else if (targetIndex < draggedOriginalIndex && currentChildIndex >= targetIndex && currentChildIndex < draggedOriginalIndex) {
                                                                    // Shift this element down (positive offset)
                                                                    val draggedElement = children.find { it.id == draggedChildId }
                                                                    if (draggedElement != null) {
                                                                        val cellSizePx = gridResume.gridConfig.cellSizeDp * density * currentZoom
                                                                        val draggedHeightPx = if (draggedElement.position.heightMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.SizeMode.WRAP_CONTENT) {
                                                                            draggedElement.position.cachedHeightDp?.let { it * density * currentZoom } ?: (draggedElement.position.rowSpan * cellSizePx)
                                                                        } else {
                                                                            draggedElement.position.rowSpan * cellSizePx
                                                                        }
                                                                        draggedHeightPx
                                                                    } else 0f
                                                                } else {
                                                                    0f
                                                                }
                                                            } else {
                                                                0f
                                                            }

                                                            // Animate the offset smoothly
                                                            val animatedOffsetY by animateFloatAsState(
                                                                targetValue = animationOffset,
                                                                animationSpec = tween(
                                                                    durationMillis = 300,
                                                                    easing = FastOutSlowInEasing
                                                                ),
                                                                label = "childReorderOffset_${child.id}"
                                                            )

                                                            key(child.id) {
                                                                    DraggableElement(
                                                                        element = child,
                                                                        gridConfig = gridResume.gridConfig,
                                                                        zoomLevel = currentZoom,
                                                                        isSelected = selectedElement?.id == child.id && !isMoveMode,
                                                                        isDragging = draggedElement?.element?.id == child.id && !isMoveMode,
                                                                        enabled = isInteractive && !isMoveMode,
                                                                        useRelativePositioning = true,
                                                                        animationOffsetY = animatedOffsetY,
                                                                        onDragStart = onDragStart,
                                                                        onDrag = onDrag,
                                                                        onDragEnd = onDragEnd,
                                                                        onResize = onResize,
                                                                        onSelect = onElementSelect,
                                                                        onDeselect = onElementDeselect,
                                                                        onOpenProperties = { _ -> onOpenProperties() },
                                                                        maxColumns = element.position.colSpan,
                                                                        maxRows = element.position.rowSpan,
                                                                        containerWidth = (element.position.colSpan * gridResume.gridConfig.cellSizeDp * density * currentZoom) - 
                                                                                ((element.padding.left + element.padding.right) * density * currentZoom)
                                                                    ) {
                                                                    // Render child content
                                                                    when (child) {
                                                                        is ResumeElement.TextElement -> TextElementRenderer(
                                                                            element = child,
                                                                            isEditing = false,
                                                                            zoomLevel = currentZoom
                                                                        )

                                                                        is ResumeElement.ImageElement -> ImageElementRenderer(
                                                                            child
                                                                        )

                                                                        is ResumeElement.ShapeElement -> ShapeElementRenderer(
                                                                            child,
                                                                            isSelected = selectedElement?.id == child.id && !isMoveMode
                                                                        )

                                                                        is ResumeElement.ContactElement -> ContactElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.WorkExperienceElement -> WorkExperienceElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.EducationElement -> EducationElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.SkillElement -> SkillElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.ProjectElement -> ProjectElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.CertificationElement -> CertificationElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.LanguageElement -> LanguageElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.ContainerElement -> {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .fillMaxSize()
                                                                                    .background(
                                                                                        Color.LightGray.copy(
                                                                                            alpha = 0.5f
                                                                                        )
                                                                                    )
                                                                            )
                                                                        }

                                                                        else -> Box(modifier = Modifier.fillMaxSize())
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    // Grid or Free layout mode - absolute positioning
                                                    ContainerElementRenderer(
                                                        element = element,
                                                        hoverProgress = hoverProgress
                                                    ) {
                                                        // Render children manually here
                                                        children.forEach { child ->
                                                            // Enable interaction ONLY if container is locked
                                                            val isInteractive = element.locked

                                                            key(child.id) {
                                                                DraggableElement(
                                                                    element = child,
                                                                    gridConfig = gridResume.gridConfig,
                                                                    zoomLevel = currentZoom,
                                                                    isSelected = selectedElement?.id == child.id && !isMoveMode,
                                                                    isDragging = draggedElement?.element?.id == child.id && !isMoveMode,
                                                                    enabled = isInteractive && !isMoveMode,
                                                                    onDragStart = onDragStart,
                                                                    onDrag = onDrag,
                                                                    onDragEnd = onDragEnd,
                                                                    onResize = onResize,
                                                                    onSelect = onElementSelect,
                                                                    onDeselect = onElementDeselect,
                                                                    onOpenProperties = { _ -> onOpenProperties() },
                                                                    maxColumns = element.position.colSpan,
                                                                    maxRows = element.position.rowSpan
                                                                ) {
                                                                    // Render child content
                                                                    when (child) {
                                                                        is ResumeElement.TextElement -> TextElementRenderer(
                                                                            element = child,
                                                                            isEditing = false,
                                                                            zoomLevel = currentZoom
                                                                        )

                                                                        is ResumeElement.ImageElement -> ImageElementRenderer(
                                                                            child
                                                                        )

                                                                        is ResumeElement.ShapeElement -> ShapeElementRenderer(
                                                                            child,
                                                                            isSelected = selectedElement?.id == child.id && !isMoveMode
                                                                        )

                                                                        is ResumeElement.ContactElement -> ContactElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.WorkExperienceElement -> WorkExperienceElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.EducationElement -> EducationElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.SkillElement -> SkillElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.ProjectElement -> ProjectElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.CertificationElement -> CertificationElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.LanguageElement -> LanguageElementRenderer(
                                                                            child,
                                                                            currentZoom
                                                                        )

                                                                        is ResumeElement.ContainerElement -> {
                                                                            Box(
                                                                                modifier = Modifier
                                                                                    .fillMaxSize()
                                                                                    .background(
                                                                                        Color.LightGray.copy(
                                                                                            alpha = 0.5f
                                                                                        )
                                                                                    )
                                                                            )
                                                                        }

                                                                        else -> Box(modifier = Modifier.fillMaxSize())
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            is ResumeElement.ChartElement -> Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        start = ((element.padding?.left ?: 8f) * currentZoom).dp,
                                                        top = ((element.padding?.top ?: 8f) * currentZoom).dp,
                                                        end = ((element.padding?.right ?: 8f) * currentZoom).dp,
                                                        bottom = ((element.padding?.bottom ?: 8f) * currentZoom).dp
                                                    )
                                                    .background(Color.LightGray)
                                            )

                                            is ResumeElement.IconElement -> Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(
                                                        start = ((element.padding?.left ?: 8f) * currentZoom).dp,
                                                        top = ((element.padding?.top ?: 8f) * currentZoom).dp,
                                                        end = ((element.padding?.right ?: 8f) * currentZoom).dp,
                                                        bottom = ((element.padding?.bottom ?: 8f) * currentZoom).dp
                                                    )
                                                    .background(Color.Cyan)
                                            )

                                            else -> Box(modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                    }

                    // Drag Ghost
                    draggedElement?.let { drag ->
                        // Calculate absolute offset by traversing up the parent chain
                        val allElements = gridResume.pages.firstOrNull()?.elements ?: emptyList()
                        var parent = allElements.filterIsInstance<ResumeElement.ContainerElement>()
                            .find { it.children.contains(drag.element.id) }

                        var accumulatedCol = 0
                        var accumulatedRow = 0
                        
                        // Check if immediate parent is vertical layout
                        val isInVerticalContainer = parent?.effectiveLayoutMode == com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.LayoutMode.VERTICAL

                        // For vertical containers, calculate Y offset differently
                        var verticalOffsetY = 0f
                        var containerWidthPx: Float? = null
                        
                        if (isInVerticalContainer && parent != null) {
                            // Use the captured original Y position + drag offset
                            // This ensures the ghost follows the cursor smoothly even when elements are reordered
                            verticalOffsetY = drag.originalCaptureY + drag.dragOffsetY
                            
                            // Calculate container width for ghost
                            val effectiveCellSizePx = cellSizePx * currentZoom
                            val parentWidthPx = parent.position.colSpan * effectiveCellSizePx
                            val paddingLeftPx = parent.padding.left * density * currentZoom
                            val paddingRightPx = parent.padding.right * density * currentZoom
                            containerWidthPx = parentWidthPx - paddingLeftPx - paddingRightPx
                        }

                        while (parent != null) {
                            accumulatedCol += parent.position.col
                            accumulatedRow += parent.position.row

                            val currentId = parent.id
                            parent = allElements.filterIsInstance<ResumeElement.ContainerElement>()
                                .find { it.children.contains(currentId) }
                        }

                        val effectiveCellSizePx = cellSizePx * currentZoom
                        val parentOffsetX = accumulatedCol * effectiveCellSizePx
                        val parentOffsetY = if (isInVerticalContainer) {
                            (accumulatedRow * effectiveCellSizePx) + verticalOffsetY
                        } else {
                            accumulatedRow * effectiveCellSizePx
                        }

                        Box(
                            modifier = Modifier.offset {
                                androidx.compose.ui.unit.IntOffset(
                                    x = parentOffsetX.roundToInt(),
                                    y = parentOffsetY.roundToInt()
                                )
                            }
                        ) {
                            DragGhost(
                                element = drag.element,
                                position = drag.currentPosition,
                                gridConfig = gridResume.gridConfig,
                                zoomLevel = currentZoom,
                                isValid = drag.isValidPosition,
                                useVerticalLayout = isInVerticalContainer,
                                containerWidthPx = containerWidthPx
                            ) {}
                        }
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
    onElementTypeSelected: (ElementType) -> Unit,
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
                item {
                    ElementTypeButton(
                        icon = Icons.Default.Inbox,
                        label = "Container",
                        onClick = { onElementTypeSelected(ElementType.CONTAINER) }
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
    onClick: () -> Unit,
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
    onShare: (android.net.Uri) -> Unit,
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
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
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

                else -> { /* No dismiss button during/after export */
                }
            }
        }
    )
}


/**
 * Image Export Dialog
 */
@Composable
private fun ImageExportDialog(
    exportState: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState,
    onDismiss: () -> Unit,
    onExport: () -> Unit,
    onShare: (android.net.Uri) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Export Image")
        },
        text = {
            when (exportState) {
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Idle -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Export your resume as a high-quality PNG image.")
                        Text(
                            "The image will be saved to your device.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Rendering -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Rendering image...")
                    }
                }

                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.SavingFile -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Saving image...")
                    }
                }

                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Success -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("✓ Image exported successfully!")
                        Text(
                            "Size: ${exportState.fileSizeBytes / 1024} KB",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Error -> {
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
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Idle -> {
                    Button(onClick = onExport) {
                        Text("Export")
                    }
                }

                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Success -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onShare(exportState.uri) }) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share")
                        }
                        Button(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }

                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Error -> {
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
                is com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState.Idle -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }

                else -> { /* No dismiss button during/after export */
                }
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
