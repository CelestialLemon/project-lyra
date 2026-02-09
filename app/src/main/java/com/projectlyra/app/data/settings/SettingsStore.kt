package com.projectlyra.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val apiKey = stringPreferencesKey("api_key")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val includeApiKeyInBackup = booleanPreferencesKey("include_api_key_in_backup")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs: Preferences ->
        AppSettings(
            apiKey = prefs[Keys.apiKey].orEmpty(),
            reminderEnabled = prefs[Keys.reminderEnabled] ?: true,
            reminderHour = prefs[Keys.reminderHour] ?: 20,
            reminderMinute = prefs[Keys.reminderMinute] ?: 0,
            includeApiKeyInBackup = prefs[Keys.includeApiKeyInBackup] ?: false,
        )
    }

    suspend fun updateApiKey(apiKey: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.apiKey] = apiKey.trim()
        }
    }

    suspend fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.reminderEnabled] = enabled
            prefs[Keys.reminderHour] = hour
            prefs[Keys.reminderMinute] = minute
        }
    }

    suspend fun updateIncludeApiKeyInBackup(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.includeApiKeyInBackup] = enabled
        }
    }
}
