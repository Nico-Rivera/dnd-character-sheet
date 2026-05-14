package com.dndsheet.rules

import com.dndsheet.domain.model.ManualOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProficiencyCalculatorTest {

    // PHB table boundary checks
    @Test fun `level 1 is +2`() = assertEquals(2, ProficiencyCalculator.bonus(1))
    @Test fun `level 4 is +2`() = assertEquals(2, ProficiencyCalculator.bonus(4))
    @Test fun `level 5 is +3`() = assertEquals(3, ProficiencyCalculator.bonus(5))
    @Test fun `level 8 is +3`() = assertEquals(3, ProficiencyCalculator.bonus(8))
    @Test fun `level 9 is +4`() = assertEquals(4, ProficiencyCalculator.bonus(9))
    @Test fun `level 12 is +4`() = assertEquals(4, ProficiencyCalculator.bonus(12))
    @Test fun `level 13 is +5`() = assertEquals(5, ProficiencyCalculator.bonus(13))
    @Test fun `level 16 is +5`() = assertEquals(5, ProficiencyCalculator.bonus(16))
    @Test fun `level 17 is +6`() = assertEquals(6, ProficiencyCalculator.bonus(17))
    @Test fun `level 20 is +6`() = assertEquals(6, ProficiencyCalculator.bonus(20))

    @Test fun `level 0 is 0`() = assertEquals(0, ProficiencyCalculator.bonus(0))

    @Test fun `multiclass total level drives PB`() {
        val c = Fixtures.character(classes = listOf(Fixtures.fighter(3), Fixtures.wizard(2)))
        assertEquals(3, ProficiencyCalculator.bonus(c))
        assertFalse(ProficiencyCalculator.isOverridden(c))
    }

    @Test fun `manual PB override wins`() {
        val c = Fixtures.character(
            classes = listOf(Fixtures.fighter(1)),
            overrides = ManualOverrides(proficiencyBonus = 42)
        )
        assertEquals(42, ProficiencyCalculator.bonus(c))
        assertTrue(ProficiencyCalculator.isOverridden(c))
    }
}
