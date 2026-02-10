package com.projectlyra.app.data.repository

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

internal fun Throwable.toUserFacingMessage(): String {
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
