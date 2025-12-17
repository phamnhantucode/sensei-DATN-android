package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.resume.GridResumeRepository
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Data class to hold resume with its design thumbnail
 */
data class ResumeWithThumbnail(
    val resume: Resume,
    val thumbnail: String? = null // Base64 encoded thumbnail from grid design
)

/**
 * ViewModel for ResumeListScreen
 * Manages the list of user's resumes
 */
class ResumeListViewModel(context: Context) : ViewModel() {

    companion object {
        private const val TAG = "ResumeListViewModel"
    }

    private val repository = ResumeRepository.getInstance(context)
    private val gridResumeRepository = GridResumeRepository.getInstance(context)

    private val _resumes = MutableStateFlow<List<ResumeWithThumbnail>>(emptyList())
    val resumes: StateFlow<List<ResumeWithThumbnail>> = _resumes.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isDeleting = MutableStateFlow(false)
    val isDeleting: StateFlow<Boolean> = _isDeleting.asStateFlow()
    
    // For undo functionality - stores the deleted resume temporarily
    private var pendingDeleteResume: ResumeWithThumbnail? = null
    private var pendingDeleteIndex: Int = -1

    init {
        loadResumes()
    }

    /**
     * Loads all resumes for the current user
     */
    fun loadResumes() {
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Always fetch from remote on first load to ensure we have the latest data
                val result = repository.getAllResumes(forceRemote = true)
                if (result.isSuccess) {
                    val resumeList = result.getOrNull() ?: emptyList()
                    
                    // Fetch thumbnails from grid resume designs
                    val gridDesignsResult = gridResumeRepository.getAllDesigns()
                    val gridDesigns = gridDesignsResult.getOrNull() ?: emptyList()
                    
                    // Debug: Log all IDs to understand the mismatch
                    Log.d(TAG, "=== THUMBNAIL DEBUG ===")
                    Log.d(TAG, "Resume IDs: ${resumeList.map { it.id }}")
                    Log.d(TAG, "Grid Design IDs: ${gridDesigns.map { it.id }}")
                    Log.d(TAG, "Grid Designs with thumbnails: ${gridDesigns.filter { it.thumbnail.isNotBlank() }.map { "${it.id} -> thumbnail: ${it.thumbnail.take(50)}..." }}")
                    
                    val thumbnailMap = gridDesigns
                        .filter { it.thumbnail.isNotBlank() }
                        .associate { it.id to it.thumbnail }
                    
                    // Combine resumes with their thumbnails
                    _resumes.value = resumeList.map { resume ->
                        val thumbnail = thumbnailMap[resume.id]
                        Log.d(TAG, "Resume ${resume.id}: thumbnail match = ${thumbnail != null}, thumbnail = ${thumbnail?.take(50) ?: "null"}")
                        ResumeWithThumbnail(
                            resume = resume,
                            thumbnail = thumbnail
                        )
                    }
                    Log.d(TAG, "Loaded ${_resumes.value.size} resumes with ${thumbnailMap.size} thumbnails")
                    
                    // No need to sync - data is already from Neon (Neon-only mode)
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to load resumes"
                    Log.e(TAG, "Failed to load resumes", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
                Log.e(TAG, "Error loading resumes", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Deletes a resume by ID with callback for undo support
     * The deletion is optimistic - UI updates immediately, then syncs to backend
     */
    fun deleteResume(resumeId: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isDeleting.value = true
                
                // Store for potential undo
                val currentList = _resumes.value
                val index = currentList.indexOfFirst { it.resume.id == resumeId }
                if (index != -1) {
                    pendingDeleteResume = currentList[index]
                    pendingDeleteIndex = index
                    
                    // Optimistically remove from UI immediately
                    _resumes.value = currentList.filter { it.resume.id != resumeId }
                }
                
                Log.d(TAG, "Deleted resume $resumeId (pending confirmation)")
                _isDeleting.value = false
                onComplete(true)
            } catch (e: Exception) {
                _error.value = e.message ?: "Error deleting resume"
                Log.e(TAG, "Error deleting resume", e)
                _isDeleting.value = false
                onComplete(false)
            }
        }
    }
    
    /**
     * Confirms the pending deletion - actually deletes from database
     */
    fun confirmDelete() {
        val resumeToDelete = pendingDeleteResume ?: return
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = repository.deleteResume(resumeToDelete.resume.id, syncToRemote = true)
                if (result.isSuccess) {
                    // Also delete the grid design if exists
                    gridResumeRepository.deleteDesign(resumeToDelete.resume.id)
                    Log.d(TAG, "Confirmed deletion of resume ${resumeToDelete.resume.id}")
                } else {
                    Log.e(TAG, "Failed to confirm delete", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error confirming delete", e)
            } finally {
                pendingDeleteResume = null
                pendingDeleteIndex = -1
            }
        }
    }
    
    /**
     * Undoes the pending deletion - restores the resume to the list
     */
    fun undoDelete() {
        val resumeToRestore = pendingDeleteResume ?: return
        val index = pendingDeleteIndex
        
        viewModelScope.launch {
            val currentList = _resumes.value.toMutableList()
            // Insert at original position if possible
            if (index in 0..currentList.size) {
                currentList.add(index, resumeToRestore)
            } else {
                currentList.add(0, resumeToRestore)
            }
            _resumes.value = currentList
            
            Log.d(TAG, "Restored resume ${resumeToRestore.resume.id}")
            pendingDeleteResume = null
            pendingDeleteIndex = -1
        }
    }

    /**
     * Duplicates an existing resume
     */
    fun duplicateResume(resumeId: String) {
        val resumeToDuplicate = _resumes.value.find { it.resume.id == resumeId }?.resume ?: return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isLoading.value = true
                
                // Create a copy with new ID and updated name
                val newId = java.util.UUID.randomUUID().toString()
                
                val newResume = resumeToDuplicate.copy(
                    id = newId,
                    personalInfo = resumeToDuplicate.personalInfo.copy(
                        fullName = "${resumeToDuplicate.personalInfo.fullName} (Copy)"
                    )
                )
                
                // Save the new resume
                val result = repository.saveResume(newResume)
                
                if (result.isSuccess) {
                    Log.d(TAG, "Duplicated resume $resumeId to $newId")
                    
                    // Also try to duplicate the design if it exists
                    try {
                        val designResult = gridResumeRepository.getDesign(resumeId)
                        val design = designResult.getOrNull()
                        if (design != null) {
                            val newDesign = design.copy(
                                id = newId
                            )
                            gridResumeRepository.saveDesign(newDesign)
                            Log.d(TAG, "Duplicated design for resume $newId")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to duplicate design (non-fatal)", e)
                    }
                    
                    // Reload list
                    loadResumes()
                } else {
                    _error.value = result.exceptionOrNull()?.message ?: "Failed to duplicate resume"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error duplicating resume", e)
                _error.value = e.message ?: "Error duplicating resume"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Parsing state with detailed progress
     */
    enum class ParsingStage {
        IDLE,
        READING_PDF,
        EXTRACTING_TEXT,
        ANALYZING_WITH_AI,
        SAVING_RESUME,
        COMPLETED,
        ERROR
    }
    
    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()
    
    private val _parsingStage = MutableStateFlow(ParsingStage.IDLE)
    val parsingStage: StateFlow<ParsingStage> = _parsingStage.asStateFlow()
    
    private val _parsingError = MutableStateFlow<String?>(null)
    val parsingError: StateFlow<String?> = _parsingError.asStateFlow()
    
    private val _parsedResume = MutableStateFlow<Resume?>(null)
    val parsedResume: StateFlow<Resume?> = _parsedResume.asStateFlow()
    
    // Store URI for retry
    private var lastPdfUri: android.net.Uri? = null

    private val resumeParserRepository = com.phamnhantucode.aicareercoach.data.ai.ResumeParserRepository()

    /**
     * Parses a resume from a PDF URI with detailed progress tracking
     */
    fun parseResumeFromPdf(context: Context, pdfUri: android.net.Uri) {
        lastPdfUri = pdfUri
        viewModelScope.launch {
            try {
                _isParsing.value = true
                _error.value = null
                _parsingError.value = null
                _parsedResume.value = null
                
                // Stage 1: Reading PDF file
                _parsingStage.value = ParsingStage.READING_PDF
                kotlinx.coroutines.delay(300) // Small delay for UX
                
                // Stage 2: Extract text from PDF
                _parsingStage.value = ParsingStage.EXTRACTING_TEXT
                val rawText = com.phamnhantucode.aicareercoach.utils.PdfTextExtractor.extractText(
                    context = context,
                    pdfUri = pdfUri
                )
                
                if (rawText.isBlank()) {
                    throw Exception("Could not extract any text from the PDF. It might be an image-only PDF or a scanned document.")
                }
                
                // Stage 3: Parse with AI
                _parsingStage.value = ParsingStage.ANALYZING_WITH_AI
                val parsedResume = resumeParserRepository.parseResume(rawText)
                
                // Stage 4: Save to repository
                _parsingStage.value = ParsingStage.SAVING_RESUME
                val result = repository.saveResume(parsedResume)
                
                if (result.isSuccess) {
                    _parsingStage.value = ParsingStage.COMPLETED
                    _parsedResume.value = parsedResume
                    // Reload list in background
                    loadResumes()
                } else {
                    throw result.exceptionOrNull() ?: Exception("Failed to save parsed resume")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing resume from PDF", e)
                _parsingStage.value = ParsingStage.ERROR
                _parsingError.value = e.message ?: "An unknown error occurred"
            } finally {
                _isParsing.value = false
            }
        }
    }
    
    /**
     * Retry parsing the last PDF
     */
    fun retryParsing(context: Context) {
        lastPdfUri?.let { uri ->
            parseResumeFromPdf(context, uri)
        }
    }
    
    /**
     * Dismiss the parsing dialog (success or error)
     */
    fun dismissParsingDialog() {
        _parsingStage.value = ParsingStage.IDLE
        _parsingError.value = null
        _parsedResume.value = null
    }

    /**
     * Clears any error message
     */
    fun clearError() {
        _error.value = null
    }

    // Resume Enhancement
    private val enhancementRepository = com.phamnhantucode.aicareercoach.data.ai.ResumeEnhancementRepository()
    
    private val _isEnhancing = MutableStateFlow(false)
    val isEnhancing: StateFlow<Boolean> = _isEnhancing.asStateFlow()
    
    private val _enhancementSuggestions = MutableStateFlow<com.phamnhantucode.aicareercoach.data.ai.ResumeEnhancementRepository.EnhancementSuggestions?>(null)
    val enhancementSuggestions: StateFlow<com.phamnhantucode.aicareercoach.data.ai.ResumeEnhancementRepository.EnhancementSuggestions?> = _enhancementSuggestions.asStateFlow()
    
    private val _enhancementError = MutableStateFlow<String?>(null)
    val enhancementError: StateFlow<String?> = _enhancementError.asStateFlow()

    private var currentEnhancingResumeId: String? = null

    fun enhanceResume(resumeId: String, jobDescription: String?) {
        val resume = _resumes.value.find { it.resume.id == resumeId }?.resume ?: return
        
        viewModelScope.launch {
            try {
                _isEnhancing.value = true
                _enhancementError.value = null
                _enhancementSuggestions.value = null
                currentEnhancingResumeId = resumeId
                
                val suggestions = enhancementRepository.enhanceResume(resume, jobDescription)
                _enhancementSuggestions.value = suggestions
                
            } catch (e: Exception) {
                Log.e(TAG, "Error enhancing resume", e)
                _enhancementError.value = e.message ?: "Failed to enhance resume"
            } finally {
                _isEnhancing.value = false
            }
        }
    }

    fun applyEnhancement(suggestionType: SuggestionType) {
        val resumeId = currentEnhancingResumeId ?: return
        val currentResume = _resumes.value.find { it.resume.id == resumeId }?.resume ?: return
        
        viewModelScope.launch {
            try {
                val updatedResume = when (suggestionType) {
                    is SuggestionType.ProfessionalSummary -> {
                        currentResume.copy(professionalSummary = suggestionType.enhanced)
                    }
                    is SuggestionType.WorkExperience -> {
                        val updatedExperiences = currentResume.workExperiences.map { exp ->
                            if (exp.id == suggestionType.id) {
                                exp.copy(responsibilities = suggestionType.responsibilities)
                            } else {
                                exp
                            }
                        }
                        currentResume.copy(workExperiences = updatedExperiences)
                    }
                    is SuggestionType.Skills -> {
                        currentResume.copy(skills = suggestionType.skills)
                    }
                }
                
                // Save the updated resume
                val result = repository.saveResume(updatedResume)
                if (result.isSuccess) {
                    // Update local state
                    _resumes.value = _resumes.value.map { resumeWithThumbnail ->
                        if (resumeWithThumbnail.resume.id == resumeId) {
                            resumeWithThumbnail.copy(resume = updatedResume)
                        } else {
                            resumeWithThumbnail
                        }
                    }
                    Log.d(TAG, "Applied enhancement to resume $resumeId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error applying enhancement", e)
                _error.value = "Failed to apply enhancement: ${e.message}"
            }
        }
    }

    fun clearEnhancementDialog() {
        _enhancementSuggestions.value = null
        _enhancementError.value = null
        currentEnhancingResumeId = null
    }
}
