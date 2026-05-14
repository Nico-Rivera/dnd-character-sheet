package com.dndsheet.rules

import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.ManualOverrides
import org.junit.Assert.assertEquals
import org.junit.Test

class PassiveCalculatorTest {

    @Test fun `passive perception is 10 plus perception bonus`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(wis = 14),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.PERCEPTION to ProficiencyLevel.PROFICIENT))
        )
        // WIS +2, PB +2 = +4 bonus → 14 passive
        assertEquals(14, PassiveCalculator.perception(c))
    }

    @Test fun `passive investigation uses INT and investigation skill`() {
        val c = Fixtures.character(scores = Fixtures.scores(intl = 16))
        // INT +3, no prof → +3 → 13 passive
        assertEquals(13, PassiveCalculator.investigation(c))
    }

    @Test fun `passive insight uses WIS`() {
        val c = Fixtures.character(scores = Fixtures.scores(wis = 8))
        // WIS -1, no prof → 9 passive
        assertEquals(9, PassiveCalculator.insight(c))
    }

    @Test fun `initiative is DEX modifier`() {
        val c = Fixtures.character(scores = Fixtures.scores(dex = 18))
        assertEquals(4, PassiveCalculator.initiative(c))
    }

    @Test fun `manual override wins for each passive`() {
        val c = Fixtures.character(
            overrides = ManualOverrides(
                passivePerception = 25,
                passiveInvestigation = 26,
                passiveInsight = 27,
                initiative = 28
            )
        )
        assertEquals(25, PassiveCalculator.perception(c))
        assertEquals(26, PassiveCalculator.investigation(c))
        assertEquals(27, PassiveCalculator.insight(c))
        assertEquals(28, PassiveCalculator.initiative(c))
    }
}
