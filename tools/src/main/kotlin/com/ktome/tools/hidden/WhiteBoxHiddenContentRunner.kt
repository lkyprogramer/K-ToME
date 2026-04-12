package com.ktome.tools.hidden

import com.ktome.core.harness.whitebox.ArtifactRetentionPolicy
import com.ktome.core.harness.whitebox.WhiteBoxAggregateReport
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxArtifact
import com.ktome.core.harness.whitebox.WhiteBoxCaseReport
import com.ktome.core.harness.whitebox.WhiteBoxCorpusSpec
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import com.ktome.core.world.solvability.SearchActionResult
import com.ktome.tools.whitebox.WhiteBoxDomainWriteRequest
import com.ktome.tools.whitebox.WhiteBoxReportWriter
import com.ktome.tools.whitebox.toVerificationReportHeader
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class WhiteBoxHiddenContentRun(
    val caseCount: Int,
    val failedAssertions: Int,
    val summaryPath: Path,
    val casesPath: Path,
    val reportPath: Path,
)

object WhiteBoxHiddenContentRunner {
    const val HARNESS_ID: String = "whiteBoxHiddenContent"
    private const val DOMAIN_ID: String = "hidden-content"
    private const val CORPUS_ID: String = "P4_OPT_PR05_HIDDEN_CONTENT_WHITEBOX"

    fun run(): WhiteBoxHiddenContentRun {
        val kernelRun =
            if (reuseHarnessOutputs()) {
                HiddenContentHarnessRunner.loadKernelRun() ?: HiddenContentHarnessKernel.execute()
            } else {
                HiddenContentHarnessKernel.execute()
            }
        val analysis = HiddenContentHarnessAnalysis.analyze(kernelRun.results)
        val registryMetrics = HiddenContentRegistrySnapshot.load()
        val outputDir = reportDir().resolveSibling("whitebox").resolve("hidden")
        Files.createDirectories(outputDir)
        val caseReports =
            kernelRun.results.map { result ->
                val joinKey =
                    WhiteBoxJoinKey(
                        seed = result.seed,
                        zoneId = result.zoneId,
                        floorIndex = result.floorIndex,
                        scenarioId = result.searchBindingId.ifBlank { "hidden-case" },
                    )
                WhiteBoxCaseReport(
                    joinKey = joinKey,
                    facts = caseFacts(result = result, registryMetrics = registryMetrics),
                    fingerprints =
                        mapOf(
                            "searchBindingId" to result.searchBindingId.ifBlank { "missing" },
                            "resolvedReturnBridgeNodeId" to result.resolvedReturnBridgeNodeId.ifBlank { "missing" },
                            "triggerType" to result.triggerType.ifBlank { "NONE" },
                        ),
                    assertions = caseAssertions(result),
                    artifacts = writeArtifacts(outputDir = outputDir, joinKey = joinKey, result = result),
                )
            }
        val result =
            WhiteBoxReportWriter.write(
                WhiteBoxDomainWriteRequest(
                    domainId = DOMAIN_ID,
                    outputDir = outputDir,
                    header = kernelRun.header.toVerificationReportHeader(corpusId = CORPUS_ID),
                    corpus =
                        WhiteBoxCorpusSpec(
                            corpusId = CORPUS_ID,
                            description = "625 deterministic Phase 4 hidden-content cases, one per formal search binding and seed, sharing the same session-driven kernel as hiddenContentHarness.",
                            sampleCount = kernelRun.results.size,
                        ),
                    cases = caseReports,
                    aggregates = aggregateReports(kernelRun = kernelRun, analysis = analysis),
                    retentionPolicy = ArtifactRetentionPolicy.ALL,
                ),
            )
        return WhiteBoxHiddenContentRun(
            caseCount = caseReports.size,
            failedAssertions = result.failedAssertions,
            summaryPath = result.summaryPath,
            casesPath = result.casesPath,
            reportPath = result.reportPath,
        )
    }

    private fun reuseHarnessOutputs(): Boolean = System.getProperty("ktome.phase4.reuseHarnessOutputs") == "true"

