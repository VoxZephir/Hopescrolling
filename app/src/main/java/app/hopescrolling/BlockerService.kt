package app.hopescrolling

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Principe :
 *  1. Android nous envoie un événement quand l'écran d'une appli surveillée change.
 *  2. On inspecte l'arbre de vues affiché (resource-id des vues, URL du navigateur…).
 *  3. Si c'est un écran de vidéos courtes, on simule Retour (ou Accueil).
 */
class BlockerService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var pendingPkg: String? = null
    private var lastActionAt = 0L

    // Limite du fil Instagram
    private var feedSwipes = 0
    private var lastFeedScrollAt = 0L

    private var lastDumpAt = 0L

    private val deferredCheck = Runnable {
        pendingPkg?.let { check(it) }
        pendingPkg = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val targets = Target.forPackage(pkg)
        if (targets.isEmpty()) return

        if (Prefs.diagnostic(this)) maybeDump(pkg)

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && pkg == INSTAGRAM) {
            onInstagramScroll(event)
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            check(pkg)
        }
        // Les événements arrivent en rafale (vidéo, scroll) : on vérifie au plus
        // toutes les 200 ms, et toujours après la dernière rafale.
        if (pendingPkg == null) {
            pendingPkg = pkg
            handler.postDelayed(deferredCheck, CHECK_INTERVAL_MS)
        }
    }

    private fun check(pkg: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastActionAt < COOLDOWN_MS) return
        val targets = Target.forPackage(pkg).filter { Prefs.isEnabled(this, it) }
        if (targets.isEmpty()) return

        targets.firstOrNull { it.kind == Kind.WHOLE_APP }?.let {
            block(it, GLOBAL_ACTION_HOME); return
        }

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        for (t in targets) {
            val hit = when (t.kind) {
                Kind.VIEW_IDS -> t.viewIds.any { id -> root.visible("$pkg:id/$id").isNotEmpty() }
                Kind.BROWSER_URL -> currentUrl(root, pkg)?.let { url ->
                    BLOCKED_URLS.any { it.containsMatchIn(url) }
                } ?: false
                else -> false
            }
            if (hit) {
                block(t, GLOBAL_ACTION_BACK); return
            }
        }
    }

    /** URL affichée dans la barre d'adresse (ignorée pendant que tu tapes dedans). */
    private fun currentUrl(root: AccessibilityNodeInfo, pkg: String): String? =
        BROWSER_URL_BARS[pkg].orEmpty()
            .flatMap { root.findAccessibilityNodeInfosByViewId("$pkg:id/$it") }
            .firstOrNull { !it.isFocused && !it.text.isNullOrBlank() }
            ?.text?.toString()

    private fun onInstagramScroll(event: AccessibilityEvent) {
        val t = Target.INSTAGRAM_FEED
        if (!Prefs.isEnabled(this, t)) return

        // Seul un scroll de grande liste compte (pas la barre des stories).
        val src = event.source ?: return
        val r = Rect().also { src.getBoundsInScreen(it) }
        if (r.height() < resources.displayMetrics.heightPixels / 2) return

        // Uniquement si on est sur l'onglet Accueil ou Explorer.
        val root = rootInActiveWindow ?: return
        val onFeed = t.viewIds.any { id ->
            root.visible("$INSTAGRAM:id/$id").any { it.isSelected }
        }
        if (!onFeed) return

        val now = SystemClock.uptimeMillis()
        if (now - lastFeedScrollAt > FEED_RESET_MS) feedSwipes = 0
        // Un nouveau swipe = un scroll après une pause.
        if (now - lastFeedScrollAt > SWIPE_GAP_MS) feedSwipes++
        lastFeedScrollAt = now

        // Le compteur reste plein : chaque tentative relance les 15 min d'attente.
        if (feedSwipes > FEED_SWIPE_BUDGET && now - lastActionAt > COOLDOWN_MS) {
            block(t, GLOBAL_ACTION_HOME, recheck = false)
        }
    }

    private fun AccessibilityNodeInfo.visible(fullId: String): List<AccessibilityNodeInfo> =
        findAccessibilityNodeInfosByViewId(fullId).filter { it.isVisibleToUser }

    private fun block(target: Target, action: Int, recheck: Boolean = true) {
        lastActionAt = SystemClock.uptimeMillis()
        performGlobalAction(action)
        Prefs.recordBlock(this)
        Toast.makeText(this, target.toast, Toast.LENGTH_SHORT).show()
        if (!recheck) return
        // Encore sur l'écran bloqué après un Retour (onglet Shorts au démarrage,
        // historique du navigateur rempli de Shorts…) : on recommence.
        handler.postDelayed({
            lastActionAt = 0L
            rootInActiveWindow?.packageName?.toString()?.let { check(it) }
        }, COOLDOWN_MS)
    }

    private fun maybeDump(pkg: String) {
        val now = SystemClock.uptimeMillis()
        if (now - lastDumpAt < 1500) return
        lastDumpAt = now
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        Prefs.saveDump(this, pkg, Diagnostic.dump(root, pkg in BROWSER_URL_BARS))
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 200L
        const val COOLDOWN_MS = 700L
        const val SWIPE_GAP_MS = 350L
    }
}
