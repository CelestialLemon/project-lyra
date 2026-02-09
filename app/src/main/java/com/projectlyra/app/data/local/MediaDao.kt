package com.projectlyra.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface MediaDao {
    @Upsert
    suspend fun upsertMediaItem(mediaItem: MediaItemEntity): Long

    @Query("SELECT * FROM media_items WHERE tmdb_id = :tmdbId AND media_type = :mediaType LIMIT 1")
    suspend fun findByTmdbAndType(tmdbId: Int, mediaType: String): MediaItemEntity?

    @Query("SELECT * FROM media_items")
    suspend fun getAllMediaItems(): List<MediaItemEntity>

    @Query("DELETE FROM media_items")
    suspend fun clearAll(): Int

    @Query("SELECT COUNT(id) FROM media_items")
    suspend fun mediaCount(): Int
}
