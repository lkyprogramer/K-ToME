package com.ktome.core.combat

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.ResistanceProfile
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.random.RandomSource
import com.ktome.core.stats.StatsCalculator
import com.ktome.core.talent.EffectTracker
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.status.StatusEffectType
import kotlin.math.roundToInt

class CombatResolver(
    private val random: RandomSource,
) {
    private val pipeline = CombatPipeline(random)

    fun resolveMelee(
        attackerAttack: Int,
        attackerAccuracy: Int,
        attackerDex: Int,
        targetDefense: Int,
        targetEvasion: Int,
        targetCurrentHp: Int,
        damageType: DamageType = DamageType.PHYSICAL,
        targetResistance: Int = 0,
        damageMultiplier: Double = 1.0,
        abilityId: String = "melee_attack",
        traceId: String = "legacy-melee",
        statusApplication: StatusApplicationRequest? = null,
        callbacks: List<PipelineCallback> = emptyList(),
    ): CombatResult {
        val outcome =
            pipeline.resolve(
                DamageRequest(
                    abilityId = abilityId,
                    traceId = traceId,
                    damageType = damageType,
                    baseDamage = attackerAttack,
                    attackerAccuracy = attackerAccuracy,
                    targetEvasion = targetEvasion,
                    attackerCritChance = CritFormula.BASE_CRIT_RATE + attackerDex * 0.002,
                    targetArmor = targetDefense,
                    targetResistance = targetResistance,
                    damageMultiplier = damageMultiplier,
                    targetCurrentHp = targetCurrentHp,
                    statusApplication = statusApplication,
                    callbacks = callbacks,
                ),
            )
        return CombatResult(
            hit = outcome.hit,
            crit = outcome.critical,
            packet = outcome.packet,
            damage = outcome.damage,
            targetKilled = outcome.targetKilled,
            trace = outcome.trace,
            envelope = outcome.envelope,
            statusApplication = outcome.statusApplication,
        )
    }

    fun resolveMelee(
        world: World,
        attacker: EntityId,
        target: EntityId,
        damageType: DamageType = DamageType.PHYSICAL,
        damageMultiplier: Double = 1.0,
        abilityId: String = "melee_attack",
        statusApplication: StatusApplicationRequest? = null,
        callbacks: List<PipelineCallback> = emptyList(),
    ): CombatResult {
        val targetHealth = requireNotNull(world.get<Health>(target)) { "Missing Health for $target" }
        val targetEffects = world.get<EffectTracker>(target)

        val attackerDerived = StatsCalculator.calculate(world, attacker)
        val targetDerived = StatsCalculator.calculate(world, target)
        val targetResistance = world.get<ResistanceProfile>(target)?.valueFor(damageType) ?: 0
        val resolvedStatusApplication = resolveWorldStatusRequest(attackerDerived, targetDerived, statusApplication)
        val statusCallbacks = buildStatusCallbacks(target, damageType, targetEffects)
        val outcome =
            pipeline.resolve(
                DamageRequest(
                    attackerId = attacker,
                    targetId = target,
                    abilityId = abilityId,
                    traceId = "${abilityId}:${attacker.value}:${target.value}:${damageType.name}",
                    damageType = damageType,
                    baseDamage = attackerDerived.attack,
                    attackerAccuracy = attackerDerived.accuracy,
                    targetEvasion = targetDerived.evasion,
                    attackerCritChance = attackerDerived.critChance,
                    targetCritResistance = targetDerived.critResistance,
                    targetArmor = targetDerived.defense,
                    targetResistance = targetResistance,
                    damageMultiplier = damageMultiplier,
                    targetCurrentHp = targetHealth.current,
                    statusApplication = resolvedStatusApplication,
                    callbacks = callbacks + statusCallbacks,
                ),
                applyDamageHook = { remainingHp -> targetHealth.current = remainingHp.coerceAtLeast(0) },
            )
        applyDamageOutcome(targetHealth, outcome)
        var removedStatusTypes: Set<StatusEffectType> = emptySet()
        if (targetEffects != null && outcome.hit && outcome.finalDamage > 0) {
            val removedOvercharge = StatusLifecycle.consumeOnDamage(targetEffects, damageType, outcome.finalDamage)
            val removedStealth = StatusLifecycle.breakOnDamage(targetEffects, outcome.finalDamage)
            removedStatusTypes = (removedOvercharge + removedStealth).mapTo(linkedSetOf()) { effect -> effect.type }
            if (removedOvercharge.isNotEmpty() || removedStealth.isNotEmpty()) {
                StatsCalculator.recalculateAndStore(world, target)
            }
        }
        return CombatResult(
            hit = outcome.hit,
            crit = outcome.critical,
            packet = outcome.packet,
            damage = outcome.damage,
            targetKilled = outcome.targetKilled,
            trace = outcome.trace,
            envelope = outcome.envelope,
            statusApplication = outcome.statusApplication,
            removedStatusTypes = removedStatusTypes,
        )
    }

    fun resolveStatusApplication(
        world: World,
        attacker: EntityId,
        target: EntityId,
        request: StatusApplicationRequest,
        hitSucceeded: Boolean = false,
    ): StatusApplicationResolution {
        val attackerDerived = StatsCalculator.calculate(world, attacker)
        val targetDerived = StatsCalculator.calculate(world, target)
        val resolvedRequest =
            requireNotNull(resolveWorldStatusRequest(attackerDerived, targetDerived, request)) {
                "Status application request cannot be null."
            }
        return ApplicationPolicyResolver.resolve(
            request = resolvedRequest,
            hitSucceeded = hitSucceeded,
            random = random,
        )
    }

    fun resolveStatusTick(
        world: World,
        source: EntityId?,
        target: EntityId,
        statusType: StatusEffectType,
        damageType: DamageType,
        rawDamage: Int,
        turn: Int = 0,
        traceId: String = "status-tick",
    ): StatusTickResult {
        val targetHealth = requireNotNull(world.get<Health>(target)) { "Missing Health for $target" }
        val targetDerived = StatsCalculator.calculate(world, target)
        val targetResistance = world.get<ResistanceProfile>(target)?.valueFor(damageType) ?: 0
        val reduction =
            DamageFormula.reduceDamage(
                rawDamage = rawDamage,
                damageType = damageType,
                targetArmor = if (damageType == DamageType.PHYSICAL) targetDerived.defense else 0,
                targetResistance = targetResistance,
                penetration = 0,
            )
        val finalDamage = maxOf(1, reduction.reducedDamage)
        targetHealth.current = (targetHealth.current - finalDamage).coerceAtLeast(0)
        val targetKilled = targetHealth.current <= 0
        val trace =
            CombatResolutionTrace(
                traceId = traceId,
                turn = turn,
                attackerId = source ?: EntityId(-1),
                targetId = target,
                abilityId = "status_tick:${statusType.schemaId.lowercase()}",
                damageType = damageType,
                steps =
                    listOf(
                        ResolutionStep(
                            stepIndex = 1,
                            stepName = "TURN_START_TICK",
                            inputs =
                                orderedMap(
                                    "statusType" to statusType.schemaId,
                                    "rawDamage" to rawDamage.toString(),
                                ),
                            outputs =
                                orderedMap(
                                    "damageType" to damageType.name,
                                ),
                        ),
                        ResolutionStep(
                            stepIndex = 2,
                            stepName = "ARMOR_RESISTANCE_REDUCTION",
                            inputs =
                                orderedMap(
                                    "targetArmor" to targetDerived.defense.toString(),
                                    "targetResistance" to targetResistance.toString(),
                                ),
                            outputs =
                                orderedMap(
                                    "mitigationValue" to reduction.reductionValue.toString(),
                                    "reducedDamage" to reduction.reducedDamage.toString(),
                                ),
                        ),
                        ResolutionStep(
                            stepIndex = 3,
                            stepName = "FINAL_DAMAGE_APPLICATION",
                            inputs =
                                orderedMap(
                                    "targetCurrentHp" to (targetHealth.current + finalDamage).toString(),
                                ),
                            outputs =
                                orderedMap(
                                    "finalDamage" to finalDamage.toString(),
                                    "remainingHp" to targetHealth.current.toString(),
                                ),
                        ),
                    ),
                result =
                    ResolutionResult(
                        hit = true,
                        rawDamage = rawDamage,
                        armorResistanceReduced = rawDamage - reduction.reducedDamage,
                        finalDamage = finalDamage,
                        targetKilled = targetKilled,
                    ),
            )
        return StatusTickResult(
            damage =
                DamageResult(
                    type = damageType,
                    rawDamage = rawDamage,
                    reducedDamage = reduction.reducedDamage,
                    finalDamage = finalDamage,
                    resistanceValue = reduction.reductionValue,
                ),
            targetKilled = targetKilled,
            trace = trace,
            envelope = CombatRuleset.statusEnvelope(),
        )
    }

    private fun resolveWorldStatusRequest(
        attackerDerived: com.ktome.core.ecs.DerivedStats,
        targetDerived: com.ktome.core.ecs.DerivedStats,
        request: StatusApplicationRequest?,
    ): StatusApplicationRequest? {
        if (request == null || !request.applicationPolicy.requiresSave()) {
            return request
        }
        val saveDimension =
            requireNotNull(request.saveDimension) {
                "StatusApplicationRequest ${request.statusId} requires saveDimension for ${request.applicationPolicy}."
            }
        return request.copy(
            power = PowerSaveFormula.powerFor(attackerDerived.powerSave, saveDimension),
            save = PowerSaveFormula.saveFor(targetDerived.powerSave, saveDimension),
        )
    }

    private fun applyDamageOutcome(
        targetHealth: Health,
        outcome: DamageOutcome,
    ) {
        if (!outcome.hit || outcome.finalDamage <= 0) {
            return
        }
        if (outcome.packet.remainingHpAfterApply > 0 && targetHealth.current <= 0) {
            targetHealth.current = outcome.packet.remainingHpAfterApply
        }
    }

    private fun buildStatusCallbacks(
        target: EntityId,
        damageType: DamageType,
        tracker: EffectTracker?,
    ): List<PipelineCallback> {
        if (tracker == null) {
            return emptyList()
        }
        val callbacks = mutableListOf<PipelineCallback>()
        if (StatusLifecycle.hasInvulnerable(tracker)) {
            callbacks +=
                PipelineCallback(
                    ownerId = target,
                    callbackName = "invulnerable_absorb",
                    phase = CombatCallbackPhase.PRE_DAMAGE_APPLY,
                    priority = 300,
                ) { context ->
                    context.rawDamage = 0
                    CallbackDecision(effect = "INVULNERABLE")
                }
        }
        val multiplier = StatusLifecycle.incomingDamageMultiplier(tracker, damageType)
        if (multiplier > 1.0) {
            callbacks +=
                PipelineCallback(
                    ownerId = target,
                    callbackName = "overcharge_vulnerability",
                    phase = CombatCallbackPhase.PRE_DAMAGE_APPLY,
                    priority = 301,
                ) { context ->
                    context.rawDamage = (context.rawDamage * multiplier).roundToInt().coerceAtLeast(0)
                    CallbackDecision(effect = "OVERCHARGE")
                }
        }
        return callbacks
    }

    private fun orderedMap(vararg entries: Pair<String, String?>): Map<String, String> =
        linkedMapOf<String, String>().apply {
            entries.forEach { (key, value) ->
                if (value != null) {
                    put(key, value)
                }
            }
        }
}
