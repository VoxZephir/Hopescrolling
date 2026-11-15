@file:OptIn(ExperimentalLayoutApi::class)

package app.hopescrolling

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hopescrolling.ui.HopeTheme
import kotlinx.coroutines.delay
import java.time.LocalDate

/** Pause consciente : respirer, dire pourquoi on ouvre l'appli, puis choisir. */
class PauseActivity : ComponentActivity() {

    private lateinit var pkg: String
    private var decided = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        pkg = intent.getStringExtra(EXTRA_PKG) ?: run { finish(); return }
        val name = Apps.label(this, pkg)
        val today = LocalDate.now()
        val opens = Stats.count(this, today, Metric.PAUSE_CONTINUE) +
            Stats.count(this, today, Metric.PAUSE_QUIT) + 1
        setContent {
            HopeTheme {
                BackHandler { quit() }
                PauseScreen(name, opens, onQuit = ::quit, onContinue = ::proceed)
            }
        }
    }

    private fun proceed() {
        decided = true
        Passes.grant(pkg)
        Passes.pausingPkg = null
        Stats.record(this, Metric.PAUSE_CONTINUE)
        finish()
    }

    private fun quit() {
        decided = true
        Passes.pausingPkg = null
        Stats.record(this, Metric.PAUSE_QUIT)
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onDestroy() {
        if (!decided && Passes.pausingPkg == pkg) Passes.pausingPkg = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        const val WAIT_S = 8
    }
}

private data class Reason(val emoji: String, val text: String, val impulsive: Boolean)

private val REASONS = listOf(
    Reason("💬", "Répondre à quelqu'un", false),
    Reason("🔎", "Chercher un truc précis", false),
    Reason("📣", "Publier quelque chose", false),
    Reason("🫠", "Par réflexe", true),
    Reason("🥱", "Je m'ennuie", true),
)

private val IDEAS = listOf(
    "Boire un grand verre d'eau 💧",
    "Marcher 5 minutes 🚶",
    "Envoyer un message à quelqu'un que tu aimes 💌",
    "Lire 3 pages d'un livre 📖",
    "Ranger un truc qui traîne 🧺",
    "Regarder par la fenêtre 1 minute 🌤️",
)

@Composable
private fun PauseScreen(name: String, opens: Int, onQuit: () -> Unit, onContinue: () -> Unit) {
    var left by remember { mutableIntStateOf(PauseActivity.WAIT_S) }
    var inhale by remember { mutableStateOf(true) }
    var reason by remember { mutableStateOf<Reason?>(null) }
    val idea = remember { IDEAS.random() }
    LaunchedEffect(Unit) { while (left > 0) { delay(1000); left-- } }
    LaunchedEffect(Unit) { while (true) { delay(4000); inhale = !inhale } }

    val t = rememberInfiniteTransition(label = "breath")
    val scale by t.animateFloat(
        0.65f, 1f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )

    Column(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F2E21), Color(0xFF081A13))))
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(0.5f))
        Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(220.dp).scale(scale).background(Color(0x332E7D57), CircleShape))
            Box(Modifier.size(160.dp).scale(scale).background(Color(0xFF2E7D57), CircleShape))
            Text(if (inhale) "Inspire…" else "Expire…", color = Color.White, fontSize = 22.sp)
        }
        Spacer(Modifier.height(32.dp))
        Text("Pourquoi tu ouvres $name ?", color = Color.White, fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("${opens}e ouverture aujourd'hui", color = Color(0xFFB2DFC9), fontSize = 15.sp)
        Spacer(Modifier.height(24.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            REASONS.forEach { r ->
                val selected = reason == r
                Text(
                    "${r.emoji}  ${r.text}",
                    color = if (selected) Color(0xFF0F2E21) else Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .background(if (selected) Color(0xFFB2DFC9) else Color(0x1FFFFFFF), RoundedCornerShape(50))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                        .clickable { reason = r }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                )
            }
        }

        AnimatedVisibility(reason?.impulsive == true) {
            Text(
                "Et si tu faisais autre chose ?\n$idea",
                color = Color(0xFFFFD54F), fontSize = 16.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onQuit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB2DFC9), contentColor = Color(0xFF0F2E21)),
        ) { Text("Non, je ferme", fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(8.dp))
        val canGo = left == 0 && reason != null
        TextButton(
            onClick = onContinue,
            enabled = canGo,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            Text(
                when {
                    left > 0 -> "Continuer vers $name dans ${left}s"
                    reason == null -> "Choisis une raison pour continuer"
                    else -> "Continuer vers $name"
                },
                color = if (canGo) Color.White else Color(0x80FFFFFF),
            )
        }
    }
}
