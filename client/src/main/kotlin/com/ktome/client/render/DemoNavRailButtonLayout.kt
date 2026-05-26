package com.ktome.client.render

import com.ktome.client.render.layout.GameShellBounds
import com.ktome.client.ui.chrome.ChromeFrameBounds
import com.ktome.client.ui.chrome.ChromeFramePainter
import com.ktome.client.ui.chrome.ChromeSurfaceKind

internal object DemoNavRailButtonLayout {
    fun resolve(
        navRail: GameShellBounds,
        itemCount: Int,
    ): List<GameShellBounds> {
        if (itemCount <= 0) {
            return emptyList()
        }
        val content = contentBounds(navRail)
        val slotSide = (content.width + 28f).coerceIn(46f, 52f)
        val gap = 10f
        val totalHeight = itemCount * slotSide + (itemCount - 1).coerceAtLeast(0) * gap
        val startY = content.top - ((content.height - totalHeight) / 2f).coerceAtLeast(0f) - slotSide
        return List(itemCount) { index ->
            GameShellBounds(
                x = content.x + (content.width - slotSide) / 2f,
                y = startY - index * (slotSide + gap),
                width = slotSide,
                height = slotSide,
            )
        }
    }

    private fun contentBounds(bounds: GameShellBounds): GameShellBounds =
        ChromeFramePainter
            .contentBounds(
                ChromeFrameBounds(bounds.x, bounds.y, bounds.width, bounds.height),
                ChromeSurfaceKind.Panel,
            ).let { content ->
                GameShellBounds(content.x, content.y, content.width, content.height)
            }
}
