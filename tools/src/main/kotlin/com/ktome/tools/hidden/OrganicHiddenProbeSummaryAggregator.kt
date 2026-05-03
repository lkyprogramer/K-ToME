package com.ktome.tools.hidden

import kotlin.math.ceil

internal object OrganicHiddenProbeSummaryAggregator {
    private const val TOP_LEAD_SHARE_EXCLUDED_ZONE_ID: String = "greenwood_fringe"
    private const val SLAG_CUE_ZONE_ID: String = "deep_iron_pit"

    fun summarize(request: OrganicHiddenProbeSummaryRequest): OrganicHiddenProbeSummary {
        val results = request.results
        val zoneBreakdown =
            results.groupBy(OrganicHiddenProbeCaseResult::zoneId)
                .mapValues { (_, zoneResults) -> aggregateZoneMetrics(zoneResults) }
                .toSortedMap()
        val combinations =
            results
                .groupBy { result -> result.professionId to result.raceId }
                .toSortedMap(compareBy<Pair<String, String>>({ it.first }, { it.second }))
                .map { (combo, comboResults) ->
                    val metrics = aggregateZoneMetrics(comboResults)
                    OrganicHiddenProbeCombinationMetrics(
                        professionId = combo.first,
                        raceId = combo.second,
                        caseCount = metrics.caseCount,
                        runsWithSearchActionCount = metrics.runsWithSearchActionCount,
                        searchActionUseCount = metrics.searchActionUseCount,
                        runsWithSearchPromptCount = metrics.runsWithSearchPromptCount,
                        searchPromptVisibleCount = metrics.searchPromptVisibleCount,
                        searchRevealCount = metrics.searchRevealCount,
                        slagCueEligibleRoomCount = metrics.slagCueEligibleRoomCount,
                        slagCueCandidateCount = metrics.slagCueCandidateCount,
                        discoveryWithoutPrimerCount = metrics.discoveryWithoutPrimerCount,
                        secretZoneEntryCount = metrics.secretZoneEntryCount,
                        averageFirstHiddenDiscoveryTurn = metrics.averageFirstHiddenDiscoveryTurn,
                        averageFirstSecretZoneEntryTurn = metrics.averageFirstSecretZoneEntryTurn,
                        firstHiddenDiscoveryTurnP50 = metrics.firstHiddenDiscoveryTurnP50,
                        firstHiddenDiscoveryTurnP90 = metrics.firstHiddenDiscoveryTurnP90,
                        firstSecretZoneEntryTurnP50 = metrics.firstSecretZoneEntryTurnP50,
                        firstSecretZoneEntryTurnP90 = metrics.firstSecretZoneEntryTurnP90,
                    )
                }
        val totalDiscoveryCount = results.count(OrganicHiddenProbeCaseResult::leadDiscovered)
        val totalSecretZoneEntryCount = results.count(OrganicHiddenProbeCaseResult::secretZoneEntered)
        val topLeadShareDenominator =
            zoneBreakdown
                .filterKeys { zoneId -> zoneId != TOP_LEAD_SHARE_EXCLUDED_ZONE_ID }
                .values
                .sumOf(OrganicHiddenProbeZoneMetrics::discoveryWithoutPrimerCount)
        val topLeadShareEntry =
            zoneBreakdown
                .filterKeys { zoneId -> zoneId != TOP_LEAD_SHARE_EXCLUDED_ZONE_ID }
                .maxByOrNull { (_, metrics) -> metrics.discoveryWithoutPrimerCount }
        val failingSecretEntryZoneIds =
            zoneBreakdown
                .filterValues { metrics -> metrics.secretZoneEntryRate < request.perZoneSecretEntryMinRate }
                .keys
                .toList()
                .sorted()
        val secretZoneDiscoveryDistribution =
            results
                .flatMap { result -> result.secretZoneIds.distinct() }
                .groupingBy { secretZoneId -> secretZoneId }
                .eachCount()
                .toSortedMap()
                .mapValues { (_, count) ->
                    if (totalSecretZoneEntryCount == 0) {
                        0.0
                    } else {
                        count.toDouble() / totalSecretZoneEntryCount.toDouble()
                    }
                }
        return OrganicHiddenProbeSummary(
            totalCases = results.size,
            distinctSeedCount = results.map(OrganicHiddenProbeCaseResult::seed).distinct().size,
            runtimeFailureCount = results.count { result -> result.runtimeFailure != null },
            searchAttemptCount = results.sumOf(OrganicHiddenProbeCaseResult::searchAttemptCount),
            runsWithSearchActionCount = results.count { result -> result.searchActionUseCount > 0 },
            searchActionUseCount = results.sumOf(OrganicHiddenProbeCaseResult::searchActionUseCount),
            runsWithSearchPromptCount = results.count { result -> result.searchPromptVisibleCount > 0 },
            searchPromptVisibleCount = results.sumOf(OrganicHiddenProbeCaseResult::searchPromptVisibleCount),
            searchRevealCount = results.sumOf(OrganicHiddenProbeCaseResult::searchRevealCount),
            discoveryWithoutPrimerCount = totalDiscoveryCount,
            secretZoneEntryCount = totalSecretZoneEntryCount,
            averageFirstHiddenDiscoveryTurn = averageOfNullable(results.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn)),
            averageFirstSecretZoneEntryTurn = averageOfNullable(results.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn)),
            firstHiddenDiscoveryTurnP50 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn), 50),
            firstHiddenDiscoveryTurnP90 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn), 90),
            firstSecretZoneEntryTurnP50 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn), 50),
            firstSecretZoneEntryTurnP90 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn), 90),
            professionIds = request.professionIds,
            raceIds = request.raceIds,
            seedsPerZoneCombo = request.seedsPerZoneCombo,
            zoneBreakdown = zoneBreakdown,
            combinations = combinations,
            zoneDiscoveryDistribution =
                zoneBreakdown.mapValues { (_, metrics) ->
                    if (totalDiscoveryCount == 0) {
                        0.0
                    } else {
                        metrics.discoveryWithoutPrimerCount.toDouble() / totalDiscoveryCount.toDouble()
                    }
                },
            secretZoneDiscoveryDistribution = secretZoneDiscoveryDistribution,
            topZoneLeadShare =
                if (topLeadShareDenominator == 0) {
                    0.0
                } else {
                    topLeadShareEntry?.value?.discoveryWithoutPrimerCount?.toDouble()?.div(topLeadShareDenominator.toDouble()) ?: 0.0
                },
            topZoneLeadShareZoneId = topLeadShareEntry?.key,
            topZoneLeadShareDenominator = topLeadShareDenominator,
            zoneSearchPromptVisibility =
                if (results.isEmpty()) {
                    0.0
                } else {
                    results.count { result -> result.searchPromptVisibleCount > 0 }.toDouble() / results.size.toDouble()
                },
            perZoneSecretConversionFloorReportOnly =
                zoneBreakdown.mapValues { (_, metrics) -> metrics.secretConversionRate },
            secretZoneSearchConversionFloorReportOnly =
                zoneBreakdown.mapValues { (_, metrics) -> metrics.secretZoneSearchConversionRate },
            perZoneSearchUseFloorReportOnly =
                zoneBreakdown.mapValues { (_, metrics) -> metrics.searchActionUseRate },
            slagCueDensityPerEligibleRoomReportOnly =
                zoneBreakdown[SLAG_CUE_ZONE_ID]
                    ?.let { metrics -> mapOf(SLAG_CUE_ZONE_ID to metrics.slagCueDensityPerEligibleRoom) }
                    .orEmpty(),
            perZoneSecretEntryMinRate = request.perZoneSecretEntryMinRate,
            failingSecretEntryZoneIds = failingSecretEntryZoneIds,
        )
    }

    private fun aggregateZoneMetrics(results: List<OrganicHiddenProbeCaseResult>): OrganicHiddenProbeZoneMetrics =
        OrganicHiddenProbeZoneMetrics(
            caseCount = results.size,
            runsWithSearchActionCount = results.count { result -> result.searchActionUseCount > 0 },
            searchActionUseCount = results.sumOf(OrganicHiddenProbeCaseResult::searchActionUseCount),
            runsWithSearchPromptCount = results.count { result -> result.searchPromptVisibleCount > 0 },
            searchPromptVisibleCount = results.sumOf(OrganicHiddenProbeCaseResult::searchPromptVisibleCount),
            searchRevealCount = results.sumOf(OrganicHiddenProbeCaseResult::searchRevealCount),
            slagCueEligibleRoomCount = results.sumOf(OrganicHiddenProbeCaseResult::slagCueEligibleRoomCount),
            slagCueCandidateCount = results.sumOf(OrganicHiddenProbeCaseResult::slagCueCandidateCount),
            discoveryWithoutPrimerCount = results.count(OrganicHiddenProbeCaseResult::discoveredWithoutPrimer),
            secretZoneEntryCount = results.count(OrganicHiddenProbeCaseResult::secretZoneEntered),
            averageFirstHiddenDiscoveryTurn = averageOfNullable(results.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn)),
            averageFirstSecretZoneEntryTurn = averageOfNullable(results.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn)),
            firstHiddenDiscoveryTurnP50 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn), 50),
            firstHiddenDiscoveryTurnP90 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstHiddenDiscoveryTurn), 90),
            firstSecretZoneEntryTurnP50 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn), 50),
            firstSecretZoneEntryTurnP90 = percentileOfNullable(results.map(OrganicHiddenProbeCaseResult::firstSecretZoneEntryTurn), 90),
        )
}

private fun percentileOfNullable(
    values: List<Int?>,
    percentile: Int,
): Int? {
    val sorted = values.filterNotNull().sorted()
    if (sorted.isEmpty()) {
        return null
    }
    val rank = ceil(percentile / 100.0 * sorted.size.toDouble()).toInt().coerceAtLeast(1) - 1
    return sorted[rank]
}

private fun averageOfNullable(values: List<Int?>): Double? {
    val present = values.filterNotNull()
    return if (present.isEmpty()) {
        null
    } else {
        present.average()
    }
}
