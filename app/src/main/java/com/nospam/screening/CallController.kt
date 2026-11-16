package com.nospam.screening

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telecom.TelecomManager
import androidx.core.content.ContextCompat

/**
 * Comandi sulla chiamata (rispondi / riaggancia) tramite TelecomManager.
 *
 * Richiede il permesso ANSWER_PHONE_CALLS. La riuscita **dipende dal dispositivo e dallo
 * stato della chiamata**: su alcuni OEM questi comandi possono non avere effetto. Ogni
 * metodo restituisce true solo se l'azione è stata invocata senza eccezioni.
 */
object CallController {

    fun canControlCalls(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ANSWER_PHONE_CALLS) ==
            PackageManager.PERMISSION_GRANTED

    /** Accetta la chiamata in arrivo. */
    fun answer(context: Context): Boolean {
        if (!canControlCalls(context)) return false
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                tm.acceptRingingCall(); true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    /** Termina la chiamata in arrivo o in corso. */
    fun hangup(context: Context): Boolean {
        if (!canControlCalls(context)) return false
        val tm = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) tm.endCall() else false
        } catch (e: Exception) {
            false
        }
    }
}
