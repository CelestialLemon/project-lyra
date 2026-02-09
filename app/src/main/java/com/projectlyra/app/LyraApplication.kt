package com.projectlyra.app

import android.app.Application
import com.projectlyra.app.di.AppContainer
import com.projectlyra.app.workers.ReminderScheduler

class LyraApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        ReminderScheduler.scheduleDaily(this)
    }
}
