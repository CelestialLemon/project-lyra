package com.projectlyra.app.data.local

import android.content.Context
import androidx.room.Room

object DatabaseFactory {
    fun create(context: Context): LyraDatabase {
        return Room.databaseBuilder(
            context,
            LyraDatabase::class.java,
            "lyra.db",
        ).fallbackToDestructiveMigration().build()
    }
}
