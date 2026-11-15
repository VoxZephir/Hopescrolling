@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package app.hopescrolling.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.hopescrolling.Apps
import app.hopescrolling.DeviceUsage
import app.hopescrolling.Equivalence
import app.hopescrolling.Metric
import app.hopescrolling.Prefs
import app.hopescrolling.Rules
import app.hopescrolling.Stats
import app.hopescrolling.formatDuration
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Date

@Composable
fun HomeScreen(serviceOn: Boolean, onEnable: () -> Unit) {
    val c = LocalContext.current
    UiState.version
    LaunchedEffect(Unit) { while (true) { delay(30_000); UiState.bump() } }

    val today = LocalDate.now()
    val hello = when (LocalTime.now().hour) {
        in 5..11 -> "Bonjour"
        in 12..17 -> "Bon après-midi"
        else -> "Bonsoir"
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.padding(start = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(hello, style = MaterialTheme.typography.headlineLarge)
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE d MMMM")).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(serviceOn)
        }

        HeroCard(serviceOn, Stats.minutesSaved(c, today), onEnable)
        if (serviceOn && !DeviceUsage.granted(c)) UsageAccessCard()

        Rules.lockdownReason(c)?.let { reason ->
            HopeCard {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(if (Rules.focusActive(c)) Icons.Rounded.CenterFocusStrong else Icons.Rounded.Bedtime)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(reason, style = MaterialTheme.typography.titleMedium)
                        Text("Tes applis surveillées sont fermées.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(Icons.Rounded.LocalFireDepartment, "${Stats.streak(c)} j", "Série", Modifier.weight(1f))
            StatTile(Icons.Rounded.Block, "${Stats.count(c, today, Metric.BLOCK)}", "Bloquées", Modifier.weight(1f))
            StatTile(Icons.Rounded.SkipNext, "${Stats.count(c, today, Metric.SKIP)}", "Sautées", Modifier.weight(1f))
        }

        ScreenTimeCard()
        WeekCard()
        EquivalenceCard()
        FocusCard()
        PauseCard()
        Spacer(Modifier.height(8.dp))
    }
}

/** Demande l'accès aux statistiques d'utilisation, nécessaire pour mesurer le temps récupéré. */
@Composable
private fun UsageAccessCard() {
    val c = LocalContext.current
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.QueryStats)
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Mesurer ton temps récupéré", style = MaterialTheme.typography.titleMedium)
                    Text("Hopescrolling compare ton temps sur ces applis à ta moyenne avant de l'installer, grâce aux statistiques d'Android. Rien ne quitte ton téléphone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            FilledTonalButton(onClick = {
                c.startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) { Text("Autoriser l'accès") }
        }
    }
}

@Composable
private fun StatusPill(on: Boolean) {
    val bg = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val fg = if (on) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(if (on) HopeGreen else Danger, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(if (on) "Actif" else "Inactif", style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

@Composable
private fun HeroCard(serviceOn: Boolean, saved: Int, onEnable: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    if (!serviceOn) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(cs.errorContainer).padding(24.dp)
        ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = cs.onErrorContainer)
            Spacer(Modifier.height(12.dp))
            Text("Protection en pause", style = MaterialTheme.typography.headlineSmall, color = cs.onErrorContainer)
            Text("Les Shorts, les Reels et tes règles ne sont pas appliqués en ce moment.",
                style = MaterialTheme.typography.bodyLarge, color = cs.onErrorContainer)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onEnable,
                colors = ButtonDefaults.buttonColors(containerColor = cs.error, contentColor = cs.onError)
            ) { Text("Réactiver") }
        }
        return
    }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp))
            .background(Brush.linearGradient(listOf(cs.primaryContainer, cs.tertiaryContainer)))
            .padding(24.dp)
    ) {
        Text("Temps récupéré aujourd'hui", style = MaterialTheme.typography.titleMedium,
            color = cs.onPrimaryContainer)
        Spacer(Modifier.height(4.dp))
        Text(formatDuration(saved), style = MaterialTheme.typography.displayMedium,
            color = cs.onPrimaryContainer)
        val c = LocalContext.current
        val base = if (DeviceUsage.granted(c)) DeviceUsage.baseline(c) else null
        Text(
            when {
                !DeviceUsage.granted(c) -> "Autorise l'accès aux données d'utilisation pour le mesurer."
                base == null -> "Android n'a pas encore assez d'historique pour comparer."
                saved > 0 -> "Soit ${Equivalence.short(saved)}, par rapport à ta moyenne d'avant (${formatDuration(base)} par jour)."
                else -> "Ta moyenne d'avant : ${formatDuration(base)} par jour sur ces applis."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onPrimaryContainer.copy(alpha = 0.8f),
        )
    }
}

