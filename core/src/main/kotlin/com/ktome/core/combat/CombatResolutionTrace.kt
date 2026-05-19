package com.ktome.core.combat

import com.ktome.core.ecs.EntityId
import kotlinx.serialization.Serializable

@Serializable
data class CombatResolutionTrace(
    val traceId: String,
    val turn: Int,
    val attackerId: EntityId,
    val targetId: EntityId,
    val abilityId: String,
    val damageType: DamageType,
    val steps: List<ResolutionStep>,
    val result: ResolutionResult,
    val passiveTriggers: List<PassiveTriggerTrace> = emptyList(),
    val childTraceIds: List<String> = emptyList(),
)

@Serializable
data class ResolutionStep(
    val stepIndex: Int,
    val stepName: String,
    val inputs: Map<String, String>,
    val outputs: Map<String, String>,
    val flags: Set<String> = emptySet(),
    val callbacks: List<CallbackRecord> = emptyList(),
)

@Serializable
data class CallbackRecord(
    val ownerId: EntityId,
    val callbackName: String,
    val priority: Int,
    val result: String,
    val effect: String? = null,
)

@Serializable
data class PassiveTriggerTrace(
    val passiveKind: String,
    val sourceId: String = "",
    val sourceKind: String = "EQUIPMENT",
    val sourceTemplateId: String = "",
    val talentRank: Int? = null,
    val sourceItemBaseId: String = sourceTemplateId,
    val sourceAffixId: String? = null,
    val sourceSpecialTemplateId: String? = null,
    val statusId: String? = null,
    val resourceType: String? = null,
    val terrainTag: String? = null,
    val condition: String? = null,
    val triggeredCount: Int = 1,
    val amount: Int? = null,
    val duration: Int? = null,
    val magnitude: Double? = null,
    val chance: Double? = null,
    val roll: Double? = null,
)

@Serializable
data class ResolutionResult(
    val hit: Boolean,
    val critical: Boolean = false,
    val critMultiplier: Double = 1.0,
    val rawDamage: Int = 0,
    val preReductionAbsorbed: Int = 0,
    val armorResistanceReduced: Int = 0,
    val penetrationContribution: Int = 0,
    val postReductionModifier: Int = 0,
    val finalDamage: Int = 0,
    val targetKilled: Boolean = false,
    val deathPrevented: Boolean = false,
)

@Serializable
data class CombatTraceSnapshot(
    val envelope: TraceEnvelope,
    val trace: CombatResolutionTrace,
)
