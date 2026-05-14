package com.dndsheet.rules

import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.Character

/**
 * Skill check bonus = ability modifier + (proficiency bonus × tier multiplier).
 *
 * Tier multipliers come from [ProficiencyLevel]:
 *   NONE = 0, HALF = 0.5 (round down), PROFICIENT = 1, EXPERTISE = 2.
 *
 * The half-prof multiplication rounds down per PHB (Bard's Jack of All Trades,
 * Champion Fighter's Remarkable Athlete). We do that explicitly with
 * [Math.floor] on the double result rather than relying on Int arithmetic
 * because EXPERTISE is exact (2.0) and FULL is exact (1.0), but HALF would
 * silently truncate to zero for a +2 PB at L1 if we used Int division.
 */
object SkillCalculator {

    fun bonus(character: Character, skill: Skill): Int {
        character.overrides.skillBonuses[skill]?.let { return it }

        val abilityMod = AbilityCalculator.modifier(character, skill.ability)
        val tier = character.proficiencies[skill]
        val pb = ProficiencyCalculator.bonus(character)
        val profPart = Math.floor(pb * tier.multiplier).toInt()
        return abilityMod + profPart
    }

    fun isOverridden(character: Character, skill: Skill): Boolean =
        character.overrides.skillBonuses.containsKey(skill)
}
