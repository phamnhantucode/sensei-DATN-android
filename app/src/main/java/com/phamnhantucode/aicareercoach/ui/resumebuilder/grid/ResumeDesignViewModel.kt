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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for Resume Design Screen
 * Manages saved grid resume designs and templates
 */
class ResumeDesignViewModel(context: Context) : ViewModel() {

    private val repository = GridResumeRepository.getInstance(context)
    private val templateLoader = TemplateLoader.getInstance(context)

    // State flows
    private val _designs = MutableStateFlow<List<GridResumeEntity>>(emptyList())
    val designs: StateFlow<List<GridResumeEntity>> = _designs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Templates loaded from assets
    private val _templates = MutableStateFlow<List<ResumeTemplate>>(emptyList())
    val templates: StateFlow<List<ResumeTemplate>> = _templates.asStateFlow()

    private val _isLoadingTemplates = MutableStateFlow(false)
    val isLoadingTemplates: StateFlow<Boolean> = _isLoadingTemplates.asStateFlow()

    init {
        loadDesigns()
        loadTemplates()
    }

    /**
     * Load templates from assets folder
     */
    private fun loadTemplates() {
        viewModelScope.launch {
            _isLoadingTemplates.value = true
            try {
                val loadedTemplates = templateLoader.getAllTemplates()
                _templates.value = loadedTemplates
            } catch (e: Exception) {
                android.util.Log.e("ResumeDesignViewModel", "Failed to load templates", e)
                _error.value = "Failed to load templates"
            } finally {
                _isLoadingTemplates.value = false
            }
        }
    }

    /**
     * Get a GridResume from a template for editing
     */
    suspend fun getTemplateGridResume(template: ResumeTemplate): GridResume? {
        return templateLoader.getTemplateGridResume(template)
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
            // Optimistically update UI immediately
            _designs.update { currentList ->
                currentList.filter { it.id != designId }
            }

            // Perform database deletion
            val result = repository.deleteDesign(designId)
            result.fold(
                onSuccess = {
                    // Successfully deleted - UI already updated
                },
                onFailure = { exception ->
                    // Rollback on failure
                    loadDesigns()
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
