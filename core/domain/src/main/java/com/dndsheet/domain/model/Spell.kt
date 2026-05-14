package com.dndsheet.domain.model

import com.dndsheet.domain.enums.DamageType
import kotlinx.serialization.Serializable

/**
 * A single spell on a character's known/prepared list.
 *
 * @param level 0 = cantrip, 1..9 = spell levels
 * @param sourceClassName identifies which class entry this spell belongs to
 *        — required for multiclass DC/attack calculation because Cleric and
 *        Wizard use different abilities for their save DCs even on the same
 *        character.
 */
@Serializable
data class Spell(
    val name: String,
    val level: Int,
    val school: String = "",
    val castingTime: String = "1 Action",
    val range: String = "",
    val components: String = "",
    val duration: String = "",
    val description: String = "",
    val damageType: DamageType? = null,
    val isPrepared: Boolean = false,
    val isRitual: Boolean = false,
    val isConcentration: Boolean = false,
    val sourceClassName: String? = null
) {
    init {
        require(level in 0..9) { "Spell level out of range: $level" }
        require(name.isNotBlank()) { "Spell name must not be blank" }
    }
}
