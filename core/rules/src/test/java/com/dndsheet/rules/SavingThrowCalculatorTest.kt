package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.model.ManualOverrides
import org.junit.Assert.assertEquals
import org.junit.Test

class SavingThrowCalculatorTest {

    @Test fun `unproficient save is just ability mod`() {
        val c = Fixtures.character(scores = Fixtures.scores(con = 14))
        assertEquals(2, SavingThrowCalculator.bonus(c, Ability.CON))
    }

    @Test fun `proficient save at L1 adds PB`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(wis = 14),
            proficiencies = Fixtures.profs(saves = mapOf(Ability.WIS to ProficiencyLevel.PROFICIENT))
        )
        // WIS 14 = +2, PB at L1 = +2, total = +4
        assertEquals(4, SavingThrowCalculator.bonus(c, Ability.WIS))
    }

    @Test fun `expertise on saves works (homebrew)`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(str = 16),
            classes = listOf(Fixtures.fighter(5)),
            proficiencies = Fixtures.profs(saves = mapOf(Ability.STR to ProficiencyLevel.EXPERTISE))
        )
        // STR 16 = +3, PB at L5 = +3, expertise → 6, total = +9
        assertEquals(9, SavingThrowCalculator.bonus(c, Ability.STR))
    }

    @Test fun `save override wins`() {
        val c = Fixtures.character(
            overrides = ManualOverrides(saveBonuses = mapOf(Ability.DEX to 77))
        )
        assertEquals(77, SavingThrowCalculator.bonus(c, Ability.DEX))
    }
}
