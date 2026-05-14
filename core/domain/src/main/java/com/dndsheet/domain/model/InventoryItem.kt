package com.dndsheet.domain.model

import kotlinx.serialization.Serializable

/**
 * A generic inventory entry. Weapons get richer modeling in [Weapon] when the
 * user actually wants to attack with them; everything else (potions, gear,
 * trinkets, weapons not in active rotation) lives here.
 */
@Serializable
data class InventoryItem(
    val name: String,
    val quantity: Int = 1,
    val weightLbs: Double = 0.0,
    val description: String = "",
    val isEquipped: Boolean = false
) {
    init {
        require(name.isNotBlank()) { "Inventory item name must not be blank" }
        require(quantity >= 0) { "Quantity cannot be negative" }
        require(weightLbs >= 0) { "Weight cannot be negative" }
    }
}
