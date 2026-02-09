package com.projectlyra.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.ui.components.PosterCard

@Composable
fun SearchRoute(
    factory: SearchViewModelFactory,
    onOpenDetails: (TrendingItem) -> Unit,
    viewModel: SearchViewModel = viewModel(factory = factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    SearchScreen(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onRetry = viewModel::retry,
        onOpenDetails = onOpenDetails,
    )
}

@Composable
private fun SearchScreen(
    uiState: SearchUiState,
    onQueryChange: (String) -> Unit,
    onRetry: () -> Unit,
    onOpenDetails: (TrendingItem) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Search Catalog", style = MaterialTheme.typography.displaySmall)
                Text(
                    text = "Find movies and TV shows, then set a status in one tap.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Movie or TV title") },
                placeholder = { Text("Search TMDB") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                    )
                },
            )
        }

        if (uiState.isLoading) {
            item {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        if (uiState.query.isBlank()) {
            item {
                Text(
                    text = "Start typing to search movies and TV shows.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else if (!uiState.isLoading && uiState.errorMessage == null && uiState.results.isEmpty() && uiState.hasSearched) {
            item {
                Text(
                    text = "No results found for \"${uiState.query.trim()}\".",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(
                items = uiState.results,
                key = { item -> "${item.mediaType.name}:${item.tmdbId}" },
            ) { item ->
                SearchResultCard(
                    item = item,
                    onOpenDetails = { onOpenDetails(item) },
                )
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: TrendingItem,
    onOpenDetails: () -> Unit,
) {
    PosterCard(
        title = item.title,
        posterPath = item.posterPath,
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpenDetails,
    )
}
