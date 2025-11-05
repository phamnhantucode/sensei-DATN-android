package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phamnhantucode.aicareercoach.data.resume.ResumeRepository
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

class ResumeBuilderViewModel(context: Context) : ViewModel() {

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
    private var isAutoSaveEnabled = true

    init {
        // Start with a blank resume
        // Users can manually load a saved resume if needed
    }

    // ========== Save/Load Functions ==========

    /**
     * Loads the latest resume for the current user
     */
    fun loadLatestResume() {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            try {
                val result = repository.getLatestResume()
                result.getOrNull()?.let { loadedResume ->
                    _resume.value = loadedResume
                }
            } catch (e: Exception) {
                // Silently fail, keep default empty resume
            } finally {
                _isLoading.value = false
            }
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
     * Triggers auto-save with debounce (saves 3 seconds after last change)
     */
    private fun triggerAutoSave() {
        if (!isAutoSaveEnabled) return

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(3000) // Wait 3 seconds after last change
            saveResume(showToast = false) // Auto-save silently
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
