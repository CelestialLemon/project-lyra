package com.projectlyra.app.feature.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModel(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val selectedStatus = MutableStateFlow(WatchStatus.WATCHING)
    private val selectedMediaFilter = MutableStateFlow(MediaFilter.BOTH)

    val items: StateFlow<List<TrackedItem>> = selectedStatus
        .flatMapLatest(libraryRepository::observeByStatus)
        .combine(selectedMediaFilter, ::filterItems)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val status: StateFlow<WatchStatus> = selectedStatus
    val mediaFilter: StateFlow<MediaFilter> = selectedMediaFilter

    fun onStatusSelected(status: WatchStatus) {
        selectedStatus.value = status
    }

    fun onMediaFilterSelected(mediaFilter: MediaFilter) {
        selectedMediaFilter.value = mediaFilter
    }

    fun onItemStatusChange(item: TrackedItem, status: WatchStatus) {
        if (item.status == status) {
            return
        }

        viewModelScope.launch {
            libraryRepository.updateTrackedStatus(
                mediaItemId = item.localId,
                status = status,
            )
        }
    }

    fun onItemRemoved(item: TrackedItem) {
        viewModelScope.launch {
            libraryRepository.removeTrackedItem(mediaItemId = item.localId)
        }
    }

    private fun filterItems(items: List<TrackedItem>, mediaFilter: MediaFilter): List<TrackedItem> {
        return when (mediaFilter) {
            MediaFilter.BOTH -> items
            MediaFilter.MOVIES -> items.filter { item -> item.mediaType == MediaType.MOVIE }
            MediaFilter.SHOWS -> items.filter { item -> item.mediaType == MediaType.TV }
        }
    }
}

enum class MediaFilter(val label: String) {
    BOTH("Both"),
    MOVIES("Movies"),
    SHOWS("Shows"),
}

class MyListViewModelFactory(
    private val libraryRepository: LibraryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MyListViewModel(libraryRepository) as T
    }
}
