package com.projectlyra.app.core.model

data class MediaDetails(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val overview: String,
    val posterPath: String,
    val backdropPath: String?,
    val releaseOrAirDate: String,
    val genres: List<String> = emptyList(),
    val genreIds: List<Int> = emptyList(),
    val runtimeMinutes: Int? = null,
    val numberOfSeasons: Int? = null,
    val numberOfEpisodes: Int? = null,
    val seasons: List<SeasonSummary> = emptyList(),
)

data class SeasonSummary(
    val seasonNumber: Int,
    val name: String,
    val episodeCount: Int,
    val airDate: String?,
    val posterPath: String?,
)
