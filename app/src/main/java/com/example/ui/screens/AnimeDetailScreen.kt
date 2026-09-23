package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.AmoledAsyncImage
import com.example.data.model.CharacterCast
import com.example.data.model.DubInfo
import com.example.data.model.EpisodeItem
import com.example.data.model.SeasonInfo
import com.example.data.model.StreamingService
import com.example.data.model.TimelineEntry
import com.example.data.model.UpdateCategory
import com.example.data.model.VerificationStatus
import com.example.data.model.WebUpdate
import com.example.ui.components.LiveCountdownView
import com.example.ui.components.OfflineWarningBanner
import com.example.ui.components.SourceAttributionDialog
import com.example.ui.components.VerificationPill
import com.example.ui.components.AmoledMetadataLoadingView
import com.example.ui.components.AmoledNeonProgressIndicator
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.AmoledSurface
import com.example.ui.theme.AmoledSurfaceVariant
import com.example.ui.theme.ConflictAmber
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.RatingGold
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.UnconfirmedBlue
import com.example.ui.theme.VerifiedGreen
import com.example.ui.viewmodel.DetailsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimeDetailScreen(
    animeId: Int,
    malId: Int?,
    viewModel: DetailsViewModel,
    onBackClick: () -> Unit,
    onAnimeClick: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(animeId, malId) {
        viewModel.loadAnime(animeId, malId)
    }

    var previewEpisode by remember { mutableStateOf<EpisodeItem?>(null) }

    if (uiState.showSourcesDialog && uiState.details != null) {
        SourceAttributionDialog(
            sources = uiState.details!!.sourcesList,
            onDismiss = { viewModel.setSourcesDialogVisible(false) }
        )
    }

    if (uiState.showJumpToDialog) {
        JumpToEpisodeDialog(
            totalEpisodes = (uiState.details?.episodeCount?.value ?: uiState.episodes.size).coerceAtLeast(1),
            onDismiss = { viewModel.setShowJumpToDialog(false) },
            onJump = { epNum -> viewModel.jumpToEpisode(epNum) }
        )
    }

    previewEpisode?.let { ep ->
        EpisodePreviewDialog(
            episode = ep,
            onDismiss = { previewEpisode = null }
        )
    }

    Scaffold(
        containerColor = AmoledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            AmoledMetadataLoadingView(
                title = "Cross-checking Anime Metadata...",
                subtitle = "Querying AniList GraphQL, Jikan REST & Open-Web",
                neonColor = NeonCyan,
                indicatorSize = 52.dp,
                testTag = "detail_metadata_loading_view",
                modifier = Modifier.padding(innerPadding)
            )
        } else if (uiState.errorMessage != null && uiState.details == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ConflictAmber,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.errorMessage ?: "Failed to load",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.refresh() },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent)
                    ) {
                        Text("Retry", color = Color.White)
                    }
                }
            }
        } else if (uiState.details != null) {
            val details = uiState.details!!

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                // 1. HERO BACKDROP & HEADER
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        // Banner image
                        val backdropUrl = details.bannerImageUrl ?: details.coverImageUrl
                        AmoledAsyncImage(
                            model = backdropUrl,
                            contentDescription = details.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Gradient fading into AMOLED black
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Black.copy(alpha = 0.7f),
                                            AmoledBlack
                                        )
                                    )
                                )
                        )

                        // Top bar navigation & actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .border(1.dp, AmoledBorder, CircleShape)
                                    .testTag("back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = TextPrimary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = { viewModel.refresh() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .border(1.dp, AmoledBorder, CircleShape)
                                        .testTag("refresh_details_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.toggleFavorite() },
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                        .border(1.dp, AmoledBorder, CircleShape)
                                        .testTag("favorite_button")
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                        contentDescription = "Favorite",
                                        tint = if (uiState.isFavorite) CrimsonAccent else TextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        // Poster card and primary title overlay
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 20.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Poster
                            Box(
                                modifier = Modifier
                                    .width(100.dp)
                                    .height(148.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.5.dp, AmoledBorder, RoundedCornerShape(8.dp))
                                    .background(AmoledSurfaceVariant)
                            ) {
                                AmoledAsyncImage(
                                    model = details.coverImageUrl,
                                    contentDescription = details.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Main Titles
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                val displayTitle = when {
                                    details.title.isNotBlank() && !details.title.equals("null", ignoreCase = true) -> details.title
                                    !details.englishTitle.isNullOrBlank() && !details.englishTitle.equals("null", ignoreCase = true) -> details.englishTitle
                                    !details.japaneseTitle.isNullOrBlank() && !details.japaneseTitle.equals("null", ignoreCase = true) -> details.japaneseTitle
                                    else -> "Anime Details"
                                }

                                val cleanEnglish = details.englishTitle?.takeIf {
                                    it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals(displayTitle, ignoreCase = true)
                                }
                                val cleanJapanese = details.japaneseTitle?.takeIf {
                                    it.isNotBlank() && !it.equals("null", ignoreCase = true) && !it.equals(displayTitle, ignoreCase = true)
                                }

                                Text(
                                    text = displayTitle,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 24.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (cleanEnglish != null) {
                                    Text(
                                        text = cleanEnglish,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (cleanJapanese != null) {
                                    Text(
                                        text = cleanJapanese,
                                        color = TextTertiary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. OFFLINE WARNING BANNER (if cached)
                if (details.isFromCache) {
                    item {
                        OfflineWarningBanner(
                            lastCachedTimestamp = details.lastUpdated,
                            onRefresh = { viewModel.refresh() },
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                }

                // 3. VERIFICATION BADGE & DATA SOURCES
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            VerificationPill(
                                status = details.releaseDate.status,
                                label = when (details.releaseDate.status) {
                                    VerificationStatus.VERIFIED, VerificationStatus.MULTIPLE_SOURCES -> "✓ Multi-Source Verified"
                                    else -> "Open-Web Attributed"
                                },
                                onClick = { viewModel.setSourcesDialogVisible(true) }
                            )

                            // Inspect sources pill
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmoledCard)
                                    .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.setSourcesDialogVisible(true) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("sources_pill")
                            ) {
                                Icon(Icons.Default.Source, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sources (${details.sourcesList.size})", color = TextSecondary, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // 4. GENRE TAGS & TRAILER BUTTON
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            details.genres.forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmoledCard)
                                        .border(1.dp, AmoledBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(text = genre, color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                        }

                        if (!details.trailerUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(details.trailerUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // ignore
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("trailer_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Trailer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 5. LIVE AIRING & UPCOMING EPISODE CARD
                if (details.nextEpisode != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = AmoledCard),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(CrimsonAccent.copy(alpha = 0.5f), AmoledBorder))),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(CrimsonAccent)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "NEXT AIRING EPISODE",
                                            color = CrimsonAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    VerificationPill(
                                        status = if (details.nextEpisode.isConfirmed) VerificationStatus.VERIFIED else VerificationStatus.UNCONFIRMED,
                                        label = if (details.nextEpisode.isConfirmed) "Confirmed Schedule" else "Estimated"
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Episode ${details.nextEpisode.episodeNumber}",
                                            color = TextPrimary,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = details.nextEpisode.formattedDate,
                                            color = TextSecondary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Airs in",
                                            color = TextTertiary,
                                            fontSize = 11.sp
                                        )
                                        LiveCountdownView(targetEpochSeconds = details.nextEpisode.airingAtEpochSeconds)
                                    }
                                }

                                if (!details.dubInfo?.upcomingDubDate.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = AmoledBorder)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = ConflictAmber,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Upcoming Dub:",
                                                color = ConflictAmber,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = details.dubInfo.upcomingDubDate,
                                                color = TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        val licensorSource = details.dubInfo.dubSources.firstOrNull()
                                        if (licensorSource != null) {
                                            Text(
                                                text = licensorSource.name,
                                                color = TextTertiary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5.5 DUB & AUDIO STATUS CARD
                if (details.dubInfo != null) {
                    item {
                        DubInformationCard(
                            dubInfo = details.dubInfo,
                            streamingServices = details.streamingServices,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 4.dp)
                        )
                    }
                }

                // 6. QUICK FACTS GRID
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Key Information",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val facts = listOf(
                            "Format" to details.format,
                            "Score" to (details.score.displayText + if (details.score.value != null) " / 10" else ""),
                            "Status" to details.status.displayText,
                            "Episodes" to details.episodeCount.displayText,
                            "Dub Status" to (details.dubInfo?.dubStatus ?: "Sub Only"),
                            "Duration" to (if (details.durationMinutes != null) "${details.durationMinutes} min/ep" else "Unknown"),
                            "Release" to details.releaseDate.displayText,
                            "Season" to (if (details.season != null && details.year != null) "${details.season} ${details.year}" else details.year?.toString() ?: "Unknown"),
                            "Source" to (details.sourceMaterial ?: "Original"),
                            "Rating" to (details.ageRating ?: "Not Rated")
                        )

                        // 3-column clean AMOLED matrix
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(AmoledCard)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            facts.chunked(3).forEach { rowItems ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    rowItems.forEach { (label, value) ->
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = label, color = TextTertiary, fontSize = 11.sp)
                                            Text(
                                                text = value,
                                                color = TextPrimary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    // Fill empty columns if chunk size < 3
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }

                // 7. SYNOPSIS
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Synopsis",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = details.synopsis,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    }
                }

                // 8. CANONICAL SEASON ENGINE VIEW
                if (details.seasons.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Canonical Seasons",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Season Engine",
                                    color = CrimsonAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                details.seasons.forEach { season ->
                                    SeasonCard(
                                        season = season,
                                        isCurrent = season.animeId == details.id,
                                        onClick = {
                                            if (season.animeId != details.id) {
                                                onAnimeClick(season.animeId, null)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 9. COMPLETE FRANCHISE TIMELINE
                if (details.franchiseTimeline.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 10.dp)
                            ) {
                                Icon(Icons.Default.Timeline, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Complete Franchise Timeline",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(details.franchiseTimeline) { item ->
                                    TimelineEntryCard(
                                        entry = item,
                                        isCurrent = item.id == details.id,
                                        onClick = {
                                            if (item.id != details.id) {
                                                onAnimeClick(item.id, item.malId)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 10. EPISODE DATABASE BROWSER (Complete & Upcoming)
                item {
                    val allEpisodes = uiState.episodes
                    val airedCount = allEpisodes.count { it.isAired }
                    val upcomingCount = allEpisodes.count { !it.isAired }
                    val dubbedCount = allEpisodes.count { it.hasDub }

                    val filteredByChip = when (uiState.episodeFilter) {
                        "AIRED" -> allEpisodes.filter { it.isAired }
                        "UPCOMING" -> allEpisodes.filter { !it.isAired }
                        "DUBBED" -> allEpisodes.filter { it.hasDub }
                        else -> allEpisodes
                    }

                    val filteredEpisodes = if (uiState.episodeSearchQuery.isBlank()) {
                        filteredByChip
                    } else {
                        filteredByChip.filter {
                            it.episodeNumber.toString() == uiState.episodeSearchQuery.trim() ||
                                    it.title.contains(uiState.episodeSearchQuery, ignoreCase = true)
                        }
                    }

                    val sortedEpisodes = if (uiState.isEpisodeSortReversed) {
                        filteredEpisodes.sortedByDescending { it.episodeNumber }
                    } else {
                        filteredEpisodes.sortedBy { it.episodeNumber }
                    }

                    // Chunking for long series (batches of 50 episodes)
                    val chunkSize = 50
                    val totalEpisodesCount = sortedEpisodes.size
                    val chunkCount = if (totalEpisodesCount > 0) ((totalEpisodesCount - 1) / chunkSize) + 1 else 0
                    val isChunkingActive = totalEpisodesCount > 25
                    val activeChunkIndex = uiState.selectedEpisodeChunkIndex.coerceIn(0, (chunkCount - 1).coerceAtLeast(0))

                    val displayedEpisodes = if (!isChunkingActive || uiState.selectedEpisodeChunkIndex == -1 || chunkCount <= 1) {
                        sortedEpisodes
                    } else {
                        val start = activeChunkIndex * chunkSize
                        val end = (start + chunkSize).coerceAtMost(totalEpisodesCount)
                        if (start < totalEpisodesCount) sortedEpisodes.subList(start, end) else sortedEpisodes
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        // Header with Title & Quick Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "All Episodes Guide",
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (allEpisodes.isNotEmpty()) {
                                    Text(
                                        text = "${allEpisodes.size} Total • $airedCount Aired • $upcomingCount Upcoming",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Quick Action Tools: Jump to # and Grid/List toggle
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Jump to Episode button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .clickable { viewModel.setShowJumpToDialog(true) }
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                        .testTag("jump_to_episode_button"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.NearMe,
                                            contentDescription = "Jump to Episode",
                                            tint = NeonCyan,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Jump",
                                            color = NeonCyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Grid / List View Toggle
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (uiState.isEpisodeGridView) CrimsonAccent.copy(alpha = 0.2f) else AmoledCard)
                                        .border(1.dp, if (uiState.isEpisodeGridView) CrimsonAccent else AmoledBorder, RoundedCornerShape(6.dp))
                                        .clickable { viewModel.toggleEpisodeGridView() }
                                        .padding(6.dp)
                                        .testTag("toggle_episode_view_mode"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (uiState.isEpisodeGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                        contentDescription = if (uiState.isEpisodeGridView) "Detailed List View" else "Compact Grid Matrix",
                                        tint = if (uiState.isEpisodeGridView) CrimsonAccent else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter chips row + Sort order button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                item {
                                    Text(
                                        text = "All (${allEpisodes.size})",
                                        color = if (uiState.episodeFilter == "ALL") TextPrimary else TextTertiary,
                                        fontSize = 12.sp,
                                        fontWeight = if (uiState.episodeFilter == "ALL") FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (uiState.episodeFilter == "ALL") CrimsonAccent.copy(alpha = 0.2f) else AmoledCard)
                                            .border(1.dp, if (uiState.episodeFilter == "ALL") CrimsonAccent else AmoledBorder, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.onEpisodeFilterChanged("ALL") }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                item {
                                    Text(
                                        text = "Aired ($airedCount)",
                                        color = if (uiState.episodeFilter == "AIRED") TextPrimary else TextTertiary,
                                        fontSize = 12.sp,
                                        fontWeight = if (uiState.episodeFilter == "AIRED") FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (uiState.episodeFilter == "AIRED") CrimsonAccent.copy(alpha = 0.2f) else AmoledCard)
                                            .border(1.dp, if (uiState.episodeFilter == "AIRED") CrimsonAccent else AmoledBorder, RoundedCornerShape(6.dp))
                                            .clickable { viewModel.onEpisodeFilterChanged("AIRED") }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                                if (upcomingCount > 0) {
                                    item {
                                        Text(
                                            text = "Upcoming ($upcomingCount)",
                                            color = if (uiState.episodeFilter == "UPCOMING") Color(0xFF00E5FF) else TextTertiary,
                                            fontSize = 12.sp,
                                            fontWeight = if (uiState.episodeFilter == "UPCOMING") FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (uiState.episodeFilter == "UPCOMING") Color(0xFF00E5FF).copy(alpha = 0.2f) else AmoledCard)
                                                .border(1.dp, if (uiState.episodeFilter == "UPCOMING") Color(0xFF00E5FF) else AmoledBorder, RoundedCornerShape(6.dp))
                                                .clickable { viewModel.onEpisodeFilterChanged("UPCOMING") }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                if (dubbedCount > 0 || details.dubInfo?.isDubAvailable == true) {
                                    item {
                                        Text(
                                            text = "Dubbed ($dubbedCount)",
                                            color = if (uiState.episodeFilter == "DUBBED") VerifiedGreen else TextTertiary,
                                            fontSize = 12.sp,
                                            fontWeight = if (uiState.episodeFilter == "DUBBED") FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (uiState.episodeFilter == "DUBBED") VerifiedGreen.copy(alpha = 0.2f) else AmoledCard)
                                                .border(1.dp, if (uiState.episodeFilter == "DUBBED") VerifiedGreen else AmoledBorder, RoundedCornerShape(6.dp))
                                                .clickable { viewModel.onEpisodeFilterChanged("DUBBED") }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Sort order toggle: 1➔N or N➔1
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmoledCard)
                                    .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                                    .clickable { viewModel.toggleEpisodeSort() }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("toggle_episode_sort_order"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = "Sort Episodes",
                                        tint = if (uiState.isEpisodeSortReversed) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (uiState.isEpisodeSortReversed) "Latest First" else "Oldest First",
                                        color = if (uiState.isEpisodeSortReversed) NeonCyan else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Episode search / filter with instant clear
                        OutlinedTextField(
                            value = uiState.episodeSearchQuery,
                            onValueChange = { viewModel.onEpisodeSearch(it) },
                            placeholder = { Text("Search episode by title, # or keyword...", color = TextTertiary, fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (uiState.episodeSearchQuery.isNotBlank()) {
                                    IconButton(
                                        onClick = { viewModel.onEpisodeSearch("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AmoledCard,
                                unfocusedContainerColor = AmoledCard,
                                focusedBorderColor = CrimsonAccent,
                                unfocusedBorderColor = AmoledBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("episode_search_field")
                        )

                        // Chunk / Batch selector bar when series has many episodes
                        if (isChunkingActive && chunkCount > 1) {
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmoledCard.copy(alpha = 0.5f))
                                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Previous Batch Arrow
                                IconButton(
                                    onClick = {
                                        if (activeChunkIndex > 0 && uiState.selectedEpisodeChunkIndex != -1) {
                                            viewModel.setEpisodeChunkIndex(activeChunkIndex - 1)
                                        }
                                    },
                                    enabled = activeChunkIndex > 0 && uiState.selectedEpisodeChunkIndex != -1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowLeft,
                                        contentDescription = "Previous Batch",
                                        tint = if (activeChunkIndex > 0 && uiState.selectedEpisodeChunkIndex != -1) NeonCyan else TextTertiary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Batch Range Chips
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    items(chunkCount) { idx ->
                                        val startEp = idx * chunkSize + 1
                                        val endEp = minOf((idx + 1) * chunkSize, totalEpisodesCount)
                                        val isSelected = uiState.selectedEpisodeChunkIndex == idx

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isSelected) NeonCyan.copy(alpha = 0.2f)
                                                    else AmoledBlack
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isSelected) NeonCyan else AmoledBorder,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.setEpisodeChunkIndex(idx) }
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                                .testTag("episode_chunk_chip_$idx"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$startEp–$endEp",
                                                color = if (isSelected) NeonCyan else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }

                                    // "All" Chip
                                    item {
                                        val isAllSelected = uiState.selectedEpisodeChunkIndex == -1
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (isAllSelected) CrimsonAccent.copy(alpha = 0.2f)
                                                    else AmoledBlack
                                                )
                                                .border(
                                                    1.dp,
                                                    if (isAllSelected) CrimsonAccent else AmoledBorder,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { viewModel.setEpisodeChunkIndex(-1) }
                                                .padding(horizontal = 9.dp, vertical = 5.dp)
                                                .testTag("episode_chunk_all_chip"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "All ($totalEpisodesCount)",
                                                color = if (isAllSelected) TextPrimary else TextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }

                                // Next Batch Arrow
                                IconButton(
                                    onClick = {
                                        if (activeChunkIndex < chunkCount - 1 && uiState.selectedEpisodeChunkIndex != -1) {
                                            viewModel.setEpisodeChunkIndex(activeChunkIndex + 1)
                                        }
                                    },
                                    enabled = activeChunkIndex < chunkCount - 1 && uiState.selectedEpisodeChunkIndex != -1,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowRight,
                                        contentDescription = "Next Batch",
                                        tint = if (activeChunkIndex < chunkCount - 1 && uiState.selectedEpisodeChunkIndex != -1) NeonCyan else TextTertiary.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Range indicator status text
                            if (uiState.selectedEpisodeChunkIndex != -1) {
                                val currentRangeStart = activeChunkIndex * chunkSize + 1
                                val currentRangeEnd = minOf((activeChunkIndex + 1) * chunkSize, totalEpisodesCount)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Showing episodes $currentRangeStart–$currentRangeEnd of $totalEpisodesCount (Batch ${activeChunkIndex + 1} of $chunkCount)",
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        if (sortedEpisodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoadingEpisodes) {
                                    AmoledNeonProgressIndicator(
                                        size = 24.dp,
                                        strokeWidth = 2.5.dp,
                                        neonColor = NeonCyan,
                                        testTag = "detail_episodes_neon_spinner"
                                    )
                                } else {
                                    Text(
                                        text = if (details.malId == null && allEpisodes.isEmpty()) "Detailed episode list unavailable for this entry." else "No episodes match filter.",
                                        color = TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            if (uiState.isEpisodeGridView) {
                                // Compact Episode Matrix / Grid View
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    displayedEpisodes.forEach { ep ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (!ep.isAired) Color(0xFF00E5FF).copy(alpha = 0.15f)
                                                    else AmoledCard
                                                )
                                                .border(
                                                    1.dp,
                                                    if (!ep.isAired) Color(0xFF00E5FF).copy(alpha = 0.4f)
                                                    else AmoledBorder,
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .clickable { previewEpisode = ep }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                                .testTag("matrix_ep_${ep.episodeNumber}"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "EP ${ep.episodeNumber}",
                                                    color = if (!ep.isAired) Color(0xFF00E5FF) else TextPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (ep.hasDub) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(VerifiedGreen)
                                                    )
                                                }
                                                if (ep.isFiller) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(ConflictAmber)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Detailed Episode List Rows
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    displayedEpisodes.forEach { ep ->
                                        EpisodeRowItem(episode = ep)
                                    }
                                }
                            }

                            // Load More / Background Fetch for Paginated Long Series
                            if (uiState.hasNextEpisodePage) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            details.malId?.let {
                                                viewModel.loadEpisodes(it, uiState.currentEpisodePage + 1)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = AmoledCard),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).testTag("load_more_episodes_single_page")
                                    ) {
                                        if (uiState.isLoadingEpisodes) {
                                            AmoledNeonProgressIndicator(
                                                size = 14.dp,
                                                strokeWidth = 2.dp,
                                                neonColor = NeonCyan,
                                                showCenterPulse = false,
                                                testTag = "detail_load_more_neon_spinner"
                                            )
                                        } else {
                                            Text(
                                                text = "Page ${uiState.currentEpisodePage + 1}",
                                                color = TextPrimary,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            details.malId?.let {
                                                viewModel.loadNextFewEpisodePages(it, 3)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan.copy(alpha = 0.15f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1.5f).testTag("load_next_300_episodes_button")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Bolt,
                                                contentDescription = null,
                                                tint = NeonCyan,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "⚡ Fetch Next 300 Eps",
                                                color = NeonCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 11. CHARACTERS & VOICE CAST
                if (details.characters.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = "Characters & Voice Cast",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )

                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(details.characters) { cast ->
                                    CharacterCastCard(cast = cast)
                                }
                            }
                        }
                    }
                }

                // 12. STUDIOS & PRODUCERS
                if (details.studios.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Animation Studios & Production",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                details.studios.forEach { studio ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AmoledCard)
                                            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = studio.name,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 13. WHERE TO WATCH (STREAMING AVAILABILITY)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Where to Watch (Official Streams)",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (details.streamingServices.isEmpty()) {
                            Text(
                                text = "No official direct streaming catalog links found for this title.",
                                color = TextTertiary,
                                fontSize = 12.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                details.streamingServices.forEach { stream ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(AmoledCard)
                                            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                                            .clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(stream.url))
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    // ignore
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(text = stream.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                                Text(text = stream.regionNotice, color = TextTertiary, fontSize = 10.sp)
                                            }
                                        }

                                        Icon(Icons.Default.OpenInNew, contentDescription = "Open", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // 14. LATEST UPDATES & WEB NEWS
                if (details.webUpdates.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Recent News & Updates",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                details.webUpdates.forEach { update ->
                                    WebUpdateCard(update = update)
                                }
                            }
                        }
                    }
                }

                // 15. AUDIT TRAIL / SOURCES PANEL
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Data Verification Audit Trail",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Information gathered and cross-checked from multiple open sources.",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmoledCard)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.sourcesList.forEach { src ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = src.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        Text(text = src.details, color = TextTertiary, fontSize = 10.sp)
                                    }
                                    VerificationPill(status = src.status)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonCard(
    season: SeasonInfo,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCurrent) CrimsonAccent.copy(alpha = 0.15f) else AmoledCard)
            .border(
                1.dp,
                if (isCurrent) CrimsonAccent.copy(alpha = 0.5f) else AmoledBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = season.title,
                color = if (isCurrent) CrimsonAccent else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            val info = listOfNotNull(
                if (season.episodeCount != null) "${season.episodeCount} eps" else null,
                season.year?.toString()
            ).joinToString(" • ")
            if (info.isNotBlank()) {
                Text(text = info, color = TextSecondary, fontSize = 11.sp)
            }
        }

        if (isCurrent) {
            Text(
                text = "Viewing",
                color = CrimsonAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TimelineEntryCard(
    entry: TimelineEntry,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = if (isCurrent) CrimsonAccent.copy(alpha = 0.15f) else AmoledCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(if (isCurrent) CrimsonAccent else AmoledBorder, AmoledBorder.copy(alpha = 0.4f))
            )
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.width(110.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(AmoledSurfaceVariant)
            ) {
                AmoledAsyncImage(
                    model = entry.coverImageUrl,
                    contentDescription = entry.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Format badge
                Text(
                    text = entry.format,
                    color = TextPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                )
            }

            Column(modifier = Modifier.padding(6.dp)) {
                Text(
                    text = entry.title,
                    color = if (isCurrent) CrimsonAccent else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = entry.year?.toString() ?: entry.relationType,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DubInformationCard(
    dubInfo: DubInfo,
    streamingServices: List<StreamingService>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDub = dubInfo.isDubAvailable
    val isPending = dubInfo.dubStatus.contains("Pending", ignoreCase = true) ||
            dubInfo.dubStatus.contains("Simuldub", ignoreCase = true) ||
            dubInfo.dubStatus.contains("Announced", ignoreCase = true)

    val accentColor = when {
        isDub -> NeonCyan
        isPending -> ConflictAmber
        else -> TextTertiary
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(
                width = 1.dp,
                color = if (!dubInfo.upcomingDubDate.isNullOrBlank()) ConflictAmber.copy(alpha = 0.4f)
                        else if (isDub) NeonCyan.copy(alpha = 0.35f) else AmoledBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Header & Status Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Text(
                        text = "AUDIO & DUB",
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.6.sp
                    )
                }

                // Status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = dubInfo.dubStatus.uppercase(),
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.4.sp
                    )
                }
            }

            // Row 2: Audio Summary + available languages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDub) "Japanese (Original) • English Dub" else "Japanese Audio with Subtitles",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (dubInfo.availableLanguages.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dubInfo.availableLanguages.take(4).forEach { lang ->
                            val shortLang = when (lang.lowercase()) {
                                "english" -> "EN"
                                "spanish", "spanish (latin)" -> "ES"
                                "portuguese", "portuguese (brazil)" -> "PT"
                                "french" -> "FR"
                                "german" -> "DE"
                                "italian" -> "IT"
                                "japanese" -> "JA"
                                else -> lang.take(3).uppercase()
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AmoledSurfaceVariant)
                                    .border(0.7.dp, AmoledBorder, RoundedCornerShape(3.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = shortLang,
                                    color = TextSecondary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Row 3: Dedicated Upcoming Dub Date Banner
            if (!dubInfo.upcomingDubDate.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ConflictAmber.copy(alpha = 0.08f))
                        .border(1.dp, ConflictAmber.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.CalendarMonth,
                                    contentDescription = "Upcoming Dub Date",
                                    tint = ConflictAmber,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "UPCOMING DUB RELEASE",
                                    color = ConflictAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            if (dubInfo.upcomingDub?.delayFromSub != null) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmoledBlack.copy(alpha = 0.7f))
                                        .border(0.7.dp, ConflictAmber.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = dubInfo.upcomingDub.delayFromSub,
                                        color = TextSecondary,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dubInfo.upcomingDubDate,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (dubInfo.upcomingDub?.episodeNumber != null) {
                                    Text(
                                        text = "Next Dub: Episode ${dubInfo.upcomingDub.episodeNumber}",
                                        color = ConflictAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val nowSec = System.currentTimeMillis() / 1000L
                            if (dubInfo.upcomingDubEpochSeconds != null && dubInfo.upcomingDubEpochSeconds > nowSec) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Dub airs in",
                                        color = TextTertiary,
                                        fontSize = 10.sp
                                    )
                                    LiveCountdownView(targetEpochSeconds = dubInfo.upcomingDubEpochSeconds)
                                }
                            }
                        }
                    }
                }
            }

            // Row 4: Schedule / Licensor info
            if (!dubInfo.dubScheduleDetails.isNullOrBlank() || dubInfo.dubLicensor != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!dubInfo.dubScheduleDetails.isNullOrBlank()) {
                        Text(
                            text = dubInfo.dubScheduleDetails,
                            color = TextTertiary,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    if (dubInfo.dubLicensor != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Licensor: ${dubInfo.dubLicensor}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                }
            }

            // Row 5: Verified Dub Sources with clickable URLs
            if (dubInfo.dubSources.isNotEmpty()) {
                HorizontalDivider(color = AmoledBorder.copy(alpha = 0.5f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            Icons.Default.Source,
                            contentDescription = "Dub Sources",
                            tint = NeonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "VERIFIED DUB SOURCES (${dubInfo.dubSources.size})",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        dubInfo.dubSources.forEach { source ->
                            val hasUrl = !source.url.isNullOrBlank()
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmoledSurfaceVariant)
                                    .border(0.8.dp, if (hasUrl) NeonCyan.copy(alpha = 0.3f) else AmoledBorder, RoundedCornerShape(6.dp))
                                    .clickable(enabled = hasUrl) {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(source.url))
                                            context.startActivity(intent)
                                        } catch (_: Exception) {}
                                    }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Verified",
                                        tint = VerifiedGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = source.name,
                                        color = TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (hasUrl) {
                                        Icon(
                                            Icons.Default.OpenInNew,
                                            contentDescription = "Open Source Link",
                                            tint = TextTertiary,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeRowItem(
    episode: EpisodeItem
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledCard)
            .border(
                1.dp,
                if (!episode.isAired) Color(0xFF00E5FF).copy(alpha = 0.35f) else AmoledBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Episode Number badge
                Box(
                    modifier = Modifier
                        .width(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (!episode.isAired) Color(0xFF00E5FF).copy(alpha = 0.15f)
                            else CrimsonAccent.copy(alpha = 0.15f)
                        )
                        .padding(vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "EP ${episode.episodeNumber}",
                        color = if (!episode.isAired) Color(0xFF00E5FF) else CrimsonAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.title,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!episode.airDate.isNullOrBlank()) {
                            Text(
                                text = episode.airDate,
                                color = if (!episode.isAired) Color(0xFF00E5FF) else TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                        if (episode.durationMinutes != null) {
                            Text(
                                text = "• ${episode.durationMinutes}m",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Status Badges (Upcoming / Dub / Filler)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!episode.isAired) {
                    Text(
                        text = "UPCOMING",
                        color = Color(0xFF00E5FF),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF00E5FF).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                if (episode.hasDub) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(VerifiedGreen.copy(alpha = 0.12f))
                            .border(0.7.dp, VerifiedGreen.copy(alpha = 0.4f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "DUB",
                            color = VerifiedGreen,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.3.sp
                        )
                    }
                } else if (!episode.dubStatusText.isNullOrBlank()) {
                    val isPending = episode.dubStatusText.contains("wk", ignoreCase = true) ||
                            episode.dubStatusText.contains("Following", ignoreCase = true)
                    val badgeColor = if (isPending) ConflictAmber else TextTertiary
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(badgeColor.copy(alpha = 0.1f))
                            .border(0.7.dp, badgeColor.copy(alpha = 0.35f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = episode.dubStatusText,
                            color = badgeColor,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                if (episode.isFiller) {
                    Text(
                        text = "Filler",
                        color = ConflictAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(ConflictAmber.copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // Expandable synopsis or live countdown
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                HorizontalDivider(color = AmoledBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(8.dp))

                if (!episode.isAired && episode.airingAtEpochSeconds != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Projected Airing Countdown",
                            color = Color(0xFF00E5FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        LiveCountdownView(targetEpochSeconds = episode.airingAtEpochSeconds)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (!episode.synopsis.isNullOrBlank()) {
                    Text(
                        text = episode.synopsis,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                } else {
                    Text(
                        text = if (!episode.isAired) "Official episode synopsis has not been released by the studio yet."
                               else "Synopsis unavailable.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                if (!episode.dubReleaseDate.isNullOrBlank()) {
                    val context = LocalContext.current
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = ConflictAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Dub Release: ${episode.dubReleaseDate}",
                                color = ConflictAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (episode.dubSources.isNotEmpty()) {
                            val source = episode.dubSources.first()
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = if (!source.url.isNullOrBlank()) {
                                    Modifier.clickable {
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url)))
                                        } catch (_: Exception) {}
                                    }
                                } else Modifier
                            ) {
                                Text(
                                    text = "Source: ${source.name}",
                                    color = TextTertiary,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                                if (!source.url.isNullOrBlank()) {
                                    Icon(
                                        Icons.Default.OpenInNew,
                                        contentDescription = "Source URL",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterCastCard(
    cast: CharacterCast
) {
    Column(
        modifier = Modifier.width(115.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AmoledSurfaceVariant)
                .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
        ) {
            AmoledAsyncImage(
                model = cast.imageUrl,
                contentDescription = cast.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Role badge
            Text(
                text = cast.role.lowercase().replaceFirstChar { it.uppercase() },
                color = TextPrimary,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 3.dp, vertical = 1.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = cast.name,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!cast.voiceActorName.isNullOrBlank()) {
            Text(
                text = "JP: ${cast.voiceActorName}",
                color = TextTertiary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!cast.englishVoiceActorName.isNullOrBlank()) {
            Text(
                text = "EN: ${cast.englishVoiceActorName}",
                color = Color(0xFF00E5FF),
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun WebUpdateCard(
    update: WebUpdate
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .clickable(enabled = !update.url.isNullOrBlank()) {
                update.url?.let {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // ignore
                    }
                }
            }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val (badgeBg, badgeText, badgeColor) = when (update.category) {
                UpdateCategory.OFFICIAL_ANNOUNCEMENT -> Triple(VerifiedGreen.copy(alpha = 0.15f), "Official", VerifiedGreen)
                UpdateCategory.REPORTED -> Triple(UnconfirmedBlue.copy(alpha = 0.15f), "Reported", UnconfirmedBlue)
                UpdateCategory.RUMOR -> Triple(ConflictAmber.copy(alpha = 0.15f), "Rumor", ConflictAmber)
                UpdateCategory.UNCONFIRMED -> Triple(Color.White.copy(alpha = 0.1f), "Unconfirmed", TextSecondary)
            }

            Text(
                text = badgeText,
                color = badgeColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(badgeBg)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            Text(
                text = "${update.source} • ${update.publicationDate}",
                color = TextTertiary,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = update.headline,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = update.summary,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun JumpToEpisodeDialog(
    totalEpisodes: Int,
    onDismiss: () -> Unit,
    onJump: (Int) -> Unit
) {
    var textValue by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmoledCard,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NearMe,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Jump to Episode",
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Quick navigation for long anime series (Episodes 1 to $totalEpisodes):",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        textValue = input.filter { it.isDigit() }
                        errorMessage = null
                    },
                    label = { Text("Episode Number", color = TextTertiary, fontSize = 12.sp) },
                    placeholder = { Text("e.g. 50", color = TextTertiary) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmoledBlack,
                        unfocusedContainerColor = AmoledBlack,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = AmoledBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("jump_to_ep_input")
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = errorMessage!!, color = CrimsonAccent, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Quick Jumps:", color = TextTertiary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val presets = listOf(
                        "Ep 1" to 1,
                        "Mid (${totalEpisodes / 2})" to (totalEpisodes / 2).coerceAtLeast(1),
                        "Latest ($totalEpisodes)" to totalEpisodes
                    )
                    presets.forEach { (label, epNum) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmoledBlack)
                                .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                                .clickable { onJump(epNum) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val epNum = textValue.toIntOrNull()
                    if (epNum != null && epNum > 0) {
                        onJump(epNum)
                    } else {
                        errorMessage = "Please enter an episode between 1 and $totalEpisodes."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("jump_to_ep_confirm_button")
            ) {
                Text("Jump", color = AmoledBlack, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextTertiary)
            }
        }
    )
}

@Composable
fun EpisodePreviewDialog(
    episode: EpisodeItem,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AmoledCard,
        shape = RoundedCornerShape(14.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (!episode.isAired) Color(0xFF00E5FF).copy(alpha = 0.2f)
                                else CrimsonAccent.copy(alpha = 0.2f)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "EP ${episode.episodeNumber}",
                            color = if (!episode.isAired) Color(0xFF00E5FF) else CrimsonAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (episode.isFiller) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FILLER",
                            color = ConflictAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(ConflictAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextTertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            Column {
                Text(
                    text = episode.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!episode.airDate.isNullOrBlank()) {
                        Text(
                            text = if (!episode.isAired) "Airs: ${episode.airDate}" else "Aired: ${episode.airDate}",
                            color = if (!episode.isAired) Color(0xFF00E5FF) else TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    if (episode.durationMinutes != null) {
                        Text(
                            text = "• ${episode.durationMinutes} min",
                            color = TextTertiary,
                            fontSize = 12.sp
                        )
                    }
                }

                if (episode.hasDub || episode.dubStatusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AmoledSurfaceVariant)
                            .border(0.8.dp, AmoledBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 2.5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (episode.hasDub) VerifiedGreen else ConflictAmber)
                        )
                        Text(
                            text = "Audio: ${episode.dubStatusText ?: "English Dub Available"}",
                            color = if (episode.hasDub) VerifiedGreen else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!episode.synopsis.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = episode.synopsis,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = AmoledBorder),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Close", color = TextPrimary)
            }
        }
    )
}
