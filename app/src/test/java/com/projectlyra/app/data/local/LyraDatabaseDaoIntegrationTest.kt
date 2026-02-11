package com.projectlyra.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LyraDatabaseDaoIntegrationTest {
    private lateinit var context: Context
    private lateinit var db: LyraDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, LyraDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun observeItemsByStatus_ordersByUpdatedAtDesc() = runBlocking {
        val mediaOneId = db.mediaDao().upsertMediaItem(
            media(1, "TV", "Older")
        )
        val mediaTwoId = db.mediaDao().upsertMediaItem(
            media(2, "TV", "Newer")
        )
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = mediaOneId, status = "WATCHING", addedAt = 1L, updatedAt = 10L)
        )
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = mediaTwoId, status = "WATCHING", addedAt = 1L, updatedAt = 20L)
        )

        val rows = db.userEntryDao().observeItemsByStatus("WATCHING").first()

        assertEquals(2, rows.size)
        assertEquals("Newer", rows[0].title)
        assertEquals("Older", rows[1].title)
    }

    @Test
    fun getLatestItemByStatus_returnsMostRecentlyUpdatedRow() = runBlocking {
        val olderId = db.mediaDao().upsertMediaItem(media(41, "TV", "Older"))
        val newerId = db.mediaDao().upsertMediaItem(media(42, "TV", "Newer"))
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = olderId, status = "WATCHING", addedAt = 1L, updatedAt = 10L)
        )
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = newerId, status = "WATCHING", addedAt = 1L, updatedAt = 20L)
        )

        val row = db.userEntryDao().getLatestItemByStatus("WATCHING")

        assertEquals("Newer", row?.title)
    }

    @Test
    fun getMediaGenresByStatus_filtersByMediaTypeAndStatus() = runBlocking {
        val completedId = db.mediaDao().upsertMediaItem(media(51, "MOVIE", "Completed").copy(genreIdsCsv = "18,35"))
        val completedTvId = db.mediaDao().upsertMediaItem(media(53, "TV", "Completed TV").copy(genreIdsCsv = "16,10765"))
        val watchingId = db.mediaDao().upsertMediaItem(media(52, "MOVIE", "Watching").copy(genreIdsCsv = "99"))
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = completedId, status = "COMPLETED", addedAt = 1L, updatedAt = 1L)
        )
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = completedTvId, status = "COMPLETED", addedAt = 1L, updatedAt = 1L)
        )
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = watchingId, status = "WATCHING", addedAt = 1L, updatedAt = 1L)
        )

        val movieRows = db.userEntryDao().getMediaGenresByStatus("COMPLETED", "MOVIE")
        val tvRows = db.userEntryDao().getMediaGenresByStatus("COMPLETED", "TV")

        assertEquals(1, movieRows.size)
        assertEquals("18,35", movieRows.first().genreIdsCsv)
        assertEquals(1, tvRows.size)
        assertEquals("16,10765", tvRows.first().genreIdsCsv)
    }

    @Test
    fun getCachedTrending_returnsRowsByPosition() = runBlocking {
        val mediaOneId = db.mediaDao().upsertMediaItem(media(11, "MOVIE", "Second"))
        val mediaTwoId = db.mediaDao().upsertMediaItem(media(12, "TV", "First"))
        db.trendingCacheDao().upsertAll(
            listOf(
                TrendingCacheEntity(mediaItemId = mediaOneId, position = 1, cachedAt = 100L),
                TrendingCacheEntity(mediaItemId = mediaTwoId, position = 0, cachedAt = 100L),
            )
        )

        val rows = db.trendingCacheDao().getCachedTrending()

        assertEquals(2, rows.size)
        assertEquals("First", rows[0].title)
        assertEquals("Second", rows[1].title)
    }

    @Test
    fun deletingMedia_cascadesUserEntriesAndReminderState() = runBlocking {
        val mediaId = db.mediaDao().upsertMediaItem(media(22, "TV", "Cascade"))
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = mediaId, status = "WATCHING", addedAt = 5L, updatedAt = 5L)
        )
        db.episodeReminderStateDao().upsert(
            EpisodeReminderStateEntity(
                mediaItemId = mediaId,
                lastCheckedAt = 5L,
                lastKnownEpisodeCount = 10,
                lastKnownSeasonCount = 1,
            )
        )

        db.mediaDao().clearAll()

        assertTrue(db.userEntryDao().getAllUserEntries().isEmpty())
        assertTrue(db.episodeReminderStateDao().getAll().isEmpty())
    }

    @Test
    fun getTvReminderCandidates_filtersByTvAndStatuses() = runBlocking {
        val tvId = db.mediaDao().upsertMediaItem(media(31, "TV", "TV Show"))
        val movieId = db.mediaDao().upsertMediaItem(media(32, "MOVIE", "Movie"))
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = tvId, status = "WATCHING", addedAt = 1L, updatedAt = 1L)
        )
        db.userEntryDao().upsertUserEntry(
            UserEntryEntity(mediaItemId = movieId, status = "WATCHING", addedAt = 1L, updatedAt = 1L)
        )

        val candidates = db.userEntryDao().getTvReminderCandidates(listOf("WATCHING", "ON_HOLD", "COMPLETED"))

        assertEquals(1, candidates.size)
        assertEquals(31, candidates.first().tmdbId)
    }

    private fun media(tmdbId: Int, mediaType: String, title: String): MediaItemEntity {
        return MediaItemEntity(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            overview = "Overview",
            posterPath = "/poster.jpg",
            releaseOrAirDate = "2024-01-01",
            metadataUpdatedAt = 1L,
        )
    }
}
