package com.projectlyra.app.ui.navigation

import com.projectlyra.app.core.model.MediaType

object DetailsDestination {
    const val ARG_MEDIA_TYPE = "mediaType"
    const val ARG_TMDB_ID = "tmdbId"
    const val ROUTE_PATTERN = "details/{$ARG_MEDIA_TYPE}/{$ARG_TMDB_ID}"

    fun route(mediaType: MediaType, tmdbId: Int): String {
        return "details/${mediaType.name.lowercase()}/$tmdbId"
    }

    fun parseMediaType(rawValue: String?): MediaType? {
        return when (rawValue?.lowercase()) {
            "movie" -> MediaType.MOVIE
            "tv" -> MediaType.TV
            else -> null
        }
    }
}
