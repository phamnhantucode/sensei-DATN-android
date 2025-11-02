package com.phamnhantucode.aicareercoach.data.industry

import android.util.Base64
import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.session.fetchToken
import com.phamnhantucode.aicareercoach.BuildConfig
import java.io.IOException
import java.net.URLEncoder
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
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
 * Repository responsible for orchestrating Industry Insight retrieval.
 * It prefers existing Neon records and falls back to Gemini to generate fresh data.
 */
class IndustryInsightsRepository(
    private val client: OkHttpClient = OkHttpClient(),
) {

    suspend fun loadIndustryInsights(forceRefresh: Boolean = false): IndustryInsightLoadResult =
        withContext(Dispatchers.IO) {
            val user = Clerk.user
                ?: throw IllegalStateException("User session unavailable. Please sign in again.")
            if (BuildConfig.NEON_API_URL.isBlank()) {
                throw IllegalStateException("Neon API URL is not configured.")
            }
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                throw IllegalStateException("Gemini API key is not configured.")
            }

            val authorizationHeader = resolveAuthorizationHeader()
                ?: throw IllegalStateException("No Neon authentication method configured.")

            val neonUser = fetchUserFromNeon(user.id, authorizationHeader)
            val userIndustry =
                neonUser.industry?.takeUnless { it.isBlank() }
                    ?: throw IllegalStateException("Set your industry during onboarding to view insights.")

            val existingInsight = neonUser.industryInsight
            val now = Instant.now()
            val needsRefresh = forceRefresh ||
                existingInsight == null ||
                existingInsight.requiresRefresh(now)

            return@withContext IndustryInsightLoadResult(
                industry = userIndustry,
                authorizationHeader = authorizationHeader,
                insight = existingInsight,
                needsRefresh = needsRefresh,
            )
        }

    suspend fun refreshIndustryInsights(
        industry: String,
        authorizationHeader: String,
    ): IndustryInsightRecord =
        withContext(Dispatchers.IO) {
            val generated = generateInsights(industry)
            return@withContext saveIndustryInsight(
                industry = industry,
                generated = generated,
                authorizationHeader = authorizationHeader
            )
        }

    suspend fun fetchOrCreateIndustryInsights(forceRefresh: Boolean = false): IndustryInsightRecord {
        val loadResult = loadIndustryInsights(forceRefresh)
        val existing = loadResult.insight
        if (existing != null && !loadResult.needsRefresh) {
            return existing
        }

        return refreshIndustryInsights(
            industry = loadResult.industry,
            authorizationHeader = loadResult.authorizationHeader
        )
    }

    /**
     * Ensures an IndustryInsight record exists for the given industry.
     * This method is designed to be called during onboarding before setting User.industry.
     *
     * @param industry The industry name to check/create
     * @param authorizationHeader The authorization header for Neon API calls
     * @return The IndustryInsightRecord (either existing or newly created)
     */
    suspend fun ensureIndustryInsightExists(
        industry: String,
        authorizationHeader: String,
    ): IndustryInsightRecord = withContext(Dispatchers.IO) {
        if (BuildConfig.NEON_API_URL.isBlank()) {
            throw IllegalStateException("Neon API URL is not configured.")
        }

        // Check if an IndustryInsight already exists for this industry
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val encodedIndustry = URLEncoder.encode(industry, UTF_8.name())
        val insightRequestUrl =
            "$apiUrl/IndustryInsight?select=*&industry=eq.$encodedIndustry&limit=1"

        val insightRequest =
            Request.Builder()
                .url(insightRequestUrl)
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

        val existingInsight = client.newCall(insightRequest).execute().use { response ->
            val bodyString = response.body?.string()
            if (response.isSuccessful && bodyString?.isNotBlank() == true) {
                val results = JSONArray(bodyString)
                if (results.length() > 0) {
                    parseIndustryInsight(results.getJSONObject(0))
                } else {
                    null
                }
            } else {
                null
            }
        }

        // If it exists, return it
        if (existingInsight != null) {
            return@withContext existingInsight
        }

        // Otherwise, create it
        // Try to generate with Gemini first, fallback to default if it fails
        return@withContext try {
            if (BuildConfig.GEMINI_API_KEY.isBlank()) {
                Log.w(TAG, "Gemini API key not configured, using default insights")
                createDefaultIndustryInsight(industry, authorizationHeader)
            } else {
                val generated = generateInsights(industry)
                saveIndustryInsight(
                    industry = industry,
                    generated = generated,
                    authorizationHeader = authorizationHeader,
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate insights with Gemini, using defaults: ${e.message}")
            createDefaultIndustryInsight(industry, authorizationHeader)
        }
    }

    private fun IndustryInsightRecord.requiresRefresh(referenceTime: Instant): Boolean {
        val dueByNextUpdate = !referenceTime.isBefore(nextUpdate)
        val dueByLastUpdated =
            lastUpdated.plus(7, ChronoUnit.DAYS).isBefore(referenceTime) ||
                lastUpdated == Instant.EPOCH
        return dueByNextUpdate || dueByLastUpdated
    }

    data class IndustryInsightLoadResult(
        val industry: String,
        val authorizationHeader: String,
        val insight: IndustryInsightRecord?,
        val needsRefresh: Boolean,
    )

    private suspend fun resolveAuthorizationHeader(): String? {
        val bearer = fetchClerkSessionToken()
            ?: BuildConfig.NEON_API_KEY.takeUnless { it.isBlank() }
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

        // Always fetch a fresh token to avoid using expired cached tokens
        return when (val result = session.fetchToken()) {
            is ClerkResult.Success -> result.value.jwt.takeUnless { it.isBlank() }
            is ClerkResult.Failure -> {
                Log.w(TAG, "Failed to fetch fresh Clerk token: ${result.error}")
                // As fallback, try cached token (might be expired but worth trying)
                session.lastActiveToken?.jwt?.takeUnless { it.isBlank() }
            }
            else -> null
        }
    }

    private fun fetchUserFromNeon(
        clerkUserId: String,
        authorizationHeader: String,
    ): NeonUserRecord {
        val encodedClerkId = URLEncoder.encode(clerkUserId, UTF_8.name())
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val userRequestUrl =
            "$apiUrl/User?select=*&clerkUserId=eq.$encodedClerkId&limit=1"

        val userRequest =
            Request.Builder()
                .url(userRequestUrl)
                .addHeader("Authorization", authorizationHeader)
                .get()
                .build()

        val industry = client.newCall(userRequest).execute().use { response ->
            val bodyString = response.body?.string()
                ?: throw IOException("Neon user fetch returned an empty body.")
            if (!response.isSuccessful) {
                throw IOException("Neon user fetch failed (${response.code}): $bodyString")
            }

            val results = JSONArray(bodyString)
            if (results.length() == 0) {
                throw IllegalStateException("No Neon user record found. Complete onboarding first.")
            }

            val userJson = results.getJSONObject(0)
            userJson.optString("industry").takeIf { it.isNotBlank() }
        }

        // Fetch IndustryInsight separately if user has an industry
        val insight = if (industry != null) {
            val encodedIndustry = URLEncoder.encode(industry, UTF_8.name())
            val insightRequestUrl =
                "$apiUrl/IndustryInsight?select=*&industry=eq.$encodedIndustry&limit=1"

            val insightRequest =
                Request.Builder()
                    .url(insightRequestUrl)
                    .addHeader("Authorization", authorizationHeader)
                    .get()
                    .build()

            client.newCall(insightRequest).execute().use { response ->
                val bodyString = response.body?.string()
                if (response.isSuccessful && bodyString?.isNotBlank() == true) {
                    val results = JSONArray(bodyString)
                    if (results.length() > 0) {
                        parseIndustryInsight(results.getJSONObject(0))
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } else {
            null
        }

        return NeonUserRecord(industry = industry, industryInsight = insight)
    }

    /**
     * Creates a default IndustryInsight record with placeholder data.
     * Used as a fallback when Gemini API is unavailable.
     */
    private fun createDefaultIndustryInsight(
        industry: String,
        authorizationHeader: String,
    ): IndustryInsightRecord {
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val nextUpdate = now.plus(1, ChronoUnit.DAYS) // Retry sooner for defaults

        val defaultInsights = GeneratedInsights(
            salaryRanges = listOf(
                SalaryRangeRecord(
                    role = "Entry Level",
                    location = "United States",
                    min = 40000,
                    median = 55000,
                    max = 70000,
                ),
                SalaryRangeRecord(
                    role = "Mid Level",
                    location = "United States",
                    min = 60000,
                    median = 80000,
                    max = 100000,
                ),
                SalaryRangeRecord(
                    role = "Senior Level",
                    location = "United States",
                    min = 90000,
                    median = 120000,
                    max = 150000,
                ),
            ),
            growthRate = 5.0f,
            demandLevel = "Moderate",
            topSkills = emptyList(),
            marketOutlook = "Neutral",
            keyTrends = emptyList(),
            recommendedSkills = emptyList(),
        )

        return saveIndustryInsight(
            industry = industry,
            generated = defaultInsights,
            authorizationHeader = authorizationHeader,
        )
    }

    private suspend fun generateInsights(industry: String): GeneratedInsights {
        val prompt = """
            Analyze the current state of the $industry industry and provide insights in ONLY the following JSON format without any additional notes or explanations:
            {
              "salaryRanges": [
                { "role": "string", "min": number, "max": number, "median": number, "location": "string" }
              ],
              "growthRate": number,
              "demandLevel": "High" | "Medium" | "Low",
              "topSkills": ["skill1", "skill2"],
              "marketOutlook": "Positive" | "Neutral" | "Negative",
              "keyTrends": ["trend1", "trend2"],
              "recommendedSkills": ["skill1", "skill2"]
            }

            IMPORTANT: Return ONLY the JSON. No additional text, notes, or markdown formatting.
            Include at least 5 common roles for salary ranges.
            Growth rate should be a percentage.
            Include at least 5 skills and trends.
        """.trimIndent()

        val requestUrl =
            HttpUrl.Builder()
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
        }

        val request =
            Request.Builder()
                .url(requestUrl)
                .addHeader("x-goog-api-key", BuildConfig.GEMINI_API_KEY)
                .addHeader("Content-Type", JSON_MEDIA_TYPE)
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()
        val timeoutClient =
            client.newBuilder()
                .callTimeout(2, TimeUnit.MINUTES)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

        val rawText =
            timeoutClient.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                    ?: throw IOException("Gemini returned an empty response.")
                if (!response.isSuccessful) {
                    throw IOException("Gemini request failed (${response.code}): $bodyString")
                }
                extractGeminiText(JSONObject(bodyString))
                    ?: throw IOException("Gemini response did not include text content.")
            }

        val cleaned = CODE_FENCE_REGEX.replace(rawText, "").trim()
        val json = try {
            JSONObject(cleaned)
        } catch (error: Exception) {
            throw IOException("Gemini returned invalid JSON: ${error.message}\n$cleaned", error)
        }

        val salaryRanges = json.optJSONArray("salaryRanges")?.let { array ->
            List(array.length()) { index ->
                val rangeJson = array.getJSONObject(index)
                SalaryRangeRecord(
                    role = rangeJson.optString("role"),
                    location = rangeJson.optString("location"),
                    min = rangeJson.optNumber("min").roundToInt(),
                    median = rangeJson.optNumber("median").roundToInt(),
                    max = rangeJson.optNumber("max").roundToInt(),
                )
            }
        } ?: emptyList()

        return GeneratedInsights(
            salaryRanges = salaryRanges,
            growthRate = json.optNumber("growthRate").toFloat(),
            demandLevel = json.optString("demandLevel"),
            topSkills = json.optStringArray("topSkills"),
            marketOutlook = json.optString("marketOutlook"),
            keyTrends = json.optStringArray("keyTrends"),
            recommendedSkills = json.optStringArray("recommendedSkills"),
        )
    }

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

    private fun saveIndustryInsight(
        industry: String,
        generated: GeneratedInsights,
        authorizationHeader: String,
    ): IndustryInsightRecord {
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val nextUpdate = now.plus(7, ChronoUnit.DAYS)
        val namingStrategies = listOf(ColumnNaming.LOWER)

        var missingColumnError: MissingColumnException? = null
        for (strategy in namingStrategies) {
            val payload = buildIndustryInsightPayload(
                naming = strategy,
                industry = industry,
                generated = generated,
                now = now,
                nextUpdate = nextUpdate,
            )

            try {
                return postIndustryInsightPayload(
                    apiUrl = apiUrl,
                    authorizationHeader = authorizationHeader,
                    payload = payload,
                )
            } catch (duplicate: DuplicateIndustryException) {
                return updateIndustryInsightPayload(
                    apiUrl = apiUrl,
                    authorizationHeader = authorizationHeader,
                    industry = industry,
                    payload = payload,
                )
            } catch (missing: MissingColumnException) {
                missingColumnError = missing
                Log.w(
                    TAG,
                    "Neon is missing column ${missing.columnName ?: "unknown"} when using ${strategy.description}; trying next naming strategy."
                )
            }
        }

        throw missingColumnError
            ?: IOException("Failed to save industry insight due to unresolved column mismatch.")
    }

    private fun buildIndustryInsightPayload(
        naming: ColumnNaming,
        industry: String,
        generated: GeneratedInsights,
        now: Instant,
        nextUpdate: Instant,
    ): JSONObject {
        return JSONObject().apply {
            put(naming.format("industry"), industry)
            put(
                naming.format("salaryRanges"),
                JSONArray().apply {
                    generated.salaryRanges.forEach { range ->
                        put(
                            JSONObject().apply {
                                put("role", range.role)
                                put("location", range.location)
                                put("min", range.min)
                                put("median", range.median)
                                put("max", range.max)
                            }
                        )
                    }
                }
            )
            put(naming.format("growthRate"), generated.growthRate.toDouble())
            put(naming.format("demandLevel"), generated.demandLevel)
            put(naming.format("topSkills"), JSONArray(generated.topSkills))
            put(naming.format("marketOutlook"), generated.marketOutlook)
            put(naming.format("keyTrends"), JSONArray(generated.keyTrends))
            put(naming.format("recommendedSkills"), JSONArray(generated.recommendedSkills))
            put(naming.format("lastUpdated"), now.toString())
            put(naming.format("nextUpdate"), nextUpdate.toString())
        }
    }

    private fun postIndustryInsightPayload(
        apiUrl: String,
        authorizationHeader: String,
        payload: JSONObject,
    ): IndustryInsightRecord {
        val request =
            Request.Builder()
                .url("$apiUrl/IndustryInsight")
                .addHeader("Authorization", authorizationHeader)
                .addHeader("Content-Type", JSON_MEDIA_TYPE)
                .addHeader("Prefer", "return=representation")
                .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                if (response.code == 400) {
                    val missingColumn = extractMissingColumn(bodyString)
                    if (missingColumn != null) {
                        throw MissingColumnException(missingColumn, bodyString)
                    }
                }
                if (response.code == 409) {
                    throw DuplicateIndustryException(bodyString)
                }
                throw IOException("Failed to save industry insight (${response.code}): $bodyString")
            }
            if (bodyString.isBlank()) {
                throw IOException("Neon returned an empty response when creating an industry insight.")
            }

            val results = JSONArray(bodyString)
            if (results.length() == 0) {
                throw IOException("Neon did not return the created insight.")
            }
            val insightJson = results.getJSONObject(0)
            return parseIndustryInsight(insightJson)
        }
    }

    private fun updateIndustryInsightPayload(
        apiUrl: String,
        authorizationHeader: String,
        industry: String,
        payload: JSONObject,
    ): IndustryInsightRecord {
        val encodedIndustry = URLEncoder.encode(industry, UTF_8.name())
        val request =
            Request.Builder()
                .url("$apiUrl/IndustryInsight?industry=eq.$encodedIndustry")
                .addHeader("Authorization", authorizationHeader)
                .addHeader("Content-Type", JSON_MEDIA_TYPE)
                .addHeader("Prefer", "return=representation")
                .patch(payload.toString().toRequestBody(JSON_MEDIA_TYPE.toMediaType()))
                .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("Failed to update industry insight (${response.code}): $bodyString")
            }
            if (bodyString.isBlank()) {
                throw IOException("Neon returned an empty response when updating an industry insight.")
            }

            val results = JSONArray(bodyString)
            if (results.length() == 0) {
                throw IOException("Neon did not return the updated insight.")
            }
            val insightJson = results.getJSONObject(0)
            return parseIndustryInsight(insightJson)
        }
    }

    private fun extractMissingColumn(bodyString: String): String? {
        val message = runCatching { JSONObject(bodyString).optString("message") }.getOrNull()
        if (message.isNullOrBlank()) return null
        val match = MISSING_COLUMN_REGEX.find(message)
        return match?.groupValues?.getOrNull(1)
    }

    private fun parseIndustryInsight(json: JSONObject): IndustryInsightRecord {
        val salaryArray = json.optJSONArrayCompat(*"salaryRanges".keyVariants()) ?: JSONArray()
        val salaryRanges = List(salaryArray.length()) { index ->
            val rangeJson = salaryArray.getJSONObject(index)
            SalaryRangeRecord(
                role = rangeJson.optStringCompat(*"role".keyVariants()),
                location = rangeJson.optStringCompat(*"location".keyVariants()),
                min = rangeJson.optNumber(*"min".keyVariants()).roundToInt(),
                median = rangeJson.optNumber(*"median".keyVariants()).roundToInt(),
                max = rangeJson.optNumber(*"max".keyVariants()).roundToInt(),
            )
        }

        return IndustryInsightRecord(
            id = json.optStringCompat(*"id".keyVariants()),
            industry = json.optStringCompat(*"industry".keyVariants()),
            salaryRanges = salaryRanges,
            growthRate = json.optNumber(*"growthRate".keyVariants()).toFloat(),
            demandLevel = json.optStringCompat(*"demandLevel".keyVariants()),
            topSkills = json.optStringArray(*"topSkills".keyVariants()),
            marketOutlook = json.optStringCompat(*"marketOutlook".keyVariants()),
            keyTrends = json.optStringArray(*"keyTrends".keyVariants()),
            recommendedSkills = json.optStringArray(*"recommendedSkills".keyVariants()),
            lastUpdated = json.optInstant(*"lastUpdated".keyVariants()),
            nextUpdate = json.optInstant(*"nextUpdate".keyVariants()),
        )
    }

    private fun JSONObject.optStringCompat(vararg keys: String): String {
        for (key in keys) {
            val value = optString(key)
            if (value.isNotBlank()) return value
        }
        return ""
    }

    private fun JSONObject.optJSONArrayCompat(vararg keys: String): JSONArray? {
        for (key in keys) {
            val array = optJSONArray(key)
            if (array != null) return array
        }
        return null
    }

    private fun JSONObject.optNumber(vararg keys: String): Double {
        for (key in keys) {
            when (val value = opt(key)) {
                is Number -> return value.toDouble()
                is String -> value.toDoubleOrNull()?.let { return it }
            }
        }
        return 0.0
    }

    private fun JSONObject.optStringArray(vararg keys: String): List<String> {
        for (key in keys) {
            val array = optJSONArray(key) ?: continue
            return List(array.length()) { index -> array.optString(index) }.filter { it.isNotBlank() }
        }
        return emptyList()
    }

    private fun JSONObject.optInstant(vararg keys: String): Instant {
        for (key in keys) {
            val raw = optString(key).takeIf { it.isNotBlank() } ?: continue
            val parsed = runCatching { Instant.parse(raw) }
                .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
                .getOrNull()
            if (parsed != null) return parsed
        }
        return Instant.EPOCH
    }

    private fun String.keyVariants(): Array<String> {
        val variants = mutableListOf(this, lowercase(Locale.US), toSnakeCase())
        return variants.distinct().toTypedArray()
    }

    private fun ColumnNaming.format(key: String): String = when (this) {
        ColumnNaming.CAMEL -> key
        ColumnNaming.LOWER -> key.lowercase(Locale.US)
        ColumnNaming.SNAKE -> key.toSnakeCase()
    }

    private fun String.toSnakeCase(): String {
        val withUnderscores = replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
            .replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2")
        return withUnderscores.lowercase(Locale.US)
    }

    data class NeonUserRecord(
        val industry: String?,
        val industryInsight: IndustryInsightRecord?,
    )

    data class IndustryInsightRecord(
        val id: String,
        val industry: String,
        val salaryRanges: List<SalaryRangeRecord>,
        val growthRate: Float,
        val demandLevel: String,
        val topSkills: List<String>,
        val marketOutlook: String,
        val keyTrends: List<String>,
        val recommendedSkills: List<String>,
        val lastUpdated: Instant,
        val nextUpdate: Instant,
    )

    data class SalaryRangeRecord(
        val role: String,
        val location: String,
        val min: Int,
        val median: Int,
        val max: Int,
    )

    data class GeneratedInsights(
        val salaryRanges: List<SalaryRangeRecord>,
        val growthRate: Float,
        val demandLevel: String,
        val topSkills: List<String>,
        val marketOutlook: String,
        val keyTrends: List<String>,
        val recommendedSkills: List<String>,
    )

    private enum class ColumnNaming(val description: String) {
        CAMEL("camelCase"),
        LOWER("lowercase"),
        SNAKE("snake_case"),
    }

    private class MissingColumnException(
        val columnName: String?,
        rawMessage: String,
    ) : IOException(rawMessage)

    private class DuplicateIndustryException(
        rawMessage: String,
    ) : IOException(rawMessage)

    companion object {
        private const val TAG = "IndustryInsightsRepo"
        private const val GEMINI_API_HOST = "generativelanguage.googleapis.com"
        private const val GEMINI_MODEL_NAME = "gemini-2.5-flash"
        private const val JSON_MEDIA_TYPE = "application/json; charset=utf-8"
        private val MISSING_COLUMN_REGEX = Regex("column \"([^\"]+)\"")
        private val CODE_FENCE_REGEX = Regex("```(?:json)?")
    }
}
