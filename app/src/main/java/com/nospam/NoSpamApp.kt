package com.nospam

import android.app.Application
import com.nospam.data.db.NoSpamDatabase
import com.nospam.data.repo.CallRepository
import com.nospam.detection.SpamDetector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application singleton. Espone le dipendenze principali senza un framework DI,
 * per mantenere il progetto leggibile. In produzione sostituibile con Hilt.
 */
class NoSpamApp : Application() {

    val database: NoSpamDatabase by lazy { NoSpamDatabase.build(this) }

    val repository: CallRepository by lazy {
        CallRepository(
            database.callRecordDao(),
            database.blockRuleDao(),
            database.callRecordingDao(),
        )
    }

    val detector: SpamDetector by lazy { SpamDetector(repository) }

    /**
     * Scope a vita-applicazione per la persistenza che NON deve essere annullata dallo
     * spegnimento di un service (inserimento forense dopo lo screening, salvataggio
     * della registrazione). Vive quanto il processo.
     */
    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        lateinit var instance: NoSpamApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
