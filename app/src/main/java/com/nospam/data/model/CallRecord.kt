package com.nospam.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Un evento di chiamata osservato dallo screening.
 *
 * I campi [prevHash] e [hash] formano una catena di custodia: ogni record incorpora
 * l'hash del precedente, così qualsiasi modifica retroattiva al registro diventa
 * rilevabile (vedi [com.nospam.forensic.EvidenceHasher]). Questo rende il registro
 * utilizzabile come allegato credibile a una segnalazione/denuncia.
 */
@Entity(tableName = "call_records")
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    /** Numero grezzo come fornito dal sistema. */
    val rawNumber: String,
    /** Numero normalizzato in E.164 quando possibile (es. +39...). */
    val e164Number: String?,
    /** Prefisso nazionale/geografico o indicazione operatore, se deducibile. */
    val regionOrCarrier: String?,

    /** Epoch millis dell'evento. */
    val timestamp: Long,
    /** Esito dello screening. */
    val verdict: Verdict,
    /** Categoria sospetta assegnata dal motore euristico. */
    val category: SpamCategory,
    /** Punteggio di rischio 0..100 prodotto dal motore. */
    val riskScore: Int,
    /** Motivazioni leggibili (quali regole hanno contribuito). */
    val reasons: List<String>,

    /** true se il numero era presente nei contatti dell'utente. */
    val knownContact: Boolean,
    /** Nota libera aggiunta dall'utente (es. "si è spacciato per banca X"). */
    val userNote: String? = null,

    // --- catena di custodia ---
    val prevHash: String,
    val hash: String,
)

enum class Verdict { ALLOWED, SILENCED, BLOCKED }
