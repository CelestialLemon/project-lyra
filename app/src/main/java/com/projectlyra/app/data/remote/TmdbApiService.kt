package com.projectlyra.app.data.remote

import com.squareup.moshi.Json
import retrofit2.http.GET
import retrofit2.http.Query

interface TmdbApiService {
    @GET("trending/all/day")
    suspend fun getTrendingAllDay(
        @Query("api_key") apiKey: String,
    ): TmdbTrendingResponse
}

data class TmdbTrendingResponse(
    val results: List<TmdbTrendingItemDto> = emptyList(),
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
)
