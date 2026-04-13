package com.ktome.tools.phase4

import com.ktome.tools.verification.VerificationBaseline
import java.nio.file.Path

internal object Phase4OwnerBaselineRegistry {
    const val SCRIPTED_HIDDEN_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json"
    const val ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json"
    const val LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json"
    const val TERMINAL_BUILD_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-terminal-build-identity-baseline.json"
    const val TERRAIN_UNIFIED_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json"
    const val TERRAIN_PER_ZONE_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-terrain-per-zone-lower-bound-baseline.json"

    private val baselinePathsByTaskId: Map<String, List<String>> =
        mapOf(
            "hiddenContentHarness" to listOf(SCRIPTED_HIDDEN_BASELINE_RELATIVE_PATH),
            "organicHiddenProbe" to listOf(ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH),
            "whiteBoxLoot" to listOf(LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH),
            "longRunLab" to listOf(TERMINAL_BUILD_BASELINE_RELATIVE_PATH),
            "terrainInteractionBatch" to listOf(TERRAIN_UNIFIED_BASELINE_RELATIVE_PATH, TERRAIN_PER_ZONE_BASELINE_RELATIVE_PATH),
        )

    fun ownerBaselinePaths(taskId: String): List<String> =
        baselinePathsByTaskId[taskId].orEmpty()

    fun readOwnerBaselines(
        repoRoot: Path,
        taskId: String,
    ): Map<String, VerificationBaseline> =
        ownerBaselinePaths(taskId).associateWith { relativePath -> read(repoRoot, relativePath) }

    private fun read(
        repoRoot: Path,
        relativePath: String,
    ): VerificationBaseline = VerificationBaseline.read(repoRoot.resolve(relativePath))
}
