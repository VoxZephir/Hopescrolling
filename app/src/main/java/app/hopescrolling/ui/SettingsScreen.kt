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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WorkspacePremium
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
    val divider = @Composable { RowDivider() }
    Column {
        ScreenHeader("Réglages", "Anti-triche, mises à jour et diagnostic.")

        if (app.hopescrolling.BuildConfig.REQUIRE_PREMIUM) {
            SectionTitle("Premium")
            HopeCard {
                ActionRow(
                    Icons.Rounded.WorkspacePremium,
                    if (app.hopescrolling.Premium.active(c)) "Premium actif" else "Passer à Premium",
                    if (app.hopescrolling.Premium.active(c)) "Merci de soutenir Hopescrolling"
                    else "Pause, limites, couvre-feu, verrou et plus",
                ) { Paywall.visible = true }
            }
        }

        SupportSection()

        SectionTitle("Anti-triche")
        HopeCard {
            Column {
                ToggleRow(Icons.Rounded.Lock, "Verrou",
                    "Pour assouplir une règle : attendre 30 s et recopier « ${Lock.PHRASE} ».",
                    Prefs.lockEnabled(c)) { on ->
                    if (on) { if (Paywall.require(c)) { Prefs.setLockEnabled(c, true); UiState.bump() } }
                    else Lock.guard(c, "désactiver le verrou") { Prefs.setLockEnabled(c, false) }
                }
                divider()
                ToggleRow(Icons.Rounded.Shield, "Protection des réglages",
                    "Empêche de désactiver le service, de forcer l'arrêt ou de désinstaller Hopescrolling.",
                    Prefs.protection(c)) { on ->
                    if (on) { if (Paywall.require(c)) { Prefs.setProtection(c, true); UiState.bump() } }
                    else Lock.guard(c, "désactiver la protection") { Prefs.setProtection(c, false) }
                }
            }
        }
        Hint("Si une mise à jour demande de réactiver le service, coupe d'abord la protection.")

        SectionTitle("Service")
        HopeCard {
            ActionRow(
                if (serviceOn) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                if (serviceOn) "Service d'accessibilité actif" else "Service d'accessibilité inactif",
                "Ouvrir les réglages d'accessibilité",
            ) { Disclosure.openAccessibility(c) }
        }

        SectionTitle("Mesure")
        HopeCard {
            val ok = app.hopescrolling.DeviceUsage.granted(c)
            ActionRow(
                Icons.Rounded.QueryStats,
                if (ok) "Accès aux données d'utilisation activé" else "Accès aux données d'utilisation",
                if (ok) "Le temps récupéré est mesuré par Android"
                else "Nécessaire pour mesurer le temps récupéré",
            ) { c.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        }

        SectionTitle("Mises à jour")
        HopeCard {
            Column {
                val version = c.packageManager.getPackageInfo(c.packageName, 0).versionName
                ActionRow(Icons.Rounded.Download, "Version $version", "Télécharger la dernière version") {
                    c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("$REPO_URL/releases/latest")))
                }
                divider()
                ActionRow(Icons.Rounded.Sync, "Mises à jour automatiques",
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
                ToggleRow(Icons.Rounded.BugReport, "Enregistrer les écrans",
                    "Si un écran n'est pas bloqué : active, ouvre cet écran 5 secondes, puis copie le diagnostic. Le texte de tes messages n'est pas enregistré.",
                    Prefs.diagnostic(c)) { on ->
                    Prefs.setDiagnostic(c, on)
                    if (on) Prefs.clearDumps(c)
                    UiState.bump()
                }
                divider()
                ActionRow(Icons.Rounded.ContentCopy, "Copier le diagnostic") {
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
        SectionTitle("Confidentialité")
        HopeCard {
            ActionRow(Icons.Rounded.PrivacyTip, "Politique de confidentialité",
                "Rien ne quitte ton téléphone") {
                c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
