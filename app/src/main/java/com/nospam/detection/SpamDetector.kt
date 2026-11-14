package com.nospam.detection

import com.nospam.data.model.BlockRule
import com.nospam.data.model.RuleAction
import com.nospam.data.model.RuleType
import com.nospam.data.model.SpamCategory
import com.nospam.data.model.Verdict
import com.nospam.data.repo.CallRepository

/**
 * Cuore decisionale. Combina:
 *   1. le regole esplicite dell'utente/community (blocklist/allowlist);
 *   2. le euristiche indipendenti dal contenuto (HeuristicRules);
 * e produce un verdetto + punteggio + motivazioni, tutto tracciabile.
 *
 * Importante: la decisione avviene PRIMA della risposta, sui soli metadati. L'app
 * non apre mai l'audio della chiamata.
 */
class SpamDetector(private val repository: CallRepository) {

    private val normalizer = NumberNormalizer("IT")

    data class Decision(
        val verdict: Verdict,
        val category: SpamCategory,
        val riskScore: Int,
        val reasons: List<String>,
        val parsed: NumberNormalizer.Parsed,
    )

    suspend fun evaluate(rawNumber: String?, knownContact: Boolean): Decision {
        val parsed = normalizer.parse(rawNumber)
        val reasons = mutableListOf<String>()

        // 1) regole esplicite: la prima corrispondenza con priorità più alta vince
        val rules = repository.activeRules()
        matchRule(rules, parsed, rawNumber)?.let { rule ->
            val verdict = when (rule.action) {
                RuleAction.ALLOW -> Verdict.ALLOWED
                RuleAction.SILENCE -> Verdict.SILENCED
                RuleAction.BLOCK -> Verdict.BLOCKED
            }
            reasons += "Regola esplicita (${rule.type}) → ${rule.action}"
            return Decision(
                verdict = verdict,
                category = if (rule.category != SpamCategory.UNKNOWN) rule.category
                else if (verdict == Verdict.ALLOWED) SpamCategory.LEGITTIMO else SpamCategory.UNKNOWN,
                riskScore = if (verdict == Verdict.BLOCKED) 100 else 0,
                reasons = reasons,
                parsed = parsed,
            )
        }

        // 2) euristiche
        val e164 = parsed.e164
        val history = if (e164 != null) repository.historyFor(e164) else emptyList()
        val since = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        val recent = history.count { it.timestamp >= since }

        val ctx = CallContext(
            raw = rawNumber,
            parsed = parsed,
            knownContact = knownContact,
            recentCallCount = recent,
            avgPrevDurationSec = 0, // estendibile se si leggono le durate dal call log di sistema
        )

        var score = 0
        var category = SpamCategory.UNKNOWN
        HeuristicRules.all.forEach { rule ->
            rule(ctx)?.let { s ->
                score += s.points
                reasons += s.reason
                if (s.category != SpamCategory.UNKNOWN && category == SpamCategory.UNKNOWN) {
                    category = s.category
                }
            }
        }
        HeuristicRules.knownContactAdjustment(ctx)?.let { s ->
            score += s.points
            reasons += s.reason
            if (s.category == SpamCategory.LEGITTIMO) category = SpamCategory.LEGITTIMO
        }

        score = score.coerceIn(0, 100)

        val verdict = when {
            score >= 70 -> Verdict.BLOCKED
            score >= 40 -> Verdict.SILENCED
            else -> Verdict.ALLOWED
        }
        if (reasons.isEmpty()) reasons += "Nessun segnale di rischio rilevato"

        return Decision(verdict, category, score, reasons, parsed)
    }

    private fun matchRule(
        rules: List<BlockRule>,
        parsed: NumberNormalizer.Parsed,
        raw: String?,
    ): BlockRule? {
        for (rule in rules) { // già ordinate per priorità decrescente
            val hit = when (rule.type) {
                RuleType.EXACT_NUMBER -> parsed.e164 != null && parsed.e164 == rule.value
                RuleType.PREFIX -> parsed.e164?.startsWith(rule.value) == true
                RuleType.REGEX -> raw != null && runCatching { Regex(rule.value).containsMatchIn(raw) }.getOrDefault(false)
                RuleType.WITHHELD -> parsed.lineType == "WITHHELD"
            }
            if (hit) return rule
        }
        return null
    }
}
