package com.ktome.core.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AIConditionScope {
    SELF,
    TARGET,
}

@Serializable
sealed interface AICondition {
    @Serializable
    @SerialName("TARGET_VISIBLE")
    data object TargetVisible : AICondition

    @Serializable
    @SerialName("TARGET_DISTANCE_LESS_THAN")
    data class TargetDistanceLessThan(
        val distance: Int,
    ) : AICondition

    @Serializable
    @SerialName("TARGET_DISTANCE_AT_MOST")
    data class TargetDistanceAtMost(
        val distance: Int,
    ) : AICondition

    @Serializable
    @SerialName("TARGET_DISTANCE_BETWEEN")
    data class TargetDistanceBetween(
        val minDistance: Int,
        val maxDistance: Int,
    ) : AICondition

    @Serializable
    @SerialName("TARGET_HP_BELOW")
    data class TargetHpBelow(
        val threshold: Double,
    ) : AICondition

    @Serializable
    @SerialName("HP_BELOW")
    data class HpBelow(
        val threshold: Double,
    ) : AICondition

    @Serializable
    @SerialName("HAS_STATUS")
    data class HasStatus(
        val statusId: String,
        val scope: AIConditionScope = AIConditionScope.SELF,
    ) : AICondition

    @Serializable
    @SerialName("TALENT_READY")
    data class TalentReady(
        val talentId: String,
    ) : AICondition

    @Serializable
    @SerialName("TURN_COUNT_MODULO")
    data class TurnCountModulo(
        val divisor: Int,
        val remainder: Int,
    ) : AICondition

    @Serializable
    @SerialName("AND")
    data class And(
        val conditions: List<AICondition>,
    ) : AICondition

    @Serializable
    @SerialName("OR")
    data class Or(
        val conditions: List<AICondition>,
    ) : AICondition

    @Serializable
    @SerialName("NOT")
    data class Not(
        val condition: AICondition,
    ) : AICondition
}
