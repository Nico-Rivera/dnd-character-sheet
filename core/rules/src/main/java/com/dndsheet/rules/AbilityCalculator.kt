package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.model.Character

/**
 * Ability score → modifier.
 *
 * Standard PHB formula: (score - 10) / 2, rounded down. We use [Math.floorDiv]
 * rather than truncating division so the rounding stays correct for scores
 * below 10 (where truncating division would round toward zero, giving e.g.
 * 0 instead of -1 for a score of 8).
 *
 * Per spec §8, the calculator consults [Character.overrides] first so a user
 * can pin a modifier even when it differs from raw arithmetic.
 */
object AbilityCalculator {

    fun modifier(score: Int): Int = Math.floorDiv(score - 10, 2)

    fun modifier(character: Character, ability: Ability): Int {
        character.overrides.abilityModifiers[ability]?.let { return it }
        return modifier(character.abilityScores[ability])
    }

    fun isOverridden(character: Character, ability: Ability): Boolean =
        character.overrides.abilityModifiers.containsKey(ability)
}
