package com.projectlyra.app.data.local

import androidx.room.ColumnInfo

data class UserStatusRow(
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int,
    @ColumnInfo(name = "media_type")
    val mediaType: String,
    val status: String,
)
