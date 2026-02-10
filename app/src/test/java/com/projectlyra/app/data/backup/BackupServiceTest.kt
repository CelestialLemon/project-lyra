package com.projectlyra.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.projectlyra.app.data.local.LyraDatabase
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.UserEntryEntity
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupServiceTest {
    private lateinit var context: Context
    private lateinit var database: LyraDatabase
    private lateinit var settingsStore: SettingsStore
    private lateinit var service: BackupService
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, LyraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settingsStore = mockk()
        service = BackupService(
            context = context,
            database = database,
            settingsStore = settingsStore,
            json = json,
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun exportToUri_writesOnlyReferencedMedia() {
        runBlocking {
            every {
                settingsStore.settings
            } returns flowOf(
                AppSettings(
                    apiKey = "ignored",
                    reminderEnabled = true,
                    reminderHour = 21,
                    reminderMinute = 15,
                    includeApiKeyInBackup = false,
                )
            )
    
            val referencedMediaId = database.mediaDao().upsertMediaItem(
                MediaItemEntity(
                    tmdbId = 101,
                    mediaType = "TV",
                    title = "Referenced",
                    overview = "Overview",
                    posterPath = "/ref.jpg",
                    releaseOrAirDate = "2024-01-01",
                    metadataUpdatedAt = 100L,
                )
            )
            database.mediaDao().upsertMediaItem(
                MediaItemEntity(
                    tmdbId = 202,
                    mediaType = "MOVIE",
                    title = "Unreferenced",
                    overview = "Overview",
                    posterPath = "/unused.jpg",
                    releaseOrAirDate = "2020-01-01",
                    metadataUpdatedAt = 100L,
                )
            )
            database.userEntryDao().upsertUserEntry(
                UserEntryEntity(
                    mediaItemId = referencedMediaId,
                    status = "WATCHING",
                    addedAt = 1L,
                    updatedAt = 2L,
                )
            )
    
            val file = File.createTempFile("lyra-backup-export", ".json", context.cacheDir)
            val uri = Uri.fromFile(file)
    
            val result = service.exportToUri(uri)
    
            assertTrue(result is BackupOperationResult.Success)
            val exported = json.decodeFromString(LyraBackupDocument.serializer(), file.readText())
            assertEquals(1, exported.mediaItems.size)
            assertEquals(101, exported.mediaItems.first().tmdbId)
            assertEquals(1, exported.userEntries.size)
            file.delete()
        }
    }

    @Test
    fun importFromUri_restoresDataAndSettings() {
        runBlocking {
            coEvery { settingsStore.updateReminder(any(), any(), any()) } returns Unit
            coEvery { settingsStore.updateIncludeApiKeyInBackup(any()) } returns Unit
            coEvery { settingsStore.updateApiKey(any()) } returns true
    
            database.mediaDao().upsertMediaItem(
                MediaItemEntity(
                    tmdbId = 999,
                    mediaType = "MOVIE",
                    title = "Old",
                    overview = "",
                    posterPath = "/old.jpg",
                    releaseOrAirDate = "1999-01-01",
                    metadataUpdatedAt = 1L,
                )
            )
    
            val document = validDocument()
            val file = File.createTempFile("lyra-backup-import", ".json", context.cacheDir)
            file.writeText(json.encodeToString(LyraBackupDocument.serializer(), document))
            val uri = Uri.fromFile(file)
    
            val result = service.importFromUri(uri)
    
            assertTrue(result is BackupOperationResult.Success)
            val mediaItems = database.mediaDao().getAllMediaItems()
            val userEntries = database.userEntryDao().getAllUserEntries()
            val reminderStates = database.episodeReminderStateDao().getAll()
            assertEquals(1, mediaItems.size)
            assertEquals(600, mediaItems.first().tmdbId)
            assertEquals(1, userEntries.size)
            assertEquals(1, reminderStates.size)
    
            coVerify(exactly = 1) { settingsStore.updateReminder(enabled = true, hour = 20, minute = 5) }
            coVerify(exactly = 1) { settingsStore.updateIncludeApiKeyInBackup(false) }
            coVerify(exactly = 0) { settingsStore.updateApiKey(any()) }
            file.delete()
        }
    }

    @Test
    fun importFromUri_returnsErrorForMalformedJson() {
        runBlocking {
            val file = File.createTempFile("lyra-backup-import-bad", ".json", context.cacheDir)
            file.writeText("{bad json")
            val uri = Uri.fromFile(file)
    
            val result = service.importFromUri(uri)
    
            assertTrue(result is BackupOperationResult.Error)
            val message = (result as BackupOperationResult.Error).message
            assertTrue(message.contains("invalid"))
            file.delete()
        }
    }

    @Test
    fun importFromUri_reportsApiKeyEncryptionFailureAfterRestore() {
        runBlocking {
            coEvery { settingsStore.updateReminder(any(), any(), any()) } returns Unit
            coEvery { settingsStore.updateIncludeApiKeyInBackup(any()) } returns Unit
            coEvery { settingsStore.updateApiKey(any()) } returns false
    
            val document = validDocument(
                settings = LyraBackupSettings(
                    reminderEnabled = true,
                    reminderHour = 20,
                    reminderMinute = 5,
                    includeApiKeyInBackup = true,
                    apiKey = "0123456789abcdef0123456789abcdef",
                )
            )
            val file = File.createTempFile("lyra-backup-import-key-fail", ".json", context.cacheDir)
            file.writeText(json.encodeToString(LyraBackupDocument.serializer(), document))
            val uri = Uri.fromFile(file)
    
            val result = service.importFromUri(uri)
    
            assertTrue(result is BackupOperationResult.Error)
            val message = (result as BackupOperationResult.Error).message
            assertTrue(message.contains("API key could not be encrypted"))
            assertEquals(1, database.mediaDao().getAllMediaItems().size)
            coVerify(exactly = 1) { settingsStore.updateApiKey("0123456789abcdef0123456789abcdef") }
            file.delete()
        }
    }

    private fun validDocument(
        settings: LyraBackupSettings = LyraBackupSettings(
            reminderEnabled = true,
            reminderHour = 20,
            reminderMinute = 5,
            includeApiKeyInBackup = false,
            apiKey = null,
        ),
    ): LyraBackupDocument {
        return LyraBackupDocument(
            schemaVersion = LYRA_BACKUP_SCHEMA_VERSION,
            exportedAtEpochMs = 1_000L,
            settings = settings,
            mediaItems = listOf(
                LyraBackupMediaItem(
                    tmdbId = 600,
                    mediaType = "TV",
                    title = "Title",
                    overview = "Overview",
                    posterPath = "/poster.jpg",
                    releaseOrAirDate = "2024-01-01",
                    metadataUpdatedAt = 50L,
                )
            ),
            userEntries = listOf(
                LyraBackupUserEntry(
                    tmdbId = 600,
                    mediaType = "TV",
                    status = "WATCHING",
                    addedAt = 10L,
                    updatedAt = 20L,
                )
            ),
            episodeReminderStates = listOf(
                LyraBackupEpisodeReminderState(
                    tmdbId = 600,
                    mediaType = "TV",
                    lastCheckedAt = 30L,
                    lastKnownEpisodeCount = 12,
                    lastKnownSeasonCount = 2,
                )
            ),
        )
    }
}
