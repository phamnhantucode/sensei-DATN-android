package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.AndroidPdfGenerator
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for the grid-based resume editor
 */
class GridEditorViewModel(private val context: Context) : ViewModel() {

    private val repository = ResumeRepository.getInstance(context)
    private val pdfExporter = AndroidPdfGenerator(context)
    private val sharedPreferences = context.getSharedPreferences("grid_resume_prefs", Context.MODE_PRIVATE)

    // Configure Gson with custom type adapter for sealed classes
    private val gson = GsonBuilder()
        .registerTypeAdapter(ResumeElement::class.java, ResumeElementTypeAdapter())
        .create()

    // Main state
    private val _gridResume = MutableStateFlow(GridResume())
    val gridResume: StateFlow<GridResume> = _gridResume.asStateFlow()

    // Selected element
    private val _selectedElement = MutableStateFlow<ResumeElement?>(null)
    val selectedElement: StateFlow<ResumeElement?> = _selectedElement.asStateFlow()

    // Drag state
    private val _draggedElement = MutableStateFlow<DragState?>(null)
    val draggedElement: StateFlow<DragState?> = _draggedElement.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // PDF Export state
    private val _pdfExportState = MutableStateFlow<PdfExportState>(PdfExportState.Idle)
    val pdfExportState: StateFlow<PdfExportState> = _pdfExportState.asStateFlow()

    // Zoom state - will be calculated dynamically based on screen size
    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    // Undo/Redo stacks
    private val undoStack = mutableListOf<GridResume>()
    private val redoStack = mutableListOf<GridResume>()
    private val maxHistorySize = 50

    // Auto-save
    private var autoSaveJob: Job? = null
    private var isAutoSaveEnabled = true

    // Events
    private val _events = MutableSharedFlow<GridEditorEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<GridEditorEvent> = _events.asSharedFlow()

    init {
        // Load or create default resume
        loadOrCreateResume()
    }

    // ============================================================================
    // Loading & Saving
    // ============================================================================

    /**
     * Loads the latest resume or creates a new one with default template
     */
    private fun loadOrCreateResume() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                // First, try to load the GridResume from SharedPreferences
                val savedGridResumeJson = sharedPreferences.getString("latest_grid_resume", null)

