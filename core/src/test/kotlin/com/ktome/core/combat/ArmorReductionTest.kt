package com.ktome.core.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ArmorReductionTest {
    @Test
    fun `physical reduction follows the armor over armor plus hundred model`() {
        assertEquals(100, DamageFormula.reducePhysicalDamage(rawDamage = 100, targetArmor = 0).reducedDamage)
        assertEquals(50, DamageFormula.reducePhysicalDamage(rawDamage = 100, targetArmor = 100).reducedDamage)

        val penetrated = DamageFormula.reducePhysicalDamage(rawDamage = 100, targetArmor = 100, armorPenetration = 30)
        assertEquals(59, penetrated.reducedDamage)
        assertEquals(30, penetrated.penetrationContribution)
        assertEquals(70, penetrated.reductionValue)
    }
}
