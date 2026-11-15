package app.hopescrolling

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams

/**
 * Google Play Billing. Produits à créer dans la Play Console (voir PLAYSTORE.md) :
 *  - abonnement « premium » avec deux forfaits de base : « monthly » et « yearly »
 *    (l'offre d'essai gratuit se règle dans la Console, sans code) ;
 *  - produit unique « premium_lifetime ».
 */
object StoreFactory {
    const val SUB = "premium"
    const val LIFETIME = "premium_lifetime"
    /** Dons : produits uniques consommables (rachetables). */
    val TIPS = listOf("tip_small", "tip_medium", "tip_large")

    val store: Store = PlayStore()

    private class PlayStore : Store, PurchasesUpdatedListener {
        private var client: BillingClient? = null
        private lateinit var app: Context
        private val details = mutableMapOf<String, ProductDetails>()
        /** offerId -> (produit, jeton d'offre pour un abonnement). */
        private val handles = mutableMapOf<String, Pair<ProductDetails, String?>>()

        override val available get() = client?.isReady == true
        override val offers get() = StoreState.offers

        override fun connect(c: Context) {
            app = c.applicationContext
            if (client?.isReady == true) { restore(app); return }
            val bc = BillingClient.newBuilder(app)
                .setListener(this)
                .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
                .build()
            client = bc
            bc.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(r: BillingResult) {
                    if (r.responseCode == BillingResponseCode.OK) {
                        queryProducts()
                        restore(app)
                    } else {
                        StoreState.message = "Google Play indisponible (${r.debugMessage})"
                    }
                }
                override fun onBillingServiceDisconnected() = Unit
            })
        }

        private fun queryProducts() {
            val bc = client ?: return
            listOf(listOf(SUB) to ProductType.SUBS, (listOf(LIFETIME) + TIPS) to ProductType.INAPP).forEach { (ids, type) ->
                val params = QueryProductDetailsParams.newBuilder().setProductList(
                    ids.map { QueryProductDetailsParams.Product.newBuilder().setProductId(it).setProductType(type).build() }
                ).build()
                bc.queryProductDetailsAsync(params) { r, list ->
                    if (r.responseCode == BillingResponseCode.OK) {
                        list.forEach { details[it.productId] = it }
                        rebuildOffers()
                    }
                }
            }
        }

        private fun rebuildOffers() {
            val out = mutableListOf<Offer>()
            handles.clear()
            details[SUB]?.subscriptionOfferDetails?.let { offers ->
                // Un forfait de base = un prix ; on préfère l'offre avec essai gratuit.
                offers.groupBy { it.basePlanId }.forEach { (plan, list) ->
                    val best = list.firstOrNull { o ->
                        o.pricingPhases.pricingPhaseList.any { it.priceAmountMicros == 0L }
                    } ?: list.first()
                    val phases = best.pricingPhases.pricingPhaseList
                    val paid = phases.last()
                    val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
                    val yearly = plan == "yearly"
                    val id = "sub_$plan"
                    handles[id] = details[SUB]!! to best.offerToken
                    out += Offer(
                        id = id,
                        title = if (yearly) "Annuel" else "Mensuel",
                        price = paid.formattedPrice + if (yearly) " / an" else " / mois",
                        detail = trial?.let { "Essai gratuit, puis renouvellement automatique" }
                            ?: "Renouvellement automatique, résiliable à tout moment",
                        highlight = yearly,
                    )
                }
            }
            details[LIFETIME]?.oneTimePurchaseOfferDetails?.let { o ->
                handles["lifetime"] = details[LIFETIME]!! to null
                out += Offer("lifetime", "À vie", o.formattedPrice, "Un seul paiement")
            }
            StoreState.offers = out.sortedBy { listOf("sub_yearly", "sub_monthly", "lifetime").indexOf(it.id) }
            StoreState.tips = TIPS.mapNotNull { id ->
                details[id]?.oneTimePurchaseOfferDetails?.let { o ->
                    handles[id] = details[id]!! to null
                    Offer(id, details[id]!!.name, o.formattedPrice, "")
                }
            }
        }

        override fun buy(activity: Activity, offerId: String) {
            val bc = client ?: return
            val (pd, token) = handles[offerId] ?: return
            val p = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd)
            if (token != null) p.setOfferToken(token)
            bc.launchBillingFlow(
                activity,
                BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(p.build())).build(),
            )
        }

        override fun tip(activity: Activity, tipId: String) = buy(activity, tipId)

        override fun onPurchasesUpdated(r: BillingResult, purchases: MutableList<Purchase>?) {
            when (r.responseCode) {
                BillingResponseCode.OK -> purchases?.forEach { handle(it) }
                BillingResponseCode.USER_CANCELED -> Unit
                else -> StoreState.message = "Achat impossible : ${r.debugMessage}"
            }
        }

        private fun handle(p: Purchase, quiet: Boolean = false) {
            if (p.purchaseState != Purchase.PurchaseState.PURCHASED) return
            if (p.products.any { it in TIPS }) {
                // Don : on le consomme pour qu'il puisse être refait. Il ne donne pas Premium.
                client?.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) { r, _ ->
                    if (r.responseCode == BillingResponseCode.OK && !quiet) {
                        StoreState.message = "Merci infiniment pour ton soutien !"
                    }
                }
                return
            }
            Premium.set(app, true)
            if (!quiet) StoreState.message = "Merci ! Premium est activé."
            if (!p.isAcknowledged) {
                client?.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
                ) { }
            }
        }

        /** Relit les achats auprès de Google Play (au lancement, ou « Restaurer »). */
        override fun restore(c: Context) {
            val bc = client ?: return
            var found = false
            var pending = 2
            var allOk = true
            listOf(ProductType.SUBS, ProductType.INAPP).forEach { type ->
                bc.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(type).build()) { r, list ->
                    if (r.responseCode == BillingResponseCode.OK) {
                        list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach {
                            if (it.products.none { id -> id in TIPS }) found = true
                            handle(it, quiet = true)
                        }
                    } else {
                        allOk = false
                    }
                    pending--
                    // Abonnement expiré ou remboursé : on retire Premium (si Google Play a bien répondu).
                    if (pending == 0 && allOk) Premium.set(app, found)
                }
            }
        }
    }
}
