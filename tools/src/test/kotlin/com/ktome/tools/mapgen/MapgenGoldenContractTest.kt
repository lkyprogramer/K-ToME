package com.ktome.tools.mapgen

import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.TopologyFingerprinting
import com.ktome.core.mapgen.isPrimaryPathReachable
import com.ktome.core.mapgen.loopEdgeCount
import com.ktome.core.mapgen.loopEdgeRatio
import com.ktome.game.data.DataLoader
import com.ktome.game.mapgen.SchemaMapgenContentCatalogFactory
import com.ktome.game.mapgen.SchemaZoneMapgenProfileResolver
import com.ktome.game.mapgen.SchemaZoneRewardProfileResolver
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapgenGoldenContractTest {
    private val json = Json { prettyPrint = true }
    private val loader = DataLoader()
    private val schemaCatalog = loader.loadSchemaCatalog()
    private val resolver = SchemaZoneMapgenProfileResolver(schemaCatalog.zones, schemaCatalog.zoneMapgenProfiles)
    private val rewardResolver = SchemaZoneRewardProfileResolver(schemaCatalog.zones, schemaCatalog.zoneRewardProfiles)
    private val contentCatalog = SchemaMapgenContentCatalogFactory.from(schemaCatalog)
    private val pipeline = HybridTopologyMapgenPipeline(profileResolver = resolver, contentCatalog = contentCatalog)

    @Test
    fun `phase4 topology golden keeps upgraded hybrid planner stable`() {
        assertGolden("golden/phase4/mapgen/hybrid-greenwood_fringe-floor1.json")
    }

    @Test
    fun `phase4 topology golden keeps fallback hybrid planner stable`() {
        assertGolden("golden/phase4/mapgen/hybrid-bandit_camp-floor1.json")
    }

    private fun assertGolden(resourcePath: String) {
        val (zoneId, floorIndex, seed) = goldenCase(resourcePath)
        val actual =
            buildCaseJson(
                zoneId = zoneId,
                floorIndex = floorIndex,
                seed = seed,
            )
        maybeRecordGolden(resourcePath = resourcePath, payload = actual)
        val expected = if (shouldUpdateGolden()) actual else parseResource(resourcePath)
        assertEquals(expected, actual)
    }

    private fun buildCaseJson(
        zoneId: String,
        floorIndex: Int,
        seed: Long,
    ): JsonObject {
        val zone = requireNotNull(schemaCatalog.zones.firstOrNull { schema -> schema.id == zoneId })
        val generatedFloor =
            pipeline.run(
                MapgenRequest(
                    zoneId = zoneId,
                    floorIndex = floorIndex,
                    seed = seed,
                    targetWidth = zone.mapSize.width,
                    targetHeight = zone.mapSize.height,
                ),
            )
        val rewardProfile = rewardResolver.resolve(zoneId)
        val pathClassCounts =
            generatedFloor.topology.nodes
                .map { node -> node.pathClass }
                .groupingBy(PathClass::name)
                .eachCount()
                .toSortedMap()
        val terrainTagDistribution =
            generatedFloor.terrainTags.values.flatten()
                .groupingBy { tag -> tag.name }
                .eachCount()
                .toSortedMap()
        return buildJsonObject {
            put("pipelineId", "hybrid_topology")
            put("zoneId", zoneId)
            put("floorIndex", floorIndex)
            put("seed", seed)
            put("criticalPathReachable", generatedFloor.topology.isPrimaryPathReachable())
            put("topologyFingerprint", TopologyFingerprinting.fingerprint(generatedFloor.topology))
            putJsonObject("topologySummary") {
                put("nodeCount", generatedFloor.topology.nodes.size)
                put("edgeCount", generatedFloor.topology.edges.size)
                put("primaryPathLength", generatedFloor.topology.primaryPathNodeIds.size)
                put("optionalLoopCount", generatedFloor.topology.optionalLoopCount)
                put("loopEdgeCount", generatedFloor.topology.loopEdgeCount())
                put("loopEdgeRatio", generatedFloor.topology.loopEdgeRatio())
                put("roomCount", generatedFloor.rooms.size)
                put("patternRoomCount", generatedFloor.rooms.count { room -> room.patternId != null })
                put("vaultPlacementCount", generatedFloor.vaultPlacements.size)
                putJsonObject("pathClassCounts") {
                    pathClassCounts.forEach { (pathClass, count) -> put(pathClass, count) }
                }
            }
            putJsonArray("biomeFamilies") {
                generatedFloor.biomeFamilyIds.sorted().forEach { familyId ->
                    add(Json.parseToJsonElement("\"$familyId\""))
                }
            }
            putJsonArray("vaultPlacements") {
                generatedFloor.vaultPlacements.sortedBy { placement -> placement.vaultId }.forEach { placement ->
                    add(
                        buildJsonObject {
                            put("vaultId", placement.vaultId)
                            put("pathClass", placement.pathClass.name)
                            put("rewardBudget", placement.rewardBudget)
                            put("threatBudget", placement.threatBudget)
                        },
                    )
                }
            }
            putJsonObject("terrainTagDistribution") {
                terrainTagDistribution.forEach { (tag, count) -> put(tag, count) }
            }
            putJsonObject("rewardProfile") {
                put("id", rewardProfile.id)
                put("rarityBonus", rewardProfile.rarityBonus)
                put("qualityBonus", rewardProfile.qualityBonus)
                put("baseRewardBudget", rewardProfile.baseRewardBudget)
            }
        }
    }

    private fun parseResource(resourcePath: String): JsonObject {
        javaClass.classLoader.getResource(resourcePath)?.let { resource ->
            return json.parseToJsonElement(Path.of(resource.toURI()).readText()) as JsonObject
        }
        val fallbackPath = goldenResourcePath(resourcePath)
        check(Files.exists(fallbackPath)) { "Missing golden resource '$resourcePath'." }
        return json.parseToJsonElement(Files.readString(fallbackPath)) as JsonObject
    }

    private fun maybeRecordGolden(
        resourcePath: String,
        payload: JsonObject,
    ) {
        if (System.getProperty("ktome.updateMapgenGolden") != "true") {
            return
        }
        val targetPath = goldenResourcePath(resourcePath)
        Files.createDirectories(targetPath.parent)
        Files.writeString(targetPath, json.encodeToString(JsonObject.serializer(), payload))
    }

    private fun shouldUpdateGolden(): Boolean = System.getProperty("ktome.updateMapgenGolden") == "true"

    private fun goldenResourcePath(resourcePath: String): Path =
        Path
            .of(requireNotNull(System.getProperty("ktome.repo.root")) { "Missing ktome.repo.root system property." })
            .resolve("tools")
            .resolve("src")
            .resolve("test")
            .resolve("resources")
            .resolve(resourcePath)

    private fun goldenCase(resourcePath: String): Triple<String, Int, Long> =
        when (resourcePath) {
            "golden/phase4/mapgen/hybrid-greenwood_fringe-floor1.json" -> Triple("greenwood_fringe", 1, 20260403010101L)
            "golden/phase4/mapgen/hybrid-bandit_camp-floor1.json" -> Triple("bandit_camp", 1, 20260403010201L)
            else -> {
                val expected = parseResource(resourcePath)
                Triple(
                    expected.requiredString("zoneId"),
                    expected.requiredInt("floorIndex"),
                    expected.requiredLong("seed"),
                )
            }
        }

    private fun JsonObject.requiredString(key: String): String = (getValue(key) as JsonPrimitive).content

    private fun JsonObject.requiredInt(key: String): Int = (getValue(key) as JsonPrimitive).content.toInt()

    private fun JsonObject.requiredLong(key: String): Long = (getValue(key) as JsonPrimitive).content.toLong()
}
