package com.ktome.tools.phase4

import java.util.Locale
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal const val SAME_ZONE_SECRET_CADENCE_MAX_OVERLAP_TARGET: Double = 0.75
internal const val SAME_ZONE_SECRET_REWARD_MAX_OVERLAP_TARGET: Double = 0.75
internal const val TERMINAL_WEAPON_BASE_DIVERSITY_TARGET: Int = 3
internal const val CROSS_PROFESSION_TOP_WEAPON_DOMINANCE_TARGET: Double = 0.50
internal const val PROFESSION_ALIGNED_WEAPON_ADOPTION_TARGET: Double = 0.75
internal const val ORGANIC_HIDDEN_DISCOVERY_RATE_TARGET: Double = 0.30
internal const val SCRIPTED_HIDDEN_VERIFICATION_RATE_TARGET: Double = 1.0

internal data class Phase4MetricSpec(
    val id: String,
    val ownerTaskId: String,
    val outputSection: String,
    val formula: String,
    val targetText: String,
    val failSemantics: String,
    val decisionNotes: String,
)

@Serializable
internal data class Phase4MetricCatalogEntry(
    @SerialName("metricId")
    val id: String,
    val ownerTaskId: String,
    val sourcePath: String,
    val outputSection: String,
    val formula: String,
    @SerialName("target")
    val targetText: String,
    val failSemantics: String,
    val decisionNotes: String,
)

internal object Phase4MetricCatalog {
    val specs: List<Phase4MetricSpec> =
        listOf(
            Phase4MetricSpec(
                id = "scriptedHiddenVerificationRate",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "scripted-vs-organic-hidden",
                formula = "(totalCases - failureCount) / totalCases",
                targetText = "100.0%",
                failSemantics = "FAIL means scripted correctness, reveal, reward, or return-bridge verification is broken.",
                decisionNotes = "This is a scripted correctness owner metric and must never be interpreted as organic experience evidence.",
            ),
            Phase4MetricSpec(
                id = "organicHiddenDiscoveryRate",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "discoveryWithoutPrimerCount / totalCases",
                targetText = formatPercent(ORGANIC_HIDDEN_DISCOVERY_RATE_TARGET),
                failSemantics = "FAIL is allowed in the first owner-metric pass; it exposes lack of organic discovery rather than scripted correctness breakage.",
                decisionNotes = "Probe runs must not use primer actions or direct reveal APIs; this metric measures real bot/pathing discovery only.",
            ),
            Phase4MetricSpec(
                id = "sameZoneSecretVsCadenceMaxOverlap",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "max(overlap(secretProfile, sameZoneCadenceProfile))",
                targetText = "< ${formatRatio(SAME_ZONE_SECRET_CADENCE_MAX_OVERLAP_TARGET)}",
                failSemantics = "FAIL means same-zone secret rewards are too similar to cadence rewards and local identity is being erased.",
                decisionNotes = "Only same-zone secret vs cadence pairs count; cross-zone averages are not valid evidence.",
            ),
            Phase4MetricSpec(
                id = "sameZoneSecretVsRewardMaxOverlap",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "max(overlap(secretProfile, sameZoneRewardProfile))",
                targetText = "< ${formatRatio(SAME_ZONE_SECRET_REWARD_MAX_OVERLAP_TARGET)}",
                failSemantics = "FAIL means same-zone secret rewards collapse into normal reward channels despite corpus-average separation.",
                decisionNotes = "This guardrail exists specifically to catch high-value local reward collisions hidden by corpus averages.",
            ),
            Phase4MetricSpec(
                id = "terminalWeaponBaseDiversity",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "distinct(fullRoute terminal weapon base ids)",
                targetText = ">= $TERMINAL_WEAPON_BASE_DIVERSITY_TARGET",
                failSemantics = "FAIL means long-run terminal builds are collapsing toward too few weapon bases.",
                decisionNotes = "The metric is read from long-run-full.json and must not be reverse-engineered from markdown.",
            ),
            Phase4MetricSpec(
                id = "crossProfessionTopWeaponDominance",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "count(most common fullRoute terminal weapon) / fullRouteCount",
                targetText = "<= ${formatPercent(CROSS_PROFESSION_TOP_WEAPON_DOMINANCE_TARGET)}",
                failSemantics = "FAIL means one weapon base is dominating terminal builds across professions.",
                decisionNotes = "This metric is the explicit owner gate for the current battle_axe convergence risk.",
            ),
            Phase4MetricSpec(
                id = "professionAlignedWeaponAdoptionRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "alignedFullRouteSampleCount / fullRouteCount",
                targetText = ">= ${formatPercent(PROFESSION_ALIGNED_WEAPON_ADOPTION_TARGET)}",
                failSemantics = "FAIL means profession identity is being swallowed by generic terminal weapon choices.",
                decisionNotes = "Allowed archetypes are frozen in V2OPT-PR-01 and should only change with explicit document updates.",
            ),
            Phase4MetricSpec(
                id = "terrainInteractionEncounterRate.aggregate",
                ownerTaskId = "terrainInteractionBatch",
                outputSection = "terrain-combat-sample-contract",
                formula = "triggeredInteractionCombatCount / taggedCombatCount",
                targetText = ">= terrain baseline relative target",
                failSemantics = "FAIL means aggregate terrain interaction encounter rate regressed below the frozen baseline uplift target.",
                decisionNotes = "Aggregate success is not sufficient; per-zone lower-bound failures must still be surfaced separately.",
            ),
            Phase4MetricSpec(
                id = "terrainInteractionEncounterRate.per_zone_lower_bound",
                ownerTaskId = "terrainInteractionBatch",
                outputSection = "terrain-combat-sample-contract",
                formula = "for each combat-sampled zone: terrainInteractionEncounterRate >= perZoneEncounterLowerBoundTarget",
                targetText = ">= terrainInteractionBatch.perZoneEncounterLowerBoundTarget",
                failSemantics = "FAIL means at least one combat-sampled zone is falling below the minimum encounter language floor even if the aggregate stays green.",
                decisionNotes = "The combat-sampled zone list and exclusion reasons are part of the contract and must be printed beside the metric.",
            ),
        )

