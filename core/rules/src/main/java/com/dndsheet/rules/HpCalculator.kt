package com.dndsheet.rules

/**
 * Hit-point math shared by both D&D 5e (2014) and the 2024 revised rules (5.5e).
 *
 * The temporary HP rules are identical across both editions:
 *  - Temporary HP is not real HP and never adds to your current/max HP.
 *  - Receiving temporary HP does NOT stack with any you already have. When you
 *    gain temp HP you choose to keep either the existing pool or the new amount,
 *    whichever is greater (see [applyTemporaryHp]).
 *  - Damage is removed from temporary HP first; any leftover damage then reduces
 *    current HP (see [takeDamage]).
 */
object HpCalculator {

    /** Result of applying damage: the new current HP and remaining temporary HP. */
    data class HpResult(val currentHp: Int, val temporaryHp: Int)

    /**
     * Applies newly received temporary HP. Temp HP does not stack: the greater of
     * the existing pool and the incoming amount is kept. Negative inputs are
     * treated as zero.
     */
    fun applyTemporaryHp(currentTemp: Int, incoming: Int): Int =
        maxOf(currentTemp.coerceAtLeast(0), incoming.coerceAtLeast(0))

    /**
     * Applies [amount] of damage, depleting temporary HP first and then current HP.
     * Current HP is floored at 0; negative damage is ignored.
     */
    fun takeDamage(currentHp: Int, temporaryHp: Int, amount: Int): HpResult {
        val damage = amount.coerceAtLeast(0)
        val temp = temporaryHp.coerceAtLeast(0)
        val absorbedByTemp = minOf(temp, damage)
        val remainingTemp = temp - absorbedByTemp
        val damageToHp = damage - absorbedByTemp
        val remainingHp = (currentHp - damageToHp).coerceAtLeast(0)
        return HpResult(currentHp = remainingHp, temporaryHp = remainingTemp)
    }

    /**
     * Heals [amount] of damage. Healing restores current HP only (never temp HP)
     * and cannot exceed [maxHp]. Negative healing is ignored.
     */
    fun heal(currentHp: Int, maxHp: Int, amount: Int): Int =
        (currentHp + amount.coerceAtLeast(0)).coerceIn(0, maxHp.coerceAtLeast(0))
}
