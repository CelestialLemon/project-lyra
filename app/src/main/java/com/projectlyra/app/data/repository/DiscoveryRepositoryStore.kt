package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.data.local.GenreMetadataDao
import com.projectlyra.app.data.local.GenreMetadataEntity
import com.projectlyra.app.data.local.MediaDao
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.TrendingCacheDao
import com.projectlyra.app.data.local.TrendingCacheEntity
import com.projectlyra.app.data.remote.TmdbApiService
import kotlinx.coroutines.CancellationException

internal class DiscoveryRepositoryStore(
    private val mediaDao: MediaDao,
    private val trendingCacheDao: TrendingCacheDao,
    private val genreMetadataDao: GenreMetadataDao,
    private val tmdbApiService: TmdbApiService,
    private val nowProvider: () -> Long,
) {
    companion object {
        private const val TRENDING_CACHE_TTL_MS = 6 * 60 * 60 * 1000L
        private const val GENRE_CACHE_TTL_MS = 7 * 24 * 60 * 60 * 1000L
        private const val TRENDING_LIMIT = 20
        private const val SEARCH_LIMIT = 30
        private const val PROFILE_GENRE_LIMIT = 3
        private const val DEFAULT_RECOMMENDATION_LIMIT = 20
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

    suspend fun getMovieRecommendations(
        apiKey: String,
        request: MovieRecommendationRequest,
        trackedMediaKeys: Set<String>,
    ): RecommendationResult {
        return getRecommendations(
            apiKey = apiKey,
            mediaType = MediaType.MOVIE,
            rawGenreIds = request.genreIds,
            limit = request.limit,
            trackedMediaKeys = trackedMediaKeys,
            discover = { normalizedApiKey, withGenres ->
                tmdbApiService.discoverMovies(
                    apiKey = normalizedApiKey,
                    withGenres = withGenres,
                ).results.mapNotNull { dto -> dto.toDomainOrNull() }
            },
        )
    }

    suspend fun getTvRecommendations(
        apiKey: String,
        request: TvRecommendationRequest,
        trackedMediaKeys: Set<String>,
    ): RecommendationResult {
        return getRecommendations(
            apiKey = apiKey,
            mediaType = MediaType.TV,
            rawGenreIds = request.genreIds,
            limit = request.limit,
            trackedMediaKeys = trackedMediaKeys,
            discover = { normalizedApiKey, withGenres ->
                tmdbApiService.discoverTvShows(
                    apiKey = normalizedApiKey,
                    withGenres = withGenres,
                ).results.mapNotNull { dto -> dto.toDomainOrNull() }
            },
        )
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
                upsertGenreMetadata(
                    mediaType = mediaType,
                    genreEntries = remoteDetails.genreIds.zip(remoteDetails.genres),
                    updatedAt = now,
                )
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

    suspend fun getTvSeasonDetails(
        apiKey: String,
        tvId: Int,
        seasonNumber: Int,
    ): TvSeasonDetailsResult {
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isEmpty()) {
            return TvSeasonDetailsResult.MissingApiKey
        }
        if (seasonNumber < 0) {
            return TvSeasonDetailsResult.Error(message = "Unable to load episodes for this season right now.")
        }

        return try {
            val seasonDetails = tmdbApiService.getTvSeasonDetails(
                tvId = tvId,
                seasonNumber = seasonNumber,
                apiKey = normalizedApiKey,
            ).toDomainOrNull(
                tvId = tvId,
                fallbackSeasonNumber = seasonNumber,
            )

            if (seasonDetails == null) {
                TvSeasonDetailsResult.Error(message = "Unable to load episodes for this season right now.")
            } else {
                TvSeasonDetailsResult.Success(details = seasonDetails)
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            TvSeasonDetailsResult.Error(message = error.toUserFacingMessage())
        }
    }

    private suspend fun getRecommendations(
        apiKey: String,
        mediaType: MediaType,
        rawGenreIds: List<Int>,
        limit: Int,
        trackedMediaKeys: Set<String>,
        discover: suspend (normalizedApiKey: String, withGenres: String) -> List<TrendingItem>,
    ): RecommendationResult {
        val cappedLimit = limit.coerceIn(1, DEFAULT_RECOMMENDATION_LIMIT)
        val fallback = loadFallbackItems(
            apiKey = apiKey,
            mediaType = mediaType,
            limit = cappedLimit,
            trackedMediaKeys = trackedMediaKeys,
        )
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isEmpty()) {
            return RecommendationResult.MissingApiKey(
                fallbackItems = fallback,
                message = "Set a TMDB API key in Settings to load live recommendations.",
            )
        }

        val normalizedGenres = rawGenreIds.filter { it > 0 }.distinct().take(PROFILE_GENRE_LIMIT)
        if (normalizedGenres.isEmpty()) {
            return RecommendationResult.Success(
                items = fallback,
                isPersonalized = false,
                usedFallback = true,
                infoMessage = "Showing fallback picks until completed titles establish genre preferences.",
            )
        }

        return try {
            refreshGenreMetadataIfStale(apiKey = normalizedApiKey)

            val personalizedItems = discover(normalizedApiKey, normalizedGenres.joinToString(","))
                .distinctBy { item -> statusKey(tmdbId = item.tmdbId, mediaType = item.mediaType) }
                .filterNot { item -> statusKey(item.tmdbId, item.mediaType) in trackedMediaKeys }
                .take(cappedLimit)

            if (personalizedItems.isEmpty()) {
                RecommendationResult.Success(
                    items = fallback,
                    isPersonalized = false,
                    usedFallback = true,
                    infoMessage = "No personalized matches right now. Showing fallback picks.",
                )
            } else {
                RecommendationResult.Success(
                    items = personalizedItems,
                    isPersonalized = true,
                    usedFallback = false,
                )
            }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            RecommendationResult.Error(
                fallbackItems = fallback,
                message = error.toUserFacingMessage(),
            )
        }
    }

    private suspend fun refreshGenreMetadataIfStale(apiKey: String) {
        refreshGenreMetadataIfStale(mediaType = MediaType.MOVIE, apiKey = apiKey)
        refreshGenreMetadataIfStale(mediaType = MediaType.TV, apiKey = apiKey)
    }

    private suspend fun refreshGenreMetadataIfStale(mediaType: MediaType, apiKey: String) {
        val latest = genreMetadataDao.latestUpdatedAt(mediaType.name)
        if (latest != null && nowProvider() - latest <= GENRE_CACHE_TTL_MS) {
            return
        }

        val now = nowProvider()
        val genres = when (mediaType) {
            MediaType.MOVIE -> tmdbApiService.getMovieGenres(apiKey = apiKey).genres
            MediaType.TV -> tmdbApiService.getTvGenres(apiKey = apiKey).genres
        }.mapNotNull { dto ->
            val name = dto.name?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            dto.id.takeIf { it > 0 }?.let { genreId -> genreId to name }
        }

        upsertGenreMetadata(mediaType = mediaType, genreEntries = genres, updatedAt = now)
    }

    private suspend fun upsertGenreMetadata(
        mediaType: MediaType,
        genreEntries: List<Pair<Int, String>>,
        updatedAt: Long,
    ) {
        if (genreEntries.isEmpty()) {
            return
        }

        genreMetadataDao.upsertAll(
            genreEntries
                .distinctBy { (genreId, _) -> genreId }
                .map { (genreId, name) ->
                    GenreMetadataEntity(
                        genreId = genreId,
                        mediaType = mediaType.name,
                        name = name,
                        updatedAt = updatedAt,
                    )
                }
        )
    }

    private suspend fun loadFallbackItems(
        apiKey: String,
        mediaType: MediaType,
        limit: Int,
        trackedMediaKeys: Set<String>,
    ): List<TrendingItem> {
        val cachedCandidates = getCachedTrending()
            .filter { item -> item.mediaType == mediaType }
            .filterNot { item -> statusKey(item.tmdbId, item.mediaType) in trackedMediaKeys }
            .take(limit)
        if (cachedCandidates.isNotEmpty()) {
            return cachedCandidates
        }

        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isEmpty()) {
            return cachedCandidates
        }

        return try {
            val remoteTrending = tmdbApiService.getTrendingAllDay(normalizedApiKey)
                .results
                .mapNotNull { dto -> dto.toDomainOrNull() }
                .distinctBy { item -> statusKey(tmdbId = item.tmdbId, mediaType = item.mediaType) }
                .take(TRENDING_LIMIT)

            if (remoteTrending.isNotEmpty()) {
                cacheTrending(items = remoteTrending, cachedAt = nowProvider())
            }

            remoteTrending
                .filter { item -> item.mediaType == mediaType }
                .filterNot { item -> statusKey(item.tmdbId, item.mediaType) in trackedMediaKeys }
                .take(limit)
                .ifEmpty { cachedCandidates }
        } catch (error: Throwable) {
            if (error is CancellationException) {
                throw error
            }
            cachedCandidates
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
                genreIdsCsv = item.genreIds.takeIf { it.isNotEmpty() }?.toGenreIdsCsv()
                    ?: existing?.genreIdsCsv.orEmpty(),
                metadataUpdatedAt = metadataUpdatedAt,
            )
        )

        return if (existingId != 0L) existingId else upsertedId
    }
}
