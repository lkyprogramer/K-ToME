package com.ktome.core.fov

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point

object Shadowcasting {
    private val xx = intArrayOf(1, 0, 0, -1, -1, 0, 0, 1)
    private val xy = intArrayOf(0, 1, -1, 0, 0, -1, 1, 0)
    private val yx = intArrayOf(0, 1, 1, 0, 0, -1, -1, 0)
    private val yy = intArrayOf(1, 0, 0, 1, -1, 0, 0, -1)

    fun computeVisible(
        map: GameMap,
        origin: Point,
        radius: Int,
    ): Set<Point> {
        require(radius >= 0) { "Radius must be non-negative." }
        if (!map.isInBounds(origin.x, origin.y)) {
            return emptySet()
        }

        val visible = mutableSetOf(origin)
        if (radius == 0) {
            return visible
        }

        repeat(8) { octant ->
            castLight(
                map = map,
                origin = origin,
                row = 1,
                startSlope = 1.0,
                endSlope = 0.0,
                radius = radius,
                octant = octant,
                visible = visible,
            )
        }

        return visible
    }

    private fun castLight(
        map: GameMap,
        origin: Point,
        row: Int,
        startSlope: Double,
        endSlope: Double,
        radius: Int,
        octant: Int,
        visible: MutableSet<Point>,
    ) {
        if (startSlope < endSlope) {
            return
        }

        val radiusSquared = radius * radius
        var currentStartSlope = startSlope

        for (distance in row..radius) {
            var blocked = false
            var nextStartSlope = currentStartSlope
            var deltaX = -distance

            while (deltaX <= 0) {
                val deltaY = -distance
                val currentX = origin.x + deltaX * xx[octant] + deltaY * xy[octant]
                val currentY = origin.y + deltaX * yx[octant] + deltaY * yy[octant]

                val leftSlope = (deltaX - 0.5) / (deltaY + 0.5)
                val rightSlope = (deltaX + 0.5) / (deltaY - 0.5)

                if (currentStartSlope < rightSlope) {
                    deltaX++
                    continue
                }
                if (endSlope > leftSlope) {
                    break
                }

                val point = Point(currentX, currentY)
                val withinRadius = deltaX * deltaX + deltaY * deltaY <= radiusSquared
                val opaque = !map.isInBounds(currentX, currentY) || map.blocksVision(currentX, currentY)

                if (withinRadius && map.isInBounds(currentX, currentY)) {
                    visible += point
                }

                if (blocked) {
                    if (opaque) {
                        nextStartSlope = rightSlope
                    } else {
                        blocked = false
                        currentStartSlope = nextStartSlope
                    }
                } else if (opaque && distance < radius) {
                    blocked = true
                    castLight(
                        map = map,
                        origin = origin,
                        row = distance + 1,
                        startSlope = currentStartSlope,
                        endSlope = leftSlope,
                        radius = radius,
                        octant = octant,
                        visible = visible,
                    )
                    nextStartSlope = rightSlope
                }

                deltaX++
            }

            if (blocked) {
                return
            }
        }
    }
}
