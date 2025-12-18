package com.phamnhantucode.aicareercoach.ui.accountsettings

import android.net.Uri

// Profile edit form data
data class EditProfileFormData(
    val firstName: String = "",
    val lastName: String = "",
    val imageUri: String? = null, // Can be local URI or existing URL
    val industry: String? = null,
    val experienceYears: String = "", // String for text field input
    val skills: String = "", // Comma-separated list
    val bio: String? = null
) {
    // Checks for changes vs original
    fun hasChanges(original: EditProfileFormData): Boolean {
        return firstName != original.firstName ||
                lastName != original.lastName ||
                imageUri != original.imageUri ||
                industry != original.industry ||
                experienceYears != original.experienceYears ||
                skills != original.skills ||
                bio != original.bio
    }

    // Validates form data
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()

        if (firstName.isBlank()) {
            errors.add("First name is required")
        }

        if (lastName.isBlank()) {
            errors.add("Last name is required")
        }

        if (experienceYears.isNotBlank()) {
            val years = experienceYears.toIntOrNull()
            if (years == null || years < 0 || years > 70) {
                errors.add("Experience years must be a number between 0 and 70")
            }
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    // Converts to skills list
    fun getSkillsList(): List<String> {
        return skills.split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    // Gets experience years as Int
    fun getExperienceYearsInt(): Int? {
        return experienceYears.toIntOrNull()
    }
}

// Form validation result
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}

// Profile editing UI state
data class EditProfileUiState(
    val isOpen: Boolean = false,
    val formData: EditProfileFormData = EditProfileFormData(),
    val originalData: EditProfileFormData = EditProfileFormData(),
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveSuccess: Boolean = false
) {
    val hasChanges: Boolean
        get() = formData.hasChanges(originalData)
}
