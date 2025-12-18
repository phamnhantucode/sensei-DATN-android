package com.phamnhantucode.aicareercoach.data.cloudinary

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONObject

// Extension property for DataStore
private val Context.imageUrlDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "image_url_cache"
)

// Caches upload mappings
class ImageUrlCache private constructor(private val context: Context) {

    companion object {
        private const val TAG = "ImageUrlCache"

        @Volatile
        private var INSTANCE: ImageUrlCache? = null

        fun getInstance(context: Context): ImageUrlCache {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ImageUrlCache(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private val URI_TO_URL_KEY = stringPreferencesKey("uri_to_url_mappings")
    }

    // Get cached URL
    suspend fun getCachedUrl(localUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val mappings = getMappings()
            mappings[localUri]
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached URL", e)
            null
        }
    }

    // Cache a mapping
    suspend fun cacheUrl(localUri: String, remoteUrl: String) = withContext(Dispatchers.IO) {
        try {
            val mappings = getMappings().toMutableMap()
            mappings[localUri] = remoteUrl
            saveMappings(mappings)
            Log.d(TAG, "Cached URL mapping: $localUri -> $remoteUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching URL", e)
        }
    }

    // Remove mapping
    suspend fun removeCachedUrl(localUri: String) = withContext(Dispatchers.IO) {
        try {
            val mappings = getMappings().toMutableMap()
            if (mappings.remove(localUri) != null) {
                saveMappings(mappings)
                Log.d(TAG, "Removed cached URL for: $localUri")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing cached URL", e)
        }
    }

    // Clears all
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        try {
            context.imageUrlDataStore.edit { preferences ->
                preferences.remove(URI_TO_URL_KEY)
            }
            Log.d(TAG, "Cleared all cached URL mappings")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    // Get all mappings
    private suspend fun getMappings(): Map<String, String> {
        return try {
            val jsonString = context.imageUrlDataStore.data
                .map { preferences ->
                    preferences[URI_TO_URL_KEY] ?: "{}"
                }
                .first()

            val jsonObject = JSONObject(jsonString)
            val mappings = mutableMapOf<String, String>()

            jsonObject.keys().forEach { key ->
                mappings[key] = jsonObject.getString(key)
            }

            mappings
        } catch (e: Exception) {
            Log.e(TAG, "Error reading mappings", e)
            emptyMap()
        }
    }

    // Save mappings
    private suspend fun saveMappings(mappings: Map<String, String>) {
        try {
            val jsonObject = JSONObject()
            mappings.forEach { (key, value) ->
                jsonObject.put(key, value)
            }

            context.imageUrlDataStore.edit { preferences ->
                preferences[URI_TO_URL_KEY] = jsonObject.toString()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saving mappings", e)
        }
    }
}
