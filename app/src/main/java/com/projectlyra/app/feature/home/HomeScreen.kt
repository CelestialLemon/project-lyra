package com.projectlyra.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.ui.components.PosterCard
import com.projectlyra.app.ui.components.StatusChip

@Composable
fun HomeRoute(
    factory: HomeViewModelFactory,
    viewModel: HomeViewModel = viewModel(factory = factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onStatusSelected = viewModel::onStatusSelected,
        trackedStatusFor = viewModel::trackedStatusFor,
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onStatusSelected: (TrendingItem, WatchStatus) -> Unit,
    trackedStatusFor: (TrendingItem) -> WatchStatus?,
) {
    if (uiState.isLoading && uiState.trending.isEmpty()) {
        FullscreenLoading()
        return
    }

    if (uiState.errorMessage != null && uiState.trending.isEmpty()) {
        FullscreenError(
            message = uiState.errorMessage,
            onRetry = onRetry,
        )
        return
    }

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
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp),
            ) {
                Text(
                    text = "Tonight's Trending",
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = "Live TMDB discovery with cached fallback.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = uiState.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
            }
        } else if (uiState.isShowingCachedData) {
            item {
                Text(
                    text = "Showing cached trending results.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        if (uiState.trending.isEmpty()) {
            item {
                Text(
                    text = "No trending titles available.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        } else {
            item {
                val hero = uiState.trending.firstOrNull()
                if (hero != null) {
                    TrendingCardWithActions(
                        item = hero,
                        trackedStatus = trackedStatusFor(hero),
                        onStatusSelected = { status -> onStatusSelected(hero, status) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth(),
                    )
                }
            }

            if (uiState.trending.size > 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Hot Right Now",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            items(uiState.trending.drop(1)) { item ->
                                TrendingCardWithActions(
                                    item = item,
                                    trackedStatus = trackedStatusFor(item),
                                    onStatusSelected = { status -> onStatusSelected(item, status) },
                                    modifier = Modifier.fillParentMaxWidth(0.78f),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Box(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TrendingCardWithActions(
    item: TrendingItem,
    trackedStatus: WatchStatus?,
    onStatusSelected: (WatchStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PosterCard(
            title = item.title,
            subtitle = item.releaseOrAirDate,
            posterPath = item.posterPath,
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WatchStatus.entries.forEach { status ->
                StatusChip(
                    text = status.label,
                    selected = trackedStatus == status,
                    onClick = {
                        if (trackedStatus != status) {
                            onStatusSelected(status)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun FullscreenLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading trending titles...",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun FullscreenError(
    message: String,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
