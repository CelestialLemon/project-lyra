package com.projectlyra.app.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.data.backup.BackupOperationResult
import com.projectlyra.app.data.backup.BackupService
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.data.settings.TmdbApiKeyValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BackupUiState(
    val isProcessing: Boolean = false,
    val resultMessage: String? = null,
    val isError: Boolean = false,
)

class SettingsViewModel(
    private val settingsStore: SettingsStore,
    private val backupService: BackupService,
) : ViewModel() {
    private val _apiKeyError = MutableStateFlow<String?>(null)
    val apiKeyError: StateFlow<String?> = _apiKeyError.asStateFlow()
    private val _backupUiState = MutableStateFlow(BackupUiState())
    val backupUiState: StateFlow<BackupUiState> = _backupUiState.asStateFlow()

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

    fun exportBackup(uri: Uri) {
        runBackupOperation { backupService.exportToUri(uri) }
    }

    fun importBackup(uri: Uri) {
        runBackupOperation { backupService.importFromUri(uri) }
    }

    private fun runBackupOperation(
        operation: suspend () -> BackupOperationResult,
    ) {
        if (_backupUiState.value.isProcessing) {
            return
        }

        viewModelScope.launch {
            _backupUiState.value = BackupUiState(isProcessing = true)
            _backupUiState.value = when (val result = operation()) {
                is BackupOperationResult.Success -> {
                    BackupUiState(
                        isProcessing = false,
                        resultMessage = result.message,
                        isError = false,
                    )
                }
                is BackupOperationResult.Error -> {
                    BackupUiState(
                        isProcessing = false,
                        resultMessage = result.message,
                        isError = true,
                    )
                }
            }
        }
    }

    fun clearBackupMessage() {
        _backupUiState.update { state ->
            state.copy(resultMessage = null, isError = false)
        }
    }
}

class SettingsViewModelFactory(
    private val settingsStore: SettingsStore,
    private val backupService: BackupService,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(settingsStore, backupService) as T
    }
}
