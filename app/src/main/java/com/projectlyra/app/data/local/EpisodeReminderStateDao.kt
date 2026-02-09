package com.projectlyra.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EpisodeReminderStateDao {
    @Query("SELECT * FROM episode_reminder_state WHERE media_item_id = :mediaItemId LIMIT 1")
    suspend fun findByMediaItemId(mediaItemId: Long): EpisodeReminderStateEntity?

    @Upsert
    suspend fun upsert(state: EpisodeReminderStateEntity): Long
}
