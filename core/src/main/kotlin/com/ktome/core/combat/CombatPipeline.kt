package com.ktome.core.combat

import com.ktome.core.ecs.EntityId
import com.ktome.core.random.RandomSource
import java.util.Locale
import kotlin.math.roundToInt

enum class CombatCallbackPhase {
    PRE_HIT_CHECK,
    ON_MISS,
    PRE_CRIT_CHECK,
    PRE_DAMAGE_CALCULATION,
    PRE_DAMAGE_APPLY,
    POST_DAMAGE_REDUCTION,
    ON_DAMAGE_APPLIED,
    ON_DAMAGE_TAKEN,
    PRE_DEATH,
    ON_KILL,
}

enum class CallbackFlow {
    CONTINUE,
    CANCEL,
    ABSORB,
}

data class CallbackDecision(
    val flow: CallbackFlow = CallbackFlow.CONTINUE,
    val effect: String? = null,
)

class PipelineCallback(
    val ownerId: EntityId,
    val callbackName: String,
    val phase: CombatCallbackPhase,
    val priority: Int,
    val handler: (MutableCombatContext) -> CallbackDecision,
)

data class ElementalInteraction(
    val interactionId: String,
    val effect: String,
    val childTraceId: String? = null,
    val triggered: Boolean = true,
)

data class DamageRequest(
    val attackerId: EntityId = EntityId(-1),
    val targetId: EntityId = EntityId(-2),
    val abilityId: String = "basic_attack",
    val traceId: String = "combat-trace",
    val turn: Int = 0,
    val damageType: DamageType = DamageType.PHYSICAL,
    val baseDamage: Int = 0,
    val damageVariance: Int = 2,
    val attackerAccuracy: Int = 0,
    val targetEvasion: Int = 0,
    val attackerCritChance: Double = CritFormula.BASE_CRIT_RATE,
    val targetCritResistance: Double = 0.0,
    val critMultiplier: Double = CritFormula.BASE_CRIT_MULTIPLIER,
    val targetArmor: Int = 0,
    val targetResistance: Int = 0,
    val attackerArmorPenetration: Int = 0,
    val attackerResistancePenetration: Int = 0,
    val damageMultiplier: Double = 1.0,
    val targetCurrentHp: Int = 1,
    val shieldPoints: Int = 0,
    val statusApplication: StatusApplicationRequest? = null,
    val elementalInteraction: ElementalInteraction? = null,
    val terrainInteractionContext: TerrainInteractionContext? = null,
    val forceHit: Boolean = false,
    val callbacks: List<PipelineCallback> = emptyList(),
    val envelope: TraceEnvelope = CombatRuleset.formulaEnvelope(),
)

data class DamageOutcome(
    val packet: DamagePacket,
    val hit: Boolean,
    val critical: Boolean,
    val damage: DamageResult? = null,
    val targetKilled: Boolean = false,
    val trace: CombatResolutionTrace,
    val envelope: TraceEnvelope,
    val statusApplication: StatusApplicationResolution? = null,
    val terrainInteraction: ElementInteractionResolution? = null,
) {
    val finalDamage: Int
        get() = packet.finalDamage

    val reducedDamage: Int
        get() = packet.reducedDamage

    val rawDamage: Int
        get() = packet.rawDamage
}

class MutableCombatContext(
    val request: DamageRequest,
) {
    var accuracy: Int = request.attackerAccuracy
    var evasion: Int = request.targetEvasion
    var critChance: Double = request.attackerCritChance
    var critMultiplier: Double = request.critMultiplier
    var critical: Boolean = false
    var damageType: DamageType = request.damageType
    var rawDamage: Int = 0
    var reducedDamage: Int = 0
    var finalDamage: Int = 0
    var preReductionAbsorbed: Int = 0
    var mitigationValue: Int = 0
    var penetrationContribution: Int = 0
    var postReductionModifier: Int = 0
    var deathPrevented: Boolean = false
    var statusResolution: StatusApplicationResolution? = null
    var terrainInteraction: ElementInteractionResolution? = null
    val childTraceIds: MutableList<String> = mutableListOf()
}

