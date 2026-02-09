package com.projectlyra.app

import android.app.Application
import com.projectlyra.app.di.AppContainer
import com.projectlyra.app.workers.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class LyraApplication : Application() {
    lateinit var container: AppContainer
        private set
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        appScope.launch {
            container.settingsStore.settings
                .map { settings ->
                    ReminderScheduleConfig(
                        enabled = settings.reminderEnabled,
                        hour = settings.reminderHour,
                        minute = settings.reminderMinute,
                    )
                }
                .distinctUntilChanged()
                .collect { config ->
                    ReminderScheduler.syncDaily(
                        context = this@LyraApplication,
                        enabled = config.enabled,
                        hour = config.hour,
                        minute = config.minute,
                    )
                }
        }
    }
}

private data class ReminderScheduleConfig(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
)
