package com.phamnhantucode.aicareercoach.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_preferences")

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM;

    companion object {
        fun fromString(value: String): ThemeMode {
            return try {
                valueOf(value.uppercase())
            } catch (e: IllegalArgumentException) {
                SYSTEM
            }
        }
    }
}

class PreferencesRepository(private val context: Context) {

    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val IS_FIRST_TIME_KEY = androidx.datastore.preferences.core.booleanPreferencesKey("is_first_time")

        @Volatile
        private var INSTANCE: PreferencesRepository? = null

        fun getInstance(context: Context): PreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PreferencesRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data
        .map { preferences ->
            val value = preferences[THEME_MODE_KEY] ?: ThemeMode.DARK.name
            ThemeMode.fromString(value)
        }

    val isFirstTimeFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_FIRST_TIME_KEY] ?: true
        }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
    }

    suspend fun setFirstTime(isFirstTime: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_FIRST_TIME_KEY] = isFirstTime
        }
    }
}
