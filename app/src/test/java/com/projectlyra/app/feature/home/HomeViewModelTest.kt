package com.projectlyra.app.feature.home

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.TrendingRefreshResult
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.testutil.MainDispatcherRule
import io.mockk.coEvery
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
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_whenMissingApiKey_showsCachedDataMessage() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        val cached = listOf(sampleTrendingItem())
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = ""))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery {
            repository.refreshTrending(apiKey = "", forceRefresh = false)
        } returns TrendingRefreshResult.MissingApiKey(cachedItems = cached)

        val viewModel = HomeViewModel(
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(cached, state.trending)
        assertTrue(state.errorMessage!!.contains("TMDB API key"))
        assertTrue(state.isShowingCachedData)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun init_observesTrackedStatuses() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        val trackedMap = mapOf("MOVIE:1" to WatchStatus.WATCHING)
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(trackedMap)
        coEvery {
            repository.refreshTrending(apiKey = "key", forceRefresh = false)
        } returns TrendingRefreshResult.Success(items = emptyList(), fromCache = false)

        val viewModel = HomeViewModel(
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        assertEquals(trackedMap, viewModel.uiState.value.trackedStatusesByMediaKey)
    }

    private fun sampleTrendingItem(): TrendingItem {
        return TrendingItem(
            tmdbId = 1,
            mediaType = MediaType.MOVIE,
            title = "Title",
            overview = "Overview",
            posterPath = "/poster.jpg",
            releaseOrAirDate = "2024-01-01",
        )
    }
}
