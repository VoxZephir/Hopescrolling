package app.hopescrolling.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hopescrolling.Prefs

/**
 * Information bien visible exigée par Google Play avant d'activer un service
 * d'accessibilité : ce qui est lu, pourquoi, ce qui en est fait, et un accord
 * explicite. Affichée avant chaque ouverture des réglages d'accessibilité tant
 * que l'accord n'a pas été donné.
 */
object Disclosure {
    private var visible by mutableStateOf(false)

    fun consented(c: Context) = Prefs.sp(c).getBoolean("a11y_consent", false)

    fun openAccessibility(c: Context) {
        if (consented(c)) c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        else visible = true
    }

    @Composable
    fun Host() {
        if (!visible) return
        val c = LocalContext.current
        AlertDialog(
            onDismissRequest = { visible = false },
            icon = { Icon(Icons.Rounded.AccessibilityNew, contentDescription = null) },
            title = { Text("Utilisation de l'accessibilité") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text("Hopescrolling utilise l'API AccessibilityService d'Android pour fonctionner.",
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    Text("Ce que le service lit", style = MaterialTheme.typography.titleSmall)
                    Text("Le contenu affiché à l'écran : l'appli au premier plan, la structure de son écran (identifiants des éléments, textes comme « Sponsorisé » ou « Suivre ») et l'adresse affichée dans les navigateurs.")
                    Spacer(Modifier.height(8.dp))
                    Text("Pourquoi", style = MaterialTheme.typography.titleSmall)
                    Text("Pour reconnaître les Shorts, les Reels, TikTok, les publicités et les suggestions, puis appuyer sur Retour, revenir à l'accueil ou faire défiler à ta place. Aussi pour appliquer tes règles (pause, limites, couvre-feu) et mesurer ton temps d'écran.")
                    Spacer(Modifier.height(8.dp))
                    Text("Ce qui en est fait", style = MaterialTheme.typography.titleSmall)
                    Text("Rien ne quitte ton téléphone. Aucune donnée n'est collectée, vendue ni partagée. Seuls des compteurs (blocages, temps par appli) sont gardés sur l'appareil, et tu peux les effacer en désinstallant l'appli. Le texte de tes messages n'est jamais enregistré.")
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = {
                        c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                    }) { Text("Lire la politique de confidentialité") }
                }
            },
            confirmButton = {
                Button(onClick = {
                    Prefs.sp(c).edit().putBoolean("a11y_consent", true).apply()
                    visible = false
                    c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }) { Text("J'accepte") }
            },
            dismissButton = {
                TextButton(onClick = { visible = false }) { Text("Refuser") }
            },
        )
    }
}
