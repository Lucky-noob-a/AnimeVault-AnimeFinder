package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AnimeSummary
import com.example.ui.components.AnimePosterCard
import com.example.ui.components.ErrorMessageView
import com.example.ui.components.LoadingSkeleton
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.RatingGold
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
                            .padding(vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        LoadingTrendingSkeleton()
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

                // Section: Trending Now (Featured AMOLED Widescreen Showcase)
                if (uiState.trending.isNotEmpty()) {
                    item {
                        TrendingNowSection(
                            trendingList = uiState.trending,
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
fun TrendingNowSection(
    trendingList: List<AnimeSummary>,
    onAnimeClick: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .testTag("trending_now_section")
    ) {
        // Section Header with AMOLED styling and neon accents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(CrimsonAccent.copy(alpha = 0.25f), NeonAmber.copy(alpha = 0.2f))
                            )
                        )
                        .border(
                            1.dp,
                            Brush.linearGradient(listOf(CrimsonAccent, NeonAmber)),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Trending Now",
                        tint = CrimsonAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Trending Now",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.4).sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // AMOLED Neon Live Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonEmerald.copy(alpha = 0.12f))
                                .border(1.dp, NeonEmerald.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "LIVE",
                                    color = NeonEmerald,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = "Most watched & discussed across community",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Text(
                text = "${trendingList.size} Titles",
                color = TextTertiary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal Scrollable Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.testTag("trending_now_row")
        ) {
            itemsIndexed(trendingList, key = { _, it -> it.id }) { index, anime ->
                TrendingThumbnailCard(
                    rank = index + 1,
                    anime = anime,
                    onClick = { onAnimeClick(anime.id, anime.malId) }
                )
            }
        }
    }
}

@Composable
fun TrendingThumbnailCard(
    rank: Int,
    anime: AnimeSummary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Choose the best high-quality thumbnail: banner image preferred for widescreen, or fallback to cover
    val thumbnailImage = remember(anime.bannerImageUrl, anime.coverImageUrl) {
        if (!anime.bannerImageUrl.isNullOrBlank()) anime.bannerImageUrl else anime.coverImageUrl
    }

    val (rankBadgeGradient, rankBadgeTextColor) = when (rank) {
        1 -> listOf(Color(0xFFFF3366), NeonAmber) to AmoledBlack
        2 -> listOf(NeonCyan, Color(0xFF0077FE)) to AmoledBlack
        3 -> listOf(NeonViolet, CrimsonAccent) to TextPrimary
        else -> listOf(AmoledCard, AmoledSurfaceVariant) to TextPrimary
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = AmoledBlack),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            1.dp,
            Brush.verticalGradient(
                listOf(
                    if (rank <= 3) CrimsonAccent.copy(alpha = 0.6f) else AmoledBorder,
                    AmoledBorder.copy(alpha = 0.3f)
                )
            )
        ),
        modifier = modifier
            .width(260.dp)
            .testTag("trending_card_${anime.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AmoledBlack)
        ) {
            // High-quality Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(145.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                    .background(AmoledSurfaceVariant)
            ) {
                if (!thumbnailImage.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailImage,
                        contentDescription = anime.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Deep AMOLED multi-stop vignette gradient for true-black blending
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.35f),
                                    Color.Transparent,
                                    AmoledBlack.copy(alpha = 0.6f),
                                    AmoledBlack
                                )
                            )
                        )
                )

                // Top Bar: Rank Badge (Left) & Rating Badge (Right)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ranking Badge with fiery AMOLED highlights
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Brush.horizontalGradient(rankBadgeGradient))
                            .border(
                                1.dp,
                                if (rank <= 3) Color.White.copy(alpha = 0.4f) else AmoledBorder,
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (rank == 1) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = AmoledBlack,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                            }
                            Text(
                                text = "#$rank",
                                color = rankBadgeTextColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Score pill
                    if (anime.score != null && anime.score > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmoledBlack.copy(alpha = 0.85f))
                                .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Score",
                                    tint = RatingGold,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = String.format("%.1f", anime.score),
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Format Pill at bottom of thumbnail
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(AmoledBlack.copy(alpha = 0.85f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = anime.format,
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Anime Meta Info (Title, status, episodes, genres)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(
                    text = anime.title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )

                if (!anime.englishTitle.isNullOrBlank() && anime.englishTitle != anime.title) {
                    Text(
                        text = anime.englishTitle,
                        color = TextTertiary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom details row: Year/Episodes + Genres
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = buildString {
                                anime.seasonYear?.let { append(it) }
                                if (anime.episodes != null) {
                                    if (isNotEmpty()) append(" • ")
                                    append("${anime.episodes} eps")
                                } else if (anime.status.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(anime.status)
                                }
                            }.ifBlank { anime.status },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Compact Genre tag if available
                    val primaryGenre = anime.genres.firstOrNull()
                    if (!primaryGenre.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(AmoledCard)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = primaryGenre,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingTrendingSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadingSkeleton(
                modifier = Modifier
                    .width(140.dp)
                    .height(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            repeat(2) {
                LoadingSkeleton(
                    modifier = Modifier
                        .width(260.dp)
                        .height(210.dp)
                )
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
