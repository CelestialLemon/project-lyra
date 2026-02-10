package com.projectlyra.app.feature.mylist

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.testutil.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MyListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun onStatusSelected_switchesObservedFlow() = runTest {
        val repository = mockk<LibraryRepository>()
        every { repository.observeByStatus(WatchStatus.WATCHING) } returns flowOf(listOf(sampleTrackedItem(1, WatchStatus.WATCHING)))
        every { repository.observeByStatus(WatchStatus.ON_HOLD) } returns flowOf(listOf(sampleTrackedItem(2, WatchStatus.ON_HOLD)))

        val viewModel = MyListViewModel(repository)
        assertEquals(1, viewModel.items.first { it.isNotEmpty() }.first().tmdbId)

        viewModel.onStatusSelected(WatchStatus.ON_HOLD)
        advanceUntilIdle()

        assertEquals(
            2,
            viewModel.items.first { rows ->
                rows.firstOrNull()?.status == WatchStatus.ON_HOLD
            }.first().tmdbId,
        )
    }

    @Test
    fun onItemStatusChange_updatesRepositoryOnlyWhenChanged() = runTest {
        val repository = mockk<LibraryRepository>()
        every { repository.observeByStatus(any()) } returns flowOf(emptyList())
        coEvery { repository.updateTrackedStatus(any(), any()) } returns Unit

        val viewModel = MyListViewModel(repository)
        val item = sampleTrackedItem(10, WatchStatus.WATCHING)

        viewModel.onItemStatusChange(item, WatchStatus.WATCHING)
        viewModel.onItemStatusChange(item, WatchStatus.COMPLETED)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.updateTrackedStatus(mediaItemId = 10L, status = WatchStatus.COMPLETED) }
    }

    @Test
    fun onItemRemoved_deletesFromRepository() = runTest {
        val repository = mockk<LibraryRepository>()
        every { repository.observeByStatus(any()) } returns flowOf(emptyList())
        coEvery { repository.removeTrackedItem(any()) } returns Unit

        val viewModel = MyListViewModel(repository)
        viewModel.onItemRemoved(sampleTrackedItem(5, WatchStatus.WATCHING))
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.removeTrackedItem(mediaItemId = 5L) }
    }

    private fun sampleTrackedItem(localId: Long, status: WatchStatus): TrackedItem {
        return TrackedItem(
            localId = localId,
            tmdbId = localId.toInt(),
            mediaType = MediaType.TV,
            title = "Title $localId",
            overview = "Overview",
            posterPath = "/poster.jpg",
            releaseOrAirDate = "2024-01-01",
            status = status,
            updatedAt = 1L,
        )
    }
}
