package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.local.GridResumeEntity
import com.phamnhantucode.aicareercoach.data.resume.GridResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for Resume Design Screen
 * Manages saved grid resume designs and templates
 */
class ResumeDesignViewModel(context: Context) : ViewModel() {

    private val repository = GridResumeRepository.getInstance(context)

    // State flows
    private val _designs = MutableStateFlow<List<GridResumeEntity>>(emptyList())
    val designs: StateFlow<List<GridResumeEntity>> = _designs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Available templates (from GridTemplateType enum)
    val templates: List<GridTemplateType> = GridTemplateType.values().toList()

    init {
        loadDesigns()
    }

    /**
     * Load all saved designs for the current user
     */
    fun loadDesigns() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            val result = repository.getAllDesigns()
            result.fold(
                onSuccess = { designList ->
                    _designs.value = designList
                    _isLoading.value = false
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load designs"
                    _isLoading.value = false
                }
            )
        }
    }

    /**
     * Delete a design by ID
     */
    fun deleteDesign(designId: String) {
        viewModelScope.launch {
            val result = repository.deleteDesign(designId)
            result.fold(
                onSuccess = {
                    // Reload designs after successful deletion
                    loadDesigns()
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete design"
                }
            )
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get design count
     */
    suspend fun getDesignCount(): Int {
        return repository.getDesignCount()
    }
}
