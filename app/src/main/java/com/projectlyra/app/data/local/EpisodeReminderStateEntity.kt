package com.projectlyra.app.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "episode_reminder_state",
    foreignKeys = [
        ForeignKey(
            entity = MediaItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["media_item_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["media_item_id"], unique = true)],
)
data class EpisodeReminderStateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "media_item_id")
    val mediaItemId: Long,
    @ColumnInfo(name = "last_checked_at")
    val lastCheckedAt: Long,
    @ColumnInfo(name = "last_known_episode_count")
    val lastKnownEpisodeCount: Int?,
    @ColumnInfo(name = "last_known_season_count")
    val lastKnownSeasonCount: Int?,
)
