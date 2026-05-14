package com.dndsheet.rules

import com.dndsheet.domain.enums.Ruleset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpellSlotCalculatorTest {

    // --- pure full caster ---

    @Test fun `wizard 1 has caster level 1`() {
        val classes = listOf(Fixtures.wizard(1))
        assertEquals(1, SpellSlotCalculator.multiclassCasterLevel(classes, Ruleset.DND_5E_2024))
    }

    @Test fun `wizard 1 slots are 2-0-0-…`() {
        val slots = SpellSlotCalculator.multiclassSlots(listOf(Fixtures.wizard(1)), Ruleset.DND_5E_2024)
        assertEquals(listOf(2, 0, 0, 0, 0, 0, 0, 0, 0), slots)
    }

    @Test fun `wizard 5 slots match PHB`() {
        val slots = SpellSlotCalculator.multiclassSlots(listOf(Fixtures.wizard(5)), Ruleset.DND_5E_2024)
        assertEquals(listOf(4, 3, 2, 0, 0, 0, 0, 0, 0), slots)
    }

    @Test fun `wizard 20 slots match PHB top row`() {
        val slots = SpellSlotCalculator.multiclassSlots(listOf(Fixtures.wizard(20)), Ruleset.DND_5E_2024)
        assertEquals(listOf(4, 3, 3, 3, 3, 2, 2, 1, 1), slots)
    }

    // --- half caster rounding diverges between editions ---

    @Test fun `paladin 5 contributes 2 in 5e (floor)`() {
        assertEquals(2, SpellSlotCalculator.multiclassCasterLevel(listOf(Fixtures.paladin(5)), Ruleset.DND_5E_2014))
    }

    @Test fun `paladin 5 contributes 3 in 5_5e (ceiling)`() {
        assertEquals(3, SpellSlotCalculator.multiclassCasterLevel(listOf(Fixtures.paladin(5)), Ruleset.DND_5E_2024))
    }

    @Test fun `cleric 5 plus paladin 5 has caster level 7 in 5e`() {
        val classes = listOf(Fixtures.cleric(5), Fixtures.paladin(5))
        // 5 (full) + 2 (paladin floor) = 7
        assertEquals(7, SpellSlotCalculator.multiclassCasterLevel(classes, Ruleset.DND_5E_2014))
    }

    @Test fun `cleric 5 plus paladin 5 has caster level 8 in 5_5e`() {
        val classes = listOf(Fixtures.cleric(5), Fixtures.paladin(5))
        // 5 (full) + 3 (paladin ceiling) = 8
        assertEquals(8, SpellSlotCalculator.multiclassCasterLevel(classes, Ruleset.DND_5E_2024))
    }

    @Test fun `cleric 5 plus paladin 5 slot row differs by edition`() {
        val classes = listOf(Fixtures.cleric(5), Fixtures.paladin(5))
        // 5e level 7: 4,3,3,1,0,…
        assertEquals(listOf(4, 3, 3, 1, 0, 0, 0, 0, 0),
            SpellSlotCalculator.multiclassSlots(classes, Ruleset.DND_5E_2014))
        // 5.5e level 8: 4,3,3,2,0,…
        assertEquals(listOf(4, 3, 3, 2, 0, 0, 0, 0, 0),
            SpellSlotCalculator.multiclassSlots(classes, Ruleset.DND_5E_2024))
    }

    // --- third caster ---

    @Test fun `eldritch knight 9 contributes 3`() {
        assertEquals(3, SpellSlotCalculator.multiclassCasterLevel(listOf(Fixtures.eldritchKnight(9)), Ruleset.DND_5E_2024))
    }

    @Test fun `arcane trickster 7 contributes 2`() {
        assertEquals(2, SpellSlotCalculator.multiclassCasterLevel(listOf(Fixtures.arcaneTrickster(7)), Ruleset.DND_5E_2024))
    }

    // --- warlock pact slots ---

    @Test fun `warlock 1 has 1 first-level pact slot`() {
        val pact = SpellSlotCalculator.warlockPactSlots(listOf(Fixtures.warlock(1)))
        assertEquals(1 to 1, pact)
    }

    @Test fun `warlock 5 has 2 third-level pact slots`() {
        val pact = SpellSlotCalculator.warlockPactSlots(listOf(Fixtures.warlock(5)))
        assertEquals(2 to 3, pact)
    }

    @Test fun `warlock 11 has 3 fifth-level pact slots`() {
        val pact = SpellSlotCalculator.warlockPactSlots(listOf(Fixtures.warlock(11)))
        assertEquals(3 to 5, pact)
    }

    @Test fun `warlock 20 has 4 fifth-level pact slots`() {
        val pact = SpellSlotCalculator.warlockPactSlots(listOf(Fixtures.warlock(20)))
        assertEquals(4 to 5, pact)
    }

    @Test fun `non-warlock has no pact slots`() {
        assertNull(SpellSlotCalculator.warlockPactSlots(listOf(Fixtures.wizard(20))))
    }

    @Test fun `warlock does not contribute to multiclass caster level`() {
        val classes = listOf(Fixtures.wizard(5), Fixtures.warlock(5))
        // wizard 5 only — warlock ignored for the multiclass table
        assertEquals(5, SpellSlotCalculator.multiclassCasterLevel(classes, Ruleset.DND_5E_2024))
    }

    @Test fun `pure non-caster has no slots`() {
        val classes = listOf(Fixtures.fighter(5), Fixtures.rogue(3), Fixtures.barbarian(2))
        assertEquals(0, SpellSlotCalculator.multiclassCasterLevel(classes, Ruleset.DND_5E_2024))
        assertEquals(List(9) { 0 }, SpellSlotCalculator.multiclassSlots(classes, Ruleset.DND_5E_2024))
    }
}
