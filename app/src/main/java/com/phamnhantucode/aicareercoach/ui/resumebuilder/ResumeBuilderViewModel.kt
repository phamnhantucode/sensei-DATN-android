package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
import com.phamnhantucode.aicareercoach.data.resume.ResumeSyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResumeBuilderViewModel(private val context: Context, private val resumeId: String? = null) : ViewModel() {

    companion object {
        private const val TAG = "ResumeBuilderViewModel"
    }

    private val repository = ResumeRepository.getInstance(context)

    private val _resume = MutableStateFlow(Resume())
    val resume: StateFlow<Resume> = _resume.asStateFlow()

    private val _exportEvents = MutableSharedFlow<ResumeExportResult>(extraBufferCapacity = 1)
    val exportEvents: SharedFlow<ResumeExportResult> = _exportEvents.asSharedFlow()

    private val _saveEvents = MutableSharedFlow<SaveResult>(extraBufferCapacity = 1)
    val saveEvents: SharedFlow<SaveResult> = _saveEvents.asSharedFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var autoSaveJob: Job? = null
    private var isAutoSaveEnabled = false
    private var isInitialLoadComplete = false
    private var cachedUserId: String? = null

    init {
        // Load resume based on resumeId or create new
        viewModelScope.launch(Dispatchers.IO) {
            // Cache user ID for WorkManager sync
            cachedUserId = repository.getCurrentUserId()
            
            if (resumeId != null) {
                // Load existing resume
                loadResumeById(resumeId)
            } else {
                // Create new resume, auto-fill from user profile
                _resume.value = Resume()
                autofillFromUserProfile()
            }
            
            // Enable auto-save after initial load completes
            isInitialLoadComplete = true
            isAutoSaveEnabled = true
        }
    }

    // ========== Save/Load Functions ==========

    /**
     * Loads a resume by ID
     */
    private suspend fun loadResumeById(id: String) {
        _isLoading.value = true
        try {
            val result = repository.getResume(id)
            result.getOrNull()?.let { loadedResume ->
                _resume.value = loadedResume
                Log.d(TAG, "Loaded resume $id")
            } ?: run {
                // Resume not found, create new one
                _resume.value = Resume()
                Log.w(TAG, "Resume $id not found, creating new")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading resume $id", e)
            _resume.value = Resume()
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Loads a specific resume by ID
     */
    fun loadResume(resumeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = repository.getResume(resumeId)
                result.getOrNull()?.let { loadedResume ->
                    _resume.value = loadedResume
                    _saveEvents.emit(SaveResult.Success("Resume loaded successfully"))
                }
            } catch (e: Exception) {
                _saveEvents.emit(SaveResult.Error("Failed to load resume: ${e.message}"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Manually saves the current resume
     */
    fun saveResume(showToast: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSaving.value = true
            try {
                val currentResume = _resume.value
                val result = repository.saveResume(currentResume, syncToRemote = true)

                if (result.isSuccess && showToast) {
                    _saveEvents.emit(SaveResult.Success("Resume saved successfully"))
                } else if (result.isFailure) {
                    _saveEvents.emit(SaveResult.Error("Failed to save resume"))
                }
            } catch (e: Exception) {
                _saveEvents.emit(SaveResult.Error("Error saving resume: ${e.message}"))
            } finally {
                _isSaving.value = false
            }
        }
    }

    /**
     * Triggers auto-save with debounce (enqueues WorkManager sync 1000ms after last change)
     * Uses WorkManager for reliable background sync that survives process death.
     */
    private fun triggerAutoSave() {
        if (!isAutoSaveEnabled || !isInitialLoadComplete) return

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(1000) // Wait 1000ms after last change - reduce concurrent save conflicts
            
            val userId = cachedUserId
            if (userId == null) {
                Log.w(TAG, "Cannot auto-save: user ID not available")
                return@launch
            }
            
            val currentResume = _resume.value
            val isUpdate = resumeId != null
            
            // Enqueue reliable background sync via WorkManager
            ResumeSyncWorker.enqueue(
                context = context,
                resume = currentResume,
                userId = userId,
                isUpdate = isUpdate
            )
        }
    }

    /**
     * Enables or disables auto-save
     */
    fun setAutoSaveEnabled(enabled: Boolean) {
        isAutoSaveEnabled = enabled
        if (!enabled) {
            autoSaveJob?.cancel()
        }
    }

    /**
     * Creates a new blank resume
     */
    fun createNewResume() {
        _resume.value = Resume()
        triggerAutoSave()
    }

    // ========== Personal Info ==========

    fun updatePersonalInfo(personalInfo: PersonalInfo) {
        _resume.update { it.copy(personalInfo = personalInfo) }
        triggerAutoSave()
    }

    /**
     * Auto-fill personal info from Clerk user and Neon user profile
     */
    suspend fun autofillFromUserProfile() {
        try {
            val clerkUser = Clerk.user
            if (clerkUser == null) {
                Log.w(TAG, "Cannot autofill: user not logged in")
                return
            }

            // Get Neon user profile for extended data
            val neonUser = try {
                NeonUserService.getUser(clerkUser.id)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to get Neon user profile", e)
                null
            }

            // Build personal info from Clerk user
            val firstName = clerkUser.firstName?.takeUnless { it.isBlank() } ?: ""
            val lastName = clerkUser.lastName?.takeUnless { it.isBlank() } ?: ""
            val fullName = "$firstName $lastName".trim()

            // Get primary email
            val primaryEmailId = clerkUser.primaryEmailAddressId
            val email = primaryEmailId?.let { id ->
                clerkUser.emailAddresses.firstOrNull { it.id == id }?.emailAddress
            } ?: clerkUser.emailAddresses.firstOrNull()?.emailAddress ?: ""

            val avatar = clerkUser.imageUrl ?: ""

            val personalInfo = PersonalInfo(
                fullName = fullName.ifBlank { _resume.value.personalInfo.fullName },
                email = email.ifBlank { _resume.value.personalInfo.email },
                phone = _resume.value.personalInfo.phone, // Keep existing
                location = _resume.value.personalInfo.location, // Keep existing
                linkedIn = _resume.value.personalInfo.linkedIn, // Keep existing
                portfolio = _resume.value.personalInfo.portfolio, // Keep existing
                github = _resume.value.personalInfo.github, // Keep existing
                avatar = avatar.ifBlank { _resume.value.personalInfo.avatar }
            )

            // Update resume with autofilled data
            _resume.update {
                var updated = it.copy(personalInfo = personalInfo)

                // If Neon user has skills, pre-populate (only if current skills are empty)
                if (neonUser != null && neonUser.skills.isNotEmpty() && it.skills.isEmpty()) {
                    updated = updated.copy(skills = neonUser.skills)
                }

                // If Neon user has bio, use as professional summary (only if current summary is empty)
                if (neonUser != null && !neonUser.bio.isNullOrBlank() && it.professionalSummary.isBlank()) {
                    updated = updated.copy(professionalSummary = neonUser.bio)
                }

                updated
            }

            Log.d(TAG, "Auto-filled resume from user profile")
        } catch (e: Exception) {
            Log.e(TAG, "Error auto-filling from user profile", e)
        }
    }

    // ========== Professional Summary ==========

    fun updateProfessionalSummary(summary: String) {
        _resume.update { it.copy(professionalSummary = summary) }
        triggerAutoSave()
    }

    // ========== Work Experience ==========

    fun addWorkExperience(experience: WorkExperience) {
        _resume.update {
            it.copy(workExperiences = it.workExperiences + experience)
        }
        triggerAutoSave()
    }

    fun updateWorkExperience(experience: WorkExperience) {
        _resume.update {
            it.copy(
                workExperiences = it.workExperiences.map { exp ->
                    if (exp.id == experience.id) experience else exp
                }
            )
        }
        triggerAutoSave()
    }

    fun removeWorkExperience(id: String) {
        _resume.update {
            it.copy(workExperiences = it.workExperiences.filter { exp -> exp.id != id })
        }
        triggerAutoSave()
    }

    // ========== Education ==========

    fun addEducation(education: Education) {
        _resume.update {
            it.copy(education = it.education + education)
        }
        triggerAutoSave()
    }

    fun updateEducation(education: Education) {
        _resume.update {
            it.copy(
                education = it.education.map { edu ->
                    if (edu.id == education.id) education else edu
                }
            )
        }
        triggerAutoSave()
    }

    fun removeEducation(id: String) {
        _resume.update {
            it.copy(education = it.education.filter { edu -> edu.id != id })
        }
        triggerAutoSave()
    }

    // ========== Skills ==========

    fun addSkill(skill: String) {
        if (skill.isNotBlank() && skill !in _resume.value.skills) {
            _resume.update {
                it.copy(skills = it.skills + skill.trim())
            }
            triggerAutoSave()
        }
    }

    fun removeSkill(skill: String) {
        _resume.update {
            it.copy(skills = it.skills.filter { s -> s != skill })
        }
        triggerAutoSave()
    }

    // ========== Projects ==========

    fun addProject(project: Project) {
        _resume.update {
            it.copy(projects = it.projects + project)
        }
        triggerAutoSave()
    }

    fun updateProject(project: Project) {
        _resume.update {
            it.copy(
                projects = it.projects.map { proj ->
                    if (proj.id == project.id) project else proj
                }
            )
        }
        triggerAutoSave()
    }

    fun removeProject(id: String) {
        _resume.update {
            it.copy(projects = it.projects.filter { proj -> proj.id != id })
        }
        triggerAutoSave()
    }

    // ========== Certifications ==========

    fun addCertification(certification: Certification) {
        _resume.update {
            it.copy(certifications = it.certifications + certification)
        }
        triggerAutoSave()
    }

    fun updateCertification(certification: Certification) {
        _resume.update {
            it.copy(
                certifications = it.certifications.map { cert ->
                    if (cert.id == certification.id) certification else cert
                }
            )
        }
        triggerAutoSave()
    }

    fun removeCertification(id: String) {
        _resume.update {
            it.copy(certifications = it.certifications.filter { cert -> cert.id != id })
        }
        triggerAutoSave()
    }

    // ========== Languages ==========

    fun addLanguage(language: Language) {
        _resume.update {
            it.copy(languages = it.languages + language)
        }
        triggerAutoSave()
    }

    fun updateLanguage(language: Language) {
        _resume.update {
            it.copy(
                languages = it.languages.map { lang ->
                    if (lang.id == language.id) language else lang
                }
            )
        }
        triggerAutoSave()
    }

    fun removeLanguage(id: String) {
        _resume.update {
            it.copy(languages = it.languages.filter { lang -> lang.id != id })
        }
        triggerAutoSave()
    }

    // ========== Theme Management ==========

    fun updateTheme(theme: ResumeTheme) {
        _resume.update { it.copy(theme = theme) }
        triggerAutoSave()
    }

    fun updateColorScheme(colorScheme: ColorScheme) {
        _resume.update { it.copy(theme = it.theme.copy(colorScheme = colorScheme)) }
        triggerAutoSave()
    }

    fun updateTypography(typography: TypographyScheme) {
        _resume.update { it.copy(theme = it.theme.copy(typography = typography)) }
        triggerAutoSave()
    }

    fun updateLayout(layout: LayoutConfig) {
        _resume.update { it.copy(theme = it.theme.copy(layout = layout)) }
        triggerAutoSave()
    }

    // ========== Section Configuration ==========

    fun toggleSectionVisibility(sectionType: ResumeSectionType) {
        _resume.update { currentResume ->
            val updatedConfig = currentResume.sectionConfig.map { config ->
                if (config.sectionType == sectionType && !sectionType.isRequired) {
                    config.copy(isVisible = !config.isVisible)
                } else {
                    config
                }
            }
            currentResume.copy(sectionConfig = updatedConfig)
        }
        triggerAutoSave()
    }

    fun reorderSections(newOrder: List<SectionConfig>) {
        _resume.update { it.copy(sectionConfig = newOrder) }
        triggerAutoSave()
    }

    fun moveSectionUp(sectionType: ResumeSectionType) {
        val currentConfig = _resume.value.sectionConfig
        val index = currentConfig.indexOfFirst { it.sectionType == sectionType }
        if (index > 0) {
            val newConfig = currentConfig.toMutableList()
            val temp = newConfig[index]
            newConfig[index] = newConfig[index - 1].copy(order = temp.order)
            newConfig[index - 1] = temp.copy(order = newConfig[index - 1].order)
            newConfig.sortBy { it.order }
            reorderSections(newConfig)
        }
    }

    fun moveSectionDown(sectionType: ResumeSectionType) {
        val currentConfig = _resume.value.sectionConfig
        val index = currentConfig.indexOfFirst { it.sectionType == sectionType }
        if (index < currentConfig.size - 1 && index >= 0) {
            val newConfig = currentConfig.toMutableList()
            val temp = newConfig[index]
            newConfig[index] = newConfig[index + 1].copy(order = temp.order)
            newConfig[index + 1] = temp.copy(order = newConfig[index + 1].order)
            newConfig.sortBy { it.order }
            reorderSections(newConfig)
        }
    }

    fun exportResume(context: Context, format: ResumeExportFormat) {
        val resumeSnapshot = _resume.value
        viewModelScope.launch(Dispatchers.IO) {
            val exporter = ResumeExporter(context)
            val result = exporter.export(format, resumeSnapshot)
            _exportEvents.emit(result)
        }
    }
}

sealed class SaveResult {
    data class Success(val message: String) : SaveResult()
    data class Error(val message: String) : SaveResult()
}
