package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AnimeDao {
    // Cache queries
    @Query("SELECT * FROM cached_anime WHERE id = :id LIMIT 1")
    suspend fun getCachedAnime(id: Int): CachedAnimeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAnime(entity: CachedAnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedAnimeList(entities: List<CachedAnimeEntity>)

    @Query("SELECT * FROM cached_anime")
    suspend fun getAllCachedAnimeList(): List<CachedAnimeEntity>

    @Query("DELETE FROM cached_anime WHERE lastCachedAt < :threshold")
    suspend fun clearOldCache(threshold: Long)

    @Query("DELETE FROM cached_anime")
    suspend fun clearAllCache()

    // Favorites queries
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteAnimeEntity>>

    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    suspend fun getAllFavoritesList(): List<FavoriteAnimeEntity>

    @Query("SELECT * FROM favorites WHERE id = :id LIMIT 1")
    fun getFavoriteById(id: Int): Flow<FavoriteAnimeEntity?>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE id = :id)")
    suspend fun isFavorite(id: Int): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(entity: FavoriteAnimeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(entities: List<FavoriteAnimeEntity>)

    @Query("DELETE FROM favorites WHERE id = :id")
    suspend fun removeFavorite(id: Int)

    @Query("DELETE FROM favorites")
    suspend fun clearAllFavorites()

    // Search History queries
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 20")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Query("SELECT * FROM search_history ORDER BY timestamp DESC")
    suspend fun getAllSearchHistoryList(): List<SearchHistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQuery(query: SearchHistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchQueries(entities: List<SearchHistoryEntity>)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearchQuery(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()

    // Stats
    @Query("SELECT COUNT(*) FROM favorites")
    suspend fun getFavoritesCount(): Int

    @Query("SELECT COUNT(*) FROM search_history")
    suspend fun getSearchHistoryCount(): Int

    @Query("SELECT COUNT(*) FROM cached_anime")
    suspend fun getCachedAnimeCount(): Int
}
