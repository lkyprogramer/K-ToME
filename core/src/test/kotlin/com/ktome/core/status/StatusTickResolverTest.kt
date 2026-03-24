package com.ktome.core.status

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.effect.AreaEffectEmitter
import com.ktome.core.effect.WorldEffect
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatusTickResolverTest {
    @Test
    fun `due effects preserve actor area world carrier order`() {
        val world = World()
        val actor = world.createEntity()
        val areaEmitterEntity = world.createEntity()
        val worldEffectEntity = world.createEntity()

        world.add(
            actor,
            StatusTracker(
                mutableListOf(
                    StatusLifecycle.createInstance(
                        type = StatusEffectType.BLEED,
                        effectId = "bleed_actor",
                        duration = 3,
                        sourceEntityId = EntityId(77),
                        appliedTurn = 1,
                        tickDamageOverride = 2,
                    ),
                ),
            ),
        )
        world.add(
            areaEmitterEntity,
            AreaEffectEmitter(
                emitterId = "poison_cloud",
                sourceEntityId = EntityId(44),
                affectedActorIds = setOf(actor),
                emitterPriority = 10,
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.POISON,
                            effectId = "poison_area",
                            duration = 3,
                            appliedTurn = 2,
                            tickDamageOverride = 2,
                        ),
                    ),
            ),
        )
        world.add(
            worldEffectEntity,
            WorldEffect(
                effectId = "arena_aura",
                affectedActorIds = setOf(actor),
                worldPriority = 20,
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "burn_world",
                            duration = 3,
                            appliedTurn = 3,
                            tickDamageOverride = 2,
                        ),
                    ),
            ),
        )

        val dueEffects = StatusTickResolver.dueEffects(world, actor)

        assertEquals(
            listOf(
                EffectCarrierKind.ACTOR,
                EffectCarrierKind.AREA,
                EffectCarrierKind.WORLD,
            ),
            dueEffects.map { dueEffect -> dueEffect.carrierKind },
        )
        assertEquals(listOf("bleed_actor", "poison_area", "burn_world"), dueEffects.map { dueEffect -> dueEffect.effect.id })
        assertEquals(EntityId(77), dueEffects.first().sourceEntityId)
    }
}
