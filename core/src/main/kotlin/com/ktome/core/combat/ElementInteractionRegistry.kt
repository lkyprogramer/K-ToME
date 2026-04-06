package com.ktome.core.combat

import com.ktome.core.ecs.EntityId
import com.ktome.core.mapgen.TerrainTag

enum class InteractionCondition {
    TARGET_ON_TERRAIN,
}

enum class InteractionEffect {
    TERRAIN_TRANSFORM,
    CHAIN_DAMAGE,
    SLIP,
}

data class ElementInteractionRule(
    val id: String,
    val triggerElement: DamageType,
    val conditionType: InteractionCondition,
    val conditionParam: String,
    val effectType: InteractionEffect,
    val effectParams: Map<String, String>,
) {
    init {
        require(id.isNotBlank()) { "ElementInteractionRule.id must not be blank." }
        require(conditionParam.isNotBlank()) { "ElementInteractionRule.conditionParam must not be blank." }
        require(effectParams.keys.all(String::isNotBlank)) { "ElementInteractionRule.effectParams keys must not be blank." }
        require(effectParams.values.all(String::isNotBlank)) { "ElementInteractionRule.effectParams values must not be blank." }
    }
}

data class TerrainInteractionTarget(
    val entityId: EntityId,
    val terrainTags: Set<TerrainTag> = emptySet(),
)

data class TerrainInteractionContext(
    val targetTerrainTags: Set<TerrainTag>,
    val adjacentTargets: List<TerrainInteractionTarget> = emptyList(),
    val interactionDepth: Int = 0,
    val sourceTags: Set<String> = emptySet(),
) {
    init {
        require(interactionDepth >= 0) { "TerrainInteractionContext.interactionDepth must not be negative." }
        require(sourceTags.all(String::isNotBlank)) { "TerrainInteractionContext.sourceTags must not contain blanks." }
    }
}

data class TerrainTransformPlan(
    val targetTerrainTags: Set<TerrainTag>,
    val durationTurns: Int = 0,
    val conductsLightning: Boolean = false,
    val tickDamageType: DamageType? = null,
    val tickDamage: Int = 0,
) {
    init {
        require(durationTurns >= 0) { "TerrainTransformPlan.durationTurns must not be negative." }
        require(tickDamage >= 0) { "TerrainTransformPlan.tickDamage must not be negative." }
        if (tickDamage > 0) {
            requireNotNull(tickDamageType) { "TerrainTransformPlan.tickDamageType is required when tickDamage > 0." }
        }
    }
}

data class TerrainInteractionChildTrace(
    val traceId: String,
    val targetId: EntityId,
    val rawDamage: Int,
    val damageMultiplier: Double,
) {
    init {
        require(traceId.isNotBlank()) { "TerrainInteractionChildTrace.traceId must not be blank." }
        require(rawDamage >= 0) { "TerrainInteractionChildTrace.rawDamage must not be negative." }
        require(damageMultiplier >= 0.0) { "TerrainInteractionChildTrace.damageMultiplier must not be negative." }
    }
}

data class TerrainSlipPlan(
    val statusId: String = "STUN",
    val durationTurns: Int = 1,
) {
    init {
        require(statusId.isNotBlank()) { "TerrainSlipPlan.statusId must not be blank." }
        require(durationTurns > 0) { "TerrainSlipPlan.durationTurns must be positive." }
    }
}

data class ElementInteractionResolution(
    val ruleId: String,
    val triggerDamageType: DamageType,
    val appliedStatusIds: List<String> = emptyList(),
    val removedStatusIds: List<String> = emptyList(),
    val terrainTransform: TerrainTransformPlan? = null,
    val bonusTargetTrace: TerrainInteractionChildTrace? = null,
    val chainTargets: List<TerrainInteractionChildTrace> = emptyList(),
    val slip: TerrainSlipPlan? = null,
    val interactionDepth: Int = 0,
) {
    init {
        require(ruleId.isNotBlank()) { "ElementInteractionResolution.ruleId must not be blank." }
        require(interactionDepth >= 0) { "ElementInteractionResolution.interactionDepth must not be negative." }
    }

    val childTraceIds: List<String>
        get() = listOfNotNull(bonusTargetTrace?.traceId) + chainTargets.map(TerrainInteractionChildTrace::traceId)
}

