package com.dndsheet.domain.model

import com.dndsheet.domain.enums.Ability

/**
 * Canonical data for every standard D&D class and the subclasses that grant
 * spellcasting to an otherwise non-casting class.
 *
 * Used by [AddClassDialog] to auto-fill hit die, spellcasting progression,
 * and spellcasting ability when the user picks a known class or subclass, so
 * they don't have to configure these manually.
 *
 * Rules notes:
 *  - Paladin and Ranger are HALF casters in both editions (rounding differs,
 *    but that lives in SpellSlotCalculator, not here).
 *  - In 5.5e (2024), Paladin/Ranger gain spellcasting at level 1 rather than
 *    level 2 — progression type is still HALF, this is just a slot-table detail.
 *  - Eldritch Knight (Fighter subclass) and Arcane Trickster (Rogue subclass)
 *    are THIRD casters. The base class has NONE; the subclass overrides it.
 *  - Artificer is a 5.5e-only HALF caster using INT.
 *  - All other base classes have NONE and no spellcasting ability.
 */

/** Pre-populated data for one class entry. */
data class ClassPreset(
    val className: String,
    val hitDie: Int,
    val progression: SpellcastingProgression,
    val spellcastingAbility: Ability?,
    /** Known subclasses for this class that can be suggested in the picker. */
    val subclasses: List<SubclassPreset> = emptyList()
)

/**
 * A subclass that may change the parent class's spellcasting progression.
 * [overrideProgression] and [overrideAbility] are non-null only when the
 * subclass grants spellcasting that the base class doesn't have.
 */
data class SubclassPreset(
    val subclassName: String,
    val overrideProgression: SpellcastingProgression? = null,
    val overrideAbility: Ability? = null
)

object ClassPresets {

    val all: List<ClassPreset> = listOf(
        ClassPreset(
            className = "Artificer",
            hitDie = 8,
            progression = SpellcastingProgression.HALF,
            spellcastingAbility = Ability.INT
        ),
        ClassPreset(
            className = "Barbarian",
            hitDie = 12,
            progression = SpellcastingProgression.NONE,
            spellcastingAbility = null
        ),
        ClassPreset(
            className = "Bard",
            hitDie = 8,
            progression = SpellcastingProgression.FULL,
            spellcastingAbility = Ability.CHA
        ),
        ClassPreset(
            className = "Cleric",
            hitDie = 8,
            progression = SpellcastingProgression.FULL,
            spellcastingAbility = Ability.WIS
        ),
        ClassPreset(
            className = "Druid",
            hitDie = 8,
            progression = SpellcastingProgression.FULL,
            spellcastingAbility = Ability.WIS
        ),
        ClassPreset(
            className = "Fighter",
            hitDie = 10,
            progression = SpellcastingProgression.NONE,
            spellcastingAbility = null,
            subclasses = listOf(
                SubclassPreset(
                    subclassName = "Eldritch Knight",
                    overrideProgression = SpellcastingProgression.THIRD,
                    overrideAbility = Ability.INT
                ),
                SubclassPreset("Battle Master"),
                SubclassPreset("Champion"),
                SubclassPreset("Psi Warrior"),
                SubclassPreset("Rune Knight"),
                SubclassPreset("Echo Knight"),
            )
        ),
        ClassPreset(
            className = "Monk",
            hitDie = 8,
            progression = SpellcastingProgression.NONE,
            spellcastingAbility = null
        ),
        ClassPreset(
            className = "Paladin",
            hitDie = 10,
            progression = SpellcastingProgression.HALF,
            spellcastingAbility = Ability.CHA
        ),
        ClassPreset(
            className = "Ranger",
            hitDie = 10,
            progression = SpellcastingProgression.HALF,
            spellcastingAbility = Ability.WIS
        ),
        ClassPreset(
            className = "Rogue",
            hitDie = 8,
            progression = SpellcastingProgression.NONE,
            spellcastingAbility = null,
            subclasses = listOf(
                SubclassPreset(
                    subclassName = "Arcane Trickster",
                    overrideProgression = SpellcastingProgression.THIRD,
                    overrideAbility = Ability.INT
                ),
                SubclassPreset("Assassin"),
                SubclassPreset("Inquisitive"),
                SubclassPreset("Mastermind"),
                SubclassPreset("Phantom"),
                SubclassPreset("Scout"),
                SubclassPreset("Soulknife"),
                SubclassPreset("Swashbuckler"),
                SubclassPreset("Thief"),
            )
        ),
        ClassPreset(
            className = "Sorcerer",
            hitDie = 6,
            progression = SpellcastingProgression.FULL,
            spellcastingAbility = Ability.CHA
        ),
        ClassPreset(
            className = "Warlock",
            hitDie = 8,
            progression = SpellcastingProgression.PACT,
            spellcastingAbility = Ability.CHA
        ),
        ClassPreset(
            className = "Wizard",
            hitDie = 6,
            progression = SpellcastingProgression.FULL,
            spellcastingAbility = Ability.INT
        ),
    )

    /** Look up a preset by class name (case-insensitive). */
    fun find(name: String): ClassPreset? =
        all.firstOrNull { it.className.equals(name.trim(), ignoreCase = true) }

    /**
     * Applies a [SubclassPreset]'s overrides to a [ClassPreset], returning the
     * effective progression and spellcasting ability for the combined entry.
     */
    fun effectiveProgression(classPreset: ClassPreset, subclassPreset: SubclassPreset?): SpellcastingProgression =
        subclassPreset?.overrideProgression ?: classPreset.progression

    fun effectiveAbility(classPreset: ClassPreset, subclassPreset: SubclassPreset?): Ability? =
        subclassPreset?.overrideAbility ?: classPreset.spellcastingAbility
}
