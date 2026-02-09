package com.projectlyra.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EpisodeReminderStateDao {
    @Query("SELECT * FROM episode_reminder_state WHERE media_item_id = :mediaItemId LIMIT 1")
    suspend fun findByMediaItemId(mediaItemId: Long): EpisodeReminderStateEntity?

    @Query("SELECT * FROM episode_reminder_state")
    suspend fun getAll(): List<EpisodeReminderStateEntity>

    @Query("DELETE FROM episode_reminder_state")
    suspend fun clearAll(): Int

    @Upsert
    suspend fun upsert(state: EpisodeReminderStateEntity): Long
}
