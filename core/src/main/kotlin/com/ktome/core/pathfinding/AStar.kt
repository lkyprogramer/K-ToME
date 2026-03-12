package com.ktome.core.pathfinding

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import kotlin.math.abs

object AStar {
    private val directions = listOf(
        Point(-1, -1),
        Point(0, -1),
        Point(1, -1),
        Point(-1, 0),
        Point(1, 0),
        Point(-1, 1),
        Point(0, 1),
        Point(1, 1),
    )

    fun findPath(
        map: GameMap,
        start: Point,
        goal: Point,
        blocked: Set<Point> = emptySet(),
    ): List<Point> {
        if (start == goal) {
            return listOf(start)
        }

        val openSet = linkedSetOf(start)
        val cameFrom = mutableMapOf<Point, Point>()
        val gScore = mutableMapOf(start to 0)
        val fScore = mutableMapOf(start to heuristic(start, goal))

        while (openSet.isNotEmpty()) {
            val current = openSet.minWith(
                compareBy<Point> { fScore[it] ?: Int.MAX_VALUE }
                    .thenBy { heuristic(it, goal) }
                    .thenBy(Point::y)
                    .thenBy(Point::x),
            )

            if (current == goal) {
                return reconstructPath(cameFrom, current)
            }

            openSet.remove(current)

            neighbors(map, current, goal, blocked).forEach { neighbor ->
                val tentativeGScore = requireNotNull(gScore[current]) + 1
                if (tentativeGScore < (gScore[neighbor] ?: Int.MAX_VALUE)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor] = tentativeGScore
                    fScore[neighbor] = tentativeGScore + heuristic(neighbor, goal)
                    openSet += neighbor
                }
            }
        }

        return emptyList()
    }

    private fun neighbors(
        map: GameMap,
        point: Point,
        goal: Point,
        blocked: Set<Point>,
    ): List<Point> =
        directions.map { point + it }
            .filter { candidate ->
                map.isInBounds(candidate.x, candidate.y) &&
                    !map.blocksMovement(candidate.x, candidate.y) &&
                    (candidate == goal || candidate !in blocked)
            }

    private fun heuristic(
        from: Point,
        to: Point,
    ): Int = maxOf(abs(from.x - to.x), abs(from.y - to.y))

    private fun reconstructPath(
        cameFrom: Map<Point, Point>,
        current: Point,
    ): List<Point> {
        val path = mutableListOf(current)
        var cursor = current
        while (cameFrom.containsKey(cursor)) {
            cursor = requireNotNull(cameFrom[cursor])
            path += cursor
        }
        return path.asReversed()
    }
}
