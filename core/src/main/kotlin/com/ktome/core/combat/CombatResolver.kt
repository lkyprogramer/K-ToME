package com.ktome.core.combat

import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.random.RandomSource
import com.ktome.core.stats.StatsCalculator

class CombatResolver(
    private val random: RandomSource,
) {
    fun resolveMelee(
        attackerAttack: Int,
        attackerAccuracy: Int,
        attackerDex: Int,
        targetDefense: Int,
        targetEvasion: Int,
        targetCurrentHp: Int,
        damageMultiplier: Double = 1.0,
    ): CombatResult {
        val hitChance = (0.85 + (attackerAccuracy - targetEvasion) * 0.01).coerceIn(0.05, 0.95)
        if (random.nextDouble() > hitChance) {
            return CombatResult(hit = false, crit = false)
        }

        val critChance = (0.05 + attackerDex * 0.002).coerceIn(0.0, 0.50)
        val crit = random.nextDouble() < critChance
        val rawDamage = attackerAttack + random.nextInt(-2, 3)
        val reducedDamage = maxOf(0, rawDamage - targetDefense)
        var finalDamage = maxOf(1, reducedDamage)
        if (crit) {
            finalDamage = maxOf(1, (finalDamage * 1.5).toInt())
        }
        finalDamage = maxOf(1, (finalDamage * damageMultiplier).toInt())

        return CombatResult(
            hit = true,
            crit = crit,
            damage = DamageResult(
                rawDamage = rawDamage,
                reducedDamage = reducedDamage,
                finalDamage = finalDamage,
            ),
            targetKilled = targetCurrentHp - finalDamage <= 0,
        )
    }

    fun resolveMelee(
        world: World,
        attacker: EntityId,
        target: EntityId,
        damageMultiplier: Double = 1.0,
    ): CombatResult {
        val attackerStats = requireNotNull(world.get<Stats>(attacker)) { "Missing Stats for $attacker" }
        val targetHealth = requireNotNull(world.get<Health>(target)) { "Missing Health for $target" }

        val attackerDerived = StatsCalculator.calculate(world, attacker)
        val targetDerived = StatsCalculator.calculate(world, target)

        return resolveMelee(
            attackerAttack = attackerDerived.attack,
            attackerAccuracy = attackerDerived.accuracy,
            attackerDex = attackerStats.dex,
            targetDefense = targetDerived.defense,
            targetEvasion = targetDerived.evasion,
            targetCurrentHp = targetHealth.current,
            damageMultiplier = damageMultiplier,
        )
    }
}
