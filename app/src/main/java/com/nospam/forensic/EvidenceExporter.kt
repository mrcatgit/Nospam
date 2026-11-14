package com.nospam.forensic

import com.nospam.data.model.CallRecord
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Esporta il registro come fascicolo JSON firmato, adatto all'allegazione a una
 * denuncia. Include il risultato della verifica di integrità e un "manifest" con gli
 * hash, così che chi riceve il file possa ri-verificarlo in autonomia.
 *
 * Nota: l'export è un'istantanea dei METADATI osservati dal dispositivo dell'utente.
 * Non contiene contenuti di conversazioni (che l'app non raccoglie).
 */
object EvidenceExporter {

    private val json = Json { prettyPrint = true; encodeDefaults = true }

    @Serializable
    data class ExportedEvent(
        val timestampIso: String,
        val rawNumber: String,
        val e164Number: String?,
        val regionOrCarrier: String?,
        val verdict: String,
        val category: String,
        val riskScore: Int,
        val reasons: List<String>,
        val knownContact: Boolean,
        val userNote: String?,
        val prevHash: String,
        val hash: String,
    )

    @Serializable
    data class EvidenceBundle(
        val tool: String = "NoSpam",
        val formatVersion: Int = 1,
        val generatedAtIso: String,
        val deviceTimeZone: String,
        val integrityValid: Boolean,
        val integrityCheckedRecords: Int,
        val integrityBrokenAtId: Long?,
        val eventCount: Int,
        val events: List<ExportedEvent>,
        /** Hash SHA-256 dell'intero elenco eventi serializzato (sigillo del fascicolo). */
        val bundleSeal: String,
    )

    private val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ITALY).apply {
        timeZone = TimeZone.getDefault()
    }

    fun buildBundle(records: List<CallRecord>): EvidenceBundle {
        val chain = EvidenceHasher.verifyChain(records)
        val events = records.map {
            ExportedEvent(
                timestampIso = iso.format(Date(it.timestamp)),
                rawNumber = it.rawNumber,
                e164Number = it.e164Number,
                regionOrCarrier = it.regionOrCarrier,
                verdict = it.verdict.name,
                category = it.category.label,
                riskScore = it.riskScore,
                reasons = it.reasons,
                knownContact = it.knownContact,
                userNote = it.userNote,
                prevHash = it.prevHash,
                hash = it.hash,
            )
        }
        val eventsJson = json.encodeToString(events)
        val seal = sha256(eventsJson)
        return EvidenceBundle(
            generatedAtIso = iso.format(Date()),
            deviceTimeZone = TimeZone.getDefault().id,
            integrityValid = chain.valid,
            integrityCheckedRecords = chain.checked,
            integrityBrokenAtId = chain.brokenAtId,
            eventCount = events.size,
            events = events,
            bundleSeal = seal,
        )
    }

    fun toJson(bundle: EvidenceBundle): String = json.encodeToString(bundle)

    private fun sha256(input: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        return md.digest(input.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}
