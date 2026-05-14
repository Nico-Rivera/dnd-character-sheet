package com.dndsheet.domain.enums

import kotlinx.serialization.Serializable

/**
 * The 18 PHB skills, with their governing ability. Both editions share this
 * list and these ability mappings.
 */
@Serializable
enum class Skill(val display: String, val ability: Ability) {
    ACROBATICS("Acrobatics", Ability.DEX),
    ANIMAL_HANDLING("Animal Handling", Ability.WIS),
    ARCANA("Arcana", Ability.INT),
    ATHLETICS("Athletics", Ability.STR),
    DECEPTION("Deception", Ability.CHA),
    HISTORY("History", Ability.INT),
    INSIGHT("Insight", Ability.WIS),
    INTIMIDATION("Intimidation", Ability.CHA),
    INVESTIGATION("Investigation", Ability.INT),
    MEDICINE("Medicine", Ability.WIS),
    NATURE("Nature", Ability.INT),
    PERCEPTION("Perception", Ability.WIS),
    PERFORMANCE("Performance", Ability.CHA),
    PERSUASION("Persuasion", Ability.CHA),
    RELIGION("Religion", Ability.INT),
    SLEIGHT_OF_HAND("Sleight of Hand", Ability.DEX),
    STEALTH("Stealth", Ability.DEX),
    SURVIVAL("Survival", Ability.WIS)
}
