package com.ktome.game.factory

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.World
import com.ktome.core.map.Point
import com.ktome.game.model.BossDefinition

class BossFactory(
    private val entityFactory: EntityFactory = EntityFactory(),
) {
    fun createBoss(
        world: World,
        definition: BossDefinition,
        position: Point,
    ): EntityId = entityFactory.createMonster(world, definition.template, position)
}
