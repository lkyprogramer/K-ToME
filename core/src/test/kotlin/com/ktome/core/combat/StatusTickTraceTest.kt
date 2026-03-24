package com.ktome.core.combat

import com.ktome.core.ecs.CombatProfile
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Health
import com.ktome.core.ecs.Stats
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.status.StatusEffectType
import com.ktome.core.support.TestRandomSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StatusTickTraceTest {
    @Test
    fun `status tick resolves into status corpus trace`() {
        val world = World()
        val target = world.createEntity()
        world.add(target, Stats(str = 1, dex = 1, con = 1, wil = 1))
        world.add(target, CombatProfile(baseAttack = 1, baseDefense = 5, baseHp = 30))
        world.add(target, Health(current = 30, max = 30))

        val result =
            CombatResolver(TestRandomSource(doubles = listOf(0.0), ints = listOf(0))).resolveStatusTick(
                world = world,
                source = EntityId(99),
                target = target,
                statusType = StatusEffectType.BLEED,
                damageType = DamageType.PHYSICAL,
                rawDamage = 8,
                turn = 7,
                traceId = "status-tick-test",
            )

        assertEquals(CombatCorpusId.STATUS, result.envelope.corpusId)
        assertEquals("status-tick-test", result.trace.traceId)
        assertEquals("TURN_START_TICK", result.trace.steps.first().stepName)
        assertTrue(result.damage.finalDamage > 0)
    }
}
