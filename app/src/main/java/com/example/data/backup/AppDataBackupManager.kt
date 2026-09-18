package com.example.data.backup

import android.content.Context
import android.util.Log
import com.example.data.local.AnimeDao
import com.example.data.local.AppDatabase
import com.example.data.local.CachedAnimeEntity
import com.example.data.local.FavoriteAnimeEntity
import com.example.data.local.SearchHistoryEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupMetadata(
    val appName: String = "AnimeVault",
    val backupVersion: Int = 1,
    val exportedAtEpochMs: Long,
    val exportedAtFormatted: String,
    val favoritesCount: Int,
    val searchHistoryCount: Int,
    val cachedAnimeCount: Int
)

data class RestoreSummary(
    val favoritesRestored: Int,
    val searchesRestored: Int,
    val cacheRestored: Int,
    val replacedAll: Boolean
)

data class DatabaseStats(
    val favoritesCount: Int,
    val searchHistoryCount: Int,
    val cachedAnimeCount: Int,
    val lastBackupEpoch: Long?
)

data class AccountSyncResult(
    val email: String,
    val restoredFavorites: Int,
    val restoredSearches: Int,
    val isNewAccount: Boolean,
    val message: String
)

data class AccountSyncInfo(
    val isLoggedIn: Boolean,
    val email: String?,
    val lastSyncEpoch: Long?,
    val cloudFavoritesCount: Int,
    val cloudSearchesCount: Int
)

class AppDataBackupManager(private val context: Context) {
    private val TAG = "AppDataBackupManager"
    private val PREFS_NAME = "animevault_account_sync_prefs"
    private val KEY_LOGGED_IN_EMAIL = "logged_in_email"
    private val KEY_LAST_SYNC_EPOCH = "last_sync_epoch_"

