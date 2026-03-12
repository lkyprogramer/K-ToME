package com.ktome.core.fov

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShadowcastingTest {
    @Test
    fun `origin is always visible even with radius zero`() {
        val map = GameMap.fromAscii(
            listOf(
                "#####",
                "#.@.#",
                "#####",
            ),
        )

        val visible = Shadowcasting.computeVisible(map, map.playerStart, radius = 0)

        assertTrue(map.playerStart in visible)
    }

    @Test
    fun `open tiles inside the radius remain visible`() {
        val map = GameMap.fromAscii(
            listOf(
                "#########",
                "#.......#",
                "#..@....#",
                "#.......#",
                "#########",
            ),
        )

        val visible = Shadowcasting.computeVisible(map, map.playerStart, radius = 2)

        assertTrue(Point(4, 2) in visible)
        assertTrue(Point(2, 1) in visible)
        assertFalse(Point(6, 2) in visible)
    }

    @Test
    fun `walls cast shadows behind them`() {
        val map = GameMap.fromAscii(
            listOf(
                "########",
                "#@.#...#",
                "#..#...#",
                "#..#...#",
                "########",
            ),
        )

        val visible = Shadowcasting.computeVisible(map, map.playerStart, radius = 10)

        assertTrue(Point(2, 2) in visible)
        assertFalse(Point(4, 1) in visible)
        assertFalse(Point(4, 2) in visible)
    }
}
