package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.SeasonSummary
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.data.local.CachedTrendingRow
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.remote.TmdbMovieDetailsDto
import com.projectlyra.app.data.remote.TmdbMultiSearchItemDto
import com.projectlyra.app.data.remote.TmdbTrendingItemDto
import com.projectlyra.app.data.remote.TmdbTvDetailsDto

internal fun CachedTrendingRow.toDomainOrNull(): TrendingItem? {
    val mappedType = runCatching { MediaType.valueOf(mediaType) }.getOrNull() ?: return null
    return TrendingItem(
        tmdbId = tmdbId,
        mediaType = mappedType,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseOrAirDate = releaseOrAirDate,
    )
}

internal fun TmdbTrendingItemDto.toDomainOrNull(): TrendingItem? {
    val mappedMediaType = when (mediaType?.lowercase()) {
        "movie" -> MediaType.MOVIE
        "tv" -> MediaType.TV
        else -> return null
    }

    val mappedTitle = when (mappedMediaType) {
        MediaType.MOVIE -> title
        MediaType.TV -> name
    }.orEmpty().trim()

    val mappedPosterPath = posterPath.orEmpty().trim()

    if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
        return null
    }

    val mappedDate = when (mappedMediaType) {
        MediaType.MOVIE -> releaseDate
        MediaType.TV -> firstAirDate
    }.orEmpty()

    return TrendingItem(
        tmdbId = id,
        mediaType = mappedMediaType,
        title = mappedTitle,
        overview = overview.orEmpty(),
        posterPath = mappedPosterPath,
        releaseOrAirDate = mappedDate,
    )
}

internal fun TmdbMultiSearchItemDto.toDomainOrNull(): TrendingItem? {
    val mappedMediaType = when (mediaType?.lowercase()) {
        "movie" -> MediaType.MOVIE
        "tv" -> MediaType.TV
        else -> return null
    }

    val mappedTitle = when (mappedMediaType) {
        MediaType.MOVIE -> title
        MediaType.TV -> name
    }.orEmpty().trim()
    val mappedPosterPath = posterPath.orEmpty().trim()

    if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
        return null
    }

    val mappedDate = when (mappedMediaType) {
        MediaType.MOVIE -> releaseDate
        MediaType.TV -> firstAirDate
    }.orEmpty()

    return TrendingItem(
        tmdbId = id,
        mediaType = mappedMediaType,
        title = mappedTitle,
        overview = overview.orEmpty(),
        posterPath = mappedPosterPath,
        releaseOrAirDate = mappedDate,
    )
}

internal fun TmdbMovieDetailsDto.toDomainOrNull(): MediaDetails? {
    val mappedTitle = title.orEmpty().trim()
    val mappedPosterPath = posterPath.orEmpty().trim()
    if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
        return null
    }

    return MediaDetails(
        tmdbId = id,
        mediaType = MediaType.MOVIE,
        title = mappedTitle,
        overview = overview.orEmpty(),
        posterPath = mappedPosterPath,
        backdropPath = backdropPath?.trim()?.takeIf { it.isNotEmpty() },
        releaseOrAirDate = releaseDate.orEmpty(),
        genres = genres.orEmpty().mapNotNull { dto -> dto.name?.trim()?.takeIf { it.isNotEmpty() } },
        runtimeMinutes = runtime?.takeIf { it > 0 },
    )
}

internal fun TmdbTvDetailsDto.toDomainOrNull(): MediaDetails? {
    val mappedTitle = name.orEmpty().trim()
    val mappedPosterPath = posterPath.orEmpty().trim()
    if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
        return null
    }

    val mappedSeasons = seasons.orEmpty()
        .mapNotNull { season ->
            val seasonNumber = season.seasonNumber ?: return@mapNotNull null
            val episodeCount = season.episodeCount ?: 0
            SeasonSummary(
                seasonNumber = seasonNumber,
                name = season.name.orEmpty().ifBlank { "Season $seasonNumber" },
                episodeCount = episodeCount,
                airDate = season.airDate?.trim()?.takeIf { it.isNotEmpty() },
                posterPath = season.posterPath?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
        .sortedBy { it.seasonNumber }

    return MediaDetails(
        tmdbId = id,
        mediaType = MediaType.TV,
        title = mappedTitle,
        overview = overview.orEmpty(),
        posterPath = mappedPosterPath,
        backdropPath = backdropPath?.trim()?.takeIf { it.isNotEmpty() },
        releaseOrAirDate = firstAirDate.orEmpty(),
        genres = genres.orEmpty().mapNotNull { dto -> dto.name?.trim()?.takeIf { it.isNotEmpty() } },
        numberOfSeasons = numberOfSeasons?.takeIf { it >= 0 } ?: mappedSeasons.size,
        numberOfEpisodes = numberOfEpisodes?.takeIf { it >= 0 },
        seasons = mappedSeasons,
    )
}

internal fun MediaItemEntity.toDetailsFallback(mediaType: MediaType): MediaDetails {
    return MediaDetails(
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        overview = overview,
        posterPath = posterPath,
        backdropPath = null,
        releaseOrAirDate = releaseOrAirDate,
    )
}

internal fun MediaDetails.asTrendingItem(): TrendingItem {
    return TrendingItem(
        tmdbId = tmdbId,
        mediaType = mediaType,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseOrAirDate = releaseOrAirDate,
    )
}
