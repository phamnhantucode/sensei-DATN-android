package com.phamnhantucode.aicareercoach.data.audio

import android.util.Base64
import android.util.Log
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * GeminiAudioService handles audio-related operations with Gemini AI,
 * including speech-to-text transcription and generating responses based on audio input.
 */
class GeminiAudioService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiAudioService"
        private const val GEMINI_MODEL = "gemini-2.5-flash"
        private const val GEMINI_HOST = "generativelanguage.googleapis.com"
    }

    /**
     * Transcribes audio file to text using Gemini's multimodal API.
     * @param audioFile The audio file to transcribe (must be .m4a, .mp3, .wav, etc.)
     * @return Transcribed text, or null if transcription failed
     */
    suspend fun transcribeAudio(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (!audioFile.exists() || audioFile.length() == 0L) {
                return@withContext Result.failure(Exception("Audio file is empty or doesn't exist"))
            }

            // Minimum file size check (very short recordings may not have enough data)
            val minFileSizeBytes = 5000L // ~5KB minimum for meaningful audio
            if (audioFile.length() < minFileSizeBytes) {
                Log.w(
                    TAG,
                    "Audio file too small: ${audioFile.length()} bytes (minimum: $minFileSizeBytes)"
                )
                return@withContext Result.failure(Exception("Recording too short. Please speak longer and try again."))
            }

            Log.d(
                TAG,
                "Transcribing audio file: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes"
            )

            // Read and encode audio file to base64
            val audioBytes = audioFile.readBytes()
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            // Determine MIME type based on file extension
            val mimeType = when (audioFile.extension.lowercase()) {
                "m4a" -> "audio/mp4"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "aac" -> "audio/aac"
                else -> "audio/mp4" // Default to mp4
            }

            // Build request payload with improved transcription prompt
            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            // Add audio data
                            put(JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", mimeType)
                                    put("data", base64Audio)
                                })
                            })
                            // Add improved prompt for transcription
                            put(JSONObject().apply {
                                put(
                                    "text",
                                    """Transcribe the speech in this audio recording exactly as spoken.
IMPORTANT RULES:
- Output ONLY the exact words spoken in the audio, nothing else
- If the audio contains no speech or is unclear, respond with exactly: [NO_SPEECH_DETECTED]
- Do NOT make up or hallucinate any text
- Do NOT add any commentary, explanations, or assumptions
- Preserve the original language and wording exactly as spoken"""
                                )
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.0) // Use 0 temperature for deterministic transcription
                    put("maxOutputTokens", 1024)
                })
            }

            // Build request URL
            val requestUrl = HttpUrl.Builder()
                .scheme("https")
                .host(GEMINI_HOST)
                .addPathSegments("v1beta/models/$GEMINI_MODEL:generateContent")
                .build()

            // Create HTTP request
            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(payload.toString().toRequestBody())
                .build()

            // Execute request
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Transcription failed: ${response.code}, body: $responseBody")
                return@withContext Result.failure(Exception("Failed to transcribe audio: ${response.code}"))
            }

            // Parse response
            val jsonResponse = JSONObject(responseBody)
            val transcribedText = extractGeminiText(jsonResponse)

            if (transcribedText.isNullOrBlank()) {
                Log.e(TAG, "No text extracted from response: $responseBody")
                return@withContext Result.failure(Exception("No transcription found in response"))
            }

            // Check for no speech detected marker
            val trimmedText = transcribedText.trim()
            if (trimmedText.equals("[NO_SPEECH_DETECTED]", ignoreCase = true) ||
                trimmedText.contains("no speech", ignoreCase = true) ||
                trimmedText.contains("no audio", ignoreCase = true) ||
                trimmedText.contains("cannot hear", ignoreCase = true) ||
                trimmedText.contains("unable to transcribe", ignoreCase = true)
            ) {
                Log.w(TAG, "No speech detected in audio")
                return@withContext Result.failure(Exception("No speech detected. Please speak clearly into the microphone and try again."))
            }

            Log.d(TAG, "Transcription successful: $trimmedText")
            Result.success(trimmedText)

        } catch (e: Exception) {
            Log.e(TAG, "Error transcribing audio", e)
            Result.failure(e)
        }
    }

    /**
     * Generates AI feedback for an interview answer using Gemini.
     * @param question The interview question
     * @param userAnswer The user's transcribed answer
     * @param category The question category (TECHNICAL, BEHAVIORAL, SITUATIONAL)
     * @return AI-generated feedback with rating
     */
    suspend fun generateFeedback(
        question: String,
        userAnswer: String,
        category: String,
    ): Result<FeedbackResult> = withContext(Dispatchers.IO) {
        try {
            // Validate API key is configured
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                Log.e(TAG, "GEMINI_API_KEY is not configured in local.properties")
                return@withContext Result.failure(Exception("Gemini API key not configured"))
            }

            val prompt = buildFeedbackPrompt(question, userAnswer, category)

            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 2048)
                })
            }

            val requestUrl = HttpUrl.Builder()
                .scheme("https")
                .host(GEMINI_HOST)
                .addPathSegments("v1beta/models/$GEMINI_MODEL:generateContent")
                .build()

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(payload.toString().toRequestBody())
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                val errorDetails = buildString {
                    append("Gemini API feedback request failed: ")
                    append("HTTP ${response.code} ${response.message}")
                    if (responseBody != null) {
                        append(", Response: ${responseBody.take(500)}")
                    } else {
                        append(", Response body is null")
                    }
                }
                Log.e(TAG, errorDetails)
                return@withContext Result.failure(Exception("Failed to generate feedback: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val feedbackText = extractGeminiText(jsonResponse)

            if (feedbackText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No feedback found in response"))
            }

            // Parse feedback to extract rating and comments
            val feedbackResult = parseFeedbackResponse(feedbackText)
            Log.d(
                TAG,
                "Feedback generated: rating=${feedbackResult.rating}, feedback=${feedbackResult.feedback}"
            )

            Result.success(feedbackResult)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating feedback", e)
            Result.failure(e)
        }
    }

    /**
     * Generates the next interview question based on context.
     * @param category Question category
     * @param previousQuestions List of previously asked questions
     * @param userProfile Optional user profile info for personalization
     * @return Generated question with correct answer
     */
    suspend fun generateNextQuestion(
        category: String,
        previousQuestions: List<String>,
        userProfile: String? = null,
    ): Result<QuestionResult> = withContext(Dispatchers.IO) {
        try {
            // Validate API key is configured
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                Log.e(TAG, "GEMINI_API_KEY is not configured in local.properties")
                return@withContext Result.failure(Exception("Gemini API key not configured"))
            }

            val prompt = buildQuestionPrompt(category, previousQuestions, userProfile)

            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.8)
                    put("maxOutputTokens", 1024)
                })
            }

            val requestUrl = HttpUrl.Builder()
                .scheme("https")
                .host(GEMINI_HOST)
                .addPathSegments("v1beta/models/$GEMINI_MODEL:generateContent")
                .build()

            val request = Request.Builder()
                .url(requestUrl)
                .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .post(payload.toString().toRequestBody())
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                val errorDetails = buildString {
                    append("Gemini API request failed: ")
                    append("HTTP ${response.code} ${response.message}")
                    if (responseBody != null) {
                        append(", Response: ${responseBody.take(500)}")
                    } else {
                        append(", Response body is null")
                    }
                }
                Log.e(TAG, errorDetails)
                return@withContext Result.failure(Exception("Failed to generate question: HTTP ${response.code}"))
            }

            val jsonResponse = JSONObject(responseBody)
            val questionText = extractGeminiText(jsonResponse)

            if (questionText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No question found in response"))
            }

            val questionResult = parseQuestionResponse(questionText)
            Log.d(TAG, "Question generated: ${questionResult.question}")

            Result.success(questionResult)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating question", e)
            Result.failure(e)
        }
    }

    private fun extractGeminiText(response: JSONObject): String? {
        return try {
            val candidates = response.optJSONArray("candidates") ?: return null
            val candidate = candidates.optJSONObject(0) ?: return null
            val content = candidate.optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            val part = parts.optJSONObject(0) ?: return null
            part.optString("text")
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting text from response", e)
            null
        }
    }

    private fun buildFeedbackPrompt(
        question: String,
        userAnswer: String,
        category: String,
    ): String {
        return """
            You are an expert interview coach. Evaluate this candidate's interview answer and provide constructive feedback.

            Question: $question
            Category: $category
            Candidate's Answer: $userAnswer

            Please provide:
            1. A rating from 1-10 (where 10 is excellent)
            2. Specific, actionable feedback on the answer
            3. Highlight strengths using **bold** for key points
            4. Suggest improvements

            Format your response as:
            RATING: [number]
            FEEDBACK: [your detailed feedback - use **bold** for important points and key terms]
        """.trimIndent()
    }

    private fun buildQuestionPrompt(
        category: String,
        previousQuestions: List<String>,
        userProfile: String?,
    ): String {
        val profileContext = userProfile?.let { "\n\nCandidate Profile: $it" } ?: ""
        val previousContext = if (previousQuestions.isNotEmpty()) {
            "\n\nPreviously asked questions (avoid duplicates):\n${
                previousQuestions.joinToString(
                    "\n- ",
                    "- "
                )
            }"
        } else {
            ""
        }

        return """
            You are an expert interviewer conducting a live mock interview via voice.
            Generate a single $category interview question that can be answered in 30-60 seconds.
            $profileContext
            $previousContext

            IMPORTANT REQUIREMENTS:
            - The question must be concise and focused on ONE specific point
            - Avoid multi-part questions or questions with multiple sub-questions
            - The expected answer should be brief (2-4 key sentences)
            - Questions should be answerable without lengthy explanations
            - For TECHNICAL questions: ask about a single concept, not multiple
            - For BEHAVIORAL questions: focus on one specific situation/example
            - For SITUATIONAL questions: present one clear scenario

            Format your response as:
            QUESTION: [the interview question - keep it short and focused]
            IDEAL_ANSWER: [2-4 key bullet points for a good answer]
        """.trimIndent()
    }

    private fun parseFeedbackResponse(feedbackText: String): FeedbackResult {
        val lines = feedbackText.lines()
        var rating = 5 // Default rating
        var feedback = feedbackText

        // Try to extract rating
        lines.forEach { line ->
            if (line.startsWith("RATING:", ignoreCase = true)) {
                val ratingStr = line.substringAfter(":").trim()
                rating = ratingStr.toIntOrNull() ?: 5
            }
        }

        // Try to extract feedback section
        val feedbackIndex = feedbackText.indexOf("FEEDBACK:", ignoreCase = true)
        if (feedbackIndex != -1) {
            feedback = feedbackText.substring(feedbackIndex + 9).trim()
        }

        return FeedbackResult(rating = rating, feedback = feedback)
    }

    private fun parseQuestionResponse(questionText: String): QuestionResult {
        val lines = questionText.lines()
        var question = ""
        var idealAnswer = ""

        lines.forEach { line ->
            when {
                line.startsWith("QUESTION:", ignoreCase = true) -> {
                    question = line.substringAfter(":").trim()
                }

                line.startsWith("IDEAL_ANSWER:", ignoreCase = true) -> {
                    idealAnswer = line.substringAfter(":").trim()
                }
            }
        }

        // If parsing failed, use the whole text as question
        if (question.isBlank()) {
            question = questionText.trim()
        }

        return QuestionResult(question = question, idealAnswer = idealAnswer)
    }
}

/**
 * Result of AI feedback generation
 */
data class FeedbackResult(
    val rating: Int,      // 1-10 rating
    val feedback: String,  // Detailed feedback text
)

/**
 * Result of question generation
 */
data class QuestionResult(
    val question: String,      // The interview question
    val idealAnswer: String,    // Key points for an ideal answer
)
