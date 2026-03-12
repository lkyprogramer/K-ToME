package com.ktome.core.map

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GameMapTest {
    @Test
    fun `fromAscii derives player start walkability and glyph rows`() {
        val map = GameMap.fromAscii(
            listOf(
                "#####",
                "#@..#",
                "#####",
            ),
        )

        assertEquals(5, map.width)
        assertEquals(3, map.height)
        assertEquals(Point(1, 1), map.playerStart)
        assertTrue(map.blocksMovement(0, 0))
        assertFalse(map.blocksMovement(2, 1))
        assertEquals(listOf(
            "#####",
            "#...#",
            "#####",
        ), map.asGlyphRows())
    }

    @Test
    fun `floorPoints returns every walkable tile exactly once`() {
        val map = GameMap.fromAscii(
            listOf(
                "#####",
                "#...#",
                "#.@.#",
                "#####",
            ),
        )

        val floorPoints = map.floorPoints()

        assertEquals(6, floorPoints.size)
        assertTrue(Point(1, 1) in floorPoints)
        assertTrue(Point(2, 2) in floorPoints)
        assertTrue(Point(3, 2) in floorPoints)
    }
}
