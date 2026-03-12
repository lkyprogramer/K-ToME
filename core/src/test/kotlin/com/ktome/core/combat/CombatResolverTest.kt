package com.ktome.core.combat

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
        assertEquals(10, result.damage?.finalDamage)
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
}
