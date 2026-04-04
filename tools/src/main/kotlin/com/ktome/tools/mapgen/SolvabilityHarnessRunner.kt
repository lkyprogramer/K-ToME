package com.ktome.tools.mapgen

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.PathClass
import com.ktome.core.world.solvability.PerceptionScore
import com.ktome.core.world.solvability.SolvabilityGraphBuilder
import com.ktome.core.world.solvability.SolvabilityProver
import com.ktome.game.data.DataLoader
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.mapgen.SchemaMapgenContentCatalogFactory
import com.ktome.game.mapgen.SchemaZoneMapgenProfileResolver
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

data class SolvabilityHarnessRun(
    val totalCases: Int,
    val failureCount: Int,
    val summaryPath: Path,
    val proofsPath: Path,
)

object SolvabilityHarnessRunner {
    const val HARNESS_ID: String = "solvabilityHarness"
    private const val SUMMARY_FILE: String = "solvability-summary.json"
    private const val PROOFS_FILE: String = "solvability-proofs.jsonl"
    private const val SEEDS_PER_FLOOR: Int = 125
    private const val SEED_BASE: Long = 20260404010000L
    private const val ZONE_SEED_BLOCK: Long = 1_000L
    private const val FLOOR_SEED_BLOCK: Long = 100L
    private val harnessPerceptionScore: PerceptionScore = PerceptionScore(baseMentalPower = 12)

    fun run(): SolvabilityHarnessRun {
        val reportDir = reportDir()
        Files.createDirectories(reportDir)

        val loader = DataLoader()
        val schemaCatalog = loader.loadSchemaCatalog()
        val upgradedZones = schemaCatalog.zones.filter(ZoneSchemaV2::isPhase4Upgraded).sortedBy(ZoneSchemaV2::id)
        val profileResolver = SchemaZoneMapgenProfileResolver(zones = schemaCatalog.zones, profiles = schemaCatalog.zoneMapgenProfiles)
        val contentCatalog = SchemaMapgenContentCatalogFactory.from(schemaCatalog)
        val pipeline = HybridTopologyMapgenPipeline(profileResolver = profileResolver, contentCatalog = contentCatalog)
        val cases = buildCases(upgradedZones)
        val seedList = cases.map { case -> case.request.seed }
        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = seedList)

        val results =
            cases.map { testCase ->
                try {
                    val generatedFloor = pipeline.run(testCase.request)
                    val graph = SolvabilityGraphBuilder.build(generatedFloor)
                    val proof = SolvabilityProver.prove(graph = graph, perceptionScore = harnessPerceptionScore)
                    val visitedNodeIds = proof.visitedNodes.mapTo(linkedSetOf()) { nodeId -> nodeId.value }
                    val criticalGateRequirements =
                        graph.edges
                            .filter { edge ->
                                edge.requiredKeys.isNotEmpty() &&
                                    graph.nodes
                                        .filter { node -> node.pathClass == PathClass.CRITICAL_PATH }
                                        .map { node -> node.id }
                                        .contains(edge.to)
                            }.flatMap { edge -> edge.requiredKeys }
                            .toSet()
                    val optionalRequirementProviders =
                        graph.nodes
                            .filter { node -> node.pathClass == PathClass.OPTIONAL }
                            .flatMap { node -> node.grants }
                            .toSet()
                    val searchStatesByBindingId =
                        proof.searchStates.associate { entry ->
                            entry.bindingId.value to entry.result.name
                        }
                    SolvabilityCaseResult(
                        zoneId = testCase.request.zoneId,
                        floorIndex = testCase.request.floorIndex,
                        seed = testCase.request.seed,
                        criticalPathReachable = proof.criticalPathReachable,
                        visitedNodes = proof.visitedNodes.map { nodeId -> nodeId.value },
                        acquiredKeys = proof.acquiredKeys.map { requirement -> requirement.value },
                        unresolvedRequirements = proof.unresolvedRequirements.map { requirement -> requirement.value },
                        optionalPathCount = proof.optionalPathCount,
                        secretPathCount = proof.secretPathCount,
                        totalReachableNodes = proof.totalReachableNodes,
                        reachabilityRatio = proof.reachabilityRatio,
                        topologySummary = topologySummary(generatedFloor),
                        searchActionCount = proof.searchActionCount,
                        searchRevealCount = proof.searchRevealCount,
                        searchFailCount = proof.searchFailCount,
                        searchStates = searchStatesByBindingId,
                        secretProofs =
                            generatedFloor.entrances
                                .sortedBy { entrance -> entrance.bindingId.value }
                                .map { entrance ->
                                    SecretProofSnapshot(
                                        bindingId = entrance.bindingId.value,
                                        entranceAnchorId = entrance.entranceAnchorId.value,
                                        targetNodeId = entrance.targetNodeId.value,
                                        resolved = entrance.targetNodeId.value in visitedNodeIds,
                                        result = searchStatesByBindingId[entrance.bindingId.value],
                                    )
                                },
                        resolvedEntranceBindings =
                            generatedFloor.resolvedEntranceBindings().map { binding ->
                                ResolvedEntranceBindingSnapshot(
                                    bindingId = binding.searchBindingId.value,
                                    entranceAnchorId = binding.entranceAnchorId.value,
                                    targetNodeId = binding.resolvedTargetNodeId.value,
                                )
                            },
                        resolvedReturnBridgeNodeIds =
                            generatedFloor.entrances.associate { entrance ->
                                entrance.bindingId.value to entrance.resolvedReturnBridgeNodeId.value
                            },
                        backtrackSatisfied =
                            criticalGateRequirements.any(optionalRequirementProviders::contains) &&
                                proof.criticalPathReachable,
                        error = null,
                    )
                } catch (exception: Exception) {
                    SolvabilityCaseResult(
                        zoneId = testCase.request.zoneId,
                        floorIndex = testCase.request.floorIndex,
                        seed = testCase.request.seed,
                        criticalPathReachable = false,
                        visitedNodes = emptyList(),
                        acquiredKeys = emptyList(),
                        unresolvedRequirements = emptyList(),
                        optionalPathCount = 0,
                        secretPathCount = 0,
                        totalReachableNodes = 0,
                        reachabilityRatio = 0f,
                        topologySummary = TopologySummarySnapshot(),
                        searchActionCount = 0,
                        searchRevealCount = 0,
                        searchFailCount = 0,
                        searchStates = emptyMap(),
                        secretProofs = emptyList(),
                        resolvedEntranceBindings = emptyList(),
                        resolvedReturnBridgeNodeIds = emptyMap(),
                        backtrackSatisfied = false,
                        error = exception.message ?: exception::class.simpleName.orEmpty(),
                    )
                }
            }

