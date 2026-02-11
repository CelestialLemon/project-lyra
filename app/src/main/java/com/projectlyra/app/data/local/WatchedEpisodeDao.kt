package com.projectlyra.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchedEpisodeDao {
    @Query(
        """
        SELECT episode_number
        FROM watched_episodes
        WHERE media_item_id = :mediaItemId
          AND season_number = :seasonNumber
        ORDER BY episode_number ASC
        """
    )
    fun observeEpisodeNumbersBySeason(mediaItemId: Long, seasonNumber: Int): Flow<List<Int>>

    @Query(
        """
        SELECT episode_number
        FROM watched_episodes
        WHERE media_item_id = :mediaItemId
          AND season_number = :seasonNumber
        ORDER BY episode_number ASC
        """
    )
    suspend fun getEpisodeNumbersBySeason(mediaItemId: Long, seasonNumber: Int): List<Int>

    @Query("SELECT * FROM watched_episodes ORDER BY media_item_id, season_number, episode_number")
    suspend fun getAll(): List<WatchedEpisodeEntity>

    @Upsert
    suspend fun upsertAll(entries: List<WatchedEpisodeEntity>)

    @Query(
        """
        DELETE FROM watched_episodes
        WHERE media_item_id = :mediaItemId
          AND season_number = :seasonNumber
          AND episode_number >= :episodeNumber
        """
    )
    suspend fun deleteFromEpisode(mediaItemId: Long, seasonNumber: Int, episodeNumber: Int): Int

    @Query(
        """
        DELETE FROM watched_episodes
        WHERE media_item_id = :mediaItemId
          AND season_number = :seasonNumber
        """
    )
    suspend fun deleteBySeason(mediaItemId: Long, seasonNumber: Int): Int

    @Query("DELETE FROM watched_episodes")
    suspend fun clearAll(): Int
}
