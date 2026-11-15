@file:OptIn(ExperimentalMaterial3Api::class)

package app.hopescrolling.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hopescrolling.Premium
import app.hopescrolling.StoreFactory
import app.hopescrolling.StoreState

const val PRIVACY_URL = "https://voxzephir.github.io/Hopescrolling/privacy.html"

object Paywall {
    var visible by mutableStateOf(false)

    /** true si Premium est actif ; sinon ouvre l'écran Premium et renvoie false. */
    fun require(c: Context): Boolean {
        if (Premium.active(c)) return true
        visible = true
        return false
    }

    @Composable
    fun Host() {
        if (!visible) return
        val c = LocalContext.current
        UiState.version
        val offers = StoreState.offers
        ModalBottomSheet(
            onDismissRequest = { visible = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconBadge(Icons.Rounded.WorkspacePremium, size = 72,
                    container = MaterialTheme.colorScheme.primaryContainer,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(16.dp))
                Text("Hopescrolling Premium", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Le blocage des Shorts, Reels et TikTok reste gratuit. Premium ajoute les outils pour reprendre la main sur toutes tes applis.",
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                Benefit(Icons.Rounded.SelfImprovement, "Pause et limite de scroll sur n'importe quelle appli")
                Benefit(Icons.Rounded.Timer, "Limites quotidiennes, jeux compris")
                Benefit(Icons.Rounded.Bedtime, "Couvre-feu et mode concentration")
                Benefit(Icons.Rounded.SkipNext, "Pubs et suggestions Instagram sautées")
                Benefit(Icons.Rounded.Lock, "Verrou et protection anti-triche")
                Benefit(Icons.Rounded.SportsEsports, "Budget Shorts, limite du fil Instagram")
                Spacer(Modifier.height(20.dp))

                if (Premium.active(c)) {
                    Text("Premium est actif. Merci !", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                } else if (offers.isEmpty()) {
                    Text("Les offres ne sont pas disponibles pour l'instant. Vérifie ta connexion et que Google Play est à jour.",
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                } else {
                    offers.forEach { o ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable {
                                c.findActivity()?.let { StoreFactory.store.buy(it, o.id) }
                            },
                            shape = RoundedCornerShape(20.dp),
                            border = if (o.highlight) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(o.title, style = MaterialTheme.typography.titleMedium)
                                    Text(o.detail, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(o.price, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                StoreState.message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = { StoreFactory.store.restore(c) }) { Text("Restaurer mes achats") }
                TextButton(onClick = {
                    c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
                }) { Text("Politique de confidentialité") }
                Text(
                    "Paiement via Google Play. Les abonnements se renouvellent automatiquement et se résilient à tout moment dans Google Play → Paiements et abonnements.",
                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp))
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is android.content.ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Bandeau en haut d'une section réservée à Premium. */
@Composable
fun PremiumBanner(text: String) {
    val c = LocalContext.current
    UiState.version
    if (Premium.active(c)) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable { Paywall.visible = true },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WorkspacePremium, contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.weight(1f))
            Text("Découvrir", style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}
