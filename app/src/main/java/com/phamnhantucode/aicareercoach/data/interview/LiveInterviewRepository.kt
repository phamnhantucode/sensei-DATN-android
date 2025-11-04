package com.phamnhantucode.aicareercoach.data.interview

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.fetchToken
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.audio.GeminiAudioService
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Repository for managing live mock interviews with speech-to-text functionality.
 * Coordinates audio recording, transcription, AI feedback, and persistence.
 */
class LiveInterviewRepository(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val database = AppDatabase.getDatabase(context)
    private val geminiAudioService = GeminiAudioService()
    private val userProfileCacheDao = database.userProfileCacheDao()

    companion object {
        private const val TAG = "LiveInterviewRepository"
    }

    /**
     * Starts a new live interview session and generates the first question.
     */
    suspend fun startLiveInterview(request: StartLiveInterviewRequest): Result<StartInterviewResult> =
        withContext(Dispatchers.IO) {
            try {
                val user = Clerk.user
                    ?: return@withContext Result.failure(Exception("User session unavailable"))

                if (BuildConfig.NEON_API_URL.isBlank()) {
                    return@withContext Result.failure(Exception("Neon API URL not configured"))
                }

                val authHeader = resolveAuthorizationHeader()
                    ?: return@withContext Result.failure(Exception("Authentication failed"))

                // Fetch Neon user profile to get the UUID
                val neonUser = fetchNeonUserProfile(user.id, authHeader)
                    ?: return@withContext Result.failure(Exception("Failed to fetch user profile"))

                // Create new interview session in database
                val sessionId = createInterviewSession(neonUser.id, authHeader)
                    ?: return@withContext Result.failure(Exception("Failed to create interview session"))

                // Generate first question
                val firstQuestionResult = geminiAudioService.generateNextQuestion(
                    category = request.interviewType.name,
                    previousQuestions = emptyList(),
                    userProfile = buildUserProfileContext(request)
                )

                if (firstQuestionResult.isFailure) {
                    return@withContext Result.failure(firstQuestionResult.exceptionOrNull() ?: Exception("Failed to generate question"))
                }

                val questionData = firstQuestionResult.getOrNull()!!
                val firstQuestion = LiveQuestion(
                    liveMockInterviewId = sessionId,
                    questionText = questionData.question,
                    category = request.interviewType.name,
                    correctAnswer = questionData.idealAnswer
                )

                Log.d(TAG, "Live interview started: sessionId=$sessionId")

                Result.success(StartInterviewResult(sessionId = sessionId, firstQuestion = firstQuestion))

            } catch (e: Exception) {
                Log.e(TAG, "Error starting live interview", e)
                Result.failure(e)
            }
        }

    /**
     * Processes a recorded audio answer: transcribes it and gets AI feedback.
     */
    suspend fun processAnswer(
        audioFile: File,
        question: LiveQuestion,
        sessionId: String,
        isLastQuestion: Boolean,
        previousQuestions: List<String>
    ): Result<AnswerResult> = withContext(Dispatchers.IO) {
        try {
            // Step 1: Transcribe audio
            val transcriptionResult = geminiAudioService.transcribeAudio(audioFile)
            if (transcriptionResult.isFailure) {
                return@withContext Result.failure(transcriptionResult.exceptionOrNull() ?: Exception("Transcription failed"))
            }

            val transcription = transcriptionResult.getOrNull()!!
            Log.d(TAG, "Audio transcribed: $transcription")

            // Step 2: Get AI feedback
            val feedbackResult = geminiAudioService.generateFeedback(
                question = question.questionText,
                userAnswer = transcription,
                category = question.category
            )

            if (feedbackResult.isFailure) {
                return@withContext Result.failure(feedbackResult.exceptionOrNull() ?: Exception("Feedback generation failed"))
            }

            val feedback = feedbackResult.getOrNull()!!

            // Step 3: Save question and answer to database
            val savedQuestion = question.copy(
                userAnswer = transcription,
                audioTranscript = transcription,
                feedback = feedback.feedback,
                rating = feedback.rating
            )

            val authHeader = resolveAuthorizationHeader()
                ?: return@withContext Result.failure(Exception("Authentication failed"))

            saveQuestionToDatabase(savedQuestion, authHeader)

            // Step 4: Generate next question if not last
            val nextQuestion = if (!isLastQuestion) {
                val nextQuestionResult = geminiAudioService.generateNextQuestion(
                    category = question.category,
                    previousQuestions = previousQuestions + question.questionText,
                    userProfile = null
                )

                if (nextQuestionResult.isSuccess) {
                    val questionData = nextQuestionResult.getOrNull()!!
                    LiveQuestion(
                        liveMockInterviewId = sessionId,
                        questionText = questionData.question,
                        category = question.category,
                        correctAnswer = questionData.idealAnswer
                    )
                } else {
                    null
                }
            } else {
                null
            }

            // Step 5: Clean up audio file
            audioFile.delete()

            Result.success(
                AnswerResult(
                    transcription = transcription,
                    feedback = feedback.feedback,
                    rating = feedback.rating,
                    nextQuestion = nextQuestion
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing answer", e)
            Result.failure(e)
        }
    }

    /**
     * Completes the interview and generates a comprehensive summary.
     */
    suspend fun completeInterview(
        sessionId: String,
        questions: List<LiveQuestion>
    ): Result<InterviewSummary> = withContext(Dispatchers.IO) {
        try {
            // Calculate overall statistics
            val answeredQuestions = questions.filter { it.rating != null }
            val overallScore = if (answeredQuestions.isNotEmpty()) {
                answeredQuestions.mapNotNull { it.rating }.average().toFloat()
            } else {
                0f
            }

            val totalDuration = questions.sumOf { it.duration ?: 0L }

            // Calculate category breakdown
            val categoryBreakdown = questions
                .filter { it.rating != null }
                .groupBy { it.category }
                .mapValues { (category, qs) ->
                    CategoryPerformance(
                        category = category,
                        averageRating = qs.mapNotNull { it.rating }.average().toFloat(),
                        questionsCount = qs.size
                    )
                }

            // Analyze strengths and areas for improvement
            val strengths = mutableListOf<String>()
            val areasForImprovement = mutableListOf<String>()

            questions.forEach { q ->
                q.rating?.let { rating ->
                    if (rating >= 8) {
                        strengths.add("Strong performance on: ${q.questionText.take(80)}...")
                    } else if (rating <= 5) {
                        areasForImprovement.add("Needs improvement: ${q.questionText.take(80)}...")
                    }
                }
            }

            if (strengths.isEmpty()) {
                strengths.add("Keep practicing to build confidence")
            }

            if (areasForImprovement.isEmpty()) {
                areasForImprovement.add("Continue refining your communication style")
            }

            val summary = InterviewSummary(
                sessionId = sessionId,
                overallScore = overallScore,
                questionsAnswered = answeredQuestions.size,
                totalDuration = totalDuration,
                categoryBreakdown = categoryBreakdown,
                strengths = strengths.take(5),
                areasForImprovement = areasForImprovement.take(5),
                questions = questions
            )

            Log.d(TAG, "Interview completed: sessionId=$sessionId, score=$overallScore")

            Result.success(summary)

        } catch (e: Exception) {
            Log.e(TAG, "Error completing interview", e)
            Result.failure(e)
        }
    }

    /**
     * Retrieves interview history for a user.
     */
    suspend fun getInterviewHistory(userId: String): Result<List<LiveMockInterviewSession>> =
        withContext(Dispatchers.IO) {
            try {
                val authHeader = resolveAuthorizationHeader()
                    ?: return@withContext Result.failure(Exception("Authentication failed"))

                val encodedUserId = URLEncoder.encode(userId, "UTF-8")
                val url = "${BuildConfig.NEON_API_URL}/LiveMockInterview?select=*&userId=eq.$encodedUserId&order=createdAt.desc&limit=20"

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    throw IOException("Failed to fetch interview history: ${response.code}")
                }

                // Parse response (simplified, you may need to adjust based on actual API response)
                val sessions = mutableListOf<LiveMockInterviewSession>()
                // TODO: Parse JSON response and convert to LiveMockInterviewSession objects

                Result.success(sessions)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching interview history", e)
                Result.failure(e)
            }
        }

    private suspend fun fetchNeonUserProfile(clerkUserId: String, authHeader: String): NeonUserProfile? {
        return try {
            val encodedClerkId = java.net.URLEncoder.encode(clerkUserId, "UTF-8")
            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val url = "$apiUrl/User?select=id,industry,skills,bio,experience&clerkUserId=eq.$encodedClerkId&limit=1"

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Failed to fetch user profile: ${response.code}")
                return null
            }

            val jsonArray = JSONArray(responseBody)
            if (jsonArray.length() > 0) {
                val json = jsonArray.getJSONObject(0)
                val skillsJson = json.optJSONArray("skills")
                val skills = if (skillsJson != null) {
                    (0 until skillsJson.length()).map { skillsJson.optString(it) }.filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                NeonUserProfile(
                    id = json.optString("id"),
                    industry = json.optString("industry").takeIf { it.isNotBlank() },
                    experienceYears = json.optInt("experience").takeUnless { json.isNull("experience") },
                    skills = skills,
                    bio = json.optString("bio").takeIf { it.isNotBlank() }
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            null
        }
    }

    private suspend fun createInterviewSession(userId: String, authHeader: String): String? {
        return try {
            val payload = JSONObject().apply {
                put("userId", userId)
            }

            val request = Request.Builder()
                .url("${BuildConfig.NEON_API_URL}/LiveMockInterview")
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to create interview session: ${response.code}")
                return null
            }

            // Parse response to get the created session ID
            val jsonArray = JSONArray(responseBody)
            if (jsonArray.length() > 0) {
                val session = jsonArray.getJSONObject(0)
                session.optString("id")
            } else {
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error creating interview session", e)
            null
        }
    }

    private suspend fun saveQuestionToDatabase(question: LiveQuestion, authHeader: String) {
        try {
            val payload = JSONObject().apply {
                put("liveMockInterviewId", question.liveMockInterviewId)
                put("question", question.questionText)
                put("correctAnswer", question.correctAnswer)
                put("userAnswer", question.userAnswer)
                put("feedback", question.feedback)
                put("rating", question.rating)
            }

            val request = Request.Builder()
                .url("${BuildConfig.NEON_API_URL}/LiveInterviewQuestion")
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to save question: ${response.code}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving question", e)
        }
    }

    private fun buildUserProfileContext(request: StartLiveInterviewRequest): String {
        val parts = mutableListOf<String>()

        request.industry?.let { parts.add("Industry: $it") }
        request.experienceLevel?.let { parts.add("Experience: $it years") }

        if (request.skills.isNotEmpty()) {
            parts.add("Skills: ${request.skills.joinToString(", ")}")
        }

        return parts.joinToString(" | ")
    }

    private suspend fun resolveAuthorizationHeader(forceRefresh: Boolean = false): String? {
        val session = Clerk.session ?: return null

        // Try to get JWT token
        if (forceRefresh) {
            val result = session.fetchToken()
            if (result is ClerkResult.Success) {
                return "Bearer ${result.value.jwt}"
            }
        }

        // Use last active token
        session.lastActiveToken?.jwt?.let { jwt ->
            return "Bearer $jwt"
        }

        // Fallback to basic auth
        val role = BuildConfig.NEON_DB_ROLE
        val password = BuildConfig.NEON_DB_PASSWORD

        if (role.isNotBlank() && password.isNotBlank()) {
            val credentials = "$role:$password"
            val encoded = android.util.Base64.encodeToString(
                credentials.toByteArray(),
                android.util.Base64.NO_WRAP
            )
            return "Basic $encoded"
        }

        return null
    }

    /**
     * Internal data class for Neon user profile.
     */
    private data class NeonUserProfile(
        val id: String,
        val industry: String?,
        val experienceYears: Int?,
        val skills: List<String>,
        val bio: String?,
    )
}