        val summaryPath = reportDir.resolve(SUMMARY_FILE)
        val proofsPath = reportDir.resolve(PROOFS_FILE)
        Files.writeString(
            summaryPath,
            Json { prettyPrint = true }.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(header = header, results = results),
            ),
        )
        Files.writeString(
            proofsPath,
            results.joinToString(separator = "\n") { result ->
                Json.encodeToString(JsonElement.serializer(), result.toJson(header = header))
            } + "\n",
        )
        return SolvabilityHarnessRun(
            totalCases = results.size,
            failureCount = results.count { result -> result.error != null || !result.criticalPathReachable },
            summaryPath = summaryPath,
            proofsPath = proofsPath,
        )
    }

    private fun buildCases(upgradedZones: List<ZoneSchemaV2>): List<SolvabilityCase> =
        buildList {
            upgradedZones.withIndex().forEach { (zoneOrdinal, zone) ->
                (1..zone.floorCount).forEach { floorIndex ->
                    repeat(SEEDS_PER_FLOOR) { seedOrdinal ->
                        add(
                            SolvabilityCase(
                                request =
                                    MapgenRequest(
                                        zoneId = zone.id,
                                        floorIndex = floorIndex,
                                        seed = composeSeed(zoneOrdinal = zoneOrdinal, floorIndex = floorIndex, seedOrdinal = seedOrdinal),
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
        results: List<SolvabilityCaseResult>,
    ): JsonObject =
        buildJsonObject {
            put("header", header.toJson())
            putJsonObject("summary") {
                put("totalCases", results.size)
                put("failureCount", results.count { result -> result.error != null || !result.criticalPathReachable })
                put("criticalPathFailureCount", results.count { result -> !result.criticalPathReachable })
                put("errorCount", results.count { result -> result.error != null })
                put("casesWithBacktrackProof", results.count(SolvabilityCaseResult::backtrackSatisfied))
                put("casesWithSecretReveal", results.count { result -> result.searchRevealCount > 0 })
                put("casesWithSearchFailure", results.count { result -> result.searchFailCount > 0 })
                put("maxReachabilityRatio", results.maxOfOrNull(SolvabilityCaseResult::reachabilityRatio) ?: 0f)
            }
        }

    private fun SolvabilityCaseResult.toJson(header: HarnessReportHeader): JsonObject =
        buildJsonObject {
            put("buildId", header.buildId)
            put("phaseId", header.phaseId)
            put("locale", header.locale)
            put("contentSchemaVersion", header.contentSchemaVersion)
            put("topologyFingerprintVersion", header.topologyFingerprintVersion)
            put("searchRuleVersion", header.searchRuleVersion)
            put("secretRuleVersion", header.secretRuleVersion)
            put("seed", seed)
            put("zoneId", zoneId)
            put("floorIndex", floorIndex)
            put("criticalPathReachable", criticalPathReachable)
            putJsonArray("visitedNodes") {
                visitedNodes.forEach { nodeId -> add(JsonPrimitive(nodeId)) }
            }
            putJsonArray("acquiredKeys") {
                acquiredKeys.forEach { requirement -> add(JsonPrimitive(requirement)) }
            }
            putJsonArray("unresolvedRequirements") {
                unresolvedRequirements.forEach { requirement -> add(JsonPrimitive(requirement)) }
            }
            put("optionalPathCount", optionalPathCount)
            put("secretPathCount", secretPathCount)
            put("totalReachableNodes", totalReachableNodes)
            put("reachabilityRatio", reachabilityRatio)
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
            put("searchActionCount", searchActionCount)
            put("searchRevealCount", searchRevealCount)
            put("searchFailCount", searchFailCount)
            put("backtrackSatisfied", backtrackSatisfied)
            putJsonObject("searchStates") {
                searchStates.toSortedMap().forEach { (bindingId, result) -> put(bindingId, result) }
            }
            putJsonArray("secretProofs") {
                secretProofs.forEach { proof ->
                    add(
                        buildJsonObject {
                            put("bindingId", proof.bindingId)
                            put("entranceAnchorId", proof.entranceAnchorId)
                            put("targetNodeId", proof.targetNodeId)
                            put("resolved", proof.resolved)
                            proof.result?.let { put("result", it) }
                        },
                    )
                }
            }
            putJsonArray("resolvedEntranceBindings") {
                resolvedEntranceBindings.forEach { binding ->
                    add(
                        buildJsonObject {
                            put("bindingId", binding.bindingId)
                            put("entranceAnchorId", binding.entranceAnchorId)
                            put("targetNodeId", binding.targetNodeId)
                        },
                    )
                }
            }
            putJsonObject("resolvedReturnBridgeNodeIds") {
                resolvedReturnBridgeNodeIds.toSortedMap().forEach { (bindingId, nodeId) -> put(bindingId, nodeId) }
            }
            error?.let { put("error", it) }
        }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.solvability.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "solvability")
        } else {
            Path.of(configured)
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

    private fun topologySummary(generatedFloor: com.ktome.core.mapgen.GeneratedFloor): TopologySummarySnapshot =
        TopologySummarySnapshot(
            nodeCount = generatedFloor.topology.nodes.size,
            edgeCount = generatedFloor.topology.edges.size,
            primaryPathLength = generatedFloor.topology.primaryPathNodeIds.size,
            optionalLoopCount = generatedFloor.topology.optionalLoopCount,
            loopEdgeCount = generatedFloor.topology.edges.count { edge -> edge.isLoop },
            loopEdgeRatio =
                if (generatedFloor.topology.edges.isEmpty()) {
                    0.0
                } else {
                    generatedFloor.topology.edges.count { edge -> edge.isLoop }.toDouble() / generatedFloor.topology.edges.size.toDouble()
                },
            roomCount = generatedFloor.rooms.size,
            patternRoomCount = generatedFloor.rooms.count { room -> room.patternId != null },
            vaultPlacementCount = generatedFloor.vaultPlacements.size,
            pathClassCounts =
                generatedFloor.topology.nodes
                    .groupingBy { node -> node.pathClass.name }
                    .eachCount(),
        )
}

private data class SolvabilityCase(
    val request: MapgenRequest,
)

private data class SolvabilityCaseResult(
    val zoneId: String,
    val floorIndex: Int,
    val seed: Long,
    val criticalPathReachable: Boolean,
    val visitedNodes: List<String>,
    val acquiredKeys: List<String>,
    val unresolvedRequirements: List<String>,
    val optionalPathCount: Int,
    val secretPathCount: Int,
    val totalReachableNodes: Int,
    val reachabilityRatio: Float,
    val topologySummary: TopologySummarySnapshot,
    val searchActionCount: Int,
    val searchRevealCount: Int,
    val searchFailCount: Int,
    val searchStates: Map<String, String>,
    val secretProofs: List<SecretProofSnapshot>,
    val resolvedEntranceBindings: List<ResolvedEntranceBindingSnapshot>,
    val resolvedReturnBridgeNodeIds: Map<String, String>,
    val backtrackSatisfied: Boolean,
    val error: String?,
)

private data class TopologySummarySnapshot(
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

private data class SecretProofSnapshot(
    val bindingId: String,
    val entranceAnchorId: String,
    val targetNodeId: String,
    val resolved: Boolean,
    val result: String?,
)

private data class ResolvedEntranceBindingSnapshot(
    val bindingId: String,
    val entranceAnchorId: String,
    val targetNodeId: String,
)

private fun ZoneSchemaV2.isPhase4Upgraded(): Boolean = mapgenProfileId != null
