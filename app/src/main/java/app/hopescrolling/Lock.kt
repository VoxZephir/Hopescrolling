package app.hopescrolling

import android.app.Activity
import android.app.AlertDialog
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

/** Verrou anti-triche : attendre 30 s et recopier une phrase pour assouplir une règle. */
object Lock {
    const val PHRASE = "Je choisis de perdre mon temps"
    private const val WAIT_MS = 30_000L

    fun guard(a: Activity, what: String, onOk: () -> Unit, onCancel: () -> Unit = {}) {
        if (!Prefs.lockEnabled(a)) { onOk(); return }
        val d = a.resources.displayMetrics.density
        val msg = TextView(a).apply {
            text = "Pour $what, attends 30 secondes et recopie :\n\n« $PHRASE »"
            textSize = 16f
        }
        val input = EditText(a).apply { hint = PHRASE }
        val box = LinearLayout(a).apply {
            orientation = LinearLayout.VERTICAL
            val p = (24 * d).toInt()
            setPadding(p, p / 2, p, 0)
            addView(msg)
            addView(input)
        }
        var waited = false
        var ok = false
        val dialog = AlertDialog.Builder(a)
            .setTitle("🔒 Verrou")
            .setView(box)
            .setPositiveButton("Confirmer") { _, _ -> ok = true; onOk() }
            .setNegativeButton("Laisser tomber", null)
            .setOnDismissListener { if (!ok) onCancel() }
            .create()
        dialog.show()
        val confirm = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        fun refresh() {
            confirm.isEnabled = waited &&
                input.text.toString().trim().equals(PHRASE, ignoreCase = true)
        }
        refresh()
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) = refresh()
        })
        val timer = object : CountDownTimer(WAIT_MS, 1000) {
            override fun onTick(left: Long) { confirm.text = "Confirmer (${(left + 999) / 1000})" }
            override fun onFinish() { confirm.text = "Confirmer"; waited = true; refresh() }
        }.start()
        dialog.setOnDismissListener { timer.cancel(); if (!ok) onCancel() }
    }
}
