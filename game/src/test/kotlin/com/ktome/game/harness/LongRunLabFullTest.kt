package com.ktome.game.harness

import com.ktome.core.item.MilestoneRewardSource
import com.ktome.core.run.RunOutcome
import com.ktome.game.FOUNDATION_SYNERGY_AFFIX_IDS
import com.ktome.game.FOUNDATION_ZONE_ROUTE
import com.ktome.game.data.DataLoader
import java.nio.file.Path
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
        val fullRouteSpecs = fullRouteMatrixSpecs()
        val branchInclusiveSpecs = branchProbeMatrixSpecs()
        val kernelExecution =
            LongRunKernelCache.execute(
                rootDir = tempDir,
                specs = fullRouteSpecs + branchInclusiveSpecs,
            )
        val reportsByName = kernelExecution.reports.associateBy(ScenarioReport::name)
        val fullRouteReports = fullRouteSpecs.map { spec -> requireNotNull(reportsByName[spec.name]) }
        val branchInclusiveReports = branchInclusiveSpecs.map { spec -> requireNotNull(reportsByName[spec.name]) }
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
        val allScenarioBreakpointMetrics = breakpointMetrics(reports)
        val fullRouteBreakpointMetrics = breakpointMetrics(fullRouteReports)
        val cadenceRewardCount = reports.sumOf(ScenarioReport::cadenceRewardCount)
        val shopRefreshPurchaseCount = reports.sumOf(ScenarioReport::shopRefreshPurchaseCount)
        val lateRunReliquaryPurchaseCount = reports.sumOf(ScenarioReport::lateRunReliquaryPurchaseCount)
        val lateRunReliquaryVisitCount = reports.sumOf(ScenarioReport::lateRunReliquaryVisitCount)
        val lateRunReliquaryRefreshCount = reports.sumOf(ScenarioReport::lateRunReliquaryRefreshCount)
        val lateRunReliquaryItemPurchaseCount = reports.sumOf(ScenarioReport::lateRunReliquaryItemPurchaseCount)
        val lateRunReliquaryNonMandatoryPurchaseCount = reports.sumOf(ScenarioReport::lateRunReliquaryNonMandatoryPurchaseCount)
        val lateRunReliquaryShardSpent = reports.sumOf(ScenarioReport::lateRunReliquaryShardSpent)
        val lateRunReliquaryTagDistribution = aggregateReliquaryTagDistribution(reports)
        val allScenarioAffixSynergyMetrics = affixSynergyMetrics(reports)
        val fullRouteAffixSynergyMetrics = affixSynergyMetrics(fullRouteReports)
        val allScenarioSynergyRewardMetrics = synergyRewardMetrics(reports)
        val fullRouteSynergyRewardMetrics = synergyRewardMetrics(fullRouteReports)
        val terminalWeaponIdentity = terminalWeaponIdentitySummary(fullRouteReports)
        val fullRouteZoneTraversalDiagnostics = aggregateZoneTraversalDiagnostics(fullRouteReports)
        val criticalPathZoneIds = criticalPathZoneIds()
        val criticalPathZoneDesignAudit = criticalPathZoneDesignAudit()
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
                    put("kernelCache", kernelExecution.cacheMetadata(Path.of(System.getProperty("ktome.repo.root", "."))))
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
                    put("lateRunReliquaryPurchaseCount", lateRunReliquaryPurchaseCount)
                    put("lateRunReliquaryVisitCount", lateRunReliquaryVisitCount)
                    put("lateRunReliquaryRefreshCount", lateRunReliquaryRefreshCount)
                    put("lateRunReliquaryItemPurchaseCount", lateRunReliquaryItemPurchaseCount)
                    put("lateRunReliquaryNonMandatoryPurchaseCount", lateRunReliquaryNonMandatoryPurchaseCount)
                    put("lateRunReliquaryShardSpent", lateRunReliquaryShardSpent)
                    putJsonObject("lateRunReliquaryTagDistribution") {
                        lateRunReliquaryTagDistribution.forEach { (tag, count) -> put(tag, count) }
                    }
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
                    put("fullRouteBreakpointPayoffObservationCount", fullRouteBreakpointMetrics.observationCount)
                    put("fullRouteBreakpointPayoffBuildHashChangeCount", fullRouteBreakpointMetrics.buildHashChangeCount)
                    put("fullRouteAffixSynergyActivationCount", fullRouteAffixSynergyMetrics.activationCount)
                    putJsonObject("fullRouteAffixSynergyActivationDistribution") {
                        fullRouteAffixSynergyMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    put("fullRouteSynergyAffixRewardCount", fullRouteSynergyRewardMetrics.rewardCount)
                    put("fullRouteSynergyAffixAdoptionCount", fullRouteSynergyRewardMetrics.adoptionCount)
                    putJsonObject("fullRouteSynergyAffixDistribution") {
                        fullRouteSynergyRewardMetrics.distribution.forEach { (affixId, count) -> put(affixId, count) }
                    }
                    putJsonObject("fullRouteBreakpointPayoffTalentDistribution") {
                        fullRouteBreakpointMetrics.talentDistribution.forEach { (talentId, count) -> put(talentId, count) }
                    }
                    putJsonObject("fullRouteBreakpointPayoffEffectDistribution") {
                        fullRouteBreakpointMetrics.effectDistribution.forEach { (effectKind, count) -> put(effectKind, count) }
                    }
                    put("terminalWeaponBaseDiversity", terminalWeaponIdentity.terminalWeaponBaseDiversity)
                    put("crossProfessionTopWeaponDominance", terminalWeaponIdentity.crossProfessionTopWeaponDominance)
                    put("professionAlignedWeaponAdoptionRate", terminalWeaponIdentity.professionAlignedWeaponAdoptionRate)
                    put("alignedFullRouteSampleCount", terminalWeaponIdentity.alignedFullRouteSampleCount)
                    put("fullRouteSampleCount", terminalWeaponIdentity.fullRouteSampleCount)
                    terminalWeaponIdentity.topWeaponBaseId?.let { topWeaponBaseId -> put("crossProfessionTopWeaponBaseId", topWeaponBaseId) }
                    put("crossProfessionTopWeaponCount", terminalWeaponIdentity.topWeaponCount)
                    putJsonObject("professionTerminalWeaponDistribution") {
                        terminalWeaponIdentity.professionTerminalWeaponDistribution.forEach { (professionId, distribution) ->
                            putJsonObject(professionId) {
                                distribution.forEach { (weaponBaseId, count) -> put(weaponBaseId, count) }
                            }
                        }
                    }
                    putJsonObject("professionTopWeaponBaseIds") {
                        terminalWeaponIdentity.professionTopWeaponBaseIds.forEach { (professionId, weaponBaseId) ->
                            put(professionId, weaponBaseId)
                        }
                    }
                    putJsonObject("professionTopWeaponSemanticTags") {
                        terminalWeaponIdentity.professionTopWeaponSemanticTags.forEach { (professionId, semanticTags) ->
                            putJsonArray(professionId) {
                                semanticTags.forEach { tag -> add(JsonPrimitive(tag)) }
                            }
                        }
                    }
                    putJsonObject("fullRouteZoneTraversalDiagnostics") {
                        fullRouteZoneTraversalDiagnostics.forEach { (zoneId, diagnostic) ->
                            putJsonObject(zoneId) {
                                put("sampleCount", diagnostic.sampleCount)
                                put("visitCount", diagnostic.visitCount)
                                put("avgPlayerTurns", diagnostic.avgPlayerTurns)
                                put("avgEnemyTurns", diagnostic.avgEnemyTurns)
                                put("avgEnemyTurnsPerPlayerTurn", diagnostic.avgEnemyTurnsPerPlayerTurn)
                                put("avgVisibleHostileTurnCount", diagnostic.avgVisibleHostileTurnCount)
                                put("avgLiveHostileWindow", diagnostic.avgLiveHostileWindow)
                                put("maxVisibleHostiles", diagnostic.maxVisibleHostiles)
                                put("objectiveAcquireSampleCount", diagnostic.objectiveAcquireSampleCount)
                                diagnostic.avgObjectiveAcquireTurn?.let { put("avgObjectiveAcquireTurn", it) }
                                diagnostic.avgObjectiveAcquireHeadlessTurnEquivalent?.let { put("avgObjectiveAcquireHeadlessTurnEquivalent", it) }
                                putJsonObject("objectiveStateDistribution") {
                                    diagnostic.objectiveStateDistribution.forEach { (state, count) -> put(state, count) }
                                }
                            }
                        }
                    }
                    putJsonArray("criticalPathZoneIds") {
                        criticalPathZoneIds.forEach { zoneId -> add(JsonPrimitive(zoneId)) }
                    }
                    putJsonObject("criticalPathZoneDesignAudit") {
                        criticalPathZoneDesignAudit.forEach { (zoneId, audit) ->
                            putJsonObject(zoneId) {
                                put("floorCount", audit.floorCount)
                                put("mapSize", audit.mapSize)
                                put("worldRole", audit.worldRole)
                                put("monsterPoolCount", audit.monsterPoolCount)
                                put("elitePoolCount", audit.elitePoolCount)
                                audit.bossEncounterId?.let { put("bossEncounterId", it) }
                                audit.objectiveSetId?.let { put("objectiveSetId", it) }
                                audit.objectiveCompletionRule?.let { put("objectiveCompletionRule", it) }
                                putJsonArray("specialMechanics") {
                                    audit.specialMechanics.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
                                }
                                putJsonArray("mechanicsWithoutDedicatedRuntimeHook") {
                                    audit.mechanicsWithoutDedicatedRuntimeHook.forEach { mechanic -> add(JsonPrimitive(mechanic)) }
                                }
                                putJsonArray("objectivePlacements") {
                                    audit.objectivePlacements.forEach { placement -> add(JsonPrimitive(placement)) }
                                }
                                putJsonObject("terrainTagWeights") {
                                    audit.terrainTagWeights.forEach { (terrainTag, weight) -> put(terrainTag, weight) }
                                }
                            }
                        }
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
                    appendLine("- lateRunReliquaryPurchaseCount: $lateRunReliquaryPurchaseCount")
                    appendLine("- lateRunReliquaryVisitCount: $lateRunReliquaryVisitCount")
                    appendLine("- lateRunReliquaryRefreshCount: $lateRunReliquaryRefreshCount")
                    appendLine("- lateRunReliquaryItemPurchaseCount: $lateRunReliquaryItemPurchaseCount")
                    appendLine("- lateRunReliquaryNonMandatoryPurchaseCount: $lateRunReliquaryNonMandatoryPurchaseCount")
                    appendLine("- lateRunReliquaryShardSpent: $lateRunReliquaryShardSpent")
                    appendLine("- lateRunReliquaryTagDistribution: ${if (lateRunReliquaryTagDistribution.isEmpty()) "none" else lateRunReliquaryTagDistribution}")
                    appendLine("- affixSynergyActivationCount: ${allScenarioAffixSynergyMetrics.activationCount}")
                    appendLine("- affixSynergyActivationDistribution: ${if (allScenarioAffixSynergyMetrics.distribution.isEmpty()) "none" else allScenarioAffixSynergyMetrics.distribution}")
                    appendLine("- synergyAffixRewardCount: ${allScenarioSynergyRewardMetrics.rewardCount}")
                    appendLine("- synergyAffixAdoptionCount: ${allScenarioSynergyRewardMetrics.adoptionCount}")
                    appendLine("- synergyAffixDistribution: ${if (allScenarioSynergyRewardMetrics.distribution.isEmpty()) "none" else allScenarioSynergyRewardMetrics.distribution}")
                    appendLine("- breakpointPayoffObservationCount: ${allScenarioBreakpointMetrics.observationCount}")
                    appendLine("- breakpointPayoffBuildHashChangeCount: ${allScenarioBreakpointMetrics.buildHashChangeCount}")
                    appendLine("- breakpointPayoffTalentDistribution: ${if (allScenarioBreakpointMetrics.talentDistribution.isEmpty()) "none" else allScenarioBreakpointMetrics.talentDistribution}")
                    appendLine("- breakpointPayoffEffectDistribution: ${if (allScenarioBreakpointMetrics.effectDistribution.isEmpty()) "none" else allScenarioBreakpointMetrics.effectDistribution}")
                    appendLine("- fullRouteBreakpointPayoffObservationCount: ${fullRouteBreakpointMetrics.observationCount}")
                    appendLine("- fullRouteBreakpointPayoffBuildHashChangeCount: ${fullRouteBreakpointMetrics.buildHashChangeCount}")
                    appendLine("- fullRouteAffixSynergyActivationCount: ${fullRouteAffixSynergyMetrics.activationCount}")
                    appendLine("- fullRouteAffixSynergyActivationDistribution: ${if (fullRouteAffixSynergyMetrics.distribution.isEmpty()) "none" else fullRouteAffixSynergyMetrics.distribution}")
                    appendLine("- fullRouteSynergyAffixRewardCount: ${fullRouteSynergyRewardMetrics.rewardCount}")
                    appendLine("- fullRouteSynergyAffixAdoptionCount: ${fullRouteSynergyRewardMetrics.adoptionCount}")
                    appendLine("- fullRouteSynergyAffixDistribution: ${if (fullRouteSynergyRewardMetrics.distribution.isEmpty()) "none" else fullRouteSynergyRewardMetrics.distribution}")
                    appendLine("- fullRouteBreakpointPayoffTalentDistribution: ${if (fullRouteBreakpointMetrics.talentDistribution.isEmpty()) "none" else fullRouteBreakpointMetrics.talentDistribution}")
                    appendLine("- fullRouteBreakpointPayoffEffectDistribution: ${if (fullRouteBreakpointMetrics.effectDistribution.isEmpty()) "none" else fullRouteBreakpointMetrics.effectDistribution}")
                    appendLine("- terminalWeaponBaseDiversity: ${terminalWeaponIdentity.terminalWeaponBaseDiversity}")
                    appendLine("- crossProfessionTopWeaponDominance: ${terminalWeaponIdentity.crossProfessionTopWeaponDominance}")
                    appendLine("- professionAlignedWeaponAdoptionRate: ${terminalWeaponIdentity.professionAlignedWeaponAdoptionRate}")
                    appendLine("- crossProfessionTopWeaponBaseId: ${terminalWeaponIdentity.topWeaponBaseId ?: "none"}")
                    appendLine("- crossProfessionTopWeaponCount: ${terminalWeaponIdentity.topWeaponCount}/${terminalWeaponIdentity.fullRouteSampleCount}")
                    appendLine("- professionTerminalWeaponDistribution: ${if (terminalWeaponIdentity.professionTerminalWeaponDistribution.isEmpty()) "none" else terminalWeaponIdentity.professionTerminalWeaponDistribution}")
                    appendLine("- professionTopWeaponBaseIds: ${if (terminalWeaponIdentity.professionTopWeaponBaseIds.isEmpty()) "none" else terminalWeaponIdentity.professionTopWeaponBaseIds}")
                    appendLine("- professionTopWeaponSemanticTags: ${if (terminalWeaponIdentity.professionTopWeaponSemanticTags.isEmpty()) "none" else terminalWeaponIdentity.professionTopWeaponSemanticTags}")
                    appendLine("- fullRouteZoneTraversalDiagnostics: ${if (fullRouteZoneTraversalDiagnostics.isEmpty()) "none" else fullRouteZoneTraversalDiagnostics}")
                    appendLine("- criticalPathZoneIds: ${criticalPathZoneIds.joinToString()}")
                    appendLine("- criticalPathZoneDesignAudit: ${if (criticalPathZoneDesignAudit.isEmpty()) "none" else criticalPathZoneDesignAudit}")
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
                        val breakpointSummary =
                            report.breakpointPayoffs.joinToString { payoff ->
                                "${payoff.talentId}@${payoff.breakpointRank}:${payoff.unlockedEffectKinds.joinToString("+")}"
                            }
                        val breakpointObservationSummary =
                            report.breakpointPayoffObservations.joinToString { observation ->
                                "${observation.talentId}@${observation.breakpointRank}:${observation.buildHashChanged}:${observation.buildHashBeforeUnlock}->${observation.buildHashAfterUnlock}"
                            }
                        appendLine(
                            "- class=${report.professionId}, race=${report.raceId}, seed=${report.seed}, scenarioType=${report.scenarioType.reportValue}, isFullRoute=${report.isFullRoute}, finalZone=${report.finalZoneId}, turns=${report.turns}, headless=${report.headlessTurnEquivalent}, routeHash=${report.zoneRouteHash}, route=${report.zonePath.joinToString(" -> ")}, objectives=${if (objectiveSummary.isBlank()) "none" else objectiveSummary}, buildHash=${report.buildHash ?: "unknown"}, breakpointPayoffs=${if (breakpointSummary.isBlank()) "none" else breakpointSummary}, breakpointPayoffObservations=${if (breakpointObservationSummary.isBlank()) "none" else breakpointObservationSummary}, affixSynergy=${report.affixSynergyActivationCount}:${if (report.affixSynergyActivationDistribution.isEmpty()) "none" else report.affixSynergyActivationDistribution}, cadence=${report.cadenceRewardCount}, refresh=${report.shopRefreshPurchaseCount}, reliquary={visits=${report.lateRunReliquaryVisitCount}, purchases=${report.lateRunReliquaryPurchaseCount}, items=${report.lateRunReliquaryItemPurchaseCount}, refreshes=${report.lateRunReliquaryRefreshCount}, nonMandatory=${report.lateRunReliquaryNonMandatoryPurchaseCount}, spent=${report.lateRunReliquaryShardSpent}, tags=${if (report.lateRunReliquaryTagDistribution.isEmpty()) "none" else report.lateRunReliquaryTagDistribution}}, milestoneRewards=${if (milestoneSummary.isBlank()) "none" else milestoneSummary}, outcome=${report.outcome}, crashedOrStalled=${report.crashedOrStalled()}",
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
        val expectedGuardProfileZoneCoverage =
            mapOf(
                "elven_ruins" to setOf("long-run-branch-elven-rogue-elf"),
                "molten_core" to setOf("long-run-branch-molten-vanguard-dwarf"),
                "underground_river" to
                    (
                        fullRouteReports.map(ScenarioReport::name).toSet() +
                            setOf("long-run-branch-crystal-arcanist-human")
                    ),
            )
        expectedGuardProfileZoneCoverage.forEach { (zoneId, expectedScenarioNames) ->
            val zoneReports = reports.filter { report -> zoneId in report.zonePath }
            val actualScenarioNames = zoneReports.map(ScenarioReport::name).toSet()
            assertEquals(
                expectedScenarioNames,
                actualScenarioNames,
                "Expected PR-20 focus zone '$zoneId' to keep its frozen long-run coverage set.",
            )
            assertTrue(
                zoneReports.all { report -> report.success && !report.crashedOrStalled() },
                "Expected PR-20 focus zone '$zoneId' to remain stable under long-run coverage, actual=${zoneReports.map { report -> "${report.name}:${report.zonePath}:${report.failureReason ?: report.stuckReason ?: report.outcome}" }}",
            )
        }
        assertTrue(
            reachedTempleCount >= 8,
            "Expected at least 8/12 full-route matrix runs to reach abyssal_temple or deeper, actual=$reachedTempleCount/${fullRouteReports.size}",
        )
        if (afterDeepIronRatio != null && fullRouteNonVictoryReports.size >= 2) {
            assertTrue(
                afterDeepIronRatio >= 0.5,
                "Expected at least 50% of full-route non-victory runs to fail after deep_iron_pit, actual=$afterDeepIronRatio",
            )
        }
        assertTrue(
            fullRouteBreakpointMetrics.observationCount >= 4,
            "Expected full-route matrix to observe at least four breakpoint payoff unlocks, actual=${fullRouteBreakpointMetrics.observationCount}",
        )
        assertTrue(
            fullRouteBreakpointMetrics.buildHashChangeCount >= 4,
            "Expected full-route matrix to record build-hash changes for observed payoff unlocks, actual=${fullRouteBreakpointMetrics.buildHashChangeCount}",
        )
        assertTrue(
            allScenarioSynergyRewardMetrics.rewardCount >= 1,
            "Expected full long-run lab to surface at least one documented synergy affix reward, actual=${allScenarioSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            fullRouteSynergyRewardMetrics.rewardCount >= 1,
            "Expected full-route matrix to surface at least one documented synergy affix reward, actual=${fullRouteSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            fullRouteSynergyRewardMetrics.adoptionCount >= 1,
            "Expected full-route matrix to keep at least one documented synergy affix in the final build, actual=${fullRouteSynergyRewardMetrics.distribution}",
        )
        assertTrue(
            fullRouteReports.any { report -> report.lateRunReliquaryShardSpent > 0 },
            "Expected at least one full-route matrix run to spend shards at abyssal_reliquary_post, actual=${fullRouteReports.map { report -> "${report.professionId}/${report.raceId}:spent=${report.lateRunReliquaryShardSpent},visits=${report.lateRunReliquaryVisitCount},purchases=${report.lateRunReliquaryPurchaseCount}" }}",
        )
        assertTrue(
            fullRouteReports.any { report -> report.lateRunReliquaryRefreshCount > 0 || report.lateRunReliquaryNonMandatoryPurchaseCount > 0 },
            "Expected at least one full-route matrix run to make a non-mandatory or refresh reliquary spend, actual=${fullRouteReports.map { report -> "${report.professionId}/${report.raceId}:refresh=${report.lateRunReliquaryRefreshCount},nonMandatory=${report.lateRunReliquaryNonMandatoryPurchaseCount},tags=${report.lateRunReliquaryTagDistribution}" }}",
        )
    }

    @Test
    fun `terminal weapon identity summary fails fast when a full-route report is missing terminal weapon contract data`() {
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                terminalWeaponIdentitySummary(
                    listOf(
                        sampleScenarioReport(
                            name = "missing-terminal-weapon",
                            professionId = "vanguard",
                            terminalWeaponBaseId = null,
                        ),
                    ),
                )
            }

        assertTrue(exception.message.orEmpty().contains("missing terminalWeaponBaseId"))
    }

    @Test
    fun `profession aligned terminal weapon rules stay in sync with the current item catalog`() {
        val fullRouteProfessionIds = fullRouteMatrixSpecs().map(ScenarioSpec::professionId).toSet()
        val allKnownItemTags = itemTagsById.values.flatten().toSet()

        assertEquals(fullRouteProfessionIds, PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES.keys)
        PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES.forEach { (professionId, rule) ->
            assertTrue(
                rule.exactWeaponBaseIds.all(itemTagsById::containsKey),
                "Profession '$professionId' references unknown terminal weapon ids ${rule.exactWeaponBaseIds - itemTagsById.keys}.",
            )
            assertTrue(
                rule.requiredItemTags.all(allKnownItemTags::contains),
                "Profession '$professionId' references unknown alignment tags ${rule.requiredItemTags - allKnownItemTags}.",
            )
        }
    }

    @Test
    fun `zone traversal diagnostics aggregate enemy turn pressure and objective timing`() {
        val reports =
            listOf(
                sampleScenarioReport(
                    name = "diag-a",
                    professionId = "vanguard",
                    terminalWeaponBaseId = "battle_axe",
                    zoneTraversalDiagnostics =
                        listOf(
                            ZoneTraversalDiagnostic(
                                zoneId = "greenwood_fringe",
                                visitCount = 1,
                                playerTurns = 100,
                                enemyTurns = 220,
                                enemyTurnsPerPlayerTurn = 2.2,
                                visibleHostileTurnCount = 35,
                                liveHostileWindow = 12,
                                maxVisibleHostiles = 3,
                                objectiveAcquireTurn = 8,
                                objectiveAcquireHeadlessTurnEquivalent = 14,
                                objectiveStateAtExit = com.ktome.core.world.ObjectiveState.COMPLETED,
                            ),
                        ),
                ),
                sampleScenarioReport(
                    name = "diag-b",
                    professionId = "templar",
                    terminalWeaponBaseId = "long_sword",
                    zoneTraversalDiagnostics =
                        listOf(
                            ZoneTraversalDiagnostic(
                                zoneId = "greenwood_fringe",
                                visitCount = 1,
                                playerTurns = 80,
                                enemyTurns = 120,
                                enemyTurnsPerPlayerTurn = 1.5,
                                visibleHostileTurnCount = 20,
                                liveHostileWindow = 7,
                                maxVisibleHostiles = 2,
                                objectiveAcquireTurn = 6,
                                objectiveAcquireHeadlessTurnEquivalent = 10,
                                objectiveStateAtExit = com.ktome.core.world.ObjectiveState.IN_PROGRESS,
                            ),
                        ),
                ),
            )

        val aggregate = requireNotNull(aggregateZoneTraversalDiagnostics(reports)["greenwood_fringe"])

        assertEquals(2, aggregate.sampleCount)
        assertEquals(2, aggregate.visitCount)
        assertEquals(90.0, aggregate.avgPlayerTurns)
        assertEquals(170.0, aggregate.avgEnemyTurns)
        assertEquals(1.85, aggregate.avgEnemyTurnsPerPlayerTurn, 0.0001)
        assertEquals(27.5, aggregate.avgVisibleHostileTurnCount)
        assertEquals(9.5, aggregate.avgLiveHostileWindow)
        assertEquals(3, aggregate.maxVisibleHostiles)
        assertEquals(2, aggregate.objectiveAcquireSampleCount)
        assertEquals(7.0, aggregate.avgObjectiveAcquireTurn)
        assertEquals(12.0, aggregate.avgObjectiveAcquireHeadlessTurnEquivalent)
        assertEquals(mapOf("COMPLETED" to 1, "IN_PROGRESS" to 1), aggregate.objectiveStateDistribution)
    }

    @Test
    fun `critical path zone design audit exposes current pacing contracts`() {
        val audit = criticalPathZoneDesignAudit()

        assertEquals(setOf("greenwood_fringe", "deep_iron_pit", "grey_gate_depths", "underground_river", "abyssal_temple"), audit.keys)
        assertTrue(audit.getValue("greenwood_fringe").objectivePlacements.any { placement -> "trail_cache@floor1:stairs_down+1,0" in placement })
        assertTrue(audit.getValue("greenwood_fringe").mechanicsWithoutDedicatedRuntimeHook.contains("trail_pressure"))
        assertTrue(audit.getValue("deep_iron_pit").objectivePlacements.any { placement -> "mine_furnace@floor2:boss_entry+0,0" in placement })
        assertTrue(audit.getValue("deep_iron_pit").mechanicsWithoutDedicatedRuntimeHook.contains("slag_alert"))
        assertTrue(audit.getValue("grey_gate_depths").objectivePlacements.any { placement -> "seal_cache@floor1:stairs_down+1,0" in placement })
        assertTrue(audit.getValue("underground_river").objectivePlacements.any { placement -> "crystal_cache_chest@floor1:stairs_down+1,0" in placement })
        assertTrue(audit.getValue("abyssal_temple").specialMechanics.contains("void_pressure"))
    }

    private fun fullRouteMatrixSpecs(): List<ScenarioSpec> {
        val professions = listOf("vanguard", "arcanist", "rogue", "templar")
        val races = listOf("human", "elf", "dwarf")
        return professions.flatMap { professionId ->
            races.map { raceId ->
                ScenarioSpec(
                    name = "long-run-full-$professionId-$raceId",
                    seed = LongRunLabSeedBank.fullRouteMatrixSeed(professionId = professionId, raceId = raceId),
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
                seed = LongRunLabSeedBank.fullRouteMatrixSeed(professionId = "vanguard", raceId = "dwarf"),
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

    private fun aggregateReliquaryTagDistribution(reports: List<ScenarioReport>): Map<String, Int> =
        reports
            .flatMap { report -> report.lateRunReliquaryTagDistribution.entries }
            .groupingBy { entry -> entry.key }
            .fold(0) { accumulator, entry -> accumulator + entry.value }
            .toSortedMap()

    private fun terminalWeaponIdentitySummary(reports: List<ScenarioReport>): TerminalWeaponIdentitySummary {
        val missingTerminalWeaponReports =
            reports.filter { report -> report.terminalWeaponBaseId.isNullOrBlank() }
        require(missingTerminalWeaponReports.isEmpty()) {
            "Full-route reports missing terminalWeaponBaseId: " +
                missingTerminalWeaponReports.joinToString { report -> "${report.name}:${report.professionId}/${report.raceId}/${report.seed}" }
        }
        val terminalWeaponByReport =
            reports.associateWith { report -> requireNotNull(report.terminalWeaponBaseId) }
        val distribution =
            terminalWeaponByReport.entries
                .groupBy { (report, _) -> report.professionId }
                .mapValues { (_, entries) ->
                    entries.groupingBy { (_, weaponBaseId) -> weaponBaseId }.eachCount().toSortedMap()
                }.toSortedMap()
        val terminalWeaponCounts = terminalWeaponByReport.values.groupingBy { weaponBaseId -> weaponBaseId }.eachCount()
        val topWeapon = terminalWeaponCounts.maxByOrNull { (_, count) -> count }
        val professionTopWeaponBaseIds =
            distribution
                .mapValues { (_, professionDistribution) ->
                    professionDistribution.maxByOrNull { (_, count) -> count }?.key
                }.mapValues { (_, weaponBaseId) -> requireNotNull(weaponBaseId) }
        val professionTopWeaponSemanticTags =
            professionTopWeaponBaseIds.mapValues { (_, weaponBaseId) -> itemSemanticTagsById[weaponBaseId].orEmpty() }
        val alignedCount =
            terminalWeaponByReport.count { (report, weaponBaseId) ->
                isProfessionAlignedTerminalWeapon(professionId = report.professionId, weaponBaseId = weaponBaseId)
            }
        return TerminalWeaponIdentitySummary(
            fullRouteSampleCount = reports.size,
            terminalWeaponBaseDiversity = terminalWeaponCounts.keys.size,
            crossProfessionTopWeaponDominance =
                if (reports.isEmpty()) {
                    0.0
                } else {
                    (topWeapon?.value ?: 0).toDouble() / reports.size.toDouble()
                },
            professionAlignedWeaponAdoptionRate =
                if (reports.isEmpty()) {
                    0.0
                } else {
                    alignedCount.toDouble() / reports.size.toDouble()
                },
            alignedFullRouteSampleCount = alignedCount,
            topWeaponBaseId = topWeapon?.key,
            topWeaponCount = topWeapon?.value ?: 0,
            professionTerminalWeaponDistribution = distribution,
            professionTopWeaponBaseIds = professionTopWeaponBaseIds,
            professionTopWeaponSemanticTags = professionTopWeaponSemanticTags,
        )
    }

    private fun aggregateZoneTraversalDiagnostics(
        reports: List<ScenarioReport>,
    ): Map<String, ZoneTraversalDiagnosticAggregate> =
        reports
            .flatMap(ScenarioReport::zoneTraversalDiagnostics)
            .groupBy(ZoneTraversalDiagnostic::zoneId)
            .toSortedMap(compareBy(::zoneDepth).thenBy { it })
            .mapValues { (_, diagnostics) ->
                val objectiveAcquireTurns = diagnostics.mapNotNull(ZoneTraversalDiagnostic::objectiveAcquireTurn)
                val objectiveAcquireHeadlessTurns = diagnostics.mapNotNull(ZoneTraversalDiagnostic::objectiveAcquireHeadlessTurnEquivalent)
                ZoneTraversalDiagnosticAggregate(
                    sampleCount = diagnostics.size,
                    visitCount = diagnostics.sumOf(ZoneTraversalDiagnostic::visitCount),
                    avgPlayerTurns = diagnostics.map(ZoneTraversalDiagnostic::playerTurns).average(),
                    avgEnemyTurns = diagnostics.map(ZoneTraversalDiagnostic::enemyTurns).average(),
                    avgEnemyTurnsPerPlayerTurn = diagnostics.map(ZoneTraversalDiagnostic::enemyTurnsPerPlayerTurn).average(),
                    avgVisibleHostileTurnCount = diagnostics.map(ZoneTraversalDiagnostic::visibleHostileTurnCount).average(),
                    avgLiveHostileWindow = diagnostics.map(ZoneTraversalDiagnostic::liveHostileWindow).average(),
                    maxVisibleHostiles = diagnostics.maxOfOrNull(ZoneTraversalDiagnostic::maxVisibleHostiles) ?: 0,
                    objectiveAcquireSampleCount = objectiveAcquireTurns.size,
                    avgObjectiveAcquireTurn =
                        objectiveAcquireTurns
                            .takeIf(List<Int>::isNotEmpty)
                            ?.average(),
                    avgObjectiveAcquireHeadlessTurnEquivalent =
                        objectiveAcquireHeadlessTurns
                            .takeIf(List<Int>::isNotEmpty)
                            ?.average(),
                    objectiveStateDistribution =
                        diagnostics
                            .mapNotNull(ZoneTraversalDiagnostic::objectiveStateAtExit)
                            .groupingBy { state -> state.name }
                            .eachCount()
                            .toSortedMap(),
                )
            }

    private fun criticalPathZoneIds(): List<String> =
        FOUNDATION_ZONE_ROUTE.drop(1).dropLast(1)

    private fun criticalPathZoneDesignAudit(): Map<String, CriticalPathZoneDesignAuditEntry> {
        val objectivesById = schemaCatalog.objectiveSets.associateBy { objective -> objective.id }
        val profilesByZone = schemaCatalog.zoneMapgenProfiles.associateBy { profile -> profile.zoneId }
        return criticalPathZoneIds()
            .map { zoneId ->
                val zone = requireNotNull(schemaCatalog.zones.firstOrNull { candidate -> candidate.id == zoneId }) {
                    "Missing critical-path zone '$zoneId' in schema catalog."
                }
                val objective = zone.objectiveSetId?.let(objectivesById::get)
                zoneId to
                    CriticalPathZoneDesignAuditEntry(
                        zoneId = zoneId,
                        floorCount = zone.floorCount,
                        mapSize = "${zone.mapSize.width}x${zone.mapSize.height}",
                        worldRole = zone.worldRole,
                        specialMechanics = zone.specialMechanics,
                        mechanicsWithoutDedicatedRuntimeHook =
                            zone.specialMechanics.filterNot { mechanic ->
                                mechanic in DEDICATED_RUNTIME_MECHANICS || mechanic in STATIC_FRONTSTAGE_MECHANICS
                            },
                        monsterPoolCount = zone.monsterPools.size,
                        elitePoolCount = zone.elitePools.size,
                        bossEncounterId = zone.bossEncounterId,
                        objectiveSetId = zone.objectiveSetId,
                        objectiveCompletionRule = objective?.completionRule,
                        objectivePlacements =
                            objective
                                ?.placements
                                ?.sortedWith(compareBy({ it.floor }, { it.interactableId }))
                                ?.map { placement ->
                                    "${placement.interactableId}@floor${placement.floor}:${placement.anchor}+${placement.offset.x},${placement.offset.y}"
                                }.orEmpty(),
                        terrainTagWeights =
                            profilesByZone[zoneId]
                                ?.terrainTagWeights
                                ?.entries
                                ?.sortedBy { entry -> entry.key.name }
                                ?.associate { entry -> entry.key.name to entry.value.toDouble() }
                                .orEmpty(),
                    )
            }.toMap(linkedMapOf())
    }

    private fun isProfessionAlignedTerminalWeapon(
        professionId: String,
        weaponBaseId: String,
    ): Boolean {
        val rule =
            requireNotNull(PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES[professionId]) {
                "Missing profession-aligned terminal weapon rule for '$professionId'."
            }
        val itemTags = itemTagsById[weaponBaseId].orEmpty()
        return rule.matches(weaponBaseId = weaponBaseId, itemTags = itemTags)
    }

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

    private data class TerminalWeaponIdentitySummary(
        val fullRouteSampleCount: Int,
        val terminalWeaponBaseDiversity: Int,
        val crossProfessionTopWeaponDominance: Double,
        val professionAlignedWeaponAdoptionRate: Double,
        val alignedFullRouteSampleCount: Int,
        val topWeaponBaseId: String?,
        val topWeaponCount: Int,
        val professionTerminalWeaponDistribution: Map<String, Map<String, Int>>,
        val professionTopWeaponBaseIds: Map<String, String>,
        val professionTopWeaponSemanticTags: Map<String, Set<String>>,
    )

    private data class ZoneTraversalDiagnosticAggregate(
        val sampleCount: Int,
        val visitCount: Int,
        val avgPlayerTurns: Double,
        val avgEnemyTurns: Double,
        val avgEnemyTurnsPerPlayerTurn: Double,
        val avgVisibleHostileTurnCount: Double,
        val avgLiveHostileWindow: Double,
        val maxVisibleHostiles: Int,
        val objectiveAcquireSampleCount: Int,
        val avgObjectiveAcquireTurn: Double?,
        val avgObjectiveAcquireHeadlessTurnEquivalent: Double?,
        val objectiveStateDistribution: Map<String, Int>,
    )

    private data class CriticalPathZoneDesignAuditEntry(
        val zoneId: String,
        val floorCount: Int,
        val mapSize: String,
        val worldRole: String,
        val specialMechanics: List<String>,
        val mechanicsWithoutDedicatedRuntimeHook: List<String>,
        val monsterPoolCount: Int,
        val elitePoolCount: Int,
        val bossEncounterId: String?,
        val objectiveSetId: String?,
        val objectiveCompletionRule: String?,
        val objectivePlacements: List<String>,
        val terrainTagWeights: Map<String, Double>,
    )

    private data class ProfessionAlignedTerminalWeaponRule(
        val exactWeaponBaseIds: Set<String> = emptySet(),
        val requiredItemTags: Set<String> = emptySet(),
    ) {
        fun matches(
            weaponBaseId: String,
            itemTags: Set<String>,
        ): Boolean = weaponBaseId in exactWeaponBaseIds || itemTags.any(requiredItemTags::contains)
    }

    private companion object {
        private val schemaCatalog = DataLoader().loadSchemaCatalog()
        private val DEDICATED_RUNTIME_MECHANICS: Set<String> =
            setOf(
                "patrol_pressure",
                "ambush_lane",
                "furnace_pressure",
                "currents",
                "crystal_shards",
                "abyssal_ward",
                "finale",
            )
        private val STATIC_FRONTSTAGE_MECHANICS: Set<String> =
            setOf(
                "line_of_sight",
            )
        private val PROFESSION_ALIGNED_TERMINAL_WEAPON_RULES: Map<String, ProfessionAlignedTerminalWeaponRule> =
            mapOf(
                "vanguard" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("battle_axe", "long_sword"),
                        requiredItemTags = setOf("frontline", "guard", "shield", "discipline"),
                    ),
                "templar" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("battle_axe", "long_sword"),
                        requiredItemTags = setOf("holy", "frontline", "guard", "shield"),
                    ),
                "rogue" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("short_sword", "hunter_bow"),
                        requiredItemTags = setOf("precision", "mobility", "finesse", "ranged", "marked"),
                    ),
                "arcanist" to
                    ProfessionAlignedTerminalWeaponRule(
                        exactWeaponBaseIds = setOf("arcane_staff"),
                        requiredItemTags = setOf("arcane", "spell", "ranged", "control"),
                    ),
            )
        private val itemTagsById: Map<String, Set<String>> =
            schemaCatalog.itemBundle.items.associate { item -> item.id to item.tags.toSet() }
        private val itemSemanticTagsById: Map<String, Set<String>> =
            DataLoader().loadItemBundle().baseItems.associate { item -> item.id to item.tags.toSet() }
    }

    private fun sampleScenarioReport(
        name: String,
        professionId: String,
        terminalWeaponBaseId: String?,
        zoneTraversalDiagnostics: List<ZoneTraversalDiagnostic> = emptyList(),
    ): ScenarioReport =
        ScenarioReport(
            name = name,
            seed = 1L,
            professionId = professionId,
            success = true,
            outcome = RunOutcome.Victory(floor = 1),
            floorReached = 1,
            turns = 10,
            goalReached = true,
            terminalWeaponBaseId = terminalWeaponBaseId,
            zoneTraversalDiagnostics = zoneTraversalDiagnostics,
        )
}
