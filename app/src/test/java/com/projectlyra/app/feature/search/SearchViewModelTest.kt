package com.projectlyra.app.feature.search

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.SearchResult
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun search_withMissingApiKey_setsHelpfulError() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = ""))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery { repository.searchTitles(apiKey = "", query = "matrix") } returns SearchResult.MissingApiKey

        val viewModel = SearchViewModel(
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        viewModel.onQueryChange("matrix")
        advanceTimeBy(500)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.hasSearched)
        assertTrue(state.errorMessage!!.contains("TMDB API key"))
        assertEquals(0, state.results.size)
    }

    @Test
    fun retry_reissuesSearchAndUpdatesResults() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery { repository.searchTitles(apiKey = "key", query = "matrix") } returnsMany listOf(
            SearchResult.Error(message = "Network down"),
            SearchResult.Success(items = listOf(sampleTrending())),
        )

        val viewModel = SearchViewModel(
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        viewModel.onQueryChange("matrix")
        advanceTimeBy(500)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Network down"))

        viewModel.retry()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.results.size)
        assertEquals("Result", viewModel.uiState.value.results.first().title)
        assertEquals(null, viewModel.uiState.value.errorMessage)
    }

    private fun sampleTrending(): TrendingItem {
        return TrendingItem(
            tmdbId = 99,
            mediaType = MediaType.MOVIE,
            title = "Result",
            overview = "Overview",
            posterPath = "/poster.jpg",
            releaseOrAirDate = "2025-01-01",
        )
    }
}
