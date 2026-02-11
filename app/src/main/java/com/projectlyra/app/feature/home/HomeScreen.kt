package com.projectlyra.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
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
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.ui.components.PosterCard

@Composable
fun HomeRoute(
    factory: HomeViewModelFactory,
    onOpenDetails: (mediaType: MediaType, tmdbId: Int) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    HomeScreen(
        uiState = uiState,
        onRetry = viewModel::retry,
        onOpenDetails = onOpenDetails,
    )
}

@Composable
private fun HomeScreen(
    uiState: HomeUiState,
    onRetry: () -> Unit,
    onOpenDetails: (mediaType: MediaType, tmdbId: Int) -> Unit,
) {
    val hasAnyRailItems = uiState.movieRail.items.isNotEmpty() || uiState.tvRail.items.isNotEmpty()
    val hasAnyContent = uiState.resumeHero != null || hasAnyRailItems

    if (uiState.isLoading && !hasAnyContent) {
        FullscreenLoading()
        return
    }

    if (uiState.errorMessage != null && !hasAnyContent) {
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
                    text = "Continue Watching",
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = "Jump back into your latest in-progress title.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (uiState.resumeHero != null) {
            item {
                ResumeHeroCard(
                    item = uiState.resumeHero,
                    onOpenDetails = { onOpenDetails(uiState.resumeHero.mediaType, uiState.resumeHero.tmdbId) },
                    onEditStatus = { onOpenDetails(uiState.resumeHero.mediaType, uiState.resumeHero.tmdbId) },
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item {
            RecommendationRailSection(
                title = "Recommended Movies",
                railState = uiState.movieRail,
                emptyMessage = "No movie recommendations available yet.",
                onRetry = onRetry,
                onOpenDetails = { item -> onOpenDetails(item.mediaType, item.tmdbId) },
            )
        }

        item {
            RecommendationRailSection(
                title = "Recommended TV Shows",
                railState = uiState.tvRail,
                emptyMessage = "No TV recommendations available yet.",
                onRetry = onRetry,
                onOpenDetails = { item -> onOpenDetails(item.mediaType, item.tmdbId) },
            )
        }

        item {
            Box(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ResumeHeroCard(
    item: TrackedItem,
    onOpenDetails: () -> Unit,
    onEditStatus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PosterCard(
            title = item.title,
            posterPath = item.posterPath,
            onClick = onOpenDetails,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedButton(
                onClick = onOpenDetails,
                modifier = Modifier.weight(1f),
            ) {
                Text("Open Details")
            }
            Button(
                onClick = onEditStatus,
                modifier = Modifier.weight(1f),
            ) {
                Text("Edit Status")
            }
        }
    }
}

@Composable
private fun RecommendationRailSection(
    title: String,
    railState: RecommendationRailUiState,
    emptyMessage: String,
    onRetry: () -> Unit,
    onOpenDetails: (TrendingItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        if (railState.isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        if (railState.infoMessage != null) {
            Text(
                text = railState.infoMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (railState.errorMessage != null) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    text = railState.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }

        if (!railState.isLoading && railState.items.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (railState.items.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                items(railState.items) { item ->
                    PosterCard(
                        title = item.title,
                        posterPath = item.posterPath,
                        onClick = { onOpenDetails(item) },
                        modifier = Modifier.fillParentMaxWidth(0.72f),
                    )
                }
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
                text = "Loading Home...",
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
