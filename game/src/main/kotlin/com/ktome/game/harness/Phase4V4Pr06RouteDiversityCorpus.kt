package com.ktome.game.harness

import com.ktome.game.routeToken
import com.ktome.game.secretRouteMarker
import com.ktome.game.zoneRouteHash

object Phase4V4Pr06RouteDiversityCorpus {
    private val professionOrder = listOf("vanguard", "arcanist", "rogue", "templar")
    private val raceOrder = listOf("human", "elf", "dwarf")
    private val defaultSummary by lazy {
        Phase4V4Pr06RouteDiversitySummary.from(smokeSpecs(corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID))
    }

    fun fullRouteSpecs(
        corpusId: String,
        initialTalentPointGrant: Int = 0,
    ): List<ScenarioSpec> =
        fullRouteEntries.mapIndexed { index, entry ->
            val professionId = professionOrder[index % professionOrder.size]
            val raceId = raceOrder[index / professionOrder.size]
            val runtimeRoute = runtimeRouteFromStart(entry.startZoneId)
            ScenarioSpec(
                name = "long-run-pr06-full-route-${entry.seed}",
                seed = entry.seed,
                zoneId = entry.startZoneId,
                professionId = professionId,
                raceId = raceId,
                zoneRoute = runtimeRoute,
                routeIndex = 0,
                routeIntent = entry.routeIntent,
                routeTokenParts = runtimeRoute,
                scenarioType = ScenarioType.FULL_ROUTE,
                corpusId = corpusId,
                maxTurns = 2200,
                goal = ScenarioGoal.ReachZoneAtLeastOrTerminal("abyssal_temple"),
                initialTalentPointGrant = initialTalentPointGrant,
                assertions = listOf(ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
            )
        }

    fun branchInclusiveSpecs(
        corpusId: String,
        initialTalentPointGrant: Int = 0,
    ): List<ScenarioSpec> =
        branchEntries.map { entry ->
            ScenarioSpec(
                name = "long-run-pr06-branch-${entry.seed}",
                seed = entry.seed,
                zoneId = entry.startZoneId,
                professionId = entry.professionId,
                raceId = entry.raceId,
                zoneRoute = entry.runtimeRoute,
                routeIndex = 0,
                routeIntent = entry.routeIntent,
                routeTokenParts = entry.routeTokenParts(),
                primarySecretZoneId = entry.primarySecretZoneId,
                scenarioType = ScenarioType.BRANCH_INCLUSIVE,
                corpusId = corpusId,
                maxTurns = 2400,
                goal = ScenarioGoal.ReachZoneAtLeastOrTerminal(entry.goalZoneId),
                initialTalentPointGrant = initialTalentPointGrant,
                assertions =
                    listOfNotNull(
                        ScenarioAssertion.NoFailure,
                        ScenarioAssertion.NoStall,
                        ScenarioAssertion.VisitedPrimarySecretZone,
                        entry.requiredVisitedZoneId?.let(ScenarioAssertion::VisitedZone),
                    ),
            )
        }

    fun routeProbeSpecs(corpusId: String): List<ScenarioSpec> =
        listOf(
            probeSpec(
                name = "long-run-pr06-route-probe-2026042417",
                seed = 2026042417L,
                startZoneId = "greenwood_fringe",
                professionId = "rogue",
                raceId = "human",
                runtimeRoute = listOf("greenwood_fringe", "deep_iron_pit"),
                routeIntent = listOf("greenwood_fringe", "grey_gate_depths"),
                scenarioType = ScenarioType.ROUTE_PROBE,
                corpusId = corpusId,
            ),
            probeSpec(
                name = "long-run-pr06-route-probe-2026042418",
                seed = 2026042418L,
                startZoneId = "deep_iron_pit",
                professionId = "templar",
                raceId = "elf",
                runtimeRoute = listOf("deep_iron_pit", "grey_gate_depths"),
                routeIntent = listOf("deep_iron_pit", "underground_river"),
                scenarioType = ScenarioType.ROUTE_PROBE,
                corpusId = corpusId,
            ),
        )

    fun lateRouteProbeSpecs(corpusId: String): List<ScenarioSpec> =
        listOf(
            probeSpec(
                name = "long-run-pr06-late-route-probe-2026042419",
                seed = 2026042419L,
                startZoneId = "deep_iron_pit",
                professionId = "vanguard",
                raceId = "dwarf",
                runtimeRoute = listOf("deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple"),
                routeIntent = listOf("grey_gate_depths", "abyssal_temple"),
                scenarioType = ScenarioType.LATE_ROUTE_PROBE,
                corpusId = corpusId,
            ),
            probeSpec(
                name = "long-run-pr06-late-route-probe-2026042420",
                seed = 2026042420L,
                startZoneId = "underground_river",
                professionId = "arcanist",
                raceId = "human",
                runtimeRoute = listOf("underground_river", "abyssal_temple"),
                routeIntent = listOf("underground_river", "abyssal_temple"),
                scenarioType = ScenarioType.LATE_ROUTE_PROBE,
                corpusId = corpusId,
            ),
        )

    fun smokeSpecs(corpusId: String): List<ScenarioSpec> =
        fullRouteSpecs(corpusId = corpusId) +
            branchInclusiveSpecs(corpusId = corpusId) +
            routeProbeSpecs(corpusId = corpusId) +
            lateRouteProbeSpecs(corpusId = corpusId)

    fun summary(corpusId: String = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID): Phase4V4Pr06RouteDiversitySummary =
        if (corpusId == HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID) {
            defaultSummary
        } else {
            Phase4V4Pr06RouteDiversitySummary.from(smokeSpecs(corpusId = corpusId))
        }

    private fun probeSpec(
        name: String,
        seed: Long,
        startZoneId: String,
        professionId: String,
        raceId: String,
        runtimeRoute: List<String>,
        routeIntent: List<String>,
        scenarioType: ScenarioType,
        corpusId: String,
    ): ScenarioSpec =
        ScenarioSpec(
            name = name,
            seed = seed,
            zoneId = startZoneId,
            professionId = professionId,
            raceId = raceId,
            zoneRoute = runtimeRoute,
            routeIndex = 0,
            routeIntent = routeIntent,
            routeTokenParts = runtimeRoute,
            probeRoute = routeIntent,
            scenarioType = scenarioType,
            corpusId = corpusId,
            maxTurns = 1000,
            goal = ScenarioGoal.ReachFloor(2),
            assertions = listOf(ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
        )

    private fun runtimeRouteFromStart(startZoneId: String): List<String> =
        when (startZoneId) {
            "greenwood_fringe" -> listOf("greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple")
            "deep_iron_pit" -> listOf("deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple")
            "underground_river" -> listOf("underground_river", "abyssal_temple")
            else -> error("Unsupported PR06 long-run start zone: $startZoneId")
        }

    private val fullRouteEntries: List<Pr06FullRouteEntry> =
        listOf(
            Pr06FullRouteEntry(2026042401L, "greenwood_fringe", listOf("greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple")),
            Pr06FullRouteEntry(2026042402L, "greenwood_fringe", listOf("greenwood_fringe", "underground_river", "grey_gate_depths", "deep_iron_pit", "abyssal_temple")),
            Pr06FullRouteEntry(2026042403L, "greenwood_fringe", listOf("greenwood_fringe", "grey_gate_depths", "underground_river", "deep_iron_pit", "abyssal_temple")),
            Pr06FullRouteEntry(2026042404L, "greenwood_fringe", listOf("greenwood_fringe", "grey_gate_depths", "deep_iron_pit", "underground_river", "abyssal_temple")),
            Pr06FullRouteEntry(2026042405L, "deep_iron_pit", listOf("deep_iron_pit", "greenwood_fringe", "underground_river", "grey_gate_depths", "abyssal_temple")),
            Pr06FullRouteEntry(2026042406L, "deep_iron_pit", listOf("deep_iron_pit", "grey_gate_depths", "greenwood_fringe", "underground_river", "abyssal_temple")),
            Pr06FullRouteEntry(2026042407L, "deep_iron_pit", listOf("deep_iron_pit", "underground_river", "greenwood_fringe", "grey_gate_depths", "abyssal_temple")),
            Pr06FullRouteEntry(2026042408L, "deep_iron_pit", listOf("deep_iron_pit", "underground_river", "grey_gate_depths", "greenwood_fringe", "abyssal_temple")),
            Pr06FullRouteEntry(2026042409L, "underground_river", listOf("underground_river", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "abyssal_temple")),
            Pr06FullRouteEntry(2026042410L, "underground_river", listOf("underground_river", "deep_iron_pit", "grey_gate_depths", "greenwood_fringe", "abyssal_temple")),
            Pr06FullRouteEntry(2026042411L, "underground_river", listOf("underground_river", "grey_gate_depths", "greenwood_fringe", "deep_iron_pit", "abyssal_temple")),
            Pr06FullRouteEntry(2026042412L, "underground_river", listOf("underground_river", "greenwood_fringe", "grey_gate_depths", "deep_iron_pit", "abyssal_temple")),
        )

    private val branchEntries: List<Pr06BranchEntry> =
        listOf(
            Pr06BranchEntry(
                seed = 2026042413L,
                startZoneId = "greenwood_fringe",
                professionId = "rogue",
                raceId = "human",
                routeIntent = listOf("greenwood_fringe", "underground_river", secretRouteMarker("underground_river_crystal_rift")),
                runtimeRoute = runtimeRouteFromStart("greenwood_fringe"),
                primarySecretZoneId = "underground_river_crystal_rift",
                secretMarkerIndex = 4,
                goalZoneId = "underground_river",
            ),
            Pr06BranchEntry(
                seed = 2026042414L,
                startZoneId = "deep_iron_pit",
                professionId = "vanguard",
                raceId = "elf",
                routeIntent = listOf("deep_iron_pit", "grey_gate_depths", "crystal_cavern", secretRouteMarker("deep_iron_smuggler_stash")),
                runtimeRoute = listOf("deep_iron_pit", "grey_gate_depths", "underground_river", "crystal_cavern", "underground_river", "abyssal_temple"),
                primarySecretZoneId = "deep_iron_smuggler_stash",
                secretMarkerIndex = 1,
                goalZoneId = "crystal_cavern",
                requiredVisitedZoneId = "crystal_cavern",
            ),
            Pr06BranchEntry(
                seed = 2026042415L,
                startZoneId = "underground_river",
                professionId = "templar",
                raceId = "dwarf",
                routeIntent = listOf("underground_river", "abyssal_temple", secretRouteMarker("abyssal_temple_warded_archive")),
                runtimeRoute = runtimeRouteFromStart("underground_river"),
                primarySecretZoneId = "abyssal_temple_warded_archive",
                secretMarkerIndex = 2,
                goalZoneId = "abyssal_temple",
            ),
            Pr06BranchEntry(
                seed = 2026042416L,
                startZoneId = "greenwood_fringe",
                professionId = "arcanist",
                raceId = "human",
                routeIntent = listOf("grey_gate_depths", "greenwood_fringe", secretRouteMarker("greenwood_hidden_cache")),
                runtimeRoute = listOf("greenwood_fringe", "bandit_camp", "greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple"),
                primarySecretZoneId = "greenwood_hidden_cache",
                secretMarkerIndex = 1,
                goalZoneId = "bandit_camp",
                requiredVisitedZoneId = "bandit_camp",
            ),
        )
}

data class Phase4V4Pr06RouteDiversitySummary(
    val scenarioTypeDistribution: Map<String, Int>,
    val zoneRouteHashDistribution: Map<String, Int>,
    val branchRouteTokens: List<String>,
    val fullRouteTokenSample: List<String>,
    val actualFullRouteHashDistinctCount: Int,
    val topHashShare: Double,
    val fullRouteIntentDistinctCount: Int,
    val probeRouteHashSample: List<String>,
    val routeTokenSample: List<String>,
) {
    fun primaryResultText(): String =
        "scenarioTypeDistribution=${scenarioTypeDistribution.toCompactText()}; " +
            "zoneRouteHashDistribution=${zoneRouteHashDistribution.toCompactText()}; " +
            "topHashShare=${"%.2f".format(topHashShare)}; " +
            "fullRouteIntentDistinctCount=$fullRouteIntentDistinctCount; " +
            "actualFullRouteHashDistinctCount=$actualFullRouteHashDistinctCount; " +
            "fullRouteTokens=${fullRouteTokenSample.joinToString("|")}; " +
            "branchInclusiveRoutes=${branchRouteTokens.take(4).joinToString("|")}; " +
            "routeTokenSample=${routeTokenSample.joinToString("|")}"

    fun evidenceResultText(): String =
            "full_route=${scenarioTypeDistribution.getValue(ScenarioType.FULL_ROUTE.reportValue)}; " +
            "branch_inclusive=${scenarioTypeDistribution.getValue(ScenarioType.BRANCH_INCLUSIVE.reportValue)}; " +
            "topHashShare<=0.40:${topHashShare <= 0.4}; " +
            "actualFullRouteHashDistinctCount=$actualFullRouteHashDistinctCount; " +
            "verifyChangedRouting=RouteHash.kt=>:game:longRunLab,loot/index.yaml=>:game:longRunLab,items/index.yaml=>:game:longRunLab,TalentSidebarPresenter.kt=>no-longRunLab"

    companion object {
        fun from(specs: List<ScenarioSpec>): Phase4V4Pr06RouteDiversitySummary {
            val scenarioDistribution =
                linkedMapOf<String, Int>().apply {
                    ScenarioType.entries.forEach { type ->
                        put(type.reportValue, specs.count { spec -> spec.scenarioType == type })
                    }
                }
            val terminalSpecs =
                specs.filterNot { spec ->
                    spec.scenarioType == ScenarioType.ROUTE_PROBE ||
                        spec.scenarioType == ScenarioType.LATE_ROUTE_PROBE
                }
            val hashDistribution = terminalSpecs.groupingBy { spec -> zoneRouteHash(spec.routeTokenParts) }.eachCount().toSortedMap()
            val topCount = hashDistribution.values.maxOrNull() ?: 0
            val fullRouteSpecs = specs.filter { spec -> spec.scenarioType == ScenarioType.FULL_ROUTE }
            val probeHashes =
                specs.mapNotNull { spec ->
                    spec.probeRoute.takeIf(List<String>::isNotEmpty)?.let(::zoneRouteHash)
                }
            return Phase4V4Pr06RouteDiversitySummary(
                scenarioTypeDistribution = scenarioDistribution,
                zoneRouteHashDistribution = hashDistribution,
                branchRouteTokens =
                    specs
                        .filter { spec -> spec.scenarioType == ScenarioType.BRANCH_INCLUSIVE }
                        .map { spec -> routeToken(spec.routeTokenParts) },
                fullRouteTokenSample =
                    fullRouteSpecs
                        .map { spec -> routeToken(spec.routeTokenParts) }
                        .distinct()
                        .sorted(),
                actualFullRouteHashDistinctCount =
                    fullRouteSpecs
                        .map { spec -> zoneRouteHash(spec.routeTokenParts) }
                        .distinct()
                        .size,
                topHashShare = if (terminalSpecs.isEmpty()) 0.0 else topCount.toDouble() / terminalSpecs.size.toDouble(),
                fullRouteIntentDistinctCount =
                    fullRouteSpecs
                        .map { spec -> routeToken(spec.routeIntent) }
                        .distinct()
                        .size,
                probeRouteHashSample = probeHashes.distinct().sorted(),
                routeTokenSample = terminalSpecs.map { spec -> routeToken(spec.routeTokenParts) }.distinct().sorted().take(8),
            )
        }
    }
}

private data class Pr06FullRouteEntry(
    val seed: Long,
    val startZoneId: String,
    val routeIntent: List<String>,
)

private data class Pr06BranchEntry(
    val seed: Long,
    val startZoneId: String,
    val professionId: String,
    val raceId: String,
    val routeIntent: List<String>,
    val runtimeRoute: List<String>,
    val primarySecretZoneId: String,
    val secretMarkerIndex: Int,
    val goalZoneId: String,
    val requiredVisitedZoneId: String? = null,
) {
    fun routeTokenParts(): List<String> {
        val route = expectedVisitedRoute()
        val marker = secretRouteMarker(primarySecretZoneId)
        val markerIndex = secretMarkerIndex.coerceAtMost(route.size)
        return route.take(markerIndex) + marker + route.drop(markerIndex)
    }

    private fun expectedVisitedRoute(): List<String> {
        val goalIndex = runtimeRoute.indexOf(goalZoneId)
        require(goalIndex >= 0) {
            "PR06 branch seed $seed goalZoneId=$goalZoneId must appear in runtimeRoute=$runtimeRoute."
        }
        return runtimeRoute.take(goalIndex + 1)
    }
}

private fun Map<String, Int>.toCompactText(): String =
    entries.joinToString(prefix = "{", postfix = "}") { (key, value) -> "$key=$value" }
