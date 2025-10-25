package com.phamnhantucode.aicareercoach.data.neon

import android.util.Log
import com.clerk.api.Clerk
import com.clerk.api.network.serialization.ClerkResult
import com.clerk.api.network.serialization.longErrorMessageOrNull
import com.clerk.api.session.fetchToken
import com.phamnhantucode.aicareercoach.BuildConfig
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.isActive

object NeonAuth {
    private const val TAG = "NeonAuth"

    /**
     * Returns a token string suitable for Neon Authorization header usage:
     * - If Clerk session JWT is available, returns that (to be used as Bearer token)
     * - Else falls back to NEON_API_KEY (also used as Bearer token)
     * - Else, if NEON_DB_ROLE/NEON_DB_PASSWORD are configured, returns empty string ""
     *   signaling callers to use Basic auth via NeonUserService.resolveAuthorizationHeader
     * - Else returns null to indicate no credentials available
     */
    suspend fun fetchNeonAuthToken(): String? {
        if (!coroutineContext.isActive) return null

        val fallbackToken = BuildConfig.NEON_API_KEY.takeUnless { it.isBlank() }
        val basicAuthConfigured = BuildConfig.NEON_DB_ROLE.isNotBlank() && BuildConfig.NEON_DB_PASSWORD.isNotBlank()

        val session = Clerk.session
        if (session == null) {
            Log.w(TAG, "Clerk session unavailable; cannot fetch Neon auth token.")
            return fallbackToken ?: if (basicAuthConfigured) "" else null
        }

        // Always fetch a fresh token to avoid using expired cached tokens
        val clerkResult = try {
            session.fetchToken()
        } catch (cancelled: Exception) {
            Log.w(TAG, "Failed to fetch token, using fallback", cancelled)
            // Cancellation or failure path handled similarly with fallbacks
            return fallbackToken ?: if (basicAuthConfigured) "" else null
        }

        return when (clerkResult) {
            is ClerkResult.Success -> {
                val jwt = clerkResult.value.jwt?.takeUnless { it.isBlank() }
                when {
                    jwt != null -> jwt
                    fallbackToken != null -> {
                        Log.w(TAG, "Fresh token empty, using fallback API key")
                        fallbackToken
                    }
                    basicAuthConfigured -> {
                        Log.w(TAG, "Fresh token empty, using Basic auth")
                        ""
                    }
                    else -> {
                        Log.w(TAG, "Clerk returned an empty Neon auth token; sync skipped.")
                        null
                    }
                }
            }
            is ClerkResult.Failure -> {
                val errorMsg = clerkResult.longErrorMessageOrNull ?: "Failed to fetch Clerk session token for Neon."
                Log.e(TAG, errorMsg)
                // Try cached token as last resort before falling back to other auth methods
                val cachedToken = session.lastActiveToken?.jwt?.takeUnless { it.isBlank() }
                cachedToken ?: fallbackToken ?: if (basicAuthConfigured) "" else null
            }
        }
    }
}
