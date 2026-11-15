package com.nospam.data.repo

import com.nospam.data.db.BlockRuleDao
import com.nospam.data.db.CallRecordDao
import com.nospam.data.db.CallRecordingDao
import com.nospam.data.model.BlockRule
import com.nospam.data.model.CallRecord
import com.nospam.data.model.CallRecording
import com.nospam.data.model.Verdict
import com.nospam.forensic.EvidenceHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Unico punto di accesso ai dati. Garantisce che ogni [CallRecord] venga inserito
 * agganciandolo alla catena di custodia (hash concatenati), così il registro resta
 * verificabile e difendibile come prova.
 */
class CallRepository(
    private val callDao: CallRecordDao,
    private val ruleDao: BlockRuleDao,
    private val recordingDao: CallRecordingDao,
) {

    // Serializza letture+scritture della catena di custodia: due chiamate screenate in
    // parallelo non devono agganciarsi allo stesso predecessore (fork della catena).
    private val chainMutex = Mutex()

    fun observeCalls(): Flow<List<CallRecord>> = callDao.observeAll()
    fun observeRecordings(): Flow<List<CallRecording>> = recordingDao.observeAll()

    suspend fun addRecording(recording: CallRecording): Long = recordingDao.insert(recording)
    suspend fun deleteRecording(recording: CallRecording) = recordingDao.delete(recording)
    suspend fun allRecordings(): List<CallRecording> = recordingDao.allOrdered()
    fun observeRules(): Flow<List<BlockRule>> = ruleDao.observeAll()
    fun totalCalls(): Flow<Int> = callDao.total()
    fun blockedCount(): Flow<Int> = callDao.countByVerdict(Verdict.BLOCKED)
    fun activeRuleCount(): Flow<Int> = ruleDao.activeCount()

    suspend fun activeRules(): List<BlockRule> = ruleDao.activeRules()

    suspend fun addRule(rule: BlockRule): Long = ruleDao.insert(rule)
    suspend fun updateRule(rule: BlockRule) = ruleDao.update(rule)
    suspend fun deleteRule(rule: BlockRule) = ruleDao.delete(rule)
    suspend fun importRules(rules: List<BlockRule>) = ruleDao.insertAll(rules)

    suspend fun historyFor(e164: String): List<CallRecord> = callDao.byNumber(e164)
    suspend fun topOffenders(sinceMillis: Long, limit: Int = 10): List<String> =
        callDao.topOffenders(sinceMillis, limit)

    suspend fun allOrdered(): List<CallRecord> = callDao.allOrdered()

    /**
     * Inserisce un evento calcolando l'hash a partire dall'ultimo record presente.
     * Il campo [CallRecord.hash] del record passato viene ignorato e ricalcolato qui,
     * per impedire che un chiamante possa falsare la catena.
     */
    suspend fun recordCall(record: CallRecord): CallRecord = chainMutex.withLock {
        val prev = callDao.latest()
        val prevHash = prev?.hash ?: EvidenceHasher.GENESIS
        val toStore = record.copy(
            prevHash = prevHash,
            hash = EvidenceHasher.hashRecord(record, prevHash),
        )
        val id = callDao.insert(toStore)
        toStore.copy(id = id)
    }

    /** Verifica l'integrità dell'intero registro. */
    suspend fun verifyIntegrity(): EvidenceHasher.ChainResult =
        EvidenceHasher.verifyChain(callDao.allOrdered())
}
