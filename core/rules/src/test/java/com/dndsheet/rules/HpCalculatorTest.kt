package com.dndsheet.rules

import org.junit.Test
import kotlin.test.assertEquals

class HpCalculatorTest {

    // --- applyTemporaryHp: temp HP never stacks, greater value wins ---

    @Test
    fun `new temp greater than existing replaces it`() {
        assertEquals(8, HpCalculator.applyTemporaryHp(currentTemp = 5, incoming = 8))
    }

    @Test
    fun `new temp lower than existing is ignored`() {
        assertEquals(5, HpCalculator.applyTemporaryHp(currentTemp = 5, incoming = 3))
    }

    @Test
    fun `equal temp keeps the value`() {
        assertEquals(5, HpCalculator.applyTemporaryHp(currentTemp = 5, incoming = 5))
    }

    @Test
    fun `negative inputs are treated as zero`() {
        assertEquals(0, HpCalculator.applyTemporaryHp(currentTemp = -4, incoming = -2))
    }

    // --- takeDamage: temp HP absorbs first, remainder hits current HP ---

    @Test
    fun `damage smaller than temp only reduces temp`() {
        val r = HpCalculator.takeDamage(currentHp = 20, temporaryHp = 10, amount = 6)
        assertEquals(20, r.currentHp)
        assertEquals(4, r.temporaryHp)
    }

    @Test
    fun `damage equal to temp clears temp and spares hp`() {
        val r = HpCalculator.takeDamage(currentHp = 20, temporaryHp = 10, amount = 10)
        assertEquals(20, r.currentHp)
        assertEquals(0, r.temporaryHp)
    }

    @Test
    fun `damage larger than temp spills over to current hp`() {
        val r = HpCalculator.takeDamage(currentHp = 20, temporaryHp = 10, amount = 16)
        assertEquals(14, r.currentHp)
        assertEquals(0, r.temporaryHp)
    }

    @Test
    fun `damage with no temp reduces current hp directly`() {
        val r = HpCalculator.takeDamage(currentHp = 20, temporaryHp = 0, amount = 7)
        assertEquals(13, r.currentHp)
        assertEquals(0, r.temporaryHp)
    }

    @Test
    fun `current hp never drops below zero`() {
        val r = HpCalculator.takeDamage(currentHp = 5, temporaryHp = 3, amount = 100)
        assertEquals(0, r.currentHp)
        assertEquals(0, r.temporaryHp)
    }

    // --- heal: restores current HP only, capped at max ---

    @Test
    fun `healing is capped at max hp`() {
        assertEquals(20, HpCalculator.heal(currentHp = 18, maxHp = 20, amount = 10))
    }

    @Test
    fun `healing adds within bounds`() {
        assertEquals(15, HpCalculator.heal(currentHp = 10, maxHp = 20, amount = 5))
    }
}
