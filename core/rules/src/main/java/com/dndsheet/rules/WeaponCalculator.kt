package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.model.Character
import com.dndsheet.domain.model.Weapon

/**
 * Weapon attack and damage math.
 *
 * Ability selection precedence (highest → lowest):
 *   1. [Weapon.abilityOverride]    — Pact of the Blade (CHA), Monk Martial Arts (DEX)
 *   2. [Weapon.isRanged]           — forces DEX (a ranged finesse weapon would
 *                                    only happen in homebrew; this is fine)
 *   3. [Weapon.isFinesse]          — higher of STR/DEX, tie → STR
 *   4. default                     — STR
 *
 * Magic bonus folds into both attack and damage. Proficiency bonus folds into
 * attack only when [Weapon.isProficient].
 */
object WeaponCalculator {

    fun abilityUsed(weapon: Weapon, character: Character): Ability {
        weapon.abilityOverride?.let { return it }
        if (weapon.isRanged) return Ability.DEX
        if (weapon.isFinesse) {
            val str = AbilityCalculator.modifier(character, Ability.STR)
            val dex = AbilityCalculator.modifier(character, Ability.DEX)
            return if (dex > str) Ability.DEX else Ability.STR  // tie → STR
        }
        return Ability.STR
    }

    fun attackBonus(character: Character, weapon: Weapon): Int {
        character.overrides.weaponAttackBonus[weapon.name]?.let { return it }

        val ability = abilityUsed(weapon, character)
        val abilityMod = AbilityCalculator.modifier(character, ability)
        val pb = if (weapon.isProficient) ProficiencyCalculator.bonus(character) else 0
        return abilityMod + pb + weapon.magicBonus
    }

    fun damageBonus(character: Character, weapon: Weapon): Int {
        character.overrides.weaponDamageBonus[weapon.name]?.let { return it }

        val ability = abilityUsed(weapon, character)
        val abilityMod = AbilityCalculator.modifier(character, ability)
        return abilityMod + weapon.magicBonus
    }

    /**
     * Damage dice as shown on the sheet. Pass [twoHanded] = true to get the
     * versatile two-handed dice when the weapon has them; otherwise returns
     * the base dice. The "+N" damage bonus is appended only when nonzero,
     * mirroring how a paper sheet reads ("1d8" vs "1d8 + 3").
     */
    fun damageDisplay(character: Character, weapon: Weapon, twoHanded: Boolean = false): String {
        val dice: String = if (twoHanded) weapon.versatileDice ?: weapon.damageDice else weapon.damageDice
        val bonus = damageBonus(character, weapon)
        return when {
            bonus > 0 -> "$dice + $bonus"
            bonus < 0 -> "$dice − ${-bonus}"
            else -> dice
        }
    }

    fun isOverridden(character: Character, weapon: Weapon): Boolean =
        character.overrides.weaponAttackBonus.containsKey(weapon.name) ||
            character.overrides.weaponDamageBonus.containsKey(weapon.name)
}
