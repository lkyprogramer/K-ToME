package com.ktome.core.movement

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MovementRulesTest {
    private val map = GameMap.fromAscii(
        listOf(
            "#####",
            "#...#",
            "#.#.#",
            "#.@.#",
            "#####",
        ),
    )

    @Test
    fun `walkable destination moves the actor`() {
        val result = MovementRules.attemptMove(map, from = map.playerStart, delta = Point(1, 0))

        assertEquals(MoveResult(destination = Point(3, 3), moved = true), result)
    }

    @Test
    fun `walls block movement`() {
        val result = MovementRules.attemptMove(map, from = map.playerStart, delta = Point(0, -1))

        assertEquals(
            MoveResult(
                destination = map.playerStart,
                moved = false,
                blockReason = MoveBlockReason.BLOCKED_BY_WALL,
            ),
            result,
        )
    }

    @Test
    fun `out of bounds moves are rejected`() {
        val result = MovementRules.attemptMove(map, from = Point(1, 1), delta = Point(-2, 0))

        assertEquals(
            MoveResult(
                destination = Point(1, 1),
                moved = false,
                blockReason = MoveBlockReason.INVALID_STEP,
            ),
            result,
        )
    }

    @Test
    fun `multi tile deltas are rejected to prevent tunneling through walls`() {
        val result = MovementRules.attemptMove(map, from = map.playerStart, delta = Point(0, -2))

        assertEquals(
            MoveResult(
                destination = map.playerStart,
                moved = false,
                blockReason = MoveBlockReason.INVALID_STEP,
            ),
            result,
        )
    }

    @Test
    fun `adjacent out of bounds moves are still reported as out of bounds`() {
        val result = MovementRules.attemptMove(map, from = Point(0, 1), delta = Point(-1, 0))

        assertEquals(
            MoveResult(
                destination = Point(0, 1),
                moved = false,
                blockReason = MoveBlockReason.OUT_OF_BOUNDS,
            ),
            result,
        )
    }

    @Test
    fun `zero delta is treated as a no-op`() {
        val result = MovementRules.attemptMove(map, from = map.playerStart, delta = Point.ZERO)

        assertEquals(MoveResult(destination = map.playerStart, moved = false), result)
    }
}
