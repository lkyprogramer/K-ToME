package com.ktome.tools.mapgen

import com.ktome.core.mapgen.BspBackedMapgenPipeline
import com.ktome.core.mapgen.MapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.MinimalTopologyMapgenPipeline
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.TopologyFingerprinting
import com.ktome.core.mapgen.isPrimaryPathReachable
import com.ktome.game.data.DataLoader
import com.ktome.game.mapgen.SchemaZoneMapgenProfileResolver
import kotlin.io.path.readText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MapgenGoldenContractTest {
    private val loader = DataLoader()
    private val schemaCatalog = loader.loadSchemaCatalog()
    private val resolver = SchemaZoneMapgenProfileResolver(schemaCatalog.zones)
    private val pipelines: Map<String, MapgenPipeline> =
        mapOf(
            "bsp_compat" to BspBackedMapgenPipeline(profileResolver = resolver),
            "minimal_topology" to MinimalTopologyMapgenPipeline(profileResolver = resolver),
        )

    @Test
    fun `phase4 topology golden keeps bsp compatibility stable`() {
        assertGolden("golden/phase4/mapgen/bsp-crystal_cavern-floor1.json")
    }

    @Test
    fun `phase4 topology golden keeps minimal planner contract stable`() {
        assertGolden("golden/phase4/mapgen/minimal-bandit_camp-floor1.json")
    }

    private fun assertGolden(resourcePath: String) {
        val expected = parseResource(resourcePath)
        val actual =
            buildCaseJson(
                pipelineId = expected.requiredString("pipelineId"),
                zoneId = expected.requiredString("zoneId"),
                floorIndex = expected.requiredInt("floorIndex"),
                seed = expected.requiredLong("seed"),
            )

        assertEquals(expected, actual)
    }

    private fun buildCaseJson(
        pipelineId: String,
        zoneId: String,
        floorIndex: Int,
        seed: Long,
    ): JsonObject {
        val zone = requireNotNull(schemaCatalog.zones.firstOrNull { schema -> schema.id == zoneId })
        val generatedFloor =
            pipelines.getValue(pipelineId).run(
                MapgenRequest(
                    zoneId = zoneId,
                    floorIndex = floorIndex,
                    seed = seed,
                    targetWidth = zone.mapSize.width,
                    targetHeight = zone.mapSize.height,
                ),
            )
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
            put("pipelineId", pipelineId)
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
                putJsonObject("pathClassCounts") {
                    pathClassCounts.forEach { (pathClass, count) -> put(pathClass, count) }
                }
            }
            putJsonObject("terrainTagDistribution") {
                terrainTagDistribution.forEach { (tag, count) -> put(tag, count) }
            }
        }
    }

    private fun parseResource(resourcePath: String): JsonObject {
        val url = requireNotNull(javaClass.classLoader.getResource(resourcePath)) { "Missing golden resource '$resourcePath'." }
        return Json.parseToJsonElement(java.nio.file.Path.of(url.toURI()).readText()) as JsonObject
    }

    private fun JsonObject.requiredString(key: String): String = (getValue(key) as JsonPrimitive).content

    private fun JsonObject.requiredInt(key: String): Int = (getValue(key) as JsonPrimitive).content.toInt()

    private fun JsonObject.requiredLong(key: String): Long = (getValue(key) as JsonPrimitive).content.toLong()
}
