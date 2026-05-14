package com.dndsheet.domain.enums

import kotlinx.serialization.Serializable

/** The six D&D ability scores. Order matches the standard PHB layout. */
@Serializable
enum class Ability(val abbr: String) {
    STR("STR"),
    DEX("DEX"),
    CON("CON"),
    INT("INT"),
    WIS("WIS"),
    CHA("CHA")
}
