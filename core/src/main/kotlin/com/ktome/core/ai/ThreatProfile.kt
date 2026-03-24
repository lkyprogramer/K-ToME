package com.ktome.core.ai

import com.ktome.core.combat.DamageFormula
import com.ktome.core.combat.DamageType
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
enum class DangerLevel {
    LOW,
    MODERATE,
    HIGH,
    LETHAL,
}

@Serializable
enum class TelegraphShape {
    CIRCLE,
    LINE,
    CONE,
}

@Serializable
enum class CounterplayTag {
    DODGE,
    INTERRUPT,
    BLOCK,
}

@Serializable
data class TelegraphStage(
    val id: String,
    val durationTurns: Int,
)

@Serializable
data class TelegraphSpec(
    val id: String,
    val shape: TelegraphShape,
    val previewTurns: Int,
    val dangerLevel: DangerLevel,
    val threatProfileId: String,
    val radius: Int? = null,
    val length: Int? = null,
    val angle: Int? = null,
    val counterplayTags: List<CounterplayTag> = emptyList(),
    val stages: List<TelegraphStage> = emptyList(),
)

@Serializable
data class LevelBand(
    val min: Int,
    val max: Int,
)

@Serializable
data class ThreatProfileDef(
    val id: String,
    val defenderArchetype: String,
    val levelBand: LevelBand,
    val difficultyId: String,
    val expectedMaxHp: Int,
    val expectedArmor: Int = 0,
    val expectedResistances: Map<DamageType, Int> = emptyMap(),
) {
    fun resistanceFor(damageType: DamageType): Int = expectedResistances[damageType] ?: 0
}

data class ThreatAssessment(
    val previewTurns: Int,
    val dangerLevel: DangerLevel,
    val expectedDamage: Int,
    val expectedHpFraction: Double,
)

object ThreatRatingResolver {
    private const val ONE_TURN_WARNING_THRESHOLD: Double = 0.30
    private const val TWO_TURN_WARNING_THRESHOLD: Double = 0.50

    fun assess(
        telegraphSpec: TelegraphSpec,
        threatProfile: ThreatProfileDef,
        baseAttack: Int,
        damageMultiplier: Double,
        damageType: DamageType?,
    ): ThreatAssessment {
        if (damageType == null) {
            return ThreatAssessment(
                previewTurns = telegraphSpec.previewTurns,
                dangerLevel = telegraphSpec.dangerLevel,
                expectedDamage = 0,
                expectedHpFraction = 0.0,
            )
        }

        val rawDamage = (baseAttack * damageMultiplier).roundToInt().coerceAtLeast(0)
        val reduced =
            DamageFormula.reduceDamage(
                rawDamage = rawDamage,
                damageType = damageType,
                targetArmor = threatProfile.expectedArmor,
                targetResistance = threatProfile.resistanceFor(damageType),
            ).reducedDamage
        val hpFraction =
            if (threatProfile.expectedMaxHp <= 0) {
                0.0
            } else {
                reduced.toDouble() / threatProfile.expectedMaxHp.toDouble()
            }
        val thresholdPreviewTurns =
            when {
                hpFraction >= TWO_TURN_WARNING_THRESHOLD -> 2
                hpFraction >= ONE_TURN_WARNING_THRESHOLD -> 1
                else -> 0
            }

        return ThreatAssessment(
            previewTurns = maxOf(telegraphSpec.previewTurns, thresholdPreviewTurns),
            dangerLevel = maxDangerLevel(telegraphSpec.dangerLevel, dangerLevelFor(hpFraction)),
            expectedDamage = reduced,
            expectedHpFraction = hpFraction,
        )
    }

    private fun dangerLevelFor(hpFraction: Double): DangerLevel =
        when {
            hpFraction >= TWO_TURN_WARNING_THRESHOLD -> DangerLevel.LETHAL
            hpFraction >= ONE_TURN_WARNING_THRESHOLD -> DangerLevel.HIGH
            hpFraction >= 0.15 -> DangerLevel.MODERATE
            else -> DangerLevel.LOW
        }

    private fun maxDangerLevel(
        left: DangerLevel,
        right: DangerLevel,
    ): DangerLevel = if (left.ordinal >= right.ordinal) left else right
}
