package com.ktome.core.combat

import kotlin.math.roundToInt

data class DamageReduction(
    val reducedDamage: Int,
    val reductionValue: Int,
    val penetrationContribution: Int,
)

object DamageFormula {
    private const val ARMOR_CONSTANT: Double = 100.0
    const val MIN_ELEMENTAL_RESISTANCE: Int = -25
    const val MAX_ELEMENTAL_RESISTANCE: Int = 75
    const val HOLY_TAG_MULTIPLIER: Double = 1.50
    private val holyTagBonusTargets: Set<String> = setOf("undead", "demon")

    fun reduceDamage(
        rawDamage: Int,
        damageType: DamageType,
        targetArmor: Int = 0,
        targetResistance: Int = 0,
        penetration: Int = 0,
    ): DamageReduction =
        if (damageType == DamageType.PHYSICAL) {
            reducePhysicalDamage(
                rawDamage = rawDamage,
                targetArmor = targetArmor,
                armorPenetration = penetration,
            )
        } else {
            reduceElementalDamage(
                rawDamage = rawDamage,
                targetResistance = targetResistance,
                resistancePenetration = penetration,
            )
        }

    fun reducePhysicalDamage(
        rawDamage: Int,
        targetArmor: Int,
        armorPenetration: Int = 0,
    ): DamageReduction {
        val effectiveArmor = (targetArmor - armorPenetration).coerceAtLeast(0)
        val reductionFraction =
            if (effectiveArmor == 0) {
                0.0
            } else {
                effectiveArmor.toDouble() / (effectiveArmor + ARMOR_CONSTANT)
            }
        val reducedDamage = (rawDamage.coerceAtLeast(0) * (1.0 - reductionFraction)).roundToInt().coerceAtLeast(0)
        return DamageReduction(
            reducedDamage = reducedDamage,
            reductionValue = effectiveArmor,
            penetrationContribution = targetArmor.coerceAtLeast(0).coerceAtMost(armorPenetration.coerceAtLeast(0)),
        )
    }

    fun effectiveResistance(
        targetResistance: Int,
        resistancePenetration: Int = 0,
    ): Int = (targetResistance - resistancePenetration).coerceIn(MIN_ELEMENTAL_RESISTANCE, MAX_ELEMENTAL_RESISTANCE)

    fun reduceElementalDamage(
        rawDamage: Int,
        targetResistance: Int,
        resistancePenetration: Int = 0,
    ): DamageReduction {
        val effectiveResistance = effectiveResistance(targetResistance, resistancePenetration)
        val multiplier = 1.0 - effectiveResistance / 100.0
        val reducedDamage = (rawDamage.coerceAtLeast(0) * multiplier).roundToInt().coerceAtLeast(0)
        return DamageReduction(
            reducedDamage = reducedDamage,
            reductionValue = effectiveResistance,
            penetrationContribution = targetResistance.coerceAtLeast(0).coerceAtMost(resistancePenetration.coerceAtLeast(0)),
        )
    }

    fun tagDamageMultiplier(
        damageType: DamageType,
        targetTags: Set<String>,
    ): Double =
        if (damageType == DamageType.HOLY && targetTags.any(holyTagBonusTargets::contains)) {
            HOLY_TAG_MULTIPLIER
        } else {
            1.0
        }
}
