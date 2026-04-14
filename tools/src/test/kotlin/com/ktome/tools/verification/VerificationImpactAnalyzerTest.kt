package com.ktome.tools.verification

import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationImpactAnalyzerTest {
    @Test
    fun `loot data change routes to loot preflight without forcing owner lab`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/resources/data/loot/index.yaml"))

        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:verifyLootPreflight"), plan.requestedTaskPaths)
        val lootImpact = plan.impactedDomains.single { impact -> impact.domainId == "loot" }
        assertTrue(lootImpact.reasons.all { reason -> !reason.ownerRequired })
    }

    @Test
    fun `data loader change expands to current phase4 content owner domains`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/data/DataLoader.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("content-pack", "hidden", "organic-hidden", "loot"), impactedDomainIds)
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4ReportOnly"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReportOnly"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:lootBalanceLab"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:organicHiddenProbe"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:contentPackHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxContentPack"))
    }

    @Test
    fun `core change expands to all current phase4 owner domains without phase report routing`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("core/src/main/kotlin/com/ktome/core/map/MapGrid.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("boss", "content-pack", "hidden", "longrun", "loot", "mapgen", "organic-hidden", "solvability", "terrain"), impactedDomainIds)
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4ReportOnly"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReportOnly"))
    }

    @Test
    fun `headless harness false negative expands to phase4 game harness producers`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("boss", "longrun", "terrain"), impactedDomainIds)
        assertTrue(plan.requestedTaskPaths.contains(":tools:bossHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:terrainInteractionBatch"))
    }

    @Test
    fun `schema and locale change routes through contract lint preflight and owner`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/resources/i18n/en-US.json"))

        assertTrue(plan.impactedDomains.any { impact -> impact.domainId == "contractLint" })
        assertTrue(plan.requestedTaskPaths.contains(":tools:verifyContractLintPreflight"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:contractLint"))
    }

    @Test
    fun `hidden validator change stays inside hidden runtime scope`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/Phase4StaticContentValidator.kt"))

        assertTrue(plan.impactedDomains.any { impact -> impact.domainId == "hidden" })
        assertFalse(plan.impactedDomains.any { impact -> impact.domainId == "organic-hidden" })
        assertTrue(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
    }

    @Test
    fun `mapgen white box runner change routes to mapgen owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxMapgenRunner.kt"))

        assertEquals(setOf("mapgen"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxMapgen"))
    }

    @Test
    fun `mapgen smoke runner change still routes to mapgen owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/MapgenSmokeRunner.kt"))

        assertEquals(setOf("mapgen"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxMapgen"))
    }

    @Test
    fun `solvability white box runner change routes to solvability owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxSolvabilityRunner.kt"))

        assertEquals(setOf("solvability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxSolvability"))
    }

    @Test
    fun `solvability proof runner change still routes to solvability owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/SolvabilityHarnessRunner.kt"))

        assertEquals(setOf("solvability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxSolvability"))
    }

    @Test
    fun `terrain harness change routes to terrain owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt"))

        assertEquals(setOf("terrain"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:terrainInteractionBatch"))
    }

    @Test
    fun `boss harness change routes to boss owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt"))

        assertEquals(setOf("boss"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:bossHarness"))
    }

    @Test
    fun `organic hidden runtime change routes to organic hidden owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt"))

        assertEquals(setOf("organic-hidden"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:organicHiddenProbe"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
    }

    @Test
    fun `long run harness change routes to longrun owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/test/kotlin/com/ktome/game/harness/LongRunLabSeedBank.kt"))

        assertEquals(setOf("longrun"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"))
    }

    @Test
    fun `loot owner baseline change routes to whitebox evaluation without rerunning loot kernel`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf(Phase4OwnerBaselineRegistry.LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH))

        assertEquals(setOf("loot"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:verifyLootPreflight"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxLoot"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:lootBalanceLab"))
    }

    @Test
    fun `longrun owner baseline change routes to report only aggregation rebuild`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf(Phase4OwnerBaselineRegistry.TERMINAL_BUILD_BASELINE_RELATIVE_PATH))

        assertEquals(setOf("longrun"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:reportPhase4Only"))
        assertFalse(plan.requestedTaskPaths.contains(":game:longRunLab"))
    }

    @Test
    fun `organic hidden owner baseline change routes to report only aggregation rebuild`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf(Phase4OwnerBaselineRegistry.ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH))

        assertEquals(setOf("organic-hidden"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:reportPhase4Only"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:organicHiddenProbe"))
    }

    @Test
    fun `phase4 aggregation code change routes to report only rebuild for migrated owner domains`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt"))

        assertEquals(setOf("hidden", "longrun", "loot", "organic-hidden", "terrain"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:reportPhase4Only"), plan.requestedTaskPaths)
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReportOnly"))
    }
}
