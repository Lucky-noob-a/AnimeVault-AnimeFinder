package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_anime")
data class CachedAnimeEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val jsonDetails: String,
    val lastCachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteAnimeEntity(
    @PrimaryKey val id: Int,
    val anilistId: Int?,
    val malId: Int?,
    val title: String,
    val englishTitle: String?,
    val coverImageUrl: String?,
    val bannerImageUrl: String?,
    val score: Double?,
    val status: String,
    val format: String,
    val year: Int?,
    val genres: String, // comma-separated
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
