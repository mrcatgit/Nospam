package com.nospam.ui

import android.app.role.RoleManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.nospam.NoSpamApp

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestCallScreeningRole()
        setContent {
            MaterialTheme {
                Surface { ConsoleScreen(vm) }
            }
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
}
