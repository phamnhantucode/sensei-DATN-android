package com.phamnhantucode.aicareercoach.data.ai

import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResponsibilityImprovementOptions
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.ResponsibilityImprovementResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Repository for AI-powered improvement of resume responsibility bullet points using Gemini AI.
 */
class ResponsibilityAIRepository(
    private val client: OkHttpClient = OkHttpClient()
) {

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

        val rawSuggestions = callGeminiApi(prompt)

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
     * Calls the Gemini API and extracts the suggestions
     */
    private suspend fun callGeminiApi(prompt: String): List<String> {
        val requestUrl = HttpUrl.Builder()
            .scheme("https")
            .host(GEMINI_API_HOST)
            .addPathSegments("v1beta/models/$GEMINI_MODEL_NAME:generateContent")
            .build()

        val payload = JSONObject().apply {
            put(
                "contents",
                JSONArray().apply {
                    put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().apply {
                                    put(JSONObject().apply { put("text", prompt) })
                                }
                            )
                        }
                    )
                }
            )
            // Add generation config for more consistent JSON output
            put(
                "generationConfig",
                JSONObject().apply {
                    put("temperature", 0.7)
                    put("topP", 0.8)
                    put("topK", 40)
                }
            )
        }

        val request = Request.Builder()
            .url(requestUrl)
            .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
            .addHeader("Content-Type", JSON_MEDIA_TYPE)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
            .build()

        val timeoutClient = client.newBuilder()
            .callTimeout(90, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val rawText = timeoutClient.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
                ?: throw IOException("Gemini returned an empty response.")
            if (!response.isSuccessful) {
                throw IOException("Gemini request failed (${response.code}): $bodyString")
            }
            extractGeminiText(JSONObject(bodyString))
                ?: throw IOException("Gemini response did not include text content.")
        }

        return parseSuggestions(rawText)
    }

    /**
     * Extracts the text content from Gemini API response
     */
    private fun extractGeminiText(response: JSONObject): String? {
        val candidates = response.optJSONArray("candidates") ?: return null
        for (i in 0 until candidates.length()) {
            val candidate = candidates.optJSONObject(i) ?: continue
            val content = candidate.optJSONObject("content") ?: continue
            val parts = content.optJSONArray("parts") ?: continue
            val collected = buildString {
                for (j in 0 until parts.length()) {
                    val part = parts.optJSONObject(j) ?: continue
                    val text = part.optString("text")
                    if (!text.isNullOrBlank()) append(text)
                }
            }
            if (collected.isNotBlank()) return collected
        }
        return null
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

    companion object {
        private const val GEMINI_API_HOST = "generativelanguage.googleapis.com"
        private const val GEMINI_MODEL_NAME = "gemini-2.5-flash"
        private const val JSON_MEDIA_TYPE = "application/json"
    }
}
