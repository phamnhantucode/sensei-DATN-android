package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models

/**
 * Options for AI-powered responsibility improvement
 * @param makeProfessional Enhance language to be more formal and business-appropriate
 * @param addMetrics Suggest quantifiable achievements (e.g., "increased by 20%")
 * @param makeConcise Shorten the text while keeping key points
 * @param makeDetailed Expand with more context and specific actions
 */
data class ResponsibilityImprovementOptions(
    val makeProfessional: Boolean = false,
    val addMetrics: Boolean = false,
    val makeConcise: Boolean = false,
    val makeDetailed: Boolean = false,
    val formatAsBullets: Boolean = true
) {
    fun hasAnySelected(): Boolean = makeProfessional || addMetrics || makeConcise || makeDetailed

    fun toPromptInstructions(): List<String> {
        val instructions = mutableListOf<String>()
        if (makeProfessional) instructions.add("Use professional and business-appropriate language with strong action verbs")
        if (addMetrics) instructions.add("Include specific metrics, numbers, or quantifiable achievements where possible")
        if (makeConcise) instructions.add("Make it concise and impactful (1-2 lines maximum)")
        if (makeDetailed) instructions.add("Expand with more context, specific actions, and detailed accomplishments")
        return instructions
    }

    fun getBulletFormatInstruction(): String {
        return if (formatAsBullets) {
            "Format the output as bullet points using the '•' character at the start of each line"
        } else {
            "Format the output as plain text without bullet points or special characters at the start"
        }
    }
}

/**
 * Result of AI improvement containing multiple suggestions
 * @param suggestions List of 3 improved responsibility variations
 * @param originalText The original responsibility text
 */
data class ResponsibilityImprovementResult(
    val suggestions: List<String>,
    val originalText: String
)

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
