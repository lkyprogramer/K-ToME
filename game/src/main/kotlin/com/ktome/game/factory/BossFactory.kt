package com.ktome.game.factory

import com.ktome.core.ai.BossEncounterState
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.map.Point
import com.ktome.game.elites.BossVariantDef
import com.ktome.game.model.BossDefinition

class BossFactory(
    private val entityFactory: EntityFactory = EntityFactory(),
) {
    fun createBoss(
        world: World,
        definition: BossDefinition,
        position: Point,
        bossVariant: BossVariantDef? = null,
    ): EntityId =
        entityFactory.createMonster(world, definition.template, position).also { bossId ->
            world.add(
                bossId,
                BossEncounterState(
                    encounterId = definition.encounter.id,
                    phaseOverrides = bossVariant?.phaseOverrides.orEmpty(),
                ),
            )
        }
}
