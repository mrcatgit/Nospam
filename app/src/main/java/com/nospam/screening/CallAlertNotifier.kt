package com.nospam.screening

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nospam.data.AppSettings
import com.nospam.data.model.SpamCategory
import com.nospam.data.model.Verdict
import com.nospam.ui.MainActivity

/**
 * Avvisa l'utente, al momento della chiamata, dell'esito dello screening tramite una
 * notifica heads-up (compare sopra la schermata di chiamata).
 *
 * Perché una notifica e non un'etichetta sulla schermata di sistema: Android non
 * consente a un'app di terze parti di sovrascrivere l'interfaccia di chiamata senza il
 * permesso speciale "Mostra sopra altre app" (SYSTEM_ALERT_WINDOW). La notifica
 * heads-up su un canale ad alta priorità è il modo standard e meno invasivo per
 * mostrare il verdetto in tempo reale.
 */
object CallAlertNotifier {

    private const val CHANNEL_ID = "nospam_call_alert"
    private const val NOTIF_ID = 88

    fun notify(
        context: Context,
        number: String?,
        verdict: Verdict,
        riskScore: Int,
        category: SpamCategory,
        knownContact: Boolean,
    ) {
        // Bloccate e sospette avvisano sempre. Le consentite seguono le impostazioni:
        // avviso per tutte le chiamate (default), opzionalmente solo per numeri sconosciuti.
        if (verdict == Verdict.ALLOWED) {
            val settings = AppSettings(context)
            if (!settings.alertAllCalls) return
            if (settings.alertOnlyUnknown && knownContact) return
        }

        createChannel(context)
        val label = number ?: "numero sconosciuto"
        val detail = "$label · rischio $riskScore/100 · ${category.label}"
        val title = when (verdict) {
            Verdict.BLOCKED -> "⛔ Chiamata spam bloccata"
            Verdict.SILENCED -> "🔕 Chiamata sospetta silenziata"
            Verdict.ALLOWED -> "✅ Chiamata consentita"
        }

        val shield = "⚠️ Non dare MAI OTP, PIN, SPID, password o dati della carta a chi " +
            "chiama. Nessuna banca o ente li chiede al telefono."
        val builder = android.app.Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(detail)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(openApp(context, number, startRecording = false, requestCode = 0))
            .setAutoCancel(true)

        // Scudo anti-truffa nella notifica espansa, per le chiamate non legittime.
        if (verdict != Verdict.ALLOWED || riskScore >= 40) {
            builder.setStyle(
                android.app.Notification.BigTextStyle().bigText("$detail\n\n$shield")
            )
        }

        // Per le chiamate non bloccate offri anche di registrare (apre l'app in primo
        // piano, poi avvia: l'FGS microfono non può partire dal background su Android 14).
        if (verdict != Verdict.BLOCKED) {
            builder.addAction(
                android.app.Notification.Action.Builder(
                    null, "Registra",
                    openApp(context, number, startRecording = true, requestCode = 1),
                ).build()
            )
        }

        context.getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, builder.build())
    }

    private fun openApp(
        context: Context,
        number: String?,
        startRecording: Boolean,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            if (startRecording) putExtra(MainActivity.EXTRA_START_RECORDING, true)
            putExtra(MainActivity.EXTRA_NUMBER, number)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(context, requestCode, intent, flags)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Avvisi chiamate",
                NotificationManager.IMPORTANCE_HIGH, // heads-up sopra la chiamata
            ).apply { description = "Mostra l'esito dello screening al momento della chiamata." }
            mgr.createNotificationChannel(channel)
        }
    }
}
