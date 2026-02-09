package com.projectlyra.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.data.repository.LibraryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel(
    private val libraryRepository: LibraryRepository,
) : ViewModel() {
    private val _trending = MutableStateFlow(libraryRepository.seedTrending())
    val trending: StateFlow<List<TrendingItem>> = _trending.asStateFlow()
}

class HomeViewModelFactory(
    private val libraryRepository: LibraryRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(libraryRepository) as T
    }
}
