package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Ruleset
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.AbilityScores
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.domain.model.ManualOverrides
import com.dndsheet.domain.model.Proficiencies
import com.dndsheet.domain.model.SpellcastingProgression

/**
 * Builders for the unit tests. The goal is to keep test setup readable —
 * tests should say what's *different* about the character they're testing,
 * not restate every field every time.
 */
object Fixtures {

    fun character(
        scores: AbilityScores = AbilityScores(),
        classes: List<ClassLevel> = listOf(fighter(1)),
        proficiencies: Proficiencies = Proficiencies(),
        ruleset: Ruleset = Ruleset.DND_5E_2024,
        overrides: ManualOverrides = ManualOverrides()
    ): Character = Character(
        name = "Test",
        abilityScores = scores,
        classes = classes,
        proficiencies = proficiencies,
        ruleset = ruleset,
        overrides = overrides
    )

    fun scores(
        str: Int = 10, dex: Int = 10, con: Int = 10,
        intl: Int = 10, wis: Int = 10, cha: Int = 10
    ) = AbilityScores(str, dex, con, intl, wis, cha)

    // --- class shortcuts ---

    fun fighter(level: Int) =
        ClassLevel("Fighter", level, hitDie = 10, progression = SpellcastingProgression.NONE)

    fun rogue(level: Int) =
        ClassLevel("Rogue", level, hitDie = 8, progression = SpellcastingProgression.NONE)

    fun barbarian(level: Int) =
        ClassLevel("Barbarian", level, hitDie = 12, progression = SpellcastingProgression.NONE)

    fun wizard(level: Int) = ClassLevel(
        "Wizard", level, hitDie = 6,
        progression = SpellcastingProgression.FULL,
        spellcastingAbility = Ability.INT
    )

    fun cleric(level: Int) = ClassLevel(
        "Cleric", level, hitDie = 8,
        progression = SpellcastingProgression.FULL,
        spellcastingAbility = Ability.WIS
    )

    fun sorcerer(level: Int) = ClassLevel(
        "Sorcerer", level, hitDie = 6,
        progression = SpellcastingProgression.FULL,
        spellcastingAbility = Ability.CHA
    )

    fun paladin(level: Int) = ClassLevel(
        "Paladin", level, hitDie = 10,
        progression = SpellcastingProgression.HALF,
        spellcastingAbility = Ability.CHA
    )

    fun ranger(level: Int) = ClassLevel(
        "Ranger", level, hitDie = 10,
        progression = SpellcastingProgression.HALF,
        spellcastingAbility = Ability.WIS
    )

    fun warlock(level: Int) = ClassLevel(
        "Warlock", level, hitDie = 8,
        progression = SpellcastingProgression.PACT,
        spellcastingAbility = Ability.CHA
    )

    fun eldritchKnight(level: Int) = ClassLevel(
        "Fighter", level, subclass = "Eldritch Knight", hitDie = 10,
        progression = SpellcastingProgression.THIRD,
        spellcastingAbility = Ability.INT
    )

    fun arcaneTrickster(level: Int) = ClassLevel(
        "Rogue", level, subclass = "Arcane Trickster", hitDie = 8,
        progression = SpellcastingProgression.THIRD,
        spellcastingAbility = Ability.INT
    )

    // --- proficiencies shortcut ---

    fun profs(
        skills: Map<Skill, ProficiencyLevel> = emptyMap(),
        saves: Map<Ability, ProficiencyLevel> = emptyMap()
    ) = Proficiencies(skills = skills, saves = saves)
}
