package com.phamnhantucode.aicareercoach.data.interview

import android.content.Context
import android.util.Base64
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.fetchToken
import com.phamnhantucode.aicareercoach.BuildConfig
import com.phamnhantucode.aicareercoach.data.local.AppDatabase
import com.phamnhantucode.aicareercoach.data.local.AssessmentCacheEntity
import com.phamnhantucode.aicareercoach.data.local.QuestionPoolDao
import com.phamnhantucode.aicareercoach.data.local.QuestionPoolEntity
import com.phamnhantucode.aicareercoach.data.local.TipsCacheEntity
import com.phamnhantucode.aicareercoach.data.local.UserProfileCacheEntity
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.text.Charsets.UTF_8
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository that orchestrates Interview Prep content:
 * - Loads user profile data from Neon
 * - Generates tailored practice material via Gemini
 * - Stores and retrieves historical assessments in Neon
 * - Caches questions locally using Room database
 */
class InterviewPrepRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(360, TimeUnit.SECONDS)
        .readTimeout(360, TimeUnit.SECONDS)
        .writeTimeout(360, TimeUnit.SECONDS)
        .build(),
    context: Context,
) {
    private val database = AppDatabase.getDatabase(context)
    private val questionPoolDao: QuestionPoolDao = database.questionPoolDao()
    private val userProfileCacheDao = database.userProfileCacheDao()
    private val assessmentCacheDao = database.assessmentCacheDao()
    private val tipsCacheDao = database.tipsCacheDao()

    suspend fun loadInterviewPrepContent(
        forceRefreshAuth: Boolean = false,
    ): InterviewPrepContent = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")

        if (BuildConfig.NEON_API_URL.isBlank()) {
            throw IllegalStateException("Neon API URL is not configured.")
        }

        var authHeader = resolveAuthorizationHeader(forceRefresh = forceRefreshAuth)
            ?: throw IllegalStateException("No Neon authentication method configured.")

        val neonUser = try {
            fetchNeonUserProfile(user.id, authHeader)
        } catch (error: IOException) {
            if (error.message?.contains("401") == true || error.message?.contains("Unauthorized") == true) {
                authHeader = resolveAuthorizationHeader(forceRefresh = true)
                    ?: throw IllegalStateException("Failed to refresh Neon authentication token.")
                fetchNeonUserProfile(user.id, authHeader)
            } else {
                throw error
            }
        }

        val assessments = fetchAssessmentsForUser(neonUser.id, authHeader)

        // Cache user profile and assessments
        cacheUserProfile(neonUser, user.id)
        cacheAssessments(neonUser.id, assessments)

        // Check question pool availability (using local Room database)
        val quizPoolCount = getUnusedQuestionsCount(neonUser.id, "quiz")
        val interviewPoolCount = getUnusedQuestionsCount(neonUser.id, "interview")

        // Generate new questions in batch if pool is low
        if (quizPoolCount < MINIMUM_POOL_SIZE || interviewPoolCount < MINIMUM_POOL_SIZE) {
            val prompt = buildGeminiPrompt(neonUser, assessments, generateBatchSize = true)
            val generated = callGemini(prompt)

            // Store generated questions in the local pool
            if (quizPoolCount < MINIMUM_POOL_SIZE) {
                storeQuestionsInPool(neonUser.id, generated.quizQuestions, "quiz")
            }
            if (interviewPoolCount < MINIMUM_POOL_SIZE) {
                storeQuestionsInPool(neonUser.id, generated.interviewQuestions, "interview")
            }
        }

        // Load questions from local pool
        val quizQuestions = fetchUnusedQuestionsFromPool(neonUser.id, "quiz", QUIZ_QUESTIONS_PER_SESSION)
        val interviewQuestions = fetchUnusedQuestionsFromPool(neonUser.id, "interview", INTERVIEW_QUESTIONS_PER_SESSION)

        // If pool is still empty (first time user), generate immediately
        val (finalQuizQuestions, finalInterviewQuestions, practiceTips, coachingNotes) = if (quizQuestions.isEmpty() || interviewQuestions.isEmpty()) {
            val prompt = buildGeminiPrompt(neonUser, assessments, generateBatchSize = true)
            val generated = callGemini(prompt)

            storeQuestionsInPool(neonUser.id, generated.quizQuestions, "quiz")
            storeQuestionsInPool(neonUser.id, generated.interviewQuestions, "interview")

            val quiz = fetchUnusedQuestionsFromPool(neonUser.id, "quiz", QUIZ_QUESTIONS_PER_SESSION)
            val interview = fetchUnusedQuestionsFromPool(neonUser.id, "interview", INTERVIEW_QUESTIONS_PER_SESSION)

            QuestionBundle(quiz, interview, generated.practiceTips, generated.coachingNotes)
        } else {
            // Try to load cached tips first, otherwise generate new ones
            val tipsAndNotes = getCachedTips(neonUser.id) ?: run {
                val generated = loadTipsAndCoachingNotes(neonUser, assessments)
                cacheTips(neonUser.id, generated.first, generated.second)
                generated
            }
            QuestionBundle(quizQuestions, interviewQuestions, tipsAndNotes.first, tipsAndNotes.second)
        }

        // Cache tips if they were generated for first-time users
        if (quizQuestions.isEmpty() || interviewQuestions.isEmpty()) {
            cacheTips(neonUser.id, practiceTips, coachingNotes)
        }

        return@withContext InterviewPrepContent(
            quizQuestions = finalQuizQuestions,
            interviewQuestions = finalInterviewQuestions,
            practiceTips = practiceTips,
            coachingNotes = coachingNotes,
            neonUser = neonUser,
            assessments = assessments,
        )
    }

    private data class QuestionBundle(
        val quizQuestions: List<RepositoryQuestionSnapshot>,
        val interviewQuestions: List<RepositoryQuestionSnapshot>,
        val practiceTips: List<PracticeTipSpec>,
        val coachingNotes: CoachingNotes?
    )

    suspend fun recordQuizAttempt(
        quizScore: Int,
        questions: List<RepositoryQuestionSnapshot>,
        improvementTip: String?,
    ) = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")
        val authHeader = resolveAuthorizationHeader()
            ?: throw IllegalStateException("No Neon authentication method configured.")
        val neonUser = fetchNeonUserProfile(user.id, authHeader)
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')

        val payload = JSONObject().apply {
            // Generate a random hex ID similar to what the database would generate
            val randomBytes = ByteArray(12)
            java.security.SecureRandom().nextBytes(randomBytes)
            val hexId = randomBytes.joinToString("") { "%02x".format(it) }
            put("id", hexId)
            put("userId", neonUser.id)
            put("quizScore", quizScore)
            put("category", "quiz")
            put("improvementTip", improvementTip ?: JSONObject.NULL)
            put(
                "questions",
                JSONArray().apply {
                    questions.forEach { snapshot ->
                        put(snapshot.toJson())
                    }
                }
            )
            val now = Instant.now().toString()
            put("createdAt", now)
            put("updatedAt", now)
        }

        val request =
            Request.Builder()
                .url("$apiUrl/Assessment")
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", JSON_MEDIA_TYPE)
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Failed to record quiz attempt (${response.code}): $bodyString")
            }
        }
    }

    suspend fun recordInterviewAttempt(
        quizScore: Int,
        questions: List<RepositoryQuestionSnapshot>,
        improvementTip: String?,
    ) = withContext(Dispatchers.IO) {
        val user = Clerk.user
            ?: throw IllegalStateException("User session unavailable. Please sign in again.")
        val authHeader = resolveAuthorizationHeader()
            ?: throw IllegalStateException("No Neon authentication method configured.")
        val neonUser = fetchNeonUserProfile(user.id, authHeader)
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')

        val payload = JSONObject().apply {
            // Generate a random hex ID similar to what the database would generate
            val randomBytes = ByteArray(12)
            java.security.SecureRandom().nextBytes(randomBytes)
            val hexId = randomBytes.joinToString("") { "%02x".format(it) }
            put("id", hexId)
            put("userId", neonUser.id)
            put("quizScore", quizScore)
            put("category", "interview")
            put("improvementTip", improvementTip ?: JSONObject.NULL)
            put(
                "questions",
                JSONArray().apply {
                    questions.forEach { snapshot ->
                        put(snapshot.toJson())
                    }
                }
            )
            val now = Instant.now().toString()
            put("createdAt", now)
            put("updatedAt", now)
        }

        val request =
            Request.Builder()
                .url("$apiUrl/Assessment")
                .addHeader("Authorization", authHeader)
                .addHeader("Content-Type", JSON_MEDIA_TYPE)
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Failed to record interview attempt (${response.code}): $bodyString")
            }
        }
    }

    suspend fun getAuthorizationHeader(): String? {
        return resolveAuthorizationHeader(forceRefresh = false)
    }

    private suspend fun resolveAuthorizationHeader(forceRefresh: Boolean = false): String? {
        val bearer = if (forceRefresh) {
            fetchClerkSessionToken()
        } else {
            fetchClerkSessionToken()
                ?: BuildConfig.NEON_API_KEY.takeUnless { it.isBlank() }
        }

        val basicAuth = BuildConfig.NEON_DB_ROLE.takeUnless { it.isBlank() }?.let { role ->
            val password = BuildConfig.NEON_DB_PASSWORD.takeUnless { it.isBlank() } ?: return@let null
            val credentials = "$role:$password"
            val encoded = Base64.encodeToString(credentials.toByteArray(UTF_8), Base64.NO_WRAP)
            "Basic $encoded"
        }

        return when {
            bearer != null -> "Bearer $bearer"
            basicAuth != null -> basicAuth
            else -> null
        }
    }

    private suspend fun fetchClerkSessionToken(): String? {
        val session = Clerk.session ?: return null
        return when (val result = session.fetchToken()) {
            is ClerkResult.Success -> result.value.jwt.takeUnless { it.isBlank() }
            is ClerkResult.Failure -> {
                Log.w(TAG, "Failed to fetch fresh Clerk token: ${result.error}")
                session.lastActiveToken?.jwt?.takeUnless { it.isBlank() }
            }
            else -> null
        }
    }

    private fun fetchNeonUserProfile(
        clerkUserId: String,
        authorizationHeader: String,
    ): NeonUserProfile {
        val encodedClerkId = URLEncoder.encode(clerkUserId, UTF_8.name())
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val userRequestUrl =
            "$apiUrl/User?select=id,industry,skills,bio,experience&clerkUserId=eq.$encodedClerkId&limit=1"

        val userRequest =
            Request.Builder()
                .url(userRequestUrl)
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

        client.newCall(userRequest).execute().use { response ->
            val bodyString = response.body?.string()
                ?: throw IOException("Neon user fetch returned an empty body.")
            if (!response.isSuccessful) {
                throw IOException("Neon user fetch failed (${response.code}): $bodyString")
            }

            val results = JSONArray(bodyString)
            if (results.length() == 0) {
                throw IllegalStateException("No Neon user record found. Complete onboarding first.")
            }
            val json = results.getJSONObject(0)
            val skillsJson = json.optJSONArray("skills") ?: JSONArray()
            val skills = List(skillsJson.length()) { index ->
                skillsJson.optString(index)
            }.filter { it.isNotBlank() }
            return NeonUserProfile(
                id = json.optString("id").takeIf { it.isNotBlank() }
                    ?: throw IOException("Neon user record missing id."),
                industry = json.optString("industry").takeIf { it.isNotBlank() },
                experienceYears = json.optInt("experience").takeUnless { json.isNull("experience") },
                skills = skills,
                bio = json.optString("bio").takeIf { it.isNotBlank() },
            )
        }
    }

    private fun fetchAssessmentsForUser(
        neonUserId: String,
        authorizationHeader: String,
    ): List<AssessmentRecord> {
        val encodedUserId = URLEncoder.encode(neonUserId, UTF_8.name())
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val requestUrl =
            "$apiUrl/Assessment?select=id,quizScore,questions,category,improvementTip,createdAt&userId=eq.$encodedUserId&order=createdAt.desc&limit=20"

        val request =
            Request.Builder()
                .url(requestUrl)
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

        return client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Failed to load assessments (${response.code}): $bodyString")
            }

            if (bodyString.isBlank()) return@use emptyList()
            val results = JSONArray(bodyString)
            List(results.length()) { index ->
                parseAssessment(results.getJSONObject(index))
            }
        }
    }

    private fun parseAssessment(json: JSONObject): AssessmentRecord {
        val questionsArray = json.optJSONArray("questions") ?: JSONArray()
        val questions = List(questionsArray.length()) { idx ->
            val q = questionsArray.optJSONObject(idx) ?: JSONObject()
            RepositoryQuestionSnapshot(
                id = q.optString("id").takeUnless { it.isBlank() } ?: "question_$idx",
                question = q.optString("question"),
                category = q.optString("category").takeUnless { it.isBlank() },
                type = q.optString("type").takeUnless { it.isBlank() },
                options = q.optJSONArray("options").toStringList(),
                correctAnswerIndex = q.optInt("correctAnswerIndex").takeUnless { q.isNull("correctAnswerIndex") },
                selectedAnswerIndex = q.optInt("selectedAnswerIndex").takeUnless { q.isNull("selectedAnswerIndex") },
                explanation = q.optString("explanation").takeUnless { it.isBlank() },
                essayResponse = q.optString("essayResponse").takeUnless { it.isBlank() },
            )
        }

        return AssessmentRecord(
            id = json.optString("id"),
            createdAt = json.optString("createdAt").toInstantOrEpoch(),
            quizScore = json.optDouble("quizScore", 0.0),
            category = json.optString("category").lowercase(Locale.US),
            improvementTip = json.optString("improvementTip").takeUnless { it.isBlank() },
            questions = questions,
        )
    }

    private suspend fun getUnusedQuestionsCount(
        userId: String,
        category: String,
    ): Int = withContext(Dispatchers.IO) {
        return@withContext try {
            questionPoolDao.getUnusedQuestionsCount(userId, category)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to count unused questions", e)
            0
        }
    }

    private suspend fun fetchUnusedQuestionsFromPool(
        userId: String,
        category: String,
        limit: Int,
    ): List<RepositoryQuestionSnapshot> = withContext(Dispatchers.IO) {
        return@withContext try {
            questionPoolDao.getUnusedQuestions(userId, category, limit).map { entity ->
                RepositoryQuestionSnapshot(
                    id = entity.id,
                    question = entity.question,
                    category = entity.questionCategory,
                    type = entity.questionType,
                    options = entity.options ?: emptyList(),
                    correctAnswerIndex = entity.correctAnswerIndex,
                    explanation = entity.explanation,
                    placeholder = entity.placeholder
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch questions from pool", e)
            emptyList()
        }
    }

    private suspend fun storeQuestionsInPool(
        userId: String,
        questions: List<RepositoryQuestionSnapshot>,
        category: String,
    ) = withContext(Dispatchers.IO) {
        if (questions.isEmpty()) return@withContext

        try {
            val entities = questions.map { question ->
                QuestionPoolEntity(
                    id = question.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString(),
                    userId = userId,
                    category = category,
                    questionType = question.type ?: "MULTIPLE_CHOICE",
                    questionCategory = question.category,
                    question = question.question,
                    options = question.options,
                    correctAnswerIndex = question.correctAnswerIndex,
                    explanation = question.explanation,
                    placeholder = question.placeholder,
                    isUsed = false,
                    createdAt = System.currentTimeMillis()
                )
            }
            questionPoolDao.insertQuestions(entities)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to store questions in pool", e)
        }
    }

    suspend fun markQuestionsAsUsed(
        questionIds: List<String>,
    ) = withContext(Dispatchers.IO) {
        if (questionIds.isEmpty()) return@withContext

        try {
            questionPoolDao.markQuestionsAsUsed(questionIds)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to mark questions as used", e)
        }
    }

    /**
     * Loads cached interview prep content without making network calls.
     * Returns null if no cached data is available.
     */
    suspend fun loadCachedContent(): InterviewPrepContent? = withContext(Dispatchers.IO) {
        try {
            val user = Clerk.user ?: return@withContext null

            // Try to get cached user profile by clerk user ID
            val userProfileCache = userProfileCacheDao.getUserProfileByClerkId(user.id) ?: return@withContext null
            val cachedProfile = NeonUserProfile(
                id = userProfileCache.userId,
                industry = userProfileCache.industry,
                experienceYears = userProfileCache.experienceYears,
                skills = userProfileCache.skills,
                bio = userProfileCache.bio
            )

            // Load cached assessments
            val cachedAssessments = getCachedAssessments(cachedProfile.id) ?: emptyList()

            // Load cached tips
            val cachedTipsAndNotes = getCachedTips(cachedProfile.id)

            // Load questions from pool
            val quizQuestions = fetchUnusedQuestionsFromPool(cachedProfile.id, "quiz", QUIZ_QUESTIONS_PER_SESSION)
            val interviewQuestions = fetchUnusedQuestionsFromPool(cachedProfile.id, "interview", INTERVIEW_QUESTIONS_PER_SESSION)

            // If we have no questions cached, return null to force refresh
            if (quizQuestions.isEmpty() && interviewQuestions.isEmpty()) {
                return@withContext null
            }

            InterviewPrepContent(
                quizQuestions = quizQuestions,
                interviewQuestions = interviewQuestions,
                practiceTips = cachedTipsAndNotes?.first ?: emptyList(),
                coachingNotes = cachedTipsAndNotes?.second,
                neonUser = cachedProfile,
                assessments = cachedAssessments
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached content", e)
            null
        }
    }

    /**
     * Fetches and caches user data silently in the background.
     * Used after login or for background refreshes.
     */
    suspend fun fetchAndCacheUserData() = withContext(Dispatchers.IO) {
        try {
            val user = Clerk.user ?: return@withContext
            if (BuildConfig.NEON_API_URL.isBlank() || BuildConfig.GEMINI_API_KEY.isBlank()) {
                return@withContext
            }

            val authHeader = resolveAuthorizationHeader(forceRefresh = false) ?: return@withContext

            // Fetch user profile
            val neonUser = try {
                fetchNeonUserProfile(user.id, authHeader)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to fetch user profile for caching", error)
                return@withContext
            }

            // Cache user profile
            cacheUserProfile(neonUser, user.id)

            // Fetch and cache assessments
            try {
                val assessments = fetchAssessmentsForUser(neonUser.id, authHeader)
                cacheAssessments(neonUser.id, assessments)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to fetch assessments for caching", error)
            }

            // Generate and cache tips if needed
            try {
                val assessments = getCachedAssessments(neonUser.id) ?: emptyList()
                val (tips, notes) = loadTipsAndCoachingNotes(neonUser, assessments)
                cacheTips(neonUser.id, tips, notes)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to generate and cache tips", error)
            }

            Log.d(TAG, "Successfully fetched and cached user data")
        } catch (error: Exception) {
            Log.w(TAG, "Failed to fetch and cache user data", error)
        }
    }

    // Cache methods for user profile
    private suspend fun getCachedUserProfile(userId: String): NeonUserProfile? = withContext(Dispatchers.IO) {
        try {
            val cached = userProfileCacheDao.getUserProfile(userId) ?: return@withContext null
            NeonUserProfile(
                id = cached.userId,
                industry = cached.industry,
                experienceYears = cached.experienceYears,
                skills = cached.skills,
                bio = cached.bio
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached user profile", e)
            null
        }
    }

    private suspend fun cacheUserProfile(profile: NeonUserProfile, clerkUserId: String) = withContext(Dispatchers.IO) {
        try {
            val entity = UserProfileCacheEntity(
                userId = profile.id,
                clerkUserId = clerkUserId,
                industry = profile.industry,
                experienceYears = profile.experienceYears,
                skills = profile.skills,
                bio = profile.bio,
                cachedAt = System.currentTimeMillis()
            )
            userProfileCacheDao.insertUserProfile(entity)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache user profile", e)
        }
    }

    // Cache methods for assessments
    private suspend fun getCachedAssessments(userId: String): List<AssessmentRecord>? = withContext(Dispatchers.IO) {
        try {
            val cached = assessmentCacheDao.getAssessmentsByUser(userId)
            if (cached.isEmpty()) return@withContext null

            cached.map { entity ->
                AssessmentRecord(
                    id = entity.id,
                    createdAt = Instant.ofEpochMilli(entity.createdAt),
                    quizScore = entity.quizScore,
                    category = entity.category,
                    improvementTip = entity.improvementTip,
                    questions = emptyList() // We don't cache full question details for assessments
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached assessments", e)
            null
        }
    }

    private suspend fun cacheAssessments(userId: String, assessments: List<AssessmentRecord>) = withContext(Dispatchers.IO) {
        try {
            val entities = assessments.map { assessment ->
                AssessmentCacheEntity(
                    id = assessment.id,
                    userId = userId,
                    createdAt = assessment.createdAt.toEpochMilli(),
                    quizScore = assessment.quizScore,
                    category = assessment.category,
                    improvementTip = assessment.improvementTip,
                    cachedAt = System.currentTimeMillis()
                )
            }
            assessmentCacheDao.insertAssessments(entities)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache assessments", e)
        }
    }

    // Cache methods for tips and coaching notes
    private suspend fun getCachedTips(userId: String): Pair<List<PracticeTipSpec>, CoachingNotes?>? = withContext(Dispatchers.IO) {
        try {
            val cached = tipsCacheDao.getTips(userId) ?: return@withContext null

            val tips = JSONArray(cached.practiceTipsJson).let { array ->
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    PracticeTipSpec(
                        category = obj.getString("category"),
                        icon = obj.getString("icon"),
                        tips = obj.getJSONArray("tips").let { tipsArr ->
                            (0 until tipsArr.length()).map { j -> tipsArr.getString(j) }
                        },
                        color = obj.getString("color")
                    )
                }
            }

            val coaching = if (cached.coachingSummary != null || cached.improvementAreas.isNotEmpty()) {
                CoachingNotes(
                    summary = cached.coachingSummary,
                    improvementAreas = cached.improvementAreas,
                    recommendedPracticeFrequency = cached.recommendedPracticeFrequency
                )
            } else null

            Pair(tips, coaching)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load cached tips", e)
            null
        }
    }

    private suspend fun cacheTips(
        userId: String,
        tips: List<PracticeTipSpec>,
        coachingNotes: CoachingNotes?
    ) = withContext(Dispatchers.IO) {
        try {
            val tipsJson = JSONArray().apply {
                tips.forEach { tip ->
                    put(JSONObject().apply {
                        put("category", tip.category)
                        put("icon", tip.icon)
                        put("tips", JSONArray(tip.tips))
                        put("color", tip.color)
                    })
                }
            }.toString()

            val entity = TipsCacheEntity(
                userId = userId,
                practiceTipsJson = tipsJson,
                coachingSummary = coachingNotes?.summary,
                improvementAreas = coachingNotes?.improvementAreas ?: emptyList(),
                recommendedPracticeFrequency = coachingNotes?.recommendedPracticeFrequency,
                cachedAt = System.currentTimeMillis()
            )
            tipsCacheDao.insertTips(entity)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cache tips", e)
        }
    }

    private suspend fun loadTipsAndCoachingNotes(
        profile: NeonUserProfile,
        assessments: List<AssessmentRecord>,
    ): Pair<List<PracticeTipSpec>, CoachingNotes?> = withContext(Dispatchers.IO) {
        val prompt = buildTipsPrompt(profile, assessments)
        val generated = callGeminiForTips(prompt)
        Pair(generated.first, generated.second)
    }

    private fun buildGeminiPrompt(
        profile: NeonUserProfile,
        assessments: List<AssessmentRecord>,
        generateBatchSize: Boolean = false,
    ): String {
        val experienceText = profile.experienceYears?.let { "$it years of experience" } ?: "experience not provided"
        val skillsText = if (profile.skills.isEmpty()) "skills not provided" else profile.skills.joinToString()
        val industryText = profile.industry ?: "unspecified industry"
        val recentScores = if (assessments.isEmpty()) {
            "no assessments recorded yet"
        } else {
            assessments.take(5).joinToString { score ->
                "${score.quizScore.roundToInt()} (${score.category})"
            }
        }

        val recurringGaps = assessments
            .flatMap { record ->
                record.improvementTip?.let { listOf(it) } ?: emptyList()
            }
            .takeIf { it.isNotEmpty() }
            ?.joinToString()
            ?: "none observed"

        val essayPracticeCount = assessments.sumOf { record ->
            record.questions.count { snapshot ->
                snapshot.type.equals("ESSAY", ignoreCase = true)
            }
        }

        return """
            You are an expert technical interview coach. Create targeted interview preparation for the following professional:
            - Industry: $industryText
            - Experience: $experienceText
            - Skills: $skillsText
            - Bio: ${profile.bio ?: "Not provided"}
            - Recent scores: $recentScores
            - Recurring improvement themes: $recurringGaps
            - Essay responses completed so far: $essayPracticeCount

            Produce STRICT JSON with the following structure and nothing else:
            {
              "quizQuestions": [
                {
                  "id": "unique string identifier",
                  "type": "MULTIPLE_CHOICE",
                  "category": "TECHNICAL" | "BEHAVIORAL" | "SITUATIONAL",
                  "question": "question text",
                  "options": ["A", "B", "C", "D"],
                  "correctAnswerIndex": number,
                  "explanation": "why this answer is correct"
                }
              ],
              "interviewQuestions": [
                {
                  "id": "unique string identifier",
                  "type": "MULTIPLE_CHOICE" | "ESSAY",
                  "category": "TECHNICAL" | "BEHAVIORAL" | "SITUATIONAL",
                  "question": "prompt text",
                  "options": ["only include for multiple choice"],
                  "correctAnswerIndex": number | null,
                  "explanation": "short coaching note or sample approach",
                  "placeholder": "short writing guidance for essay questions"
                }
              ],
              "practiceTips": [
                {
                  "category": "short label",
                  "icon": "one of: lightbulb, target, chat, rocket, tools, book, graph",
                  "color": "#RRGGBB",
                  "tips": ["bullet tip 1", "bullet tip 2", "bullet tip 3"]
                }
              ],
              "coachingNotes": {
                "summary": "2 sentence overview tailored to the user",
                "improvementAreas": ["focus area 1", "focus area 2"],
                "recommendedPracticeFrequency": "short recommendation like '3 sessions per week'"
              }
            }

            Requirements:
            - Provide at least ${if (generateBatchSize) BATCH_QUIZ_SIZE else 10} quizQuestions.
            - ALL quizQuestions MUST be MULTIPLE_CHOICE type only (no ESSAY questions in quizQuestions).
            - Provide at least ${if (generateBatchSize) BATCH_INTERVIEW_SIZE else 4} interviewQuestions with at least ${if (generateBatchSize) BATCH_INTERVIEW_SIZE / 2 else 2} essay prompts.
            - All JSON strings must escape quotes properly.
            - Return ONLY the JSON object without Markdown or commentary.
            ${if (generateBatchSize) "- Generate diverse questions covering different topics and difficulty levels." else ""}
        """.trimIndent()
    }

    private fun buildTipsPrompt(
        profile: NeonUserProfile,
        assessments: List<AssessmentRecord>,
    ): String {
        val experienceText = profile.experienceYears?.let { "$it years of experience" } ?: "experience not provided"
        val skillsText = if (profile.skills.isEmpty()) "skills not provided" else profile.skills.joinToString()
        val industryText = profile.industry ?: "unspecified industry"
        val recentScores = if (assessments.isEmpty()) {
            "no assessments recorded yet"
        } else {
            assessments.take(5).joinToString { score ->
                "${score.quizScore.roundToInt()} (${score.category})"
            }
        }

        return """
            You are an expert technical interview coach. Create practice tips and coaching notes for the following professional:
            - Industry: $industryText
            - Experience: $experienceText
            - Skills: $skillsText
            - Recent scores: $recentScores

            Produce STRICT JSON with the following structure and nothing else:
            {
              "practiceTips": [
                {
                  "category": "short label",
                  "icon": "one of: lightbulb, target, chat, rocket, tools, book, graph",
                  "color": "#RRGGBB",
                  "tips": ["bullet tip 1", "bullet tip 2", "bullet tip 3"]
                }
              ],
              "coachingNotes": {
                "summary": "2 sentence overview tailored to the user",
                "improvementAreas": ["focus area 1", "focus area 2"],
                "recommendedPracticeFrequency": "short recommendation like '3 sessions per week'"
              }
            }

            - All JSON strings must escape quotes properly.
            - Return ONLY the JSON object without Markdown or commentary.
        """.trimIndent()
    }

    private suspend fun callGeminiForTips(prompt: String): Pair<List<PracticeTipSpec>, CoachingNotes?> {
        val messages = listOf(
            com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message(role = "user", content = prompt)
        )
        
        val rawText = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(messages)

        val cleaned = CODE_FENCE_REGEX.replace(rawText, "").trim()
        val json = try {
            JSONObject(cleaned)
        } catch (error: Exception) {
            throw IOException("AI Service returned invalid JSON: ${error.message}\n$cleaned", error)
        }

        val practiceTips = json.optJSONArray("practiceTips").toTipSpecs()
        val coachingNotes =
            json.optJSONObject("coachingNotes")?.let { notes ->
                CoachingNotes(
                    summary = notes.optString("summary").takeUnless { it.isBlank() },
                    improvementAreas = notes.optJSONArray("improvementAreas").toStringList(),
                    recommendedPracticeFrequency = notes.optString("recommendedPracticeFrequency")
                        .takeUnless { it.isBlank() }
                )
            }

        return Pair(practiceTips, coachingNotes)
    }

    private suspend fun callGemini(prompt: String): GeminiInterviewBundle {
        val messages = listOf(
            com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message(role = "user", content = prompt)
        )

        // Increase timeout for batch generation if needed implicitly handled by OpenRouterService default or we can add param
        val rawText = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(messages)

        val cleaned = CODE_FENCE_REGEX.replace(rawText, "").trim()
        val json = try {
            JSONObject(cleaned)
        } catch (error: Exception) {
            throw IOException("AI Service returned invalid JSON: ${error.message}\n$cleaned", error)
        }

        val quizQuestions = json.optJSONArray("quizQuestions").toQuestionSpecs()
        val interviewQuestions = json.optJSONArray("interviewQuestions").toQuestionSpecs()
        val practiceTips = json.optJSONArray("practiceTips").toTipSpecs()
        val coachingNotes =
            json.optJSONObject("coachingNotes")?.let { notes ->
                CoachingNotes(
                    summary = notes.optString("summary").takeUnless { it.isBlank() },
                    improvementAreas = notes.optJSONArray("improvementAreas").toStringList(),
                    recommendedPracticeFrequency = notes.optString("recommendedPracticeFrequency")
                        .takeUnless { it.isBlank() }
                )
            }

        return GeminiInterviewBundle(
            quizQuestions = quizQuestions,
            interviewQuestions = interviewQuestions,
            practiceTips = practiceTips,
            coachingNotes = coachingNotes,
        )
    }

    // specific extractGeminiText is no longer needed as OpenRouterService handles it, 
    // but we can leave it or remove it. It's safe to remove if unused.
    // However, I will just remove the private method below via a separate chunk or let it be dead code for a moment if I don't select it.
    // Actually, I'll remove it to be clean.



    private fun JSONArray?.toQuestionSpecs(): List<RepositoryQuestionSnapshot> {
        if (this == null || length() == 0) return emptyList()
        return List(length()) { index ->
            val json = optJSONObject(index) ?: JSONObject()
            val options = json.optJSONArray("options").toStringList()
            RepositoryQuestionSnapshot(
                id = json.optString("id").takeUnless { it.isBlank() } ?: "question_${index + 1}",
                question = json.optString("question"),
                category = json.optString("category").takeUnless { it.isBlank() },
                type = json.optString("type").takeUnless { it.isBlank() },
                options = options,
                correctAnswerIndex = json.optInt("correctAnswerIndex").takeUnless { json.isNull("correctAnswerIndex") },
                explanation = json.optString("explanation").takeUnless { it.isBlank() },
                placeholder = json.optString("placeholder").takeUnless { it.isBlank() },
            )
        }
    }

    private fun JSONArray?.toTipSpecs(): List<PracticeTipSpec> {
        if (this == null || length() == 0) return emptyList()
        return List(length()) { index ->
            val json = optJSONObject(index) ?: JSONObject()
            PracticeTipSpec(
                category = json.optString("category").takeUnless { it.isBlank() } ?: "Coaching Tip",
                icon = json.optString("icon").takeUnless { it.isBlank() } ?: "lightbulb",
                color = json.optString("color").takeUnless { it.isBlank() } ?: "#4F46E5",
                tips = json.optJSONArray("tips").toStringList().ifEmpty {
                    listOf(json.optString("summary").takeUnless { it.isBlank() } ?: "Stay consistent with practice.")
                }
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null || length() == 0) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            val value = optString(i)
            if (!value.isNullOrBlank()) list.add(value)
        }
        return list
    }

    private fun RepositoryQuestionSnapshot.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("question", question)
            put("category", category ?: JSONObject.NULL)
            put("type", type ?: JSONObject.NULL)
            put("options", JSONArray(options))
            if (correctAnswerIndex != null) put("correctAnswerIndex", correctAnswerIndex) else put("correctAnswerIndex", JSONObject.NULL)
            if (selectedAnswerIndex != null) put("selectedAnswerIndex", selectedAnswerIndex) else put("selectedAnswerIndex", JSONObject.NULL)
            if (explanation != null) put("explanation", explanation) else put("explanation", JSONObject.NULL)
            if (placeholder != null) put("placeholder", placeholder) else put("placeholder", JSONObject.NULL)
            if (essayResponse != null) put("essayResponse", essayResponse) else put("essayResponse", JSONObject.NULL)
        }
    }

    private fun String.toInstantOrEpoch(): Instant {
        if (isBlank()) return Instant.EPOCH
        return runCatching { Instant.parse(this) }
            .recoverCatching { OffsetDateTime.parse(this).toInstant() }
            .getOrElse { Instant.EPOCH }
    }

    data class InterviewPrepContent(
        val quizQuestions: List<RepositoryQuestionSnapshot>,
        val interviewQuestions: List<RepositoryQuestionSnapshot>,
        val practiceTips: List<PracticeTipSpec>,
        val coachingNotes: CoachingNotes?,
        val neonUser: NeonUserProfile,
        val assessments: List<AssessmentRecord>,
    )

    data class RepositoryQuestionSnapshot(
        val id: String,
        val question: String,
        val category: String?,
        val type: String?,
        val options: List<String> = emptyList(),
        val correctAnswerIndex: Int? = null,
        val selectedAnswerIndex: Int? = null,
        val explanation: String? = null,
        val placeholder: String? = null,
        val essayResponse: String? = null,
    )

    data class PracticeTipSpec(
        val category: String,
        val icon: String,
        val color: String,
        val tips: List<String>,
    )

    data class CoachingNotes(
        val summary: String?,
        val improvementAreas: List<String>,
        val recommendedPracticeFrequency: String?,
    )

    data class NeonUserProfile(
        val id: String,
        val industry: String?,
        val experienceYears: Int?,
        val skills: List<String>,
        val bio: String?,
    )

    data class AssessmentRecord(
        val id: String,
        val createdAt: Instant,
        val quizScore: Double,
        val category: String,
        val improvementTip: String?,
        val questions: List<RepositoryQuestionSnapshot>,
    )

    private data class GeminiInterviewBundle(
        val quizQuestions: List<RepositoryQuestionSnapshot>,
        val interviewQuestions: List<RepositoryQuestionSnapshot>,
        val practiceTips: List<PracticeTipSpec>,
        val coachingNotes: CoachingNotes?,
    )

    /**
     * Pre-loads quiz and interview question pools in the background after user info is fetched.
     * This helps improve UX by having questions ready when the user wants to start.
     */
    suspend fun preloadQuestionPools() = withContext(Dispatchers.IO) {
        try {
            val user = Clerk.user ?: return@withContext
            if (BuildConfig.NEON_API_URL.isBlank() || BuildConfig.GEMINI_API_KEY.isBlank()) {
                return@withContext
            }

            val authHeader = resolveAuthorizationHeader(forceRefresh = false) ?: return@withContext
            val neonUser = try {
                fetchNeonUserProfile(user.id, authHeader)
            } catch (error: Exception) {
                Log.w(TAG, "Failed to fetch user profile for preloading", error)
                return@withContext
            }

            // Check question pool availability
            val quizPoolCount = getUnusedQuestionsCount(neonUser.id, "quiz")
            val interviewPoolCount = getUnusedQuestionsCount(neonUser.id, "interview")

            // Generate new questions in batch if pool is low
            if (quizPoolCount < MINIMUM_POOL_SIZE || interviewPoolCount < MINIMUM_POOL_SIZE) {
                val assessments = try {
                    fetchAssessmentsForUser(neonUser.id, authHeader)
                } catch (error: Exception) {
                    Log.w(TAG, "Failed to fetch assessments for preloading", error)
                    emptyList()
                }

                val prompt = buildGeminiPrompt(neonUser, assessments, generateBatchSize = true)
                val generated = callGemini(prompt)

                // Store generated questions in the local pool
                if (quizPoolCount < MINIMUM_POOL_SIZE) {
                    storeQuestionsInPool(neonUser.id, generated.quizQuestions, "quiz")
                }
                if (interviewPoolCount < MINIMUM_POOL_SIZE) {
                    storeQuestionsInPool(neonUser.id, generated.interviewQuestions, "interview")
                }
                Log.d(TAG, "Preloaded question pools successfully")
            }
        } catch (error: Exception) {
            Log.w(TAG, "Failed to preload question pools", error)
        }
    }

    companion object {
        private const val TAG = "InterviewPrepRepo"
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        private val CODE_FENCE_REGEX = Regex("```(?:json)?")

        // Question pool configuration

        // Question pool configuration
        private const val MINIMUM_POOL_SIZE = 6 // Trigger batch generation when below this (30% of 20 = ~70-80% used)
        private const val BATCH_QUIZ_SIZE = 20 // Generate 20 quiz questions per batch
        private const val BATCH_INTERVIEW_SIZE = 4 // Generate 4 interview questions per batch
        private const val QUIZ_QUESTIONS_PER_SESSION = 10 // Show 10 questions per quiz
        private const val INTERVIEW_QUESTIONS_PER_SESSION = 4 // Show 4 questions per interview
    }
}
