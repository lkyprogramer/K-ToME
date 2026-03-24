package com.ktome.core.effect

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.status.StatusTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class EffectCarrierTest {
    @Test
    fun `actor cleanse does not remove area or world carriers`() {
        val world = World()
        val actor = world.createEntity()
        val areaEmitterEntity = world.createEntity()
        val worldEffectEntity = world.createEntity()
        val tracker = StatusTracker()

        world.add(actor, tracker)
        world.add(
            areaEmitterEntity,
            AreaEffectEmitter(
                emitterId = "poison_cloud",
                sourceEntityId = EntityId(21),
                affectedActorIds = setOf(actor),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.POISON,
                            effectId = "poison_area",
                            duration = 3,
                            sourceEntityId = EntityId(21),
                        ),
                    ),
            ),
        )
        world.add(
            worldEffectEntity,
            WorldEffect(
                effectId = "arena_aura",
                affectedActorIds = setOf(actor),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "burn_world",
                            duration = 3,
                        ),
                    ),
            ),
        )

        StatusLifecycle.applyEffect(
            tracker,
            StatusLifecycle.createInstance(
                type = StatusEffectType.CURSE,
                effectId = "curse_actor",
                duration = 4,
            ),
        )

        val removed = StatusLifecycle.cleanse(tracker, maxEffectsRemoved = 1)

        assertEquals(listOf(StatusEffectType.CURSE), removed.map { effect -> effect.type })
        assertFalse(tracker.activeEffects().any { effect -> effect.id == "poison_area" || effect.id == "burn_world" })
        assertEquals(1, requireNotNull(world.get<AreaEffectEmitter>(areaEmitterEntity)).effects.size)
        assertEquals(1, requireNotNull(world.get<WorldEffect>(worldEffectEntity)).effects.size)
    }
}
