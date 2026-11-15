package app.hopescrolling

import android.content.Context
import java.time.LocalDate

/** Ce qu'on compte, jour par jour. */
enum class Metric(
    /** Minutes récupérées estimées par occurrence. */
    val minutes: Int,
    /** Est-ce que ça veut dire qu'on a "craqué" ce jour-là (casse la série) ? */
    val breaksStreak: Boolean = false,
) {
    BLOCK(2),            // un Short/Reel/TikTok bloqué
    SKIP(0),             // une pub ou suggestion sautée
    PAUSE_QUIT(5),       // tu as renoncé à ouvrir l'appli après la pause
    PAUSE_CONTINUE(0),   // tu as continué après la pause
    FEED_LIMIT(10, breaksStreak = true),   // limite du fil atteinte
    BUDGET_LIMIT(0, breaksStreak = true),  // budget Shorts épuisé
    CURFEW(5, breaksStreak = true),        // tentative pendant le couvre-feu / la concentration
}

object Stats {
    private fun key(day: LocalDate, m: Metric) = "d_${day}_${m.name}"

    fun record(c: Context, m: Metric) {
        val p = Prefs.sp(c)
        val today = LocalDate.now()
        val e = p.edit().putInt(key(today, m), p.getInt(key(today, m), 0) + 1)
        if (!p.contains("first_day")) e.putString("first_day", today.toString())
        e.apply()
        HopeWidget.refresh(c)
    }

    fun count(c: Context, day: LocalDate, m: Metric) = Prefs.sp(c).getInt(key(day, m), 0)

    fun total(c: Context, m: Metric): Int =
        Prefs.sp(c).all.entries.filter { it.key.startsWith("d_") && it.key.endsWith("_${m.name}") }
            .sumOf { (it.value as? Int) ?: 0 }

    /** Estimation des minutes récupérées un jour donné. */
    fun minutesSaved(c: Context, day: LocalDate) =
        Metric.entries.sumOf { count(c, day, it) * it.minutes }

    fun totalMinutesSaved(c: Context) = Metric.entries.sumOf { total(c, it) * it.minutes }

    private fun goodDay(c: Context, day: LocalDate) =
        Metric.entries.filter { it.breaksStreak }.all { count(c, day, it) == 0 }

    /** Jours consécutifs (aujourd'hui compris) sans limite atteinte. */
    fun streak(c: Context): Int {
        val first = Prefs.sp(c).getString("first_day", null)?.let(LocalDate::parse) ?: LocalDate.now()
        var day = LocalDate.now()
        var n = 0
        while (!day.isBefore(first) && goodDay(c, day)) {
            n++
            day = day.minusDays(1)
        }
        return n
    }

    // --- Temps passé sur les Shorts (pour le budget quotidien) ---
    private fun shortsKey() = "shorts_ms_${LocalDate.now()}"
    fun shortsMsToday(c: Context) = Prefs.sp(c).getLong(shortsKey(), 0L)
    fun addShortsMs(c: Context, ms: Long) =
        Prefs.sp(c).edit().putLong(shortsKey(), shortsMsToday(c) + ms).apply()
}