                if (savedGridResumeJson != null) {
                    // Load from SharedPreferences
                    try {
                        val savedGridResume = gson.fromJson(savedGridResumeJson, GridResume::class.java)
                        _gridResume.value = savedGridResume
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // If parsing fails, fall back to creating default
                        _gridResume.value = createDefaultResume()
                    }
                } else {
                    // No saved GridResume, try to load from form resume
                    val result = repository.getLatestResume()
                    val formResume = result.getOrNull()

                    if (formResume != null) {
                        // Convert form resume to grid resume
                        _gridResume.value = formResume.toGridResume()
                    } else {
                        // Create new resume with default template
                        _gridResume.value = createDefaultResume()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _gridResume.value = createDefaultResume()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads a specific resume by converting from form format
     */
    fun loadResume(formResume: Resume) {
        saveToUndoStack()
        _gridResume.value = formResume.toGridResume()
        _events.tryEmit(GridEditorEvent.ResumeLoaded)
    }

    /**
     * Loads a grid resume directly
     */
    fun loadGridResume(resume: GridResume) {
        saveToUndoStack()
        _gridResume.value = resume
        _events.tryEmit(GridEditorEvent.ResumeLoaded)
    }

    /**
     * Saves the current resume
     */
    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                // Save GridResume directly to SharedPreferences as JSON
                val gridResumeJson = gson.toJson(_gridResume.value)
                sharedPreferences.edit()
                    .putString("latest_grid_resume", gridResumeJson)
                    .apply()

                // Also convert to form resume for compatibility with other parts of the app
                val formResume = _gridResume.value.toFormResume()
                val result = repository.saveResume(formResume, syncToRemote = true)

                if (result.isSuccess) {
                    _events.emit(GridEditorEvent.SaveSuccess("Resume saved successfully"))
                } else {
                    _events.emit(GridEditorEvent.SaveError("Failed to save resume"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _events.emit(GridEditorEvent.SaveError("Error: ${e.message}"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Triggers auto-save after a delay
     */
    private fun triggerAutoSave() {
        if (!isAutoSaveEnabled) return

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(3000) // 3 second debounce
            save()
        }
    }

    // ============================================================================
    // Element Management
    // ============================================================================

    /**
     * Adds a new element to the canvas
     */
    fun addElement(elementType: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType) {
        saveToUndoStack()

        val currentPage = _gridResume.value.pages.firstOrNull() ?: ResumePage()

        // Find next available position
        val elementSize = getDefaultElementSize(elementType)
        val position = GridUtils.findNextAvailablePosition(
            elementSize = elementSize,
            elements = currentPage.elements,
            gridConfig = _gridResume.value.gridConfig
        ) ?: GridPosition(0, 0, elementSize.first, elementSize.second)

        // Create new element based on type
        val newElement = createElementOfType(elementType, position)

        // Add to page
        val updatedPage = currentPage.addElement(newElement)
        updatePage(updatedPage)

        // Select the new element
        _selectedElement.value = newElement

        triggerAutoSave()
    }

    /**
     * Updates an existing element
     */
    fun updateElement(updatedElement: ResumeElement) {
        saveToUndoStack()

        val currentPage = _gridResume.value.pages.firstOrNull() ?: return

        val updatedPage = currentPage.updateElement(updatedElement.id) { updatedElement }
        updatePage(updatedPage)

        // Update selection if this is the selected element
        if (_selectedElement.value?.id == updatedElement.id) {
            _selectedElement.value = updatedElement
        }

        triggerAutoSave()
    }

    /**
     * Removes an element
     */
    fun removeElement(elementId: String) {
        saveToUndoStack()

        val currentPage = _gridResume.value.pages.firstOrNull() ?: return

        val updatedPage = currentPage.removeElement(elementId)
        updatePage(updatedPage)

        // Deselect if this was the selected element
        if (_selectedElement.value?.id == elementId) {
            _selectedElement.value = null
        }

        triggerAutoSave()
    }

    /**
     * Selects an element
     */
    fun selectElement(element: ResumeElement) {
        // Always fetch the latest version of the element from the page
        // to ensure we have the most up-to-date properties
        val currentPage = _gridResume.value.pages.firstOrNull()
        val latestElement = currentPage?.elements?.find { it.id == element.id } ?: element
        _selectedElement.value = latestElement
    }

    /**
     * Deselects the current element
     */
    fun deselectElement() {
        _selectedElement.value = null
    }

    // ============================================================================
    // Drag & Drop
    // ============================================================================

    /**
     * Starts dragging an element
     */
    fun startDrag(element: ResumeElement) {
        // Get the latest version of the element from the page to ensure we have all recent changes
        val currentPage = _gridResume.value.pages.firstOrNull()
        val latestElement = currentPage?.elements?.find { it.id == element.id } ?: element

        _draggedElement.value = DragState(
            element = latestElement,
            originalPosition = latestElement.position,
            currentPosition = latestElement.position,
            isValidPosition = true
        )
    }

    /**
     * Updates drag position during drag
     */
    fun updateDragPosition(newPosition: GridPosition) {
        val currentDrag = _draggedElement.value ?: return
        val currentPage = _gridResume.value.pages.firstOrNull() ?: return

        // Check if position is valid
        val isValid = GridUtils.isValidPosition(newPosition, _gridResume.value.gridConfig) &&
                      !GridUtils.hasCollision(
                          position = newPosition,
                          elements = currentPage.elements,
                          excludeId = currentDrag.element.id
                      )

        _draggedElement.value = currentDrag.copy(
            currentPosition = newPosition,
            isValidPosition = isValid
        )
    }

    /**
     * Ends drag operation
     */
    fun endDrag(finalPosition: GridPosition) {
        val dragState = _draggedElement.value ?: return

        // Clamp position to ensure it's within bounds
        val clampedPosition = GridUtils.clampPosition(finalPosition, _gridResume.value.gridConfig)

        if (clampedPosition != dragState.originalPosition) {
            saveToUndoStack()

            val currentPage = _gridResume.value.pages.firstOrNull() ?: return

            // IMPORTANT: Get the latest element from the page, not from dragState
            // This ensures we preserve any property changes made during the drag
            val latestElementFromPage = currentPage.elements.find { it.id == dragState.element.id }

            if (latestElementFromPage != null) {
                // Update only the position, preserving all other properties
                val updatedElement = updateElementPositionValue(latestElementFromPage, clampedPosition)

                val updatedPage = currentPage.updateElement(updatedElement.id) { updatedElement }
                updatePage(updatedPage)

                // Update selection - fetch the latest version from the updated page
                if (_selectedElement.value?.id == updatedElement.id) {
                    val finalElement = updatedPage.elements.find { it.id == updatedElement.id }
                    _selectedElement.value = finalElement
                }
            }

            triggerAutoSave()
        }

        _draggedElement.value = null
    }

    // ============================================================================
    // Zoom
    // ============================================================================

    /**
     * Zoom in (increases zoom level by 10%)
     */
    fun zoomIn() {
        val newZoom = (_zoomLevel.value * 1.1f).coerceAtMost(2f)
        _zoomLevel.value = newZoom
    }

    /**
     * Zoom out (decreases zoom level by 10%)
     */
    fun zoomOut() {
        val newZoom = (_zoomLevel.value / 1.1f).coerceAtLeast(0.25f)
        _zoomLevel.value = newZoom
    }

    /**
     * Set zoom level directly (for pinch-to-zoom gestures)
     */
    fun setZoomLevel(zoom: Float) {
        _zoomLevel.value = zoom.coerceIn(0.25f, 2f)
    }

    /**
     * Reset zoom to 100%
     */
    fun resetZoom() {
        _zoomLevel.value = 1f
    }

    // ============================================================================
    // Grid Configuration
    // ============================================================================

    /**
     * Toggles grid visibility
     */
    fun toggleGrid() {
        _gridResume.update { resume ->
            resume.copy(
                gridConfig = resume.gridConfig.copy(
                    showGrid = !resume.gridConfig.showGrid
                )
            )
        }
    }

    /**
     * Toggles snap to grid
     */
    fun toggleSnap() {
        _gridResume.update { resume ->
            resume.copy(
                gridConfig = resume.gridConfig.copy(
                    snapToGrid = !resume.gridConfig.snapToGrid
                )
            )
        }
    }

    /**
     * Updates grid configuration
     */
    fun updateGridConfig(config: GridConfig) {
        _gridResume.update { resume ->
            resume.copy(gridConfig = config)
        }
    }

    // ============================================================================
    // Templates
    // ============================================================================

    /**
     * Applies a template to the resume
     */
    fun applyTemplate(template: GridTemplateType) {
        saveToUndoStack()

        // For now, just clear and recreate with template
        // In a real implementation, you'd have predefined templates
        val newResume = createResumeWithTemplate(template)
        _gridResume.value = newResume

        _events.tryEmit(GridEditorEvent.TemplateApplied(template))
        triggerAutoSave()
    }

    // ============================================================================
    // Undo/Redo
    // ============================================================================

    /**
     * Undo last action
     */
    fun undo() {
        if (undoStack.isEmpty()) return

        // Save current state to redo stack
        redoStack.add(0, _gridResume.value)
        if (redoStack.size > maxHistorySize) {
            redoStack.removeAt(redoStack.size - 1)
        }

        // Restore from undo stack
        _gridResume.value = undoStack.removeAt(0)
        _selectedElement.value = null
    }

    /**
     * Redo last undone action
     */
    fun redo() {
        if (redoStack.isEmpty()) return

        // Save current state to undo stack
        undoStack.add(0, _gridResume.value)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(undoStack.size - 1)
        }

        // Restore from redo stack
        _gridResume.value = redoStack.removeAt(0)
        _selectedElement.value = null
    }

    /**
     * Saves current state to undo stack
     */
    private fun saveToUndoStack() {
        undoStack.add(0, _gridResume.value)
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(undoStack.size - 1)
        }
        // Clear redo stack when new action is performed
        redoStack.clear()
    }

    // ============================================================================
    // Helper Functions
    // ============================================================================

    private fun updatePage(updatedPage: ResumePage) {
        _gridResume.update { resume ->
            resume.copy(
                pages = resume.pages.map { page ->
                    if (page.id == updatedPage.id) updatedPage else page
                }
            )
        }
    }

    private fun createDefaultResume(): GridResume {
        return GridResume(
            name = "New Resume",
            pages = listOf(
                ResumePage(
                    elements = emptyList()
                )
            )
        )
    }

    private fun createResumeWithTemplate(template: GridTemplateType): GridResume {
        // For MVP, just return default
        // TODO: Implement actual templates
        return createDefaultResume().copy(
            name = "Resume - ${template.name} Template"
        )
    }

    private fun createElementOfType(
        type: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType,
        position: GridPosition
    ): ResumeElement {
        return when (type) {
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.TEXT -> {
                ResumeElement.TextElement(
                    position = position,
                    content = "New Text",
                    textStyle = TextStyle(
                        fontSize = 14f,
                        color = 0xFF000000
                    )
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.IMAGE -> {
                ResumeElement.ImageElement(
                    position = position,
                    imageUrl = "",
                    contentScale = ImageScale.FIT
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.SHAPE -> {
                ResumeElement.ShapeElement(
                    position = position,
                    shapeType = ShapeType.RECTANGLE,
                    style = ElementStyle(
                        backgroundColor = 0xFFE0E0E0
                    )
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.DIVIDER -> {
                ResumeElement.ShapeElement(
                    position = position,
                    shapeType = ShapeType.DIVIDER,
                    style = ElementStyle(
                        backgroundColor = 0xFF000000
                    ),
                    customHeightDp = 2f // Thin divider line (2dp)
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.CHART -> {
                ResumeElement.ChartElement(
                    position = position,
                    chartType = ChartType.HORIZONTAL_BAR,
                    data = ChartData()
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.CONTAINER -> {
                ResumeElement.ContainerElement(
                    position = position
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.ICON -> {
                ResumeElement.IconElement(
                    position = position,
                    iconName = "star",
                    iconType = IconType.MATERIAL
                )
            }
        }
    }

    private fun getDefaultElementSize(type: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType): Pair<Int, Int> {
        return when (type) {
            ElementType.TEXT -> Pair(8, 24) // 8 rows, 24 cols
            ElementType.IMAGE -> Pair(16, 16) // Square
            ElementType.SHAPE -> Pair(4, 48) // Full width line
            ElementType.CHART -> Pair(12, 24) // Rectangular
            ElementType.CONTAINER -> Pair(16, 24)
            ElementType.ICON -> Pair(4, 4) // Single cell
            ElementType.DIVIDER -> Pair(4, 48) // Full width thin line
        }
    }

    private fun updateElementPositionValue(element: ResumeElement, position: GridPosition): ResumeElement {
        return when (element) {
            is ResumeElement.TextElement -> element.copy(position = position)
            is ResumeElement.ImageElement -> element.copy(position = position)
            is ResumeElement.ShapeElement -> element.copy(position = position)
            is ResumeElement.ChartElement -> element.copy(position = position)
            is ResumeElement.ContainerElement -> element.copy(position = position)
            is ResumeElement.IconElement -> element.copy(position = position)
        }
    }

    // ============================================================================
    // PDF Export
    // ============================================================================

    /**
     * Export resume to PDF
     */
    fun exportToPdf() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = pdfExporter.exportToDownloads(_gridResume.value)

                result.onSuccess { exportResult ->
                    _pdfExportState.value = PdfExportState.Success(exportResult.uri, exportResult.fileSizeBytes)
                    _events.emit(GridEditorEvent.PdfExportSuccess(exportResult.uri, exportResult.fileSizeBytes))
                }.onFailure { throwable ->
                    _pdfExportState.value = PdfExportState.Error(throwable, throwable.message ?: "Unknown error")
                    _events.emit(GridEditorEvent.PdfExportError(throwable.message ?: "Export failed"))
                }
            } catch (e: Exception) {
                _pdfExportState.value = PdfExportState.Error(e, e.message ?: "Unknown error")
                _events.emit(GridEditorEvent.PdfExportError(e.message ?: "Export failed"))
            }
        }
    }

    /**
     * Reset export state
     */
    fun resetExportState() {
        _pdfExportState.value = PdfExportState.Idle
    }
}

/**
 * Drag state during drag operation
 */
data class DragState(
    val element: ResumeElement,
    val originalPosition: GridPosition,
    val currentPosition: GridPosition,
    val isValidPosition: Boolean
)

/**
 * Events emitted by the ViewModel
 */
sealed class GridEditorEvent {
    object ResumeLoaded : GridEditorEvent()
    data class SaveSuccess(val message: String) : GridEditorEvent()
    data class SaveError(val message: String) : GridEditorEvent()
    data class TemplateApplied(val template: GridTemplateType) : GridEditorEvent()
    data class PdfExportSuccess(val uri: Uri, val fileSizeBytes: Long) : GridEditorEvent()
    data class PdfExportError(val message: String) : GridEditorEvent()
}
