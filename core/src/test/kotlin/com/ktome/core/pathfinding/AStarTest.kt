package com.ktome.core.pathfinding

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AStarTest {
    @Test
    fun findsPathWhenReachable() {
        val map = GameMap.fromAscii(
            rows = listOf(
                "#####",
                "#...#",
                "#.#.#",
                "#...#",
                "#####",
            ),
            playerStart = Point(1, 1),
        )

        val path = AStar.findPath(map, Point(1, 1), Point(3, 3))

        assertEquals(Point(1, 1), path.first())
        assertEquals(Point(3, 3), path.last())
        assertTrue(path.size >= 3)
    }

    @Test
    fun returnsEmptyWhenUnreachable() {
        val map = GameMap.fromAscii(
            rows = listOf(
                "#####",
                "#.#.#",
                "#####",
                "#.#.#",
                "#####",
            ),
            playerStart = Point(1, 1),
        )

        assertTrue(AStar.findPath(map, Point(1, 1), Point(3, 3)).isEmpty())
    }

    @Test
    fun avoidsBlockedPoints() {
        val map = GameMap.fromAscii(
            rows = listOf(
                "#######",
                "#.....#",
                "#######",
            ),
            playerStart = Point(1, 1),
        )

        assertTrue(AStar.findPath(map, Point(1, 1), Point(5, 1), blocked = setOf(Point(3, 1))).isEmpty())
    }
}
