package com.phamnhantucode.aicareercoach.data.interview

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.audio.GeminiAudioService
import com.phamnhantucode.aicareercoach.data.audio.VoskSpeechRecognizer
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
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
 * Coordinates audio recording, transcription (via Vosk), AI feedback (via Gemini), and persistence.
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
    private val voskRecognizer = VoskSpeechRecognizer(context)
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
                Log.d(TAG, "startLiveInterview() called with request: $request")

                val user = Clerk.user
                Log.d(TAG, "Clerk.user: ${if (user != null) "ID=${user.id}" else "NULL"}")
                if (user == null) {
                    Log.e(TAG, "User session unavailable")
                    return@withContext Result.failure(Exception("User session unavailable"))
                }

                val apiUrl = BuildConfig.NEON_API_URL
                Log.d(TAG, "NEON_API_URL configured: ${apiUrl.isNotBlank()}, value=${if (apiUrl.isNotBlank()) apiUrl else "BLANK"}")
                if (apiUrl.isBlank()) {
                    Log.e(TAG, "Neon API URL not configured")
                    return@withContext Result.failure(Exception("Neon API URL not configured"))
                }

                Log.d(TAG, "Fetching authentication token...")
                val authToken = NeonAuth.fetchNeonAuthToken()
                Log.d(TAG, "Auth token fetched: ${authToken != null}")

                val authHeader = buildAuthorizationHeader(authToken)
                if (authHeader == null) {
                    Log.e(TAG, "Failed to build authorization header")
                    return@withContext Result.failure(Exception("Authentication failed"))
                }
                Log.d(TAG, "Authorization header built successfully")

                // Fetch Neon user profile to get the UUID
                Log.d(TAG, "Fetching Neon user profile for Clerk user: ${user.id}")
                val neonUser = fetchNeonUserProfile(user.id, authHeader)
                Log.d(TAG, "Neon user profile fetched: ${if (neonUser != null) "SUCCESS (id=${neonUser.id})" else "NULL (FAILED)"}")
                if (neonUser == null) {
                    Log.e(TAG, "Failed to fetch user profile from Neon database for Clerk user: ${user.id}")
                    return@withContext Result.failure(Exception("Failed to fetch user profile"))
                }

                // Create new interview session in database
                Log.d(TAG, "Creating interview session for user=${neonUser.id}, role=${request.interviewType.name}, yoes=${request.experienceLevel ?: 0}")
                val sessionId = createInterviewSession(
                    userId = neonUser.id,
                    authHeader = authHeader,
                    role = request.interviewType.name,
                    description = "Mock interview for ${request.interviewType.name}",
                    yoes = request.experienceLevel ?: 0
                )
                Log.d(TAG, "Interview session created: ${if (sessionId != null) "SUCCESS (id=$sessionId)" else "NULL (FAILED)"}")
                if (sessionId == null) {
                    Log.e(TAG, "Failed to create interview session in database")
                    return@withContext Result.failure(Exception("Failed to create interview session"))
                }

                // Generate first question
                Log.d(TAG, "Generating first question for category=${request.interviewType.name}")
                val firstQuestionResult = geminiAudioService.generateNextQuestion(
                    category = request.interviewType.name,
                    previousQuestions = emptyList(),
                    userProfile = buildUserProfileContext(request)
                )
                Log.d(TAG, "First question generation result: ${if (firstQuestionResult.isSuccess) "SUCCESS" else "FAILED"}")

                if (firstQuestionResult.isFailure) {
                    val error = firstQuestionResult.exceptionOrNull() ?: Exception("Failed to generate question")
                    Log.e(TAG, "Failed to generate first question", error)

                    // Provide specific error message based on exception
                    val userMessage = when {
                        error.message?.contains("API key not configured") == true ->
                            "Gemini API key is not configured. Please add GEMINI_API_KEY to local.properties"
                        error.message?.contains("HTTP 401") == true || error.message?.contains("HTTP 403") == true ->
                            "Gemini API authentication failed. Please check your API key in local.properties"
                        error.message?.contains("HTTP 429") == true ->
                            "Gemini API rate limit exceeded. Please try again later"
                        error.message?.contains("HTTP 5") == true ->
                            "Gemini API server error. Please try again later"
                        else -> "Failed to generate interview question: ${error.message}"
                    }

                    return@withContext Result.failure(Exception(userMessage))
                }

                val questionData = firstQuestionResult.getOrNull()!!
                val firstQuestion = LiveQuestion(
                    liveMockInterviewId = sessionId,
                    questionText = questionData.question,
                    category = request.interviewType.name,
                    correctAnswer = questionData.idealAnswer
                )

                Log.d(TAG, "Live interview started successfully: sessionId=$sessionId, questionText=${questionData.question.take(50)}...")

                Result.success(StartInterviewResult(sessionId = sessionId, firstQuestion = firstQuestion))

            } catch (e: Exception) {
                Log.e(TAG, "Exception in startLiveInterview()", e)
                Result.failure(e)
            }
        }

    /**
     * Processes a recorded audio answer: transcribes it using Vosk and gets AI feedback from Gemini.
     */
    suspend fun processAnswer(
        audioFile: File,
        question: LiveQuestion,
        sessionId: String,
        isLastQuestion: Boolean,
        previousQuestions: List<String>
    ): Result<AnswerResult> = withContext(Dispatchers.IO) {
        try {
            // Pre-validate audio file
            if (!audioFile.exists() || audioFile.length() <= 44) {
                Log.e(TAG, "Audio file validation failed: exists=${audioFile.exists()}, size=${audioFile.length()}")
                audioFile.delete()
                return@withContext Result.failure(
                    Exception("Audio recording failed. Please try again.")
                )
            }

            Log.d(TAG, "Processing audio file: ${audioFile.absolutePath}, size=${audioFile.length()} bytes")

            // Step 1: Transcribe audio using Vosk (offline, accurate speech-to-text)
            Log.d(TAG, "Transcribing audio with Vosk...")
            val transcriptionResult = voskRecognizer.transcribeAudio(audioFile)

            if (transcriptionResult.isFailure) {
                val error = transcriptionResult.exceptionOrNull()
                val userMessage = when {
                    error?.message?.contains("corrupted") == true || error?.message?.contains("invalid") == true ->
                        "Audio file was corrupted. Please try recording again."
                    error?.message?.contains("No speech detected") == true ->
                        "No speech detected. Please speak clearly into the microphone."
                    error?.message?.contains("model not available") == true ->
                        "Speech recognition is initializing. Please wait and try again."
                    error?.message?.contains("empty") == true ->
                        "Recording too short. Please speak for at least 2 seconds."
                    else ->
                        "Transcription failed: ${error?.message ?: "Unknown error"}"
                }

                Log.e(TAG, "Transcription failed: ${error?.message}", error)

                // Clean up audio file
                audioFile.delete()

                return@withContext Result.failure(Exception(userMessage))
            }

            val transcription = transcriptionResult.getOrNull()!!
            Log.d(TAG, "Audio transcribed with Vosk: $transcription")

            // Step 2: Get AI feedback from Gemini (uses text, not audio)
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

            val authToken = NeonAuth.fetchNeonAuthToken()
            val authHeader = buildAuthorizationHeader(authToken)
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
            // Clean up audio file on error
            audioFile.delete()
            Result.failure(Exception("Failed to process answer: ${e.message}"))
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
                val authToken = NeonAuth.fetchNeonAuthToken()
                val authHeader = buildAuthorizationHeader(authToken)
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
            Log.d(TAG, "fetchNeonUserProfile() called for clerkUserId=$clerkUserId")

            val encodedClerkId = java.net.URLEncoder.encode(clerkUserId, "UTF-8")
            val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
            val url = "$apiUrl/User?select=id,industry,skills,bio,experience&clerkUserId=eq.$encodedClerkId&limit=1"
            Log.d(TAG, "Fetching from URL: $url")

            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .get()
                .build()

            Log.d(TAG, "Executing HTTP request...")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d(TAG, "HTTP Response: code=${response.code}, hasBody=${responseBody != null}, bodyLength=${responseBody?.length ?: 0}")

            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Failed to fetch user profile: HTTP ${response.code}, body=${responseBody?.take(200)}")
                return null
            }

            Log.d(TAG, "Response body (first 200 chars): ${responseBody.take(200)}")

            val jsonArray = JSONArray(responseBody)
            Log.d(TAG, "JSON array length: ${jsonArray.length()}")

            if (jsonArray.length() > 0) {
                val json = jsonArray.getJSONObject(0)
                val skillsJson = json.optJSONArray("skills")
                val skills = if (skillsJson != null) {
                    (0 until skillsJson.length()).map { skillsJson.optString(it) }.filter { it.isNotBlank() }
                } else {
                    emptyList()
                }

                val profile = NeonUserProfile(
                    id = json.optString("id"),
                    industry = json.optString("industry").takeIf { it.isNotBlank() },
                    experienceYears = json.optInt("experience").takeUnless { json.isNull("experience") },
                    skills = skills,
                    bio = json.optString("bio").takeIf { it.isNotBlank() }
                )
                Log.d(TAG, "User profile parsed successfully: id=${profile.id}, industry=${profile.industry}, skills=${profile.skills.size}")
                profile
            } else {
                Log.e(TAG, "No user found in Neon database for Clerk user: $clerkUserId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception in fetchNeonUserProfile()", e)
            null
        }
    }

    private suspend fun createInterviewSession(
        userId: String,
        authHeader: String,
        role: String,
        description: String,
        yoes: Int
    ): String? {
        return try {
            Log.d(TAG, "createInterviewSession() called with userId=$userId, role=$role, yoes=$yoes")

            val randomBytes = ByteArray(12)
            java.security.SecureRandom().nextBytes(randomBytes)
            val hexId = randomBytes.joinToString("") { "%02x".format(it) }
            val now = java.time.Instant.now().toString()

            val payload = JSONObject().apply {
                put("id", hexId)
                put("userId", userId)
                put("role", role)
                put("description", description)
                put("yoes", yoes)
                put("createdAt", now)
                put("updatedAt", now)
            }
            Log.d(TAG, "Generated session ID: $hexId")
            Log.d(TAG, "Request payload: $payload")

            val apiUrl = "${BuildConfig.NEON_API_URL}/LiveMockInterview"
            Log.d(TAG, "POST to: $apiUrl")

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            Log.d(TAG, "Executing HTTP POST request...")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            Log.d(TAG, "HTTP Response: code=${response.code}, hasBody=${responseBody != null}, bodyLength=${responseBody?.length ?: 0}")

            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to create interview session: HTTP ${response.code}")
                Log.e(TAG, "Response body: ${responseBody ?: "null"}")
                return null
            }

            Log.d(TAG, "Response body: ${responseBody}")

            // Parse response to get the created session ID
            val jsonArray = JSONArray(responseBody)
            Log.d(TAG, "Response JSON array length: ${jsonArray.length()}")

            if (jsonArray.length() > 0) {
                val session = jsonArray.getJSONObject(0)
                val returnedId = session.optString("id")
                Log.d(TAG, "Session created successfully with ID: $returnedId")
                returnedId
            } else {
                Log.e(TAG, "Response array is empty - no session returned")
                null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception in createInterviewSession()", e)
            null
        }
    }

    private suspend fun saveQuestionToDatabase(question: LiveQuestion, authHeader: String) {
        try {
            val randomBytes = ByteArray(12)
            java.security.SecureRandom().nextBytes(randomBytes)
            val hexId = randomBytes.joinToString("") { "%02x".format(it) }
            val now = java.time.Instant.now().toString()

            val payload = JSONObject().apply {
                put("id", hexId)
                put("liveMockInterviewId", question.liveMockInterviewId)
                put("question", question.questionText)
                put("correctAnswer", question.correctAnswer)
                put("userAnswer", question.userAnswer)
                put("feedback", question.feedback)
                put("rating", question.rating)
                put("createdAt", now)
                put("updatedAt", now)
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

    /**
     * Builds an HTTP Authorization header from an auth token.
     * Returns null if no valid authentication is available.
     *
     * TODO: Consider moving to shared utility in NeonAuth.
     * This pattern is duplicated in NeonGridResumeService and NeonUserService.
     */
    private fun buildAuthorizationHeader(authToken: String?): String? {
        return when {
            authToken.isNullOrBlank() -> {
                Log.e(TAG, "No authentication token provided")
                null
            }
            authToken == "" -> {
                // Empty string signals to use Basic auth fallback
                val role = BuildConfig.NEON_DB_ROLE
                val password = BuildConfig.NEON_DB_PASSWORD
                if (role.isNotBlank() && password.isNotBlank()) {
                    val credentials = "$role:$password"
                    val encoded = android.util.Base64.encodeToString(
                        credentials.toByteArray(),
                        android.util.Base64.NO_WRAP
                    )
                    Log.d(TAG, "Using Basic authentication fallback")
                    "Basic $encoded"
                } else {
                    Log.e(TAG, "Basic auth credentials not configured")
                    null
                }
            }
            else -> {
                Log.d(TAG, "Using Bearer token authentication")
                "Bearer $authToken"
            }
        }
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
