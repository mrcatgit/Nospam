package com.nospam.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.nospam.recording.CallRecordingService
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nospam.data.model.BlockRule
import com.nospam.data.model.CallRecord
import com.nospam.data.model.RuleAction
import com.nospam.data.model.RuleType
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
fun ConsoleScreen(
    vm: ConsoleViewModel,
    onOpenRules: () -> Unit = {},
    onOpenRecordings: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    pendingRecordNumber: String? = null,
    onPendingRecordConsumed: () -> Unit = {},
) {
    val calls by vm.calls.collectAsState()
    val total by vm.total.collectAsState()
    val blocked by vm.blocked.collectAsState()
    val activeRules by vm.activeRules.collectAsState()
    val recordings by vm.recordings.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Stato di registrazione AUTOREVOLE: viene dal service, non da un flag locale.
    val recording by CallRecordingService.isRecording.collectAsState()

    // Avvio registrazione protetto dal permesso microfono: senza permesso non si avvia
    // (altrimenti startForeground con tipo microfono lancia SecurityException).
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) CallRecordingService.start(context, null) }

    fun startWithPermission() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            CallRecordingService.start(context, null)
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    fun toggleRecording() {
        if (recording) CallRecordingService.stop(context) else startWithPermission()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NoSpam · Console") },
                actions = {
                    IconButton(onClick = onOpenRecordings) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = "Registrazioni")
                    }
                    IconButton(onClick = onOpenRules) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Regole")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Impostazioni")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            val uris = vm.buildShareUris(context) // snapshot unico, su IO
                            EvidenceShare.launchChooser(context, uris)
                        }
                    }) {
                        Icon(Icons.Filled.Share, contentDescription = "Condividi fascicolo")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                // In chiamata, attiva il vivavoce per captare entrambe le voci.
                onClick = { toggleRecording() },
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
                items(calls) { rec ->
                    CallRow(
                        r = rec,
                        onSearch = { WebLookup.search(context, rec.e164Number ?: rec.rawNumber) },
                        onBlockNumber = {
                            val v = rec.e164Number ?: rec.rawNumber
                            if (v.isNotBlank()) {
                                vm.addRule(
                                    BlockRule(
                                        type = RuleType.EXACT_NUMBER, value = v,
                                        action = RuleAction.BLOCK, category = rec.category,
                                        source = "manuale",
                                    )
                                )
                                android.widget.Toast.makeText(context, "Numero bloccato", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBlockSeries = rec.e164Number?.takeIf { it.length > 4 }?.let { e164 ->
                            {
                                val prefix = e164.dropLast(1)
                                vm.addRule(
                                    BlockRule(
                                        type = RuleType.PREFIX, value = prefix,
                                        action = RuleAction.BLOCK, category = rec.category,
                                        source = "serie",
                                    )
                                )
                                android.widget.Toast.makeText(context, "Serie bloccata: $prefix…", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                    )
                }
            }
        }
    }

    // Conferma in-app per l'avvio della registrazione dalla notifica. Richiesta esplicita
    // all'utente: MainActivity è esportata, quindi non avviamo mai la registrazione solo
    // perché qualcuno ha lanciato l'app con l'extra.
    if (pendingRecordNumber != null) {
        AlertDialog(
            onDismissRequest = onPendingRecordConsumed,
            title = { Text("Avviare la registrazione?") },
            text = {
                Text(
                    "Chiamata da $pendingRecordNumber. Attiva il vivavoce e conferma per " +
                        "registrare. La registrazione di una chiamata a cui partecipi è lecita.",
                )
            },
            confirmButton = {
                TextButton(onClick = { startWithPermission(); onPendingRecordConsumed() }) {
                    Text("Registra")
                }
            },
            dismissButton = {
                TextButton(onClick = onPendingRecordConsumed) { Text("Annulla") }
            },
        )
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
private fun CallRow(
    r: CallRecord,
    onSearch: () -> Unit,
    onBlockNumber: () -> Unit,
    onBlockSeries: (() -> Unit)?,
) {
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
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TextButton(onClick = onSearch) { Text("Cerca online") }
                TextButton(onClick = onBlockNumber) { Text("Blocca") }
                if (onBlockSeries != null) {
                    TextButton(onClick = onBlockSeries) { Text("Blocca serie") }
                }
            }
        }
    }
}

private fun verdictLabel(v: Verdict) = when (v) {
    Verdict.BLOCKED -> "⛔ Bloccata"
    Verdict.SILENCED -> "🔕 Silenziata"
    Verdict.ALLOWED -> "✅ Permessa"
}
