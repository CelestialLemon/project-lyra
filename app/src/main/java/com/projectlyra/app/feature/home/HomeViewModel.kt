package com.projectlyra.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.TrendingRefreshResult
import com.projectlyra.app.data.settings.SettingsStore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class HomeUiState(
    val trending: List<TrendingItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isShowingCachedData: Boolean = false,
    val trackedStatusesByMediaKey: Map<String, WatchStatus> = emptyMap(),
)

class HomeViewModel(
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeTrackedStatuses()
        refreshTrending(forceRefresh = false)
    }

    fun retry() {
        refreshTrending(forceRefresh = true)
    }

    fun onStatusSelected(item: TrendingItem, status: WatchStatus) {
        viewModelScope.launch {
            libraryRepository.upsertTrackedStatus(item = item, status = status)
        }
    }

    fun trackedStatusFor(item: TrendingItem): WatchStatus? {
        val key = "${item.mediaType.name}:${item.tmdbId}"
        return uiState.value.trackedStatusesByMediaKey[key]
    }

    private fun refreshTrending(forceRefresh: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val apiKey = settingsStore.settings.first().apiKey
            when (val result = libraryRepository.refreshTrending(apiKey = apiKey, forceRefresh = forceRefresh)) {
                is TrendingRefreshResult.Success -> {
                    _uiState.value = HomeUiState(
                        trending = result.items,
                        isLoading = false,
                        errorMessage = null,
                        isShowingCachedData = result.fromCache,
                        trackedStatusesByMediaKey = _uiState.value.trackedStatusesByMediaKey,
                    )
                }

                is TrendingRefreshResult.MissingApiKey -> {
                    _uiState.value = HomeUiState(
                        trending = result.cachedItems,
                        isLoading = false,
                        errorMessage = "Set a TMDB API key in Settings to load live trending.",
                        isShowingCachedData = result.cachedItems.isNotEmpty(),
                        trackedStatusesByMediaKey = _uiState.value.trackedStatusesByMediaKey,
                    )
                }

                is TrendingRefreshResult.Error -> {
                    _uiState.value = HomeUiState(
                        trending = result.cachedItems,
                        isLoading = false,
                        errorMessage = result.message,
                        isShowingCachedData = result.cachedItems.isNotEmpty(),
                        trackedStatusesByMediaKey = _uiState.value.trackedStatusesByMediaKey,
                    )
                }
            }
        }
    }

    private fun observeTrackedStatuses() {
        viewModelScope.launch {
            libraryRepository.observeTrackedStatusesByMediaKey().collect { trackedStatusesByMediaKey ->
                _uiState.value = _uiState.value.copy(trackedStatusesByMediaKey = trackedStatusesByMediaKey)
            }
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
