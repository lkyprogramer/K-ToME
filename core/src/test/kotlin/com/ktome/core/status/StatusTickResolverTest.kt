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

    @Test
    fun `due effects include non damaging area and world carrier statuses`() {
        val world = World()
        val actor = world.createEntity()
        val areaEmitterEntity = world.createEntity()
        val worldEffectEntity = world.createEntity()

        world.add(
            areaEmitterEntity,
            AreaEffectEmitter(
                emitterId = "armor_break_cloud",
                sourceEntityId = EntityId(12),
                affectedActorIds = setOf(actor),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.ARMOR_BREAK,
                            effectId = "armor_break_area",
                            duration = 2,
                            appliedTurn = 4,
                        ),
                    ),
            ),
        )
        world.add(
            worldEffectEntity,
            WorldEffect(
                effectId = "arcane_field",
                affectedActorIds = setOf(actor),
                effects =
                    mutableListOf(
                        StatusLifecycle.createInstance(
                            type = StatusEffectType.ARCANE_SHIELD_BUFF,
                            effectId = "arcane_shield_world",
                            duration = 2,
                            appliedTurn = 5,
                        ),
                    ),
            ),
        )

        val dueEffects = StatusTickResolver.dueEffects(world, actor)

        assertEquals(listOf("armor_break_area", "arcane_shield_world"), dueEffects.map { dueEffect -> dueEffect.effect.id })
        assertEquals(
            listOf(EffectCarrierKind.AREA, EffectCarrierKind.WORLD),
            dueEffects.map { dueEffect -> dueEffect.carrierKind },
        )
    }
}
