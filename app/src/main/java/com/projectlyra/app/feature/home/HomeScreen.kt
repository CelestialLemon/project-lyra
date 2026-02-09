package com.projectlyra.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
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
fun HomeRoute(
    factory: HomeViewModelFactory,
    viewModel: HomeViewModel = viewModel(factory = factory),
) {
    val trending by viewModel.trending.collectAsState()

    HomeScreen(trending = trending)
}

@Composable
private fun HomeScreen(trending: List<TrendingItem>) {
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
                    text = "Poster-first discovery with quick status updates.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            val hero = trending.firstOrNull()
            if (hero != null) {
                PosterCard(
                    title = hero.title,
                    subtitle = hero.releaseOrAirDate,
                    posterPath = hero.posterPath,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                )
            }
        }

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
                    items(trending.drop(1)) { item ->
                        Box(modifier = Modifier.fillParentMaxWidth(0.78f)) {
                            PosterCard(
                                title = item.title,
                                subtitle = item.releaseOrAirDate,
                                posterPath = item.posterPath,
                                modifier = Modifier.fillMaxWidth(),
                            )
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
