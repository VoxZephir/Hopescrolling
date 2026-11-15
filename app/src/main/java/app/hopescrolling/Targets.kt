package app.hopescrolling

enum class Kind {
    /** Une vue précise (resource-id) n'existe que sur le lecteur Shorts/Reels -> Retour. */
    VIEW_IDS,
    /** L'appli entière est du scroll infini -> écran d'accueil. */
    WHOLE_APP,
    /** Navigateur : on lit l'URL dans la barre d'adresse -> Retour. */
    BROWSER_URL,
    /** Fil Instagram : on laisse quelques swipes, puis écran d'accueil. */
    FEED_LIMIT,
    /** Fil Instagram : saute les pubs et les posts de comptes non suivis. */
    SKIP_POSTS,
}

/**
 * Les resource-id viennent de l'inspection de l'arbre de vues (mode diagnostic
 * de l'appli, ou `adb shell uiautomator dump`). Si une mise à jour d'une appli les
 * renomme, il suffit d'ajouter le nouveau nom ici.
 */
enum class Target(
    val label: String,
    val emoji: String,
    val description: String,
    val toast: String,
    val kind: Kind,
    val packages: List<String>,
    val viewIds: List<String> = emptyList(),
) {
    YOUTUBE_SHORTS(
        label = "YouTube Shorts",
        emoji = "▶️",
        description = "Retour automatique dès qu'un Short s'ouvre. Le reste de YouTube marche normalement.",
        toast = "YouTube Shorts bloqué 🛑",
        kind = Kind.VIEW_IDS,
        packages = listOf(YOUTUBE),
        viewIds = listOf(
            "reel_recycler",
            "reel_player_page_container",
            "reel_watch_player",
            "reel_watch_fragment_root",
        ),
    ),
    INSTAGRAM_REELS(
        label = "Instagram Reels",
        emoji = "📸",
        description = "L'onglet Reels et les Reels ouverts depuis le fil sont refermés.",
        toast = "Reels bloqués 🛑",
        kind = Kind.VIEW_IDS,
        packages = listOf(INSTAGRAM),
        viewIds = listOf(
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "clips_video_container",
        ),
    ),
    INSTAGRAM_FEED(
        label = "Limite du fil Instagram",
        emoji = "🧱",
        description = "$FEED_SWIPE_BUDGET swipes dans Accueil et Explorer, puis retour à l'écran d'accueil. Le compteur repart à zéro après 15 min.",
        toast = "Fil Instagram terminé pour l'instant 🌅",
        kind = Kind.FEED_LIMIT,
        packages = listOf(INSTAGRAM),
        // Onglets de la barre du bas où le scroll est limité.
        viewIds = listOf("feed_tab", "search_tab"),
    ),
    INSTAGRAM_SUGGESTED(
        label = "Pubs et suggestions Instagram",
        emoji = "⏭️",
        description = "Les posts sponsorisés et ceux des comptes que tu ne suis pas sont sautés automatiquement.",
        toast = "",
        kind = Kind.SKIP_POSTS,
        packages = listOf(INSTAGRAM),
        viewIds = listOf("feed_tab"),
    ),
    TIKTOK(
        label = "TikTok",
        emoji = "🎵",
        description = "L'appli entière : elle se referme dès qu'elle s'ouvre.",
        toast = "TikTok bloqué 🛑",
        kind = Kind.WHOLE_APP,
        packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
    ),
    BROWSER(
        label = "Dans le navigateur",
        emoji = "🌐",
        description = "youtube.com/shorts, instagram.com/reels et tiktok.com dans Brave, Chrome, Firefox et DuckDuckGo.",
        toast = "Bloqué dans le navigateur 🛑",
        kind = Kind.BROWSER_URL,
        packages = BROWSER_URL_BARS.keys.toList(),
    );

    companion object {
        fun forPackage(pkg: String): List<Target> = entries.filter { pkg in it.packages }
    }
}

const val INSTAGRAM = "com.instagram.android"
const val YOUTUBE = "com.google.android.youtube"

/** Nombre de swipes autorisés dans le fil Instagram avant d'être renvoyé à l'accueil. */
const val FEED_SWIPE_BUDGET = 10

/** Le compteur de swipes repart à zéro après ce temps sans scroller le fil. */
const val FEED_RESET_MS = 15 * 60 * 1000L

/** Navigateur -> resource-id(s) de sa barre d'adresse. */
val BROWSER_URL_BARS: Map<String, List<String>> = mapOf(
    "com.brave.browser" to listOf("url_bar"),
    "com.brave.browser_beta" to listOf("url_bar"),
    "com.android.chrome" to listOf("url_bar"),
    "org.mozilla.firefox" to listOf("mozac_browser_toolbar_url_view", "url_bar_title"),
    "com.duckduckgo.mobile.android" to listOf("omnibarTextInput"),
)

val BLOCKED_URLS: List<Regex> = listOf(
    Regex("""youtube\.com/shorts""", RegexOption.IGNORE_CASE),
    Regex("""instagram\.com/reels?(/|$)""", RegexOption.IGNORE_CASE),
    Regex("""tiktok\.com""", RegexOption.IGNORE_CASE),
)

/**
 * Textes (exacts, sans tenir compte de la casse) qui signalent un post d'un compte
 * que tu ne suis pas : pub, suggestion, ou bouton "Suivre" dans l'en-tête du post.
 */
val SUGGESTED_MARKERS: Set<String> = setOf(
    "sponsorisé", "sponsorisée", "sponsored", "publicité",
    "suggestions pour vous", "suggéré pour vous", "suggested for you",
    "publications suggérées", "suggested posts",
    "suivre", "follow",
)
