package com.nospam

import android.app.Application
import com.nospam.data.db.NoSpamDatabase
import com.nospam.data.repo.CallRepository
import com.nospam.detection.SpamDetector

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

    companion object {
        lateinit var instance: NoSpamApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
