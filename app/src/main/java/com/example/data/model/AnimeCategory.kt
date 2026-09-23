package com.example.data.model

enum class CategoryType(val displayName: String) {
    ALL("All Categories"),
    GENRE("Genres"),
    DEMOGRAPHIC("Demographics"),
    THEME("Themes")
}

data class AnimeCategory(
    val id: String,
    val name: String,
    val type: CategoryType,
    val description: String,
    val emoji: String,
    val popular: Boolean = false
)

object AnimeCategoryCatalog {
    val ALL_CATEGORIES: List<AnimeCategory> = listOf(
        // High-profile core categories requested: Action, Romance, Seinen
        AnimeCategory(
            id = "Action",
            name = "Action",
            type = CategoryType.GENRE,
            description = "High-octane battles, martial arts & adrenaline",
            emoji = "⚔️",
            popular = true
        ),
        AnimeCategory(
            id = "Romance",
            name = "Romance",
            type = CategoryType.GENRE,
            description = "Heartwarming love stories, drama & relationships",
            emoji = "💖",
            popular = true
        ),
        AnimeCategory(
            id = "Seinen",
            name = "Seinen",
            type = CategoryType.DEMOGRAPHIC,
            description = "Mature narratives, psychological depth & complex themes",
            emoji = "🎯",
            popular = true
        ),
        AnimeCategory(
            id = "Shounen",
            name = "Shounen",
            type = CategoryType.DEMOGRAPHIC,
            description = "Epic journeys, courage, camaraderie & willpower",
            emoji = "🔥",
            popular = true
        ),
        AnimeCategory(
            id = "Fantasy",
            name = "Fantasy",
            type = CategoryType.GENRE,
            description = "Spells, mythical beasts & enchanted realms",
            emoji = "🧙",
            popular = true
        ),
        AnimeCategory(
            id = "Sci-Fi",
            name = "Sci-Fi",
            type = CategoryType.GENRE,
            description = "Space exploration, advanced tech & cybernetics",
            emoji = "🚀",
            popular = true
        ),
        AnimeCategory(
            id = "Comedy",
            name = "Comedy",
            type = CategoryType.GENRE,
            description = "Hilarious antics, witty parodies & laughs",
            emoji = "😂",
            popular = true
        ),
        AnimeCategory(
            id = "Isekai",
            name = "Isekai",
            type = CategoryType.THEME,
            description = "Transported or reincarnated into fantasy dimensions",
            emoji = "🌀",
            popular = true
        ),
        AnimeCategory(
            id = "Psychological",
            name = "Psychological",
            type = CategoryType.GENRE,
            description = "Mind games, psychological suspense & dark philosophy",
            emoji = "🧠",
            popular = true
        ),
        AnimeCategory(
            id = "Mystery",
            name = "Mystery",
            type = CategoryType.GENRE,
            description = "Enigmas, detective deductions & puzzles",
            emoji = "🔍",
            popular = true
        ),
        AnimeCategory(
            id = "Slice of Life",
            name = "Slice of Life",
            type = CategoryType.GENRE,
            description = "Everyday warmth, school clubs & relaxed moments",
            emoji = "☕",
            popular = true
        ),
        AnimeCategory(
            id = "Drama",
            name = "Drama",
            type = CategoryType.GENRE,
            description = "Deep emotions, personal conflict & catharsis",
            emoji = "🎭",
            popular = true
        ),
        AnimeCategory(
            id = "Supernatural",
            name = "Supernatural",
            type = CategoryType.GENRE,
            description = "Spirits, occult phenomenons & otherworldly powers",
            emoji = "👻",
            popular = true
        ),
        AnimeCategory(
            id = "Sports",
            name = "Sports",
            type = CategoryType.GENRE,
            description = "Competitive athletics, tournament arcs & teamwork",
            emoji = "⚽",
            popular = true
        ),
        AnimeCategory(
            id = "Cyberpunk",
            name = "Cyberpunk",
            type = CategoryType.THEME,
            description = "High-tech dystopias, hacker underworlds & neon streets",
            emoji = "🌆",
            popular = false
        ),
        AnimeCategory(
            id = "Shoujo",
            name = "Shoujo",
            type = CategoryType.DEMOGRAPHIC,
            description = "Emotional nuance, personal growth & romantic bonds",
            emoji = "🌸",
            popular = false
        ),
        AnimeCategory(
            id = "Josei",
            name = "Josei",
            type = CategoryType.DEMOGRAPHIC,
            description = "Realistic adult romance, work life & life choices",
            emoji = "🍷",
            popular = false
        ),
        AnimeCategory(
            id = "Horror",
            name = "Horror",
            type = CategoryType.GENRE,
            description = "Spine-chilling terror, survival fear & macabre mysteries",
            emoji = "🩸",
            popular = false
        ),
        AnimeCategory(
            id = "Mecha",
            name = "Mecha",
            type = CategoryType.GENRE,
            description = "Piloted armored suits, robotic combat & galactic wars",
            emoji = "🤖",
            popular = false
        ),
        AnimeCategory(
            id = "Adventure",
            name = "Adventure",
            type = CategoryType.GENRE,
            description = "Expeditions, vast worlds & grand treasure hunts",
            emoji = "🗺️",
            popular = false
        ),
        AnimeCategory(
            id = "Music",
            name = "Music",
            type = CategoryType.GENRE,
            description = "Musical bands, idol stages & acoustic passion",
            emoji = "🎵",
            popular = false
        ),
        AnimeCategory(
            id = "Thriller",
            name = "Thriller",
            type = CategoryType.GENRE,
            description = "High-stakes suspense, racing clock & lethal conspiracies",
            emoji = "⚡",
            popular = false
        ),
        // Comprehensive Anime Themes
        AnimeCategory(
            id = "School",
            name = "School",
            type = CategoryType.THEME,
            description = "High school youth, academies, club activities & coming-of-age",
            emoji = "🏫",
            popular = true
        ),
        AnimeCategory(
            id = "Super Power",
            name = "Super Power",
            type = CategoryType.THEME,
            description = "Superhuman abilities, quirks, mystical energies & hero powers",
            emoji = "⚡",
            popular = true
        ),
        AnimeCategory(
            id = "Military",
            name = "Military",
            type = CategoryType.THEME,
            description = "Tactical combat, armed forces, strategy & frontline warfare",
            emoji = "🎖️",
            popular = false
        ),
        AnimeCategory(
            id = "Time Travel",
            name = "Time Travel",
            type = CategoryType.THEME,
            description = "Temporal paradoxes, worldline loops & shifting destinies",
            emoji = "⏳",
            popular = true
        ),
        AnimeCategory(
            id = "Martial Arts",
            name = "Martial Arts",
            type = CategoryType.THEME,
            description = "Hand-to-hand discipline, dojo tournaments & warrior spirits",
            emoji = "🥋",
            popular = false
        ),
        AnimeCategory(
            id = "Space",
            name = "Space",
            type = CategoryType.THEME,
            description = "Interplanetary odysseys, starships, galaxies & cosmic fleets",
            emoji = "🌌",
            popular = false
        ),
        AnimeCategory(
            id = "Historical",
            name = "Historical",
            type = CategoryType.THEME,
            description = "Feudal dynasties, samurai era, kingdoms & historical epics",
            emoji = "🏯",
            popular = false
        ),
        AnimeCategory(
            id = "Demons",
            name = "Demons",
            type = CategoryType.THEME,
            description = "Yokai, underworld beasts, cursed spirits & demon slayers",
            emoji = "👹",
            popular = true
        ),
        AnimeCategory(
            id = "Magic",
            name = "Magic",
            type = CategoryType.THEME,
            description = "Grimoires, spellcasting circles, magical academies & wizards",
            emoji = "🪄",
            popular = true
        ),
        AnimeCategory(
            id = "Survival",
            name = "Survival",
            type = CategoryType.THEME,
            description = "Life-or-death battle royales, grim dilemmas & survival grit",
            emoji = "🏕️",
            popular = false
        ),
        AnimeCategory(
            id = "Mythology",
            name = "Mythology",
            type = CategoryType.THEME,
            description = "Gods, legendary folklore, divine pantheons & ancient myths",
            emoji = "🏛️",
            popular = false
        ),
        AnimeCategory(
            id = "Parody",
            name = "Parody",
            type = CategoryType.THEME,
            description = "Anime trope parodies, self-aware meta comedy & satire",
            emoji = "🎭",
            popular = false
        ),
        AnimeCategory(
            id = "Post-Apocalyptic",
            name = "Post-Apocalyptic",
            type = CategoryType.THEME,
            description = "Ruined civilizations, fallout wastelands & surviving humanity",
            emoji = "☢️",
            popular = false
        ),
        AnimeCategory(
            id = "Vampire",
            name = "Vampire",
            type = CategoryType.THEME,
            description = "Nocturnal bloodlines, gothic castles & immortal hunters",
            emoji = "🧛",
            popular = false
        ),
        AnimeCategory(
            id = "Detective",
            name = "Detective",
            type = CategoryType.THEME,
            description = "Criminal investigations, deductive reasoning & cold cases",
            emoji = "🕵️",
            popular = false
        ),
        AnimeCategory(
            id = "Gore",
            name = "Gore",
            type = CategoryType.THEME,
            description = "Visceral intensity, graphic combat & dark psychological horror",
            emoji = "🩸",
            popular = false
        )
    )

    val POPULAR_CATEGORIES = ALL_CATEGORIES.filter { it.popular }

    val GENRES: List<AnimeCategory> by lazy { ALL_CATEGORIES.filter { it.type == CategoryType.GENRE } }
    val THEMES: List<AnimeCategory> by lazy { ALL_CATEGORIES.filter { it.type == CategoryType.THEME } }
    val DEMOGRAPHICS: List<AnimeCategory> by lazy { ALL_CATEGORIES.filter { it.type == CategoryType.DEMOGRAPHIC } }

    fun find(nameOrId: String?): AnimeCategory? {
        if (nameOrId.isNullOrBlank()) return null
        return ALL_CATEGORIES.firstOrNull {
            it.name.equals(nameOrId, ignoreCase = true) || it.id.equals(nameOrId, ignoreCase = true)
        }
    }

    fun isGenre(categoryName: String): Boolean {
        val cat = find(categoryName)
        return cat?.type == CategoryType.GENRE
    }

    fun isDemographic(categoryName: String): Boolean {
        val cat = find(categoryName)
        return cat?.type == CategoryType.DEMOGRAPHIC
    }

    fun isTheme(categoryName: String): Boolean {
        val cat = find(categoryName)
        return cat?.type == CategoryType.THEME
    }
}
