package com.projectlyra.app.di

import android.content.Context
import com.projectlyra.app.data.local.DatabaseFactory
import com.projectlyra.app.data.repository.LibraryRepository
import com.projectlyra.app.data.settings.SettingsStore

class AppContainer(context: Context) {
    private val database = DatabaseFactory.create(context)

    val libraryRepository = LibraryRepository(
        mediaDao = database.mediaDao(),
        userEntryDao = database.userEntryDao(),
    )

    val settingsStore = SettingsStore(context)
}
