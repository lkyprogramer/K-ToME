package com.ktome.tools.phase4

import com.ktome.tools.verification.VerificationBaseline
import com.ktome.tools.verification.VerificationExpectedMetricRange
import java.util.Locale

internal object Phase4OwnerMetricTargets {
    fun targetText(
        metricId: String,
        range: VerificationExpectedMetricRange,
    ): String =
        when (metricId) {
            "scriptedHiddenVerificationRate",
            "leadDiscoveryRate",
            "secretConversionRate",
            "topZoneLeadShare",
            "zoneSearchPromptVisibility",
            "dynamicPoolCoverage",
            "crossProfessionTopWeaponDominance",
            "professionAlignedWeaponAdoptionRate",
            "professionCapstoneAdoptionRate",
            "nonWeaponBuildPayoffRate",
            "learnedTalentChoiceEventRate",
            "multiTreeInvestmentAboveThresholdRate",
            "breakpointChoiceEventRate",
            "inscriptionInstallOrReplaceRate",
            "phaseTransitionObservedRatio",
            "variantTraceDivergenceRatio",
            "bossVariantPhaseOverrideSchemaCoverage",
            "bossVariantPhaseOverrideRuntimeTriggerCoverage",
            "bossVariantPhaseOverrideTelegraphCoverage",
            "frontstageHighPriorityCueRetainedRate",
            "frontstageCueExpiryParity",
            "frontstageSecretCueVisibilityRate",
            "frontstageSearchCueVisibilityRate",
            "zoneHookCoverage",
            "topFiveAffixExposureShare",
            "samplePackContentPlayerVisibilityRate.reportOnly",
            "zoneRouteHashDiversity.topHashShare",
            "topologyCategoryDiversityPerSmokeRun.reportOnly",
            "milestoneRewardSlotBalance.maxSlotShare",
            "milestoneRewardSlotBalance.WEAPON",
            "milestoneRewardSlotBalance.OFF_HAND",
            "milestoneRewardSlotBalance.ARMOR",
            "milestoneRewardSlotBalance.ACCESSORY",
            "milestoneRewardSlotBalance.CONSUMABLE_OR_UTILITY",
            ->
                renderBoundTarget(
                    range = range,
                    formatter = ::formatPercent,
                )

            "professionCapstoneSeenRate" -> renderProfessionCapstoneSeenTarget(range)

            "professionCapstoneAdoptionFloor",
            "nonWeaponBuildPayoffFloor",
            -> renderProfessionFloorTarget(range)

            "milestoneRewardAdoptionDelta" -> renderMilestoneRewardAdoptionDeltaTarget(range)

            "sameZoneSecretVsCadenceMaxOverlap",
            "sameZoneSecretVsRewardMaxOverlap",
            "minVariantActionTraceDivergenceScore",
            ->
                renderBoundTarget(
                    range = range,
                    formatter = ::formatRatio,
                )

            "secretZoneRewardAuthorityViolations" ->
                renderBoundTarget(
                    range = range,
                    formatter = ::formatNumber,
                )

            "terminalWeaponBaseDiversity" ->
                "${renderBoundTarget(range = range, formatter = ::formatNumber)} weapon bases"

            "bossVariantBasePhaseCountMin" ->
                renderBoundTarget(
                    range = range,
                    formatter = ::formatNumber,
                )

            "bossVariantPhaseOverrideActionDistinctCount.reportOnly" ->
                "min per-variant action count ${renderBoundTarget(range = range, formatter = ::formatNumber)}"

            "avgObjectiveAcquireTurn" ->
                "min critical-path zone avg >= ${formatNumber(checkNotNull(range.minimumAcceptedValue()))}"

            "avgVisibleHostileTurnCount",
            "avgEnemyTurns",
            ->
                "min critical-path zone avg >= ${formatNumber(checkNotNull(range.minimumAcceptedValue()))}"

            "criticalPathCombatFloorSatisfied" ->
                "100.0% critical-path zones satisfy objective/combat floors"

            "terrainInteractionEncounterRate.aggregate" -> renderTerrainAggregateTarget(range)

            "terrainInteractionEncounterRate.per_zone_lower_bound" ->
                "0 failing sampled zones (sampled zone rate >= ${formatPercent(range.requireMetadataDouble("perZoneEncounterLowerBoundTarget"))})"

            else -> renderBoundTarget(range = range, formatter = ::formatNumber)
        }

