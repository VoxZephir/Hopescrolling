package app.hopescrolling

import android.app.Activity
import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.time.LocalDate
import java.util.Date

@Suppress("DEPRECATION") // android.widget.Switch : pas besoin d'AndroidX pour ça
class MainActivity : Activity() {

    private lateinit var root: LinearLayout
    private val refreshers = mutableListOf<() -> Unit>()
    private val d get() = resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        root = findViewById(R.id.root)
        build()
    }

    override fun onResume() {
        super.onResume()
        refreshers.forEach { it() }
    }

    // ---------- petits constructeurs d'UI ----------

    private fun title(t: String) = TextView(this).apply {
        text = t; textSize = 20f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        setPadding(0, dp(32), 0, dp(4))
    }.also { root.addView(it) }

    private fun note(t: String) = TextView(this).apply {
        text = t; textSize = 13f; alpha = 0.7f
        setPadding(0, dp(2), 0, dp(6))
    }.also { root.addView(it) }

    private fun text(size: Float = 16f, bind: () -> String) = TextView(this).apply {
        textSize = size
    }.also { tv ->
        root.addView(tv)
        refreshers += { tv.text = bind() }
    }

    private fun button(bind: () -> String, onClick: () -> Unit) = Button(this).also { b ->
        b.setOnClickListener { onClick(); refreshers.forEach { it() } }
        root.addView(b, LinearLayout.LayoutParams(-1, -2))
        refreshers += { b.text = bind() }
    }

    /** Interrupteur : activer est libre, désactiver passe par le verrou. */
    private fun guardedSwitch(label: String, get: () -> Boolean, set: (Boolean) -> Unit) {
        val sw = Switch(this).apply {
            text = label; textSize = 16f
            setPadding(0, dp(10), 0, dp(10))
        }
        var reverting = false
        sw.setOnCheckedChangeListener { b, on ->
            if (reverting) return@setOnCheckedChangeListener
            if (on) set(true)
            else Lock.guard(this, "désactiver « $label »",
                onOk = { set(false); refreshers.forEach { it() } },
                onCancel = { reverting = true; b.isChecked = true; reverting = false })
        }
        root.addView(sw)
        refreshers += { reverting = true; sw.isChecked = get(); reverting = false }
    }

    // ---------- écran ----------

    private fun build() {
        TextView(this).apply {
            text = getString(R.string.app_name); textSize = 32f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(24), 0, 0)
        }.also { root.addView(it) }
        note(getString(R.string.tagline))

        text(18f) { if (isServiceEnabled()) "✅ Blocage actif" else "⚠️ Blocage inactif — active le service d'accessibilité" }
        button({ if (isServiceEnabled()) "Réglages d'accessibilité" else "Activer dans Accessibilité" }) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        note(getString(R.string.hint))

        // --- Stats ---
        title("Tes stats")
        text {
            val today = LocalDate.now()
            val total = Stats.totalMinutesSaved(this)
            "🌅 Aujourd'hui : ${Stats.minutesSaved(this, today)} min récupérées\n" +
                "🔥 Série : ${Stats.streak(this)} jour(s) sans atteindre une limite\n" +
                "⏳ Total : ${total / 60} h ${total % 60} min\n" +
                "🛑 ${Stats.total(this, Metric.BLOCK)} vidéos bloquées · " +
                "⏭️ ${Stats.total(this, Metric.SKIP)} pubs/suggestions sautées\n" +
                "🧘 Après la pause : ${Stats.total(this, Metric.PAUSE_QUIT)} fois fermé, " +
                "${Stats.total(this, Metric.PAUSE_CONTINUE)} fois continué"
        }
        val chart = StatsView(this)
        root.addView(chart)
        refreshers += { chart.invalidate() }
        note("Minutes récupérées sur 7 jours (estimation : 2 min par vidéo bloquée, 5 min par appli fermée après la pause, 10 min par limite de fil atteinte). Ajoute le widget Hopescrolling sur ton écran d'accueil pour les voir en permanence.")

        // --- Blocages ---
        title("Quoi bloquer")
        Target.entries.forEach { t ->
            guardedSwitch(t.label, { Prefs.isEnabled(this, t) }, { Prefs.setEnabled(this, t, it) })
        }

        // --- Pause consciente ---
        title("Pause avant d'ouvrir")
        note("8 secondes de respiration et une question avant d'entrer. Une fois passée, l'appli reste libre tant que tu l'utilises (+3 min).")
        PAUSE_APPS.forEach { (pkg, name) ->
            guardedSwitch(name, { Prefs.pauseEnabled(this, pkg) }, { Prefs.setPauseEnabled(this, pkg, it) })
        }

        // --- Règles ---
        title("Règles")
        note("Pendant le couvre-feu et le mode concentration, Instagram, YouTube et TikTok sont entièrement bloqués.")
        guardedSwitch("Couvre-feu", { Prefs.curfewOn(this) }, { Prefs.setCurfewOn(this, it) })
        button({ "Horaires : ${Rules.fmt(Prefs.curfewStart(this))} → ${Rules.fmt(Prefs.curfewEnd(this))}" }) {
            Lock.guard(this, "changer les horaires du couvre-feu", onOk = ::pickCurfew)
        }
        button({
            val b = Prefs.shortsBudgetMin(this)
            if (b == 0) "Budget Shorts/Reels : aucun (toujours bloqués)"
            else "Budget Shorts/Reels : $b min/jour (reste ${ShortsBudget.remainingMin(this)} min)"
        }) { pickBudget() }
        button({
            if (Rules.focusActive(this)) "🎯 Concentration jusqu'à " +
                DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(Prefs.focusUntil(this))) +
                " — arrêter"
            else "🎯 Mode concentration"
        }) {
            if (Rules.focusActive(this)) {
                Lock.guard(this, "arrêter le mode concentration",
                    onOk = { Prefs.setFocusUntil(this, 0); refreshers.forEach { it() } })
            } else pickFocus()
        }

        // --- Anti-triche ---
        title("Anti-triche")
        note("Verrou : pour assouplir une règle, il faudra attendre 30 s et recopier « ${Lock.PHRASE} ».")
        guardedSwitch("Verrou", { Prefs.lockEnabled(this) }, { Prefs.setLockEnabled(this, it) })
        note("Protection : bloque la page d'accessibilité de Hopescrolling, ses infos d'appli (forcer l'arrêt) et sa désinstallation. Pour installer une mise à jour qui demande de réactiver le service, coupe d'abord la protection.")
        guardedSwitch("Protection des réglages", { Prefs.protection(this) }, { Prefs.setProtection(this, it) })

        // --- Mises à jour ---
        title("Mises à jour")
        text(14f) { "Version installée : ${packageManager.getPackageInfo(packageName, 0).versionName}" }
        note("Pour les recevoir automatiquement : installe Obtainium, puis « Ajouter une appli » avec l'URL $REPO_URL")
        button({ "Télécharger la dernière version" }) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$REPO_URL/releases/latest")))
        }

        // --- Diagnostic ---
        title(getString(R.string.diag_title))
        note(getString(R.string.diag_help))
        guardedSwitch(getString(R.string.diag_switch), { Prefs.diagnostic(this) }) {
            Prefs.setDiagnostic(this, it)
            if (it) Prefs.clearDumps(this)
        }
        button({ getString(R.string.diag_copy) }) {
            val dumps = Prefs.allDumps(this)
            if (dumps.isBlank()) {
                Toast.makeText(this, R.string.diag_empty, Toast.LENGTH_LONG).show()
            } else {
                getSystemService(ClipboardManager::class.java)
                    .setPrimaryClip(ClipData.newPlainText("Hopescrolling diagnostic", dumps))
                Toast.makeText(this, R.string.diag_copied, Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(View(this), LinearLayout.LayoutParams(-1, dp(48)))
    }

    private fun pickCurfew() {
        val s = Prefs.curfewStart(this)
        TimePickerDialog(this, { _, h1, m1 ->
            val e = Prefs.curfewEnd(this)
            TimePickerDialog(this, { _, h2, m2 ->
                Prefs.setCurfew(this, h1 * 60 + m1, h2 * 60 + m2)
                refreshers.forEach { it() }
            }, e / 60, e % 60, true).apply { setTitle("Fin du couvre-feu") }.show()
        }, s / 60, s % 60, true).apply { setTitle("Début du couvre-feu") }.show()
    }

    private fun pickBudget() {
        val options = intArrayOf(0, 5, 10, 15, 30)
        val labels = options.map { if (it == 0) "Aucun (toujours bloqués)" else "$it min par jour" }
        AlertDialog.Builder(this)
            .setTitle("Budget Shorts/Reels")
            .setItems(labels.toTypedArray()) { _, i ->
                val v = options[i]
                val apply = { Prefs.setShortsBudgetMin(this, v); refreshers.forEach { it() } }
                // Augmenter le budget = assouplir -> verrou.
                if (v > Prefs.shortsBudgetMin(this)) Lock.guard(this, "augmenter ton budget", onOk = apply)
                else apply()
            }
            .show()
    }

    private fun pickFocus() {
        val mins = intArrayOf(30, 60, 120, 240)
        AlertDialog.Builder(this)
            .setTitle("Tout bloquer pendant…")
            .setItems(arrayOf("30 min", "1 h", "2 h", "4 h")) { _, i ->
                Prefs.setFocusUntil(this, System.currentTimeMillis() + mins[i] * 60_000L)
                refreshers.forEach { it() }
            }
            .show()
    }

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, BlockerService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    companion object {
        const val REPO_URL = "https://github.com/VoxZephir/Hopescrolling"
    }
}
