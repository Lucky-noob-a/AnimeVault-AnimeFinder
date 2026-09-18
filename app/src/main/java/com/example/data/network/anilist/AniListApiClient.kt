package com.example.data.network.anilist

import android.util.Log
import com.example.data.model.AnimeSummary
import com.example.data.model.CharacterCast
import com.example.data.model.StudioInfo
import com.example.data.model.TimelineEntry
import com.example.data.model.UpcomingEpisodeInfo
import com.example.data.model.VerificationStatus
import com.example.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class AiringScheduleNode(
    val id: Int,
    val episode: Int,
    val airingAt: Long,
    val timeUntilAiring: Long
)

data class AniListMediaDetails(
    val id: Int,
    val malId: Int?,
    val romajiTitle: String,
    val englishTitle: String?,
    val nativeTitle: String?,
    val description: String,
    val status: String,
    val format: String,
    val episodes: Int?,
    val duration: Int?,
    val startDate: String?,
    val endDate: String?,
    val season: String?,
    val seasonYear: Int?,
    val source: String?,
    val score: Double?,
    val coverImage: String?,
    val bannerImage: String?,
    val genres: List<String>,
    val studios: List<StudioInfo>,
    val nextEpisode: UpcomingEpisodeInfo?,
    val trailerId: String?,
    val trailerSite: String?,
    val externalLinks: List<Pair<String, String>>, // (site, url)
    val relations: List<TimelineEntry>,
    val characters: List<CharacterCast>,
    val airingSchedule: List<AiringScheduleNode> = emptyList(),
    val dubLanguages: List<String> = emptyList()
)

object AniListApiClient {
    private const val TAG = "AniListApiClient"
    private const val ANILIST_URL = "https://graphql.anilist.co"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun executeGraphQL(query: String, variables: JSONObject = JSONObject()): JSONObject? = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        val request = Request.Builder()
            .url(ANILIST_URL)
            .post(payload.toString().toRequestBody(JSON_MEDIA_TYPE))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build()

