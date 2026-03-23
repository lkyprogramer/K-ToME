package com.ktome.core.combat

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.ResistanceProfile
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.random.RandomSource
import com.ktome.core.stats.StatsCalculator

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

        val attackerDerived = StatsCalculator.calculate(world, attacker)
        val targetDerived = StatsCalculator.calculate(world, target)
        val targetResistance = world.get<ResistanceProfile>(target)?.valueFor(damageType) ?: 0
        val resolvedStatusApplication = resolveWorldStatusRequest(attackerDerived, targetDerived, statusApplication)
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
                    callbacks = callbacks,
                ),
                applyDamageHook = { remainingHp -> targetHealth.current = remainingHp.coerceAtLeast(0) },
            )
        applyDamageOutcome(targetHealth, outcome)
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
        targetHealth.current = outcome.packet.remainingHpAfterApply.coerceAtLeast(0)
    }
}
