package com.ktome.game

import com.ktome.core.ecs.Position
import com.ktome.core.ecs.Stamina
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.talent.CooldownState
import com.ktome.core.talent.TalentLoadout
import com.ktome.core.map.Point
import com.ktome.game.data.DataLoader
import com.ktome.game.factory.BossFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class BossFactoryTest {
    @Test
    fun `createBoss installs stamina cooldowns and talents`() {
        val world = World()
        val definition = DataLoader().loadBossDefinition()

        val bossId = BossFactory().createBoss(world, definition, Point(7, 8))

        assertEquals(Point(7, 8), requireNotNull(world.get<Position>(bossId)).toPoint())
        assertNotNull(world.get<Stamina>(bossId))
        assertNotNull(world.get<CooldownState>(bossId))
        assertEquals(2, requireNotNull(world.get<TalentLoadout>(bossId)).slotToTalentId.size)
    }
}