    private fun caseAssertions(result: HiddenContentCaseResult): List<WhiteBoxAssertionResult> =
        listOf(
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.execution_success",
                passed = result.failure == null,
                message = result.failure ?: "Hidden-content scenario executed successfully.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.search_result_typed",
                passed = result.searchActionResult in SearchActionResult.entries.map(SearchActionResult::name),
                message = "Search action result remains inside the typed SearchActionResult contract.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.hidden_events_optional_or_secret_only",
                passed = result.optionalOnlyTriggerPathClassesWithinOptionalOrSecret,
                message = "optionalOnly hidden events only execute from OPTIONAL / SECRET paths.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.secret_reward_node_present",
                passed = !result.secretZoneEntered || result.secretRewardNodePresent,
                message = "Every entered secret zone exposes a formal reward node.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.reward_bridge_backed_by_loot_budget",
                passed = result.rewardBridgeBackedByLootBudget,
                message = "Hidden-event and secret-zone rewards stay inside the formal LootBudget bridge.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.encounter_bridge_backed_by_threat_budget",
                passed = result.encounterBridgeBackedByThreatBudget,
                message = "Secret encounters remain represented in EncounterThreatBudget deltas.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.search_failure_keeps_mainline_reachable",
                passed = result.searchFailureKeepsMainlineReachable,
                message = "FAILED_CHECK search attempts do not block the mainline route.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.return_bridge_present",
                passed = result.resolvedReturnBridgeNodeId.isNotBlank(),
                message = "Every hidden-content case records a resolved return bridge node id.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.returns_to_mainline",
                passed = result.returnedToMainline,
                message = "Entered secret zones can return to the mainline path.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.solvability_proof_matches_search_result",
                passed = result.solvabilityProofMatchesSearchAction,
                message = "Runtime SearchAction result matches SolvabilityProof for the same binding.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.solvability_proof_covers_return_bridge",
                passed = result.solvabilityProofCoversReturnBridge,
                message = "Resolved return bridge node stays reachable inside SolvabilityProof.",
            ),
            WhiteBoxAssertionResult(
                ruleId = "hidden-content.case.mainline_remains_reachable",
                passed = result.criticalPathReachable,
                message = "Hidden content never becomes a mainline hard gate.",
            ),
        )

    private fun aggregateReports(
        kernelRun: HiddenContentKernelRun,
        analysis: HiddenContentAnalysis,
    ): List<WhiteBoxAggregateReport> {
        val summary = analysis.summary
        return listOf(
            WhiteBoxAggregateReport(
                groupId = "corpus",
                sampleCount = kernelRun.results.size,
                metrics =
                    buildJsonObject {
                        put("hiddenEventTriggerRate", summary.hiddenEventTriggerRate)
                        put("secretZoneDiscoveryRate", summary.secretZoneDiscoveryRate)
                        put("explicitSearchRevealCount", summary.explicitSearchRevealCount)
                        put("zeroHiddenEventZoneCount", summary.zeroHiddenEventZoneCount)
                        put("zeroSecretZoneZoneCount", summary.zeroSecretZoneZoneCount)
                        put("criticalPathFailureCount", summary.criticalPathFailureCount)
                        put("triggerContextFailureCount", summary.triggerContextFailureCount)
                        put("secretRewardNodeMissingCount", summary.secretRewardNodeMissingCount)
                        put("rewardBudgetFailureCount", summary.rewardBudgetFailureCount)
                        put("threatBudgetFailureCount", summary.threatBudgetFailureCount)
                        put("searchFailureBlockingCount", summary.searchFailureBlockingCount)
                        put("proofMismatchCount", summary.proofMismatchCount)
                        put("zoneCount", analysis.zoneBreakdown.size)
                        put("hiddenEventRegistryCount", summary.hiddenEventRegistryCount)
                        put("secretZoneRegistryCount", summary.secretZoneRegistryCount)
                        put("hiddenTriggerTypeCoverage", summary.hiddenTriggerTypeCoverage)
                        putJsonArray("hiddenTriggerTypeSet") {
                            summary.hiddenTriggerTypeSet.sorted().forEach { triggerType -> add(JsonPrimitive(triggerType)) }
                        }
                        put("secretEntranceBindingCoverage", summary.secretEntranceBindingCoverage)
                        putJsonArray("secretEntranceBindingSet") {
                            summary.secretEntranceBindingSet.sorted().forEach { bindingId -> add(JsonPrimitive(bindingId)) }
                        }
                    },
                assertions =
                    listOf(
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.trigger_rate_threshold",
                            passed = summary.hiddenEventTriggerRate >= MIN_HIDDEN_EVENT_TRIGGER_RATE,
                            message = "At least 30% of deterministic cases trigger a hidden event.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.secret_rate_threshold",
                            passed = summary.secretZoneDiscoveryRate >= MIN_SECRET_ZONE_DISCOVERY_RATE,
                            message = "At least 10% of deterministic cases discover a secret zone.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.explicit_search_reveal_present",
                            passed = summary.explicitSearchRevealCount > 0,
                            message = "At least one deterministic case discovers hidden content via explicit SearchAction.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.hidden_event_zone_coverage",
                            passed = summary.zeroHiddenEventZoneCount == 0,
                            message = "Every upgraded target zone records at least one hidden-event trigger.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.secret_zone_coverage",
                            passed = summary.zeroSecretZoneZoneCount == 0,
                            message = "Every upgraded target zone records at least one secret-zone discovery.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.hidden_content_not_mainline_gate",
                            passed = summary.criticalPathFailureCount == 0,
                            message = "Hidden content never becomes a critical-path hard gate.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.hidden_events_optional_or_secret_only",
                            passed = summary.triggerContextFailureCount == 0,
                            message = "optionalOnly hidden events stay constrained to OPTIONAL / SECRET paths.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.hidden_event_registry_count",
                            passed = summary.hiddenEventRegistryCount >= MIN_HIDDEN_EVENT_REGISTRY_COUNT,
                            message = "Hidden event registry count reaches the OPT PR-05 floor.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.secret_reward_node_present",
                            passed = summary.secretRewardNodeMissingCount == 0,
                            message = "Every entered secret zone retains a formal reward node.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.reward_bridge_backed_by_loot_budget",
                            passed = summary.rewardBudgetFailureCount == 0,
                            message = "Reward bridge sources stay visible in LootBudget deltas.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.encounter_bridge_backed_by_threat_budget",
                            passed = summary.threatBudgetFailureCount == 0,
                            message = "Secret encounter bridge sources stay visible in EncounterThreatBudget deltas.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.search_failure_non_blocking",
                            passed = summary.searchFailureBlockingCount == 0,
                            message = "FAILED_CHECK search attempts never block the mainline route.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.return_bridge_proof_consistency",
                            passed = summary.proofMismatchCount == 0,
                            message = "Return bridge facts remain consistent with runtime destination and SolvabilityProof.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.trigger_type_coverage",
                            passed = summary.hiddenTriggerTypeCoverage >= MIN_HIDDEN_TRIGGER_TYPE_COVERAGE,
                            message = "Hidden event registry covers the OPT PR-05 trigger taxonomy floor.",
                        ),
                        WhiteBoxAssertionResult(
                            ruleId = "hidden-content.aggregate.binding_coverage",
                            passed = summary.secretEntranceBindingCoverage >= MIN_SECRET_ENTRANCE_BINDING_COVERAGE,
                            message = "Secret-zone registry covers the OPT PR-05 entrance-binding diversity floor.",
                        ),
                    ),
            ),
        )
    }

    private fun writeArtifacts(
        outputDir: Path,
        joinKey: WhiteBoxJoinKey,
        result: HiddenContentCaseResult,
    ): List<WhiteBoxArtifact> =
        listOf(
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "trigger-timeline",
                kind = "trigger_timeline",
                fileName = "trigger-timeline.md",
                summary = "Search trigger, hidden-event sequence, and log timeline for this deterministic case.",
                content = renderTriggerTimeline(result),
                tags = listOf("timeline", "hidden-content"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "search-action-results",
                kind = "table",
                fileName = "search-action-results.md",
                summary = "Typed SearchAction result table and proof-side search state for the case.",
                content = renderSearchActionResults(result),
                tags = listOf("table", "search"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "return-bridge-proof",
                kind = "proof",
                fileName = "return-bridge-proof.md",
                summary = "Secret-zone entry, resolved return bridge, runtime return node, and proof coverage summary.",
                content = renderReturnBridgeProof(result),
                tags = listOf("proof", "return-bridge"),
            ),
            WhiteBoxReportWriter.writeTextArtifact(
                outputDir = outputDir,
                joinKey = joinKey,
                artifactId = "reward-bridge-summary",
                kind = "summary",
                fileName = "reward-bridge-summary.md",
                summary = "Reward and threat budget bridge summary for hidden events and secret zones.",
                content = renderRewardBridgeSummary(result),
                tags = listOf("summary", "reward-bridge"),
            ),
        )

    private fun renderTriggerTimeline(result: HiddenContentCaseResult): String =
        buildString {
            appendLine("# Hidden Content Timeline")
            appendLine()
            appendLine("- zoneId: ${result.zoneId}")
            appendLine("- seed: ${result.seed}")
            appendLine("- searchBindingId: ${result.searchBindingId}")
            appendLine("- entranceBindingId: ${result.entranceBindingId}")
            appendLine("- searchActionResult: ${result.searchActionResult}")
            appendLine("- triggerType: ${result.triggerType}")
            appendLine("- triggerTypes: ${result.triggerTypes.joinToString().ifBlank { "none" }}")
            appendLine("- triggerPathClasses: ${result.triggerPathClasses.joinToString().ifBlank { "none" }}")
            appendLine("- hiddenEventIds: ${result.hiddenEventIds.joinToString().ifBlank { "none" }}")
            appendLine("- secretZoneId: ${result.secretZoneId ?: "none"}")
            appendLine("- logKeys: ${result.logKeys.joinToString().ifBlank { "none" }}")
            appendLine("- caseFailureReasons: ${result.gateFailureReasons().joinToString().ifBlank { "none" }}")
        }

    private fun renderSearchActionResults(result: HiddenContentCaseResult): String =
        buildString {
            appendLine("# SearchAction Results")
            appendLine()
            appendLine("| Field | Value |")
            appendLine("| --- | --- |")
            appendLine("| searchBindingId | `${result.searchBindingId}` |")
            appendLine("| entranceBindingId | `${result.entranceBindingId}` |")
            appendLine("| runtime searchActionResult | `${result.searchActionResult}` |")
            appendLine("| proof searchActionResult | `${result.proofSearchActionResult ?: "missing"}` |")
            appendLine("| searchFailureKeepsMainlineReachable | `${result.searchFailureKeepsMainlineReachable}` |")
            appendLine("| criticalPathReachable | `${result.criticalPathReachable}` |")
            appendLine("| explicitSearchReveal | `${result.explicitSearchReveal}` |")
            appendLine("| optionalOnlyTriggerPathClassesWithinOptionalOrSecret | `${result.optionalOnlyTriggerPathClassesWithinOptionalOrSecret}` |")
        }

    private fun renderReturnBridgeProof(result: HiddenContentCaseResult): String =
        buildString {
            appendLine("# Secret Zone Entry / Return Bridge Proof")
            appendLine()
            appendLine("| Field | Value |")
            appendLine("| --- | --- |")
            appendLine("| secretZoneEntered | `${result.secretZoneEntered}` |")
            appendLine("| secretRewardNodePresent | `${result.secretRewardNodePresent}` |")
            appendLine("| resolvedReturnBridgeNodeId | `${result.resolvedReturnBridgeNodeId}` |")
            appendLine("| returnedRoomNodeId | `${result.returnedRoomNodeId ?: "none"}` |")
            appendLine("| returnedPoint | `${result.returnedPoint ?: "none"}` |")
            appendLine("| expectedReturnPoint | `${result.expectedReturnPoint ?: "none"}` |")
            appendLine("| returnedToMainline | `${result.returnedToMainline}` |")
            appendLine("| returnBridgeMatchesResolvedNodeId | `${result.returnBridgeMatchesResolvedNodeId}` |")
            appendLine("| solvabilityProofCoversReturnBridge | `${result.solvabilityProofCoversReturnBridge}` |")
        }

    private fun renderRewardBridgeSummary(result: HiddenContentCaseResult): String =
        buildString {
            appendLine("# Reward Bridge Summary")
            appendLine()
            appendLine("## Presentation Sources")
            result.rewardSources.ifEmpty { listOf("none") }.forEach { source -> appendLine("- `${source}`") }
            appendLine()
            appendLine("## LootBudget Sources")
            result.rewardBudgetSources.ifEmpty { listOf("none") }.forEach { source -> appendLine("- `${source}`") }
            appendLine()
            appendLine("## Expected LootBudget Sources")
            result.expectedRewardBudgetSources.ifEmpty { listOf("none") }.forEach { source -> appendLine("- `${source}`") }
            appendLine()
            appendLine("## EncounterThreatBudget Sources")
            result.threatBudgetSources.ifEmpty { listOf("none") }.forEach { source -> appendLine("- `${source}`") }
            appendLine()
            appendLine("## Expected EncounterThreatBudget Sources")
            result.expectedThreatBudgetSources.ifEmpty { listOf("none") }.forEach { source -> appendLine("- `${source}`") }
            appendLine()
            appendLine("- rewardBridgeBackedByLootBudget: `${result.rewardBridgeBackedByLootBudget}`")
            appendLine("- encounterBridgeBackedByThreatBudget: `${result.encounterBridgeBackedByThreatBudget}`")
        }

    private fun caseFacts(
        result: HiddenContentCaseResult,
        registryMetrics: HiddenContentRegistryMetrics,
    ): JsonObject =
        buildJsonObject {
            put("zoneId", result.zoneId)
            put("floorIndex", result.floorIndex)
            put("searchBindingId", result.searchBindingId)
            put("entranceBindingId", result.entranceBindingId)
            put("resolvedReturnBridgeNodeId", result.resolvedReturnBridgeNodeId)
            put("searchActionResult", result.searchActionResult)
            put("triggerType", result.triggerType)
            put("secretZoneId", result.secretZoneId)
            put("secretZoneEntered", result.secretZoneEntered)
            put("secretRewardNodePresent", result.secretRewardNodePresent)
            put("criticalPathReachable", result.criticalPathReachable)
            put("searchFailureKeepsMainlineReachable", result.searchFailureKeepsMainlineReachable)
            put("returnedToMainline", result.returnedToMainline)
            put("returnedRoomNodeId", result.returnedRoomNodeId)
            put("returnedPoint", result.returnedPoint)
            put("expectedReturnPoint", result.expectedReturnPoint)
            put("returnBridgeMatchesResolvedNodeId", result.returnBridgeMatchesResolvedNodeId)
            put("proofSearchActionResult", result.proofSearchActionResult)
            put("solvabilityProofMatchesSearchAction", result.solvabilityProofMatchesSearchAction)
            put("solvabilityProofCoversReturnBridge", result.solvabilityProofCoversReturnBridge)
            put("explicitSearchReveal", result.explicitSearchReveal)
            put("triggerPathClassesWithinOptionalOrSecret", result.optionalOnlyTriggerPathClassesWithinOptionalOrSecret)
            put("optionalOnlyTriggerPathClassesWithinOptionalOrSecret", result.optionalOnlyTriggerPathClassesWithinOptionalOrSecret)
            put("rewardBridgeBackedByLootBudget", result.rewardBridgeBackedByLootBudget)
            put("encounterBridgeBackedByThreatBudget", result.encounterBridgeBackedByThreatBudget)
            putJsonArray("hiddenEventIds") {
                result.hiddenEventIds.forEach { hiddenEventId -> add(JsonPrimitive(hiddenEventId)) }
            }
            putJsonArray("triggerTypes") {
                result.triggerTypes.forEach { triggerType -> add(JsonPrimitive(triggerType)) }
            }
            putJsonArray("hiddenTriggerTypeSet") {
                registryMetrics.hiddenTriggerTypeSet.sorted().forEach { triggerType -> add(JsonPrimitive(triggerType)) }
            }
            put("hiddenEventRegistryCount", registryMetrics.hiddenEventRegistryCount)
            putJsonArray("triggerPathClasses") {
                result.triggerPathClasses.forEach { pathClass -> add(JsonPrimitive(pathClass)) }
            }
            putJsonArray("secretEntranceBindingSet") {
                registryMetrics.secretEntranceBindingSet.sorted().forEach { bindingId -> add(JsonPrimitive(bindingId)) }
            }
            put("secretZoneRegistryCount", registryMetrics.secretZoneRegistryCount)
            putJsonArray("rewardSources") {
                result.rewardSources.forEach { rewardSource -> add(JsonPrimitive(rewardSource)) }
            }
            putJsonArray("rewardBudgetSources") {
                result.rewardBudgetSources.forEach { rewardSource -> add(JsonPrimitive(rewardSource)) }
            }
            putJsonArray("expectedRewardBudgetSources") {
                result.expectedRewardBudgetSources.forEach { rewardSource -> add(JsonPrimitive(rewardSource)) }
            }
            putJsonArray("threatBudgetSources") {
                result.threatBudgetSources.forEach { threatSource -> add(JsonPrimitive(threatSource)) }
            }
            putJsonArray("expectedThreatBudgetSources") {
                result.expectedThreatBudgetSources.forEach { threatSource -> add(JsonPrimitive(threatSource)) }
            }
            putJsonArray("caseFailureReasons") {
                result.gateFailureReasons().forEach { reason -> add(JsonPrimitive(reason)) }
            }
        }
}
