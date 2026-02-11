package com.projectlyra.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface GenreMetadataDao {
    @Upsert
    suspend fun upsertAll(entities: List<GenreMetadataEntity>)

    @Query("SELECT * FROM genre_metadata WHERE media_type = :mediaType")
    suspend fun getByMediaType(mediaType: String): List<GenreMetadataEntity>

    @Query("SELECT MAX(updated_at) FROM genre_metadata WHERE media_type = :mediaType")
    suspend fun latestUpdatedAt(mediaType: String): Long?

    @Query("DELETE FROM genre_metadata")
    suspend fun clearAll(): Int
}
