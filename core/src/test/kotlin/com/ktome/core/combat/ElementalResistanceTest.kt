package com.ktome.core.combat

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ElementalResistanceTest {
    @Test
    fun `elemental resistance clamps to the frozen range and supports penetration`() {
        val capped = DamageFormula.reduceElementalDamage(rawDamage = 100, targetResistance = 90)
        assertEquals(75, capped.reductionValue)
        assertEquals(25, capped.reducedDamage)

        val penetrated = DamageFormula.reduceElementalDamage(rawDamage = 100, targetResistance = 50, resistancePenetration = 20)
        assertEquals(30, penetrated.reductionValue)
        assertEquals(70, penetrated.reducedDamage)
        assertEquals(20, penetrated.penetrationContribution)
    }

    @Test
    fun `negative elemental resistance increases damage but never below minus twenty five`() {
        val vulnerable = DamageFormula.reduceElementalDamage(rawDamage = 100, targetResistance = -40)
        assertEquals(-25, vulnerable.reductionValue)
        assertEquals(125, vulnerable.reducedDamage)
    }

    @Test
    fun `holy bonus uses undead demon tag path instead of default negative resistance`() {
        assertEquals(1.50, DamageFormula.tagDamageMultiplier(DamageType.HOLY, setOf("undead", "elite")), 0.0001)
        assertEquals(1.50, DamageFormula.tagDamageMultiplier(DamageType.HOLY, setOf("demon")), 0.0001)
        assertEquals(1.0, DamageFormula.tagDamageMultiplier(DamageType.HOLY, setOf("cultist")), 0.0001)
        assertEquals(1.0, DamageFormula.tagDamageMultiplier(DamageType.FIRE, setOf("undead")), 0.0001)
    }
}
