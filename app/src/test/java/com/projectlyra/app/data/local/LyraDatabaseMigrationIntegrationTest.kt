package com.projectlyra.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyraDatabaseMigrationIntegrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun migration1To4_preservesTrackedDataAndCreatesNewTables() {
        runBlocking {
            val name = "migration-v1-${UUID.randomUUID()}.db"
            context.deleteDatabase(name)
            createLegacyDatabase(name = name, version = 1) { db ->
                db.execSQL(
                    """
                    INSERT INTO media_items (tmdb_id, media_type, title, overview, poster_path, release_or_air_date, metadata_updated_at)
                    VALUES (101, 'TV', 'Legacy Show', 'Overview', '/legacy.jpg', '2020-01-01', 10)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO user_entries (media_item_id, status, added_at, updated_at)
                    VALUES (1, 'WATCHING', 10, 20)
                    """.trimIndent()
                )
            }
    
            val migrated = Room.databaseBuilder(context, LyraDatabase::class.java, name)
                .addMigrations(*LyraDatabaseMigrations.ALL)
                .allowMainThreadQueries()
                .build()
    
            assertEquals(1, migrated.mediaDao().mediaCount())
            assertEquals(1, migrated.userEntryDao().getAllUserEntries().size)
            assertTrue(tableExists(migrated.openHelper.writableDatabase, "trending_cache"))
            assertTrue(tableExists(migrated.openHelper.writableDatabase, "episode_reminder_state"))
            assertTrue(tableExists(migrated.openHelper.writableDatabase, "genre_metadata"))
            assertTrue(columnExists(migrated.openHelper.writableDatabase, "media_items", "genre_ids"))
    
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migration2To4_preservesTrendingCacheRows() {
        runBlocking {
            val name = "migration-v2-${UUID.randomUUID()}.db"
            context.deleteDatabase(name)
            createLegacyDatabase(name = name, version = 2) { db ->
                db.execSQL(
                    """
                    INSERT INTO media_items (tmdb_id, media_type, title, overview, poster_path, release_or_air_date, metadata_updated_at)
                    VALUES (202, 'MOVIE', 'Legacy Movie', 'Overview', '/legacy-movie.jpg', '2021-02-02', 30)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO trending_cache (media_item_id, position, cached_at)
                    VALUES (1, 0, 99)
                    """.trimIndent()
                )
            }
    
            val migrated = Room.databaseBuilder(context, LyraDatabase::class.java, name)
                .addMigrations(*LyraDatabaseMigrations.ALL)
                .allowMainThreadQueries()
                .build()
    
            val cached = migrated.trendingCacheDao().getCachedTrending()
            assertEquals(1, cached.size)
            assertEquals(202, cached.first().tmdbId)
            assertEquals(99L, migrated.trendingCacheDao().latestCachedAt())
            assertTrue(tableExists(migrated.openHelper.writableDatabase, "episode_reminder_state"))
            assertTrue(tableExists(migrated.openHelper.writableDatabase, "genre_metadata"))
            assertTrue(columnExists(migrated.openHelper.writableDatabase, "media_items", "genre_ids"))
    
            migrated.close()
            context.deleteDatabase(name)
        }
    }

    @Test
    fun migration3To4_preservesMediaAndAddsGenreSchema() {
        runBlocking {
            val name = "migration-v3-${UUID.randomUUID()}.db"
            context.deleteDatabase(name)
            createLegacyDatabase(name = name, version = 3) { db ->
                db.execSQL(
                    """
                    INSERT INTO media_items (tmdb_id, media_type, title, overview, poster_path, release_or_air_date, metadata_updated_at)
                    VALUES (303, 'TV', 'Legacy TV', 'Overview', '/legacy-tv.jpg', '2022-02-02', 40)
                    """.trimIndent()
                )
            }

            val migrated = Room.databaseBuilder(context, LyraDatabase::class.java, name)
                .addMigrations(*LyraDatabaseMigrations.ALL)
                .allowMainThreadQueries()
                .build()

            assertEquals(1, migrated.mediaDao().mediaCount())
            assertTrue(columnExists(migrated.openHelper.writableDatabase, "media_items", "genre_ids"))
            assertTrue(tableExists(migrated.openHelper.writableDatabase, "genre_metadata"))

            migrated.close()
            context.deleteDatabase(name)
        }
    }

    private fun createLegacyDatabase(
        name: String,
        version: Int,
        seed: (SupportSQLiteDatabase) -> Unit,
    ) {
        val callback = object : SupportSQLiteOpenHelper.Callback(version) {
            override fun onCreate(db: SupportSQLiteDatabase) {
                createVersion1Schema(db)
                if (version >= 2) {
                    createVersion2Schema(db)
                }
                if (version >= 3) {
                    createVersion3Schema(db)
                }
            }

            override fun onUpgrade(
                db: SupportSQLiteDatabase,
                oldVersion: Int,
                newVersion: Int,
            ) = Unit
        }
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(name)
            .callback(callback)
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        val db = helper.writableDatabase
        seed(db)
        db.close()
        helper.close()
    }

    private fun createVersion1Schema(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `media_items` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `tmdb_id` INTEGER NOT NULL,
                `media_type` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `overview` TEXT NOT NULL,
                `poster_path` TEXT NOT NULL,
                `release_or_air_date` TEXT NOT NULL,
                `metadata_updated_at` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_media_items_tmdb_id_media_type` ON `media_items` (`tmdb_id`, `media_type`)"
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `user_entries` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `media_item_id` INTEGER NOT NULL,
                `status` TEXT NOT NULL,
                `added_at` INTEGER NOT NULL,
                `updated_at` INTEGER NOT NULL,
                FOREIGN KEY(`media_item_id`) REFERENCES `media_items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_user_entries_media_item_id` ON `user_entries` (`media_item_id`)"
        )
    }

    private fun createVersion2Schema(db: SupportSQLiteDatabase) {
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

    private fun createVersion3Schema(db: SupportSQLiteDatabase) {
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

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean {
        db.query("SELECT name FROM sqlite_master WHERE type='table' AND name=?", arrayOf(table)).use { cursor ->
            return cursor.moveToFirst()
        }
    }

    private fun columnExists(db: SupportSQLiteDatabase, table: String, column: String): Boolean {
        db.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (nameIndex >= 0 && cursor.getString(nameIndex) == column) {
                    return true
                }
            }
        }
        return false
    }
}
