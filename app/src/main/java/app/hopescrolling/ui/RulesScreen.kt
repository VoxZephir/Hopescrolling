@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.hopescrolling.ui

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.hopescrolling.Prefs
import app.hopescrolling.Rules
import app.hopescrolling.ShortsBudget
import app.hopescrolling.Stats

@Composable
fun RulesScreen() {
    val c = LocalContext.current
    UiState.version
    Column {
        ScreenHeader("Règles", "Des limites qui s'appliquent toutes seules.")

        SectionTitle("Couvre-feu")
        HopeCard {
            Column {
                ToggleRow("🌙", "Couvre-feu",
                    "Instagram, YouTube et TikTok sont fermés pendant ces heures.",
                    Prefs.curfewOn(c)) { on ->
                    if (on) { Prefs.setCurfewOn(c, true); UiState.bump() }
                    else Lock.guard(c, "désactiver le couvre-feu") { Prefs.setCurfewOn(c, false) }
                }
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TimeBox("De", Prefs.curfewStart(c), Modifier.weight(1f)) {
                        Lock.guard(c, "changer les horaires du couvre-feu") { pickTime(c, true) }
                    }
                    Text("→", style = MaterialTheme.typography.headlineSmall)
                    TimeBox("À", Prefs.curfewEnd(c), Modifier.weight(1f)) {
                        Lock.guard(c, "changer les horaires du couvre-feu") { pickTime(c, false) }
                    }
                }
            }
        }

        SectionTitle("Budget Shorts / Reels")
        HopeCard {
            Column(Modifier.padding(20.dp)) {
                val budget = Prefs.shortsBudgetMin(c)
                Text(
                    if (budget == 0) "Toujours bloqués" else "$budget min par jour",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text("Les Shorts et les Reels restent ouverts tant qu'il reste du budget.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 5, 10, 15, 30).forEach { v ->
                        FilterChip(
                            selected = budget == v,
                            onClick = {
                                if (v > budget) Lock.guard(c, "augmenter ton budget") { Prefs.setShortsBudgetMin(c, v) }
                                else { Prefs.setShortsBudgetMin(c, v); UiState.bump() }
                            },
                            label = { Text(if (v == 0) "Aucun" else "$v min") },
                        )
                    }
                }
                if (budget > 0) {
                    val usedMin = (Stats.shortsMsToday(c) / 60_000).toInt()
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { (usedMin / budget.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Utilisé aujourd'hui : $usedMin min · reste ${ShortsBudget.remainingMin(c)} min",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Hint("Le mode concentration se lance depuis l'onglet Accueil.")
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TimeBox(label: String, minutes: Int, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(Rules.fmt(minutes), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

private fun pickTime(c: Context, start: Boolean) {
    val cur = if (start) Prefs.curfewStart(c) else Prefs.curfewEnd(c)
    TimePickerDialog(c, { _, h, m ->
        val v = h * 60 + m
        if (start) Prefs.setCurfew(c, v, Prefs.curfewEnd(c))
        else Prefs.setCurfew(c, Prefs.curfewStart(c), v)
        UiState.bump()
    }, cur / 60, cur % 60, true).show()
}
