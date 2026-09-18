package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upcoming
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AnimeSummary
import com.example.ui.components.AnimePosterCard
import com.example.ui.components.ErrorMessageView
import com.example.ui.components.LoadingSkeleton
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAnimeClick: (Int, Int?) -> Unit,
    onNavigateToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val favorites by viewModel.favorites.collectAsState()

    Scaffold(
        containerColor = AmoledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // App Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(CrimsonAccent)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AnimeVault",
                                color = TextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Text(
                            text = "Discover any anime.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    IconButton(
                        onClick = { viewModel.loadHomeData() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(AmoledCard)
                            .border(1.dp, AmoledBorder, CircleShape)
                            .testTag("refresh_home_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Data",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Search Bar Trigger
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
                        .clickable { onNavigateToSearch() }
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("home_search_trigger")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextTertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Search anime (e.g. One Piece, Attack on Titan)...",
                            color = TextTertiary,
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (uiState.isLoading) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        LoadingSectionSkeleton("Trending Now")
                        LoadingSectionSkeleton("Currently Airing")
                    }
                }
            } else if (uiState.errorMessage != null && uiState.trending.isEmpty()) {
                item {
                    ErrorMessageView(
                        message = uiState.errorMessage ?: "Failed to load",
                        onRetry = { viewModel.loadHomeData() }
                    )
                }
            } else {
                // Section: Your Favorites (if any)
                if (favorites.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Your Favorites",
                            icon = Icons.Default.Favorite,
                            items = favorites.map {
                                AnimeSummary(
                                    id = it.id,
                                    anilistId = it.anilistId,
                                    malId = it.malId,
                                    title = it.title,
                                    englishTitle = it.englishTitle,
                                    coverImageUrl = it.coverImageUrl,
                                    bannerImageUrl = it.bannerImageUrl,
                                    score = it.score,
                                    status = it.status,
                                    format = it.format,
                                    seasonYear = it.year
                                )
                            },
                            onAnimeClick = onAnimeClick
                        )
                    }
                }

                // Section: Trending
                if (uiState.trending.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Trending Now",
                            icon = Icons.Default.Whatshot,
                            items = uiState.trending,
                            onAnimeClick = onAnimeClick
                        )
                    }
                }

                // Section: Currently Airing
                if (uiState.airing.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Currently Airing",
                            icon = Icons.Default.PlayArrow,
                            items = uiState.airing,
                            onAnimeClick = onAnimeClick
                        )
                    }
                }

                // Section: Popular
                if (uiState.popular.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "All-Time Popular",
                            icon = Icons.Default.ElectricBolt,
                            items = uiState.popular,
                            onAnimeClick = onAnimeClick
                        )
                    }
                }

                // Section: Upcoming
                if (uiState.upcoming.isNotEmpty()) {
                    item {
                        HomeSection(
                            title = "Upcoming Anticipated",
                            icon = Icons.Default.Upcoming,
                            items = uiState.upcoming,
                            onAnimeClick = onAnimeClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSection(
    title: String,
    icon: ImageVector,
    items: List<AnimeSummary>,
    onAnimeClick: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CrimsonAccent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(items, key = { it.id }) { anime ->
                AnimePosterCard(
                    anime = anime,
                    onClick = { onAnimeClick(anime.id, anime.malId) }
                )
            }
        }
    }
}

@Composable
fun LoadingSectionSkeleton(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                LoadingSkeleton(
                    modifier = Modifier
                        .width(130.dp)
                        .height(190.dp)
                )
            }
        }
    }
}
