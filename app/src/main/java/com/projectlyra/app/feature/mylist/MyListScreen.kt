package com.projectlyra.app.feature.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.ui.components.PosterCard
import com.projectlyra.app.ui.components.StatusChip

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
        onItemStatusChange = viewModel::onItemStatusChange,
        onItemRemove = viewModel::onItemRemoved,
        onOpenDetails = onOpenDetails,
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun MyListScreen(
    selectedStatus: WatchStatus,
    items: List<TrackedItem>,
    onStatusChange: (WatchStatus) -> Unit,
    onItemStatusChange: (TrackedItem, WatchStatus) -> Unit,
    onItemRemove: (TrackedItem) -> Unit,
    onOpenDetails: (TrackedItem) -> Unit,
) {
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
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                WatchStatus.entries.forEach { status ->
                    StatusChip(
                        text = status.label,
                        selected = selectedStatus == status,
                        onClick = { onStatusChange(status) },
                    )
                }
            }
        }

        if (items.isEmpty()) {
            item {
                Text(
                    text = "No titles in ${selectedStatus.label} yet.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
        } else {
            items(items, key = { it.localId }) { item ->
                TrackedItemCard(
                    item = item,
                    onStatusChange = { newStatus -> onItemStatusChange(item, newStatus) },
                    onRemove = { onItemRemove(item) },
                    onOpenDetails = { onOpenDetails(item) },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun TrackedItemCard(
    item: TrackedItem,
    onStatusChange: (WatchStatus) -> Unit,
    onRemove: () -> Unit,
    onOpenDetails: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        PosterCard(
            title = item.title,
            subtitle = "${item.mediaType.name.lowercase()} · ${item.releaseOrAirDate}",
            posterPath = item.posterPath,
            modifier = Modifier.fillMaxWidth(),
            onClick = onOpenDetails,
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WatchStatus.entries.forEach { status ->
                StatusChip(
                    text = status.label,
                    selected = item.status == status,
                    onClick = {
                        if (item.status != status) {
                            onStatusChange(status)
                        }
                    },
                )
            }
        }

        OutlinedButton(onClick = onRemove) {
            Text(text = "Remove from List")
        }
    }
}
