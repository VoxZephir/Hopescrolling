package app.hopescrolling.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.hopescrolling.PAUSE_APPS
import app.hopescrolling.Prefs
import app.hopescrolling.Target

@Composable
fun BlocksScreen() {
    val c = LocalContext.current
    UiState.version
    Column {
        ScreenHeader("Blocages", "Ce que Hopescrolling referme pour toi.")

        SectionTitle("Vidéos courtes")
        HopeCard {
            Column {
                Target.entries.forEachIndexed { i, t ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ToggleRow(t.emoji, t.label, t.description, Prefs.isEnabled(c, t)) { on ->
                        if (on) { Prefs.setEnabled(c, t, true); UiState.bump() }
                        else Lock.guard(c, "désactiver « ${t.label} »") { Prefs.setEnabled(c, t, false) }
                    }
                }
            }
        }

        SectionTitle("Pause avant d'ouvrir")
        HopeCard {
            Column {
                PAUSE_APPS.entries.forEachIndexed { i, (pkg, name) ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    ToggleRow(
                        if (name == "Instagram") "📸" else "▶️",
                        name,
                        "8 secondes de respiration et une question avant d'entrer.",
                        Prefs.pauseEnabled(c, pkg),
                    ) { on ->
                        if (on) { Prefs.setPauseEnabled(c, pkg, true); UiState.bump() }
                        else Lock.guard(c, "désactiver la pause avant $name") { Prefs.setPauseEnabled(c, pkg, false) }
                    }
                }
            }
        }
        Hint("Une fois la pause passée, l'appli reste libre tant que tu l'utilises, plus 3 minutes.")
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String) {
    Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
