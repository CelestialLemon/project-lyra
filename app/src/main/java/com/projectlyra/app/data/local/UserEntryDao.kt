package com.projectlyra.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UserEntryDao {
    @Upsert
    suspend fun upsertUserEntry(userEntryEntity: UserEntryEntity): Long

    @Query("SELECT * FROM user_entries WHERE media_item_id = :mediaItemId LIMIT 1")
    suspend fun findByMediaItemId(mediaItemId: Long): UserEntryEntity?

    @Query("SELECT * FROM user_entries")
    suspend fun getAllUserEntries(): List<UserEntryEntity>

    @Query("DELETE FROM user_entries")
    suspend fun clearAll(): Int

    @Query("DELETE FROM user_entries WHERE media_item_id = :mediaItemId")
    suspend fun deleteByMediaItemId(mediaItemId: Long): Int

    @Query(
        """
        SELECT
            m.id AS local_id,
            m.tmdb_id AS tmdb_id,
            m.media_type AS media_type,
            m.title,
            m.overview,
            m.poster_path AS poster_path,
            m.release_or_air_date AS release_or_air_date,
            u.status,
            u.updated_at AS updated_at
        FROM user_entries u
        INNER JOIN media_items m ON m.id = u.media_item_id
        WHERE u.status = :status
        ORDER BY u.updated_at DESC
        """
    )
    fun observeItemsByStatus(status: String): Flow<List<UserListRow>>

    @Query(
        """
        SELECT
            m.tmdb_id AS tmdb_id,
            m.media_type AS media_type,
            u.status
        FROM user_entries u
        INNER JOIN media_items m ON m.id = u.media_item_id
        """
    )
    fun observeTrackedStatuses(): Flow<List<UserStatusRow>>

    @Query(
        """
        SELECT
            m.id AS local_id,
            m.tmdb_id AS tmdb_id,
            m.title,
            u.status
        FROM user_entries u
        INNER JOIN media_items m ON m.id = u.media_item_id
        WHERE m.media_type = 'TV' AND u.status IN (:statuses)
        """
    )
    suspend fun getTvReminderCandidates(statuses: List<String>): List<TvReminderCandidateRow>
}
