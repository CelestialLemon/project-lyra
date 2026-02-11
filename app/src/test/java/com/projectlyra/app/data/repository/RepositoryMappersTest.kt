package com.projectlyra.app.data.repository

import com.projectlyra.app.data.remote.TmdbTvEpisodeDto
import com.projectlyra.app.data.remote.TmdbTvSeasonDetailsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryMappersTest {
    @Test
    fun tvSeasonMapper_filtersInvalidEpisodes_andSortsAscending() {
        val dto = TmdbTvSeasonDetailsDto(
            id = 77,
            seasonNumber = 3,
            name = "Season 3",
            episodes = listOf(
                TmdbTvEpisodeDto(
                    id = 99,
                    episodeNumber = 3,
                    name = "Third",
                    stillPath = "/three.jpg",
                    airDate = "2024-02-02",
                    runtime = 50,
                ),
                TmdbTvEpisodeDto(
                    id = null,
                    episodeNumber = 1,
                    name = "Invalid Missing Id",
                    stillPath = null,
                    airDate = null,
                    runtime = 40,
                ),
                TmdbTvEpisodeDto(
                    id = 10,
                    episodeNumber = 1,
                    name = "",
                    stillPath = "",
                    airDate = "",
                    runtime = -1,
                ),
            ),
        )

        val mapped = dto.toDomainOrNull(tvId = 55, fallbackSeasonNumber = 1)

        assertEquals(55, mapped?.tvId)
        assertEquals(3, mapped?.seasonNumber)
        assertEquals(1, mapped?.episodes?.first()?.episodeNumber)
        assertEquals(3, mapped?.episodes?.last()?.episodeNumber)
        assertEquals("Episode 1", mapped?.episodes?.first()?.title)
        assertNull(mapped?.episodes?.first()?.stillPath)
        assertNull(mapped?.episodes?.first()?.airDate)
        assertNull(mapped?.episodes?.first()?.runtimeMinutes)
    }

    @Test
    fun tvSeasonMapper_usesFallbackSeasonNumber_whenMissing() {
        val dto = TmdbTvSeasonDetailsDto(
            id = 88,
            seasonNumber = null,
            name = "",
            episodes = emptyList(),
        )

        val mapped = dto.toDomainOrNull(tvId = 101, fallbackSeasonNumber = 0)

        assertEquals(0, mapped?.seasonNumber)
        assertEquals("Season 0", mapped?.seasonName)
        assertTrue(mapped?.episodes?.isEmpty() == true)
    }
}
