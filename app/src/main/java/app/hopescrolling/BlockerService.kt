package app.hopescrolling

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GestureResultCallback
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.inputmethod.InputMethodManager
import java.time.LocalDate
import android.graphics.Path
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

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

    private var lastProtectAt = 0L

    private val deferredCheck = Runnable {
        pendingPkg?.let { check(it) }
        pendingPkg = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) foreground(pkg)
        if (pkg in SETTINGS_PKGS) { protectSettings(pkg); return }
        if (gate(pkg)) return
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED &&
            pkg in Apps.config(this).scroll
        ) onGenericScroll(pkg, event)
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
                // Budget quotidien : on laisse passer tant qu'il en reste.
                if (ShortsBudget.allow(this)) return
                val m = if (Prefs.shortsBudgetMin(this) > 0) Metric.BUDGET_LIMIT else Metric.BLOCK
                block(t, GLOBAL_ACTION_BACK, metric = m); return
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
        // Instagram n'expose pas l'onglet sélectionné : on reconnaît le fil à ses
        // posts (en-tête « row_feed ») avec la barre d'onglets visible.
        if (!onInstagramFeed(root)) return

        val screenH = resources.displayMetrics.heightPixels
        val markers = (SUGGESTED_SEARCH.asSequence()
            .flatMap { root.findAccessibilityNodeInfosByText(it).asSequence() } +
            root.findAccessibilityNodeInfosByViewId("$INSTAGRAM:id/inline_follow_button").asSequence())
            .filter { it.isVisibleToUser }
            .filter { n ->
                n.viewIdResourceName?.endsWith(":id/inline_follow_button") == true ||
                    isSuggestedMarker(n.text) || isSuggestedMarker(n.contentDescription)
            }
            .mapNotNull { n -> feedListOf(n, screenH)?.let { Rect().also { r -> n.getBoundsInScreen(r) } to it } }
            .sortedBy { it.first.top }
            .toList()
        if (markers.isEmpty()) return

        val list = markers.first().second
        val l = Rect().also { list.getBoundsInScreen(it) }
        // Taille estimée d'un post à partir de son en-tête : média carré
        // (largeur de l'écran) + boutons, likes et légende.
        val post = l.width() + (POST_EXTRA_DP * resources.displayMetrics.density).toInt()
        // Plusieurs suggestions à la suite : on les enchaîne dans le même geste.
        // Un marqueur qui commence avant la fin du post précédent (+ marge) fait
        // partie de la série.
        var end = markers.first().first.top + post
        var count = 1
        for ((r, _) in markers.drop(1)) {
            if (r.top > end + post / 3) break
            if (r.top > end - post / 2) { end = r.top + post; count++ }
        }
        lastSkipAt = now
        skipping = true
        repeat(count) { Stats.record(this, Metric.SKIP) }
        swipeUp(l, end - l.top)
    }

    private fun onInstagramFeed(root: AccessibilityNodeInfo): Boolean =
        root.visible("$INSTAGRAM:id/feed_tab").isNotEmpty() &&
            (root.visible("$INSTAGRAM:id/row_feed_profile_header").isNotEmpty() ||
                root.visible("$INSTAGRAM:id/row_feed_photo_profile_name").isNotEmpty())

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
        val stroke = GestureDescription.StrokeDescription(move, 0, 280, true)
        val still = stroke.continueStroke(hold, 0, 90, false)
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

    /** Fin du saut : on revérifie tout de suite (la suggestion suivante est peut-être déjà là). */
    private fun finishSkip() {
        handler.postDelayed({
            skipping = false
            lastSkipAt = 0L
            rootInActiveWindow?.packageName?.toString()?.let { check(it) }
        }, 120)
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
        // Onglets principaux (barre du bas visible), hors lecteur de Reels.
        val onFeed = root.visible("$INSTAGRAM:id/feed_tab").isNotEmpty() &&
            root.visible("$INSTAGRAM:id/clips_viewer_view_pager").isEmpty()
        if (!onFeed) return

        val now = SystemClock.uptimeMillis()
        if (now - lastFeedScrollAt > FEED_RESET_MS) feedSwipes = 0
        // Un nouveau swipe = un scroll après une pause.
        if (now - lastFeedScrollAt > SWIPE_GAP_MS) feedSwipes++
        lastFeedScrollAt = now

        // Le compteur reste plein : chaque tentative relance les 15 min d'attente.
        if (feedSwipes > Prefs.scrollBudget(this) && now - lastActionAt > COOLDOWN_MS) {
            block(t, GLOBAL_ACTION_HOME, recheck = false, metric = Metric.FEED_LIMIT)
        }
    }

    /** Swipes par appli : (nombre, dernier scroll). */
    private val scrollState = mutableMapOf<String, Pair<Int, Long>>()

    /**
     * Limite de scroll générique (LinkedIn, Facebook, X…) : on compte les swipes
     * sur les grandes listes. Au-delà du budget, retour à l'écran d'accueil.
     * Le compteur repart à zéro après 15 min sans scroller dans cette appli.
     */
    private fun onGenericScroll(pkg: String, event: AccessibilityEvent) {
        val src = event.source ?: return
        val r = Rect().also { src.getBoundsInScreen(it) }
        if (r.height() < resources.displayMetrics.heightPixels / 2) return
        val now = SystemClock.uptimeMillis()
        val (prev, last) = scrollState[pkg] ?: (0 to 0L)
        var swipes = if (now - last > FEED_RESET_MS) 0 else prev
        if (now - last > SWIPE_GAP_MS) swipes++
        scrollState[pkg] = swipes to now
        if (swipes > Prefs.scrollBudget(this) && now - lastActionAt > COOLDOWN_MS) {
            lastActionAt = now
            performGlobalAction(GLOBAL_ACTION_HOME)
            Stats.record(this, Metric.FEED_LIMIT)
            toast("${Apps.label(this, pkg)} : fil terminé pour l'instant")
        }
    }

    private fun AccessibilityNodeInfo.visible(fullId: String): List<AccessibilityNodeInfo> =
        findAccessibilityNodeInfosByViewId(fullId).filter { it.isVisibleToUser }

    private fun block(
        target: Target,
        action: Int,
        recheck: Boolean = true,
        metric: Metric = Metric.BLOCK,
    ) {
        lastActionAt = SystemClock.uptimeMillis()
        performGlobalAction(action)
        Stats.record(this, metric)
        toast(if (metric == Metric.BUDGET_LIMIT) "Budget Shorts du jour épuisé" else target.toast)
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

    private lateinit var notice: Notice

    /** Message discret en haut de l'écran, avec le temps récupéré du jour. */
    private fun toast(title: String, subtitle: String? = null) {
        val saved = Stats.minutesSaved(this, LocalDate.now())
        notice.show(
            title,
            subtitle ?: if (saved > 0) "${formatDuration(saved)} récupérées aujourd'hui · ${Equivalence.short(saved)}"
            else "Le reste de tes applis reste disponible",
        )
    }

    // ---------- Temps d'écran ----------

    private var fgPkg: String? = null
    private var fgSince = 0L
    private var keyboards = emptySet<String>()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) {
            when (i.action) {
                Intent.ACTION_SCREEN_OFF -> { flushScreenTime(); fgPkg = null }
                Intent.ACTION_USER_PRESENT -> foreground(rootInActiveWindow?.packageName?.toString())
            }
        }
    }

    /**
     * Toutes les 30 s : enregistre le temps d'écran et applique les limites à l'appli
     * au premier plan. Indispensable pour les jeux, qui n'envoient presque pas
     * d'événements d'accessibilité une fois lancés.
     */
    private val periodicFlush = object : Runnable {
        override fun run() {
            fgPkg?.let { pkg ->
                flushScreenTime()
                enforceTimeRules(pkg)
            }
            handler.postDelayed(this, 30_000)
        }
    }

    private val warned = mutableSetOf<String>()

    /** Limite quotidienne, couvre-feu et concentration pour l'appli au premier plan. */
    private fun enforceTimeRules(pkg: String): Boolean {
        val cfg = Apps.config(this)
        val now = SystemClock.uptimeMillis()
        if (pkg in cfg.lockdown) {
            Rules.lockdownReason(this)?.let { reason ->
                if (now - lastActionAt > COOLDOWN_MS) {
                    lastActionAt = now
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    Stats.record(this, Metric.CURFEW)
                    toast(reason)
                }
                return true
            }
        }
        val limit = cfg.limits[pkg] ?: return false
        val usedMs = Stats.screenMsToday(this, pkg) + if (fgPkg == pkg) now - fgSince else 0
        val leftMin = limit - (usedMs / 60_000L).toInt()
        val key = "${LocalDate.now()}_$pkg"
        if (leftMin <= 0) {
            if (now - lastActionAt > COOLDOWN_MS) {
                lastActionAt = now
                performGlobalAction(GLOBAL_ACTION_HOME)
                Stats.record(this, Metric.DAILY_LIMIT)
                toast("${Apps.label(this, pkg)} : limite du jour atteinte",
                    "$limit min par jour. À demain !")
            }
            return true
        }
        if (leftMin <= 5 && warned.add(key)) {
            toast("Plus que $leftMin min sur ${Apps.label(this, pkg)}", "Limite quotidienne : $limit min")
        }
        return false
    }

    /** Appelé quand une autre appli passe au premier plan. */
    private fun foreground(pkg: String?) {
        if (pkg == null || pkg == fgPkg || pkg in IGNORED_FOREGROUND || pkg in keyboards) return
        flushScreenTime()
        fgPkg = pkg
        fgSince = SystemClock.uptimeMillis()
    }

    private fun flushScreenTime() {
        val pkg = fgPkg ?: return
        val now = SystemClock.uptimeMillis()
        if (pkg in Apps.config(this).managed || pkg in TIKTOK_PKGS) {
            Stats.addScreenMs(this, pkg, now - fgSince)
        }
        fgSince = now
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        notice = Notice(this)
        keyboards = getSystemService(InputMethodManager::class.java)
            .enabledInputMethodList.map { it.packageName }.toSet()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }
        handler.postDelayed(periodicFlush, 30_000)
    }

    /**
     * Couvre-feu, mode concentration et pause consciente.
     * Renvoie true si l'événement est entièrement traité (rien d'autre à vérifier).
     */
    private fun gate(pkg: String): Boolean {
        val now = SystemClock.uptimeMillis()
        val cfg = Apps.config(this)
        if ((pkg in cfg.lockdown || pkg in cfg.limits) && enforceTimeRules(pkg)) return true
        if (pkg !in cfg.pause) return false
        if (Passes.has(pkg)) { Passes.extend(pkg); return false }
        if (Passes.pausingPkg == pkg && now - Passes.pausingSince < 30_000) return true
        // Uniquement si l'appli est vraiment au premier plan.
        if (rootInActiveWindow?.packageName?.toString() != pkg) return true
        Passes.pausingPkg = pkg
        Passes.pausingSince = now
        startActivity(
            Intent(this, PauseActivity::class.java)
                .putExtra(PauseActivity.EXTRA_PKG, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        )
        return true
    }

    /**
     * Protection des réglages : empêche d'ouvrir la page qui désactive le service,
     * la page "Infos de l'appli" (forcer l'arrêt, désinstaller) et la désinstallation.
     */
    private fun protectSettings(pkg: String) {
        if (!Prefs.protection(this)) return
        val now = SystemClock.uptimeMillis()
        if (now - lastProtectAt < 500) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return
        val app = getString(R.string.app_name)
        val texts = root.findAccessibilityNodeInfosByText(app)
            .filter { it.isVisibleToUser }
            .mapNotNull { (it.text ?: it.contentDescription)?.toString()?.trim() }
        if (texts.isEmpty()) return

        fun hasText(words: List<String>) = words.any { w ->
            root.findAccessibilityNodeInfosByText(w).any { it.isVisibleToUser }
        }
        val danger = if (pkg == SETTINGS) {
            texts.any { t -> USE_PREFIXES.any { t.startsWith(it, ignoreCase = true) } } ||
                (texts.any { it == app } && hasText(APP_INFO_TEXTS))
        } else {
            hasText(UNINSTALL_TEXTS) // l'installateur, mais pas pour une mise à jour
        }
        if (danger) {
            lastProtectAt = now
            performGlobalAction(GLOBAL_ACTION_BACK)
            toast("Protégé par Hopescrolling", "Désactive d'abord la protection dans l'appli")
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        flushScreenTime()
        runCatching { unregisterReceiver(screenReceiver) }
        if (::notice.isInitialized) notice.hide()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private companion object {
        const val CHECK_INTERVAL_MS = 200L
        const val COOLDOWN_MS = 700L
        const val SWIPE_GAP_MS = 350L
        const val SKIP_COOLDOWN_MS = 250L
        const val POST_EXTRA_DP = 170

        const val SETTINGS = "com.android.settings"
        val SETTINGS_PKGS = setOf(SETTINGS, "com.google.android.packageinstaller", "com.android.packageinstaller")
        val USE_PREFIXES = listOf("Utiliser ", "Use ")
        val APP_INFO_TEXTS = listOf("Forcer l'arrêt", "Force stop", "Désinstaller", "Uninstall")
        val UNINSTALL_TEXTS = listOf("désinstaller", "uninstall")
        /** Ne changent pas l'appli "au premier plan" (volet des notifications, nous-mêmes…). */
        val IGNORED_FOREGROUND = setOf("com.android.systemui", "app.hopescrolling")
    }
}
