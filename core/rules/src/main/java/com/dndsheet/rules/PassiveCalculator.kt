package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.Character

/**
 * Passive scores: 10 + the corresponding skill bonus. Initiative is just DEX
 * mod (feature-based bonuses like Alert layer on in a later commit).
 */
object PassiveCalculator {

    /**
     * Generic passive score for any skill: 10 + the skill's full bonus.
     * Delegates to the three named methods for Perception, Investigation and
     * Insight so their manual-override support is preserved.
     */
    fun passive(character: Character, skill: Skill): Int = when (skill) {
        Skill.PERCEPTION    -> perception(character)
        Skill.INVESTIGATION -> investigation(character)
        Skill.INSIGHT       -> insight(character)
        else                -> 10 + SkillCalculator.bonus(character, skill)
    }

    fun perception(character: Character): Int =
        character.overrides.passivePerception
            ?: (10 + SkillCalculator.bonus(character, Skill.PERCEPTION))

    fun investigation(character: Character): Int =
        character.overrides.passiveInvestigation
            ?: (10 + SkillCalculator.bonus(character, Skill.INVESTIGATION))

    fun insight(character: Character): Int =
        character.overrides.passiveInsight
            ?: (10 + SkillCalculator.bonus(character, Skill.INSIGHT))

    fun initiative(character: Character): Int =
        character.overrides.initiative
            ?: AbilityCalculator.modifier(character, Ability.DEX)
}
