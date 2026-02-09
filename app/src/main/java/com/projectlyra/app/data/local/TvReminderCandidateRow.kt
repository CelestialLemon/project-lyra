package com.projectlyra.app.data.local

import androidx.room.ColumnInfo

data class TvReminderCandidateRow(
    @ColumnInfo(name = "local_id")
    val localId: Long,
    @ColumnInfo(name = "tmdb_id")
    val tmdbId: Int,
    val title: String,
    val status: String,
)
