package com.ktome.tools.verification

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

        assertEquals(setOf("content-pack", "hidden", "loot"), impactedDomainIds)
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4ReportOnly"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:lootBalanceLab"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:contentPackHarness"))
    }

    @Test
    fun `core change expands to all current phase4 owner domains without phase report routing`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("core/src/main/kotlin/com/ktome/core/map/MapGrid.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("boss", "content-pack", "hidden", "longrun", "loot", "mapgen", "solvability", "terrain"), impactedDomainIds)
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4ReportOnly"))
    }

    @Test
    fun `headless harness false negative expands to phase4 game harness producers`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("boss", "longrun", "terrain"), impactedDomainIds)
        assertTrue(plan.requestedTaskPaths.contains(":game:bossHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"))
        assertTrue(plan.requestedTaskPaths.contains(":game:terrainInteractionBatch"))
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
        assertTrue(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
    }

    @Test
    fun `mapgen runner change routes to mapgen owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/MapgenSmokeRunner.kt"))

        assertEquals(setOf("mapgen"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:mapgenSmoke"))
    }

    @Test
    fun `solvability runner change routes to solvability owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/SolvabilityHarnessRunner.kt"))

        assertEquals(setOf("solvability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:solvabilityHarness"))
    }

    @Test
    fun `terrain harness change routes to terrain owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt"))

        assertEquals(setOf("terrain"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":game:terrainInteractionBatch"))
    }

    @Test
    fun `boss harness change routes to boss owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt"))

        assertEquals(setOf("boss"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":game:bossHarness"))
    }

    @Test
    fun `long run harness change routes to longrun owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/test/kotlin/com/ktome/game/harness/LongRunLabSeedBank.kt"))

        assertEquals(setOf("longrun"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"))
    }
}
