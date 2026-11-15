package app.hopescrolling

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/** Applis connues pour être addictives, proposées en premier dans la liste. */
val KNOWN_APPS: List<String> = listOf(
    // Réseaux sociaux
    "com.linkedin.android",
    INSTAGRAM,
    YOUTUBE,
    "com.facebook.katana",          // Facebook
    "com.facebook.lite",            // Facebook Lite
    "com.instagram.barcelona",      // Threads
    "com.twitter.android",          // X
    "com.reddit.frontpage",         // Reddit
    "com.snapchat.android",         // Snapchat
    "com.pinterest",                // Pinterest
    "com.tumblr",                   // Tumblr
    "com.bereal.ft",                // BeReal
    "xyz.blueskyweb.app",           // Bluesky
    "org.joinmastodon.android",     // Mastodon
    "com.quora.android",            // Quora
    "com.ninegag.android.app",      // 9GAG
    // Vidéo et streaming
    "com.zhiliaoapp.musically",     // TikTok
    "com.ss.android.ugc.trill",     // TikTok (autre région)
    "tv.twitch.android.app",        // Twitch
    "com.netflix.mediaclient",      // Netflix
    "com.google.android.apps.youtube.music", // YouTube Music
    "com.discord",                  // Discord
    // Shopping (scroll infini aussi)
    "fr.vinted",                    // Vinted
    "fr.leboncoin",                 // leboncoin
    "com.amazon.mShop.android.shopping", // Amazon
    "com.einnovation.temu",         // Temu
    "com.zzkko",                    // Shein
    "com.alibaba.aliexpresshd",     // AliExpress
    // Rencontres
    "com.tinder",                   // Tinder
    "com.bumble.app",               // Bumble
    "co.hinge.app",                 // Hinge
    // Actus
    "com.google.android.apps.magazines", // Google Actualités
)

/**
 * Applis surveillées et leurs options. Les réglages sont dans SharedPreferences ;
 * on garde une copie en mémoire pour que le service ne relise pas tout à
 * chaque événement.
 */
object Apps {
    private const val KEY = "managed_apps"
    private val DEFAULT = setOf("com.linkedin.android", INSTAGRAM, YOUTUBE)

    class Config(
        val managed: Set<String>,
        val pause: Set<String>,
        val scroll: Set<String>,
        val lockdown: Set<String>,
    )

    @Volatile private var cache: Config? = null

    fun config(c: Context): Config = cache ?: build(c).also { cache = it }
    fun invalidate() { cache = null }

    private fun build(c: Context): Config {
        val m = managed(c)
        return Config(
            managed = m,
            pause = m.filter { pause(c, it) }.toSet(),
            scroll = m.filter { scroll(c, it) && hasGenericScroll(it) }.toSet(),
            lockdown = m.filter { lockdown(c, it) }.toSet() + TIKTOK_PKGS,
        )
    }

    fun managed(c: Context): Set<String> =
        Prefs.sp(c).getStringSet(KEY, null)?.toSet() ?: DEFAULT

    fun add(c: Context, pkg: String) {
        Prefs.sp(c).edit().putStringSet(KEY, managed(c) + pkg).apply()
        invalidate()
    }

    fun remove(c: Context, pkg: String) {
        Prefs.sp(c).edit().putStringSet(KEY, managed(c) - pkg).apply()
        invalidate()
    }

    // Options par appli (activées par défaut, sauf la limite de scroll sur YouTube)
    fun pause(c: Context, pkg: String) = Prefs.sp(c).getBoolean("pause_$pkg", true)
    fun scroll(c: Context, pkg: String) = Prefs.sp(c).getBoolean("scroll_$pkg", pkg != YOUTUBE)
    fun lockdown(c: Context, pkg: String) = Prefs.sp(c).getBoolean("lockdown_$pkg", true)

    fun setPause(c: Context, pkg: String, v: Boolean) = set(c, "pause_$pkg", v)
    fun setScroll(c: Context, pkg: String, v: Boolean) = set(c, "scroll_$pkg", v)
    fun setLockdown(c: Context, pkg: String, v: Boolean) = set(c, "lockdown_$pkg", v)

    private fun set(c: Context, k: String, v: Boolean) {
        Prefs.sp(c).edit().putBoolean(k, v).apply()
        invalidate()
    }

    /** Instagram a sa propre limite (onglets Accueil/Explorer uniquement). */
    fun hasGenericScroll(pkg: String) = pkg != INSTAGRAM

    fun label(c: Context, pkg: String): String = try {
        val pm = c.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }

    fun isInstalled(c: Context, pkg: String) = try {
        c.packageManager.getApplicationInfo(pkg, 0); true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Toutes les applis qui ont une icône dans le lanceur (sauf Hopescrolling). */
    fun launchable(c: Context): List<String> =
        c.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0
        ).map { it.activityInfo.packageName }
            .filter { it != c.packageName }
            .distinct()
}

val TIKTOK_PKGS = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
