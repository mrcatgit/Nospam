package com.nospam.data

import android.content.Context

/**
 * Impostazioni dell'utente, su SharedPreferences. Lette al volo (anche dal servizio di
 * screening) e modificabili dalla schermata Impostazioni.
 */
class AppSettings(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("nospam_settings", Context.MODE_PRIVATE)

    /** Se true, mostra un avviso per OGNI chiamata (incluse quelle consentite). */
    var alertAllCalls: Boolean
        get() = prefs.getBoolean(KEY_ALERT_ALL, true)
        set(value) = prefs.edit().putBoolean(KEY_ALERT_ALL, value).apply()

    /**
     * Se true, l'avviso per le chiamate consentite compare SOLO per numeri non in rubrica
     * (i contatti noti non generano avviso). Non incide su bloccate/sospette.
     */
    var alertOnlyUnknown: Boolean
        get() = prefs.getBoolean(KEY_ONLY_UNKNOWN, false)
        set(value) = prefs.edit().putBoolean(KEY_ONLY_UNKNOWN, value).apply()

    /**
     * Se true (default), mostra un popup grande sopra la chiamata invece della notifica,
     * quando il permesso "Mostra sopra altre app" è concesso. Senza permesso si usa
     * comunque la notifica.
     */
    var useOverlay: Boolean
        get() = prefs.getBoolean(KEY_USE_OVERLAY, true)
        set(value) = prefs.edit().putBoolean(KEY_USE_OVERLAY, value).apply()

    companion object {
        private const val KEY_ALERT_ALL = "alert_all_calls"
        private const val KEY_ONLY_UNKNOWN = "alert_only_unknown"
        private const val KEY_USE_OVERLAY = "use_overlay"
    }
}
