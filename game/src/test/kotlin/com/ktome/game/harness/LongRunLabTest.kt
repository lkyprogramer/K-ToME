package com.ktome.game.harness

import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_PRIMARY_BREAKPOINT_PAYOFF_TALENTS
import com.ktome.game.FOUNDATION_SYNERGY_AFFIX_IDS
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

class LongRunLabTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    @Tag("longRunLab")
    fun `nightly long run lab distinguishes full route smoke from probes`() {
        val harness = HeadlessRunHarness(rootDir = tempDir)
        val officialSliceReports =
            listOf(
                fullRouteSmokeSpec(
                    name = "long-run-vanguard-20260312",
                    seed = 20260312L,
                    professionId = "vanguard",
                ),
                fullRouteSmokeSpec(
                    name = "long-run-arcanist-20260313",
                    seed = 20260313L,
                    professionId = "arcanist",
                    saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40),
                    assertions =
                        listOf(
                            ScenarioAssertion.CheckpointRoundTrip,
                            ScenarioAssertion.NoFailure,
                            ScenarioAssertion.NoStall,
                        ),
                ),
                fullRouteSmokeSpec(
                    name = "long-run-rogue-20260360",
                    seed = 20260360L,
                    professionId = "rogue",
                ),
                fullRouteSmokeSpec(
                    name = "long-run-templar-20260315",
                    seed = 20260315L,
                    professionId = "templar",
                ),
            ).map(harness::run)
        val advancedFullRouteReports =
            listOf(
                fullRouteSmokeSpec(
                    name = "long-run-advanced-berserker-20260318",
                    seed = 20260318L,
                    professionId = "berserker",
                    maxTurns = 2200,
                ),
                fullRouteSmokeSpec(
                    name = "long-run-advanced-spellblade-20260319",
                    seed = 20260319L,
                    professionId = "spellblade",
                    maxTurns = 2200,
                ),
            ).map(harness::run)
        val advancedLateRouteProbeReports =
            listOf(
                lateRouteProbeSpec(
                    name = "long-run-advanced-berserker-late-route-probe-20260318",
                    seed = 20260318L,
                    professionId = "berserker",
                ),
                lateRouteProbeSpec(
                    name = "long-run-advanced-spellblade-late-route-probe-20260319",
                    seed = 20260319L,
                    professionId = "spellblade",
                ),
            ).map(harness::run)
        val branchInclusiveReports =
            listOf(
                branchInclusiveSmokeSpec(
                    name = "long-run-branch-rogue-20260320",
                    seed = 20260320L,
                    professionId = "rogue",
                    zoneRoute = FOUNDATION_BANDIT_ROUTE,
                    branchZoneId = "bandit_camp",
                    maxTurns = 2400,
                ),
            ).map(harness::run)
        val routeProbeReports =
            listOf(
                ScenarioSpec(
                    name = "long-run-rogue-deep-iron-pit-route-probe",
                    seed = 20260316L,
                    zoneId = "deep_iron_pit",
                    professionId = "rogue",
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 2,
                    scenarioType = ScenarioType.ROUTE_PROBE,
                    corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
                    maxTurns = 900,
                    goal = ScenarioGoal.ReachFloor(2),
                    saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40),
                    assertions =
                        listOf(
                            ScenarioAssertion.ReachedFloorAtLeast(2),
                            ScenarioAssertion.CheckpointRoundTrip,
                            ScenarioAssertion.NoFailure,
                            ScenarioAssertion.NoStall,
                        ),
                ),
                ScenarioSpec(
                    name = "long-run-templar-grey-gate-depths-route-probe",
                    seed = 20260317L,
                    zoneId = "grey_gate_depths",
                    professionId = "templar",
                    zoneRoute = FOUNDATION_ZONE_ROUTE,
                    routeIndex = 3,
                    scenarioType = ScenarioType.ROUTE_PROBE,
                    corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
                    maxTurns = 900,
                    goal = ScenarioGoal.ReachFloor(2),
                    saveLoadCheckpoint = SaveLoadCheckpoint(floor = 1, continueTurns = 40),
                    assertions =
                        listOf(
                            ScenarioAssertion.ReachedFloorAtLeast(2),
                            ScenarioAssertion.CheckpointRoundTrip,
                            ScenarioAssertion.NoFailure,
                            ScenarioAssertion.NoStall,
                        ),
                ),
            ).map(harness::run)
        val reports =
            officialSliceReports +
                advancedFullRouteReports +
                advancedLateRouteProbeReports +
                branchInclusiveReports +
                routeProbeReports

        val fullRouteReports = reports.filter { report -> report.scenarioType == ScenarioType.FULL_ROUTE }
        val lateRouteProbeReports = reports.filter { report -> report.scenarioType == ScenarioType.LATE_ROUTE_PROBE }
        val foundationReports = reports.filter { report -> report.professionId in FOUNDATION_PRIMARY_BREAKPOINT_PAYOFF_TALENTS.keys }
        val scenarioTypeDistribution = scenarioTypeDistribution(reports)
        val nonVictoryReports = reports.filter { report -> report.outcome !is RunOutcome.Victory }
        val deathDistribution = nonVictoryReports.groupingBy(ScenarioReport::finalZoneId).eachCount().toSortedMap()
        val routeHashDistribution = reports.groupingBy(ScenarioReport::zoneRouteHash).eachCount().toSortedMap()
        val milestoneRewards = reports.flatMap(ScenarioReport::milestoneRewards)
        val allScenarioBreakpointMetrics = breakpointMetrics(reports)
        val foundationBreakpointMetrics = breakpointMetrics(foundationReports)
        val cadenceRewardCount = reports.sumOf(ScenarioReport::cadenceRewardCount)
        val shopRefreshPurchaseCount = reports.sumOf(ScenarioReport::shopRefreshPurchaseCount)
        val allScenarioAffixSynergyMetrics = affixSynergyMetrics(reports)
        val foundationAffixSynergyMetrics = affixSynergyMetrics(foundationReports)
        val allScenarioSynergyRewardMetrics = synergyRewardMetrics(reports)
        val foundationSynergyRewardMetrics = synergyRewardMetrics(foundationReports)
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
        val averageTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::turns).average()
        val averageHeadlessTurns = if (reports.isEmpty()) 0.0 else reports.map(ScenarioReport::headlessTurnEquivalent).average()
        val officialTerminalCount = officialSliceReports.count(ScenarioReport::success)
        val advancedFullRouteSuccessCount = advancedFullRouteReports.count(ScenarioReport::success)
        val advancedLateRouteProbeSuccessCount = advancedLateRouteProbeReports.count(ScenarioReport::success)
        val failingReports = reports.filterNot(ScenarioReport::success)
        val fullRouteCount = fullRouteReports.size
        val branchInclusiveCount = branchInclusiveReports.size
        val routeProbeCount = routeProbeReports.size
        val lateRouteProbeCount = lateRouteProbeReports.size
        val summary =
            buildJsonObject {
                put("sliceId", "phase3-pr15-long-run-smoke-v1")
                put("buildId", HarnessMetadata.BUILD_ID)
                put("phaseId", HarnessMetadata.PHASE_ID)
                put("rulesetVersion", HarnessMetadata.RULESET_VERSION)
                put("traceSchemaVersion", HarnessMetadata.TRACE_SCHEMA_VERSION)
                put("corpusId", HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID)
                put("profileId", HarnessMetadata.PROFILE_ID)
                put("localeId", reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed")
                put("seedCount", reports.size)
                put("fullRouteCount", fullRouteCount)
                put("branchInclusiveCount", branchInclusiveCount)
                put("routeProbeCount", routeProbeCount)
                put("lateRouteProbeCount", lateRouteProbeCount)
                put("officialSliceCount", officialSliceReports.size)
                put("advancedFullRouteCount", advancedFullRouteReports.size)
                put("advancedLateRouteProbeCount", advancedLateRouteProbeReports.size)
                put("officialTerminalCount", officialTerminalCount)
                put("advancedFullRouteSuccessCount", advancedFullRouteSuccessCount)
                put("advancedLateRouteProbeSuccessCount", advancedLateRouteProbeSuccessCount)
                put("branchInclusiveSuccessCount", branchInclusiveReports.count(ScenarioReport::success))
                put("routeProbeSuccessCount", routeProbeReports.count(ScenarioReport::success))
                put("averageTurns", averageTurns)
                put("averageHeadlessTurns", averageHeadlessTurns)
                put("cadenceRewardCount", cadenceRewardCount)
                put("shopRefreshPurchaseCount", shopRefreshPurchaseCount)
                put("affixSynergyActivationCount", allScenarioAffixSynergyMetrics.activationCount)
                putJsonObject("affixSynergyActivationDistribution") {
                    allScenarioAffixSynergyMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                }
                put("synergyAffixRewardCount", allScenarioSynergyRewardMetrics.rewardCount)
                put("synergyAffixAdoptionCount", allScenarioSynergyRewardMetrics.adoptionCount)
                putJsonObject("synergyAffixDistribution") {
                    allScenarioSynergyRewardMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                }
                put("breakpointPayoffObservationCount", allScenarioBreakpointMetrics.observationCount)
                put("breakpointPayoffBuildHashChangeCount", allScenarioBreakpointMetrics.buildHashChangeCount)
                putJsonObject("breakpointPayoffTalentDistribution") {
                    allScenarioBreakpointMetrics.talentDistribution.forEach { (talentId, count) -> put(talentId, count) }
                }
                putJsonObject("breakpointPayoffEffectDistribution") {
                    allScenarioBreakpointMetrics.effectDistribution.forEach { (effectKind, count) -> put(effectKind, count) }
                }
                put("foundationBreakpointPayoffObservationCount", foundationBreakpointMetrics.observationCount)
                put("foundationBreakpointPayoffBuildHashChangeCount", foundationBreakpointMetrics.buildHashChangeCount)
                put("foundationAffixSynergyActivationCount", foundationAffixSynergyMetrics.activationCount)
                putJsonObject("foundationAffixSynergyActivationDistribution") {
                    foundationAffixSynergyMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                }
                put("foundationSynergyAffixRewardCount", foundationSynergyRewardMetrics.rewardCount)
                put("foundationSynergyAffixAdoptionCount", foundationSynergyRewardMetrics.adoptionCount)
                putJsonObject("foundationSynergyAffixDistribution") {
                    foundationSynergyRewardMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                }
                putJsonObject("foundationBreakpointPayoffTalentDistribution") {
                    foundationBreakpointMetrics.talentDistribution.forEach { (talentId, count) -> put(talentId, count) }
                }
                putJsonObject("foundationBreakpointPayoffEffectDistribution") {
                    foundationBreakpointMetrics.effectDistribution.forEach { (effectKind, count) -> put(effectKind, count) }
                }
                putJsonObject("scenarioTypeDistribution") {
                    scenarioTypeDistribution.forEach { (scenarioType, count) -> put(scenarioType, count) }
                }
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
            }

        HarnessReportWriter.writeJsonAndMarkdown(
            fileStem = "long-run-summary",
            payload = summary,
            markdown =
                buildString {
                    appendLine("# Long Run Lab")
                    appendLine("- sliceId: phase3-pr15-long-run-smoke-v1")
                    appendLine("- buildId: ${HarnessMetadata.BUILD_ID}")
                    appendLine("- phaseId: ${HarnessMetadata.PHASE_ID}")
                    appendLine("- rulesetVersion: ${HarnessMetadata.RULESET_VERSION}")
                    appendLine("- traceSchemaVersion: ${HarnessMetadata.TRACE_SCHEMA_VERSION}")
                    appendLine("- corpusId: ${HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID}")
                    appendLine("- profileId: ${HarnessMetadata.PROFILE_ID}")
                    appendLine("- localeId: ${reports.map(ScenarioReport::localeId).distinct().singleOrNull() ?: "mixed"}")
                    appendLine("- fullRouteCount: $fullRouteCount")
                    appendLine("- branchInclusiveCount: $branchInclusiveCount")
                    appendLine("- routeProbeCount: $routeProbeCount")
                    appendLine("- lateRouteProbeCount: $lateRouteProbeCount")
                    appendLine("- officialTerminalCount: $officialTerminalCount/${officialSliceReports.size}")
                    appendLine("- advancedFullRouteSuccessCount: $advancedFullRouteSuccessCount/${advancedFullRouteReports.size}")
                    appendLine("- advancedLateRouteProbeSuccessCount: $advancedLateRouteProbeSuccessCount/${advancedLateRouteProbeReports.size}")
                    appendLine("- branchInclusiveSuccessCount: ${branchInclusiveReports.count(ScenarioReport::success)}/${branchInclusiveReports.size}")
                    appendLine("- routeProbeSuccessCount: ${routeProbeReports.count(ScenarioReport::success)}/${routeProbeReports.size}")
                    appendLine("- scenarioTypeDistribution: $scenarioTypeDistribution")
                    appendLine("- averageTurns: $averageTurns")
                    appendLine("- averageHeadlessTurns: $averageHeadlessTurns")
                    appendLine("- cadenceRewardCount: $cadenceRewardCount")
                    appendLine("- shopRefreshPurchaseCount: $shopRefreshPurchaseCount")
                    appendLine("- affixSynergyActivationCount: ${allScenarioAffixSynergyMetrics.activationCount}")
                    appendLine("- affixSynergyActivationDistribution: ${if (allScenarioAffixSynergyMetrics.distribution.isEmpty()) "none" else allScenarioAffixSynergyMetrics.distribution}")
                    appendLine("- synergyAffixRewardCount: ${allScenarioSynergyRewardMetrics.rewardCount}")
                    appendLine("- synergyAffixAdoptionCount: ${allScenarioSynergyRewardMetrics.adoptionCount}")
                    appendLine("- synergyAffixDistribution: ${if (allScenarioSynergyRewardMetrics.distribution.isEmpty()) "none" else allScenarioSynergyRewardMetrics.distribution}")
                    appendLine("- breakpointPayoffObservationCount: ${allScenarioBreakpointMetrics.observationCount}")
                    appendLine("- breakpointPayoffBuildHashChangeCount: ${allScenarioBreakpointMetrics.buildHashChangeCount}")
                    appendLine("- breakpointPayoffTalentDistribution: ${if (allScenarioBreakpointMetrics.talentDistribution.isEmpty()) "none" else allScenarioBreakpointMetrics.talentDistribution}")
                    appendLine("- breakpointPayoffEffectDistribution: ${if (allScenarioBreakpointMetrics.effectDistribution.isEmpty()) "none" else allScenarioBreakpointMetrics.effectDistribution}")
                    appendLine("- foundationBreakpointPayoffObservationCount: ${foundationBreakpointMetrics.observationCount}")
                    appendLine("- foundationBreakpointPayoffBuildHashChangeCount: ${foundationBreakpointMetrics.buildHashChangeCount}")
                    appendLine("- foundationAffixSynergyActivationCount: ${foundationAffixSynergyMetrics.activationCount}")
                    appendLine("- foundationAffixSynergyActivationDistribution: ${if (foundationAffixSynergyMetrics.distribution.isEmpty()) "none" else foundationAffixSynergyMetrics.distribution}")
                    appendLine("- foundationSynergyAffixRewardCount: ${foundationSynergyRewardMetrics.rewardCount}")
                    appendLine("- foundationSynergyAffixAdoptionCount: ${foundationSynergyRewardMetrics.adoptionCount}")
                    appendLine("- foundationSynergyAffixDistribution: ${if (foundationSynergyRewardMetrics.distribution.isEmpty()) "none" else foundationSynergyRewardMetrics.distribution}")
                    appendLine("- foundationBreakpointPayoffTalentDistribution: ${if (foundationBreakpointMetrics.talentDistribution.isEmpty()) "none" else foundationBreakpointMetrics.talentDistribution}")
                    appendLine("- foundationBreakpointPayoffEffectDistribution: ${if (foundationBreakpointMetrics.effectDistribution.isEmpty()) "none" else foundationBreakpointMetrics.effectDistribution}")
                    appendLine("- deathDistribution: ${if (deathDistribution.isEmpty()) "none" else deathDistribution}")
                    appendLine("- zoneRouteHashDistribution: ${if (routeHashDistribution.isEmpty()) "none" else routeHashDistribution}")
                    appendLine("- milestoneRewardQualityDistribution: ${if (milestoneRewardQualityDistribution.isEmpty()) "none" else milestoneRewardQualityDistribution}")
                    appendLine("- milestoneAffixCountDistribution: ${if (milestoneAffixCountDistribution.isEmpty()) "none" else milestoneAffixCountDistribution}")
                    appendLine("- milestoneRewardAdoptionDistribution: ${if (milestoneRewardAdoptionDistribution.isEmpty()) "none" else milestoneRewardAdoptionDistribution}")
                    appendLine("- milestoneRewardSlotDistribution: ${if (milestoneRewardSlotDistribution.isEmpty()) "none" else milestoneRewardSlotDistribution}")
                    appendLine("- routeRewardAffixUsageSummary: ${if (routeRewardAffixUsageSummary.isEmpty()) "none" else routeRewardAffixUsageSummary}")
                    reports.forEach { report ->
                        val milestoneSummary =
                            report.milestoneRewards.joinToString { reward ->
                                "${reward.rewardSource}:${reward.baseItemId}:${reward.equipSlot.name}:before=${reward.equippedBaseItemIdBeforeReward ?: "empty"}:final=${reward.equippedBaseItemIdAtRunEnd ?: "empty"}:adopted=${reward.adoptedInFinalBuild}:${reward.qualityTier.name}:${if (reward.affixIds.isEmpty()) "none" else reward.affixIds.joinToString("+")}"
                            }
                        val breakpointSummary =
                            report.breakpointPayoffs.joinToString { payoff ->
                                "${payoff.talentId}@${payoff.breakpointRank}:${payoff.unlockedEffectKinds.joinToString("+")}"
                            }
                        val breakpointObservationSummary =
                            report.breakpointPayoffObservations.joinToString { observation ->
                                "${observation.talentId}@${observation.breakpointRank}:${observation.buildHashChanged}:${observation.buildHashBeforeUnlock}->${observation.buildHashAfterUnlock}"
                            }
                        appendLine(
                            "- profession=${report.professionId}, race=${report.raceId}, seed=${report.seed}, zone=${report.zoneId}, routeIndex=${report.routeIndex}, scenarioType=${report.scenarioType.reportValue}, isFullRoute=${report.isFullRoute}, success=${report.success}, floor=${report.floorReached}, turns=${report.turns}, headless=${report.headlessTurnEquivalent}, finalZone=${report.finalZoneId}, routeHash=${report.zoneRouteHash}, buildHash=${report.buildHash ?: "unknown"}, breakpointPayoffs=${if (breakpointSummary.isBlank()) "none" else breakpointSummary}, breakpointPayoffObservations=${if (breakpointObservationSummary.isBlank()) "none" else breakpointObservationSummary}, affixSynergy=${report.affixSynergyActivationCount}:${if (report.affixSynergyActivationDistribution.isEmpty()) "none" else report.affixSynergyActivationDistribution}, cadence=${report.cadenceRewardCount}, refresh=${report.shopRefreshPurchaseCount}, milestoneRewards=${if (milestoneSummary.isBlank()) "none" else milestoneSummary}, outcome=${report.outcome}",
                        )
                    }
                },
        )

        assertTrue(
            failingReports.isEmpty(),
            failingReports.joinToString(separator = "\n") { report ->
                val tail = (report.assertionFailures + listOfNotNull(report.failureReason, report.stuckReason)).joinToString()
                "${report.professionId}/${report.seed}/${report.zoneId}/${report.scenarioType.reportValue}: ${tail.ifBlank { report.outcome.toString() }}"
            },
        )
        assertTrue(
            officialTerminalCount == officialSliceReports.size,
            "Expected official full-route smoke to reach a terminal state, actual=$officialTerminalCount/${officialSliceReports.size}",
        )
        assertTrue(
            advancedFullRouteSuccessCount == advancedFullRouteReports.size,
            "Expected advanced-class complete smokes to start from shattered_outpost and reach a terminal state, actual=$advancedFullRouteSuccessCount/${advancedFullRouteReports.size}",
        )
        assertTrue(
            advancedLateRouteProbeSuccessCount == advancedLateRouteProbeReports.size,
            "Expected advanced-class late-route probes to remain viable without replacing full-route smoke, actual=$advancedLateRouteProbeSuccessCount/${advancedLateRouteProbeReports.size}",
        )
        assertTrue(
            fullRouteReports.all(ScenarioReport::isFullRoute),
            "Expected every full-route smoke sample to carry isFullRoute=true.",
        )
        assertTrue(
            advancedLateRouteProbeReports.none(ScenarioReport::isFullRoute),
            "Expected late-route probes to stay downgraded and never masquerade as full-route samples.",
        )
        assertTrue(
            routeHashDistribution.size >= 3,
            "Expected smoke long-run lab to exercise at least three distinct route hashes across full-route, branch-inclusive and probe scenarios, actual=$routeHashDistribution",
        )
        assertTrue(
            branchInclusiveReports.all(ScenarioReport::success),
            "Expected branch-inclusive smoke probes to complete without harness failures.",
        )
        FOUNDATION_PRIMARY_BREAKPOINT_PAYOFF_TALENTS.forEach { (professionId, talentId) ->
            val report = officialSliceReports.first { it.professionId == professionId }
            assertTrue(
                report.breakpointPayoffObservations.any { observation -> observation.talentId == talentId },
                "Expected smoke foundation run $professionId to observe breakpoint payoff $talentId, actual=${report.breakpointPayoffObservations}",
            )
            assertTrue(
                report.breakpointPayoffObservations.any { observation -> observation.talentId == talentId && observation.buildHashChanged },
                "Expected smoke foundation run $professionId to record a build-hash change when unlocking $talentId.",
            )
        }
        assertTrue(
            foundationBreakpointMetrics.talentDistribution.keys.containsAll(FOUNDATION_PRIMARY_BREAKPOINT_PAYOFF_TALENTS.values),
            "Expected foundation-only smoke metrics to expose all documented base-class payoff talents, actual=${foundationBreakpointMetrics.talentDistribution}",
        )
        assertTrue(
            allScenarioSynergyRewardMetrics.rewardCount >= 1,
            "Expected smoke long-run lab to surface at least one documented synergy affix reward, actual=${allScenarioSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            foundationSynergyRewardMetrics.rewardCount >= 1,
            "Expected foundation-only smoke metrics to surface at least one documented synergy affix reward, actual=${foundationSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            foundationSynergyRewardMetrics.adoptionCount >= 1,
            "Expected foundation-only smoke metrics to keep at least one documented synergy affix in the final build, actual=${foundationSynergyRewardMetrics.distribution}",
        )
    }

    private fun fullRouteSmokeSpec(
        name: String,
        seed: Long,
        professionId: String,
        maxTurns: Int = 1800,
        saveLoadCheckpoint: SaveLoadCheckpoint? = null,
        assertions: List<ScenarioAssertion> = listOf(ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
    ): ScenarioSpec =
        ScenarioSpec(
            name = name,
            seed = seed,
            zoneId = FOUNDATION_ZONE_ROUTE.first(),
            professionId = professionId,
            zoneRoute = FOUNDATION_ZONE_ROUTE,
            routeIndex = 0,
            scenarioType = ScenarioType.FULL_ROUTE,
            corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
            maxTurns = maxTurns,
            goal = ScenarioGoal.ReachTerminal,
            saveLoadCheckpoint = saveLoadCheckpoint,
            assertions = assertions,
        )

    private fun lateRouteProbeSpec(
        name: String,
        seed: Long,
        professionId: String,
    ): ScenarioSpec =
        ScenarioSpec(
            name = name,
            seed = seed,
            professionId = professionId,
            zoneId = "abyssal_temple",
            zoneRoute = listOf("abyssal_temple", "abyssal_heart"),
            routeIndex = 0,
            scenarioType = ScenarioType.LATE_ROUTE_PROBE,
            corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
            maxTurns = 1600,
            goal = ScenarioGoal.ReachZoneAtLeastOrTerminal("abyssal_heart"),
            assertions = listOf(ScenarioAssertion.NoFailure, ScenarioAssertion.NoStall),
        )

    private fun branchInclusiveSmokeSpec(
        name: String,
        seed: Long,
        professionId: String,
        zoneRoute: List<String>,
        branchZoneId: String,
        maxTurns: Int,
    ): ScenarioSpec =
        ScenarioSpec(
            name = name,
            seed = seed,
            zoneId = FOUNDATION_ZONE_ROUTE.first(),
            professionId = professionId,
            zoneRoute = zoneRoute,
            routeIndex = 0,
            scenarioType = ScenarioType.BRANCH_INCLUSIVE,
            corpusId = HarnessMetadata.LONG_RUN_SMOKE_CORPUS_ID,
            maxTurns = maxTurns,
            goal = ScenarioGoal.ReachTerminal,
            assertions =
                listOf(
                    ScenarioAssertion.NoFailure,
                    ScenarioAssertion.NoStall,
                    ScenarioAssertion.VisitedZone(branchZoneId),
                ),
        )

    private fun breakpointMetrics(reports: List<ScenarioReport>): BreakpointMetrics {
        val breakpointPayoffs = reports.flatMap(ScenarioReport::breakpointPayoffs)
        val observations = reports.flatMap(ScenarioReport::breakpointPayoffObservations)
        return BreakpointMetrics(
            observationCount = observations.size,
            buildHashChangeCount = observations.count { observation -> observation.buildHashChanged },
            talentDistribution = breakpointPayoffs.groupingBy { payoff -> payoff.talentId }.eachCount().toSortedMap(),
            effectDistribution =
                breakpointPayoffs
                    .flatMap { payoff -> payoff.unlockedEffectKinds }
                    .groupingBy { effectKind -> effectKind }
                    .eachCount()
                    .toSortedMap(),
        )
    }

    private fun affixSynergyMetrics(reports: List<ScenarioReport>): AffixSynergyMetrics =
        AffixSynergyMetrics(
            activationCount = reports.sumOf(ScenarioReport::affixSynergyActivationCount),
            distribution =
                reports
                    .flatMap { report -> report.affixSynergyActivationDistribution.entries }
                    .groupingBy { (affixId, _) -> affixId }
                    .fold(0) { accumulator, (_, count) -> accumulator + count }
                    .toSortedMap(),
        )

    private fun synergyRewardMetrics(reports: List<ScenarioReport>): SynergyRewardMetrics =
        SynergyRewardMetrics(
            rewardCount =
                reports.sumOf { report ->
                    val synergyAffixIds = FOUNDATION_SYNERGY_AFFIX_IDS[report.professionId].orEmpty()
                    report.milestoneRewards.count { reward -> reward.affixIds.any(synergyAffixIds::contains) }
                },
            adoptionCount =
                reports.sumOf { report ->
                    val synergyAffixIds = FOUNDATION_SYNERGY_AFFIX_IDS[report.professionId].orEmpty()
                    report.milestoneRewards.count { reward -> reward.adoptedInFinalBuild && reward.affixIds.any(synergyAffixIds::contains) }
                },
            distribution =
                reports
                    .flatMap { report ->
                        val synergyAffixIds = FOUNDATION_SYNERGY_AFFIX_IDS[report.professionId].orEmpty()
                        report.milestoneRewards.flatMap { reward -> reward.affixIds.filter(synergyAffixIds::contains) }
                    }.groupingBy { affixId -> affixId }
                    .eachCount()
                    .toSortedMap(),
        )

    private data class BreakpointMetrics(
        val observationCount: Int,
        val buildHashChangeCount: Int,
        val talentDistribution: Map<String, Int>,
        val effectDistribution: Map<String, Int>,
    )

    private data class AffixSynergyMetrics(
        val activationCount: Int,
        val distribution: Map<String, Int>,
    )

    private data class SynergyRewardMetrics(
        val rewardCount: Int,
        val adoptionCount: Int,
        val distribution: Map<String, Int>,
    )
}
