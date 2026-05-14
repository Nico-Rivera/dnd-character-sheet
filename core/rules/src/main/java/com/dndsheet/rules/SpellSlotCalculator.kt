package com.dndsheet.rules

import com.dndsheet.domain.enums.Ruleset
import com.dndsheet.domain.model.ClassLevel
import com.dndsheet.domain.model.SpellcastingProgression

/**
 * Spell slot tables.
 *
 * Two separate concerns:
 *   1. The standard multiclass slot table (PHB p.165) — Bard/Cleric/Druid/
 *      Sorcerer/Wizard contribute full caster levels, Paladin/Ranger half,
 *      Eldritch Knight/Arcane Trickster third. Warlock does NOT contribute.
 *   2. Warlock pact slots — a separate, smaller table that always uses
 *      slots of the warlock's current pact-slot level.
 *
 * Half-caster rounding is the one place rules diverge between editions:
 *   - 5e (2014): floor — Paladin 5 / Ranger 5 contributes 5/2 = 2 each, → 4
 *   - 5.5e (2024): ceiling — Paladin 5 / Ranger 5 contributes 5/2 rounded up = 3 each → 6
 *
 *  This affects a Cleric 5 / Paladin 5 character meaningfully: caster level 7
 *  in 5e vs 8 in 5.5e, which changes the slot row.
 */
object SpellSlotCalculator {

    // Multiclass caster table: rows indexed by total caster level (1..20).
    // Each row is a list of slot counts for level-1 through level-9 spells.
    private val MULTICLASS_SLOTS: List<List<Int>> = listOf(
        listOf(2, 0, 0, 0, 0, 0, 0, 0, 0),  // 1
        listOf(3, 0, 0, 0, 0, 0, 0, 0, 0),  // 2
        listOf(4, 2, 0, 0, 0, 0, 0, 0, 0),  // 3
        listOf(4, 3, 0, 0, 0, 0, 0, 0, 0),  // 4
        listOf(4, 3, 2, 0, 0, 0, 0, 0, 0),  // 5
        listOf(4, 3, 3, 0, 0, 0, 0, 0, 0),  // 6
        listOf(4, 3, 3, 1, 0, 0, 0, 0, 0),  // 7
        listOf(4, 3, 3, 2, 0, 0, 0, 0, 0),  // 8
        listOf(4, 3, 3, 3, 1, 0, 0, 0, 0),  // 9
        listOf(4, 3, 3, 3, 2, 0, 0, 0, 0),  // 10
        listOf(4, 3, 3, 3, 2, 1, 0, 0, 0),  // 11
        listOf(4, 3, 3, 3, 2, 1, 0, 0, 0),  // 12
        listOf(4, 3, 3, 3, 2, 1, 1, 0, 0),  // 13
        listOf(4, 3, 3, 3, 2, 1, 1, 0, 0),  // 14
        listOf(4, 3, 3, 3, 2, 1, 1, 1, 0),  // 15
        listOf(4, 3, 3, 3, 2, 1, 1, 1, 0),  // 16
        listOf(4, 3, 3, 3, 2, 1, 1, 1, 1),  // 17
        listOf(4, 3, 3, 3, 3, 1, 1, 1, 1),  // 18
        listOf(4, 3, 3, 3, 3, 2, 1, 1, 1),  // 19
        listOf(4, 3, 3, 3, 3, 2, 2, 1, 1)   // 20
    )

    // Warlock pact magic table: rows indexed by Warlock level (1..20).
    // Each row is (slot count, slot level). Higher slot levels mean more
    // powerful pact slots, refreshed on short rest.
    private val PACT_SLOTS: List<Pair<Int, Int>> = listOf(
        1 to 1, 2 to 1,                              // 1-2
        2 to 2, 2 to 2,                              // 3-4
        2 to 3, 2 to 3,                              // 5-6
        2 to 4, 2 to 4,                              // 7-8
        2 to 5, 2 to 5, 3 to 5, 3 to 5,              // 9-12
        3 to 5, 3 to 5, 3 to 5, 3 to 5,              // 13-16
        4 to 5, 4 to 5, 4 to 5, 4 to 5               // 17-20
    )

    /**
     * Effective caster level for the multiclass slot table.
     * Warlock levels never contribute here — pact magic is tracked separately.
     */
    fun multiclassCasterLevel(classes: List<ClassLevel>, ruleset: Ruleset): Int {
        var total = 0
        for (cl in classes) {
            total += when (cl.progression) {
                SpellcastingProgression.FULL -> cl.level
                SpellcastingProgression.HALF -> when (ruleset) {
                    Ruleset.DND_5E_2014 -> cl.level / 2                 // floor
                    Ruleset.DND_5E_2024 -> (cl.level + 1) / 2           // ceiling
                }
                SpellcastingProgression.THIRD -> cl.level / 3            // floor in both editions
                SpellcastingProgression.PACT,
                SpellcastingProgression.NONE -> 0
            }
        }
        return total
    }

    /**
     * Slots for level 1..9 spells from the multiclass table.
     * Returns a 9-element list (slots for level 1..9 in order).
     * All zeros if the character has no non-warlock spellcasting.
     */
    fun multiclassSlots(classes: List<ClassLevel>, ruleset: Ruleset): List<Int> {
        val casterLevel = multiclassCasterLevel(classes, ruleset)
        if (casterLevel == 0) return List(9) { 0 }
        return MULTICLASS_SLOTS[casterLevel - 1]
    }

    /**
     * Warlock pact slots. Returns null if the character has no warlock levels.
     * The pair is (slot count, slot level) — e.g. a Warlock 5 has 2 slots of
     * 3rd-level pact magic.
     */
    fun warlockPactSlots(classes: List<ClassLevel>): Pair<Int, Int>? {
        val warlockLevel = classes
            .filter { it.progression == SpellcastingProgression.PACT }
            .sumOf { it.level }
        if (warlockLevel == 0) return null
        return PACT_SLOTS[warlockLevel - 1]
    }
}
