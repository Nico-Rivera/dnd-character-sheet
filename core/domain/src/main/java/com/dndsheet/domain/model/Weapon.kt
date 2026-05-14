package com.dndsheet.domain.model

import com.dndsheet.domain.enums.Ability
import com.dndsheet.domain.enums.DamageType
import kotlinx.serialization.Serializable

/**
 * A weapon entry on a character sheet. The fields here are what the engine
 * needs to compute attack and damage; descriptive flavor (weight, cost,
 * description) belongs on an [InventoryItem] when the weapon is also tracked
 * as inventory.
 *
 * @param damageDice e.g. "1d8", "2d6" — left as a string because the engine
 *        doesn't roll dice (yet); the UI shows it verbatim.
 * @param versatileDice optional two-handed damage dice for versatile weapons
 *        (longsword "1d8" / "1d10"). When null, the weapon is not versatile.
 * @param isFinesse Finesse weapons let the attacker pick STR or DEX. The
 *        engine picks the higher modifier (tie → STR per WeaponCalculator).
 * @param isRanged Ranged weapons force DEX, regardless of finesse.
 * @param magicBonus +1/+2/+3 etc. Folded into both attack and damage.
 * @param abilityOverride Forces a specific ability (e.g. Pact of the Blade
 *        always uses CHA, Monk Martial Arts uses DEX even on a non-finesse
 *        weapon). Overrides finesse and ranged rules.
 * @param mastery 5.5e weapon mastery property (e.g. "Sap", "Vex", "Cleave").
 *        Stored but not yet applied — feature lands when the mastery rule
 *        is built out.
 */
@Serializable
data class Weapon(
    val name: String,
    val damageDice: String,
    val damageType: DamageType,
    val versatileDice: String? = null,
    val isFinesse: Boolean = false,
    val isRanged: Boolean = false,
    val isProficient: Boolean = true,
    val magicBonus: Int = 0,
    val abilityOverride: Ability? = null,
    val mastery: String? = null
) {
    init {
        require(name.isNotBlank()) { "Weapon name must not be blank" }
        require(damageDice.isNotBlank()) { "Damage dice must not be blank" }
        require(magicBonus in 0..5) { "Magic bonus out of plausible range: $magicBonus" }
    }
}
