# AnimeVault Release Guide & Changelog 🚀

This document outlines the release process, release notes for **v1.0.0**, and instructions for publishing releases on GitHub.

---

## 🔖 Current Release: v1.0.0

**Release Date:** September 2026  
**Target SDK:** Android 15 (API 36)  
**Minimum SDK:** Android 7.0 (API 24)  
**Binary Artifact:** `AnimeVault-v1.0.0.apk`

### 📋 What's New in v1.0.0

#### 1. 🔍 Multi-Source Anime Intelligence
- Simultaneous queries to **AniList GraphQL** and **Jikan v4 (MyAnimeList REST)**.
- Smart deduplication, fuzzy score averaging, and discrepancy detection across airing statuses and episode counts.
- Built-in live ping monitor for source health tracking.

#### 2. 🎙️ Sub & Dub Audio Information
- Comprehensive language breakdown displaying whether English, Japanese, Spanish, French, or German audio tracks are available.
- English ADR voice actor listings and localization studios (Crunchyroll, Funimation, Sentai Filmworks).
- Streaming service pointers (Crunchyroll, Netflix, Hulu, Prime Video).

#### 3. 📅 Complete Episode Tracker & Upcoming Schedule
- Complete episode listings with titles, episode numbers, and air dates.
- Automatic countdown badges for upcoming unreleased episodes.
- Visual distinctions between aired episodes and forthcoming episodes.

#### 4. ☁️ Mail Log In with Automatic Backup & Restore
- Seamless email sign-in replacing manual JSON file management.
- Instant automatic restore of favorites and search queries upon logging in on any device.
- Continuous background auto-backup whenever anime favorites or history items change.
- One-tap "Sync Now" and clean session management.

#### 5. 🖤 Pure AMOLED Visual Design
- #000000 true-black canvas with crimson accents.
- Modern Jetpack Compose Material 3 implementation with fluid animations and 48dp+ accessible touch targets.

#### 6. 💾 Local Room Persistence
- Full offline caching of your vault and search history with AndroidX Room.

---

## 🛠️ How to Publish a Release on GitHub

### Option A: Automated Release via GitHub Actions (Recommended)

This repository includes `.github/workflows/release.yml`. When you push a git tag, GitHub Actions automatically runs tests, builds the APK, and creates a GitHub release.

1. **Tag the commit:**
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0: AnimeVault Initial Release"
   ```

2. **Push the tag to GitHub:**
   ```bash
   git push origin v1.0.0
   ```

3. **Check GitHub Actions:**
   - Go to the **Actions** tab on your GitHub repository.
   - The `Build & Publish Release` workflow will trigger automatically.
   - Once completed, the new release with `AnimeVault-v1.0.0.apk` will appear under **Releases**.

---

### Option B: Manual Release via GitHub Web UI

1. **Build the APK locally:**
   ```bash
   gradle assembleDebug
   ```
   The APK is generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

2. **Go to GitHub:**
   - Navigate to your repository on GitHub.
   - On the right sidebar, click **Releases** -> **Draft a new release**.

3. **Fill in the details:**
   - **Tag version**: `v1.0.0` (click "Create new tag: v1.0.0 on publish")
   - **Target**: `main`
   - **Release title**: `AnimeVault v1.0.0 - Pure AMOLED Anime Intelligence`
   - **Description**: Copy and paste the *What's New in v1.0.0* section above.
   - **Attach binaries**: Rename `app-debug.apk` to `AnimeVault-v1.0.0.apk` and drag-and-drop it into the **Attach binaries** box.

4. **Publish:**
   - Click **Publish release**.

---

## 📦 Release Checklist

Before tagging or publishing any new version:

- [x] Code compiles without errors (`compile_applet` / `gradle assembleDebug`).
- [x] All unit and Robolectric tests pass (`gradle :app:testDebugUnitTest`).
- [x] `versionCode` and `versionName` in `app/build.gradle.kts` match the release tag.
- [x] Adaptive launcher icon is properly rendered.
- [x] Tested mail login and automatic backup/restore flow.
- [x] Verified episode and audio dubbing lists on both popular and airing anime.
