package com.ktome.tools.mapgen

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.mapgen.BspBackedMapgenPipeline
import com.ktome.core.mapgen.MapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.MinimalTopologyMapgenPipeline
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.TopologyFingerprinting
import com.ktome.core.mapgen.isPrimaryPathReachable
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.mapgen.SchemaZoneMapgenProfileResolver
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

data class MapgenSmokeRun(
    val totalCases: Int,
    val distinctSeedCount: Int,
    val failureCount: Int,
    val emptyMapCount: Int,
    val unreachableCount: Int,
    val summaryPath: Path,
    val seedsPath: Path,
    val p95GenerationMillis: Long,
)

object MapgenSmokeRunner {
    const val HARNESS_ID: String = "mapgenSmoke"
    private const val REPORT_FILE: String = "mapgen-smoke-summary.json"
    private const val REPORT_JSONL_FILE: String = "mapgen-smoke-seeds.jsonl"
    private const val BSP_PIPELINE_ID: String = "bsp_compat"
    private const val MINIMAL_PIPELINE_ID: String = "minimal_topology"
    private const val BSP_SEEDS_PER_FLOOR: Int = 24
    private const val MINIMAL_SEEDS_PER_ZONE: Int = 3
    private const val BSP_SEED_BASE: Long = 20260401010000L
    private const val MINIMAL_SEED_BASE: Long = 20260411010000L
    private const val ZONE_SEED_BLOCK: Long = 1_000L
    private const val FLOOR_SEED_BLOCK: Long = 100L

