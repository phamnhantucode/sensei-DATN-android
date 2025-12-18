package com.phamnhantucode.aicareercoach.data.ai

import com.google.gson.GsonBuilder
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

// Connects to OpenRouter
object OpenRouterService {

    private const val BASE_URL = "https://openrouter.ai/api/v1"
    private const val DEFAULT_MODEL = "google/gemini-2.5-flash-lite"
    private const val SITE_URL = "https://aicareercoach.app"
    private const val SITE_NAME = "AI Career Coach"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder().create()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Sends chat request
    suspend fun chatCompletion(
        messages: List<Message>,
        model: String = DEFAULT_MODEL,
        temperature: Double? = null,
        responseFormat: ResponseFormat? = null
    ): String = withContext(Dispatchers.IO) {
        if (BuildConfig.OPENROUTER_API_KEY.isBlank()) {
            throw IllegalStateException("OPENROUTER_API_KEY is not configured in local.properties")
        }

        val requestBody = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            response_format = responseFormat
        )

        val jsonBody = gson.toJson(requestBody)

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENROUTER_API_KEY}")
            .addHeader("HTTP-Referer", SITE_URL)
            .addHeader("X-Title", SITE_NAME)
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
            if (!response.isSuccessful) {
                throw IOException("OpenRouter request failed (${response.code}): $bodyString")
            }
            if (bodyString.isNullOrBlank()) {
                throw IOException("OpenRouter returned empty response")
            }

            try {
                val chatResponse = gson.fromJson(bodyString, ChatResponse::class.java)
                chatResponse.choices.firstOrNull()?.message?.content
                    ?: throw IOException("No content in OpenRouter response")
            } catch (e: Exception) {
                 throw IOException("Failed to parse OpenRouter response: $bodyString", e)
            }
        }
    }

    // Models
    data class ChatRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double? = null,
        val response_format: ResponseFormat? = null
    )

    data class Message(
        val role: String,
        val content: String
    )

    data class ResponseFormat(
        val type: String
    )

    data class ChatResponse(
        val id: String?,
        val choices: List<Choice>,
        val model: String?
    )

    data class Choice(
        val message: Message,
        val finish_reason: String?
    )
}
