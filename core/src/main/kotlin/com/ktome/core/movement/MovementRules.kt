package com.ktome.core.movement

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import kotlin.math.abs

enum class MoveBlockReason {
    INVALID_STEP,
    OUT_OF_BOUNDS,
    BLOCKED_BY_WALL,
}

data class MoveResult(
    val destination: Point,
    val moved: Boolean,
    val blockReason: MoveBlockReason? = null,
)

object MovementRules {
    fun attemptMove(
        map: GameMap,
        from: Point,
        delta: Point,
    ): MoveResult {
        if (delta == Point.ZERO) {
            return MoveResult(destination = from, moved = false)
        }

        if (!isSingleStep(delta)) {
            return MoveResult(
                destination = from,
                moved = false,
                blockReason = MoveBlockReason.INVALID_STEP,
            )
        }

        val target = from + delta
        if (!map.isInBounds(target.x, target.y)) {
            return MoveResult(
                destination = from,
                moved = false,
                blockReason = MoveBlockReason.OUT_OF_BOUNDS,
            )
        }

        if (map.blocksMovement(target.x, target.y)) {
            return MoveResult(
                destination = from,
                moved = false,
                blockReason = MoveBlockReason.BLOCKED_BY_WALL,
            )
        }

        return MoveResult(destination = target, moved = true)
    }

    private fun isSingleStep(delta: Point): Boolean = abs(delta.x) <= 1 && abs(delta.y) <= 1
}
