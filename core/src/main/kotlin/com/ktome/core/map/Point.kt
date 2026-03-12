package com.ktome.core.map

import kotlin.math.abs

data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)

    operator fun minus(other: Point): Point = Point(x - other.x, y - other.y)

    fun chebyshevDistanceTo(other: Point): Int = maxOf(abs(x - other.x), abs(y - other.y))

    fun manhattanDistanceTo(other: Point): Int = abs(x - other.x) + abs(y - other.y)

    fun isAdjacentTo(other: Point): Boolean = this != other && chebyshevDistanceTo(other) == 1

    companion object {
        val ZERO: Point = Point(0, 0)
        val CARDINAL_DIRECTIONS: List<Point> = listOf(
            Point(0, -1),
            Point(1, 0),
            Point(0, 1),
            Point(-1, 0),
        )
        val ALL_DIRECTIONS: List<Point> = listOf(
            Point(0, -1),
            Point(1, -1),
            Point(1, 0),
            Point(1, 1),
            Point(0, 1),
            Point(-1, 1),
            Point(-1, 0),
            Point(-1, -1),
        )
    }
}
