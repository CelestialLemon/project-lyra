package com.projectlyra.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.data.settings.TmdbApiKeyValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val _apiKeyError = MutableStateFlow<String?>(null)
    val apiKeyError: StateFlow<String?> = _apiKeyError.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    init {
        viewModelScope.launch {
            settingsStore.migrateLegacyApiKeyIfNeeded()
        }
    }

    fun onApiKeyDraftChanged(value: String) {
        _apiKeyError.value = TmdbApiKeyValidator.validate(value)
    }

    fun updateApiKey(value: String) {
        val validationError = TmdbApiKeyValidator.validate(value)
        if (validationError != null) {
            _apiKeyError.value = validationError
            return
        }

        viewModelScope.launch {
            val wasSaved = settingsStore.updateApiKey(value)
            _apiKeyError.value = if (wasSaved) null else "Unable to encrypt API key on this device."
        }
    }

    fun updateIncludeApiKeyInBackup(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.updateIncludeApiKeyInBackup(enabled)
        }
    }

    fun updateReminder(enabled: Boolean) {
        val current = settings.value
        viewModelScope.launch {
            settingsStore.updateReminder(enabled = enabled, hour = current.reminderHour, minute = current.reminderMinute)
        }
    }
}

class SettingsViewModelFactory(
    private val settingsStore: SettingsStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsStore) as T
    }
}
