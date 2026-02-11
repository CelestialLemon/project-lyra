package com.projectlyra.app.data.backup

import com.projectlyra.app.data.local.EpisodeReminderStateEntity
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.UserEntryEntity
import com.projectlyra.app.data.local.WatchedEpisodeEntity

internal data class RestorableUserEntry(
    val tmdbId: Int,
    val mediaType: String,
    val status: String,
    val addedAt: Long,
    val updatedAt: Long,
)

internal data class RestorableEpisodeReminderState(
    val tmdbId: Int,
    val mediaType: String,
    val lastCheckedAt: Long,
    val lastKnownEpisodeCount: Int?,
    val lastKnownSeasonCount: Int?,
)

internal data class BackupRestorePlan(
    val mediaItems: List<MediaItemEntity>,
    val userEntries: List<RestorableUserEntry>,
    val episodeReminderStates: List<RestorableEpisodeReminderState>,
    val watchedEpisodes: List<RestorableWatchedEpisode>,
)

internal data class RestorableWatchedEpisode(
    val tmdbId: Int,
    val mediaType: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
)

internal object BackupRestorePlanFactory {
    fun create(document: LyraBackupDocument): BackupRestorePlan {
        val mediaItems = document.mediaItems.map { media ->
            val normalizedMediaType = normalizeMediaType(media.mediaType)
                ?: throw IllegalArgumentException("Unsupported media type ${media.mediaType}")
            MediaItemEntity(
                tmdbId = media.tmdbId,
                mediaType = normalizedMediaType,
                title = media.title,
                overview = media.overview,
                posterPath = media.posterPath,
                releaseOrAirDate = media.releaseOrAirDate,
                genreIdsCsv = media.genreIdsCsv,
                metadataUpdatedAt = media.metadataUpdatedAt,
            )
        }

        val userEntries = document.userEntries.map { entry ->
            val normalizedMediaType = normalizeMediaType(entry.mediaType)
                ?: throw IllegalArgumentException("Unsupported media type ${entry.mediaType}")
            val normalizedStatus = normalizeStatus(entry.status)
                ?: throw IllegalArgumentException("Unsupported status ${entry.status}")
            RestorableUserEntry(
                tmdbId = entry.tmdbId,
                mediaType = normalizedMediaType,
                status = normalizedStatus,
                addedAt = entry.addedAt,
                updatedAt = entry.updatedAt,
            )
        }

        val reminderStates = document.episodeReminderStates.map { state ->
            val normalizedMediaType = normalizeMediaType(state.mediaType)
                ?: throw IllegalArgumentException("Unsupported media type ${state.mediaType}")
            RestorableEpisodeReminderState(
                tmdbId = state.tmdbId,
                mediaType = normalizedMediaType,
                lastCheckedAt = state.lastCheckedAt,
                lastKnownEpisodeCount = state.lastKnownEpisodeCount,
                lastKnownSeasonCount = state.lastKnownSeasonCount,
            )
        }

        val watchedEpisodes = document.watchedEpisodes.map { watchedEpisode ->
            val normalizedMediaType = normalizeMediaType(watchedEpisode.mediaType)
                ?: throw IllegalArgumentException("Unsupported media type ${watchedEpisode.mediaType}")
            RestorableWatchedEpisode(
                tmdbId = watchedEpisode.tmdbId,
                mediaType = normalizedMediaType,
                seasonNumber = watchedEpisode.seasonNumber,
                episodeNumber = watchedEpisode.episodeNumber,
            )
        }

        return BackupRestorePlan(
            mediaItems = mediaItems,
            userEntries = userEntries,
            episodeReminderStates = reminderStates,
            watchedEpisodes = watchedEpisodes,
        )
    }
}

internal fun RestorableEpisodeReminderState.toEntity(mediaItemId: Long): EpisodeReminderStateEntity {
    return EpisodeReminderStateEntity(
        mediaItemId = mediaItemId,
        lastCheckedAt = lastCheckedAt,
        lastKnownEpisodeCount = lastKnownEpisodeCount,
        lastKnownSeasonCount = lastKnownSeasonCount,
    )
}

internal fun RestorableUserEntry.toEntity(mediaItemId: Long): UserEntryEntity {
    return UserEntryEntity(
        mediaItemId = mediaItemId,
        status = status,
        addedAt = addedAt,
        updatedAt = updatedAt,
    )
}

internal fun RestorableWatchedEpisode.toEntity(mediaItemId: Long): WatchedEpisodeEntity {
    return WatchedEpisodeEntity(
        mediaItemId = mediaItemId,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
    )
}