    fun passes(
        range: VerificationExpectedMetricRange,
        actualValue: Double,
    ): Boolean {
        return range.passesMinimumBound(actualValue) && range.passesMaximumBound(actualValue)
    }

    private fun renderTerrainAggregateTarget(range: VerificationExpectedMetricRange): String {
        val baselineValue =
            checkNotNull(range.baselineValue) {
                "terrainInteractionEncounterRate.aggregate must declare baselineValue."
            }
        val targetRelativeIncrease =
            checkNotNull(range.targetRelativeIncrease) {
                "terrainInteractionEncounterRate.aggregate must declare targetRelativeIncrease."
            }
        val minimumAcceptedValue =
            checkNotNull(range.minimumAcceptedValue()) {
                "terrainInteractionEncounterRate.aggregate must resolve a minimumAcceptedValue."
            }
        return ">= ${formatPercentPrecise(minimumAcceptedValue)} (baseline ${formatPercentPrecise(baselineValue)} +${formatPercentPrecise(targetRelativeIncrease)})"
    }

    private fun renderProfessionCapstoneSeenTarget(range: VerificationExpectedMetricRange): String {
        val aggregateTarget = renderBoundTarget(range = range, formatter = ::formatPercent)
        val perProfessionSeenMinCount =
            range.metadata["perProfessionSeenMinCount"]?.toString()?.trim('"')?.toIntOrNull()
        return if (perProfessionSeenMinCount == null) {
            aggregateTarget
        } else {
            "$aggregateTarget + every profession seenCount >= $perProfessionSeenMinCount"
        }
    }

    private fun renderProfessionFloorTarget(range: VerificationExpectedMetricRange): String {
        val minimumAcceptedValue =
            checkNotNull(range.minimumAcceptedValue()) {
                "${range.metricId} must declare a minimum foundation profession floor."
            }
        return "${formatNumber(minimumAcceptedValue)}/4 foundation professions"
    }

    private fun renderMilestoneRewardAdoptionDeltaTarget(range: VerificationExpectedMetricRange): String {
        val minimumAcceptedValue =
            checkNotNull(range.minimumAcceptedValue()) {
                "milestoneRewardAdoptionDelta must declare a minimum adopted-minus-not-adopted delta."
            }
        return "adopted > notAdopted (delta >= ${formatNumber(minimumAcceptedValue)})"
    }

    private fun renderBoundTarget(
        range: VerificationExpectedMetricRange,
        formatter: (Double) -> String,
    ): String {
        val minimumAcceptedValue = range.minimumAcceptedValue()
        val maximumAcceptedValue = range.maximumAcceptedValue()
        return when {
            minimumAcceptedValue != null && maximumAcceptedValue != null ->
                "${range.minimumBoundOperator()} ${formatter(minimumAcceptedValue)} .. ${range.maximumBoundOperator()} ${formatter(maximumAcceptedValue)}"
            minimumAcceptedValue != null -> "${range.minimumBoundOperator()} ${formatter(minimumAcceptedValue)}"
            maximumAcceptedValue != null -> "${range.maximumBoundOperator()} ${formatter(maximumAcceptedValue)}"
            else -> "defined by baseline"
        }
    }

    private fun VerificationExpectedMetricRange.requireMetadataDouble(key: String): Double =
        metadata[key]?.toString()?.trim('"')?.toDouble()
            ?: error("VerificationExpectedMetricRange($metricId) metadata must declare numeric '$key'.")

    private fun formatPercent(value: Double): String = String.format(Locale.US, "%.1f%%", value * 100.0)

    private fun formatPercentPrecise(value: Double): String = String.format(Locale.US, "%.2f%%", value * 100.0)

    private fun formatRatio(value: Double): String = String.format(Locale.US, "%.3f", value)

    private fun formatNumber(value: Double): String =
        if (value == value.toInt().toDouble()) {
            value.toInt().toString()
        } else {
            String.format(Locale.US, "%.6f", value)
        }
}

internal fun VerificationBaseline.requiredMetric(metricId: String): VerificationExpectedMetricRange =
    checkNotNull(expectedMetricRange(metricId)) { "Missing baseline metric '$metricId' in $baselineId." }
