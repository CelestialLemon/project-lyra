package com.projectlyra.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        MediaItemEntity::class,
        UserEntryEntity::class,
        TrendingCacheEntity::class,
        EpisodeReminderStateEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class LyraDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun userEntryDao(): UserEntryDao
    abstract fun trendingCacheDao(): TrendingCacheDao
    abstract fun episodeReminderStateDao(): EpisodeReminderStateDao
}
