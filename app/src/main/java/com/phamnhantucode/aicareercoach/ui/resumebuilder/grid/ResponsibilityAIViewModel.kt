package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.ai.ResponsibilityAIRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing AI-powered responsibility improvement
 */
class ResponsibilityAIViewModel(
    private val repository: ResponsibilityAIRepository = ResponsibilityAIRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ResponsibilityAIState>(ResponsibilityAIState.Initial)
    val uiState: StateFlow<ResponsibilityAIState> = _uiState.asStateFlow()

    private val _options = MutableStateFlow(ResponsibilityImprovementOptions())
    val options: StateFlow<ResponsibilityImprovementOptions> = _options.asStateFlow()

    private val _showCreditDialog = MutableStateFlow(false)
    val showCreditDialog: StateFlow<Boolean> = _showCreditDialog.asStateFlow()

    fun dismissCreditDialog() {
        _showCreditDialog.value = false
    }

    fun openPurchaseScreen() {
        // TODO: Navigation to purchase screen
        _showCreditDialog.value = false
    }


    fun updateOptions(newOptions: ResponsibilityImprovementOptions) {
        _options.value = newOptions
    }


    fun toggleOption(optionType: OptionType) {
        _options.value = when (optionType) {
            OptionType.PROFESSIONAL -> _options.value.copy(makeProfessional = !_options.value.makeProfessional)
            OptionType.ADD_METRICS -> _options.value.copy(addMetrics = !_options.value.addMetrics)
            OptionType.CONCISE -> _options.value.copy(makeConcise = !_options.value.makeConcise)
            OptionType.DETAILED -> _options.value.copy(makeDetailed = !_options.value.makeDetailed)
            OptionType.FORMAT_AS_BULLETS -> _options.value.copy(formatAsBullets = !_options.value.formatAsBullets)
        }
    }


    fun setBulletFormat(enabled: Boolean) {
        _options.value = _options.value.copy(formatAsBullets = enabled)
    }


    fun generateSuggestions(
        currentText: String,
        jobTitle: String,
        company: String,
        userContext: ResponsibilityAIRepository.UserContext? = null,
        otherResponsibilities: List<String> = emptyList()
    ) {
        if (!_options.value.hasAnySelected()) {
            _uiState.value = ResponsibilityAIState.Error("Please select at least one improvement option")
            return
        }

        viewModelScope.launch {
            _uiState.value = ResponsibilityAIState.Loading
            try {
                val result = repository.improveResponsibility(
                    currentText = currentText,
                    jobTitle = jobTitle,
                    company = company,
                    options = _options.value,
                    userContext = userContext,
                    otherResponsibilities = otherResponsibilities
                )
                _uiState.value = ResponsibilityAIState.Success(result)
            } catch (e: Exception) {
                if (e is com.phamnhantucode.aicareercoach.data.neon.NeonUserService.InsufficientCreditException) {
                    _showCreditDialog.value = true
                    _uiState.value = ResponsibilityAIState.Initial
                    return@launch
                }
                _uiState.value = ResponsibilityAIState.Error(
                    e.message ?: "Failed to generate suggestions. Please try again."
                )
            }
        }
    }


    fun reset() {
        _uiState.value = ResponsibilityAIState.Initial
        _options.value = ResponsibilityImprovementOptions()
    }


    fun dismissError() {
        if (_uiState.value is ResponsibilityAIState.Error) {
            _uiState.value = ResponsibilityAIState.Initial
        }
    }
}

/**
 * UI State for the AI improvement feature
 */
sealed class ResponsibilityAIState {
    object Initial : ResponsibilityAIState()
    object Loading : ResponsibilityAIState()
    data class Success(val result: ResponsibilityImprovementResult) : ResponsibilityAIState()
    data class Error(val message: String) : ResponsibilityAIState()
}

/**
 * Types of improvement options
 */
enum class OptionType {
    PROFESSIONAL,
    ADD_METRICS,
    CONCISE,
    DETAILED,
    FORMAT_AS_BULLETS
}
