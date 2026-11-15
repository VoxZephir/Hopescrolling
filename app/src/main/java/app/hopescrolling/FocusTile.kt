package app.hopescrolling

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import java.text.DateFormat
import java.util.Date

/** Tuile des réglages rapides : lance 1 h de concentration. */
class FocusTile : TileService() {

    override fun onStartListening() = update()

    override fun onClick() {
        if (!Rules.focusActive(this)) {
            Rules.startFocus(this, 60)
            update()
        } else {
            // Arrêter passe par l'appli (et donc par le verrou s'il est activé).
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_IMMUTABLE))
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(intent)
            }
        }
    }

    private fun update() {
        val tile = qsTile ?: return
        val active = Rules.focusActive(this)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Concentration"
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle = if (active)
                "Jusqu'à " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(Prefs.focusUntil(this)))
            else "1 h"
        }
        tile.updateTile()
    }
}
