package com.projectlyra.app.data.local

import androidx.room.ColumnInfo

data class CachedTrendingRow(
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int,
    @ColumnInfo(name = "media_type")
    val mediaType: String,
    val title: String,
    val overview: String,
    @ColumnInfo(name = "poster_path")
    val posterPath: String,
    @ColumnInfo(name = "release_or_air_date")
    val releaseOrAirDate: String,
)
