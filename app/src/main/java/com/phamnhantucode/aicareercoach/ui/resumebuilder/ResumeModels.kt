package com.phamnhantucode.aicareercoach.ui.resumebuilder

import java.time.LocalDate

data class Resume(
    val personalInfo: PersonalInfo = PersonalInfo(),
    val professionalSummary: String = "",
    val workExperiences: List<WorkExperience> = emptyList(),
    val education: List<Education> = emptyList(),
    val skills: List<String> = emptyList(),
    val projects: List<Project> = emptyList(),
    val certifications: List<Certification> = emptyList(),
    val languages: List<Language> = emptyList()
)

data class PersonalInfo(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val location: String = "",
    val linkedIn: String = "",
    val portfolio: String = "",
    val github: String = ""
)

data class WorkExperience(
    val id: String = java.util.UUID.randomUUID().toString(),
    val jobTitle: String = "",
    val company: String = "",
    val location: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val isCurrentRole: Boolean = false,
    val responsibilities: List<String> = emptyList()
)

data class Education(
    val id: String = java.util.UUID.randomUUID().toString(),
    val degree: String = "",
    val institution: String = "",
    val location: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val gpa: String = "",
    val achievements: List<String> = emptyList()
)

data class Project(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val technologies: List<String> = emptyList(),
    val link: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null
)

data class Certification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val issuer: String = "",
    val issueDate: LocalDate? = null,
    val expiryDate: LocalDate? = null,
    val credentialId: String = ""
)

data class Language(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String = "",
    val proficiency: LanguageProficiency = LanguageProficiency.INTERMEDIATE
)

enum class LanguageProficiency(val displayName: String) {
    ELEMENTARY("Elementary"),
    INTERMEDIATE("Intermediate"),
    PROFICIENT("Proficient"),
    FLUENT("Fluent"),
    NATIVE("Native")
}
