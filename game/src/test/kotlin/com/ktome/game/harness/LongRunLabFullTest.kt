package com.ktome.game.harness

import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import java.nio.file.Path
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LongRunLabFullTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `full long run lab separates full route gate from branch inclusive probes`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val fullRouteReports = fullRouteMatrixSpecs().map(harness::run)
        val branchInclusiveReports = branchProbeMatrixSpecs().map(harness::run)
        val reports = fullRouteReports + branchInclusiveReports
        val routeProbeReports = reports.filter { report -> report.scenarioType == ScenarioType.ROUTE_PROBE }
        val lateRouteProbeReports = reports.filter { report -> report.scenarioType == ScenarioType.LATE_ROUTE_PROBE }
        val fullRouteNonVictoryReports = fullRouteReports.filter { report -> report.outcome !is RunOutcome.Victory }
        val reachedTempleCount = fullRouteReports.count { report -> zoneDepth(report.finalZoneId) >= zoneDepth("abyssal_temple") }
        val failuresAfterDeepIron =
            fullRouteNonVictoryReports.count { report -> zoneDepth(report.finalZoneId) > zoneDepth("deep_iron_pit") }
        val afterDeepIronRatio =
            if (fullRouteNonVictoryReports.isEmpty()) {
                null
            } else {
                failuresAfterDeepIron.toDouble() / fullRouteNonVictoryReports.size.toDouble()
            }
        val averageTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::turns).average()
        val averageHeadlessTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::headlessTurnEquivalent).average()
        val branchSampleCount = branchInclusiveReports.size
        val deathDistribution = fullRouteNonVictoryReports.groupingBy(ScenarioReport::finalZoneId).eachCount().toSortedMap()
        val routeHashDistribution = reports.groupingBy(ScenarioReport::zoneRouteHash).eachCount().toSortedMap()
        val branchRouteHashDistribution = branchInclusiveReports.groupingBy(ScenarioReport::zoneRouteHash).eachCount().toSortedMap()
        val scenarioTypeDistribution = scenarioTypeDistribution(reports, includeZeroCounts = true)
        val milestoneRewards = reports.flatMap(ScenarioReport::milestoneRewards)
        val cadenceRewardCount = reports.sumOf(ScenarioReport::cadenceRewardCount)
        val shopRefreshPurchaseCount = reports.sumOf(ScenarioReport::shopRefreshPurchaseCount)
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
        val failingReports = reports.filterNot(ScenarioReport::success)

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "long-run-full",
            payload =
                buildJsonObject {
                    put("sliceId", "phase3-pr15-long-run-full-v1")
                    put("buildId", HarnessMetadata.BUILD_ID)
                    put("phaseId", HarnessMetadata.PHASE_ID)
                    put("rulesetVersion", HarnessMetadata.RULESET_VERSION)
                    put("traceSchemaVersion", HarnessMetadata.TRACE_SCHEMA_VERSION)
                    put("corpusId", HarnessMetadata.LONG_RUN_FULL_CORPUS_ID)
                    put("profileId", HarnessMetadata.PROFILE_ID)
                    put("localeId", reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed")
                    put("scenarioCount", reports.size)
                    put("fullRouteCount", fullRouteReports.size)
                    put("branchInclusiveCount", branchInclusiveReports.size)
                    put("routeProbeCount", routeProbeReports.size)
                    put("lateRouteProbeCount", lateRouteProbeReports.size)
                    put("branchSampleCount", branchSampleCount)
                    put("reachedTempleCount", reachedTempleCount)
                    put("nonVictoryCount", fullRouteNonVictoryReports.size)
                    put("failuresAfterDeepIron", failuresAfterDeepIron)
                    if (afterDeepIronRatio == null) {
                        put("afterDeepIronRatio", "N/A")
                    } else {
                        put("afterDeepIronRatio", afterDeepIronRatio)
                    }
                    put("averageTurns", averageTurns)
                    put("averageHeadlessTurns", averageHeadlessTurns)
                    put("cadenceRewardCount", cadenceRewardCount)
                    put("shopRefreshPurchaseCount", shopRefreshPurchaseCount)
                    putJsonObject("scenarioTypeDistribution") {
                        scenarioTypeDistribution.forEach { (scenarioType, count) -> put(scenarioType, count) }
                    }
                    putJsonObject("deathDistribution") {
                        deathDistribution.forEach { (zoneId, count) -> put(zoneId, count) }
                    }
                    putJsonObject("zoneRouteHashDistribution") {
                        routeHashDistribution.forEach { (routeHash, count) -> put(routeHash, count) }
                    }
                    putJsonObject("branchRouteHashDistribution") {
                        branchRouteHashDistribution.forEach { (routeHash, count) -> put(routeHash, count) }
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
                    appendLine("- sliceId: phase3-pr15-long-run-full-v1")
                    appendLine("- buildId: ${HarnessMetadata.BUILD_ID}")
                    appendLine("- phaseId: ${HarnessMetadata.PHASE_ID}")
                    appendLine("- rulesetVersion: ${HarnessMetadata.RULESET_VERSION}")
                    appendLine("- traceSchemaVersion: ${HarnessMetadata.TRACE_SCHEMA_VERSION}")
                    appendLine("- corpusId: ${HarnessMetadata.LONG_RUN_FULL_CORPUS_ID}")
                    appendLine("- profileId: ${HarnessMetadata.PROFILE_ID}")
                    appendLine("- localeId: ${reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed"}")
                    appendLine("- fullRouteCount: ${fullRouteReports.size}")
                    appendLine("- branchInclusiveCount: ${branchInclusiveReports.size}")
                    appendLine("- routeProbeCount: ${routeProbeReports.size}")
                    appendLine("- lateRouteProbeCount: ${lateRouteProbeReports.size}")
                    appendLine("- branchSampleCount: $branchSampleCount/${reports.size}")
                    appendLine("- reachedTempleCount: $reachedTempleCount/${fullRouteReports.size}")
                    appendLine("- nonVictoryCount: ${fullRouteNonVictoryReports.size}")
                    appendLine("- failuresAfterDeepIron: $failuresAfterDeepIron")
                    appendLine("- afterDeepIronRatio: ${afterDeepIronRatio ?: "N/A"}")
                    appendLine("- scenarioTypeDistribution: $scenarioTypeDistribution")
                    appendLine("- averageTurns: $averageTurns")
                    appendLine("- averageHeadlessTurns: $averageHeadlessTurns")
                    appendLine("- cadenceRewardCount: $cadenceRewardCount")
                    appendLine("- shopRefreshPurchaseCount: $shopRefreshPurchaseCount")
                    appendLine("- deathDistribution: ${if (deathDistribution.isEmpty()) "none" else deathDistribution}")
                    appendLine("- zoneRouteHashDistribution: ${if (routeHashDistribution.isEmpty()) "none" else routeHashDistribution}")
                    appendLine("- branchRouteHashDistribution: ${if (branchRouteHashDistribution.isEmpty()) "none" else branchRouteHashDistribution}")
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
                            "- class=${report.professionId}, race=${report.raceId}, seed=${report.seed}, scenarioType=${report.scenarioType.reportValue}, isFullRoute=${report.isFullRoute}, finalZone=${report.finalZoneId}, turns=${report.turns}, headless=${report.headlessTurnEquivalent}, routeHash=${report.zoneRouteHash}, route=${report.zonePath.joinToString(" -> ")}, objectives=${if (objectiveSummary.isBlank()) "none" else objectiveSummary}, buildHash=${report.buildHash ?: "unknown"}, cadence=${report.cadenceRewardCount}, refresh=${report.shopRefreshPurchaseCount}, milestoneRewards=${if (milestoneSummary.isBlank()) "none" else milestoneSummary}, outcome=${report.outcome}, crashedOrStalled=${report.crashedOrStalled()}",
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
                "${report.professionId}/${report.raceId}/${report.seed}/${report.scenarioType.reportValue}: ${report.failureReason ?: report.stuckReason ?: "unknown"}"
            },
        )
        assertTrue(
            failingReports.isEmpty(),
            failingReports.joinToString(separator = "\n") { report ->
                val tail = (report.assertionFailures + listOfNotNull(report.failureReason, report.stuckReason)).joinToString()
                "${report.professionId}/${report.raceId}/${report.seed}/${report.scenarioType.reportValue}: ${tail.ifBlank { report.outcome.toString() }}"
            },
        )
        assertTrue(
            fullRouteReports.size == 12,
            "Expected full-route matrix to freeze at 12 foundation runs, actual=${fullRouteReports.size}",
        )
        assertTrue(
            branchInclusiveReports.size == 4,
            "Expected branch-inclusive probe matrix to freeze at four route variants, actual=${branchInclusiveReports.size}",
        )
        assertTrue(
            fullRouteReports.all { report ->
                report.isFullRoute &&
                    report.zoneId == FOUNDATION_ZONE_ROUTE.first() &&
                    report.routeIndex == 0
            },
            "Expected every full-route matrix sample to start at shattered_outpost routeIndex=0.",
        )
        assertTrue(
            branchInclusiveReports.all { report ->
                !report.isFullRoute &&
                    report.zoneId == FOUNDATION_ZONE_ROUTE.first() &&
                    report.zonePath.any(OPTIONAL_ROUTE_ZONE_IDS::contains)
            },
            "Expected branch-inclusive probes to start at shattered_outpost but remain explicitly downgraded from full-route gate.",
        )
        assertTrue(
            routeProbeReports.isEmpty(),
            "LongRunLabFullTest should no longer include direct route probes; those belong in smoke labs only.",
        )
        assertTrue(
            lateRouteProbeReports.isEmpty(),
            "LongRunLabFullTest should no longer include late-route probes; those belong in smoke labs only.",
        )
        assertTrue(
            fullRouteReports.all { report -> report.headlessTurnEquivalent <= 3000 },
            "Expected all full-route gate runs to stay within headlessTurnEquivalent <= 3000.",
        )
        assertTrue(
            branchSampleCount >= 4,
            "Expected branch-inclusive matrix to include all four optional branches, actual=$branchSampleCount/${reports.size}",
        )
        assertTrue(
            branchRouteHashDistribution.size >= 3,
            "Expected branch-inclusive probe matrix to exercise at least 3 distinct route hashes, actual=$branchRouteHashDistribution",
        )
        assertTrue(
            branchInclusiveReports.any { report ->
                report.zonePath.contains("underground_river") && report.zonePath.contains("crystal_cavern")
            },
            "Expected branch-inclusive matrix to keep at least one routed sample through underground_river -> crystal_cavern.",
        )
        assertTrue(
            reachedTempleCount >= 8,
            "Expected at least 8/12 full-route matrix runs to reach abyssal_temple or deeper, actual=$reachedTempleCount/${fullRouteReports.size}",
        )
        if (afterDeepIronRatio != null) {
            assertTrue(
                afterDeepIronRatio >= 0.5,
                "Expected at least 50% of full-route non-victory runs to fail after deep_iron_pit, actual=$afterDeepIronRatio",
            )
        }
    }

    private fun fullRouteMatrixSpecs(): List<ScenarioSpec> {
        val professions = listOf("vanguard", "arcanist", "rogue", "templar")
        val races = listOf("human", "elf", "dwarf")
        return professions.flatMapIndexed { professionIndex, professionId ->
            races.mapIndexed { raceIndex, raceId ->
                ScenarioSpec(
                    name = "long-run-full-$professionId-$raceId",
                    seed = 20260330L + professionIndex * 10L + raceIndex,
                    professionId = professionId,
                    raceId = raceId,
                    zoneId = FOUNDATION_ZONE_ROUTE.first(),
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 0,
                    scenarioType = ScenarioType.FULL_ROUTE,
                    corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
                    maxTurns = 1800,
                    goal = ScenarioGoal.ReachTerminal,
                    assertions = emptyList(),
                )
            }
        }
    }

    private fun branchProbeMatrixSpecs(): List<ScenarioSpec> =
        listOf(
            branchInclusiveSpec(
                name = "long-run-branch-bandit-rogue-human",
                seed = 20260320L,
                professionId = "rogue",
                raceId = "human",
                zoneRoute = FOUNDATION_BANDIT_ROUTE,
                goalZoneId = "bandit_camp",
            ),
            branchInclusiveSpec(
                name = "long-run-branch-elven-rogue-elf",
                seed = 20260451L,
                professionId = "rogue",
                raceId = "elf",
                zoneRoute = FOUNDATION_ELVEN_ROUTE,
                goalZoneId = "elven_ruins",
            ),
            branchInclusiveSpec(
                name = "long-run-branch-molten-vanguard-dwarf",
                seed = 20260432L,
                professionId = "vanguard",
                raceId = "dwarf",
                zoneRoute = FOUNDATION_MOLTEN_ROUTE,
                goalZoneId = "molten_core",
            ),
            branchInclusiveSpec(
                name = "long-run-branch-crystal-arcanist-human",
                seed = 20260440L,
                professionId = "arcanist",
                raceId = "human",
                zoneRoute = FOUNDATION_CRYSTAL_ROUTE,
                goalZoneId = "crystal_cavern",
            ),
        )

    private fun branchInclusiveSpec(
        name: String,
        seed: Long,
        professionId: String,
        raceId: String,
        zoneRoute: List<String>,
        goalZoneId: String,
    ): ScenarioSpec =
        ScenarioSpec(
            name = name,
            seed = seed,
            professionId = professionId,
            raceId = raceId,
            zoneId = FOUNDATION_ZONE_ROUTE.first(),
            zoneRoute = zoneRoute,
            routeIndex = 0,
            scenarioType = ScenarioType.BRANCH_INCLUSIVE,
            corpusId = HarnessMetadata.LONG_RUN_FULL_CORPUS_ID,
            maxTurns = 1800,
            goal = ScenarioGoal.ReachZoneAtLeastOrTerminal(goalZoneId),
            assertions =
                listOf(
                    ScenarioAssertion.NoFailure,
                    ScenarioAssertion.NoStall,
                    ScenarioAssertion.VisitedZone(goalZoneId),
                ),
        )

}
