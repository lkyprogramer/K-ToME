package com.ktome.game

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.mapgen.TerrainTag

internal data class TerrainPreferenceMatch(
    val preferredTerrainTags: Set<TerrainTag>,
    val directTerrainTags: Set<TerrainTag>,
    val adjacentTerrainTags: Set<TerrainTag>,
) {
    val implemented: Boolean
        get() = terrainPreferenceImplemented(preferredTerrainTags, directTerrainTags, adjacentTerrainTags)
}

internal fun <T> terrainPreferenceImplemented(
    preferredTerrainTags: Set<T>,
    directTerrainTags: Set<T>,
    adjacentTerrainTags: Set<T>,
): Boolean =
    preferredTerrainTags.isEmpty() ||
        directTerrainTags.any(preferredTerrainTags::contains) ||
        adjacentTerrainTags.any(preferredTerrainTags::contains)

internal data class TerrainAwareRoomPlacement(
    val room: Room,
    val spawnPoint: Point,
    val preferenceMatch: TerrainPreferenceMatch,
)

internal fun chooseTerrainAwareRoomPlacement(
    roomCandidates: List<Room>,
    fallbackRoom: Room,
    map: GameMap,
    occupiedPoints: Set<Point>,
    terrainTagsByPoint: Map<Point, Set<TerrainTag>>,
    preferredTerrainTags: Set<TerrainTag>,
): TerrainAwareRoomPlacement {
    require(roomCandidates.isNotEmpty()) { "Terrain-aware room placement requires at least one room candidate." }

    if (preferredTerrainTags.isEmpty()) {
        return TerrainAwareRoomPlacement(
            room = fallbackRoom,
            spawnPoint = fallbackSpawnPoint(room = fallbackRoom, map = map, occupiedPoints = occupiedPoints),
            preferenceMatch = emptyTerrainPreferenceMatch(),
        )
    }

    val fallbackPlacement =
        chooseBestSpawnPointInRoom(
            room = fallbackRoom,
            map = map,
            occupiedPoints = occupiedPoints,
            terrainTagsByPoint = terrainTagsByPoint,
            preferredTerrainTags = preferredTerrainTags,
        )
    if (fallbackPlacement.match.implemented) {
        return TerrainAwareRoomPlacement(
            room = fallbackRoom,
            spawnPoint = fallbackPlacement.point,
            preferenceMatch = fallbackPlacement.match,
        )
    }

    val scoredRooms =
        roomCandidates.mapIndexed { index, room ->
            val bestPoint =
                chooseBestSpawnPointInRoom(
                    room = room,
                    map = map,
                    occupiedPoints = occupiedPoints,
                    terrainTagsByPoint = terrainTagsByPoint,
                    preferredTerrainTags = preferredTerrainTags,
                )
            ScoredRoomPlacement(
                room = room,
                index = index,
                point = bestPoint.point,
                match = bestPoint.match,
                distanceFromFallback = room.center.chebyshevDistanceTo(fallbackRoom.center),
            )
        }
    val bestRoom = scoredRooms.maxWithOrNull(roomPlacementComparator()) ?: error("Missing room placement candidate.")
    val selected = if (bestRoom.match.implemented) bestRoom else fallbackPlacement.toScoredRoom(fallbackRoom)
    return TerrainAwareRoomPlacement(
        room = selected.room,
        spawnPoint = selected.point,
        preferenceMatch = selected.match,
    )
}

internal fun chooseTerrainAwarePoint(
    candidatePoints: List<Point>,
    fallbackPoint: Point,
    map: GameMap,
    occupiedPoints: Set<Point>,
    terrainTagsByPoint: Map<Point, Set<TerrainTag>>,
    preferredTerrainTags: Set<TerrainTag>,
): Pair<Point, TerrainPreferenceMatch> {
    val filteredCandidates =
        candidatePoints
            .asSequence()
            .filter { point ->
                map.isInBounds(point.x, point.y) &&
                    !map[point].blocksMovement &&
                    point !in occupiedPoints
            }.distinct()
            .toList()
    require(filteredCandidates.isNotEmpty()) { "Terrain-aware point placement requires at least one passable candidate." }

    if (preferredTerrainTags.isEmpty()) {
        val resolvedFallbackPoint = filteredCandidates.firstOrNull { point -> point == fallbackPoint } ?: filteredCandidates.first()
        return resolvedFallbackPoint to emptyTerrainPreferenceMatch()
    }

    val scored =
        filteredCandidates.mapIndexed { index, point ->
            ScoredPointPlacement(
                point = point,
                index = index,
                match =
                    terrainPreferenceMatchFor(
                        point = point,
                        map = map,
                        terrainTagsByPoint = terrainTagsByPoint,
                        preferredTerrainTags = preferredTerrainTags,
                    ),
                fallbackPoint = point == fallbackPoint,
            )
        }
    val best = scored.maxWithOrNull(pointPlacementComparator()) ?: error("Missing point placement candidate.")
    return if (best.match.implemented) {
        best.point to best.match
    } else {
        fallbackPoint to terrainPreferenceMatchFor(
            point = fallbackPoint,
            map = map,
            terrainTagsByPoint = terrainTagsByPoint,
            preferredTerrainTags = preferredTerrainTags,
        )
    }
}

private data class BestRoomPoint(
    val point: Point,
    val match: TerrainPreferenceMatch,
)

private data class ScoredPointPlacement(
    val point: Point,
    val index: Int,
    val match: TerrainPreferenceMatch,
    val fallbackPoint: Boolean,
)

