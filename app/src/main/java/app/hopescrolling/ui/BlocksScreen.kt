@file:OptIn(ExperimentalMaterial3Api::class)

package app.hopescrolling.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.hopescrolling.Apps
import app.hopescrolling.Prefs
import app.hopescrolling.Target

@Composable
fun BlocksScreen() {
    val c = LocalContext.current
    UiState.version
    var openApp by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }

    Column {
        ScreenHeader("Blocages", "Ce que Hopescrolling referme pour toi.")

        SectionTitle("Applis surveillées")
        HopeCard {
            Column {
                val apps = Apps.managed(c).filter { Apps.isInstalled(c, it) }
                    .sortedBy { Apps.label(c, it).lowercase() }
                apps.forEachIndexed { i, pkg ->
                    if (i > 0) RowDivider()
                    AppRow(pkg) { openApp = pkg }
                }
                if (apps.isNotEmpty()) RowDivider()
                Row(
                    Modifier.fillMaxWidth().clickable { adding = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Rounded.Add, container = MaterialTheme.colorScheme.primaryContainer, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(16.dp))
                    Text("Ajouter une appli", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Hint("Pour chaque appli : pause à l'ouverture, limite de scroll, et fermeture pendant le couvre-feu et le mode concentration.")

        SectionTitle("Vidéos courtes")
        HopeCard {
            Column {
                Target.entries.forEachIndexed { i, t ->
                    if (i > 0) RowDivider()
                    ToggleRow(iconFor(t), t.label, t.description, Prefs.isEnabled(c, t)) { on ->
                        if (on) { Prefs.setEnabled(c, t, true); UiState.bump() }
                        else Lock.guard(c, "désactiver « ${t.label} »") { Prefs.setEnabled(c, t, false) }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    openApp?.let { pkg -> AppSheet(pkg) { openApp = null } }
    if (adding) AddAppDialog { adding = false }
}

@Composable
private fun AppRow(pkg: String, onClick: () -> Unit) {
    val c = LocalContext.current
    val opts = buildList {
        if (Apps.pause(c, pkg)) add("Pause")
        if (Apps.hasGenericScroll(pkg) && Apps.scroll(c, pkg)) add("Limite de scroll")
        if (Apps.lockdown(c, pkg)) add("Couvre-feu")
    }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(pkg)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(Apps.label(c, pkg), style = MaterialTheme.typography.titleMedium)
            Text(
                if (opts.isEmpty()) "Aucune règle" else opts.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Réglages d'une appli surveillée. */
@Composable
private fun AppSheet(pkg: String, onClose: () -> Unit) {
    val c = LocalContext.current
    UiState.version
    val name = Apps.label(c, pkg)
    ModalBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(pkg, 56)
                Spacer(Modifier.width(16.dp))
                Text(name, style = MaterialTheme.typography.headlineSmall)
            }
            ToggleRow(Icons.Rounded.SelfImprovement, "Pause avant d'ouvrir",
                "8 secondes de respiration et une question avant d'entrer.",
                Apps.pause(c, pkg)) { on ->
                if (on) { Apps.setPause(c, pkg, true); UiState.bump() }
                else Lock.guard(c, "désactiver la pause avant $name") { Apps.setPause(c, pkg, false) }
            }
            if (Apps.hasGenericScroll(pkg)) {
                ToggleRow(Icons.Rounded.Swipe, "Limite de scroll",
                    "${Prefs.scrollBudget(c)} swipes dans un fil, puis retour à l'écran d'accueil. Le compteur repart à zéro après 15 min.",
                    Apps.scroll(c, pkg)) { on ->
                    if (on) { Apps.setScroll(c, pkg, true); UiState.bump() }
                    else Lock.guard(c, "désactiver la limite de scroll de $name") { Apps.setScroll(c, pkg, false) }
                }
            } else {
                Hint("La limite de scroll d'Instagram se règle dans « Vidéos courtes » : elle ne compte que les onglets Accueil et Explorer.")
            }
            ToggleRow(Icons.Rounded.Bedtime, "Couvre-feu et concentration",
                "Fermée pendant le couvre-feu et le mode concentration.",
                Apps.lockdown(c, pkg)) { on ->
                if (on) { Apps.setLockdown(c, pkg, true); UiState.bump() }
                else Lock.guard(c, "retirer $name du couvre-feu") { Apps.setLockdown(c, pkg, false) }
            }
            OutlinedButton(
                onClick = {
                    Lock.guard(c, "ne plus surveiller $name") { Apps.remove(c, pkg); onClose() }
                },
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).fillMaxWidth(),
            ) { Text("Ne plus surveiller") }
        }
    }
}

private fun iconFor(t: Target): ImageVector = when (t) {
    Target.YOUTUBE_SHORTS -> Icons.Rounded.SmartDisplay
    Target.INSTAGRAM_REELS -> Icons.Rounded.Movie
    Target.INSTAGRAM_FEED -> Icons.Rounded.ViewAgenda
    Target.INSTAGRAM_SUGGESTED -> Icons.Rounded.SkipNext
    Target.TIKTOK -> Icons.Rounded.MusicNote
    Target.BROWSER -> Icons.Rounded.Language
}
