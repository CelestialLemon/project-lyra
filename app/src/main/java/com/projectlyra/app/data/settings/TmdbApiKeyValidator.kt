package com.projectlyra.app.data.settings

object TmdbApiKeyValidator {
    private val tmdbV3KeyRegex = Regex("^[A-Fa-f0-9]{32}$")
    const val HELPER_TEXT = "Expected format: 32-character TMDB v3 API key (hex)."
    private const val INVALID_FORMAT_MESSAGE = "Invalid format. Use exactly 32 hexadecimal characters."

    fun validate(input: String): String? {
        val normalized = input.trim()
        if (normalized.isEmpty()) return null
        return if (tmdbV3KeyRegex.matches(normalized)) null else INVALID_FORMAT_MESSAGE
    }
}
