package com.projectlyra.app.core.model

data class TvSeasonDetails(
    val tvId: Int,
    val seasonNumber: Int,
    val seasonName: String,
    val episodes: List<TvEpisodeDetails>,
)

data class TvEpisodeDetails(
    val episodeId: Int,
    val episodeNumber: Int,
    val title: String,
    val stillPath: String?,
    val airDate: String?,
    val runtimeMinutes: Int?,
    val overview: String? = null,
)
