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

// Manages Industry Insights
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

            val authorizationHeader = resolveAuthorizationHeader()
                ?: throw IllegalStateException("No Neon authentication method configured.")

            val neonUser = fetchUserFromNeon(user.id, authorizationHeader)
            val userIndustry =
                neonUser.industry?.takeUnless { it.isBlank() }
                    ?: throw IllegalStateException("Set your industry during onboarding to view insights.")

            val existingInsight = neonUser.industryInsight
            val now = Instant.now()
            
            // Log the cache status for debugging
            if (existingInsight != null) {
                Log.d(TAG, "Found existing insight for ${existingInsight.industry}. Last Updated: ${existingInsight.lastUpdated}, Next Update: ${existingInsight.nextUpdate}")
            } else {
                Log.d(TAG, "No existing insight found for $userIndustry")
            }

            val needsRefresh = forceRefresh ||
                existingInsight == null ||
                existingInsight.requiresRefresh(now)
            
            Log.d(TAG, "Load Result: needsRefresh=$needsRefresh (Force=$forceRefresh)")

            return@withContext IndustryInsightLoadResult(
                industry = userIndustry,
                authorizationHeader = authorizationHeader,
                insight = existingInsight,
                needsRefresh = needsRefresh,
                creditBalance = neonUser.creditBalance
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

    // Ensure insight exists
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
        // Try to generate with AI first, fallback to default if it fails
        return@withContext try {
             val generated = generateInsights(industry)
             saveIndustryInsight(
                 industry = industry,
                 generated = generated,
                 authorizationHeader = authorizationHeader,
             )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to generate insights with AI, using defaults: ${e.message}")
            createDefaultIndustryInsight(industry, authorizationHeader)
        }
    }

    private fun IndustryInsightRecord.requiresRefresh(referenceTime: Instant): Boolean {
        val dueByNextUpdate = !referenceTime.isBefore(nextUpdate)
        val daysSinceUpdate = ChronoUnit.DAYS.between(lastUpdated, referenceTime)
        val dueByLastUpdated = daysSinceUpdate >= 7 || lastUpdated == Instant.EPOCH
                
        if (dueByNextUpdate) Log.d(TAG, "Refresh required: Past nextUpdate time ($nextUpdate)")
        if (dueByLastUpdated) Log.d(TAG, "Refresh required: Insight is old ($daysSinceUpdate days) or invalid date ($lastUpdated)")
        
        return dueByNextUpdate || dueByLastUpdated
    }

    data class IndustryInsightLoadResult(
        val industry: String,
        val authorizationHeader: String,
        val insight: IndustryInsightRecord?,
        val needsRefresh: Boolean,
        val creditBalance: Int?,
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

        var neonUserId: String? = null
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
            neonUserId = userJson.getString("id")
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

        val creditBalance = if (neonUserId != null) {
            fetchCreditBalance(neonUserId!!, authorizationHeader)
        } else null

        return NeonUserRecord(industry = industry, industryInsight = insight, creditBalance = creditBalance)
    }

    private fun fetchCreditBalance(userId: String, authorizationHeader: String): Int? {
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val requestUrl = "$apiUrl/UserCredit?userId=eq.$userId&select=balance&limit=1"
        
         val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()
            
        return try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrBlank()) {
                    val json = JSONArray(body)
                    if (json.length() > 0) {
                        json.getJSONObject(0).optInt("balance")
                    } else null
                } else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch credit balance", e)
            null
        }
    }

    // Create default data
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

        val messages = listOf(
            com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.Message("user", prompt)
        )

        // Request JSON object response format
        val responseFormat = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.ResponseFormat(type = "json_object")
        
        val rawText = com.phamnhantucode.aicareercoach.data.ai.OpenRouterService.chatCompletion(
            messages = messages,
            responseFormat = responseFormat
        )

        val cleaned = CODE_FENCE_REGEX.replace(rawText, "").trim()
        val json = try {
            JSONObject(cleaned)
        } catch (error: Exception) {
            throw IOException("AI Service returned invalid JSON: ${error.message}\n$cleaned", error)
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


    private fun saveIndustryInsight(
        industry: String,
        generated: GeneratedInsights,
        authorizationHeader: String,
    ): IndustryInsightRecord {
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        val now = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val nextUpdate = now.plus(7, ChronoUnit.DAYS)
        val namingStrategies = listOf(ColumnNaming.CAMEL)

        var missingColumnError: MissingColumnException? = null
        for (strategy in namingStrategies) {
            val postPayload = buildIndustryInsightPayload(
                naming = strategy,
                industry = industry,
                generated = generated,
                now = now,
                nextUpdate = nextUpdate,
                includeId = true,
            )

            try {
                return postIndustryInsightPayload(
                    apiUrl = apiUrl,
                    authorizationHeader = authorizationHeader,
                    payload = postPayload,
                )
            } catch (duplicate: DuplicateIndustryException) {
                // For update, don't include the id field
                val patchPayload = buildIndustryInsightPayload(
                    naming = strategy,
                    industry = industry,
                    generated = generated,
                    now = now,
                    nextUpdate = nextUpdate,
                    includeId = false,
                )
                return updateIndustryInsightPayload(
                    apiUrl = apiUrl,
                    authorizationHeader = authorizationHeader,
                    industry = industry,
                    payload = patchPayload,
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
        includeId: Boolean = false,
    ): JSONObject {
        return JSONObject().apply {
            // Include ID only for POST requests, not for PATCH
            if (includeId) {
                // Generate a random hex ID similar to what the database would generate
                val randomBytes = ByteArray(12)
                java.security.SecureRandom().nextBytes(randomBytes)
                val hexId = randomBytes.joinToString("") { "%02x".format(it) }
                put(naming.format("id"), hexId)
            }
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
            val parsed = parseFlexibleInstant(raw)
            if (parsed != null) return parsed
            
            Log.w(TAG, "Failed to parse date for key '$key': '$raw'. Defaulting to EPOCH.")
        }
        return Instant.EPOCH
    }

    private fun parseFlexibleInstant(raw: String): Instant? {
        return try {
            // Try standard ISO-8601 (2023-10-01T12:00:00Z)
            Instant.parse(raw)
        } catch (e: Exception) {
            try {
                // Try with offset (2023-10-01T12:00:00+01:00)
                OffsetDateTime.parse(raw).toInstant()
            } catch (e2: Exception) {
                try {
                    // Try SQL Timestamp format (2023-10-01 12:00:00) - Assume UTC
                    // Handle variable fractional seconds or none
                    val cleanRaw = raw.replace("T", " ")
                    val pattern = if (cleanRaw.length > 19) "yyyy-MM-dd HH:mm:ss.SSSSSS" else "yyyy-MM-dd HH:mm:ss"
                    // If the string is shorter than the pattern (e.g. less micros), we might need to be more adaptive
                    // But usually Postgres gives 6 digits or 0.
                    
                    // Simple variable parsing:
                    val formatter = java.time.format.DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd HH:mm:ss")
                        .appendOptional(java.time.format.DateTimeFormatterBuilder().appendPattern(".SSSSSS").toFormatter())
                        .appendOptional(java.time.format.DateTimeFormatterBuilder().appendPattern(".SSS").toFormatter())
                        .appendOptional(java.time.format.DateTimeFormatterBuilder().appendPattern(".S").toFormatter())
                        .toFormatter()

                    java.time.LocalDateTime.parse(cleanRaw, formatter)
                        .atZone(java.time.ZoneId.of("UTC"))
                        .toInstant()
                } catch (e3: Exception) {
                    null
                }
            }
        }
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
        val creditBalance: Int? = null,
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
