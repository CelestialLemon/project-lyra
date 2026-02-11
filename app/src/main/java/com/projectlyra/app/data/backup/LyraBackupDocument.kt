package com.projectlyra.app.data.backup

import kotlinx.serialization.Serializable

const val LYRA_BACKUP_SCHEMA_VERSION = 1

@Serializable
data class LyraBackupDocument(
    val schemaVersion: Int,
    val exportedAtEpochMs: Long,
    val settings: LyraBackupSettings,
    val mediaItems: List<LyraBackupMediaItem>,
    val userEntries: List<LyraBackupUserEntry>,
    val episodeReminderStates: List<LyraBackupEpisodeReminderState>,
)

@Serializable
data class LyraBackupSettings(
    val reminderEnabled: Boolean,
    val reminderHour: Int,
    val reminderMinute: Int,
    val includeApiKeyInBackup: Boolean,
    val apiKey: String? = null,
)

@Serializable
data class LyraBackupMediaItem(
    val tmdbId: Int,
    val mediaType: String,
    val title: String,
    val overview: String,
    val posterPath: String,
    val releaseOrAirDate: String,
    val genreIdsCsv: String = "",
    val metadataUpdatedAt: Long,
)

@Serializable
data class LyraBackupUserEntry(
    val tmdbId: Int,
    val mediaType: String,
    val status: String,
    val addedAt: Long,
    val updatedAt: Long,
)

@Serializable
data class LyraBackupEpisodeReminderState(
    val tmdbId: Int,
    val mediaType: String,
    val lastCheckedAt: Long,
    val lastKnownEpisodeCount: Int? = null,
    val lastKnownSeasonCount: Int? = null,
)
