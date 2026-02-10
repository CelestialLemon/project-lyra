package com.projectlyra.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class SettingsStore(
    private val context: Context,
    private val apiKeyCrypto: ApiKeyCrypto = ApiKeyCrypto(),
) {
    private object Keys {
        val apiKeyLegacy = stringPreferencesKey("api_key")
        val apiKeyEncrypted = stringPreferencesKey("api_key_encrypted")
        val apiKeyIv = stringPreferencesKey("api_key_iv")
        val reminderEnabled = booleanPreferencesKey("reminder_enabled")
        val reminderHour = intPreferencesKey("reminder_hour")
        val reminderMinute = intPreferencesKey("reminder_minute")
        val includeApiKeyInBackup = booleanPreferencesKey("include_api_key_in_backup")
        val dynamicAccentEnabled = booleanPreferencesKey("dynamic_accent_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs: Preferences ->
        val decryptedApiKey = decryptApiKeyOrEmpty(
            encryptedKey = prefs[Keys.apiKeyEncrypted],
            iv = prefs[Keys.apiKeyIv],
        )
        AppSettings(
            apiKey = decryptedApiKey.ifEmpty { prefs[Keys.apiKeyLegacy].orEmpty() },
            reminderEnabled = prefs[Keys.reminderEnabled] ?: true,
            reminderHour = prefs[Keys.reminderHour] ?: 20,
            reminderMinute = prefs[Keys.reminderMinute] ?: 0,
            includeApiKeyInBackup = prefs[Keys.includeApiKeyInBackup] ?: false,
            dynamicAccentEnabled = if (AppSettings.dynamicAccentSupported) {
                prefs[Keys.dynamicAccentEnabled] ?: true
            } else {
                false
            },
        )
    }.flowOn(Dispatchers.IO)

    suspend fun migrateLegacyApiKeyIfNeeded() = withContext(Dispatchers.IO) {
        val prefs = context.settingsDataStore.data.first()
        val legacyKey = prefs[Keys.apiKeyLegacy].orEmpty().trim()
        val encryptedKey = prefs[Keys.apiKeyEncrypted]
        val iv = prefs[Keys.apiKeyIv]

        if (legacyKey.isEmpty() || (!encryptedKey.isNullOrBlank() && !iv.isNullOrBlank())) return@withContext

        val encryptedPayload = apiKeyCrypto.encrypt(legacyKey) ?: return@withContext
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.apiKeyEncrypted] = encryptedPayload.ciphertext
            prefs[Keys.apiKeyIv] = encryptedPayload.iv
            prefs.remove(Keys.apiKeyLegacy)
        }
    }

    suspend fun updateApiKey(apiKey: String): Boolean = withContext(Dispatchers.IO) {
        val normalized = apiKey.trim()
        if (normalized.isEmpty()) {
            context.settingsDataStore.edit { prefs ->
                prefs.remove(Keys.apiKeyLegacy)
                prefs.remove(Keys.apiKeyEncrypted)
                prefs.remove(Keys.apiKeyIv)
            }
            return@withContext true
        }

        val encryptedPayload = apiKeyCrypto.encrypt(normalized) ?: return@withContext false
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.apiKeyEncrypted] = encryptedPayload.ciphertext
            prefs[Keys.apiKeyIv] = encryptedPayload.iv
            prefs.remove(Keys.apiKeyLegacy)
        }
        true
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

    suspend fun updateDynamicAccent(enabled: Boolean) {
        val persistedValue = if (AppSettings.dynamicAccentSupported) enabled else false
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.dynamicAccentEnabled] = persistedValue
        }
    }

    private fun decryptApiKeyOrEmpty(encryptedKey: String?, iv: String?): String {
        if (encryptedKey.isNullOrBlank() || iv.isNullOrBlank()) return ""
        return apiKeyCrypto.decrypt(ciphertext = encryptedKey, iv = iv).orEmpty()
    }
}
