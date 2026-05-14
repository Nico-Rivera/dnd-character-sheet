package com.dndsheet.rules

import com.dndsheet.domain.model.Character
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.domain.model.SpellcastingProgression

/**
 * Per-class spell save DC and attack modifier.
 *
 * A multiclass character can have two spellcasting classes that use different
 * abilities (Cleric uses WIS, Wizard uses INT), so these are computed per
 * [ClassLevel] rather than per character. The UI shows one row per
 * spellcasting class.
 *
 *   spell save DC       = 8 + PB + spellcasting ability modifier
 *   spell attack bonus  =     PB + spellcasting ability modifier
 *
 * Warlock pact magic uses the same formula — only the slots differ.
 */
object SpellcastingCalculator {

    fun isSpellcaster(classLevel: ClassLevel): Boolean =
        classLevel.progression != SpellcastingProgression.NONE &&
            classLevel.spellcastingAbility != null

    fun spellSaveDc(character: Character, classLevel: ClassLevel): Int? {
        if (!isSpellcaster(classLevel)) return null
        character.overrides.spellSaveDc[classLevel.className]?.let { return it }

        val abilityMod = AbilityCalculator.modifier(character, classLevel.spellcastingAbility!!)
        val pb = ProficiencyCalculator.bonus(character)
        return 8 + pb + abilityMod
    }

    fun spellAttackBonus(character: Character, classLevel: ClassLevel): Int? {
        if (!isSpellcaster(classLevel)) return null
        character.overrides.spellAttackBonus[classLevel.className]?.let { return it }

        val abilityMod = AbilityCalculator.modifier(character, classLevel.spellcastingAbility!!)
        val pb = ProficiencyCalculator.bonus(character)
        return pb + abilityMod
    }

    /** Convenience: returns one row per spellcasting class on the character. */
    fun rows(character: Character): List<SpellcastingRow> =
        character.classes
            .filter { isSpellcaster(it) }
            .map { cl ->
                SpellcastingRow(
                    className = cl.className,
                    ability = cl.spellcastingAbility!!,
                    saveDc = spellSaveDc(character, cl)!!,
                    attackBonus = spellAttackBonus(character, cl)!!
                )
            }
}

data class SpellcastingRow(
    val className: String,
    val ability: com.dndsheet.domain.enums.Ability,
    val saveDc: Int,
    val attackBonus: Int
)
