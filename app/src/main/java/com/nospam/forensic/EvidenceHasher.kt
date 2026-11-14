package com.nospam.forensic

import com.nospam.data.model.CallRecord
import java.security.MessageDigest

/**
 * Catena di custodia per il registro chiamate.
 *
 * Ogni record viene firmato con SHA-256 su un payload canonico che include l'hash del
 * record precedente. Questo crea una catena ininterrotta: modificare o eliminare un
 * record a posteriori rompe tutti gli hash successivi, e la rottura è rilevabile con
 * [verifyChain]. È lo stesso principio di integrità usato nei log a prova di
 * manomissione: non prova "chi" ha chiamato, ma prova che il TUO registro non è stato
 * alterato dopo la raccolta — requisito chiave per una prova credibile.
 */
object EvidenceHasher {

    /** Hash iniziale della catena (nessun record precedente). */
    const val GENESIS = "0000000000000000000000000000000000000000000000000000000000000000"

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Payload canonico e deterministico. L'ordine dei campi è fisso e NON include
     * [CallRecord.hash] (ovviamente) né l'id autogenerato, che non è noto al momento
     * del calcolo.
     */
    fun canonicalPayload(r: CallRecord, prevHash: String): String = buildString {
        append(prevHash); append('|')
        append(r.rawNumber); append('|')
        append(r.e164Number ?: ""); append('|')
        append(r.regionOrCarrier ?: ""); append('|')
        append(r.timestamp); append('|')
        append(r.verdict.name); append('|')
        append(r.category.name); append('|')
        append(r.riskScore); append('|')
        append(r.reasons.joinToString(",")); append('|')
        append(r.knownContact); append('|')
        append(r.userNote ?: "")
    }

    fun hashRecord(r: CallRecord, prevHash: String): String =
        sha256(canonicalPayload(r, prevHash))

    data class ChainResult(
        val valid: Boolean,
        val checked: Int,
        /** id del primo record in cui la catena risulta rotta, se presente. */
        val brokenAtId: Long? = null,
    )

    /** Ricalcola l'intera catena e segnala la prima incongruenza. */
    fun verifyChain(records: List<CallRecord>): ChainResult {
        var prev = GENESIS
        for (r in records) {
            val expected = hashRecord(r, prev)
            if (expected != r.hash || r.prevHash != prev) {
                return ChainResult(valid = false, checked = records.size, brokenAtId = r.id)
            }
            prev = r.hash
        }
        return ChainResult(valid = true, checked = records.size)
    }
}
