package com.ktome.game

import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.PlayerControlled
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.add
import com.ktome.core.map.BspConfig
import com.ktome.core.map.BspGenerator

object GameModule {
    fun newFoundationSession(config: FoundationGameConfig = FoundationGameConfig()): FoundationGameSession {
        val map = BspGenerator(
            seed = config.seed,
            config = BspConfig(width = config.width, height = config.height),
        ).generate()

        val world = World()
        val playerId = world.createEntity()
        world.add(playerId, Position(map.playerStart.x, map.playerStart.y))
        world.add(playerId, Glyph('@'))
        world.add(playerId, PlayerControlled)

        return FoundationGameSession(
            config = config,
            map = map,
            world = world,
            playerId = playerId,
        )
    }
}
