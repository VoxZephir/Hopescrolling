package app.hopescrolling

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Le principe (le même que celui des applis du Play Store) :
 *  1. Android nous envoie un événement quand l'écran de YouTube/Instagram change.
 *  2. On regarde l'arbre de vues affiché et on cherche une vue propre au lecteur
 *     Shorts/Reels (par son resource-id).
 *  3. Si on la trouve, on simule le bouton Retour. Le reste de l'appli marche
 *     normalement.
 */
class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingPkg: String? = null
    private var lastActionAt = 0L

    private val deferredCheck = Runnable {
        pendingPkg?.let { check(it) }
        pendingPkg = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val target = Target.forPackage(pkg) ?: return
        if (!Prefs.isEnabled(this, target)) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            check(pkg)
        }
        // Les événements de contenu arrivent en rafale pendant la lecture vidéo :
        // on regroupe et on vérifie au plus toutes les 200 ms, mais on vérifie
        // toujours après la dernière rafale (sinon on pourrait rater l'écran final).
        if (pendingPkg == null) {
            pendingPkg = pkg
            handler.postDelayed(deferredCheck, CHECK_INTERVAL_MS)
        }
    }

    private fun check(pkg: String) {
        val target = Target.forPackage(pkg) ?: return
        if (!Prefs.isEnabled(this, target)) return
        val now = SystemClock.uptimeMillis()
        if (now - lastActionAt < COOLDOWN_MS) return

        if (target.wholeApp) {
            block(target, GLOBAL_ACTION_HOME)
            return
        }

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        if (target.viewIds.any { id -> root.hasVisible("$pkg:id/$id") }) {
            block(target, GLOBAL_ACTION_BACK)
        }
    }

    private fun AccessibilityNodeInfo.hasVisible(fullId: String): Boolean =
        findAccessibilityNodeInfosByViewId(fullId).any { it.isVisibleToUser }

    private fun block(target: Target, action: Int) {
        lastActionAt = SystemClock.uptimeMillis()
        performGlobalAction(action)
        Prefs.recordBlock(this)
        Toast.makeText(this, getString(R.string.blocked_toast, target.label), Toast.LENGTH_SHORT).show()
        // L'écran Shorts peut encore être là après un seul Retour (ex. onglet
        // Shorts ouvert au démarrage) : on revérifie peu après.
        handler.postDelayed({
            lastActionAt = 0L
            rootInActiveWindow?.packageName?.toString()?.let { check(it) }
        }, COOLDOWN_MS)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 200L
        const val COOLDOWN_MS = 700L
    }
}
