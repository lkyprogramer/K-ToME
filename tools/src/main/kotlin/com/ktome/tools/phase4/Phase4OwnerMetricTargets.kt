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
            "organicHiddenDiscoveryRate",
            "crossProfessionTopWeaponDominance",
            "professionAlignedWeaponAdoptionRate",
            ->
                renderBoundTarget(
                    range = range,
                    formatter = ::formatPercent,
                )

            "sameZoneSecretVsCadenceMaxOverlap",
            "sameZoneSecretVsRewardMaxOverlap",
            ->
                renderBoundTarget(
                    range = range,
                    formatter = ::formatRatio,
                )

            "terminalWeaponBaseDiversity" ->
                renderBoundTarget(
                    range = range,
                    formatter = ::formatNumber,
                )

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
