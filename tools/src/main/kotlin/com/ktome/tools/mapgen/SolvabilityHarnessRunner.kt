package com.ktome.tools.mapgen

import com.ktome.core.harness.HarnessReportHeader
import com.ktome.core.harness.toJson
import com.ktome.core.mapgen.HybridTopologyMapgenPipeline
import com.ktome.core.mapgen.MapgenRequest
import com.ktome.core.mapgen.PathClass
import com.ktome.core.mapgen.isFormalHiddenEntranceAnchor
import com.ktome.core.world.solvability.PerceptionScore
import com.ktome.core.world.solvability.SolvabilityGraph
import com.ktome.core.world.solvability.SolvabilityGraphBuilder
import com.ktome.core.world.solvability.SolvabilityProof
import com.ktome.core.world.solvability.SolvabilityProver
import com.ktome.game.data.schema.SchemaCatalog
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.game.hidden.HiddenConditionKey
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
    private const val ZONE_SEED_BLOCK: Long = 1_000_000L
    private const val FLOOR_SEED_BLOCK: Long = 10_000L
    private val harnessPerceptionScore: PerceptionScore = PerceptionScore(baseMentalPower = 12)

    internal data class SolvabilityZoneFloorKey(
        val zoneId: String,
        val floorIndex: Int,
    )

    fun run(): SolvabilityHarnessRun {
        val reportDir = reportDir()
        Files.createDirectories(reportDir)

        val executionContext = MapgenSmokeRunner.loadExecutionContext()
        val upgradedZones = executionContext.schemaCatalog.zones.filter(ZoneSchemaV2::isPhase4Upgraded).sortedBy(ZoneSchemaV2::id)
        val cases =
            buildCases(
                upgradedZones = upgradedZones,
                primerDiscoveryTagsByZoneAndFloor = primerDiscoveryTagsByZoneAndFloor(executionContext.schemaCatalog, upgradedZones),
                requiredHiddenAnchorFamiliesByZoneAndFloor =
                    requiredHiddenAnchorFamiliesByZoneAndFloor(executionContext.schemaCatalog, upgradedZones),
            )
        val distinctSeedList = cases.map { case -> case.request.seed }.distinct()
        require(distinctSeedList.size == cases.size) {
            "solvabilityHarness cases must keep a one-to-one seed corpus; got ${distinctSeedList.size} distinct seeds for ${cases.size} cases."
        }
        val header = phase4HarnessHeader(harnessId = HARNESS_ID, seedList = distinctSeedList)

        val results =
            cases.map { testCase ->
                executeCase(executionContext, testCase).toCaseResult()
            }

        val summaryPath = reportDir.resolve(SUMMARY_FILE)
        val proofsPath = reportDir.resolve(PROOFS_FILE)
        Files.writeString(
            summaryPath,
            Json { prettyPrint = true }.encodeToString(
                JsonElement.serializer(),
                buildSummaryPayload(
                    header = header,
                    results = results,
                    distinctSeedCount = distinctSeedList.size,
                ),
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
            failureCount =
                results.count { result ->
                    result.error != null ||
                        !result.criticalPathReachable ||
                        !result.hiddenAnchorFamiliesSatisfied
                },
            summaryPath = summaryPath,
            proofsPath = proofsPath,
        )
    }

    internal fun buildCases(
        upgradedZones: List<ZoneSchemaV2>,
        primerDiscoveryTagsByZoneAndFloor: Map<Pair<String, Int>, Set<String>> = emptyMap(),
        requiredHiddenAnchorFamiliesByZoneAndFloor: Map<Pair<String, Int>, Set<String>> = emptyMap(),
        seedsPerFloor: Int = SEEDS_PER_FLOOR,
    ): List<SolvabilityCase> =
        buildList {
            upgradedZones.withIndex().forEach { (zoneOrdinal, zone) ->
                (1..zone.floorCount).forEach { floorIndex ->
                    repeat(seedsPerFloor) { seedOrdinal ->
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
                                providedDiscoveryTags = primerDiscoveryTagsByZoneAndFloor[zone.id to floorIndex].orEmpty(),
                                requiredHiddenAnchorFamilies =
                                    requiredHiddenAnchorFamiliesByZoneAndFloor[zone.id to floorIndex].orEmpty(),
                            ),
                        )
                    }
                }
            }
        }

    /**
     * Freeze the reveal-fail lane against the same deterministic zone/floor seed schedule while
     * intentionally withholding primer discovery tags. This keeps fail coverage explicit and
     * independent from whether the reveal-success lane happens to surface a failed-search case.
     */
    internal fun buildRevealFailCases(
        upgradedZones: List<ZoneSchemaV2>,
        requiredHiddenAnchorFamiliesByZoneAndFloor: Map<Pair<String, Int>, Set<String>> = emptyMap(),
        fixtureZoneFloors: Set<SolvabilityZoneFloorKey> = emptySet(),
        seedsPerFloor: Int = 1,
    ): List<SolvabilityCase> =
        buildCases(
            upgradedZones = upgradedZones,
            primerDiscoveryTagsByZoneAndFloor = emptyMap(),
            requiredHiddenAnchorFamiliesByZoneAndFloor = requiredHiddenAnchorFamiliesByZoneAndFloor,
            seedsPerFloor = seedsPerFloor,
        ).filter { testCase ->
            fixtureZoneFloors.isEmpty() ||
                SolvabilityZoneFloorKey(
                    zoneId = testCase.request.zoneId,
                    floorIndex = testCase.request.floorIndex,
                ) in fixtureZoneFloors
        }

    internal fun executeCase(
        executionContext: MapgenExecutionContext,
        testCase: SolvabilityCase,
    ): SolvabilityExecutedCase =
        try {
            val generatedFloor = executionContext.pipeline.run(testCase.request)
            val graph = SolvabilityGraphBuilder.build(generatedFloor)
            val proof =
                SolvabilityProver.prove(
                    graph = graph,
                    perceptionScore = harnessPerceptionScore,
                    providedTags = testCase.providedDiscoveryTags,
                )
            SolvabilityExecutedCase(
                testCase = testCase,
                generatedFloor = generatedFloor,
                graph = graph,
                proof = proof,
                error = null,
            )
        } catch (exception: Exception) {
            SolvabilityExecutedCase(
                testCase = testCase,
                generatedFloor = null,
                graph = null,
                proof = null,
                error = exception.message ?: exception::class.simpleName.orEmpty(),
            )
        }

    private fun buildSummaryPayload(
        header: HarnessReportHeader,
        results: List<SolvabilityCaseResult>,
        distinctSeedCount: Int,
    ): JsonObject {
        val providedDiscoveryTags = results.flatMapTo(linkedSetOf(), SolvabilityCaseResult::providedDiscoveryTags)
        val requiredHiddenAnchorFamilies = results.flatMapTo(linkedSetOf(), SolvabilityCaseResult::requiredHiddenAnchorFamilies)
        val observedHiddenAnchorFamilies = results.flatMapTo(linkedSetOf(), SolvabilityCaseResult::observedHiddenAnchorFamilies)
        return buildJsonObject {
            put("header", header.toJson())
            putJsonObject("summary") {
                put("totalCases", results.size)
                put("distinctSeedCount", distinctSeedCount)
                put(
                    "failureCount",
                    results.count { result ->
                        result.error != null ||
                            !result.criticalPathReachable ||
                            !result.hiddenAnchorFamiliesSatisfied
                    },
                )
                put("criticalPathFailureCount", results.count { result -> !result.criticalPathReachable })
                put("errorCount", results.count { result -> result.error != null })
                put("casesWithBacktrackProof", results.count(SolvabilityCaseResult::backtrackSatisfied))
                put("casesWithSecretReveal", results.count { result -> result.searchRevealCount > 0 })
                put("casesWithSearchFailure", results.count { result -> result.searchFailCount > 0 })
                put("maxReachabilityRatio", results.maxOfOrNull(SolvabilityCaseResult::reachabilityRatio) ?: 0f)
                put("providedDiscoveryTagCount", providedDiscoveryTags.size)
                putJsonArray("providedDiscoveryTags") {
                    providedDiscoveryTags.sorted().forEach { tag -> add(JsonPrimitive(tag)) }
                }
                put("hiddenAnchorFamilyFailureCount", results.count { result -> !result.hiddenAnchorFamiliesSatisfied })
                putJsonArray("requiredHiddenAnchorFamilies") {
                    requiredHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
                }
                putJsonArray("observedHiddenAnchorFamilies") {
                    observedHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
                }
            }
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
            putJsonArray("providedDiscoveryTags") {
                providedDiscoveryTags.sorted().forEach { tag -> add(JsonPrimitive(tag)) }
            }
            put("hiddenAnchorFamiliesSatisfied", hiddenAnchorFamiliesSatisfied)
            putJsonArray("requiredHiddenAnchorFamilies") {
                requiredHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
            putJsonArray("observedHiddenAnchorFamilies") {
                observedHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
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

    internal fun topologySummary(generatedFloor: com.ktome.core.mapgen.GeneratedFloor): TopologySummarySnapshot =
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

internal data class SolvabilityExecutedCase(
    val testCase: SolvabilityCase,
    val generatedFloor: com.ktome.core.mapgen.GeneratedFloor?,
    val graph: SolvabilityGraph?,
    val proof: SolvabilityProof?,
    val error: String?,
) {
    fun toCaseResult(): SolvabilityCaseResult {
        val floor = generatedFloor
        val solvabilityGraph = graph
        val solvabilityProof = proof
        if (floor == null || solvabilityGraph == null || solvabilityProof == null) {
            return SolvabilityCaseResult(
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
                providedDiscoveryTags = testCase.providedDiscoveryTags,
                requiredHiddenAnchorFamilies = testCase.requiredHiddenAnchorFamilies,
                observedHiddenAnchorFamilies = emptySet(),
                hiddenAnchorFamiliesSatisfied = false,
                backtrackSatisfied = false,
                error = error,
            )
        }
        val visitedNodeIds = solvabilityProof.visitedNodes.mapTo(linkedSetOf()) { nodeId -> nodeId.value }
        val criticalGateRequirements =
            solvabilityGraph.edges
                .filter { edge ->
                    edge.requiredKeys.isNotEmpty() &&
                        solvabilityGraph.nodes
                            .filter { node -> node.pathClass == PathClass.CRITICAL_PATH }
                            .map { node -> node.id }
                            .contains(edge.to)
                }.flatMap { edge -> edge.requiredKeys }
                .toSet()
        val optionalRequirementProviders =
            solvabilityGraph.nodes
                .filter { node -> node.pathClass == PathClass.OPTIONAL }
                .flatMap { node -> node.grants }
                .toSet()
        val searchStatesByBindingId =
            solvabilityProof.searchStates.associate { entry ->
                entry.bindingId.value to entry.result.name
            }
        val observedHiddenAnchorFamilies = observedHiddenAnchorFamiliesForFloor(floor)
        return SolvabilityCaseResult(
            zoneId = testCase.request.zoneId,
            floorIndex = testCase.request.floorIndex,
            seed = testCase.request.seed,
            criticalPathReachable = solvabilityProof.criticalPathReachable,
            visitedNodes = solvabilityProof.visitedNodes.map { nodeId -> nodeId.value },
            acquiredKeys = solvabilityProof.acquiredKeys.map { requirement -> requirement.value },
            unresolvedRequirements = solvabilityProof.unresolvedRequirements.map { requirement -> requirement.value },
            optionalPathCount = solvabilityProof.optionalPathCount,
            secretPathCount = solvabilityProof.secretPathCount,
            totalReachableNodes = solvabilityProof.totalReachableNodes,
            reachabilityRatio = solvabilityProof.reachabilityRatio,
            topologySummary = SolvabilityHarnessRunner.topologySummary(floor),
            searchActionCount = solvabilityProof.searchActionCount,
            searchRevealCount = solvabilityProof.searchRevealCount,
            searchFailCount = solvabilityProof.searchFailCount,
            searchStates = searchStatesByBindingId,
            secretProofs =
                floor.entrances
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
                floor.resolvedEntranceBindings().map { binding ->
                    ResolvedEntranceBindingSnapshot(
                        bindingId = binding.searchBindingId.value,
                        entranceAnchorId = binding.entranceAnchorId.value,
                        targetNodeId = binding.resolvedTargetNodeId.value,
                    )
                },
            resolvedReturnBridgeNodeIds =
                floor.entrances.associate { entrance ->
                    entrance.bindingId.value to entrance.resolvedReturnBridgeNodeId.value
                },
            providedDiscoveryTags = testCase.providedDiscoveryTags,
            requiredHiddenAnchorFamilies = testCase.requiredHiddenAnchorFamilies,
            observedHiddenAnchorFamilies = observedHiddenAnchorFamilies,
            hiddenAnchorFamiliesSatisfied = testCase.requiredHiddenAnchorFamilies.all(observedHiddenAnchorFamilies::contains),
            backtrackSatisfied =
                criticalGateRequirements.any(optionalRequirementProviders::contains) &&
                    solvabilityProof.criticalPathReachable,
            error = error,
        )
    }
}

internal data class SolvabilityCase(
    val request: MapgenRequest,
    val providedDiscoveryTags: Set<String> = emptySet(),
    val requiredHiddenAnchorFamilies: Set<String> = emptySet(),
)

internal data class SolvabilityCaseResult(
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
    val providedDiscoveryTags: Set<String>,
    val requiredHiddenAnchorFamilies: Set<String>,
    val observedHiddenAnchorFamilies: Set<String>,
    val hiddenAnchorFamiliesSatisfied: Boolean,
    val backtrackSatisfied: Boolean,
    val error: String?,
)

internal data class TopologySummarySnapshot(
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

internal data class SecretProofSnapshot(
    val bindingId: String,
    val entranceAnchorId: String,
    val targetNodeId: String,
    val resolved: Boolean,
    val result: String?,
)

internal data class ResolvedEntranceBindingSnapshot(
    val bindingId: String,
    val entranceAnchorId: String,
    val targetNodeId: String,
)

internal fun primerDiscoveryTagsForCase(
    schemaCatalog: SchemaCatalog,
    zoneId: String,
    floorIndex: Int,
): Set<String> =
    schemaCatalog.hiddenEvents
        .asSequence()
        .filter { hiddenEvent -> hiddenEvent.grantedDiscoveryTags.isNotEmpty() }
        .filter { hiddenEvent ->
            hiddenEvent.conditions.all { condition ->
                when (condition.key) {
                    HiddenConditionKey.ZONE_ID -> condition.expectedValue == zoneId
                    HiddenConditionKey.FLOOR_INDEX -> condition.expectedValue == floorIndex.toString()
                    else -> true
                }
            }
        }.flatMap { hiddenEvent -> hiddenEvent.grantedDiscoveryTags.asSequence() }
        .toCollection(linkedSetOf())

internal fun requiredHiddenAnchorFamiliesForZoneFloor(
    schemaCatalog: SchemaCatalog,
    zoneId: String,
    floorIndex: Int,
): Set<String> =
    schemaCatalog.zones
        .firstOrNull { zone -> zone.id == zoneId }
        ?.let { zone ->
            val profileId = zone.resolvedMapgenProfileId(floorIndex)
            schemaCatalog.zoneMapgenProfiles
                .firstOrNull { profile -> profile.id == profileId }
                ?.hiddenEntrancePlans
                ?.asSequence()
                ?.map { plan -> plan.sourceAnchorId.value }
                ?.toCollection(linkedSetOf())
                ?: emptySet()
        } ?: emptySet()

internal fun requiredHiddenAnchorFamiliesForZone(
    schemaCatalog: SchemaCatalog,
    zoneId: String,
): Set<String> =
    schemaCatalog.zones
        .firstOrNull { zone -> zone.id == zoneId }
        ?.let { zone ->
            (1..zone.floorCount)
                .asSequence()
                .flatMap { floorIndex -> requiredHiddenAnchorFamiliesForZoneFloor(schemaCatalog, zoneId, floorIndex).asSequence() }
                .toCollection(linkedSetOf())
        } ?: emptySet()

internal fun observedHiddenAnchorFamiliesForFloor(
    generatedFloor: com.ktome.core.mapgen.GeneratedFloor,
): Set<String> =
    generatedFloor.entrances
        .asSequence()
        .map { entrance -> entrance.entranceAnchorId }
        .filter { anchorId -> anchorId.isFormalHiddenEntranceAnchor() }
        .map { anchorId -> anchorId.value }
        .toCollection(linkedSetOf())

internal fun primerDiscoveryTagsByZoneAndFloor(
    schemaCatalog: SchemaCatalog,
    upgradedZones: List<ZoneSchemaV2>,
): Map<Pair<String, Int>, Set<String>> =
    buildMap {
        upgradedZones.forEach { zone ->
            (1..zone.floorCount).forEach { floorIndex ->
                put(zone.id to floorIndex, primerDiscoveryTagsForCase(schemaCatalog, zone.id, floorIndex))
            }
        }
    }

internal fun requiredHiddenAnchorFamiliesByZone(
    schemaCatalog: SchemaCatalog,
): Map<String, Set<String>> =
    schemaCatalog.zones.associate { zone -> zone.id to requiredHiddenAnchorFamiliesForZone(schemaCatalog, zone.id) }

internal fun requiredHiddenAnchorFamiliesByZoneAndFloor(
    schemaCatalog: SchemaCatalog,
    upgradedZones: List<ZoneSchemaV2>,
): Map<Pair<String, Int>, Set<String>> =
    buildMap {
        upgradedZones.forEach { zone ->
            (1..zone.floorCount).forEach { floorIndex ->
                put(zone.id to floorIndex, requiredHiddenAnchorFamiliesForZoneFloor(schemaCatalog, zone.id, floorIndex))
            }
        }
    }

internal fun ZoneSchemaV2.isPhase4Upgraded(): Boolean = mapgenProfileId != null
