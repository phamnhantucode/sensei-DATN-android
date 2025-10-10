package com.phamnhantucode.aicareercoach.ui.resumebuilder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ResumeBuilderViewModel : ViewModel() {

    private val _resume = MutableStateFlow(Resume())
    val resume: StateFlow<Resume> = _resume.asStateFlow()
    private val _exportEvents = MutableSharedFlow<ResumeExportResult>(extraBufferCapacity = 1)
    val exportEvents: SharedFlow<ResumeExportResult> = _exportEvents.asSharedFlow()

    // Personal Info
    fun updatePersonalInfo(personalInfo: PersonalInfo) {
        _resume.update { it.copy(personalInfo = personalInfo) }
    }

    // Professional Summary
    fun updateProfessionalSummary(summary: String) {
        _resume.update { it.copy(professionalSummary = summary) }
    }

    // Work Experience
    fun addWorkExperience(experience: WorkExperience) {
        _resume.update {
            it.copy(workExperiences = it.workExperiences + experience)
        }
    }

    fun updateWorkExperience(experience: WorkExperience) {
        _resume.update {
            it.copy(
                workExperiences = it.workExperiences.map { exp ->
                    if (exp.id == experience.id) experience else exp
                }
            )
        }
    }

    fun removeWorkExperience(id: String) {
        _resume.update {
            it.copy(workExperiences = it.workExperiences.filter { exp -> exp.id != id })
        }
    }

    // Education
    fun addEducation(education: Education) {
        _resume.update {
            it.copy(education = it.education + education)
        }
    }

    fun updateEducation(education: Education) {
        _resume.update {
            it.copy(
                education = it.education.map { edu ->
                    if (edu.id == education.id) education else edu
                }
            )
        }
    }

    fun removeEducation(id: String) {
        _resume.update {
            it.copy(education = it.education.filter { edu -> edu.id != id })
        }
    }

    // Skills
    fun addSkill(skill: String) {
        if (skill.isNotBlank() && skill !in _resume.value.skills) {
            _resume.update {
                it.copy(skills = it.skills + skill.trim())
            }
        }
    }

    fun removeSkill(skill: String) {
        _resume.update {
            it.copy(skills = it.skills.filter { s -> s != skill })
        }
    }

    // Projects
    fun addProject(project: Project) {
        _resume.update {
            it.copy(projects = it.projects + project)
        }
    }

    fun updateProject(project: Project) {
        _resume.update {
            it.copy(
                projects = it.projects.map { proj ->
                    if (proj.id == project.id) project else proj
                }
            )
        }
    }

    fun removeProject(id: String) {
        _resume.update {
            it.copy(projects = it.projects.filter { proj -> proj.id != id })
        }
    }

    // Certifications
    fun addCertification(certification: Certification) {
        _resume.update {
            it.copy(certifications = it.certifications + certification)
        }
    }

    fun updateCertification(certification: Certification) {
        _resume.update {
            it.copy(
                certifications = it.certifications.map { cert ->
                    if (cert.id == certification.id) certification else cert
                }
            )
        }
    }

    fun removeCertification(id: String) {
        _resume.update {
            it.copy(certifications = it.certifications.filter { cert -> cert.id != id })
        }
    }

    // Languages
    fun addLanguage(language: Language) {
        _resume.update {
            it.copy(languages = it.languages + language)
        }
    }

    fun updateLanguage(language: Language) {
        _resume.update {
            it.copy(
                languages = it.languages.map { lang ->
                    if (lang.id == language.id) language else lang
                }
            )
        }
    }

    fun removeLanguage(id: String) {
        _resume.update {
            it.copy(languages = it.languages.filter { lang -> lang.id != id })
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
