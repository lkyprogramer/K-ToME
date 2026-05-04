package com.ktome.tools.phase4

import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test

class ReportPhase4MaterializationTest {
    @Test
    @Tag("reportPhase4")
    fun `reportPhase4 materializes canonical aggregate into configured repo output dir`() {
        val compareLegacy = System.getProperty("ktome.phase4.aggregate.compareLegacy")?.toBooleanStrictOrNull() ?: false
        val outputDir = configuredAggregateReportDir()
        Files.createDirectories(outputDir)
        val staleComparisonPath = outputDir.resolve("report-phase4-legacy-comparison.json")
        if (!compareLegacy) {
            Files.writeString(staleComparisonPath, """{"stale":true}""")
        }
        val startedAt = Instant.now()

        val run = ReportPhase4Runner.run(compareLegacy = compareLegacy)

        assertEquals(outputDir.resolve("report-phase4-summary.json").toAbsolutePath().normalize(), run.summaryPath.toAbsolutePath().normalize())
        assertEquals(outputDir.resolve("report-phase4-summary.md").toAbsolutePath().normalize(), run.markdownPath.toAbsolutePath().normalize())
        assertTrue(Files.exists(run.summaryPath), "Expected canonical summary at ${run.summaryPath}")
        assertTrue(Files.exists(run.markdownPath), "Expected canonical markdown at ${run.markdownPath}")
        assertTrue(
            !Files.getLastModifiedTime(run.summaryPath).toInstant().isBefore(startedAt.minusSeconds(1)),
            "Expected ${run.summaryPath} to be refreshed during this task execution.",
        )

        val payload = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(run.summaryPath)).jsonObject
        val markdown = Files.readString(run.markdownPath)
        val debugArtifactPath = outputDir.resolve("build-identity-debug.json")
        val debugArtifact = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(debugArtifactPath)).jsonObject
        val ownerMetrics = payload.getValue("ownerMetrics").jsonArray
        val sections = payload.getValue("sections").jsonObject
        val sourceCoverageMetric =
            ownerMetrics.first { metric ->
                metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSourceCoverage.reportOnly"
            }.jsonObject

        assertEquals("report-phase4-v2", payload.getValue("schemaVersion").jsonPrimitive.content)
        assertEquals("P4", payload.getValue("phaseId").jsonPrimitive.content)
        assertEquals("14", payload.getValue("inputCount").jsonPrimitive.content)
        assertEquals("67", payload.getValue("ownerMetricCount").jsonPrimitive.content)
        assertEquals(67, ownerMetrics.size)
        assertTrue(sections.containsKey("criticalPathPacing"))
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgObjectiveAcquireTurn" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgVisibleHostileTurnCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "avgEnemyTurns" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "criticalPathCombatFloorSatisfied" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageHighPriorityCueRetainedRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageCueDedupAppliedCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageCueExpiryParity" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageSecretCueVisibilityRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "frontstageSearchCueVisibilityRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "zoneHookCoverage" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "topZoneLeadShare" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "zoneSearchPromptVisibility" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "dynamicPoolCoverage" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "specialTierPassiveFamilyDuplicateCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "topFiveAffixExposureShare" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSeenRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneSourceCoverage.reportOnly" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionFloor" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffFloor" })
        assertFalse(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "professionCapstoneAdoptionFloor.reportOnly" })
        assertFalse(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffFloor.reportOnly" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardAdoptionDelta" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "milestoneRewardSlotBalance.maxSlotShare" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "nonWeaponBuildPayoffRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "starterProfessionTalentMaxCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "learnedTalentChoiceEventRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "multiTreeInvestmentAboveThresholdRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "breakpointChoiceEventRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "talentTreePrimaryInvestmentDistribution" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "talentReserveSwapCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "rankBreakpointAdoptionByTalent" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "autoLearnedNonStarterTalentCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "starterInscriptionMaxCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "fullSlotInscriptionPurchaseBlockedWithoutReplacementCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionInstallOrReplaceRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionReplacementProbeSuccessCount" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "terminalInscriptionLoadoutDiversity" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionCategoryCountDistribution" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "shopInscriptionOfferConversionRate" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "inscriptionReplaceReasonDistribution" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "phaseTransitionObservedRatio" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "variantTraceDivergenceRatio" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "minVariantActionTraceDivergenceScore" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantBasePhaseCountMin" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantPhaseOverrideSchemaCoverage" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantPhaseOverrideRuntimeTriggerCoverage" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantPhaseOverrideTelegraphCoverage" })
        assertTrue(ownerMetrics.any { metric -> metric.jsonObject.getValue("metricId").jsonPrimitive.content == "bossVariantPhaseOverrideActionDistinctCount.reportOnly" })
        assertTrue(markdown.contains("## Critical Path Pacing"))
        assertTrue(markdown.contains("## Boss Phase Identity"))
        assertTrue(markdown.contains("## Solvability WhiteBox"))
        assertTrue(markdown.contains("headline owner metrics"))
        assertTrue(markdown.contains("single-task lane-aware artifact"))
        assertTrue(markdown.contains("### Critical Path Design Audit"))
        assertTrue(markdown.contains("criticalPathZoneIds"))
        assertTrue(markdown.contains("criticalPathCombatFloorSatisfied"))
        assertTrue(Files.exists(debugArtifactPath))
        assertTrue(debugArtifact.containsKey("rewardSourceSelections"))
        assertTrue(debugArtifact.containsKey("topRejectedCapstoneCandidates"))
        assertTrue(debugArtifact.containsKey("perProfessionSourceCoverage"))
        assertEquals(
            "${debugArtifact.getValue("perProfessionSourceCoverage").jsonArray.count { coverage -> coverage.jsonObject.getValue("covered").jsonPrimitive.content.toBooleanStrict() }}/${debugArtifact.getValue("perProfessionSourceCoverage").jsonArray.size}",
            sourceCoverageMetric.getValue("currentValueText").jsonPrimitive.content,
        )
        assertEquals(
            sourceCoverageMetric.getValue("currentValue").jsonObject.getValue("topRejectedCapstoneCandidates"),
            debugArtifact.getValue("topRejectedCapstoneCandidates"),
        )
        assertEquals(
            sourceCoverageMetric.getValue("currentValue").jsonObject.getValue("professionSourceCoverage"),
            debugArtifact.getValue("perProfessionSourceCoverage"),
        )

        if (compareLegacy) {
            assertNotNull(run.comparisonPath)
            val comparisonPath = requireNotNull(run.comparisonPath)
            assertTrue(Files.exists(comparisonPath), "Expected canonical parity report at $comparisonPath")
            val comparison = Phase4ReportFixtureTestSupport.json.parseToJsonElement(Files.readString(comparisonPath)).jsonObject
            assertEquals("0", comparison.getValue("mismatchCount").jsonPrimitive.content)
            assertEquals("67", comparison.getValue("metricCount").jsonPrimitive.content)
        } else {
            assertNull(run.comparisonPath)
            assertFalse(Files.exists(staleComparisonPath), "Canonical report-only run should remove stale legacy comparison artifacts.")
        }
    }

    private fun configuredAggregateReportDir(): Path {
        val configured = System.getProperty("ktome.phase4.aggregate.reportDir")
        return if (configured.isNullOrBlank()) {
            Path.of("tools", "build", "reports", "verification", "phase4")
        } else {
            Path.of(configured)
        }
    }
}
