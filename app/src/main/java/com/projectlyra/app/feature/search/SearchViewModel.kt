package com.projectlyra.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.repository.SearchResult
import com.projectlyra.app.data.settings.SettingsStore
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val results: List<TrendingItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasSearched: Boolean = false,
    val trackedStatusesByMediaKey: Map<String, WatchStatus> = emptyMap(),
)

class SearchViewModel(
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    private val retryRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        observeTrackedStatuses()
        observeSearchQuery()
    }

    fun onQueryChange(rawQuery: String) {
        val previousNormalizedQuery = uiState.value.query.trim()
        val trimmedQuery = rawQuery.trim()
        if (trimmedQuery.isEmpty()) {
            _uiState.update {
                it.copy(
                    query = rawQuery,
                    results = emptyList(),
                    isLoading = false,
                    errorMessage = null,
                    hasSearched = false,
                )
            }
        } else if (trimmedQuery == previousNormalizedQuery) {
            _uiState.update {
                it.copy(
                    query = rawQuery,
                    errorMessage = null,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    query = rawQuery,
                    results = emptyList(),
                    errorMessage = null,
                    hasSearched = false,
                )
            }
        }
        queryFlow.value = rawQuery
    }

    fun onStatusSelected(item: TrendingItem, status: WatchStatus) {
        if (trackedStatusFor(item) == status) {
            return
        }

        viewModelScope.launch {
            libraryRepository.upsertTrackedStatus(item = item, status = status)
        }
    }

    fun trackedStatusFor(item: TrendingItem): WatchStatus? {
        val key = "${item.mediaType.name}:${item.tmdbId}"
        return uiState.value.trackedStatusesByMediaKey[key]
    }

    fun retry() {
        if (uiState.value.query.trim().isEmpty()) {
            return
        }
        retryRequests.tryEmit(Unit)
    }

    @OptIn(FlowPreview::class)
    private fun observeSearchQuery() {
        viewModelScope.launch {
            merge(
                queryFlow
                    .map { query -> query.trim() }
                    .debounce(450)
                    .distinctUntilChanged(),
                retryRequests.map { uiState.value.query.trim() },
            )
                .collectLatest { query ->
                    runSearch(query = query)
                }
        }
    }

    private suspend fun runSearch(query: String) {
        if (query.isEmpty()) {
            _uiState.update {
                it.copy(
                    results = emptyList(),
                    isLoading = false,
                    errorMessage = null,
                    hasSearched = false,
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
                hasSearched = true,
                results = emptyList(),
            )
        }

        val apiKey = settingsStore.settings.first().apiKey
        val result = libraryRepository.searchTitles(apiKey = apiKey, query = query)
        if (uiState.value.query.trim() != query) {
            return
        }

        when (result) {
            is SearchResult.Success -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = result.items,
                        errorMessage = null,
                        hasSearched = true,
                    )
                }
            }

            SearchResult.MissingApiKey -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = emptyList(),
                        errorMessage = "Set a TMDB API key in Settings to search movies and TV shows.",
                        hasSearched = true,
                    )
                }
            }

            is SearchResult.Error -> {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        results = emptyList(),
                        errorMessage = result.message,
                        hasSearched = true,
                    )
                }
            }
        }
    }

    private fun observeTrackedStatuses() {
        viewModelScope.launch {
            libraryRepository.observeTrackedStatusesByMediaKey().collect { trackedStatusesByMediaKey ->
                _uiState.update { it.copy(trackedStatusesByMediaKey = trackedStatusesByMediaKey) }
            }
        }
    }
}

class SearchViewModelFactory(
    private val libraryRepository: LibraryRepository,
    private val settingsStore: SettingsStore,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SearchViewModel(
            libraryRepository = libraryRepository,
            settingsStore = settingsStore,
        ) as T
    }
}
