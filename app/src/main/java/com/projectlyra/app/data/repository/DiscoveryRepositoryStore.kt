package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.data.local.MediaDao
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.TrendingCacheDao
import com.projectlyra.app.data.local.TrendingCacheEntity
import com.projectlyra.app.data.remote.TmdbApiService
import kotlinx.coroutines.CancellationException

internal class DiscoveryRepositoryStore(
    private val mediaDao: MediaDao,
    private val trendingCacheDao: TrendingCacheDao,
    private val tmdbApiService: TmdbApiService,
    private val nowProvider: () -> Long,
) {
    companion object {
        private const val TRENDING_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        private const val TRENDING_LIMIT = 20
        private const val SEARCH_LIMIT = 30
    }

    suspend fun getCachedTrending(): List<TrendingItem> {
        return trendingCacheDao.getCachedTrending().mapNotNull { row -> row.toDomainOrNull() }
    }

    suspend fun refreshTrending(apiKey: String, forceRefresh: Boolean): TrendingRefreshResult {
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
                .distinctBy { item -> statusKey(tmdbId = item.tmdbId, mediaType = item.mediaType) }
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
                val now = nowProvider()
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
                .distinctBy { item -> statusKey(tmdbId = item.tmdbId, mediaType = item.mediaType) }
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
                val now = nowProvider()
                upsertMediaMetadata(item = remoteDetails.asTrendingItem(), metadataUpdatedAt = now)
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

    private suspend fun isCacheFresh(cachedTrending: List<TrendingItem>): Boolean {
        if (cachedTrending.isEmpty()) {
            return false
        }

        val latestCachedAt = trendingCacheDao.latestCachedAt() ?: return false
        return nowProvider() - latestCachedAt <= TRENDING_CACHE_TTL_MS
    }

    private suspend fun cacheTrending(items: List<TrendingItem>, cachedAt: Long) {
        trendingCacheDao.clearAll()

        val cacheEntries = items.mapIndexed { index, item ->
            TrendingCacheEntity(
                mediaItemId = upsertMediaMetadata(item = item, metadataUpdatedAt = cachedAt),
                position = index,
                cachedAt = cachedAt,
            )
        }

        trendingCacheDao.upsertAll(cacheEntries)
    }

    private suspend fun upsertMediaMetadata(item: TrendingItem, metadataUpdatedAt: Long): Long {
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
}
