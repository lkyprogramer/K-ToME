package com.ktome.tools.mapgen

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.map.Point
import com.ktome.core.mapgen.GeneratedFloor
import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.RoomInstance
import com.ktome.core.mapgen.TopologyFingerprinting
import com.ktome.core.mapgen.VaultPlacement
import com.ktome.core.mapgen.isPrimaryPathReachable
import com.ktome.core.mapgen.loopEdgeCount
import com.ktome.core.mapgen.loopEdgeRatio
import com.ktome.core.pathfinding.AStar
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.mapgen.SchemaMapgenContentCatalogFactory
import com.ktome.game.mapgen.SchemaZoneMapgenProfileResolver
import com.ktome.game.mapgen.SchemaZoneRewardProfileResolver
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
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
    private const val HYBRID_PIPELINE_ID: String = "hybrid_topology"
    private const val SEEDS_PER_FLOOR: Int = 24
    private const val SEED_BASE: Long = 20260402010000L
    private const val ZONE_SEED_BLOCK: Long = 1_000L
    private const val FLOOR_SEED_BLOCK: Long = 100L

    fun run(): MapgenSmokeRun {
        val reportDir = reportDir()
        Files.createDirectories(reportDir)

        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val contentCatalog = SchemaMapgenContentCatalogFactory.from(schemaCatalog)
        val profileResolver = SchemaZoneMapgenProfileResolver(zones = schemaCatalog.zones, profiles = schemaCatalog.zoneMapgenProfiles)
        val rewardResolver = SchemaZoneRewardProfileResolver(zones = schemaCatalog.zones, profiles = schemaCatalog.zoneRewardProfiles)
        val pipeline = HybridTopologyMapgenPipeline(profileResolver = profileResolver, contentCatalog = contentCatalog)
        val cases = buildCases(schemaCatalog.zones)
        val distinctSeedList = cases.map { case -> case.request.seed }.distinct()
        require(distinctSeedList.size == cases.size) {
            "mapgenSmoke cases must keep a one-to-one seed corpus; got ${distinctSeedList.size} distinct seeds for ${cases.size} cases."
        }

        val results =
            cases.map { case ->
                val rewardProfile = rewardResolver.resolve(case.request.zoneId)
                val startedAt = System.nanoTime()
                try {
                    val generatedFloor = pipeline.run(case.request)
                    val durationMillis = (System.nanoTime() - startedAt) / 1_000_000
                    MapgenCaseResult(
                        pipelineId = case.pipelineId,
                        zoneId = case.request.zoneId,
                        floorIndex = case.request.floorIndex,
                        seed = case.request.seed,
                        durationMillis = durationMillis,
                        emptyMap = generatedFloor.map.floorPoints().isEmpty(),
                        criticalPathReachable = isPrimaryPathWalkable(generatedFloor),
                        topologyFingerprint = TopologyFingerprinting.fingerprint(generatedFloor.topology),
                        topologySummary =
                            TopologySummary(
                                nodeCount = generatedFloor.topology.nodes.size,
                                edgeCount = generatedFloor.topology.edges.size,
                                primaryPathLength = generatedFloor.topology.primaryPathNodeIds.size,
                                optionalLoopCount = generatedFloor.topology.optionalLoopCount,
                                loopEdgeCount = generatedFloor.topology.loopEdgeCount(),
                                loopEdgeRatio = generatedFloor.topology.loopEdgeRatio(),
                                roomCount = generatedFloor.rooms.size,
                                patternRoomCount = generatedFloor.rooms.count { room -> room.patternId != null },
                                vaultPlacementCount = generatedFloor.vaultPlacements.size,
                                pathClassCounts = countPathClasses(generatedFloor.topology.nodes.map { node -> node.pathClass }),
                            ),
                        biomeFamilies = generatedFloor.biomeFamilyIds.sorted(),
                        terrainTagDistribution = countTerrainTags(generatedFloor.terrainTags),
                        vaultPlacements = generatedFloor.vaultPlacements.map(::toVaultPlacementSnapshot).sortedBy(VaultPlacementSnapshot::vaultId),
                        rewardProfile =
                            RewardProfileSnapshot(
                                id = rewardProfile.id,
                                rarityBonus = rewardProfile.rarityBonus,
                                qualityBonus = rewardProfile.qualityBonus,
                                baseRewardBudget = rewardProfile.baseRewardBudget,
                            ),
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
                        biomeFamilies = emptyList(),
                        terrainTagDistribution = emptyMap(),
                        vaultPlacements = emptyList(),
                        rewardProfile =
                            RewardProfileSnapshot(
                                id = rewardProfile.id,
                                rarityBonus = rewardProfile.rarityBonus,
                                qualityBonus = rewardProfile.qualityBonus,
                                baseRewardBudget = rewardProfile.baseRewardBudget,
                            ),
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

    private fun buildCases(zones: List<ZoneSchemaV2>): List<MapgenCase> =
        buildList {
            zones.sortedBy(ZoneSchemaV2::id)
                .withIndex()
                .forEach { (zoneOrdinal, zone) ->
                    (1..zone.floorCount).forEach { floorIndex ->
                        repeat(SEEDS_PER_FLOOR) { seedOrdinal ->
                            add(
                                MapgenCase(
                                    pipelineId = HYBRID_PIPELINE_ID,
                                    request =
                                        MapgenRequest(
                                            zoneId = zone.id,
                                            floorIndex = floorIndex,
                                            seed =
                                                composeSeed(
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
                put("casesWithVaults", results.count { result -> result.vaultPlacements.isNotEmpty() })
                put("maxLoopEdgeRatio", results.maxOfOrNull { result -> result.topologySummary.loopEdgeRatio } ?: 0.0)
                put("averageLoopEdgeRatio", results.map { result -> result.topologySummary.loopEdgeRatio }.averageOrZero())
                putJsonObject("pipelineCounts") {
                    results.groupingBy(MapgenCaseResult::pipelineId).eachCount()
                        .toSortedMap()
                        .forEach { (pipelineId, count) -> put(pipelineId, count) }
                }
                putJsonObject("biomeFamilyUsage") {
                    results.flatMap(MapgenCaseResult::biomeFamilies)
                        .groupingBy { familyId -> familyId }
                        .eachCount()
                        .toSortedMap()
                        .forEach { (familyId, count) -> put(familyId, count) }
                }
                putJsonObject("biomeMixCounts") {
                    results.groupingBy { result -> result.biomeFamilies.joinToString(separator = "+").ifBlank { "none" } }
                        .eachCount()
                        .toSortedMap()
                        .forEach { (mixId, count) -> put(mixId, count) }
                }
                putJsonObject("vaultRewardBudgetBuckets") {
                    aggregateBudgetBuckets(results) { placement -> placement.rewardBudget }
                        .forEach { (bucket, count) -> put(bucket, count) }
                }
                putJsonObject("vaultThreatBudgetBuckets") {
                    aggregateBudgetBuckets(results) { placement -> placement.threatBudget }
                        .forEach { (bucket, count) -> put(bucket, count) }
                }
            }
        }

    private fun composeSeed(
        zoneOrdinal: Int,
        floorIndex: Int,
        seedOrdinal: Int,
    ): Long =
        SEED_BASE +
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
            put("loopEdgeRatio", topologySummary.loopEdgeRatio)
            put("topologyFingerprint", topologyFingerprint)
            put("durationMillis", durationMillis)
            putJsonObject("topologySummary") {
                put("nodeCount", topologySummary.nodeCount)
                put("edgeCount", topologySummary.edgeCount)
                put("primaryPathLength", topologySummary.primaryPathLength)
                put("optionalLoopCount", topologySummary.optionalLoopCount)
                put("loopEdgeCount", topologySummary.loopEdgeCount)
                put("loopEdgeRatio", topologySummary.loopEdgeRatio)
                put("roomCount", topologySummary.roomCount)
                put("patternRoomCount", topologySummary.patternRoomCount)
                put("vaultPlacementCount", topologySummary.vaultPlacementCount)
                putJsonObject("pathClassCounts") {
                    topologySummary.pathClassCounts.toSortedMap().forEach { (pathClass, count) -> put(pathClass, count) }
                }
            }
            putJsonArray("biomeFamilies") {
                biomeFamilies.forEach { familyId -> add(JsonPrimitive(familyId)) }
            }
            putJsonObject("terrainTagDistribution") {
                terrainTagDistribution.toSortedMap().forEach { (tag, count) -> put(tag, count) }
            }
            putJsonArray("vaultPlacements") {
                vaultPlacements.forEach { placement ->
                    add(
                        buildJsonObject {
                            put("vaultId", placement.vaultId)
                            put("pathClass", placement.pathClass)
                            put("rewardBudget", placement.rewardBudget)
                            put("rewardBudgetBucket", budgetBucket(placement.rewardBudget))
                            put("threatBudget", placement.threatBudget)
                            put("threatBudgetBucket", budgetBucket(placement.threatBudget))
                            putJsonArray("requiredTerrainTags") {
                                placement.requiredTerrainTags.forEach { tag -> add(JsonPrimitive(tag)) }
                            }
                        },
                    )
                }
            }
            putJsonObject("rewardProfile") {
                put("id", rewardProfile.id)
                put("rarityBonus", rewardProfile.rarityBonus)
                put("qualityBonus", rewardProfile.qualityBonus)
                put("baseRewardBudget", rewardProfile.baseRewardBudget)
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

    private fun isPrimaryPathWalkable(generatedFloor: GeneratedFloor): Boolean {
        if (!generatedFloor.topology.isPrimaryPathReachable()) {
            return false
        }
        val roomsByNodeId = generatedFloor.rooms.associateBy(RoomInstance::nodeId)
        val centers =
            generatedFloor.topology.primaryPathNodeIds.map { nodeId ->
                roomsByNodeId[nodeId]?.let { room -> Point(room.x + room.width / 2, room.y + room.height / 2) }
            }
        if (centers.any { point -> point == null }) {
            return false
        }
        return centers
            .filterNotNull()
            .zipWithNext()
            .all { (from, to) ->
                AStar.findPath(map = generatedFloor.map, start = from, goal = to).isNotEmpty()
            }
    }

    private fun aggregateBudgetBuckets(
        results: List<MapgenCaseResult>,
        selector: (VaultPlacementSnapshot) -> Int,
    ): Map<String, Int> =
        results.flatMap(MapgenCaseResult::vaultPlacements)
            .groupingBy { placement -> budgetBucket(selector(placement)) }
            .eachCount()
            .toSortedMap()

    private fun budgetBucket(value: Int): String =
        when {
            value <= 0 -> "0"
            value <= 3 -> "1-3"
            value <= 5 -> "4-5"
            else -> "6+"
        }

    private fun toVaultPlacementSnapshot(placement: VaultPlacement): VaultPlacementSnapshot =
        VaultPlacementSnapshot(
            vaultId = placement.vaultId,
            pathClass = placement.pathClass.name,
            rewardBudget = placement.rewardBudget,
            threatBudget = placement.threatBudget,
            requiredTerrainTags = placement.requiredTerrainTags.map { tag -> tag.name }.sorted(),
        )

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.mapgen.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "mapgen")
        } else {
            Path.of(configured)
        }
    }
}

private fun Iterable<Double>.averageOrZero(): Double {
    val values = toList()
    return if (values.isEmpty()) {
        0.0
    } else {
        values.average()
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
    val biomeFamilies: List<String>,
    val terrainTagDistribution: Map<String, Int>,
    val vaultPlacements: List<VaultPlacementSnapshot>,
    val rewardProfile: RewardProfileSnapshot,
    val error: String?,
)

private data class RewardProfileSnapshot(
    val id: String,
    val rarityBonus: Float,
    val qualityBonus: Int,
    val baseRewardBudget: Int,
)

private data class VaultPlacementSnapshot(
    val vaultId: String,
    val pathClass: String,
    val rewardBudget: Int,
    val threatBudget: Int,
    val requiredTerrainTags: List<String>,
)

private data class TopologySummary(
    val nodeCount: Int = 0,
    val edgeCount: Int = 0,
    val primaryPathLength: Int = 0,
    val optionalLoopCount: Int = 0,
    val loopEdgeCount: Int = 0,
    val loopEdgeRatio: Double = 0.0,
    val roomCount: Int = 0,
    val patternRoomCount: Int = 0,
    val vaultPlacementCount: Int = 0,
    val pathClassCounts: Map<String, Int> = emptyMap(),
)
