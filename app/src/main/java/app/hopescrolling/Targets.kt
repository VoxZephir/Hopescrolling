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
    val description: String,
    val toast: String,
    val kind: Kind,
    val packages: List<String>,
    val viewIds: List<String> = emptyList(),
    /** Réservé à Premium dans la version Play Store. */
    val premium: Boolean = false,
) {
    YOUTUBE_SHORTS(
        label = "YouTube Shorts",
        description = "Retour automatique dès qu'un Short s'ouvre. Le reste de YouTube marche normalement.",
        toast = "Short refermé",
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
        description = "L'onglet Reels et les Reels ouverts depuis le fil sont refermés.",
        toast = "Reel refermé",
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
        description = "Quelques swipes dans Accueil et Explorer (réglable dans Règles), puis retour à l'écran d'accueil.",
        toast = "Fil Instagram terminé pour l'instant",
        kind = Kind.FEED_LIMIT,
        packages = listOf(INSTAGRAM),
        // Onglets de la barre du bas où le scroll est limité.
        viewIds = listOf("feed_tab", "search_tab"),
        premium = true,
    ),
    INSTAGRAM_SUGGESTED(
        label = "Pubs et suggestions Instagram",
        description = "Les posts sponsorisés et ceux des comptes que tu ne suis pas sont sautés automatiquement.",
        toast = "",
        kind = Kind.SKIP_POSTS,
        packages = listOf(INSTAGRAM),
        viewIds = listOf("feed_tab"),
        premium = true,
    ),
    TIKTOK(
        label = "TikTok",
        description = "L'appli entière : elle se referme dès qu'elle s'ouvre.",
        toast = "TikTok refermé",
        kind = Kind.WHOLE_APP,
        packages = TIKTOK_PKGS.toList(),
    ),
    BROWSER(
        label = "Dans le navigateur",
        description = "youtube.com/shorts, instagram.com/reels et tiktok.com dans Brave, Chrome, Firefox et DuckDuckGo.",
        toast = "Vidéo courte refermée",
        kind = Kind.BROWSER_URL,
        packages = BROWSER_URL_BARS.keys.toList(),
    );

    companion object {
        fun forPackage(pkg: String): List<Target> = entries.filter { pkg in it.packages }
    }
}

const val INSTAGRAM = "com.instagram.android"
const val YOUTUBE = "com.google.android.youtube"

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
    "suggestions", "suggestion", "suggestions pour vous", "suggéré pour vous", "suggested for you",
    "publications suggérées", "suggested posts", "suggested",
    "suivre", "follow",
)

/** Mots cherchés dans l'écran (recherche par sous-chaîne), filtrés ensuite par [isSuggestedMarker]. */
val SUGGESTED_SEARCH = listOf("sponsoris", "sponsored", "publicité", "suggest", "suggér", "suivre", "follow")

/**
 * Texte d'un élément qui signale un post d'un compte non suivi : libellé exact
 * (« Suggestions », « Sponsorisé »…), bouton « Suivre <nom> », ou description
 * « Suggestion Reel de … ». Exclut « Suivi(e) », « Followers », « Following »…
 */
fun isSuggestedMarker(raw: CharSequence?): Boolean {
    val t = raw?.toString()?.trim()?.lowercase() ?: return false
    if (t in SUGGESTED_MARKERS) return true
    return t.startsWith("suivre ") || t.startsWith("follow ") ||
        t.startsWith("suggestion reel") || t.startsWith("suggestion de") ||
        t.startsWith("sponsorisé") || t.startsWith("sponsored")
}
