package com.dndsheet.rules

import com.dndsheet.domain.model.Character

/**
 * Proficiency bonus from total character level.
 *
 * Closed-form rather than a table lookup: PB = 2 + floor((level - 1) / 4).
 * That gives +2 at L1–4, +3 at L5–8, +4 at L9–12, +5 at L13–16, +6 at L17–20.
 * Matches the PHB table exactly and is cheaper than a lookup.
 */
object ProficiencyCalculator {

    fun bonus(totalLevel: Int): Int {
        require(totalLevel >= 0) { "Total level cannot be negative" }
        if (totalLevel == 0) return 0   // not a PHB case, but defensive for a brand-new sheet
        return 2 + (totalLevel - 1) / 4
    }

    fun bonus(character: Character): Int {
        character.overrides.proficiencyBonus?.let { return it }
        return bonus(character.totalLevel)
    }

    fun isOverridden(character: Character): Boolean =
        character.overrides.proficiencyBonus != null
}
