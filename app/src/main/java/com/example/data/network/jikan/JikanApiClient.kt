package com.example.data.network.jikan

import android.util.Log
import com.example.data.model.AnimeSummary
import com.example.data.model.EpisodeItem
import com.example.data.model.StreamingService
import com.example.data.model.UpdateCategory
import com.example.data.model.VerificationStatus
import com.example.data.model.WebUpdate
import com.example.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

data class JikanAnimeDetails(
    val malId: Int,
    val title: String,
    val englishTitle: String?,
    val japaneseTitle: String?,
    val synopsis: String?,
    val type: String?,
    val episodes: Int?,
    val status: String?,
    val airedString: String?,
    val duration: String?,
    val rating: String?,
    val score: Double?,
    val scoredBy: Int?,
    val season: String?,
    val year: Int?,
    val studios: List<String>,
    val producers: List<String>,
    val licensors: List<String>,
    val genres: List<String>,
    val posterUrl: String?,
    val trailerUrl: String?,
    val broadcastString: String?
)

data class PaginatedEpisodes(
    val episodes: List<EpisodeItem>,
    val currentPage: Int,
    val lastPage: Int,
    val hasNextPage: Boolean
)

object JikanApiClient {
    private const val TAG = "JikanApiClient"
    private const val BASE_URL = "https://api.jikan.moe/v4"