object ElementInteractionRegistry {
    const val VERSION: Int = 1
    const val MAX_INTERACTION_DEPTH: Int = 2

    const val TERRAIN_LIGHTNING_WATER_CHAIN: String = "terrain_lightning_water_chain"
    const val TERRAIN_FIRE_OIL_IGNITE: String = "terrain_fire_oil_ignite"
    const val TERRAIN_COLD_WATER_FREEZE: String = "terrain_cold_water_freeze"
    const val TERRAIN_FIRE_ICE_MELT: String = "terrain_fire_ice_melt"
    const val TERRAIN_PHYSICAL_ICE_SLIP: String = "terrain_physical_ice_slip"

    private const val OIL_BURN_DURATION_TURNS: Int = 3
    private const val WATER_FREEZE_DURATION_TURNS: Int = 3
    private const val ICE_MELT_DURATION_TURNS: Int = 3
    private const val OIL_BURN_TICK_DAMAGE: Int = 4

    private val rules: Map<String, ElementInteractionRule> =
        listOf(
            ElementInteractionRule(
                id = TERRAIN_LIGHTNING_WATER_CHAIN,
                triggerElement = DamageType.LIGHTNING,
                conditionType = InteractionCondition.TARGET_ON_TERRAIN,
                conditionParam = TerrainTag.WATER.name,
                effectType = InteractionEffect.CHAIN_DAMAGE,
                effectParams =
                    mapOf(
                        "bonusTargetMultiplier" to "0.25",
                        "chainDamageMultiplier" to "0.50",
                    ),
            ),
            ElementInteractionRule(
                id = TERRAIN_FIRE_OIL_IGNITE,
                triggerElement = DamageType.FIRE,
                conditionType = InteractionCondition.TARGET_ON_TERRAIN,
                conditionParam = TerrainTag.OIL.name,
                effectType = InteractionEffect.TERRAIN_TRANSFORM,
                effectParams =
                    mapOf(
                        "durationTurns" to OIL_BURN_DURATION_TURNS.toString(),
                        "tickDamageType" to DamageType.FIRE.name,
                        "tickDamage" to OIL_BURN_TICK_DAMAGE.toString(),
                    ),
            ),
            ElementInteractionRule(
                id = TERRAIN_COLD_WATER_FREEZE,
                triggerElement = DamageType.COLD,
                conditionType = InteractionCondition.TARGET_ON_TERRAIN,
                conditionParam = TerrainTag.WATER.name,
                effectType = InteractionEffect.TERRAIN_TRANSFORM,
                effectParams =
                    mapOf(
                        "resultTerrain" to TerrainTag.ICE.name,
                        "durationTurns" to WATER_FREEZE_DURATION_TURNS.toString(),
                    ),
            ),
            ElementInteractionRule(
                id = TERRAIN_FIRE_ICE_MELT,
                triggerElement = DamageType.FIRE,
                conditionType = InteractionCondition.TARGET_ON_TERRAIN,
                conditionParam = TerrainTag.ICE.name,
                effectType = InteractionEffect.TERRAIN_TRANSFORM,
                effectParams =
                    mapOf(
                        "resultTerrain" to TerrainTag.WATER.name,
                        "durationTurns" to ICE_MELT_DURATION_TURNS.toString(),
                    ),
            ),
            ElementInteractionRule(
                id = TERRAIN_PHYSICAL_ICE_SLIP,
                triggerElement = DamageType.PHYSICAL,
                conditionType = InteractionCondition.TARGET_ON_TERRAIN,
                conditionParam = TerrainTag.ICE.name,
                effectType = InteractionEffect.SLIP,
                effectParams =
                    mapOf(
                        "statusId" to "STUN",
                        "durationTurns" to "1",
                    ),
            ),
        ).associateBy(ElementInteractionRule::id)

    fun all(): List<ElementInteractionRule> = rules.values.toList()

    fun resolve(
        request: DamageRequest,
        finalDamage: Int,
        terrainContext: TerrainInteractionContext,
    ): ElementInteractionResolution? {
        if (finalDamage <= 0 || terrainContext.interactionDepth >= MAX_INTERACTION_DEPTH) {
            return null
        }
        return all()
            .firstOrNull { rule -> matches(rule, request.damageType, terrainContext.targetTerrainTags) }
            ?.toResolution(
                request = request,
                finalDamage = finalDamage,
                terrainContext = terrainContext,
            )
    }

