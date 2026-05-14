package com.dndsheet.rules

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.model.ManualOverrides
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpellcastingCalculatorTest {

    @Test fun `wizard at L1 with INT 16`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(intl = 16),
            classes = listOf(Fixtures.wizard(1))
        )
        // PB +2, INT +3 → DC 8 + 2 + 3 = 13; attack +5
        val rows = SpellcastingCalculator.rows(c)
        assertEquals(1, rows.size)
        assertEquals("Wizard", rows[0].className)
        assertEquals(Ability.INT, rows[0].ability)
        assertEquals(13, rows[0].saveDc)
        assertEquals(5, rows[0].attackBonus)
    }

    @Test fun `cleric and wizard multiclass have separate rows with separate abilities`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(wis = 16, intl = 14),
            classes = listOf(Fixtures.cleric(3), Fixtures.wizard(2))
        )
        // total level 5 → PB +3
        val rows = SpellcastingCalculator.rows(c)
        assertEquals(2, rows.size)
        val cleric = rows.first { it.className == "Cleric" }
        val wizard = rows.first { it.className == "Wizard" }
        // Cleric: WIS +3, PB +3 → DC 14, attack +6
        assertEquals(14, cleric.saveDc)
        assertEquals(6, cleric.attackBonus)
        // Wizard: INT +2, PB +3 → DC 13, attack +5
        assertEquals(13, wizard.saveDc)
        assertEquals(5, wizard.attackBonus)
    }

    @Test fun `non-spellcaster class produces no row`() {
        val c = Fixtures.character(
            classes = listOf(Fixtures.fighter(5))
        )
        assertEquals(0, SpellcastingCalculator.rows(c).size)
    }

    @Test fun `spellSaveDc returns null for non-spellcaster`() {
        val c = Fixtures.character(classes = listOf(Fixtures.fighter(5)))
        assertNull(SpellcastingCalculator.spellSaveDc(c, c.classes[0]))
        assertNull(SpellcastingCalculator.spellAttackBonus(c, c.classes[0]))
    }

    @Test fun `manual DC override wins per class`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(intl = 16),
            classes = listOf(Fixtures.wizard(1)),
            overrides = ManualOverrides(spellSaveDc = mapOf("Wizard" to 30))
        )
        assertEquals(30, SpellcastingCalculator.spellSaveDc(c, c.classes[0]))
    }

    @Test fun `manual attack override wins per class`() {
        val c = Fixtures.character(
            scores = Fixtures.scores(intl = 16),
            classes = listOf(Fixtures.wizard(1)),
            overrides = ManualOverrides(spellAttackBonus = mapOf("Wizard" to 20))
        )
        assertEquals(20, SpellcastingCalculator.spellAttackBonus(c, c.classes[0]))
    }
}
