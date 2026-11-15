package app.hopescrolling

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var stats: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        stats = findViewById(R.id.stats)

        findViewById<Button>(R.id.open_settings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        val list = findViewById<LinearLayout>(R.id.targets)
        Target.entries.forEach { t ->
            @Suppress("DEPRECATION")
            val sw = Switch(this).apply {
                text = t.label
                textSize = 17f
                isChecked = Prefs.isEnabled(this@MainActivity, t)
                setPadding(0, 24, 0, 24)
                setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this@MainActivity, t, on) }
            }
            list.addView(sw)
        }
    }

    override fun onResume() {
        super.onResume()
        val on = isServiceEnabled()
        status.setText(if (on) R.string.status_on else R.string.status_off)
        findViewById<Button>(R.id.open_settings).setText(
            if (on) R.string.open_settings_on else R.string.open_settings_off
        )
        stats.text = getString(R.string.stats, Prefs.todayCount(this), Prefs.totalCount(this))
    }

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, BlockerService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
