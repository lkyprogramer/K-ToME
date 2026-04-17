package com.ktome.tools.verification

import java.nio.file.Files
import java.nio.file.Path
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ScopeCoverageLintCaseResult(
    val caseId: String,
    val changedFiles: List<String>,
    val impactedDomainIds: List<String>,
    val requestedTaskPaths: List<String>,
)

@Serializable
data class ScopeCoverageLintSummary(
    val caseCount: Int,
    val cases: List<ScopeCoverageLintCaseResult>,
)

data class ScopeCoverageLintRun(
    val caseCount: Int,
    val summaryPath: Path,
)

object ScopeCoverageLintRunner {
    private const val SUMMARY_FILE_NAME: String = "scope-coverage-lint-summary.json"
    private val json: Json = Json { prettyPrint = true }

    fun run(): ScopeCoverageLintRun {
        val outputDir = reportDir()
        Files.createDirectories(outputDir)
        val cases =
            listOf(
                planCase(
                    caseId = "loot_data_scope",
                    changedFiles = listOf("game/src/main/resources/data/loot/index.yaml"),
                ),
                planCase(
                    caseId = "hidden_data_scope",
                    changedFiles = listOf("game/src/main/resources/data/events/index.yaml"),
                ),
                planCase(
                    caseId = "content_pack_sample_scope",
                    changedFiles = listOf("examples/content-packs/sample.flooded_relics/manifest.yaml"),
                ),
                planCase(
                    caseId = "schema_i18n_scope",
                    changedFiles = listOf("game/src/main/resources/i18n/en-US.json"),
                ),
                planCase(
                    caseId = "maintainability_governance_scope",
                    changedFiles = listOf("docs/rule/ai-change-governance.md"),
                ),
                planCase(
                    caseId = "maintainability_baseline_scope",
                    changedFiles = listOf("maintainability-baseline.json"),
                ),
                planCase(
                    caseId = "core_phase4_owner_false_negative",
                    changedFiles = listOf("core/src/main/kotlin/com/ktome/core/map/MapGrid.kt"),
                ),
                planCase(
                    caseId = "data_loader_false_negative",
                    changedFiles = listOf("game/src/main/kotlin/com/ktome/game/data/DataLoader.kt"),
                ),
                planCase(
                    caseId = "foundation_session_false_negative",
                    changedFiles = listOf("game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt"),
                ),
                planCase(
                    caseId = "headless_harness_false_negative",
                    changedFiles = listOf("game/src/main/kotlin/com/ktome/game/harness/HeadlessRunHarness.kt"),
                ),
                planCase(
                    caseId = "mapgen_owner_scope",
                    changedFiles = listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/MapgenSmokeRunner.kt"),
                ),
                planCase(
                    caseId = "solvability_owner_scope",
                    changedFiles = listOf("tools/src/main/kotlin/com/ktome/tools/mapgen/SolvabilityHarnessRunner.kt"),
                ),
                planCase(
                    caseId = "terrain_owner_scope",
                    changedFiles = listOf("game/src/test/kotlin/com/ktome/game/harness/TerrainInteractionBatchTest.kt"),
                ),
                planCase(
                    caseId = "boss_owner_scope",
                    changedFiles = listOf("game/src/test/kotlin/com/ktome/game/harness/BossHarnessTest.kt"),
                ),
                planCase(
                    caseId = "organic_hidden_owner_scope",
                    changedFiles = listOf("tools/src/main/kotlin/com/ktome/tools/hidden/OrganicHiddenProbeRunner.kt"),
                ),
                planCase(
                    caseId = "longrun_owner_scope",
                    changedFiles = listOf("game/src/test/kotlin/com/ktome/game/harness/LongRunLabSeedBank.kt"),
                ),
                planCase(
                    caseId = "scripted_hidden_owner_baseline_scope",
                    changedFiles = listOf("docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json"),
                ),
                planCase(
                    caseId = "organic_hidden_owner_baseline_scope",
                    changedFiles = listOf("docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json"),
                ),
                planCase(
                    caseId = "loot_owner_baseline_scope",
                    changedFiles = listOf("docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json"),
                ),
                planCase(
                    caseId = "terrain_owner_baseline_scope",
                    changedFiles = listOf("docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json"),
                ),
                planCase(
                    caseId = "longrun_owner_baseline_scope",
                    changedFiles =
                        listOf(
                            "docs/review/phase4/opt/baselines/2026-04-12-phase4-terminal-build-identity-baseline.json",
                            "docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json",
                        ),
                ),
                planCase(
                    caseId = "phase4_report_only_scope",
                    changedFiles = listOf("tools/src/main/kotlin/com/ktome/tools/phase4/Phase4AggregationInputRunner.kt"),
                ),
                planCase(
                    caseId = "phase4_report_helper_scope",
                    changedFiles = listOf("tools/src/main/kotlin/com/ktome/tools/phase4/Phase4CriticalPathPacing.kt"),
                ),
            )
        val summary =
            ScopeCoverageLintSummary(
                caseCount = cases.size,
                cases = cases,
            )
        val summaryPath = outputDir.resolve(SUMMARY_FILE_NAME)
        Files.writeString(summaryPath, json.encodeToString(summary))
        return ScopeCoverageLintRun(
            caseCount = summary.caseCount,
            summaryPath = summaryPath,
        )
    }

    private fun planCase(
        caseId: String,
        changedFiles: List<String>,
    ): ScopeCoverageLintCaseResult {
        val plan = VerificationImpactAnalyzer.analyze(changedFiles)
        return ScopeCoverageLintCaseResult(
            caseId = caseId,
            changedFiles = plan.changedFiles,
            impactedDomainIds = plan.impactedDomains.map(VerificationDomainImpact::domainId),
            requestedTaskPaths = plan.requestedTaskPaths,
        )
    }

    private fun reportDir(): Path =
        Path.of(
            requireNotNull(System.getProperty("ktome.phase4.scopeCoverage.reportDir")) {
                "ktome.phase4.scopeCoverage.reportDir system property is required for scopeCoverageLint output."
            },
        )
}
