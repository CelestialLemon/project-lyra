package com.projectlyra.app.data.backup

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.WatchStatus

internal object BackupDocumentValidator {
    private val SUPPORTED_SCHEMA_VERSIONS = setOf(1, LYRA_BACKUP_SCHEMA_VERSION)

    fun validate(document: LyraBackupDocument): String? {
        if (document.schemaVersion !in SUPPORTED_SCHEMA_VERSIONS) {
            return "Unsupported backup schema version ${document.schemaVersion}."
        }

        if (document.settings.reminderHour !in 0..23) {
            return "Backup reminder hour must be between 0 and 23."
        }

        if (document.settings.reminderMinute !in 0..59) {
            return "Backup reminder minute must be between 0 and 59."
        }

        val mediaKeys = mutableSetOf<String>()
        document.mediaItems.forEachIndexed { index, media ->
            if (media.tmdbId <= 0) {
                return "Media item #${index + 1} has an invalid TMDB id."
            }
            val normalizedMediaType = normalizeMediaType(media.mediaType)
                ?: return "Media item #${index + 1} has an unsupported media type."
            if (media.title.isBlank()) {
                return "Media item #${index + 1} is missing a title."
            }
            if (media.posterPath.isBlank()) {
                return "Media item #${index + 1} is missing a poster path."
            }
            if (media.metadataUpdatedAt < 0L) {
                return "Media item #${index + 1} has an invalid metadata timestamp."
            }

            val key = mediaKey(media.tmdbId, normalizedMediaType)
            if (!mediaKeys.add(key)) {
                return "Backup contains duplicate media items for TMDB ${media.tmdbId} (${normalizedMediaType.lowercase()})."
            }
        }

        val userEntryKeys = mutableSetOf<String>()
        document.userEntries.forEachIndexed { index, entry ->
            if (entry.tmdbId <= 0) {
                return "User entry #${index + 1} has an invalid TMDB id."
            }
            val normalizedMediaType = normalizeMediaType(entry.mediaType)
                ?: return "User entry #${index + 1} has an unsupported media type."
            if (normalizeStatus(entry.status) == null) {
                return "User entry #${index + 1} has an unsupported status."
            }
            if (entry.addedAt < 0L || entry.updatedAt < 0L) {
                return "User entry #${index + 1} has invalid timestamps."
            }

            val key = mediaKey(entry.tmdbId, normalizedMediaType)
            if (key !in mediaKeys) {
                return "User entry #${index + 1} references media not present in backup."
            }
            if (!userEntryKeys.add(key)) {
                return "Backup contains duplicate user entries for TMDB ${entry.tmdbId} (${normalizedMediaType.lowercase()})."
            }
        }

        val reminderKeys = mutableSetOf<String>()
        document.episodeReminderStates.forEachIndexed { index, state ->
            if (state.tmdbId <= 0) {
                return "Reminder state #${index + 1} has an invalid TMDB id."
            }
            val normalizedMediaType = normalizeMediaType(state.mediaType)
                ?: return "Reminder state #${index + 1} has an unsupported media type."
            if (state.lastCheckedAt < 0L) {
                return "Reminder state #${index + 1} has an invalid check timestamp."
            }

            val key = mediaKey(state.tmdbId, normalizedMediaType)
            if (key !in mediaKeys) {
                return "Reminder state #${index + 1} references media not present in backup."
            }
            if (!reminderKeys.add(key)) {
                return "Backup contains duplicate reminder states for TMDB ${state.tmdbId} (${normalizedMediaType.lowercase()})."
            }
        }

        val watchedEpisodeKeys = mutableSetOf<String>()
        document.watchedEpisodes.forEachIndexed { index, watchedEpisode ->
            if (watchedEpisode.tmdbId <= 0) {
                return "Watched episode #${index + 1} has an invalid TMDB id."
            }
            val normalizedMediaType = normalizeMediaType(watchedEpisode.mediaType)
                ?: return "Watched episode #${index + 1} has an unsupported media type."
            if (watchedEpisode.seasonNumber < 0) {
                return "Watched episode #${index + 1} has an invalid season number."
            }
            if (watchedEpisode.episodeNumber <= 0) {
                return "Watched episode #${index + 1} has an invalid episode number."
            }

            val mediaReference = mediaKey(watchedEpisode.tmdbId, normalizedMediaType)
            if (mediaReference !in mediaKeys) {
                return "Watched episode #${index + 1} references media not present in backup."
            }

            val episodeKey = watchedEpisodeKey(
                tmdbId = watchedEpisode.tmdbId,
                mediaType = normalizedMediaType,
                seasonNumber = watchedEpisode.seasonNumber,
                episodeNumber = watchedEpisode.episodeNumber,
            )
            if (!watchedEpisodeKeys.add(episodeKey)) {
                return "Backup contains duplicate watched episodes for TMDB ${watchedEpisode.tmdbId} (${normalizedMediaType.lowercase()}) season ${watchedEpisode.seasonNumber} episode ${watchedEpisode.episodeNumber}."
            }
        }

        return null
    }
}

internal fun normalizeMediaType(value: String): String? {
    return when (value.trim().uppercase()) {
        MediaType.MOVIE.name -> MediaType.MOVIE.name
        MediaType.TV.name -> MediaType.TV.name
        else -> null
    }
}

internal fun normalizeStatus(value: String): String? {
    return runCatching {
        WatchStatus.valueOf(value.trim().uppercase()).name
    }.getOrNull()
}

internal fun mediaKey(tmdbId: Int, mediaType: String): String {
    return "$mediaType:$tmdbId"
}

internal fun watchedEpisodeKey(
    tmdbId: Int,
    mediaType: String,
    seasonNumber: Int,
    episodeNumber: Int,
): String {
    return "$mediaType:$tmdbId:$seasonNumber:$episodeNumber"
}
