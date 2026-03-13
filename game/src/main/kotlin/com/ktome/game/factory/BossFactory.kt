package com.ktome.game.factory

import com.ktome.core.ecs.DerivedStats
import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.ecs.get
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.TalentLoadout
import com.ktome.game.model.BossDefinition
import com.ktome.core.map.Point

class BossFactory(
    private val entityFactory: EntityFactory = EntityFactory(),
) {
    fun createBoss(
        world: World,
        definition: BossDefinition,
        position: Point,
    ): EntityId {
        val bossId = entityFactory.createMonster(world, definition.template, position)
        val derivedStats = requireNotNull(world.get<DerivedStats>(bossId))
        world.add(bossId, Stamina(current = derivedStats.maxStamina, max = derivedStats.maxStamina))
        world.add(bossId, CooldownState())
        world.add(
            bossId,
            TalentLoadout(
                slotToTalentId = definition.talentLevels.keys.mapIndexed { index, talentId -> (index + 1) to talentId }.toMap(linkedMapOf()).toMutableMap(),
                talentLevels = definition.talentLevels.toMutableMap(),
            ),
        )
        return bossId
    }
}
