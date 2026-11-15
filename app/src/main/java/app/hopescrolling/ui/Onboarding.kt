package app.hopescrolling.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Trois étapes : présentation, activation du service, c'est prêt. */
@Composable
fun Onboarding(serviceOn: Boolean, onDone: () -> Unit) {
    val c = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(serviceOn) { if (serviceOn && step == 1) step = 2 }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface).safeDrawingPadding()
            .padding(24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                Box(
                    Modifier.padding(4.dp).size(if (i == step) 24.dp else 8.dp, 8.dp).background(
                        if (i <= step) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape,
                    )
                )
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                (slideInHorizontally { it / 3 } + fadeIn()) togetherWith (slideOutHorizontally { -it / 3 } + fadeOut())
            },
            modifier = Modifier.weight(1f),
            label = "onboarding",
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when (s) {
                    0 -> Page(Icons.Rounded.WbTwilight, "Reprends ton temps",
                        "Hopescrolling referme les Shorts, les Reels et TikTok à ta place, sans bloquer le reste de tes applis.\n\nTu gardes YouTube pour les vidéos que tu cherches, et Instagram pour tes potes et tes messages.")
                    1 -> {
                        Page(Icons.Rounded.AccessibilityNew, "Active le service",
                            "Hopescrolling utilise un service d'accessibilité pour voir quel écran est affiché et appuyer sur Retour à ta place.\n\nIl voit les changements d'écran mais n'agit que sur les applis que tu choisis. Il n'a pas accès à Internet : rien ne quitte ton téléphone.")
                        Spacer(Modifier.height(24.dp))
                        Tip("1", "Appuie sur « Ouvrir l'accessibilité », choisis Hopescrolling et active-le.")
                        Tip("2", "Option grisée ? Ouvre les infos de l'appli, appuie sur ⋮ en haut à droite, puis sur « Autoriser les paramètres restreints ».")
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = {
                            c.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${c.packageName}")))
                        }) { Text("Infos de l'appli") }
                    }
                    else -> Page(Icons.Rounded.CheckCircle, "C'est prêt",
                        "Les Shorts, les Reels et TikTok sont bloqués. LinkedIn, Instagram et YouTube ont une pause à l'ouverture et une limite de scroll.\n\nTout se règle dans les onglets Blocages et Règles.")
                }
            }
        }
        Button(
            onClick = {
                when (step) {
                    0 -> step = 1
                    1 -> if (serviceOn) step = 2
                        else c.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    else -> onDone()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(when (step) {
                0 -> "Commencer"
                1 -> if (serviceOn) "Continuer" else "Ouvrir l'accessibilité"
                else -> "C'est parti"
            }, fontSize = 17.sp)
        }
        if (step < 2) {
            TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Plus tard")
            }
        }
    }
}

@Composable
private fun Page(icon: ImageVector, title: String, body: String) {
    IconBadge(icon, size = 96,
        container = MaterialTheme.colorScheme.primaryContainer,
        tint = MaterialTheme.colorScheme.onPrimaryContainer)
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun Tip(n: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(28.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(n, color = MaterialTheme.colorScheme.onPrimaryContainer) }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