    private suspend fun executeGetWithRetry(url: String, maxRetries: Int = 2): JSONObject? = withContext(Dispatchers.IO) {
        var retries = 0
        var backoffMs = 800L

        while (retries <= maxRetries) {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()

            try {
                NetworkClient.okHttpClient.newCall(request).execute().use { response ->
                    if (response.code == 429) {
                        Log.w(TAG, "Jikan 429 Rate limited, backing off $backoffMs ms...")
                        delay(backoffMs)
                        backoffMs *= 2
                        retries++
                        return@use
                    }
                    if (!response.isSuccessful) {
                        Log.w(TAG, "Jikan request failed url=$url code=${response.code}")
                        return@withContext null
                    }
                    val body = response.body?.string() ?: return@withContext null
                    return@withContext JSONObject(body)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Jikan network error on $url", e)
                retries++
                delay(backoffMs)
            }
        }
        null
    }

    suspend fun searchAnime(query: String, limit: Int = 20): List<AnimeSummary> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "$BASE_URL/anime?q=$encoded&limit=$limit&order_by=popularity&sort=asc"
        val json = executeGetWithRetry(url) ?: return@withContext emptyList()
        val data = json.optJSONArray("data") ?: return@withContext emptyList()

        val list = mutableListOf<AnimeSummary>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val malId = item.optInt("mal_id")
            val title = item.optString("title", "Untitled")
            val englishTitle = item.optString("title_english", null)
            val japaneseTitle = item.optString("title_japanese", null)

            val images = item.optJSONObject("images")?.optJSONObject("webp")
                ?: item.optJSONObject("images")?.optJSONObject("jpg")
            val posterUrl = images?.optString("large_image_url") ?: images?.optString("image_url")

            val score = if (item.has("score") && !item.isNull("score")) item.optDouble("score") else null
            val status = item.optString("status", "Unknown")
            val type = item.optString("type", "TV")
            val episodes = if (item.has("episodes") && !item.isNull("episodes")) item.optInt("episodes") else null
            val year = if (item.has("year") && !item.isNull("year")) item.optInt("year") else null

            val genreArray = item.optJSONArray("genres")
            val genres = mutableListOf<String>()
            if (genreArray != null) {
                for (g in 0 until genreArray.length()) {
                    genres.add(genreArray.optJSONObject(g)?.optString("name") ?: "")
                }
            }

            list.add(
                AnimeSummary(
                    id = malId,
                    malId = malId,
                    title = englishTitle ?: title,
                    englishTitle = englishTitle,
                    japaneseTitle = japaneseTitle,
                    coverImageUrl = posterUrl,
                    bannerImageUrl = null,
                    score = score,
                    status = status,
                    format = type,
                    episodes = episodes,
                    seasonYear = year,
                    genres = genres.filter { it.isNotBlank() },
                    verificationStatus = VerificationStatus.VERIFIED
                )
            )
        }
        list
    }

    suspend fun getAnimeFull(malId: Int): JikanAnimeDetails? = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/anime/$malId/full"
        val json = executeGetWithRetry(url) ?: return@withContext null
        val data = json.optJSONObject("data") ?: return@withContext null

        val title = data.optString("title", "Untitled")
        val englishTitle = data.optString("title_english", null)
        val japaneseTitle = data.optString("title_japanese", null)
        val synopsis = data.optString("synopsis", null)
        val type = data.optString("type", "TV")
        val episodes = if (data.has("episodes") && !data.isNull("episodes")) data.optInt("episodes") else null
        val status = data.optString("status", null)

        val airedObj = data.optJSONObject("aired")
        val airedString = airedObj?.optString("string", null)

        val duration = data.optString("duration", null)
        val rating = data.optString("rating", null)
        val score = if (data.has("score") && !data.isNull("score")) data.optDouble("score") else null
        val scoredBy = if (data.has("scored_by") && !data.isNull("scored_by")) data.optInt("scored_by") else null
        val season = data.optString("season", null)
        val year = if (data.has("year") && !data.isNull("year")) data.optInt("year") else null

        val studios = extractNames(data.optJSONArray("studios"))
        val producers = extractNames(data.optJSONArray("producers"))
        val licensors = extractNames(data.optJSONArray("licensors"))
        val genres = extractNames(data.optJSONArray("genres"))

        val images = data.optJSONObject("images")?.optJSONObject("webp")
            ?: data.optJSONObject("images")?.optJSONObject("jpg")
        val posterUrl = images?.optString("large_image_url") ?: images?.optString("image_url")

        val trailerUrl = data.optJSONObject("trailer")?.optString("url", null)
        val broadcastString = data.optJSONObject("broadcast")?.optString("string", null)

        JikanAnimeDetails(
            malId = malId,
            title = title,
            englishTitle = englishTitle,
            japaneseTitle = japaneseTitle,
            synopsis = synopsis,
            type = type,
            episodes = episodes,
            status = status,
            airedString = airedString,
            duration = duration,
            rating = rating,
            score = score,
            scoredBy = scoredBy,
            season = season,
            year = year,
            studios = studios,
            producers = producers,
            licensors = licensors,
            genres = genres,
            posterUrl = posterUrl,
            trailerUrl = trailerUrl,
            broadcastString = broadcastString
        )
    }

    suspend fun getEpisodes(malId: Int, page: Int = 1): PaginatedEpisodes = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/anime/$malId/episodes?page=$page"
        val json = executeGetWithRetry(url) ?: return@withContext PaginatedEpisodes(emptyList(), page, 1, false)

        val data = json.optJSONArray("data") ?: JSONArray()
        val pagination = json.optJSONObject("pagination")
        val lastPage = pagination?.optInt("last_visible_page", 1) ?: 1
        val hasNext = pagination?.optBoolean("has_next_page", false) ?: false

        val episodes = mutableListOf<EpisodeItem>()
        for (i in 0 until data.length()) {
            val ep = data.optJSONObject(i) ?: continue
            val epNum = ep.optInt("mal_id", i + 1)
            val title = ep.optString("title", "Episode $epNum")
            val aired = ep.optString("aired", null)?.take(10) // YYYY-MM-DD
            val filler = ep.optBoolean("filler", false)

            episodes.add(
                EpisodeItem(
                    episodeNumber = epNum,
                    title = title,
                    airDate = aired,
                    durationMinutes = null,
                    isFiller = filler
                )
            )
        }

        PaginatedEpisodes(
            episodes = episodes,
            currentPage = page,
            lastPage = lastPage,
            hasNextPage = hasNext
        )
    }

    suspend fun getNews(malId: Int): List<WebUpdate> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/anime/$malId/news"
        val json = executeGetWithRetry(url) ?: return@withContext emptyList()
        val data = json.optJSONArray("data") ?: return@withContext emptyList()

        val newsList = mutableListOf<WebUpdate>()
        for (i in 0 until minOf(data.length(), 6)) {
            val item = data.optJSONObject(i) ?: continue
            val headline = item.optString("title", "Anime Update")
            val forumUrl = item.optString("forum_url", null)
            val date = item.optString("date", "").take(10)
            val excerpt = item.optString("excerpt", "No summary provided.")
            val author = item.optString("author_username", "ANN News")

            val category = when {
                headline.contains("Announced", ignoreCase = true) || headline.contains("Season", ignoreCase = true) || headline.contains("Official", ignoreCase = true) -> UpdateCategory.OFFICIAL_ANNOUNCEMENT
                headline.contains("Delay", ignoreCase = true) || headline.contains("Postponed", ignoreCase = true) || headline.contains("Broadcast", ignoreCase = true) -> UpdateCategory.REPORTED
                headline.contains("Rumor", ignoreCase = true) || headline.contains("Leaked", ignoreCase = true) -> UpdateCategory.RUMOR
                else -> UpdateCategory.REPORTED
            }

            newsList.add(
                WebUpdate(
                    headline = headline,
                    source = "Anime News Network ($author)",
                    url = forumUrl,
                    publicationDate = date,
                    summary = excerpt,
                    category = category
                )
            )
        }
        newsList
    }

    suspend fun getStreaming(malId: Int): List<StreamingService> = withContext(Dispatchers.IO) {
        val url = "$BASE_URL/anime/$malId/streaming"
        val json = executeGetWithRetry(url) ?: return@withContext emptyList()
        val data = json.optJSONArray("data") ?: return@withContext emptyList()

        val list = mutableListOf<StreamingService>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i) ?: continue
            val name = item.optString("name", "Streaming Provider")
            val linkUrl = item.optString("url", "")
            if (linkUrl.isNotBlank()) {
                list.add(
                    StreamingService(
                        name = name,
                        url = linkUrl,
                        regionNotice = "Official licensed stream"
                    )
                )
            }
        }
        list
    }

    private fun extractNames(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i)
            val name = item?.optString("name")
            if (!name.isNullOrBlank()) {
                list.add(name)
            }
        }
        return list
    }
}
