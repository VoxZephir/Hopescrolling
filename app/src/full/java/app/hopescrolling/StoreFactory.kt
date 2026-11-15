package app.hopescrolling

import android.app.Activity
import android.content.Context

/** Version GitHub / Obtainium : pas de boutique, tout est débloqué. */
object StoreFactory {
    val store: Store = object : Store {
        override val available = false
        override val offers = emptyList<Offer>()
        override fun connect(c: Context) = Unit
        override fun buy(activity: Activity, offerId: String) = Unit
        override fun tip(activity: Activity, tipId: String) = Unit
        override fun restore(c: Context) = Unit
    }
}
