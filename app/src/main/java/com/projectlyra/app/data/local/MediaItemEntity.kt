package com.projectlyra.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [Index(value = ["tmdb_id", "media_type"], unique = true)],
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
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
    @ColumnInfo(name = "genre_ids")
    val genreIdsCsv: String = "",
    @ColumnInfo(name = "metadata_updated_at")
    val metadataUpdatedAt: Long,
)
