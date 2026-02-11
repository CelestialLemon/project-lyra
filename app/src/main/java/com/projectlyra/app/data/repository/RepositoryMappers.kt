package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.SeasonSummary
import com.projectlyra.app.core.model.TvEpisodeDetails
import com.projectlyra.app.core.model.TvSeasonDetails
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.data.local.CachedTrendingRow
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.remote.TmdbDiscoverMovieDto
import com.projectlyra.app.data.remote.TmdbDiscoverTvDto
import com.projectlyra.app.data.remote.TmdbMovieDetailsDto
import com.projectlyra.app.data.remote.TmdbMultiSearchItemDto
import com.projectlyra.app.data.remote.TmdbTrendingItemDto
import com.projectlyra.app.data.remote.TmdbTvDetailsDto
import com.projectlyra.app.data.remote.TmdbTvSeasonDetailsDto

internal fun CachedTrendingRow.toDomainOrNull(): TrendingItem? {
    val mappedType = runCatching { MediaType.valueOf(mediaType) }.getOrNull() ?: return null
    return TrendingItem(
        tmdbId = tmdbId,
        mediaType = mappedType,
        title = title,
        overview = overview,
        posterPath = posterPath,
        releaseOrAirDate = releaseOrAirDate,
        genreIds = emptyList(),
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
        genreIds = genreIds.orEmpty().filter { it > 0 }.distinct(),
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
        genreIds = genreIds.orEmpty().filter { it > 0 }.distinct(),
    )
}

internal fun TmdbDiscoverMovieDto.toDomainOrNull(): TrendingItem? {
    val mappedTitle = title.orEmpty().trim()
    val mappedPosterPath = posterPath.orEmpty().trim()
    if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
        return null
    }

    return TrendingItem(
        tmdbId = id,
        mediaType = MediaType.MOVIE,
        title = mappedTitle,
        overview = overview.orEmpty(),
        posterPath = mappedPosterPath,
        releaseOrAirDate = releaseDate.orEmpty(),
        genreIds = genreIds.orEmpty().filter { it > 0 }.distinct(),
    )
}

internal fun TmdbDiscoverTvDto.toDomainOrNull(): TrendingItem? {
    val mappedTitle = name.orEmpty().trim()
    val mappedPosterPath = posterPath.orEmpty().trim()
    if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
        return null
    }

    return TrendingItem(
        tmdbId = id,
        mediaType = MediaType.TV,
        title = mappedTitle,
        overview = overview.orEmpty(),
        posterPath = mappedPosterPath,
        releaseOrAirDate = firstAirDate.orEmpty(),
        genreIds = genreIds.orEmpty().filter { it > 0 }.distinct(),
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
        genreIds = genres.orEmpty().map { it.id }.filter { it > 0 }.distinct(),
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
        genreIds = genres.orEmpty().map { it.id }.filter { it > 0 }.distinct(),
        numberOfSeasons = numberOfSeasons?.takeIf { it >= 0 } ?: mappedSeasons.size,
        numberOfEpisodes = numberOfEpisodes?.takeIf { it >= 0 },
        seasons = mappedSeasons,
    )
}

internal fun TmdbTvSeasonDetailsDto.toDomainOrNull(tvId: Int, fallbackSeasonNumber: Int): TvSeasonDetails? {
    val mappedSeasonNumber = seasonNumber ?: fallbackSeasonNumber
    if (mappedSeasonNumber < 0) {
        return null
    }

    val mappedEpisodes = episodes.orEmpty()
        .mapNotNull { episode ->
            val episodeId = episode.id ?: return@mapNotNull null
            val episodeNumber = episode.episodeNumber ?: return@mapNotNull null
            if (episodeNumber <= 0) {
                return@mapNotNull null
            }
            TvEpisodeDetails(
                episodeId = episodeId,
                episodeNumber = episodeNumber,
                title = episode.name.orEmpty().ifBlank { "Episode $episodeNumber" },
                stillPath = episode.stillPath?.trim()?.takeIf { it.isNotEmpty() },
                airDate = episode.airDate?.trim()?.takeIf { it.isNotEmpty() },
                runtimeMinutes = episode.runtime?.takeIf { it > 0 },
                overview = episode.overview?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
        .sortedBy { it.episodeNumber }

    return TvSeasonDetails(
        tvId = tvId,
        seasonNumber = mappedSeasonNumber,
        seasonName = name.orEmpty().ifBlank { "Season $mappedSeasonNumber" },
        episodes = mappedEpisodes,
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
        genreIds = parseGenreIdsCsv(genreIdsCsv),
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
        genreIds = genreIds,
    )
}

internal fun List<Int>.toGenreIdsCsv(): String {
    return this.filter { it > 0 }.distinct().joinToString(",")
}

internal fun parseGenreIdsCsv(raw: String): List<Int> {
    if (raw.isBlank()) {
        return emptyList()
    }
    return raw.split(",")
        .mapNotNull { token -> token.trim().toIntOrNull() }
        .filter { id -> id > 0 }
        .distinct()
}
