package com.ktome.client.ui.layout

enum class PaneFocusAnchor {
    WORLD,
    CONTEXT,
    CHARACTER_ACTION,
}

internal class PaneFocusController(
    initialAnchor: PaneFocusAnchor = PaneFocusAnchor.WORLD,
) {
    private var mapAnchor: PaneFocusAnchor = initialAnchor
    private var suspendedMapAnchor: PaneFocusAnchor? = null

    val currentAnchor: PaneFocusAnchor
        get() = mapAnchor

    fun move(delta: Int): PaneFocusAnchor {
        val anchors = PaneFocusAnchor.entries
        val currentIndex = anchors.indexOf(mapAnchor)
        val nextIndex = Math.floorMod(currentIndex + delta, anchors.size)
        mapAnchor = anchors[nextIndex]
        return mapAnchor
    }

    fun set(anchor: PaneFocusAnchor): PaneFocusAnchor {
        mapAnchor = anchor
        suspendedMapAnchor = null
        return mapAnchor
    }

    fun onModalOpened() {
        if (suspendedMapAnchor == null) {
            suspendedMapAnchor = mapAnchor
        }
    }

    fun onModalClosed() {
        suspendedMapAnchor?.let { anchor ->
            mapAnchor = anchor
        }
        suspendedMapAnchor = null
    }

    fun onPassiveTakeover() {
        mapAnchor = PaneFocusAnchor.WORLD
        suspendedMapAnchor = null
    }
}
