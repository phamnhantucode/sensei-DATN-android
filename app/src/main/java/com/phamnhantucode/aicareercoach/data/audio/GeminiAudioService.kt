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
        private const val GEMINI_MODEL = "gemini-2.0-flash-exp"
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

            Log.d(TAG, "Transcribing audio file: ${audioFile.absolutePath}, size: ${audioFile.length()} bytes")

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

            // Build request payload
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
                            // Add prompt for transcription
                            put(JSONObject().apply {
                                put("text", "Please transcribe this audio accurately. Only provide the transcribed text without any additional commentary.")
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.1)
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

            Log.d(TAG, "Transcription successful: $transcribedText")
            Result.success(transcribedText.trim())

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
        category: String
    ): Result<FeedbackResult> = withContext(Dispatchers.IO) {
        try {
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
                Log.e(TAG, "Feedback generation failed: ${response.code}")
                return@withContext Result.failure(Exception("Failed to generate feedback"))
            }

            val jsonResponse = JSONObject(responseBody)
            val feedbackText = extractGeminiText(jsonResponse)

            if (feedbackText.isNullOrBlank()) {
                return@withContext Result.failure(Exception("No feedback found in response"))
            }

            // Parse feedback to extract rating and comments
            val feedbackResult = parseFeedbackResponse(feedbackText)
            Log.d(TAG, "Feedback generated: rating=${feedbackResult.rating}, feedback=${feedbackResult.feedback}")

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
        userProfile: String? = null
    ): Result<QuestionResult> = withContext(Dispatchers.IO) {
        try {
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
                return@withContext Result.failure(Exception("Failed to generate question"))
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

    private fun buildFeedbackPrompt(question: String, userAnswer: String, category: String): String {
        return """
            You are an expert interview coach. Evaluate this candidate's interview answer and provide constructive feedback.

            Question: $question
            Category: $category
            Candidate's Answer: $userAnswer

            Please provide:
            1. A rating from 1-10 (where 10 is excellent)
            2. Specific, actionable feedback on the answer
            3. Highlight strengths
            4. Suggest improvements

            Format your response as:
            RATING: [number]
            FEEDBACK: [your detailed feedback]
        """.trimIndent()
    }

    private fun buildQuestionPrompt(
        category: String,
        previousQuestions: List<String>,
        userProfile: String?
    ): String {
        val profileContext = userProfile?.let { "\n\nCandidate Profile: $it" } ?: ""
        val previousContext = if (previousQuestions.isNotEmpty()) {
            "\n\nPreviously asked questions (avoid duplicates):\n${previousQuestions.joinToString("\n- ", "- ")}"
        } else {
            ""
        }

        return """
            You are an expert interviewer. Generate a single $category interview question.
            $profileContext
            $previousContext

            Format your response as:
            QUESTION: [the interview question]
            IDEAL_ANSWER: [key points that should be in a good answer]
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
    val feedback: String  // Detailed feedback text
)

/**
 * Result of question generation
 */
data class QuestionResult(
    val question: String,      // The interview question
    val idealAnswer: String    // Key points for an ideal answer
)
