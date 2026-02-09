package com.projectlyra.app.di

import android.content.Context
import com.projectlyra.app.data.local.DatabaseFactory
import com.projectlyra.app.data.remote.TmdbClientFactory
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.settings.SettingsStore

class AppContainer(context: Context) {
    private val database = DatabaseFactory.create(context)
    private val tmdbApiService = TmdbClientFactory.create()

    val libraryRepository = LibraryRepository(
        mediaDao = database.mediaDao(),
        userEntryDao = database.userEntryDao(),
        trendingCacheDao = database.trendingCacheDao(),
        episodeReminderStateDao = database.episodeReminderStateDao(),
        tmdbApiService = tmdbApiService,
    )

    val settingsStore = SettingsStore(context)
}
