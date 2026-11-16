package com.nospam.screening

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.nospam.NoSpamApp
import com.nospam.R
import com.nospam.data.model.SpamCategory
import com.nospam.data.model.Verdict
import com.nospam.recording.CallRecordingService
import com.nospam.ui.MainActivity
import com.nospam.ui.WebLookup
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Popup grande sopra la schermata di chiamata (richiede il permesso "Mostra sopra altre
 * app"). Mostra numero, rischio e motivazioni, con i pulsanti Registra/Ferma,
 * Rispondi/Riaggancia e Chiudi.
 *
 * È best-effort e dipende dal dispositivo: se il permesso non c'è, non mostra nulla (lo
 * screening usa allora la notifica). I comandi di chiamata possono non funzionare su
 * tutti gli OEM.
 */
object CallOverlay {

    private val main = Handler(Looper.getMainLooper())
    private var view: View? = null
    private var recordButton: Button? = null
    private var stateJob: Job? = null
    private var autoDismiss: Runnable? = null

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    @SuppressLint("InflateParams")
    fun show(
        context: Context,
        number: String?,
        verdict: Verdict,
        riskScore: Int,
        category: SpamCategory,
        reasons: List<String>,
    ) {
        if (!canShow(context)) return
        main.post { showInternal(context.applicationContext, number, verdict, riskScore, category, reasons) }
    }

    private fun showInternal(
        appContext: Context,
        number: String?,
        verdict: Verdict,
        riskScore: Int,
        category: SpamCategory,
        reasons: List<String>,
    ) {
        removeInternal() // niente doppioni

        val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        val v = LayoutInflater.from(appContext).inflate(R.layout.overlay_call, null)

        val title = v.findViewById<TextView>(R.id.overlay_title)
        val numberView = v.findViewById<TextView>(R.id.overlay_number)
        val info = v.findViewById<TextView>(R.id.overlay_info)
        val record = v.findViewById<Button>(R.id.overlay_record)
        val answer = v.findViewById<Button>(R.id.overlay_answer)
        val hangup = v.findViewById<Button>(R.id.overlay_hangup)
        val search = v.findViewById<Button>(R.id.overlay_search)
        val close = v.findViewById<Button>(R.id.overlay_close)

        title.text = when (verdict) {
            Verdict.BLOCKED -> "⛔ Chiamata spam bloccata"
            Verdict.SILENCED -> "🔕 Chiamata sospetta"
            Verdict.ALLOWED -> "✅ Chiamata consentita"
        }
        numberView.text = number ?: "numero sconosciuto"
        info.text = "Rischio $riskScore/100 · ${category.label}\n" + reasons.joinToString(" · ")

        record.setOnClickListener { onRecordClicked(appContext, number) }
        answer.setOnClickListener { onCallControl(appContext, answer = true) }
        hangup.setOnClickListener { onCallControl(appContext, answer = false) }
        search.setOnClickListener { WebLookup.search(appContext, number) }
        close.setOnClickListener { removeInternal() }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP
            y = 80
        }

        try {
            wm.addView(v, params)
        } catch (e: Exception) {
            return
        }
        view = v
        recordButton = record
        observeRecordingState()

        // Auto-chiusura dopo 60s per non lasciarlo appeso.
        autoDismiss = Runnable { removeInternal() }.also { main.postDelayed(it, 60_000) }
    }

    private fun onCallControl(appContext: Context, answer: Boolean) {
        if (!CallController.canControlCalls(appContext)) {
            toast(appContext, "Serve il permesso telefono: attivalo in NoSpam → Impostazioni")
            return
        }
        val ok = if (answer) CallController.answer(appContext) else CallController.hangup(appContext)
        if (ok) {
            if (!answer) removeInternal()
        } else {
            toast(appContext, "Il tuo telefono non consente questo comando a NoSpam")
        }
    }

    private fun toast(context: Context, msg: String) {
        main.post { Toast.makeText(context, msg, Toast.LENGTH_LONG).show() }
    }

    private fun onRecordClicked(appContext: Context, number: String?) {
        if (CallRecordingService.isRecording.value) {
            CallRecordingService.stop(appContext)
        } else {
            // Con permesso overlay l'avvio in background è generalmente consentito; se il
            // dispositivo lo nega, apriamo l'app in primo piano per la conferma/avvio.
            try {
                CallRecordingService.start(appContext, number)
            } catch (e: Exception) {
                val i = Intent(appContext, MainActivity::class.java).apply {
                    putExtra(MainActivity.EXTRA_START_RECORDING, true)
                    putExtra(MainActivity.EXTRA_NUMBER, number)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                appContext.startActivity(i)
            }
        }
    }

    private fun observeRecordingState() {
        stateJob?.cancel()
        stateJob = NoSpamApp.instance.appScope.launch {
            CallRecordingService.isRecording.collect { recording ->
                main.post {
                    recordButton?.text = if (recording) "⏹ Ferma registrazione" else "● Registra"
                }
            }
        }
    }

    private fun removeInternal() {
        autoDismiss?.let { main.removeCallbacks(it) }
        autoDismiss = null
        stateJob?.cancel()
        stateJob = null
        val v = view ?: return
        view = null
        recordButton = null
        val wm = v.context.applicationContext
            .getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        runCatching { wm?.removeView(v) }
    }
}