        try {
            NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "AniList request failed code=${response.code}")
                    return@withContext null
                }
                val bodyString = response.body?.string() ?: return@withContext null
                JSONObject(bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "AniList query error", e)
            null
        }
    }

    suspend fun searchAnime(searchQuery: String, page: Int = 1, perPage: Int = 20): List<AnimeSummary> = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(search: ${'$'}search, type: ANIME, sort: [SEARCH_MATCH, POPULARITY_DESC]) {
                  id
                  idMal
                  title {
                    romaji
                    english
                    native
                  }
                  coverImage {
                    large
                    medium
                  }
                  bannerImage
                  averageScore
                  status
                  format
                  episodes
                  seasonYear
                  genres
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("search", searchQuery)
            put("page", page)
            put("perPage", perPage)
        }

        val json = executeGraphQL(query, variables) ?: return@withContext emptyList()
        parseMediaList(json.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media"))
    }

    suspend fun getHomeMediaSections(): Map<String, List<AnimeSummary>> = withContext(Dispatchers.IO) {
        val query = """
            query {
              trending: Page(page: 1, perPage: 10) {
                media(type: ANIME, sort: TRENDING_DESC) {
                  id idMal title { romaji english native } coverImage { large medium } bannerImage averageScore status format episodes seasonYear genres
                }
              }
              airing: Page(page: 1, perPage: 10) {
                media(type: ANIME, status: RELEASING, sort: POPULARITY_DESC) {
                  id idMal title { romaji english native } coverImage { large medium } bannerImage averageScore status format episodes seasonYear genres
                }
              }
              popular: Page(page: 1, perPage: 10) {
                media(type: ANIME, sort: POPULARITY_DESC) {
                  id idMal title { romaji english native } coverImage { large medium } bannerImage averageScore status format episodes seasonYear genres
                }
              }
              upcoming: Page(page: 1, perPage: 10) {
                media(type: ANIME, status: NOT_YET_RELEASED, sort: POPULARITY_DESC) {
                  id idMal title { romaji english native } coverImage { large medium } bannerImage averageScore status format episodes seasonYear genres
                }
              }
            }
        """.trimIndent()

        val json = executeGraphQL(query) ?: return@withContext emptyMap()
        val data = json.optJSONObject("data") ?: return@withContext emptyMap()

        mapOf(
            "trending" to parseMediaList(data.optJSONObject("trending")?.optJSONArray("media")),
            "airing" to parseMediaList(data.optJSONObject("airing")?.optJSONArray("media")),
            "popular" to parseMediaList(data.optJSONObject("popular")?.optJSONArray("media")),
            "upcoming" to parseMediaList(data.optJSONObject("upcoming")?.optJSONArray("media"))
        )
    }

    suspend fun getMediaDetails(id: Int): AniListMediaDetails? = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}id: Int) {
              Media(id: ${'$'}id, type: ANIME) {
                id
                idMal
                title {
                  romaji
                  english
                  native
                }
                description(asHtml: false)
                status
                format
                episodes
                duration
                startDate { year month day }
                endDate { year month day }
                season
                seasonYear
                source
                averageScore
                coverImage {
                  extraLarge
                  large
                  medium
                }
                bannerImage
                genres
                studios(isMain: true) {
                  edges {
                    isMain
                    node {
                      id
                      name
                      isAnimationStudio
                      siteUrl
                    }
                  }
                }
                nextAiringEpisode {
                  episode
                  timeUntilAiring
                  airingAt
                }
                airingSchedule(perPage: 50) {
                  nodes {
                    id
                    episode
                    airingAt
                    timeUntilAiring
                  }
                }
                trailer {
                  id
                  site
                  thumbnail
                }
                externalLinks {
                  id
                  url
                  site
                  type
                }
                relations {
                  edges {
                    relationType
                    node {
                      id
                      idMal
                      title {
                        romaji
                        english
                      }
                      format
                      status
                      seasonYear
                      coverImage {
                        large
                        medium
                      }
                    }
                  }
                }
                characters(sort: [ROLE, RELEVANCE], perPage: 25) {
                  edges {
                    role
                    node {
                      id
                      name {
                        full
                        native
                      }
                      image {
                        large
                        medium
                      }
                    }
                    voiceActors {
                      id
                      name {
                        full
                        native
                      }
                      image {
                        medium
                      }
                      languageV2
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("id", id) }
        val json = executeGraphQL(query, variables) ?: return@withContext null
        val media = json.optJSONObject("data")?.optJSONObject("Media") ?: return@withContext null
        parseMediaDetails(media)
    }

    suspend fun getMediaDetailsBySearch(titleQuery: String): AniListMediaDetails? = withContext(Dispatchers.IO) {
        val query = """
            query (${'$'}search: String) {
              Media(search: ${'$'}search, type: ANIME) {
                id
                idMal
                title {
                  romaji
                  english
                  native
                }
                description(asHtml: false)
                status
                format
                episodes
                duration
                startDate { year month day }
                endDate { year month day }
                season
                seasonYear
                source
                averageScore
                coverImage {
                  extraLarge
                  large
                  medium
                }
                bannerImage
                genres
                studios(isMain: true) {
                  edges {
                    isMain
                    node {
                      id
                      name
                      isAnimationStudio
                      siteUrl
                    }
                  }
                }
                nextAiringEpisode {
                  episode
                  timeUntilAiring
                  airingAt
                }
                airingSchedule(perPage: 50) {
                  nodes {
                    id
                    episode
                    airingAt
                    timeUntilAiring
                  }
                }
                trailer {
                  id
                  site
                  thumbnail
                }
                externalLinks {
                  id
                  url
                  site
                  type
                }
                relations {
                  edges {
                    relationType
                    node {
                      id
                      idMal
                      title {
                        romaji
                        english
                      }
                      format
                      status
                      seasonYear
                      coverImage {
                        large
                        medium
                      }
                    }
                  }
                }
                characters(sort: [ROLE, RELEVANCE], perPage: 25) {
                  edges {
                    role
                    node {
                      id
                      name {
                        full
                        native
                      }
                      image {
                        large
                        medium
                      }
                    }
                    voiceActors {
                      id
                      name {
                        full
                        native
                      }
                      image {
                        medium
                      }
                      languageV2
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("search", titleQuery) }
        val json = executeGraphQL(query, variables) ?: return@withContext null
        val media = json.optJSONObject("data")?.optJSONObject("Media") ?: return@withContext null
        parseMediaDetails(media)
    }

    private fun parseMediaList(array: JSONArray?): List<AnimeSummary> {
        if (array == null) return emptyList()
        val list = mutableListOf<AnimeSummary>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val titleObj = item.optJSONObject("title")
            val romaji = titleObj?.optString("romaji", "") ?: ""
            val english = titleObj?.optString("english", null)
            val native = titleObj?.optString("native", null)
            val chosenTitle = when {
                !english.isNullOrBlank() -> english
                romaji.isNotBlank() -> romaji
                else -> native ?: "Untitled"
            }

            val coverObj = item.optJSONObject("coverImage")
            val coverUrl = coverObj?.optString("large") ?: coverObj?.optString("medium")
            val bannerUrl = item.optString("bannerImage", null)

            val avgScore = if (item.has("averageScore") && !item.isNull("averageScore")) {
                item.optDouble("averageScore") / 10.0 // convert 0-100 to 0-10
            } else null

            val genreArray = item.optJSONArray("genres")
            val genres = mutableListOf<String>()
            if (genreArray != null) {
                for (g in 0 until genreArray.length()) {
                    genres.add(genreArray.optString(g))
                }
            }

            list.add(
                AnimeSummary(
                    id = item.optInt("id"),
                    anilistId = item.optInt("id"),
                    malId = if (item.has("idMal") && !item.isNull("idMal")) item.optInt("idMal") else null,
                    title = chosenTitle,
                    englishTitle = english,
                    japaneseTitle = native,
                    coverImageUrl = coverUrl,
                    bannerImageUrl = bannerUrl,
                    score = avgScore,
                    status = formatStatus(item.optString("status", "UNKNOWN")),
                    format = item.optString("format", "TV"),
                    episodes = if (item.has("episodes") && !item.isNull("episodes")) item.optInt("episodes") else null,
                    seasonYear = if (item.has("seasonYear") && !item.isNull("seasonYear")) item.optInt("seasonYear") else null,
                    genres = genres,
                    verificationStatus = VerificationStatus.VERIFIED
                )
            )
        }
        return list
    }

    private fun parseMediaDetails(media: JSONObject): AniListMediaDetails {
        val titleObj = media.optJSONObject("title")
        val romaji = titleObj?.optString("romaji", "") ?: ""
        val english = titleObj?.optString("english", null)
        val native = titleObj?.optString("native", null)

        val desc = media.optString("description", "No synopsis available.")
            .replace("<br>", "\n")
            .replace("<i>", "")
            .replace("</i>", "")
            .replace("<b>", "")
            .replace("</b>", "")

        val coverObj = media.optJSONObject("coverImage")
        val coverUrl = coverObj?.optString("extraLarge") ?: coverObj?.optString("large") ?: coverObj?.optString("medium")
        val bannerUrl = media.optString("bannerImage", null)

        val score = if (media.has("averageScore") && !media.isNull("averageScore")) {
            media.optDouble("averageScore") / 10.0
        } else null

        val startObj = media.optJSONObject("startDate")
        val startDateStr = formatDate(startObj)

        val endObj = media.optJSONObject("endDate")
        val endDateStr = formatDate(endObj)

        val genreArray = media.optJSONArray("genres")
        val genres = mutableListOf<String>()
        if (genreArray != null) {
            for (g in 0 until genreArray.length()) {
                genres.add(genreArray.optString(g))
            }
        }

        // Studios
        val studioList = mutableListOf<StudioInfo>()
        val studioEdges = media.optJSONObject("studios")?.optJSONArray("edges")
        if (studioEdges != null) {
            for (s in 0 until studioEdges.length()) {
                val edge = studioEdges.optJSONObject(s) ?: continue
                val isMain = edge.optBoolean("isMain", true)
                val node = edge.optJSONObject("node") ?: continue
                studioList.add(
                    StudioInfo(
                        id = node.optInt("id"),
                        name = node.optString("name", "Studio"),
                        isAnimationStudio = node.optBoolean("isAnimationStudio", true),
                        siteUrl = node.optString("siteUrl", null)
                    )
                )
            }
        }

        // Upcoming Episode
        var upcomingInfo: UpcomingEpisodeInfo? = null
        val nextObj = media.optJSONObject("nextAiringEpisode")
        if (nextObj != null && !nextObj.isNull("airingAt")) {
            val epNum = nextObj.optInt("episode")
            val timeUntilSec = nextObj.optLong("timeUntilAiring")
            val airingAtSec = nextObj.optLong("airingAt")

            val sdf = SimpleDateFormat("MMM d, yyyy · HH:mm z", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("Asia/Tokyo")
            }
            val formattedDate = sdf.format(Date(airingAtSec * 1000L)) + " (JST)"

            upcomingInfo = UpcomingEpisodeInfo(
                episodeNumber = epNum,
                timeUntilAiringSeconds = timeUntilSec,
                airingAtEpochSeconds = airingAtSec,
                formattedDate = formattedDate,
                isConfirmed = true
            )
        }

        // Trailer
        val trailerObj = media.optJSONObject("trailer")
        val trailerId = trailerObj?.optString("id", null)
        val trailerSite = trailerObj?.optString("site", null)

        // External Links
        val linksList = mutableListOf<Pair<String, String>>()
        val extArray = media.optJSONArray("externalLinks")
        if (extArray != null) {
            for (e in 0 until extArray.length()) {
                val linkObj = extArray.optJSONObject(e) ?: continue
                val site = linkObj.optString("site", "Official Link")
                val url = linkObj.optString("url", "")
                if (url.isNotBlank()) {
                    linksList.add(site to url)
                }
            }
        }

        // Relations
        val relationsList = mutableListOf<TimelineEntry>()
        val relEdges = media.optJSONObject("relations")?.optJSONArray("edges")
        if (relEdges != null) {
            for (r in 0 until relEdges.length()) {
                val edge = relEdges.optJSONObject(r) ?: continue
                val relType = edge.optString("relationType", "RELATED")
                val node = edge.optJSONObject("node") ?: continue
                val relTitleObj = node.optJSONObject("title")
                val rTitle = relTitleObj?.optString("english")
                    ?: relTitleObj?.optString("romaji")
                    ?: "Related Media"
                val relCover = node.optJSONObject("coverImage")?.optString("large")
                    ?: node.optJSONObject("coverImage")?.optString("medium")

                relationsList.add(
                    TimelineEntry(
                        id = node.optInt("id"),
                        malId = if (node.has("idMal") && !node.isNull("idMal")) node.optInt("idMal") else null,
                        title = rTitle,
                        year = if (node.has("seasonYear") && !node.isNull("seasonYear")) node.optInt("seasonYear") else null,
                        format = node.optString("format", "TV"),
                        relationType = relType,
                        coverImageUrl = relCover
                    )
                )
            }
        }

        // Airing Schedule for Upcoming Episodes
        val airingScheduleList = mutableListOf<AiringScheduleNode>()
        val scheduleNodes = media.optJSONObject("airingSchedule")?.optJSONArray("nodes")
        if (scheduleNodes != null) {
            for (s in 0 until scheduleNodes.length()) {
                val sNode = scheduleNodes.optJSONObject(s) ?: continue
                airingScheduleList.add(
                    AiringScheduleNode(
                        id = sNode.optInt("id"),
                        episode = sNode.optInt("episode"),
                        airingAt = sNode.optLong("airingAt"),
                        timeUntilAiring = sNode.optLong("timeUntilAiring")
                    )
                )
            }
        }

        // Characters & Voice Cast (both Japanese and English Dub)
        val characterList = mutableListOf<CharacterCast>()
        val dubLanguagesSet = mutableSetOf<String>()
        val charEdges = media.optJSONObject("characters")?.optJSONArray("edges")
        if (charEdges != null) {
            for (c in 0 until charEdges.length()) {
                val edge = charEdges.optJSONObject(c) ?: continue
                val role = edge.optString("role", "SUPPORTING")
                val charNode = edge.optJSONObject("node") ?: continue
                val charId = charNode.optInt("id")
                val nameObj = charNode.optJSONObject("name")
                val charName = nameObj?.optString("full", "Character") ?: "Character"
                val charNative = nameObj?.optString("native", null)
                val charImg = charNode.optJSONObject("image")?.optString("large")
                    ?: charNode.optJSONObject("image")?.optString("medium")

                val vaArray = edge.optJSONArray("voiceActors")
                var jaVaName: String? = null
                var jaVaNative: String? = null
                var jaVaImg: String? = null
                var enVaName: String? = null
                var enVaImg: String? = null

                if (vaArray != null && vaArray.length() > 0) {
                    for (v in 0 until vaArray.length()) {
                        val vaNode = vaArray.optJSONObject(v) ?: continue
                        val lang = vaNode.optString("languageV2", "")
                        if (lang.isNotBlank() && !lang.equals("Japanese", ignoreCase = true)) {
                            dubLanguagesSet.add(lang)
                        }

                        val vaNameObj = vaNode.optJSONObject("name")
                        val vName = vaNameObj?.optString("full", null)
                        val vNative = vaNameObj?.optString("native", null)
                        val vImg = vaNode.optJSONObject("image")?.optString("medium")

                        if ((lang.equals("Japanese", ignoreCase = true) || lang.isBlank()) && jaVaName == null) {
                            jaVaName = vName
                            jaVaNative = vNative
                            jaVaImg = vImg
                        } else if (lang.equals("English", ignoreCase = true) && enVaName == null) {
                            enVaName = vName
                            enVaImg = vImg
                        }
                    }
                    // Fallback to first voice actor if Japanese wasn't explicitly matched
                    if (jaVaName == null && vaArray.length() > 0) {
                        val firstNode = vaArray.optJSONObject(0)
                        jaVaName = firstNode?.optJSONObject("name")?.optString("full", null)
                        jaVaNative = firstNode?.optJSONObject("name")?.optString("native", null)
                        jaVaImg = firstNode?.optJSONObject("image")?.optString("medium")
                    }
                }

                characterList.add(
                    CharacterCast(
                        id = charId,
                        name = charName,
                        nativeName = charNative,
                        role = role,
                        imageUrl = charImg,
                        voiceActorName = jaVaName,
                        voiceActorNative = jaVaNative,
                        voiceActorImageUrl = jaVaImg,
                        englishVoiceActorName = enVaName,
                        englishVoiceActorImageUrl = enVaImg
                    )
                )
            }
        }

        return AniListMediaDetails(
            id = media.optInt("id"),
            malId = if (media.has("idMal") && !media.isNull("idMal")) media.optInt("idMal") else null,
            romajiTitle = romaji,
            englishTitle = english,
            nativeTitle = native,
            description = desc,
            status = formatStatus(media.optString("status", "UNKNOWN")),
            format = media.optString("format", "TV"),
            episodes = if (media.has("episodes") && !media.isNull("episodes")) media.optInt("episodes") else null,
            duration = if (media.has("duration") && !media.isNull("duration")) media.optInt("duration") else null,
            startDate = startDateStr,
            endDate = endDateStr,
            season = media.optString("season", null),
            seasonYear = if (media.has("seasonYear") && !media.isNull("seasonYear")) media.optInt("seasonYear") else null,
            source = media.optString("source", null),
            score = score,
            coverImage = coverUrl,
            bannerImage = bannerUrl,
            genres = genres,
            studios = studioList,
            nextEpisode = upcomingInfo,
            trailerId = trailerId,
            trailerSite = trailerSite,
            externalLinks = linksList,
            relations = relationsList,
            characters = characterList,
            airingSchedule = airingScheduleList,
            dubLanguages = dubLanguagesSet.toList()
        )
    }

    private fun formatDate(dateObj: JSONObject?): String? {
        if (dateObj == null) return null
        val year = if (dateObj.has("year") && !dateObj.isNull("year")) dateObj.optInt("year") else return null
        val month = if (dateObj.has("month") && !dateObj.isNull("month")) dateObj.optInt("month") else null
        val day = if (dateObj.has("day") && !dateObj.isNull("day")) dateObj.optInt("day") else null

        return when {
            month != null && day != null -> {
                val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val mStr = if (month in 1..12) months[month - 1] else month.toString()
                "$mStr $day, $year"
            }
            month != null -> {
                val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                "${months.getOrNull(month - 1) ?: month} $year"
            }
            else -> "$year"
        }
    }

    private fun formatStatus(status: String): String {
        return when (status.uppercase()) {
            "RELEASING" -> "Currently Airing"
            "FINISHED" -> "Finished Airing"
            "NOT_YET_RELEASED" -> "Upcoming"
            "CANCELLED" -> "Cancelled"
            "HIATUS" -> "On Hiatus"
            else -> status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}
