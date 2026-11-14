package com.nospam.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Regola di blocco/consenso. Il motore valuta le regole in ordine di [priority]
 * decrescente; la prima che combacia determina l'azione.
 */
@Entity(tableName = "block_rules")
data class BlockRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: RuleType,
    /** Il valore da confrontare: numero E.164, prefisso, o pattern regex. */
    val value: String,
    val action: RuleAction,
    val category: SpamCategory = SpamCategory.UNKNOWN,
    val priority: Int = 100,
    val enabled: Boolean = true,
    /** Origine della regola: manuale, import community, euristica appresa. */
    val source: String = "manual",
    val createdAt: Long = System.currentTimeMillis(),
)

enum class RuleType {
    EXACT_NUMBER,   // corrispondenza esatta E.164
    PREFIX,         // prefisso (es. +3906, 800, numerazioni premium)
    REGEX,          // pattern avanzato
    WITHHELD,       // numero nascosto / anonimo
}

enum class RuleAction { ALLOW, SILENCE, BLOCK }
