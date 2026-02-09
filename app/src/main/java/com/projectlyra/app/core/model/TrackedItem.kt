package com.projectlyra.app.core.model

data class TrackedItem(
    val localId: Long,
    val tmdbId: Int,
    val mediaType: MediaType,
    val title: String,
    val overview: String,
    val posterPath: String,
    val releaseOrAirDate: String,
    val status: WatchStatus,
    val updatedAt: Long,
)
