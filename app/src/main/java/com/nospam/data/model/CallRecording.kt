package com.nospam.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Registrazione audio di una chiamata a cui l'utente ha partecipato, avviata
 * manualmente dall'utente. Captata dal microfono (vivavoce), NON dal flusso della
 * telefonata: l'app non ha né chiede accesso all'audio di rete.
 *
 * Il campo [fileSha256] sigilla il file audio: consente di dimostrare che la
 * registrazione allegata a una denuncia non è stata alterata dopo l'acquisizione.
 */
@Entity(tableName = "call_recordings")
data class CallRecording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val e164Number: String?,
    val startedAt: Long,
    val durationMs: Long,
    /** Percorso del file audio nello storage privato dell'app. */
    val filePath: String,
    /** SHA-256 del file audio al termine della registrazione. */
    val fileSha256: String,
    val note: String? = null,
    /** id dell'eventuale CallRecord collegato. */
    val linkedCallRecordId: Long? = null,
)
