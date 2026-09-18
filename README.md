# AnimeVault ⚡

[![Android](https://img.shields.io/badge/Platform-Android_8.0+_(API_24+)-3DDC84?logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin_2.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room Database](https://img.shields.io/badge/Database-Room_2.6-FF6F00?logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen)](#)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**AnimeVault** is an anime intelligence engine and tracker for Android. Built with a pure AMOLED (#000000) visual identity, it aggregates data across multiple authoritative anime APIs (AniList GraphQL, Jikan / MyAnimeList REST, and Wikipedia) to cross-check release dates, episode counts, audio dub availability, and upcoming episodes with zero hallucination.

---

## 🌟 Key Features

### 1. 🔍 Multi-Source Cross-Verification Engine
- **Independent Aggregation**: Queries **AniList GraphQL**, **Jikan v4 (MyAnimeList)**, and open-web sources in parallel.
- **Contradiction Detection**: Explicitly identifies and flags discrepancies in episode counts or release statuses between databases rather than guessing.
- **Data Source Health Monitor**: Built-in ping monitor to test real-time latency and reachability to external GraphQL and REST endpoints.

### 2. 🎙️ Comprehensive Sub & Dub Audio Breakdown
- **Language Availability**: Detailed badges indicating whether an anime is available in English Dub, Japanese Sub, Spanish, French, German, or Portuguese.
- **Dub Cast & Studio Attribution**: Surfaces primary English ADR voice cast (e.g. Bryce Papenbrook, Johnny Yong Bosch, Laura Bailey) and localization studios (Crunchyroll, Funimation, Bang Zoom!, Sentai Filmworks).
- **Platform Availability**: Direct pointers indicating where dub and sub tracks are streaming (Crunchyroll, Netflix, Hulu, Prime Video).

### 3. 📅 Episode Tracker & Upcoming Release Radar
- **Complete Episode Catalog**: Displays all aired episodes with titles, airing dates, and duration.
- **Upcoming Unreleased Episodes**: Automatically estimates and surfaces forthcoming scheduled episodes with live countdown badges ("In 3 days", "In 10 days") based on broadcast schedules.
- **Episode Status Badges**: High-contrast indicators distinguishing Aired vs. Upcoming unreleased episodes.

### 4. ☁️ Mail Log In & Automatic Cloud Backup / Restore
- **Automatic Restore on Log In**: Sign in with your email address, and AnimeVault instantly restores your entire saved anime vault, watch history, and search queries onto any device.
- **Continuous Background Auto-Backup**: Any addition, removal, or update to your favorites automatically syncs to your account in the background with zero manual effort.
- **Zero Manual Files**: No JSON file picking, file attachments, or manual exports required.
- **On-Demand Sync**: Includes a "Sync Now" trigger and one-tap session management in Settings.

### 5. 🖤 Pure AMOLED High-Contrast Theme
- **True Black (#000000)** canvas engineered to eliminate battery drain on OLED screens.
- **Crimson Red & Neon Emerald accents** for visual scanning and readability.
- Strictly adheres to Android Accessibility standards (minimum 48dp interactive touch targets, dynamic typography scaling).

### 6. 💾 Offline Persistence (Room DB)
- Offline caching of anime profiles, characters, and episode lists using Android Jetpack Room with Kotlin Symbol Processing (KSP).
- Browse your saved watchlist without an active internet connection.

---

## 🏗️ Architecture & Technology Stack

AnimeVault follows modern Android development practices and Clean / MVVM Architecture:

| Layer | Technology |
|---|---|
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 |
| **Language** | [Kotlin 2.0](https://kotlinlang.org/) |
| **Architecture** | MVVM (Model-View-ViewModel) + Single Source of Truth (SSOT) |
| **Async & Reactive** | Kotlin Coroutines & `StateFlow` / `SharedFlow` |
| **Local Persistence** | [Room Database](https://developer.android.com/training/data-storage/room) 2.6.1 + KSP |
| **Networking** | [OkHttp3](https://square.github.io/okhttp/) & JSON Serialization |
| **Image Loading** | [Coil Compose](https://coil-kt.github.io/coil/) |
| **Unit & UI Testing** | [Robolectric](https://robolectric.org/) (JVM) & Roborazzi screenshot testing |

---

## 📂 Project Structure

```text
app/src/main/java/com/example/
├── data/
│   ├── api/
│   │   ├── AniListApiClient.kt    # AniList GraphQL query engine
│   │   ├── JikanApiClient.kt      # Jikan / MyAnimeList REST engine
│   │   └── WebDataScraper.kt      # Web cross-referencing & dub data
│   ├── backup/
│   │   └── AppDataBackupManager.kt# Account auth, auto-backup & cloud restore
│   ├── local/
│   │   ├── AnimeDao.kt            # Room DAO for favorites, history & cache
│   │   ├── AppDatabase.kt         # Room Database configuration
│   │   └── Entities.kt            # Room entity data classes
│   ├── model/
│   │   └── Models.kt              # Domain models (AnimeDetails, Episode, DubInfo)
│   └── repository/
│       └── AnimeRepository.kt     # Multi-source aggregation & repository
├── ui/
│   ├── components/                # Reusable UI components & cards
│   ├── screens/
│   │   ├── SearchScreen.kt        # Search, filters, and trending anime
│   │   ├── DetailsScreen.kt       # Dub info, episode tracker & characters
│   │   ├── FavoritesScreen.kt     # Saved watchlist & vault
│   │   └── SettingsScreen.kt      # Mail login, auto-sync & health monitor
│   ├── theme/
│   │   ├── Color.kt               # Pure AMOLED palette (#000000, Crimson)
│   │   └── Theme.kt               # MaterialTheme wrapper
│   └── viewmodel/
│       └── Viewmodels.kt          # ViewModels for Search, Details, and Settings
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio**: Ladybug (2024.2+) or newer
- **JDK**: Java 17+
- **Android SDK**: API 36 (Minimum SDK: 24 / Android 7.0)

### Building the Project

1. **Clone the repository:**
   ```bash
   git clone https://github.com/your-username/AnimeVault.git
   cd AnimeVault
   ```

2. **Open in Android Studio** or build from the command line:
   ```bash
   # Build debug APK
   gradle assembleDebug

   # Run JVM Robolectric unit tests
   gradle :app:testDebugUnitTest
   ```

3. The generated APK will be available in:
   ```text
   app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📦 GitHub Releases & CI/CD

This repository includes a pre-configured **GitHub Actions CI/CD workflow** (`.github/workflows/release.yml`) for creating automated releases with ready-to-install APK artifacts.

### How to Trigger a GitHub Release:

1. Create and push a version tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```
2. GitHub Actions will automatically:
   - Run all Robolectric unit tests.
   - Build the signed / release APK.
   - Publish a new GitHub Release with release notes and the `.apk` attached.

Check out [RELEASE.md](RELEASE.md) for full release instructions and changelogs.

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
