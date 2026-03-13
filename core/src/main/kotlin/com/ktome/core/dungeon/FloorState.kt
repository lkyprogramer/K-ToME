package com.ktome.core.dungeon

import com.ktome.core.map.Point

enum class StairDirection {
    UP,
    DOWN,
    ;

    fun deltaFloor(): Int =
        when (this) {
            UP -> -1
            DOWN -> 1
        }
}

data class FloorState<T>(
    val floor: Int,
    val stairsUp: Point? = null,
    val stairsDown: Point? = null,
    val payload: T,
) {
    init {
        require(floor > 0) { "Floor numbers must be positive." }
    }

    fun entryPoint(from: StairDirection): Point =
        when (from) {
            StairDirection.DOWN -> requireNotNull(stairsUp) { "Floor $floor has no upstairs entry point." }
            StairDirection.UP -> requireNotNull(stairsDown) { "Floor $floor has no downstairs entry point." }
        }
}

data class DungeonTransition<T>(
    val fromFloor: Int,
    val toFloor: Int,
    val entryPoint: Point,
    val state: FloorState<T>,
)
