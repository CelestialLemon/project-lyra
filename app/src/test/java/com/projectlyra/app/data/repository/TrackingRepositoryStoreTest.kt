package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.local.CompletedMediaGenreRow
import com.projectlyra.app.data.local.EpisodeReminderStateDao
import com.projectlyra.app.data.local.EpisodeReminderStateEntity
import com.projectlyra.app.data.local.MediaDao
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.TrackedMediaKeyRow
import com.projectlyra.app.data.local.TvReminderCandidateRow
import com.projectlyra.app.data.local.UserEntryDao
import com.projectlyra.app.data.local.UserEntryEntity
import com.projectlyra.app.data.local.UserListRow
import com.projectlyra.app.data.local.UserStatusRow
import com.projectlyra.app.data.local.WatchedEpisodeDao
import com.projectlyra.app.data.local.WatchedEpisodeEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingRepositoryStoreTest {
    @Test
    fun upsertTrackedStatus_persistsMediaAndStatus() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 100L },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 101), WatchStatus.WATCHING)

        val items = store.observeByStatus(WatchStatus.WATCHING).first()
        assertEquals(1, items.size)
        assertEquals(101, items.first().tmdbId)
        assertEquals(WatchStatus.WATCHING, items.first().status)
    }

    @Test
    fun getResumeCandidate_prefersWatchingOverOnHold() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        var now = 100L
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { now },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 1, title = "On Hold Show"), WatchStatus.ON_HOLD)
        now = 200L
        store.upsertTrackedStatus(sampleItem(tmdbId = 2, title = "Watching Show"), WatchStatus.WATCHING)

        val candidate = store.getResumeCandidate()

        assertEquals(2, candidate?.tmdbId)
        assertEquals(WatchStatus.WATCHING, candidate?.status)
    }

    @Test
    fun getResumeCandidate_fallsBackToOnHoldWhenWatchingMissing() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 100L },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 44, title = "On Hold Candidate"), WatchStatus.ON_HOLD)

        val candidate = store.getResumeCandidate()

        assertEquals(44, candidate?.tmdbId)
        assertEquals(WatchStatus.ON_HOLD, candidate?.status)
    }

    @Test
    fun getTopCompletedGenreIds_returnsTopThreeByFrequencyPerMediaType() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 100L },
        )

        store.upsertTrackedStatus(
            sampleItem(tmdbId = 1001, mediaType = MediaType.MOVIE, title = "A", genreIds = listOf(18, 28)),
            WatchStatus.COMPLETED,
        )
        store.upsertTrackedStatus(
            sampleItem(tmdbId = 1002, mediaType = MediaType.MOVIE, title = "B", genreIds = listOf(18, 35)),
            WatchStatus.COMPLETED,
        )
        store.upsertTrackedStatus(
            sampleItem(tmdbId = 1003, mediaType = MediaType.TV, title = "C", genreIds = listOf(18, 35, 12)),
            WatchStatus.COMPLETED,
        )

        val movieGenres = store.getTopCompletedGenreIds(mediaType = MediaType.MOVIE, limit = 3)
        val tvGenres = store.getTopCompletedGenreIds(mediaType = MediaType.TV, limit = 3)

        assertEquals(listOf(18, 28, 35), movieGenres)
        assertEquals(listOf(12, 18, 35), tvGenres)
    }

    @Test
    fun updateTrackedStatus_withSameStatus_doesNotBumpTimestamp() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        var now = 100L
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { now },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 111), WatchStatus.WATCHING)
        val mediaId = mediaDao.findByTmdbAndType(111, MediaType.TV.name)!!.id
        val originalUpdatedAt = userEntryDao.findByMediaItemId(mediaId)!!.updatedAt
        now = 250L

        store.updateTrackedStatus(mediaItemId = mediaId, status = WatchStatus.WATCHING)

        val unchangedUpdatedAt = userEntryDao.findByMediaItemId(mediaId)!!.updatedAt
        assertEquals(originalUpdatedAt, unchangedUpdatedAt)
    }

    @Test
    fun observeTrackedStatusesByMediaKey_ignoresInvalidEnums() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 1L },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 201, mediaType = MediaType.MOVIE), WatchStatus.COMPLETED)
        val invalidMediaId = mediaDao.upsertMediaItem(
            MediaItemEntity(
                tmdbId = 202,
                mediaType = "ANIME",
                title = "Bad media type",
                overview = "",
                posterPath = "/bad.jpg",
                releaseOrAirDate = "2025-01-01",
                metadataUpdatedAt = 10L,
            )
        )
        userEntryDao.upsertUserEntry(
            UserEntryEntity(
                mediaItemId = invalidMediaId,
                status = "INVALID_STATUS",
                addedAt = 10L,
                updatedAt = 10L,
            )
        )

        val map = store.observeTrackedStatusesByMediaKey().first()

        assertEquals(1, map.size)
        assertEquals(WatchStatus.COMPLETED, map["MOVIE:201"])
        assertFalse(map.containsKey("ANIME:202"))
    }

    @Test
    fun getTvReminderCandidates_returnsOnlyEligibleStatuses() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 1L },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 301, title = "Watching"), WatchStatus.WATCHING)
        store.upsertTrackedStatus(sampleItem(tmdbId = 302, title = "Completed"), WatchStatus.COMPLETED)
        store.upsertTrackedStatus(sampleItem(tmdbId = 303, title = "On Hold"), WatchStatus.ON_HOLD)
        store.upsertTrackedStatus(sampleItem(tmdbId = 304, title = "Dropped"), WatchStatus.DROPPED)
        store.upsertTrackedStatus(
            sampleItem(tmdbId = 305, title = "Movie", mediaType = MediaType.MOVIE),
            WatchStatus.WATCHING,
        )

        val candidates = store.getTvReminderCandidates()
        val ids = candidates.map { it.tmdbId }.toSet()

        assertEquals(3, candidates.size)
        assertTrue(ids.contains(301))
        assertTrue(ids.contains(302))
        assertTrue(ids.contains(303))
        assertFalse(ids.contains(304))
        assertFalse(ids.contains(305))
    }

    @Test
    fun upsertEpisodeReminderState_updatesExistingRow() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 1L },
        )
        val mediaId = mediaDao.upsertMediaItem(
            MediaItemEntity(
                tmdbId = 401,
                mediaType = MediaType.TV.name,
                title = "Title",
                overview = "",
                posterPath = "/a.jpg",
                releaseOrAirDate = "2024-01-01",
                metadataUpdatedAt = 1L,
            )
        )

        store.upsertEpisodeReminderState(
            mediaItemId = mediaId,
            lastCheckedAt = 10L,
            lastKnownEpisodeCount = 8,
            lastKnownSeasonCount = 1,
        )
        store.upsertEpisodeReminderState(
            mediaItemId = mediaId,
            lastCheckedAt = 20L,
            lastKnownEpisodeCount = 10,
            lastKnownSeasonCount = 2,
        )

        val snapshot = store.getEpisodeReminderState(mediaId)
        assertEquals(20L, snapshot?.lastCheckedAt)
        assertEquals(10, snapshot?.lastKnownEpisodeCount)
        assertEquals(2, snapshot?.lastKnownSeasonCount)
        assertEquals(1, reminderDao.getAll().size)
    }

    @Test
    fun markWatchedUpToEpisode_persistsEpisodesFromOneToSelectedEpisode() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 1L },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 511), WatchStatus.WATCHING)
        store.markWatchedUpToEpisode(tmdbId = 511, seasonNumber = 2, episodeNumber = 3)

        val watched = store.observeWatchedEpisodeNumbersBySeason(tmdbId = 511, seasonNumber = 2).first()
        assertEquals(setOf(1, 2, 3), watched)
    }

    @Test
    fun markUnwatchedFromEpisode_removesSelectedAndLaterEpisodesOnly() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 1L },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 512), WatchStatus.WATCHING)
        store.markWatchedUpToEpisode(tmdbId = 512, seasonNumber = 1, episodeNumber = 5)

        store.markUnwatchedFromEpisode(tmdbId = 512, seasonNumber = 1, episodeNumber = 3)

        val watched = store.observeWatchedEpisodeNumbersBySeason(tmdbId = 512, seasonNumber = 1).first()
        assertEquals(setOf(1, 2), watched)
    }

    @Test
    fun clearTrackedStatus_keepsWatchedEpisodes() = runBlocking {
        val mediaDao = FakeMediaDao()
        val userEntryDao = FakeUserEntryDao(mediaDao)
        val reminderDao = FakeEpisodeReminderStateDao()
        val watchedDao = FakeWatchedEpisodeDao()
        val store = TrackingRepositoryStore(
            mediaDao = mediaDao,
            userEntryDao = userEntryDao,
            episodeReminderStateDao = reminderDao,
            watchedEpisodeDao = watchedDao,
            nowProvider = { 1L },
        )

        store.upsertTrackedStatus(sampleItem(tmdbId = 513), WatchStatus.WATCHING)
        store.markWatchedUpToEpisode(tmdbId = 513, seasonNumber = 1, episodeNumber = 2)

        store.clearTrackedStatus(tmdbId = 513, mediaType = MediaType.TV)

        val watched = store.observeWatchedEpisodeNumbersBySeason(tmdbId = 513, seasonNumber = 1).first()
        assertEquals(setOf(1, 2), watched)
    }

    private fun sampleItem(
        tmdbId: Int,
        title: String = "Sample",
        mediaType: MediaType = MediaType.TV,
        genreIds: List<Int> = emptyList(),
    ): TrendingItem {
        return TrendingItem(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            overview = "Overview",
            posterPath = "/poster.jpg",
            releaseOrAirDate = "2024-01-01",
            genreIds = genreIds,
        )
    }

    private class FakeMediaDao : MediaDao {
        private val itemsById = linkedMapOf<Long, MediaItemEntity>()
        private var nextId = 1L

        override suspend fun upsertMediaItem(mediaItem: MediaItemEntity): Long {
            val existing = itemsById.values.firstOrNull {
                it.tmdbId == mediaItem.tmdbId && it.mediaType == mediaItem.mediaType
            }
            val id = when {
                mediaItem.id != 0L -> mediaItem.id
                existing != null -> existing.id
                else -> nextId++
            }
            itemsById[id] = mediaItem.copy(id = id)
            return id
        }

        override suspend fun findByTmdbAndType(tmdbId: Int, mediaType: String): MediaItemEntity? {
            return itemsById.values.firstOrNull { it.tmdbId == tmdbId && it.mediaType == mediaType }
        }

        override suspend fun getAllMediaItems(): List<MediaItemEntity> = itemsById.values.toList()

        override suspend fun clearAll(): Int {
            val count = itemsById.size
            itemsById.clear()
            return count
        }

        override suspend fun mediaCount(): Int = itemsById.size

        fun byId(id: Long): MediaItemEntity? = itemsById[id]
    }

    private class FakeUserEntryDao(
        private val mediaDao: FakeMediaDao,
    ) : UserEntryDao {
        private val entriesByMediaId = linkedMapOf<Long, UserEntryEntity>()
        private val mutationTick = MutableStateFlow(0)
        private var nextId = 1L

        override suspend fun upsertUserEntry(userEntryEntity: UserEntryEntity): Long {
            val existing = entriesByMediaId[userEntryEntity.mediaItemId]
            val id = when {
                userEntryEntity.id != 0L -> userEntryEntity.id
                existing != null -> existing.id
                else -> nextId++
            }
            entriesByMediaId[userEntryEntity.mediaItemId] = userEntryEntity.copy(id = id)
            mutationTick.value += 1
            return id
        }

        override suspend fun findByMediaItemId(mediaItemId: Long): UserEntryEntity? {
            return entriesByMediaId[mediaItemId]
        }

        override suspend fun getAllUserEntries(): List<UserEntryEntity> = entriesByMediaId.values.toList()

        override suspend fun clearAll(): Int {
            val count = entriesByMediaId.size
            entriesByMediaId.clear()
            mutationTick.value += 1
            return count
        }

        override suspend fun deleteByMediaItemId(mediaItemId: Long): Int {
            val deleted = if (entriesByMediaId.remove(mediaItemId) != null) 1 else 0
            if (deleted > 0) {
                mutationTick.value += 1
            }
            return deleted
        }

        override fun observeItemsByStatus(status: String): Flow<List<UserListRow>> {
            return mutationTick.map {
                entriesByMediaId.values
                    .filter { entry -> entry.status == status }
                    .mapNotNull { entry -> entry.toUserListRow(mediaDao) }
                    .sortedByDescending { row -> row.updatedAt }
            }
        }

        override suspend fun getLatestItemByStatus(status: String): UserListRow? {
            return entriesByMediaId.values
                .filter { entry -> entry.status == status }
                .sortedByDescending { entry -> entry.updatedAt }
                .firstOrNull()
                ?.toUserListRow(mediaDao)
        }

        override fun observeTrackedStatuses(): Flow<List<UserStatusRow>> {
            return mutationTick.map {
                entriesByMediaId.values.mapNotNull { entry ->
                    val media = mediaDao.byId(entry.mediaItemId) ?: return@mapNotNull null
                    UserStatusRow(
                        tmdbId = media.tmdbId,
                        mediaType = media.mediaType,
                        status = entry.status,
                    )
                }
            }
        }

        override suspend fun getTrackedMediaKeys(): List<TrackedMediaKeyRow> {
            return entriesByMediaId.values.mapNotNull { entry ->
                val media = mediaDao.byId(entry.mediaItemId) ?: return@mapNotNull null
                TrackedMediaKeyRow(
                    tmdbId = media.tmdbId,
                    mediaType = media.mediaType,
                )
            }
        }

        override suspend fun getMediaGenresByStatus(status: String, mediaType: String): List<CompletedMediaGenreRow> {
            return entriesByMediaId.values
                .filter { entry ->
                    if (entry.status != status) {
                        return@filter false
                    }
                    val media = mediaDao.byId(entry.mediaItemId) ?: return@filter false
                    media.mediaType == mediaType
                }
                .mapNotNull { entry ->
                    val media = mediaDao.byId(entry.mediaItemId) ?: return@mapNotNull null
                    CompletedMediaGenreRow(genreIdsCsv = media.genreIdsCsv)
                }
        }

        override suspend fun getTvReminderCandidates(statuses: List<String>): List<TvReminderCandidateRow> {
            return entriesByMediaId.values.mapNotNull { entry ->
                val media = mediaDao.byId(entry.mediaItemId) ?: return@mapNotNull null
                if (media.mediaType != MediaType.TV.name || entry.status !in statuses) {
                    return@mapNotNull null
                }
                TvReminderCandidateRow(
                    localId = media.id,
                    tmdbId = media.tmdbId,
                    title = media.title,
                    status = entry.status,
                )
            }
        }

        private fun UserEntryEntity.toUserListRow(mediaDao: FakeMediaDao): UserListRow? {
            val media = mediaDao.byId(mediaItemId) ?: return null
            return UserListRow(
                localId = media.id,
                tmdbId = media.tmdbId,
                mediaType = media.mediaType,
                title = media.title,
                overview = media.overview,
                posterPath = media.posterPath,
                releaseOrAirDate = media.releaseOrAirDate,
                status = status,
                updatedAt = updatedAt,
            )
        }
    }

    private class FakeEpisodeReminderStateDao : EpisodeReminderStateDao {
        private val rowsByMediaId = linkedMapOf<Long, EpisodeReminderStateEntity>()
        private var nextId = 1L

        override suspend fun findByMediaItemId(mediaItemId: Long): EpisodeReminderStateEntity? {
            return rowsByMediaId[mediaItemId]
        }

        override suspend fun getAll(): List<EpisodeReminderStateEntity> = rowsByMediaId.values.toList()

        override suspend fun clearAll(): Int {
            val count = rowsByMediaId.size
            rowsByMediaId.clear()
            return count
        }

        override suspend fun upsert(state: EpisodeReminderStateEntity): Long {
            val existing = rowsByMediaId[state.mediaItemId]
            val id = when {
                state.id != 0L -> state.id
                existing != null -> existing.id
                else -> nextId++
            }
            rowsByMediaId[state.mediaItemId] = state.copy(id = id)
            return id
        }
    }

    private class FakeWatchedEpisodeDao : WatchedEpisodeDao {
        private val rows = mutableListOf<WatchedEpisodeEntity>()
        private val mutationTick = MutableStateFlow(0)
        private var nextId = 1L

        override fun observeEpisodeNumbersBySeason(mediaItemId: Long, seasonNumber: Int): Flow<List<Int>> {
            return mutationTick.map {
                rows.asSequence()
                    .filter { row -> row.mediaItemId == mediaItemId && row.seasonNumber == seasonNumber }
                    .map { row -> row.episodeNumber }
                    .sorted()
                    .toList()
            }
        }

        override suspend fun getEpisodeNumbersBySeason(mediaItemId: Long, seasonNumber: Int): List<Int> {
            return rows
                .filter { row -> row.mediaItemId == mediaItemId && row.seasonNumber == seasonNumber }
                .map { row -> row.episodeNumber }
                .sorted()
        }

        override suspend fun getAll(): List<WatchedEpisodeEntity> {
            return rows.sortedWith(compareBy({ it.mediaItemId }, { it.seasonNumber }, { it.episodeNumber }))
        }

        override suspend fun upsertAll(entries: List<WatchedEpisodeEntity>) {
            entries.forEach { incoming ->
                val existingIndex = rows.indexOfFirst { row ->
                    row.mediaItemId == incoming.mediaItemId &&
                        row.seasonNumber == incoming.seasonNumber &&
                        row.episodeNumber == incoming.episodeNumber
                }
                if (existingIndex >= 0) {
                    rows[existingIndex] = incoming.copy(id = rows[existingIndex].id)
                } else {
                    rows += incoming.copy(id = (if (incoming.id == 0L) nextId++ else incoming.id))
                }
            }
            mutationTick.value += 1
        }

        override suspend fun deleteFromEpisode(mediaItemId: Long, seasonNumber: Int, episodeNumber: Int): Int {
            val originalSize = rows.size
            rows.removeAll { row ->
                row.mediaItemId == mediaItemId &&
                    row.seasonNumber == seasonNumber &&
                    row.episodeNumber >= episodeNumber
            }
            val deleted = originalSize - rows.size
            if (deleted > 0) {
                mutationTick.value += 1
            }
            return deleted
        }

        override suspend fun deleteBySeason(mediaItemId: Long, seasonNumber: Int): Int {
            val originalSize = rows.size
            rows.removeAll { row ->
                row.mediaItemId == mediaItemId && row.seasonNumber == seasonNumber
            }
            val deleted = originalSize - rows.size
            if (deleted > 0) {
                mutationTick.value += 1
            }
            return deleted
        }

        override suspend fun clearAll(): Int {
            val size = rows.size
            rows.clear()
            if (size > 0) {
                mutationTick.value += 1
            }
            return size
        }
    }
}
