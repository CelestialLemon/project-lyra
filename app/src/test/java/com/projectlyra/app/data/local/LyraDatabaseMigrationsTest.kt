package com.projectlyra.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyraDatabaseMigrationsTest {
    @Test
    fun migration1To2_createsTrendingCacheTableAndIndexes() {
        val executedSql = mutableListOf<String>()
        val db = recordingDatabase(executedSql)

        LyraDatabaseMigrations.MIGRATION_1_2.migrate(db)

        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `trending_cache`") })
        assertTrue(executedSql.any { it.contains("index_trending_cache_position") })
        assertTrue(executedSql.any { it.contains("index_trending_cache_media_item_id") })
    }

    @Test
    fun migration2To3_createsReminderStateTableAndIndex() {
        val executedSql = mutableListOf<String>()
        val db = recordingDatabase(executedSql)

        LyraDatabaseMigrations.MIGRATION_2_3.migrate(db)

        assertTrue(executedSql.any { it.contains("CREATE TABLE IF NOT EXISTS `episode_reminder_state`") })
        assertTrue(executedSql.any { it.contains("FOREIGN KEY(`media_item_id`) REFERENCES `media_items`(`id`)") })
        assertTrue(executedSql.any { it.contains("index_episode_reminder_state_media_item_id") })
    }

    @Test
    fun allMigrations_areOrderedSequentially() {
        assertEquals(2, LyraDatabaseMigrations.ALL.size)
        assertEquals(1, LyraDatabaseMigrations.ALL[0].startVersion)
        assertEquals(2, LyraDatabaseMigrations.ALL[0].endVersion)
        assertEquals(2, LyraDatabaseMigrations.ALL[1].startVersion)
        assertEquals(3, LyraDatabaseMigrations.ALL[1].endVersion)
    }

    private fun recordingDatabase(executedSql: MutableList<String>): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java),
        ) { _, method, args ->
            when (method.name) {
                "execSQL" -> {
                    if (args?.isNotEmpty() == true && args[0] is String) {
                        executedSql += args[0] as String
                    }
                    null
                }
                "toString" -> "RecordingSupportSQLiteDatabase"
                else -> defaultValue(method.returnType)
            }
        } as SupportSQLiteDatabase
    }

    private fun defaultValue(returnType: Class<*>): Any? {
        return when {
            returnType == java.lang.Boolean.TYPE -> false
            returnType == java.lang.Integer.TYPE -> 0
            returnType == java.lang.Long.TYPE -> 0L
            returnType == java.lang.Float.TYPE -> 0f
            returnType == java.lang.Double.TYPE -> 0.0
            returnType == java.lang.Short.TYPE -> 0.toShort()
            returnType == java.lang.Byte.TYPE -> 0.toByte()
            returnType == java.lang.Character.TYPE -> '\u0000'
            else -> null
        }
    }
}
