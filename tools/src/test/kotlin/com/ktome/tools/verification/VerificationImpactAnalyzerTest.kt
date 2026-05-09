package com.ktome.tools.verification

import com.ktome.tools.phase4.Phase4OwnerBaselineRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class VerificationImpactAnalyzerTest {
    @Test
    fun `pr06 reward build affix and hidden data owner surfaces route to longrun owner lab`() {
        val changedFiles =
            listOf(
                "game/src/main/resources/data/loot/index.yaml",
                "game/src/main/resources/data/items/index.yaml",
                "game/src/main/resources/data/build-identity/index.yaml",
                "game/src/main/resources/data/secret-zones/index.yaml",
                "game/src/main/resources/data/events/index.yaml",
                "game/src/main/resources/data/mapgen/zones/index.yaml",
            )

        changedFiles.forEach { changedFile ->
            val plan = VerificationImpactAnalyzer.analyze(listOf(changedFile))
            val longrunImpact = plan.impactedDomains.single { impact -> impact.domainId == "longrun" }

            assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"), "changedFile=$changedFile plan=$plan")
            assertTrue(longrunImpact.reasons.any { reason -> reason.ownerRequired }, "changedFile=$changedFile impact=$longrunImpact")
        }

        val lootPlan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/resources/data/loot/index.yaml"))
        assertTrue(lootPlan.requestedPreflightTaskPaths.contains(":tools:verifyLootPreflight"))
    }

    @Test
    fun `data loader change expands to current phase4 content owner domains`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/data/DataLoader.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("content-pack", "hidden", "organic-hidden", "loot", "maintainability"), impactedDomainIds)
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4ReportOnly"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReportOnly"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:lootBalanceLab"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:organicHiddenProbe"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:contentPackHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxContentPack"))
        assertTrue(plan.requestedPreflightTaskPaths.contains(":tools:verifyLootPreflight"))
        assertTrue(plan.requestedPreflightTaskPaths.contains(":tools:verifyHiddenPreflight"))
        assertTrue(plan.requestedPreflightTaskPaths.contains(":tools:verifyContentPackPreflight"))
        assertFalse(plan.requestedPreflightTaskPaths.contains(":tools:lootBalanceLab"))
    }

    @Test
    fun `core change expands to all current phase4 owner domains without phase report routing`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("core/src/main/kotlin/com/ktome/core/map/MapGrid.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("boss", "content-pack", "hidden", "longrun", "loot", "maintainability", "mapgen", "organic-hidden", "solvability", "terrain"), impactedDomainIds)
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4ReportOnly"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReportOnly"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
    }

    @Test
    fun `presentation-only item snapshot changes do not expand into phase4 owner domains`() {
        val changedFiles =
            listOf(
                VerificationImpactHints.RENDER_SNAPSHOT_PATH,
                VerificationImpactHints.FOUNDATION_GAME_SESSION_PATH,
            )
        val plan =
            VerificationImpactAnalyzer.analyze(
                changedFiles = changedFiles,
                impactHints = VerificationImpactHints(presentationOnlySnapshotFiles = changedFiles.toSet()),
            )

        assertEquals(setOf("maintainability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedTaskPaths)
        assertFalse(plan.requestedTaskPaths.contains(":tools:lootBalanceLab"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:whiteBoxLoot"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:terrainInteractionBatch"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:bossHarness"))
        assertFalse(plan.requestedTaskPaths.contains(":game:longRunLab"))
    }

    @Test
    fun `headless harness false negative expands to phase4 game harness producers`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt"))
        val impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet()

        assertEquals(setOf("boss", "longrun", "maintainability", "terrain"), impactedDomainIds)
        assertTrue(plan.requestedTaskPaths.contains(":tools:bossHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:terrainInteractionBatch"))
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedPreflightTaskPaths)
    }

    @Test
    fun `route hash changes route to long run owner evidence`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/RouteHash.kt"))

        assertTrue(plan.impactedDomains.any { impact -> impact.domainId == "longrun" })
        assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"))
    }

    @Test
    fun `talent sidebar presentation changes do not route to long run owner evidence`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("client/src/main/kotlin/com/ktome/client/ui/talent/TalentSidebarPresenter.kt"))

        assertFalse(plan.impactedDomains.any { impact -> impact.domainId == "longrun" })
        assertFalse(plan.requestedTaskPaths.contains(":game:longRunLab"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
    }

    @Test
    fun `client renderer changes route to client smoke and golden evidence`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt"))

        assertEquals(setOf("client-ui-evidence", "maintainability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":client:clientSmoke"))
        assertTrue(plan.requestedTaskPaths.contains(":client:goldenScreenshot"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedPreflightTaskPaths)
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
        assertTrue(plan.requestedPreflightTaskPaths.contains(":tools:verifyHiddenPreflight"))
        assertFalse(plan.requestedPreflightTaskPaths.contains(":tools:hiddenContentHarness"))
    }

    @Test
    fun `mapgen white box runner change routes to mapgen owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxMapgenRunner.kt"))

        assertEquals(setOf("maintainability", "mapgen"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxMapgen"))
    }

    @Test
    fun `mapgen smoke runner change still routes to mapgen owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/MapgenSmokeRunner.kt"))

        assertEquals(setOf("maintainability", "mapgen"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxMapgen"))
    }

    @Test
    fun `solvability white box runner change routes to solvability owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/WhiteBoxSolvabilityRunner.kt"))

        assertEquals(setOf("maintainability", "solvability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:whiteBoxSolvability"))
    }

    @Test
    fun `solvability proof runner change still routes to solvability owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/SolvabilityHarnessRunner.kt"))

        assertEquals(setOf("maintainability", "solvability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
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
    fun `boss owner baseline change routes to report only aggregation rebuild`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf(Phase4OwnerBaselineRegistry.BOSS_PHASE_IDENTITY_BASELINE_RELATIVE_PATH))

        assertEquals(setOf("boss"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:reportPhase4Only"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:bossHarness"))
        assertEquals(listOf(":tools:scopeCoverageLint"), plan.requestedPreflightTaskPaths)
    }

    @Test
    fun `organic hidden runtime change routes to organic hidden owner task`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt"))

        assertEquals(setOf("maintainability", "organic-hidden"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:organicHiddenProbe"))
        assertTrue(plan.requestedTaskPaths.contains(":tools:maintainabilityLint"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedPreflightTaskPaths)
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
        assertEquals(listOf(":tools:scopeCoverageLint"), plan.requestedPreflightTaskPaths)
    }

    @Test
    fun `organic hidden owner baseline change routes to report only aggregation rebuild`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf(Phase4OwnerBaselineRegistry.ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH))

        assertEquals(setOf("organic-hidden"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:reportPhase4Only"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:organicHiddenProbe"))
        assertEquals(listOf(":tools:scopeCoverageLint"), plan.requestedPreflightTaskPaths)
    }

    @Test
    fun `phase4 aggregation code change routes to report only rebuild for migrated owner domains`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt"))

        assertEquals(setOf("boss", "hidden", "longrun", "loot", "maintainability", "organic-hidden", "terrain"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertEquals(setOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint", ":tools:reportPhase4Only"), plan.requestedTaskPaths.toSet())
        assertEquals(setOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedPreflightTaskPaths.toSet())
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReport"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:phase4LegacyReportOnly"))
    }

    @Test
    fun `phase4 shared report helper change routes to report only rebuild for migrated owner domains`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("tools/src/main/kotlin/com/ktome/tools/phase4/Phase4CriticalPathPacing.kt"))

        assertEquals(setOf("boss", "hidden", "longrun", "loot", "maintainability", "organic-hidden", "terrain"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertEquals(setOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint", ":tools:reportPhase4Only"), plan.requestedTaskPaths.toSet())
        assertEquals(setOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedPreflightTaskPaths.toSet())
    }

    @Test
    fun `governance doc change routes to maintainability lint only`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("docs/rule/ai-change-governance.md"))

        assertEquals(setOf("maintainability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedTaskPaths)
    }

    @Test
    fun `maintainability baseline change routes to maintainability lint only`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("maintainability-baseline.json"))

        assertEquals(setOf("maintainability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedTaskPaths)
    }

    @Test
    fun `generic governance docs outside lint inputs do not route to maintainability lint`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("AGENTS.md"))

        assertTrue(plan.impactedDomains.isEmpty())
        assertEquals(listOf(":tools:scopeCoverageLint"), plan.requestedTaskPaths)
        assertEquals(listOf(":tools:scopeCoverageLint"), plan.requestedPreflightTaskPaths)
    }

    @Test
    fun `dark ui sprite sheet and manifest changes route to pr00 dry run dark gates`() {
        val changedFiles =
            listOf(
                "UI/sprite-sheets/sheet-plan.yaml",
                "UI/sprite-sheets/key-registry.yaml",
                "assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png",
                "assets-src/image/contact-sheets/dark-v1/r01-ui-chrome-contact.png",
                "client/src/main/resources/dark-v1/ui/action_attack.png",
                "assets-src/image/manifests/phase2-visual-manifest.json",
                "client/src/main/resources/manifests/visual-manifest.json",
                "assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl",
                "scripts/asset_pipeline_common.py",
                "scripts/codex-generate-image.py",
                "scripts/manifest-lint.py",
            )

        changedFiles.forEach { changedFile ->
            val plan = VerificationImpactAnalyzer.analyze(listOf(changedFile))

            assertEquals(
                setOf("dark-uiux-pipeline"),
                plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet(),
                "changedFile=$changedFile plan=$plan",
            )
            assertTrue(plan.requestedTaskPaths.contains(":tools:darkKeyRegistryLint"), "changedFile=$changedFile plan=$plan")
            assertTrue(plan.requestedTaskPaths.contains(":tools:darkSpriteSheetLint"), "changedFile=$changedFile plan=$plan")
            assertTrue(plan.requestedTaskPaths.contains(":tools:spriteSheetMapLint"), "changedFile=$changedFile plan=$plan")
            assertTrue(plan.requestedTaskPaths.contains(":tools:darkManifestCoveragePr00DryRun"), "changedFile=$changedFile plan=$plan")
            assertFalse(plan.requestedTaskPaths.contains(":tools:darkManifestCoverageLint"), "changedFile=$changedFile plan=$plan")
            assertEquals(listOf(":tools:scopeCoverageLint"), plan.requestedPreflightTaskPaths, "changedFile=$changedFile plan=$plan")
        }
    }

    @Test
    fun `foundation session false negative no longer fans validation changes into loot and hidden owner domains`() {
        val plan = VerificationImpactAnalyzer.analyze(listOf("game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt"))

        assertEquals(setOf("boss", "longrun", "maintainability"), plan.impactedDomains.map(VerificationDomainImpact::domainId).toSet())
        assertTrue(plan.requestedTaskPaths.contains(":tools:bossHarness"))
        assertTrue(plan.requestedTaskPaths.contains(":game:longRunLab"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:lootBalanceLab"))
        assertFalse(plan.requestedTaskPaths.contains(":tools:hiddenContentHarness"))
        assertEquals(listOf(":tools:scopeCoverageLint", ":tools:maintainabilityLint"), plan.requestedPreflightTaskPaths)
    }
}
