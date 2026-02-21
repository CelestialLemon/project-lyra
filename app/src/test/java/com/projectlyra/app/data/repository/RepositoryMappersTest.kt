package com.projectlyra.app.data.repository

import com.projectlyra.app.data.remote.TmdbTvEpisodeDto
import com.projectlyra.app.data.remote.TmdbCastMemberDto
import com.projectlyra.app.data.remote.TmdbCreditsDto
import com.projectlyra.app.data.remote.TmdbGenreDto
import com.projectlyra.app.data.remote.TmdbMovieDetailsDto
import com.projectlyra.app.data.remote.TmdbTvDetailsDto
import com.projectlyra.app.data.remote.TmdbSeasonDto
import com.projectlyra.app.data.remote.TmdbTvSeasonDetailsDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryMappersTest {
    @Test
    fun movieDetailsMapper_mapsCast_andDropsInvalidMembers() {
        val dto = TmdbMovieDetailsDto(
            id = 11,
            title = "Example Movie",
            overview = "Overview",
            posterPath = "/poster.jpg",
            backdropPath = null,
            releaseDate = "2024-01-01",
            runtime = 120,
            genres = listOf(TmdbGenreDto(id = 1, name = "Drama")),
            credits = TmdbCreditsDto(
                cast = listOf(
                    TmdbCastMemberDto(id = 1, name = "  Actor One  ", character = " Lead ", profilePath = " /a.jpg "),
                    TmdbCastMemberDto(id = null, name = "No Id", character = null, profilePath = null),
                    TmdbCastMemberDto(id = 2, name = " ", character = "Unknown", profilePath = null),
                )
            ),
        )

        val mapped = dto.toDomainOrNull()

        assertEquals(1, mapped?.cast?.size)
        assertEquals("Actor One", mapped?.cast?.first()?.name)
        assertEquals("Lead", mapped?.cast?.first()?.character)
        assertEquals("/a.jpg", mapped?.cast?.first()?.profilePath)
    }

    @Test
    fun tvDetailsMapper_mapsCastAndSeasonList() {
        val dto = TmdbTvDetailsDto(
            id = 12,
            name = "Example Show",
            overview = "Overview",
            posterPath = "/poster.jpg",
            backdropPath = null,
            firstAirDate = "2023-01-01",
            numberOfSeasons = 1,
            numberOfEpisodes = 8,
            seasons = listOf(TmdbSeasonDto(seasonNumber = 1, name = "Season 1", episodeCount = 8, airDate = null, posterPath = null)),
            genres = listOf(TmdbGenreDto(id = 2, name = "Sci-Fi")),
            credits = TmdbCreditsDto(
                cast = listOf(
                    TmdbCastMemberDto(id = 3, name = "Actor Two", character = null, profilePath = null),
                )
            ),
        )

        val mapped = dto.toDomainOrNull()

        assertEquals(1, mapped?.seasons?.size)
        assertEquals(1, mapped?.cast?.size)
        assertEquals("Actor Two", mapped?.cast?.first()?.name)
        assertNull(mapped?.cast?.first()?.character)
    }

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
