package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.SeasonSummary
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.local.CachedTrendingRow
import com.projectlyra.app.data.local.EpisodeReminderStateDao
import com.projectlyra.app.data.local.EpisodeReminderStateEntity
import com.projectlyra.app.data.local.MediaDao
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.TrendingCacheDao
import com.projectlyra.app.data.local.TrendingCacheEntity
import com.projectlyra.app.data.local.UserEntryDao
import com.projectlyra.app.data.local.UserEntryEntity
import com.projectlyra.app.data.remote.TmdbApiService
import com.projectlyra.app.data.remote.TmdbMultiSearchItemDto
import com.projectlyra.app.data.remote.TmdbMovieDetailsDto
import com.projectlyra.app.data.remote.TmdbTvDetailsDto
import com.projectlyra.app.data.remote.TmdbTrendingItemDto
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.map
import retrofit2.HttpException

sealed interface TrendingRefreshResult {
    data class Success(
        val items: List<TrendingItem>,
        val fromCache: Boolean,
    ) : TrendingRefreshResult

    data class Error(
        val cachedItems: List<TrendingItem>,
        val message: String,
    ) : TrendingRefreshResult

    data class MissingApiKey(
        val cachedItems: List<TrendingItem>,
    ) : TrendingRefreshResult
}

sealed interface MediaDetailsResult {
    data class Success(
        val details: MediaDetails,
    ) : MediaDetailsResult

    data class MissingApiKey(
        val localFallback: MediaDetails?,
    ) : MediaDetailsResult

    data class Error(
        val localFallback: MediaDetails?,
        val message: String,
    ) : MediaDetailsResult
}

sealed interface SearchResult {
    data class Success(
        val items: List<TrendingItem>,
    ) : SearchResult

    data object MissingApiKey : SearchResult

    data class Error(
        val message: String,
    ) : SearchResult
}

data class ReminderTrackedShow(
    val localId: Long,
    val tmdbId: Int,
    val title: String,
    val status: WatchStatus,
)

data class EpisodeReminderStateSnapshot(
    val mediaItemId: Long,
    val lastCheckedAt: Long,
    val lastKnownEpisodeCount: Int?,
    val lastKnownSeasonCount: Int?,
)

