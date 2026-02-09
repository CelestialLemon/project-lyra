package com.projectlyra.app.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.local.LyraDatabase
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.UserEntryEntity
import com.projectlyra.app.data.local.EpisodeReminderStateEntity
import com.projectlyra.app.data.settings.SettingsStore
import com.projectlyra.app.data.settings.TmdbApiKeyValidator
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

sealed interface BackupOperationResult {
    data class Success(val message: String) : BackupOperationResult
    data class Error(val message: String) : BackupOperationResult
}

class BackupService(
    private val context: Context,
    private val database: LyraDatabase,
    private val settingsStore: SettingsStore,
    private val json: Json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
        explicitNulls = false
    },
) {
    suspend fun exportToUri(uri: Uri): BackupOperationResult = withContext(Dispatchers.IO) {
        val mediaDao = database.mediaDao()
        val userEntryDao = database.userEntryDao()
        val episodeReminderStateDao = database.episodeReminderStateDao()

        val settings = settingsStore.settings.first()
        val mediaItems = mediaDao.getAllMediaItems()
        val userEntries = userEntryDao.getAllUserEntries()
        val reminderStates = episodeReminderStateDao.getAll()

        val mediaById = mediaItems.associateBy { it.id }
        val referencedMediaIds = buildSet {
            userEntries.forEach { entry -> add(entry.mediaItemId) }
            reminderStates.forEach { state -> add(state.mediaItemId) }
        }

        val backupMediaItems = mediaItems
            .asSequence()
            .filter { media -> media.id in referencedMediaIds }
            .sortedBy { media -> media.id }
            .map { media ->
                LyraBackupMediaItem(
                    tmdbId = media.tmdbId,
                    mediaType = media.mediaType,
                    title = media.title,
                    overview = media.overview,
                    posterPath = media.posterPath,
                    releaseOrAirDate = media.releaseOrAirDate,
                    metadataUpdatedAt = media.metadataUpdatedAt,
                )
            }.toList()

        val backupUserEntries = userEntries
            .asSequence()
            .mapNotNull { entry ->
                val media = mediaById[entry.mediaItemId] ?: return@mapNotNull null
                LyraBackupUserEntry(
                    tmdbId = media.tmdbId,
                    mediaType = media.mediaType,
                    status = entry.status,
                    addedAt = entry.addedAt,
                    updatedAt = entry.updatedAt,
                )
            }
            .sortedWith(compareBy({ it.mediaType }, { it.tmdbId }))
            .toList()

        val backupReminderStates = reminderStates
            .asSequence()
            .mapNotNull { state ->
                val media = mediaById[state.mediaItemId] ?: return@mapNotNull null
                LyraBackupEpisodeReminderState(
                    tmdbId = media.tmdbId,
                    mediaType = media.mediaType,
                    lastCheckedAt = state.lastCheckedAt,
                    lastKnownEpisodeCount = state.lastKnownEpisodeCount,
                    lastKnownSeasonCount = state.lastKnownSeasonCount,
                )
            }
            .sortedWith(compareBy({ it.mediaType }, { it.tmdbId }))
            .toList()

        val backupDocument = LyraBackupDocument(
            schemaVersion = LYRA_BACKUP_SCHEMA_VERSION,
            exportedAtEpochMs = System.currentTimeMillis(),
            settings = LyraBackupSettings(
                reminderEnabled = settings.reminderEnabled,
                reminderHour = settings.reminderHour,
                reminderMinute = settings.reminderMinute,
                includeApiKeyInBackup = settings.includeApiKeyInBackup,
                apiKey = if (settings.includeApiKeyInBackup) settings.apiKey else null,
            ),
            mediaItems = backupMediaItems,
            userEntries = backupUserEntries,
            episodeReminderStates = backupReminderStates,
        )

        val payload = runCatching { json.encodeToString(LyraBackupDocument.serializer(), backupDocument) }
            .getOrElse {
                return@withContext BackupOperationResult.Error("Unable to build backup payload.")
            }

        val wasWritten = writeText(uri = uri, text = payload)
        if (!wasWritten) {
            return@withContext BackupOperationResult.Error("Unable to write backup file to the selected location.")
        }

        BackupOperationResult.Success(
            message = "Backup exported (${backupUserEntries.size} tracked items).",
        )
    }

    suspend fun importFromUri(uri: Uri): BackupOperationResult = withContext(Dispatchers.IO) {
        val payload = readText(uri)
            ?: return@withContext BackupOperationResult.Error("Unable to read the selected backup file.")

        val backupDocument = try {
            json.decodeFromString(LyraBackupDocument.serializer(), payload)
        } catch (_: SerializationException) {
            return@withContext BackupOperationResult.Error("Backup JSON is invalid or not supported.")
        } catch (_: IllegalArgumentException) {
            return@withContext BackupOperationResult.Error("Backup JSON contains malformed values.")
        }

        val validationError = validateBackupDocument(backupDocument)
        if (validationError != null) {
            return@withContext BackupOperationResult.Error(validationError)
        }

        val normalizedApiKey = backupDocument.settings.apiKey?.trim()
        if (normalizedApiKey != null && normalizedApiKey.isNotEmpty()) {
            val apiKeyError = TmdbApiKeyValidator.validate(normalizedApiKey)
            if (apiKeyError != null) {
                return@withContext BackupOperationResult.Error("Backup API key is invalid: $apiKeyError")
            }
        }

        val mediaDao = database.mediaDao()
        val userEntryDao = database.userEntryDao()
        val episodeReminderStateDao = database.episodeReminderStateDao()
        val trendingCacheDao = database.trendingCacheDao()

        database.withTransaction {
            trendingCacheDao.clearAll()
            episodeReminderStateDao.clearAll()
            userEntryDao.clearAll()
            mediaDao.clearAll()

            val mediaIdByKey = mutableMapOf<String, Long>()
            backupDocument.mediaItems.forEach { media ->
                val normalizedMediaType = normalizeMediaType(media.mediaType) ?: return@forEach
                val insertedId = mediaDao.upsertMediaItem(
                    MediaItemEntity(
                        tmdbId = media.tmdbId,
                        mediaType = normalizedMediaType,
                        title = media.title,
                        overview = media.overview,
                        posterPath = media.posterPath,
                        releaseOrAirDate = media.releaseOrAirDate,
                        metadataUpdatedAt = media.metadataUpdatedAt,
                    )
                )
                mediaIdByKey[mediaKey(media.tmdbId, normalizedMediaType)] = insertedId
            }

            backupDocument.userEntries.forEach { entry ->
                val normalizedMediaType = normalizeMediaType(entry.mediaType) ?: return@forEach
                val normalizedStatus = normalizeStatus(entry.status) ?: return@forEach
                val mediaItemId = mediaIdByKey[mediaKey(entry.tmdbId, normalizedMediaType)] ?: return@forEach
                userEntryDao.upsertUserEntry(
                    UserEntryEntity(
                        mediaItemId = mediaItemId,
                        status = normalizedStatus,
                        addedAt = entry.addedAt,
                        updatedAt = entry.updatedAt,
                    )
                )
            }

            backupDocument.episodeReminderStates.forEach { state ->
                val normalizedMediaType = normalizeMediaType(state.mediaType) ?: return@forEach
                val mediaItemId = mediaIdByKey[mediaKey(state.tmdbId, normalizedMediaType)] ?: return@forEach
                episodeReminderStateDao.upsert(
                    EpisodeReminderStateEntity(
                        mediaItemId = mediaItemId,
                        lastCheckedAt = state.lastCheckedAt,
                        lastKnownEpisodeCount = state.lastKnownEpisodeCount,
                        lastKnownSeasonCount = state.lastKnownSeasonCount,
                    )
                )
            }
        }

        settingsStore.updateReminder(
            enabled = backupDocument.settings.reminderEnabled,
            hour = backupDocument.settings.reminderHour,
            minute = backupDocument.settings.reminderMinute,
        )
        settingsStore.updateIncludeApiKeyInBackup(backupDocument.settings.includeApiKeyInBackup)

        if (backupDocument.settings.apiKey != null) {
            val wasSaved = settingsStore.updateApiKey(backupDocument.settings.apiKey)
            if (!wasSaved) {
                return@withContext BackupOperationResult.Error(
                    "Data was restored, but API key could not be encrypted on this device.",
                )
            }
        }

        BackupOperationResult.Success(
            message = "Backup imported (${backupDocument.userEntries.size} tracked items restored).",
        )
    }

    private fun validateBackupDocument(document: LyraBackupDocument): String? {
        if (document.schemaVersion != LYRA_BACKUP_SCHEMA_VERSION) {
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

            val mediaKey = mediaKey(entry.tmdbId, normalizedMediaType)
            if (mediaKey !in mediaKeys) {
                return "User entry #${index + 1} references media not present in backup."
            }
            if (!userEntryKeys.add(mediaKey)) {
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

            val mediaKey = mediaKey(state.tmdbId, normalizedMediaType)
            if (mediaKey !in mediaKeys) {
                return "Reminder state #${index + 1} references media not present in backup."
            }
            if (!reminderKeys.add(mediaKey)) {
                return "Backup contains duplicate reminder states for TMDB ${state.tmdbId} (${normalizedMediaType.lowercase()})."
            }
        }

        return null
    }

    private fun normalizeMediaType(value: String): String? {
        return when (value.trim().uppercase()) {
            MediaType.MOVIE.name -> MediaType.MOVIE.name
            MediaType.TV.name -> MediaType.TV.name
            else -> null
        }
    }

    private fun normalizeStatus(value: String): String? {
        return runCatching {
            WatchStatus.valueOf(value.trim().uppercase()).name
        }.getOrNull()
    }

    private fun mediaKey(tmdbId: Int, mediaType: String): String {
        return "$mediaType:$tmdbId"
    }

    private fun readText(uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.reader(Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            }
        }.getOrNull()
    }

    private fun writeText(uri: Uri, text: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.writer(Charsets.UTF_8).use { writer ->
                    writer.write(text)
                }
            } != null
        }.recoverCatching { error ->
            if (error is IOException) {
                false
            } else {
                throw error
            }
        }.getOrDefault(false)
    }
}
