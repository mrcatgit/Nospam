package com.nospam.recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Propone all'utente di registrare una chiamata sospetta tramite una notifica con
 * azione. L'avvio della registrazione resta un gesto ESPLICITO dell'utente (tocco sulla
 * notifica): nessuna registrazione parte da sola.
 *
 * Viene invocata dallo screening quando una chiamata non bloccata risulta a rischio.
 */
object RecordingNotifier {

    private const val CHANNEL_ID = "nospam_offer_recording"
    private const val NOTIF_ID = 77

    fun offer(context: Context, number: String?) {
        createChannel(context)

        val startIntent = Intent(context, CallRecordingService::class.java).apply {
            action = CallRecordingService.ACTION_START
            putExtra(CallRecordingService.EXTRA_NUMBER, number)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getForegroundService(context, 0, startIntent, flags)

        val label = number ?: "numero sconosciuto"
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setContentTitle("Chiamata sospetta da $label")
            .setContentText("Attiva il vivavoce e tocca per registrare come prova.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .addAction(
                android.app.Notification.Action.Builder(
                    null, "Registra", pending,
                ).build()
            )
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIF_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Proposta di registrazione",
                    NotificationManager.IMPORTANCE_HIGH,
                )
            )
        }
    }
}
