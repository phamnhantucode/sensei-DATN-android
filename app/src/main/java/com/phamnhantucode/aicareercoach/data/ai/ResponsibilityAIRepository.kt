package com.phamnhantucode.aicareercoach.data.ai

import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResponsibilityImprovementOptions
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResponsibilityImprovementResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException

/**
 * Repository for AI-powered improvement of resume responsibility bullet points using OpenRouter AI.
 */
class ResponsibilityAIRepository {

    /**
     * Improves a responsibility bullet point using AI
     * @param currentText The current responsibility text
     * @param jobTitle The job title for context
     * @param company The company name for context
     * @param options The improvement options selected by the user
     * @param userContext Optional user profile information
     * @param otherResponsibilities Other responsibilities in the same job to avoid duplication
     * @return ResponsibilityImprovementResult containing 3 improved variations
     */
    suspend fun improveResponsibility(
        currentText: String,
        jobTitle: String,
        company: String,
        options: ResponsibilityImprovementOptions,
        userContext: UserContext? = null,
        otherResponsibilities: List<String> = emptyList()
    ): ResponsibilityImprovementResult = withContext(Dispatchers.IO) {
        if (currentText.isBlank()) {
            throw IllegalArgumentException("Responsibility text cannot be empty")
        }

        if (!options.hasAnySelected()) {
            throw IllegalArgumentException("At least one improvement option must be selected")
        }

        val prompt = buildPrompt(
            currentText = currentText,
            jobTitle = jobTitle,
            company = company,
            options = options,
            userContext = userContext,
            otherResponsibilities = otherResponsibilities
        )

        val rawSuggestions = callOpenRouterApi(prompt)

        // Detect bullet format from original text
        val bulletInfo = detectBulletFormat(currentText)

        // Normalize suggestions based on user preference
        val normalizedSuggestions = when {
            // User wants bullet format
            options.formatAsBullets -> {
                val bulletChar = if (bulletInfo.hasBullets) bulletInfo.bulletChar else "•"
                rawSuggestions.map { suggestion ->
                    normalizeBullets(suggestion, bulletChar)
                }
            }
            // User wants plain text (strip bullets if AI added them)
            !options.formatAsBullets -> {
                rawSuggestions.map { suggestion ->
                    stripBullets(suggestion)
                }
            }
            // Default behavior
            else -> rawSuggestions
        }

        return@withContext ResponsibilityImprovementResult(
            suggestions = normalizedSuggestions,
            originalText = currentText
        )
    }

    /**
     * Builds the AI prompt for responsibility improvement
     */
    private fun buildPrompt(
        currentText: String,
        jobTitle: String,
        company: String,
        options: ResponsibilityImprovementOptions,
        userContext: UserContext?,
        otherResponsibilities: List<String>
    ): String {
        val instructions = options.toPromptInstructions()

        val contextSection = buildString {
            if (userContext != null) {
                if (userContext.industry.isNotBlank()) {
                    append("Industry: ${userContext.industry}\n")
                }
                if (userContext.skills.isNotEmpty()) {
                    append("Relevant skills: ${userContext.skills.joinToString(", ")}\n")
                }
                if (userContext.experienceYears != null) {
                    append("Years of experience: ${userContext.experienceYears}\n")
                }
            }
        }.trim()

        val otherResponsibilitiesSection = if (otherResponsibilities.isNotEmpty()) {
            """

            Other responsibilities for this role (avoid duplication):
            ${otherResponsibilities.joinToString("\n") { "- $it" }}
            """.trimIndent()
        } else ""

        // Detect bullet format from input
        val bulletInfo = detectBulletFormat(currentText)

        // Determine formatting based on user option and input
        val formattingInstructions = when {
            // User explicitly wants bullet format
            options.formatAsBullets -> {
                val bulletChar = if (bulletInfo.hasBullets) bulletInfo.bulletChar else "•"
                """

                CRITICAL FORMATTING REQUIREMENTS:
                - ${options.getBulletFormatInstruction()}
                - Each bullet point should start with "$bulletChar" followed by a space
                - If input has multiple bullets on separate lines, keep them on separate lines in the output
                - Preserve the bullet structure: each line should be a separate bullet point
                """
            }
            // User wants plain text (no bullets)
            !options.formatAsBullets -> {
                """

                CRITICAL FORMATTING REQUIREMENTS:
                - ${options.getBulletFormatInstruction()}
                - Do NOT include bullet points, dashes, or any special characters at the start of lines
                - Format as clean, professional sentences
                - If multiple responsibilities, separate them with newlines but without bullets
                """
            }
            // Fallback (preserve input format)
            else -> {
                if (bulletInfo.hasBullets) {
                    """

                    FORMATTING: The input contains ${bulletInfo.bulletCount} bullet point(s) using "${bulletInfo.bulletChar}". Maintain this format.
                    """
                } else ""
            }
        }

        return """
            You are a professional resume writer helping improve a job responsibility bullet point.

            Job Context:
            Position: $jobTitle
            Company: $company
            ${if (contextSection.isNotBlank()) "$contextSection" else ""}

            Current Responsibility:
            "$currentText"
            $otherResponsibilitiesSection

            Improvement Requirements:
            ${instructions.joinToString("\n") { "- $it" }}

            Additional Guidelines:
            - Start with a strong action verb (e.g., Led, Developed, Implemented, Architected, Optimized)
            - Focus on impact and achievements, not just tasks
            - Be specific and concrete
            - Use present tense for current roles, past tense for previous roles
            - Avoid buzzwords and clichés
            - Each variation should be meaningfully different
            $formattingInstructions

            Generate exactly 3 distinct improved versions of this responsibility.
            Return ONLY a valid JSON array with 3 strings, no additional text or formatting:
            ["variation 1", "variation 2", "variation 3"]

            Important: Return ONLY the JSON array, nothing else. Do not include markdown code blocks or explanations.
        """.trimIndent()
    }

