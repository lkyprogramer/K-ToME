package com.ktome.core.combat

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class P2toP3FormulaComparisonTest {
    @Test
    fun `phase2 to phase3 comparison stays inside per metric thresholds and explains meaningful deltas`() {
        val scenarios =
            listOf(
                ComparisonScenario(
                    metric = MetricType.HIT_RATE,
                    legacyValue = HitFormula.linearHitChance(60, 0),
                    modernValue = HitFormula.sigmoidHitChance(60, 0),
                    allowedDelta = 0.10,
                    expectedReason = "SIGMOID_UPGRADE",
                ),
                ComparisonScenario(
                    metric = MetricType.DAMAGE,
                    legacyValue = legacyElementalDamage(rawDamage = 100, targetResistance = 20),
                    modernValue = DamageFormula.reduceElementalDamage(rawDamage = 100, targetResistance = 20, resistancePenetration = 10).reducedDamage.toDouble(),
                    allowedDelta = 0.30,
                    expectedReason = "RESISTANCE_CLAMP_UPDATE",
                ),
                ComparisonScenario(
                    metric = MetricType.STATUS_APPLY,
                    legacyValue = legacyStatusApplyChance(power = 40, save = 20),
                    modernValue = PowerSaveFormula.applyChance(power = 40, save = 20),
                    allowedDelta = 0.10,
                    expectedReason = "POWER_SAVE_MODEL",
                ),
            )

        scenarios.forEach { scenario ->
            val delta = scenario.metric.delta(scenario.legacyValue, scenario.modernValue)
            assertTrue(
                delta <= scenario.allowedDelta,
                "${scenario.metric} delta $delta exceeded ${scenario.allowedDelta}",
            )
            if (delta > 0.05) {
                val reasons = scenario.reasons()
                assertFalse(reasons.isEmpty(), "Expected at least one reason tag for ${scenario.metric}.")
                assertTrue(reasons.contains(scenario.expectedReason))
            }
        }
    }

    private fun legacyElementalDamage(
        rawDamage: Int,
        targetResistance: Int,
    ): Double {
        val effectiveResistance = targetResistance.coerceIn(-25, 75)
        return rawDamage * (1.0 - effectiveResistance / 100.0)
    }

    private fun legacyStatusApplyChance(
        power: Int,
        save: Int,
    ): Double = (0.50 + (power - save) * 0.005).coerceIn(0.10, 0.90)

    private data class ComparisonScenario(
        val metric: MetricType,
        val legacyValue: Double,
        val modernValue: Double,
        val allowedDelta: Double,
        val expectedReason: String,
    ) {
        fun reasons(): Set<String> =
            when (metric) {
                MetricType.HIT_RATE -> setOf("SIGMOID_UPGRADE")
                MetricType.DAMAGE -> setOf("RESISTANCE_CLAMP_UPDATE")
                MetricType.STATUS_APPLY -> setOf("POWER_SAVE_MODEL")
            }
    }

    private enum class MetricType {
        DAMAGE,
        HIT_RATE,
        STATUS_APPLY,
        ;

        fun delta(
            legacyValue: Double,
            modernValue: Double,
        ): Double =
            when (this) {
                DAMAGE -> {
                    if (legacyValue == 0.0) {
                        0.0
                    } else {
                        kotlin.math.abs(modernValue - legacyValue) / legacyValue
                    }
                }

                HIT_RATE,
                STATUS_APPLY,
                -> kotlin.math.abs(modernValue - legacyValue)
            }
    }
}
