package app.hopescrolling

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.hopescrolling.ui.BlocksScreen
import app.hopescrolling.ui.HomeScreen
import app.hopescrolling.ui.HopeTheme
import app.hopescrolling.ui.Lock as LockGate
import app.hopescrolling.ui.Disclosure
import app.hopescrolling.ui.Onboarding
import app.hopescrolling.ui.Paywall
import app.hopescrolling.ui.RulesScreen
import app.hopescrolling.ui.SettingsScreen
import app.hopescrolling.ui.UiState

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        StoreFactory.store.connect(this) // version Play Store : prix et achats
        setContent {
            HopeTheme {
                UiState.version
                val serviceOn = isServiceEnabled()
                var onboarded by rememberSaveable {
                    mutableIntStateOf(if (Prefs.sp(this).getBoolean("onboarded", false) || serviceOn) 1 else 0)
                }
                if (onboarded == 0) {
                    Onboarding(serviceOn) {
                        Prefs.sp(this).edit().putBoolean("onboarded", true).apply()
                        onboarded = 1
                    }
                } else {
                    Main(serviceOn)
                }
                LockGate.Host()
                Paywall.Host()
                Disclosure.Host()
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun Main(serviceOn: Boolean) {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        val tabs = listOf(
            Triple("Accueil", Icons.Rounded.Home, 0),
            Triple("Blocages", Icons.Rounded.Shield, 1),
            Triple("Règles", Icons.Rounded.Tune, 2),
            Triple("Réglages", Icons.Rounded.Settings, 3),
        )
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEach { (label, icon, i) ->
                        NavigationBarItem(
                            selected = tab == i,
                            onClick = { tab = i },
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { padding ->
            AnimatedContent(
                targetState = tab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.padding(padding),
                label = "tabs",
            ) { t ->
                Box(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    when (t) {
                        0 -> HomeScreen(serviceOn) { Disclosure.openAccessibility(this@MainActivity) }
                        1 -> BlocksScreen()
                        2 -> RulesScreen()
                        else -> SettingsScreen(serviceOn)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        UiState.bump() // l'état du service ou les stats ont pu changer
    }

    private fun isServiceEnabled(): Boolean {
        val me = ComponentName(this, BlockerService::class.java)
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }
}
