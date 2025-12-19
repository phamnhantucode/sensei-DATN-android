package com.phamnhantucode.aicareercoach.data.ai

import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResponsibilityImprovementOptions
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResponsibilityImprovementResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException

// Improves resume bullets with AI
class ResponsibilityAIRepository {

    // Improves a bullet point
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
        
        val user = Clerk.user ?: throw IllegalStateException("User session unavailable")
        val authToken = NeonAuth.fetchNeonAuthToken() ?: throw IllegalStateException("Authentication unavailable")
        val authHeader = "Bearer $authToken"
        
        try {
            val neonUserId = NeonUserService.fetchNeonUserId(user.id, authHeader)
            NeonUserService.deductCredit(neonUserId, 1, "Responsibility Improvement", authToken)
        } catch (e: Exception) {
            // If it's InsufficientCreditException, let it bubble up.
            // If fetching user ID fails, wrap or rethrow.
             if (e is NeonUserService.InsufficientCreditException) throw e
             throw IOException("Failed to process credit deduction: ${e.message}", e)
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

        // Check bullet style
        val bulletInfo = detectBulletFormat(currentText)

        // Normalize suggestions based on user preference
        val normalizedSuggestions = when {
            // Bullet format
            options.formatAsBullets -> {
                val bulletChar = if (bulletInfo.hasBullets) bulletInfo.bulletChar else "•"
                rawSuggestions.map { suggestion ->
                    normalizeBullets(suggestion, bulletChar)
                }
            }
            // Plain text
            !options.formatAsBullets -> {
                rawSuggestions.map { suggestion ->
                    stripBullets(suggestion)
                }
            }
            // Default
            else -> rawSuggestions
        }

        return@withContext ResponsibilityImprovementResult(
            suggestions = normalizedSuggestions,
            originalText = currentText
        )
    }

    // Build prompt
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

        // Check bullet info
        val bulletInfo = detectBulletFormat(currentText)

        // Set usage rules
        val formattingInstructions = when {
            // Bullets
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
            // No bullets
            !options.formatAsBullets -> {
                """
                
                CRITICAL FORMATTING REQUIREMENTS:
                - ${options.getBulletFormatInstruction()}
                - Do NOT include bullet points, dashes, or any special characters at the start of lines
                - Format as clean, professional sentences
                - If multiple responsibilities, separate them with newlines but without bullets
                """
            }
            // Keep existing format
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

    // Call AI and get suggestions
    private suspend fun callOpenRouterApi(prompt: String): List<String> {
        val messages = listOf(
            OpenRouterService.Message(role = "user", content = prompt)
        )
        // Use JSON if possible, else prompt
        val rawText = OpenRouterService.chatCompletion(
            messages = messages,
            temperature = 0.7
        )

        return parseSuggestions(rawText)
    }

    // Extract 3 suggestions
    private fun parseSuggestions(rawText: String): List<String> {
        // Try JSON first
        try {
            // Remove markdown
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
            // Ignore JSON errors
        }

        // Fallback: split text
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .filter { !it.startsWith("#") && !it.startsWith("```") }
            .map { line ->
                // Clean strings
                line.replace(Regex("^[0-9]+[.)]\\s*"), "")
                    .trim('"', '\'', ' ')
            }
            .filter { it.length > 10 } // Filter out very short lines

        if (lines.size >= 3) {
            return lines.take(3)
        }

        throw IOException("Failed to parse 3 suggestions from AI response. Got: ${lines.size} suggestions")
    }

    // Check for bullets
    private fun detectBulletFormat(text: String): BulletFormatInfo {
        val lines = text.lines().filter { it.isNotBlank() }

        // Bullet patterns
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

    // Prepare bullet text
    private fun normalizeBullets(text: String, bulletChar: String): String {
        if (bulletChar.isEmpty()) return text

        val lines = text.lines()
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank() && !trimmed.startsWith(bulletChar)) {
                // Fix bullet char
                val withoutBullet = trimmed
                    .replace(Regex("^[•\\-*–—→›]\\s+"), "")
                    .replace(Regex("^[0-9]+[.)]\\s+"), "")
                "$bulletChar $withoutBullet"
            } else {
                line
            }
        }
    }

    // Remove bullets
    private fun stripBullets(text: String): String {
        val lines = text.lines()
        return lines.joinToString("\n") { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                // Remove bullets
                trimmed
                    .replace(Regex("^[•\\-*–—→›]\\s+"), "")
                    .replace(Regex("^[0-9]+[.)]\\s+"), "")
            } else {
                line
            }
        }
    }

    // Bullet info
    private data class BulletFormatInfo(
        val hasBullets: Boolean,
        val bulletChar: String,
        val bulletCount: Int
    )

    // User context
    data class UserContext(
        val industry: String = "",
        val skills: List<String> = emptyList(),
        val experienceYears: Int? = null
    )
}
