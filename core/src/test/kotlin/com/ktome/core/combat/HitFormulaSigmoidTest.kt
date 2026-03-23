package com.ktome.core.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HitFormulaSigmoidTest {
    @Test
    fun `sigmoid hit chance keeps the documented midpoint and bounds`() {
        assertEquals(0.50, HitFormula.sigmoidHitChance(0, 10), 1e-6)
        assertEquals(0.5888, HitFormula.sigmoidHitChance(10, 10), 1e-4)
        assertTrue(HitFormula.sigmoidHitChance(1_000, -1_000) <= HitFormula.MAX_HIT_CHANCE)
        assertTrue(HitFormula.sigmoidHitChance(-1_000, 1_000) >= HitFormula.MIN_HIT_CHANCE)
    }

    @Test
    fun `sigmoid hit chance drops and rises smoothly around the midpoint`() {
        val disadvantaged = HitFormula.sigmoidHitChance(10, 20)
        val even = HitFormula.sigmoidHitChance(20, 20)
        val advantaged = HitFormula.sigmoidHitChance(40, 20)

        assertTrue(disadvantaged < even)
        assertTrue(even < advantaged)
        assertTrue(advantaged < 0.95)
    }
}
