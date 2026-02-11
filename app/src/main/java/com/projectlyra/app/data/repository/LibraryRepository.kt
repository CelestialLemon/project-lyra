package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaDetails
import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.core.model.TrackedItem
import com.projectlyra.app.core.model.TrendingItem
import com.projectlyra.app.core.model.WatchStatus
import com.projectlyra.app.data.local.EpisodeReminderStateDao
import com.projectlyra.app.data.local.GenreMetadataDao
import com.projectlyra.app.data.local.MediaDao
import com.projectlyra.app.data.local.TrendingCacheDao
import com.projectlyra.app.data.local.UserEntryDao
import com.projectlyra.app.data.remote.TmdbApiService
import kotlinx.coroutines.flow.Flow

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

data class MovieRecommendationRequest(
    val genreIds: List<Int>,
    val limit: Int = 20,
)

data class TvRecommendationRequest(
    val genreIds: List<Int>,
    val limit: Int = 20,
)

sealed interface RecommendationResult {
    data class Success(
        val items: List<TrendingItem>,
        val isPersonalized: Boolean,
        val usedFallback: Boolean,
        val infoMessage: String? = null,
    ) : RecommendationResult

    data class MissingApiKey(
        val fallbackItems: List<TrendingItem>,
        val message: String,
    ) : RecommendationResult

    data class Error(
        val fallbackItems: List<TrendingItem>,
        val message: String,
    ) : RecommendationResult
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
    mediaDao: MediaDao,
    userEntryDao: UserEntryDao,
    trendingCacheDao: TrendingCacheDao,
    episodeReminderStateDao: EpisodeReminderStateDao,
    genreMetadataDao: GenreMetadataDao,
    tmdbApiService: TmdbApiService,
    private val nowProvider: () -> Long = System::currentTimeMillis,
) {
    private val trackingStore = TrackingRepositoryStore(
        mediaDao = mediaDao,
        userEntryDao = userEntryDao,
        episodeReminderStateDao = episodeReminderStateDao,
        nowProvider = nowProvider,
    )

    private val discoveryStore = DiscoveryRepositoryStore(
        mediaDao = mediaDao,
        trendingCacheDao = trendingCacheDao,
        genreMetadataDao = genreMetadataDao,
        tmdbApiService = tmdbApiService,
        nowProvider = nowProvider,
    )

    fun observeByStatus(status: WatchStatus): Flow<List<TrackedItem>> {
        return trackingStore.observeByStatus(status)
    }

    fun observeTrackedStatusesByMediaKey(): Flow<Map<String, WatchStatus>> {
        return trackingStore.observeTrackedStatusesByMediaKey()
    }

    suspend fun upsertTrackedStatus(item: TrendingItem, status: WatchStatus) {
        trackingStore.upsertTrackedStatus(item = item, status = status)
    }

    suspend fun updateTrackedStatus(mediaItemId: Long, status: WatchStatus) {
        trackingStore.updateTrackedStatus(
            mediaItemId = mediaItemId,
            status = status,
        )
    }

    suspend fun removeTrackedItem(mediaItemId: Long) {
        trackingStore.removeTrackedItem(mediaItemId)
    }

    suspend fun clearTrackedStatus(tmdbId: Int, mediaType: MediaType) {
        trackingStore.clearTrackedStatus(
            tmdbId = tmdbId,
            mediaType = mediaType,
        )
    }

    suspend fun getResumeHeroCandidate(): TrackedItem? {
        return trackingStore.getResumeCandidate()
    }

    suspend fun getTopCompletedGenreIds(mediaType: MediaType, limit: Int = 3): List<Int> {
        return trackingStore.getTopCompletedGenreIds(mediaType = mediaType, limit = limit)
    }

    suspend fun getTvReminderCandidates(): List<ReminderTrackedShow> {
        return trackingStore.getTvReminderCandidates()
    }

    suspend fun getEpisodeReminderState(mediaItemId: Long): EpisodeReminderStateSnapshot? {
        return trackingStore.getEpisodeReminderState(mediaItemId)
    }

    suspend fun upsertEpisodeReminderState(
        mediaItemId: Long,
        lastCheckedAt: Long,
        lastKnownEpisodeCount: Int?,
        lastKnownSeasonCount: Int?,
    ) {
        trackingStore.upsertEpisodeReminderState(
            mediaItemId = mediaItemId,
            lastCheckedAt = lastCheckedAt,
            lastKnownEpisodeCount = lastKnownEpisodeCount,
            lastKnownSeasonCount = lastKnownSeasonCount,
        )
    }

    suspend fun getCachedTrending(): List<TrendingItem> {
        return discoveryStore.getCachedTrending()
    }

    suspend fun refreshTrending(apiKey: String, forceRefresh: Boolean = false): TrendingRefreshResult {
        return discoveryStore.refreshTrending(apiKey = apiKey, forceRefresh = forceRefresh)
    }

    suspend fun searchTitles(apiKey: String, query: String): SearchResult {
        return discoveryStore.searchTitles(apiKey = apiKey, query = query)
    }

    suspend fun getMediaDetails(
        apiKey: String,
        tmdbId: Int,
        mediaType: MediaType,
    ): MediaDetailsResult {
        return discoveryStore.getMediaDetails(
            apiKey = apiKey,
            tmdbId = tmdbId,
            mediaType = mediaType,
        )
    }

    suspend fun getRecommendedMovies(
        apiKey: String,
        request: MovieRecommendationRequest,
    ): RecommendationResult {
        return discoveryStore.getMovieRecommendations(
            apiKey = apiKey,
            request = request,
            trackedMediaKeys = trackingStore.getTrackedMediaKeys(),
        )
    }

    suspend fun getRecommendedTvShows(
        apiKey: String,
        request: TvRecommendationRequest,
    ): RecommendationResult {
        return discoveryStore.getTvRecommendations(
            apiKey = apiKey,
            request = request,
            trackedMediaKeys = trackingStore.getTrackedMediaKeys(),
        )
    }

    suspend fun ensureSeedData() {
        trackingStore.ensureSeedData()
    }

    fun seedTrending(): List<TrendingItem> {
        return trackingStore.seedTrending()
    }
}
