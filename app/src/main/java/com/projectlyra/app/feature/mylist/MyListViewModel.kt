package com.projectlyra.app.feature.mylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MyListViewModel(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val selectedStatus = MutableStateFlow(WatchStatus.WATCHING)

    val items: StateFlow<List<TrackedItem>> = selectedStatus
        .flatMapLatest(libraryRepository::observeByStatus)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val status: StateFlow<WatchStatus> = selectedStatus

    init {
        viewModelScope.launch {
            libraryRepository.ensureSeedData()
        }
    }

    fun onStatusSelected(status: WatchStatus) {
        selectedStatus.value = status
    }
}

class MyListViewModelFactory(
    private val libraryRepository: LibraryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MyListViewModel(libraryRepository) as T
    }
}
