package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.model.Character

/**
 * Saving throw bonus = ability modifier + (PB × tier).
 *
 * Same shape as [SkillCalculator] — homebrew expertise-on-saves or half-prof
 * saves both work without engine changes because we read [ProficiencyLevel]
 * straight from the character's proficiencies.
 */
object SavingThrowCalculator {

    fun bonus(character: Character, ability: Ability): Int {
        character.overrides.saveBonuses[ability]?.let { return it }

        val abilityMod = AbilityCalculator.modifier(character, ability)
        val tier = character.proficiencies[ability]
        val pb = ProficiencyCalculator.bonus(character)
        val profPart = Math.floor(pb * tier.multiplier).toInt()
        return abilityMod + profPart
    }

    fun isOverridden(character: Character, ability: Ability): Boolean =
        character.overrides.saveBonuses.containsKey(ability)
}
