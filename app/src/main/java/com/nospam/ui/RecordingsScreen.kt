package com.nospam.ui

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nospam.data.model.CallRecording
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Elenco delle registrazioni audio con riproduzione in-app (play/stop), durata, data,
 * condivisione del singolo file ed eliminazione (con conferma).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingsScreen(vm: ConsoleViewModel, onBack: () -> Unit) {
    val recordings by vm.recordings.collectAsState()
    val context = LocalContext.current

    var playingId by remember { mutableStateOf<Long?>(null) }
    var toDelete by remember { mutableStateOf<CallRecording?>(null) }
    val player = remember { MediaPlayer() }

    fun stopPlayback() {
        runCatching { if (player.isPlaying) player.stop(); player.reset() }
        playingId = null
    }
    fun play(rec: CallRecording) {
        stopPlayback()
        runCatching {
            player.setDataSource(rec.filePath)
            player.setOnCompletionListener { playingId = null }
            player.prepare()
            player.start()
            playingId = rec.id
        }.onFailure { playingId = null }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { player.release() } }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrazioni") },
                navigationIcon = {
                    IconButton(onClick = { stopPlayback(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { pad ->
        if (recordings.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(pad).padding(24.dp)) {
                Text(
                    "Nessuna registrazione. Durante una chiamata, attiva il vivavoce e tocca " +
                        "\"Registra\" nella console. Le registrazioni compariranno qui.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(recordings) { rec ->
                    RecordingRow(
                        rec = rec,
                        isPlaying = playingId == rec.id,
                        onPlayToggle = { if (playingId == rec.id) stopPlayback() else play(rec) },
                        onShare = { EvidenceShare.shareAudio(context, rec.filePath) },
                        onDelete = { toDelete = rec },
                    )
                }
            }
        }
    }

    toDelete?.let { rec ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Eliminare la registrazione?") },
            text = {
                Text(
                    "Il file audio e la sua voce nel registro prove verranno rimossi " +
                        "definitivamente. Se ti serve come prova, condividilo prima.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (playingId == rec.id) stopPlayback()
                    vm.deleteRecording(rec)
                    toDelete = null
                }) { Text("Elimina") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Annulla") } },
        )
    }
}

private val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    return "%02d:%02d".format(totalSec / 60, totalSec % 60)
}

@Composable
private fun RecordingRow(
    rec: CallRecording,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val exists = remember(rec.filePath) { File(rec.filePath).exists() }
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPlayToggle, enabled = exists) {
                Icon(
                    if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Ferma" else "Riproduci",
                )
            }
            Column(Modifier.weight(1f)) {
                Text(rec.e164Number ?: rec.number, fontWeight = FontWeight.SemiBold)
                Text(
                    "${fmt.format(Date(rec.startedAt))} · ${formatDuration(rec.durationMs)}" +
                        if (!exists) " · file mancante" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "sha256 " + rec.fileSha256.take(12) + "…",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            IconButton(onClick = onShare, enabled = exists) {
                Icon(Icons.Filled.Share, contentDescription = "Condividi")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Elimina")
            }
        }
    }
}
