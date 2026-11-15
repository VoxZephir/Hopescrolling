package app.hopescrolling

import android.content.Context
import java.time.LocalTime

object Prefs {
    private const val FILE = "hopescrolling"

    fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    private fun bool(c: Context, k: String, def: Boolean) = sp(c).getBoolean(k, def)
    private fun setBool(c: Context, k: String, v: Boolean) = sp(c).edit().putBoolean(k, v).apply()
    private fun int(c: Context, k: String, def: Int) = sp(c).getInt(k, def)
    private fun setInt(c: Context, k: String, v: Int) = sp(c).edit().putInt(k, v).apply()

    // --- Blocages ---
    fun isEnabled(c: Context, t: Target) = bool(c, "enabled_${t.name}", true)
    fun setEnabled(c: Context, t: Target, v: Boolean) = setBool(c, "enabled_${t.name}", v)

    // --- Pause consciente ---
    fun pauseEnabled(c: Context, pkg: String) = bool(c, "pause_$pkg", true)
    fun setPauseEnabled(c: Context, pkg: String, v: Boolean) = setBool(c, "pause_$pkg", v)

    // --- Anti-triche ---
    fun lockEnabled(c: Context) = bool(c, "lock", false)
    fun setLockEnabled(c: Context, v: Boolean) = setBool(c, "lock", v)
    fun protection(c: Context) = bool(c, "protect", false)
    fun setProtection(c: Context, v: Boolean) = setBool(c, "protect", v)

    // --- Règles horaires ---
    fun curfewOn(c: Context) = bool(c, "curfew", false)
    fun setCurfewOn(c: Context, v: Boolean) = setBool(c, "curfew", v)
    /** En minutes depuis minuit. */
    fun curfewStart(c: Context) = int(c, "curfew_start", 23 * 60)
    fun curfewEnd(c: Context) = int(c, "curfew_end", 7 * 60)
    fun setCurfew(c: Context, start: Int, end: Int) =
        sp(c).edit().putInt("curfew_start", start).putInt("curfew_end", end).apply()

    /** Minutes de Shorts/Reels autorisées par jour (0 = toujours bloqués). */
    fun shortsBudgetMin(c: Context) = int(c, "shorts_budget", 0)
    fun setShortsBudgetMin(c: Context, v: Int) = setInt(c, "shorts_budget", v)

    /** Fin du mode concentration (epoch ms), 0 si inactif. */
    fun focusUntil(c: Context) = sp(c).getLong("focus_until", 0L)
    fun setFocusUntil(c: Context, v: Long) = sp(c).edit().putLong("focus_until", v).apply()

    // --- Mode diagnostic ---
    fun diagnostic(c: Context) = bool(c, "diag", false)
    fun setDiagnostic(c: Context, v: Boolean) = setBool(c, "diag", v)

    fun saveDump(c: Context, pkg: String, dump: String) =
        sp(c).edit().putString("dump_$pkg", "${LocalTime.now().withNano(0)}\n$dump").apply()

    fun allDumps(c: Context): String =
        sp(c).all.filterKeys { it.startsWith("dump_") }.entries
            .joinToString("\n") { (k, v) -> "===== ${k.removePrefix("dump_")} @ $v" }

    fun clearDumps(c: Context) {
        val e = sp(c).edit()
        sp(c).all.keys.filter { it.startsWith("dump_") }.forEach { e.remove(it) }
        e.apply()
    }
}
