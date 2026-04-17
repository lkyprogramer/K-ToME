package com.ktome.tools.phase4

import com.ktome.tools.verification.VerificationBaseline
import java.io.File
import java.nio.file.Path

internal object Phase4OwnerBaselineRegistry {
    private const val OWNER_BASELINE_OVERRIDE_PROPERTY_PREFIX: String = "ktome.phase4.ownerBaselineOverride."

    const val SCRIPTED_HIDDEN_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-scripted-hidden-owner-baseline.json"
    const val ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-organic-hidden-owner-baseline.json"
    const val LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-loot-local-reward-identity-baseline.json"
    const val TERMINAL_BUILD_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-terminal-build-identity-baseline.json"
    const val CRITICAL_PATH_PACING_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-16-phase4-critical-path-pacing-owner-baseline.json"
    const val TERRAIN_UNIFIED_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json"
    const val TERRAIN_PER_ZONE_BASELINE_RELATIVE_PATH: String =
        "docs/review/phase4/opt/baselines/2026-04-12-phase4-terrain-per-zone-lower-bound-baseline.json"

    private val baselinePathsByTaskId: Map<String, List<String>> =
        mapOf(
            "hiddenContentHarness" to listOf(SCRIPTED_HIDDEN_BASELINE_RELATIVE_PATH),
            "organicHiddenProbe" to listOf(ORGANIC_HIDDEN_BASELINE_RELATIVE_PATH),
            "whiteBoxLoot" to listOf(LOOT_LOCAL_REWARD_BASELINE_RELATIVE_PATH),
            "longRunLab" to listOf(TERMINAL_BUILD_BASELINE_RELATIVE_PATH, CRITICAL_PATH_PACING_BASELINE_RELATIVE_PATH),
            "terrainInteractionBatch" to listOf(TERRAIN_UNIFIED_BASELINE_RELATIVE_PATH, TERRAIN_PER_ZONE_BASELINE_RELATIVE_PATH),
        )

    fun registeredTaskIds(): Set<String> = baselinePathsByTaskId.keys

    fun ownerBaselinePaths(taskId: String): List<String> =
        resolveBaselinePaths(taskId = taskId, defaultPaths = baselinePathsByTaskId[taskId].orEmpty())

    fun scriptedHiddenBaselinePath(): String = ownerBaselinePaths("hiddenContentHarness").single()

    fun organicHiddenBaselinePath(): String = ownerBaselinePaths("organicHiddenProbe").single()

    fun lootBaselinePath(): String = ownerBaselinePaths("whiteBoxLoot").single()

    fun terminalBuildBaselinePath(): String = ownerBaselinePaths("longRunLab")[0]

    fun criticalPathPacingBaselinePath(): String = ownerBaselinePaths("longRunLab")[1]

    fun terrainUnifiedBaselinePath(): String = ownerBaselinePaths("terrainInteractionBatch")[0]

    fun terrainPerZoneBaselinePath(): String = ownerBaselinePaths("terrainInteractionBatch")[1]

    fun readOwnerBaselines(
        repoRoot: Path,
        taskId: String,
    ): Map<String, VerificationBaseline> =
        ownerBaselinePaths(taskId).associateWith { relativePath -> read(repoRoot, relativePath) }

    private fun resolveBaselinePaths(
        taskId: String,
        defaultPaths: List<String>,
    ): List<String> {
        val override =
            System.getProperty("$OWNER_BASELINE_OVERRIDE_PROPERTY_PREFIX$taskId")
                ?.split(File.pathSeparatorChar)
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                .orEmpty()
        if (override.isEmpty()) {
            return defaultPaths
        }
        require(override.size == defaultPaths.size) {
            "Baseline override for $taskId must provide ${defaultPaths.size} path(s), found ${override.size}."
        }
        return override
    }

    private fun read(
        repoRoot: Path,
        relativePath: String,
    ): VerificationBaseline = VerificationBaseline.read(repoRoot.resolve(relativePath))
}