    /**
     * Calls the OpenRouter Service and extracts the suggestions
     */
    private suspend fun callOpenRouterApi(prompt: String): List<String> {
        val messages = listOf(
            OpenRouterService.Message(role = "user", content = prompt)
        )
        // Request JSON object for easier parsing if model supports it, but since we ask for array, we rely on prompt instructions primarily
        // We can pass null or specific format if we switch to models that support strict schema
        val rawText = OpenRouterService.chatCompletion(
            messages = messages,
            temperature = 0.7
        )

        return parseSuggestions(rawText)
    }

    /**
     * Parses the AI response to extract the 3 suggestions
     * Handles both JSON array format and fallback text parsing
     */
    private fun parseSuggestions(rawText: String): List<String> {
        // Try to parse as JSON array first
        try {
            // Clean up potential markdown code blocks
            val cleaned = rawText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = JSONArray(cleaned)
            val suggestions = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val suggestion = jsonArray.getString(i).trim()
                if (suggestion.isNotBlank()) {
                    suggestions.add(suggestion)
                }
            }

            if (suggestions.size >= 3) {
                return suggestions.take(3)
            }
        } catch (e: Exception) {
            // If JSON parsing fails, try to extract suggestions from text
        }

        // Fallback: try to split by numbered list or newlines
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { !it.startsWith("#") && !it.startsWith("```") }
            .map { line ->
                // Remove leading numbers or quotes, but preserve bullets
                line.replace(Regex("^[0-9]+[.)]\\s*"), "")
                    .trim('"', '\'', ' ')
            }
            .filter { it.length > 10 } // Filter out very short lines

        if (lines.size >= 3) {
            return lines.take(3)
        }

        throw IOException("Failed to parse 3 suggestions from AI response. Got: ${lines.size} suggestions")
    }

    /**
     * Detects bullet format from input text
     */
    private fun detectBulletFormat(text: String): BulletFormatInfo {
        val lines = text.lines().filter { it.isNotBlank() }

        // Common bullet patterns
        val bulletPatterns = listOf(
            "•" to Regex("^\\s*•\\s+"),
            "-" to Regex("^\\s*-\\s+"),
            "*" to Regex("^\\s*\\*\\s+"),
            "–" to Regex("^\\s*–\\s+"),  // en dash
            "—" to Regex("^\\s*—\\s+"),  // em dash
            "→" to Regex("^\\s*→\\s+"),
            "›" to Regex("^\\s*›\\s+"),
        )

        // Check each pattern
        for ((bullet, pattern) in bulletPatterns) {
            val matchingLines = lines.count { pattern.containsMatchIn(it) }
            if (matchingLines > 0) {
                return BulletFormatInfo(
                    hasBullets = true,
                    bulletChar = bullet,
                    bulletCount = matchingLines
                )
            }
        }

        // Check for numbered lists
        val numberedPattern = Regex("^\\s*[0-9]+[.)]]\\s+")
        val numberedCount = lines.count { numberedPattern.containsMatchIn(it) }
        if (numberedCount > 0) {
            return BulletFormatInfo(
                hasBullets = true,
                bulletChar = "1.",
                bulletCount = numberedCount
            )
        }

        return BulletFormatInfo(hasBullets = false, bulletChar = "", bulletCount = 0)
    }

    /**
     * Normalizes bullet formatting in text
     */
    private fun normalizeBullets(text: String, bulletChar: String): String {
        if (bulletChar.isEmpty()) return text

        val lines = text.lines()
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith(bulletChar)) {
                // Remove any existing bullet and add the correct one
                val withoutBullet = trimmed
                    .replace(Regex("^[•\\-*–—→›]\\s+"), "")
                    .replace(Regex("^[0-9]+[.)]\\s+"), "")
                "$bulletChar $withoutBullet"
            } else {
                line
            }
        }
    }

    /**
     * Strips all bullet formatting from text
     */
    private fun stripBullets(text: String): String {
        val lines = text.lines()
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                // Remove all bullet types
                trimmed
                    .replace(Regex("^[•\\-*–—→›]\\s+"), "")
                    .replace(Regex("^[0-9]+[.)]\\s+"), "")
            } else {
                line
            }
        }
    }

    /**
     * Information about bullet formatting in text
     */
    private data class BulletFormatInfo(
        val hasBullets: Boolean,
        val bulletChar: String,
        val bulletCount: Int
    )

    /**
     * User context for improving responsibilities
     */
    data class UserContext(
        val industry: String = "",
        val skills: List<String> = emptyList(),
        val experienceYears: Int? = null
    )
}
