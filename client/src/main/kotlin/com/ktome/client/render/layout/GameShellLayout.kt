package com.ktome.client.render.layout

internal data class RectInt(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(width >= 0) { "RectInt width must be non-negative." }
        require(height >= 0) { "RectInt height must be non-negative." }
    }

    val right: Int get() = x + width
    val top: Int get() = y + height

    fun contains(
        px: Int,
        py: Int,
    ): Boolean = px in x until right && py in y until top
}

internal data class InsetsInt(
    val left: Int = 0,
    val right: Int = 0,
    val top: Int = 0,
    val bottom: Int = 0,
)

internal data class ModalSafeBounds(
    val left: Int,
    val right: Int,
    val top: Int,
    val bottom: Int,
) {
    init {
        require(right >= left) { "Modal safe bounds right must be >= left." }
        require(top >= bottom) { "Modal safe bounds top must be >= bottom." }
    }

    val width: Int get() = right - left
    val height: Int get() = top - bottom

    fun toRectInt(): RectInt = RectInt(left, bottom, width, height)
}

internal data class GameShellBounds(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val right: Float get() = x + width
    val top: Float get() = y + height

    fun overlaps(other: GameShellBounds): Boolean =
        x < other.right &&
            right > other.x &&
            y < other.top &&
            top > other.y
}

internal data class GameShellLayout(
    val leftRailBounds: GameShellBounds,
    val mapBounds: GameShellBounds,
    val rightPanelBounds: GameShellBounds,
    val bottomHudBounds: GameShellBounds,
    val shellContentBounds: GameShellBounds,
    val modalSafeBounds: ModalSafeBounds,
    val bottomLogReservedBounds: GameShellBounds,
    val cellAlignedMapBounds: RectInt,
    val mapInnerPadding: InsetsInt,
) {
    init {
        require(!leftRailBounds.overlaps(mapBounds)) { "Left rail must not overlap map bounds." }
        require(!mapBounds.overlaps(rightPanelBounds)) { "Map bounds must not overlap right panel bounds." }
        require(!bottomHudBounds.overlaps(mapBounds)) { "Bottom HUD must not overlap map bounds." }
        require(!bottomHudBounds.overlaps(leftRailBounds)) { "Bottom HUD must not overlap left rail bounds." }
        require(!bottomHudBounds.overlaps(rightPanelBounds)) { "Bottom HUD must not overlap right panel bounds." }
        require(shellContentBounds.width > 0f && shellContentBounds.height > 0f) { "Shell content bounds must be positive." }
        require(cellAlignedMapBounds.width > 0 && cellAlignedMapBounds.height > 0) { "Cell-aligned map bounds must be positive." }
    }
}
