package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.local.MediaDao
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.UserEntryDao
import com.projectlyra.app.data.local.UserEntryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LibraryRepository(
    private val mediaDao: MediaDao,
    private val userEntryDao: UserEntryDao,
) {
    fun observeByStatus(status: WatchStatus): Flow<List<TrackedItem>> {
        return userEntryDao.observeItemsByStatus(status.name).map { rows ->
            rows.map { row ->
                TrackedItem(
                    localId = row.localId,
                    tmdbId = row.tmdbId,
                    mediaType = MediaType.valueOf(row.mediaType),
                    title = row.title,
                    overview = row.overview,
                    posterPath = row.posterPath,
                    releaseOrAirDate = row.releaseOrAirDate,
                    status = WatchStatus.valueOf(row.status),
                    updatedAt = row.updatedAt,
                )
            }
        }
    }

    suspend fun ensureSeedData() {
        if (mediaDao.mediaCount() > 0) {
            return
        }

        val now = System.currentTimeMillis()
        seedTrending().take(5).forEachIndexed { index, item ->
            val mediaId = mediaDao.upsertMediaItem(
                MediaItemEntity(
                    tmdbId = item.tmdbId,
                    mediaType = item.mediaType.name,
                    title = item.title,
                    overview = item.overview,
                    posterPath = item.posterPath,
                    releaseOrAirDate = item.releaseOrAirDate,
                    metadataUpdatedAt = now,
                )
            )

            val status = when (index) {
                0 -> WatchStatus.WATCHING
                1 -> WatchStatus.ON_HOLD
                2 -> WatchStatus.WANT_TO_WATCH
                3 -> WatchStatus.COMPLETED
                else -> WatchStatus.DROPPED
            }

            userEntryDao.upsertUserEntry(
                UserEntryEntity(
                    mediaItemId = mediaId,
                    status = status.name,
                    addedAt = now,
                    updatedAt = now,
                )
            )
        }
    }

    fun seedTrending(): List<TrendingItem> {
        return listOf(
            TrendingItem(
                tmdbId = 1399,
                mediaType = MediaType.TV,
                title = "House of the Dragon",
                overview = "A dragon dynasty fractures under ambition, prophecy, and civil war.",
                posterPath = "/z2yahl2uefxDCl0nogcRBstwruJ.jpg",
                releaseOrAirDate = "2022-08-21",
            ),
            TrendingItem(
                tmdbId = 157336,
                mediaType = MediaType.MOVIE,
                title = "Interstellar",
                overview = "A team travels through a wormhole to preserve humanity's future.",
                posterPath = "/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
                releaseOrAirDate = "2014-11-05",
            ),
            TrendingItem(
                tmdbId = 94997,
                mediaType = MediaType.TV,
                title = "House of Ninjas",
                overview = "A hidden ninja family is pulled back into covert conflict.",
                posterPath = "/f7F6Z6R2B7R9Uv1zArYrusS4x2M.jpg",
                releaseOrAirDate = "2024-02-15",
            ),
            TrendingItem(
                tmdbId = 823464,
                mediaType = MediaType.MOVIE,
                title = "Godzilla Minus One",
                overview = "Post-war Japan faces a new crisis when Godzilla emerges.",
                posterPath = "/hkxxMIGaiCTmrEArK7J56JTKUlB.jpg",
                releaseOrAirDate = "2023-11-03",
            ),
            TrendingItem(
                tmdbId = 66732,
                mediaType = MediaType.TV,
                title = "Stranger Things",
                overview = "A small town uncovers a dark dimension beyond reality.",
                posterPath = "/49WJfeN0moxb9IPfGn8AIqMGskD.jpg",
                releaseOrAirDate = "2016-07-15",
            ),
            TrendingItem(
                tmdbId = 603,
                mediaType = MediaType.MOVIE,
                title = "The Matrix",
                overview = "A hacker discovers the world is a simulation and fights back.",
                posterPath = "/f89U3ADr1oiB1s9GkdPOEpXUk5H.jpg",
                releaseOrAirDate = "1999-03-31",
            ),
        )
    }
}
