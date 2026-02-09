package com.projectlyra.app.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class EpisodeReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        // Worker scaffold. The TMDB episode check + notification dispatch
        // will be implemented after remote integration is wired.
        return Result.success()
    }
}
