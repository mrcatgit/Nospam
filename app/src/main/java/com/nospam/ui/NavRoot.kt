package com.nospam.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Navigazione minimale tra le due schermate senza dipendere da una libreria di routing:
 * la console e la gestione regole. Sufficiente per l'attuale superficie dell'app.
 */
@Composable
fun NavRoot(
    vm: ConsoleViewModel,
    pendingRecordNumber: String? = null,
    onPendingRecordConsumed: () -> Unit = {},
) {
    var screen by remember { mutableStateOf(Screen.CONSOLE) }
    when (screen) {
        Screen.CONSOLE -> ConsoleScreen(
            vm,
            onOpenRules = { screen = Screen.RULES },
            onOpenRecordings = { screen = Screen.RECORDINGS },
            pendingRecordNumber = pendingRecordNumber,
            onPendingRecordConsumed = onPendingRecordConsumed,
        )
        Screen.RULES -> RulesScreen(vm, onBack = { screen = Screen.CONSOLE })
        Screen.RECORDINGS -> RecordingsScreen(vm, onBack = { screen = Screen.CONSOLE })
    }
}

private enum class Screen { CONSOLE, RULES, RECORDINGS }
