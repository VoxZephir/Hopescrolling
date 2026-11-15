@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.hopescrolling.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hopescrolling.Metric
import app.hopescrolling.Prefs
import app.hopescrolling.Rules
import app.hopescrolling.Stats
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
fun HomeScreen(serviceOn: Boolean, onEnable: () -> Unit) {
    val c = LocalContext.current
    UiState.version // relit l'état quand il change
    // Rafraîchit les comptes à rebours chaque minute.
    LaunchedEffect(Unit) { while (true) { delay(30_000); UiState.bump() } }

    val today = LocalDate.now()
    val saved = Stats.minutesSaved(c, today)
    val hour = LocalTime.now().hour
    val hello = when (hour) {
        in 5..11 -> "Bonjour"
        in 12..17 -> "Bon après-midi"
        else -> "Bonsoir"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.padding(start = 8.dp, top = 8.dp, bottom = 4.dp)) {
            Text("$hello 👋", style = MaterialTheme.typography.headlineMedium)
            Text(
                today.format(DateTimeFormatter.ofPattern("EEEE d MMMM")).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HeroCard(serviceOn, saved, onEnable)

        Rules.lockdownReason(c)?.let { reason ->
            HopeCard {
                Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(if (Rules.focusActive(c)) "🎯" else "🌙", fontSize = 28.sp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(reason, style = MaterialTheme.typography.titleMedium)
                        Text("Les applis surveillées sont fermées.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("🔥", "${Stats.streak(c)} j", "Série", Modifier.weight(1f))
            StatTile("🛑", "${Stats.count(c, today, Metric.BLOCK)}", "Vidéos bloquées", Modifier.weight(1f))
            StatTile("⏭️", "${Stats.count(c, today, Metric.SKIP)}", "Pubs sautées", Modifier.weight(1f))
        }

        HopeCard {
            Column(Modifier.padding(20.dp)) {
                Text("7 derniers jours", style = MaterialTheme.typography.titleMedium)
                val total = Stats.totalMinutesSaved(c)
                Text("${total / 60} h ${total % 60} min récupérées au total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
                WeekChart(days, days.map { Stats.minutesSaved(c, it) })
            }
        }

        FocusCard()
        PauseCard()

        Hint("Les minutes sont une estimation : 2 min par vidéo bloquée, 5 min quand tu fermes après la pause, 10 min par limite de fil atteinte.")
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HeroCard(serviceOn: Boolean, saved: Int, onEnable: () -> Unit) {
    val brush = if (serviceOn) Brush.linearGradient(listOf(DeepGreen, HopeGreen, Sunrise))
    else Brush.linearGradient(listOf(Color(0xFF5A1F22), Danger))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(brush).padding(24.dp)
    ) {
        Text(
            if (serviceOn) "✅ Protection active" else "⚠️ Protection en pause",
            color = Color.White, style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))
        if (serviceOn) {
            Text("$saved min", color = Color.White, fontSize = 52.sp,
                style = MaterialTheme.typography.displaySmall)
            Text("récupérées aujourd'hui 🌅", color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium)
        } else {
            Text("Les Shorts et les Reels ne sont pas bloqués en ce moment.",
                color = Color.White, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onEnable,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Danger),
            ) { Text("Activer") }
        }
    }
}

@Composable
private fun FocusCard() {
    val c = LocalContext.current
    UiState.version
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                EmojiBadge("🎯")
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Mode concentration", style = MaterialTheme.typography.titleMedium)
                    Text("Ferme tes applis surveillées pendant un moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            if (Rules.focusActive(c)) {
                val until = Prefs.focusUntil(c)
                val start = Prefs.sp(c).getLong("focus_start", until)
                val now = System.currentTimeMillis()
                val progress = if (until > start) (now - start).toFloat() / (until - start) else 0f
                val leftMin = ((until - now) / 60_000).toInt() + 1
                Text("Encore $leftMin min · jusqu'à " +
                    DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(until)),
                    style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = {
                    Lock.guard(c, "arrêter le mode concentration") { Prefs.setFocusUntil(c, 0) }
                }) { Text("Arrêter") }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(30 to "30 min", 60 to "1 h", 120 to "2 h", 240 to "4 h").forEach { (m, l) ->
                        FilledTonalButton(onClick = {
                            val now = System.currentTimeMillis()
                            Prefs.sp(c).edit().putLong("focus_start", now).apply()
                            Prefs.setFocusUntil(c, now + m * 60_000L)
                            UiState.bump()
                        }) { Text(l) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PauseCard() {
    val c = LocalContext.current
    UiState.version
    val today = LocalDate.now()
    val quit = Stats.count(c, today, Metric.PAUSE_QUIT)
    val cont = Stats.count(c, today, Metric.PAUSE_CONTINUE)
    if (quit + cont == 0) return
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Text("🧘 Pauses aujourd'hui", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Tu as fermé $quit fois sur ${quit + cont}.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { quit / (quit + cont).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
