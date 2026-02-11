package com.projectlyra.app.feature.details

import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.SeasonSummary
import com.projectlyra.app.core.model.TvEpisodeDetails
import com.projectlyra.app.core.model.TvSeasonDetails
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.MediaDetailsResult
import com.projectlyra.app.data.repository.TvSeasonDetailsResult
import com.projectlyra.app.data.settings.AppSettings
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
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
        val fallback = sampleMovieDetails()
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
        } returns MediaDetailsResult.Success(details = sampleMovieDetails())
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

    @Test
    fun refresh_tvDetails_selectsLowestSeasonAndLoadsEpisodes() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery {
            repository.getMediaDetails(apiKey = "key", tmdbId = 99, mediaType = MediaType.TV)
        } returns MediaDetailsResult.Success(details = sampleTvDetails())
        coEvery {
            repository.getTvSeasonDetails(apiKey = "key", tvId = 99, seasonNumber = 0)
        } returns TvSeasonDetailsResult.Success(
            details = sampleSeasonDetails(
                tvId = 99,
                seasonNumber = 0,
                episodes = listOf(
                    TvEpisodeDetails(
                        episodeId = 101,
                        episodeNumber = 1,
                        title = "Pilot",
                        stillPath = null,
                        airDate = "2020-01-01",
                        runtimeMinutes = 40,
                    )
                ),
            )
        )
        coEvery {
            repository.observeWatchedEpisodeNumbersBySeason(tmdbId = 99, seasonNumber = 0)
        } returns flowOf(setOf(1))

        val viewModel = DetailsViewModel(
            tmdbId = 99,
            mediaType = MediaType.TV,
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(0, state.selectedSeasonNumber)
        assertEquals(1, state.selectedSeasonEpisodes.size)
        assertEquals(setOf(1), state.watchedEpisodeNumbers)
    }

    @Test
    fun onSeasonSelected_reusesCachedSeasonPayloadOnReturn() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery {
            repository.getMediaDetails(apiKey = "key", tmdbId = 100, mediaType = MediaType.TV)
        } returns MediaDetailsResult.Success(
            details = sampleTvDetails(
                tmdbId = 100,
                seasons = listOf(
                    SeasonSummary(seasonNumber = 0, name = "Specials", episodeCount = 2, airDate = null, posterPath = null),
                    SeasonSummary(seasonNumber = 1, name = "Season 1", episodeCount = 2, airDate = null, posterPath = null),
                )
            )
        )

        coEvery {
            repository.getTvSeasonDetails(apiKey = "key", tvId = 100, seasonNumber = 0)
        } returns TvSeasonDetailsResult.Success(details = sampleSeasonDetails(tvId = 100, seasonNumber = 0))
        coEvery {
            repository.getTvSeasonDetails(apiKey = "key", tvId = 100, seasonNumber = 1)
        } returns TvSeasonDetailsResult.Success(details = sampleSeasonDetails(tvId = 100, seasonNumber = 1))
        coEvery {
            repository.observeWatchedEpisodeNumbersBySeason(tmdbId = 100, seasonNumber = 0)
        } returns flowOf(emptySet())
        coEvery {
            repository.observeWatchedEpisodeNumbersBySeason(tmdbId = 100, seasonNumber = 1)
        } returns flowOf(emptySet())

        val viewModel = DetailsViewModel(
            tmdbId = 100,
            mediaType = MediaType.TV,
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        viewModel.onSeasonSelected(1)
        advanceUntilIdle()
        viewModel.onSeasonSelected(0)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.getTvSeasonDetails(apiKey = "key", tvId = 100, seasonNumber = 0) }
        coVerify(exactly = 1) { repository.getTvSeasonDetails(apiKey = "key", tvId = 100, seasonNumber = 1) }
    }

    @Test
    fun onEpisodeActions_andSeasonComplete_triggerRepositoryMutations() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery {
            repository.getMediaDetails(apiKey = "key", tmdbId = 101, mediaType = MediaType.TV)
        } returns MediaDetailsResult.Success(
            details = sampleTvDetails(
                tmdbId = 101,
                seasons = listOf(
                    SeasonSummary(seasonNumber = 1, name = "Season 1", episodeCount = 4, airDate = null, posterPath = null),
                )
            )
        )
        coEvery {
            repository.getTvSeasonDetails(apiKey = "key", tvId = 101, seasonNumber = 1)
        } returns TvSeasonDetailsResult.Success(
            details = sampleSeasonDetails(
                tvId = 101,
                seasonNumber = 1,
                episodes = listOf(
                    TvEpisodeDetails(episodeId = 1, episodeNumber = 1, title = "E1", stillPath = null, airDate = "2025-01-01", runtimeMinutes = 45),
                    TvEpisodeDetails(episodeId = 2, episodeNumber = 2, title = "E2", stillPath = null, airDate = null, runtimeMinutes = 45),
                    TvEpisodeDetails(episodeId = 3, episodeNumber = 3, title = "E3", stillPath = null, airDate = "2026-12-20", runtimeMinutes = 45),
                ),
            )
        )
        coEvery {
            repository.observeWatchedEpisodeNumbersBySeason(tmdbId = 101, seasonNumber = 1)
        } returns flowOf(emptySet())
        coEvery { repository.markWatchedUpToEpisode(tmdbId = 101, seasonNumber = 1, episodeNumber = 2) } returns Unit
        coEvery { repository.markUnwatchedFromEpisode(tmdbId = 101, seasonNumber = 1, episodeNumber = 2) } returns Unit
        coEvery {
            repository.markEpisodesWatched(
                tmdbId = 101,
                seasonNumber = 1,
                episodeNumbers = listOf(1, 2),
            )
        } returns Unit

        val viewModel = DetailsViewModel(
            tmdbId = 101,
            mediaType = MediaType.TV,
            libraryRepository = repository,
            settingsStore = settingsStore,
            todayProvider = { LocalDate.of(2026, 2, 11) },
        )
        advanceUntilIdle()

        viewModel.onMarkEpisodeWatchedUpTo(2)
        viewModel.onMarkEpisodeUnwatchedFrom(2)
        viewModel.onMarkSeasonComplete()
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.markWatchedUpToEpisode(tmdbId = 101, seasonNumber = 1, episodeNumber = 2) }
        coVerify(exactly = 1) { repository.markUnwatchedFromEpisode(tmdbId = 101, seasonNumber = 1, episodeNumber = 2) }
        coVerify(exactly = 1) {
            repository.markEpisodesWatched(
                tmdbId = 101,
                seasonNumber = 1,
                episodeNumbers = listOf(1, 2),
            )
        }
    }

    @Test
    fun onEpisodeMutationFailure_setsMutationErrorWithoutSeasonLoadError() = runTest {
        val repository = mockk<LibraryRepository>()
        val settingsStore = mockk<SettingsStore>()
        every { settingsStore.settings } returns flowOf(AppSettings(apiKey = "key"))
        every { repository.observeTrackedStatusesByMediaKey() } returns flowOf(emptyMap())
        coEvery {
            repository.getMediaDetails(apiKey = "key", tmdbId = 102, mediaType = MediaType.TV)
        } returns MediaDetailsResult.Success(
            details = sampleTvDetails(
                tmdbId = 102,
                seasons = listOf(
                    SeasonSummary(seasonNumber = 1, name = "Season 1", episodeCount = 4, airDate = null, posterPath = null),
                )
            )
        )
        coEvery {
            repository.getTvSeasonDetails(apiKey = "key", tvId = 102, seasonNumber = 1)
        } returns TvSeasonDetailsResult.Success(details = sampleSeasonDetails(tvId = 102, seasonNumber = 1))
        coEvery {
            repository.observeWatchedEpisodeNumbersBySeason(tmdbId = 102, seasonNumber = 1)
        } returns flowOf(emptySet())
        coEvery {
            repository.markWatchedUpToEpisode(tmdbId = 102, seasonNumber = 1, episodeNumber = 2)
        } throws IllegalStateException("write failed")

        val viewModel = DetailsViewModel(
            tmdbId = 102,
            mediaType = MediaType.TV,
            libraryRepository = repository,
            settingsStore = settingsStore,
        )
        advanceUntilIdle()

        viewModel.onMarkEpisodeWatchedUpTo(2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.episodeMutationErrorMessage?.contains("Unable to update episode progress") == true)
        assertEquals(null, state.seasonErrorMessage)
        assertTrue(state.selectedSeasonEpisodes.isNotEmpty())
    }

    private fun sampleMovieDetails(): MediaDetails {
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

    private fun sampleTvDetails(
        tmdbId: Int = 99,
        seasons: List<SeasonSummary> = listOf(
            SeasonSummary(seasonNumber = 2, name = "Season 2", episodeCount = 10, airDate = null, posterPath = null),
            SeasonSummary(seasonNumber = 0, name = "Specials", episodeCount = 2, airDate = null, posterPath = null),
            SeasonSummary(seasonNumber = 1, name = "Season 1", episodeCount = 10, airDate = null, posterPath = null),
        ),
    ): MediaDetails {
        return MediaDetails(
            tmdbId = tmdbId,
            mediaType = MediaType.TV,
            title = "TV Title",
            overview = "Overview",
            posterPath = "/poster.jpg",
            backdropPath = null,
            releaseOrAirDate = "2020-01-01",
            numberOfSeasons = 3,
            numberOfEpisodes = 22,
            seasons = seasons,
        )
    }

    private fun sampleSeasonDetails(
        tvId: Int,
        seasonNumber: Int,
        episodes: List<TvEpisodeDetails> = listOf(
            TvEpisodeDetails(
                episodeId = seasonNumber * 10 + 1,
                episodeNumber = 1,
                title = "Episode 1",
                stillPath = null,
                airDate = "2020-01-01",
                runtimeMinutes = 45,
            )
        ),
    ): TvSeasonDetails {
        return TvSeasonDetails(
            tvId = tvId,
            seasonNumber = seasonNumber,
            seasonName = "Season $seasonNumber",
            episodes = episodes,
        )
    }
}
