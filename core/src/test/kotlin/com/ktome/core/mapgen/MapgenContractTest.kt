package com.ktome.core.mapgen

import com.ktome.core.map.GameMap
import com.ktome.core.map.Point
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class MapgenContractTest {
    private val resolver =
        object : ZoneMapgenProfileResolver {
            override fun resolve(zoneId: String): ZoneMapgenProfile =
                ZoneMapgenProfile(
                    id = "$zoneId.test",
                    zoneId = zoneId,
                    allowedBiomeFamilies = setOf("family.test"),
                    loopCountRange = 1..1,
                    vaultPool = setOf("vault.test"),
                    terrainTagWeights = mapOf(TerrainTag.WATER to 1.0f, TerrainTag.ICE to 0.5f),
                    roomTagFilter = setOf("start", "hub", "goal", "optional", "hidden_cache"),
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
    fun `hybrid topology pipeline keeps room biome and vault invariants`() {
        val pipeline =
            HybridTopologyMapgenPipeline(
                profileResolver = resolver,
                roomDefs =
                    listOf(
                        RoomDef("room.entry", RoomShape.RECT, 8..9, 7..8, setOf("general", "start", "hidden_cache")),
                        RoomDef("room.hub", RoomShape.RECT, 9..10, 8..9, setOf("general", "hub", "hidden_cache")),
                        RoomDef("room.goal", RoomShape.RECT, 9..10, 8..9, setOf("general", "goal", "hidden_cache")),
                        RoomDef("room.optional", RoomShape.ROUND, 8..9, 8..9, setOf("general", "optional", "hidden_cache")),
                    ),
                patternTemplates = listOf(PatternTemplateDef("cross_pillars", listOf(".....", ".#.#.", "..#..", ".#.#.", "....."))),
                patternRooms =
                    listOf(
                        PatternRoomDef(
                            id = "pattern_room.test",
                            baseRoomId = "room.hub",
                            patternId = "cross_pillars",
                            requiredTags = setOf("hub"),
                            spawnWeight = 5,
                        ),
                    ),
                vaultTemplates = listOf(VaultTemplateDef("briar_cache", listOf(".....", ".#.#.", "..#..", ".#.#.", "....."))),
                vaults =
                    listOf(
                        VaultDef(
                            id = "vault.test",
                            templateId = "briar_cache",
                            pathClass = PathClass.OPTIONAL,
                            threatBudget = 4,
                            rewardBudget = 6,
                            allowOnBiomeFamilies = setOf("family.test"),
                            requiredTerrainTags = setOf(TerrainTag.WATER),
                        ),
                    ),
                biomeFamilies =
                    listOf(
                        BiomeFamilyDef(
                            id = "family.test",
                            primaryTileSet = "tileset.test",
                            secondaryTileSet = null,
                            terrainTagWeights = mapOf(TerrainTag.WATER to 0.20f),
                            allowedRoomTags = setOf("start", "hub", "goal", "optional", "hidden_cache"),
                        ),
                    ),
            )
        val request = MapgenRequest(zoneId = "greenwood_fringe", floorIndex = 1, seed = 2026040103L, targetWidth = 72, targetHeight = 46)

        val generated = pipeline.run(request)

        assertTrue(generated.topology.isPrimaryPathReachable())
        assertTrue(generated.rooms.isNotEmpty())
        assertTrue(generated.rooms.map { room -> room.nodeId }.toSet() == generated.topology.nodes.map { node -> node.id }.toSet())
        assertTrue(generated.biomeFamilyIds.size <= 2)
        if (generated.topology.optionalLoopCount > 0) {
            assertTrue(generated.topology.loopEdgeRatio() in 0.15..0.35)
        }
        assertTrue(generated.vaultPlacements.all { placement -> placement.pathClass == PathClass.OPTIONAL })
        assertTrue(generated.vaultPlacements.all { placement -> placement.rewardBudget >= 0 && placement.threatBudget >= 0 })
        assertTrue(generated.vaultPlacements.isNotEmpty())
        assertTrue(generated.terrainTags.isNotEmpty())
        assertFalse(generated.map.floorPoints().isEmpty())
    }

    @Test
    fun `hybrid planner binds room definitions to the node biome family before instantiation`() {
        val roomDefsById =
            listOf(
                RoomDef("room.bog.primary", RoomShape.RECT, 8..9, 7..8, setOf("general", "bog", "start", "route", "hub", "goal")),
                RoomDef("room.forest.primary", RoomShape.RECT, 8..9, 7..8, setOf("general", "forest", "start", "route", "hub", "goal")),
                RoomDef("room.forest.optional", RoomShape.RECT, 8..9, 7..8, setOf("general", "forest", "optional")),
            ).associateBy(RoomDef::id)
        val planner =
            HybridTopologyPlanner(
                roomDefsById = roomDefsById,
                biomeFamiliesById =
                    mapOf(
                        "bog" to BiomeFamilyDef("bog", "tileset.bog", null, emptyMap(), setOf("bog", "general")),
                        "forest" to BiomeFamilyDef("forest", "tileset.forest", null, emptyMap(), setOf("forest", "general")),
                    ),
            )

        val topology =
            planner.plan(
                profile =
                    ZoneMapgenProfile(
                        id = "mixed.zone",
                        zoneId = "mixed_zone",
                        allowedBiomeFamilies = setOf("forest", "bog"),
                        loopCountRange = 0..1,
                        vaultPool = emptySet(),
                        terrainTagWeights = emptyMap(),
                        roomTagFilter = emptySet(),
                    ),
                request = MapgenRequest(zoneId = "mixed_zone", floorIndex = 1, seed = 2026040104L, targetWidth = 84, targetHeight = 52),
            )

        assertTrue(topology.nodes.all { node -> node.biomeFamilyId != null })
        topology.nodes.forEach { node ->
            val roomDef = roomDefsById.getValue(node.roomDefId)
            assertTrue(roomDef.tags.contains(requireNotNull(node.biomeFamilyId)) || roomDef.tags.none { it in setOf("bog", "forest") })
        }
    }

    @Test
    fun `hybrid planner rotates optional node biome families instead of pinning them to one family`() {
        val planner =
            HybridTopologyPlanner(
                roomDefsById =
                    listOf(
                        RoomDef("room.bog.primary", RoomShape.RECT, 8..9, 7..8, setOf("general", "bog", "start", "route", "hub", "goal")),
                        RoomDef("room.forest.primary", RoomShape.RECT, 8..9, 7..8, setOf("general", "forest", "start", "route", "hub", "goal")),
                        RoomDef("room.bog.optional", RoomShape.RECT, 8..9, 7..8, setOf("general", "bog", "optional")),
                        RoomDef("room.forest.optional", RoomShape.RECT, 8..9, 7..8, setOf("general", "forest", "optional")),
                    ).associateBy(RoomDef::id),
                biomeFamiliesById =
                    mapOf(
                        "bog" to BiomeFamilyDef("bog", "tileset.bog", null, emptyMap(), setOf("bog", "general")),
                        "forest" to BiomeFamilyDef("forest", "tileset.forest", null, emptyMap(), setOf("forest", "general")),
                    ),
            )

        val topology =
            planner.plan(
                profile =
                    ZoneMapgenProfile(
                        id = "mixed.zone.optional",
                        zoneId = "mixed_zone",
                        allowedBiomeFamilies = setOf("forest", "bog"),
                        loopCountRange = 2..2,
                        vaultPool = emptySet(),
                        terrainTagWeights = emptyMap(),
                        roomTagFilter = emptySet(),
                    ),
                request = MapgenRequest(zoneId = "mixed_zone", floorIndex = 1, seed = 2026040105L, targetWidth = 84, targetHeight = 52),
            )

        val optionalFamilies =
            topology.nodes
                .filter { node -> node.pathClass == PathClass.OPTIONAL }
                .mapNotNull(TopologyNode::biomeFamilyId)
                .toSet()

        assertEquals(setOf("bog", "forest"), optionalFamilies)
    }

    @Test
    fun `hybrid planner fails fast when zone room tag filter drifts outside biome contract`() {
        val planner =
            HybridTopologyPlanner(
                roomDefsById =
                    mapOf(
                        "room.entry" to RoomDef("room.entry", RoomShape.RECT, 8..9, 7..8, setOf("general", "start")),
                    ),
                biomeFamiliesById =
                    mapOf(
                        "family.test" to
                            BiomeFamilyDef(
                                id = "family.test",
                                primaryTileSet = "tileset.test",
                                secondaryTileSet = null,
                                terrainTagWeights = emptyMap(),
                                allowedRoomTags = setOf("bridge"),
                            ),
                    ),
            )

        assertThrows(IllegalArgumentException::class.java) {
            planner.plan(
                profile =
                    ZoneMapgenProfile(
                        id = "broken.zone",
                        zoneId = "broken_zone",
                        allowedBiomeFamilies = setOf("family.test"),
                        loopCountRange = 0..0,
                        vaultPool = emptySet(),
                        terrainTagWeights = emptyMap(),
                        roomTagFilter = setOf("ritual"),
                    ),
                request = MapgenRequest(zoneId = "broken_zone", floorIndex = 1, seed = 42L, targetWidth = 70, targetHeight = 45),
            )
        }
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
        assertEquals(generated.topology.nodes.map { node -> node.id }.toSet(), generated.rooms.map { room -> room.nodeId }.toSet())
        assertTrue(generated.rooms.all { room -> room.biomeFamilyId == LEGACY_COMPATIBILITY_BIOME_FAMILY_ID })
    }
}
