package com.projectlyra.app.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    private const val REMINDER_WORK_NAME = "episode-reminder-work"
    private const val DEFAULT_REMINDER_HOUR = 20
    private const val DEFAULT_REMINDER_MINUTE = 0
    private const val SCHEDULER_PREFS = "reminder_scheduler_prefs"
    private const val KEY_LAST_HOUR = "last_hour"
    private const val KEY_LAST_MINUTE = "last_minute"

    fun syncDaily(
        context: Context,
        enabled: Boolean,
        hour: Int = DEFAULT_REMINDER_HOUR,
        minute: Int = DEFAULT_REMINDER_MINUTE,
    ) {
        val workManager = WorkManager.getInstance(context)
        val prefs = context.getSharedPreferences(SCHEDULER_PREFS, Context.MODE_PRIVATE)

        if (!enabled) {
            workManager.cancelUniqueWork(REMINDER_WORK_NAME)
            prefs.edit()
                .remove(KEY_LAST_HOUR)
                .remove(KEY_LAST_MINUTE)
                .apply()
            return
        }

        val normalizedHour = hour.coerceIn(0, 23)
        val normalizedMinute = minute.coerceIn(0, 59)
        val previousHour = prefs.takeIf { it.contains(KEY_LAST_HOUR) }?.getInt(KEY_LAST_HOUR, normalizedHour)
        val previousMinute = prefs.takeIf { it.contains(KEY_LAST_MINUTE) }?.getInt(KEY_LAST_MINUTE, normalizedMinute)
        val hasScheduleChanged = previousHour != normalizedHour || previousMinute != normalizedMinute

        val initialDelay = computeInitialDelayMillis(hour = normalizedHour, minute = normalizedMinute)
        val request = PeriodicWorkRequestBuilder<EpisodeReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            REMINDER_WORK_NAME,
            if (hasScheduleChanged) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.UPDATE
            },
            request,
        )

        prefs.edit()
            .putInt(KEY_LAST_HOUR, normalizedHour)
            .putInt(KEY_LAST_MINUTE, normalizedMinute)
            .apply()
    }

    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var nextRun = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }
        return Duration.between(now, nextRun).toMillis().coerceAtLeast(0L)
    }
}
