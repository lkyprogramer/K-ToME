package com.ktome.core.turn

import com.ktome.core.ecs.EntityId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TurnSchedulerTest {
    @Test
    fun speed100ActsEveryTick() {
        val actor = EntityId(1)
        val firstTick = TurnScheduler.tick(listOf(actorState(actor, speed = 100, energy = 0)))
        val secondTick = TurnScheduler.tick(listOf(actorState(actor, speed = 100, energy = firstTick.remainingEnergy.getValue(actor))))

        assertEquals(listOf(actor), firstTick.actionQueue)
        assertEquals(0, firstTick.remainingEnergy.getValue(actor))
        assertEquals(listOf(actor), secondTick.actionQueue)
    }

    @Test
    fun speed200ActsTwicePerTick() {
        val actor = EntityId(1)
        val tick = TurnScheduler.tick(listOf(actorState(actor, speed = 200, energy = 0)))

        assertEquals(listOf(actor, actor), tick.actionQueue)
        assertEquals(0, tick.remainingEnergy.getValue(actor))
    }

    @Test
    fun speed50ActsEveryOtherTick() {
        val actor = EntityId(1)
        val firstTick = TurnScheduler.tick(listOf(actorState(actor, speed = 50, energy = 0)))
        val secondTick = TurnScheduler.tick(listOf(actorState(actor, speed = 50, energy = firstTick.remainingEnergy.getValue(actor))))

        assertEquals(emptyList<EntityId>(), firstTick.actionQueue)
        assertEquals(50, firstTick.remainingEnergy.getValue(actor))
        assertEquals(listOf(actor), secondTick.actionQueue)
        assertEquals(0, secondTick.remainingEnergy.getValue(actor))
    }

    @Test
    fun multipleEntitiesOrdering() {
        val fast = EntityId(1)
        val normal = EntityId(2)
        val tick = TurnScheduler.tick(
            listOf(
                actorState(fast, speed = 200, energy = 0),
                actorState(normal, speed = 100, energy = 0),
            ),
        )

        assertEquals(listOf(fast, normal, fast), tick.actionQueue)
        assertEquals(0, tick.remainingEnergy.getValue(fast))
        assertEquals(0, tick.remainingEnergy.getValue(normal))
    }

    @Test
    fun deterministicWithSameSetup() {
        val actor = EntityId(1)
        val first = TurnScheduler.tick(listOf(actorState(actor, speed = 130, energy = 40)))
        val second = TurnScheduler.tick(listOf(actorState(actor, speed = 130, energy = 40)))

        assertEquals(first, second)
    }

    private fun actorState(
        entityId: EntityId,
        speed: Int,
        energy: Int,
    ): TurnActorState = TurnActorState(entityId = entityId, speed = speed, energy = energy)
}
