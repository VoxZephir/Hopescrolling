package app.hopescrolling

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.time.LocalDate

/** Widget d'écran d'accueil : minutes récupérées aujourd'hui + série. */
class HopeWidget : AppWidgetProvider() {
    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = refresh(c)

    companion object {
        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val cn = ComponentName(c, HopeWidget::class.java)
            if (m.getAppWidgetIds(cn).isEmpty()) return
            val v = RemoteViews(c.packageName, R.layout.widget)
            v.setTextViewText(R.id.w_saved, "🌅 ${Stats.minutesSaved(c, LocalDate.now())} min récupérées")
            v.setTextViewText(R.id.w_streak, "🔥 Série : ${Stats.streak(c)} j")
            val open = PendingIntent.getActivity(
                c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            v.setOnClickPendingIntent(R.id.w_root, open)
            m.updateAppWidget(cn, v)
        }
    }
}
