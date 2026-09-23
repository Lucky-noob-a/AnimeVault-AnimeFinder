package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.backup.AppDataBackupManager
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteAnimeEntity
import com.example.data.local.SearchHistoryEntity
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDataBackupTest {

    private lateinit var context: Context
    private lateinit var backupManager: AppDataBackupManager
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        backupManager = AppDataBackupManager(context)
        database = AppDatabase.getDatabase(context)

        // Reset state
        backupManager.logout()
        runBlocking {
            val dao = database.animeDao()
            dao.clearAllFavorites()
            dao.clearSearchHistory()
            dao.clearAllCache()
        }
    }

    @Test
    fun testMailLoginAndInitialAutoBackup() = runBlocking {
        val dao = database.animeDao()

        // Insert initial test favorite
        val testFav = FavoriteAnimeEntity(
            id = 9999,
            anilistId = 9999,
            malId = 9999,
            title = "Attack on Titan",
            englishTitle = "Attack on Titan",
            coverImageUrl = "https://example.com/cover.jpg",
            bannerImageUrl = null,
            score = 8.5,
            status = "Finished",
            format = "TV",
            year = 2013,
            genres = "Action, Drama"
        )
        dao.addFavorite(testFav)
        dao.insertSearchQuery(SearchHistoryEntity(query = "Attack on Titan"))

        val email = "testuser@example.com"
        val loginResult = backupManager.loginWithEmail(email)
        assertTrue("Login should succeed", loginResult.isSuccess)
        val result = loginResult.getOrNull()
        assertNotNull(result)
        assertEquals(email, result!!.email)

        assertTrue(backupManager.isLoggedIn())
        assertEquals(email, backupManager.getLoggedInEmail())

        val syncInfo = backupManager.getAccountSyncInfo()
        assertTrue(syncInfo.isLoggedIn)
        assertEquals(email, syncInfo.email)
        assertNotNull(syncInfo.lastSyncEpoch)
        assertTrue("Cloud backup should have stored favorites", syncInfo.cloudFavoritesCount >= 1)
    }

    @Test
    fun testMailLoginAutomaticRestoreOnLogin() = runBlocking {
        val dao = database.animeDao()
        val email = "testuser@example.com"

        // 1. Log in initially and save a favorite
        backupManager.loginWithEmail(email)
        val testFav = FavoriteAnimeEntity(
            id = 8888,
            anilistId = 8888,
            malId = 8888,
            title = "Steins;Gate",
            englishTitle = "Steins;Gate",
            coverImageUrl = null,
            bannerImageUrl = null,
            score = 9.1,
            status = "Finished",
            format = "TV",
            year = 2011,
            genres = "Sci-Fi, Thriller"
        )
        dao.addFavorite(testFav)
        dao.insertSearchQuery(SearchHistoryEntity(query = "Steins;Gate"))

        // Auto backup
        val backupResult = backupManager.triggerAutoBackup()
        assertTrue("Auto backup should succeed", backupResult.isSuccess)

        // 2. User logs out and clears device data (simulating new phone / app re-install)
        backupManager.logout()
        assertFalse(backupManager.isLoggedIn())
        dao.clearAllFavorites()
        dao.clearSearchHistory()
        assertEquals(0, dao.getFavoritesCount())
        assertEquals(0, dao.getSearchHistoryCount())

        // 3. User logs in with email: Automatic Restore on Login!
        val restoreLoginResult = backupManager.loginWithEmail(email)
        assertTrue("Login should succeed", restoreLoginResult.isSuccess)
        val restoreResult = restoreLoginResult.getOrNull()
        assertNotNull(restoreResult)
        assertFalse(restoreResult!!.isNewAccount)
        assertTrue("Should have restored at least 1 favorite", restoreResult.restoredFavorites >= 1)

        // Verify database is populated from automatic restore
        val restoredFavs = dao.getAllFavoritesList()
        assertTrue("Favorites should be automatically restored in Room", restoredFavs.any { it.id == 8888 })
        val restoredQueries = dao.getAllSearchHistoryList()
        assertTrue("Search queries should be restored", restoredQueries.any { it.query == "Steins;Gate" })
    }

    @Test
    fun testManualSyncNow() = runBlocking {
        val email = "testuser@example.com"
        backupManager.loginWithEmail(email)

        val syncResult = backupManager.syncNow()
        assertTrue("Sync now should succeed", syncResult.isSuccess)
        val res = syncResult.getOrNull()
        assertNotNull(res)
        assertEquals(email, res!!.email)
    }

    @Test
    fun testInvalidEmailValidation() = runBlocking {
        val result = backupManager.loginWithEmail("invalidemail")
        assertTrue("Invalid email should fail", result.isFailure)
    }
}
