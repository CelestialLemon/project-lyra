package com.projectlyra.app.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("trending/all/day")
    suspend fun getTrendingAllDay(
        @Query("api_key") apiKey: String,
    ): TmdbTrendingResponse

    @GET("search/multi")
    suspend fun searchMulti(
        @Query("api_key") apiKey: String,
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
    ): TmdbMultiSearchResponse

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
    ): TmdbMovieDetailsDto

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
    ): TmdbTvDetailsDto

    @GET("discover/movie")
    suspend fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1,
    ): TmdbDiscoverMovieResponse

    @GET("discover/tv")
    suspend fun discoverTvShows(
        @Query("api_key") apiKey: String,
        @Query("with_genres") withGenres: String,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "en-US",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("page") page: Int = 1,
    ): TmdbDiscoverTvResponse

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
    ): TmdbGenreListResponse

    @GET("genre/tv/list")
    suspend fun getTvGenres(
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "en-US",
    ): TmdbGenreListResponse
}

data class TmdbTrendingResponse(
    val results: List<TmdbTrendingItemDto> = emptyList(),
)

data class TmdbMultiSearchResponse(
    val results: List<TmdbMultiSearchItemDto> = emptyList(),
)

data class TmdbTrendingItemDto(
    val id: Int,
    @Json(name = "media_type")
    val mediaType: String?,
    val title: String?,
    val name: String?,
    val overview: String?,
    @Json(name = "poster_path")
    val posterPath: String?,
    @Json(name = "release_date")
    val releaseDate: String?,
    @Json(name = "first_air_date")
    val firstAirDate: String?,
    @Json(name = "genre_ids")
    val genreIds: List<Int>? = null,
)

data class TmdbMultiSearchItemDto(
    val id: Int,
    @Json(name = "media_type")
    val mediaType: String?,
    val title: String?,
    val name: String?,
    val overview: String?,
    @Json(name = "poster_path")
    val posterPath: String?,
    @Json(name = "release_date")
    val releaseDate: String?,
    @Json(name = "first_air_date")
    val firstAirDate: String?,
    @Json(name = "genre_ids")
    val genreIds: List<Int>? = null,
)

data class TmdbGenreDto(
    val id: Int,
    val name: String?,
)

data class TmdbGenreListResponse(
    val genres: List<TmdbGenreDto> = emptyList(),
)

data class TmdbDiscoverMovieResponse(
    val results: List<TmdbDiscoverMovieDto> = emptyList(),
)

data class TmdbDiscoverMovieDto(
    val id: Int,
    val title: String?,
    val overview: String?,
    @Json(name = "poster_path")
    val posterPath: String?,
    @Json(name = "release_date")
    val releaseDate: String?,
    @Json(name = "genre_ids")
    val genreIds: List<Int>? = null,
)

data class TmdbDiscoverTvResponse(
    val results: List<TmdbDiscoverTvDto> = emptyList(),
)

data class TmdbDiscoverTvDto(
    val id: Int,
    val name: String?,
    val overview: String?,
    @Json(name = "poster_path")
    val posterPath: String?,
    @Json(name = "first_air_date")
    val firstAirDate: String?,
    @Json(name = "genre_ids")
    val genreIds: List<Int>? = null,
)

data class TmdbMovieDetailsDto(
    val id: Int,
    val title: String?,
    val overview: String?,
    @Json(name = "poster_path")
    val posterPath: String?,
    @Json(name = "backdrop_path")
    val backdropPath: String?,
    @Json(name = "release_date")
    val releaseDate: String?,
    val runtime: Int?,
    val genres: List<TmdbGenreDto>?,
)

data class TmdbTvDetailsDto(
    val id: Int,
    val name: String?,
    val overview: String?,
    @Json(name = "poster_path")
    val posterPath: String?,
    @Json(name = "backdrop_path")
    val backdropPath: String?,
    @Json(name = "first_air_date")
    val firstAirDate: String?,
    @Json(name = "number_of_seasons")
    val numberOfSeasons: Int?,
    @Json(name = "number_of_episodes")
    val numberOfEpisodes: Int?,
    val seasons: List<TmdbSeasonDto>?,
    val genres: List<TmdbGenreDto>?,
)

data class TmdbSeasonDto(
    @Json(name = "season_number")
    val seasonNumber: Int?,
    val name: String?,
    @Json(name = "episode_count")
    val episodeCount: Int?,
    @Json(name = "air_date")
    val airDate: String?,
    @Json(name = "poster_path")
    val posterPath: String?,
)
