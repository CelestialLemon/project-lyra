package com.projectlyra.app.data.local

import androidx.room.ColumnInfo

data class UserListRow(
    @ColumnInfo(name = "local_id")
    val localId: Long,
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
    val status: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
