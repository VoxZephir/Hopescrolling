package app.hopescrolling

import android.content.Context
import java.time.LocalDate

object Prefs {
    private const val FILE = "hopescrolling"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun isEnabled(c: Context, t: Target) = sp(c).getBoolean("enabled_${t.name}", true)
    fun setEnabled(c: Context, t: Target, v: Boolean) =
        sp(c).edit().putBoolean("enabled_${t.name}", v).apply()

    fun recordBlock(c: Context) {
        val p = sp(c)
        val today = LocalDate.now().toString()
        val todayCount = if (p.getString("day", null) == today) p.getInt("today", 0) else 0
        p.edit()
            .putString("day", today)
            .putInt("today", todayCount + 1)
            .putInt("total", p.getInt("total", 0) + 1)
            .apply()
    }

    fun todayCount(c: Context): Int {
        val p = sp(c)
        return if (p.getString("day", null) == LocalDate.now().toString()) p.getInt("today", 0) else 0
    }

    fun totalCount(c: Context) = sp(c).getInt("total", 0)
}