class CombatPipeline(
    private val random: RandomSource,
) {
    fun resolve(
        request: DamageRequest,
        applyDamageHook: ((Int) -> Unit)? = null,
    ): DamageOutcome {
        val context = MutableCombatContext(request)
        val steps = mutableListOf<ResolutionStep>()

        val preHitCallbacks = invokeCallbacks(CombatCallbackPhase.PRE_HIT_CHECK, context)
        val effectiveEvasion = DiminishingReturns.effectiveEvasion(context.evasion)
        val hitChance = if (request.forceHit) 1.0 else HitFormula.sigmoidHitChance(context.accuracy.toDouble(), effectiveEvasion)
        val hitRoll = if (request.forceHit) 0.0 else random.nextDouble()
        val hit = request.forceHit || hitRoll < hitChance
        val hitFlags = linkedSetOf<String>()
        if (request.forceHit) {
            hitFlags += "FORCE_HIT"
        }
        if (!hit) {
            hitFlags += "MISS"
        }
        steps +=
            ResolutionStep(
                stepIndex = 1,
                stepName = "HIT_CHECK",
                inputs =
                    orderedMap(
                        "accuracy" to context.accuracy.asString(),
                        "rawEvasion" to context.evasion.asString(),
                        "effectiveEvasion" to effectiveEvasion.asString(),
                        "hitFormula" to "SIGMOID",
                    ),
                outputs =
                    orderedMap(
                        "hitChance" to hitChance.asString(),
                        "roll" to hitRoll.asString(),
                        "hit" to hit.asString(),
                        "forceHit" to request.forceHit.asString(),
                    ),
                flags = hitFlags,
                callbacks = preHitCallbacks.records,
            )
        if (!hit) {
            val missCallbacks = invokeCallbacks(CombatCallbackPhase.ON_MISS, context)
            steps +=
                ResolutionStep(
                    stepIndex = 2,
                    stepName = "MISS_CALLBACKS",
                    inputs =
                        orderedMap(
                            "hit" to false.asString(),
                        ),
                    outputs =
                        orderedMap(
                            "missCallbacksTriggered" to true.asString(),
                        ),
                    flags = linkedSetOf("MISS"),
                    callbacks = missCallbacks.records,
                )
            resolveStatusIfAbsent(
                statusRequest = request.statusApplication,
                context = context,
                hitSucceeded = false,
            )
            return terminalOutcome(
                request = request,
                context = context,
                steps = steps,
                targetKilled = false,
                rawDamage = 0,
                armorResistanceReduced = 0,
            )
        }

        val preCritCallbacks = invokeCallbacks(CombatCallbackPhase.PRE_CRIT_CHECK, context)
        val critRoll =
            CritFormula.rollCrit(
                random = random,
                baseCritRate = context.critChance,
                critResistance = request.targetCritResistance,
            )
        context.critical = critRoll.isCritical
        if (!context.critical) {
            context.critMultiplier = 1.0
        }
        steps +=
            ResolutionStep(
                stepIndex = 2,
                stepName = "CRIT_CHECK",
                inputs =
                    orderedMap(
                        "critChance" to context.critChance.asString(),
                        "critResistance" to request.targetCritResistance.asString(),
                    ),
                outputs =
                    orderedMap(
                        "effectiveCritChance" to critRoll.effectiveCritChance.asString(),
                        "roll" to critRoll.roll.asString(),
                        "critical" to context.critical.asString(),
                        "critMultiplier" to context.critMultiplier.asString(),
                    ),
                callbacks = preCritCallbacks.records,
            )

        val varianceRoll = rollDamageVariance(request.damageVariance)
        val rolledDamage = (request.baseDamage + varianceRoll).coerceAtLeast(0)
        var computedRawDamage = rolledDamage
        if (context.critical) {
            computedRawDamage = (computedRawDamage * context.critMultiplier).roundToInt().coerceAtLeast(1)
        }
        computedRawDamage = (computedRawDamage * request.damageMultiplier).roundToInt().coerceAtLeast(0)
        context.rawDamage = computedRawDamage
        val preDamageCallbacks = invokeCallbacks(CombatCallbackPhase.PRE_DAMAGE_CALCULATION, context)
        context.rawDamage = context.rawDamage.coerceAtLeast(0)
        val rawDamageBeforeShield = context.rawDamage
        steps +=
            ResolutionStep(
                stepIndex = 3,
                stepName = "RAW_DAMAGE_CALCULATION",
                inputs =
                    orderedMap(
                        "baseDamage" to request.baseDamage.asString(),
                        "varianceRoll" to varianceRoll.asString(),
                        "damageMultiplier" to request.damageMultiplier.asString(),
                        "critical" to context.critical.asString(),
                    ),
                outputs =
                    orderedMap(
                        "rolledDamage" to rolledDamage.asString(),
                        "rawDamage" to context.rawDamage.asString(),
                    ),
                callbacks = preDamageCallbacks.records,
            )

        val preReductionCallbacks = invokeCallbacks(CombatCallbackPhase.PRE_DAMAGE_APPLY, context)
        if (preReductionCallbacks.flow == CallbackFlow.ABSORB) {
            context.rawDamage = 0
        }
        val absorbedByShield = context.rawDamage.coerceAtLeast(0).coerceAtMost(request.shieldPoints.coerceAtLeast(0))
        context.preReductionAbsorbed = absorbedByShield
        context.rawDamage = (context.rawDamage - absorbedByShield).coerceAtLeast(0)
        val preReductionFlags = linkedSetOf<String>()
        if (absorbedByShield > 0) {
            preReductionFlags += "SHIELD_ABSORB"
        }
        if (preReductionCallbacks.flow == CallbackFlow.ABSORB) {
            preReductionFlags += "ABSORB"
        }
        steps +=
            ResolutionStep(
                stepIndex = 4,
                stepName = "PRE_REDUCTION_CALLBACKS",
                inputs =
                    orderedMap(
                        "incomingRawDamage" to rawDamageBeforeShield.asString(),
                        "shieldPoints" to request.shieldPoints.asString(),
                    ),
                outputs =
                    orderedMap(
                        "damageType" to context.damageType.name,
                        "absorbedByShield" to absorbedByShield.asString(),
                        "remainingRawDamage" to context.rawDamage.asString(),
                    ),
                flags = preReductionFlags,
                callbacks = preReductionCallbacks.records,
            )
        if (context.rawDamage == 0) {
            resolveStatusIfAbsent(
                statusRequest = request.statusApplication,
                context = context,
                hitSucceeded = true,
            )
            return terminalOutcome(
                request = request,
                context = context,
                steps = steps,
                targetKilled = false,
                rawDamage = rawDamageBeforeShield,
                armorResistanceReduced = 0,
            )
        }

        val reduction =
            DamageFormula.reduceDamage(
                rawDamage = context.rawDamage,
                damageType = context.damageType,
                targetArmor = request.targetArmor,
                targetResistance = request.targetResistance,
                penetration =
                    if (context.damageType == DamageType.PHYSICAL) {
                        request.attackerArmorPenetration
                    } else {
                        request.attackerResistancePenetration
                    },
            )
        context.reducedDamage = reduction.reducedDamage
        context.mitigationValue = reduction.reductionValue
        context.penetrationContribution = reduction.penetrationContribution
        val armorResistanceReduced = context.rawDamage - context.reducedDamage
        val mitigationKey = if (context.damageType == DamageType.PHYSICAL) "effectiveArmor" else "effectiveResistance"
        steps +=
            ResolutionStep(
                stepIndex = 5,
                stepName = "ARMOR_RESISTANCE_REDUCTION",
                inputs =
                    orderedMap(
                        "damageType" to context.damageType.name,
                        "rawDamage" to context.rawDamage.asString(),
                        "targetArmor" to request.targetArmor.asString(),
                        "targetResistance" to request.targetResistance.asString(),
                    ),
                outputs =
                    orderedMap(
                        mitigationKey to context.mitigationValue.asString(),
                        "reducedDamage" to context.reducedDamage.asString(),
                    ),
            )

        steps +=
            ResolutionStep(
                stepIndex = 6,
                stepName = "PENETRATION_APPLICATION",
                inputs =
                    orderedMap(
                        "armorPenetration" to request.attackerArmorPenetration.asString(),
                        "resistancePenetration" to request.attackerResistancePenetration.asString(),
                    ),
                outputs =
                    orderedMap(
                        "penetrationContribution" to context.penetrationContribution.asString(),
                    ),
            )

        val reducedBeforeCallbacks = context.reducedDamage
        val postReductionCallbacks = invokeCallbacks(CombatCallbackPhase.POST_DAMAGE_REDUCTION, context)
        if (postReductionCallbacks.flow == CallbackFlow.ABSORB) {
            context.reducedDamage = 0
        }
        context.reducedDamage = context.reducedDamage.coerceAtLeast(0)
        context.postReductionModifier = context.reducedDamage - reducedBeforeCallbacks
        val postReductionFlags = linkedSetOf<String>()
        if (postReductionCallbacks.flow == CallbackFlow.ABSORB) {
            postReductionFlags += "ABSORB"
        }
        steps +=
            ResolutionStep(
                stepIndex = 7,
                stepName = "POST_REDUCTION_CALLBACKS",
                inputs =
                    orderedMap(
                        "reducedDamageBeforeCallbacks" to reducedBeforeCallbacks.asString(),
                    ),
                outputs =
                    orderedMap(
                        "postReductionModifier" to context.postReductionModifier.asString(),
                        "reducedDamage" to context.reducedDamage.asString(),
                    ),
                flags = postReductionFlags,
                callbacks = postReductionCallbacks.records,
            )
        if (postReductionCallbacks.flow == CallbackFlow.ABSORB) {
            resolveStatusIfAbsent(
                statusRequest = request.statusApplication,
                context = context,
                hitSucceeded = true,
            )
            return terminalOutcome(
                request = request,
                context = context,
                steps = steps,
                targetKilled = false,
                rawDamage = rawDamageBeforeShield,
                armorResistanceReduced = armorResistanceReduced,
            )
        }

        context.finalDamage = maxOf(1, context.reducedDamage)
        val remainingHp = (request.targetCurrentHp - context.finalDamage).coerceAtLeast(0)
        applyDamageHook?.invoke(remainingHp)
        val onDamageAppliedCallbacks = invokeCallbacks(CombatCallbackPhase.ON_DAMAGE_APPLIED, context)
        steps +=
            ResolutionStep(
                stepIndex = 8,
                stepName = "FINAL_DAMAGE_APPLICATION",
                inputs =
                    orderedMap(
                        "targetCurrentHp" to request.targetCurrentHp.asString(),
                        "damageBeforeClamp" to context.reducedDamage.asString(),
                    ),
                outputs =
                    orderedMap(
                        "finalDamage" to context.finalDamage.asString(),
                        "remainingHp" to remainingHp.asString(),
                    ),
                callbacks = onDamageAppliedCallbacks.records,
            )

        val onDamageTakenCallbacks = invokeCallbacks(CombatCallbackPhase.ON_DAMAGE_TAKEN, context)
        resolveStatusIfAbsent(
            statusRequest = request.statusApplication,
            context = context,
            hitSucceeded = true,
        )
        context.terrainInteraction =
            request.terrainInteractionContext?.let { terrainContext ->
                ElementInteractionRegistry.resolve(
                    request = request,
                    finalDamage = context.finalDamage,
                    terrainContext = terrainContext,
                )
            }
        context.terrainInteraction?.childTraceIds?.forEach(context.childTraceIds::add)
        request.elementalInteraction
            ?.takeIf(ElementalInteraction::triggered)
            ?.childTraceId
            ?.let(context.childTraceIds::add)
        val onDamageFlags = linkedSetOf<String>()
        if (request.elementalInteraction?.triggered == true) {
            onDamageFlags += "ELEMENTAL_INTERACTION"
        }
        if (context.terrainInteraction != null) {
            onDamageFlags += "TERRAIN_INTERACTION"
        }
        steps +=
            ResolutionStep(
                stepIndex = 9,
                stepName = "ON_DAMAGE_TAKEN_CALLBACKS",
                inputs =
                    orderedMap(
                        "finalDamage" to context.finalDamage.asString(),
                    ),
                outputs =
                    orderedMap(
                        "statusApplied" to context.statusResolution?.applied?.asString(),
                        "statusReason" to context.statusResolution?.reasonTag,
                        "elementalInteractionId" to request.elementalInteraction?.interactionId,
                        "childTraceId" to request.elementalInteraction?.childTraceId,
                        "terrainInteractionRuleId" to context.terrainInteraction?.ruleId,
                        "terrainInteractionChildTraceIds" to context.terrainInteraction?.childTraceIds?.joinToString(","),
                        "terrainInteractionRemovedStatusIds" to context.terrainInteraction?.removedStatusIds?.joinToString(","),
                        "terrainInteractionAppliedStatusIds" to context.terrainInteraction?.appliedStatusIds?.joinToString(","),
                    ),
                flags = onDamageFlags,
                callbacks = onDamageTakenCallbacks.records,
            )

        val wouldKill = remainingHp == 0
        val preDeathCallbacks =
            if (wouldKill) {
                invokeCallbacks(CombatCallbackPhase.PRE_DEATH, context)
            } else {
                CallbackBatchResult()
            }
        val targetKilled = wouldKill && !context.deathPrevented
        steps +=
            ResolutionStep(
                stepIndex = 10,
                stepName = "DEATH_CHECK",
                inputs =
                    orderedMap(
                        "wouldKill" to wouldKill.asString(),
                    ),
                outputs =
                    orderedMap(
                        "deathPrevented" to context.deathPrevented.asString(),
                        "targetKilled" to targetKilled.asString(),
                    ),
                callbacks = preDeathCallbacks.records,
            )

        val onKillCallbacks =
            if (targetKilled) {
                invokeCallbacks(CombatCallbackPhase.ON_KILL, context)
            } else {
                CallbackBatchResult()
            }
        steps +=
            ResolutionStep(
                stepIndex = 11,
                stepName = "ON_KILL_CALLBACKS",
                inputs =
                    orderedMap(
                        "targetKilled" to targetKilled.asString(),
                    ),
                outputs =
                    orderedMap(
                        "killCallbacksTriggered" to targetKilled.asString(),
                    ),
                callbacks = onKillCallbacks.records,
            )

        steps +=
            ResolutionStep(
                stepIndex = 12,
                stepName = "EXPERIENCE_AND_LOOT",
                inputs =
                    orderedMap(
                        "targetKilled" to targetKilled.asString(),
                    ),
                outputs =
                    orderedMap(
                        "experienceGranted" to false.asString(),
                        "lootGenerated" to false.asString(),
                    ),
                flags = linkedSetOf("NO_RUNTIME_LOOT"),
            )

        return terminalOutcome(
            request = request,
            context = context,
            steps = steps,
            targetKilled = targetKilled,
            rawDamage = rawDamageBeforeShield,
            armorResistanceReduced = armorResistanceReduced,
        )
    }

    private fun terminalOutcome(
        request: DamageRequest,
        context: MutableCombatContext,
        steps: List<ResolutionStep>,
        targetKilled: Boolean,
        rawDamage: Int,
        armorResistanceReduced: Int,
    ): DamageOutcome {
        val remainingHpAfterApply =
            when {
                context.finalDamage <= 0 -> request.targetCurrentHp
                context.deathPrevented && request.targetCurrentHp <= context.finalDamage -> 1
                else -> (request.targetCurrentHp - context.finalDamage).coerceAtLeast(0)
            }
        val packet =
            DamagePacket(
                request = request,
                hitConfirmed = steps.firstOrNull()?.outputs?.get("hit")?.toBooleanStrictOrNull() ?: false,
                critical = context.critical,
                damageType = context.damageType,
                rawDamage = rawDamage,
                reducedDamage = context.reducedDamage,
                finalDamage = context.finalDamage,
                remainingHpAfterApply = remainingHpAfterApply,
                mitigationValue = context.mitigationValue,
                preReductionAbsorbed = context.preReductionAbsorbed,
                penetrationContribution = context.penetrationContribution,
                postReductionModifier = context.postReductionModifier,
            )
        val trace =
            CombatResolutionTrace(
                traceId = request.traceId,
                turn = request.turn,
                attackerId = request.attackerId,
                targetId = request.targetId,
                abilityId = request.abilityId,
                damageType = context.damageType,
                steps = steps,
                result =
                    ResolutionResult(
                        hit = packet.hitConfirmed,
                        critical = context.critical,
                        critMultiplier = if (context.critical) context.critMultiplier else 1.0,
                        rawDamage = rawDamage,
                        preReductionAbsorbed = context.preReductionAbsorbed,
                        armorResistanceReduced = armorResistanceReduced,
                        penetrationContribution = context.penetrationContribution,
                        postReductionModifier = context.postReductionModifier,
                        finalDamage = context.finalDamage,
                        targetKilled = targetKilled,
                        deathPrevented = context.deathPrevented,
                    ),
                childTraceIds = context.childTraceIds.toList(),
            )
        val damageResult =
            if (packet.hitConfirmed) {
                DamageResult(
                    type = packet.damageType,
                    rawDamage = packet.rawDamage,
                    reducedDamage = packet.reducedDamage,
                    finalDamage = packet.finalDamage,
                    resistanceValue = if (packet.damageType.isElemental) packet.mitigationValue else 0,
                    preReductionAbsorbed = packet.preReductionAbsorbed,
                    penetrationContribution = packet.penetrationContribution,
                )
            } else {
                null
            }
        return DamageOutcome(
            packet = packet,
            hit = packet.hitConfirmed,
            critical = context.critical,
            damage = damageResult,
            targetKilled = targetKilled,
            trace = trace,
            envelope = request.envelope,
            statusApplication = context.statusResolution,
            terrainInteraction = context.terrainInteraction,
        )
    }

    private fun invokeCallbacks(
        phase: CombatCallbackPhase,
        context: MutableCombatContext,
    ): CallbackBatchResult {
        val records = mutableListOf<CallbackRecord>()
        var flow = CallbackFlow.CONTINUE
        val orderedCallbacks =
            context.request.callbacks
                .asSequence()
                .filter { callback -> callback.phase == phase }
                .sortedWith(compareBy<PipelineCallback> { callback -> callback.priority }.thenBy { callback -> callback.ownerId.value })
                .toList()
        for (callback in orderedCallbacks) {
            val decision = callback.handler.invoke(context)
            records +=
                CallbackRecord(
                    ownerId = callback.ownerId,
                    callbackName = callback.callbackName,
                    priority = callback.priority,
                    result = decision.flow.name,
                    effect = decision.effect,
                )
            if (decision.flow == CallbackFlow.CANCEL || decision.flow == CallbackFlow.ABSORB) {
                flow = decision.flow
                break
            }
        }
        return CallbackBatchResult(
            flow = flow,
            records = records,
        )
    }

    private fun rollDamageVariance(variance: Int): Int {
        if (variance <= 0) {
            return 0
        }
        return random.nextInt(-variance, variance + 1)
    }

    private fun resolveStatusIfAbsent(
        statusRequest: StatusApplicationRequest?,
        context: MutableCombatContext,
        hitSucceeded: Boolean,
    ) {
        if (context.statusResolution != null || statusRequest == null) {
            return
        }
        context.statusResolution =
            ApplicationPolicyResolver.resolve(
                request = statusRequest,
                hitSucceeded = hitSucceeded,
                random = random,
            )
    }

    private data class CallbackBatchResult(
        val flow: CallbackFlow = CallbackFlow.CONTINUE,
        val records: List<CallbackRecord> = emptyList(),
    )

    private fun orderedMap(vararg entries: Pair<String, String?>): Map<String, String> =
        linkedMapOf<String, String>().apply {
            entries.forEach { (key, value) ->
                if (value != null) {
                    put(key, value)
                }
            }
        }

    private fun Int.asString(): String = toString()

    private fun Boolean.asString(): String = toString()

    private fun Double.asString(): String = String.format(Locale.US, "%.4f", this)
}
