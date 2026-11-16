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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nospam.data.model.BlockRule
import com.nospam.data.model.RuleAction
import com.nospam.data.model.RuleType

/**
 * Gestione della blocklist/allowlist: elenco regole attive + aggiunta/eliminazione.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(vm: ConsoleViewModel, onBack: () -> Unit) {
    val rules by vm.rules.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Regole") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Aggiungi regola")
            }
        },
    ) { pad ->
        if (rules.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(pad).padding(24.dp)) {
                Text(
                    "Nessuna regola. Aggiungine una per bloccare numeri, prefissi o " +
                        "pattern, oppure per mettere in allowlist un contatto fidato.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(rules) { rule -> RuleRow(rule) { vm.deleteRule(rule) } }
            }
        }
    }

    if (showDialog) {
        AddRuleDialog(
            onDismiss = { showDialog = false },
            onConfirm = { rule -> vm.addRule(rule); showDialog = false },
        )
    }
}

@Composable
private fun RuleRow(rule: BlockRule, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(rule.value, style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AssistChip(onClick = {}, label = { Text(actionLabel(rule.action)) })
                    AssistChip(onClick = {}, label = { Text(typeLabel(rule.type)) })
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Elimina")
            }
        }
    }
}

@Composable
private fun AddRuleDialog(onDismiss: () -> Unit, onConfirm: (BlockRule) -> Unit) {
    var value by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RuleType.EXACT_NUMBER) }
    var action by remember { mutableStateOf(RuleAction.BLOCK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuova regola") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Valore (numero E.164, prefisso o regex)") },
                    singleLine = true,
                )
                Text("Tipo", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RuleType.entries.forEach { t ->
                        AssistChip(
                            onClick = { type = t },
                            label = { Text(typeLabel(t)) },
                            enabled = type != t,
                        )
                    }
                }
                Text("Azione", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RuleAction.entries.forEach { a ->
                        AssistChip(
                            onClick = { action = a },
                            label = { Text(actionLabel(a)) },
                            enabled = action != a,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank() || type == RuleType.WITHHELD,
                onClick = {
                    onConfirm(
                        BlockRule(
                            type = type,
                            value = if (type == RuleType.WITHHELD) "withheld" else value.trim(),
                            action = action,
                            priority = if (action == RuleAction.ALLOW) 200 else 100,
                            source = "manual",
                        )
                    )
                },
            ) { Text("Aggiungi") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
    )
}

private fun typeLabel(t: RuleType) = when (t) {
    RuleType.EXACT_NUMBER -> "Numero"
    RuleType.PREFIX -> "Prefisso"
    RuleType.REGEX -> "Regex"
    RuleType.WITHHELD -> "Anonimo"
}

private fun actionLabel(a: RuleAction) = when (a) {
    RuleAction.ALLOW -> "Permetti"
    RuleAction.SILENCE -> "Silenzia"
    RuleAction.BLOCK -> "Blocca"
}
