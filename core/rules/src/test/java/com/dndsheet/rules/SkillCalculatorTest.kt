package com.dndsheet.rules

import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Skill
import com.dndsheet.domain.model.ManualOverrides
import org.junit.Assert.assertEquals
import org.junit.Test

class SkillCalculatorTest {

    @Test fun `unproficient stealth is just DEX modifier`() {
        val c = Fixtures.character(scores = Fixtures.scores(dex = 14))
        assertEquals(2, SkillCalculator.bonus(c, Skill.STEALTH))  // DEX 14 → +2
    }

    @Test fun `proficient athletics at level 1 adds PB`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(str = 16),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.ATHLETICS to ProficiencyLevel.PROFICIENT))
        )
        // STR 16 = +3, PB at L1 = +2 → +5
        assertEquals(5, SkillCalculator.bonus(c, Skill.ATHLETICS))
    }

    @Test fun `expertise doubles PB at level 5`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(dex = 14),
            classes = listOf(Fixtures.rogue(5)),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.STEALTH to ProficiencyLevel.EXPERTISE))
        )
        // DEX 14 = +2, PB at L5 = +3, expertise → 2x PB = 6, total = +8
        assertEquals(8, SkillCalculator.bonus(c, Skill.STEALTH))
    }

    @Test fun `half prof at L1 adds 1 (floor of PB over 2)`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(intl = 14),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.ARCANA to ProficiencyLevel.HALF))
        )
        // INT 14 = +2, half PB at L1 = floor(0.5 * 2) = 1, total = 3
        assertEquals(3, SkillCalculator.bonus(c, Skill.ARCANA))
    }

    @Test fun `half prof at L4 still rounds down`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(intl = 14),
            classes = listOf(Fixtures.fighter(4)),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.ARCANA to ProficiencyLevel.HALF))
        )
        // PB still +2 at L4 → half = 1, INT mod = 2, total = 3
        assertEquals(3, SkillCalculator.bonus(c, Skill.ARCANA))
    }

    @Test fun `half prof at L5 gives floor of 1_5 which is 1`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(intl = 14),
            classes = listOf(Fixtures.fighter(5)),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.ARCANA to ProficiencyLevel.HALF))
        )
        // PB at L5 = +3, half = floor(1.5) = 1, INT = +2, total = 3
        assertEquals(3, SkillCalculator.bonus(c, Skill.ARCANA))
    }

    @Test fun `half prof at L9 gives floor of 2 which is 2`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(intl = 14),
            classes = listOf(Fixtures.fighter(9)),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.ARCANA to ProficiencyLevel.HALF))
        )
        // PB at L9 = +4, half = 2, INT = +2, total = 4
        assertEquals(4, SkillCalculator.bonus(c, Skill.ARCANA))
    }

    @Test fun `negative ability modifier propagates`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(cha = 6),
            proficiencies = Fixtures.profs(skills = mapOf(Skill.PERSUASION to ProficiencyLevel.PROFICIENT))
        )
        // CHA 6 = -2, PB at L1 = +2, total = 0
        assertEquals(0, SkillCalculator.bonus(c, Skill.PERSUASION))
    }

    @Test fun `skill override wins`() {
        val c = Fixtures.character(
            overrides = ManualOverrides(skillBonuses = mapOf(Skill.STEALTH to 99))
        )
        assertEquals(99, SkillCalculator.bonus(c, Skill.STEALTH))
    }
}
