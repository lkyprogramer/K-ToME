package com.ktome.tools.hidden

import kotlin.math.ceil

internal object OrganicHiddenProbeSummaryAggregator {
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
            perZoneSecretEntryMinRate = request.perZoneSecretEntryMinRate,
            failingSecretEntryZoneIds = failingSecretEntryZoneIds,
        )
    }

    private fun aggregateZoneMetrics(results: List<OrganicHiddenProbeCaseResult>): OrganicHiddenProbeZoneMetrics =
        OrganicHiddenProbeZoneMetrics(
            caseCount = results.size,
            runsWithSearchActionCount = results.count { result -> result.searchActionUseCount > 0 },
            searchActionUseCount = results.sumOf(OrganicHiddenProbeCaseResult::searchActionUseCount),
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
