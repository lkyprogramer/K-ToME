package com.ktome.game

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameModulePatrolRouteTest {
    @Test
    fun `patrol waypoints stay on walkable tiles for irregular room corners`() {
        val map =
            GameMap.fromAscii(
                rows =
                    listOf(
                        "#########",
                        "#.......#",
                        "#.#####.#",
                        "#.#...#.#",
                        "#.#####.#",
                        "#.......#",
                        "#########",
                    ),
                playerStart = Point(4, 3),
            )
        val room = Room(x = 1, y = 1, width = 7, height = 5)

        val waypoints = buildPatrolWaypoints(room = room, map = map)

        assertTrue(waypoints.isNotEmpty())
        assertTrue(waypoints.all { point -> map.isInBounds(point.x, point.y) && !map[point].blocksMovement })
    }
}
