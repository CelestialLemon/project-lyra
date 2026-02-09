package com.projectlyra.app.feature.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.ui.components.PosterCard

@Composable
fun MyListRoute(
    factory: MyListViewModelFactory,
    onOpenDetails: (TrackedItem) -> Unit,
    viewModel: MyListViewModel = viewModel(factory = factory),
) {
    val selectedStatus by viewModel.status.collectAsState()
    val items by viewModel.items.collectAsState()

    MyListScreen(
        selectedStatus = selectedStatus,
        items = items,
        onStatusChange = viewModel::onStatusSelected,
        onOpenDetails = onOpenDetails,
    )
}

@Composable
private fun MyListScreen(
    selectedStatus: WatchStatus,
    items: List<TrackedItem>,
    onStatusChange: (WatchStatus) -> Unit,
    onOpenDetails: (TrackedItem) -> Unit,
) {
    val statuses = WatchStatus.entries
    val selectedTabIndex = statuses.indexOf(selectedStatus).coerceAtLeast(0)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("My Lists", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Track every title by status and pick up where you left off.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
            ) {
                statuses.forEach { status ->
                    Tab(
                        selected = selectedStatus == status,
                        onClick = { onStatusChange(status) },
                        text = {
                            Text(
                                text = status.label,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                    )
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Text(
                    text = emptyStateMessage(selectedStatus),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            items(items, key = { it.localId }) { item ->
                TrackedItemCard(
                    item = item,
                    onOpenDetails = { onOpenDetails(item) },
                )
            }
        }
    }
}

@Composable
private fun TrackedItemCard(
    item: TrackedItem,
    onOpenDetails: () -> Unit,
) {
    PosterCard(
        title = item.title,
        posterPath = item.posterPath,
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenDetails,
    )
}

private fun emptyStateMessage(status: WatchStatus): String = when (status) {
    WatchStatus.WANT_TO_WATCH -> "No titles queued yet. Add a title to your watchlist."
    WatchStatus.WATCHING -> "Nothing currently in progress. Start a title to track it here."
    WatchStatus.ON_HOLD -> "No paused titles right now."
    WatchStatus.DROPPED -> "No dropped titles yet."
    WatchStatus.COMPLETED -> "No completed titles yet. Finished titles will show up here."
}