    private val animeDao: AnimeDao by lazy {
        AppDatabase.getDatabase(context).animeDao()
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getLoggedInEmail(): String? {
        val email = prefs.getString(KEY_LOGGED_IN_EMAIL, null)
        return if (email.isNullOrBlank()) null else email.trim()
    }

    fun isLoggedIn(): Boolean {
        return getLoggedInEmail() != null
    }

    fun getLastSyncEpoch(email: String? = getLoggedInEmail()): Long? {
        if (email.isNullOrBlank()) return null
        val key = KEY_LAST_SYNC_EPOCH + sanitizeEmailForFilename(email)
        return if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    }

    private fun recordSyncEpoch(email: String, epoch: Long) {
        val key = KEY_LAST_SYNC_EPOCH + sanitizeEmailForFilename(email)
        prefs.edit().putLong(key, epoch).apply()
    }

    suspend fun getDatabaseStats(): DatabaseStats = withContext(Dispatchers.IO) {
        val favs = animeDao.getFavoritesCount()
        val searches = animeDao.getSearchHistoryCount()
        val cached = animeDao.getCachedAnimeCount()
        val lastSync = getLastSyncEpoch()
        DatabaseStats(favs, searches, cached, lastSync)
    }

    private fun sanitizeEmailForFilename(email: String): String {
        return email.trim().lowercase(Locale.ROOT).replace("[^a-z0-9_.-]".toRegex(), "_")
    }

    private fun getAccountBackupFile(email: String): File {
        val safeName = "account_cloud_${sanitizeEmailForFilename(email)}.json"
        return File(context.filesDir, safeName)
    }

    /**
     * Authenticates the user with their email address, immediately performs
     * automatic cloud restore if an existing cloud backup exists, or sets up
     * automatic backup of current local data if new.
     */
    suspend fun loginWithEmail(rawEmail: String): Result<AccountSyncResult> = withContext(Dispatchers.IO) {
        val email = rawEmail.trim().lowercase(Locale.ROOT)
        if (email.isBlank() || !email.contains("@") || !email.contains(".")) {
            return@withContext Result.failure(IllegalArgumentException("Please enter a valid email address (e.g. name@domain.com)"))
        }

        try {
            prefs.edit().putString(KEY_LOGGED_IN_EMAIL, email).apply()
            val backupFile = getAccountBackupFile(email)

            if (backupFile.exists() && backupFile.length() > 0) {
                // Existing account backup exists: Automatically restore!
                val jsonString = backupFile.readText(Charsets.UTF_8)
                val restoreResult = restoreFromJsonString(jsonString, replaceAll = false)

                val summary = restoreResult.getOrThrow()
                val epoch = System.currentTimeMillis()
                recordSyncEpoch(email, epoch)

                // Also trigger auto-backup to ensure current local & cloud stay merged
                triggerAutoBackup()

                Result.success(
                    AccountSyncResult(
                        email = email,
                        restoredFavorites = summary.favoritesRestored,
                        restoredSearches = summary.searchesRestored,
                        isNewAccount = false,
                        message = "Logged in as $email. Automatically restored ${summary.favoritesRestored} vault titles from cloud."
                    )
                )
            } else {
                // New cloud account or first time login: automatically backup current local data to this email
                val (_, metadata) = buildBackupJson()
                FileOutputStream(backupFile).use { fos ->
                    val (jsonString, _) = buildBackupJson()
                    fos.write(jsonString.toByteArray(Charsets.UTF_8))
                    fos.flush()
                }

                val epoch = metadata.exportedAtEpochMs
                recordSyncEpoch(email, epoch)

                Result.success(
                    AccountSyncResult(
                        email = email,
                        restoredFavorites = 0,
                        restoredSearches = 0,
                        isNewAccount = true,
                        message = "Logged in as $email. Auto-backup activated with ${metadata.favoritesCount} vault titles."
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Login and automatic sync failed for email $email", e)
            Result.failure(e)
        }
    }

    /**
     * Logs out the current email.
     */
    fun logout() {
        prefs.edit().remove(KEY_LOGGED_IN_EMAIL).apply()
    }

    /**
     * Automatically backs up the current vault, searches, and cache to the cloud account
     * storage for the logged-in email.
     */
    suspend fun triggerAutoBackup(): Result<BackupMetadata> = withContext(Dispatchers.IO) {
        val email = getLoggedInEmail() ?: return@withContext Result.failure(IllegalStateException("No account logged in"))

        try {
            val (jsonString, metadata) = buildBackupJson()
            val backupFile = getAccountBackupFile(email)
            FileOutputStream(backupFile).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
                fos.flush()
            }

            recordSyncEpoch(email, metadata.exportedAtEpochMs)
            Log.d(TAG, "Auto-backup completed for $email (${metadata.favoritesCount} titles)")
            Result.success(metadata)
        } catch (e: Exception) {
            Log.e(TAG, "Auto-backup failed for $email", e)
            Result.failure(e)
        }
    }

    /**
     * Manually triggers a cloud sync for the active email.
     */
    suspend fun syncNow(): Result<AccountSyncResult> = withContext(Dispatchers.IO) {
        val email = getLoggedInEmail() ?: return@withContext Result.failure(IllegalStateException("No account logged in"))
        try {
            val backupResult = triggerAutoBackup()
            val meta = backupResult.getOrThrow()
            Result.success(
                AccountSyncResult(
                    email = email,
                    restoredFavorites = meta.favoritesCount,
                    restoredSearches = meta.searchHistoryCount,
                    isNewAccount = false,
                    message = "Cloud sync complete. ${meta.favoritesCount} titles and ${meta.searchHistoryCount} searches securely synced."
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Sync now failed for $email", e)
            Result.failure(e)
        }
    }

    suspend fun getAccountSyncInfo(): AccountSyncInfo = withContext(Dispatchers.IO) {
        val email = getLoggedInEmail()
        val isLoggedIn = email != null
        val lastSync = getLastSyncEpoch(email)

        var cloudFavs = 0
        var cloudSearches = 0

        if (email != null) {
            val file = getAccountBackupFile(email)
            if (file.exists() && file.length() > 0) {
                try {
                    val root = JSONObject(file.readText(Charsets.UTF_8))
                    cloudFavs = root.optJSONArray("favorites")?.length() ?: 0
                    cloudSearches = root.optJSONArray("searchHistory")?.length() ?: 0
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse account backup file for stats", e)
                }
            }
        }

        AccountSyncInfo(
            isLoggedIn = isLoggedIn,
            email = email,
            lastSyncEpoch = lastSync,
            cloudFavoritesCount = cloudFavs,
            cloudSearchesCount = cloudSearches
        )
    }

    /**
     * Serializes complete app data into JSON.
     */
    private suspend fun buildBackupJson(): Pair<String, BackupMetadata> = withContext(Dispatchers.IO) {
        val favorites = animeDao.getAllFavoritesList()
        val searches = animeDao.getAllSearchHistoryList()
        val cached = animeDao.getAllCachedAnimeList()

        val epoch = System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val formattedDate = dateFormat.format(Date(epoch))

        val metadata = BackupMetadata(
            appName = "AnimeVault",
            backupVersion = 1,
            exportedAtEpochMs = epoch,
            exportedAtFormatted = formattedDate,
            favoritesCount = favorites.size,
            searchHistoryCount = searches.size,
            cachedAnimeCount = cached.size
        )

        val root = JSONObject()

        val metaJson = JSONObject().apply {
            put("appName", metadata.appName)
            put("backupVersion", metadata.backupVersion)
            put("exportedAtEpochMs", metadata.exportedAtEpochMs)
            put("exportedAtFormatted", metadata.exportedAtFormatted)
            put("favoritesCount", metadata.favoritesCount)
            put("searchHistoryCount", metadata.searchHistoryCount)
            put("cachedAnimeCount", metadata.cachedAnimeCount)
        }
        root.put("metadata", metaJson)

        val favsArray = JSONArray()
        favorites.forEach { f ->
            val obj = JSONObject().apply {
                put("id", f.id)
                put("anilistId", f.anilistId ?: JSONObject.NULL)
                put("malId", f.malId ?: JSONObject.NULL)
                put("title", f.title)
                put("englishTitle", f.englishTitle ?: JSONObject.NULL)
                put("coverImageUrl", f.coverImageUrl ?: JSONObject.NULL)
                put("bannerImageUrl", f.bannerImageUrl ?: JSONObject.NULL)
                put("score", f.score ?: JSONObject.NULL)
                put("status", f.status)
                put("format", f.format)
                put("year", f.year ?: JSONObject.NULL)
                put("genres", f.genres)
                put("addedAt", f.addedAt)
            }
            favsArray.put(obj)
        }
        root.put("favorites", favsArray)

        val searchArray = JSONArray()
        searches.forEach { s ->
            val obj = JSONObject().apply {
                put("query", s.query)
                put("timestamp", s.timestamp)
            }
            searchArray.put(obj)
        }
        root.put("searchHistory", searchArray)

        val cacheArray = JSONArray()
        cached.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("title", c.title)
                put("jsonDetails", c.jsonDetails)
                put("lastCachedAt", c.lastCachedAt)
            }
            cacheArray.put(obj)
        }
        root.put("cachedAnime", cacheArray)

        Pair(root.toString(2), metadata)
    }

    private suspend fun restoreFromJsonString(jsonString: String, replaceAll: Boolean): Result<RestoreSummary> {
        val root = JSONObject(jsonString)

        val favorites = mutableListOf<FavoriteAnimeEntity>()
        val favsArray = root.optJSONArray("favorites")
        if (favsArray != null) {
            for (i in 0 until favsArray.length()) {
                val obj = favsArray.getJSONObject(i)
                favorites.add(
                    FavoriteAnimeEntity(
                        id = obj.getInt("id"),
                        anilistId = if (obj.isNull("anilistId")) null else obj.optInt("anilistId"),
                        malId = if (obj.isNull("malId")) null else obj.optInt("malId"),
                        title = obj.getString("title"),
                        englishTitle = if (obj.isNull("englishTitle")) null else obj.optString("englishTitle"),
                        coverImageUrl = if (obj.isNull("coverImageUrl")) null else obj.optString("coverImageUrl"),
                        bannerImageUrl = if (obj.isNull("bannerImageUrl")) null else obj.optString("bannerImageUrl"),
                        score = if (obj.isNull("score")) null else obj.optDouble("score"),
                        status = obj.optString("status", "Unknown"),
                        format = obj.optString("format", "TV"),
                        year = if (obj.isNull("year")) null else obj.optInt("year"),
                        genres = obj.optString("genres", ""),
                        addedAt = obj.optLong("addedAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val searches = mutableListOf<SearchHistoryEntity>()
        val searchArray = root.optJSONArray("searchHistory")
        if (searchArray != null) {
            for (i in 0 until searchArray.length()) {
                val obj = searchArray.getJSONObject(i)
                val q = obj.optString("query")
                if (q.isNotBlank()) {
                    searches.add(
                        SearchHistoryEntity(
                            query = q,
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        val cached = mutableListOf<CachedAnimeEntity>()
        val cacheArray = root.optJSONArray("cachedAnime")
        if (cacheArray != null) {
            for (i in 0 until cacheArray.length()) {
                val obj = cacheArray.getJSONObject(i)
                val id = obj.optInt("id")
                val details = obj.optString("jsonDetails")
                if (id > 0 && details.isNotBlank()) {
                    cached.add(
                        CachedAnimeEntity(
                            id = id,
                            title = obj.optString("title", "Anime $id"),
                            jsonDetails = details,
                            lastCachedAt = obj.optLong("lastCachedAt", System.currentTimeMillis())
                        )
                    )
                }
            }
        }

        if (replaceAll) {
            animeDao.clearAllFavorites()
            animeDao.clearSearchHistory()
            animeDao.clearAllCache()
        }

        if (favorites.isNotEmpty()) {
            animeDao.insertFavorites(favorites)
        }
        if (searches.isNotEmpty()) {
            animeDao.insertSearchQueries(searches)
        }
        if (cached.isNotEmpty()) {
            animeDao.insertCachedAnimeList(cached)
        }

        return Result.success(
            RestoreSummary(
                favoritesRestored = favorites.size,
                searchesRestored = searches.size,
                cacheRestored = cached.size,
                replacedAll = replaceAll
            )
        )
    }
}
