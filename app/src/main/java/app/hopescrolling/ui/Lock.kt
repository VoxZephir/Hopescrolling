package app.hopescrolling.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.hopescrolling.Prefs
import kotlinx.coroutines.delay

/** Verrou anti-triche : attendre 30 s et recopier une phrase pour assouplir une règle. */
object Lock {
    const val PHRASE = "Je choisis de perdre mon temps"
    const val WAIT_S = 30

    private class Request(val what: String, val onOk: () -> Unit)
    private var pending by mutableStateOf<Request?>(null)

    /** Exécute [onOk] tout de suite, ou après le verrou s'il est activé. */
    fun guard(c: Context, what: String, onOk: () -> Unit) {
        if (!Prefs.lockEnabled(c)) { onOk(); UiState.bump(); return }
        pending = Request(what) { onOk(); UiState.bump() }
    }

    /** À placer une fois en haut de l'écran. */
    @Composable
    fun Host() {
        val req = pending ?: return
        var left by remember(req) { mutableIntStateOf(WAIT_S) }
        var typed by remember(req) { mutableStateOf("") }
        LaunchedEffect(req) {
            while (left > 0) { delay(1000); left-- }
        }
        val ok = left == 0 && typed.trim().equals(PHRASE, ignoreCase = true)
        AlertDialog(
            onDismissRequest = { pending = null },
            icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
            title = { Text("Verrou") },
            text = {
                Column {
                    Text("Pour ${req.what}, attends $WAIT_S secondes et recopie :")
                    Spacer(Modifier.height(8.dp))
                    Text("« $PHRASE »", fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = typed,
                        onValueChange = { typed = it },
                        placeholder = { Text(PHRASE) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { 1f - left / WAIT_S.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(enabled = ok, onClick = { pending = null; req.onOk() }) {
                    Text(if (left > 0) "Confirmer (${left}s)" else "Confirmer")
                }
            },
            dismissButton = {
                TextButton(onClick = { pending = null }) { Text("Laisser tomber") }
            },
        )
    }
}
