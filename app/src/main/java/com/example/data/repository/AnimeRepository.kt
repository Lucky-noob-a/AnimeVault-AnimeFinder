package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.engine.DubEngine
import com.example.data.engine.SeasonEngine
import com.example.data.local.AnimeDao
import com.example.data.local.AppDatabase
import com.example.data.local.CachedAnimeEntity
import com.example.data.local.FavoriteAnimeEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.AnimeDetails
import com.example.data.model.AnimeSummary
import com.example.data.model.CharacterCast
import com.example.data.model.DubInfo
import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonInfo
import com.example.data.model.SourceAttribution
import com.example.data.model.SourceSummary
import com.example.data.model.StreamingService
import com.example.data.model.StudioInfo
import com.example.data.model.TimelineEntry
import com.example.data.model.UpcomingEpisodeInfo
import com.example.data.model.UpdateCategory
import com.example.data.model.VerificationStatus
import com.example.data.model.VerifiedFact
import com.example.data.model.WebUpdate
import com.example.data.network.anilist.AiringScheduleNode
import com.example.data.network.anilist.AniListApiClient
import com.example.data.network.anilist.AniListMediaDetails
import com.example.data.network.jikan.JikanApiClient
import com.example.data.network.jikan.PaginatedEpisodes
import com.example.data.network.web.WebDataEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AnimeRepository(private val context: Context) {
    private val TAG = "AnimeRepository"
    private val animeDao: AnimeDao by lazy {
        AppDatabase.getDatabase(context).animeDao()
    }
    private val backupManager: com.example.data.backup.AppDataBackupManager by lazy {
        com.example.data.backup.AppDataBackupManager(context)
    }

    /**
     * Searches AniList and Jikan in parallel, deduplicates, normalizes,
     * and ranks exact title matches first.
     */
    suspend fun searchAnime(query: String): List<AnimeSummary> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        // Record search in history
        try {
            animeDao.insertSearchQuery(SearchHistoryEntity(query = trimmed))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to insert search query history", e)
        }

        coroutineScope {
            val anilistDeferred = async {
                try {
                    AniListApiClient.searchAnime(trimmed, perPage = 15)
                } catch (e: Exception) {
                    Log.e(TAG, "AniList search failed", e)
                    emptyList()
                }
            }
            val jikanDeferred = async {
                try {
                    JikanApiClient.searchAnime(trimmed, limit = 15)
                } catch (e: Exception) {
                    Log.e(TAG, "Jikan search failed", e)
                    emptyList()
                }
            }

            val anilistResults = anilistDeferred.await()
            val jikanResults = jikanDeferred.await()

            // Merge and deduplicate by title normalization & malId
            val merged = mutableListOf<AnimeSummary>()
            val seenTitles = mutableSetOf<String>()
            val seenMalIds = mutableSetOf<Int>()

            // Add AniList results first
            for (item in anilistResults) {
                val normTitle = normalizeTitle(item.title)
                seenTitles.add(normTitle)
                item.malId?.let { seenMalIds.add(it) }
                merged.add(item)
            }

            // Add non-duplicate Jikan results
            for (item in jikanResults) {
                val normTitle = normalizeTitle(item.title)
                val malId = item.malId
                if (normTitle !in seenTitles && (malId == null || malId !in seenMalIds)) {
                    seenTitles.add(normTitle)
                    malId?.let { seenMalIds.add(it) }
                    merged.add(item)
                }
            }

            // Rank: exact matches first, then prefix matches, then substring
            val queryLower = trimmed.lowercase()
            merged.sortedWith(
                compareByDescending<AnimeSummary> {
                    when {
                        it.title.equals(queryLower, ignoreCase = true) ||
                                it.englishTitle.equals(queryLower, ignoreCase = true) -> 3
                        it.title.startsWith(queryLower, ignoreCase = true) ||
                                (it.englishTitle?.startsWith(queryLower, ignoreCase = true) == true) -> 2
                        it.title.contains(queryLower, ignoreCase = true) ||
                                (it.englishTitle?.contains(queryLower, ignoreCase = true) == true) -> 1
                        else -> 0
                    }
                }.thenByDescending { it.score ?: 0.0 }
            )
        }
    }

    private val categoryCache = java.util.concurrent.ConcurrentHashMap<String, List<AnimeSummary>>()

    /**
     * Fetches top anime for a specific category (e.g., Action, Romance, Seinen)
     */
    suspend fun getAnimeByCategory(category: String, forceRefresh: Boolean = false): List<AnimeSummary> = withContext(Dispatchers.IO) {
        val trimmed = category.trim()
        if (trimmed.isBlank() || trimmed.equals("ALL", ignoreCase = true)) return@withContext emptyList()

        if (!forceRefresh && categoryCache.containsKey(trimmed)) {
            val cached = categoryCache[trimmed]
            if (!cached.isNullOrEmpty()) return@withContext cached
        }

        try {
            val results = AniListApiClient.getAnimeByCategory(trimmed, perPage = 25)
            if (results.isNotEmpty()) {
                categoryCache[trimmed] = results
                return@withContext results
            }
        } catch (e: Exception) {
            Log.e(TAG, "AniList getAnimeByCategory failed for $trimmed", e)
        }

        // Fallback: search query for category
        try {
            val fallback = searchAnime(trimmed)
            if (fallback.isNotEmpty()) {
                categoryCache[trimmed] = fallback
                return@withContext fallback
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fallback search failed for category $trimmed", e)
        }

        emptyList()
    }

    /**
     * Loads Home Screen Sections (Trending, Currently Airing, Popular, Upcoming)
     */
    suspend fun getHomeScreenSections(): Map<String, List<AnimeSummary>> = withContext(Dispatchers.IO) {
        try {
            val sections = AniListApiClient.getHomeMediaSections().toMutableMap()
            if (sections.isNotEmpty()) {
                val upcoming = sections["upcoming"] ?: emptyList()
                val airing = sections["airing"] ?: emptyList()
                val dubsList = (upcoming.filter { !it.upcomingDubDate.isNullOrBlank() } +
                    airing.filter { !it.upcomingDubDate.isNullOrBlank() })
                    .distinctBy { it.id }
                if (dubsList.isNotEmpty()) {
                    sections["upcomingDubs"] = dubsList
                }
                return@withContext sections
            }
        } catch (e: Exception) {
            Log.e(TAG, "AniList home sections failed, fallback to Jikan", e)
        }

        // Fallback to Jikan popular and airing
        try {
            val airing = JikanApiClient.searchAnime("popular", 10)
            mapOf("trending" to airing, "airing" to airing, "popular" to airing, "upcoming" to emptyList())
        } catch (e: Exception) {
            Log.e(TAG, "Jikan fallback failed", e)
            emptyMap()
        }
    }

    /**
     * Loads complete, cross-checked anime details from AniList, Jikan, and Open-Web.
     */
    suspend fun getAnimeDetails(animeId: Int, malId: Int? = null, forceRefresh: Boolean = false): AnimeDetails? = withContext(Dispatchers.IO) {
        // 1. Check local Room cache if not forcing refresh
        if (!forceRefresh) {
            val cached = animeDao.getCachedAnime(animeId)
            if (cached != null) {
                val details = deserializeDetails(cached.jsonDetails)
                if (details != null) {
                    return@withContext details.copy(isFromCache = true)
                }
            }
        }

        coroutineScope {
            // Fetch AniList details
            val anilistDeferred = async {
                try {
                    AniListApiClient.getMediaDetails(animeId)
                } catch (e: Exception) {
                    Log.e(TAG, "AniList getMediaDetails failed", e)
                    null
                }
            }

            val anilistDetails = anilistDeferred.await()
            val effectiveMalId = malId ?: anilistDetails?.malId

            // Fetch Jikan & Wikipedia details in parallel
            val jikanDeferred = async {
                if (effectiveMalId != null) {
                    try {
                        JikanApiClient.getAnimeFull(effectiveMalId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Jikan getAnimeFull failed", e)
                        null
                    }
                } else null
            }

            val jikanNewsDeferred = async {
                if (effectiveMalId != null) {
                    try {
                        JikanApiClient.getNews(effectiveMalId)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()
            }

            val jikanStreamingDeferred = async {
                if (effectiveMalId != null) {
                    try {
                        JikanApiClient.getStreaming(effectiveMalId)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else emptyList()
            }

            val wikiDeferred = async {
                val titleToSearch = anilistDetails?.englishTitle
                    ?: anilistDetails?.romajiTitle
                    ?: "Anime"
                WebDataEngine.searchWikipedia(titleToSearch)
            }

            val jikanDetails = jikanDeferred.await()
            val jikanNews = jikanNewsDeferred.await()
            val jikanStreaming = jikanStreamingDeferred.await()
            val wikiResult = wikiDeferred.await()

            if (anilistDetails == null && jikanDetails == null) {
                // Fallback to cache even if stale
                val fallbackCache = animeDao.getCachedAnime(animeId)
                if (fallbackCache != null) {
                    val details = deserializeDetails(fallbackCache.jsonDetails)
                    if (details != null) return@coroutineScope details.copy(isFromCache = true)
                }
                return@coroutineScope null
            }

            // Cross-check metadata & sources
            val officialWebsite = anilistDetails?.externalLinks
                ?.firstOrNull { it.first.contains("Official", ignoreCase = true) || it.first.contains("Site", ignoreCase = true) }
                ?.second

            val verifiedReleaseDate = WebDataEngine.verifyReleaseDate(
                anilistDate = anilistDetails?.startDate,
                jikanAiredString = jikanDetails?.airedString,
                officialSiteUrl = officialWebsite
            )

            val verifiedEpisodes = WebDataEngine.verifyEpisodeCount(
                anilistEpisodes = anilistDetails?.episodes,
                jikanEpisodes = jikanDetails?.episodes,
                status = anilistDetails?.status ?: jikanDetails?.status ?: "Unknown"
            )

            val verifiedScore = WebDataEngine.verifyScore(
                anilistScore = anilistDetails?.score,
                jikanScore = jikanDetails?.score
            )

            val verifiedStatus = WebDataEngine.verifyStatus(
                anilistStatus = anilistDetails?.status,
                jikanStatus = jikanDetails?.status
            )

            // Titles
            val chosenTitle = listOfNotNull(
                anilistDetails?.englishTitle,
                jikanDetails?.englishTitle,
                anilistDetails?.romajiTitle,
                jikanDetails?.title,
                anilistDetails?.nativeTitle
            ).firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) } ?: "Untitled"

            val englishTitle = listOfNotNull(anilistDetails?.englishTitle, jikanDetails?.englishTitle)
                .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
            val japaneseTitle = listOfNotNull(anilistDetails?.nativeTitle, jikanDetails?.japaneseTitle)
                .firstOrNull { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

            // Synopsis: prefer longer, more descriptive one
            val synopsis = when {
                !anilistDetails?.description.isNullOrBlank() && anilistDetails!!.description.length > 30 -> anilistDetails.description
                !jikanDetails?.synopsis.isNullOrBlank() -> jikanDetails!!.synopsis!!
                else -> wikiResult?.extract ?: "Information unavailable."
            }

            // Studios
            val studios = mutableListOf<StudioInfo>()
            if (!anilistDetails?.studios.isNullOrEmpty()) {
                studios.addAll(anilistDetails!!.studios)
            } else if (!jikanDetails?.studios.isNullOrEmpty()) {
                studios.addAll(jikanDetails!!.studios.map { StudioInfo(name = it, isAnimationStudio = true) })
            }

            // Streaming services
            val streamingServices = mutableListOf<StreamingService>()
            // From AniList external links
            anilistDetails?.externalLinks?.forEach { (site, url) ->
                if (site.contains("Crunchyroll", ignoreCase = true) ||
                    site.contains("Netflix", ignoreCase = true) ||
                    site.contains("Hulu", ignoreCase = true) ||
                    site.contains("Disney", ignoreCase = true) ||
                    site.contains("Prime", ignoreCase = true) ||
                    site.contains("HIDIVE", ignoreCase = true) ||
                    site.contains("Bilibili", ignoreCase = true)
                ) {
                    streamingServices.add(StreamingService(name = site, url = url))
                }
            }
            // From Jikan streaming
            jikanStreaming.forEach { js ->
                if (streamingServices.none { it.name.equals(js.name, ignoreCase = true) }) {
                    streamingServices.add(js)
                }
            }

            // Web updates / News
            val webUpdates = mutableListOf<WebUpdate>()
            webUpdates.addAll(jikanNews)
            if (wikiResult?.snippet != null) {
                webUpdates.add(
                    WebUpdate(
                        headline = "Wikipedia Encyclopedic Record",
                        source = "Wikipedia (English)",
                        url = wikiResult.pageUrl,
                        publicationDate = "Open Web",
                        summary = wikiResult.snippet,
                        category = UpdateCategory.REPORTED
                    )
                )
            }

            // Canonical Seasons & Franchise Timeline
            val relations = anilistDetails?.relations ?: emptyList()
            val format = anilistDetails?.format ?: jikanDetails?.type ?: "TV"
            val canonicalSeasons = SeasonEngine.extractCanonicalSeasons(
                currentAnimeId = animeId,
                currentAnimeTitle = chosenTitle,
                currentEpisodes = verifiedEpisodes.value,
                currentYear = anilistDetails?.seasonYear ?: jikanDetails?.year,
                currentFormat = format,
                relations = relations
            )

            val franchiseTimeline = SeasonEngine.organizeFranchiseTimeline(
                currentAnimeId = animeId,
                currentAnimeTitle = chosenTitle,
                currentYear = anilistDetails?.seasonYear ?: jikanDetails?.year,
                currentFormat = format,
                relations = relations
            )

            // Sources Summary
            val sourcesSummary = WebDataEngine.buildSourcesSummary(
                anilistId = animeId,
                malId = effectiveMalId,
                officialSiteUrl = officialWebsite,
                streamingUrls = streamingServices.map { it.name to it.url },
                wikipediaUrl = wikiResult?.pageUrl
            )

            // Trailer
            val trailerUrl = when {
                !anilistDetails?.trailerId.isNullOrBlank() && anilistDetails!!.trailerSite.equals("youtube", ignoreCase = true) ->
                    "https://www.youtube.com/watch?v=${anilistDetails.trailerId}"
                !jikanDetails?.trailerUrl.isNullOrBlank() -> jikanDetails!!.trailerUrl
                else -> null
            }

            // Synthesize Dub Information
            val dubInfo = DubEngine.analyzeDub(
                characters = anilistDetails?.characters ?: emptyList(),
                licensors = jikanDetails?.licensors ?: emptyList(),
                streamingServices = streamingServices,
                status = verifiedStatus.displayText,
                totalEpisodes = verifiedEpisodes.value,
                format = format,
                dubLanguagesFromAniList = anilistDetails?.dubLanguages ?: emptyList(),
                nextEpisode = anilistDetails?.nextEpisode,
                startDateYear = anilistDetails?.startDateYear,
                startDateMonth = anilistDetails?.startDateMonth,
                startDateDay = anilistDetails?.startDateDay,
                externalLinks = anilistDetails?.externalLinks ?: emptyList()
            )

            // Build Complete Episodes List including upcoming ones yet to release
            val completeEpisodes = buildCompleteEpisodeList(
                malId = effectiveMalId,
                anilistId = animeId,
                totalEpisodes = verifiedEpisodes.value,
                status = verifiedStatus.displayText,
                nextEpisode = anilistDetails?.nextEpisode,
                airingSchedule = anilistDetails?.airingSchedule,
                dubInfo = dubInfo
            )

            val finalDetails = AnimeDetails(
                id = animeId,
                anilistId = animeId,
                malId = effectiveMalId,
                title = chosenTitle,
                englishTitle = englishTitle,
                japaneseTitle = japaneseTitle,
                synopsis = synopsis,
                coverImageUrl = anilistDetails?.coverImage ?: jikanDetails?.posterUrl,
                bannerImageUrl = anilistDetails?.bannerImage,
                score = verifiedScore,
                status = verifiedStatus,
                format = format,
                episodeCount = verifiedEpisodes,
                durationMinutes = anilistDetails?.duration,
                releaseDate = verifiedReleaseDate,
                latestReleaseDate = anilistDetails?.endDate ?: jikanDetails?.airedString,
                season = anilistDetails?.season ?: jikanDetails?.season,
                year = anilistDetails?.seasonYear ?: jikanDetails?.year,
                sourceMaterial = anilistDetails?.source,
                ageRating = jikanDetails?.rating,
                genres = anilistDetails?.genres ?: jikanDetails?.genres ?: emptyList(),
                studios = studios,
                producers = jikanDetails?.producers ?: emptyList(),
                licensors = jikanDetails?.licensors ?: emptyList(),
                nextEpisode = anilistDetails?.nextEpisode,
                trailerUrl = trailerUrl,
                trailerSite = anilistDetails?.trailerSite ?: "YouTube",
                officialWebsite = officialWebsite,
                streamingServices = streamingServices,
                characters = anilistDetails?.characters ?: emptyList(),
                seasons = canonicalSeasons,
                franchiseTimeline = franchiseTimeline,
                webUpdates = webUpdates,
                sourcesList = sourcesSummary,
                lastUpdated = System.currentTimeMillis(),
                isFromCache = false,
                dubInfo = dubInfo,
                allEpisodes = completeEpisodes
            )

            // Cache to local Room database
            try {
                val json = serializeDetails(finalDetails)
                animeDao.insertCachedAnime(
                    CachedAnimeEntity(
                        id = animeId,
                        title = chosenTitle,
                        jsonDetails = json,
                        lastCachedAt = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache anime details", e)
            }

            finalDetails
        }
    }

    /**
     * Builds a comprehensive list of all episodes, including upcoming/unreleased episodes
     * with projected schedules, countdowns, and real-time dubbing indicators.
     */
    suspend fun buildCompleteEpisodeList(
        malId: Int?,
        anilistId: Int?,
        totalEpisodes: Int?,
        status: String?,
        nextEpisode: UpcomingEpisodeInfo?,
        airingSchedule: List<AiringScheduleNode>?,
        dubInfo: DubInfo?
    ): List<EpisodeItem> = withContext(Dispatchers.IO) {
        val fetchedEpisodes = mutableListOf<EpisodeItem>()

        // 1. Fetch available episodes from Jikan if malId is present
        if (malId != null && malId > 0) {
            try {
                val page1 = JikanApiClient.getEpisodes(malId, 1)
                fetchedEpisodes.addAll(page1.episodes)
                if (page1.hasNextPage && fetchedEpisodes.size <= 50) {
                    try {
                        val page2 = JikanApiClient.getEpisodes(malId, 2)
                        fetchedEpisodes.addAll(page2.episodes)
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not fetch Jikan episodes for malId $malId", e)
            }
        }

        val episodesByNumber = fetchedEpisodes.associateBy { it.episodeNumber }.toMutableMap()

        // 2. Identify episode bounds and airing states
        val nowEpoch = System.currentTimeMillis() / 1000L
        val normalizedStatus = status?.uppercase() ?: "UNKNOWN"
        val isCompleted = normalizedStatus.contains("FINISHED") || normalizedStatus.contains("COMPLETED")
        val isNotStarted = normalizedStatus.contains("NOT_YET_RELEASED") || normalizedStatus.contains("UPCOMING")

        val scheduleMap = airingSchedule?.associateBy { it.episode } ?: emptyMap()
        val nextEpNum = nextEpisode?.episodeNumber ?: if (isNotStarted) 1 else null

        val maxFetchedNum = if (episodesByNumber.isNotEmpty()) episodesByNumber.keys.maxOrNull() ?: 0 else 0
        val targetCount = maxOf(
            totalEpisodes ?: 0,
            scheduleMap.keys.maxOrNull() ?: 0,
            nextEpNum ?: 0,
            maxFetchedNum,
            if (isNotStarted) (totalEpisodes ?: 12) else 0
        )

        val finalEpisodes = mutableListOf<EpisodeItem>()
        val countToGenerate = if (targetCount > 0) targetCount else maxOf(maxFetchedNum, 1)

        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Tokyo")
        }

        for (epNum in 1..countToGenerate) {
            val existing = episodesByNumber[epNum]
            val scheduleNode = scheduleMap[epNum]

            val isAired: Boolean
            val airingEpoch: Long?
            val timeUntilSeconds: Long?
            val airDateString: String?

            if (isCompleted) {
                isAired = true
                airingEpoch = null
                timeUntilSeconds = null
                airDateString = existing?.airDate ?: "Broadcasted"
            } else if (isNotStarted) {
                isAired = false
                if (scheduleNode != null) {
                    airingEpoch = scheduleNode.airingAt
                    timeUntilSeconds = scheduleNode.timeUntilAiring
                    airDateString = "Scheduled: " + sdf.format(Date(scheduleNode.airingAt * 1000L))
                } else if (nextEpisode != null && nextEpNum != null && epNum >= nextEpNum) {
                    val offsetWeeks = epNum - nextEpNum
                    val projectedEpoch = nextEpisode.airingAtEpochSeconds + (offsetWeeks * 7L * 86400L)
                    airingEpoch = projectedEpoch
                    timeUntilSeconds = maxOf(0L, projectedEpoch - nowEpoch)
                    airDateString = "Expected: " + sdf.format(Date(projectedEpoch * 1000L))
                } else {
                    airingEpoch = null
                    timeUntilSeconds = null
                    airDateString = "Upcoming"
                }
            } else {
                // Releasing / Ongoing
                if (nextEpNum != null && epNum >= nextEpNum) {
                    isAired = false
                    if (scheduleNode != null) {
                        airingEpoch = scheduleNode.airingAt
                        timeUntilSeconds = scheduleNode.timeUntilAiring
                        airDateString = "Scheduled: " + sdf.format(Date(scheduleNode.airingAt * 1000L))
                    } else if (nextEpisode != null) {
                        val offsetWeeks = epNum - nextEpNum
                        val projectedEpoch = nextEpisode.airingAtEpochSeconds + (offsetWeeks * 7L * 86400L)
                        airingEpoch = projectedEpoch
                        timeUntilSeconds = maxOf(0L, projectedEpoch - nowEpoch)
                        airDateString = "Expected: " + sdf.format(Date(projectedEpoch * 1000L))
                    } else {
                        airingEpoch = null
                        timeUntilSeconds = null
                        airDateString = "Scheduled"
                    }
                } else if (scheduleNode != null && scheduleNode.airingAt > nowEpoch) {
                    isAired = false
                    airingEpoch = scheduleNode.airingAt
                    timeUntilSeconds = scheduleNode.timeUntilAiring
                    airDateString = "Scheduled: " + sdf.format(Date(scheduleNode.airingAt * 1000L))
                } else {
                    isAired = true
                    airingEpoch = null
                    timeUntilSeconds = null
                    airDateString = existing?.airDate ?: "Broadcasted"
                }
            }

            val epTitle = existing?.title ?: if (isAired) "Episode $epNum" else "Episode $epNum (Upcoming)"

            finalEpisodes.add(
                EpisodeItem(
                    episodeNumber = epNum,
                    title = epTitle,
                    airDate = airDateString,
                    durationMinutes = existing?.durationMinutes,
                    synopsis = existing?.synopsis,
                    isFiller = existing?.isFiller ?: false,
                    isAired = isAired,
                    airingAtEpochSeconds = airingEpoch,
                    timeUntilAiringSeconds = timeUntilSeconds
                )
            )
        }

        // 3. Decorate with Dub Information
        if (dubInfo != null) {
            DubEngine.decorateEpisodesWithDub(finalEpisodes, dubInfo, status)
        } else {
            finalEpisodes
        }
    }

    /**
     * Loads paginated episodes for an anime using Jikan API.
     */
    suspend fun getEpisodes(malId: Int, page: Int = 1): PaginatedEpisodes = withContext(Dispatchers.IO) {
        try {
            JikanApiClient.getEpisodes(malId, page)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get episodes for malId=$malId page=$page", e)
            PaginatedEpisodes(emptyList(), page, 1, false)
        }
    }

    // Favorites Room Flow
    val allFavorites: Flow<List<FavoriteAnimeEntity>> = animeDao.getAllFavorites()

    suspend fun isFavorite(id: Int): Boolean = withContext(Dispatchers.IO) {
        animeDao.isFavorite(id)
    }

    suspend fun toggleFavorite(details: AnimeDetails): Boolean = withContext(Dispatchers.IO) {
        val isFav = animeDao.isFavorite(details.id)
        val added = if (isFav) {
            animeDao.removeFavorite(details.id)
            false
        } else {
            animeDao.addFavorite(
                FavoriteAnimeEntity(
                    id = details.id,
                    anilistId = details.anilistId,
                    malId = details.malId,
                    title = details.title,
                    englishTitle = details.englishTitle,
                    coverImageUrl = details.coverImageUrl,
                    bannerImageUrl = details.bannerImageUrl,
                    score = details.score.value,
                    status = details.status.displayText,
                    format = details.format,
                    year = details.year,
                    genres = details.genres.joinToString(",")
                )
            )
            true
        }
        try {
            if (backupManager.isLoggedIn()) {
                backupManager.triggerAutoBackup()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto-backup trigger on favorite change skipped", e)
        }
        added
    }

    // Search History Room Flow
    val searchHistory: Flow<List<String>> = animeDao.getSearchHistory().map { list ->
        list.map { it.query }
    }

    suspend fun deleteSearchHistoryItem(query: String) = withContext(Dispatchers.IO) {
        animeDao.deleteSearchQuery(query)
        try {
            if (backupManager.isLoggedIn()) {
                backupManager.triggerAutoBackup()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto-backup trigger on delete query skipped", e)
        }
    }

    suspend fun clearSearchHistory() = withContext(Dispatchers.IO) {
        animeDao.clearSearchHistory()
        try {
            if (backupManager.isLoggedIn()) {
                backupManager.triggerAutoBackup()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Auto-backup trigger on clear search history skipped", e)
        }
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        animeDao.clearAllCache()
    }

    private fun normalizeTitle(title: String): String {
        return title.lowercase()
            .replace(":", "")
            .replace("-", " ")
            .replace("  ", " ")
            .trim()
    }

    // JSON serialization for Room offline cache
    private fun serializeDetails(details: AnimeDetails): String {
        val json = JSONObject()
        json.put("id", details.id)
        json.put("anilistId", details.anilistId ?: JSONObject.NULL)
        json.put("malId", details.malId ?: JSONObject.NULL)
        json.put("title", details.title)
        json.put("englishTitle", details.englishTitle ?: JSONObject.NULL)
        json.put("japaneseTitle", details.japaneseTitle ?: JSONObject.NULL)
        json.put("synopsis", details.synopsis)
        json.put("coverImageUrl", details.coverImageUrl ?: JSONObject.NULL)
        json.put("bannerImageUrl", details.bannerImageUrl ?: JSONObject.NULL)
        json.put("format", details.format)
        json.put("durationMinutes", details.durationMinutes ?: JSONObject.NULL)
        json.put("season", details.season ?: JSONObject.NULL)
        json.put("year", details.year ?: JSONObject.NULL)
        json.put("sourceMaterial", details.sourceMaterial ?: JSONObject.NULL)
        json.put("ageRating", details.ageRating ?: JSONObject.NULL)
        json.put("officialWebsite", details.officialWebsite ?: JSONObject.NULL)
        json.put("trailerUrl", details.trailerUrl ?: JSONObject.NULL)
        json.put("lastUpdated", details.lastUpdated)

        // Verified facts
        json.put("score_val", details.score.value ?: JSONObject.NULL)
        json.put("score_text", details.score.displayText)
        json.put("score_status", details.score.status.name)

        json.put("status_val", details.status.value ?: JSONObject.NULL)
        json.put("status_text", details.status.displayText)
        json.put("status_status", details.status.status.name)

        json.put("episodes_val", details.episodeCount.value ?: JSONObject.NULL)
        json.put("episodes_text", details.episodeCount.displayText)
        json.put("episodes_status", details.episodeCount.status.name)

        json.put("release_val", details.releaseDate.value ?: JSONObject.NULL)
        json.put("release_text", details.releaseDate.displayText)
        json.put("release_status", details.releaseDate.status.name)

        // Upcoming Episode
        if (details.nextEpisode != null) {
            val up = JSONObject().apply {
                put("episodeNumber", details.nextEpisode.episodeNumber)
                put("timeUntilAiringSeconds", details.nextEpisode.timeUntilAiringSeconds)
                put("airingAtEpochSeconds", details.nextEpisode.airingAtEpochSeconds)
                put("formattedDate", details.nextEpisode.formattedDate)
                put("isConfirmed", details.nextEpisode.isConfirmed)
            }
            json.put("nextEpisode", up)
        }

        // Genres
        val genreArr = JSONArray()
        details.genres.forEach { genreArr.put(it) }
        json.put("genres", genreArr)

        // Studios
        val studioArr = JSONArray()
        details.studios.forEach {
            val s = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("isAnimationStudio", it.isAnimationStudio)
                put("siteUrl", it.siteUrl ?: JSONObject.NULL)
            }
            studioArr.put(s)
        }
        json.put("studios", studioArr)

        // Streaming
        val streamArr = JSONArray()
        details.streamingServices.forEach {
            val s = JSONObject().apply {
                put("name", it.name)
                put("url", it.url)
                put("regionNotice", it.regionNotice)
            }
            streamArr.put(s)
        }
        json.put("streamingServices", streamArr)

        // Characters
        val charArr = JSONArray()
        details.characters.forEach {
            val c = JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("nativeName", it.nativeName ?: JSONObject.NULL)
                put("role", it.role)
                put("imageUrl", it.imageUrl ?: JSONObject.NULL)
                put("voiceActorName", it.voiceActorName ?: JSONObject.NULL)
                put("voiceActorNative", it.voiceActorNative ?: JSONObject.NULL)
                put("voiceActorImageUrl", it.voiceActorImageUrl ?: JSONObject.NULL)
                put("englishVoiceActorName", it.englishVoiceActorName ?: JSONObject.NULL)
                put("englishVoiceActorImageUrl", it.englishVoiceActorImageUrl ?: JSONObject.NULL)
            }
            charArr.put(c)
        }
        json.put("characters", charArr)

        // Dub Information
        if (details.dubInfo != null) {
            val d = JSONObject().apply {
                put("isDubAvailable", details.dubInfo.isDubAvailable)
                put("dubStatus", details.dubInfo.dubStatus)
                put("primaryDubLanguage", details.dubInfo.primaryDubLanguage)
                val lArr = JSONArray()
                details.dubInfo.availableLanguages.forEach { lArr.put(it) }
                put("availableLanguages", lArr)
                put("dubLicensor", details.dubInfo.dubLicensor ?: JSONObject.NULL)
                put("dubScheduleDetails", details.dubInfo.dubScheduleDetails ?: JSONObject.NULL)
                put("totalDubbedEpisodes", details.dubInfo.totalDubbedEpisodes ?: JSONObject.NULL)
            }
            json.put("dubInfo", d)
        }

        // All Episodes
        val epArr = JSONArray()
        details.allEpisodes.forEach { ep ->
            val eObj = JSONObject().apply {
                put("episodeNumber", ep.episodeNumber)
                put("title", ep.title)
                put("airDate", ep.airDate ?: JSONObject.NULL)
                put("durationMinutes", ep.durationMinutes ?: JSONObject.NULL)
                put("synopsis", ep.synopsis ?: JSONObject.NULL)
                put("isFiller", ep.isFiller)
                put("isAired", ep.isAired)
                put("airingAtEpochSeconds", ep.airingAtEpochSeconds ?: JSONObject.NULL)
                put("timeUntilAiringSeconds", ep.timeUntilAiringSeconds ?: JSONObject.NULL)
                put("hasDub", ep.hasDub)
                put("dubStatusText", ep.dubStatusText ?: JSONObject.NULL)
            }
            epArr.put(eObj)
        }
        json.put("allEpisodes", epArr)

        // Seasons
        val seasonArr = JSONArray()
        details.seasons.forEach {
            val s = JSONObject().apply {
                put("seasonNumber", it.seasonNumber)
                put("title", it.title)
                put("episodeCount", it.episodeCount ?: JSONObject.NULL)
                put("year", it.year ?: JSONObject.NULL)
                put("animeId", it.animeId)
            }
            seasonArr.put(s)
        }
        json.put("seasons", seasonArr)

        // Franchise Timeline
        val timeArr = JSONArray()
        details.franchiseTimeline.forEach {
            val t = JSONObject().apply {
                put("id", it.id)
                put("malId", it.malId ?: JSONObject.NULL)
                put("title", it.title)
                put("year", it.year ?: JSONObject.NULL)
                put("format", it.format)
                put("relationType", it.relationType)
                put("coverImageUrl", it.coverImageUrl ?: JSONObject.NULL)
            }
            timeArr.put(t)
        }
        json.put("franchiseTimeline", timeArr)

        // Sources
        val srcArr = JSONArray()
        details.sourcesList.forEach {
            val src = JSONObject().apply {
                put("name", it.name)
                put("url", it.url ?: JSONObject.NULL)
                put("lastChecked", it.lastChecked)
                put("status", it.status.name)
                put("details", it.details)
            }
            srcArr.put(src)
        }
        json.put("sourcesList", srcArr)

        // Web Updates
        val updArr = JSONArray()
        details.webUpdates.forEach {
            val u = JSONObject().apply {
                put("headline", it.headline)
                put("source", it.source)
                put("url", it.url ?: JSONObject.NULL)
                put("publicationDate", it.publicationDate)
                put("summary", it.summary)
                put("category", it.category.name)
            }
            updArr.put(u)
        }
        json.put("webUpdates", updArr)

        return json.toString()
    }

    private fun deserializeDetails(jsonString: String): AnimeDetails? {
        return try {
            val json = JSONObject(jsonString)

            val genres = mutableListOf<String>()
            val gArr = json.optJSONArray("genres")
            if (gArr != null) {
                for (i in 0 until gArr.length()) genres.add(gArr.getString(i))
            }

            val studios = mutableListOf<StudioInfo>()
            val sArr = json.optJSONArray("studios")
            if (sArr != null) {
                for (i in 0 until sArr.length()) {
                    val s = sArr.getJSONObject(i)
                    studios.add(
                        StudioInfo(
                            id = s.optInt("id"),
                            name = s.getString("name"),
                            isAnimationStudio = s.optBoolean("isAnimationStudio", true),
                            siteUrl = if (s.isNull("siteUrl")) null else s.optString("siteUrl")
                        )
                    )
                }
            }

            val streaming = mutableListOf<StreamingService>()
            val stArr = json.optJSONArray("streamingServices")
            if (stArr != null) {
                for (i in 0 until stArr.length()) {
                    val s = stArr.getJSONObject(i)
                    streaming.add(
                        StreamingService(
                            name = s.getString("name"),
                            url = s.getString("url"),
                            regionNotice = s.optString("regionNotice", "Regional availability may vary")
                        )
                    )
                }
            }

            val characters = mutableListOf<CharacterCast>()
            val cArr = json.optJSONArray("characters")
            if (cArr != null) {
                for (i in 0 until cArr.length()) {
                    val c = cArr.getJSONObject(i)
                    characters.add(
                        CharacterCast(
                            id = c.optInt("id"),
                            name = c.getString("name"),
                            nativeName = if (c.isNull("nativeName")) null else c.optString("nativeName"),
                            role = c.optString("role", "MAIN"),
                            imageUrl = if (c.isNull("imageUrl")) null else c.optString("imageUrl"),
                            voiceActorName = if (c.isNull("voiceActorName")) null else c.optString("voiceActorName"),
                            voiceActorNative = if (c.isNull("voiceActorNative")) null else c.optString("voiceActorNative"),
                            voiceActorImageUrl = if (c.isNull("voiceActorImageUrl")) null else c.optString("voiceActorImageUrl"),
                            englishVoiceActorName = if (c.isNull("englishVoiceActorName")) null else c.optString("englishVoiceActorName"),
                            englishVoiceActorImageUrl = if (c.isNull("englishVoiceActorImageUrl")) null else c.optString("englishVoiceActorImageUrl")
                        )
                    )
                }
            }

            val seasons = mutableListOf<SeasonInfo>()
            val seArr = json.optJSONArray("seasons")
            if (seArr != null) {
                for (i in 0 until seArr.length()) {
                    val se = seArr.getJSONObject(i)
                    seasons.add(
                        SeasonInfo(
                            seasonNumber = se.getInt("seasonNumber"),
                            title = se.getString("title"),
                            episodeCount = if (se.isNull("episodeCount")) null else se.optInt("episodeCount"),
                            year = if (se.isNull("year")) null else se.optInt("year"),
                            animeId = se.getInt("animeId")
                        )
                    )
                }
            }

            val timeline = mutableListOf<TimelineEntry>()
            val tArr = json.optJSONArray("franchiseTimeline")
            if (tArr != null) {
                for (i in 0 until tArr.length()) {
                    val t = tArr.getJSONObject(i)
                    timeline.add(
                        TimelineEntry(
                            id = t.getInt("id"),
                            malId = if (t.isNull("malId")) null else t.optInt("malId"),
                            title = t.getString("title"),
                            year = if (t.isNull("year")) null else t.optInt("year"),
                            format = t.getString("format"),
                            relationType = t.getString("relationType"),
                            coverImageUrl = if (t.isNull("coverImageUrl")) null else t.optString("coverImageUrl")
                        )
                    )
                }
            }

            val sources = mutableListOf<SourceSummary>()
            val srcArr = json.optJSONArray("sourcesList")
            if (srcArr != null) {
                for (i in 0 until srcArr.length()) {
                    val src = srcArr.getJSONObject(i)
                    sources.add(
                        SourceSummary(
                            name = src.getString("name"),
                            url = if (src.isNull("url")) null else src.optString("url"),
                            lastChecked = src.getLong("lastChecked"),
                            status = VerificationStatus.valueOf(src.getString("status")),
                            details = src.getString("details")
                        )
                    )
                }
            }

            val updates = mutableListOf<WebUpdate>()
            val uArr = json.optJSONArray("webUpdates")
            if (uArr != null) {
                for (i in 0 until uArr.length()) {
                    val u = uArr.getJSONObject(i)
                    updates.add(
                        WebUpdate(
                            headline = u.getString("headline"),
                            source = u.getString("source"),
                            url = if (u.isNull("url")) null else u.optString("url"),
                            publicationDate = u.getString("publicationDate"),
                            summary = u.getString("summary"),
                            category = UpdateCategory.valueOf(u.getString("category"))
                        )
                    )
                }
            }

            var nextEp: UpcomingEpisodeInfo? = null
            if (json.has("nextEpisode") && !json.isNull("nextEpisode")) {
                val n = json.getJSONObject("nextEpisode")
                nextEp = UpcomingEpisodeInfo(
                    episodeNumber = n.getInt("episodeNumber"),
                    timeUntilAiringSeconds = n.getLong("timeUntilAiringSeconds"),
                    airingAtEpochSeconds = n.getLong("airingAtEpochSeconds"),
                    formattedDate = n.getString("formattedDate"),
                    isConfirmed = n.getBoolean("isConfirmed")
                )
            }

            val scoreVal = if (json.isNull("score_val")) null else json.optDouble("score_val")
            val scoreText = json.optString("score_text", "N/A")
            val scoreStatus = VerificationStatus.valueOf(json.optString("score_status", "SINGLE_SOURCE"))

            val statusVal = if (json.isNull("status_val")) null else json.optString("status_val")
            val statusText = json.optString("status_text", "Unknown")
            val statusStatus = VerificationStatus.valueOf(json.optString("status_status", "SINGLE_SOURCE"))

            val epVal = if (json.isNull("episodes_val")) null else json.optInt("episodes_val")
            val epText = json.optString("episodes_text", "Unknown")
            val epStatus = VerificationStatus.valueOf(json.optString("episodes_status", "SINGLE_SOURCE"))

            val relVal = if (json.isNull("release_val")) null else json.optString("release_val")
            val relText = json.optString("release_text", "Unknown")
            val relStatus = VerificationStatus.valueOf(json.optString("release_status", "SINGLE_SOURCE"))

            var dubInfo: DubInfo? = null
            if (json.has("dubInfo") && !json.isNull("dubInfo")) {
                val d = json.getJSONObject("dubInfo")
                val langs = mutableListOf<String>()
                val lArr = d.optJSONArray("availableLanguages")
                if (lArr != null) {
                    for (i in 0 until lArr.length()) langs.add(lArr.getString(i))
                }
                dubInfo = DubInfo(
                    isDubAvailable = d.optBoolean("isDubAvailable", false),
                    dubStatus = d.optString("dubStatus", "Sub Only"),
                    primaryDubLanguage = d.optString("primaryDubLanguage", "English"),
                    availableLanguages = langs,
                    dubLicensor = if (d.isNull("dubLicensor")) null else d.optString("dubLicensor"),
                    dubScheduleDetails = if (d.isNull("dubScheduleDetails")) null else d.optString("dubScheduleDetails"),
                    totalDubbedEpisodes = if (d.isNull("totalDubbedEpisodes")) null else d.optInt("totalDubbedEpisodes")
                )
            }

            val allEpisodesList = mutableListOf<EpisodeItem>()
            val epArr = json.optJSONArray("allEpisodes")
            if (epArr != null) {
                for (i in 0 until epArr.length()) {
                    val epObj = epArr.getJSONObject(i)
                    allEpisodesList.add(
                        EpisodeItem(
                            episodeNumber = epObj.getInt("episodeNumber"),
                            title = epObj.getString("title"),
                            airDate = if (epObj.isNull("airDate")) null else epObj.optString("airDate"),
                            durationMinutes = if (epObj.isNull("durationMinutes")) null else epObj.optInt("durationMinutes"),
                            synopsis = if (epObj.isNull("synopsis")) null else epObj.optString("synopsis"),
                            isFiller = epObj.optBoolean("isFiller", false),
                            isAired = epObj.optBoolean("isAired", true),
                            airingAtEpochSeconds = if (epObj.isNull("airingAtEpochSeconds")) null else epObj.optLong("airingAtEpochSeconds"),
                            timeUntilAiringSeconds = if (epObj.isNull("timeUntilAiringSeconds")) null else epObj.optLong("timeUntilAiringSeconds"),
                            hasDub = epObj.optBoolean("hasDub", false),
                            dubStatusText = if (epObj.isNull("dubStatusText")) null else epObj.optString("dubStatusText")
                        )
                    )
                }
            }

            AnimeDetails(
                id = json.getInt("id"),
                anilistId = if (json.isNull("anilistId")) null else json.optInt("anilistId"),
                malId = if (json.isNull("malId")) null else json.optInt("malId"),
                title = json.getString("title"),
                englishTitle = if (json.isNull("englishTitle")) null else json.optString("englishTitle"),
                japaneseTitle = if (json.isNull("japaneseTitle")) null else json.optString("japaneseTitle"),
                synopsis = json.getString("synopsis"),
                coverImageUrl = if (json.isNull("coverImageUrl")) null else json.optString("coverImageUrl"),
                bannerImageUrl = if (json.isNull("bannerImageUrl")) null else json.optString("bannerImageUrl"),
                score = VerifiedFact(scoreVal, scoreText, scoreStatus),
                status = VerifiedFact(statusVal, statusText, statusStatus),
                format = json.getString("format"),
                episodeCount = VerifiedFact(epVal, epText, epStatus),
                durationMinutes = if (json.isNull("durationMinutes")) null else json.optInt("durationMinutes"),
                releaseDate = VerifiedFact(relVal, relText, relStatus),
                season = if (json.isNull("season")) null else json.optString("season"),
                year = if (json.isNull("year")) null else json.optInt("year"),
                sourceMaterial = if (json.isNull("sourceMaterial")) null else json.optString("sourceMaterial"),
                ageRating = if (json.isNull("ageRating")) null else json.optString("ageRating"),
                genres = genres,
                studios = studios,
                producers = emptyList(),
                licensors = emptyList(),
                nextEpisode = nextEp,
                trailerUrl = if (json.isNull("trailerUrl")) null else json.optString("trailerUrl"),
                officialWebsite = if (json.isNull("officialWebsite")) null else json.optString("officialWebsite"),
                streamingServices = streaming,
                characters = characters,
                seasons = seasons,
                franchiseTimeline = timeline,
                webUpdates = updates,
                sourcesList = sources,
                lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis()),
                isFromCache = true,
                dubInfo = dubInfo,
                allEpisodes = allEpisodesList
            )
        } catch (e: Exception) {
            Log.e(TAG, "Deserialization error", e)
            null
        }
    }
}
