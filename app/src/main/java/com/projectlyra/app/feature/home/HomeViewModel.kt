package com.projectlyra.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.MovieRecommendationRequest
import com.projectlyra.app.data.repository.RecommendationResult
import com.projectlyra.app.data.repository.TvRecommendationRequest
import com.projectlyra.app.data.settings.SettingsStore
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RecommendationRailUiState(
    val items: List<TrendingItem> = emptyList(),
    val isLoading: Boolean = true,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

data class HomeUiState(
    val resumeHero: TrackedItem? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val movieRail: RecommendationRailUiState = RecommendationRailUiState(),
    val tvRail: RecommendationRailUiState = RecommendationRailUiState(),
)

class HomeViewModel(
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        refreshHome()
    }

    fun retry() {
        refreshHome()
    }

    private fun refreshHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState(
                resumeHero = _uiState.value.resumeHero,
                isLoading = true,
            )

            val apiKey = settingsStore.settings.first().apiKey

            try {
                val resumeDeferred = async { libraryRepository.getResumeHeroCandidate() }
                val movieGenresDeferred = async {
                    libraryRepository.getTopCompletedGenreIds(
                        mediaType = MediaType.MOVIE,
                        limit = 3,
                    )
                }
                val tvGenresDeferred = async {
                    libraryRepository.getTopCompletedGenreIds(
                        mediaType = MediaType.TV,
                        limit = 3,
                    )
                }

                val movieDeferred = async {
                    libraryRepository.getRecommendedMovies(
                        apiKey = apiKey,
                        request = MovieRecommendationRequest(genreIds = movieGenresDeferred.await()),
                    )
                }
                val tvDeferred = async {
                    libraryRepository.getRecommendedTvShows(
                        apiKey = apiKey,
                        request = TvRecommendationRequest(genreIds = tvGenresDeferred.await()),
                    )
                }

                _uiState.value = HomeUiState(
                    resumeHero = resumeDeferred.await(),
                    isLoading = false,
                    errorMessage = null,
                    movieRail = movieDeferred.await().toRailState(),
                    tvRail = tvDeferred.await().toRailState(),
                )
            } catch (error: Throwable) {
                if (error is CancellationException) {
                    throw error
                }
                _uiState.value = HomeUiState(
                    resumeHero = _uiState.value.resumeHero,
                    isLoading = false,
                    errorMessage = "Unable to load Home right now. Please retry.",
                    movieRail = RecommendationRailUiState(isLoading = false),
                    tvRail = RecommendationRailUiState(isLoading = false),
                )
            }
        }
    }

    private fun RecommendationResult.toRailState(): RecommendationRailUiState {
        return when (this) {
            is RecommendationResult.Success -> RecommendationRailUiState(
                items = items,
                isLoading = false,
                infoMessage = infoMessage,
                errorMessage = null,
            )

            is RecommendationResult.MissingApiKey -> RecommendationRailUiState(
                items = fallbackItems,
                isLoading = false,
                infoMessage = if (fallbackItems.isNotEmpty()) message else null,
                errorMessage = if (fallbackItems.isEmpty()) message else null,
            )

            is RecommendationResult.Error -> RecommendationRailUiState(
                items = fallbackItems,
                isLoading = false,
                infoMessage = if (fallbackItems.isNotEmpty()) {
                    "Showing fallback picks while recommendations recover."
                } else {
                    null
                },
                errorMessage = if (fallbackItems.isEmpty()) message else null,
            )
        }
    }
}

class HomeViewModelFactory(
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(libraryRepository, settingsStore) as T
    }
}
