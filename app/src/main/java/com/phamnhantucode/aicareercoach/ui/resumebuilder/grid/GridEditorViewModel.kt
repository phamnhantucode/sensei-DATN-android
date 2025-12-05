package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.phamnhantucode.aicareercoach.data.cloudinary.ThumbnailUploadManager
import com.phamnhantucode.aicareercoach.data.resume.GridResumeRepository
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.converters.toFormResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.converters.toGridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.AndroidPdfGenerator
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.AndroidImageExporter
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.ImageExportState
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.export.PdfExportState
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.pagination.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

/**
 * ViewModel for the grid-based resume editor
 * @param context Application context
 * @param designId Optional design ID to load existing design
 * @param templateName Optional template name to apply template
 * @param isNewDesign Flag to indicate if this is a new blank design
 */
class GridEditorViewModel(
    private val context: Context,
    private val designId: String? = null,
    private val templateName: String? = null,
    private val isNewDesign: Boolean = false,
    private val initialLinkedResumeId: String? = null
) : ViewModel() {

    private val gridResumeRepository = GridResumeRepository.getInstance(context)
    private val resumeRepository = ResumeRepository.getInstance(context)
    private val pdfExporter = AndroidPdfGenerator(context)
    private val imageExporter = AndroidImageExporter(context)
    private val thumbnailGenerator = ThumbnailGenerator(context)
    private val thumbnailUploadManager = ThumbnailUploadManager.getInstance(context)
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

    // Hover timer for auto-add to container
    private var hoverTimerJob: Job? = null

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // PDF Export state
    private val _pdfExportState = MutableStateFlow<PdfExportState>(PdfExportState.Idle)
    val pdfExportState: StateFlow<PdfExportState> = _pdfExportState.asStateFlow()

    // Image Export state
    private val _imageExportState = MutableStateFlow<ImageExportState>(ImageExportState.Idle)
    val imageExportState: StateFlow<ImageExportState> = _imageExportState.asStateFlow()

    // Zoom state - will be calculated dynamically based on screen size
    private val _zoomLevel = MutableStateFlow(1f)
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    // Move mode state - when enabled, user can pan and zoom without interacting with elements
    private val _isMoveMode = MutableStateFlow(false)
    val isMoveMode: StateFlow<Boolean> = _isMoveMode.asStateFlow()

    // ============================================================================
    // Multi-Page Support
    // ============================================================================
    
    // Current page index for editing
    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    // Auto-pagination enabled state
    private val _autoPaginationEnabled = MutableStateFlow(true)
    val autoPaginationEnabled: StateFlow<Boolean> = _autoPaginationEnabled.asStateFlow()

    // Page thumbnails for UI display
    private val _pageThumbnails = MutableStateFlow<Map<String, String>>(emptyMap())
    val pageThumbnails: StateFlow<Map<String, String>> = _pageThumbnails.asStateFlow()

    // Thumbnail generation job tracker
    private var thumbnailGenerationJob: Job? = null
    
    // Overflow detection state for visual indicators
    private val _overflowInfo = MutableStateFlow<OverflowInfo?>(null)
    val overflowInfo: StateFlow<OverflowInfo?> = _overflowInfo.asStateFlow()
    
    // Pagination engine (lazy initialized)
    private val heightCalculator by lazy { 
        ElementHeightCalculator(context, _gridResume.value.gridConfig) 
    }
    private val paginationEngine by lazy { 
        PaginationEngine(_gridResume.value.gridConfig, heightCalculator) 
    }

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
            // Cancel thumbnail generation job
            thumbnailGenerationJob?.cancel()

            // Perform final save synchronously
            val gridResumeJson = gson.toJson(_gridResume.value)
            sharedPreferences.edit()
                .putString("latest_grid_resume", gridResumeJson)
                .commit() // Using commit() instead of apply() to ensure synchronous save before destruction
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ============================================================================
    // Loading & Saving
    // ============================================================================

    /**
     * Loads the latest resume or creates a new one with default template
     * If isNewDesign is true, creates a blank design without loading from SharedPreferences
     * If designId is provided, loads that specific design from GridResumeRepository
     * If templateName is provided, applies that template
     */
    private fun loadOrCreateResume() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                when {
                    // Case 0: Create new blank design (takes priority over all other cases)
                    isNewDesign -> {
                        _gridResume.value = createDefaultResume()
                        android.util.Log.d("GridEditorViewModel", "Created new blank design")
                    }

                    // Case 1: Load specific design by ID
                    designId != null -> {
                        val result = gridResumeRepository.getDesign(designId)
                        val gridResume = result.getOrNull()
                        if (gridResume != null) {
                            val freeModeResume = migrateToFreeLayoutMode(gridResume)
                            _gridResume.value = migrateToHighRes(freeModeResume)
                            android.util.Log.d("GridEditorViewModel", "Loaded design: $designId")
                        } else {
                            // Design not found, create blank
                            android.util.Log.w("GridEditorViewModel", "Design $designId not found, creating blank")
                            _gridResume.value = createDefaultResume()
                        }
                    }

                    // Case 2: Apply template (create new design from template asset path)
                    templateName != null -> {
                        // Check if templateName is an asset path (contains '/')
                        if (templateName.contains("/")) {
                            // Load template from assets
                            val templateLoader = TemplateLoader.getInstance(context)
                            val category = templateName.substringAfter("template/").substringBefore("/")
                            val template = templateLoader.loadTemplateFromAsset(templateName, category)
                            
                            if (template?.gridResume != null) {
                                // Create a new resume from the template
                                val newResume = templateLoader.getTemplateGridResume(template)
                                if (newResume != null) {
                                    val freeModeResume = migrateToFreeLayoutMode(newResume)
                                    // If we have a linked resume ID, use it to prevent creating a duplicate
                                    val resumeWithId = if (initialLinkedResumeId != null) {
                                        freeModeResume.copy(id = initialLinkedResumeId)
                                    } else {
                                        freeModeResume
                                    }
                                    _gridResume.value = migrateToHighRes(resumeWithId)
                                    android.util.Log.d("GridEditorViewModel", "Loaded template from assets: $templateName")
                                } else {
                                    _gridResume.value = createDefaultResume()
                                }
                            } else {
                                android.util.Log.w("GridEditorViewModel", "Failed to load template from assets: $templateName")
                                _gridResume.value = createDefaultResume()
                            }
                        } else {
                            // Fallback: treat as GridTemplateType enum name
                            val templateType = try {
                                GridTemplateType.valueOf(templateName.uppercase())
                            } catch (e: Exception) {
                                GridTemplateType.PROFESSIONAL
                            }

                            // Create resume with template
                            val templateResume = createResumeWithTemplate(templateType)
                            // If we have a linked resume ID, use it to prevent creating a duplicate
                            val resumeWithId = if (initialLinkedResumeId != null) {
                                templateResume.copy(id = initialLinkedResumeId)
                            } else {
                                templateResume
                            }
                            _gridResume.value = migrateToHighRes(resumeWithId)
                        }

                        // Note: Template loading complete, user data syncing handled by syncUserDataOnLoad()
                        android.util.Log.d("GridEditorViewModel", "Applied template: $templateName")
                    }

                    // Case 3: Default - load latest from SharedPreferences or GridResumeRepository
                    else -> {
                        // Try to load the GridResume from SharedPreferences
                        val savedGridResumeJson = sharedPreferences.getString("latest_grid_resume", null)

                        if (savedGridResumeJson != null) {
                            // Load from SharedPreferences
                            try {
                                val savedGridResume = gson.fromJson(savedGridResumeJson, GridResume::class.java)
                                // Migrate to FREE layout mode for all pages
                                val freeModeResume = migrateToFreeLayoutMode(savedGridResume)
                                // Migrate to high-res grid if needed
                                _gridResume.value = migrateToHighRes(freeModeResume)
                                android.util.Log.d("GridEditorViewModel", "Loaded from SharedPreferences")
                            } catch (e: Exception) {
                                // If parsing fails, try loading from GridResumeRepository
                                android.util.Log.e("GridEditorViewModel", "Failed to deserialize GridResume from SharedPreferences", e)

                                val result = gridResumeRepository.getLatestDesign()
                                val gridResume = result.getOrNull()

                                if (gridResume != null) {
                                    android.util.Log.d("GridEditorViewModel", "Recovered from GridResumeRepository")
                                    val freeModeResume = migrateToFreeLayoutMode(gridResume)
                                    _gridResume.value = migrateToHighRes(freeModeResume)
                                } else {
                                    android.util.Log.w("GridEditorViewModel", "No resume found, creating default")
                                    _gridResume.value = createDefaultResume()
                                }
                            }
                        } else {
                            // No saved GridResume, try to load latest from GridResumeRepository
                            val result = gridResumeRepository.getLatestDesign()
                            val gridResume = result.getOrNull()

                            if (gridResume != null) {
                                val freeModeResume = migrateToFreeLayoutMode(gridResume)
                                _gridResume.value = migrateToHighRes(freeModeResume)
                                android.util.Log.d("GridEditorViewModel", "Loaded latest design from repository")
                            } else {
                                // Create new resume with default template
                                _gridResume.value = createDefaultResume()
                                android.util.Log.d("GridEditorViewModel", "No saved designs, created default")
                            }
                        }
                    }
                }

                // Automatically sync user data from Resume Builder for tagged elements
                // This ensures elements with tags always have the latest user information
                syncUserDataOnLoad()

                // Generate thumbnails for all pages
                generateAllPageThumbnails()
            } catch (e: Exception) {
                e.printStackTrace()
                _gridResume.value = createDefaultResume()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Migrates a resume to the new high-resolution grid (96x136)
     * Scales all positions and sizes by 2x
     */
    private fun migrateToHighRes(resume: GridResume): GridResume {
        // Check if migration is needed (old column count was 48)
        if (resume.gridConfig.columns >= 96) return resume

        android.util.Log.d("GridEditorViewModel", "Migrating resume to high-res grid (96x136)")

        // Create new config with high resolution
        val newConfig = GridConfig(
            columns = 96,
            rows = 136,
            cellSizeDp = GridConfig.CELL_SIZE_FOR_A4
        )

        // Migrate all pages
        val newPages = resume.pages.map { page ->
            val newElements = page.elements.map { element ->
                // Scale position and size by 2
                val newPosition = element.position.copy(
                    row = element.position.row * 2,
                    col = element.position.col * 2,
                    rowSpan = element.position.rowSpan * 2,
                    colSpan = element.position.colSpan * 2
                )
                
                // Update element with new position
                // Use the specific copy method for each type to preserve all properties
                when (element) {
                    is ResumeElement.TextElement -> element.copy(position = newPosition)
                    is ResumeElement.ImageElement -> element.copy(position = newPosition)
                    is ResumeElement.ShapeElement -> element.copy(position = newPosition)
                    is ResumeElement.ChartElement -> element.copy(position = newPosition)
                    is ResumeElement.ContainerElement -> element.copy(position = newPosition)
                    is ResumeElement.IconElement -> element.copy(position = newPosition)
                    is ResumeElement.ContactElement -> element.copy(position = newPosition)
                    is ResumeElement.WorkExperienceElement -> element.copy(position = newPosition)
                    is ResumeElement.EducationElement -> element.copy(position = newPosition)
                    is ResumeElement.SkillElement -> element.copy(position = newPosition)
                    is ResumeElement.ProjectElement -> element.copy(position = newPosition)
                    is ResumeElement.CertificationElement -> element.copy(position = newPosition)
                    is ResumeElement.LanguageElement -> element.copy(position = newPosition)
                }
            }
            page.copy(elements = newElements)
        }

        return resume.copy(
            gridConfig = newConfig,
            pages = newPages
        )
    }

    /**
     * Loads a specific resume by converting from form format (backward compatibility)
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
     * Saves the current resume directly as GridResume to local and remote storage
     * Also updates the linked Resume table's json field with GridResume data
     */
    fun save() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                // Generate thumbnail for the design
                val base64Thumbnail = try {
                    thumbnailGenerator.generateThumbnail(_gridResume.value)
                } catch (e: Exception) {
                    android.util.Log.e("GridEditorViewModel", "Failed to generate thumbnail", e)
                    ""
                }

                // Upload thumbnail to Cloudinary for faster loading in resume list
                val thumbnail = if (base64Thumbnail.isNotEmpty()) {
                    try {
                        val uploadResult = thumbnailUploadManager.uploadThumbnail(
                            base64Thumbnail = base64Thumbnail,
                            resumeId = _gridResume.value.id
                        )
                        uploadResult.getOrElse { base64Thumbnail }
                    } catch (e: Exception) {
                        android.util.Log.e("GridEditorViewModel", "Failed to upload thumbnail", e)
                        base64Thumbnail // Fallback to Base64 if upload fails
                    }
                } else {
                    ""
                }

                // Save GridResume to SharedPreferences for quick restore
                val gridResumeJson = gson.toJson(_gridResume.value)
                sharedPreferences.edit()
                    .putString("latest_grid_resume", gridResumeJson)
                    .commit()

                // Check if design already exists
                val existing = gridResumeRepository.getDesign(_gridResume.value.id).getOrNull()
                
                android.util.Log.d("GridEditorViewModel", "Saving design: id=${_gridResume.value.id}, thumbnail=${thumbnail.take(50)}..., existing=${existing != null}")

                // Direct GridResume sync (no form conversion)
                val result = if (existing != null) {
                    android.util.Log.d("GridEditorViewModel", "Updating existing design")
                    gridResumeRepository.updateDesign(
                        gridResume = _gridResume.value,
                        thumbnail = thumbnail,
                        syncToRemote = true
                    )
                } else {
                    android.util.Log.d("GridEditorViewModel", "Creating new design")
                    gridResumeRepository.saveDesign(
                        gridResume = _gridResume.value,
                        thumbnail = thumbnail,
                        syncToRemote = true
                    )
                }

                // Also update the linked Resume table's json field with GridResume data
                if (result.isSuccess && initialLinkedResumeId != null) {
                    try {
                        // Get the linked resume
                        val linkedResumeResult = resumeRepository.getResume(initialLinkedResumeId)
                        val linkedResume = linkedResumeResult.getOrNull()
                        if (linkedResume != null) {
                            // Update the Resume with GridResume JSON in the 'json' field
                            // Include the thumbnail that was uploaded
                            val gridResumeWithThumbnail = _gridResume.value.copy(thumbnail = thumbnail)
                            @Suppress("DEPRECATION")
                            resumeRepository.updateResume(
                                resume = linkedResume,
                                syncToRemote = true,
                                gridResume = gridResumeWithThumbnail
                            )
                            android.util.Log.d("GridEditorViewModel", "Updated linked Resume ${initialLinkedResumeId} with GridResume JSON (thumbnail: ${thumbnail.take(50)}...)")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("GridEditorViewModel", "Failed to update linked Resume", e)
                        // Don't fail the whole save if this fails
                    }
                }

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
     * Saves immediately without debouncing (for critical operations like navigation away)
     * This is a fire-and-forget operation that doesn't emit events
     */
    fun saveImmediately() {
        try {
            // Cancel pending auto-save to avoid duplicate saves
            autoSaveJob?.cancel()

            // Save GridResume to SharedPreferences synchronously
            val gridResumeJson = gson.toJson(_gridResume.value)
            sharedPreferences.edit()
                .putString("latest_grid_resume", gridResumeJson)
                .commit() // Synchronous save ensures completion

            // Note: We don't save to repository here as it requires coroutine
            // The SharedPreferences save is the critical one for data preservation
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Triggers auto-save after a delay
     */
    private fun triggerAutoSave() {
        if (!isAutoSaveEnabled) return

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(500) // 500ms debounce - faster saves, minimal data loss risk
            save()
        }
    }

    // ============================================================================
    // Element Management
    // ============================================================================

    /**
     * Adds a new element to the canvas
     */
    fun addElement(elementType: ElementType) {
        saveToUndoStack()

        val currentPage = getCurrentPage()

        // Find next available position
        val elementSize = getDefaultElementSize(elementType)
        val position = GridUtils.findNextAvailablePosition(
            elementSize = elementSize,
            elements = currentPage.elements,
            gridConfig = _gridResume.value.gridConfig
        ) ?: GridPosition(0, 0, elementSize.first, elementSize.second)

        // Calculate next z-index to ensure new element appears on top
        val maxZIndex = currentPage.elements.maxOfOrNull { it.zIndex } ?: -1
        val newZIndex = maxZIndex + 1

        // Create new element based on type
        val newElement = createElementOfType(elementType, position)
            .update(zIndex = newZIndex)

        // Add to page
        val updatedPage = currentPage.addElement(newElement)
        updatePage(updatedPage)

        // Select the new element
        _selectedElement.value = newElement

        // Regenerate thumbnail for current page
        regenerateCurrentPageThumbnail()

        triggerAutoSave()

        // Check for overflow after adding element
        if (_autoPaginationEnabled.value) {
            detectOverflow()
        }
    }

    /**
     * Updates an existing element
     */
    fun updateElement(updatedElement: ResumeElement) {
        saveToUndoStack()

        val currentPage = getCurrentPage()

        val updatedPage = currentPage.updateElement(updatedElement.id) { updatedElement }
        updatePage(updatedPage)

        // Update selection if this is the selected element
        if (_selectedElement.value?.id == updatedElement.id) {
            _selectedElement.value = updatedElement
        }

        // Regenerate thumbnail for current page
        regenerateCurrentPageThumbnail()

        triggerAutoSave()

        // Check for overflow after updating element
        if (_autoPaginationEnabled.value) {
            detectOverflow()
        }
    }

    /**
     * Updates a container's layout mode and adjusts child elements accordingly
     * When switching to VERTICAL mode, children are repositioned to stack vertically
     */
    fun updateContainerLayoutMode(container: ResumeElement.ContainerElement, newMode: LayoutMode) {
        saveToUndoStack()

        val currentPage = getCurrentPage()
        
        // Update the container's layout mode
        val updatedContainer = container.copy(layoutMode = newMode)
        
        // If switching to VERTICAL mode, reposition all children
        val updatedElements = if (newMode == LayoutMode.VERTICAL) {
            currentPage.elements.map { element ->
                if (element.id == container.id) {
                    // Return the updated container
                    updatedContainer
                } else if (container.children.contains(element.id)) {
                    // Reposition child: stack vertically, full width, wrap height
                    val childIndex = container.children.indexOf(element.id)
                    element.update(
                        position = element.position.copy(
                            row = childIndex, // Stack vertically by index
                            col = 0, // Start at column 0
                            colSpan = container.position.colSpan, // Full width of container
                            widthMode = SizeMode.FIXED, // Use fixed width mode
                            heightMode = SizeMode.WRAP_CONTENT, // Wrap height to content
                            cachedHeightDp = null // Clear cached height to recalculate
                        )
                    )
                } else {
                    element
                }
            }
        } else {
            // For FREE or GRID modes, just update the container
            currentPage.elements.map { element ->
                if (element.id == container.id) updatedContainer else element
            }
        }
        
        val updatedPage = currentPage.copy(elements = updatedElements)
        updatePage(updatedPage)
        
        // Update selection if this is the selected element
        if (_selectedElement.value?.id == container.id) {
            _selectedElement.value = updatedContainer
        }
        
        triggerAutoSave()
    }

    /**
     * Reorders a child element within its container
     * Used for drag-and-drop reordering in vertical layout containers
     */
    fun reorderChildInContainer(containerId: String, childId: String, newIndex: Int) {
        saveToUndoStack()

        val currentPage = getCurrentPage()
        val container = currentPage.elements.find { it.id == containerId } as? ResumeElement.ContainerElement ?: return

        // Check if child exists in container
        if (!container.children.contains(childId)) return

        // Remove child from old position and insert at new position
        val oldIndex = container.children.indexOf(childId)
        if (oldIndex == newIndex) return // No change needed

        val mutableChildren = container.children.toMutableList()
        mutableChildren.removeAt(oldIndex)
        val clampedIndex = newIndex.coerceIn(0, mutableChildren.size)
        mutableChildren.add(clampedIndex, childId)

        // Update container with new children order
        val updatedContainer = container.copy(children = mutableChildren)

        // If vertical layout, reposition all children based on new order
        val updatedElements = if (container.effectiveLayoutMode == LayoutMode.VERTICAL) {
            currentPage.elements.map { element ->
                if (element.id == containerId) {
                    updatedContainer
                } else if (mutableChildren.contains(element.id)) {
                    // Reposition child based on new index
                    val childIndex = mutableChildren.indexOf(element.id)
                    element.update(
                        position = element.position.copy(row = childIndex)
                    )
                } else {
                    element
                }
            }
        } else {
            // For non-vertical layouts, just update the container
            currentPage.elements.map { element ->
                if (element.id == containerId) updatedContainer else element
            }
        }

        val updatedPage = currentPage.copy(elements = updatedElements)
        updatePage(updatedPage)

        triggerAutoSave()
    }

    /**
     * Removes an element
     */
    fun removeElement(elementId: String) {
        saveToUndoStack()

        val currentPage = getCurrentPage()

        val updatedPage = currentPage.removeElement(elementId)
        updatePage(updatedPage)

        // Deselect if this was the selected element
        if (_selectedElement.value?.id == elementId) {
            _selectedElement.value = null
        }

        // Regenerate thumbnail for current page
        regenerateCurrentPageThumbnail()

        triggerAutoSave()
    }

    /**
     * Selects an element
     */
    fun selectElement(element: ResumeElement) {
        // Always fetch the latest version of the element from the page
        // to ensure we have the most up-to-date properties
        val currentPage = getCurrentPage()
        val latestElement = currentPage?.elements?.find { it.id == element.id } ?: element
        _selectedElement.value = latestElement
    }

    /**
     * Deselects the current element
     */
    fun deselectElement() {
        _selectedElement.value = null
    }

    /**
     * Toggles element visibility
     */
    fun toggleElementVisibility(elementId: String) {
        saveToUndoStack()

        val currentPage = getCurrentPage()
        val element = currentPage.elements.find { it.id == elementId } ?: return

        // Use generic update extension
        val updatedElement = element.update(isVisible = !element.isVisible)

        val updatedPage = currentPage.updateElement(elementId) { updatedElement }
        updatePage(updatedPage)
        
        // Update selection if needed
        if (_selectedElement.value?.id == elementId) {
            _selectedElement.value = updatedElement
        }

        triggerAutoSave()
    }

    /**
     * Toggles element lock state
     */
    fun toggleElementLock(elementId: String) {
        saveToUndoStack()

        val currentPage = getCurrentPage()
        val element = currentPage.elements.find { it.id == elementId } ?: return

        // Use generic update extension
        val updatedElement = element.update(locked = !element.locked)

        val updatedPage = currentPage.updateElement(elementId) { updatedElement }
        updatePage(updatedPage)

        // Update selection if needed
        if (_selectedElement.value?.id == elementId) {
            _selectedElement.value = updatedElement
        }

        triggerAutoSave()
    }

    /**
     * Moves an element to a new layer (Z-index)
     * @param fromIndex The current index in the list (sorted by Z-index DESCENDING)
     * @param toIndex The new index in the list
     */
    fun moveElementLayer(fromIndex: Int, toIndex: Int) {
        saveToUndoStack()

        val currentPage = getCurrentPage()
        
        // Get elements sorted by Z-index DESCENDING (Front to Back)
        // This matches the UI display order
        val sortedElements = currentPage.elements.sortedByDescending { it.zIndex }.toMutableList()
        
        if (fromIndex !in sortedElements.indices || toIndex !in sortedElements.indices) return
        
        // Move the element in the list
        val element = sortedElements.removeAt(fromIndex)
        sortedElements.add(toIndex, element)
        
        // Re-assign Z-indices based on the new list order
        // The first item in the list (index 0) should have the highest Z-index
        // The last item should have the lowest
        val totalElements = sortedElements.size
        val updatedElements = sortedElements.mapIndexed { index, el ->
            val newZIndex = totalElements - 1 - index
            
            if (el.zIndex == newZIndex) el else {
                // Use generic update extension
                el.update(zIndex = newZIndex)
            }
        }
        
        // Update the page with the new elements list
        val updatedPage = currentPage.copy(elements = updatedElements)
        updatePage(updatedPage)
        
        triggerAutoSave()
    }

    /**
     * Moves an element into a container
     */
    fun moveElementToContainer(elementId: String, containerId: String) {
        saveToUndoStack()

        val currentPage = getCurrentPage()
        val element = currentPage.elements.find { it.id == elementId } ?: return
        val container = currentPage.elements.find { it.id == containerId } as? ResumeElement.ContainerElement ?: return

        // Prevent circular dependency
        if (elementId == containerId) return
        if (element is ResumeElement.ContainerElement && isAncestor(element, containerId, currentPage.elements)) return

        // Remove from old parent if any
        val updatedElements = currentPage.elements.map { el ->
            if (el is ResumeElement.ContainerElement && el.children.contains(elementId)) {
                el.copy(children = el.children - elementId)
            } else {
                el
            }
        }.toMutableList()

        // Update container with new child
        val updatedContainerIndex = updatedElements.indexOfFirst { it.id == containerId }
        if (updatedContainerIndex != -1) {
            val updatedContainer = (updatedElements[updatedContainerIndex] as ResumeElement.ContainerElement).let {
                it.copy(children = it.children + elementId)
            }
            updatedElements[updatedContainerIndex] = updatedContainer
        }

        // Update element position to be relative to container
        // We need to calculate the relative position
        // For now, let's just reset to 0,0 or keep absolute if we can't calculate
        // Ideally: relativeRow = elementRow - containerRow
        val newRow = (element.position.row - container.position.row).coerceAtLeast(0)
        val newCol = (element.position.col - container.position.col).coerceAtLeast(0)
        
        val updatedElement = element.update(
            position = element.position.copy(row = newRow, col = newCol)
        )
        
        val elementIndex = updatedElements.indexOfFirst { it.id == elementId }
        if (elementIndex != -1) {
            updatedElements[elementIndex] = updatedElement
        }

        val updatedPage = currentPage.copy(elements = updatedElements)
        updatePage(updatedPage)
        
        triggerAutoSave()
    }

    /**
     * Moves an element out of a container (to top level)
     */
    fun moveElementOut(elementId: String) {
        saveToUndoStack()

        val currentPage = getCurrentPage()
        val element = currentPage.elements.find { it.id == elementId } ?: return

        // Find parent first to calculate absolute position
        val parent = currentPage.elements.find { 
            it is ResumeElement.ContainerElement && it.children.contains(elementId) 
        } as? ResumeElement.ContainerElement

        // Remove from parent(s)
        val updatedElements = currentPage.elements.map { el ->
            if (el is ResumeElement.ContainerElement && el.children.contains(elementId)) {
                el.copy(children = el.children - elementId)
            } else {
                el
            }
        }.toMutableList()

        if (parent != null) {
            val absoluteRow = parent.position.row + element.position.row
            val absoluteCol = parent.position.col + element.position.col
            
            val updatedElement = element.update(
                position = element.position.copy(row = absoluteRow, col = absoluteCol)
            )
            
            val elementIndex = updatedElements.indexOfFirst { it.id == elementId }
            if (elementIndex != -1) {
                updatedElements[elementIndex] = updatedElement
            }
        }

        val updatedPage = currentPage.copy(elements = updatedElements)
        updatePage(updatedPage)
        
        triggerAutoSave()
    }

    private fun isAncestor(potentialAncestor: ResumeElement.ContainerElement, targetId: String, allElements: List<ResumeElement>): Boolean {
        if (potentialAncestor.children.contains(targetId)) return true
        
        for (childId in potentialAncestor.children) {
            val child = allElements.find { it.id == childId }
            if (child is ResumeElement.ContainerElement) {
                if (isAncestor(child, targetId, allElements)) return true
            }
        }
        return false
    }

    // ============================================================================
    // Drag & Drop
    // ============================================================================

    /**
     * Starts dragging an element
     */
    fun startDrag(element: ResumeElement, originalY: Float = 0f) {
        // Get the latest version of the element from the page to ensure we have all recent changes
        val currentPage = getCurrentPage()
        val latestElement = currentPage?.elements?.find { it.id == element.id } ?: element

        _draggedElement.value = DragState(
            element = latestElement,
            originalPosition = latestElement.position,
            currentPosition = latestElement.position,
            isValidPosition = true,
            originalCaptureY = originalY
        )
    }

    /**
     * Updates drag position during drag
     */
    fun updateDragPosition(newPosition: GridPosition, offsetYPx: Float? = null, density: Float = 1f) {
        val currentDrag = _draggedElement.value ?: return
        val currentPage = getCurrentPage()

        // Check if element is a child of a vertical layout container
        val parentContainer = currentPage.elements.find { element ->
            element is ResumeElement.ContainerElement &&
            element.children.contains(currentDrag.element.id)
        } as? ResumeElement.ContainerElement

        // Calculate target insertion index for vertical containers
        val targetInsertionIndex = if (parentContainer != null &&
                                       parentContainer.effectiveLayoutMode == LayoutMode.VERTICAL &&
                                       offsetYPx != null) {
            // Use cumulative height calculation for accurate targeting
            calculateVerticalTargetIndex(
                parentContainer = parentContainer,
                draggedElementId = currentDrag.element.id,
                offsetYPx = offsetYPx,
                density = density,
                zoom = _zoomLevel.value,
                allElements = currentPage.elements
            )
        } else {
            null
        }

        // Check if position is valid
        // Use page's layout mode to determine collision behavior
        val isValid = GridUtils.isValidPosition(newPosition, _gridResume.value.gridConfig) &&
                      !GridUtils.hasCollision(
                          position = newPosition,
                          elements = currentPage.elements,
                          excludeId = currentDrag.element.id,
                          layoutMode = currentPage.layoutMode  // Pass page layout mode
                      )

        _draggedElement.value = currentDrag.copy(
            currentPosition = newPosition,
            isValidPosition = isValid,
            targetInsertionIndex = targetInsertionIndex,
            dragOffsetY = offsetYPx ?: 0f
        )
    }

    /**
     * Calculates the target insertion index for vertical container reordering
     * based on the actual cumulative heights of children and the Y offset.
     *
     * Uses the "half-height" rule: when the dragged element crosses the midpoint
     * of a sibling element, the target index changes to create space at that position.
     */
    private fun calculateVerticalTargetIndex(
        parentContainer: ResumeElement.ContainerElement,
        draggedElementId: String,
        offsetYPx: Float,
        density: Float,
        zoom: Float,
        allElements: List<ResumeElement>
    ): Int {
        val currentIndex = parentContainer.children.indexOf(draggedElementId)
        if (currentIndex == -1) return 0

        val cellSizePx = _gridResume.value.gridConfig.cellSizeDp * density * zoom

        // Build a list of child heights (excluding the dragged element)
        val childHeights = mutableListOf<Pair<Int, Float>>() // Pair of (originalIndex, height)

        for ((index, childId) in parentContainer.children.withIndex()) {
            if (childId == draggedElementId) continue

            val childElement = allElements.find { it.id == childId } ?: continue

            // Calculate child height using same logic as ghost positioning
            val childHeightPx = if (childElement.position.heightMode == SizeMode.WRAP_CONTENT) {
                val cachedHeight = childElement.position.cachedHeightDp
                cachedHeight?.let { it * density * zoom } ?: (childElement.position.rowSpan * cellSizePx)
            } else {
                childElement.position.rowSpan * cellSizePx
            }

            childHeights.add(index to childHeightPx)
        }

        // Determine target index based on offsetYPx
        var cumulativeHeight = 0f
        var targetIndex = currentIndex // Initialize to current index, not 0

        if (offsetYPx > 0) {
            // Moving down
            for ((originalIndex, height) in childHeights) {
                // Only consider children after the current position
                if (originalIndex <= currentIndex) {
                    continue
                }

                // Check if we've crossed the midpoint of this child
                // cumulativeHeight tracks the height of intervening siblings we've already passed
                if (offsetYPx >= cumulativeHeight + (height / 2f)) {
                    targetIndex = originalIndex
                    cumulativeHeight += height
                } else {
                    break
                }
            }
        } else if (offsetYPx < 0) {
            // Moving up - work backwards from current position
            val childrenBeforeCurrent = childHeights.filter { it.first < currentIndex }.reversed()

            var negativeOffset = 0f
            targetIndex = currentIndex

            for ((originalIndex, height) in childrenBeforeCurrent) {
                // Check if we've crossed the midpoint of this child (going upward)
                // We need to move up past half of this child's height
                if (offsetYPx <= negativeOffset - (height / 2f)) {
                    targetIndex = originalIndex
                    negativeOffset -= height
                } else {
                    break
                }
            }
        } else {
            // No movement
            targetIndex = currentIndex
        }

        return targetIndex.coerceIn(0, parentContainer.children.size)
    }

    /**
     * Ends drag operation
     */
    fun endDrag(finalPosition: GridPosition) {
        val dragState = _draggedElement.value ?: return

        // Check if hovering over a container - if so, add to container immediately
        // Check if hovering over a container AND the hover timer has completed (progress >= 1.0)
        if (dragState.hoveredContainerId != null && dragState.hoverProgress >= 1.0f) {
            // Add element to container
            moveElementToContainer(dragState.element.id, dragState.hoveredContainerId!!)

            // Cancel hover timer and cleanup
            cancelHoverTimer()
            _draggedElement.value = null
            return
        }

        val currentPage = getCurrentPage()
        
        // Check if element is a child of a vertical layout container
        val parentContainer = currentPage.elements.find { element ->
            element is ResumeElement.ContainerElement &&
            element.children.contains(dragState.element.id)
        } as? ResumeElement.ContainerElement

        // If parent is a vertical container, handle reordering
        if (parentContainer != null && parentContainer.effectiveLayoutMode == LayoutMode.VERTICAL) {
            val currentIndex = parentContainer.children.indexOf(dragState.element.id)

            // Use the target index that was calculated during drag with accurate cumulative heights
            // This ensures the drop position matches the visual feedback shown during drag
            val targetIndex = dragState.targetInsertionIndex ?: currentIndex

            if (targetIndex != currentIndex) {
                // Reorder within container
                reorderChildInContainer(parentContainer.id, dragState.element.id, targetIndex)
            }

            // Cleanup
            cancelHoverTimer()
            _draggedElement.value = null
            return
        }

        // Normal drag end - update position
        // Clamp position to ensure it's within bounds
        val clampedPosition = GridUtils.clampPosition(finalPosition, _gridResume.value.gridConfig)

        if (clampedPosition != dragState.originalPosition) {
            saveToUndoStack()

            // IMPORTANT: Get the latest element from the page, not from dragState
            // This ensures we preserve any property changes made during the drag
            val latestElementFromPage = currentPage.elements.find { it.id == dragState.element.id }

            if (latestElementFromPage != null) {
                // Update only the position, preserving all other properties
                // Use generic update extension
                val updatedElement = latestElementFromPage.update(position = clampedPosition)

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

        // Cleanup hover state and drag state
        cancelHoverTimer()
        _draggedElement.value = null
    }

    /**
     * Called when dragging over a container - starts hover timer for auto-add
     */
    fun onDragOverContainer(containerId: String, context: android.content.Context) {
        val dragState = _draggedElement.value ?: return
        val currentPage = getCurrentPage()

        // Find the container element
        val container = currentPage.elements.find { it.id == containerId } as? ResumeElement.ContainerElement
        if (container == null) return

        // Only trigger for locked containers
        if (!container.locked) {
            cancelHoverTimer()
            return
        }

        // Don't trigger if element is already a child of this container
        if (container.children.contains(dragState.element.id)) {
            cancelHoverTimer()
            return
        }

        // If already hovering over this container, do nothing (timer continues)
        if (dragState.hoveredContainerId == containerId) {
            return
        }

        // Hovering over a new container - cancel old timer and start new one
        cancelHoverTimer()

        // Update drag state with new hovered container
        // Start with a small progress to show immediate visual feedback
        _draggedElement.value = dragState.copy(
            hoveredContainerId = containerId,
            hoverProgress = 0.1f
        )

        // Start 1.5 second timer with progress updates
        hoverTimerJob = viewModelScope.launch {
            val totalDuration = 1500L // 1.5 seconds
            val updateInterval = 16L // ~60fps
            val steps = (totalDuration / updateInterval).toInt()

            for (i in 1..steps) {
                delay(updateInterval)

                val currentDrag = _draggedElement.value
                // Check if still dragging over the same container
                if (currentDrag == null || currentDrag.hoveredContainerId != containerId) {
                    break
                }

                // Map remaining progress (0.1 to 1.0)
                val rawProgress = i.toFloat() / steps
                val progress = 0.1f + (rawProgress * 0.9f)
                
                _draggedElement.value = currentDrag.copy(hoverProgress = progress.coerceIn(0f, 1f))

                // Trigger haptic feedback when timer completes
                if (progress >= 1.0f) {
                    triggerHapticFeedback(context)
                    break
                }
            }
        }
    }

    /**
     * Cancels the hover timer (when drag exits container or drag ends)
     */
    fun cancelHoverTimer() {
        hoverTimerJob?.cancel()
        hoverTimerJob = null

        val currentDrag = _draggedElement.value ?: return
        if (currentDrag.hoveredContainerId != null || currentDrag.hoverProgress > 0f) {
            _draggedElement.value = currentDrag.copy(
                hoveredContainerId = null,
                hoverProgress = 0f
            )
        }
    }

    /**
     * Triggers device haptic feedback
     */
    private fun triggerHapticFeedback(context: android.content.Context) {
        try {
            val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as android.os.Vibrator
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(50)
            }
        } catch (e: Exception) {
            // Ignore vibration errors (permission not granted or device doesn't support)
            e.printStackTrace()
        }
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
     * Toggles page number display on exported PDF
     */
    fun togglePageNumbers() {
        _gridResume.update { resume ->
            resume.copy(
                gridConfig = resume.gridConfig.copy(
                    showPageNumbers = !resume.gridConfig.showPageNumbers
                )
            )
        }
    }

    /**
     * Sets page number position
     */
    fun setPageNumberPosition(position: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.PageNumberPosition) {
        _gridResume.update { resume ->
            resume.copy(
                gridConfig = resume.gridConfig.copy(
                    pageNumberPosition = position
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
     * Toggles move mode (pan and zoom without element interaction)
     */
    fun toggleMoveMode() {
        _isMoveMode.update { !it }
        // Deselect any selected element when entering move mode
        if (_isMoveMode.value) {
            deselectElement()
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
     * @deprecated Form-based Resume sync is deprecated. This method is no longer used.
     */
    @Deprecated("Form-based Resume sync is deprecated")
    private suspend fun syncUserDataOnLoad() {
        // No-op: Form-based Resume sync is deprecated
        // User data should be directly embedded in GridResume elements
    }

    /**
     * Apply user data from Resume to tagged elements in the template
     * This replaces tagged TextElements, ImageElements, and WorkExperienceElements with actual user data
     *
     * @param resume User's resume data from Resume Builder
     * @param saveToUndo Whether to save current state to undo stack (default: true)
     */
    fun applyUserDataToTemplate(
        resume: Resume,
        saveToUndo: Boolean = true
    ) {
        if (saveToUndo) {
            saveToUndoStack()
        }

        val currentPages = _gridResume.value.pages
        val updatedPages = currentPages.map { page ->
            val updatedElements = page.elements.map { element ->
                val tag = element.userInfoTag
                if (tag != null && tag != UserInfoTag.NONE) {
                    // Create updated element based on tag
                    createUpdatedElementFromTag(element, tag, resume)
                } else if (element is ResumeElement.ContactElement) {
                    // ContactElement items may have individual userInfoTags even if element itself doesn't
                    updateContactElementItems(element, resume)
                } else {
                    element
                }
            }
            page.copy(elements = updatedElements)
        }

        val updatedResume = _gridResume.value.copy(pages = updatedPages)
        _gridResume.value = updatedResume

        if (saveToUndo) {
            triggerAutoSave()
        }
    }

    /**
     * Helper to create updated element from user data tag
     */
    private fun createUpdatedElementFromTag(
        element: ResumeElement,
        tag: UserInfoTag,
        resume: Resume
    ): ResumeElement {
        return when (element) {
            is ResumeElement.TextElement -> {
                val newContent = when (tag) {
                    UserInfoTag.NAME -> resume.personalInfo.fullName
                    UserInfoTag.EMAIL -> resume.personalInfo.email
                    UserInfoTag.PHONE -> resume.personalInfo.phone
                    UserInfoTag.LOCATION -> resume.personalInfo.location
                    UserInfoTag.LINKEDIN -> resume.personalInfo.linkedIn
                    UserInfoTag.GITHUB -> resume.personalInfo.github
                    UserInfoTag.WEBSITE -> resume.personalInfo.portfolio
                    UserInfoTag.PROFESSIONAL_SUMMARY -> resume.professionalSummary
                    else -> element.content
                }
                if (newContent.isNotEmpty()) element.copy(content = newContent) else element
            }
            is ResumeElement.ImageElement -> {
                if (tag == UserInfoTag.AVATAR && resume.personalInfo.avatar.isNotEmpty()) {
                    element.copy(imageUrl = resume.personalInfo.avatar)
                } else {
                    element
                }
            }
            is ResumeElement.WorkExperienceElement -> {
                if (tag == UserInfoTag.WORK_EXPERIENCE && resume.workExperiences.isNotEmpty()) {
                    // Convert form work experience to grid work experience items
                    val newItems = resume.workExperiences.map { work ->
                        WorkExperienceItem(
                            jobTitle = work.jobTitle,
                            company = work.company,
                            location = work.location,
                            startDate = work.startDate?.toString() ?: "",
                            endDate = work.endDate?.toString() ?: "",
                            isCurrentRole = work.isCurrentRole,
                            responsibilities = work.responsibilities.map { ResponsibilityItem(text = it) }
                        )
                    }
                    element.copy(items = newItems)
                } else {
                    element
                }
            }
            is ResumeElement.EducationElement -> {
                if (tag == UserInfoTag.EDUCATION && resume.education.isNotEmpty()) {
                    val newItems = resume.education.map { edu ->
                        EducationItem(
                            degree = edu.degree,
                            institution = edu.institution,
                            location = edu.location,
                            startDate = edu.startDate?.toString() ?: "",
                            endDate = edu.endDate?.toString() ?: "",
                            gpa = edu.gpa,
                            achievements = edu.achievements.map { AchievementItem(text = it) }
                        )
                    }
                    element.copy(items = newItems)
                } else {
                    element
                }
            }
            is ResumeElement.SkillElement -> {
                if (tag == UserInfoTag.SKILLS && resume.skills.isNotEmpty()) {
                    val newItems = resume.skills.map { skill ->
                        SkillItem(name = skill)
                    }
                    element.copy(items = newItems)
                } else {
                    element
                }
            }
            is ResumeElement.ProjectElement -> {
                if (tag == UserInfoTag.PROJECTS && resume.projects.isNotEmpty()) {
                    val newItems = resume.projects.map { project ->
                        ProjectItem(
                            name = project.title,
                            description = project.description,
                            startDate = project.startDate?.toString() ?: "",
                            endDate = project.endDate?.toString() ?: "",
                            technologies = project.technologies.joinToString(", "),
                            link = project.link
                        )
                    }
                    element.copy(items = newItems)
                } else {
                    element
                }
            }
            is ResumeElement.CertificationElement -> {
                if (tag == UserInfoTag.CERTIFICATIONS && resume.certifications.isNotEmpty()) {
                    val newItems = resume.certifications.map { cert ->
                        CertificationItem(
                            name = cert.name,
                            issuer = cert.issuer,
                            issueDate = cert.issueDate?.toString() ?: "",
                            expiryDate = cert.expiryDate?.toString() ?: "",
                            credentialId = cert.credentialId
                        )
                    }
                    element.copy(items = newItems)
                } else {
                    element
                }
            }
            is ResumeElement.LanguageElement -> {
                if (tag == UserInfoTag.LANGUAGES && resume.languages.isNotEmpty()) {
                    val newItems = resume.languages.map { lang ->
                        LanguageItem(
                            name = lang.name,
                            proficiencyLabel = lang.proficiency.name
                        )
                    }
                    element.copy(items = newItems)
                } else {
                    element
                }
            }
            is ResumeElement.ContactElement -> {
                // Update specific contact items within the element
                val newItems = element.items.map { item ->
                    val newValue = when (item.userInfoTag) {
                        UserInfoTag.EMAIL -> resume.personalInfo.email
                        UserInfoTag.PHONE -> resume.personalInfo.phone
                        UserInfoTag.LOCATION -> resume.personalInfo.location
                        UserInfoTag.LINKEDIN -> resume.personalInfo.linkedIn
                        UserInfoTag.GITHUB -> resume.personalInfo.github
                        UserInfoTag.WEBSITE -> resume.personalInfo.portfolio
                        else -> item.value
                    }
                    if (newValue.isNotEmpty()) item.copy(value = newValue) else item
                }
                element.copy(items = newItems)
            }
            else -> element
        }
    }

    /**
     * Helper to update ContactElement items with user data
     * Called when ContactElement itself has no userInfoTag but its items may have individual tags
     */
    private fun updateContactElementItems(
        element: ResumeElement.ContactElement,
        resume: Resume
    ): ResumeElement.ContactElement {
        val newItems = element.items.map { item ->
            val newValue = when (item.userInfoTag) {
                UserInfoTag.EMAIL -> resume.personalInfo.email
                UserInfoTag.PHONE -> resume.personalInfo.phone
                UserInfoTag.LOCATION -> resume.personalInfo.location
                UserInfoTag.LINKEDIN -> resume.personalInfo.linkedIn
                UserInfoTag.GITHUB -> resume.personalInfo.github
                UserInfoTag.WEBSITE -> resume.personalInfo.portfolio
                else -> item.value
            }
            if (newValue.isNotEmpty()) item.copy(value = newValue) else item
        }
        return element.copy(items = newItems)
    }

    // ============================================================================
    // Undo / Redo
    // ============================================================================

    /**
     * Undo last action
     */
    fun undo() {
        if (undoStack.isNotEmpty()) {
            // Save current state to redo stack
            redoStack.add(_gridResume.value)
            if (redoStack.size > maxHistorySize) redoStack.removeAt(0)

            // Restore from undo stack
            val previousState = undoStack.removeAt(undoStack.lastIndex)
            _gridResume.value = previousState

            triggerAutoSave()
        }
    }

    /**
     * Redo last undone action
     */
    fun redo() {
        if (redoStack.isNotEmpty()) {
            // Save current state to undo stack
            undoStack.add(_gridResume.value)
            if (undoStack.size > maxHistorySize) undoStack.removeAt(0)

            // Restore from redo stack
            val nextState = redoStack.removeAt(redoStack.lastIndex)
            _gridResume.value = nextState

            triggerAutoSave()
        }
    }

    /**
     * Saves current state to undo stack
     */
    private fun saveToUndoStack() {
        undoStack.add(_gridResume.value)
        if (undoStack.size > maxHistorySize) undoStack.removeAt(0)
        redoStack.clear()
    }

    // ============================================================================
    // PDF Export
    // ============================================================================

    /**
     * Exports the resume to PDF
     */
    /**
     * Exports the resume to PDF
     */
    fun exportToPdf(file: java.io.File, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Collect the flow to actually execute the PDF generation
                pdfExporter.generatePdf(_gridResume.value, file).collect { state ->
                    when (state) {
                        is PdfExportState.Idle,
                        is PdfExportState.PreparingImages,
                        is PdfExportState.RenderingPage,
                        is PdfExportState.SavingFile -> {
                            // Emit progress states
                            _pdfExportState.value = state
                        }
                        is PdfExportState.Success -> {
                            // Override state with our URI (generatePdf returns File URI, we want the MediaStore URI)
                            val fileSize = try {
                                context.contentResolver.openFileDescriptor(uri, "r")?.use {
                                    it.statSize
                                } ?: state.fileSizeBytes
                            } catch (e: Exception) {
                                state.fileSizeBytes
                            }
                            _pdfExportState.value = PdfExportState.Success(uri, fileSize)
                        }
                        is PdfExportState.Error -> {
                            _pdfExportState.value = state
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _pdfExportState.value = PdfExportState.Error(e, e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Resets PDF export state
     */
    fun resetPdfExportState() {
        _pdfExportState.value = PdfExportState.Idle
    }

    /**
     * Exports the resume to an image file
     */
    fun exportToImage(file: java.io.File, uri: Uri) {
        viewModelScope.launch {
            imageExporter.generateImage(_gridResume.value, file)
                .collect { state ->
                    _imageExportState.value = state
                }
        }
    }

    /**
     * Resets Image export state
     */
    fun resetImageExportState() {
        _imageExportState.value = ImageExportState.Idle
    }

    // ============================================================================
    // Helpers
    // ============================================================================

    private fun updatePage(page: ResumePage) {
        val currentPages = _gridResume.value.pages.toMutableList()
        val pageIndex = currentPages.indexOfFirst { it.id == page.id }
        if (pageIndex != -1) {
            currentPages[pageIndex] = page
            _gridResume.value = _gridResume.value.copy(pages = currentPages)
        }
    }

    private fun createDefaultResume(): GridResume {
        return GridResume(
            // Use linkedResumeId if provided to ensure design ID matches Resume ID
            id = initialLinkedResumeId ?: UUID.randomUUID().toString(),
            name = "My Resume",
            pages = listOf(ResumePage())
        )
    }

    private fun createResumeWithTemplate(templateType: GridTemplateType): GridResume {
        // This would typically load from a template repository or factory
        // For now, we'll just return a basic resume with the template type
        // In a real app, this would populate elements based on the template
        return GridResume(
            // Use linkedResumeId if provided to ensure design ID matches Resume ID
            id = initialLinkedResumeId ?: UUID.randomUUID().toString(),
            name = "My Resume (${templateType.name})",
            pages = listOf(ResumePage())
        )
    }

    private fun getDefaultElementSize(type: ElementType): Pair<Int, Int> {
        return when (type) {
            ElementType.TEXT -> Pair(4, 12) // rowSpan, colSpan
            ElementType.IMAGE -> Pair(8, 8)
            ElementType.SHAPE -> Pair(4, 4)
            ElementType.DIVIDER -> Pair(1, _gridResume.value.gridConfig.columns)
            ElementType.CHART -> Pair(4, 12)
            ElementType.ICON -> Pair(2, 2)
            ElementType.CONTAINER -> Pair(10, 48)
            ElementType.CONTACT -> Pair(2, 48)
            ElementType.WORK_EXPERIENCE -> Pair(10, 48)
            ElementType.EDUCATION -> Pair(8, 48)
            ElementType.SKILL -> Pair(6, 48)
            ElementType.PROJECT -> Pair(8, 48)
            ElementType.CERTIFICATION -> Pair(6, 48)
            ElementType.LANGUAGE -> Pair(4, 48)
        }
    }

    private fun createElementOfType(type: ElementType, position: GridPosition): ResumeElement {
        return when (type) {
            ElementType.TEXT -> ResumeElement.TextElement(position = position.copy(heightMode = SizeMode.WRAP_CONTENT), content = "New Text")
            ElementType.IMAGE -> ResumeElement.ImageElement(position = position)
            ElementType.SHAPE -> ResumeElement.ShapeElement(position = position)
            ElementType.DIVIDER -> ResumeElement.ShapeElement(
                position = position.copy(
                    col = 0,
                    colSpan = _gridResume.value.gridConfig.columns
                ),
                shapeType = ShapeType.DIVIDER,
                style = ElementStyle(
                    borderColor = 0xFF9E9E9E // Gray 500 color
                )
            )
            ElementType.CHART -> ResumeElement.ChartElement(position = position)
            ElementType.ICON -> ResumeElement.IconElement(position = position, iconName = "star")
            ElementType.CONTAINER -> ResumeElement.ContainerElement(position = position)
            ElementType.CONTACT -> ResumeElement.ContactElement(position = position)
            ElementType.WORK_EXPERIENCE -> ResumeElement.WorkExperienceElement(position = position)
            ElementType.EDUCATION -> ResumeElement.EducationElement(position = position)
            ElementType.SKILL -> ResumeElement.SkillElement(position = position)
            ElementType.PROJECT -> ResumeElement.ProjectElement(position = position)
            ElementType.CERTIFICATION -> ResumeElement.CertificationElement(position = position)
            ElementType.LANGUAGE -> ResumeElement.LanguageElement(position = position)
        }
    }

    /**
     * Migrates old resumes to FREE layout mode
     * This ensures loaded resumes allow element overlapping
     */
    private fun migrateToFreeLayoutMode(resume: GridResume): GridResume {
        val updatedPages = resume.pages.map { page ->
            page.copy(layoutMode = LayoutMode.FREE)
        }
        return resume.copy(pages = updatedPages)
    }
    
    // ============================================================================
    // Multi-Page Management
    // ============================================================================
    
    /**
     * Get current page being edited
     */
    fun getCurrentPage(): ResumePage {
        return _gridResume.value.pages.getOrElse(_currentPageIndex.value) {
            getCurrentPage()
        }
    }
    
    /**
     * Get total page count
     */
    fun getPageCount(): Int = _gridResume.value.pages.size
    
    /**
     * Navigate to a specific page
     * 
     * @param index Zero-based page index
     */
    fun setCurrentPage(index: Int) {
        val maxIndex = (_gridResume.value.pages.size - 1).coerceAtLeast(0)
        val newIndex = index.coerceIn(0, maxIndex)
        
        if (newIndex != _currentPageIndex.value) {
            _currentPageIndex.value = newIndex
            _selectedElement.value = null // Deselect when changing pages
            _events.tryEmit(GridEditorEvent.PageChanged(newIndex, _gridResume.value.pages.size))
        }
    }
    
    /**
     * Navigate to the next page (if available)
     */
    fun nextPage() {
        if (_currentPageIndex.value < _gridResume.value.pages.size - 1) {
            setCurrentPage(_currentPageIndex.value + 1)
        }
    }
    
    /**
     * Navigate to the previous page (if available)
     */
    fun previousPage() {
        if (_currentPageIndex.value > 0) {
            setCurrentPage(_currentPageIndex.value - 1)
        }
    }
    
    /**
     * Add a new blank page after the current page
     */
    fun addPage() {
        saveToUndoStack()
        
        val currentPage = getCurrentPage()
        val insertIndex = _currentPageIndex.value + 1
        
        val newPage = ResumePage(
            backgroundColor = currentPage.backgroundColor,
            layoutMode = currentPage.layoutMode,
            paginationInfo = PagePaginationInfo(
                pageIndex = insertIndex,
                isOverflowPage = false
            )
        )
        
        val updatedPages = _gridResume.value.pages.toMutableList().apply {
            add(insertIndex, newPage)
        }
        
        // Update page indices for all pages after the insertion
        val reindexedPages = updatedPages.mapIndexed { index, page ->
            page.copy(
                paginationInfo = page.paginationInfo?.copy(pageIndex = index)
                    ?: PagePaginationInfo(pageIndex = index)
            )
        }
        
        _gridResume.update { resume ->
            resume.copy(pages = reindexedPages)
        }
        
        // Navigate to new page
        _currentPageIndex.value = insertIndex
        _events.tryEmit(GridEditorEvent.PageAdded(insertIndex))

        // Generate thumbnail for the new page
        generatePageThumbnail(newPage.id)

        triggerAutoSave()
    }
    
    /**
     * Add a new page at a specific index
     * 
     * @param index Where to insert the new page
     */
    fun addPageAt(index: Int) {
        saveToUndoStack()
        
        val clampedIndex = index.coerceIn(0, _gridResume.value.pages.size)
        val currentPage = getCurrentPage()
        
        val newPage = ResumePage(
            backgroundColor = currentPage.backgroundColor,
            layoutMode = currentPage.layoutMode,
            paginationInfo = PagePaginationInfo(pageIndex = clampedIndex)
        )
        
        val updatedPages = _gridResume.value.pages.toMutableList().apply {
            add(clampedIndex, newPage)
        }
        
        val reindexedPages = updatedPages.mapIndexed { idx, page ->
            page.copy(
                paginationInfo = page.paginationInfo?.copy(pageIndex = idx)
                    ?: PagePaginationInfo(pageIndex = idx)
            )
        }
        
        _gridResume.update { resume ->
            resume.copy(pages = reindexedPages)
        }
        
        _events.tryEmit(GridEditorEvent.PageAdded(clampedIndex))
        triggerAutoSave()
    }
    
    /**
     * Remove a page by index
     * Cannot remove the last remaining page
     * 
     * @param index Page index to remove
     * @return true if page was removed, false if not possible
     */
    fun removePage(index: Int): Boolean {
        if (_gridResume.value.pages.size <= 1) {
            return false // Cannot remove last page
        }

        if (index !in _gridResume.value.pages.indices) {
            return false
        }

        saveToUndoStack()

        // Get the page ID before removing it
        val removedPageId = _gridResume.value.pages[index].id

        val updatedPages = _gridResume.value.pages.toMutableList().apply {
            removeAt(index)
        }
        
        // Reindex remaining pages
        val reindexedPages = updatedPages.mapIndexed { idx, page ->
            page.copy(
                paginationInfo = page.paginationInfo?.copy(pageIndex = idx)
                    ?: PagePaginationInfo(pageIndex = idx)
            )
        }
        
        _gridResume.update { resume ->
            resume.copy(pages = reindexedPages)
        }
        
        // Adjust current page index if needed
        if (_currentPageIndex.value >= _gridResume.value.pages.size) {
            _currentPageIndex.value = _gridResume.value.pages.size - 1
        } else if (_currentPageIndex.value > index) {
            _currentPageIndex.value = _currentPageIndex.value - 1
        }
        
        // Clear thumbnail for removed page
        clearPageThumbnail(removedPageId)

        _events.tryEmit(GridEditorEvent.PageRemoved(index))
        triggerAutoSave()

        return true
    }
    
    /**
     * Remove current page
     */
    fun removeCurrentPage(): Boolean {
        return removePage(_currentPageIndex.value)
    }
    
    /**
     * Duplicate a page
     * 
     * @param index Page index to duplicate
     */
    fun duplicatePage(index: Int) {
        if (index !in _gridResume.value.pages.indices) return
        
        saveToUndoStack()
        
        val sourcePage = _gridResume.value.pages[index]
        val insertIndex = index + 1
        
        // Create deep copy of elements with new IDs
        val copiedElements = sourcePage.elements.map { element ->
            copyElementWithNewId(element)
        }
        
        val newPage = sourcePage.copy(
            id = UUID.randomUUID().toString(),
            elements = copiedElements,
            paginationInfo = PagePaginationInfo(pageIndex = insertIndex, isOverflowPage = false)
        )
        
        val updatedPages = _gridResume.value.pages.toMutableList().apply {
            add(insertIndex, newPage)
        }
        
        val reindexedPages = updatedPages.mapIndexed { idx, page ->
            page.copy(
                paginationInfo = page.paginationInfo?.copy(pageIndex = idx)
                    ?: PagePaginationInfo(pageIndex = idx)
            )
        }
        
        _gridResume.update { resume ->
            resume.copy(pages = reindexedPages)
        }
        
        _currentPageIndex.value = insertIndex
        _events.tryEmit(GridEditorEvent.PageAdded(insertIndex))
        triggerAutoSave()
    }
    
    /**
     * Create a copy of an element with a new ID
     */
    private fun copyElementWithNewId(element: ResumeElement): ResumeElement {
        val newId = UUID.randomUUID().toString()
        return when (element) {
            is ResumeElement.TextElement -> element.copy(id = newId)
            is ResumeElement.ImageElement -> element.copy(id = newId)
            is ResumeElement.ShapeElement -> element.copy(id = newId)
            is ResumeElement.ChartElement -> element.copy(id = newId)
            is ResumeElement.ContainerElement -> element.copy(
                id = newId,
                children = element.children // Note: child IDs remain same - may need updating
            )
            is ResumeElement.IconElement -> element.copy(id = newId)
            is ResumeElement.ContactElement -> element.copy(id = newId)
            is ResumeElement.WorkExperienceElement -> element.copy(id = newId)
            is ResumeElement.EducationElement -> element.copy(id = newId)
            is ResumeElement.SkillElement -> element.copy(id = newId)
            is ResumeElement.ProjectElement -> element.copy(id = newId)
            is ResumeElement.CertificationElement -> element.copy(id = newId)
            is ResumeElement.LanguageElement -> element.copy(id = newId)
        }
    }
    
    // ============================================================================
    // Pagination & Overflow Detection
    // ============================================================================
    
    /**
     * Enable or disable auto-pagination
     */
    fun setAutoPaginationEnabled(enabled: Boolean) {
        _autoPaginationEnabled.value = enabled
        if (enabled) {
            checkAndAutoPaginate()
        }
    }
    
    /**
     * Manually trigger pagination
     * Analyzes all pages and splits/moves content as needed
     */
    fun triggerPagination() {
        viewModelScope.launch(Dispatchers.Default) {
            saveToUndoStack()
            
            val originalPageCount = _gridResume.value.pages.size
            val result = paginationEngine.paginate(_gridResume.value)
            
            withContext(Dispatchers.Main) {
                _gridResume.update { resume ->
                    resume.copy(
                        pages = result.pages,
                        linkedElementGroups = result.linkedElementGroups
                    )
                }
                
                // Count elements that were moved/split
                val elementsMovedCount = result.linkedElementGroups.sumOf { 
                    it.linkedElementIds.size 
                }
                
                _events.emit(GridEditorEvent.PaginationCompleted(
                    pageCount = result.pages.size,
                    elementsMovedCount = elementsMovedCount
                ))
                
                // Adjust current page if needed
                if (_currentPageIndex.value >= result.pages.size) {
                    _currentPageIndex.value = (result.pages.size - 1).coerceAtLeast(0)
                }
                
                // Clear overflow info after successful pagination
                _overflowInfo.value = null
                
                triggerAutoSave()
            }
        }
    }
    
    /**
     * Check for overflow and auto-paginate if enabled
     * Called internally after content changes
     */
    private fun checkAndAutoPaginate() {
        if (!_autoPaginationEnabled.value) return
        
        viewModelScope.launch(Dispatchers.Default) {
            // First detect overflow
            val allElements = _gridResume.value.pages.flatMap { it.elements }
            val currentPage = getCurrentPage()
            val overflow = paginationEngine.detectOverflow(currentPage, allElements)
            
            withContext(Dispatchers.Main) {
                _overflowInfo.value = overflow
            }
            
            // If significant overflow, auto-paginate
            if (overflow != null && overflow.overflowAmountDp > 10f) {
                val result = paginationEngine.paginate(_gridResume.value)
                
                // Only update if pages actually changed
                if (result.pages.size != _gridResume.value.pages.size ||
                    result.pages != _gridResume.value.pages) {
                    
                    withContext(Dispatchers.Main) {
                        _gridResume.update { resume ->
                            resume.copy(
                                pages = result.pages,
                                linkedElementGroups = result.linkedElementGroups
                            )
                        }
                        
                        _events.emit(GridEditorEvent.PaginationCompleted(
                            pageCount = result.pages.size,
                            elementsMovedCount = result.linkedElementGroups.sumOf { it.linkedElementIds.size }
                        ))
                        
                        _overflowInfo.value = null
                    }
                }
            }
        }
    }
    
    /**
     * Detect overflow on current page without paginating
     * Updates the overflowInfo state for visual indicators
     */
    fun detectOverflow() {
        viewModelScope.launch(Dispatchers.Default) {
            val allElements = _gridResume.value.pages.flatMap { it.elements }
            val currentPage = getCurrentPage()
            val overflow = paginationEngine.detectOverflow(currentPage, allElements)
            
            withContext(Dispatchers.Main) {
                _overflowInfo.value = overflow
                
                if (overflow != null) {
                    _events.tryEmit(GridEditorEvent.OverflowDetected(
                        overflowAmountDp = overflow.overflowAmountDp,
                        elementCount = overflow.overflowingElementIds.size
                    ))
                }
            }
        }
    }
    
    /**
     * Move an element to a different page
     * 
     * @param elementId Element to move
     * @param targetPageIndex Destination page
     */
    fun moveElementToPage(elementId: String, targetPageIndex: Int) {
        if (targetPageIndex !in _gridResume.value.pages.indices) return
        
        saveToUndoStack()
        
        // Find current page containing the element
        val sourcePageIndex = _gridResume.value.pages.indexOfFirst { page ->
            page.elements.any { it.id == elementId }
        }
        
        if (sourcePageIndex == -1 || sourcePageIndex == targetPageIndex) return
        
        val element = _gridResume.value.pages[sourcePageIndex].elements
            .find { it.id == elementId } ?: return
        
        val updatedPages = _gridResume.value.pages.toMutableList()
        
        // Remove from source page
        updatedPages[sourcePageIndex] = updatedPages[sourcePageIndex].copy(
            elements = updatedPages[sourcePageIndex].elements.filter { it.id != elementId }
        )
        
        // Add to target page (position at top)
        val elementAtTop = updateElementPosition(element, element.position.copy(row = 0))
        updatedPages[targetPageIndex] = updatedPages[targetPageIndex].copy(
            elements = updatedPages[targetPageIndex].elements + elementAtTop
        )
        
        _gridResume.update { resume ->
            resume.copy(pages = updatedPages)
        }
        
        // Select the moved element on the target page
        _currentPageIndex.value = targetPageIndex
        _selectedElement.value = elementAtTop
        
        triggerAutoSave()
    }
    
    /**
     * Helper to update element position
     */
    private fun updateElementPosition(element: ResumeElement, newPosition: GridPosition): ResumeElement {
        return when (element) {
            is ResumeElement.TextElement -> element.copy(position = newPosition)
            is ResumeElement.ImageElement -> element.copy(position = newPosition)
            is ResumeElement.ShapeElement -> element.copy(position = newPosition)
            is ResumeElement.ChartElement -> element.copy(position = newPosition)
            is ResumeElement.ContainerElement -> element.copy(position = newPosition)
            is ResumeElement.IconElement -> element.copy(position = newPosition)
            is ResumeElement.ContactElement -> element.copy(position = newPosition)
            is ResumeElement.WorkExperienceElement -> element.copy(position = newPosition)
            is ResumeElement.EducationElement -> element.copy(position = newPosition)
            is ResumeElement.SkillElement -> element.copy(position = newPosition)
            is ResumeElement.ProjectElement -> element.copy(position = newPosition)
            is ResumeElement.CertificationElement -> element.copy(position = newPosition)
            is ResumeElement.LanguageElement -> element.copy(position = newPosition)
        }
    }
    
    /**
     * Get information about linked element groups (for UI display)
     */
    fun getLinkedGroupForElement(elementId: String): LinkedElementGroup? {
        return _gridResume.value.getLinkedGroupForElement(elementId)
    }
    
    /**
     * Check if an element is a continuation from a previous page
     */
    fun isElementContinuation(elementId: String): Boolean {
        return _gridResume.value.linkedElementGroups.any { group ->
            group.linkedElementIds.contains(elementId)
        }
    }
    
    /**
     * Check if an element continues on the next page
     */
    fun elementContinuesOnNextPage(elementId: String): Boolean {
        return _gridResume.value.linkedElementGroups.any { group ->
            group.sourceElementId == elementId && group.linkedElementIds.isNotEmpty()
        }
    }

    // ============================================================================
    // Page Thumbnail Management
    // ============================================================================

    /**
     * Generate thumbnail for a specific page
     */
    private fun generatePageThumbnail(pageId: String, forceRegenerate: Boolean = false) {
        if (!forceRegenerate && _pageThumbnails.value.containsKey(pageId)) return

        val page = _gridResume.value.pages.find { it.id == pageId } ?: return

        viewModelScope.launch(Dispatchers.Default) {
            try {
                val singlePageResume = _gridResume.value.copy(pages = listOf(page))
                val thumbnailBase64 = thumbnailGenerator.generateThumbnail(singlePageResume)

                if (thumbnailBase64.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _pageThumbnails.update { it + (pageId to thumbnailBase64) }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("GridEditorViewModel", "Thumbnail generation failed", e)
            }
        }
    }

    /**
     * Generate thumbnails for all pages
     */
    fun generateAllPageThumbnails() {
        _gridResume.value.pages.forEach { page ->
            generatePageThumbnail(page.id, forceRegenerate = false)
        }
    }

    /**
     * Regenerate thumbnail for current page with debouncing
     */
    private fun regenerateCurrentPageThumbnail() {
        thumbnailGenerationJob?.cancel()
        thumbnailGenerationJob = viewModelScope.launch {
            delay(500) // Debounce: avoid excessive regeneration
            val currentPage = getCurrentPage()
            generatePageThumbnail(currentPage.id, forceRegenerate = true)
        }
    }

    /**
     * Clear thumbnail for a specific page
     */
    private fun clearPageThumbnail(pageId: String) {
        _pageThumbnails.update { it - pageId }
    }
}

/**
 * Data class for drag state
 */
data class DragState(
    val element: ResumeElement,
    val originalPosition: GridPosition,
    val currentPosition: GridPosition,
    val isValidPosition: Boolean,
    val hoveredContainerId: String? = null,  // Container being hovered over
    val hoverProgress: Float = 0f,            // 0.0 to 1.0 progress towards auto-add
    val targetInsertionIndex: Int? = null,     // Target index for vertical container reordering
    val dragOffsetY: Float = 0f,              // Vertical drag offset in pixels
    val originalCaptureY: Float = 0f          // Original Y position in pixels (captured at drag start)
)

/**
 * Events for UI consumption
 */
sealed class GridEditorEvent {
    object ResumeLoaded : GridEditorEvent()
    data class SaveSuccess(val message: String) : GridEditorEvent()
    data class SaveError(val message: String) : GridEditorEvent()
    data class TemplateApplied(val template: GridTemplateType) : GridEditorEvent()
    
    // Multi-page events
    data class PageChanged(val pageIndex: Int, val totalPages: Int) : GridEditorEvent()
    data class PageAdded(val pageIndex: Int) : GridEditorEvent()
    data class PageRemoved(val pageIndex: Int) : GridEditorEvent()
    data class PaginationCompleted(val pageCount: Int, val elementsMovedCount: Int) : GridEditorEvent()
    data class OverflowDetected(val overflowAmountDp: Float, val elementCount: Int) : GridEditorEvent()
}

