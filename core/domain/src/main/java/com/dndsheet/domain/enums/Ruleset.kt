package com.dndsheet.domain.enums

import kotlinx.serialization.Serializable

/**
 * Which edition's rules a character was built under. Stored on the
 * [com.dndsheet.domain.model.Character] so two characters in the same
 * campaign can sit side-by-side under different rulesets without the app
 * needing a global mode flag.
 */
@Serializable
enum class Ruleset(val display: String) {
    DND_5E_2014("D&D 5e (2014)"),
    DND_5E_2024("D&D 2024 (5.5e)")
}
