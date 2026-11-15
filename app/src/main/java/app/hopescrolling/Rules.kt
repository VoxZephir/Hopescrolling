package app.hopescrolling

import android.content.Context
import android.os.SystemClock
import java.text.DateFormat
import java.time.LocalTime
import java.util.Date

object Rules {
    fun fmt(minutes: Int) = "%02d:%02d".format(minutes / 60, minutes % 60)

    fun inCurfew(c: Context): Boolean {
        if (!Prefs.curfewOn(c)) return false
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val s = Prefs.curfewStart(c)
        val e = Prefs.curfewEnd(c)
        return if (s <= e) now in s until e else now >= s || now < e
    }

    fun focusActive(c: Context) = Prefs.focusUntil(c) > System.currentTimeMillis()

    fun startFocus(c: Context, minutes: Int) {
        val now = System.currentTimeMillis()
        Prefs.sp(c).edit().putLong("focus_start", now).apply()
        Prefs.setFocusUntil(c, now + minutes * 60_000L)
        HopeWidget.refresh(c)
        FocusWidget.refresh(c)
    }

    /** Pourquoi tout est bloqué en ce moment (null si rien). */
    fun lockdownReason(c: Context): String? = when {
        focusActive(c) -> "Mode concentration jusqu'à " +
            DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(Prefs.focusUntil(c)))
        inCurfew(c) -> "Couvre-feu jusqu'à ${fmt(Prefs.curfewEnd(c))}"
        else -> null
    }
}

/** Budget quotidien de Shorts/Reels. */
object ShortsBudget {
    private var lastSeen = 0L

    /** Appelé quand un écran Shorts est détecté. Renvoie true s'il reste du budget. */
    fun allow(c: Context): Boolean {
        val limitMs = Prefs.shortsBudgetMin(c) * 60_000L
        if (limitMs == 0L) return false
        val now = SystemClock.uptimeMillis()
        val gap = now - lastSeen
        // Deux détections rapprochées = tu regardes en continu.
        if (gap in 1..5000) Stats.addShortsMs(c, gap)
        lastSeen = now
        return Stats.shortsMsToday(c) < limitMs
    }

    fun remainingMin(c: Context): Int {
        val left = Prefs.shortsBudgetMin(c) * 60_000L - Stats.shortsMsToday(c)
        return (left.coerceAtLeast(0) / 60_000L).toInt()
    }
}

/** Autorisations temporaires après la pause (en mémoire). */
object Passes {
    /** Une fois passée la pause, l'appli reste libre tant que tu l'utilises, + 3 min. */
    const val IDLE_MS = 3 * 60 * 1000L

    private val until = mutableMapOf<String, Long>()
    var pausingPkg: String? = null
    var pausingSince = 0L

    fun has(pkg: String) = (until[pkg] ?: 0L) > SystemClock.uptimeMillis()
    fun grant(pkg: String) { until[pkg] = SystemClock.uptimeMillis() + IDLE_MS }
    fun extend(pkg: String) { if (has(pkg)) grant(pkg) }
}
