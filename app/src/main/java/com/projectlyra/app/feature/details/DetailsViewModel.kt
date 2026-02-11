package com.projectlyra.app.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.MediaDetailsResult
import com.projectlyra.app.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailsUiState(
    val isLoading: Boolean = true,
    val details: MediaDetails? = null,
    val trackedStatus: WatchStatus? = null,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)

class DetailsViewModel(
    private val tmdbId: Int,
    private val mediaType: MediaType,
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DetailsUiState())
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private val mediaStatusKey = "${mediaType.name}:$tmdbId"

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
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            details = result.details,
                            infoMessage = null,
                            errorMessage = null,
                        )
                    }
                }

                is MediaDetailsResult.MissingApiKey -> {
                    _uiState.update {
                        val hasFallback = result.localFallback != null
                        it.copy(
                            isLoading = false,
                            details = result.localFallback ?: it.details,
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
                        )
                    }
                }

                is MediaDetailsResult.Error -> {
                    _uiState.update {
                        val fallback = result.localFallback
                        it.copy(
                            isLoading = false,
                            details = fallback ?: it.details,
                            infoMessage = if (fallback != null) {
                                "Showing saved details while live metadata is unavailable."
                            } else {
                                null
                            },
                            errorMessage = if (fallback != null) null else result.message,
                        )
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

    private fun observeTrackedStatus() {
        viewModelScope.launch {
            libraryRepository.observeTrackedStatusesByMediaKey().collect { statuses ->
                _uiState.update { it.copy(trackedStatus = statuses[mediaStatusKey]) }
            }
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
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailsViewModel(
            tmdbId = tmdbId,
            mediaType = mediaType,
            libraryRepository = libraryRepository,
            settingsStore = settingsStore,
        ) as T
    }
}
