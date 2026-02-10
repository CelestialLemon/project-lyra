package com.projectlyra.app.feature.settings

import com.projectlyra.app.data.backup.BackupOperationResult
import com.projectlyra.app.data.backup.BackupService
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updateApiKey_withInvalidValue_setsValidationError() = runTest {
        val settingsStore = mockk<SettingsStore>()
        val backupService = mockk<BackupService>()
        every { settingsStore.settings } returns flowOf(AppSettings())
        coEvery { settingsStore.migrateLegacyApiKeyIfNeeded() } returns Unit

        val viewModel = SettingsViewModel(settingsStore = settingsStore, backupService = backupService)
        advanceUntilIdle()

        viewModel.updateApiKey("not-a-valid-key")
        advanceUntilIdle()

        assertTrue(viewModel.apiKeyError.value!!.contains("Invalid format"))
        coVerify(exactly = 0) { settingsStore.updateApiKey(any()) }
    }

    @Test
    fun exportBackup_updatesBackupUiStateOnSuccess() = runTest {
        val settingsStore = mockk<SettingsStore>()
        val backupService = mockk<BackupService>()
        val exportUri = mockk<android.net.Uri>()
        every { settingsStore.settings } returns flowOf(AppSettings())
        coEvery { settingsStore.migrateLegacyApiKeyIfNeeded() } returns Unit
        coEvery { backupService.exportToUri(any()) } returns BackupOperationResult.Success("Exported")

        val viewModel = SettingsViewModel(settingsStore = settingsStore, backupService = backupService)
        advanceUntilIdle()

        viewModel.exportBackup(exportUri)
        advanceUntilIdle()

        assertEquals(false, viewModel.backupUiState.value.isProcessing)
        assertEquals("Exported", viewModel.backupUiState.value.resultMessage)
        assertEquals(false, viewModel.backupUiState.value.isError)
    }

    @Test
    fun importBackup_updatesBackupUiStateOnError() = runTest {
        val settingsStore = mockk<SettingsStore>()
        val backupService = mockk<BackupService>()
        val importUri = mockk<android.net.Uri>()
        every { settingsStore.settings } returns flowOf(AppSettings())
        coEvery { settingsStore.migrateLegacyApiKeyIfNeeded() } returns Unit
        coEvery { backupService.importFromUri(any()) } returns BackupOperationResult.Error("Invalid backup")

        val viewModel = SettingsViewModel(settingsStore = settingsStore, backupService = backupService)
        advanceUntilIdle()

        viewModel.importBackup(importUri)
        advanceUntilIdle()

        assertEquals("Invalid backup", viewModel.backupUiState.value.resultMessage)
        assertTrue(viewModel.backupUiState.value.isError)
    }
}
