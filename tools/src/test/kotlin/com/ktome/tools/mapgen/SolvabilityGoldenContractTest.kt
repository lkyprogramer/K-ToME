package com.ktome.tools.mapgen

import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.PathClass
import com.ktome.core.world.solvability.PerceptionScore
import com.ktome.core.world.solvability.SolvabilityGraphBuilder
import com.ktome.core.world.solvability.SolvabilityProver
import com.ktome.game.data.DataLoader
import com.ktome.game.mapgen.SchemaMapgenContentCatalogFactory
import com.ktome.game.mapgen.SchemaZoneMapgenProfileResolver
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
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class SolvabilityGoldenContractTest {
    private val json = Json { prettyPrint = true }
    private val loader = DataLoader()
    private val schemaCatalog = loader.loadSchemaCatalog()
    private val resolver = SchemaZoneMapgenProfileResolver(schemaCatalog.zones, schemaCatalog.zoneMapgenProfiles)
    private val contentCatalog = SchemaMapgenContentCatalogFactory.from(schemaCatalog)
    private val pipeline = HybridTopologyMapgenPipeline(profileResolver = resolver, contentCatalog = contentCatalog)
    private val perceptionScore = PerceptionScore(baseMentalPower = 12)

    @Test
    @Tag("solvabilityHarness")
    fun `phase4 solvability golden keeps optional backtrack proof stable`() {
        assertGolden("golden/phase4/solvability/backtrack-greenwood_fringe-floor1.json")
    }

    @Test
    @Tag("solvabilityHarness")
    fun `phase4 solvability golden keeps perception failure proof stable`() {
        assertGolden("golden/phase4/solvability/perception-abyssal_temple-floor1.json")
    }

    @Test
    fun `required hidden anchor families respect floor aware mapgen bindings`() {
        assertEquals(setOf("hidden.branch"), requiredHiddenAnchorFamiliesForZoneFloor(schemaCatalog, zoneId = "greenwood_fringe", floorIndex = 1))
        assertEquals(
            setOf("hidden.critical.adjacent"),
            requiredHiddenAnchorFamiliesForZoneFloor(schemaCatalog, zoneId = "greenwood_fringe", floorIndex = 2),
        )
    }

    private fun assertGolden(resourcePath: String) {
        val (zoneId, floorIndex, seed) = goldenCase(resourcePath)
        val actual = buildCaseJson(zoneId = zoneId, floorIndex = floorIndex, seed = seed)
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
        val graph = SolvabilityGraphBuilder.build(generatedFloor)
        val providedDiscoveryTags = primerDiscoveryTagsForCase(schemaCatalog = schemaCatalog, zoneId = zoneId, floorIndex = floorIndex)
        val requiredHiddenAnchorFamilies =
            requiredHiddenAnchorFamiliesForZoneFloor(schemaCatalog = schemaCatalog, zoneId = zoneId, floorIndex = floorIndex)
        val observedHiddenAnchorFamilies = observedHiddenAnchorFamiliesForFloor(generatedFloor)
        val proof =
            SolvabilityProver.prove(
                graph = graph,
                perceptionScore = perceptionScore,
                providedTags = providedDiscoveryTags,
            )
        return buildJsonObject {
            put("zoneId", zoneId)
            put("floorIndex", floorIndex)
            put("seed", seed)
            put("criticalPathReachable", proof.criticalPathReachable)
            putJsonArray("visitedNodes") {
                proof.visitedNodes.forEach { nodeId -> add(JsonPrimitive(nodeId.value)) }
            }
            putJsonArray("acquiredKeys") {
                proof.acquiredKeys.forEach { requirement -> add(JsonPrimitive(requirement.value)) }
            }
            putJsonArray("unresolvedRequirements") {
                proof.unresolvedRequirements.forEach { requirement -> add(JsonPrimitive(requirement.value)) }
            }
            put("optionalPathCount", proof.optionalPathCount)
            put("secretPathCount", proof.secretPathCount)
            put("totalReachableNodes", proof.totalReachableNodes)
            put("reachabilityRatio", proof.reachabilityRatio)
            putJsonObject("topologySummary") {
                put("nodeCount", generatedFloor.topology.nodes.size)
                put("edgeCount", generatedFloor.topology.edges.size)
                put("primaryPathLength", generatedFloor.topology.primaryPathNodeIds.size)
                put("optionalLoopCount", generatedFloor.topology.optionalLoopCount)
                put("loopEdgeCount", generatedFloor.topology.edges.count { edge -> edge.isLoop })
                put(
                    "loopEdgeRatio",
                    if (generatedFloor.topology.edges.isEmpty()) {
                        0.0
                    } else {
                        generatedFloor.topology.edges.count { edge -> edge.isLoop }.toDouble() / generatedFloor.topology.edges.size.toDouble()
                    },
                )
                put("roomCount", generatedFloor.rooms.size)
                put("patternRoomCount", generatedFloor.rooms.count { room -> room.patternId != null })
                put("vaultPlacementCount", generatedFloor.vaultPlacements.size)
                putJsonObject("pathClassCounts") {
                    generatedFloor.topology.nodes
                        .groupingBy { node -> node.pathClass.name }
                        .eachCount()
                        .toSortedMap()
                        .forEach { (pathClass, count) -> put(pathClass, count) }
                }
            }
            put("searchActionCount", proof.searchActionCount)
            put("searchRevealCount", proof.searchRevealCount)
            put("searchFailCount", proof.searchFailCount)
            putJsonArray("providedDiscoveryTags") {
                providedDiscoveryTags.sorted().forEach { tag -> add(JsonPrimitive(tag)) }
            }
            put("hiddenAnchorFamiliesSatisfied", requiredHiddenAnchorFamilies.all(observedHiddenAnchorFamilies::contains))
            putJsonArray("requiredHiddenAnchorFamilies") {
                requiredHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
            putJsonArray("observedHiddenAnchorFamilies") {
                observedHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
            putJsonObject("searchStates") {
                proof.searchStates.associate { entry -> entry.bindingId.value to entry.result.name }
                    .toSortedMap()
                    .forEach { (bindingId, result) -> put(bindingId, result) }
            }
            putJsonArray("secretProofs") {
                val visitedNodeIds = proof.visitedNodes.mapTo(linkedSetOf()) { nodeId -> nodeId.value }
                generatedFloor.entrances
                    .sortedBy { entrance -> entrance.bindingId.value }
                    .forEach { entrance ->
                        add(
                            buildJsonObject {
                                put("bindingId", entrance.bindingId.value)
                                put("entranceAnchorId", entrance.entranceAnchorId.value)
                                put("targetNodeId", entrance.targetNodeId.value)
                                put("resolved", entrance.targetNodeId.value in visitedNodeIds)
                                proof.searchStates
                                    .firstOrNull { entry -> entry.bindingId == entrance.bindingId }
                                    ?.result
                                    ?.name
                                    ?.let { result -> put("result", result) }
                            },
                        )
                    }
            }
            putJsonArray("resolvedEntranceBindings") {
                generatedFloor.resolvedEntranceBindings().forEach { binding ->
                    add(
                        buildJsonObject {
                            put("bindingId", binding.searchBindingId.value)
                            put("entranceAnchorId", binding.entranceAnchorId.value)
                            put("targetNodeId", binding.resolvedTargetNodeId.value)
                        },
                    )
                }
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
        if (System.getProperty("ktome.updateSolvabilityGolden") != "true") {
            return
        }
        val targetPath = goldenResourcePath(resourcePath)
        Files.createDirectories(targetPath.parent)
        Files.writeString(targetPath, json.encodeToString(JsonObject.serializer(), payload))
    }

    private fun shouldUpdateGolden(): Boolean = System.getProperty("ktome.updateSolvabilityGolden") == "true"

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
            "golden/phase4/solvability/backtrack-greenwood_fringe-floor1.json" -> Triple("greenwood_fringe", 1, 20260404010101L)
            "golden/phase4/solvability/perception-abyssal_temple-floor1.json" -> Triple("abyssal_temple", 1, 20260404010201L)
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
