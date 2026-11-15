package app.hopescrolling

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.graphics.Path
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

    // Saut des posts suggérés
    private var skipping = false
    private var lastSkipAt = 0L

    private val deferredCheck = Runnable {
        pendingPkg?.let { check(it) }
        pendingPkg = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        val targets = Target.forPackage(pkg)
        if (targets.isEmpty()) return

        if (Prefs.diagnostic(this)) maybeDump(pkg)

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED && pkg == INSTAGRAM && !skipping) {
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

        if (Target.INSTAGRAM_SUGGESTED in targets) skipSuggested(root)
    }

    /**
     * Si un post d'un compte non suivi est visible dans le fil Accueil, on fait
     * défiler le fil (geste de swipe simulé) pour l'amener hors de l'écran.
     */
    private fun skipSuggested(root: AccessibilityNodeInfo) {
        val now = SystemClock.uptimeMillis()
        if (skipping || now - lastSkipAt < SKIP_COOLDOWN_MS) return
        val onHome = Target.INSTAGRAM_SUGGESTED.viewIds.any { id ->
            root.visible("$INSTAGRAM:id/$id").any { it.isSelected }
        }
        if (!onHome) return

        val screenH = resources.displayMetrics.heightPixels
        val marker = SUGGESTED_MARKERS.asSequence()
            .flatMap { root.findAccessibilityNodeInfosByText(it).asSequence() }
            .filter { it.isVisibleToUser }
            .filter { n ->
                val txt = (n.text ?: n.contentDescription)?.toString()?.trim()?.lowercase()
                txt in SUGGESTED_MARKERS
            }
            .mapNotNull { n -> feedListOf(n, screenH)?.let { n to it } }
            .minByOrNull { (n, _) -> Rect().also { r -> n.getBoundsInScreen(r) }.top }
            ?: return

        val (node, list) = marker
        val m = Rect().also { node.getBoundsInScreen(it) }
        val l = Rect().also { list.getBoundsInScreen(it) }
        val d = resources.displayMetrics.density
        // Taille estimée d'un post à partir de son en-tête : média carré
        // (largeur de l'écran) + boutons, likes et légende.
        val distance = (m.top - l.top) + l.width() + (POST_EXTRA_DP * d).toInt()
        lastSkipAt = now
        skipping = true
        Prefs.recordSkip(this)
        swipeUp(l, distance)
    }

    /** Remonte vers le parent jusqu'à la grande liste qui défile (le fil). */
    private fun feedListOf(n: AccessibilityNodeInfo, screenH: Int): AccessibilityNodeInfo? {
        var cur: AccessibilityNodeInfo? = n.parent
        var depth = 0
        while (cur != null && depth < 25) {
            if (cur.isScrollable) {
                val r = Rect().also { cur!!.getBoundsInScreen(it) }
                if (r.height() > screenH / 2) return cur
            }
            cur = cur.parent
            depth++
        }
        return null
    }

    /**
     * Swipe vers le haut de [distance] px dans [list], en plusieurs gestes si besoin.
     * Le doigt s'immobilise à la fin pour éviter que le fil continue sur sa lancée.
     */
    private fun swipeUp(list: Rect, distance: Int) {
        val margin = list.height() / 10
        val maxStep = list.height() - 2 * margin
        val step = minOf(distance, maxStep)
        if (step < 20) { finishSkip(); return }
        val x = list.exactCenterX()
        val startY = (list.bottom - margin).toFloat()
        val endY = startY - step

        val move = Path().apply { moveTo(x, startY); lineTo(x, endY) }
        val hold = Path().apply { moveTo(x, endY); lineTo(x, endY + 1f) }
        val stroke = GestureDescription.StrokeDescription(move, 0, 450, true)
        val still = stroke.continueStroke(hold, 0, 200, false)
        val first = GestureDescription.Builder().addStroke(stroke).build()
        val second = GestureDescription.Builder().addStroke(still).build()

        val done = object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription) {
                dispatchGesture(second, object : GestureResultCallback() {
                    override fun onCompleted(g: GestureDescription) {
                        if (distance - step > 20) swipeUp(list, distance - step) else finishSkip()
                    }
                    override fun onCancelled(g: GestureDescription) = finishSkip()
                }, null)
            }
            override fun onCancelled(g: GestureDescription) = finishSkip()
        }
        if (!dispatchGesture(first, done, null)) finishSkip()
    }

    private fun finishSkip() {
        handler.postDelayed({
            skipping = false
            lastSkipAt = SystemClock.uptimeMillis()
            rootInActiveWindow?.packageName?.toString()?.let { check(it) }
        }, 300)
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
        const val SKIP_COOLDOWN_MS = 600L
        const val POST_EXTRA_DP = 170
    }
}
