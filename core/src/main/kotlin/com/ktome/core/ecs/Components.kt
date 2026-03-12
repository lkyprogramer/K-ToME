package com.ktome.core.ecs

import com.ktome.core.map.Point

data class Position(var x: Int, var y: Int) {
    fun toPoint(): Point = Point(x, y)

    fun moveTo(point: Point) {
        x = point.x
        y = point.y
    }
}

data class Glyph(val value: Char)

data object PlayerControlled
