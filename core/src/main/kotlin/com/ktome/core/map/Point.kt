package com.ktome.core.map

data class Point(val x: Int, val y: Int) {
    operator fun plus(other: Point): Point = Point(x + other.x, y + other.y)

    companion object {
        val ZERO: Point = Point(0, 0)
    }
}
