package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.model.ManualOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AbilityCalculatorTest {

    @Test fun `score 10 is +0`() = assertEquals(0, AbilityCalculator.modifier(10))
    @Test fun `score 11 is still +0`() = assertEquals(0, AbilityCalculator.modifier(11))
    @Test fun `score 12 is +1`() = assertEquals(1, AbilityCalculator.modifier(12))
    @Test fun `score 16 is +3`() = assertEquals(3, AbilityCalculator.modifier(16))
    @Test fun `score 20 is +5`() = assertEquals(5, AbilityCalculator.modifier(20))
    @Test fun `score 30 is +10`() = assertEquals(10, AbilityCalculator.modifier(30))

    // Below-10 cases — these are the ones truncating integer division gets wrong.
    @Test fun `score 9 is -1`() = assertEquals(-1, AbilityCalculator.modifier(9))
    @Test fun `score 8 is -1`() = assertEquals(-1, AbilityCalculator.modifier(8))
    @Test fun `score 7 is -2`() = assertEquals(-2, AbilityCalculator.modifier(7))
    @Test fun `score 6 is -2`() = assertEquals(-2, AbilityCalculator.modifier(6))
    @Test fun `score 1 is -5`() = assertEquals(-5, AbilityCalculator.modifier(1))

    @Test fun `character lookup uses raw score by default`() {
        val c = Fixtures.character(scores = Fixtures.scores(str = 16))
        assertEquals(3, AbilityCalculator.modifier(c, Ability.STR))
        assertFalse(AbilityCalculator.isOverridden(c, Ability.STR))
    }

    @Test fun `manual override wins over raw score`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(str = 16),
            overrides = ManualOverrides(abilityModifiers = mapOf(Ability.STR to 99))
        )
        assertEquals(99, AbilityCalculator.modifier(c, Ability.STR))
        assertTrue(AbilityCalculator.isOverridden(c, Ability.STR))
    }
}
