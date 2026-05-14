package com.dndsheet.domain.model

import com.dndsheet.domain.enums.Ability
import kotlinx.serialization.Serializable

/**
 * Spellcasting progression for a class. Drives the multiclass slot table in
 * [com.dndsheet.rules.SpellSlotCalculator].
 *
 * - FULL    — Bard, Cleric, Druid, Sorcerer, Wizard (1 level = 1 caster level)
 * - HALF    — Paladin, Ranger (rounding rule differs 5e vs 5.5e)
 * - THIRD   — Eldritch Knight, Arcane Trickster (rounds down in both editions)
 * - PACT    — Warlock (separate slot track, doesn't combine for multiclass)
 * - NONE    — Barbarian, Fighter (non-subclass), Monk, Rogue (non-subclass)
 */
@Serializable
enum class SpellcastingProgression { FULL, HALF, THIRD, PACT, NONE }

/**
 * One class entry on a (possibly multiclassed) character. A character with
 * `Cleric 5 / Paladin 3` has two [ClassLevel] entries.
 *
 * @param subclass null until the character reaches subclass level (3 in 5e for
 *        most, 1 for Cleric/Sorcerer/Warlock; in 5.5e everyone picks at 3).
 *        Subclass choice can affect spellcasting progression (Eldritch Knight,
 *        Arcane Trickster) so the [progression] field on this entry is what
 *        the engine reads, not the base class.
 */
@Serializable
data class ClassLevel(
    val className: String,
    val level: Int,
    val subclass: String? = null,
    val progression: SpellcastingProgression = SpellcastingProgression.NONE,
    val spellcastingAbility: Ability? = null,
    val hitDie: Int = 8        // d8 default; Barbarian = 12, Fighter/Paladin/Ranger = 10, Wizard/Sorcerer = 6
) {
    init {
        require(level in 1..20) { "Class level out of range: $level" }
        require(className.isNotBlank()) { "Class name must not be blank" }
        require(hitDie in listOf(6, 8, 10, 12)) { "Hit die must be d6/d8/d10/d12, got d$hitDie" }
        if (progression != SpellcastingProgression.NONE) {
            require(spellcastingAbility != null) {
                "Spellcasting class $className must declare a spellcastingAbility"
            }
        }
    }
}
