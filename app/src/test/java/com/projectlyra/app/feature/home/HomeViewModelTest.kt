package com.projectlyra.app.feature.home

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.MovieRecommendationRequest
import com.projectlyra.app.data.repository.RecommendationResult
import com.projectlyra.app.data.repository.TvRecommendationRequest
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun init_loadsResumeHeroAndRecommendations() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        val hero = sampleTrackedItem(status = WatchStatus.WATCHING)
        val movieItems = listOf(sampleTrendingItem(tmdbId = 21, mediaType = MediaType.MOVIE))
        val tvItems = listOf(sampleTrendingItem(tmdbId = 31, mediaType = MediaType.TV))

        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        coEvery { repository.getResumeHeroCandidate() } returns hero
        coEvery {
            repository.getTopCompletedGenreIds(mediaType = MediaType.MOVIE, limit = 3)
        } returns listOf(878, 35, 12)
        coEvery {
            repository.getTopCompletedGenreIds(mediaType = MediaType.TV, limit = 3)
        } returns listOf(16, 10765, 18)
        coEvery {
            repository.getRecommendedMovies(
                apiKey = "key",
                request = MovieRecommendationRequest(genreIds = listOf(878, 35, 12)),
            )
        } returns RecommendationResult.Success(
            items = movieItems,
            isPersonalized = true,
            usedFallback = false,
        )
        coEvery {
            repository.getRecommendedTvShows(
                apiKey = "key",
                request = TvRecommendationRequest(genreIds = listOf(16, 10765, 18)),
            )
        } returns RecommendationResult.Success(
            items = tvItems,
            isPersonalized = true,
            usedFallback = false,
        )

        val viewModel = HomeViewModel(
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(hero, state.resumeHero)
        assertEquals(movieItems, state.movieRail.items)
        assertEquals(tvItems, state.tvRail.items)
        assertNull(state.errorMessage)
        assertTrue(!state.isLoading)
    }

    @Test
    fun init_whenMissingApiKey_usesFallbackRailsWithoutCrashing() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        val fallbackMovie = sampleTrendingItem(tmdbId = 42, mediaType = MediaType.MOVIE)

        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = ""))
        coEvery { repository.getResumeHeroCandidate() } returns null
        coEvery {
            repository.getTopCompletedGenreIds(mediaType = MediaType.MOVIE, limit = 3)
        } returns emptyList()
        coEvery {
            repository.getTopCompletedGenreIds(mediaType = MediaType.TV, limit = 3)
        } returns emptyList()
        coEvery {
            repository.getRecommendedMovies(
                apiKey = "",
                request = MovieRecommendationRequest(genreIds = emptyList()),
            )
        } returns RecommendationResult.MissingApiKey(
            fallbackItems = listOf(fallbackMovie),
            message = "Set a TMDB API key in Settings to load live recommendations.",
        )
        coEvery {
            repository.getRecommendedTvShows(
                apiKey = "",
                request = TvRecommendationRequest(genreIds = emptyList()),
            )
        } returns RecommendationResult.MissingApiKey(
            fallbackItems = emptyList(),
            message = "Set a TMDB API key in Settings to load live recommendations.",
        )

        val viewModel = HomeViewModel(
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.movieRail.items.size)
        assertTrue(state.movieRail.errorMessage == null)
        assertTrue(state.tvRail.errorMessage!!.contains("TMDB API key"))
        assertTrue(!state.isLoading)
    }

    private fun sampleTrendingItem(tmdbId: Int, mediaType: MediaType): TrendingItem {
        return TrendingItem(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = "Title",
            overview = "Overview",
            posterPath = "/poster.jpg",
            releaseOrAirDate = "2024-01-01",
        )
    }

    private fun sampleTrackedItem(status: WatchStatus): TrackedItem {
        return TrackedItem(
            localId = 1L,
            tmdbId = 100,
            mediaType = MediaType.TV,
            title = "Hero",
            overview = "Overview",
            posterPath = "/hero.jpg",
            releaseOrAirDate = "2020-01-01",
            status = status,
            updatedAt = 123L,
        )
    }
}
