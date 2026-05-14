package com.dndsheet.domain.model

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.ProficiencyLevel
import com.dndsheet.domain.enums.Skill
import kotlinx.serialization.Serializable

/**
 * Per-skill and per-save proficiency tiers, plus free-form proficiency lists
 * (armor, weapons, tools, languages) that don't feed the math but do appear
 * on the sheet.
 *
 * Missing entries in the maps default to NONE. The engine never reads from
 * the maps directly — it goes through [get] so the default is centralised.
 */
@Serializable
data class Proficiencies(
    val skills: Map<Skill, ProficiencyLevel> = emptyMap(),
    val saves: Map<Ability, ProficiencyLevel> = emptyMap(),
    val armor: List<String> = emptyList(),
    val weapons: List<String> = emptyList(),
    val tools: List<String> = emptyList(),
    val languages: List<String> = emptyList()
) {
    operator fun get(skill: Skill): ProficiencyLevel =
        skills[skill] ?: ProficiencyLevel.NONE

    operator fun get(ability: Ability): ProficiencyLevel =
        saves[ability] ?: ProficiencyLevel.NONE
}
