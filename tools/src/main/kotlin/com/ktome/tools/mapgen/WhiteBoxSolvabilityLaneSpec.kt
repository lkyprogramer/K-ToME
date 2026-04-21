package com.ktome.tools.mapgen

import com.ktome.core.harness.whitebox.WhiteBoxAggregateRule
import com.ktome.core.harness.whitebox.WhiteBoxAssertionResult
import com.ktome.core.harness.whitebox.WhiteBoxJoinKey
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

internal data class WhiteBoxSolvabilityLaneSpec(
    val laneId: String,
    val description: String,
    val cases: List<SolvabilityCase>,
    val zoneFloorAggregateRules: List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>>,
    val corpusAggregateRules: List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>>,
)

internal data class WhiteBoxSolvabilityCaseData(
    val laneId: String,
    val executedCase: SolvabilityExecutedCase,
    val result: SolvabilityCaseResult,
) {
    val joinKey: WhiteBoxJoinKey
        get() =
            WhiteBoxJoinKey(
                seed = result.seed,
                zoneId = result.zoneId,
                floorIndex = result.floorIndex,
                scenarioId = laneId,
            )
}

internal fun whiteBoxSolvabilityZoneFloorMetrics(caseData: List<WhiteBoxSolvabilityCaseData>): JsonObject =
    buildJsonObject {
        put("criticalPathFailureCount", caseData.count { data -> !data.result.criticalPathReachable })
        put("backtrackProofCount", caseData.count { data -> data.result.backtrackSatisfied })
        put("casesWithProofTrace", caseData.count { data -> data.result.visitedNodes.isNotEmpty() })
        put("casesWithReveal", caseData.count { data -> data.result.searchRevealCount > 0 })
        put("casesWithFail", caseData.count { data -> data.result.searchFailCount > 0 })
        put("hiddenAnchorFamilyFailureCount", caseData.count { data -> !data.result.hiddenAnchorFamiliesSatisfied })
        put("maxReachabilityRatio", caseData.maxOfOrNull { data -> data.result.reachabilityRatio } ?: 0f)
    }

internal fun whiteBoxSolvabilityCorpusMetrics(caseData: List<WhiteBoxSolvabilityCaseData>): JsonObject =
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
        putJsonArray("failStateTaxonomy") {
            caseData
                .flatMap { data -> data.result.searchStates.values }
                .filter { state -> state != "REVEALED" }
                .toCollection(linkedSetOf())
                .sorted()
                .forEach { state -> add(JsonPrimitive(state)) }
        }
    }

internal fun revealSuccessZoneFloorRules(expectedCaseCount: Int): List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>> =
    listOf(
        WhiteBoxAggregateRule { groupedCases ->
            val metrics = whiteBoxSolvabilityZoneFloorMetrics(groupedCases)
            listOf(
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.zone_floor_case_count",
                    passed = groupedCases.size == expectedCaseCount,
                    message = "Each reveal-success zone/floor corpus should contain $expectedCaseCount deterministic cases.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.zone_floor_critical_path_zero_failures",
                    passed = metrics.metricIntValue("criticalPathFailureCount") == 0,
                    message = "Reveal-success zone/floor corpus keeps critical path failures at 0.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.zone_floor_explainable_proofs",
                    passed = metrics.metricIntValue("casesWithProofTrace") == groupedCases.size,
                    message = "Reveal-success zone/floor corpus keeps a readable proof trace for every sampled case.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.zone_floor_hidden_anchor_families_resolved",
                    passed = metrics.metricIntValue("hiddenAnchorFamilyFailureCount") == 0,
                    message = "Reveal-success zone/floor corpus resolves the formal hidden-anchor families required by the upgraded topology contract.",
                    context = metrics,
                ),
            )
        },
    )

internal val revealSuccessCorpusRules: List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>> =
    listOf(
        WhiteBoxAggregateRule { caseData ->
            val metrics = whiteBoxSolvabilityCorpusMetrics(caseData)
            listOf(
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.corpus_reveal_coverage",
                    passed = metrics.metricIntValue("casesWithReveal") > 0,
                    message = "Reveal-success corpus contains at least one reveal-success case.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.corpus_backtrack_coverage",
                    passed = metrics.metricIntValue("casesWithBacktrackProof") > 0,
                    message = "Reveal-success corpus contains at least one OPTIONAL -> CRITICAL_PATH backtrack proof case.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.corpus_critical_path_zero_failures",
                    passed = metrics.metricIntValue("criticalPathFailureCount") == 0,
                    message = "Reveal-success corpus keeps critical path failures at 0.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_success.corpus_hidden_anchor_families_resolved",
                    passed = metrics.metricIntValue("hiddenAnchorFamilyFailureCount") == 0,
                    message = "Reveal-success corpus resolves the formal hidden-anchor families across all sampled cases.",
                    context = metrics,
                ),
            )
        },
    )

internal fun revealFailZoneFloorRules(expectedCaseCount: Int): List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>> =
    listOf(
        WhiteBoxAggregateRule { groupedCases ->
            val metrics = whiteBoxSolvabilityZoneFloorMetrics(groupedCases)
            listOf(
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_fail.zone_floor_case_count",
                    passed = groupedCases.size == expectedCaseCount,
                    message = "Each reveal-fail zone/floor corpus should contain $expectedCaseCount deterministic fixture case.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_fail.zone_floor_fixture_fails",
                    passed = metrics.metricIntValue("casesWithFail") == groupedCases.size,
                    message = "Reveal-fail fixtures must all preserve failed-search outcomes.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_fail.zone_floor_proof_trace_present",
                    passed = metrics.metricIntValue("casesWithProofTrace") == groupedCases.size,
                    message = "Reveal-fail fixtures still retain a readable proof trace for every sampled case.",
                    context = metrics,
                ),
            )
        },
    )

internal val revealFailCorpusRules: List<WhiteBoxAggregateRule<WhiteBoxSolvabilityCaseData>> =
    listOf(
        WhiteBoxAggregateRule { caseData ->
            val metrics = whiteBoxSolvabilityCorpusMetrics(caseData)
            listOf(
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_fail.corpus_fail_coverage",
                    passed = metrics.metricIntValue("casesWithFail") == caseData.size,
                    message = "Reveal-fail corpus contains only deterministic failed-search fixtures.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_fail.corpus_reveal_absent",
                    passed = metrics.metricIntValue("casesWithReveal") == 0,
                    message = "Reveal-fail corpus must not silently drift back into reveal-success cases.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_fail.corpus_fail_taxonomy",
                    passed = metrics.getValue("failStateTaxonomy").jsonArray.isNotEmpty(),
                    message = "Reveal-fail corpus preserves a readable fail-state taxonomy.",
                    context = metrics,
                ),
                WhiteBoxAssertionResult(
                    ruleId = "solvability.aggregate.reveal_fail.corpus_proof_trace_present",
                    passed = metrics.metricIntValue("casesWithProofTrace") == caseData.size,
                    message = "Reveal-fail corpus preserves proof traces for every fixture case.",
                    context = metrics,
                ),
            )
        },
    )

private fun JsonObject.metricIntValue(key: String): Int = (getValue(key) as JsonPrimitive).content.toInt()
