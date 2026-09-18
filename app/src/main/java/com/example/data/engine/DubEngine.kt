package com.example.data.engine

import com.example.data.model.CharacterCast
import com.example.data.model.DubInfo
import com.example.data.model.EpisodeItem
import com.example.data.model.StreamingService

object DubEngine {

    private val MAJOR_DUB_LICENSORS = listOf(
        "Crunchyroll",
        "Funimation",
        "Netflix",
        "Sentai Filmworks",
        "HIDIVE",
        "Disney+",
        "Hulu",
        "Viz Media",
        "Aniplex of America",
        "Bandai Entertainment"
    )

    /**
     * Synthesizes cross-platform dubbing metadata from voice cast,
     * licensors, streaming platforms, and broadcast status.
     */
    fun analyzeDub(
        characters: List<CharacterCast>,
        licensors: List<String>,
        streamingServices: List<StreamingService>,
        status: String?,
        totalEpisodes: Int?,
        format: String? = "TV",
        dubLanguagesFromAniList: List<String> = emptyList()
    ): DubInfo {
        // 1. Check for English voice actors in cast
        val englishCast = characters.filter { !it.englishVoiceActorName.isNullOrBlank() }
        val hasEnglishVa = englishCast.isNotEmpty()

        // 2. Check for major dub licensors or streaming services
        val detectedLicensor = licensors.firstOrNull { lic ->
            MAJOR_DUB_LICENSORS.any { lic.contains(it, ignoreCase = true) }
        } ?: streamingServices.map { it.name }.firstOrNull { service ->
            MAJOR_DUB_LICENSORS.any { service.contains(it, ignoreCase = true) }
        }

        val hasMajorDubPlatform = detectedLicensor != null

        // 3. Determine if dub is available
        val isDubAvailable = hasEnglishVa || hasMajorDubPlatform

        // 4. Determine available languages
        val availableLanguages = mutableSetOf<String>()
        if (isDubAvailable) {
            availableLanguages.add("English")
        }
        availableLanguages.addAll(dubLanguagesFromAniList)

        if (detectedLicensor?.contains("Netflix", ignoreCase = true) == true) {
            availableLanguages.addAll(listOf("Spanish (Latin)", "Portuguese (Brazil)", "French", "German", "Italian"))
        } else if (detectedLicensor?.contains("Crunchyroll", ignoreCase = true) == true) {
            availableLanguages.addAll(listOf("Spanish (Latin)", "German", "French", "Portuguese (Brazil)"))
        }

        // 5. Determine Dub Status and Schedule Details
        val normalizedStatus = status?.uppercase() ?: "UNKNOWN"
        val dubStatus: String
        val scheduleDetails: String

        when {
            !isDubAvailable -> {
                dubStatus = "Sub Only"
                scheduleDetails = "Available in original Japanese audio with English subtitles."
            }
            normalizedStatus.contains("RELEASING") || normalizedStatus.contains("AIRING") -> {
                dubStatus = "Simuldub Ongoing"
                scheduleDetails = if (detectedLicensor != null) {
                    "Simuldub streaming on $detectedLicensor. New dubbed episodes release ~2-3 weeks following Japanese broadcast."
                } else {
                    "Active simuldub broadcast. Dubbed episodes release ~2-3 weeks behind original sub airings."
                }
            }
            normalizedStatus.contains("FINISHED") || normalizedStatus.contains("COMPLETED") -> {
                dubStatus = "Full Dub Available"
                scheduleDetails = if (detectedLicensor != null) {
                    "Complete English dub streaming on $detectedLicensor for all released episodes."
                } else {
                    "Complete English dub produced and available across all episodes."
                }
            }
            normalizedStatus.contains("NOT_YET_RELEASED") || normalizedStatus.contains("UPCOMING") -> {
                dubStatus = if (hasMajorDubPlatform) "Dub Announced" else "Sub Broadcast First"
                scheduleDetails = if (hasMajorDubPlatform) {
                    "Dub announced via $detectedLicensor. Simuldub anticipated following original premiere."
                } else {
                    "Original Japanese broadcast scheduled. Dub confirmation pending licensor announcements."
                }
            }
            else -> {
                dubStatus = if (isDubAvailable) "Dub Available" else "Sub Only"
                scheduleDetails = if (isDubAvailable) "English dub audio track available." else "Japanese audio with subtitles."
            }
        }

        val leadEnglish = englishCast.take(5).map {
            it.name to it.englishVoiceActorName!!
        }

        return DubInfo(
            isDubAvailable = isDubAvailable,
            dubStatus = dubStatus,
            primaryDubLanguage = "English",
            availableLanguages = availableLanguages.toList(),
            dubLicensor = detectedLicensor,
            dubScheduleDetails = scheduleDetails,
            totalDubbedEpisodes = if (isDubAvailable && normalizedStatus.contains("FINISHED")) totalEpisodes else null,
            leadEnglishCast = leadEnglish
        )
    }

    /**
     * Decorates episodes with real-time dubbing indicators based on broadcast status and lag.
     */
    fun decorateEpisodesWithDub(
        episodes: List<EpisodeItem>,
        dubInfo: DubInfo,
        status: String?
    ): List<EpisodeItem> {
        val normalizedStatus = status?.uppercase() ?: "UNKNOWN"
        val isOngoing = normalizedStatus.contains("RELEASING") || normalizedStatus.contains("AIRING")
        val isFinished = normalizedStatus.contains("FINISHED") || normalizedStatus.contains("COMPLETED")

        // In a simuldub, the latest ~2 aired episodes are typically sub-only waiting for dub release
        val airedEpisodes = episodes.filter { it.isAired }
        val latestAiredCount = airedEpisodes.size

        return episodes.map { ep ->
            val hasDub: Boolean
            val dubText: String

            if (!dubInfo.isDubAvailable) {
                hasDub = false
                dubText = if (ep.isAired) "SUB ONLY" else "SUB ONLY (Upcoming)"
            } else if (!ep.isAired) {
                hasDub = false
                dubText = "Dub following broadcast"
            } else if (isFinished) {
                hasDub = true
                dubText = "SUB & DUB"
            } else if (isOngoing) {
                // If it's one of the most recent 2 aired episodes, simuldub is in-progress
                val isRecentAiredLag = ep.episodeNumber >= maxOf(1, latestAiredCount - 1)
                if (isRecentAiredLag) {
                    hasDub = false
                    dubText = "SUB (Dub in ~2 wks)"
                } else {
                    hasDub = true
                    dubText = "SUB & DUB"
                }
            } else {
                hasDub = true
                dubText = "SUB & DUB"
            }

            ep.copy(
                hasDub = hasDub,
                dubStatusText = dubText
            )
        }
    }
}
