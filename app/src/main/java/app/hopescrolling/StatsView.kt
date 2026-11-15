package app.hopescrolling

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Barres : minutes récupérées sur les 7 derniers jours. */
class StatsView(context: Context) : View(context) {
    private val d = resources.displayMetrics.density
    private val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#1DB86A") }
    private val today = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFB300") }
    private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        textSize = 12 * d
        textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(w: Int, h: Int) =
        setMeasuredDimension(MeasureSpec.getSize(w), (160 * d).toInt())

    override fun onDraw(canvas: Canvas) {
        val days = (6 downTo 0).map { LocalDate.now().minusDays(it.toLong()) }
        val values = days.map { Stats.minutesSaved(context, it) }
        val max = maxOf(values.maxOrNull() ?: 0, 10)
        val slot = width / 7f
        val top = 18 * d
        val bottom = height - 20 * d
        days.forEachIndexed { i, day ->
            val cx = slot * i + slot / 2
            val h = (bottom - top) * values[i] / max
            val r = RectF(cx - slot * 0.3f, bottom - h, cx + slot * 0.3f, bottom)
            canvas.drawRoundRect(r, 4 * d, 4 * d, if (i == 6) today else bar)
            if (values[i] > 0) canvas.drawText("${values[i]}", cx, bottom - h - 4 * d, label)
            val name = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            canvas.drawText(name.take(3), cx, height - 4 * d, label)
        }
    }
}
