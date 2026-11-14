package com.nospam.report

import com.nospam.data.model.CallRecord
import com.nospam.data.model.Verdict
import com.nospam.forensic.EvidenceHasher
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Genera il testo di un fascicolo di segnalazione/denuncia in italiano, pronto da
 * consegnare alla Polizia Postale (anche tramite il portale www.commissariatodips.it)
 * e/o all'AGCOM per le violazioni sul telemarketing.
 *
 * Il modello NON inventa dati: compila ciò che è stato realmente osservato dal
 * dispositivo e lascia tra parentesi quadre i campi che solo l'utente può fornire.
 */
class ComplaintReport(
    private val denuncianteNome: String = "[NOME E COGNOME]",
    private val denuncianteCf: String = "[CODICE FISCALE]",
    private val denuncianteContatto: String = "[EMAIL / TELEFONO DI CONTATTO]",
    private val numeroVittima: String = "[TUO NUMERO DI TELEFONO]",
) {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.ITALY)

    fun build(records: List<CallRecord>): String {
        val chain = EvidenceHasher.verifyChain(records)
        val relevant = records.filter { it.category != com.nospam.data.model.SpamCategory.LEGITTIMO }
        val blocked = records.count { it.verdict == Verdict.BLOCKED }
        val first = records.minByOrNull { it.timestamp }?.timestamp
        val last = records.maxByOrNull { it.timestamp }?.timestamp

        val sb = StringBuilder()
        sb.appendLine("ESPOSTO / SEGNALAZIONE PER MOLESTIE E TENTATIVI DI TRUFFA TELEFONICA")
        sb.appendLine("=".repeat(70))
        sb.appendLine()
        sb.appendLine("Generato con l'app NoSpam il ${dateFmt.format(Date())}")
        sb.appendLine()
        sb.appendLine("1. DATI DEL SEGNALANTE")
        sb.appendLine("   Nome e cognome: $denuncianteNome")
        sb.appendLine("   Codice fiscale: $denuncianteCf")
        sb.appendLine("   Recapito: $denuncianteContatto")
        sb.appendLine("   Numero di utenza oggetto delle chiamate: $numeroVittima")
        sb.appendLine()
        sb.appendLine("2. OGGETTO")
        sb.appendLine("   Il/la sottoscritto/a segnala di essere destinatario/a di ripetute")
        sb.appendLine("   chiamate telefoniche indesiderate e di tentativi di truffa, come")
        sb.appendLine("   documentato nel registro allegato, raccolto automaticamente dal")
        sb.appendLine("   proprio dispositivo mobile.")
        sb.appendLine()
        sb.appendLine("3. SINTESI DEL PERIODO")
        sb.appendLine("   Eventi registrati: ${records.size}")
        sb.appendLine("   Di cui classificati come sospetti/molesti: ${relevant.size}")
        sb.appendLine("   Chiamate bloccate automaticamente: $blocked")
        if (first != null) sb.appendLine("   Prima chiamata: ${dateFmt.format(Date(first))}")
        if (last != null) sb.appendLine("   Ultima chiamata: ${dateFmt.format(Date(last))}")
        sb.appendLine()
        sb.appendLine("4. NUMERI RICORRENTI")
        relevant.groupBy { it.e164Number ?: it.rawNumber }
            .mapValues { it.value.size }
            .entries.sortedByDescending { it.value }
            .take(15)
            .forEach { (num, count) ->
                val cat = relevant.firstOrNull { (it.e164Number ?: it.rawNumber) == num }?.category?.label
                sb.appendLine("   - $num  ($count chiamate)  tipologia: ${cat ?: "n/d"}")
            }
        sb.appendLine()
        sb.appendLine("5. DETTAGLIO CRONOLOGICO (metadati)")
        sb.appendLine("   Nota: NoSpam registra esclusivamente metadati (numero, data/ora,")
        sb.appendLine("   esito, indizi di rischio). NON registra il contenuto delle")
        sb.appendLine("   conversazioni.")
        sb.appendLine()
        records.sortedBy { it.timestamp }.forEach { r ->
            sb.appendLine("   [${dateFmt.format(Date(r.timestamp))}] ${r.e164Number ?: r.rawNumber}")
            sb.appendLine("       esito=${r.verdict}  rischio=${r.riskScore}/100  tipologia=${r.category.label}")
            if (r.regionOrCarrier != null) sb.appendLine("       area/operatore=${r.regionOrCarrier}")
            sb.appendLine("       indizi: ${r.reasons.joinToString("; ")}")
            if (!r.userNote.isNullOrBlank()) sb.appendLine("       nota utente: ${r.userNote}")
            sb.appendLine("       hash=${r.hash}")
        }
        sb.appendLine()
        sb.appendLine("6. INTEGRITÀ DELLE PROVE")
        sb.appendLine("   Il registro è protetto da una catena di hash SHA-256 (ogni record")
        sb.appendLine("   incorpora l'hash del precedente). Verifica automatica:")
        sb.appendLine("   - Integro: ${if (chain.valid) "SÌ" else "NO"}")
        sb.appendLine("   - Record verificati: ${chain.checked}")
        if (!chain.valid) sb.appendLine("   - Prima incongruenza al record id=${chain.brokenAtId}")
        sb.appendLine("   Il fascicolo JSON allegato consente a terzi di ri-verificare gli hash.")
        sb.appendLine()
        sb.appendLine("7. RICHIESTA")
        sb.appendLine("   Si chiede all'Autorità di voler svolgere gli accertamenti di")
        sb.appendLine("   competenza, inclusa — ove ne ricorrano i presupposti — l'acquisizione")
        sb.appendLine("   dei dati di traffico presso i gestori telefonici al fine di")
        sb.appendLine("   identificare i responsabili, e di perseguire le condotte illecite.")
        sb.appendLine()
        sb.appendLine("   Luogo e data: [LUOGO], ${SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date())}")
        sb.appendLine("   Firma: _______________________")
        sb.appendLine()
        sb.appendLine("Allegati: registro eventi in formato JSON firmato (NoSpam evidence bundle).")
        return sb.toString()
    }
}
