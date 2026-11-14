package com.nospam.forensic

import com.nospam.data.model.CallRecord
import com.nospam.data.model.SpamCategory
import com.nospam.data.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Verifica che la catena di custodia rilevi le manomissioni. */
class EvidenceHasherTest {

    private fun record(id: Long, number: String, prevHash: String): CallRecord {
        val base = CallRecord(
            id = id,
            rawNumber = number,
            e164Number = number,
            regionOrCarrier = "IT",
            timestamp = 1_700_000_000_000 + id,
            verdict = Verdict.BLOCKED,
            category = SpamCategory.TELEMARKETING,
            riskScore = 80,
            reasons = listOf("test"),
            knownContact = false,
            prevHash = prevHash,
            hash = "",
        )
        return base.copy(hash = EvidenceHasher.hashRecord(base, prevHash))
    }

    private fun chainOf(n: Int): List<CallRecord> {
        val list = mutableListOf<CallRecord>()
        var prev = EvidenceHasher.GENESIS
        for (i in 1..n) {
            val r = record(i.toLong(), "+39333000000$i", prev)
            list += r
            prev = r.hash
        }
        return list
    }

    @Test fun validChainVerifies() {
        val chain = chainOf(5)
        val result = EvidenceHasher.verifyChain(chain)
        assertTrue(result.valid)
        assertEquals(5, result.checked)
    }

    @Test fun tamperingIsDetected() {
        val chain = chainOf(5).toMutableList()
        // Un attaccante cambia il numero del 3° record mantenendo l'hash vecchio
        chain[2] = chain[2].copy(e164Number = "+390000000000")
        val result = EvidenceHasher.verifyChain(chain)
        assertFalse(result.valid)
        assertEquals(3L, result.brokenAtId)
    }
}
