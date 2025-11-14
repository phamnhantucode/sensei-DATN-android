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

    override fun onCleared() {
        super.onCleared()
        // Save data before ViewModel is destroyed to prevent data loss
        // Use runBlocking to ensure save completes before destruction
        try {
            // Cancel pending auto-save to avoid duplicate saves
            autoSaveJob?.cancel()

            // Perform final save synchronously
            val gridResumeJson = gson.toJson(_gridResume.value)
            sharedPreferences.edit()
                .putString("latest_grid_resume", gridResumeJson)
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
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

                // Automatically sync user data from Resume Builder for tagged elements
                // This ensures elements with tags always have the latest user information
                syncUserDataOnLoad()
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
        viewModelScope.launch(Dispatchers.IO) {
            saveToUndoStack()
            _gridResume.value = formResume.toGridResume()
            syncUserDataOnLoad()
            _events.tryEmit(GridEditorEvent.ResumeLoaded)
        }
    }

    /**
     * Loads a grid resume directly
     */
    fun loadGridResume(resume: GridResume) {
        viewModelScope.launch(Dispatchers.IO) {
            saveToUndoStack()
            _gridResume.value = resume
            syncUserDataOnLoad()
            _events.tryEmit(GridEditorEvent.ResumeLoaded)
        }
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
            delay(1500) // 1.5 second debounce - faster saves, less data loss risk
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

    /**
     * Automatically syncs user data from Resume Builder on load
     * Called after resume is loaded to ensure tagged elements have latest data
     */
    private suspend fun syncUserDataOnLoad() {
        try {
            val result = repository.getLatestResume()
            result.getOrNull()?.let { formResume ->
                // Update all tagged elements with fresh user data
                applyUserDataToTemplate(formResume, saveToUndo = false)
            }
        } catch (e: Exception) {
            // Silently fail - not critical, user can manually refresh
            e.printStackTrace()
        }
    }

    /**
     * Apply user data from Resume to tagged elements in the template
     * This replaces tagged TextElements, ImageElements, and WorkExperienceElements with actual user data
     *
     * @param resume User's resume data from Resume Builder
     * @param saveToUndo Whether to save current state to undo stack (default: true)
     */
    fun applyUserDataToTemplate(
        resume: com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume,
        saveToUndo: Boolean = true
    ) {
        if (saveToUndo) {
            saveToUndoStack()
        }

        val currentPages = _gridResume.value.pages
        val updatedPages = currentPages.map { page ->
            val updatedElements = page.elements.map { element ->
                val tag = element.userInfoTag
                if (tag == null || tag == UserInfoTag.NONE) {
                    return@map element
                }

                when (element) {
                    is ResumeElement.TextElement -> {
                        val content = when (tag) {
                            UserInfoTag.NAME -> resume.personalInfo.fullName
                            UserInfoTag.EMAIL -> resume.personalInfo.email
                            UserInfoTag.PHONE -> resume.personalInfo.phone
                            UserInfoTag.LOCATION -> resume.personalInfo.location
                            UserInfoTag.GITHUB -> resume.personalInfo.github
                            UserInfoTag.LINKEDIN -> resume.personalInfo.linkedIn
                            UserInfoTag.WEBSITE -> resume.personalInfo.portfolio
                            UserInfoTag.AVATAR -> element.content // Avatar doesn't apply to text
                            UserInfoTag.WORK_EXPERIENCE -> element.content // Work experience doesn't apply to text
                            UserInfoTag.EDUCATION -> element.content // Education doesn't apply to text
                            UserInfoTag.NONE -> element.content
                        }
                        element.copy(content = content)
                    }
                    is ResumeElement.ImageElement -> {
                        if (tag == UserInfoTag.AVATAR && resume.personalInfo.avatar.isNotEmpty()) {
                            element.copy(imageUrl = resume.personalInfo.avatar)
                        } else {
                            element
                        }
                    }
                    is ResumeElement.ContactElement -> {
                        // Update contact items with user info based on their tags
                        val updatedItems = element.items.map { item ->
                            val tag = item.userInfoTag ?: return@map item
                            val value = when (tag) {
                                UserInfoTag.NAME -> resume.personalInfo.fullName
                                UserInfoTag.EMAIL -> resume.personalInfo.email
                                UserInfoTag.PHONE -> resume.personalInfo.phone
                                UserInfoTag.LOCATION -> resume.personalInfo.location
                                UserInfoTag.GITHUB -> resume.personalInfo.github
                                UserInfoTag.LINKEDIN -> resume.personalInfo.linkedIn
                                UserInfoTag.WEBSITE -> resume.personalInfo.portfolio
                                UserInfoTag.AVATAR -> item.value // Avatar doesn't apply to contact
                                UserInfoTag.WORK_EXPERIENCE -> item.value // Work experience doesn't apply to contact
                                UserInfoTag.EDUCATION -> item.value // Education doesn't apply to contact
                                UserInfoTag.NONE -> item.value
                            }
                            item.copy(value = value)
                        }
                        element.copy(items = updatedItems)
                    }
                    is ResumeElement.WorkExperienceElement -> {
                        if (tag == UserInfoTag.WORK_EXPERIENCE && resume.workExperiences.isNotEmpty()) {
                            // Convert form WorkExperience to grid WorkExperienceItem
                            val workExperienceItems = resume.workExperiences.map { work ->
                                WorkExperienceItem(
                                    jobTitle = work.jobTitle,
                                    company = work.company,
                                    location = work.location,
                                    startDate = work.startDate?.format(
                                        java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                    ) ?: "",
                                    endDate = if (work.isCurrentRole) {
                                        "Present"
                                    } else {
                                        work.endDate?.format(
                                            java.time.format.DateTimeFormatter.ofPattern("MMM yyyy")
                                        ) ?: ""
                                    },
                                    isCurrentRole = work.isCurrentRole,
                                    responsibilities = work.responsibilities.map { resp ->
                                        ResponsibilityItem(text = resp)
                                    }
                                )
                            }
                            element.copy(items = workExperienceItems)
                        } else {
                            element
                        }
                    }
                    else -> element
                }
            }
            page.copy(elements = updatedElements)
        }

        _gridResume.value = _gridResume.value.copy(pages = updatedPages)

        if (saveToUndo) {
            triggerAutoSave()
        }
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
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.CONTACT -> {
                ResumeElement.ContactElement(
                    position = position,
                    items = listOf(
                        ContactItem(
                            type = ContactType.PHONE,
                            value = "+1 (555) 123-4567",
                            label = "Phone:",
                            iconName = "phone",
                            userInfoTag = UserInfoTag.PHONE
                        ),
                        ContactItem(
                            type = ContactType.EMAIL,
                            value = "email@example.com",
                            label = "Email:",
                            iconName = "email",
                            userInfoTag = UserInfoTag.EMAIL
                        ),
                        ContactItem(
                            type = ContactType.ADDRESS,
                            value = "City, State",
                            label = "Location:",
                            iconName = "location_on",
                            userInfoTag = UserInfoTag.LOCATION
                        )
                    ),
                    iconStyle = ContactIconStyle.ICON,
                    spacing = 8f,
                    orientation = ContactOrientation.VERTICAL,
                    textStyle = TextStyle(
                        fontSize = 12f,
                        color = 0xFF000000
                    ),
                    iconSize = 16f
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.WORK_EXPERIENCE -> {
                ResumeElement.WorkExperienceElement(
                    position = position,
                    items = listOf(
                        WorkExperienceItem(
                            jobTitle = "Job Title",
                            company = "Company Name",
                            location = "Location",
                            startDate = "2020-01-01",
                            endDate = "2022-12-31",
                            isCurrentRole = false,
                            responsibilities = listOf(
                                ResponsibilityItem(text = "Responsibility 1"),
                                ResponsibilityItem(text = "Responsibility 2")
                            )
                        )
                    )
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.EDUCATION -> {
                ResumeElement.EducationElement(
                    position = position,
                    items = listOf(
                        EducationItem(
                            degree = "Degree Name",
                            institution = "University Name",
                            location = "Location",
                            startDate = "2016-09-01",
                            endDate = "2020-05-31",
                            gpa = "3.8",
                            achievements = listOf(
                                AchievementItem(text = "Achievement 1"),
                                AchievementItem(text = "Achievement 2")
                            )
                        )
                    )
                )
            }
            com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ElementType.SKILL -> {
                ResumeElement.SkillElement(
                    position = position,
                    items = listOf(
                        SkillItem(name = "Kotlin", proficiency = 0.9f, proficiencyLabel = "Expert"),
                        SkillItem(name = "Android Development", proficiency = 0.85f, proficiencyLabel = "Advanced"),
                        SkillItem(name = "Jetpack Compose", proficiency = 0.8f, proficiencyLabel = "Advanced"),
                        SkillItem(name = "Java", proficiency = 0.75f, proficiencyLabel = "Proficient"),
                        SkillItem(name = "Git", proficiency = 0.7f, proficiencyLabel = "Proficient")
                    ),
                    displayStyle = SkillDisplayStyle.LIST
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
            ElementType.CONTACT -> Pair(12, 20) // Vertical list of contact items
            ElementType.WORK_EXPERIENCE -> Pair(20, 48) // Full width with multiple work items
            ElementType.EDUCATION -> Pair(20, 48) // Full width with multiple education items
            ElementType.SKILL -> Pair(16, 48) // Full width with skills list
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
            is ResumeElement.ContactElement -> element.copy(position = position)
            is ResumeElement.WorkExperienceElement -> element.copy(position = position)
            is ResumeElement.EducationElement -> element.copy(position = position)
            is ResumeElement.SkillElement -> element.copy(position = position)
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
