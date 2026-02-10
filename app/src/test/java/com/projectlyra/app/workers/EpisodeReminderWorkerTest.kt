package com.projectlyra.app.workers

import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.projectlyra.app.LyraApplication
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.di.AppContainer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.net.UnknownHostException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EpisodeReminderWorkerTest {
    private lateinit var app: LyraApplication
    private lateinit var container: AppContainer
    private lateinit var repository: LibraryRepository
    private lateinit var settingsStore: SettingsStore

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        container = mockk()
        repository = mockk()
        settingsStore = mockk()

        every { container.libraryRepository } returns repository
        every { container.settingsStore } returns settingsStore

        val field = LyraApplication::class.java.getDeclaredField("container")
        field.isAccessible = true
        field.set(app, container)
    }

    @Test
    fun doWork_returnsSuccessWhenReminderDisabled() = runBlocking {
        every { settingsStore.settings } returns flowOf(AppSettings(reminderEnabled = false, apiKey = "abc"))

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        coVerify(exactly = 0) { repository.getTvReminderCandidates() }
    }

    @Test
    fun doWork_returnsSuccessWhenApiKeyMissing() = runBlocking {
        every { settingsStore.settings } returns flowOf(AppSettings(reminderEnabled = true, apiKey = "  "))

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
        coVerify(exactly = 0) { repository.getTvReminderCandidates() }
    }

    @Test
    fun doWork_returnsSuccessWhenNoCandidates() = runBlocking {
        every { settingsStore.settings } returns flowOf(AppSettings(reminderEnabled = true, apiKey = "api-key"))
        coEvery { repository.getTvReminderCandidates() } returns emptyList()

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Success)
    }

    @Test
    fun doWork_returnsRetryForRetryableFailures() = runBlocking {
        every { settingsStore.settings } returns flowOf(AppSettings(reminderEnabled = true, apiKey = "api-key"))
        coEvery { repository.getTvReminderCandidates() } returns listOf(
            com.projectlyra.app.data.repository.ReminderTrackedShow(
                localId = 1L,
                tmdbId = 42,
                title = "Show",
                status = WatchStatus.WATCHING,
            )
        )
        coEvery {
            repository.getMediaDetails(
                apiKey = any(),
                tmdbId = 42,
                mediaType = MediaType.TV,
            )
        } throws UnknownHostException("offline")

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Retry)
    }

    @Test
    fun doWork_returnsFailureForNonRetryableFailures() = runBlocking {
        every { settingsStore.settings } returns flowOf(AppSettings(reminderEnabled = true, apiKey = "api-key"))
        coEvery { repository.getTvReminderCandidates() } returns listOf(
            com.projectlyra.app.data.repository.ReminderTrackedShow(
                localId = 1L,
                tmdbId = 42,
                title = "Show",
                status = WatchStatus.WATCHING,
            )
        )
        coEvery {
            repository.getMediaDetails(
                apiKey = any(),
                tmdbId = 42,
                mediaType = MediaType.TV,
            )
        } throws IllegalStateException("boom")

        val result = buildWorker().doWork()

        assertTrue(result is ListenableWorker.Result.Failure)
    }

    private fun buildWorker(): EpisodeReminderWorker {
        return TestListenableWorkerBuilder<EpisodeReminderWorker>(app).build()
    }
}
