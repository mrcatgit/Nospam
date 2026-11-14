package com.nospam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nospam.recording.CallRecordingService
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nospam.data.model.CallRecord
import com.nospam.data.model.Verdict
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Console di comando: statistiche in alto, timeline eventi sotto. Deliberatamente
 * compatta; l'estensione a più schede (regole, export) è prevista nella roadmap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleScreen(vm: ConsoleViewModel) {
    val calls by vm.calls.collectAsState()
    val total by vm.total.collectAsState()
    val blocked by vm.blocked.collectAsState()
    val activeRules by vm.activeRules.collectAsState()
    val recordings by vm.recordings.collectAsState()

    val context = LocalContext.current
    var recording by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("NoSpam · Console") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (recording) {
                        CallRecordingService.stop(context)
                    } else {
                        // In chiamata, attiva il vivavoce per captare entrambe le voci.
                        CallRecordingService.start(context, number = null)
                    }
                    recording = !recording
                },
                icon = {
                    Icon(
                        if (recording) Icons.Filled.Stop else Icons.Filled.FiberManualRecord,
                        contentDescription = null,
                    )
                },
                text = { Text(if (recording) "Ferma" else "Registra") },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Totali", total.toString(), Modifier.weight(1f))
                StatCard("Bloccate", blocked.toString(), Modifier.weight(1f))
                StatCard("Regole", activeRules.toString(), Modifier.weight(1f))
                StatCard("Reg.", recordings.size.toString(), Modifier.weight(1f))
            }
            Text("Eventi recenti", style = MaterialTheme.typography.titleMedium)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(calls) { CallRow(it) }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private val fmt = SimpleDateFormat("dd/MM HH:mm", Locale.ITALY)

@Composable
private fun CallRow(r: CallRecord) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(r.e164Number ?: r.rawNumber.ifBlank { "Anonimo" }, fontWeight = FontWeight.SemiBold)
                Text(verdictLabel(r.verdict))
            }
            Text("${fmt.format(Date(r.timestamp))} · rischio ${r.riskScore}/100 · ${r.category.label}",
                style = MaterialTheme.typography.bodySmall)
            Text(r.reasons.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
            Text(r.hash.take(16) + "…", fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun verdictLabel(v: Verdict) = when (v) {
    Verdict.BLOCKED -> "⛔ Bloccata"
    Verdict.SILENCED -> "🔕 Silenziata"
    Verdict.ALLOWED -> "✅ Permessa"
}
