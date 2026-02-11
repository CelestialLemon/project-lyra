package com.projectlyra.app.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupRestorePlanTest {
    @Test
    fun validator_rejectsDuplicateUserEntries() {
        val doc = validDocument().copy(
            userEntries = listOf(
                validUserEntry(),
                validUserEntry(),
            )
        )

        val error = BackupDocumentValidator.validate(doc)

        assertNotNull(error)
        assertTrue(error!!.contains("duplicate user entries"))
    }

    @Test
    fun validator_rejectsUserEntryReferencingMissingMedia() {
        val doc = validDocument().copy(
            mediaItems = emptyList(),
            userEntries = listOf(validUserEntry()),
        )

        val error = BackupDocumentValidator.validate(doc)

        assertNotNull(error)
        assertTrue(error!!.contains("references media not present"))
    }

    @Test
    fun restorePlanFactory_normalizesMediaTypesAndStatuses() {
        val doc = validDocument(
            mediaType = "movie",
            status = "watching",
        )

        val validationError = BackupDocumentValidator.validate(doc)
        assertNull(validationError)

        val plan = BackupRestorePlanFactory.create(doc)

        assertEquals(1, plan.mediaItems.size)
        assertEquals("MOVIE", plan.mediaItems.first().mediaType)
        assertEquals(1, plan.userEntries.size)
        assertEquals("MOVIE", plan.userEntries.first().mediaType)
        assertEquals("WATCHING", plan.userEntries.first().status)
        assertEquals(1, plan.episodeReminderStates.size)
        assertEquals("MOVIE", plan.episodeReminderStates.first().mediaType)
        assertEquals(1, plan.watchedEpisodes.size)
        assertEquals("MOVIE", plan.watchedEpisodes.first().mediaType)
    }

    @Test
    fun validator_rejectsDuplicateWatchedEpisodes() {
        val doc = validDocument(
            mediaType = "TV",
            watchedEpisodes = listOf(
                LyraBackupWatchedEpisode(
                    tmdbId = 42,
                    mediaType = "TV",
                    seasonNumber = 1,
                    episodeNumber = 2,
                ),
                LyraBackupWatchedEpisode(
                    tmdbId = 42,
                    mediaType = "TV",
                    seasonNumber = 1,
                    episodeNumber = 2,
                ),
            ),
        )

        val error = BackupDocumentValidator.validate(doc)

        assertNotNull(error)
        assertTrue(error!!.contains("duplicate watched episodes"))
    }

    @Test
    fun validator_rejectsInvalidWatchedEpisodeBounds() {
        val doc = validDocument(
            watchedEpisodes = listOf(
                LyraBackupWatchedEpisode(
                    tmdbId = 42,
                    mediaType = "MOVIE",
                    seasonNumber = -1,
                    episodeNumber = 0,
                )
            ),
        )

        val error = BackupDocumentValidator.validate(doc)

        assertNotNull(error)
        assertTrue(error!!.contains("invalid season number"))
    }

    private fun validDocument(
        mediaType: String = "MOVIE",
        status: String = "WATCHING",
        watchedEpisodes: List<LyraBackupWatchedEpisode> = listOf(
            LyraBackupWatchedEpisode(
                tmdbId = 42,
                mediaType = mediaType,
                seasonNumber = 1,
                episodeNumber = 1,
            )
        ),
    ): LyraBackupDocument {
        return LyraBackupDocument(
            schemaVersion = LYRA_BACKUP_SCHEMA_VERSION,
            exportedAtEpochMs = 1_700_000_000_000,
            settings = LyraBackupSettings(
                reminderEnabled = true,
                reminderHour = 20,
                reminderMinute = 0,
                includeApiKeyInBackup = false,
                apiKey = null,
            ),
            mediaItems = listOf(
                LyraBackupMediaItem(
                    tmdbId = 42,
                    mediaType = mediaType,
                    title = "Title",
                    overview = "Overview",
                    posterPath = "/poster.jpg",
                    releaseOrAirDate = "2024-01-01",
                    metadataUpdatedAt = 1234L,
                )
            ),
            userEntries = listOf(validUserEntry(mediaType = mediaType, status = status)),
            episodeReminderStates = listOf(
                LyraBackupEpisodeReminderState(
                    tmdbId = 42,
                    mediaType = mediaType,
                    lastCheckedAt = 5678L,
                    lastKnownEpisodeCount = 10,
                    lastKnownSeasonCount = 2,
                )
            ),
            watchedEpisodes = watchedEpisodes,
        )
    }

    private fun validUserEntry(
        mediaType: String = "MOVIE",
        status: String = "WATCHING",
    ): LyraBackupUserEntry {
        return LyraBackupUserEntry(
            tmdbId = 42,
            mediaType = mediaType,
            status = status,
            addedAt = 100L,
            updatedAt = 200L,
        )
    }
}
