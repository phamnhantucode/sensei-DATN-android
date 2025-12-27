package com.phamnhantucode.aicareercoach.data.payment

import android.util.Log
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class PaymentRepository {

    private val client = OkHttpClient()

    data class PaymentConfig(
        val paymentIntent: String,
        val ephemeralKey: String,
        val customer: String,
        val publishableKey: String
    )

    suspend fun fetchPaymentConfig(token: String): Result<PaymentConfig> = withContext(Dispatchers.IO) {
        try {
            val url = "${BuildConfig.NEON_API_URL.replace("/neondb/rest/v1", "")}/api/mobile/payment-intent"
            // Note: Adjusting the URL to point to the NextJS API, not Neon DB
            // Assuming the app uses a common base URL or we need to construct it.
            // For now, I will assume we need to use the server URL. 
            // Since NEON_API_URL is likely for the database, I should probably check if there is a SERVER_URL. 
            // But looking at build.gradle.kts, there isn't one. 
            // I will use a placeholder or derived URL, but for local testing with the provided context,
            // I might need to ask or assume localhost/tunnel. 
            // However, the report mentioned "d:/Workspace/DATN/sensei-DATN", so it's a local NextJS app.
            
            // Wait, checking the build.gradle again.
            // val neonApiUrl = ...
            // There is no SERVER_URL.
            
            // I will implement it such that it expects the API to be at a specific location.
            // For this task, I will hardcode a standard local IP for emulator or allow dynamic config if possible.
            // But better, I'll use a constant I can swap.
            
            val serverUrl = "https://sensei-datn-lmae.vercel.app/"
            
            val request = Request.Builder()
                .url("$serverUrl/api/mobile/payment-intent")
                .addHeader("Authorization", "Bearer $token")
                .post(okhttp3.RequestBody.create(null, ByteArray(0))) // Empty POST
                .build()

            val response: Response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Unexpected code $response"))
            }
            
            val responseBody = response.body?.string() ?: return@withContext Result.failure(IOException("Empty response"))
            val json = JSONObject(responseBody)
            
            val config = PaymentConfig(
                paymentIntent = json.getString("paymentIntent"),
                ephemeralKey = json.getString("ephemeralKey"),
                customer = json.getString("customer"),
                publishableKey = json.getString("publishableKey")
            )
            
            Result.success(config)
        } catch (e: Exception) {
            Log.e("PaymentRepository", "Error fetching payment config", e)
            Result.failure(e)
        }
    }
}