    fun run(): MapgenSmokeRun {
        val reportDir = reportDir()
        Files.createDirectories(reportDir)

        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val resolver = SchemaZoneMapgenProfileResolver(schemaCatalog.zones)
        val cases = buildCases(schemaCatalog.zones)
        val distinctSeedList = cases.map { case -> case.request.seed }.distinct()
        require(distinctSeedList.size == cases.size) {
            "mapgenSmoke cases must keep a one-to-one seed corpus; got ${distinctSeedList.size} distinct seeds for ${cases.size} cases."
        }
        val pipelines =
            mapOf(
                BSP_PIPELINE_ID to BspBackedMapgenPipeline(profileResolver = resolver),
                MINIMAL_PIPELINE_ID to MinimalTopologyMapgenPipeline(profileResolver = resolver),
            )
        val results =
            cases.map { case ->
                val startedAt = System.nanoTime()
                try {
                    val generatedFloor = pipelines.getValue(case.pipelineId).run(case.request)
                    val durationMillis = (System.nanoTime() - startedAt) / 1_000_000
                    val terrainCounts = countTerrainTags(generatedFloor.terrainTags)
                    MapgenCaseResult(
                        pipelineId = case.pipelineId,
                        zoneId = case.request.zoneId,
                        floorIndex = case.request.floorIndex,
                        seed = case.request.seed,
                        durationMillis = durationMillis,
                        emptyMap = generatedFloor.map.floorPoints().isEmpty(),
                        criticalPathReachable = generatedFloor.topology.isPrimaryPathReachable(),
                        topologyFingerprint = TopologyFingerprinting.fingerprint(generatedFloor.topology),
                        topologySummary =
                            TopologySummary(
                                nodeCount = generatedFloor.topology.nodes.size,
                                edgeCount = generatedFloor.topology.edges.size,
                                primaryPathLength = generatedFloor.topology.primaryPathNodeIds.size,
                                optionalLoopCount = generatedFloor.topology.optionalLoopCount,
                                pathClassCounts = countPathClasses(generatedFloor.topology.nodes.map { node -> node.pathClass }),
                            ),
                        terrainTagDistribution = terrainCounts,
                        error = null,
                    )
                } catch (ex: Exception) {
                    MapgenCaseResult(
                        pipelineId = case.pipelineId,
                        zoneId = case.request.zoneId,
                        floorIndex = case.request.floorIndex,
                        seed = case.request.seed,
                        durationMillis = 0,
                        emptyMap = true,
                        criticalPathReachable = false,
                        topologyFingerprint = "",
                        topologySummary = TopologySummary(),
                        terrainTagDistribution = emptyMap(),
                        error = ex.message ?: ex::class.simpleName.orEmpty(),
                    )
                }
            }

        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = distinctSeedList)
        val emptyMapCount = results.count(MapgenCaseResult::emptyMap)
        val unreachableCount = results.count { result -> !result.criticalPathReachable }
        val failureCount = results.count { result -> result.error != null }
        val durations = results.map(MapgenCaseResult::durationMillis).sorted()
        val p95GenerationMillis =
            if (durations.isEmpty()) {
                0L
            } else {
                durations[((durations.size - 1) * 95) / 100]
            }
        val summaryPath = reportDir.resolve(REPORT_FILE)
        val seedsPath = reportDir.resolve(REPORT_JSONL_FILE)
        Files.writeString(
            summaryPath,
            Json { prettyPrint = true }.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(
                    header = header,
                    totalCases = cases.size,
                    distinctSeedCount = distinctSeedList.size,
                    failureCount = failureCount,
                    emptyMapCount = emptyMapCount,
                    unreachableCount = unreachableCount,
                    p95GenerationMillis = p95GenerationMillis,
                    results = results,
                ),
            ),
        )
        Files.writeString(
            seedsPath,
            results.joinToString(separator = "\n") { result ->
                Json.encodeToString(JsonElement.serializer(), result.toJson(header = header))
            } + "\n",
        )
        return MapgenSmokeRun(
            totalCases = cases.size,
            distinctSeedCount = distinctSeedList.size,
            failureCount = failureCount,
            emptyMapCount = emptyMapCount,
            unreachableCount = unreachableCount,
            summaryPath = summaryPath,
            seedsPath = seedsPath,
            p95GenerationMillis = p95GenerationMillis,
        )
    }

    private fun buildCases(zones: List<ZoneSchemaV2>): List<MapgenCase> {
        val indexedZones = zones.sortedBy(ZoneSchemaV2::id).withIndex().toList()
        val bspCases =
            buildList {
                indexedZones.forEach { (zoneOrdinal, zone) ->
                    (1..zone.floorCount).forEach { floorIndex ->
                        repeat(BSP_SEEDS_PER_FLOOR) { seedOrdinal ->
                            add(
                                MapgenCase(
                                    pipelineId = BSP_PIPELINE_ID,
                                    request =
                                        MapgenRequest(
                                            zoneId = zone.id,
                                            floorIndex = floorIndex,
                                            seed =
                                                composeSeed(
                                                    base = BSP_SEED_BASE,
                                                    zoneOrdinal = zoneOrdinal,
                                                    floorIndex = floorIndex,
                                                    seedOrdinal = seedOrdinal,
                                                ),
                                            targetWidth = zone.mapSize.width,
                                            targetHeight = zone.mapSize.height,
                                        ),
                                ),
                            )
                        }
                    }
                }
            }
        val minimalProbeZones =
            indexedZones
                .filter { (_, zone) -> zone.worldRole == "optional" }
                .take(4)
                .ifEmpty { indexedZones.take(4) }
        val minimalCases =
            minimalProbeZones
                .flatMap { (zoneOrdinal, zone) ->
                    (0 until MINIMAL_SEEDS_PER_ZONE).map { seedOrdinal ->
                        MapgenCase(
                            pipelineId = MINIMAL_PIPELINE_ID,
                            request =
                                MapgenRequest(
                                    zoneId = zone.id,
                                    floorIndex = 1,
                                    seed =
                                        composeSeed(
                                            base = MINIMAL_SEED_BASE,
                                            zoneOrdinal = zoneOrdinal,
                                            floorIndex = 1,
                                            seedOrdinal = seedOrdinal,
                                        ),
                                    targetWidth = zone.mapSize.width,
                                    targetHeight = zone.mapSize.height,
                                ),
                        )
                    }
                }
        return bspCases + minimalCases
    }

    private fun buildSummaryPayload(
        header: HarnessReportHeader,
        totalCases: Int,
        distinctSeedCount: Int,
        failureCount: Int,
        emptyMapCount: Int,
        unreachableCount: Int,
        p95GenerationMillis: Long,
        results: List<MapgenCaseResult>,
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
            putJsonObject("summary") {
                put("totalCases", totalCases)
                put("distinctSeedCount", distinctSeedCount)
                put("failureCount", failureCount)
                put("emptyMapCount", emptyMapCount)
                put("unreachableCount", unreachableCount)
                put("p95GenerationMillis", p95GenerationMillis)
                putJsonObject("pipelineCounts") {
                    results.groupingBy(MapgenCaseResult::pipelineId).eachCount()
                        .toSortedMap()
                        .forEach { (pipelineId, count) -> put(pipelineId, count) }
                }
            }
        }

    private fun composeSeed(
        base: Long,
        zoneOrdinal: Int,
        floorIndex: Int,
        seedOrdinal: Int,
    ): Long =
        base +
            (zoneOrdinal.toLong() * ZONE_SEED_BLOCK) +
            (floorIndex.toLong() * FLOOR_SEED_BLOCK) +
            seedOrdinal.toLong()

    private fun MapgenCaseResult.toJson(header: HarnessReportHeader): JsonObject =
        buildJsonObject {
            put("buildId", header.buildId)
            put("phaseId", header.phaseId)
            put("locale", header.locale)
            put("contentSchemaVersion", header.contentSchemaVersion)
            put("topologyFingerprintVersion", header.topologyFingerprintVersion)
            put("seed", seed)
            put("zoneId", zoneId)
            put("floorIndex", floorIndex)
            put("pipelineId", pipelineId)
            put("criticalPathReachable", criticalPathReachable)
            put("loopCount", topologySummary.optionalLoopCount)
            put("topologyFingerprint", topologyFingerprint)
            put("durationMillis", durationMillis)
            putJsonObject("topologySummary") {
                put("nodeCount", topologySummary.nodeCount)
                put("edgeCount", topologySummary.edgeCount)
                put("primaryPathLength", topologySummary.primaryPathLength)
                put("optionalLoopCount", topologySummary.optionalLoopCount)
                putJsonObject("pathClassCounts") {
                    topologySummary.pathClassCounts.toSortedMap().forEach { (pathClass, count) -> put(pathClass, count) }
                }
            }
            putJsonObject("terrainTagDistribution") {
                terrainTagDistribution.toSortedMap().forEach { (tag, count) -> put(tag, count) }
            }
            error?.let { put("error", it) }
        }

    private fun countTerrainTags(terrainTags: Map<com.ktome.core.map.Point, Set<com.ktome.core.mapgen.TerrainTag>>): Map<String, Int> =
        buildMap {
            terrainTags.values.flatten().groupingBy { tag -> tag.name }.eachCount()
                .toSortedMap()
                .forEach { (tag, count) -> put(tag, count) }
        }

    private fun countPathClasses(pathClasses: List<PathClass>): Map<String, Int> =
        buildMap {
            pathClasses.groupingBy(PathClass::name).eachCount()
                .toSortedMap()
                .forEach { (pathClass, count) -> put(pathClass, count) }
        }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.mapgen.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "mapgen")
        } else {
            Path.of(configured)
        }
    }
}

private data class MapgenCase(
    val pipelineId: String,
    val request: MapgenRequest,
)

private data class MapgenCaseResult(
    val pipelineId: String,
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val durationMillis: Long,
    val emptyMap: Boolean,
    val criticalPathReachable: Boolean,
    val topologyFingerprint: String,
    val topologySummary: TopologySummary,
    val terrainTagDistribution: Map<String, Int>,
    val error: String?,
)

private data class TopologySummary(
    val nodeCount: Int = 0,
    val edgeCount: Int = 0,
    val primaryPathLength: Int = 0,
    val optionalLoopCount: Int = 0,
    val pathClassCounts: Map<String, Int> = emptyMap(),
)
