package app.hopescrolling

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Temps d'écran mesuré par Android (statistiques d'utilisation), avec l'accord de
 * l'utilisateur (« Accès aux données d'utilisation »).
 *
 * Temps récupéré d'un jour = ta moyenne quotidienne sur les applis surveillées
 * AVANT Hopescrolling − ton temps ce jour-là. Android garde quelques jours
 * d'historique : la moyenne de référence est calculée sur les jours précédant
 * l'installation, puis mémorisée.
 */
object DeviceUsage {
    private val zone get() = ZoneId.systemDefault()

    fun granted(c: Context): Boolean {
        val ops = c.getSystemService(AppOpsManager::class.java)
        @Suppress("DEPRECATION")
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        else ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Applis prises en compte : surveillées + celles dont Hopescrolling bloque les vidéos courtes. */
    fun tracked(c: Context): Set<String> =
        Apps.config(c).managed + Apps.preview(c) + setOf(INSTAGRAM, YOUTUBE) + TIKTOK_PKGS

    private fun startOf(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()

    // Le calcul d'une journée parcourt tous ses événements : on garde le résultat
    // (1 min pour aujourd'hui, définitivement pour les jours passés).
    private val cache = mutableMapOf<LocalDate, Pair<Long, Map<String, Long>>>()

    /** Millisecondes au premier plan par appli, un jour donné. */
    fun usage(c: Context, day: LocalDate): Map<String, Long> {
        val today = LocalDate.now()
        val now = System.currentTimeMillis()
        cache[day]?.let { (at, v) -> if (day != today || now - at < 60_000) return v }
        val usm = c.getSystemService(UsageStatsManager::class.java)
        val start = startOf(day)
        val end = minOf(startOf(day.plusDays(1)), now)
        val events = usm.queryEvents(start, end)
        val out = mutableMapOf<String, Long>()
        val e = UsageEvents.Event()
        var fg: String? = null
        var since = start
        fun close(t: Long) { fg?.let { out[it] = (out[it] ?: 0L) + (t - since) }; fg = null }
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (fg != e.packageName) { close(e.timeStamp); fg = e.packageName; since = e.timeStamp }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> if (fg == e.packageName) close(e.timeStamp)
                UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                UsageEvents.Event.KEYGUARD_SHOWN -> close(e.timeStamp)
            }
        }
        close(end)
        cache[day] = now to out
        return out
    }

    fun trackedMinutes(c: Context, day: LocalDate): Int {
        val t = tracked(c)
        return (usage(c, day).filterKeys { it in t }.values.sum() / 60_000L).toInt()
    }

    /**
     * Moyenne quotidienne (minutes) sur les applis suivies, avant l'installation.
     * Calculée appli par appli sur les 14 jours précédant le premier jour
     * d'utilisation, puis mémorisée. Pour une appli ajoutée plus tard dont
     * Android n'a plus l'historique d'avant, on prend les 14 derniers jours.
     * null si Android n'a aucun historique.
     */
    fun baseline(c: Context): Int? {
        val p = Prefs.sp(c)
        val missing = tracked(c).filter { !p.contains("baseline_$it") }
        if (missing.isNotEmpty()) {
            val first = p.getString("first_day", null)?.let(LocalDate::parse) ?: LocalDate.now()
            val before = averages(c, first.minusDays(14), first)
            val recent = averages(c, LocalDate.now().minusDays(14), LocalDate.now())
            val e = p.edit()
            for (pkg in missing) {
                val v = before?.let { it.second[pkg] ?: 0 } ?: recent?.let { it.second[pkg] ?: 0 } ?: continue
                e.putInt("baseline_$pkg", v)
            }
            before?.let { e.putInt("baseline_days", it.first) }
            e.apply()
        }
        val known = tracked(c).filter { p.contains("baseline_$it") }
        if (known.isEmpty()) return null
        return known.sumOf { p.getInt("baseline_$it", 0) }
    }

    /** (jours couverts, minutes moyennes par jour et par appli) sur [from, to), ou null. */
    private fun averages(c: Context, from: LocalDate, to: LocalDate): Pair<Int, Map<String, Int>>? {
        val usm = c.getSystemService(UsageStatsManager::class.java)
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startOf(from), startOf(to))
        if (stats.isNullOrEmpty()) return null
        val days = stats.map { java.time.Instant.ofEpochMilli(it.firstTimeStamp).atZone(zone).toLocalDate() }
            .filter { !it.isBefore(from) && it.isBefore(to) }.toSet().size
        if (days < 2) return null
        val per = stats.groupBy { it.packageName }
            .mapValues { (_, l) -> (l.sumOf { it.totalTimeInForeground } / days / 60_000L).toInt() }
        return days to per
    }

    fun baselineDays(c: Context) = Prefs.sp(c).getInt("baseline_days", 0)

    /**
     * Minutes récupérées un jour donné. Aujourd'hui, la moyenne est comptée au
     * prorata de la journée écoulée (7 h → minuit), pour ne pas annoncer dès le
     * matin une journée entière de temps gagné.
     */
    fun saved(c: Context, day: LocalDate): Int {
        val base = baseline(c) ?: return 0
        val used = trackedMinutes(c, day)
        val expected = if (day == LocalDate.now()) {
            val m = LocalTime.now().let { it.hour * 60 + it.minute }
            (base * ((m - 7 * 60).coerceIn(0, 17 * 60) / (17f * 60))).toInt()
        } else base
        return (expected - used).coerceAtLeast(0)
    }
}
