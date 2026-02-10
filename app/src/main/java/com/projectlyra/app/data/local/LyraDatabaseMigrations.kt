package com.projectlyra.app.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object LyraDatabaseMigrations {
    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `trending_cache` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `media_item_id` INTEGER NOT NULL,
                    `position` INTEGER NOT NULL,
                    `cached_at` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_trending_cache_position` ON `trending_cache` (`position`)"
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_trending_cache_media_item_id` ON `trending_cache` (`media_item_id`)"
            )
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `episode_reminder_state` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `media_item_id` INTEGER NOT NULL,
                    `last_checked_at` INTEGER NOT NULL,
                    `last_known_episode_count` INTEGER,
                    `last_known_season_count` INTEGER,
                    FOREIGN KEY(`media_item_id`) REFERENCES `media_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_episode_reminder_state_media_item_id` ON `episode_reminder_state` (`media_item_id`)"
            )
        }
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
    )
}
