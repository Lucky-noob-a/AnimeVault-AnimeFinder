package com.example

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UpcomingTitleParsingTest {

    @Test
    fun testUpcomingAnimeWithNullEnglishTitleDoesNotReturnLiteralNull() {
        // Simulates AniList GraphQL response for upcoming anticipated anime where English localization is null
        val rawJson = """
            {
                "id": 16498,
                "idMal": 52034,
                "title": {
                    "romaji": "Chainsaw Man: Reze-hen",
                    "english": null,
                    "native": "劇場版『チェンソーマン レゼ篇』"
                },
                "coverImage": {
                    "large": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/medium/bx16498.jpg"
                },
                "status": "NOT_YET_RELEASED",
                "format": "MOVIE",
                "averageScore": null,
                "genres": ["Action", "Supernatural"]
            }
        """.trimIndent()

        val jsonObject = JSONObject(rawJson)
        val titleObj = jsonObject.optJSONObject("title")

        // Helper method under test:
        fun optCleanString(obj: JSONObject?, key: String, fallback: String? = null): String? {
            if (obj == null || !obj.has(key) || obj.isNull(key)) return fallback
            val str = obj.optString(key).trim()
            return if (str.isEmpty() || str.equals("null", ignoreCase = true)) fallback else str
        }

        val english = optCleanString(titleObj, "english")
        val romaji = optCleanString(titleObj, "romaji")
        val native = optCleanString(titleObj, "native")

        val chosenTitle = when {
            !english.isNullOrBlank() -> english
            !romaji.isNullOrBlank() -> romaji
            !native.isNullOrBlank() -> native
            else -> "Upcoming Anime"
        }

        assertNull("English title should be parsed as null instead of literal 'null'", english)
        assertNotEquals("Title must never be literal 'null'", "null", chosenTitle)
        assertEquals("Title should fallback to romaji when english is null", "Chainsaw Man: Reze-hen", chosenTitle)
    }

    @Test
    fun testUpcomingAnimeWithOnlyNativeTitle() {
        val rawJson = """
            {
                "id": 99999,
                "title": {
                    "romaji": null,
                    "english": null,
                    "native": "新作アニメ"
                }
            }
        """.trimIndent()

        val jsonObject = JSONObject(rawJson)
        val titleObj = jsonObject.optJSONObject("title")

        fun optCleanString(obj: JSONObject?, key: String, fallback: String? = null): String? {
            if (obj == null || !obj.has(key) || obj.isNull(key)) return fallback
            val str = obj.optString(key).trim()
            return if (str.isEmpty() || str.equals("null", ignoreCase = true)) fallback else str
        }

        val english = optCleanString(titleObj, "english")
        val romaji = optCleanString(titleObj, "romaji")
        val native = optCleanString(titleObj, "native")

        val chosenTitle = when {
            !english.isNullOrBlank() -> english
            !romaji.isNullOrBlank() -> romaji
            !native.isNullOrBlank() -> native
            else -> "Upcoming Anime"
        }

        assertEquals("Should use native title if english and romaji are null", "新作アニメ", chosenTitle)
    }
}
