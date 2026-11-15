package app.hopescrolling

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import java.time.LocalDate

/** Pause consciente : quelques respirations avant d'ouvrir Instagram / YouTube. */
class PauseActivity : Activity() {

    private lateinit var pkg: String
    private var decided = false
    private var timer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pkg = intent.getStringExtra(EXTRA_PKG) ?: run { finish(); return }
        val name = PAUSE_APPS[pkg] ?: "cette appli"
        val d = resources.displayMetrics.density
        fun dp(v: Int) = (v * d).toInt()

        val today = LocalDate.now()
        val opens = Stats.count(this, today, Metric.PAUSE_CONTINUE) +
            Stats.count(this, today, Metric.PAUSE_QUIT) + 1

        val circle = View(this).apply {
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#2E7D57"))
            }
        }
        val breath = TextView(this).apply {
            text = "Inspire…"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val circleBox = FrameLayout(this).apply {
            addView(circle, FrameLayout.LayoutParams(dp(200), dp(200), Gravity.CENTER))
            addView(breath, FrameLayout.LayoutParams(dp(200), dp(200), Gravity.CENTER))
        }

        val question = TextView(this).apply {
            text = "Pourquoi tu ouvres $name ?"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, dp(40), 0, dp(8))
        }
        val sub = TextView(this).apply {
            text = "${opens}e ouverture aujourd'hui"
            textSize = 15f
            setTextColor(Color.parseColor("#B2DFC9"))
            gravity = Gravity.CENTER
        }
        val quit = Button(this).apply {
            text = "Non, je ferme"
            setOnClickListener { quit() }
        }
        val go = Button(this).apply {
            isEnabled = false
            alpha = 0.6f
            setOnClickListener { proceed() }
        }

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(32))
            setBackgroundColor(Color.parseColor("#0F2E21"))
            addView(circleBox, LinearLayout.LayoutParams(-1, dp(240)))
            addView(question)
            addView(sub)
            addView(quit, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(48) })
            addView(go, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
        setContentView(col)

        // Respiration : 4 s on gonfle, 4 s on dégonfle.
        circle.startAnimation(ScaleAnimation(
            0.6f, 1f, 0.6f, 1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f,
        ).apply {
            duration = 4000
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
            setAnimationListener(object : Animation.AnimationListener {
                var inhale = true
                override fun onAnimationStart(a: Animation) = Unit
                override fun onAnimationEnd(a: Animation) = Unit
                override fun onAnimationRepeat(a: Animation) {
                    inhale = !inhale
                    breath.text = if (inhale) "Inspire…" else "Expire…"
                }
            })
        })

        timer = object : CountDownTimer(WAIT_MS, 1000) {
            override fun onTick(left: Long) {
                go.text = "Continuer vers $name (${(left + 999) / 1000})"
            }
            override fun onFinish() {
                go.text = "Continuer vers $name"
                go.isEnabled = true
                go.alpha = 1f
            }
        }.start()
    }

    private fun proceed() {
        decided = true
        Passes.grant(pkg)
        Passes.pausingPkg = null
        Stats.record(this, Metric.PAUSE_CONTINUE)
        finish()
    }

    private fun quit() {
        decided = true
        Passes.pausingPkg = null
        Stats.record(this, Metric.PAUSE_QUIT)
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = quit()

    override fun onDestroy() {
        timer?.cancel()
        if (!decided && Passes.pausingPkg == pkg) Passes.pausingPkg = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val WAIT_MS = 8000L
    }
}
