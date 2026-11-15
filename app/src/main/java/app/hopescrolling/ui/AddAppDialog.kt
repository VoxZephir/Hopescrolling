package app.hopescrolling.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.hopescrolling.Apps
import app.hopescrolling.KNOWN_APPS

/** Plein écran : suggestions d'applis addictives installées, puis toutes les autres. */
@Composable
fun AddAppDialog(onClose: () -> Unit) {
    val c = LocalContext.current
    var query by remember { mutableStateOf("") }
    var added by remember { mutableStateOf(setOf<String>()) }
    val all = remember {
        Apps.launchable(c).map { it to Apps.label(c, it) }.sortedBy { it.second.lowercase() }
    }
    val managed = Apps.managed(c) + added
    val candidates = all.filter { (pkg, _) -> pkg !in managed }
        .filter { (_, label) -> query.isBlank() || label.contains(query.trim(), ignoreCase = true) }
    val suggested = candidates.filter { it.first in KNOWN_APPS }
        .sortedBy { KNOWN_APPS.indexOf(it.first) }
    val others = candidates.filter { it.first !in KNOWN_APPS }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.safeDrawingPadding()) {
                Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Fermer") }
                    Text("Ajouter une appli", style = MaterialTheme.typography.titleLarge)
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("Rechercher") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
                LazyColumn(Modifier.fillMaxSize()) {
                    if (suggested.isNotEmpty()) {
                        item { SectionTitle("Suggestions", Modifier.padding(start = 8.dp)) }
                        items(suggested, key = { it.first }) { (pkg, label) ->
                            PickRow(pkg, label) { Apps.add(c, pkg); added = added + pkg; UiState.bump() }
                        }
                    }
                    if (others.isNotEmpty()) {
                        item { SectionTitle("Toutes les applis", Modifier.padding(start = 8.dp)) }
                        items(others, key = { it.first }) { (pkg, label) ->
                            PickRow(pkg, label) { Apps.add(c, pkg); added = added + pkg; UiState.bump() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickRow(pkg: String, label: String, onAdd: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onAdd).padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(pkg, 40)
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.Add, contentDescription = "Ajouter", tint = MaterialTheme.colorScheme.primary)
    }
}
