package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmoledBlack
import com.example.ui.theme.AmoledBorder
import com.example.ui.theme.AmoledCard
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VerifiedGreen
import com.example.ui.viewmodel.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val healthList by viewModel.healthStatus.collectAsState()
    val isTesting by viewModel.isTestingSources.collectAsState()
    val syncState by viewModel.accountSyncState.collectAsState()

    Scaffold(
        containerColor = AmoledBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = CrimsonAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Settings & Data Engine",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Text(
                    text = "Mail sign-in, cloud auto-backup & restore, and cache management",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = AmoledBorder)
            }

            // Mail Login & Automatic Cloud Backup & Restore Card
            item {
                AccountCloudSyncCard(
                    syncState = syncState,
                    onEmailChange = { viewModel.onEmailInputChange(it) },
                    onLoginClick = {
                        viewModel.loginWithEmail { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    },
                    onSyncNowClick = {
                        viewModel.syncNow { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLogoutClick = {
                        viewModel.logout {
                            Toast.makeText(context, "Logged out successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // Storage & Cache
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Offline Persistence & Cache", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AnimeVault caches cross-checked profiles to Room database for instantaneous offline access.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.clearCache {
                                    viewModel.refreshSyncInfo()
                                    Toast.makeText(context, "Local cache cleared", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmoledBlack),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_cache_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Cache", color = TextPrimary, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.clearSearchHistory {
                                    viewModel.refreshSyncInfo()
                                    Toast.makeText(context, "Search history cleared", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AmoledBlack),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_searches_button")
                        ) {
                            Text("Clear Searches", color = TextPrimary, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Data Sources Health Check
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = CrimsonAccent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Data Sources Health", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }

                        if (isTesting) {
                            CircularProgressIndicator(color = CrimsonAccent, modifier = Modifier.size(18.dp))
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.testSources() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.testTag("test_sources_button")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Ping", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    healthList.forEach { health ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = health.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = health.details, color = TextSecondary, fontSize = 11.sp)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (health.isReachable) {
                                    Text(text = "${health.pingMs}ms", color = TextTertiary, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(VerifiedGreen)
                                    )
                                } else {
                                    Text(text = "Failed", color = CrimsonAccent, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Source Architecture Core Policy
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AmoledCard)
                        .border(1.dp, AmoledBorder, RoundedCornerShape(10.dp))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = VerifiedGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Multi-Source Verification Engine", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val points = listOf(
                        "No Single Static Database" to "Information is aggregated from AniList GraphQL, Jikan/MyAnimeList REST, Wikipedia, and official production sources.",
                        "Contradiction Detection" to "Discrepancies in episode counts or release dates between sources are visibly flagged rather than guessed.",
                        "Zero Hallucination" to "If a fact is unconfirmed or unavailable from verifiable sources, it is explicitly shown as 'Unconfirmed' or 'Unavailable'.",
                        "Pure AMOLED Design" to "Optimized for true black OLED displays (#000000) for maximum contrast and battery preservation."
                    )

                    points.forEach { (title, desc) ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(text = title, color = CrimsonAccent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(text = desc, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                        }
                    }
                }
            }

            // Version info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "AnimeVault v1.0.0", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(text = "Open Data Anime Intelligence Engine", color = TextTertiary, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
fun AccountCloudSyncCard(
    syncState: com.example.ui.viewmodel.AccountSyncState,
    onEmailChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stats = syncState.databaseStats
    val lastSyncFormatted = remember(syncState.lastSyncEpoch) {
        if (syncState.lastSyncEpoch != null && syncState.lastSyncEpoch > 0) {
            SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault()).format(Date(syncState.lastSyncEpoch))
        } else {
            "Never synced"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AmoledCard)
            .border(
                1.dp,
                if (syncState.isLoggedIn) VerifiedGreen.copy(alpha = 0.5f) else AmoledBorder,
                RoundedCornerShape(10.dp)
            )
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (syncState.isLoggedIn) Icons.Default.CloudDone else Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = if (syncState.isLoggedIn) VerifiedGreen else CrimsonAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (syncState.isLoggedIn) "Cloud Account & Auto-Sync" else "Mail Log In & Cloud Backup",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (syncState.isLoggedIn) "Automatic backup & restore enabled" else "Automatic backup and instant restore on login",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            if (syncState.isSyncing) {
                CircularProgressIndicator(
                    color = CrimsonAccent,
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (syncState.isLoggedIn) {
            // Logged in UI
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(AmoledBlack)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CrimsonAccent.copy(alpha = 0.2f))
                                .border(1.dp, CrimsonAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = CrimsonAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = syncState.loggedInEmail ?: "Connected Account",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(VerifiedGreen)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Auto-Backup & Restore Active",
                                    color = VerifiedGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = AmoledBorder)
                Spacer(modifier = Modifier.height(10.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatItem(
                        count = "${stats?.favoritesCount ?: 0}",
                        label = "Vault Titles"
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(AmoledBorder))
                    StatItem(
                        count = "${stats?.searchHistoryCount ?: 0}",
                        label = "Searches"
                    )
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(AmoledBorder))
                    StatItem(
                        count = "${stats?.cachedAnimeCount ?: 0}",
                        label = "Cached"
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Last Synced: $lastSyncFormatted (Automatic on changes)",
                    color = TextTertiary,
                    fontSize = 10.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSyncNowClick,
                    enabled = !syncState.isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("sync_now_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (syncState.isSyncing) "Syncing..." else "Sync Now",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onLogoutClick,
                    enabled = !syncState.isSyncing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmoledBorder),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log Out",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            // Logged out / Log in form
            Text(
                text = "Log in with your email address to automatically back up your anime vault, watchlists, and search history. When logging in on any device, your entire vault restores instantly.",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = syncState.emailInput,
                onValueChange = onEmailChange,
                singleLine = true,
                label = { Text("Email Address", color = TextSecondary, fontSize = 12.sp) },
                placeholder = { Text("agrawallucky31@gmail.com", color = TextTertiary, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = CrimsonAccent,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = AmoledBlack,
                    unfocusedContainerColor = AmoledBlack,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedIndicatorColor = CrimsonAccent,
                    unfocusedIndicatorColor = AmoledBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mail_login_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick suggestion chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(AmoledBlack)
                    .border(1.dp, AmoledBorder, RoundedCornerShape(16.dp))
                    .clickable { onEmailChange("agrawallucky31@gmail.com") }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .testTag("quick_fill_email_chip")
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = TextTertiary,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Quick Fill: agrawallucky31@gmail.com",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onLoginClick,
                enabled = !syncState.isSyncing && syncState.emailInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("mail_login_button")
            ) {
                if (syncState.isSyncing) {
                    CircularProgressIndicator(color = TextPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logging in & Restoring...", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log In & Auto-Restore", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (syncState.statusMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = syncState.statusMessage ?: "",
                    color = if (syncState.isError) CrimsonAccent else VerifiedGreen,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Feature bullets
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(AmoledBlack.copy(alpha = 0.5f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("• Automatic restore: Saved vault titles restore instantly on login.", color = TextTertiary, fontSize = 10.sp)
                Text("• Automatic backup: Any added/removed favorites sync immediately.", color = TextTertiary, fontSize = 10.sp)
                Text("• No manual files: No export or import file picking required.", color = TextTertiary, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun StatItem(
    count: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = count,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}
