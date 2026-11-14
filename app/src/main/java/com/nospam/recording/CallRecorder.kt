package com.nospam.recording

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import com.nospam.forensic.FileHasher
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registratore audio basato su microfono.
 *
 * ⚠️ Perché il microfono e non il "flusso chiamata": da Android 10 le app di terze
 * parti non possono accedere all'audio della telefonata (AudioSource.VOICE_CALL è
 * riservato alle app di sistema). Captiamo quindi acusticamente dal microfono: con il
 * VIVAVOCE attivo si registrano entrambe le voci. È legale (l'utente è parte della
 * conversazione) e non intercetta comunicazioni di terzi.
 *
 * Produce un file AAC/M4A nello storage privato e ne calcola lo SHA-256 alla chiusura.
 */
class CallRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var startedAt: Long = 0

    val isRecording: Boolean get() = recorder != null

    data class Result(val file: File, val sha256: String, val startedAt: Long, val durationMs: Long)

    private fun outputDir(): File =
        File(context.filesDir, "recordings").apply { if (!exists()) mkdirs() }

    fun start(): Boolean {
        if (isRecording) return false
        val name = "rec_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ITALY).format(Date()) + ".m4a"
        val file = File(outputDir(), name)

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()

        return try {
            rec.apply {
                // MIC: cattura acustica (usare in vivavoce). VOICE_CALL non è accessibile.
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128_000)
                setAudioSamplingRate(44_100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = rec
            currentFile = file
            startedAt = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            runCatching { rec.release() }
            file.delete()
            false
        }
    }

    /** Ferma la registrazione e restituisce il file sigillato, o null in caso di errore. */
    fun stop(): Result? {
        val rec = recorder ?: return null
        val file = currentFile ?: return null
        return try {
            rec.stop()
            rec.release()
            val duration = System.currentTimeMillis() - startedAt
            val hash = FileHasher.sha256(file)
            Result(file, hash, startedAt, duration)
        } catch (e: Exception) {
            // stop() può fallire se la registrazione è troppo breve: scarta il file.
            runCatching { rec.release() }
            file.delete()
            null
        } finally {
            recorder = null
            currentFile = null
            startedAt = 0
        }
    }
}
