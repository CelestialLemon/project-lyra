package com.projectlyra.app.data.local

import androidx.room.ColumnInfo

data class CompletedMediaGenreRow(
    @ColumnInfo(name = "genre_ids")
    val genreIdsCsv: String,
)
