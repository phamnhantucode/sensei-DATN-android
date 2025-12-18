package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonResumeService
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MarkdownUiState {
    object Loading : MarkdownUiState()
    object Syncing : MarkdownUiState()
    object Success : MarkdownUiState()
    data class Error(val message: String) : MarkdownUiState()
}

class ResumeMarkdownViewModel(private val context: Context) : ViewModel() {

    companion object {
        private const val TAG = "ResumeMarkdownViewModel"
    }

    private val repository = ResumeRepository.getInstance(context)
    private val exporter = ResumeExporter(context)

    private val _uiState = MutableStateFlow<MarkdownUiState>(MarkdownUiState.Loading)
    val uiState: StateFlow<MarkdownUiState> = _uiState.asStateFlow()

    private val _markdown = MutableStateFlow("")
    val markdown: StateFlow<String> = _markdown.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportEvents = MutableSharedFlow<ResumeExportResult>(extraBufferCapacity = 1)
    val exportEvents: SharedFlow<ResumeExportResult> = _exportEvents.asSharedFlow()

    private val _syncEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val syncEvents: SharedFlow<String> = _syncEvents.asSharedFlow()

    private var currentResume: Resume? = null

    init {
        loadAndSyncResume()
    }

    private fun loadAndSyncResume() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = MarkdownUiState.Loading

            try {

                val result = repository.getLatestResume()
                var resume = result.getOrNull()

                // Auto-create resume for new users if none exists
                if (resume == null) {
                    Log.d(TAG, "No resume found, creating default resume for new user")
                    resume = Resume() // Create empty resume with default values


                    repository.saveResume(resume, syncToRemote = true)
                    Log.d(TAG, "Default resume created and saved with ID: ${resume.id}")
                }

                currentResume = resume


                val markdownContent = ResumeFormatter.toMarkdown(resume)
                _markdown.value = markdownContent

                // Show success state first
                _uiState.value = MarkdownUiState.Syncing


                syncToNeon(resume)

            } catch (e: Exception) {
                Log.e(TAG, "Error loading resume", e)
                _uiState.value = MarkdownUiState.Error("Failed to load resume: ${e.message}")
            }
        }
    }

    private suspend fun syncToNeon(resume: Resume) {
        try {
            val userId = Clerk.user?.id
            if (userId == null) {
                Log.w(TAG, "User not logged in, skipping cloud sync")
                _uiState.value = MarkdownUiState.Success
                _syncEvents.emit("Signed out - local only")
                return
            }

            Log.d(TAG, "Syncing resume to Neon database...")
            val authToken = NeonAuth.fetchNeonAuthToken()
            val result = NeonResumeService.saveResume(resume, userId, authToken)

            if (result.isSuccess) {
                Log.d(TAG, "Resume synced successfully")
                _uiState.value = MarkdownUiState.Success
                _syncEvents.emit("Synced to cloud")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error"
                Log.w(TAG, "Failed to sync resume: $error")
                _uiState.value = MarkdownUiState.Success // Still show content
                _syncEvents.emit("Sync failed: $error")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Neon", e)
            _uiState.value = MarkdownUiState.Success // Still show content
            _syncEvents.emit("Sync error: ${e.message}")
        }
    }

    fun retry() {
        loadAndSyncResume()
    }

    fun exportMarkdown() {
        val resume = currentResume ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val result = exporter.export(ResumeExportFormat.MARKDOWN, resume)
                _exportEvents.emit(result)
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun exportPdf() {
        val resume = currentResume ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _isExporting.value = true
            try {
                val result = exporter.export(ResumeExportFormat.PDF, resume)
                _exportEvents.emit(result)
            } finally {
                _isExporting.value = false
            }
        }
    }
}
