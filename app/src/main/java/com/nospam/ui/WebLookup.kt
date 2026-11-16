package com.nospam.ui

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Verifica del numero sul web in un tocco: apre il browser su una ricerca che fa
 * emergere le segnalazioni della community (tellows, forum "chi ha chiamato", ecc.).
 *
 * Non invia dati a NoSpam: è il browser dell'utente a fare la ricerca. Nessuna API,
 * nessuna chiave.
 */
object WebLookup {

    fun search(context: Context, number: String?) {
        val q = number?.trim().orEmpty().filter { it.isDigit() || it == '+' }
        if (q.isBlank()) return
        val url = "https://www.google.com/search?q=" + Uri.encode("\"$q\" chi chiama spam truffa")
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
