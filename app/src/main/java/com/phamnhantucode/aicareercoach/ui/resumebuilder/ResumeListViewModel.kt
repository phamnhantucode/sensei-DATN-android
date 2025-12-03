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
                    Log.d(TAG, "Grid Designs with thumbnails: ${gridDesigns.filter { it.thumbnail.isNotBlank() }.map { "${it.id} -> has thumbnail" }}")
                    
                    val thumbnailMap = gridDesigns
                        .filter { it.thumbnail.isNotBlank() }
                        .associate { it.id to it.thumbnail }
                    
                    // Combine resumes with their thumbnails
                    _resumes.value = resumeList.map { resume ->
                        val thumbnail = thumbnailMap[resume.id]
                        Log.d(TAG, "Resume ${resume.id}: thumbnail match = ${thumbnail != null}")
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
     * Clears any error message
     */
    fun clearError() {
        _error.value = null
    }
}
