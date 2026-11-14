package com.nospam.detection

import com.nospam.data.model.SpamCategory

/**
 * Regole euristiche indipendenti dal contenuto della chiamata. Ognuna osserva solo
 * metadati (numero, formato, frequenza) e restituisce un contributo al punteggio di
 * rischio più una possibile categoria. Il motore somma i contributi.
 *
 * Nessuna di queste regole "ascolta" la chiamata: operano prima della risposta.
 */
data class Signal(
    val points: Int,
    val reason: String,
    val category: SpamCategory = SpamCategory.UNKNOWN,
)

/** Contesto passato alle regole per una singola chiamata in arrivo. */
data class CallContext(
    val raw: String?,
    val parsed: NumberNormalizer.Parsed,
    val knownContact: Boolean,
    /** Quante volte questo numero ha chiamato nelle ultime 24h. */
    val recentCallCount: Int,
    /** Durata media (sec) delle chiamate precedenti da questo numero; 0 se N/D. */
    val avgPrevDurationSec: Int,
)

object HeuristicRules {

    /** Prefissi di numerazioni spesso usate per telemarketing/truffe in Italia. */
    private val SUSPICIOUS_PREFIXES = listOf(
        "+3906754", // esempi di range noti per call center massivi
        "+39894",   // numerazioni a sovrapprezzo
        "+39895",
        "+39899",
    )

    /** Prefissi internazionali tipici delle truffe "wangiri" (uno squillo). */
    private val WANGIRI_PREFIXES = listOf(
        "+216", "+225", "+375", "+371", "+676", "+888",
    )

    val all: List<(CallContext) -> Signal?> = listOf(
        // Numero anonimo/nascosto
        { ctx ->
            if (ctx.parsed.lineType == "WITHHELD")
                Signal(25, "Numero nascosto o anonimo", SpamCategory.UNKNOWN) else null
        },
        // Numero non valido / malformato → spesso spoofing
        { ctx ->
            if (!ctx.parsed.valid && ctx.raw != null && ctx.parsed.lineType != "WITHHELD")
                Signal(35, "Numero non valido secondo il piano di numerazione (possibile spoofing)",
                    SpamCategory.SPOOFING) else null
        },
        // Prefissi a sovrapprezzo / call center noti
        { ctx ->
            val e = ctx.parsed.e164
            if (e != null && SUSPICIOUS_PREFIXES.any { e.startsWith(it) })
                Signal(40, "Prefisso associato a telemarketing massivo o numerazione a sovrapprezzo",
                    SpamCategory.TELEMARKETING) else null
        },
        // Wangiri
        { ctx ->
            val e = ctx.parsed.e164
            if (e != null && WANGIRI_PREFIXES.any { e.startsWith(it) })
                Signal(45, "Prefisso internazionale tipico delle truffe 'wangiri'",
                    SpamCategory.WANGIRI) else null
        },
        // Alta frequenza di chiamate in poco tempo
        { ctx ->
            when {
                ctx.recentCallCount >= 5 ->
                    Signal(30, "Chiamate ripetute (${ctx.recentCallCount} nelle ultime 24h)",
                        SpamCategory.ROBOCALL)
                ctx.recentCallCount in 3..4 ->
                    Signal(15, "Chiamate ripetute (${ctx.recentCallCount} nelle ultime 24h)")
                else -> null
            }
        },
        // Chiamate precedenti sempre brevissime → robocall / squillo
        { ctx ->
            if (ctx.avgPrevDurationSec in 1..3 && ctx.recentCallCount >= 2)
                Signal(20, "Chiamate precedenti di durata irrisoria (pattern robocall)",
                    SpamCategory.ROBOCALL) else null
        },
        // Numero fuori area ma presentato come nazionale e già segnalato spesso
        { ctx ->
            if (ctx.parsed.valid && ctx.parsed.region != null && ctx.parsed.region != "IT" &&
                ctx.recentCallCount >= 2)
                Signal(10, "Numero estero con chiamate ripetute") else null
        },
    )

    /** Un chiamante presente in rubrica abbassa fortemente il rischio. */
    fun knownContactAdjustment(ctx: CallContext): Signal? =
        if (ctx.knownContact)
            Signal(-80, "Numero presente nei contatti", SpamCategory.LEGITTIMO) else null
}
