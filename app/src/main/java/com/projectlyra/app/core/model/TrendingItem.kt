package com.projectlyra.app.core.model

data class TrendingItem(
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val overview: String,
    val posterPath: String,
    val releaseOrAirDate: String,
)
