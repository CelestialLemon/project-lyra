package com.projectlyra.app.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.projectlyra.app.LyraApplication
import com.projectlyra.app.MainActivity
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.repository.MediaDetailsResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class EpisodeReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    companion object {
        private const val CHANNEL_ID = "episode_reminders"
        private const val CHANNEL_NAME = "Episode reminders"
        private const val NOTIFICATION_ID = 7001
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as? LyraApplication ?: return Result.failure()
        val repository = app.container.libraryRepository
        val settingsStore = app.container.settingsStore

        return try {
            val settings = settingsStore.settings.first()
            if (!settings.reminderEnabled) {
                return Result.success()
            }

            val apiKey = settings.apiKey.trim()
            if (apiKey.isEmpty()) {
                return Result.success()
            }

            val candidates = repository.getTvReminderCandidates()
            if (candidates.isEmpty()) {
                return Result.success()
            }

            val updates = mutableListOf<ShowEpisodeUpdate>()
            val now = System.currentTimeMillis()

            candidates.forEach { candidate ->
                val detailsResult = repository.getMediaDetails(
                    apiKey = apiKey,
                    tmdbId = candidate.tmdbId,
                    mediaType = MediaType.TV,
                )
                val details = when (detailsResult) {
                    is MediaDetailsResult.Success -> detailsResult.details
                    is MediaDetailsResult.Error -> return@forEach
                    is MediaDetailsResult.MissingApiKey -> return Result.success()
                }

                val latestEpisodeCount = details.numberOfEpisodes
                    ?.takeIf { it >= 0 }
                    ?: details.seasons.sumOf { it.episodeCount }.takeIf { it > 0 }
                val latestSeasonCount = details.numberOfSeasons
                    ?.takeIf { it >= 0 }
                    ?: details.seasons.size.takeIf { it > 0 }

                val previousState = repository.getEpisodeReminderState(candidate.localId)
                var effectiveStatus = candidate.status

                if (
                    candidate.status == WatchStatus.COMPLETED &&
                    latestSeasonCount != null &&
                    previousState?.lastKnownSeasonCount != null &&
                    latestSeasonCount > previousState.lastKnownSeasonCount
                ) {
                    repository.updateTrackedStatus(candidate.localId, WatchStatus.ON_HOLD)
                    effectiveStatus = WatchStatus.ON_HOLD
                }

                if (
                    previousState != null &&
                    (effectiveStatus == WatchStatus.WATCHING || effectiveStatus == WatchStatus.ON_HOLD) &&
                    latestEpisodeCount != null &&
                    previousState.lastKnownEpisodeCount != null &&
                    latestEpisodeCount > previousState.lastKnownEpisodeCount
                ) {
                    updates += ShowEpisodeUpdate(
                        title = candidate.title,
                        newEpisodes = latestEpisodeCount - previousState.lastKnownEpisodeCount,
                    )
                }

                repository.upsertEpisodeReminderState(
                    mediaItemId = candidate.localId,
                    lastCheckedAt = now,
                    lastKnownEpisodeCount = latestEpisodeCount ?: previousState?.lastKnownEpisodeCount,
                    lastKnownSeasonCount = latestSeasonCount ?: previousState?.lastKnownSeasonCount,
                )
            }

            if (updates.isNotEmpty()) {
                ensureNotificationChannel()
                postReminderNotification(updates)
            }

            Result.success()
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            Result.success()
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifies when tracked TV episodes are newly available."
        }
        manager.createNotificationChannel(channel)
    }

    private fun postReminderNotification(updates: List<ShowEpisodeUpdate>) {
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return
        }
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = if (updates.size == 1) {
            "${updates.first().title} has new episodes"
        } else {
            "${updates.size} tracked shows have updates"
        }
        val body = if (updates.size == 1) {
            val count = updates.first().newEpisodes
            "$count new ${if (count == 1) "episode" else "episodes"} available now."
        } else {
            val sample = updates.take(3).joinToString(" | ") { "${it.title} (+${it.newEpisodes})" }
            if (updates.size > 3) "$sample | +${updates.size - 3} more" else sample
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
    }

    private data class ShowEpisodeUpdate(
        val title: String,
        val newEpisodes: Int,
    )
}
