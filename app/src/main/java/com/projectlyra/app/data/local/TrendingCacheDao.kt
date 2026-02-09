package com.projectlyra.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface TrendingCacheDao {
    @Query(
        """
        SELECT
            m.tmdb_id AS tmdb_id,
            m.media_type AS media_type,
            m.title,
            m.overview,
            m.poster_path AS poster_path,
            m.release_or_air_date AS release_or_air_date
        FROM trending_cache c
        INNER JOIN media_items m ON m.id = c.media_item_id
        ORDER BY c.position ASC
        """
    )
    suspend fun getCachedTrending(): List<CachedTrendingRow>

    @Query("SELECT MAX(cached_at) FROM trending_cache")
    suspend fun latestCachedAt(): Long?

    @Query("DELETE FROM trending_cache")
    suspend fun clearAll()

    @Upsert
    suspend fun upsertAll(entries: List<TrendingCacheEntity>)
}