/** Temps réellement passé sur les applis surveillées, mesuré par le service. */
@Composable
private fun ScreenTimeCard() {
    val c = LocalContext.current
    UiState.version
    val today = LocalDate.now()
    val total = Stats.screenMinutes(c, today)
    val avg = Stats.screenAverage(c)
    val apps = Stats.screenByApp(c, today).take(4)
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Text("Temps d'écran", style = MaterialTheme.typography.titleMedium)
            Text("Sur tes applis surveillées, aujourd'hui", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatDuration(total), style = MaterialTheme.typography.displaySmall)
                if (avg != null && avg > 0) {
                    val diff = ((total - avg) * 100f / avg).toInt()
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (diff <= 0) "−${-diff} % vs ta moyenne" else "+$diff % vs ta moyenne",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (diff <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            if (apps.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Rien pour l'instant. Le temps s'affiche dès que tu ouvres une appli surveillée.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val max = apps.maxOfOrNull { it.second } ?: 1
            apps.forEach { (pkg, min) ->
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppIcon(pkg, 28)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(Apps.label(c, pkg), style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            Text(formatDuration(min), style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { min / max.toFloat() },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekCard() {
    val c = LocalContext.current
    UiState.version
    var mode by rememberSaveable { mutableIntStateOf(0) }
    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Text("7 derniers jours", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(mode == 0, { mode = 0 }, { Text("Récupéré") })
                FilterChip(mode == 1, { mode = 1 }, { Text("Temps d'écran") })
            }
            Spacer(Modifier.height(16.dp))
            WeekChart(days, days.map {
                if (mode == 0) Stats.minutesSaved(c, it) else Stats.screenMinutes(c, it)
            })
        }
    }
}

@Composable
private fun EquivalenceCard() {
    val c = LocalContext.current
    UiState.version
    val total = Stats.totalMinutesSaved(c)
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Text("Depuis le début", style = MaterialTheme.typography.titleMedium)
            Text("${formatDuration(total)} récupérées par rapport à ta moyenne d'avant, c'est l'équivalent de :",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            val items = Equivalence.DASHBOARD
            items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { e -> EquivalenceTile(e, total, Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun iconFor(e: Equivalence): ImageVector = when (e) {
    Equivalence.BOOKS, Equivalence.PAGES -> Icons.AutoMirrored.Rounded.MenuBook
    Equivalence.WALK_KM -> Icons.AutoMirrored.Rounded.DirectionsWalk
    Equivalence.MOVIES -> Icons.Rounded.Movie
    Equivalence.WORKOUTS -> Icons.Rounded.FitnessCenter
    Equivalence.LESSONS -> Icons.Rounded.SelfImprovement
    Equivalence.NIGHTS -> Icons.Rounded.Bedtime
}

@Composable
private fun EquivalenceTile(e: Equivalence, minutes: Int, modifier: Modifier) {
    val text = e.format(minutes)
    val number = text.substringBefore(' ')
    val unit = text.substringAfter(' ')
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
    ) {
        Icon(iconFor(e), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(number, style = MaterialTheme.typography.headlineMedium)
        Text(unit, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun FocusCard() {
    val c = LocalContext.current
    UiState.version
    HopeCard {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.CenterFocusStrong)
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
                Text("Encore ${formatDuration(leftMin)} · jusqu'à " +
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
                            if (Paywall.require(c)) { Rules.startFocus(c, m); UiState.bump() }
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Rounded.SelfImprovement)
                Column(Modifier.padding(start = 16.dp)) {
                    Text("Pauses aujourd'hui", style = MaterialTheme.typography.titleMedium)
                    Text("Tu as refermé $quit fois sur ${quit + cont}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { quit / (quit + cont).toFloat() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
