package com.nospam.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.nospam.report.ComplaintPdfGenerator
import java.io.File

/**
 * Prepara e condivide il fascicolo probatorio verso un'app a scelta dell'utente
 * (email, messaggistica, salvataggio su file) tramite il selettore di sistema.
 *
 * Mette insieme:
 *   - esposto in italiano (.txt);
 *   - evidence bundle firmato (.json);
 *   - i file audio sigillati delle registrazioni.
 *
 * Usa FileProvider: nessun file privato viene esposto se non tramite URI temporanei
 * con permesso di sola lettura concesso all'app ricevente.
 */
object EvidenceShare {

    private const val AUTHORITY = "com.nospam.fileprovider"

    fun share(
        context: Context,
        esposto: String,
        evidenceJson: String,
        audioPaths: List<String>,
    ) {
        val dir = File(context.cacheDir, "export").apply { if (!exists()) mkdirs() }
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
}
