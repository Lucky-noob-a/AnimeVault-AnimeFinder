package com.example.data.engine

import com.example.data.model.SeasonInfo
import com.example.data.model.TimelineEntry

object SeasonEngine {

    /**
     * Determines canonical seasons and franchise groupings from relationship graph and title metadata.
     * Accurately differentiates main series seasons from OVAs, movies, and spin-offs.
     */
    fun extractCanonicalSeasons(
        currentAnimeId: Int,
        currentAnimeTitle: String,
        currentEpisodes: Int?,
        currentYear: Int?,
        currentFormat: String,
        relations: List<TimelineEntry>
    ): List<SeasonInfo> {
        val seasons = mutableListOf<SeasonInfo>()

        // Collect all TV format entries linked as PREQUEL, SEQUEL, or MAIN
        val tvRelations = relations.filter {
            (it.format.equals("TV", ignoreCase = true) || it.format.equals("TV_SHORT", ignoreCase = true)) &&
                    (it.relationType.equals("PREQUEL", ignoreCase = true) ||
                            it.relationType.equals("SEQUEL", ignoreCase = true) ||
                            it.relationType.equals("PARENT", ignoreCase = true) ||
                            it.relationType.equals("SIDE_STORY", ignoreCase = false))
        }

        // Add current entry
        val allEntries = (tvRelations + TimelineEntry(
            id = currentAnimeId,
            title = currentAnimeTitle,
            year = currentYear,
            format = currentFormat,
            relationType = "CURRENT"
        )).distinctBy { it.id }
            .sortedWith(compareBy({ it.year ?: 9999 }, { it.id }))

        var seasonIndex = 1
        for (entry in allEntries) {
            val detectedNumber = parseSeasonNumberFromTitle(entry.title) ?: seasonIndex
            val displayTitle = cleanSeasonTitle(entry.title, detectedNumber)

            seasons.add(
                SeasonInfo(
                    seasonNumber = detectedNumber,
                    title = displayTitle,
                    episodeCount = if (entry.id == currentAnimeId) currentEpisodes else null,
                    year = entry.year,
                    animeId = entry.id,
                    isCanonical = true
                )
            )
            seasonIndex++
        }

        return seasons.sortedBy { it.seasonNumber }
    }

    /**
     * Organizes all franchise relations into chronological categories:
     * - Main Story (TV Seasons)
     * - Movies
     * - OVAs & ONAs
     * - Specials & Recaps
     * - Spin-offs & Side Stories
     */
    fun organizeFranchiseTimeline(
        currentAnimeId: Int,
        currentAnimeTitle: String,
        currentYear: Int?,
        currentFormat: String,
        relations: List<TimelineEntry>
    ): List<TimelineEntry> {
        val allEntries = (relations + TimelineEntry(
            id = currentAnimeId,
            title = currentAnimeTitle,
            year = currentYear,
            format = currentFormat,
            relationType = "CURRENT"
        )).distinctBy { it.id }

        // Sort chronologically by year, or by ID
        return allEntries.sortedWith(
            compareBy(
                { it.year ?: 9999 },
                { formatPriority(it.format) },
                { it.title }
            )
        )
    }

    private fun parseSeasonNumberFromTitle(title: String): Int? {
        val lower = title.lowercase()
        val regexPatterns = listOf(
            Regex("""season\s*(\d+)"""),
            Regex("""(\d+)(?:st|nd|rd|th)\s*season"""),
            Regex("""part\s*(\d+)"""),
            Regex("""s(\d+)""")
        )

        for (regex in regexPatterns) {
            val match = regex.find(lower)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }

        if (lower.contains("final season") || lower.contains("the final")) {
            return 4 // or last
        }

        return null
    }

    private fun cleanSeasonTitle(title: String, seasonNum: Int): String {
        return when {
            title.contains("Season", ignoreCase = true) -> title
            title.contains("Part", ignoreCase = true) -> title
            else -> "$title (Season $seasonNum)"
        }
    }

    private fun formatPriority(format: String): Int {
        return when (format.uppercase()) {
            "TV" -> 1
            "MOVIE" -> 2
            "OVA" -> 3
            "ONA" -> 4
            "SPECIAL" -> 5
            else -> 6
        }
    }
}
