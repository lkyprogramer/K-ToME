package com.ktome.core.map

data class Room(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "Room width must be positive." }
        require(height > 0) { "Room height must be positive." }
    }

    val left: Int
        get() = x

    val right: Int
        get() = x + width - 1

    val top: Int
        get() = y

    val bottom: Int
        get() = y + height - 1

    val center: Point
        get() = Point(x + width / 2, y + height / 2)

    fun intersects(other: Room): Boolean =
        left <= other.right &&
            right >= other.left &&
            top <= other.bottom &&
            bottom >= other.top
}
