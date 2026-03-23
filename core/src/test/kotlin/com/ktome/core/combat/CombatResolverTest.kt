package com.ktome.core.combat

import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.random.RandomSource
import com.ktome.core.support.TestRandomSource
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CombatResolverTest {
    @Test
    fun highAttackVsLowDefense() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)))

        val result = resolver.resolveMelee(
            attackerAttack = 12,
            attackerAccuracy = 20,
            attackerDex = 10,
            targetDefense = 2,
            targetEvasion = 0,
            targetCurrentHp = 20,
        )

        assertTrue(result.hit)
        assertFalse(result.crit)
        assertNotNull(result.damage)
        assertEquals(12, result.damage?.finalDamage)
    }

    @Test
    fun lowAttackVsHighDefense() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(-2)))

        val result = resolver.resolveMelee(
            attackerAttack = 3,
            attackerAccuracy = 20,
            attackerDex = 10,
            targetDefense = 20,
            targetEvasion = 0,
            targetCurrentHp = 10,
        )

        assertEquals(1, result.damage?.finalDamage)
    }

    @Test
    fun critDamageIs150Percent() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.0), ints = listOf(0)))

        val result = resolver.resolveMelee(
            attackerAttack = 10,
            attackerAccuracy = 20,
            attackerDex = 20,
            targetDefense = 0,
            targetEvasion = 0,
            targetCurrentHp = 20,
        )

        assertTrue(result.crit)
        assertEquals(15, result.damage?.finalDamage)
    }

    @Test
    fun guaranteedMinHitChance() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.049, 0.99), ints = listOf(0)))

        val result = resolver.resolveMelee(
            attackerAttack = 10,
            attackerAccuracy = 0,
            attackerDex = 10,
            targetDefense = 0,
            targetEvasion = 500,
            targetCurrentHp = 20,
        )

        assertTrue(result.hit)
    }

    @Test
    fun guaranteedMaxHitChance() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.96)))

        val result = resolver.resolveMelee(
            attackerAttack = 10,
            attackerAccuracy = 500,
            attackerDex = 10,
            targetDefense = 0,
            targetEvasion = 0,
            targetCurrentHp = 20,
        )

        assertFalse(result.hit)
    }

    @Test
    fun targetKilledWhenHpDepleted() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(2)))

        val result = resolver.resolveMelee(
            attackerAttack = 12,
            attackerAccuracy = 20,
            attackerDex = 10,
            targetDefense = 0,
            targetEvasion = 0,
            targetCurrentHp = 10,
        )

        assertTrue(result.targetKilled)
    }

    @Test
    fun deterministicWithSameRng() {
        val first = CombatResolver(RandomSource.from(Random(1234)))
        val second = CombatResolver(RandomSource.from(Random(1234)))

        val firstResult = first.resolveMelee(10, 15, 12, 3, 6, 20)
        val secondResult = second.resolveMelee(10, 15, 12, 3, 6, 20)

        assertEquals(firstResult, secondResult)
    }

    @Test
    fun `elemental damage is reduced by resistance and carries its type`() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)))

        val result =
            resolver.resolveMelee(
                attackerAttack = 12,
                attackerAccuracy = 20,
                attackerDex = 10,
                targetDefense = 99,
                targetEvasion = 0,
                targetCurrentHp = 20,
                damageType = DamageType.FIRE,
                targetResistance = 50,
            )

        assertTrue(result.hit)
        assertEquals(DamageType.FIRE, result.damage?.type)
        assertEquals(6, result.damage?.finalDamage)
        assertEquals(50, result.damage?.resistanceValue)
    }

    @Test
    fun `physical damage ignores elemental resistance and still uses armor`() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)))

        val result =
            resolver.resolveMelee(
                attackerAttack = 12,
                attackerAccuracy = 20,
                attackerDex = 10,
                targetDefense = 4,
                targetEvasion = 0,
                targetCurrentHp = 20,
                damageType = DamageType.PHYSICAL,
                targetResistance = 75,
            )

        assertTrue(result.hit)
        assertEquals(DamageType.PHYSICAL, result.damage?.type)
        assertEquals(12, result.damage?.finalDamage)
        assertEquals(0, result.damage?.resistanceValue)
    }

    @Test
    fun `elemental vulnerability increases final damage and preserves negative resistance`() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)))

        val result =
            resolver.resolveMelee(
                attackerAttack = 12,
                attackerAccuracy = 20,
                attackerDex = 10,
                targetDefense = 99,
                targetEvasion = 0,
                targetCurrentHp = 20,
                damageType = DamageType.COLD,
                targetResistance = -25,
            )

        assertTrue(result.hit)
        assertEquals(DamageType.COLD, result.damage?.type)
        assertEquals(15, result.damage?.finalDamage)
        assertEquals(-25, result.damage?.resistanceValue)
    }

    @Test
    fun `world on damage applied callback observes health after tentative damage application`() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)))
        val world = World()
        val attacker = world.createEntity()
        val target = world.createEntity()
        world.add(attacker, Stats(str = 10, dex = 10, con = 10, wil = 10))
        world.add(attacker, CombatProfile(baseAttack = 12, baseDefense = 0, baseAccuracy = 20, baseHp = 40))
        world.add(attacker, Health(current = 40, max = 40))
        world.add(target, Stats(str = 10, dex = 0, con = 10, wil = 10))
        world.add(target, CombatProfile(baseAttack = 4, baseDefense = 0, baseAccuracy = 0, baseEvasion = 0, baseHp = 20))
        world.add(target, Health(current = 20, max = 20))

        var observedHealthDuringCallback = -1
        val result =
            resolver.resolveMelee(
                world = world,
                attacker = attacker,
                target = target,
                callbacks =
                    listOf(
                        PipelineCallback(
                            ownerId = EntityId(99),
                            callbackName = "observe_applied_health",
                            phase = CombatCallbackPhase.ON_DAMAGE_APPLIED,
                            priority = 10,
                        ) {
                            observedHealthDuringCallback = requireNotNull(world.get<Health>(target)).current
                            CallbackDecision()
                        },
                    ),
            )

        assertTrue(result.hit)
        val expectedHealthAfterApply = (20 - result.finalDamage).coerceAtLeast(0)
        assertEquals(expectedHealthAfterApply, observedHealthDuringCallback)
        assertEquals(observedHealthDuringCallback, requireNotNull(world.get<Health>(target)).current)
    }

    @Test
    fun `world damage callbacks can mutate target health without resolver overwriting it`() {
        val resolver = CombatResolver(TestRandomSource(doubles = listOf(0.0, 0.99), ints = listOf(0)))
        val world = World()
        val attacker = world.createEntity()
        val target = world.createEntity()
        world.add(attacker, Stats(str = 10, dex = 10, con = 10, wil = 10))
        world.add(attacker, CombatProfile(baseAttack = 12, baseDefense = 0, baseAccuracy = 20, baseHp = 40))
        world.add(attacker, Health(current = 40, max = 40))
        world.add(target, Stats(str = 10, dex = 0, con = 10, wil = 10))
        world.add(target, CombatProfile(baseAttack = 4, baseDefense = 0, baseAccuracy = 0, baseEvasion = 0, baseHp = 30))
        world.add(target, Health(current = 30, max = 30))

        val result =
            resolver.resolveMelee(
                world = world,
                attacker = attacker,
                target = target,
                callbacks =
                    listOf(
                        PipelineCallback(
                            ownerId = EntityId(100),
                            callbackName = "heal_on_damage_taken",
                            phase = CombatCallbackPhase.ON_DAMAGE_TAKEN,
                            priority = 10,
                        ) {
                            requireNotNull(world.get<Health>(target)).current += 5
                            CallbackDecision()
                        },
                    ),
            )

        assertTrue(result.hit)
        val expectedTentativeHealth = (30 - result.finalDamage).coerceAtLeast(0)
        assertEquals(expectedTentativeHealth + 5, requireNotNull(world.get<Health>(target)).current)
    }
}
