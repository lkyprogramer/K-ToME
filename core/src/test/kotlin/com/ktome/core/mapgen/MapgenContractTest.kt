package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MapgenContractTest {
    private val resolver =
        object : ZoneMapgenProfileResolver {
            override fun resolve(zoneId: String): ZoneMapgenProfile =
                ZoneMapgenProfile(
                    zoneId = zoneId,
                    allowedBiomeFamilies = setOf("biome.test"),
                    loopCountRange = 0..1,
                    vaultPool = emptySet(),
                    terrainTagWeights = mapOf(TerrainTag.WATER to 1.0f, TerrainTag.ICE to 0.5f),
                    roomTagFilter = setOf("optional_branch", "test_room"),
                )
        }

    @Test
    fun `bsp backed pipeline is deterministic for identical request`() {
        val pipeline = BspBackedMapgenPipeline(profileResolver = resolver)
        val request = MapgenRequest(zoneId = "greenwood_fringe", floorIndex = 1, seed = 2026040101L, targetWidth = 70, targetHeight = 45)

        val left = pipeline.run(request)
        val right = pipeline.run(request)

        assertEquals(TopologyFingerprinting.fingerprint(left.topology), TopologyFingerprinting.fingerprint(right.topology))
        assertEquals(left.terrainTags, right.terrainTags)
        assertEquals(TopologyFingerprinting.terrainTagHash(left.terrainTags), TopologyFingerprinting.terrainTagHash(right.terrainTags))
        assertEquals(left.map.asGlyphRows(), right.map.asGlyphRows())
        assertTrue(left.topology.primaryPathNodeIds.isNotEmpty())
    }

    @Test
    fun `minimal topology pipeline produces non bsp optional branch contract`() {
        val pipeline = MinimalTopologyMapgenPipeline(profileResolver = resolver)
        val request = MapgenRequest(zoneId = "underground_river", floorIndex = 2, seed = 2026040102L, targetWidth = 84, targetHeight = 52)

        val generated = pipeline.run(request)

        assertTrue(generated.topology.isPrimaryPathReachable())
        assertTrue(generated.topology.nodes.any { node -> node.pathClass == PathClass.OPTIONAL })
        assertEquals(1, generated.topology.optionalLoopCount)
        assertTrue(generated.topology.edges.any { edge -> edge.isLoop && edge.from == NodeId("optional") && edge.to == NodeId("goal") })
        assertTrue(generated.topology.primaryPathNodeIds.size >= 3)
        assertFalse(generated.map.floorPoints().isEmpty())
    }

    @Test
    fun `compatibility generated floor preserves empty terrain tag contract`() {
        val map = GameMap.fromAscii(rows = listOf("#####", "#...#", "#.@.#", "#...#", "#####"), playerStart = Point(2, 2))

        val generated =
            GeneratedFloor.compatibility(
                zoneId = "compat-zone",
                floorIndex = 1,
                seed = 42L,
                map = map,
                terrainTags = emptyMap(),
            )

        assertEquals(TopologyFingerprinting.terrainTagHash(emptyMap()), TopologyFingerprinting.terrainTagHash(generated.terrainTags))
        assertTrue(generated.topology.primaryPathNodeIds.isNotEmpty())
        assertTrue(generated.topology.isPrimaryPathReachable())
    }
}
