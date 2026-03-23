package com.ktome.core.combat

import com.ktome.core.ecs.Stats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PowerSaveFormulaTest {
    @Test
    fun `apply chance uses the frozen sigmoid curve`() {
        assertEquals(0.50, PowerSaveFormula.applyChance(power = 20, save = 20), 1e-6)
        assertTrue(PowerSaveFormula.applyChance(power = 60, save = 20) > 0.80)
        assertTrue(PowerSaveFormula.applyChance(power = 0, save = 60) < 0.15)
    }

    @Test
    fun `power save stats follow the documented attribute formulas`() {
        val stats = PowerSaveFormula.calculate(Stats(str = 10, dex = 8, con = 9, wil = 7), level = 5)

        assertEquals(27, stats.physicalPower)
        assertEquals(25, stats.physicalSave)
        assertEquals(22, stats.mentalPower)
        assertEquals(23, stats.mentalSave)
        assertEquals(23, stats.spellPower)
        assertEquals(24, stats.spellSave)
    }
}
