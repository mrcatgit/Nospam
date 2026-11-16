package com.nospam.recording

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.nospam.NoSpamApp
import com.nospam.data.model.CallRecording
import com.nospam.forensic.FileHasher
import com.nospam.ui.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Foreground service che mantiene attiva la registrazione mentre l'utente è in chiamata.
 * Avviato e fermato ESPLICITAMENTE dall'utente; una notifica persistente con azione
 * "Ferma" segnala sempre che la registrazione è in corso.
 *
 * `onStartCommand` è serializzato sul thread principale, quindi START/STOP sono ordinati.
 * I metadati vengono catturati in modo immutabile prima del salvataggio asincrono, e la
 * persistenza gira sullo scope a vita-applicazione: così un'operazione successiva non può
 * annullare un salvataggio in corso, e lo spegnimento del service non lo interrompe.
 */
class CallRecordingService : Service() {

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
                // startForeground sempre (anche sul duplicato) per onorare l'obbligo FGS.
                startForegroundCompat()
                if (recorder.isRecording) {
                    // Duplicato: una registrazione è già in corso, non fare nulla.
                } else {
                    number = intent.getStringExtra(EXTRA_NUMBER)
                    if (recorder.start()) {
                        _isRecording.value = true
                    } else {
                        stopForegroundAndSelf()
                    }
                }
            }
            ACTION_STOP -> finishAndSave()
        }
        return START_NOT_STICKY
    }

    private fun finishAndSave() {
        val stopped = recorder.stop()
        _isRecording.value = false
        if (stopped == null) {
            stopForegroundAndSelf()
            return
        }
        // Snapshot immutabile dei metadati prima del salvataggio asincrono.
        val capturedNumber = number
        val app = applicationContext as NoSpamApp
        app.appScope.launch {
            val sha = FileHasher.sha256(stopped.file) // intero file: fuori dal main thread
            app.repository.addRecording(
                CallRecording(
                    number = capturedNumber ?: "sconosciuto",
                    e164Number = capturedNumber,
                    startedAt = stopped.startedAt,
                    durationMs = stopped.durationMs,
                    filePath = stopped.file.absolutePath,
                    fileSha256 = sha,
                )
            )
        }
        stopForegroundAndSelf()
    }

    private fun stopForegroundAndSelf() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        _isRecording.value = false
        super.onDestroy()
    }

    private fun startForegroundCompat() {
        val stopPending = PendingIntent.getService(
            this, 2,
            Intent(this, CallRecordingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val openApp = PendingIntent.getActivity(
            this, 3,
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("NoSpam · registrazione in corso")
            .setContentText("Attiva il vivavoce. Tocca \"Ferma\" per terminare.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openApp)
            .addAction(Notification.Action.Builder(null, "Ferma", stopPending).build())
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
            } else {
                startForeground(NOTIF_ID, notification)
            }
        } catch (e: Exception) {
            // Es. ForegroundServiceStartNotAllowedException quando l'avvio parte dal
            // background su Android 14 senza i requisiti: non andare in crash, termina.
            _isRecording.value = false
            stopSelf()
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

        /** Stato di registrazione autorevole, osservabile dalla UI. */
        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

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
