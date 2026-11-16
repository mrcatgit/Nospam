package com.nospam.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import com.nospam.NoSpamApp
import com.nospam.data.model.CallRecord
import com.nospam.data.model.Verdict
import com.nospam.detection.ContactLookup
import com.nospam.forensic.EvidenceHasher
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

    override fun onScreenCall(callDetails: Call.Details) {
        val raw = callDetails.handle?.schemeSpecificPart
        val app = applicationContext as NoSpamApp

        // Persistiamo sullo scope a vita-applicazione: Telecom può slegare e distruggere
        // il service subito dopo respondToCall(), ma l'inserimento forense deve completare.
        app.appScope.launch {
            // Consulta la rubrica (se READ_CONTACTS è concesso): un contatto noto non
            // deve essere bloccato/silenziato. Senza permesso, isKnown restituisce false.
            val knownContact = ContactLookup.isKnown(applicationContext, raw)
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

            // Avvisa l'utente dell'esito al momento della chiamata (notifica heads-up):
            // spam bloccato, sospetta silenziata, o possibile spam che è passato. Per le
            // non bloccate la notifica offre anche di registrare.
            CallAlertNotifier.notify(
                applicationContext,
                decision.parsed.e164 ?: raw,
                decision.verdict,
                decision.riskScore,
                decision.category,
            )

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
