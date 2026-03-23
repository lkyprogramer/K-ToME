package com.ktome.core.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DiminishingReturnsTest {
    @Test
    fun `hyperbolic diminishing returns hits the documented half value points`() {
        assertEquals(75.0, DiminishingReturns.effectiveEvasion(150), 1e-6)
        assertEquals(40.0, DiminishingReturns.effectiveHpRegen(80.0), 1e-6)
    }

    @Test
    fun `marginal gain decreases as the raw value grows`() {
        val early = DiminishingReturns.marginalValue(rawValue = 10.0, halfValueConstant = DiminishingReturns.DR_EVASION_C)
        val late = DiminishingReturns.marginalValue(rawValue = 300.0, halfValueConstant = DiminishingReturns.DR_EVASION_C)

        assertTrue(early > late)
    }
}
