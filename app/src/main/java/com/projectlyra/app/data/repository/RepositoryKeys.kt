package com.projectlyra.app.data.repository

import com.projectlyra.app.core.model.MediaType

internal fun statusKey(tmdbId: Int, mediaType: MediaType): String {
    return "${mediaType.name}:$tmdbId"
}
