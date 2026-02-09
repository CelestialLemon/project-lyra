package com.projectlyra.app.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsStore: SettingsStore,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsStore.settings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AppSettings(),
        )

    fun updateApiKey(value: String) {
        viewModelScope.launch {
            settingsStore.updateApiKey(value)
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
