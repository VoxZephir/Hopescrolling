package app.hopescrolling

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Carte qui glisse en haut de l'écran après un blocage (remplace les toasts).
 * Une fenêtre d'accessibilité : pas de permission supplémentaire, et elle ne
 * capte pas les touchers.
 */
class Notice(private val service: AccessibilityService) {
    private val wm = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var view: View? = null
    private val d = service.resources.displayMetrics.density
    private fun dp(v: Int) = (v * d).toInt()

    fun show(title: String, subtitle: String) {
        hide()
        val icon = ImageView(service).apply {
            setImageResource(R.drawable.ic_notice)
            setColorFilter(Color.parseColor("#0F2E21"))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#9FE3BE"))
            }
            val p = dp(8)
            setPadding(p, p, p, p)
        }
        val texts = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(service).apply {
                text = title
                setTextColor(Color.WHITE)
                textSize = 15f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            })
            addView(TextView(service).apply {
                text = subtitle
                setTextColor(Color.parseColor("#B8CFC2"))
                textSize = 13f
            })
        }
        val card = LinearLayout(service).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dp(24).toFloat()
                setColor(Color.parseColor("#F21A2420"))
            }
            setPadding(dp(12), dp(12), dp(20), dp(12))
            elevation = dp(8).toFloat()
            addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)))
            addView(texts, LinearLayout.LayoutParams(-2, -2).apply { marginStart = dp(14) })
            alpha = 0f
            translationY = -dp(24).toFloat()
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = dp(56)
        }
        runCatching { wm.addView(card, params) }.onFailure { return }
        view = card
        card.animate().alpha(1f).translationY(0f).setDuration(220).start()
        handler.postDelayed({
            card.animate().alpha(0f).translationY(-dp(16).toFloat()).setDuration(200)
                .withEndAction { if (view === card) hide() }.start()
        }, 2600)
    }

    fun hide() {
        handler.removeCallbacksAndMessages(null)
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }
}
