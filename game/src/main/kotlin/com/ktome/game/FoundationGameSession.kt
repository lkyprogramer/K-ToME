package com.ktome.game

import com.ktome.core.ecs.EntityId
import com.ktome.core.ecs.Glyph
import com.ktome.core.ecs.Position
import com.ktome.core.ecs.World
import com.ktome.core.ecs.get
import com.ktome.core.fov.Shadowcasting
import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.movement.MovementRules

class FoundationGameSession internal constructor(
    val config: FoundationGameConfig,
    val map: GameMap,
    private val world: World,
    val playerId: EntityId,
) {
    private var visibleTiles: Set<Point> = emptySet()
    private val exploredTiles = linkedSetOf<Point>()

    init {
        refreshFov()
    }

    fun playerPosition(): Point = requireNotNull(world.get<Position>(playerId)).toPoint()

    fun playerGlyph(): Char = world.get<Glyph>(playerId)?.value ?: '@'

    fun visibleTiles(): Set<Point> = visibleTiles.toSet()

    fun exploredTiles(): Set<Point> = exploredTiles.toSet()

    fun movePlayer(delta: Point): Boolean {
        val position = requireNotNull(world.get<Position>(playerId))
        val result = MovementRules.attemptMove(map, position.toPoint(), delta)
        if (!result.moved) {
            return false
        }

        position.moveTo(result.destination)
        refreshFov()
        return true
    }

    private fun refreshFov() {
        visibleTiles = Shadowcasting.computeVisible(
            map = map,
            origin = playerPosition(),
            radius = config.fovRadius,
        )
        exploredTiles += visibleTiles
    }
}
