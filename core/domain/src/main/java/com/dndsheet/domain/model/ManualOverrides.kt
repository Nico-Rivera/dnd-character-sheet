package com.dndsheet.domain.model

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.Skill
import kotlinx.serialization.Serializable

/**
 * Per-spec §8: every calculated value can be pinned by the user. When an
 * override is present, the calculator returns the pinned value verbatim and
 * the UI can flag the field as "manual".
 *
 * Keys are kept narrow (one map per concept) rather than a generic
 * Map<String, Int> because that gives compile-time safety against typos —
 * `overrides.skillBonuses[Skill.STEALTH]` won't compile if Stealth is removed.
 *
 * String-keyed maps (weaponAttackBonus, weaponDamageBonus, spellSaveDc,
 * spellAttackBonus) are keyed by weapon name and class name respectively
 * because those are user-defined identifiers and there's no enum for them.
 */
@Serializable
data class ManualOverrides(
    val abilityModifiers: Map<Ability, Int> = emptyMap(),
    val proficiencyBonus: Int? = null,
    val skillBonuses: Map<Skill, Int> = emptyMap(),
    val saveBonuses: Map<Ability, Int> = emptyMap(),
    val passivePerception: Int? = null,
    val passiveInvestigation: Int? = null,
    val passiveInsight: Int? = null,
    val initiative: Int? = null,
    val weaponAttackBonus: Map<String, Int> = emptyMap(),
    val weaponDamageBonus: Map<String, Int> = emptyMap(),
    val spellSaveDc: Map<String, Int> = emptyMap(),
    val spellAttackBonus: Map<String, Int> = emptyMap()
)
