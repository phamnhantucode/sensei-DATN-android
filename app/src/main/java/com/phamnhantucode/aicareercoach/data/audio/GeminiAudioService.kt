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
        question: String,
        userAnswer: String,
        category: String,
    ): Result<FeedbackResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildFeedbackPrompt(question, userAnswer, category)

            val messages = listOf(
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message("user", prompt)
            )

            // Use logic from OpenRouterService
            val feedbackText = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite" // Consistent with CoverLetter or use default
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
        questionsAndAnswers: List<Triple<String, String, String>>
    ): Result<List<FeedbackResult>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildBatchFeedbackPrompt(questionsAndAnswers)

            val messages = listOf(
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message("user", prompt)
            )

            val response = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(
                messages = messages,
                model = "google/gemini-2.5-flash-lite"
            )

            if (response.isBlank()) {
                return@withContext Result.failure(Exception("No feedback found in response"))
            }

            val feedbackList = parseBatchFeedbackResponse(response, questionsAndAnswers.size)
            Log.d(TAG, "Generated feedback for ${feedbackList.size} questions")

            Result.success(feedbackList)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating batch feedback", e)
            Result.failure(e)
        }
    }

    // Generate all questions
    suspend fun generateAllQuestions(
        category: String,
        questionCount: Int,
        userProfile: String? = null,
    ): Result<List<QuestionResult>> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildBatchQuestionPrompt(category, questionCount, userProfile)

            val messages = listOf(
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message("user", prompt)
            )

            val response = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(
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

            Log.d(TAG, "Generated ${questions.size} questions for category: $category")
            Result.success(questions)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating batch questions", e)
            Result.failure(e)
        }
    }

    // Generate next question
    suspend fun generateNextQuestion(
        category: String,
        previousQuestions: List<String>,
        userProfile: String? = null,
    ): Result<QuestionResult> = withContext(Dispatchers.IO) {
        try {
            val prompt = buildQuestionPrompt(category, previousQuestions, userProfile)

            val messages = listOf(
                com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message("user", prompt)
            )

            val questionText = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(
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

    private fun buildBatchQuestionPrompt(
        category: String,
        questionCount: Int,
        userProfile: String?,
    ): String {
        val profileContext = userProfile?.let { "\n\nCandidate Profile: $it" } ?: ""

        return """
            You are an expert interviewer conducting a live mock interview via voice.
            Generate exactly $questionCount different $category interview questions for a complete interview session.
            Each question should be answerable in 30-60 seconds.
            $profileContext

            IMPORTANT REQUIREMENTS:
            - Each question must be unique and cover different aspects of $category skills
            - Questions should be concise and focused on ONE specific point each
            - Avoid multi-part questions or questions with multiple sub-questions
            - The expected answers should be brief (2-4 key sentences each)
            - Questions should be answerable without lengthy explanations
            - For TECHNICAL questions: ask about different concepts/technologies
            - For BEHAVIORAL questions: focus on different situations/examples
            - For GENERAL questions: cover different interview aspects
            - Create a progression from easier to more challenging questions

            Format your response as:
            QUESTION_1: [first interview question]
            IDEAL_ANSWER_1: [2-4 key bullet points for a good answer]

            QUESTION_2: [second interview question]
            IDEAL_ANSWER_2: [2-4 key bullet points for a good answer]

            ... and so on for all $questionCount questions.
        """.trimIndent()
    }

    private fun buildBatchFeedbackPrompt(
        questionsAndAnswers: List<Triple<String, String, String>>
    ): String {
        val questionsText = questionsAndAnswers.mapIndexed { index, (question, answer, category) ->
            """
            QUESTION_${index + 1} ($category):
            Q: $question
            A: $answer
            """.trimIndent()
        }.joinToString("\n\n")

        return """
            You are an expert interview coach evaluating a candidate's complete interview performance.
            Analyze ALL the questions and answers together to provide comprehensive feedback.

            INTERVIEW SESSION:
            $questionsText

            For EACH question, provide:
            1. A rating from 1-10 (where 10 is excellent)
            2. Specific, actionable feedback on the answer
            3. Highlight strengths using **bold** for key points
            4. Suggest improvements

            Consider the overall interview performance and look for patterns across answers.

            Format your response as:
            FEEDBACK_1:
            RATING: [number]
            FEEDBACK: [detailed feedback for question 1]

            FEEDBACK_2:
            RATING: [number] 
            FEEDBACK: [detailed feedback for question 2]

            ... and so on for all questions.
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

    private fun parseBatchQuestionResponse(responseText: String): List<QuestionResult> {
        val questions = mutableListOf<QuestionResult>()
        val lines = responseText.lines()
        
        var currentQuestion = ""
        var currentIdealAnswer = ""
        var questionNumber = 1

        lines.forEach { line ->
            when {
                line.startsWith("QUESTION_$questionNumber:", ignoreCase = true) -> {
                    currentQuestion = line.substringAfter(":").trim()
                }
                line.startsWith("IDEAL_ANSWER_$questionNumber:", ignoreCase = true) -> {
                    currentIdealAnswer = line.substringAfter(":").trim()
                    
                    // Add the completed question
                    if (currentQuestion.isNotBlank()) {
                        questions.add(QuestionResult(
                            question = currentQuestion,
                            idealAnswer = currentIdealAnswer
                        ))
                    }
                    
                    // Reset for next question
                    currentQuestion = ""
                    currentIdealAnswer = ""
                    questionNumber++
                }
            }
        }

        return questions
    }

    private fun parseBatchFeedbackResponse(responseText: String, expectedCount: Int): List<FeedbackResult> {
        val feedbackList = mutableListOf<FeedbackResult>()
        
        for (i in 1..expectedCount) {
            val feedbackPattern = "FEEDBACK_$i:"
            val nextFeedbackPattern = "FEEDBACK_${i + 1}:"
            
            val startIndex = responseText.indexOf(feedbackPattern, ignoreCase = true)
            if (startIndex == -1) continue
            
            val endIndex = responseText.indexOf(nextFeedbackPattern, ignoreCase = true)
            val feedbackSection = if (endIndex != -1) {
                responseText.substring(startIndex, endIndex)
            } else {
                responseText.substring(startIndex)
            }
            
            // Parse this feedback section
            val feedback = parseFeedbackResponse(feedbackSection)
            feedbackList.add(feedback)
        }
        
        return feedbackList
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
