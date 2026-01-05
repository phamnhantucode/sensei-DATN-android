package com.phamnhantucode.aicareercoach.data.interview

import android.content.Context
import android.util.Log
import com.clerk.api.Clerk
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.ai.PromptFactory
import com.phamnhantucode.aicareercoach.data.audio.GeminiAudioService
import com.phamnhantucode.aicareercoach.data.audio.VoskSpeechRecognizer
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import com.phamnhantucode.aicareercoach.data.neon.NeonAuth
import com.phamnhantucode.aicareercoach.data.neon.NeonUserService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
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

// Manages live mock interviews
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
    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "LiveInterviewRepository"
    }

    // Starts batch interview
    suspend fun startBatchInterview(request: StartLiveInterviewRequest): Result<BatchStartInterviewResult> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "startBatchInterview() called with request: $request")

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

                val sessionId: String
                
                if (request.sessionId != null) {
                    // Reuse existing session
                    sessionId = request.sessionId
                    Log.d(TAG, "Reusing existing session ID: $sessionId")
                    // Skip credit deduction for retry
                } else {
                    // Deduct Credit
                    com.phamnhantucode.aicareercoach.data.neon.NeonUserService.deductCredit(neonUser.id, 1, "Live Interview Session", authHeader)
    
                    // Use job title as role if available, otherwise fallback to interview type name
                    val sessionRole = if (request.jobTitle.isNotBlank()) request.jobTitle else request.interviewType.name
                    val sessionDesc = "Mock interview for $sessionRole"

                    // Create new interview session in database
                    Log.d(TAG, "Creating interview session for user=${neonUser.id}, role=$sessionRole, yoes=${request.experienceLevel ?: 0}")
                    val newSessionId = createInterviewSession(
                        userId = neonUser.id,
                        authHeader = authHeader,
                        role = sessionRole,
                        description = sessionDesc,
                        yoes = request.experienceLevel ?: 0
                    )
                    Log.d(TAG, "Interview session created: ${if (newSessionId != null) "SUCCESS (id=$newSessionId)" else "NULL (FAILED)"}")
                    if (newSessionId == null) {
                        Log.e(TAG, "Failed to create interview session in database")
                        return@withContext Result.failure(Exception("Failed to create interview session"))
                    }
                    sessionId = newSessionId
                }

                // Generate ALL questions at once OR reuse existing
                val questionsPool: List<LiveQuestion>
                
                if (request.existingQuestions != null && request.existingQuestions.isNotEmpty()) {
                    Log.d(TAG, "Reusing ${request.existingQuestions.size} existing questions")
                    
                    if (request.sessionId != null) {
                         // If reusing session, we just use the existing questions as is (with their original IDs)
                         // But we need to reset them in the database
                         questionsPool = request.existingQuestions.map { 
                             it.copy(
                                 userAnswer = null,
                                 feedback = null,
                                 rating = null,
                                 audioTranscript = null,
                                 duration = null
                             )
                         }
                         
                         // Reset these questions in DB
                         repositoryScope.launch {
                            resetQuestionsInDatabase(questionsPool, authHeader)
                         }
                    } else {
                        // If NEW session but reusing questions (copying), map to new objects
                        questionsPool = request.existingQuestions.map { existing ->
                             LiveQuestion(
                                liveMockInterviewId = sessionId,
                                questionText = existing.questionText,
                                category = existing.category,
                                correctAnswer = existing.correctAnswer
                            )
                        }
                    }
                } else {
                    Log.d(TAG, "Generating ${request.questionCount} questions for category=${request.interviewType.name}")
                    val questionsResult = geminiAudioService.generateAllQuestions(
                        context = buildContextStr(request),
                        category = request.interviewType.name,
                        questionCount = request.questionCount
                    )
                    Log.d(TAG, "Batch question generation result: ${if (questionsResult.isSuccess) "SUCCESS" else "FAILED"}")
    
                    if (questionsResult.isFailure) {
                        val error = questionsResult.exceptionOrNull() ?: Exception("Failed to generate questions")
                        Log.e(TAG, "Failed to generate questions", error)
                        
                        // Provide specific error message based on exception
                        val userMessage = when {
                            error.message?.contains("API key not configured") == true ->
                                "OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"
                            error.message?.contains("HTTP 401") == true || error.message?.contains("HTTP 403") == true ->
                                "AI Service authentication failed. Please check your API key in local.properties"
                            error.message?.contains("HTTP 429") == true ->
                                "AI Service rate limit exceeded. Please try again later"
                            error.message?.contains("HTTP 5") == true ->
                                "AI Service server error. Please try again later"
                            else -> "Failed to generate interview questions: ${error.message}"
                        }
    
                        return@withContext Result.failure(Exception(userMessage))
                    }
                    
                    val questionResults = questionsResult.getOrNull()!!
                    
                    questionsPool = questionResults.map { questionData ->
                        LiveQuestion(
                            liveMockInterviewId = sessionId,
                            questionText = questionData.question,
                            category = request.interviewType.name,
                            correctAnswer = questionData.idealAnswer
                        )
                    }
                }
                
                // Convert to LiveQuestion objects and save to database in background
                val liveQuestions = questionsPool

                // Save all questions to database in background (fire and forget)
                // ONLY if it is a new session. If reusing old session, we already reset them above.
                if (request.sessionId == null) {
                    repositoryScope.launch {
                        try {
                            saveAllQuestionsToDatabase(liveQuestions, authHeader)
                            Log.d(TAG, "All questions saved to database successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save questions to database", e)
                        }
                    }
                }

                Log.d(TAG, "Batch interview started successfully: sessionId=$sessionId, questionCount=${liveQuestions.size}")

                Result.success(BatchStartInterviewResult(
                    sessionId = sessionId, 
                    questions = liveQuestions
                ))

            } catch (e: Exception) {
                Log.e(TAG, "Exception in startBatchInterview()", e)
                Result.failure(e)
            }
        }

    // Starts live interview
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

                val sessionId: String
                
                if (request.sessionId != null) {
                    sessionId = request.sessionId
                    Log.d(TAG, "Reusing existing session ID: $sessionId")
                } else {
                    // Deduct Credit
                    com.phamnhantucode.aicareercoach.data.neon.NeonUserService.deductCredit(neonUser.id, 1, "Live Interview Session", authHeader)
    
                    // Use job title as role if available, otherwise fallback to interview type name
                    val sessionRole = if (request.jobTitle.isNotBlank()) request.jobTitle else request.interviewType.name
                    val sessionDesc = "Mock interview for $sessionRole"

                    // Create new interview session in database
                    Log.d(TAG, "Creating interview session for user=${neonUser.id}, role=$sessionRole, yoes=${request.experienceLevel ?: 0}")
                    val newSessionId = createInterviewSession(
                        userId = neonUser.id,
                        authHeader = authHeader,
                        role = sessionRole,
                        description = sessionDesc,
                        yoes = request.experienceLevel ?: 0
                    )
                    Log.d(TAG, "Interview session created: ${if (newSessionId != null) "SUCCESS (id=$newSessionId)" else "NULL (FAILED)"}")
                    if (newSessionId == null) {
                        Log.e(TAG, "Failed to create interview session in database")
                        return@withContext Result.failure(Exception("Failed to create interview session"))
                    }
                    sessionId = newSessionId
                }

                // Generate first question OR reuse existing
                val firstQuestion: LiveQuestion

                if (request.existingQuestions != null && request.existingQuestions.isNotEmpty()) {
                     Log.d(TAG, "Reusing existing question for first question")
                     val existing = request.existingQuestions.first()
                     
                     if (request.sessionId != null) {
                         // Reusing existing session - just reset the object state locally
                         // We reset the DB for ALL questions below if needed, or individually
                         // For immediate mode, we might want to reset them all at start?
                         // Let's reset all passed questions
                         
                          repositoryScope.launch {
                             resetQuestionsInDatabase(request.existingQuestions, authHeader)
                         }
                         
                         firstQuestion = existing.copy(
                             userAnswer = null,
                             feedback = null,
                             rating = null,
                             audioTranscript = null,
                             duration = null
                         )
                     } else {
                         // New session, new question object
                         firstQuestion = LiveQuestion(
                            liveMockInterviewId = sessionId,
                            questionText = existing.questionText,
                            category = existing.category,
                            correctAnswer = existing.correctAnswer
                        )
                     }
                } else {
                    Log.d(TAG, "Generating first question for category=${request.interviewType.name}")
                    val firstQuestionResult = geminiAudioService.generateNextQuestion(
                        context = buildContextStr(request),
                        category = request.interviewType.name,
                        previousQuestions = emptyList()
                    )
                    Log.d(TAG, "First question generation result: ${if (firstQuestionResult.isSuccess) "SUCCESS" else "FAILED"}")
    
                    if (firstQuestionResult.isFailure) {
                        val error = firstQuestionResult.exceptionOrNull() ?: Exception("Failed to generate question")
                        Log.e(TAG, "Failed to generate first question", error)
    
                        // Provide specific error message based on exception
                        val userMessage = when {
                            error.message?.contains("API key not configured") == true ->
                                "OpenRouter API key is not configured. Please add OPENROUTER_API_KEY to local.properties"
                            error.message?.contains("HTTP 401") == true || error.message?.contains("HTTP 403") == true ->
                                "AI Service authentication failed. Please check your API key in local.properties"
                            error.message?.contains("HTTP 429") == true ->
                                "AI Service rate limit exceeded. Please try again later"
                            error.message?.contains("HTTP 5") == true ->
                                "AI Service server error. Please try again later"
                            else -> "Failed to generate interview question: ${error.message}"
                        }
    
                        return@withContext Result.failure(Exception(userMessage))
                    }
    
                    val questionData = firstQuestionResult.getOrNull()!!
                    firstQuestion = LiveQuestion(
                        liveMockInterviewId = sessionId,
                        questionText = questionData.question,
                        category = request.interviewType.name,
                        correctAnswer = questionData.idealAnswer
                    )
                }

                Log.d(TAG, "Live interview started successfully: sessionId=$sessionId, questionText=${firstQuestion.questionText.take(50)}...")

                Result.success(StartInterviewResult(sessionId = sessionId, firstQuestion = firstQuestion))

            } catch (e: Exception) {
                Log.e(TAG, "Exception in startLiveInterview()", e)
                Result.failure(e)
            }
        }

    // Processes answer
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
            // We use an empty request context for feedback generation as we don't have the original request here,
            // but for feedback, the strict prompt requirements are less dependent on user bio than on the Q&A itself.
            val contextStr = buildContextStr(StartLiveInterviewRequest.createEmpty())

            val feedbackResult = geminiAudioService.generateFeedback(
                context = contextStr,
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
                    context = contextStr, // Reuse context
                    category = question.category,
                    previousQuestions = previousQuestions + question.questionText
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

    // Completes interview
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

    // Retrieves interview history
    suspend fun getInterviewHistory(clerkUserId: String): Result<List<LiveMockInterviewSession>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "getInterviewHistory() called for clerkUserId=$clerkUserId")

                val authToken = NeonAuth.fetchNeonAuthToken()
                val authHeader = buildAuthorizationHeader(authToken)
                    ?: return@withContext Result.failure(Exception("Authentication failed"))

                // First, resolve the Neon User ID from the Clerk User ID
                val neonUser = fetchNeonUserProfile(clerkUserId, authHeader)
                if (neonUser == null) {
                    Log.w(TAG, "Neon user not found for Clerk ID $clerkUserId. Returning empty history.")
                    return@withContext Result.success(emptyList())
                }

                val neonUserId = neonUser.id
                Log.d(TAG, "Resolved Neon User ID: $neonUserId")

                val encodedUserId = URLEncoder.encode(neonUserId, "UTF-8")
                val url = "${BuildConfig.NEON_API_URL}/LiveMockInterview?select=*&userId=eq.$encodedUserId&order=createdAt.desc&limit=20"

                Log.d(TAG, "Fetching history from URL: $url")

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch interview history: HTTP ${response.code}, body=$responseBody")
                    throw IOException("Failed to fetch interview history: ${response.code}")
                }

                Log.d(TAG, "History fetch successful. Body length: ${responseBody?.length}")

                // Parse response (simplified, you may need to adjust based on actual API response)
                val sessions = mutableListOf<LiveMockInterviewSession>()
                val jsonArray = JSONArray(responseBody)
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    
                    // Parse dates
                    val createdAtStr = item.optString("createdAt")
                    // Robust date parsing
                    val startedAt = try {
                        if (createdAtStr.isNotEmpty()) {
                            try {
                                java.time.Instant.parse(createdAtStr).toEpochMilli()
                            } catch (e: Exception) {
                                try {
                                    java.time.OffsetDateTime.parse(createdAtStr).toInstant().toEpochMilli()
                                } catch (e2: Exception) {
                                    // Fallback for simple ISO local date time (assume UTC)
                                    java.time.LocalDateTime.parse(createdAtStr).toInstant(java.time.ZoneOffset.UTC).toEpochMilli()
                                }
                            }
                        } else null
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse date: $createdAtStr", e)
                        null 
                    }
                    
                    // Map role to InterviewType if possible, otherwise default
                    val role = item.optString("role")
                    val interviewType = try {
                        InterviewType.valueOf(role)
                    } catch (e: Exception) {
                        InterviewType.GENERAL
                    }
                    
                    val session = LiveMockInterviewSession(
                        id = item.optString("id"),
                        userId = item.optString("userId"),
                        interviewType = interviewType,
                        status = InterviewStatus.COMPLETED, // Assuming history items are past/completed
                        jobTitle = role, // Use role as job title for display
                        jobDescription = item.optString("description"),
                        startedAt = startedAt
                    )
                    sessions.add(session)
                }

                Log.d(TAG, "Parsed ${sessions.size} sessions")
                Result.success(sessions)

            } catch (e: Exception) {
                Log.e(TAG, "Error fetching interview history", e)
                Result.failure(e)
            }
        }

    // Retrieves questions for a specific session
    suspend fun getQuestionsForSession(sessionId: String): Result<List<LiveQuestion>> =
        withContext(Dispatchers.IO) {
             try {
                Log.d(TAG, "getQuestionsForSession() called for sessionId=$sessionId")
                
                val authToken = NeonAuth.fetchNeonAuthToken()
                val authHeader = buildAuthorizationHeader(authToken)
                    ?: return@withContext Result.failure(Exception("Authentication failed"))
                    
                val url = "${BuildConfig.NEON_API_URL}/LiveInterviewQuestion?select=*&liveMockInterviewId=eq.$sessionId&order=createdAt.asc"
                
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to fetch questions: HTTP ${response.code}")
                    throw IOException("Failed to fetch questions: ${response.code}")
                }
                
                val questions = mutableListOf<LiveQuestion>()
                val jsonArray = JSONArray(responseBody)
                
                for (i in 0 until jsonArray.length()) {
                    val item = jsonArray.getJSONObject(i)
                    val q = LiveQuestion(
                        id = item.optString("id"),
                        liveMockInterviewId = item.optString("liveMockInterviewId"),
                        questionText = item.getString("question"),
                        category = item.optString("category", "General"),
                        correctAnswer = item.optString("correctAnswer"),
                        userAnswer = item.optString("userAnswer").takeIf { it != "null" },
                        feedback = item.optString("feedback").takeIf { it != "null" },
                        rating = if (item.isNull("rating")) null else item.getInt("rating")
                    )
                    questions.add(q)
                }
                
                Log.d(TAG, "Fetched ${questions.size} questions for session $sessionId")
                Result.success(questions)

             } catch (e: Exception) {
                 Log.e(TAG, "Error fetching questions for session", e)
                 Result.failure(e)
             }
        }


        
    private suspend fun resetQuestionsInDatabase(questions: List<LiveQuestion>, authHeader: String) {
        questions.forEach { question ->
            try {
                // Reset fields to null/empty in DB
                val payload = JSONObject().apply {
                    put("userAnswer", JSONObject.NULL)
                    put("feedback", JSONObject.NULL)
                    put("rating", JSONObject.NULL)
                    put("updatedAt", java.time.Instant.now().toString())
                }
                
                // Using LiveInterviewQuestion endpoint with PATCH
                 val url = "${BuildConfig.NEON_API_URL}/LiveInterviewQuestion?id=eq.${question.id}"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", authHeader)
                    .addHeader("Content-Type", "application/json")
                    .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    Log.e(TAG, "Failed to reset question ${question.id}: ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error resetting question ${question.id}", e)
            }
        }
    }

    // Processes batch answers
    suspend fun processBatchAnswers(
        sessionId: String,
        questionsAndAnswers: List<Triple<LiveQuestion, String, String>> // (question, transcription, category)
    ): Result<List<LiveQuestion>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing ${questionsAndAnswers.size} answers for batch feedback")

            // Prepare data for batch feedback
            val feedbackInput = questionsAndAnswers.map { (question, transcription, _) ->
                Triple(question.questionText, transcription, question.category)
            }

            // Fetch context
            val contextStr = buildContextStr(StartLiveInterviewRequest.createEmpty())

            // Generate batch feedback
            val feedbackResult = geminiAudioService.generateBatchFeedback(contextStr, feedbackInput)
            
            if (feedbackResult.isFailure) {
                return@withContext Result.failure(
                    feedbackResult.exceptionOrNull() ?: Exception("Batch feedback generation failed")
                )
            }

            
            val feedbackList = feedbackResult.getOrNull()!!
            
            // Update questions with feedback and save to database
            // Auth headers are already available from above

            val updatedQuestions = questionsAndAnswers.mapIndexed { index, (question, transcription, _) ->
                val feedback = if (index < feedbackList.size) feedbackList[index] else null
                
                question.copy(
                    userAnswer = transcription,
                    audioTranscript = transcription,
                    feedback = feedback?.feedback,
                    rating = feedback?.rating
                ).also { updatedQuestion ->
                    val authToken = NeonAuth.fetchNeonAuthToken()
                    val authHeader = buildAuthorizationHeader(authToken)
                    if (authHeader != null) {
                        // Update question in database with feedback
                        updateQuestionInDatabase(updatedQuestion, authHeader)
                    }
                }
            }

            Log.d(TAG, "Batch feedback processing completed for ${updatedQuestions.size} questions")
            Result.success(updatedQuestions)

        } catch (e: Exception) {
            Log.e(TAG, "Error in batch answer processing", e)
            Result.failure(e)
        }
    }

    // Saves user answer
    suspend fun saveUserAnswer(
        questionId: String,
        sessionId: String,
        transcription: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authToken = NeonAuth.fetchNeonAuthToken()
            val authHeader = buildAuthorizationHeader(authToken)
                ?: return@withContext Result.failure(Exception("Authentication failed"))

            // Update question with user answer only
            updateQuestionAnswerInDatabase(questionId, transcription, authHeader)
            
            Log.d(TAG, "User answer saved for question: $questionId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error saving user answer", e)
            Result.failure(e)
        }
    }

    private suspend fun saveAllQuestionsToDatabase(questions: List<LiveQuestion>, authHeader: String) {
        questions.forEach { question ->
            try {
                saveQuestionToDatabase(question, authHeader)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save question: ${question.questionText}", e)
            }
        }
    }

    private suspend fun updateQuestionInDatabase(question: LiveQuestion, authHeader: String) {
        try {
            val payload = JSONObject().apply {
                put("userAnswer", question.userAnswer)
                put("feedback", question.feedback)
                put("rating", question.rating)
                put("updatedAt", java.time.Instant.now().toString())
            }

            val url = "${BuildConfig.NEON_API_URL}/LiveInterviewQuestion?id=eq.${question.id}"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to update question: ${response.code}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating question in database", e)
        }
    }

    private suspend fun updateQuestionAnswerInDatabase(questionId: String, userAnswer: String, authHeader: String) {
        try {
            val payload = JSONObject().apply {
                put("userAnswer", userAnswer)
                put("updatedAt", java.time.Instant.now().toString())
            }

            val url = "${BuildConfig.NEON_API_URL}/LiveInterviewQuestion?id=eq.$questionId"
            val request = Request.Builder()
                .url(url)
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", "application/json")
                .patch(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e(TAG, "Failed to update question answer: ${response.code}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error updating question answer", e)
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

    private suspend fun buildContextStr(
        request: StartLiveInterviewRequest
    ): String = withContext(Dispatchers.IO) {
         // Use only Request data (from UI inputs)
         val contextIndustry = request.industry ?: "Technology"
         val contextYoE = request.experienceLevel?.toString() ?: "0"
         val contextSkills = request.skills 
         
         // Helper to format resume into bio
         suspend fun getFormattedResume(): String {
             val resumeId = request.resumeId ?: return request.resumeContent ?: ""
             
             // If we have an ID but content is already filled, prefer content? 
             // Logic: If user specifically clicked "refresh", the ID is new. 
             // But StartLiveInterviewRequest might have content passed from a previous screen if manually copied.
             // We stick to ID if present for freshness.
             
             val repo = com.phamnhantucode.aicareercoach.data.resume.ResumeRepository.getInstance(context)
             val resume = repo.getResume(resumeId).getOrNull() ?: return request.resumeContent ?: ""
             
             // Format similar to ResumeEnhancementRepository.buildResumeText but concise
             return buildString {
                 appendLine("RESUME SUMMARY:")
                 appendLine("Profession: ${resume.personalInfo.profession}")
                 
                 if (resume.workExperiences.isNotEmpty()) {
                    appendLine("Experience:")
                    resume.workExperiences.take(3).forEach { exp ->
                        appendLine("- ${exp.jobTitle} at ${exp.company}: ${exp.responsibilities.take(2).joinToString("; ")}")
                    }
                 }
                 
                 if (resume.education.isNotEmpty()) {
                    val latestEdu = resume.education.first()
                    appendLine("Education: ${latestEdu.degree} at ${latestEdu.institution}")
                 }
                 
                 if (resume.skills.isNotEmpty()) {
                    appendLine("Skills: ${resume.skills.joinToString(", ")}")
                 }
                 
                 // Append original content if any as backup or specific notes
                 if (!request.resumeContent.isNullOrBlank()) {
                     appendLine(" Additional Notes: ${request.resumeContent}")
                 }
             }
         }

         val resumeBio = getFormattedResume()

         // Combine Job context into Bio
         var contextBio = resumeBio
         if (request.jobTitle.isNotBlank()) {
             contextBio = "Target Job: ${request.jobTitle}. $contextBio"
         }
         if (request.jobDescription.isNotBlank()) {
             contextBio = "$contextBio. Job Desc: ${request.jobDescription}"
         }
         
         return@withContext """
             CONTEXT:
             - Role/Industry: ${contextIndustry}
             - Experience: ${contextYoE} years
             - Core Skills: ${contextSkills.joinToString(", ")}
             - Background: ${contextBio}
         """.trimIndent()
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
