package app.hopescrolling

/**
 * Chaque cible = une appli + la façon de reconnaître l'écran "vidéo courte".
 *
 * viewIds : identifiants (resource-id) des vues qui n'existent QUE sur le lecteur
 *           Shorts/Reels. Trouvés en inspectant l'arbre de vues avec
 *           `adb shell uiautomator dump` (voir README). Si une mise à jour de
 *           l'appli les renomme, il suffit d'ajouter le nouveau nom ici.
 * wholeApp : l'appli entière est du scroll infini -> retour à l'écran d'accueil.
 */
enum class Target(
    val label: String,
    val packages: List<String>,
    val viewIds: List<String> = emptyList(),
    val wholeApp: Boolean = false,
) {
    YOUTUBE_SHORTS(
        label = "YouTube Shorts",
        packages = listOf("com.google.android.youtube"),
        viewIds = listOf(
            "reel_recycler",
            "reel_player_page_container",
            "reel_watch_player",
            "reel_watch_fragment_root",
        ),
    ),
    INSTAGRAM_REELS(
        label = "Instagram Reels",
        packages = listOf("com.instagram.android"),
        viewIds = listOf(
            "clips_viewer_view_pager",
            "clips_viewer_container",
            "clips_video_container",
        ),
    ),
    TIKTOK(
        label = "TikTok (appli entière)",
        packages = listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        wholeApp = true,
    );

    companion object {
        fun forPackage(pkg: String): Target? = entries.firstOrNull { pkg in it.packages }
    }
}
