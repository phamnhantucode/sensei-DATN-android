package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

/**
 * ViewModel for the grid-based resume editor
 */
class GridEditorViewModel(context: Context) : ViewModel() {

    private val repository = ResumeRepository.getInstance(context)

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
                // Try to load latest resume
                val result = repository.getLatestResume()
                val formResume = result.getOrNull()

                if (formResume != null) {
                    // Convert form resume to grid resume
                    _gridResume.value = formResume.toGridResume()
                } else {
                    // Create new resume with default template
                    _gridResume.value = createDefaultResume()
                }
            } catch (e: Exception) {
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
                // For now, we'll save a simplified version
                // In a real implementation, you'd save the GridResume to a separate table
                // or convert it back to form Resume

                // Convert to form resume for compatibility
                val formResume = _gridResume.value.toFormResume()
                val result = repository.saveResume(formResume, syncToRemote = true)

                if (result.isSuccess) {
                    _events.emit(GridEditorEvent.SaveSuccess("Resume saved successfully"))
                } else {
                    _events.emit(GridEditorEvent.SaveError("Failed to save resume"))
                }
            } catch (e: Exception) {
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
        _selectedElement.value = element
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
        _draggedElement.value = DragState(
            element = element,
            originalPosition = element.position,
            currentPosition = element.position,
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

        if (dragState.isValidPosition && finalPosition != dragState.originalPosition) {
            saveToUndoStack()

            // Update element position
            val updatedElement = updateElementPositionValue(dragState.element, finalPosition)
            val currentPage = _gridResume.value.pages.firstOrNull() ?: return

            val updatedPage = currentPage.updateElement(updatedElement.id) { updatedElement }
            updatePage(updatedPage)

            // Update selection
            if (_selectedElement.value?.id == updatedElement.id) {
                _selectedElement.value = updatedElement
            }

            triggerAutoSave()
        }

        _draggedElement.value = null
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
                    elements = listOf(
                        // Add a default title text element
                        ResumeElement.TextElement(
                            position = GridPosition(0, 0, 2, 12),
                            content = "Your Name",
                            textStyle = TextStyle(
                                fontSize = 32f,
                                fontWeight = FontWeight.Bold,
                                color = 0xFF000000
                            ),
                            alignment = TextAlignment.CENTER
                        )
                    )
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
            ElementType.TEXT -> Pair(2, 6) // 2 rows, 6 cols
            ElementType.IMAGE -> Pair(4, 4) // Square
            ElementType.SHAPE -> Pair(1, 12) // Full width line
            ElementType.CHART -> Pair(3, 6) // Rectangular
            ElementType.CONTAINER -> Pair(4, 6)
            ElementType.ICON -> Pair(1, 1) // Single cell
            ElementType.DIVIDER -> Pair(1, 12) // Full width thin line
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
}