    private fun matches(
        rule: ElementInteractionRule,
        damageType: DamageType,
        terrainTags: Set<TerrainTag>,
    ): Boolean {
        if (rule.triggerElement != damageType) {
            return false
        }
        if (rule.conditionType != InteractionCondition.TARGET_ON_TERRAIN) {
            return false
        }
        return TerrainTag.valueOf(rule.conditionParam) in terrainTags
    }

    private fun ElementInteractionRule.toResolution(
        request: DamageRequest,
        finalDamage: Int,
        terrainContext: TerrainInteractionContext,
    ): ElementInteractionResolution =
        when (id) {
            TERRAIN_LIGHTNING_WATER_CHAIN -> {
                val bonusMultiplier = effectParams.requiredDouble("bonusTargetMultiplier")
                val chainMultiplier = effectParams.requiredDouble("chainDamageMultiplier")
                ElementInteractionResolution(
                    ruleId = id,
                    triggerDamageType = request.damageType,
                    bonusTargetTrace =
                        TerrainInteractionChildTrace(
                            traceId = "${request.traceId}:$id:bonus:${request.targetId.value}",
                            targetId = request.targetId,
                            rawDamage = (finalDamage * bonusMultiplier).toInt().coerceAtLeast(1),
                            damageMultiplier = bonusMultiplier,
                        ),
                    chainTargets =
                        terrainContext.adjacentTargets
                            .filter { target -> TerrainTag.WATER in target.terrainTags }
                            .map { target ->
                                TerrainInteractionChildTrace(
                                    traceId = "${request.traceId}:$id:chain:${target.entityId.value}",
                                    targetId = target.entityId,
                                    rawDamage = (finalDamage * chainMultiplier).toInt().coerceAtLeast(1),
                                    damageMultiplier = chainMultiplier,
                                )
                            },
                    interactionDepth = terrainContext.interactionDepth,
                )
            }

            TERRAIN_FIRE_OIL_IGNITE ->
                ElementInteractionResolution(
                    ruleId = id,
                    triggerDamageType = request.damageType,
                    terrainTransform =
                        TerrainTransformPlan(
                            targetTerrainTags = setOf(TerrainTag.OIL),
                            durationTurns = effectParams.requiredInt("durationTurns"),
                            tickDamageType = DamageType.valueOf(effectParams.getValue("tickDamageType")),
                            tickDamage = effectParams.requiredInt("tickDamage"),
                        ),
                    interactionDepth = terrainContext.interactionDepth,
                )

            TERRAIN_COLD_WATER_FREEZE ->
                ElementInteractionResolution(
                    ruleId = id,
                    triggerDamageType = request.damageType,
                    terrainTransform =
                        TerrainTransformPlan(
                            targetTerrainTags = setOf(TerrainTag.valueOf(effectParams.getValue("resultTerrain"))),
                            durationTurns = effectParams.requiredInt("durationTurns"),
                        ),
                    interactionDepth = terrainContext.interactionDepth,
                )

            TERRAIN_FIRE_ICE_MELT ->
                ElementInteractionResolution(
                    ruleId = id,
                    triggerDamageType = request.damageType,
                    removedStatusIds = listOf("FREEZE"),
                    terrainTransform =
                        TerrainTransformPlan(
                            targetTerrainTags = setOf(TerrainTag.valueOf(effectParams.getValue("resultTerrain"))),
                            durationTurns = effectParams.requiredInt("durationTurns"),
                            conductsLightning = true,
                        ),
                    interactionDepth = terrainContext.interactionDepth,
                )

            TERRAIN_PHYSICAL_ICE_SLIP ->
                ElementInteractionResolution(
                    ruleId = id,
                    triggerDamageType = request.damageType,
                    appliedStatusIds = listOf(effectParams.getValue("statusId")),
                    slip =
                        TerrainSlipPlan(
                            statusId = effectParams.getValue("statusId"),
                            durationTurns = effectParams.requiredInt("durationTurns"),
                        ),
                    interactionDepth = terrainContext.interactionDepth,
                )

            else -> null
        } ?: error("Unsupported element interaction rule: $id")

    private fun Map<String, String>.requiredInt(key: String): Int = getValue(key).toInt()

    private fun Map<String, String>.requiredDouble(key: String): Double = getValue(key).toDouble()
}
