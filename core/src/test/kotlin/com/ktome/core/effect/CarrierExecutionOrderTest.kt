package com.ktome.core.effect

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.status.StatusEffectType
import com.ktome.core.status.StatusLifecycle
import com.ktome.core.status.StatusTickResolver
import com.ktome.core.status.StatusTracker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CarrierExecutionOrderTest {
    @Test
    fun `due effects use stable carrier order and tie breakers`() {
        val world = World()
        val actor = world.createEntity()
        val areaAlphaEntity = world.createEntity()
        val areaBetaEntity = world.createEntity()
        val worldAlphaEntity = world.createEntity()
        val worldBetaEntity = world.createEntity()

        world.add(
            actor,
            StatusTracker(
                mutableListOf(
                    StatusLifecycle.createInstance(
                        type = StatusEffectType.BLEED,
                        effectId = "actor_early",
                        duration = 3,
                        appliedTurn = 1,
                    ),
                    StatusLifecycle.createInstance(
                        type = StatusEffectType.POISON,
                        effectId = "actor_late",
                        duration = 3,
                        appliedTurn = 2,
                    ),
                ),
            ),
        )
        world.add(
            areaBetaEntity,
            AreaEffectEmitter(
                emitterId = "beta_emitter",
                sourceEntityId = EntityId(12),
                affectedActorIds = setOf(actor),
                emitterPriority = 20,
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "area_beta",
                            duration = 3,
                        ),
                    ),
            ),
        )
        world.add(
            areaAlphaEntity,
            AreaEffectEmitter(
                emitterId = "alpha_emitter",
                sourceEntityId = EntityId(11),
                affectedActorIds = setOf(actor),
                emitterPriority = 10,
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "area_alpha",
                            duration = 3,
                        ),
                    ),
            ),
        )
        world.add(
            worldBetaEntity,
            WorldEffect(
                effectId = "world_beta",
                affectedActorIds = setOf(actor),
                worldPriority = 20,
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "world_beta_effect",
                            duration = 3,
                        ),
                    ),
            ),
        )
        world.add(
            worldAlphaEntity,
            WorldEffect(
                effectId = "world_alpha",
                affectedActorIds = setOf(actor),
                worldPriority = 10,
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.BURN,
                            effectId = "world_alpha_effect",
                            duration = 3,
                        ),
                    ),
            ),
        )

        val dueEffects = StatusTickResolver.dueEffects(world, actor)

        assertEquals(
            listOf(
                "actor_early",
                "actor_late",
                "area_alpha",
                "area_beta",
                "world_alpha_effect",
                "world_beta_effect",
            ),
            dueEffects.map { dueEffect -> dueEffect.effect.id },
        )
    }
}
