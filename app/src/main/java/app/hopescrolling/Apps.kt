package app.hopescrolling

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.SystemClock

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

    private const val EXCLUDED = "excluded_games"

    class Config(
        val managed: Set<String>,
        val pause: Set<String>,
        val scroll: Set<String>,
        val lockdown: Set<String>,
        /** Appli -> minutes autorisées par jour. */
        val limits: Map<String, Int>,
        val builtAt: Long,
    )

    @Volatile private var cache: Config? = null

    /** Copie en mémoire, reconstruite après un changement ou toutes les 5 min (nouveaux jeux). */
    fun config(c: Context): Config {
        val cur = cache
        if (cur != null && SystemClock.uptimeMillis() - cur.builtAt < 5 * 60_000) return cur
        return build(c).also { cache = it }
    }

    fun invalidate() { cache = null }

    private fun build(c: Context): Config {
        // Sans Premium (version Play Store), les applis surveillées n'ont aucune règle.
        val m = if (Premium.active(c)) computeManaged(c) else emptySet()
        return Config(
            managed = m,
            pause = m.filter { pause(c, it) }.toSet(),
            scroll = m.filter { scroll(c, it) && hasGenericScroll(it) }.toSet(),
            lockdown = m.filter { lockdown(c, it) }.toSet() + TIKTOK_PKGS,
            limits = m.associateWith { dailyLimit(c, it) }.filterValues { it > 0 },
            builtAt = SystemClock.uptimeMillis(),
        )
    }

    /** Applis choisies, même sans Premium (pour les afficher verrouillées). */
    fun preview(c: Context): Set<String> = explicit(c)

    /** Applis surveillées : choisies à la main, plus tous les jeux si l'option est activée. */
    fun managed(c: Context): Set<String> = config(c).managed

    private fun explicit(c: Context): Set<String> =
        Prefs.sp(c).getStringSet(KEY, null)?.toSet() ?: DEFAULT

    private fun excludedGames(c: Context): Set<String> =
        Prefs.sp(c).getStringSet(EXCLUDED, null)?.toSet() ?: emptySet()

    private fun computeManaged(c: Context): Set<String> {
        val base = explicit(c)
        if (!allGames(c)) return base
        return base + (installedGames(c) - excludedGames(c))
    }

    fun add(c: Context, pkg: String) {
        Prefs.sp(c).edit()
            .putStringSet(KEY, explicit(c) + pkg)
            .putStringSet(EXCLUDED, excludedGames(c) - pkg)
            .apply()
        invalidate()
    }

    fun remove(c: Context, pkg: String) {
        val e = Prefs.sp(c).edit().putStringSet(KEY, explicit(c) - pkg)
        if (isGame(c, pkg)) e.putStringSet(EXCLUDED, excludedGames(c) + pkg)
        e.apply()
        invalidate()
    }

    // --- Jeux ---
    fun allGames(c: Context) = Prefs.sp(c).getBoolean("all_games", false)
    fun setAllGames(c: Context, v: Boolean) = set(c, "all_games", v)

    fun isGame(c: Context, pkg: String): Boolean = try {
        val ai = c.packageManager.getApplicationInfo(pkg, 0)
        @Suppress("DEPRECATION")
        ai.category == ApplicationInfo.CATEGORY_GAME || (ai.flags and ApplicationInfo.FLAG_IS_GAME) != 0
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun installedGames(c: Context): Set<String> = launchable(c).filter { isGame(c, it) }.toSet()

    // Options par appli. Par défaut : pause et couvre-feu activés ; limite de scroll
    // sauf sur YouTube et les jeux ; limite quotidienne de 60 min pour les jeux.
    fun pause(c: Context, pkg: String) = Prefs.sp(c).getBoolean("pause_$pkg", true)
    fun scroll(c: Context, pkg: String) =
        Prefs.sp(c).getBoolean("scroll_$pkg", pkg != YOUTUBE && !isGame(c, pkg))
    fun lockdown(c: Context, pkg: String) = Prefs.sp(c).getBoolean("lockdown_$pkg", true)
    fun dailyLimit(c: Context, pkg: String) =
        Prefs.sp(c).getInt("limit_$pkg", if (isGame(c, pkg)) 60 else 0)
    fun setDailyLimit(c: Context, pkg: String, minutes: Int) {
        Prefs.sp(c).edit().putInt("limit_$pkg", minutes).apply()
        invalidate()
    }

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
