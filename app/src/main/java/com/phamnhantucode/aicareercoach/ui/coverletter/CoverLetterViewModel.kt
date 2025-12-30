package com.phamnhantucode.aicareercoach.ui.coverletter

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.coverletter.CoverLetterRepository
import com.phamnhantucode.aicareercoach.data.coverletter.EmailType
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.ui.resumebuilder.Resume
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant

data class CoverLetterEntry(
    val id: String,
    val type: EmailType,
    val companyName: String,
    val jobTitle: String,
    val jobDescription: String,
    val content: String = "",
    val createdAt: Instant
)

sealed interface CoverLetterUiState {
    data object Loading : CoverLetterUiState
    data class Success(val coverLetters: List<CoverLetterEntry>) : CoverLetterUiState
    data class Error(val message: String) : CoverLetterUiState
}

sealed interface GenerationState {
    data object Idle : GenerationState
    data object Generating : GenerationState
    data class Success(val content: String) : GenerationState
    data class Error(val message: String) : GenerationState
}

class CoverLetterViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = CoverLetterRepository()
    private val resumeRepository = ResumeRepository.getInstance(application)

    private val _uiState = MutableStateFlow<CoverLetterUiState>(CoverLetterUiState.Loading)
    val uiState: StateFlow<CoverLetterUiState> = _uiState.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    private val _resumes = MutableStateFlow<List<Resume>>(emptyList())
    val resumes: StateFlow<List<Resume>> = _resumes.asStateFlow()

    private val _showCreditDialog = MutableStateFlow(false)
    val showCreditDialog: StateFlow<Boolean> = _showCreditDialog.asStateFlow()

    private val _selectedType = MutableStateFlow(EmailType.APPLICATION)
    val selectedType: StateFlow<EmailType> = _selectedType.asStateFlow()

    init {
        loadCoverLetters()
        loadResumes()
    }

    private fun loadResumes() {
        viewModelScope.launch {
            try {

                val result = resumeRepository.getAllResumes(forceRemote = true)
                if (result.isSuccess) {
                    _resumes.value = result.getOrNull() ?: emptyList()
                } else {
                    Log.e(TAG, "Failed to load resumes: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading resumes", e)
            }
        }
    }

    fun loadCoverLetters() {
        viewModelScope.launch {
            _uiState.update { CoverLetterUiState.Loading }
            try {
                val records = repository.fetchUserCoverLetters()
                val entries = records.map { record ->
                    CoverLetterEntry(
                        id = record.id,
                        type = record.type,
                        companyName = record.companyName,
                        jobTitle = record.jobTitle,
                        jobDescription = record.jobDescription,
                        content = record.content,
                        createdAt = record.createdAt
                    )
                }
                _uiState.update { CoverLetterUiState.Success(entries) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load cover letters", e)
                _uiState.update {
                    CoverLetterUiState.Error(e.message ?: "Failed to load cover letters")
                }
            }
        }
    }

    fun setEmailType(type: EmailType) {
        _selectedType.value = type
    }

    fun generateEmail(
        type: EmailType,
        inputs: Map<String, String>,
        onSuccess: (CoverLetterEntry) -> Unit
    ) {
        viewModelScope.launch {
            _generationState.update { GenerationState.Generating }
            try {

                val generated = repository.generateEmail(
                    type = type,
                    inputs = inputs
                )


                val companyName = inputs["companyName"] ?: ""
                val savedJobTitle = when (type) {
                    EmailType.APPLICATION -> inputs["jobTitle"] ?: ""
                    EmailType.PROSPECTING -> inputs["targetRole"] ?: "Networking"
                    EmailType.REFERRAL -> "Referral Request"
                    EmailType.THANK_YOU -> inputs["jobTitle"] ?: "Interview Follow-up"
                    else -> ""
                }
                val savedJobDescription = when (type) {
                    EmailType.APPLICATION -> inputs["jobDescription"] ?: ""
                    EmailType.PROSPECTING -> inputs["context"] ?: ""
                    EmailType.REFERRAL -> inputs["relationship"] ?: ""
                    EmailType.THANK_YOU -> inputs["topic"] ?: ""
                    else -> ""
                }


                val saved = repository.saveCoverLetter(
                    type = type,
                    companyName = companyName,
                    jobTitle = savedJobTitle,
                    jobDescription = savedJobDescription,
                    content = generated.content,
                    status = "draft"
                )

                val entry = CoverLetterEntry(
                    id = saved.id,
                    type = saved.type,
                    companyName = saved.companyName,
                    jobTitle = saved.jobTitle,
                    jobDescription = saved.jobDescription,
                    content = saved.content,
                    createdAt = saved.createdAt
                )


                _uiState.update { currentState ->
                    when (currentState) {
                        is CoverLetterUiState.Success -> {
                            CoverLetterUiState.Success(listOf(entry) + currentState.coverLetters)
                        }
                        else -> CoverLetterUiState.Success(listOf(entry))
                    }
                }

                _generationState.update { GenerationState.Success(generated.content) }
                onSuccess(entry)
            } catch (e: Exception) {
                if (e is com.phamnhantucode.aicareercoach.data.neon.NeonUserService.InsufficientCreditException) {
                    _showCreditDialog.value = true
                    _generationState.update { GenerationState.Idle }
                    return@launch
                }
                Log.e(TAG, "Failed to generate email", e)
                val errorMessage = when {
                    e.message?.contains("User session unavailable") == true ->
                        "Please sign in to generate emails"
                    e.message?.contains("Set your industry") == true ->
                        "Please complete your profile to generate personalized emails"
                    e.message?.contains("Gemini") == true ->
                        "AI generation failed. Please try again."
                    e.message?.contains("Neon") == true ->
                        "Database error. Please check your connection."
                    else -> e.message ?: "Failed to generate email"
                }
                _generationState.update { GenerationState.Error(errorMessage) }
            }
        }
    }

    fun deleteCoverLetter(entry: CoverLetterEntry) {
        viewModelScope.launch {
            try {
                repository.deleteCoverLetter(entry.id)


                _uiState.update { currentState ->
                    when (currentState) {
                        is CoverLetterUiState.Success -> {
                            CoverLetterUiState.Success(
                                currentState.coverLetters.filter { it.id != entry.id }
                            )
                        }
                        else -> currentState
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete item", e)

            }
        }
    }

    fun resetGenerationState() {
        _generationState.update { GenerationState.Idle }
    }

    fun dismissCreditDialog() {
        _showCreditDialog.value = false
    }

    fun openPurchaseScreen() {

        _showCreditDialog.value = false
    }

    companion object {
        private const val TAG = "CoverLetterViewModel"
    }
}
