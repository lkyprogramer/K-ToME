package com.ktome.game.harness

import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class LongRunLabFullTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `full long run lab satisfies phase 3 thresholds`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val professions = listOf("vanguard", "arcanist", "rogue", "templar")
        val races = listOf("human", "elf", "dwarf")
        val banditRoute =
            listOf(
                "shattered_outpost",
                "greenwood_fringe",
                "bandit_camp",
                "greenwood_fringe",
                "deep_iron_pit",
                "grey_gate_depths",
                "underground_river",
                "abyssal_temple",
                "abyssal_heart",
            )
        val elvenRoute =
            listOf(
                "shattered_outpost",
                "greenwood_fringe",
                "elven_ruins",
                "greenwood_fringe",
                "deep_iron_pit",
                "grey_gate_depths",
                "underground_river",
                "abyssal_temple",
                "abyssal_heart",
            )
        val crystalRoute =
            listOf(
                "shattered_outpost",
                "greenwood_fringe",
                "deep_iron_pit",
                "grey_gate_depths",
                "underground_river",
                "crystal_cavern",
                "underground_river",
                "abyssal_temple",
                "abyssal_heart",
            )
        val moltenRoute =
            listOf(
                "shattered_outpost",
                "greenwood_fringe",
                "deep_iron_pit",
                "molten_core",
                "deep_iron_pit",
                "grey_gate_depths",
                "underground_river",
                "abyssal_temple",
                "abyssal_heart",
            )
        val scenarioStarts =
            mapOf(
                "vanguard:human" to RouteStartSpec(zoneId = FOUNDATION_ZONE_ROUTE.first(), zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 0),
                "vanguard:elf" to RouteStartSpec(zoneId = FOUNDATION_ZONE_ROUTE.first(), zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 0),
                "vanguard:dwarf" to RouteStartSpec(zoneId = "molten_core", zoneRoute = moltenRoute, routeIndex = 3),
                "arcanist:human" to RouteStartSpec(zoneId = "crystal_cavern", zoneRoute = crystalRoute, routeIndex = 5),
                "arcanist:elf" to RouteStartSpec(zoneId = FOUNDATION_ZONE_ROUTE.first(), zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 0),
                "arcanist:dwarf" to RouteStartSpec(zoneId = "underground_river", zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 4),
                "rogue:human" to RouteStartSpec(zoneId = FOUNDATION_ZONE_ROUTE.first(), zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 0),
                "rogue:elf" to RouteStartSpec(zoneId = "elven_ruins", zoneRoute = elvenRoute, routeIndex = 2),
                "rogue:dwarf" to RouteStartSpec(zoneId = FOUNDATION_ZONE_ROUTE.first(), zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 0),
                "templar:human" to RouteStartSpec(zoneId = "bandit_camp", zoneRoute = banditRoute, routeIndex = 2),
                "templar:elf" to RouteStartSpec(zoneId = FOUNDATION_ZONE_ROUTE.first(), zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 0),
                "templar:dwarf" to RouteStartSpec(zoneId = FOUNDATION_ZONE_ROUTE.first(), zoneRoute = FOUNDATION_ZONE_ROUTE, routeIndex = 0),
            )
        val reports =
            professions.flatMapIndexed { professionIndex, professionId ->
                races.mapIndexed { raceIndex, raceId ->
                    val routePlan = requireNotNull(scenarioStarts["$professionId:$raceId"])
                    harness.run(
                        ScenarioSpec(
                            name = "long-run-full-$professionId-$raceId",
                            seed = 20260330L + professionIndex * 10L + raceIndex,
                            professionId = professionId,
                            raceId = raceId,
                            zoneId = routePlan.zoneId,
                            zoneRoute = routePlan.zoneRoute,
                            routeIndex = routePlan.routeIndex,
                            corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                            maxTurns = 1800,
                            goal = ScenarioGoal.ReachTerminal,
                            saveLoadCheckpoint =
                                if (professionId == "vanguard" && raceId == "human") {
                                    SaveLoadCheckpoint(floor = 1, continueTurns = 40)
                                } else {
                                    null
                                },
                            assertions =
                                if (professionId == "vanguard" && raceId == "human") {
                                    listOf(ScenarioAssertion.CheckpointRoundTrip)
                                } else {
                                    emptyList()
                                },
                        ),
                    )
                }
            }

        val reachedTempleCount = reports.count { report -> zoneDepth(report.finalZoneId) >= zoneDepth("abyssal_temple") }
        val nonVictoryReports = reports.filter { report -> report.outcome !is RunOutcome.Victory }
        val failuresAfterDeepIron =
            nonVictoryReports.count { report -> zoneDepth(report.finalZoneId) > zoneDepth("deep_iron_pit") }
        val afterDeepIronRatio =
            if (nonVictoryReports.isEmpty()) {
                null
            } else {
                failuresAfterDeepIron.toDouble() / nonVictoryReports.size.toDouble()
            }
        val averageTurns =
            if (reports.isEmpty()) {
                0.0
            } else {
                reports.map(ScenarioReport::turns).average()
            }
        val averageHeadlessTurns =
            if (reports.isEmpty()) {
                0.0
            } else {
                reports.map(ScenarioReport::headlessTurnEquivalent).average()
            }
        val branchSampleCount = reports.count { report -> report.zonePath.any(OPTIONAL_ZONE_IDS::contains) }
        val deathDistribution = nonVictoryReports.groupingBy(ScenarioReport::finalZoneId).eachCount().toSortedMap()
        val routeHashDistribution = reports.groupingBy(ScenarioReport::zoneRouteHash).eachCount().toSortedMap()
        val milestoneRewards = reports.flatMap(ScenarioReport::milestoneRewards)
        val milestoneRewardQualityDistribution = milestoneRewards.groupingBy { it.qualityTier.name }.eachCount().toSortedMap()
        val milestoneAffixCountDistribution = milestoneRewards.groupingBy { it.affixIds.size.toString() }.eachCount().toSortedMap()
        val milestoneRewardAdoptionDistribution =
            milestoneRewards
                .groupingBy { reward -> if (reward.adoptedInFinalBuild) "adopted" else "notAdopted" }
                .eachCount()
                .toSortedMap()
        val milestoneRewardSlotDistribution = milestoneRewards.groupingBy { it.equipSlot.name }.eachCount().toSortedMap()
        val routeRewardAffixUsageSummary =
            milestoneRewards
                .filter { reward -> reward.rewardSource == MilestoneRewardSource.ROUTE }
                .flatMap { reward -> reward.affixIds.ifEmpty { listOf("none") } }
                .groupingBy { it }
                .eachCount()
                .toSortedMap()

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "long-run-full",
            payload =
                buildJsonObject {
                    put("sliceId", "phase3-pr07-long-run-full-v2")
                    put("buildId", HarnessMetadata.BUILD_ID)
                    put("phaseId", HarnessMetadata.PHASE_ID)
                    put("rulesetVersion", HarnessMetadata.RULESET_VERSION)
                    put("traceSchemaVersion", HarnessMetadata.TRACE_SCHEMA_VERSION)
                    put("corpusId", HarnessMetadata.LONG_RUN_FULL_CORPUS_ID)
                    put("profileId", HarnessMetadata.PROFILE_ID)
                    put("localeId", reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed")
                    put("scenarioCount", reports.size)
                    put("branchSampleCount", branchSampleCount)
                    put("reachedTempleCount", reachedTempleCount)
                    put("nonVictoryCount", nonVictoryReports.size)
                    put("failuresAfterDeepIron", failuresAfterDeepIron)
                    if (afterDeepIronRatio == null) {
                        put("afterDeepIronRatio", "N/A")
                    } else {
                        put("afterDeepIronRatio", afterDeepIronRatio)
                    }
                    put("averageTurns", averageTurns)
                    put("averageHeadlessTurns", averageHeadlessTurns)
                    putJsonObject("deathDistribution") {
                        deathDistribution.forEach { (zoneId, count) -> put(zoneId, count) }
                    }
                    putJsonObject("zoneRouteHashDistribution") {
                        routeHashDistribution.forEach { (routeHash, count) -> put(routeHash, count) }
                    }
                    putJsonObject("milestoneRewardQualityDistribution") {
                        milestoneRewardQualityDistribution.forEach { (quality, count) -> put(quality, count) }
                    }
                    putJsonObject("milestoneAffixCountDistribution") {
                        milestoneAffixCountDistribution.forEach { (affixCount, count) -> put(affixCount, count) }
                    }
                    putJsonObject("milestoneRewardAdoptionDistribution") {
                        milestoneRewardAdoptionDistribution.forEach { (adoption, count) -> put(adoption, count) }
                    }
                    putJsonObject("milestoneRewardSlotDistribution") {
                        milestoneRewardSlotDistribution.forEach { (slotId, count) -> put(slotId, count) }
                    }
                    putJsonObject("routeRewardAffixUsageSummary") {
                        routeRewardAffixUsageSummary.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    putJsonArray("reports") {
                        reports.forEach { add(it.toJson()) }
                    }
                },
            markdown =
                buildString {
                    appendLine("# Long Run Lab Full")
                    appendLine("- sliceId: phase3-pr07-long-run-full-v2")
                    appendLine("- buildId: ${HarnessMetadata.BUILD_ID}")
                    appendLine("- phaseId: ${HarnessMetadata.PHASE_ID}")
                    appendLine("- rulesetVersion: ${HarnessMetadata.RULESET_VERSION}")
                    appendLine("- traceSchemaVersion: ${HarnessMetadata.TRACE_SCHEMA_VERSION}")
                    appendLine("- corpusId: ${HarnessMetadata.LONG_RUN_FULL_CORPUS_ID}")
                    appendLine("- profileId: ${HarnessMetadata.PROFILE_ID}")
                    appendLine("- localeId: ${reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed"}")
                    appendLine("- branchSampleCount: $branchSampleCount/${reports.size}")
                    appendLine("- reachedTempleCount: $reachedTempleCount/${reports.size}")
                    appendLine("- nonVictoryCount: ${nonVictoryReports.size}")
                    appendLine("- failuresAfterDeepIron: $failuresAfterDeepIron")
                    appendLine("- afterDeepIronRatio: ${afterDeepIronRatio ?: "N/A"}")
                    appendLine("- averageTurns: $averageTurns")
                    appendLine("- averageHeadlessTurns: $averageHeadlessTurns")
                    appendLine("- deathDistribution: ${if (deathDistribution.isEmpty()) "none" else deathDistribution}")
                    appendLine("- zoneRouteHashDistribution: ${if (routeHashDistribution.isEmpty()) "none" else routeHashDistribution}")
                    appendLine("- milestoneRewardQualityDistribution: ${if (milestoneRewardQualityDistribution.isEmpty()) "none" else milestoneRewardQualityDistribution}")
                    appendLine("- milestoneAffixCountDistribution: ${if (milestoneAffixCountDistribution.isEmpty()) "none" else milestoneAffixCountDistribution}")
                    appendLine("- milestoneRewardAdoptionDistribution: ${if (milestoneRewardAdoptionDistribution.isEmpty()) "none" else milestoneRewardAdoptionDistribution}")
                    appendLine("- milestoneRewardSlotDistribution: ${if (milestoneRewardSlotDistribution.isEmpty()) "none" else milestoneRewardSlotDistribution}")
                    appendLine("- routeRewardAffixUsageSummary: ${if (routeRewardAffixUsageSummary.isEmpty()) "none" else routeRewardAffixUsageSummary}")
                    reports.forEach { report ->
                        val objectiveSummary =
                            report.zoneObjectiveSummaries.joinToString { summary ->
                                "${summary.zoneId}:${summary.state.name}${if (summary.completionFlagGranted) "#flag" else ""}"
                            }
                        val milestoneSummary =
                            report.milestoneRewards.joinToString { reward ->
                                "${reward.rewardSource}:${reward.baseItemId}:${reward.equipSlot.name}:before=${reward.equippedBaseItemIdBeforeReward ?: "empty"}:final=${reward.equippedBaseItemIdAtRunEnd ?: "empty"}:adopted=${reward.adoptedInFinalBuild}:${reward.qualityTier.name}:${if (reward.affixIds.isEmpty()) "none" else reward.affixIds.joinToString("+")}"
                            }
                        appendLine(
                            "- class=${report.professionId}, race=${report.raceId}, seed=${report.seed}, finalZone=${report.finalZoneId}, turns=${report.turns}, headless=${report.headlessTurnEquivalent}, routeHash=${report.zoneRouteHash}, route=${report.zonePath.joinToString(" -> ")}, objectives=${if (objectiveSummary.isBlank()) "none" else objectiveSummary}, buildHash=${report.buildHash ?: "unknown"}, milestoneRewards=${if (milestoneSummary.isBlank()) "none" else milestoneSummary}, outcome=${report.outcome}, crashedOrStalled=${report.crashedOrStalled()}",
                        )
                        if (report.headlessTurnEquivalent > 2900 || report.outcome !is RunOutcome.Victory) {
                            val zoneHeadlessSummary =
                                report.zoneHeadlessMilestones.joinToString { milestone ->
                                    "${milestone.zoneId}:${milestone.headlessTurnEquivalent}(+${milestone.deltaHeadlessTurns})"
                                }
                            appendLine("  zoneHeadless=$zoneHeadlessSummary")
                        }
                        if (report.finalZoneId == "shattered_outpost") {
                            val captainTraceSummary =
                                report.captainEncounterTrace.takeLast(6).joinToString { entry ->
                                    "t${entry.turnIndex}/h${entry.headlessTurnEquivalent}/hp${entry.playerHp}/${entry.playerMaxHp}/res${entry.playerResourceCurrent}/${entry.playerResourceMax}/${entry.playerResourceTypeId}/boss${entry.captainHp ?: -1}/${entry.captainMaxHp ?: -1}/d${entry.captainDistance ?: -1}/${entry.command ?: "-"}"
                                }
                            appendLine("  captainTrace=$captainTraceSummary")
                        }
                    }
                },
        )

        assertTrue(
            reports.none(ScenarioReport::crashedOrStalled),
            reports.filter(ScenarioReport::crashedOrStalled).joinToString(separator = "\n") { report ->
                "${report.professionId}/${report.raceId}/${report.seed}: ${report.failureReason ?: report.stuckReason ?: "unknown"}"
            },
        )
        assertTrue(
            reports.all { report -> report.headlessTurnEquivalent <= 3000 },
            "Expected all full-lab runs to stay within headlessTurnEquivalent <= 3000.",
        )
        assertTrue(
            branchSampleCount >= 4,
            "Expected branch-inclusive long-run full matrix to include at least 4 non-mainline samples, actual=$branchSampleCount/${reports.size}",
        )
        assertTrue(
            routeHashDistribution.size >= 3,
            "Expected long-run full matrix to exercise at least 3 distinct route hashes, actual=$routeHashDistribution",
        )
        assertTrue(
            reachedTempleCount >= 8,
            "Expected at least 8/12 matrix runs to reach abyssal_temple or deeper, actual=$reachedTempleCount/${reports.size}",
        )
        if (afterDeepIronRatio != null) {
            assertTrue(
                afterDeepIronRatio >= 0.5,
                "Expected at least 50% of non-victory runs to fail after deep_iron_pit, actual=$afterDeepIronRatio",
            )
        }
    }

    private fun zoneDepth(zoneId: String): Int =
        when (zoneId) {
            "shattered_outpost" -> 0
            "greenwood_fringe" -> 1
            "bandit_camp" -> 2
            "elven_ruins" -> 3
            "deep_iron_pit" -> 4
            "molten_core" -> 5
            "grey_gate_depths" -> 6
            "underground_river" -> 7
            "crystal_cavern" -> 8
            "abyssal_temple" -> 9
            "abyssal_heart" -> 10
            else -> -1
        }

    private companion object {
        val OPTIONAL_ZONE_IDS: Set<String> = setOf("bandit_camp", "elven_ruins", "molten_core", "crystal_cavern")
    }

    private data class RouteStartSpec(
        val zoneId: String,
        val zoneRoute: List<String>,
        val routeIndex: Int,
    )
}
