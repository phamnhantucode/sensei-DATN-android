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
import com.phamnhantucode.aicareercoach.data.ai.PromptFactory
import com.phamnhantucode.aicareercoach.data.ai.OpenRouterService
import java.io.File
import java.util.concurrent.TimeUnit

// Audio operations with Gemini
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

    // Transcribe audio
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

    // Generate feedback
    suspend fun generateFeedback(
        context: String,
        question: String,
        userAnswer: String,
        category: String,
    ): Result<FeedbackResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = PromptFactory.createAudioFeedbackPrompt(
                context = context,
                question = question,
                userAnswer = userAnswer,
                category = category
            )

            val messages = listOf(
                OpenRouterService.Message("user", prompt)
            )

            // Use logic from OpenRouterService
            val feedbackText = OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite" // Consistent with updated strategy
            )

            if (feedbackText.isBlank()) {
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

    // Batch feedback
    suspend fun generateBatchFeedback(
        context: String,
        feedbackInput: List<Triple<String, String, String>>
    ): Result<List<FeedbackResult>> = withContext(Dispatchers.IO) {
        try {
            val prompt = PromptFactory.createBatchFeedbackPrompt(
                context = context,
                feedbackInput = feedbackInput
            )

            val messages = listOf(
                OpenRouterService.Message("user", prompt)
            )

            val response = OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite"
            )

            if (response.isBlank()) {
                return@withContext Result.failure(Exception("No feedback found in response"))
            }

            val feedbackList = parseBatchFeedbackResponse(response, feedbackInput.size)
            Log.d(TAG, "Generated feedback for ${feedbackList.size} questions")

            Result.success(feedbackList)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating batch feedback", e)
            Result.failure(e)
        }
    }

    // Generate all questions
    suspend fun generateAllQuestions(
        context: String,
        category: String,
        questionCount: Int
    ): Result<List<QuestionResult>> = withContext(Dispatchers.IO) {
        try {
            val prompt = PromptFactory.createInterviewQuestionsPrompt(
                context = context,
                focusTopic = category,
                questionCount = questionCount,
                historyContext = "", // Could pass recent performance if available
                includeQuiz = false,
                includeOpenEnded = true
            )

            val messages = listOf(
                OpenRouterService.Message("user", prompt)
            )

            val response = OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite"
            )

            if (response.isBlank()) {
                return@withContext Result.failure(Exception("No questions found in response"))
            }

            val questions = parseBatchQuestionResponse(response)
            if (questions.isEmpty()) {
                return@withContext Result.failure(Exception("Failed to parse questions from response"))
            }
            // Ensure we only return 'questionCount' items if AI over-generated
            val limitedQuestions = questions.take(questionCount)

            Log.d(TAG, "Generated ${limitedQuestions.size} questions for category: $category")
            Result.success(limitedQuestions)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating batch questions", e)
            Result.failure(e)
        }
    }

    // Generate next question
    suspend fun generateNextQuestion(
        context: String,
        category: String,
        previousQuestions: List<String>
    ): Result<QuestionResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = PromptFactory.createSingleInterviewQuestionPrompt(
                context = context,
                focusTopic = category,
                previousQuestions = previousQuestions
            )

            val messages = listOf(
                OpenRouterService.Message("user", prompt)
            )

            val questionText = OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite"
            )

            if (questionText.isBlank()) {
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

    private val CODE_FENCE_REGEX = Regex("```(?:json)?")







    private fun parseQuestionResponse(questionText: String): QuestionResult {
        val cleaned = CODE_FENCE_REGEX.replace(questionText, "").trim()
        return try {
            val json = JSONObject(cleaned)
            val answer = json.optString("idealAnswer").takeIf { it.isNotBlank() }
                ?: json.optString("correctAnswer").takeIf { it.isNotBlank() }
                ?: json.optString("explanation")
                
            QuestionResult(
                question = json.optString("question"),
                idealAnswer = answer
            )
        } catch (e: Exception) {
            // Fallback for non-JSON response
            Log.w(TAG, "Failed to parse JSON question: $cleaned")
             QuestionResult(question = cleaned, idealAnswer = "")
        }
    }

    private fun parseBatchQuestionResponse(responseText: String): List<QuestionResult> {
         val cleaned = CODE_FENCE_REGEX.replace(responseText, "").trim()
         return try {
             val json = JSONObject(cleaned)
             val questionsArray = json.optJSONArray("interviewQuestions") ?: JSONArray()
             val results = mutableListOf<QuestionResult>()
             for (i in 0 until questionsArray.length()) {
                 val q = questionsArray.getJSONObject(i)
                 val answer = q.optString("explanation").takeIf { it.isNotBlank() }
                     ?: q.optString("correctAnswer").takeIf { it.isNotBlank() }
                     ?: q.optString("idealAnswer")
                 
                 results.add(QuestionResult(
                     question = q.optString("question"),
                     idealAnswer = answer
                 ))
             }
             results
         } catch (e: Exception) {
             Log.e(TAG, "Failed to parse batch JSON questions", e)
             emptyList()
         }
    }

    private fun parseBatchFeedbackResponse(responseText: String, expectedCount: Int): List<FeedbackResult> {
         val cleaned = CODE_FENCE_REGEX.replace(responseText, "").trim()
         return try {
             val json = JSONObject(cleaned)
             val feedbackArray = json.optJSONArray("feedbackList") ?: JSONArray()
             val results = mutableListOf<FeedbackResult>()
             for (i in 0 until feedbackArray.length()) {
                 val f = feedbackArray.getJSONObject(i)
                 results.add(FeedbackResult(
                     rating = f.optInt("rating", 5),
                     feedback = f.optString("feedback")
                 ))
             }
             results
         } catch (e: Exception) {
             Log.e(TAG, "Failed to parse batch JSON feedback", e)
             emptyList()
         }
    }

    private fun parseFeedbackResponse(feedbackText: String): FeedbackResult {
         val cleaned = CODE_FENCE_REGEX.replace(feedbackText, "").trim()
         return try {
             val json = JSONObject(cleaned)
             FeedbackResult(
                 rating = json.optInt("rating", 5),
                 feedback = json.optString("feedback")
             )
         } catch (e: Exception) {
             parseFeedbackText(cleaned)
         }
    }

    fun parseFeedbackText(text: String): FeedbackResult {
        val lines = text.lines()
        var rating = 5
        var feedback = text

        lines.forEach { line ->
            if (line.startsWith("RATING:", ignoreCase = true)) {
                val ratingStr = line.substringAfter(":").trim()
                rating = ratingStr.toIntOrNull() ?: 5
            }
        }
        val feedbackIndex = text.indexOf("FEEDBACK:", ignoreCase = true)
        if (feedbackIndex != -1) {
            feedback = text.substring(feedbackIndex + 9).trim()
        }
        return FeedbackResult(rating, feedback)
    }
}

// Feedback Result
data class FeedbackResult(
    val rating: Int,      // 1-10 rating
    val feedback: String,  // Detailed feedback text
)

// Question Result
data class QuestionResult(
    val question: String,      // The interview question
    val idealAnswer: String,    // Key points for an ideal answer
)
