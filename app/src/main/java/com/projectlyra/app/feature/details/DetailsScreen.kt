package com.projectlyra.app.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.SeasonSummary
import com.projectlyra.app.core.model.TvEpisodeDetails
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.core.yearOrBlank
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val EpisodeDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.US)

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
        onSeasonSelected = viewModel::onSeasonSelected,
        onRetrySeason = viewModel::retrySelectedSeason,
        onMarkEpisodeWatchedUpTo = viewModel::onMarkEpisodeWatchedUpTo,
        onMarkEpisodeUnwatchedFrom = viewModel::onMarkEpisodeUnwatchedFrom,
        onMarkSeasonComplete = viewModel::onMarkSeasonComplete,
    )
}

@Composable
private fun DetailsScreen(
    uiState: DetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onStatusSelected: (WatchStatus?) -> Unit,
    onSeasonSelected: (Int) -> Unit,
    onRetrySeason: () -> Unit,
    onMarkEpisodeWatchedUpTo: (Int) -> Unit,
    onMarkEpisodeUnwatchedFrom: (Int) -> Unit,
    onMarkSeasonComplete: () -> Unit,
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
                TvSeasonsSectionHeader()
            }

            item {
                SeasonSelector(
                    seasons = uiState.seasons,
                    selectedSeasonNumber = uiState.selectedSeasonNumber,
                    onSeasonSelected = onSeasonSelected,
                )
            }

            item {
                FilledTonalButton(
                    onClick = onMarkSeasonComplete,
                    enabled = uiState.selectedSeasonNumber != null &&
                        uiState.selectedSeasonEpisodes.isNotEmpty() &&
                        !uiState.isSeasonLoading,
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth(),
                ) {
                    Text(text = "Mark season as complete")
                }
            }

            if (uiState.isSeasonLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (uiState.seasonErrorMessage != null) {
                item {
                    SeasonErrorState(
                        message = uiState.seasonErrorMessage,
                        onRetry = onRetrySeason,
                    )
                }
            }

            if (!uiState.isSeasonLoading && uiState.seasonErrorMessage == null) {
                if (uiState.selectedSeasonEpisodes.isEmpty()) {
                    item {
                        Text(
                            text = "No episodes available for the selected season.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                } else {
                    items(uiState.selectedSeasonEpisodes, key = { episode -> episode.episodeId }) { episode ->
                        EpisodeCard(
                            episode = episode,
                            isWatched = episode.episodeNumber in uiState.watchedEpisodeNumbers,
                            onMarkWatchedUpTo = { onMarkEpisodeWatchedUpTo(episode.episodeNumber) },
                            onMarkUnwatchedFrom = { onMarkEpisodeUnwatchedFrom(episode.episodeNumber) },
                        )
                    }
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
            .aspectRatio(16f / 9f),
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
                            MaterialTheme.colorScheme.background.copy(alpha = 0.2f),
                            MaterialTheme.colorScheme.background.copy(alpha = 0.6f),
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

@OptIn(ExperimentalMaterial3Api::class)
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
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedTextField(
                value = selectedStatus?.label ?: "Not tracked",
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                label = { Text("Tracking status") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
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
                    .width(124.dp)
                    .height(186.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (details.genres.isNotEmpty()) {
                    Text(
                        text = details.genres.joinToString(" | "),
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
                if (details.mediaType == MediaType.TV) {
                    val seasonsCount = details.numberOfSeasons ?: details.seasons.size
                    val episodeCount = details.numberOfEpisodes ?: details.seasons.sumOf { it.episodeCount }
                    MetadataLabel(
                        label = "Totals",
                        value = "$seasonsCount seasons | $episodeCount episodes",
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
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun TvSeasonsSectionHeader() {
    Text(
        text = "Seasons",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonSelector(
    seasons: List<SeasonSummary>,
    selectedSeasonNumber: Int?,
    onSeasonSelected: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSeason = seasons.firstOrNull { it.seasonNumber == selectedSeasonNumber }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            if (seasons.isNotEmpty()) {
                expanded = !expanded
            }
        },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selectedSeason?.name ?: "Select season",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text("Season") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            seasons.forEach { season ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = season.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        expanded = false
                        onSeasonSelected(season.seasonNumber)
                    },
                )
            }
        }
    }
}

@Composable
private fun SeasonErrorState(
    message: String,
    onRetry: () -> Unit,
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
            FilledTonalButton(onClick = onRetry) {
                Text("Retry season load")
            }
        }
    }
}

@Composable
private fun EpisodeCard(
    episode: TvEpisodeDetails,
    isWatched: Boolean,
    onMarkWatchedUpTo: () -> Unit,
    onMarkUnwatchedFrom: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val runtimeText = episode.runtimeMinutes?.let { minutes -> "$minutes min" } ?: "Runtime unavailable"
    val descriptionText = episode.overview?.trim().orEmpty().ifBlank { "Description unavailable." }

    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                EpisodeStill(stillPath = episode.stillPath, title = episode.title)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = episode.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "Episode ${episode.episodeNumber}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = formatEpisodeAirDate(episode.airDate),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "|",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = runtimeText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    EpisodeProgressIndicator(isWatched = isWatched)
                }

                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = "Episode actions",
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        if (isWatched) {
                            DropdownMenuItem(
                                text = { Text("Mark unwatched from this episode") },
                                onClick = {
                                    menuExpanded = false
                                    onMarkUnwatchedFrom()
                                },
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Mark watched up to this episode") },
                                onClick = {
                                    menuExpanded = false
                                    onMarkWatchedUpTo()
                                },
                            )
                        }
                    }
                }
            }

            Text(
                text = descriptionText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun EpisodeStill(
    stillPath: String?,
    title: String,
) {
    if (!stillPath.isNullOrBlank()) {
        AsyncImage(
            model = "https://image.tmdb.org/t/p/w300$stillPath",
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(152.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Box(
            modifier = Modifier
                .width(152.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No image",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EpisodeProgressIndicator(isWatched: Boolean) {
    Icon(
        imageVector = if (isWatched) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
        contentDescription = if (isWatched) "Episode watched" else "Episode unwatched",
        tint = if (isWatched) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        modifier = Modifier.size(18.dp),
    )
}

private fun formatEpisodeAirDate(rawAirDate: String?): String {
    val normalized = rawAirDate?.trim().orEmpty()
    if (normalized.isBlank()) {
        return "Release date unavailable"
    }

    return try {
        LocalDate.parse(normalized).format(EpisodeDateFormatter)
    } catch (_: DateTimeParseException) {
        normalized
    }
}
