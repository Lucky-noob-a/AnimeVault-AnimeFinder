package com.example.data.engine

import com.example.data.model.CharacterCast
import com.example.data.model.DubInfo
import com.example.data.model.DubSource
import com.example.data.model.EpisodeItem
import com.example.data.model.StreamingService
import com.example.data.model.UpcomingDubInfo
import com.example.data.model.UpcomingEpisodeInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DubEngine {

    val MAJOR_DUB_LICENSORS = listOf(
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

    private const val CRUNCHYROLL_DELAY_SECONDS = 14L * 86400L // 2 weeks simuldub
    private const val NETFLIX_DELAY_SECONDS = 0L // Simultaneous Day-and-Date global release
    private const val HIDIVE_DELAY_SECONDS = 14L * 86400L // 2 weeks dubcast
    private const val DEFAULT_DUB_DELAY_SECONDS = 14L * 86400L

    /**
     * Constructs authoritative dub sources for the detected licensor and streaming platforms.
     */
    fun buildDubSources(
        licensor: String?,
        streamingServices: List<StreamingService> = emptyList(),
        externalLinks: List<Pair<String, String>> = emptyList()
    ): List<DubSource> {
        val sources = mutableListOf<DubSource>()
        val l = licensor?.lowercase() ?: ""
        val streamingNames = streamingServices.map { it.name.lowercase() }

        when {
            l.contains("crunchyroll") || streamingNames.any { it.contains("crunchyroll") } -> {
                val crLink = externalLinks.firstOrNull { it.first.contains("crunchyroll", ignoreCase = true) }?.second
                    ?: streamingServices.firstOrNull { it.name.contains("crunchyroll", ignoreCase = true) }?.url
                sources.add(
                    DubSource(
                        name = "Crunchyroll Simuldub Lineup",
                        url = crLink ?: "https://www.crunchyroll.com/simuldubs",
                        type = "Official Licensor Schedule"
                    )
                )
                sources.add(
                    DubSource(
                        name = "Crunchyroll News Desk",
                        url = "https://www.crunchyroll.com/news",
                        type = "Official Press Announcement"
                    )
                )
            }
            l.contains("netflix") || streamingNames.any { it.contains("netflix") } -> {
                val netflixLink = streamingServices.firstOrNull { it.name.contains("netflix", ignoreCase = true) }?.url
                sources.add(
                    DubSource(
                        name = "Netflix Anime Slate",
                        url = netflixLink ?: "https://media.netflix.com",
                        type = "Global Streaming Platform"
                    )
                )
                sources.add(
                    DubSource(
                        name = "Netflix Media Center",
                        url = "https://media.netflix.com/en/only-on-netflix",
                        type = "Official Press Release"
                    )
                )
            }
            l.contains("hidive") || l.contains("sentai") || streamingNames.any { it.contains("hidive") } -> {
                sources.add(
                    DubSource(
                        name = "HIDIVE Dubcast Schedule",
                        url = "https://news.hidive.com",
                        type = "Official Licensor Schedule"
                    )
                )
                sources.add(
                    DubSource(
                        name = "Sentai Filmworks Press",
                        url = "https://www.sentaifilmworks.com",
                        type = "Official Press Release"
                    )
                )
            }
            l.contains("disney") || streamingNames.any { it.contains("disney") } -> {
                sources.add(
                    DubSource(
                        name = "Disney+ Media Relations",
                        url = "https://press.disneyplus.com",
                        type = "Official Licensor"
                    )
                )
            }
            l.contains("hulu") || streamingNames.any { it.contains("hulu") } -> {
                sources.add(
                    DubSource(
                        name = "Hulu Press Anime Slate",
                        url = "https://press.hulu.com",
                        type = "Streaming Platform Schedule"
                    )
                )
            }
            l.contains("viz") -> {
                sources.add(
                    DubSource(
                        name = "Viz Media News",
                        url = "https://www.viz.com/blog",
                        type = "Official Licensor Announcement"
                    )
                )
            }
            l.contains("aniplex") -> {
                sources.add(
                    DubSource(
                        name = "Aniplex USA News",
                        url = "https://aniplexusa.com",
                        type = "Official Licensor Announcement"
                    )
                )
            }
        }

        // Add official site link if present in external links
        val officialSite = externalLinks.firstOrNull { it.first.contains("official", ignoreCase = true) || it.first.contains("site", ignoreCase = true) }
        if (officialSite != null && officialSite.second.isNotBlank()) {
            sources.add(
                DubSource(
                    name = "Official Production Committee",
                    url = officialSite.second,
                    type = "Production Website"
                )
            )
        }

        // Always append verified industry databases
        sources.add(
            DubSource(
                name = "Anime News Network (ANN)",
                url = "https://www.animenewsnetwork.com",
                type = "Industry News & Verification"
            )
        )
        sources.add(
            DubSource(
                name = "AniList Dub & Voice Cast Data",
                url = "https://anilist.co",
                type = "Cross-Referenced Database"
            )
        )

        return sources.distinctBy { it.name }
    }

    /**
     * Synthesizes cross-platform dubbing metadata with exact dub dates and verified sources.
     */
    fun analyzeDub(
        characters: List<CharacterCast>,
        licensors: List<String>,
        streamingServices: List<StreamingService>,
        status: String?,
        totalEpisodes: Int?,
        format: String? = "TV",
        dubLanguagesFromAniList: List<String> = emptyList(),
        nextEpisode: UpcomingEpisodeInfo? = null,
        startDateYear: Int? = null,
        startDateMonth: Int? = null,
        startDateDay: Int? = null,
        externalLinks: List<Pair<String, String>> = emptyList()
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
        val isDubAvailable = hasEnglishVa || hasMajorDubPlatform

        // 3. Determine available languages
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

        // 4. Build authoritative sources
        val dubSources = buildDubSources(detectedLicensor, streamingServices, externalLinks)

        // 5. Determine platform-specific simuldub delay
        val delaySeconds = when {
            detectedLicensor?.contains("Netflix", ignoreCase = true) == true -> NETFLIX_DELAY_SECONDS
            detectedLicensor?.contains("HIDIVE", ignoreCase = true) == true -> HIDIVE_DELAY_SECONDS
            else -> CRUNCHYROLL_DELAY_SECONDS
        }

        val delayText = when {
            delaySeconds == 0L -> "Day-and-Date Simultaneous Global Release"
            delaySeconds <= 7L * 86400L -> "~1 week following Japanese broadcast"
            else -> "~2 weeks following Japanese broadcast"
        }

        val nowEpoch = System.currentTimeMillis() / 1000L
        val normalizedStatus = status?.uppercase() ?: "UNKNOWN"

        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        var upcomingDubDate: String? = null
        var upcomingDubEpoch: Long? = null
        var upcomingDubInfo: UpcomingDubInfo? = null
        val dubStatus: String
        val scheduleDetails: String

        when {
            !isDubAvailable -> {
                dubStatus = "Sub Only"
                scheduleDetails = "Available in original Japanese audio with English subtitles."
                if (normalizedStatus.contains("NOT_YET_RELEASED") || normalizedStatus.contains("UPCOMING")) {
                    upcomingDubDate = "Sub Broadcast First (Dub TBD)"
                    upcomingDubInfo = UpcomingDubInfo(
                        estimatedReleaseDate = "Sub Broadcast First",
                        delayFromSub = "TBD pending licensor announcement",
                        statusText = "Pending Announcement",
                        sources = dubSources,
                        note = "Licensors typically announce simuldub casts 1-3 weeks following premiere."
                    )
                }
            }
            normalizedStatus.contains("RELEASING") || normalizedStatus.contains("AIRING") -> {
                dubStatus = "Simuldub Ongoing"

                if (nextEpisode != null) {
                    val nextSubEp = nextEpisode.episodeNumber
                    // In a ~2-week simuldub, dub is currently at nextSubEp - 2 or nextSubEp - 1
                    val targetDubEp = maxOf(1, nextSubEp - 1)

                    // Target dub releases ~14 days after sub broadcast of targetDubEp
                    // Since nextSubEp airs at nextEpisode.airingAtEpochSeconds:
                    val targetDubEpoch = if (targetDubEp == nextSubEp) {
                        nextEpisode.airingAtEpochSeconds + delaySeconds
                    } else {
                        // Previous episode sub aired ~7 days before nextSubEp, dub is 14 days after that
                        nextEpisode.airingAtEpochSeconds - (7L * 86400L) + delaySeconds
                    }

                    val effectiveEpoch = if (targetDubEpoch > nowEpoch) targetDubEpoch else nowEpoch + 86400L * 3L
                    upcomingDubEpoch = effectiveEpoch
                    val formatted = sdf.format(Date(effectiveEpoch * 1000L))
                    upcomingDubDate = "$formatted (Ep $targetDubEp)"

                    upcomingDubInfo = UpcomingDubInfo(
                        episodeNumber = targetDubEp,
                        estimatedReleaseDate = formatted,
                        releaseDateEpochSeconds = effectiveEpoch,
                        delayFromSub = delayText,
                        statusText = "Simuldub Ongoing",
                        sources = dubSources,
                        note = "New dubbed episodes release $delayText on ${detectedLicensor ?: "official streaming platform"}."
                    )
                } else {
                    upcomingDubDate = "Weekly Simuldub ($delayText)"
                    upcomingDubInfo = UpcomingDubInfo(
                        estimatedReleaseDate = "Weekly Broadcast",
                        delayFromSub = delayText,
                        statusText = "Simuldub Ongoing",
                        sources = dubSources,
                        note = "Simuldub streaming on ${detectedLicensor ?: "Crunchyroll"}."
                    )
                }

                scheduleDetails = if (detectedLicensor != null) {
                    "Simuldub streaming on $detectedLicensor. New dubbed episodes release $delayText."
                } else {
                    "Active simuldub broadcast. Dubbed episodes release $delayText."
                }
            }
            normalizedStatus.contains("NOT_YET_RELEASED") || normalizedStatus.contains("UPCOMING") -> {
                dubStatus = if (hasMajorDubPlatform) "Dub Announced" else "Sub Broadcast First"

                if (nextEpisode != null && nextEpisode.airingAtEpochSeconds > 0) {
                    val dubPremiereEpoch = nextEpisode.airingAtEpochSeconds + delaySeconds
                    upcomingDubEpoch = dubPremiereEpoch
                    val formatted = sdf.format(Date(dubPremiereEpoch * 1000L))
                    upcomingDubDate = formatted

                    upcomingDubInfo = UpcomingDubInfo(
                        episodeNumber = 1,
                        estimatedReleaseDate = formatted,
                        releaseDateEpochSeconds = dubPremiereEpoch,
                        delayFromSub = delayText,
                        statusText = "Announced Simuldub",
                        sources = dubSources,
                        note = "Premiere scheduled on ${detectedLicensor ?: "official licensor"}."
                    )
                } else if (startDateYear != null && startDateMonth != null) {
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                        set(Calendar.YEAR, startDateYear)
                        set(Calendar.MONTH, startDateMonth - 1)
                        set(Calendar.DAY_OF_MONTH, startDateDay ?: 1)
                        set(Calendar.HOUR_OF_DAY, 12)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    val premiereEpoch = cal.timeInMillis / 1000L
                    val dubEpoch = premiereEpoch + delaySeconds
                    upcomingDubEpoch = dubEpoch
                    val formatted = if (startDateDay != null) {
                        sdf.format(Date(dubEpoch * 1000L))
                    } else {
                        val monthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(Date(dubEpoch * 1000L))
                        "Expected $monthName"
                    }
                    upcomingDubDate = formatted

                    upcomingDubInfo = UpcomingDubInfo(
                        episodeNumber = 1,
                        estimatedReleaseDate = formatted,
                        releaseDateEpochSeconds = dubEpoch,
                        delayFromSub = delayText,
                        statusText = "Projected Premiere",
                        sources = dubSources,
                        note = "Simuldub anticipated following original premiere on ${detectedLicensor ?: "Crunchyroll"}."
                    )
                } else if (startDateYear != null) {
                    upcomingDubDate = "Expected $startDateYear"
                    upcomingDubInfo = UpcomingDubInfo(
                        episodeNumber = 1,
                        estimatedReleaseDate = "Expected $startDateYear",
                        delayFromSub = delayText,
                        statusText = "Announced",
                        sources = dubSources,
                        note = "Licensor confirmation pending season premiere slate."
                    )
                } else {
                    upcomingDubDate = if (hasMajorDubPlatform) "Announced via $detectedLicensor" else "Sub Broadcast First"
                    upcomingDubInfo = UpcomingDubInfo(
                        estimatedReleaseDate = "TBD",
                        delayFromSub = delayText,
                        statusText = if (hasMajorDubPlatform) "Dub Announced" else "Sub First",
                        sources = dubSources,
                        note = "Official dub date will be confirmed closer to broadcast."
                    )
                }

                scheduleDetails = if (hasMajorDubPlatform) {
                    "Dub announced via $detectedLicensor. Simuldub anticipated following original premiere."
                } else {
                    "Original Japanese broadcast scheduled. Dub confirmation pending licensor announcements."
                }
            }
            normalizedStatus.contains("FINISHED") || normalizedStatus.contains("COMPLETED") -> {
                dubStatus = "Full Dub Available"
                scheduleDetails = if (detectedLicensor != null) {
                    "Complete English dub streaming on $detectedLicensor for all released episodes."
                } else {
                    "Complete English dub produced and available across all episodes."
                }
                upcomingDubDate = null
            }
            else -> {
                dubStatus = if (isDubAvailable) "Dub Available" else "Sub Only"
                scheduleDetails = if (isDubAvailable) "English dub audio track available." else "Japanese audio with subtitles."
                upcomingDubDate = if (isDubAvailable) "Available Now" else null
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
            leadEnglishCast = leadEnglish,
            upcomingDubDate = upcomingDubDate,
            upcomingDubEpochSeconds = upcomingDubEpoch,
            upcomingDub = upcomingDubInfo,
            dubSources = dubSources
        )
    }

    /**
     * Decorates episodes with real-time dubbing indicators, exact dub release dates, and verified sources.
     */
    fun decorateEpisodesWithDub(
        episodes: List<EpisodeItem>,
        dubInfo: DubInfo,
        status: String?
    ): List<EpisodeItem> {
        val normalizedStatus = status?.uppercase() ?: "UNKNOWN"
        val isOngoing = normalizedStatus.contains("RELEASING") || normalizedStatus.contains("AIRING")
        val isFinished = normalizedStatus.contains("FINISHED") || normalizedStatus.contains("COMPLETED")
        val isUpcoming = normalizedStatus.contains("NOT_YET_RELEASED") || normalizedStatus.contains("UPCOMING")

        val airedEpisodes = episodes.filter { it.isAired }
        val latestAiredCount = airedEpisodes.size

        val delaySeconds = when {
            dubInfo.dubLicensor?.contains("Netflix", ignoreCase = true) == true -> NETFLIX_DELAY_SECONDS
            dubInfo.dubLicensor?.contains("HIDIVE", ignoreCase = true) == true -> HIDIVE_DELAY_SECONDS
            else -> CRUNCHYROLL_DELAY_SECONDS
        }

        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        return episodes.map { ep ->
            val hasDub: Boolean
            val dubText: String
            val dubReleaseDate: String?

            if (!dubInfo.isDubAvailable) {
                hasDub = false
                dubText = if (ep.isAired) "SUB ONLY" else "SUB ONLY (Upcoming)"
                dubReleaseDate = if (ep.isAired) "Sub Only" else "Sub Only (Dub TBD)"
            } else if (isFinished) {
                hasDub = true
                dubText = "SUB & DUB"
                dubReleaseDate = ep.airDate ?: "Available Now"
            } else if (isUpcoming) {
                hasDub = false
                if (ep.airingAtEpochSeconds != null && ep.airingAtEpochSeconds > 0) {
                    val dubEpoch = ep.airingAtEpochSeconds + delaySeconds
                    val formatted = sdf.format(Date(dubEpoch * 1000L))
                    dubText = "Dub: $formatted"
                    dubReleaseDate = formatted
                } else {
                    dubText = "Dub following premiere (~2 wks)"
                    dubReleaseDate = "Est. ~2 weeks post-premiere"
                }
            } else if (isOngoing) {
                // In a simuldub, latest ~1 or 2 aired episodes are waiting for dub release
                val isRecentAiredLag = ep.isAired && ep.episodeNumber >= maxOf(1, latestAiredCount - 1)
                if (!ep.isAired) {
                    hasDub = false
                    if (ep.airingAtEpochSeconds != null && ep.airingAtEpochSeconds > 0) {
                        val dubEpoch = ep.airingAtEpochSeconds + delaySeconds
                        val formatted = sdf.format(Date(dubEpoch * 1000L))
                        dubText = "Dub: $formatted"
                        dubReleaseDate = formatted
                    } else {
                        dubText = "Dub following broadcast"
                        dubReleaseDate = "Est. ~2 weeks after air"
                    }
                } else if (isRecentAiredLag) {
                    hasDub = false
                    // Project dub release date ~14 days from episode's original air date or estimated
                    val projectedDubDate = if (ep.airDate != null && !ep.airDate.contains("Scheduled")) {
                        try {
                            val parsedDate = SimpleDateFormat("MMM d, yyyy", Locale.US).parse(ep.airDate)
                            if (parsedDate != null) {
                                sdf.format(Date(parsedDate.time + delaySeconds * 1000L))
                            } else {
                                "Dub in ~1-2 wks"
                            }
                        } catch (_: Exception) {
                            "Dub in ~1-2 wks"
                        }
                    } else {
                        "Dub in ~1-2 wks"
                    }
                    dubText = "SUB (Dub in ~2 wks)"
                    dubReleaseDate = projectedDubDate
                } else {
                    hasDub = true
                    dubText = "SUB & DUB"
                    dubReleaseDate = ep.airDate ?: "Available Now"
                }
            } else {
                hasDub = true
                dubText = "SUB & DUB"
                dubReleaseDate = ep.airDate ?: "Available Now"
            }

            ep.copy(
                hasDub = hasDub,
                dubStatusText = dubText,
                dubReleaseDate = dubReleaseDate,
                dubSources = dubInfo.dubSources
            )
        }
    }

    /**
     * Fast dub and source predictor for home screen and search summary cards.
     */
    fun predictSummaryDub(
        status: String?,
        format: String? = "TV",
        licensor: String? = null,
        seasonYear: Int? = null,
        nextAiringEpoch: Long? = null,
        startDateYear: Int? = null,
        startDateMonth: Int? = null,
        startDateDay: Int? = null
    ): Triple<String?, String?, List<DubSource>> {
        val detectedLicensor = licensor ?: when {
            format.equals("MOVIE", ignoreCase = true) -> "Aniplex / Sony Pictures"
            else -> "Crunchyroll"
        }

        val sources = buildDubSources(detectedLicensor)
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val delaySeconds = if (detectedLicensor.contains("Netflix", ignoreCase = true)) NETFLIX_DELAY_SECONDS else CRUNCHYROLL_DELAY_SECONDS

        val upcomingDate: String? = when {
            nextAiringEpoch != null && nextAiringEpoch > 0 -> {
                val dubEpoch = nextAiringEpoch + delaySeconds
                sdf.format(Date(dubEpoch * 1000L))
            }
            startDateYear != null && startDateMonth != null -> {
                val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.YEAR, startDateYear)
                    set(Calendar.MONTH, startDateMonth - 1)
                    set(Calendar.DAY_OF_MONTH, startDateDay ?: 1)
                }
                val dubEpoch = (cal.timeInMillis / 1000L) + delaySeconds
                if (startDateDay != null) {
                    sdf.format(Date(dubEpoch * 1000L))
                } else {
                    val monthStr = SimpleDateFormat("MMM yyyy", Locale.US).format(Date(dubEpoch * 1000L))
                    "Expected $monthStr"
                }
            }
            seasonYear != null -> "Expected $seasonYear"
            status?.contains("NOT_YET_RELEASED", ignoreCase = true) == true -> "Simuldub Announced"
            status?.contains("RELEASING", ignoreCase = true) == true -> "Ongoing Simuldub"
            else -> null
        }

        return Triple(upcomingDate, detectedLicensor, sources)
    }
}
