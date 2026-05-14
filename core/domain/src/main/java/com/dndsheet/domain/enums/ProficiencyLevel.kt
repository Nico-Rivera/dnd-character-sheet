package com.dndsheet.domain.enums

import kotlinx.serialization.Serializable

/**
 * Per-skill / per-save proficiency tier.
 *
 * Modeling these as a single enum (rather than separate booleans for "proficient"
 * and "expertise") lets the skill/save calculators stay branch-light, and makes
 * homebrew tiers like half-proficiency (Bard's Jack of All Trades) trivially
 * representable on saves too — the spec calls for that flexibility in §8.
 */
@Serializable
enum class ProficiencyLevel(val multiplier: Double) {
    NONE(0.0),
    HALF(0.5),        // round down per PHB
    PROFICIENT(1.0),
    EXPERTISE(2.0)
}
