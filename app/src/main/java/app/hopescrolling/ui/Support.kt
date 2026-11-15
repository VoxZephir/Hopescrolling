@file:OptIn(ExperimentalLayoutApi::class)

package app.hopescrolling.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.hopescrolling.BuildConfig
import app.hopescrolling.Prefs
import app.hopescrolling.Stats
import app.hopescrolling.StoreFactory
import app.hopescrolling.StoreState
import app.hopescrolling.formatDuration

object Support {
    /** Dons possibles dans cette version ? (Play : produits Google Play ; GitHub : lien configuré.) */
    fun available(): Boolean =
        if (BuildConfig.REQUIRE_PREMIUM) StoreState.tips.isNotEmpty() else BuildConfig.DONATE_URL.isNotBlank()
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}

/** Boutons de don : montants Google Play, ou lien externe dans la version GitHub. */
@Composable
fun DonateButtons() {
    val c = LocalContext.current
    if (BuildConfig.REQUIRE_PREMIUM) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StoreState.tips.forEach { t ->
                FilledTonalButton(onClick = { c.activity()?.let { StoreFactory.store.tip(it, t.id) } }) {
                    Text(t.price)
                }
            }
        }
        StoreState.message?.let {
            Spacer(Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    } else {
        FilledTonalButton(onClick = {
            c.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BuildConfig.DONATE_URL)))
        }) { Text("Faire un don") }
    }
}

/** Section « Soutenir » des réglages. */
@Composable
fun SupportSection() {
    UiState.version
    if (!Support.available()) return
    SectionTitle("Soutenir Hopescrolling")
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Favorite)
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Faire un don", style = MaterialTheme.typography.titleMedium)
                    Text("Hopescrolling n'a ni pub ni traceur. Un don, même petit, aide à le faire vivre.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            DonateButtons()
        }
    }
}

/**
 * Carte discrète sur l'accueil, une seule fois, quand l'appli t'a fait gagner
 * au moins 5 h. Elle disparaît définitivement dès qu'on la ferme.
 */
@Composable
fun SupportNudge() {
    val c = LocalContext.current
    UiState.version
    val p = Prefs.sp(c)
    if (!Support.available() || p.getBoolean("support_nudge_done", false)) return
    val total = Stats.totalMinutesSaved(c)
    if (total < 5 * 60) return
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.Favorite)
                Column(Modifier.padding(start = 16.dp)) {
                    Text("${formatDuration(total)} récupérées", style = MaterialTheme.typography.titleMedium)
                    Text("Si Hopescrolling t'aide, tu peux soutenir son développement.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            DonateButtons()
            TextButton(onClick = {
                p.edit().putBoolean("support_nudge_done", true).apply()
                UiState.bump()
            }) { Text("Plus tard") }
        }
    }
}
