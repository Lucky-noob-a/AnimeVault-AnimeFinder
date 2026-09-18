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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Source
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
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

    if (uiState.showSourcesDialog && uiState.details != null) {
        SourceAttributionDialog(
            sources = uiState.details!!.sourcesList,
            onDismiss = { viewModel.setSourcesDialogVisible(false) }
        )
    }

    Scaffold(
        containerColor = AmoledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CrimsonAccent, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Cross-checking AniList, Jikan & Open-Web...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
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
                        if (!backdropUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = backdropUrl,
                                contentDescription = details.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

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
                                if (!details.coverImageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = details.coverImageUrl,
                                        contentDescription = details.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            // Main Titles
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = details.title,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 24.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (!details.englishTitle.isNullOrBlank() && !details.englishTitle.equals(details.title, ignoreCase = true)) {
                                    Text(
                                        text = details.englishTitle,
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!details.japaneseTitle.isNullOrBlank()) {
                                    Text(
                                        text = details.japaneseTitle,
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

                // 3. VERIFICATION BADGE & SOURCE CONFLICT ALERT
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
                                    VerificationStatus.CONFLICTING -> "⚠ Source Conflict Detected"
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

                        // Conflict details banner if conflict exists
                        if (details.releaseDate.status == VerificationStatus.CONFLICTING || details.episodeCount.status == VerificationStatus.CONFLICTING) {
                            val conflictText = details.releaseDate.conflictDetails ?: details.episodeCount.conflictDetails ?: "Discrepancy found across data sources."
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ConflictAmber.copy(alpha = 0.15f))
                                    .border(1.dp, ConflictAmber.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = ConflictAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = conflictText,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
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
                                .padding(horizontal = 20.dp, vertical = 6.dp)
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

                    var showAllEpisodes by remember { mutableStateOf(false) }

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
                            Column {
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
                            if (details.dubInfo?.isDubAvailable == true) {
                                Text(
                                    text = "DUBBED ($dubbedCount)",
                                    color = VerifiedGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(VerifiedGreen.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter chips: All, Aired, Upcoming, Dubbed
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Episode search / filter
                        OutlinedTextField(
                            value = uiState.episodeSearchQuery,
                            onValueChange = { viewModel.onEpisodeSearch(it) },
                            placeholder = { Text("Filter episodes by title or #...", color = TextTertiary, fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextTertiary, modifier = Modifier.size(16.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = AmoledCard,
                                unfocusedContainerColor = AmoledCard,
                                focusedBorderColor = CrimsonAccent,
                                unfocusedBorderColor = AmoledBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (filteredEpisodes.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isLoadingEpisodes) {
                                    CircularProgressIndicator(color = CrimsonAccent, modifier = Modifier.size(24.dp))
                                } else {
                                    Text(
                                        text = if (details.malId == null && allEpisodes.isEmpty()) "Detailed episode list unavailable for this entry." else "No episodes match filter.",
                                        color = TextTertiary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        } else {
                            val displayLimit = if (showAllEpisodes || filteredEpisodes.size <= 30) filteredEpisodes.size else 30

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                filteredEpisodes.take(displayLimit).forEach { ep ->
                                    EpisodeRowItem(episode = ep)
                                }
                            }

                            if (filteredEpisodes.size > 30) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showAllEpisodes = !showAllEpisodes },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmoledCard),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (showAllEpisodes) "Show Less" else "Show All ${filteredEpisodes.size} Episodes (Including Upcoming)",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            if (uiState.hasNextEpisodePage) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        details.malId?.let {
                                            viewModel.loadEpisodes(it, uiState.currentEpisodePage + 1)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmoledCard),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (uiState.isLoadingEpisodes) {
                                        CircularProgressIndicator(color = CrimsonAccent, modifier = Modifier.size(16.dp))
                                    } else {
                                        Text("Load More Broadcast History (Page ${uiState.currentEpisodePage + 1})", color = TextPrimary, fontSize = 12.sp)
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
                if (!entry.coverImageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = entry.coverImageUrl,
                        contentDescription = entry.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

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

@Composable
fun DubInformationCard(
    dubInfo: DubInfo,
    streamingServices: List<StreamingService>,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AmoledCard),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(
                    if (dubInfo.isDubAvailable) Color(0xFF00E5FF).copy(alpha = 0.5f) else AmoledBorder,
                    AmoledBorder
                )
            )
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
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
                            .background(if (dubInfo.isDubAvailable) Color(0xFF00E5FF) else ConflictAmber)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AUDIO & DUB INFORMATION",
                        color = if (dubInfo.isDubAvailable) Color(0xFF00E5FF) else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }

                val (badgeBg, badgeTextColor) = when {
                    dubInfo.isDubAvailable -> Pair(Color(0xFF00E5FF).copy(alpha = 0.15f), Color(0xFF00E5FF))
                    dubInfo.dubStatus.contains("Pending", ignoreCase = true) -> Pair(ConflictAmber.copy(alpha = 0.15f), ConflictAmber)
                    else -> Pair(AmoledSurfaceVariant, TextTertiary)
                }
                Text(
                    text = dubInfo.dubStatus.uppercase(),
                    color = badgeTextColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (dubInfo.isDubAvailable) "English & Regional Dubs Available" else "Japanese Audio with Subtitles",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            if (!dubInfo.dubScheduleDetails.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dubInfo.dubScheduleDetails,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Languages & Licensor row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dubInfo.availableLanguages.forEach { lang ->
                    Text(
                        text = lang,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AmoledSurfaceVariant)
                            .border(1.dp, AmoledBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                if (dubInfo.dubLicensor != null) {
                    Text(
                        text = "Licensor: ${dubInfo.dubLicensor}",
                        color = TextTertiary,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AmoledSurfaceVariant)
                            .border(1.dp, AmoledBorder, RoundedCornerShape(4.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
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
                    Text(
                        text = "SUB & DUB",
                        color = VerifiedGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VerifiedGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                } else if (!episode.dubStatusText.isNullOrBlank()) {
                    val isPending = episode.dubStatusText.contains("wk", ignoreCase = true) ||
                            episode.dubStatusText.contains("Following", ignoreCase = true)
                    Text(
                        text = episode.dubStatusText,
                        color = if (isPending) ConflictAmber else TextTertiary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isPending) ConflictAmber.copy(alpha = 0.12f) else AmoledSurfaceVariant)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    )
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
            if (!cast.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = cast.imageUrl,
                    contentDescription = cast.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

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
