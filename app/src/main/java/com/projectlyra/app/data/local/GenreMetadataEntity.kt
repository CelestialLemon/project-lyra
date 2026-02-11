package com.projectlyra.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "genre_metadata",
    indices = [Index(value = ["genre_id", "media_type"], unique = true)],
)
data class GenreMetadataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "genre_id")
    val genreId: Int,
    @ColumnInfo(name = "media_type")
    val mediaType: String,
    val name: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