class LibraryRepository(
    private val mediaDao: MediaDao,
    private val userEntryDao: UserEntryDao,
    private val trendingCacheDao: TrendingCacheDao,
    private val episodeReminderStateDao: EpisodeReminderStateDao,
    private val tmdbApiService: TmdbApiService,
) {
    companion object {
        private const val TRENDING_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        private const val TRENDING_LIMIT = 20
        private const val SEARCH_LIMIT = 30
    }

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

    fun observeTrackedStatusesByMediaKey(): Flow<Map<String, WatchStatus>> {
        return userEntryDao.observeTrackedStatuses().map { rows ->
            rows.mapNotNull { row ->
                val mappedMediaType = runCatching { MediaType.valueOf(row.mediaType) }.getOrNull()
                    ?: return@mapNotNull null
                val mappedStatus = runCatching { WatchStatus.valueOf(row.status) }.getOrNull()
                    ?: return@mapNotNull null
                statusKey(tmdbId = row.tmdbId, mediaType = mappedMediaType) to mappedStatus
            }.toMap()
        }
    }

    suspend fun upsertTrackedStatus(item: TrendingItem, status: WatchStatus) {
        val now = System.currentTimeMillis()
        val mediaItemId = upsertTrendingMedia(item = item, metadataUpdatedAt = now)
        upsertUserStatus(mediaItemId = mediaItemId, status = status, updatedAt = now)
    }

    suspend fun updateTrackedStatus(mediaItemId: Long, status: WatchStatus) {
        upsertUserStatus(
            mediaItemId = mediaItemId,
            status = status,
            updatedAt = System.currentTimeMillis(),
        )
    }

    suspend fun removeTrackedItem(mediaItemId: Long) {
        userEntryDao.deleteByMediaItemId(mediaItemId)
    }

    suspend fun getTvReminderCandidates(): List<ReminderTrackedShow> {
        val eligibleStatuses = listOf(
            WatchStatus.WATCHING.name,
            WatchStatus.ON_HOLD.name,
            WatchStatus.COMPLETED.name,
        )
        return userEntryDao.getTvReminderCandidates(eligibleStatuses)
            .mapNotNull { row ->
                val mappedStatus = runCatching { WatchStatus.valueOf(row.status) }.getOrNull()
                    ?: return@mapNotNull null
                ReminderTrackedShow(
                    localId = row.localId,
                    tmdbId = row.tmdbId,
                    title = row.title,
                    status = mappedStatus,
                )
            }
    }

    suspend fun getEpisodeReminderState(mediaItemId: Long): EpisodeReminderStateSnapshot? {
        return episodeReminderStateDao.findByMediaItemId(mediaItemId)?.let { state ->
            EpisodeReminderStateSnapshot(
                mediaItemId = state.mediaItemId,
                lastCheckedAt = state.lastCheckedAt,
                lastKnownEpisodeCount = state.lastKnownEpisodeCount,
                lastKnownSeasonCount = state.lastKnownSeasonCount,
            )
        }
    }

    suspend fun upsertEpisodeReminderState(
        mediaItemId: Long,
        lastCheckedAt: Long,
        lastKnownEpisodeCount: Int?,
        lastKnownSeasonCount: Int?,
    ) {
        val existing = episodeReminderStateDao.findByMediaItemId(mediaItemId)
        episodeReminderStateDao.upsert(
            EpisodeReminderStateEntity(
                id = existing?.id ?: 0,
                mediaItemId = mediaItemId,
                lastCheckedAt = lastCheckedAt,
                lastKnownEpisodeCount = lastKnownEpisodeCount,
                lastKnownSeasonCount = lastKnownSeasonCount,
            )
        )
    }

    suspend fun getCachedTrending(): List<TrendingItem> {
        return trendingCacheDao.getCachedTrending().mapNotNull { row -> row.toDomainOrNull() }
    }

    suspend fun refreshTrending(apiKey: String, forceRefresh: Boolean = false): TrendingRefreshResult {
        val cachedTrending = getCachedTrending()
        val normalizedApiKey = apiKey.trim()

        if (normalizedApiKey.isEmpty()) {
            return TrendingRefreshResult.MissingApiKey(cachedTrending)
        }

        if (!forceRefresh && isCacheFresh(cachedTrending)) {
            return TrendingRefreshResult.Success(
                items = cachedTrending,
                fromCache = true,
            )
        }

        return try {
            val remoteTrending = tmdbApiService.getTrendingAllDay(normalizedApiKey)
                .results
                .mapNotNull { dto -> dto.toDomainOrNull() }
                .distinctBy { item -> "${item.mediaType.name}:${item.tmdbId}" }
                .take(TRENDING_LIMIT)

            if (remoteTrending.isEmpty()) {
                if (cachedTrending.isNotEmpty()) {
                    TrendingRefreshResult.Success(
                        items = cachedTrending,
                        fromCache = true,
                    )
                } else {
                    TrendingRefreshResult.Error(
                        cachedItems = emptyList(),
                        message = "No trending titles are available right now.",
                    )
                }
            } else {
                val now = System.currentTimeMillis()
                cacheTrending(items = remoteTrending, cachedAt = now)
                TrendingRefreshResult.Success(
                    items = remoteTrending,
                    fromCache = false,
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            TrendingRefreshResult.Error(
                cachedItems = cachedTrending,
                message = error.toUserFacingMessage(),
            )
        }
    }

    suspend fun searchTitles(apiKey: String, query: String): SearchResult {
        val normalizedApiKey = apiKey.trim()
        val normalizedQuery = query.trim()

        if (normalizedQuery.isEmpty()) {
            return SearchResult.Success(items = emptyList())
        }

        if (normalizedApiKey.isEmpty()) {
            return SearchResult.MissingApiKey
        }

        return try {
            val results = tmdbApiService.searchMulti(
                apiKey = normalizedApiKey,
                query = normalizedQuery,
            ).results
                .mapNotNull { dto -> dto.toDomainOrNull() }
                .distinctBy { item -> "${item.mediaType.name}:${item.tmdbId}" }
                .take(SEARCH_LIMIT)

            SearchResult.Success(items = results)
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            SearchResult.Error(message = error.toUserFacingMessage())
        }
    }

    suspend fun getMediaDetails(
        apiKey: String,
        tmdbId: Int,
        mediaType: MediaType,
    ): MediaDetailsResult {
        val normalizedApiKey = apiKey.trim()
        val localFallback = mediaDao.findByTmdbAndType(tmdbId = tmdbId, mediaType = mediaType.name)
            ?.toDetailsFallback(mediaType = mediaType)

        if (normalizedApiKey.isEmpty()) {
            return MediaDetailsResult.MissingApiKey(localFallback)
        }

        return try {
            val remoteDetails = when (mediaType) {
                MediaType.MOVIE -> tmdbApiService.getMovieDetails(movieId = tmdbId, apiKey = normalizedApiKey).toDomainOrNull()
                MediaType.TV -> tmdbApiService.getTvDetails(tvId = tmdbId, apiKey = normalizedApiKey).toDomainOrNull()
            }

            if (remoteDetails == null) {
                MediaDetailsResult.Error(
                    localFallback = localFallback,
                    message = "Unable to load details for this title right now.",
                )
            } else {
                val now = System.currentTimeMillis()
                upsertTrendingMedia(item = remoteDetails.asTrendingItem(), metadataUpdatedAt = now)
                MediaDetailsResult.Success(details = remoteDetails)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            MediaDetailsResult.Error(
                localFallback = localFallback,
                message = error.toUserFacingMessage(),
            )
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

    private suspend fun isCacheFresh(cachedTrending: List<TrendingItem>): Boolean {
        if (cachedTrending.isEmpty()) {
            return false
        }

        val latestCachedAt = trendingCacheDao.latestCachedAt() ?: return false
        return System.currentTimeMillis() - latestCachedAt <= TRENDING_CACHE_TTL_MS
    }

    private suspend fun cacheTrending(items: List<TrendingItem>, cachedAt: Long) {
        trendingCacheDao.clearAll()

        val cacheEntries = items.mapIndexed { index, item ->
            TrendingCacheEntity(
                mediaItemId = upsertTrendingMedia(item = item, metadataUpdatedAt = cachedAt),
                position = index,
                cachedAt = cachedAt,
            )
        }

        trendingCacheDao.upsertAll(cacheEntries)
    }

    private suspend fun upsertTrendingMedia(item: TrendingItem, metadataUpdatedAt: Long): Long {
        val existing = mediaDao.findByTmdbAndType(item.tmdbId, item.mediaType.name)
        val existingId = existing?.id ?: 0L

        val upsertedId = mediaDao.upsertMediaItem(
            MediaItemEntity(
                id = existingId,
                tmdbId = item.tmdbId,
                mediaType = item.mediaType.name,
                title = item.title,
                overview = item.overview,
                posterPath = item.posterPath,
                releaseOrAirDate = item.releaseOrAirDate,
                metadataUpdatedAt = metadataUpdatedAt,
            )
        )

        return if (existingId != 0L) existingId else upsertedId
    }

    private fun CachedTrendingRow.toDomainOrNull(): TrendingItem? {
        val mappedType = runCatching { MediaType.valueOf(mediaType) }.getOrNull() ?: return null
        return TrendingItem(
            tmdbId = tmdbId,
            mediaType = mappedType,
            title = title,
            overview = overview,
            posterPath = posterPath,
            releaseOrAirDate = releaseOrAirDate,
        )
    }

    private fun TmdbTrendingItemDto.toDomainOrNull(): TrendingItem? {
        val mappedMediaType = when (mediaType?.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> return null
        }

        val mappedTitle = when (mappedMediaType) {
            MediaType.MOVIE -> title
            MediaType.TV -> name
        }.orEmpty().trim()

        val mappedPosterPath = posterPath.orEmpty().trim()

        if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
            return null
        }

        val mappedDate = when (mappedMediaType) {
            MediaType.MOVIE -> releaseDate
            MediaType.TV -> firstAirDate
        }.orEmpty()

        return TrendingItem(
            tmdbId = id,
            mediaType = mappedMediaType,
            title = mappedTitle,
            overview = overview.orEmpty(),
            posterPath = mappedPosterPath,
            releaseOrAirDate = mappedDate,
        )
    }

    private fun TmdbMultiSearchItemDto.toDomainOrNull(): TrendingItem? {
        val mappedMediaType = when (mediaType?.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> return null
        }

        val mappedTitle = when (mappedMediaType) {
            MediaType.MOVIE -> title
            MediaType.TV -> name
        }.orEmpty().trim()
        val mappedPosterPath = posterPath.orEmpty().trim()

        if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
            return null
        }

        val mappedDate = when (mappedMediaType) {
            MediaType.MOVIE -> releaseDate
            MediaType.TV -> firstAirDate
        }.orEmpty()

        return TrendingItem(
            tmdbId = id,
            mediaType = mappedMediaType,
            title = mappedTitle,
            overview = overview.orEmpty(),
            posterPath = mappedPosterPath,
            releaseOrAirDate = mappedDate,
        )
    }

    private fun TmdbMovieDetailsDto.toDomainOrNull(): MediaDetails? {
        val mappedTitle = title.orEmpty().trim()
        val mappedPosterPath = posterPath.orEmpty().trim()
        if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
            return null
        }

        return MediaDetails(
            tmdbId = id,
            mediaType = MediaType.MOVIE,
            title = mappedTitle,
            overview = overview.orEmpty(),
            posterPath = mappedPosterPath,
            backdropPath = backdropPath?.trim()?.takeIf { it.isNotEmpty() },
            releaseOrAirDate = releaseDate.orEmpty(),
            genres = genres.orEmpty().mapNotNull { dto -> dto.name?.trim()?.takeIf { it.isNotEmpty() } },
            runtimeMinutes = runtime?.takeIf { it > 0 },
        )
    }

    private fun TmdbTvDetailsDto.toDomainOrNull(): MediaDetails? {
        val mappedTitle = name.orEmpty().trim()
        val mappedPosterPath = posterPath.orEmpty().trim()
        if (mappedTitle.isEmpty() || mappedPosterPath.isEmpty()) {
            return null
        }

        val mappedSeasons = seasons.orEmpty()
            .mapNotNull { season ->
                val seasonNumber = season.seasonNumber ?: return@mapNotNull null
                val episodeCount = season.episodeCount ?: 0
                SeasonSummary(
                    seasonNumber = seasonNumber,
                    name = season.name.orEmpty().ifBlank { "Season $seasonNumber" },
                    episodeCount = episodeCount,
                    airDate = season.airDate?.trim()?.takeIf { it.isNotEmpty() },
                    posterPath = season.posterPath?.trim()?.takeIf { it.isNotEmpty() },
                )
            }
            .sortedBy { it.seasonNumber }

        return MediaDetails(
            tmdbId = id,
            mediaType = MediaType.TV,
            title = mappedTitle,
            overview = overview.orEmpty(),
            posterPath = mappedPosterPath,
            backdropPath = backdropPath?.trim()?.takeIf { it.isNotEmpty() },
            releaseOrAirDate = firstAirDate.orEmpty(),
            genres = genres.orEmpty().mapNotNull { dto -> dto.name?.trim()?.takeIf { it.isNotEmpty() } },
            numberOfSeasons = numberOfSeasons?.takeIf { it >= 0 } ?: mappedSeasons.size,
            numberOfEpisodes = numberOfEpisodes?.takeIf { it >= 0 },
            seasons = mappedSeasons,
        )
    }

    private fun MediaItemEntity.toDetailsFallback(mediaType: MediaType): MediaDetails {
        return MediaDetails(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            overview = overview,
            posterPath = posterPath,
            backdropPath = null,
            releaseOrAirDate = releaseOrAirDate,
        )
    }

    private fun MediaDetails.asTrendingItem(): TrendingItem {
        return TrendingItem(
            tmdbId = tmdbId,
            mediaType = mediaType,
            title = title,
            overview = overview,
            posterPath = posterPath,
            releaseOrAirDate = releaseOrAirDate,
        )
    }

    private fun Throwable.toUserFacingMessage(): String {
        return when (this) {
            is HttpException -> {
                when (code()) {
                    401, 403 -> "TMDB API key is invalid. Update it in Settings and retry."
                    429 -> "TMDB rate limit reached. Please retry in a moment."
                    else -> "TMDB request failed (${code()}). Please retry."
                }
            }

            is SocketTimeoutException -> "TMDB is responding slowly (request timed out). Please retry."
            is UnknownHostException -> "Cannot reach TMDB right now. Check DNS/VPN/network and retry."
            is IOException -> "Network path to TMDB is unavailable right now. Please retry."
            else -> "Unable to load TMDB data right now."
        }
    }

    private suspend fun upsertUserStatus(
        mediaItemId: Long,
        status: WatchStatus,
        updatedAt: Long,
    ) {
        val existingEntry = userEntryDao.findByMediaItemId(mediaItemId)
        if (existingEntry?.status == status.name) {
            return
        }

        userEntryDao.upsertUserEntry(
            UserEntryEntity(
                id = existingEntry?.id ?: 0,
                mediaItemId = mediaItemId,
                status = status.name,
                addedAt = existingEntry?.addedAt ?: updatedAt,
                updatedAt = updatedAt,
            )
        )
    }

    private fun statusKey(tmdbId: Int, mediaType: MediaType): String {
        return "${mediaType.name}:$tmdbId"
    }
}
