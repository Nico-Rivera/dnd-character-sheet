package com.dndsheet.app.debug

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.Alignment
import com.dndsheet.domain.enums.DamageType
import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Ruleset
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.AbilityScores
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.domain.model.Proficiencies
import com.dndsheet.domain.model.Spell
import com.dndsheet.domain.model.SpellcastingProgression
import com.dndsheet.domain.model.Weapon

/**
 * Sample characters for development. They exercise a variety of engine
 * paths (multiclass spellcaster, finesse weapon, expertise) so that "did
 * the calculation render correctly?" is checkable visually without typing
 * a character in by hand.
 *
 * Production code never references this; the list screen calls into it
 * only when the user taps "Add example character".
 */
object SeedData {

    fun exampleWizardCleric(): Character = Character(
        name = "Eltharion",
        species = "Elf",
        background = "Sage",
        alignment = Alignment.CHAOTIC_GOOD,
        ruleset = Ruleset.DND_5E_2024,

        classes = listOf(
            ClassLevel(
                className = "Wizard",
                level = 4,
                subclass = "Evocation",
                hitDie = 6,
                progression = SpellcastingProgression.FULL,
                spellcastingAbility = Ability.INT
            ),
            ClassLevel(
                className = "Cleric",
                level = 1,
                subclass = "Light",
                hitDie = 8,
                progression = SpellcastingProgression.FULL,
                spellcastingAbility = Ability.WIS
            )
        ),

        abilityScores = AbilityScores(
            strength = 10, dexterity = 14, constitution = 13,
            intelligence = 17, wisdom = 12, charisma = 8
        ),

        proficiencies = Proficiencies(
            skills = mapOf(
                Skill.ARCANA to ProficiencyLevel.EXPERTISE,
                Skill.HISTORY to ProficiencyLevel.PROFICIENT,
                Skill.INSIGHT to ProficiencyLevel.PROFICIENT,
                Skill.PERCEPTION to ProficiencyLevel.HALF
            ),
            saves = mapOf(
                Ability.INT to ProficiencyLevel.PROFICIENT,
                Ability.WIS to ProficiencyLevel.PROFICIENT
            ),
            armor = listOf("Light Armor"),
            weapons = listOf("Daggers", "Quarterstaffs", "Slings"),
            tools = listOf("Calligrapher's Supplies"),
            languages = listOf("Common", "Elvish", "Draconic")
        ),

        weapons = listOf(
            Weapon("Quarterstaff", "1d6", DamageType.BLUDGEONING, versatileDice = "1d8"),
            Weapon("Dagger", "1d4", DamageType.PIERCING, isFinesse = true)
        ),

        spells = listOf(
            Spell("Fire Bolt", level = 0, sourceClassName = "Wizard"),
            Spell("Mage Hand", level = 0, sourceClassName = "Wizard"),
            Spell("Magic Missile", level = 1, sourceClassName = "Wizard", isPrepared = true),
            Spell("Shield", level = 1, sourceClassName = "Wizard", isPrepared = true),
            Spell("Misty Step", level = 2, sourceClassName = "Wizard", isPrepared = true),
            Spell("Sacred Flame", level = 0, sourceClassName = "Cleric"),
            Spell("Cure Wounds", level = 1, sourceClassName = "Cleric", isPrepared = true)
        ),

        maxHp = 32,
        currentHp = 32
    )
}
