package com.ktome.game

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import com.ktome.core.map.Room
import com.ktome.core.map.TileType
import com.ktome.core.mapgen.TerrainTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TerrainAwarePlacementTest {
    @Test
    fun `room placement prefers direct terrain match over fallback room`() {
        val leftRoom = Room(x = 1, y = 1, width = 3, height = 3)
        val rightRoom = Room(x = 5, y = 1, width = 3, height = 3)
        val map = testMap(leftRoom, rightRoom)

        val placement =
            chooseTerrainAwareRoomPlacement(
                roomCandidates = listOf(leftRoom, rightRoom),
                fallbackRoom = leftRoom,
                map = map,
                occupiedPoints = emptySet(),
                terrainTagsByPoint = mapOf(Point(6, 2) to setOf(TerrainTag.OIL)),
                preferredTerrainTags = setOf(TerrainTag.OIL),
            )

        assertEquals(rightRoom, placement.room)
        assertEquals(Point(6, 2), placement.spawnPoint)
        assertEquals(setOf(TerrainTag.OIL), placement.preferenceMatch.directTerrainTags)
        assertTrue(placement.preferenceMatch.implemented)
    }

    @Test
    fun `room placement falls back to adjacent terrain when room lacks direct match`() {
        val room = Room(x = 1, y = 1, width = 3, height = 3)
        val map = testMap(room)
        val adjacentTerrain = Point(room.right + 1, room.center.y)
        val terrainTags = mapOf(adjacentTerrain to setOf(TerrainTag.WATER))

        val placement =
            chooseTerrainAwareRoomPlacement(
                roomCandidates = listOf(room),
                fallbackRoom = room,
                map = map,
                occupiedPoints = emptySet(),
                terrainTagsByPoint = terrainTags,
                preferredTerrainTags = setOf(TerrainTag.WATER),
            )

        assertTrue(placement.preferenceMatch.directTerrainTags.isEmpty())
        assertEquals(setOf(TerrainTag.WATER), placement.preferenceMatch.adjacentTerrainTags)
        assertTrue(placement.preferenceMatch.implemented)
    }

    @Test
    fun `room placement keeps fallback room when it already satisfies terrain preference`() {
        val fallbackRoom = Room(x = 1, y = 1, width = 3, height = 3)
        val alternateRoom = Room(x = 5, y = 1, width = 3, height = 3)
        val map = testMap(fallbackRoom, alternateRoom)

        val placement =
            chooseTerrainAwareRoomPlacement(
                roomCandidates = listOf(fallbackRoom, alternateRoom),
                fallbackRoom = fallbackRoom,
                map = map,
                occupiedPoints = emptySet(),
                terrainTagsByPoint =
                    mapOf(
                        Point(2, 2) to setOf(TerrainTag.WATER),
                        Point(6, 2) to setOf(TerrainTag.WATER),
                    ),
                preferredTerrainTags = setOf(TerrainTag.WATER),
            )

        assertEquals(fallbackRoom, placement.room)
        assertEquals(Point(2, 2), placement.spawnPoint)
        assertTrue(placement.preferenceMatch.implemented)
    }

    @Test
    fun `room placement prefers the nearest matching room to the planned fallback route`() {
        val fallbackRoom = Room(x = 1, y = 1, width = 3, height = 3)
        val nearMatchRoom = Room(x = 5, y = 1, width = 3, height = 3)
        val farMatchRoom = Room(x = 13, y = 1, width = 3, height = 3)
        val map = testMap(fallbackRoom, nearMatchRoom, farMatchRoom)

        val placement =
            chooseTerrainAwareRoomPlacement(
                roomCandidates = listOf(fallbackRoom, nearMatchRoom, farMatchRoom),
                fallbackRoom = fallbackRoom,
                map = map,
                occupiedPoints = emptySet(),
                terrainTagsByPoint =
                    mapOf(
                        Point(6, 2) to setOf(TerrainTag.WATER),
                        Point(14, 2) to setOf(TerrainTag.WATER),
                    ),
                preferredTerrainTags = setOf(TerrainTag.WATER),
            )

        assertEquals(nearMatchRoom, placement.room)
        assertEquals(Point(6, 2), placement.spawnPoint)
    }

    @Test
    fun `room placement keeps deterministic fallback when terrain preference is empty`() {
        val fallbackRoom = Room(x = 1, y = 1, width = 3, height = 3)
        val alternateRoom = Room(x = 5, y = 1, width = 3, height = 3)
        val map = testMap(fallbackRoom, alternateRoom)

        val placement =
            chooseTerrainAwareRoomPlacement(
                roomCandidates = listOf(fallbackRoom, alternateRoom),
                fallbackRoom = fallbackRoom,
                map = map,
                occupiedPoints = emptySet(),
                terrainTagsByPoint = mapOf(Point(6, 2) to setOf(TerrainTag.WATER)),
                preferredTerrainTags = emptySet(),
        )

        assertEquals(fallbackRoom, placement.room)
        assertEquals(fallbackRoom.center, placement.spawnPoint)
        assertTrue(placement.preferenceMatch.implemented)
        assertTrue(placement.preferenceMatch.directTerrainTags.isEmpty())
        assertTrue(placement.preferenceMatch.adjacentTerrainTags.isEmpty())
    }

    @Test
    fun `point placement keeps deterministic fallback when no preferred terrain exists`() {
        val room = Room(x = 1, y = 1, width = 3, height = 3)
        val map = testMap(room)
        val candidatePoints = listOf(Point(2, 2), Point(1, 1), Point(3, 3))

        val (point, match) =
            chooseTerrainAwarePoint(
                candidatePoints = candidatePoints,
                fallbackPoint = Point(2, 2),
                map = map,
                occupiedPoints = emptySet(),
                terrainTagsByPoint = emptyMap(),
                preferredTerrainTags = setOf(TerrainTag.ICE),
            )

        assertEquals(Point(2, 2), point)
        assertFalse(match.implemented)
        assertTrue(match.directTerrainTags.isEmpty())
        assertTrue(match.adjacentTerrainTags.isEmpty())
    }

    private fun testMap(vararg rooms: Room): GameMap {
        val maxRight = rooms.maxOf(Room::right)
        val maxBottom = rooms.maxOf(Room::bottom)
        val builder = GameMap.Builder(width = maxRight + 3, height = maxBottom + 3)
        rooms.forEach(builder::carveRoom)
        rooms.toList().zipWithNext().forEach { (left, right) ->
            val corridorXRange = (left.right until right.left).toList()
            corridorXRange.forEach { x -> builder.setTile(Point(x, left.center.y), TileType.FLOOR) }
            val yStart = minOf(left.center.y, right.center.y)
            val yEnd = maxOf(left.center.y, right.center.y)
            (yStart..yEnd).forEach { y -> builder.setTile(Point(right.left, y), TileType.FLOOR) }
        }
        return builder.build(rooms = rooms.toList(), playerStart = rooms.first().center)
    }
}
