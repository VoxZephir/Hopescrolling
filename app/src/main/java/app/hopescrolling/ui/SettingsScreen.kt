package app.hopescrolling.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.hopescrolling.Prefs

const val REPO_URL = "https://github.com/VoxZephir/Hopescrolling"

@Composable
fun SettingsScreen(serviceOn: Boolean) {
    val c = LocalContext.current
    UiState.version
    val divider = @Composable { HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant) }
    Column {
        ScreenHeader("Réglages", "Anti-triche, mises à jour et diagnostic.")

        SectionTitle("Anti-triche")
        HopeCard {
            Column {
                ToggleRow("🔒", "Verrou",
                    "Pour assouplir une règle : attendre 30 s et recopier « ${Lock.PHRASE} ».",
                    Prefs.lockEnabled(c)) { on ->
                    if (on) { Prefs.setLockEnabled(c, true); UiState.bump() }
                    else Lock.guard(c, "désactiver le verrou") { Prefs.setLockEnabled(c, false) }
                }
                divider()
                ToggleRow("🛡️", "Protection des réglages",
                    "Empêche de désactiver le service, de forcer l'arrêt ou de désinstaller Hopescrolling.",
                    Prefs.protection(c)) { on ->
                    if (on) { Prefs.setProtection(c, true); UiState.bump() }
                    else Lock.guard(c, "désactiver la protection") { Prefs.setProtection(c, false) }
                }
            }
        }
        Hint("Si une mise à jour demande de réactiver le service, coupe d'abord la protection.")

        SectionTitle("Service")
        HopeCard {
            ActionRow(
                if (serviceOn) "✅" else "⚠️",
                if (serviceOn) "Service d'accessibilité actif" else "Service d'accessibilité inactif",
                "Ouvrir les réglages d'accessibilité",
            ) { c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        }

        SectionTitle("Mises à jour")
        HopeCard {
            Column {
                val version = c.packageManager.getPackageInfo(c.packageName, 0).versionName
                ActionRow("⬇️", "Version $version", "Télécharger la dernière version") {
                    c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$REPO_URL/releases/latest")))
                }
                divider()
                ActionRow("🔄", "Mises à jour automatiques",
                    "Avec Obtainium : « Ajouter une appli » puis colle l'adresse du dépôt (copiée en appuyant ici).") {
                    c.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("Hopescrolling", REPO_URL))
                    Toast.makeText(c, "Adresse copiée", Toast.LENGTH_SHORT).show()
                }
            }
        }

        SectionTitle("Diagnostic")
        HopeCard {
            Column {
                ToggleRow("🩺", "Enregistrer les écrans",
                    "Si un écran n'est pas bloqué : active, ouvre cet écran 5 secondes, puis copie le diagnostic. Le texte de tes messages n'est pas enregistré.",
                    Prefs.diagnostic(c)) { on ->
                    Prefs.setDiagnostic(c, on)
                    if (on) Prefs.clearDumps(c)
                    UiState.bump()
                }
                divider()
                ActionRow("📋", "Copier le diagnostic") {
                    val dumps = Prefs.allDumps(c)
                    if (dumps.isBlank()) {
                        Toast.makeText(c, "Rien d'enregistré : active le mode puis ouvre l'appli à analyser.",
                            Toast.LENGTH_LONG).show()
                    } else {
                        c.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("Hopescrolling diagnostic", dumps))
                        Toast.makeText(c, "Diagnostic copié", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        Hint("Hopescrolling n'a pas accès à Internet : rien ne quitte ton téléphone.")
        Spacer(Modifier.height(16.dp))
    }
}
