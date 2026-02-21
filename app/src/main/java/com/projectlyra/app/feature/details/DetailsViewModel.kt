package com.projectlyra.app.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.SeasonSummary
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.TvEpisodeDetails
import com.projectlyra.app.core.model.TvSeasonDetails
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.MediaDetailsResult
import com.projectlyra.app.data.repository.TvSeasonDetailsResult
import com.projectlyra.app.data.settings.SettingsStore
import java.time.LocalDate
import java.time.format.DateTimeParseException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class DetailsContentTab {
    SEASONS,
    CAST,
}

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: MediaDetails? = null,
    val seasons: List<SeasonSummary> = emptyList(),
    val selectedContentTab: DetailsContentTab = DetailsContentTab.CAST,
    val selectedSeasonNumber: Int? = null,
    val selectedSeasonEpisodes: List<TvEpisodeDetails> = emptyList(),
    val watchedEpisodeNumbers: Set<Int> = emptySet(),
    val isSeasonLoading: Boolean = false,
    val seasonErrorMessage: String? = null,
    val episodeMutationErrorMessage: String? = null,
    val trackedStatus: WatchStatus? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

class DetailsViewModel(
    private val tmdbId: Int,
    private val mediaType: MediaType,
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val mediaStatusKey = "${mediaType.name}:$tmdbId"
    private val seasonCache = mutableMapOf<Int, TvSeasonDetails>()
    private var watchedSeasonJob: Job? = null

    init {
        observeTrackedStatus()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }

            val apiKey = settingsStore.settings.first().apiKey
            when (val result = libraryRepository.getMediaDetails(apiKey = apiKey, tmdbId = tmdbId, mediaType = mediaType)) {
                is MediaDetailsResult.Success -> {
                    val sortedSeasons = result.details.seasons.sortedBy { it.seasonNumber }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            details = result.details,
                            seasons = sortedSeasons,
                            infoMessage = null,
                            errorMessage = null,
                            selectedContentTab = if (mediaType == MediaType.TV && sortedSeasons.isNotEmpty()) {
                                DetailsContentTab.SEASONS
                            } else {
                                DetailsContentTab.CAST
                            },
                            selectedSeasonNumber = sortedSeasons.firstOrNull()?.seasonNumber,
                            selectedSeasonEpisodes = emptyList(),
                            watchedEpisodeNumbers = emptySet(),
                            isSeasonLoading = false,
                            seasonErrorMessage = null,
                            episodeMutationErrorMessage = null,
                        )
                    }
                    if (mediaType == MediaType.TV) {
                        initializeSelectedSeason(sortedSeasons)
                    } else {
                        watchedSeasonJob?.cancel()
                    }
                }

                is MediaDetailsResult.MissingApiKey -> {
                    _uiState.update {
                        val hasFallback = result.localFallback != null
                        val fallback = result.localFallback ?: it.details
                        val sortedSeasons = fallback?.seasons?.sortedBy { season -> season.seasonNumber }.orEmpty()
                        it.copy(
                            isLoading = false,
                            details = fallback,
                            seasons = sortedSeasons,
                            infoMessage = if (hasFallback) {
                                "Showing saved details. Add a TMDB API key in Settings for live metadata."
                            } else {
                                null
                            },
                            errorMessage = if (hasFallback) {
                                null
                            } else {
                                "Set a TMDB API key in Settings to load title details."
                            },
                            selectedContentTab = if (mediaType == MediaType.TV && sortedSeasons.isNotEmpty()) {
                                DetailsContentTab.SEASONS
                            } else {
                                DetailsContentTab.CAST
                            },
                            selectedSeasonNumber = sortedSeasons.firstOrNull()?.seasonNumber,
                            selectedSeasonEpisodes = emptyList(),
                            watchedEpisodeNumbers = emptySet(),
                            isSeasonLoading = false,
                            seasonErrorMessage = null,
                            episodeMutationErrorMessage = null,
                        )
                    }
                    if (mediaType == MediaType.TV) {
                        initializeSelectedSeason(_uiState.value.seasons)
                    } else {
                        watchedSeasonJob?.cancel()
                    }
                }

                is MediaDetailsResult.Error -> {
                    _uiState.update {
                        val fallback = result.localFallback
                        val details = fallback ?: it.details
                        val sortedSeasons = details?.seasons?.sortedBy { season -> season.seasonNumber }.orEmpty()
                        it.copy(
                            isLoading = false,
                            details = details,
                            seasons = sortedSeasons,
                            infoMessage = if (fallback != null) {
                                "Showing saved details while live metadata is unavailable."
                            } else {
                                null
                            },
                            errorMessage = if (fallback != null) null else result.message,
                            selectedContentTab = if (mediaType == MediaType.TV && sortedSeasons.isNotEmpty()) {
                                DetailsContentTab.SEASONS
                            } else {
                                DetailsContentTab.CAST
                            },
                            selectedSeasonNumber = sortedSeasons.firstOrNull()?.seasonNumber,
                            selectedSeasonEpisodes = emptyList(),
                            watchedEpisodeNumbers = emptySet(),
                            isSeasonLoading = false,
                            seasonErrorMessage = null,
                            episodeMutationErrorMessage = null,
                        )
                    }
                    if (mediaType == MediaType.TV) {
                        initializeSelectedSeason(_uiState.value.seasons)
                    } else {
                        watchedSeasonJob?.cancel()
                    }
                }
            }
        }
    }

    fun onStatusSelected(status: WatchStatus?) {
        val details = _uiState.value.details
        if (status != null && details == null) {
            return
        }
        if (_uiState.value.trackedStatus == status) {
            return
        }

        _uiState.update { it.copy(trackedStatus = status) }
        viewModelScope.launch {
            if (status == null) {
                libraryRepository.clearTrackedStatus(tmdbId = tmdbId, mediaType = mediaType)
            } else {
                libraryRepository.upsertTrackedStatus(item = details!!.asTrendingItem(), status = status)
            }
        }
    }

    fun onContentTabSelected(tab: DetailsContentTab) {
        _uiState.update { it.copy(selectedContentTab = tab) }
    }

    fun onSeasonSelected(seasonNumber: Int) {
        val seasons = _uiState.value.seasons
        if (seasons.none { it.seasonNumber == seasonNumber }) {
            return
        }
        if (_uiState.value.selectedSeasonNumber == seasonNumber) {
            return
        }
        _uiState.update {
            it.copy(
                selectedSeasonNumber = seasonNumber,
                selectedSeasonEpisodes = seasonCache[seasonNumber]?.episodes.orEmpty(),
                watchedEpisodeNumbers = emptySet(),
                seasonErrorMessage = null,
                episodeMutationErrorMessage = null,
                isSeasonLoading = seasonNumber !in seasonCache,
            )
        }
        observeWatchedSeason(seasonNumber)
        loadSeasonIfNeeded(seasonNumber = seasonNumber, forceRefresh = false)
    }

    fun retrySelectedSeason() {
        val seasonNumber = _uiState.value.selectedSeasonNumber ?: return
        _uiState.update { it.copy(seasonErrorMessage = null, episodeMutationErrorMessage = null) }
        loadSeasonIfNeeded(seasonNumber = seasonNumber, forceRefresh = true)
    }

    fun onMarkEpisodeWatchedUpTo(episodeNumber: Int) {
        val seasonNumber = _uiState.value.selectedSeasonNumber ?: return
        if (episodeNumber <= 0) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(episodeMutationErrorMessage = null) }
            runCatching {
                libraryRepository.markWatchedUpToEpisode(
                    tmdbId = tmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                )
            }.onSuccess {
                _uiState.update { state -> state.copy(episodeMutationErrorMessage = null) }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(episodeMutationErrorMessage = "Unable to update episode progress right now.")
                }
            }
        }
    }

    fun onMarkEpisodeUnwatchedFrom(episodeNumber: Int) {
        val seasonNumber = _uiState.value.selectedSeasonNumber ?: return
        if (episodeNumber <= 0) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(episodeMutationErrorMessage = null) }
            runCatching {
                libraryRepository.markUnwatchedFromEpisode(
                    tmdbId = tmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                )
            }.onSuccess {
                _uiState.update { state -> state.copy(episodeMutationErrorMessage = null) }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(episodeMutationErrorMessage = "Unable to update episode progress right now.")
                }
            }
        }
    }

    fun onMarkSeasonComplete() {
        val seasonNumber = _uiState.value.selectedSeasonNumber ?: return
        val season = seasonCache[seasonNumber] ?: return
        val today = todayProvider()
        val eligibleEpisodes = season.episodes
            .filter { episode -> isEligibleForSeasonComplete(episode, today) }
            .map { episode -> episode.episodeNumber }
        if (eligibleEpisodes.isEmpty()) {
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(episodeMutationErrorMessage = null) }
            runCatching {
                libraryRepository.markEpisodesWatched(
                    tmdbId = tmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumbers = eligibleEpisodes,
                )
            }.onSuccess {
                _uiState.update { state -> state.copy(episodeMutationErrorMessage = null) }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(episodeMutationErrorMessage = "Unable to mark this season as complete right now.")
                }
            }
        }
    }

    private fun observeTrackedStatus() {
        viewModelScope.launch {
            libraryRepository.observeTrackedStatusesByMediaKey().collect { statuses ->
                _uiState.update { it.copy(trackedStatus = statuses[mediaStatusKey]) }
            }
        }
    }

    private fun initializeSelectedSeason(seasons: List<SeasonSummary>) {
        val selected = seasons.firstOrNull()?.seasonNumber
        if (selected == null) {
            watchedSeasonJob?.cancel()
            _uiState.update {
                it.copy(
                    selectedSeasonNumber = null,
                    selectedSeasonEpisodes = emptyList(),
                    watchedEpisodeNumbers = emptySet(),
                    isSeasonLoading = false,
                    seasonErrorMessage = null,
                    episodeMutationErrorMessage = null,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                selectedContentTab = DetailsContentTab.SEASONS,
                selectedSeasonNumber = selected,
                selectedSeasonEpisodes = seasonCache[selected]?.episodes.orEmpty(),
                watchedEpisodeNumbers = emptySet(),
                isSeasonLoading = selected !in seasonCache,
                seasonErrorMessage = null,
                episodeMutationErrorMessage = null,
            )
        }
        observeWatchedSeason(selected)
        loadSeasonIfNeeded(seasonNumber = selected, forceRefresh = false)
    }

    private fun observeWatchedSeason(seasonNumber: Int) {
        watchedSeasonJob?.cancel()
        watchedSeasonJob = viewModelScope.launch {
            val flow = libraryRepository.observeWatchedEpisodeNumbersBySeason(
                tmdbId = tmdbId,
                seasonNumber = seasonNumber,
            )
            flow.collect { watched ->
                _uiState.update { state ->
                    if (state.selectedSeasonNumber != seasonNumber) {
                        state
                    } else {
                        state.copy(watchedEpisodeNumbers = watched)
                    }
                }
            }
        }
    }

    private fun loadSeasonIfNeeded(seasonNumber: Int, forceRefresh: Boolean) {
        if (!forceRefresh && seasonNumber in seasonCache) {
            _uiState.update { state ->
                if (state.selectedSeasonNumber != seasonNumber) {
                    state
                } else {
                    state.copy(
                        selectedSeasonEpisodes = seasonCache[seasonNumber]?.episodes.orEmpty(),
                        isSeasonLoading = false,
                        seasonErrorMessage = null,
                        episodeMutationErrorMessage = null,
                    )
                }
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                if (state.selectedSeasonNumber != seasonNumber) {
                    state
                } else {
                    state.copy(
                        isSeasonLoading = true,
                        seasonErrorMessage = null,
                        episodeMutationErrorMessage = null,
                        selectedSeasonEpisodes = if (forceRefresh) emptyList() else state.selectedSeasonEpisodes,
                    )
                }
            }

            val apiKey = settingsStore.settings.first().apiKey
            when (
                val result = libraryRepository.getTvSeasonDetails(
                    apiKey = apiKey,
                    tvId = tmdbId,
                    seasonNumber = seasonNumber,
                )
            ) {
                is TvSeasonDetailsResult.Success -> {
                    seasonCache[seasonNumber] = result.details
                    _uiState.update { state ->
                        if (state.selectedSeasonNumber != seasonNumber) {
                            state
                        } else {
                            state.copy(
                                selectedSeasonEpisodes = result.details.episodes,
                                isSeasonLoading = false,
                                seasonErrorMessage = null,
                                episodeMutationErrorMessage = null,
                            )
                        }
                    }
                }

                is TvSeasonDetailsResult.MissingApiKey -> {
                    _uiState.update { state ->
                        if (state.selectedSeasonNumber != seasonNumber) {
                            state
                        } else {
                            state.copy(
                                isSeasonLoading = false,
                                selectedSeasonEpisodes = emptyList(),
                                seasonErrorMessage = "Set a TMDB API key in Settings to load season episodes.",
                            )
                        }
                    }
                }

                is TvSeasonDetailsResult.Error -> {
                    _uiState.update { state ->
                        if (state.selectedSeasonNumber != seasonNumber) {
                            state
                        } else {
                            state.copy(
                                isSeasonLoading = false,
                                selectedSeasonEpisodes = emptyList(),
                                seasonErrorMessage = result.message,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isEligibleForSeasonComplete(episode: TvEpisodeDetails, today: LocalDate): Boolean {
        val rawAirDate = episode.airDate?.trim().orEmpty()
        if (rawAirDate.isBlank()) {
            return true
        }
        return try {
            !LocalDate.parse(rawAirDate).isAfter(today)
        } catch (_: DateTimeParseException) {
            true
        }
    }

    private fun MediaDetails.asTrendingItem(): TrendingItem {
        return TrendingItem(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            overview = overview,
            posterPath = posterPath,
            releaseOrAirDate = releaseOrAirDate,
            genreIds = genreIds,
        )
    }
}

class DetailsViewModelFactory(
    private val tmdbId: Int,
    private val mediaType: MediaType,
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
    private val todayProvider: () -> LocalDate = { LocalDate.now() },
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailsViewModel(
            tmdbId = tmdbId,
            mediaType = mediaType,
            libraryRepository = libraryRepository,
            settingsStore = settingsStore,
            todayProvider = todayProvider,
        ) as T
    }
}
