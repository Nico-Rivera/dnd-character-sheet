package com.dndsheet.domain.model

import com.dndsheet.domain.enums.Ability
import kotlinx.serialization.Serializable

/**
 * Raw ability scores for a character (before racial/feature/item bonuses are
 * folded in by the calculation engine).
 *
 * Defaults to 10 across the board — the "average human" baseline — so a newly
 * created character is in a sensible state even before the user fills anything
 * in. The 1..30 range mirrors the PHB hard cap; values outside that are
 * rejected at construction so corrupted save files can't bypass the rules.
 */
@Serializable
data class AbilityScores(
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10
) {
    init {
        require(strength in 1..30) { "Strength out of range: $strength" }
        require(dexterity in 1..30) { "Dexterity out of range: $dexterity" }
        require(constitution in 1..30) { "Constitution out of range: $constitution" }
        require(intelligence in 1..30) { "Intelligence out of range: $intelligence" }
        require(wisdom in 1..30) { "Wisdom out of range: $wisdom" }
        require(charisma in 1..30) { "Charisma out of range: $charisma" }
    }

    /** Indexed lookup so the rules engine can ask for an ability generically. */
    operator fun get(ability: Ability): Int = when (ability) {
        Ability.STR -> strength
        Ability.DEX -> dexterity
        Ability.CON -> constitution
        Ability.INT -> intelligence
        Ability.WIS -> wisdom
        Ability.CHA -> charisma
    }
}
