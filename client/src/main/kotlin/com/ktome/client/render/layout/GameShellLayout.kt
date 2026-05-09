package com.ktome.client.render.layout

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
) {
    init {
        require(!leftRailBounds.overlaps(mapBounds)) { "Left rail must not overlap map bounds." }
        require(!mapBounds.overlaps(rightPanelBounds)) { "Map bounds must not overlap right panel bounds." }
        require(!bottomHudBounds.overlaps(mapBounds)) { "Bottom HUD must not overlap map bounds." }
        require(!bottomHudBounds.overlaps(leftRailBounds)) { "Bottom HUD must not overlap left rail bounds." }
        require(!bottomHudBounds.overlaps(rightPanelBounds)) { "Bottom HUD must not overlap right panel bounds." }
    }
}
