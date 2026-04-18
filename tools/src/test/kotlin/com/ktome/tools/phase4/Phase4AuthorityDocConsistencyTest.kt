package com.ktome.tools.phase4

import com.ktome.game.loot.foundationBuildIdentityByProfessionId
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Phase4AuthorityDocConsistencyTest {
    @Test
    fun `phase4 authority docs point phase4Report at the canonical unified aggregate`() {
        val whiteBoxFramework =
            Files.readString(
                repoRoot().resolve("docs/2026-04-04-unified-white-box-verification-framework.md"),
            )
        val phase4Guide =
            Files.readString(
                repoRoot().resolve("docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md"),
            )
        val phase4Roadmap =
            Files.readString(
                repoRoot().resolve("docs/phase4/roadmap.md"),
            )
        val phase4Checklist =
            Files.readString(
                repoRoot().resolve("docs/phase4/2026-03-13-phase4-verification-checklist.md"),
            )
        val v2optPr01 =
            Files.readString(
                repoRoot().resolve("docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-01-experience-gate-and-owner-metrics.md"),
            )
        val buildIdentityHardeningPlan =
            Files.readString(
                repoRoot().resolve("docs/opt/2026-04-17-phase4-build-identity-end-to-end-hardening-and-fast-fail-plan.md"),
            )
        val terminalBuildBaseline =
            Json.parseToJsonElement(
                Files.readString(
                    repoRoot().resolve("docs/review/phase4/opt/baselines/2026-04-12-phase4-terminal-build-identity-baseline.json"),
                ),
            ).jsonObject
        val lootBaseline =
            Json.parseToJsonElement(
                Files.readString(
                    repoRoot().resolve("docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json"),
                ),
            ).jsonObject
        val terminalRanges =
            terminalBuildBaseline.getValue("expectedMetricRanges").jsonArray.associateBy { range ->
                range.jsonObject.getValue("metricId").jsonPrimitive.content
            }
        val lootRanges =
            lootBaseline.getValue("expectedMetricRanges").jsonArray.associateBy { range ->
                range.jsonObject.getValue("metricId").jsonPrimitive.content
            }

        assertTrue(whiteBoxFramework.contains("tools/build/reports/verification/phase4/report-phase4-summary.{json,md}"))
        assertTrue(whiteBoxFramework.contains("phase4LegacyReport"))
        assertTrue(phase4Guide.contains("tools/build/reports/verification/phase4/report-phase4-summary.{json,md}"))
        assertTrue(phase4Guide.contains("phase4LegacyReport"))
        assertTrue(phase4Roadmap.contains("critical-path pacing"))
        assertTrue(phase4Roadmap.contains("profession capstone"))
        assertTrue(phase4Checklist.contains("critical-path pacing"))
        assertTrue(phase4Checklist.contains("professionCapstoneAdoptionRate"))
        assertTrue(phase4Checklist.contains("professionCapstoneBreakdown"))
        assertTrue(phase4Checklist.contains("specialTierPassiveFamilyDuplicateCount"))
        assertTrue(phase4Checklist.contains("specialTierPassiveFamilyDuplicateSummary"))
        assertTrue(phase4Checklist.contains("professionCapstoneSourceCoverage.reportOnly"))
        assertTrue(phase4Checklist.contains("build-identity-debug.json"))
        assertTrue(phase4Checklist.contains("dynamicPoolTargetProfiles"))
        assertTrue(v2optPr01.contains("critical path pacing"))
        assertTrue(buildIdentityHardeningPlan.contains("data/build-identity/index.yaml"))
        assertTrue(buildIdentityHardeningPlan.contains("reportOnlyFloors"))
        assertEquals(setOf("arcanist", "rogue", "templar", "vanguard"), foundationBuildIdentityByProfessionId.keys)
        assertTrue(foundationBuildIdentityByProfessionId.values.all { identity -> identity.reportOnlyFloors.adoptionMinCount > 0 })
        assertTrue(foundationBuildIdentityByProfessionId.values.all { identity -> identity.reportOnlyFloors.nonWeaponMinCount > 0 })
        assertTrue(terminalRanges.containsKey("professionCapstoneAdoptionRate"))
        assertTrue(
            terminalRanges.getValue("professionCapstoneSeenRate").jsonObject
                .getValue("metadata")
                .jsonObject["perProfessionSeenMinCount"]
                ?.jsonPrimitive
                ?.content
                ?.toInt() ?: 0 > 0,
        )
        assertTrue(
            terminalRanges.getValue("nonWeaponBuildPayoffRate").jsonObject["minValue"]?.jsonPrimitive?.content?.toDouble() ?: 0.0 > 0.0,
        )
        assertTrue(lootRanges.containsKey("specialTierPassiveFamilyDuplicateCount"))
    }

    private fun repoRoot(): Path =
        System.getProperty("ktome.repo.root")
            ?.let(Path::of)
            ?: Path.of("").toAbsolutePath().normalize()
}
