package com.nospam.ui

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.nospam.NoSpamApp
import com.nospam.recording.CallRecordingService

/**
 * Entry point. Richiede il ruolo di sistema "app ID chiamante & spam"
 * (RoleManager.ROLE_CALL_SCREENING), che è il meccanismo ufficiale per attivare lo
 * screening senza permessi invasivi. Poi mostra la console Compose.
 */
class MainActivity : ComponentActivity() {

    private val vm: ConsoleViewModel by viewModels {
        ConsoleViewModel.Factory((application as NoSpamApp).repository)
    }

    private val roleLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { /* esito */ }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { /* esito */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCallScreeningRole()
        requestRuntimePermissions()
        setContent {
            MaterialTheme {
                Surface { NavRoot(vm) }
            }
        }
        maybeStartRecording(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        maybeStartRecording(intent)
    }

    /**
     * Avvia la registrazione quando l'app è aperta dall'azione "Registra" della notifica.
     * L'avvio avviene qui, con l'Activity in primo piano, così il foreground service di
     * tipo microfono è consentito anche su Android 14. Richiede il permesso microfono.
     */
    private fun maybeStartRecording(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_START_RECORDING, false) != true) return
        intent.removeExtra(EXTRA_START_RECORDING)
        val number = intent.getStringExtra(EXTRA_NUMBER)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            CallRecordingService.start(this, number)
        } else {
            // Senza permesso non si può registrare: chiedilo (l'utente potrà riprovare).
            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = getSystemService(Context.ROLE_SERVICE) as RoleManager
            if (rm.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) &&
                !rm.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)
            ) {
                roleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
            }
        }
    }

    /** Chiede i permessi runtime necessari alla registrazione (microfono + notifiche). */
    private fun requestRuntimePermissions() {
        val needed = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
    }

    companion object {
        const val EXTRA_START_RECORDING = "com.nospam.START_RECORDING"
        const val EXTRA_NUMBER = "com.nospam.NUMBER"
    }
}
