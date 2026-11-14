package com.nospam.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.nospam.NoSpamApp
import com.nospam.data.model.CallRecord
import com.nospam.data.model.Verdict
import com.nospam.forensic.EvidenceHasher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servizio ufficiale di screening di Android. Il sistema lo chiama per OGNI chiamata
 * in arrivo quando NoSpam è l'app predefinita per ID chiamante & spam.
 *
 * Qui decidiamo, sui soli metadati, se:
 *   - lasciar squillare (ALLOWED),
 *   - silenziare (SILENCED → niente suoneria, resta nel log),
 *   - rifiutare (BLOCKED → la chiamata non raggiunge l'utente).
 *
 * Non viene mai aperto l'audio: `CallScreeningService` opera prima della risposta e
 * non espone il contenuto della conversazione. Ogni decisione viene registrata nel
 * registro forense a prova di manomissione.
 */
class NoSpamScreeningService : CallScreeningService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val raw = callDetails.handle?.schemeSpecificPart
        val app = applicationContext as NoSpamApp

        scope.launch {
            // La rubrica viene consultata altrove (ContactLookup) se il permesso è dato;
            // qui manteniamo il flusso semplice e privo di dipendenze dal contesto UI.
            val knownContact = false
            val decision = app.detector.evaluate(raw, knownContact)

            val response = CallResponse.Builder().apply {
                when (decision.verdict) {
                    Verdict.BLOCKED -> {
                        setDisallowCall(true)
                        setRejectCall(true)
                        setSkipCallLog(false)      // teniamo traccia a sistema
                        setSkipNotification(true)
                    }
                    Verdict.SILENCED -> {
                        setSilenceCall(true)       // niente suoneria, la chiamata resta
                    }
                    Verdict.ALLOWED -> { /* nessuna restrizione */ }
                }
            }.build()

            respondToCall(callDetails, response)

            // Registrazione nel registro forense (metadati, non contenuto)
            app.repository.recordCall(
                CallRecord(
                    rawNumber = raw ?: "",
                    e164Number = decision.parsed.e164,
                    regionOrCarrier = decision.parsed.description ?: decision.parsed.region,
                    timestamp = System.currentTimeMillis(),
                    verdict = decision.verdict,
                    category = decision.category,
                    riskScore = decision.riskScore,
                    reasons = decision.reasons,
                    knownContact = knownContact,
                    prevHash = EvidenceHasher.GENESIS, // ricalcolato dal repository
                    hash = "",                          // ricalcolato dal repository
                )
            )
        }
    }
}
