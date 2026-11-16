package com.nospam.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nospam.report.ComplaintPdfGenerator
import java.io.File
import java.util.UUID

/**
 * Prepara e condivide il fascicolo probatorio (esposto .txt + PDF, bundle JSON firmato,
 * file audio) tramite il selettore di sistema, usando FileProvider.
 *
 * Ogni export viene scritto in una cartella UNICA: così un URI già concesso in una
 * condivisione precedente continua a puntare al suo contenuto originale e non viene
 * sovrascritto da un export successivo.
 */
object EvidenceShare {

    private const val AUTHORITY = "com.nospam.fileprovider"

    /**
     * Scrive i file del fascicolo in una cartella unica e restituisce gli URI da
     * condividere. Operazione di I/O (scrive file e rende il PDF): chiamare su
     * Dispatchers.IO, non sul thread principale.
     */
    fun buildShareUris(
        context: Context,
        esposto: String,
        evidenceJson: String,
        audioPaths: List<String>,
    ): ArrayList<Uri> {
        val dir = File(
            context.cacheDir,
            "export/${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}",
        ).apply { mkdirs() }

        val txt = File(dir, "esposto_nospam.txt").apply { writeText(esposto) }
        val json = File(dir, "evidence_bundle.json").apply { writeText(evidenceJson) }
        val pdf = runCatching { ComplaintPdfGenerator().render(esposto, dir) }.getOrNull()

        val uris = ArrayList<Uri>()
        uris += FileProvider.getUriForFile(context, AUTHORITY, txt)
        if (pdf != null) uris += FileProvider.getUriForFile(context, AUTHORITY, pdf)
        uris += FileProvider.getUriForFile(context, AUTHORITY, json)
        audioPaths.forEach { path ->
            val f = File(path)
            if (f.exists()) uris += FileProvider.getUriForFile(context, AUTHORITY, f)
        }
        return uris
    }

    /** Avvia il selettore di condivisione. Solo UI: chiamare sul thread principale. */
    fun launchChooser(context: Context, uris: ArrayList<Uri>) {
        if (uris.isEmpty()) return
        val send = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, "Segnalazione chiamate spam/truffa - NoSpam")
            putExtra(
                Intent.EXTRA_TEXT,
                "In allegato: esposto, registro eventi firmato (JSON) e file audio sigillati " +
                    "con SHA-256. Generato con NoSpam.",
            )
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Condividi fascicolo")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Condivide un singolo file audio di registrazione. */
    fun shareAudio(context: Context, path: String) {
        val f = File(path)
        if (!f.exists()) return
        val uri = FileProvider.getUriForFile(context, AUTHORITY, f)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(send, "Condividi registrazione")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
