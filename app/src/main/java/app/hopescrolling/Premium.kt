package app.hopescrolling

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Premium n'existe que dans la version Play Store. La version GitHub/Obtainium
 * (REQUIRE_PREMIUM = false) a tout débloqué.
 */
object Premium {
    fun active(c: Context): Boolean =
        !BuildConfig.REQUIRE_PREMIUM || Prefs.sp(c).getBoolean("premium", false)

    fun set(c: Context, v: Boolean) {
        Prefs.sp(c).edit().putBoolean("premium", v).apply()
        Apps.invalidate()
    }
}

/** Une offre affichée sur l'écran Premium (prix fournis par Google Play). */
data class Offer(
    val id: String,
    val title: String,
    val price: String,
    val detail: String,
    val highlight: Boolean = false,
)

/** Accès à la boutique. La version full a une implémentation vide. */
interface Store {
    /** false = pas de boutique (version full, ou Google Play indisponible). */
    val available: Boolean
    val offers: List<Offer>
    fun connect(c: Context)
    fun buy(activity: Activity, offerId: String)
    fun restore(c: Context)
}

/** État de la boutique observé par l'interface. */
object StoreState {
    var offers by mutableStateOf<List<Offer>>(emptyList())
    var message by mutableStateOf<String?>(null)
}
