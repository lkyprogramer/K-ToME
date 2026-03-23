package com.ktome.core.combat

import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CritFormulaTest {
    @Test
    fun `crit resistance subtracts effective crit chance before cap`() {
        assertEquals(0.30, CritFormula.effectiveCritChance(baseCritRate = 0.35, critResistance = 0.05), 1e-6)
        assertEquals(0.50, CritFormula.effectiveCritChance(baseCritRate = 0.70), 1e-6)
        assertEquals(0.0, CritFormula.effectiveCritChance(baseCritRate = 0.05, critResistance = 0.10), 1e-6)
    }

    @Test
    fun `crit roll uses the clamped effective crit chance`() {
        val critical = CritFormula.rollCrit(TestRandomSource(doubles = listOf(0.24)), baseCritRate = 0.20, critBonus = 0.10, critResistance = 0.05)
        val resisted = CritFormula.rollCrit(TestRandomSource(doubles = listOf(0.26)), baseCritRate = 0.20, critBonus = 0.10, critResistance = 0.05)

        assertTrue(critical.isCritical)
        assertFalse(resisted.isCritical)
        assertEquals(0.25, critical.effectiveCritChance, 1e-6)
    }
}
