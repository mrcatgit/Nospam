package com.nospam.forensic

import com.nospam.data.model.CallRecord
import com.nospam.data.model.CallRecording
import com.nospam.data.model.SpamCategory
import com.nospam.data.model.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Valida la serializzazione del fascicolo prove (richiede il plugin Kotlin
 * serialization): se i serializer non venissero generati, toJson lancerebbe a runtime.
 * Questo test gira sulla JVM (testDebugUnitTest), senza emulatore.
 */
class EvidenceExporterTest {

    private fun record(id: Long, prevHash: String): CallRecord {
        val base = CallRecord(
            id = id,
            rawNumber = "+39894120455",
            e164Number = "+39894120455",
            regionOrCarrier = "Italia",
            timestamp = 1_700_000_000_000 + id,
            verdict = Verdict.BLOCKED,
            category = SpamCategory.TELEMARKETING,
            riskScore = 85,
            reasons = listOf("Prefisso a sovrapprezzo", "Chiamate ripetute"),
            knownContact = false,
            prevHash = prevHash,
            hash = "",
        )
        return base.copy(hash = EvidenceHasher.hashRecord(base, prevHash))
    }

    @Test fun bundleSerializesAndSeals() {
        val r1 = record(1, EvidenceHasher.GENESIS)
        val r2 = record(2, r1.hash)
        val recordings = listOf(
            CallRecording(
                id = 1,
                number = "+39894120455",
                e164Number = "+39894120455",
                startedAt = 1_700_000_100_000,
                durationMs = 42_000,
                filePath = "/data/rec_1.m4a",
                fileSha256 = "a".repeat(64),
            )
        )

        val bundle = EvidenceExporter.buildBundle(listOf(r1, r2), recordings)
        val json = EvidenceExporter.toJson(bundle)

        // Serializzazione riuscita e contenuti attesi presenti
        assertTrue(json.contains("\"tool\": \"NoSpam\""))
        assertTrue(json.contains("\"eventCount\": 2"))
        assertTrue(json.contains("\"recordingCount\": 1"))
        assertTrue(json.contains("rec_1.m4a"))
        assertEquals(true, bundle.integrityValid)
        assertEquals(64, bundle.bundleSeal.length) // SHA-256 esadecimale
    }
}
