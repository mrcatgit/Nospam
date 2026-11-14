package com.nospam.data.model

/**
 * Tassonomia dei tipi di molestia telefonica rilevante per la segnalazione.
 * Le etichette aiutano a compilare il campo "tipologia" del fascicolo denuncia.
 */
enum class SpamCategory(val label: String) {
    UNKNOWN("Non classificato"),
    TELEMARKETING("Telemarketing molesto"),
    ROBOCALL("Chiamata automatica / registrata"),
    SPOOFING("Numero contraffatto (spoofing)"),
    PHISHING_VOCALE("Vishing / phishing vocale"),
    FINTO_OPERATORE("Finto operatore (banca/energia/TLC)"),
    FINTE_AUTORITA("Finta autorità (forze dell'ordine, enti)"),
    SUPPORTO_TECNICO("Finto supporto tecnico"),
    WANGIRI("Wangiri (uno squillo / richiamo a pagamento)"),
    TRUFFA_INVESTIMENTI("Finti investimenti / cripto"),
    LEGITTIMO("Legittimo"),
}
