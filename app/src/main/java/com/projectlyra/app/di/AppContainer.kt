package com.projectlyra.app.di

import android.content.Context
import com.projectlyra.app.data.backup.BackupService
import com.projectlyra.app.data.local.DatabaseFactory
import com.projectlyra.app.data.remote.TmdbClientFactory
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.settings.SettingsStore

class AppContainer(context: Context) {
    private val database = DatabaseFactory.create(context)
    private val tmdbApiService = TmdbClientFactory.create()
    val settingsStore = SettingsStore(context)

    val libraryRepository = LibraryRepository(
        mediaDao = database.mediaDao(),
        userEntryDao = database.userEntryDao(),
        trendingCacheDao = database.trendingCacheDao(),
        episodeReminderStateDao = database.episodeReminderStateDao(),
        watchedEpisodeDao = database.watchedEpisodeDao(),
        genreMetadataDao = database.genreMetadataDao(),
        tmdbApiService = tmdbApiService,
    )

    val backupService = BackupService(
        context = context,
        database = database,
        settingsStore = settingsStore,
    )
}
