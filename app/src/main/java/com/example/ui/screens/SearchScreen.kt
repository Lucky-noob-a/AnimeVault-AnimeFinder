package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.ui.components.AmoledMetadataLoadingView
import com.example.ui.components.AmoledNeonProgressIndicator
import com.example.ui.theme.NeonCyan
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.AnimeCategory
import com.example.data.model.AnimeCategoryCatalog
import com.example.data.model.AnimeSummary
import com.example.data.model.CategoryType
import com.example.ui.components.AmoledAsyncImage
import com.example.ui.components.ErrorMessageView
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
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.TagFilterTab
import com.example.ui.viewmodel.TagMatchMode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onAnimeClick: (Int, Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val focusManager = LocalFocusManager.current

    val formatFilters = listOf("ALL", "TV", "MOVIE", "OVA", "SPECIAL")

    // Dynamic category & theme tags collected from results plus catalog
    val allAvailableTags = remember(uiState.results) {
        val resultTags = uiState.results
            .flatMap { it.genres }
            .filter { it.isNotBlank() }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .map { it.first }

        val catalogList = AnimeCategoryCatalog.ALL_CATEGORIES.map { it.name }
        (resultTags + catalogList).distinct()
    }

    // Filter tags depending on the active tab (All Tags, Genres, Themes, Demographics)
    val displayedTags = remember(allAvailableTags, uiState.activeTagTab) {
        val filtered = when (uiState.activeTagTab) {
            TagFilterTab.ALL -> allAvailableTags
            TagFilterTab.GENRES -> allAvailableTags.filter {
                AnimeCategoryCatalog.isGenre(it) || (!AnimeCategoryCatalog.isTheme(it) && !AnimeCategoryCatalog.isDemographic(it))
            }
            TagFilterTab.THEMES -> allAvailableTags.filter { AnimeCategoryCatalog.isTheme(it) }
            TagFilterTab.DEMOGRAPHICS -> allAvailableTags.filter { AnimeCategoryCatalog.isDemographic(it) }
        }
        listOf("ALL") + filtered
    }

    // Counts per tag based on the active format filter
    val tagCounts = remember(uiState.results, uiState.selectedFormat) {
        val formatFiltered = if (uiState.selectedFormat == "ALL") {
            uiState.results
        } else {
            uiState.results.filter { it.format.equals(uiState.selectedFormat, ignoreCase = true) }
        }
        val counts = mutableMapOf<String, Int>()
        counts["ALL"] = formatFiltered.size
        formatFiltered.forEach { anime ->
            anime.genres.forEach { tag ->
                counts[tag] = (counts[tag] ?: 0) + 1
            }
        }
        counts
    }

    // Dynamically filtered results based on both format and multi-tag filters
    val filteredResults = remember(
        uiState.results,
        uiState.selectedFormat,
        uiState.selectedTags,
        uiState.selectedGenre,
        uiState.selectedGenres,
        uiState.tagMatchMode
    ) {
        uiState.results.filter { anime ->
            val matchesFormat = uiState.selectedFormat == "ALL" ||
                    anime.format.equals(uiState.selectedFormat, ignoreCase = true)

            val activeTags = when {
                uiState.selectedTags.isNotEmpty() -> uiState.selectedTags
                uiState.selectedGenres.isNotEmpty() -> uiState.selectedGenres
                uiState.selectedGenre != "ALL" -> setOf(uiState.selectedGenre)
                else -> emptySet()
            }

            val matchesTags = when {
                activeTags.isEmpty() -> true
                uiState.tagMatchMode == TagMatchMode.ALL -> {
                    activeTags.all { tag ->
                        anime.genres.any { it.equals(tag, ignoreCase = true) }
                    }
                }
                else -> { // TagMatchMode.ANY
                    activeTags.any { tag ->
                        anime.genres.any { it.equals(tag, ignoreCase = true) }
                    }
                }
            }

            matchesFormat && matchesTags
        }
    }

    Scaffold(
        containerColor = AmoledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header Search Input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    placeholder = {
                        Text("Search anime across AniList & Jikan...", color = TextTertiary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (uiState.query.isNotBlank()) CrimsonAccent else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (uiState.query.isNotBlank()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        focusManager.clearFocus()
                        viewModel.performSearch(uiState.query)
                    }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = AmoledCard,
                        unfocusedContainerColor = AmoledCard,
                        focusedBorderColor = CrimsonAccent,
                        unfocusedBorderColor = AmoledBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = CrimsonAccent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Format filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(formatFilters) { format ->
                        val isSelected = uiState.selectedFormat == format
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.onSelectFormat(format) },
                            label = {
                                Text(
                                    text = if (format == "ALL") "All Formats" else format,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AmoledCard,
                                selectedContainerColor = CrimsonAccent.copy(alpha = 0.2f),
                                labelColor = TextSecondary,
                                selectedLabelColor = CrimsonAccent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = AmoledBorder,
                                selectedBorderColor = CrimsonAccent
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = AmoledBorder)

            // Content Area
            if (uiState.isLoading) {
                AmoledMetadataLoadingView(
                    title = "Searching Anime Databases...",
                    subtitle = "Cross-referencing AniList, Jikan & Web APIs",
                    neonColor = NeonCyan,
                    indicatorSize = 44.dp,
                    testTag = "search_metadata_loading_view"
                )
            } else if (uiState.query.isBlank() && !uiState.hasSearched) {
                // Recent Searches & Popular Suggestions
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (recentSearches.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Recent Searches",
                                        color = TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.clearAllHistory() },
                                    modifier = Modifier.testTag("clear_history_button")
                                ) {
                                    Text("Clear All", color = CrimsonAccent, fontSize = 12.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(recentSearches) { searchItem ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.performSearch(searchItem)
                                    }
                                    .padding(vertical = 10.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = searchItem,
                                    color = TextPrimary,
                                    fontSize = 14.sp
                                )
                                IconButton(
                                    onClick = { viewModel.deleteHistoryItem(searchItem) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = TextTertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = AmoledBorder)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Quick Jump Suggestions
                    item {
                        Text(
                            text = "Popular Searches",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val suggestions = listOf("One Piece", "Attack on Titan", "Demon Slayer", "Solo Leveling", "Jujutsu Kaisen", "Bleach", "Naruto", "Frieren")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suggestions.take(4).forEach { s ->
                                    SuggestionChip(text = s, onClick = { viewModel.performSearch(s) })
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                suggestions.drop(4).forEach { s ->
                                    SuggestionChip(text = s, onClick = { viewModel.performSearch(s) })
                                }
                            }
                        }

                        // Explore by Genre Quick Row
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalOffer,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Explore by Genre",
                                color = TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("explore_genres_row")
                        ) {
                            val popularGenres = listOf("Action", "Romance", "Seinen", "Shounen", "Fantasy", "Sci-Fi", "Comedy", "Isekai", "Psychological", "Adventure", "Mystery", "Supernatural", "Slice of Life", "Sports")
                            items(popularGenres) { genre ->
                                val cat = AnimeCategoryCatalog.find(genre)
                                val emoji = cat?.emoji ?: "🎯"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(AmoledCard)
                                        .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.exploreGenre(genre) }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                        .testTag("explore_genre_${genre.lowercase()}")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = emoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = genre,
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // Browse Categories Hub (Action, Romance, Seinen, etc.)
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CrimsonAccent.copy(alpha = 0.15f))
                                        .border(1.dp, CrimsonAccent.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterList,
                                        contentDescription = null,
                                        tint = CrimsonAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Browse Categories",
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Curated collections by genre, demographic & theme",
                                        color = TextTertiary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Type Filter Pills (All, Genres, Demographics, Themes)
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.testTag("category_type_filter_row")
                        ) {
                            items(CategoryType.values()) { type ->
                                val isSelected = uiState.selectedCategoryType == type
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.onSelectCategoryType(type) },
                                    label = {
                                        Text(
                                            text = type.displayName,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = AmoledCard,
                                        selectedContainerColor = NeonCyan.copy(alpha = 0.2f),
                                        labelColor = TextSecondary,
                                        selectedLabelColor = NeonCyan
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = AmoledBorder,
                                        selectedBorderColor = NeonCyan
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filtered category cards displayed in 2-column layout
                        val displayedCategories = remember(uiState.selectedCategoryType) {
                            when (uiState.selectedCategoryType) {
                                CategoryType.ALL -> AnimeCategoryCatalog.ALL_CATEGORIES
                                CategoryType.GENRE -> AnimeCategoryCatalog.ALL_CATEGORIES.filter { it.type == CategoryType.GENRE }
                                CategoryType.DEMOGRAPHIC -> AnimeCategoryCatalog.ALL_CATEGORIES.filter { it.type == CategoryType.DEMOGRAPHIC }
                                CategoryType.THEME -> AnimeCategoryCatalog.ALL_CATEGORIES.filter { it.type == CategoryType.THEME }
                            }
                        }

                        val pairs = displayedCategories.chunked(2)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("category_cards_grid")
                        ) {
                            pairs.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CategoryCard(
                                        category = pair[0],
                                        onClick = { viewModel.browseCategory(pair[0].name) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (pair.size > 1) {
                                        CategoryCard(
                                            category = pair[1],
                                            onClick = { viewModel.browseCategory(pair[1].name) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Search Results or Category Browse Screen with Tag Filter UI at the top
                Column(modifier = Modifier.fillMaxSize()) {
                    val activeTags = uiState.selectedTags.ifEmpty {
                        if (uiState.selectedGenre != "ALL") setOf(uiState.selectedGenre) else emptySet()
                    }

                    TagFilterBar(
                        tags = displayedTags,
                        selectedTags = activeTags,
                        tagCounts = tagCounts,
                        activeTab = uiState.activeTagTab,
                        matchMode = uiState.tagMatchMode,
                        onSelectTab = { viewModel.setTagFilterTab(it) },
                        onToggleTag = { viewModel.toggleTag(it) },
                        onToggleMatchMode = { viewModel.toggleTagMatchMode() },
                        onOpenAllTags = { viewModel.setTagPickerExpanded(true) },
                        onClearAllTags = { viewModel.clearAllTags() }
                    )

                    HorizontalDivider(color = AmoledBorder)

                    if ((uiState.isCategoryBrowseMode || activeTags.isNotEmpty()) && uiState.query.isBlank()) {
                        TagBrowseHeroBanner(
                            selectedTags = activeTags,
                            matchMode = uiState.tagMatchMode,
                            count = filteredResults.size,
                            onClear = { viewModel.clearAllTags() },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    if (filteredResults.isEmpty()) {
                        if (activeTags.isNotEmpty()) {
                            EmptyTagFilterView(
                                selectedTags = activeTags,
                                totalCount = tagCounts["ALL"] ?: uiState.results.size,
                                query = uiState.query,
                                matchMode = uiState.tagMatchMode,
                                onSwitchMatchMode = { viewModel.toggleTagMatchMode() },
                                onReset = { viewModel.clearAllTags() }
                            )
                        } else {
                            ErrorMessageView(
                                message = uiState.errorMessage ?: "No anime found for '${uiState.query}'. Try another title."
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val resultsText = if (activeTags.isEmpty()) {
                                        "${filteredResults.size} results found"
                                    } else {
                                        "${filteredResults.size} titles matching tags"
                                    }
                                    Text(
                                        text = resultsText,
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )

                                    if (activeTags.isNotEmpty()) {
                                        Text(
                                            text = "Filtered from ${tagCounts["ALL"] ?: uiState.results.size} total (${uiState.tagMatchMode.displayName})",
                                            color = TextTertiary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            items(filteredResults, key = { it.id }) { anime ->
                                SearchResultItem(
                                    anime = anime,
                                    selectedTags = activeTags,
                                    onTagClick = { tag -> viewModel.toggleTag(tag) },
                                    onClick = { onAnimeClick(anime.id, anime.malId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (uiState.isTagPickerExpanded) {
        TagFilterBottomSheet(
            viewModel = viewModel,
            tagCounts = tagCounts,
            totalResults = filteredResults.size,
            onDismiss = { viewModel.setTagPickerExpanded(false) }
        )
    }
}

@Composable
fun SuggestionChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SearchResultItem(
    anime: AnimeSummary,
    selectedTags: Set<String> = emptySet(),
    onTagClick: ((String) -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Poster
        Box(
            modifier = Modifier
                .width(68.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(AmoledSurfaceVariant)
        ) {
            AmoledAsyncImage(
                model = anime.coverImageUrl,
                contentDescription = anime.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Info
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = anime.title,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (anime.score != null && anime.score > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = RatingGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = String.format("%.1f", anime.score),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (!anime.englishTitle.isNullOrBlank() && !anime.englishTitle.equals(anime.title, ignoreCase = true)) {
                Text(
                    text = anime.englishTitle,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Metadata row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = anime.format,
                    color = CrimsonAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "•",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
                Text(
                    text = anime.seasonYear?.toString() ?: anime.status,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                if (anime.episodes != null) {
                    Text(
                        text = "•",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "${anime.episodes} eps",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Verification & Interactive Tag Pills
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VerificationPill(status = anime.verificationStatus)

                if (anime.genres.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        items(anime.genres.take(4)) { tag ->
                            val isSelected = selectedTags.any { it.equals(tag, ignoreCase = true) }
                            val isTheme = AnimeCategoryCatalog.isTheme(tag)
                            val cat = AnimeCategoryCatalog.find(tag)
                            val emoji = cat?.emoji

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when {
                                            isSelected && isTheme -> RatingGold.copy(alpha = 0.25f)
                                            isSelected -> NeonCyan.copy(alpha = 0.25f)
                                            else -> AmoledSurface
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 1.dp else 0.5.dp,
                                        color = when {
                                            isSelected && isTheme -> RatingGold
                                            isSelected -> NeonCyan
                                            else -> AmoledBorder
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { onTagClick?.invoke(tag) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (emoji != null) {
                                        Text(text = emoji, fontSize = 9.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                    }
                                    Text(
                                        text = tag,
                                        color = when {
                                            isSelected && isTheme -> RatingGold
                                            isSelected -> NeonCyan
                                            else -> TextSecondary
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
fun CategoryCard(
    category: AnimeCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (category.type) {
        CategoryType.DEMOGRAPHIC -> RatingGold
        CategoryType.THEME -> NeonCyan
        else -> CrimsonAccent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledCard)
            .border(1.dp, AmoledBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp)
            .testTag("category_card_${category.name.lowercase()}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(typeColor.copy(alpha = 0.15f))
                        .border(1.dp, typeColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = category.emoji, fontSize = 18.sp)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeColor.copy(alpha = 0.15f))
                        .border(0.8.dp, typeColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = category.type.name,
                        color = typeColor,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = category.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = category.description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TagBrowseHeroBanner(
    selectedTags: Set<String>,
    matchMode: TagMatchMode,
    count: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectedTags.isEmpty()) return

    if (selectedTags.size == 1) {
        val tagName = selectedTags.first()
        val category = AnimeCategoryCatalog.find(tagName)
        val emoji = category?.emoji ?: "🎯"
        val isTheme = AnimeCategoryCatalog.isTheme(tagName)
        val isDemographic = AnimeCategoryCatalog.isDemographic(tagName)
        val typeName = when {
            isTheme -> "THEME"
            isDemographic -> "DEMOGRAPHIC"
            else -> "GENRE"
        }
        val description = category?.description ?: "Curated titles for $tagName"
        val accentColor = when {
            isTheme -> RatingGold
            isDemographic -> CrimsonAccent
            else -> NeonCyan
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AmoledCard)
                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .testTag("tag_browse_hero_banner")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 22.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeName,
                                color = accentColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Browsing by Tag",
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "$tagName Anime",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "$count titles • $description",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AmoledSurface)
                        .border(1.dp, AmoledBorder, CircleShape)
                        .testTag("close_tag_browse_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear Tag",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    } else {
        // Multi-tag banner
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AmoledCard)
                .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                .padding(12.dp)
                .testTag("tag_browse_hero_banner_multi")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonCyan.copy(alpha = 0.18f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Multi-tag filter",
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(NeonCyan.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${selectedTags.size} TAGS ACTIVE",
                                color = NeonCyan,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = matchMode.description,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = selectedTags.joinToString(" + "),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = "$count titles matching selected tags",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = onClear,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AmoledSurface)
                        .border(1.dp, AmoledBorder, CircleShape)
                        .testTag("close_tag_browse_multi_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear All Tags",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBrowseHeroBanner(
    categoryName: String,
    count: Int,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = AnimeCategoryCatalog.find(categoryName)
    val emoji = category?.emoji ?: "🎯"
    val typeName = category?.type?.name ?: "CATEGORY"
    val description = category?.description ?: "Curated titles sorted by popularity & score"
    val accentColor = when (category?.type) {
        CategoryType.DEMOGRAPHIC -> RatingGold
        CategoryType.THEME -> NeonCyan
        else -> CrimsonAccent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AmoledCard)
            .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("category_browse_hero_banner")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.18f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeName,
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Browsing Category",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "$categoryName Anime",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "$count titles • $description",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AmoledSurface)
                    .border(1.dp, AmoledBorder, CircleShape)
                    .testTag("close_category_browse_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear Category",
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Filter UI at the top of the search results screen that allows users to toggle
 * between anime genres using chips, updating the search results dynamically.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenreFilterBar(
    genres: List<String>,
    selectedGenre: String,
    genreCounts: Map<String, Int>,
    onSelectGenre: (String) -> Unit,
    onClearFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AmoledBlack)
            .padding(vertical = 8.dp)
            .testTag("genre_filter_container")
    ) {
        // Filter Header with active genre indicator & quick reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Genre Filter",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filter by Category",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selectedGenre != "ALL") {
                    val cat = AnimeCategoryCatalog.find(selectedGenre)
                    val emoji = cat?.emoji ?: "🎯"
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$emoji ${selectedGenre.uppercase()}",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            if (selectedGenre != "ALL") {
                TextButton(
                    onClick = onClearFilter,
                    modifier = Modifier.testTag("clear_genre_filter_button")
                ) {
                    Text(
                        text = "Reset",
                        color = CrimsonAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal scrollable genre chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("genre_filter_chips_row")
        ) {
            items(genres, key = { it }) { genre ->
                val isSelected = selectedGenre.equals(genre, ignoreCase = true)
                val count = genreCounts[genre] ?: 0
                val cat = AnimeCategoryCatalog.find(genre)

                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectGenre(genre) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = if (genre == "ALL") CrimsonAccent else NeonCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else if (cat != null) {
                        {
                            Text(text = cat.emoji, fontSize = 12.sp)
                        }
                    } else null,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (genre == "ALL") "All Categories" else genre,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (count > 0 || genre == "ALL") {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    fontSize = 10.sp,
                                    color = if (isSelected) {
                                        if (genre == "ALL") CrimsonAccent else NeonCyan
                                    } else TextTertiary
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AmoledCard,
                        selectedContainerColor = if (genre == "ALL") CrimsonAccent.copy(alpha = 0.2f) else NeonCyan.copy(alpha = 0.18f),
                        labelColor = if (count > 0) TextSecondary else TextTertiary,
                        selectedLabelColor = if (genre == "ALL") CrimsonAccent else NeonCyan
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = AmoledBorder,
                        selectedBorderColor = if (genre == "ALL") CrimsonAccent else NeonCyan,
                        borderWidth = if (isSelected) 1.5.dp else 1.dp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag(
                        if (genre == "ALL") "genre_chip_all"
                        else "genre_chip_${genre.lowercase().replace(" ", "_")}"
                    )
                )
            }
        }
    }
}

/**
 * High-contrast AMOLED Empty state shown when a genre filter yields no matching anime.
 */
@Composable
fun EmptyGenreFilterView(
    genre: String,
    totalCount: Int,
    query: String,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cat = AnimeCategoryCatalog.find(genre)
    val emoji = cat?.emoji ?: "🎯"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.12f))
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 24.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No \"$genre\" Anime Found",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (query.isNotBlank()) {
                "None of the $totalCount titles found for \"$query\" match the \"$genre\" category filter."
            } else {
                "No anime titles found for the \"$genre\" category. Try selecting another category."
            },
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Button(
            onClick = onReset,
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.testTag("reset_genre_filter_button")
        ) {
            Text(
                text = if (totalCount > 0) "Show All $totalCount Results" else "Reset Category",
                color = AmoledBlack,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

/**
 * Modern AMOLED Tag Filter Bar supporting multi-tag selection, categorized
 * tabs (All, Genres, Themes, Demographics), AND/OR match mode, and bottom sheet tag picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFilterBar(
    tags: List<String>,
    selectedTags: Set<String>,
    tagCounts: Map<String, Int>,
    activeTab: TagFilterTab,
    matchMode: TagMatchMode,
    onSelectTab: (TagFilterTab) -> Unit,
    onToggleTag: (String) -> Unit,
    onToggleMatchMode: () -> Unit,
    onOpenAllTags: () -> Unit,
    onClearAllTags: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AmoledBlack)
            .padding(vertical = 8.dp)
            .testTag("tag_filter_container")
    ) {
        // 1. Top Header Row: Title, active count, match mode toggle, full sheet button, reset
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(NeonCyan.copy(alpha = 0.15f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Tag Filter",
                        tint = NeonCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Filter by Tags",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                if (selectedTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(NeonCyan.copy(alpha = 0.2f))
                            .border(0.8.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${selectedTags.size} active",
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Match Mode toggle button (shows if multiple tags selected or clicked)
                if (selectedTags.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AmoledSurface)
                            .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                            .clickable { onToggleMatchMode() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("tag_match_mode_toggle")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Toggle Match Mode",
                                tint = NeonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = matchMode.displayName,
                                color = NeonCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Full Tag Catalog Bottom Sheet button
                IconButton(
                    onClick = onOpenAllTags,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AmoledSurface)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(6.dp))
                        .testTag("open_tag_catalog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Browse All Tags",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                if (selectedTags.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAllTags,
                        modifier = Modifier.testTag("clear_all_tags_button")
                    ) {
                        Text(
                            text = "Reset",
                            color = CrimsonAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // 2. Active Selected Tags Strip (if any tag is active)
        if (selectedTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.testTag("active_tags_row")
            ) {
                items(selectedTags.toList(), key = { "active_$it" }) { tag ->
                    val isTheme = AnimeCategoryCatalog.isTheme(tag)
                    val cat = AnimeCategoryCatalog.find(tag)
                    val emoji = cat?.emoji

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isTheme) RatingGold.copy(alpha = 0.18f) else NeonCyan.copy(alpha = 0.18f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isTheme) RatingGold.copy(alpha = 0.6f) else NeonCyan.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onToggleTag(tag) }
                            .padding(start = 8.dp, end = 6.dp, top = 3.dp, bottom = 3.dp)
                            .testTag("active_tag_chip_${tag.lowercase().replace(" ", "_")}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (emoji != null) {
                                Text(text = emoji, fontSize = 11.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = tag,
                                color = if (isTheme) RatingGold else NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove $tag",
                                tint = if (isTheme) RatingGold else NeonCyan,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Tab Filter Category Selector (All Tags, Genres, Themes, Demographics)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.testTag("tag_category_tabs_row")
        ) {
            items(TagFilterTab.values()) { tab ->
                val isSelected = activeTab == tab
                val tabEmoji = when (tab) {
                    TagFilterTab.ALL -> "🏷️"
                    TagFilterTab.GENRES -> "🎭"
                    TagFilterTab.THEMES -> "🎨"
                    TagFilterTab.DEMOGRAPHICS -> "👥"
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isSelected) AmoledSurfaceVariant else AmoledSurface
                        )
                        .border(
                            width = if (isSelected) 1.dp else 0.5.dp,
                            color = if (isSelected) NeonCyan else AmoledBorder,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("tag_tab_${tab.name.lowercase()}")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = tabEmoji, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tab.displayName,
                            color = if (isSelected) NeonCyan else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 4. Horizontal scrollable tag chips for the selected tab
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.testTag("tag_filter_chips_row")
        ) {
            items(tags, key = { it }) { tag ->
                val isAllChip = tag == "ALL"
                val isSelected = if (isAllChip) selectedTags.isEmpty() else selectedTags.contains(tag)
                val count = tagCounts[tag] ?: 0
                val cat = AnimeCategoryCatalog.find(tag)
                val isTheme = AnimeCategoryCatalog.isTheme(tag)
                val isDemo = AnimeCategoryCatalog.isDemographic(tag)

                val accentColor = when {
                    isAllChip -> CrimsonAccent
                    isTheme -> RatingGold
                    isDemo -> CrimsonAccent
                    else -> NeonCyan
                }

                FilterChip(
                    selected = isSelected,
                    onClick = { onToggleTag(tag) },
                    leadingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    } else if (cat != null) {
                        {
                            Text(text = cat.emoji, fontSize = 12.sp)
                        }
                    } else null,
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isAllChip) "All Tags" else tag,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            if (count > 0 || isAllChip) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "($count)",
                                    fontSize = 10.sp,
                                    color = if (isSelected) accentColor else TextTertiary
                                )
                            }
                        }
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = AmoledCard,
                        selectedContainerColor = accentColor.copy(alpha = 0.18f),
                        labelColor = if (count > 0) TextSecondary else TextTertiary,
                        selectedLabelColor = accentColor
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = AmoledBorder,
                        selectedBorderColor = accentColor,
                        borderWidth = if (isSelected) 1.5.dp else 1.dp
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag(
                        if (isAllChip) "tag_chip_all"
                        else "tag_chip_${tag.lowercase().replace(" ", "_")}"
                    )
                )
            }
        }
    }
}

/**
 * Bottom Sheet dialog presenting a full, categorized tag browser with instant search,
 * AND/OR matching toggle, and genres/themes/demographics tag clouds.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TagFilterBottomSheet(
    viewModel: SearchViewModel,
    tagCounts: Map<String, Int>,
    totalResults: Int,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AmoledBlack,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("tag_filter_bottom_sheet")
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonCyan.copy(alpha = 0.15f))
                            .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Tag Filter",
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Browse & Filter by Tags",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Combine genres, themes, and demographics",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AmoledSurface)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search tags input field
            OutlinedTextField(
                value = uiState.tagSearchQuery,
                onValueChange = { viewModel.setTagSearchQuery(it) },
                placeholder = { Text("Search genres or themes (e.g. Isekai, Cyberpunk)...", fontSize = 12.sp, color = TextTertiary) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Tags",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = if (uiState.tagSearchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { viewModel.setTagSearchQuery("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else null,
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = AmoledBorder,
                    focusedContainerColor = AmoledSurface,
                    unfocusedContainerColor = AmoledSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tag_sheet_search_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Match Mode Toggle (Any Tag vs All Tags)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledSurface)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (uiState.tagMatchMode == TagMatchMode.ANY) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (uiState.tagMatchMode == TagMatchMode.ANY) 1.dp else 0.dp,
                            color = if (uiState.tagMatchMode == TagMatchMode.ANY) NeonCyan else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { if (uiState.tagMatchMode != TagMatchMode.ANY) viewModel.toggleTagMatchMode() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Match Any Tag (OR)",
                        color = if (uiState.tagMatchMode == TagMatchMode.ANY) NeonCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (uiState.tagMatchMode == TagMatchMode.ANY) FontWeight.Bold else FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (uiState.tagMatchMode == TagMatchMode.ALL) NeonCyan.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            width = if (uiState.tagMatchMode == TagMatchMode.ALL) 1.dp else 0.dp,
                            color = if (uiState.tagMatchMode == TagMatchMode.ALL) NeonCyan else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { if (uiState.tagMatchMode != TagMatchMode.ALL) viewModel.toggleTagMatchMode() }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Match All Tags (AND)",
                        color = if (uiState.tagMatchMode == TagMatchMode.ALL) NeonCyan else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (uiState.tagMatchMode == TagMatchMode.ALL) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable list of categories
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(340.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val q = uiState.tagSearchQuery.trim()

                // Section 1: Themes
                val themes = AnimeCategoryCatalog.THEMES.filter {
                    q.isEmpty() || it.name.contains(q, ignoreCase = true)
                }
                if (themes.isNotEmpty()) {
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎨", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Anime Themes",
                                    color = RatingGold,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${themes.size})",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                themes.forEach { theme ->
                                    val isSelected = uiState.selectedTags.contains(theme.name)
                                    val count = tagCounts[theme.name] ?: 0
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) RatingGold.copy(alpha = 0.22f) else AmoledCard
                                            )
                                            .border(
                                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                                color = if (isSelected) RatingGold else AmoledBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.toggleTag(theme.name) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                            .testTag("sheet_tag_${theme.name.lowercase().replace(" ", "_")}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = theme.emoji, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = theme.name,
                                                color = if (isSelected) RatingGold else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (count > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$count",
                                                    color = if (isSelected) RatingGold else TextTertiary,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Genres
                val genres = AnimeCategoryCatalog.GENRES.filter {
                    q.isEmpty() || it.name.contains(q, ignoreCase = true)
                }
                if (genres.isNotEmpty()) {
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🎭", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Genres",
                                    color = NeonCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${genres.size})",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                genres.forEach { genre ->
                                    val isSelected = uiState.selectedTags.contains(genre.name)
                                    val count = tagCounts[genre.name] ?: 0
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) NeonCyan.copy(alpha = 0.22f) else AmoledCard
                                            )
                                            .border(
                                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                                color = if (isSelected) NeonCyan else AmoledBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.toggleTag(genre.name) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                            .testTag("sheet_tag_${genre.name.lowercase().replace(" ", "_")}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = genre.emoji, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = genre.name,
                                                color = if (isSelected) NeonCyan else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (count > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$count",
                                                    color = if (isSelected) NeonCyan else TextTertiary,
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Demographics
                val demos = AnimeCategoryCatalog.DEMOGRAPHICS.filter {
                    q.isEmpty() || it.name.contains(q, ignoreCase = true)
                }
                if (demos.isNotEmpty()) {
                    item {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "👥", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Demographics",
                                    color = CrimsonAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${demos.size})",
                                    color = TextTertiary,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                demos.forEach { demo ->
                                    val isSelected = uiState.selectedTags.contains(demo.name)
                                    val count = tagCounts[demo.name] ?: 0
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) CrimsonAccent.copy(alpha = 0.22f) else AmoledCard
                                            )
                                            .border(
                                                width = if (isSelected) 1.2.dp else 0.8.dp,
                                                color = if (isSelected) CrimsonAccent else AmoledBorder,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.toggleTag(demo.name) }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                            .testTag("sheet_tag_${demo.name.lowercase().replace(" ", "_")}")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = demo.emoji, fontSize = 11.sp)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = demo.name,
                                                color = if (isSelected) CrimsonAccent else TextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (count > 0) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "$count",
                                                    color = if (isSelected) CrimsonAccent else TextTertiary,
                                                    fontSize = 9.sp
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

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.selectedTags.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.clearAllTags() },
                        colors = ButtonDefaults.buttonColors(containerColor = AmoledSurface),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                            .testTag("sheet_reset_tags_button")
                    ) {
                        Text(
                            text = "Reset All",
                            color = CrimsonAccent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(2f)
                        .testTag("sheet_apply_tags_button")
                ) {
                    Text(
                        text = if (totalResults > 0) "Apply ($totalResults Titles)" else "Apply Filters",
                        color = AmoledBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * High-contrast AMOLED Empty state shown when tag filtering yields no matching anime.
 */
@Composable
fun EmptyTagFilterView(
    selectedTags: Set<String>,
    totalCount: Int,
    query: String,
    matchMode: TagMatchMode,
    onSwitchMatchMode: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.12f))
                .border(1.dp, NeonCyan.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "No Anime Matching Selected Tags",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Display active tags
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectedTags.take(3).forEach { tag ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AmoledSurface)
                        .border(0.8.dp, AmoledBorder, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = tag, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            if (selectedTags.size > 3) {
                Text(text = "+${selectedTags.size - 3} more", color = TextTertiary, fontSize = 10.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (query.isNotBlank()) {
                "None of the $totalCount titles found for \"$query\" match the combination of tags (${matchMode.displayName})."
            } else {
                "No anime titles match the combination of selected tags (${matchMode.displayName})."
            },
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (matchMode == TagMatchMode.ALL && selectedTags.size > 1) {
                Button(
                    onClick = onSwitchMatchMode,
                    colors = ButtonDefaults.buttonColors(containerColor = AmoledSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("switch_to_match_any_button")
                ) {
                    Text(
                        text = "Try 'Match Any' Mode",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Button(
                onClick = onReset,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("reset_tag_filter_button")
            ) {
                Text(
                    text = if (totalCount > 0) "Show All $totalCount Results" else "Reset Tags",
                    color = AmoledBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}
