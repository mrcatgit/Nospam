package com.nospam.detection

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

/**
 * Verifica se un numero è presente nei contatti dell'utente, tramite PhoneLookup.
 *
 * Richiede il permesso READ_CONTACTS; se non concesso, la query lancia SecurityException
 * che qui viene intercettata restituendo false (nessun crash, nessuna allowlist). Così
 * la funzione è best-effort: se l'utente concede i contatti, i numeri noti vengono
 * riconosciuti e non bloccati; altrimenti il comportamento resta sicuro.
 */
object ContactLookup {

    fun isKnown(context: Context, number: String?): Boolean {
        if (number.isNullOrBlank()) return false
        return try {
            val uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number),
            )
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup._ID),
                null, null, null,
            )?.use { it.moveToFirst() } ?: false
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }
}
