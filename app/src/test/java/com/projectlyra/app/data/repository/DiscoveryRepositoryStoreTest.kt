package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaType
import com.projectlyra.app.data.local.CachedTrendingRow
import com.projectlyra.app.data.local.GenreMetadataDao
import com.projectlyra.app.data.local.GenreMetadataEntity
import com.projectlyra.app.data.local.MediaDao
import com.projectlyra.app.data.local.MediaItemEntity
import com.projectlyra.app.data.local.TrendingCacheDao
import com.projectlyra.app.data.local.TrendingCacheEntity
import com.projectlyra.app.data.remote.TmdbApiService
import com.projectlyra.app.data.remote.TmdbDiscoverMovieDto
import com.projectlyra.app.data.remote.TmdbDiscoverMovieResponse
import com.projectlyra.app.data.remote.TmdbDiscoverTvResponse
import com.projectlyra.app.data.remote.TmdbGenreDto
import com.projectlyra.app.data.remote.TmdbGenreListResponse
import com.projectlyra.app.data.remote.TmdbMovieDetailsDto
import com.projectlyra.app.data.remote.TmdbMultiSearchResponse
import com.projectlyra.app.data.remote.TmdbSeasonDto
import com.projectlyra.app.data.remote.TmdbTrendingItemDto
import com.projectlyra.app.data.remote.TmdbTrendingResponse
import com.projectlyra.app.data.remote.TmdbTvDetailsDto
import com.projectlyra.app.data.remote.TmdbTvEpisodeDto
import com.projectlyra.app.data.remote.TmdbTvSeasonDetailsDto
import java.net.UnknownHostException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscoveryRepositoryStoreTest {
    @Test
    fun refreshTrending_returnsMissingApiKeyWithCachedItems() = runBlocking {
        val mediaDao = FakeMediaDao()
        val trendingCacheDao = FakeTrendingCacheDao(mediaDao)
        val genreMetadataDao = FakeGenreMetadataDao()
        val api = FakeTmdbApiService()
        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = trendingCacheDao,
            genreMetadataDao = genreMetadataDao,
            tmdbApiService = api,
            nowProvider = { 1_000L },
        )

        val mediaId = mediaDao.upsertMediaItem(
            MediaItemEntity(
                tmdbId = 101,
                mediaType = MediaType.MOVIE.name,
                title = "Sample",
                overview = "Overview",
                posterPath = "/a.jpg",
                releaseOrAirDate = "2024-01-01",
                metadataUpdatedAt = 900L,
            )
        )
        trendingCacheDao.upsertAll(
            listOf(
                TrendingCacheEntity(
                    mediaItemId = mediaId,
                    position = 0,
                    cachedAt = 900L,
                )
            )
        )

        val result = store.refreshTrending(apiKey = "", forceRefresh = false)

        assertTrue(result is TrendingRefreshResult.MissingApiKey)
        val cached = (result as TrendingRefreshResult.MissingApiKey).cachedItems
        assertEquals(1, cached.size)
        assertEquals(101, cached.first().tmdbId)
    }

    @Test
    fun refreshTrending_fetchesAndCachesRemoteResults() = runBlocking {
        val mediaDao = FakeMediaDao()
        val trendingCacheDao = FakeTrendingCacheDao(mediaDao)
        val genreMetadataDao = FakeGenreMetadataDao()
        val api = FakeTmdbApiService().apply {
            trendingHandler = {
                TmdbTrendingResponse(
                    results = listOf(
                        TmdbTrendingItemDto(
                            id = 200,
                            mediaType = "movie",
                            title = "Valid Movie",
                            name = null,
                            overview = "ok",
                            posterPath = "/poster.jpg",
                            releaseDate = "2020-01-01",
                            firstAirDate = null,
                        ),
                        TmdbTrendingItemDto(
                            id = 201,
                            mediaType = "movie",
                            title = "No Poster",
                            name = null,
                            overview = "skip",
                            posterPath = null,
                            releaseDate = "2020-01-02",
                            firstAirDate = null,
                        ),
                    )
                )
            }
        }

        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = trendingCacheDao,
            genreMetadataDao = genreMetadataDao,
            tmdbApiService = api,
            nowProvider = { 2_000L },
        )

        val result = store.refreshTrending(apiKey = "key", forceRefresh = true)

        assertTrue(result is TrendingRefreshResult.Success)
        val success = result as TrendingRefreshResult.Success
        assertTrue(!success.fromCache)
        assertEquals(1, success.items.size)
        assertEquals(1, trendingCacheDao.cacheSize())
        assertEquals(1, mediaDao.mediaCount())
    }

    @Test
    fun searchTitles_mapsNetworkErrorsToUserMessage() = runBlocking {
        val mediaDao = FakeMediaDao()
        val trendingCacheDao = FakeTrendingCacheDao(mediaDao)
        val genreMetadataDao = FakeGenreMetadataDao()
        val api = FakeTmdbApiService().apply {
            searchHandler = { _, _ -> throw UnknownHostException("offline") }
        }
        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = trendingCacheDao,
            genreMetadataDao = genreMetadataDao,
            tmdbApiService = api,
            nowProvider = { 3_000L },
        )

        val result = store.searchTitles(apiKey = "key", query = "matrix")

        assertTrue(result is SearchResult.Error)
        val message = (result as SearchResult.Error).message
        assertTrue(message.contains("Cannot reach TMDB"))
    }

    @Test
    fun getMediaDetails_returnsFallbackWhenApiKeyMissing() = runBlocking {
        val mediaDao = FakeMediaDao()
        val trendingCacheDao = FakeTrendingCacheDao(mediaDao)
        val genreMetadataDao = FakeGenreMetadataDao()
        val api = FakeTmdbApiService()
        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = trendingCacheDao,
            genreMetadataDao = genreMetadataDao,
            tmdbApiService = api,
            nowProvider = { 4_000L },
        )

        mediaDao.upsertMediaItem(
            MediaItemEntity(
                tmdbId = 777,
                mediaType = MediaType.MOVIE.name,
                title = "Saved Title",
                overview = "Saved Overview",
                posterPath = "/saved.jpg",
                releaseOrAirDate = "2018-01-01",
                metadataUpdatedAt = 100L,
            )
        )

        val result = store.getMediaDetails(
            apiKey = " ",
            tmdbId = 777,
            mediaType = MediaType.MOVIE,
        )

        assertTrue(result is MediaDetailsResult.MissingApiKey)
        val fallback = (result as MediaDetailsResult.MissingApiKey).localFallback
        assertNotNull(fallback)
        assertEquals("Saved Title", fallback?.title)
    }

    @Test
    fun getMovieRecommendations_filtersTrackedItems() = runBlocking {
        val mediaDao = FakeMediaDao()
        val trendingCacheDao = FakeTrendingCacheDao(mediaDao)
        val genreMetadataDao = FakeGenreMetadataDao()
        val api = FakeTmdbApiService().apply {
            discoverMovieHandler = { _, _ ->
                TmdbDiscoverMovieResponse(
                    results = listOf(
                        TmdbDiscoverMovieDto(
                            id = 10,
                            title = "Tracked",
                            overview = "",
                            posterPath = "/tracked.jpg",
                            releaseDate = "2022-01-01",
                            genreIds = listOf(18),
                        ),
                        TmdbDiscoverMovieDto(
                            id = 11,
                            title = "Fresh",
                            overview = "",
                            posterPath = "/fresh.jpg",
                            releaseDate = "2023-01-01",
                            genreIds = listOf(18),
                        ),
                    )
                )
            }
        }

        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = trendingCacheDao,
            genreMetadataDao = genreMetadataDao,
            tmdbApiService = api,
            nowProvider = { 5_000L },
        )

        val result = store.getMovieRecommendations(
            apiKey = "key",
            request = MovieRecommendationRequest(genreIds = listOf(18, 12, 35)),
            trackedMediaKeys = setOf("MOVIE:10"),
        )

        assertTrue(result is RecommendationResult.Success)
        val success = result as RecommendationResult.Success
        assertTrue(success.isPersonalized)
        assertEquals(1, success.items.size)
        assertEquals(11, success.items.first().tmdbId)
    }

    @Test
    fun getMovieRecommendations_whenProfileMissing_usesFallback() = runBlocking {
        val mediaDao = FakeMediaDao()
        val trendingCacheDao = FakeTrendingCacheDao(mediaDao)
        val genreMetadataDao = FakeGenreMetadataDao()
        val api = FakeTmdbApiService().apply {
            trendingHandler = {
                TmdbTrendingResponse(
                    results = listOf(
                        TmdbTrendingItemDto(
                            id = 12,
                            mediaType = "movie",
                            title = "Fallback Movie",
                            name = null,
                            overview = "",
                            posterPath = "/fallback.jpg",
                            releaseDate = "2020-01-01",
                            firstAirDate = null,
                        )
                    )
                )
            }
        }

        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = trendingCacheDao,
            genreMetadataDao = genreMetadataDao,
            tmdbApiService = api,
            nowProvider = { 6_000L },
        )

        val result = store.getMovieRecommendations(
            apiKey = "key",
            request = MovieRecommendationRequest(genreIds = emptyList()),
            trackedMediaKeys = emptySet(),
        )

        assertTrue(result is RecommendationResult.Success)
        val success = result as RecommendationResult.Success
        assertTrue(!success.isPersonalized)
        assertTrue(success.usedFallback)
        assertEquals(12, success.items.first().tmdbId)
    }

    @Test
    fun getMovieRecommendations_onApiFailure_returnsFallbackWithErrorContract() = runBlocking {
        val mediaDao = FakeMediaDao()
        val trendingCacheDao = FakeTrendingCacheDao(mediaDao)
        val genreMetadataDao = FakeGenreMetadataDao()
        val api = FakeTmdbApiService().apply {
            trendingHandler = {
                TmdbTrendingResponse(
                    results = listOf(
                        TmdbTrendingItemDto(
                            id = 13,
                            mediaType = "movie",
                            title = "Fallback Movie",
                            name = null,
                            overview = "",
                            posterPath = "/fallback.jpg",
                            releaseDate = "2020-01-01",
                            firstAirDate = null,
                        )
                    )
                )
            }
            discoverMovieHandler = { _, _ -> throw UnknownHostException("offline") }
        }

        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = trendingCacheDao,
            genreMetadataDao = genreMetadataDao,
            tmdbApiService = api,
            nowProvider = { 7_000L },
        )

        val result = store.getMovieRecommendations(
            apiKey = "key",
            request = MovieRecommendationRequest(genreIds = listOf(18)),
            trackedMediaKeys = emptySet(),
        )

        assertTrue(result is RecommendationResult.Error)
        val error = result as RecommendationResult.Error
        assertEquals(13, error.fallbackItems.first().tmdbId)
        assertTrue(error.message.contains("Cannot reach TMDB"))
    }

    @Test
    fun getTvSeasonDetails_returnsMissingApiKeyWhenNotConfigured() = runBlocking {
        val store = DiscoveryRepositoryStore(
            mediaDao = FakeMediaDao(),
            trendingCacheDao = FakeTrendingCacheDao(FakeMediaDao()),
            genreMetadataDao = FakeGenreMetadataDao(),
            tmdbApiService = FakeTmdbApiService(),
            nowProvider = { 8_000L },
        )

        val result = store.getTvSeasonDetails(apiKey = " ", tvId = 10, seasonNumber = 1)

        assertTrue(result is TvSeasonDetailsResult.MissingApiKey)
    }

    @Test
    fun getTvSeasonDetails_returnsMappedEpisodesOnSuccess() = runBlocking {
        val mediaDao = FakeMediaDao()
        val store = DiscoveryRepositoryStore(
            mediaDao = mediaDao,
            trendingCacheDao = FakeTrendingCacheDao(mediaDao),
            genreMetadataDao = FakeGenreMetadataDao(),
            tmdbApiService = FakeTmdbApiService(),
            nowProvider = { 9_000L },
        )

        val result = store.getTvSeasonDetails(apiKey = "key", tvId = 10, seasonNumber = 2)

        assertTrue(result is TvSeasonDetailsResult.Success)
        val success = result as TvSeasonDetailsResult.Success
        assertEquals(2, success.details.seasonNumber)
        assertEquals(1, success.details.episodes.size)
        assertEquals(1, success.details.episodes.first().episodeNumber)
    }

    private class FakeMediaDao : MediaDao {
        private val itemsById = linkedMapOf<Long, MediaItemEntity>()
        private var nextId = 1L

        override suspend fun upsertMediaItem(mediaItem: MediaItemEntity): Long {
            val key = key(mediaItem.tmdbId, mediaItem.mediaType)
            val existingByKey = itemsById.values.firstOrNull { key(it.tmdbId, it.mediaType) == key }
            val targetId = when {
                mediaItem.id != 0L -> mediaItem.id
                existingByKey != null -> existingByKey.id
                else -> nextId++
            }

            itemsById[targetId] = mediaItem.copy(id = targetId)
            return targetId
        }

        override suspend fun findByTmdbAndType(tmdbId: Int, mediaType: String): MediaItemEntity? {
            return itemsById.values.firstOrNull { it.tmdbId == tmdbId && it.mediaType == mediaType }
        }

        override suspend fun getAllMediaItems(): List<MediaItemEntity> {
            return itemsById.values.toList()
        }

        override suspend fun clearAll(): Int {
            val count = itemsById.size
            itemsById.clear()
            return count
        }

        override suspend fun mediaCount(): Int {
            return itemsById.size
        }

        fun byId(id: Long): MediaItemEntity? {
            return itemsById[id]
        }

        private fun key(tmdbId: Int, mediaType: String): String {
            return "$mediaType:$tmdbId"
        }
    }

    private class FakeTrendingCacheDao(
        private val mediaDao: FakeMediaDao,
    ) : TrendingCacheDao {
        private val entries = mutableListOf<TrendingCacheEntity>()

        override suspend fun getCachedTrending(): List<CachedTrendingRow> {
            return entries
                .sortedBy { it.position }
                .mapNotNull { entry ->
                    val media = mediaDao.byId(entry.mediaItemId) ?: return@mapNotNull null
                    CachedTrendingRow(
                        tmdbId = media.tmdbId,
                        mediaType = media.mediaType,
                        title = media.title,
                        overview = media.overview,
                        posterPath = media.posterPath,
                        releaseOrAirDate = media.releaseOrAirDate,
                    )
                }
        }

        override suspend fun latestCachedAt(): Long? {
            return entries.maxOfOrNull { it.cachedAt }
        }

        override suspend fun clearAll() {
            entries.clear()
        }

        override suspend fun upsertAll(entries: List<TrendingCacheEntity>) {
            this.entries.clear()
            this.entries += entries
        }

        fun cacheSize(): Int {
            return entries.size
        }
    }

    private class FakeGenreMetadataDao : GenreMetadataDao {
        private val rows = mutableListOf<GenreMetadataEntity>()

        override suspend fun upsertAll(entities: List<GenreMetadataEntity>) {
            entities.forEach { incoming ->
                val existingIndex = rows.indexOfFirst {
                    it.genreId == incoming.genreId && it.mediaType == incoming.mediaType
                }
                if (existingIndex >= 0) {
                    rows[existingIndex] = incoming.copy(id = rows[existingIndex].id)
                } else {
                    rows += incoming.copy(id = (rows.size + 1).toLong())
                }
            }
        }

        override suspend fun getByMediaType(mediaType: String): List<GenreMetadataEntity> {
            return rows.filter { it.mediaType == mediaType }
        }

        override suspend fun latestUpdatedAt(mediaType: String): Long? {
            return rows.filter { it.mediaType == mediaType }.maxOfOrNull { it.updatedAt }
        }

        override suspend fun clearAll(): Int {
            val size = rows.size
            rows.clear()
            return size
        }
    }

    private class FakeTmdbApiService : TmdbApiService {
        var trendingHandler: suspend (String) -> TmdbTrendingResponse = { TmdbTrendingResponse() }
        var searchHandler: suspend (String, String) -> TmdbMultiSearchResponse = { _, _ -> TmdbMultiSearchResponse() }
        var movieDetailsHandler: suspend (Int, String) -> TmdbMovieDetailsDto = { movieId, _ ->
            TmdbMovieDetailsDto(
                id = movieId,
                title = "Movie $movieId",
                overview = "",
                posterPath = "/movie.jpg",
                backdropPath = null,
                releaseDate = "2024-01-01",
                runtime = 120,
                genres = listOf(TmdbGenreDto(1, "Drama")),
            )
        }
        var tvDetailsHandler: suspend (Int, String) -> TmdbTvDetailsDto = { tvId, _ ->
            TmdbTvDetailsDto(
                id = tvId,
                name = "TV $tvId",
                overview = "",
                posterPath = "/tv.jpg",
                backdropPath = null,
                firstAirDate = "2024-01-01",
                numberOfSeasons = 1,
                numberOfEpisodes = 10,
                seasons = listOf(
                    TmdbSeasonDto(
                        seasonNumber = 1,
                        name = "Season 1",
                        episodeCount = 10,
                        airDate = "2024-01-01",
                        posterPath = "/s1.jpg",
                    )
                ),
                genres = listOf(TmdbGenreDto(2, "Sci-Fi")),
            )
        }
        var discoverMovieHandler: suspend (String, String) -> TmdbDiscoverMovieResponse = { _, _ ->
            TmdbDiscoverMovieResponse()
        }
        var discoverTvHandler: suspend (String, String) -> TmdbDiscoverTvResponse = { _, _ ->
            TmdbDiscoverTvResponse()
        }
        var tvSeasonDetailsHandler: suspend (Int, Int, String) -> TmdbTvSeasonDetailsDto = { tvId, seasonNumber, _ ->
            TmdbTvSeasonDetailsDto(
                id = tvId * 1_000 + seasonNumber,
                seasonNumber = seasonNumber,
                name = "Season $seasonNumber",
                episodes = listOf(
                    TmdbTvEpisodeDto(
                        id = 1,
                        episodeNumber = 1,
                        name = "Episode 1",
                        stillPath = "/still.jpg",
                        airDate = "2024-01-01",
                        runtime = 42,
                    )
                ),
            )
        }
        var movieGenresHandler: suspend (String) -> TmdbGenreListResponse = {
            TmdbGenreListResponse(genres = listOf(TmdbGenreDto(18, "Drama")))
        }
        var tvGenresHandler: suspend (String) -> TmdbGenreListResponse = {
            TmdbGenreListResponse(genres = listOf(TmdbGenreDto(10765, "Sci-Fi & Fantasy")))
        }

        override suspend fun getTrendingAllDay(apiKey: String): TmdbTrendingResponse {
            return trendingHandler(apiKey)
        }

        override suspend fun searchMulti(
            apiKey: String,
            query: String,
            includeAdult: Boolean,
            language: String,
            page: Int,
        ): TmdbMultiSearchResponse {
            return searchHandler(apiKey, query)
        }

        override suspend fun getMovieDetails(movieId: Int, apiKey: String): TmdbMovieDetailsDto {
            return movieDetailsHandler(movieId, apiKey)
        }

        override suspend fun getTvDetails(tvId: Int, apiKey: String): TmdbTvDetailsDto {
            return tvDetailsHandler(tvId, apiKey)
        }

        override suspend fun getTvSeasonDetails(
            tvId: Int,
            seasonNumber: Int,
            apiKey: String,
        ): TmdbTvSeasonDetailsDto {
            return tvSeasonDetailsHandler(tvId, seasonNumber, apiKey)
        }

        override suspend fun discoverMovies(
            apiKey: String,
            withGenres: String,
            includeAdult: Boolean,
            language: String,
            sortBy: String,
            page: Int,
        ): TmdbDiscoverMovieResponse {
            return discoverMovieHandler(apiKey, withGenres)
        }

        override suspend fun discoverTvShows(
            apiKey: String,
            withGenres: String,
            includeAdult: Boolean,
            language: String,
            sortBy: String,
            page: Int,
        ): TmdbDiscoverTvResponse {
            return discoverTvHandler(apiKey, withGenres)
        }

        override suspend fun getMovieGenres(apiKey: String, language: String): TmdbGenreListResponse {
            return movieGenresHandler(apiKey)
        }

        override suspend fun getTvGenres(apiKey: String, language: String): TmdbGenreListResponse {
            return tvGenresHandler(apiKey)
        }
    }
}
