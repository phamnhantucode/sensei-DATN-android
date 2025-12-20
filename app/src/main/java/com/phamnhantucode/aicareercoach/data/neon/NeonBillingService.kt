package com.phamnhantucode.aicareercoach.data.neon

import android.util.Log
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

object NeonBillingService {
    private const val TAG = "NeonBillingService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchCreditPacks(): List<CreditPack> = withContext(Dispatchers.IO) {
        val authHeader = resolveAuthorizationHeader()
            ?: throw IOException("No Neon auth credentials available")

        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        // Fetch active credit packs, ordered by displayOrder
        val url = "$apiUrl/CreditPack?isActive=eq.true&platform=in.(all,android)&order=displayOrder.asc"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", authHeader)
            .get()
            .build()
            
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                
                if (!response.isSuccessful) {
                    throw IOException("Failed to fetch credit packs (${response.code}): $body")
                }
                
                if (body.isNullOrBlank()) {
                    return@withContext emptyList()
                }

                val jsonArray = JSONArray(body)
                val packs = mutableListOf<CreditPack>()
                
                for (i in 0 until jsonArray.length()) {
                    try {
                        packs.add(parseCreditPack(jsonArray.getJSONObject(i)))
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing credit pack at index $i", e)
                    }
                }
                
                return@withContext packs
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching credit packs", e)
            return@withContext emptyList()
        }
    }

    suspend fun recordPurchase(
        userId: String,
        creditPack: CreditPack,
        purchaseToken: String,
        orderId: String,
        authToken: String? = null
    ) = withContext(Dispatchers.IO) {
        val authHeader = resolveAuthorizationHeader(authToken)
            ?: throw IOException("No Neon auth credentials available")
            
        val apiUrl = BuildConfig.NEON_API_URL.trimEnd('/')
        
        // 1. Log the transaction
        val transactionId = generateRandomId()
        val transactionPayload = JSONObject().apply {
            put("id", transactionId)
            put("userId", userId)
            put("amount", creditPack.credits + creditPack.bonusCredits)
            put("type", "PURCHASE")
            put("description", "Purchased ${creditPack.name}")
            put("provider", "google_play")
            put("providerTransactionId", orderId)
            put("creditPackId", creditPack.id)
            put("createdAt", java.time.Instant.now().toString())
        }

        val transactionRequest = Request.Builder()
            .url("$apiUrl/CreditTransaction")
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .post(transactionPayload.toString().toRequestBody(jsonMediaType))
            .build()
            
        client.newCall(transactionRequest).execute().use { 
            if (!it.isSuccessful) throw IOException("Failed to log transaction: ${it.code}")
        }

        // 2. Update user balance
        val currentBalance = fetchCreditBalance(userId, authHeader, apiUrl)
        val newBalance = currentBalance + creditPack.credits + creditPack.bonusCredits
        
        val updatePayload = JSONObject().apply {
            put("balance", newBalance)
            put("updatedAt", java.time.Instant.now().toString())
        }
        
        val updateRequest = Request.Builder()
            .url("$apiUrl/UserCredit?userId=eq.$userId")
            .addHeader("Authorization", authHeader)
            .addHeader("Content-Type", "application/json")
            .patch(updatePayload.toString().toRequestBody(jsonMediaType))
            .build()
            
        client.newCall(updateRequest).execute().use {
             if (!it.isSuccessful) throw IOException("Failed to update balance: ${it.code}")
        }
        
        Log.d(TAG, "Successfully recorded purchase for user $userId. New balance: $newBalance")
    }

    private fun fetchCreditBalance(userId: String, authorizationHeader: String, apiUrl: String): Int {
        val requestUrl = "$apiUrl/UserCredit?userId=eq.$userId&select=balance&limit=1"
        
         val request = Request.Builder()
            .url(requestUrl)
            .addHeader("Authorization", authorizationHeader)
            .get()
            .build()
            
        client.newCall(request).execute().use { response ->
            val body = response.body?.string()
            if (!response.isSuccessful) {
                // If it fails, we might just assume 0 or throw, but here we want to be safe to add
                // In a real app, this should be transactional or handled by backend functions
                throw IOException("Failed to fetch credit balance for update")
            }
            
            if (!body.isNullOrBlank()) {
                val json = JSONArray(body)
                if (json.length() > 0) {
                    return json.getJSONObject(0).optInt("balance")
                }
            }
            return 0
        }
    }

    private fun parseCreditPack(json: JSONObject): CreditPack {
        return CreditPack(
            id = json.getString("id"),
            name = json.getString("name"),
            credits = json.getInt("credits"),
            price = BigDecimal(json.getString("price")),
            platform = json.optString("platform", "all"),
            googlePlaySku = json.optString("googlePlaySku").takeIf { it != "null" && it.isNotBlank() },
            bonusCredits = json.optInt("bonusCredits", 0)
        )
    }

    // Duplicated private helper from NeonUserService
    private suspend fun resolveAuthorizationHeader(authToken: String? = null): String? {
        if (authToken != null) {
            return if (authToken.startsWith("Bearer ", ignoreCase = true) || 
                       authToken.startsWith("Basic ", ignoreCase = true)) {
                authToken
            } else {
                "Bearer $authToken"
            }
        }
        val fetched = NeonAuth.fetchNeonAuthToken()
        return if (!fetched.isNullOrBlank()) "Bearer $fetched" else null
    }

    // Duplicated private helper
    private fun generateRandomId(): String {
        val randomBytes = ByteArray(12)
        java.security.SecureRandom().nextBytes(randomBytes)
        return randomBytes.joinToString("") { "%02x".format(it) }
    }
}
