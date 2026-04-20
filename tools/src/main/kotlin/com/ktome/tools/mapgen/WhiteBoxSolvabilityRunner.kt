package com.ktome.tools.mapgen

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAggregateRule
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCaseRule
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.core.world.solvability.SolvabilityGraph
import com.ktome.game.data.schema.ZoneSchemaV2
import com.ktome.tools.whitebox.WhiteBoxDomainWriteRequest
import com.ktome.tools.whitebox.WhiteBoxReportWriter
import com.ktome.tools.whitebox.WhiteBoxRuleEvaluator
import com.ktome.tools.whitebox.toVerificationReportHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

data class WhiteBoxSolvabilityRun(
    val totalCases: Int,
    val failedAssertions: Int,
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
)

object WhiteBoxSolvabilityRunner {
    const val HARNESS_ID: String = "whiteBoxSolvability"
    private const val DOMAIN_ID: String = "solvability"
    private const val SEEDS_PER_FLOOR: Int = 5
    private const val CORPUS_ID: String = "P4_OPT_PR05_SOLVABILITY_WHITEBOX"
    private val caseRules: List<WhiteBoxCaseRule<WhiteBoxSolvabilityCaseData>> =
        listOf(
            WhiteBoxCaseRule { caseData ->
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.execution_success",
                        passed = caseData.result.error == null,
                        message = caseData.result.error ?: "Solvability proof execution succeeded.",
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.critical_path_reachable",
                        passed = caseData.result.criticalPathReachable,
                        message = "Critical path remains reachable under the proof runner.",
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.unresolved_requirements_empty",
                        passed = caseData.result.unresolvedRequirements.isEmpty(),
                        message = "Critical-path proof leaves no unresolved requirements.",
                        context =
                            buildJsonObject {
                                put("unresolvedRequirementCount", caseData.result.unresolvedRequirements.size)
                            },
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.proof_trace_present",
                        passed = caseData.result.visitedNodes.isNotEmpty(),
                        message = "Successful proof execution records a readable visited-node trace.",
                        context =
                            buildJsonObject {
                                put("visitedNodeCount", caseData.result.visitedNodes.size)
                                put("searchStateCount", caseData.result.searchStates.size)
                                put("secretProofCount", caseData.result.secretProofs.size)
                            },
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                val validSearchStates = SearchActionResult.entries.map(SearchActionResult::name).toSet()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.search_states_typed",
                        passed = caseData.result.searchStates.values.all(validSearchStates::contains),
                        message = "All search states stay within the typed SearchActionResult contract.",
                        context =
                            buildJsonObject {
                                putJsonArray("states") {
                                    caseData.result.searchStates.values.sorted().forEach { state -> add(JsonPrimitive(state)) }
                                }
                            },
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.hidden_anchor_families_resolved",
                        passed = caseData.result.hiddenAnchorFamiliesSatisfied,
                        message = "Hidden entrance anchor families observed in the topology satisfy the zone's formal hidden-anchor contract.",
                        context =
                            buildJsonObject {
                                putJsonArray("requiredHiddenAnchorFamilies") {
                                    caseData.result.requiredHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
                                }
                                putJsonArray("observedHiddenAnchorFamilies") {
                                    caseData.result.observedHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
                                }
                            },
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.reveal_fail_non_blocking",
                        passed = caseData.result.searchFailCount == 0 || caseData.result.criticalPathReachable,
                        message = "Reveal failure never blocks the critical path.",
                        context =
                            buildJsonObject {
                                put("searchFailCount", caseData.result.searchFailCount)
                                put("criticalPathReachable", caseData.result.criticalPathReachable)
                            },
                    ),
                )
            },
            WhiteBoxCaseRule { caseData ->
                caseData.executedCase.generatedFloor ?: return@WhiteBoxCaseRule emptyList()
                val proofBindingIds = caseData.result.secretProofs.map(SecretProofSnapshot::bindingId).toSet()
                val resolvedBindingIds = caseData.result.resolvedEntranceBindings.map(ResolvedEntranceBindingSnapshot::bindingId).toSet()
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.case.secret_bindings_consistent",
                        passed =
                            resolvedBindingIds.all(proofBindingIds::contains) &&
                                caseData.result.resolvedReturnBridgeNodeIds.keys.all(proofBindingIds::contains),
                        message = "Secret proof bindings and resolved entrance bindings stay consistent.",
                    ),
                )
            },
        )
    private val zoneFloorAggregateRules: List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>> =
        listOf(
            WhiteBoxAggregateRule { groupedCases ->
                val metrics = zoneFloorMetrics(groupedCases)
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.zone_floor_case_count",
                        passed = groupedCases.size == SEEDS_PER_FLOOR,
                        message = "Each zone/floor corpus should contain $SEEDS_PER_FLOOR deterministic cases.",
                        context = metrics,
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.zone_floor_critical_path_zero_failures",
                        passed = metrics.intValue("criticalPathFailureCount") == 0,
                        message = "Each zone/floor corpus keeps critical path failures at 0.",
                        context = metrics,
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.zone_floor_explainable_proofs",
                        passed = metrics.intValue("casesWithProofTrace") == groupedCases.size,
                        message = "Each zone/floor corpus keeps a readable proof trace for every sampled case.",
                        context = metrics,
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.zone_floor_hidden_anchor_families_resolved",
                        passed = metrics.intValue("hiddenAnchorFamilyFailureCount") == 0,
                        message = "Each zone/floor corpus resolves the formal hidden-anchor families required by the upgraded topology contract.",
                        context = metrics,
                    ),
                )
            },
        )
    private val corpusAggregateRules: List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>> =
        listOf(
            WhiteBoxAggregateRule { caseData ->
                val metrics = corpusMetrics(caseData)
                listOf(
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.corpus_reveal_coverage",
                        passed = metrics.intValue("casesWithReveal") > 0,
                        message = "Corpus contains at least one reveal-success case.",
                        context = metrics,
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.corpus_backtrack_coverage",
                        passed = metrics.intValue("casesWithBacktrackProof") > 0,
                        message = "Corpus contains at least one OPTIONAL -> CRITICAL_PATH backtrack proof case.",
                        context = metrics,
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.corpus_critical_path_zero_failures",
                        passed = metrics.intValue("criticalPathFailureCount") == 0,
                        message = "Corpus keeps critical path failures at 0.",
                        context = metrics,
                    ),
                    WhiteBoxAssertionResult(
                        ruleId = "solvability.aggregate.corpus_hidden_anchor_families_resolved",
                        passed = metrics.intValue("hiddenAnchorFamilyFailureCount") == 0,
                        message = "Corpus resolves the formal hidden-anchor families required by OPT PR-05 across all sampled cases.",
                        context = metrics,
                    ),
                )
            },
        )

    fun run(): WhiteBoxSolvabilityRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)

        val executionContext = MapgenSmokeRunner.loadExecutionContext()
        val upgradedZones = executionContext.schemaCatalog.zones.filter(ZoneSchemaV2::isPhase4Upgraded).sortedBy(ZoneSchemaV2::id)
        val cases =
            SolvabilityHarnessRunner.buildCases(
                upgradedZones = upgradedZones,
                primerDiscoveryTagsByZoneAndFloor = primerDiscoveryTagsByZoneAndFloor(executionContext.schemaCatalog, upgradedZones),
                requiredHiddenAnchorFamiliesByZoneAndFloor =
                    requiredHiddenAnchorFamiliesByZoneAndFloor(executionContext.schemaCatalog, upgradedZones),
                seedsPerFloor = SEEDS_PER_FLOOR,
            )
        val distinctSeedList = cases.map { case -> case.request.seed }.distinct()
        require(distinctSeedList.size == cases.size) {
            "whiteBoxSolvability corpus must keep a one-to-one seed corpus; got ${distinctSeedList.size} distinct seeds for ${cases.size} cases."
        }
        val header =
            phase4HarnessHeader(harnessId = HARNESS_ID, seedList = distinctSeedList)
                .toVerificationReportHeader(corpusId = CORPUS_ID)
        val corpus =
            WhiteBoxCorpusSpec(
                corpusId = CORPUS_ID,
                description = "First 5 deterministic solvability seeds per floor for the 4 Phase 4 upgraded zones after OPT PR-05 hidden primer and anchor alignment.",
                sampleCount = cases.size,
            )

        val caseData =
            cases.map { testCase ->
                val executedCase = SolvabilityHarnessRunner.executeCase(executionContext, testCase)
                val result = executedCase.toCaseResult()
                WhiteBoxSolvabilityCaseData(
                    executedCase = executedCase,
                    result = result,
                )
            }
        val caseReports =
            caseData.map { caseDataEntry ->
                val assertions = caseAssertions(caseDataEntry)
                val artifacts =
                    if (
                        caseDataEntry.executedCase.generatedFloor != null &&
                        WhiteBoxReportWriter.shouldWriteArtifacts(
                            retentionPolicy = ArtifactRetentionPolicy.ALL,
                            joinKey = caseDataEntry.joinKey,
                            assertions = assertions,
                        )
                    ) {
                        writeArtifacts(
                            outputDir = outputDir,
                            caseData = caseDataEntry,
                        )
                    } else {
                        emptyList()
                    }
                WhiteBoxCaseReport(
                    joinKey = caseDataEntry.joinKey,
                    facts = caseFacts(caseDataEntry),
                    fingerprints = fingerprints(caseDataEntry),
                    assertions = assertions,
                    artifacts = artifacts,
                )
            }
        val aggregates = buildAggregates(caseData)
        val writeResult =
            WhiteBoxReportWriter.write(
                WhiteBoxDomainWriteRequest(
                    domainId = DOMAIN_ID,
                    outputDir = outputDir,
                    header = header,
                    corpus = corpus,
                    cases = caseReports,
                    aggregates = aggregates,
                    retentionPolicy = ArtifactRetentionPolicy.ALL,
                ),
            )
        return WhiteBoxSolvabilityRun(
            totalCases = caseReports.size,
            failedAssertions = writeResult.failedAssertions,
            summaryPath = writeResult.summaryPath,
            casesPath = writeResult.casesPath,
            reportPath = writeResult.reportPath,
        )
    }

    private fun caseAssertions(caseData: WhiteBoxSolvabilityCaseData): List<WhiteBoxAssertionResult> =
        WhiteBoxRuleEvaluator.evaluateCaseRules(caseData, caseRules)

    private fun buildAggregates(caseData: List<WhiteBoxSolvabilityCaseData>): List<WhiteBoxAggregateReport> {
        val byZoneFloor = caseData.groupBy { data -> data.result.zoneId to data.result.floorIndex }
        val zoneFloorAggregates =
            byZoneFloor.toSortedMap(compareBy<Pair<String, Int>> { pair -> pair.first }.thenBy { pair -> pair.second })
                .map { (zoneFloor, groupedCases) ->
                    WhiteBoxAggregateReport(
                        groupId = "${zoneFloor.first}:floor-${zoneFloor.second}",
                        sampleCount = groupedCases.size,
                        metrics = zoneFloorMetrics(groupedCases),
                        assertions = WhiteBoxRuleEvaluator.evaluateAggregateRules(groupedCases, zoneFloorAggregateRules),
                    )
                }
        val corpusAggregate =
            WhiteBoxAggregateReport(
                groupId = "corpus",
                sampleCount = caseData.size,
                metrics = corpusMetrics(caseData),
                assertions = WhiteBoxRuleEvaluator.evaluateAggregateRules(caseData, corpusAggregateRules),
            )
        return zoneFloorAggregates + corpusAggregate
    }

    private fun zoneFloorMetrics(caseData: List<WhiteBoxSolvabilityCaseData>): JsonObject =
        buildJsonObject {
            put("criticalPathFailureCount", caseData.count { data -> !data.result.criticalPathReachable })
            put("backtrackProofCount", caseData.count { data -> data.result.backtrackSatisfied })
            put(
                "casesWithProofTrace",
                caseData.count { data -> data.result.visitedNodes.isNotEmpty() },
            )
            put("casesWithReveal", caseData.count { data -> data.result.searchRevealCount > 0 })
            put("casesWithFail", caseData.count { data -> data.result.searchFailCount > 0 })
            put("hiddenAnchorFamilyFailureCount", caseData.count { data -> !data.result.hiddenAnchorFamiliesSatisfied })
            put("maxReachabilityRatio", caseData.maxOfOrNull { data -> data.result.reachabilityRatio } ?: 0f)
        }

    private fun corpusMetrics(caseData: List<WhiteBoxSolvabilityCaseData>): JsonObject =
        buildJsonObject {
            put("criticalPathFailureCount", caseData.count { data -> !data.result.criticalPathReachable })
            put("casesWithReveal", caseData.count { data -> data.result.searchRevealCount > 0 })
            put("casesWithFail", caseData.count { data -> data.result.searchFailCount > 0 })
            put("casesWithBacktrackProof", caseData.count { data -> data.result.backtrackSatisfied })
            put("casesWithProofTrace", caseData.count { data -> data.result.visitedNodes.isNotEmpty() })
            put("hiddenAnchorFamilyFailureCount", caseData.count { data -> !data.result.hiddenAnchorFamiliesSatisfied })
            putJsonArray("providedDiscoveryTags") {
                caseData.flatMapTo(linkedSetOf(), { data -> data.result.providedDiscoveryTags }).sorted().forEach { tag -> add(JsonPrimitive(tag)) }
            }
            putJsonArray("requiredHiddenAnchorFamilies") {
                caseData.flatMapTo(linkedSetOf(), { data -> data.result.requiredHiddenAnchorFamilies }).sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
            putJsonArray("observedHiddenAnchorFamilies") {
                caseData.flatMapTo(linkedSetOf(), { data -> data.result.observedHiddenAnchorFamilies }).sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
        }

    private fun caseFacts(caseData: WhiteBoxSolvabilityCaseData): JsonObject {
        val result = caseData.result
        return buildJsonObject {
            put("seed", result.seed)
            put("zoneId", result.zoneId)
            put("floorIndex", result.floorIndex)
            put("criticalPathReachable", result.criticalPathReachable)
            put("optionalPathCount", result.optionalPathCount)
            put("secretPathCount", result.secretPathCount)
            put("totalReachableNodes", result.totalReachableNodes)
            put("reachabilityRatio", result.reachabilityRatio)
            put("searchActionCount", result.searchActionCount)
            put("searchRevealCount", result.searchRevealCount)
            put("searchFailCount", result.searchFailCount)
            put("backtrackSatisfied", result.backtrackSatisfied)
            putJsonArray("providedDiscoveryTags") {
                result.providedDiscoveryTags.sorted().forEach { tag -> add(JsonPrimitive(tag)) }
            }
            put("hiddenAnchorFamiliesSatisfied", result.hiddenAnchorFamiliesSatisfied)
            putJsonArray("requiredHiddenAnchorFamilies") {
                result.requiredHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
            putJsonArray("observedHiddenAnchorFamilies") {
                result.observedHiddenAnchorFamilies.sorted().forEach { family -> add(JsonPrimitive(family)) }
            }
            putJsonArray("visitedNodes") {
                result.visitedNodes.forEach { nodeId -> add(JsonPrimitive(nodeId)) }
            }
            putJsonArray("acquiredKeys") {
                result.acquiredKeys.forEach { requirement -> add(JsonPrimitive(requirement)) }
            }
            putJsonArray("unresolvedRequirements") {
                result.unresolvedRequirements.forEach { requirement -> add(JsonPrimitive(requirement)) }
            }
            putJsonObject("searchStates") {
                result.searchStates.toSortedMap().forEach { (bindingId, state) -> put(bindingId, state) }
            }
            putJsonArray("secretProofs") {
                result.secretProofs.forEach { proof ->
                    add(
                        buildJsonObject {
                            put("bindingId", proof.bindingId)
                            put("entranceAnchorId", proof.entranceAnchorId)
                            put("targetNodeId", proof.targetNodeId)
                            put("resolved", proof.resolved)
                            proof.result?.let { resolved -> put("result", resolved) }
                        },
                    )
                }
            }
            putJsonArray("resolvedEntranceBindings") {
                result.resolvedEntranceBindings.forEach { binding ->
                    add(
                        buildJsonObject {
                            put("bindingId", binding.bindingId)
                            put("entranceAnchorId", binding.entranceAnchorId)
                            put("targetNodeId", binding.targetNodeId)
                        },
                    )
                }
            }
            result.error?.let { error -> put("error", error) }
        }
    }

    private fun fingerprints(caseData: WhiteBoxSolvabilityCaseData): Map<String, String> =
        buildMap {
            put("visitedNodes", caseData.result.visitedNodes.joinToString(separator = "|").ifBlank { "none" })
            put("acquiredKeys", caseData.result.acquiredKeys.joinToString(separator = "|").ifBlank { "none" })
            put(
                "searchStates",
                caseData.result.searchStates.toSortedMap().entries.joinToString(separator = "|") { (bindingId, state) -> "$bindingId=$state" }.ifBlank { "none" },
            )
        }

    private fun writeArtifacts(
        outputDir: Path,
        caseData: WhiteBoxSolvabilityCaseData,
    ): List<com.ktome.core.harness.whitebox.WhiteBoxArtifact> {
        val floor = requireNotNull(caseData.executedCase.generatedFloor)
        val graph = requireNotNull(caseData.executedCase.graph)
        val proof = requireNotNull(caseData.executedCase.proof)
        return listOf(
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "topology-overview",
                kind = "overview",
                fileName = "topology-overview.md",
                summary = "Topology and reachability overview for the proof case.",
                content = renderTopologyOverview(caseData, floor, graph),
                tags = listOf("markdown"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "proof-path",
                kind = "trace",
                fileName = "proof-path.md",
                summary = "Visited nodes, acquired requirements, and unresolved requirements.",
                content = renderProofPath(caseData, proof),
                tags = listOf("markdown"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "search-state-table",
                kind = "table",
                fileName = "search-state-table.md",
                summary = "Search action counts and per-binding typed results.",
                content = renderSearchStates(caseData),
                tags = listOf("markdown"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = caseData.joinKey,
                artifactId = "secret-proof-table",
                kind = "table",
                fileName = "secret-proof-table.md",
                summary = "Secret entrance proof and resolved entrance binding table.",
                content = renderSecretProofs(caseData),
                tags = listOf("markdown"),
            ),
        )
    }

    private fun renderTopologyOverview(
        caseData: WhiteBoxSolvabilityCaseData,
        floor: com.ktome.core.mapgen.GeneratedFloor,
        graph: SolvabilityGraph,
    ): String =
        buildString {
            appendLine("# Topology Overview")
            appendLine("- zoneId: `${caseData.result.zoneId}`")
            appendLine("- floorIndex: `${caseData.result.floorIndex}`")
            appendLine("- seed: `${caseData.result.seed}`")
            appendLine("- entryNodeId: `${graph.entryNodeId.value}`")
            appendLine("- criticalPathReachable: `${caseData.result.criticalPathReachable}`")
            appendLine("- optionalPathCount: `${caseData.result.optionalPathCount}`")
            appendLine("- secretPathCount: `${caseData.result.secretPathCount}`")
            appendLine("- totalReachableNodes: `${caseData.result.totalReachableNodes}`")
            appendLine("- roomCount: `${floor.rooms.size}`")
            appendLine("- entranceCount: `${floor.entrances.size}`")
            appendLine()
            appendLine("## Path Class Counts")
            caseData.result.topologySummary.pathClassCounts.toSortedMap().forEach { (pathClass, count) ->
                appendLine("- `${pathClass}` = `${count}`")
            }
        }

    private fun renderProofPath(
        caseData: WhiteBoxSolvabilityCaseData,
        proof: com.ktome.core.world.solvability.SolvabilityProof,
    ): String =
        buildString {
            appendLine("# Proof Path")
            appendLine("## Visited Nodes")
            if (caseData.result.visitedNodes.isEmpty()) {
                appendLine("- none")
            } else {
                caseData.result.visitedNodes.forEachIndexed { index, nodeId ->
                    appendLine("${index + 1}. `${nodeId}`")
                }
            }
            appendLine()
            appendLine("## Acquired Keys")
            if (caseData.result.acquiredKeys.isEmpty()) {
                appendLine("- none")
            } else {
                caseData.result.acquiredKeys.forEach { requirement -> appendLine("- `${requirement}`") }
            }
            appendLine()
            appendLine("## Unresolved Requirements")
            if (caseData.result.unresolvedRequirements.isEmpty()) {
                appendLine("- none")
            } else {
                caseData.result.unresolvedRequirements.forEach { requirement -> appendLine("- `${requirement}`") }
            }
            appendLine()
            appendLine("## Counters")
            appendLine("- searchActionCount: `${proof.searchActionCount}`")
            appendLine("- searchRevealCount: `${proof.searchRevealCount}`")
            appendLine("- searchFailCount: `${proof.searchFailCount}`")
            appendLine("- backtrackSatisfied: `${caseData.result.backtrackSatisfied}`")
        }

    private fun renderSearchStates(caseData: WhiteBoxSolvabilityCaseData): String =
        buildString {
            appendLine("# Search States")
            appendLine("| Binding | Result |")
            appendLine("| --- | --- |")
            if (caseData.result.searchStates.isEmpty()) {
                appendLine("| none | none |")
            } else {
                caseData.result.searchStates.toSortedMap().forEach { (bindingId, state) ->
                    appendLine("| `${bindingId}` | `${state}` |")
                }
            }
        }

    private fun renderSecretProofs(caseData: WhiteBoxSolvabilityCaseData): String =
        buildString {
            appendLine("# Secret Proofs")
            appendLine("## Secret Entrances")
            appendLine("| Binding | Anchor | Target Node | Resolved | Result |")
            appendLine("| --- | --- | --- | --- | --- |")
            if (caseData.result.secretProofs.isEmpty()) {
                appendLine("| none | none | none | none | none |")
            } else {
                caseData.result.secretProofs.forEach { proof ->
                    appendLine(
                        "| `${proof.bindingId}` | `${proof.entranceAnchorId}` | `${proof.targetNodeId}` | `${proof.resolved}` | `${proof.result ?: "none"}` |",
                    )
                }
            }
            appendLine()
            appendLine("## Resolved Entrance Bindings")
            appendLine("| Binding | Anchor | Target Node | Return Bridge |")
            appendLine("| --- | --- | --- | --- |")
            if (caseData.result.resolvedEntranceBindings.isEmpty()) {
                appendLine("| none | none | none | none |")
            } else {
                caseData.result.resolvedEntranceBindings.forEach { binding ->
                    appendLine(
                        "| `${binding.bindingId}` | `${binding.entranceAnchorId}` | `${binding.targetNodeId}` | `${caseData.result.resolvedReturnBridgeNodeIds[binding.bindingId] ?: "missing"}` |",
                    )
                }
            }
        }

    private fun reportDir(): Path {
        val configured = System.getProperty("ktome.phase4.whitebox.solvability.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "phase4", "whitebox", DOMAIN_ID)
        } else {
            Path.of(configured)
        }
    }
}

private data class WhiteBoxSolvabilityCaseData(
    val executedCase: SolvabilityExecutedCase,
    val result: SolvabilityCaseResult,
) {
    val joinKey: WhiteBoxJoinKey
        get() =
            WhiteBoxJoinKey(
                seed = result.seed,
                zoneId = result.zoneId,
                floorIndex = result.floorIndex,
            )
}

private fun JsonObject.intValue(key: String): Int = (getValue(key) as JsonPrimitive).content.toInt()
