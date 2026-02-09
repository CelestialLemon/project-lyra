package com.projectlyra.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MediaItemEntity::class, UserEntryEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LyraDatabase : RoomDatabase() {
    abstract fun mediaDao(): MediaDao
    abstract fun userEntryDao(): UserEntryDao
}
