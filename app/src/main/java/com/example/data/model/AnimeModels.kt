package com.example.data.model

enum class VerificationStatus {
    VERIFIED,
    MULTIPLE_SOURCES,
    SINGLE_SOURCE,
    CONFLICTING,
    UNCONFIRMED,
    UNAVAILABLE
}

enum class UpdateCategory {
    OFFICIAL_ANNOUNCEMENT,
    REPORTED,
    RUMOR,
    UNCONFIRMED
}

data class SourceAttribution(
    val sourceName: String,
    val sourceUrl: String?,
    val retrievedAt: Long = System.currentTimeMillis(),
    val dataType: String,
    val confidence: Float = 1.0f,
    val notes: String? = null
)

data class VerifiedFact<T>(
    val value: T?,
    val displayText: String,
    val status: VerificationStatus,
    val attributions: List<SourceAttribution> = emptyList(),
    val conflictDetails: String? = null
)

data class AnimeSummary(
    val id: Int,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val title: String,
    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val score: Double? = null,
    val status: String = "Unknown",
    val format: String = "TV",
    val episodes: Int? = null,
    val seasonYear: Int? = null,
    val genres: List<String> = emptyList(),
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED
)

data class StudioInfo(
    val id: Int = 0,
    val name: String,
    val isAnimationStudio: Boolean = true,
    val siteUrl: String? = null
)

data class UpcomingEpisodeInfo(
    val episodeNumber: Int,
    val timeUntilAiringSeconds: Long,
    val airingAtEpochSeconds: Long,
    val formattedDate: String,
    val isConfirmed: Boolean
)

data class StreamingService(
    val name: String,
    val url: String,
    val regionNotice: String = "Regional availability may vary"
)

data class DubInfo(
    val isDubAvailable: Boolean = false,
    val dubStatus: String = "Sub Only", // e.g. "Full Dub Available", "Simuldub Ongoing", "Announced", "Sub Only"
    val primaryDubLanguage: String = "English",
    val availableLanguages: List<String> = emptyList(), // e.g. ["English", "Spanish", "French", "German", "Portuguese"]
    val dubLicensor: String? = null, // e.g. "Crunchyroll", "Netflix", "Sentai Filmworks"
    val dubScheduleDetails: String? = null, // e.g. "Simuldubs release ~2 weeks after Japanese broadcast"
    val totalDubbedEpisodes: Int? = null,
    val leadEnglishCast: List<Pair<String, String>> = emptyList() // Character name -> English VA name
)

data class CharacterCast(
    val id: Int,
    val name: String,
    val nativeName: String? = null,
    val role: String = "MAIN",
    val imageUrl: String? = null,
    val voiceActorName: String? = null,
    val voiceActorNative: String? = null,
    val voiceActorImageUrl: String? = null,
    val englishVoiceActorName: String? = null,
    val englishVoiceActorImageUrl: String? = null
)

data class SeasonInfo(
    val seasonNumber: Int,
    val title: String,
    val episodeCount: Int?,
    val year: Int?,
    val animeId: Int,
    val isCanonical: Boolean = true
)

data class TimelineEntry(
    val id: Int,
    val malId: Int? = null,
    val title: String,
    val year: Int?,
    val format: String,
    val relationType: String,
    val coverImageUrl: String? = null
)

data class EpisodeItem(
    val episodeNumber: Int,
    val title: String,
    val airDate: String? = null,
    val durationMinutes: Int? = null,
    val synopsis: String? = null,
    val isFiller: Boolean = false,
    val isAired: Boolean = true,
    val airingAtEpochSeconds: Long? = null,
    val timeUntilAiringSeconds: Long? = null,
    val hasDub: Boolean = false,
    val dubStatusText: String? = null // e.g. "English Dub", "Sub Only", "Dub in ~2 wks", "Upcoming"
)

data class WebUpdate(
    val headline: String,
    val source: String,
    val url: String?,
    val publicationDate: String,
    val summary: String,
    val category: UpdateCategory
)

data class SourceSummary(
    val name: String,
    val url: String?,
    val lastChecked: Long,
    val status: VerificationStatus,
    val details: String
)

data class AnimeDetails(
    val id: Int,
    val anilistId: Int? = null,
    val malId: Int? = null,
    val title: String,
    val englishTitle: String? = null,
    val japaneseTitle: String? = null,
    val synopsis: String,
    val coverImageUrl: String? = null,
    val bannerImageUrl: String? = null,
    val score: VerifiedFact<Double>,
    val status: VerifiedFact<String>,
    val format: String,
    val episodeCount: VerifiedFact<Int>,
    val durationMinutes: Int? = null,
    val releaseDate: VerifiedFact<String>,
    val latestReleaseDate: String? = null,
    val season: String? = null,
    val year: Int? = null,
    val sourceMaterial: String? = null,
    val ageRating: String? = null,
    val genres: List<String> = emptyList(),
    val studios: List<StudioInfo> = emptyList(),
    val producers: List<String> = emptyList(),
    val licensors: List<String> = emptyList(),
    val nextEpisode: UpcomingEpisodeInfo? = null,
    val trailerUrl: String? = null,
    val trailerSite: String? = null,
    val officialWebsite: String? = null,
    val streamingServices: List<StreamingService> = emptyList(),
    val characters: List<CharacterCast> = emptyList(),
    val seasons: List<SeasonInfo> = emptyList(),
    val franchiseTimeline: List<TimelineEntry> = emptyList(),
    val webUpdates: List<WebUpdate> = emptyList(),
    val sourcesList: List<SourceSummary> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFromCache: Boolean = false,
    val dubInfo: DubInfo? = null,
    val allEpisodes: List<EpisodeItem> = emptyList()
)
