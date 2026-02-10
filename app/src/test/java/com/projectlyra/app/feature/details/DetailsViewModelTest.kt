package com.projectlyra.app.feature.details

import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.MediaDetailsResult
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
class DetailsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun refresh_missingApiKeyWithFallback_showsInfoMessage() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        val fallback = sampleDetails()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = ""))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery {
            repository.getMediaDetails(apiKey = "", tmdbId = 7, mediaType = MediaType.MOVIE)
        } returns MediaDetailsResult.MissingApiKey(localFallback = fallback)

        val viewModel = DetailsViewModel(
            tmdbId = 7,
            mediaType = MediaType.MOVIE,
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Local Title", state.details?.title)
        assertTrue(state.infoMessage!!.contains("saved details"))
        assertEquals(null, state.errorMessage)
    }

    @Test
    fun onStatusSelected_null_clearsTrackedStatus() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(mapOf("MOVIE:7" to WatchStatus.WATCHING))
        coEvery {
            repository.getMediaDetails(apiKey = "key", tmdbId = 7, mediaType = MediaType.MOVIE)
        } returns MediaDetailsResult.Success(details = sampleDetails())
        coEvery { repository.clearTrackedStatus(tmdbId = 7, mediaType = MediaType.MOVIE) } returns Unit

        val viewModel = DetailsViewModel(
            tmdbId = 7,
            mediaType = MediaType.MOVIE,
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()
        assertEquals(WatchStatus.WATCHING, viewModel.uiState.value.trackedStatus)

        viewModel.onStatusSelected(null)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.clearTrackedStatus(tmdbId = 7, mediaType = MediaType.MOVIE) }
    }

    private fun sampleDetails(): MediaDetails {
        return MediaDetails(
            tmdbId = 7,
            mediaType = MediaType.MOVIE,
            title = "Local Title",
            overview = "Overview",
            posterPath = "/poster.jpg",
            backdropPath = null,
            releaseOrAirDate = "2024-01-01",
        )
    }
}
