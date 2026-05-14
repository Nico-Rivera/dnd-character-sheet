package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.DamageType
import com.dndsheet.domain.model.ManualOverrides
import com.dndsheet.domain.model.Weapon
import org.junit.Assert.assertEquals
import org.junit.Test

class WeaponCalculatorTest {

    private val longsword = Weapon(
        name = "Longsword",
        damageDice = "1d8",
        damageType = DamageType.SLASHING,
        versatileDice = "1d10"
    )
    private val rapier = Weapon(
        name = "Rapier",
        damageDice = "1d8",
        damageType = DamageType.PIERCING,
        isFinesse = true
    )
    private val longbow = Weapon(
        name = "Longbow",
        damageDice = "1d8",
        damageType = DamageType.PIERCING,
        isRanged = true
    )
    private val pactBlade = Weapon(
        name = "Pact Blade",
        damageDice = "1d8",
        damageType = DamageType.SLASHING,
        abilityOverride = Ability.CHA
    )

    @Test fun `non-finesse weapon uses STR`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 16, dex = 18))
        // STR +3 + PB +2 = +5 attack; STR +3 damage
        assertEquals(5, WeaponCalculator.attackBonus(c, longsword))
        assertEquals(3, WeaponCalculator.damageBonus(c, longsword))
    }

    @Test fun `finesse picks higher of STR or DEX`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 10, dex = 18))
        // DEX +4 + PB +2 = +6 attack; +4 damage
        assertEquals(6, WeaponCalculator.attackBonus(c, rapier))
        assertEquals(4, WeaponCalculator.damageBonus(c, rapier))
    }

    @Test fun `finesse tie goes to STR`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 14, dex = 14))
        assertEquals(Ability.STR, WeaponCalculator.abilityUsed(rapier, c))
    }

    @Test fun `ranged forces DEX even if STR is higher`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 20, dex = 10))
        // DEX +0 + PB +2 = +2 attack; +0 damage
        assertEquals(2, WeaponCalculator.attackBonus(c, longbow))
        assertEquals(0, WeaponCalculator.damageBonus(c, longbow))
    }

    @Test fun `ability override beats finesse and ranged`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 8, dex = 8, cha = 18))
        // CHA +4 + PB +2 = +6 attack; +4 damage
        assertEquals(6, WeaponCalculator.attackBonus(c, pactBlade))
        assertEquals(4, WeaponCalculator.damageBonus(c, pactBlade))
    }

    @Test fun `non-proficient weapon skips PB on attack but keeps damage mod`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 16))
        val improv = longsword.copy(isProficient = false)
        assertEquals(3, WeaponCalculator.attackBonus(c, improv))  // STR only
        assertEquals(3, WeaponCalculator.damageBonus(c, improv))
    }

    @Test fun `magic bonus folds into both attack and damage`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 16))
        val plus2 = longsword.copy(magicBonus = 2)
        // STR +3 + PB +2 + magic +2 = +7 attack; STR +3 + magic +2 = +5 damage
        assertEquals(7, WeaponCalculator.attackBonus(c, plus2))
        assertEquals(5, WeaponCalculator.damageBonus(c, plus2))
    }

    @Test fun `damage display shows versatile two-handed dice when requested`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 16))
        assertEquals("1d8 + 3", WeaponCalculator.damageDisplay(c, longsword))
        assertEquals("1d10 + 3", WeaponCalculator.damageDisplay(c, longsword, twoHanded = true))
    }

    @Test fun `damage display omits zero bonus`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 10))
        assertEquals("1d8", WeaponCalculator.damageDisplay(c, longsword))
    }

    @Test fun `damage display shows negative bonus with minus sign`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 8))
        // STR 8 → -1
        assertEquals("1d8 − 1", WeaponCalculator.damageDisplay(c, longsword))
    }

    @Test fun `attack override wins`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(str = 16),
            overrides = ManualOverrides(weaponAttackBonus = mapOf("Longsword" to 99))
        )
        assertEquals(99, WeaponCalculator.attackBonus(c, longsword))
    }

    @Test fun `damage override wins`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(str = 16),
            overrides = ManualOverrides(weaponDamageBonus = mapOf("Longsword" to 88))
        )
        assertEquals(88, WeaponCalculator.damageBonus(c, longsword))
    }
}
