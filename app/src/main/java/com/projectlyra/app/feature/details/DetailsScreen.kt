package com.projectlyra.app.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.projectlyra.app.core.yearOrBlank
import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.WatchStatus

@Composable
fun DetailsRoute(
    factory: DetailsViewModelFactory,
    onBack: () -> Unit,
    viewModel: DetailsViewModel = viewModel(factory = factory),
) {
    val uiState by viewModel.uiState.collectAsState()

    DetailsScreen(
        uiState = uiState,
        onBack = onBack,
        onRetry = viewModel::refresh,
        onStatusSelected = viewModel::onStatusSelected,
    )
}

@Composable
private fun DetailsScreen(
    uiState: DetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStatusSelected: (WatchStatus?) -> Unit,
) {
    val details = uiState.details
    if (uiState.isLoading && details == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (details == null) {
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
                    text = uiState.errorMessage ?: "Unable to open details for this title.",
                    color = MaterialTheme.colorScheme.error,
                )
                Button(onClick = onRetry) { Text("Retry") }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            DetailsHeader(
                details = details,
                onBack = onBack,
            )
        }

        item {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (uiState.infoMessage != null) {
                    Text(
                        text = uiState.infoMessage,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (uiState.errorMessage != null) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                StatusSection(
                    selectedStatus = uiState.trackedStatus,
                    onStatusSelected = onStatusSelected,
                )
            }
        }

        item {
            MetadataSection(details = details)
        }

        if (details.mediaType == MediaType.TV) {
            item {
                SeasonsOverview(details = details)
            }
            if (details.seasons.isNotEmpty()) {
                items(details.seasons, key = { it.seasonNumber }) { season ->
                    SeasonCard(
                        name = season.name,
                        seasonNumber = season.seasonNumber,
                        episodeCount = season.episodeCount,
                        airDate = season.airDate,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsHeader(
    details: MediaDetails,
    onBack: () -> Unit,
) {
    val backdropPath = details.backdropPath ?: details.posterPath
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(330.dp),
    ) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w780$backdropPath",
            contentDescription = details.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.background,
                        )
                    )
                ),
        )

        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(top = 12.dp, start = 8.dp)
                .align(Alignment.TopStart),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = details.title,
                style = MaterialTheme.typography.displaySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(details.mediaType.name.lowercase())
                    val releaseYear = yearOrBlank(details.releaseOrAirDate)
                    if (releaseYear.isNotBlank()) {
                        append("  |  ")
                        append(releaseYear)
                    }
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatusSection(
    selectedStatus: WatchStatus?,
    onStatusSelected: (WatchStatus?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Status",
            style = MaterialTheme.typography.titleLarge,
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = selectedStatus?.label ?: "Not tracked")
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Not tracked") },
                    onClick = {
                        expanded = false
                        if (selectedStatus != null) {
                            onStatusSelected(null)
                        }
                    },
                )
                WatchStatus.entries.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status.label) },
                        onClick = {
                            expanded = false
                            if (selectedStatus != status) {
                                onStatusSelected(status)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataSection(details: MediaDetails) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AsyncImage(
                model = "https://image.tmdb.org/t/p/w342${details.posterPath}",
                contentDescription = details.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(128.dp)
                    .height(190.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (details.genres.isNotEmpty()) {
                    Text(
                        text = details.genres.joinToString(" • "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (details.runtimeMinutes != null) {
                    MetadataLabel(
                        label = "Runtime",
                        value = "${details.runtimeMinutes} min",
                    )
                }
                if (details.numberOfSeasons != null) {
                    MetadataLabel(
                        label = "Seasons",
                        value = details.numberOfSeasons.toString(),
                    )
                }
                if (details.numberOfEpisodes != null) {
                    MetadataLabel(
                        label = "Episodes",
                        value = details.numberOfEpisodes.toString(),
                    )
                }
            }
        }

        Text(
            text = "Overview",
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = details.overview.ifBlank { "Overview not available yet." },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetadataLabel(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SeasonsOverview(details: MediaDetails) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Seasons & Episodes",
            style = MaterialTheme.typography.titleLarge,
        )
        val seasonsCount = details.numberOfSeasons ?: details.seasons.size
        val episodeCount = details.numberOfEpisodes ?: details.seasons.sumOf { it.episodeCount }
        Text(
            text = "$seasonsCount seasons • $episodeCount episodes",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (details.seasons.isEmpty()) {
            Text(
                text = "Season-level details are not available for this show yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SeasonCard(
    name: String,
    seasonNumber: Int,
    episodeCount: Int,
    airDate: String?,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "Season $seasonNumber • $episodeCount episodes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
            if (!airDate.isNullOrBlank()) {
                val airYear = yearOrBlank(airDate)
                if (airYear.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "First aired: $airYear",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
