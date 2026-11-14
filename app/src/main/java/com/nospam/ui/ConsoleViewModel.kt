package com.nospam.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nospam.data.model.BlockRule
import com.nospam.data.model.CallRecord
import com.nospam.data.repo.CallRepository
import com.nospam.forensic.EvidenceExporter
import com.nospam.forensic.EvidenceHasher
import com.nospam.report.ComplaintReport
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Stato e azioni della console. Espone i flussi reattivi del registro e delle regole,
 * e le operazioni di export (fascicolo JSON e testo denuncia).
 */
class ConsoleViewModel(private val repo: CallRepository) : ViewModel() {

    val calls: StateFlow<List<CallRecord>> =
        repo.observeCalls().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val rules: StateFlow<List<BlockRule>> =
        repo.observeRules().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val total: StateFlow<Int> =
        repo.totalCalls().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val blocked: StateFlow<Int> =
        repo.blockedCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val activeRules: StateFlow<Int> =
        repo.activeRuleCount().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    val recordings: StateFlow<List<com.nospam.data.model.CallRecording>> =
        repo.observeRecordings().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addRule(rule: BlockRule) = viewModelScope.launch { repo.addRule(rule) }
    fun deleteRule(rule: BlockRule) = viewModelScope.launch { repo.deleteRule(rule) }

    suspend fun buildEvidenceJson(): String =
        EvidenceExporter.toJson(
            EvidenceExporter.buildBundle(repo.allOrdered(), repo.allRecordings())
        )

    suspend fun buildComplaintText(): String =
        ComplaintReport().build(repo.allOrdered(), repo.allRecordings())

    suspend fun verifyIntegrity(): EvidenceHasher.ChainResult = repo.verifyIntegrity()

    class Factory(private val repo: CallRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ConsoleViewModel(repo) as T
    }
}
