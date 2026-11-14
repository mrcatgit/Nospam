package com.nospam.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.nospam.NoSpamApp
import com.nospam.data.model.CallRecording
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Foreground service che mantiene attiva la registrazione mentre l'utente è in chiamata.
 * È avviato e fermato ESPLICITAMENTE dall'utente (nessun avvio automatico nascosto):
 * una notifica persistente segnala sempre che la registrazione è in corso.
 *
 * Avvio:  Intent action ACTION_START con extra EXTRA_NUMBER (opzionale)
 * Stop:   Intent action ACTION_STOP
 */
class CallRecordingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var recorder: CallRecorder
    private var number: String? = null

    override fun onCreate() {
        super.onCreate()
        recorder = CallRecorder(applicationContext)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                number = intent.getStringExtra(EXTRA_NUMBER)
                startForegroundCompat()
                if (!recorder.start()) {
                    stopSelf()
                }
            }
            ACTION_STOP -> finishAndSave()
        }
        return START_NOT_STICKY
    }

    private fun finishAndSave() {
        val result = recorder.stop()
        if (result != null) {
            val app = applicationContext as NoSpamApp
            scope.launch {
                app.repository.addRecording(
                    CallRecording(
                        number = number ?: "sconosciuto",
                        e164Number = number,
                        startedAt = result.startedAt,
                        durationMs = result.durationMs,
                        filePath = result.file.absolutePath,
                        fileSha256 = result.sha256,
                    )
                )
            }
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundCompat() {
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NoSpam · registrazione in corso")
            .setContentText("Attiva il vivavoce per registrare entrambe le voci. Tocca per fermare.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = getSystemService(NotificationManager::class.java)
            mgr.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Registrazione chiamate", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.nospam.recording.START"
        const val ACTION_STOP = "com.nospam.recording.STOP"
        const val EXTRA_NUMBER = "number"
        private const val CHANNEL_ID = "nospam_recording"
        private const val NOTIF_ID = 42

        fun start(context: Context, number: String?) {
            val i = Intent(context, CallRecordingService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_NUMBER, number)
            }
            context.startForegroundService(i)
        }

        fun stop(context: Context) {
            val i = Intent(context, CallRecordingService::class.java).apply { action = ACTION_STOP }
            context.startService(i)
        }
    }
}
