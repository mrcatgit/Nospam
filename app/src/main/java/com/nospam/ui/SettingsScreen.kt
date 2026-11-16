package com.nospam.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nospam.data.AppSettings

/**
 * Impostazioni dell'app. Al momento: comportamento degli avvisi di chiamata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { AppSettings(context) }

    var alertAll by remember { mutableStateOf(settings.alertAllCalls) }
    var onlyUnknown by remember { mutableStateOf(settings.alertOnlyUnknown) }
    var useOverlay by remember { mutableStateOf(settings.useOverlay) }

    val phonePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* esito gestito dal sistema */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
    ) { pad ->
        Column(
            Modifier.fillMaxSize().padding(pad).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Avvisi di chiamata", style = MaterialTheme.typography.titleMedium)

            SettingRow(
                title = "Avvisa per tutte le chiamate",
                subtitle = "Mostra un avviso anche per le chiamate consentite. Se disattivato, " +
                    "avvisa solo per quelle bloccate o sospette.",
                checked = alertAll,
                onCheckedChange = { alertAll = it; settings.alertAllCalls = it },
            )

            SettingRow(
                title = "Solo numeri sconosciuti",
                subtitle = "Per le chiamate consentite, avvisa solo se il numero non è in rubrica. " +
                    "I contatti noti non generano avviso.",
                checked = onlyUnknown,
                enabled = alertAll,
                onCheckedChange = { onlyUnknown = it; settings.alertOnlyUnknown = it },
            )

            Text(
                "Le chiamate bloccate e sospette generano sempre un avviso, a prescindere da " +
                    "queste opzioni.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )

            HorizontalDivider(Modifier.padding(vertical = 16.dp))
            Text("Popup sopra la chiamata", style = MaterialTheme.typography.titleMedium)

            SettingRow(
                title = "Mostra popup grande sopra la chiamata",
                subtitle = "Invece della notifica, mostra un riquadro grande con numero, rischio " +
                    "e i pulsanti Registra/Ferma, Rispondi e Riaggancia. Richiede il permesso " +
                    "\"Mostra sopra altre app\". Senza permesso si usa la notifica.",
                checked = useOverlay,
                onCheckedChange = { useOverlay = it; settings.useOverlay = it },
            )

            val canOverlay = Settings.canDrawOverlays(context)
            if (useOverlay && !canOverlay) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text("Concedi permesso \"Mostra sopra altre app\"") }
            }

            OutlinedButton(
                onClick = { phonePermission.launch(Manifest.permission.ANSWER_PHONE_CALLS) },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Abilita Rispondi/Riaggancia (permesso telefono)") }

            Text(
                "Rispondere o riagganciare dal popup richiede il permesso telefono e può non " +
                    "funzionare su tutti i modelli. Provalo sul tuo dispositivo.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked && enabled, enabled = enabled, onCheckedChange = onCheckedChange)
    }
}
