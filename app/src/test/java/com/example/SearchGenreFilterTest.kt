package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AnimeSummary
import com.example.data.model.VerificationStatus
import com.example.ui.viewmodel.SearchViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchGenreFilterTest {

    @Test
    fun testInitialGenreFilterState() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = SearchViewModel(app)

        assertEquals("ALL", viewModel.uiState.value.selectedGenre)
        assertTrue(viewModel.uiState.value.selectedGenres.isEmpty())
    }

    @Test
    fun testSelectAndToggleGenre() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = SearchViewModel(app)

        // Select Action
        viewModel.onSelectGenre("Action")
        assertEquals("Action", viewModel.uiState.value.selectedGenre)
        assertEquals(setOf("Action"), viewModel.uiState.value.selectedGenres)

        // Toggle Action again should reset to ALL
        viewModel.onSelectGenre("Action")
        assertEquals("ALL", viewModel.uiState.value.selectedGenre)
        assertTrue(viewModel.uiState.value.selectedGenres.isEmpty())

        // Select Fantasy then switch to Sci-Fi
        viewModel.toggleGenre("Fantasy")
        assertEquals("Fantasy", viewModel.uiState.value.selectedGenre)
        viewModel.toggleGenre("Sci-Fi")
        assertEquals("Sci-Fi", viewModel.uiState.value.selectedGenre)

        // Clear filter
        viewModel.clearGenreFilter()
        assertEquals("ALL", viewModel.uiState.value.selectedGenre)
        assertTrue(viewModel.uiState.value.selectedGenres.isEmpty())
    }

    @Test
    fun testDynamicFilteringByGenre() {
        val animeList = listOf(
            AnimeSummary(
                id = 1,
                title = "Naruto",
                genres = listOf("Action", "Adventure", "Fantasy"),
                format = "TV",
                verificationStatus = VerificationStatus.VERIFIED
            ),
            AnimeSummary(
                id = 2,
                title = "Death Note",
                genres = listOf("Mystery", "Psychological", "Supernatural", "Thriller"),
                format = "TV",
                verificationStatus = VerificationStatus.VERIFIED
            ),
            AnimeSummary(
                id = 3,
                title = "Kimi no Na wa",
                genres = listOf("Romance", "Drama", "Supernatural"),
                format = "MOVIE",
                verificationStatus = VerificationStatus.VERIFIED
            )
        )

        // Filter Action
        val actionAnime = animeList.filter { anime ->
            anime.genres.any { it.equals("Action", ignoreCase = true) }
        }
        assertEquals(1, actionAnime.size)
        assertEquals("Naruto", actionAnime.first().title)

        // Filter Supernatural
        val supernaturalAnime = animeList.filter { anime ->
            anime.genres.any { it.equals("Supernatural", ignoreCase = true) }
        }
        assertEquals(2, supernaturalAnime.size)
        assertTrue(supernaturalAnime.any { it.title == "Death Note" })
        assertTrue(supernaturalAnime.any { it.title == "Kimi no Na wa" })

        // Filter Non-matching genre
        val sportsAnime = animeList.filter { anime ->
            anime.genres.any { it.equals("Sports", ignoreCase = true) }
        }
        assertEquals(0, sportsAnime.size)
    }

    @Test
    fun testCategoryCatalogHasActionRomanceAndSeinen() {
        val action = com.example.data.model.AnimeCategoryCatalog.find("Action")
        val romance = com.example.data.model.AnimeCategoryCatalog.find("Romance")
        val seinen = com.example.data.model.AnimeCategoryCatalog.find("Seinen")

        org.junit.Assert.assertNotNull(action)
        org.junit.Assert.assertNotNull(romance)
        org.junit.Assert.assertNotNull(seinen)

        assertEquals(com.example.data.model.CategoryType.GENRE, action?.type)
        assertEquals(com.example.data.model.CategoryType.GENRE, romance?.type)
        assertEquals(com.example.data.model.CategoryType.DEMOGRAPHIC, seinen?.type)
        assertEquals("⚔️", action?.emoji)
        assertEquals("💖", romance?.emoji)
        assertEquals("🎯", seinen?.emoji)
    }

    @Test
    fun testBrowseCategorySetsCorrectState() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = SearchViewModel(app)

        // Browse Seinen
        viewModel.browseCategory("Seinen")
        assertEquals("Seinen", viewModel.uiState.value.selectedCategory)
        assertEquals("Seinen", viewModel.uiState.value.selectedGenre)
        assertTrue(viewModel.uiState.value.isCategoryBrowseMode)

        // Change Category Type filter tab
        viewModel.onSelectCategoryType(com.example.data.model.CategoryType.DEMOGRAPHIC)
        assertEquals(com.example.data.model.CategoryType.DEMOGRAPHIC, viewModel.uiState.value.selectedCategoryType)

        // Clear genre filter should reset category browse mode
        viewModel.clearGenreFilter()
        assertEquals("ALL", viewModel.uiState.value.selectedGenre)
        assertEquals("ALL", viewModel.uiState.value.selectedCategory)
        assertFalse(viewModel.uiState.value.isCategoryBrowseMode)
    }

    @Test
    fun testDemographicAndThemeFiltering() {
        val animeList = listOf(
            AnimeSummary(
                id = 10,
                title = "Berserk",
                genres = listOf("Action", "Dark Fantasy", "Seinen", "Horror"),
                format = "TV",
                verificationStatus = VerificationStatus.VERIFIED
            ),
            AnimeSummary(
                id = 11,
                title = "Toradora!",
                genres = listOf("Romance", "Comedy", "Drama", "School"),
                format = "TV",
                verificationStatus = VerificationStatus.VERIFIED
            ),
            AnimeSummary(
                id = 12,
                title = "Monster",
                genres = listOf("Drama", "Mystery", "Psychological", "Seinen", "Suspense"),
                format = "TV",
                verificationStatus = VerificationStatus.VERIFIED
            )
        )

        val seinenAnime = animeList.filter { it.genres.any { g -> g.equals("Seinen", ignoreCase = true) } }
        assertEquals(2, seinenAnime.size)
        assertTrue(seinenAnime.any { it.title == "Berserk" })
        assertTrue(seinenAnime.any { it.title == "Monster" })

        val romanceAnime = animeList.filter { it.genres.any { g -> g.equals("Romance", ignoreCase = true) } }
        assertEquals(1, romanceAnime.size)
        assertEquals("Toradora!", romanceAnime.first().title)

        val actionAnime = animeList.filter { it.genres.any { g -> g.equals("Action", ignoreCase = true) } }
        assertEquals(1, actionAnime.size)
        assertEquals("Berserk", actionAnime.first().title)
    }
}
