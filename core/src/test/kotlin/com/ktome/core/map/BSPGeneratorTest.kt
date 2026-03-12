package com.ktome.core.map

import java.util.ArrayDeque
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BSPGeneratorTest {
    @Test
    fun `same seed generates the same map`() {
        val generator = BspGenerator(seed = 20260312L)

        val first = generator.generate()
        val second = generator.generate()

        assertEquals(first.asGlyphRows(), second.asGlyphRows())
        assertEquals(first.rooms, second.rooms)
        assertEquals(first.playerStart, second.playerStart)
    }

    @Test
    fun `generated rooms never overlap`() {
        val map = BspGenerator(seed = 42L).generate()

        map.rooms.forEachIndexed { index, room ->
            map.rooms.drop(index + 1).forEach { otherRoom ->
                assertFalse(room.intersects(otherRoom))
            }
        }
    }

    @Test
    fun `generated dungeon is fully connected from the player start`() {
        val map = BspGenerator(seed = 7L).generate()

        val reachable = floodFill(map, map.playerStart)

        assertEquals(map.floorPoints(), reachable)
    }

    @Test
    fun `generated rooms and player start stay inside bounds`() {
        val map = BspGenerator(seed = 99L).generate()

        assertTrue(map.rooms.isNotEmpty())
        assertTrue(map.playerStart in map.floorPoints())
        map.rooms.forEach { room ->
            assertTrue(map.isInBounds(room.left, room.top))
            assertTrue(map.isInBounds(room.right, room.bottom))
        }
    }

    private fun floodFill(
        map: GameMap,
        start: Point,
    ): Set<Point> {
        val queue = ArrayDeque<Point>()
        val visited = linkedSetOf<Point>()

        queue.add(start)
        visited.add(start)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            neighbors(current).forEach { next ->
                if (!map.isInBounds(next.x, next.y) || map.blocksMovement(next.x, next.y) || next in visited) {
                    return@forEach
                }

                visited.add(next)
                queue.add(next)
            }
        }

        return visited
    }

    private fun neighbors(point: Point): List<Point> =
        listOf(
            Point(point.x + 1, point.y),
            Point(point.x - 1, point.y),
            Point(point.x, point.y + 1),
            Point(point.x, point.y - 1),
        )
}
