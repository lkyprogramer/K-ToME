package com.ktome.tools.phase4

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal data class Phase4MetricSpec(
    val id: String,
    val ownerTaskId: String,
    val outputSection: String,
    val formula: String,
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
                failSemantics = "FAIL means scripted correctness, reveal, reward, or return-bridge verification is broken.",
                decisionNotes = "This is a scripted correctness owner metric and must never be interpreted as organic experience evidence.",
            ),
            Phase4MetricSpec(
                id = "leadDiscoveryRate",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "leadDiscoveryCount / totalCases",
                failSemantics = "FAIL is allowed in the first owner-metric pass; it exposes missing lead discovery rather than scripted correctness breakage.",
                decisionNotes = "Lead discovery only counts revealed entrances or actual secret-zone entry; firstHiddenDiscoveryTurn and discovery distributions are supporting evidence, not substitute headline success signals.",
            ),
            Phase4MetricSpec(
                id = "secretConversionRate",
                ownerTaskId = "organicHiddenProbe",
                outputSection = "scripted-vs-organic-hidden",
                formula = "secretConversionCount / leadDiscoveryCount",
                failSemantics = "FAIL means the organic probe can see hidden leads but still does not convert them into real secret-zone entry often enough.",
                decisionNotes = "The report must also surface per-zone secret-entry failures and supporting timing evidence so zones like abyssal_temple cannot hide behind aggregate discovery.",
            ),
            Phase4MetricSpec(
                id = "sameZoneSecretVsCadenceMaxOverlap",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "max(overlap(secretProfile, sameZoneCadenceProfile))",
                failSemantics = "FAIL means same-zone secret rewards are too similar to cadence rewards and local identity is being erased.",
                decisionNotes = "Only same-zone secret vs cadence pairs count; cross-zone averages are not valid evidence.",
            ),
            Phase4MetricSpec(
                id = "sameZoneSecretVsRewardMaxOverlap",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "max(overlap(secretProfile, sameZoneRewardProfile))",
                failSemantics = "FAIL means same-zone secret rewards collapse into normal reward channels despite corpus-average separation.",
                decisionNotes = "This guardrail exists specifically to catch high-value local reward collisions hidden by corpus averages.",
            ),
            Phase4MetricSpec(
                id = "dynamicPoolCoverage",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "dynamicPoolTargetProfileCount / 10",
                failSemantics = "FAIL means at least one PR-02 target profile still ships as a fixed list instead of a dynamic pool.",
                decisionNotes = "The denominator is frozen to the ten PR-02 target profiles; partial coverage is not acceptable evidence.",
            ),
            Phase4MetricSpec(
                id = "specialTierPassiveFamilyDuplicateCount",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "count(same-zone duplicate passive families across unique/artifact templates)",
                failSemantics = "FAIL means same-zone special-tier rewards still collapse into overlapping passive families.",
                decisionNotes = "This metric is derived from the official special-template corpus and should remain at zero once PR-02 identity cleanup lands.",
            ),
            Phase4MetricSpec(
                id = "secretZoneRewardAuthorityViolations",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "count(shared secret reward authority scan violations for secret-zone bindings)",
                failSemantics = "FAIL means the shared SecretRewardAuthority contract no longer resolves secret-zone rewards solely from SecretZoneDef.rewardProfileId.",
                decisionNotes = "This is the single-source-of-truth guardrail for the landed authority helper; non-secret-zone LOOT_PROFILE remains legal, but secret-zone fallback drift must stay at zero.",
            ),
            Phase4MetricSpec(
                id = "frontstageHighPriorityCueRetainedRate",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "cases retaining CRITICAL/HIGH frontstage action cue / cases expected to surface high-priority search or secret cue",
                failSemantics = "FAIL means high-value search/secret cues can be displaced before the player-facing snapshot exposes them.",
                decisionNotes = "This metric must consume typed FrontstageActionCueSnapshot priority/category fields, not localized text or client-side ordering.",
            ),
            Phase4MetricSpec(
                id = "frontstageCueDedupAppliedCount",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "count(focused repeated-stableKey probes where duplicate log entries collapse to one frontstage cue)",
                failSemantics = "FAIL means stableKey replacement is not active and repeated triggers can grow the frontstage queue.",
                decisionNotes = "Dedup is verified with the runtime session contract so the report does not infer queue behavior from display text.",
            ),
            Phase4MetricSpec(
                id = "frontstageCueExpiryParity",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "focused TTL probe pass count / focused TTL probe count",
                failSemantics = "FAIL means priority-specific TTL semantics drifted from the formal action cue contract.",
                decisionNotes = "The probe uses the same session snapshot path as player rendering and locks CRITICAL/HIGH/MEDIUM/LOW expiry boundaries.",
            ),
            Phase4MetricSpec(
                id = "frontstageSecretCueVisibilityRate",
                ownerTaskId = "hiddenContentHarness",
                outputSection = "frontstage-action-cue",
                formula = "entered secret-zone cases with SECRET frontstage cue / entered secret-zone case count",
                failSemantics = "FAIL means secret-zone action cues are logged or white-box visible but not retained in player-facing frontstage snapshot.",
                decisionNotes = "Secret cue visibility is anchored in the hidden-content corpus and typed category field, not a client tone heuristic.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneSourceCoverage.reportOnly",
                ownerTaskId = "whiteBoxLoot",
                outputSection = "local-reward-identity",
                formula = "covered preferred profession/source pairs / total preferred profession/source pairs",
                failSemantics = "FAIL means at least one preferred profession/source pair still has no legal capstone path on the official main-path sources.",
                decisionNotes = "Unlike the staged adoption and non-weapon floors, preferred source coverage is part of the current hardening contract and must fail before long-run verification.",
            ),
            Phase4MetricSpec(
                id = "avgObjectiveAcquireTurn",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "min(critical-path zone avgObjectiveAcquireTurn)",
                failSemantics = "FAIL means at least one critical-path zone still allows objective acquisition too close to zone entry.",
                decisionNotes = "The report must keep per-zone breakdown and failing-zone ids; this metric is the minimum across critical-path zones, not a corpus average.",
            ),
            Phase4MetricSpec(
                id = "avgVisibleHostileTurnCount",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "min(critical-path zone avgVisibleHostileTurnCount)",
                failSemantics = "FAIL means at least one critical-path zone is still effectively combat-empty in visible hostile turns.",
                decisionNotes = "This is a pacing floor guardrail, not a balance tuning target; the minimum critical-path zone owns the verdict.",
            ),
            Phase4MetricSpec(
                id = "avgEnemyTurns",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "min(critical-path zone avgEnemyTurns)",
                failSemantics = "FAIL means at least one critical-path zone still collapses to zero or near-zero enemy activity.",
                decisionNotes = "Use longRunLab zone traversal diagnostics directly; do not reconstruct this from markdown.",
            ),
            Phase4MetricSpec(
                id = "criticalPathCombatFloorSatisfied",
                ownerTaskId = "longRunLab",
                outputSection = "critical-path-pacing",
                formula = "satisfiedCriticalPathZoneCount / criticalPathZoneCount",
                failSemantics = "FAIL means at least one critical-path zone misses the formal objective/combat pacing floors.",
                decisionNotes = "This is the aggregate pacing owner gate; the section must still print which zones failed and why.",
            ),
            Phase4MetricSpec(
                id = "terminalWeaponBaseDiversity",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "distinct(fullRoute terminal weapon base ids)",
                failSemantics = "FAIL means long-run terminal builds are collapsing toward too few weapon bases.",
                decisionNotes = "The metric is read from long-run-full.json and must not be reverse-engineered from markdown.",
            ),
            Phase4MetricSpec(
                id = "crossProfessionTopWeaponDominance",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "count(most common fullRoute terminal weapon) / fullRouteCount",
                failSemantics = "FAIL means one weapon base is dominating terminal builds across professions.",
                decisionNotes = "This metric is the explicit owner gate for the current battle_axe convergence risk.",
            ),
            Phase4MetricSpec(
                id = "professionAlignedWeaponAdoptionRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "alignedFullRouteSampleCount / fullRouteCount",
                failSemantics = "FAIL means profession identity is being swallowed by generic terminal weapon choices.",
                decisionNotes = "Allowed archetypes are frozen in V2OPT-PR-01 and should only change with explicit document updates.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneSeenRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "fullRoute runs with at least one profession capstone seen / fullRouteCount",
                failSemantics = "FAIL means main-path capstone chase targets still are not materially visible across full-route runs.",
                decisionNotes = "This metric must be derived from milestone reward summaries and printed with per-profession capstone breakdown.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneAdoptionRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "fullRoute runs adopting at least one profession capstone / fullRouteCount",
                failSemantics = "FAIL means profession capstones are being seen but still are not converting into terminal-build decisions often enough.",
                decisionNotes = "Use the same profession-capstone milestone summary as professionCapstoneSeenRate; the report must keep the shared per-profession breakdown visible.",
            ),
            Phase4MetricSpec(
                id = "nonWeaponBuildPayoffRate",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "fullRoute runs adopting a non-weapon profession capstone / fullRouteCount",
                failSemantics = "FAIL means OFF_HAND / ARMOR build-defining payoff still is not landing often enough in terminal builds.",
                decisionNotes = "Use the same profession-capstone evidence chain as professionCapstoneSeenRate; do not invent a second build summary source.",
            ),
            Phase4MetricSpec(
                id = "professionCapstoneAdoptionFloor.reportOnly",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "count(professions meeting build-identity adoption floor) / foundationProfessionCount",
                failSemantics = "APPROVED_DEBT means per-profession adoption floor is visible but not blocking yet.",
                decisionNotes = "The floor is derived from build-identity reportOnlyFloors and is intentionally report-only in PR-04.",
            ),
            Phase4MetricSpec(
                id = "nonWeaponBuildPayoffFloor.reportOnly",
                ownerTaskId = "longRunLab",
                outputSection = "terminal-build-identity",
                formula = "count(professions meeting build-identity non-weapon floor) / foundationProfessionCount",
                failSemantics = "APPROVED_DEBT means per-profession non-weapon payoff floor is visible but not blocking yet.",
                decisionNotes = "The floor is derived from build-identity reportOnlyFloors and should only become blocking in a later gate-cutover PR.",
            ),
            Phase4MetricSpec(
                id = "phaseTransitionObservedRatio",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "boss samples with observed formal phase transition / boss sample count",
                failSemantics = "FAIL means at least one boss sample no longer exercises the formal phase transition path required by PR-04.",
                decisionNotes = "This is bossHarness owner evidence, not a supporting input metric; it must remain visible in reportPhase4 owner metrics.",
            ),
            Phase4MetricSpec(
                id = "variantTraceDivergenceRatio",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "variant/base boss pairs with divergent AI/action trace / variant/base boss pair count",
                failSemantics = "FAIL means at least one boss variant still behaves like its base encounter despite distinct variant identity data.",
                decisionNotes = "Trace divergence is the headline variant identity metric; pair-level trace evidence remains supporting detail.",
            ),
            Phase4MetricSpec(
                id = "minVariantActionTraceDivergenceScore",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "min(selected-action divergence score across variant/base boss pairs)",
                failSemantics = "FAIL means the weakest boss variant/base pair falls below the frozen action-divergence floor.",
                decisionNotes = "The minimum owns the verdict so aggregate trace diversity cannot hide a near-identical variant.",
            ),
            Phase4MetricSpec(
                id = "bossVariantBasePhaseCountMin",
                ownerTaskId = "bossHarness",
                outputSection = "boss-phase-identity",
                formula = "min(base encounter phase count across formal boss variants)",
                failSemantics = "FAIL means a formal boss variant is bound to a base encounter without enough phase structure.",
                decisionNotes = "This keeps variant identity tied to BossEncounter phase contract rather than generic mutation metadata.",
            ),
            Phase4MetricSpec(
                id = "terrainInteractionEncounterRate.aggregate",
                ownerTaskId = "terrainInteractionBatch",
                outputSection = "terrain-combat-sample-contract",
                formula = "triggeredInteractionCombatCount / taggedCombatCount",
                failSemantics = "FAIL means aggregate terrain interaction encounter rate regressed below the frozen baseline uplift target.",
                decisionNotes = "Aggregate success is not sufficient; per-zone lower-bound failures must still be surfaced separately.",
            ),
            Phase4MetricSpec(
                id = "terrainInteractionEncounterRate.per_zone_lower_bound",
                ownerTaskId = "terrainInteractionBatch",
                outputSection = "terrain-combat-sample-contract",
                formula = "for each combat-sampled zone: terrainInteractionEncounterRate >= perZoneEncounterLowerBoundTarget",
                failSemantics = "FAIL means at least one combat-sampled zone is falling below the minimum encounter language floor even if the aggregate stays green.",
                decisionNotes = "The combat-sampled zone list and exclusion reasons are part of the contract and must be printed beside the metric.",
            ),
        )

    fun ownerTaskIds(): Set<String> = specs.mapTo(linkedSetOf(), Phase4MetricSpec::ownerTaskId)

    fun metricIds(
        ownerTaskId: String,
        outputSection: String,
    ): List<String> =
        specs
            .filter { spec -> spec.ownerTaskId == ownerTaskId && spec.outputSection == outputSection }
            .map(Phase4MetricSpec::id)

    fun bossPhaseIdentityMetricIds(): List<String> =
        metricIds(
            ownerTaskId = "bossHarness",
            outputSection = "boss-phase-identity",
        )

    fun entryFor(
        metricId: String,
        sourcePathByTaskId: Map<String, String>,
        targetTextByMetricId: Map<String, String>,
    ): Phase4MetricCatalogEntry {
        val spec = requireSpec(metricId)
        return Phase4MetricCatalogEntry(
            id = spec.id,
            ownerTaskId = spec.ownerTaskId,
            sourcePath = checkNotNull(sourcePathByTaskId[spec.ownerTaskId]) { "Missing sourcePath for phase4 task '${spec.ownerTaskId}'." },
            outputSection = spec.outputSection,
            formula = spec.formula,
            targetText = checkNotNull(targetTextByMetricId[spec.id]) { "Missing phase4 targetText for '${spec.id}'." },
            failSemantics = spec.failSemantics,
            decisionNotes = spec.decisionNotes,
        )
    }

    fun entries(
        sourcePathByTaskId: Map<String, String>,
        targetTextByMetricId: Map<String, String>,
    ): List<Phase4MetricCatalogEntry> =
        specs.map { spec ->
            entryFor(
                metricId = spec.id,
                sourcePathByTaskId = sourcePathByTaskId,
                targetTextByMetricId = targetTextByMetricId,
            )
        }

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
