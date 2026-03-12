package com.ktome.core.map

enum class TileType(
    val glyph: Char,
    val blocksMovement: Boolean,
    val blocksVision: Boolean,
) {
    WALL('#', blocksMovement = true, blocksVision = true),
    FLOOR('.', blocksMovement = false, blocksVision = false),
}