    fun entryFor(
        metricId: String,
        sourcePathByTaskId: Map<String, String>,
    ): Phase4MetricCatalogEntry {
        val spec = requireSpec(metricId)
        return Phase4MetricCatalogEntry(
            id = spec.id,
            ownerTaskId = spec.ownerTaskId,
            sourcePath = checkNotNull(sourcePathByTaskId[spec.ownerTaskId]) { "Missing sourcePath for phase4 task '${spec.ownerTaskId}'." },
            outputSection = spec.outputSection,
            formula = spec.formula,
            targetText = spec.targetText,
            failSemantics = spec.failSemantics,
            decisionNotes = spec.decisionNotes,
        )
    }

    fun entries(sourcePathByTaskId: Map<String, String>): List<Phase4MetricCatalogEntry> =
        specs.map { spec -> entryFor(metricId = spec.id, sourcePathByTaskId = sourcePathByTaskId) }

    fun requireSpec(metricId: String): Phase4MetricSpec =
        checkNotNull(specs.firstOrNull { spec -> spec.id == metricId }) { "Missing Phase 4 metric spec for '$metricId'." }
}

internal fun Phase4MetricCatalogEntry.toJson() =
    buildJsonObject {
        put("metricId", id)
        put("ownerTaskId", ownerTaskId)
        put("sourcePath", sourcePath)
        put("outputSection", outputSection)
        put("formula", formula)
        put("target", targetText)
        put("failSemantics", failSemantics)
        put("decisionNotes", decisionNotes)
    }

private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value * 100.0)

private fun formatRatio(value: Double): String = String.format(Locale.US, "%.3f", value)