private data class ScoredRoomPlacement(
    val room: Room,
    val index: Int,
    val point: Point,
    val match: TerrainPreferenceMatch,
    val distanceFromFallback: Int,
)

private fun BestRoomPoint.toScoredRoom(room: Room): ScoredRoomPlacement =
    ScoredRoomPlacement(
        room = room,
        index = Int.MIN_VALUE,
        point = point,
        match = match,
        distanceFromFallback = 0,
    )

private fun chooseBestSpawnPointInRoom(
    room: Room,
    map: GameMap,
    occupiedPoints: Set<Point>,
    terrainTagsByPoint: Map<Point, Set<TerrainTag>>,
    preferredTerrainTags: Set<TerrainTag>,
): BestRoomPoint {
    val candidatePoints = orderedSpawnCandidates(room = room, map = map, occupiedPoints = occupiedPoints)
    if (preferredTerrainTags.isEmpty()) {
        return BestRoomPoint(
            point = candidatePoints.firstOrNull() ?: room.center,
            match = emptyTerrainPreferenceMatch(),
        )
    }
    if (candidatePoints.isEmpty()) {
        return BestRoomPoint(
            point = room.center,
            match =
                terrainPreferenceMatchFor(
                    point = room.center,
                    map = map,
                    terrainTagsByPoint = terrainTagsByPoint,
                    preferredTerrainTags = preferredTerrainTags,
                ),
        )
    }
    val scored =
        candidatePoints.mapIndexed { index, point ->
            ScoredPointPlacement(
                point = point,
                index = index,
                match =
                    terrainPreferenceMatchFor(
                        point = point,
                        map = map,
                        terrainTagsByPoint = terrainTagsByPoint,
                        preferredTerrainTags = preferredTerrainTags,
                    ),
                fallbackPoint = index == 0,
            )
        }
    val best = scored.maxWithOrNull(pointPlacementComparator()) ?: error("Missing room point placement candidate.")
    return BestRoomPoint(point = best.point, match = best.match)
}

private fun terrainPreferenceMatchFor(
    point: Point,
    map: GameMap,
    terrainTagsByPoint: Map<Point, Set<TerrainTag>>,
    preferredTerrainTags: Set<TerrainTag>,
): TerrainPreferenceMatch {
    if (preferredTerrainTags.isEmpty()) {
        return emptyTerrainPreferenceMatch()
    }
    val directTerrainTags = terrainTagsByPoint[point].orEmpty().intersect(preferredTerrainTags)
    val adjacentTerrainTags =
        Point.ALL_DIRECTIONS
            .asSequence()
            .map { delta -> point + delta }
            .filter { adjacent -> map.isInBounds(adjacent.x, adjacent.y) }
            .flatMap { adjacent -> terrainTagsByPoint[adjacent].orEmpty().asSequence() }
            .filter(preferredTerrainTags::contains)
            .toCollection(linkedSetOf())
    return TerrainPreferenceMatch(
        preferredTerrainTags = preferredTerrainTags,
        directTerrainTags = directTerrainTags,
        adjacentTerrainTags = adjacentTerrainTags,
    )
}

private fun emptyTerrainPreferenceMatch(): TerrainPreferenceMatch =
    TerrainPreferenceMatch(
        preferredTerrainTags = emptySet(),
        directTerrainTags = emptySet(),
        adjacentTerrainTags = emptySet(),
    )

private fun fallbackSpawnPoint(
    room: Room,
    map: GameMap,
    occupiedPoints: Set<Point>,
): Point = orderedSpawnCandidates(room = room, map = map, occupiedPoints = occupiedPoints).firstOrNull() ?: room.center

private fun orderedSpawnCandidates(
    room: Room,
    map: GameMap,
    occupiedPoints: Set<Point>,
): List<Point> {
    val reserved = occupiedPoints.toMutableSet()
    val preferred = mutableListOf<Point>()
    preferred += room.center
    preferred +=
        listOf(
            Point(room.center.x + 1, room.center.y),
            Point(room.center.x - 1, room.center.y),
            Point(room.center.x, room.center.y + 1),
            Point(room.center.x, room.center.y - 1),
            Point(room.left + 1, room.top + 1),
            Point(room.right - 1, room.bottom - 1),
            Point(room.left + 1, room.bottom - 1),
            Point(room.right - 1, room.top + 1),
        )
    preferred +=
        (room.left + 1 until room.right).flatMap { x ->
            (room.top + 1 until room.bottom).map { y -> Point(x, y) }
        }
    val result = mutableListOf<Point>()
    preferred.distinct().forEach { point ->
        if (!room.contains(point) || map[point].blocksMovement || point in reserved) {
            return@forEach
        }
        result += point
        reserved += point
    }
    return result
}

private fun pointPlacementComparator(): Comparator<ScoredPointPlacement> =
    compareBy<ScoredPointPlacement>(
        { it.match.directTerrainTags.isNotEmpty() },
        { it.match.directTerrainTags.size },
        { it.match.adjacentTerrainTags.isNotEmpty() },
        { it.match.adjacentTerrainTags.size },
        { it.fallbackPoint },
        { -it.index },
    )

private fun roomPlacementComparator(): Comparator<ScoredRoomPlacement> =
    compareBy<ScoredRoomPlacement>(
        { it.match.directTerrainTags.isNotEmpty() },
        { it.match.directTerrainTags.size },
        { it.match.adjacentTerrainTags.isNotEmpty() },
        { it.match.adjacentTerrainTags.size },
        { -it.distanceFromFallback },
        { -it.index },
    )
