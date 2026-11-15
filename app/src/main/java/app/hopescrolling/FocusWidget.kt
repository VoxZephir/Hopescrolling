package app.hopescrolling

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import java.text.DateFormat
import java.util.Date

/** Widget « Concentration » : un appui lance 1 h ; pendant la session, il ouvre l'appli. */
class FocusWidget : AppWidgetProvider() {

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) = refresh(c)

    override fun onReceive(c: Context, intent: Intent) {
        super.onReceive(c, intent)
        if (intent.action == ACTION_START && !Rules.focusActive(c)) {
            Rules.startFocus(c, 60)
        }
    }

    companion object {
        const val ACTION_START = "app.hopescrolling.FOCUS_START"

        fun refresh(c: Context) {
            val m = AppWidgetManager.getInstance(c)
            val cn = ComponentName(c, FocusWidget::class.java)
            if (m.getAppWidgetIds(cn).isEmpty()) return
            val v = RemoteViews(c.packageName, R.layout.widget_focus)
            val active = Rules.focusActive(c)
            v.setTextViewText(R.id.f_title, if (active) "Concentration en cours" else "Concentration")
            v.setTextViewText(
                R.id.f_sub,
                if (active) "Jusqu'à " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(Prefs.focusUntil(c)))
                else "Appuie pour 1 h sans distractions",
            )
            val click = if (active) {
                PendingIntent.getActivity(c, 1, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            } else {
                PendingIntent.getBroadcast(
                    c, 2, Intent(c, FocusWidget::class.java).setAction(ACTION_START),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }
            v.setOnClickPendingIntent(R.id.f_root, click)
            m.updateAppWidget(cn, v)
        }
    }
}
