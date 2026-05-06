package com.ktome.game.validation

import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.contentpack.ContentPackKeyResolutionSummary
import com.ktome.game.contentpack.ContentPackOverlaySummary
import com.ktome.game.contentpack.ContentPackVisibilityComparisonSummary

data class ValidationSummarySnapshot(
    val preset: ValidationPreset,
    val seed: Long,
    val seedCorpus: List<Long>,
    val zoneId: String,
    val floor: Int,
    val activePackIds: List<String>,
    val activePackSummaries: List<ContentPackOverlaySummary> = emptyList(),
    val touchedContentIds: List<String> = emptyList(),
    val packKeyResolutionSummary: ContentPackKeyResolutionSummary = ContentPackKeyResolutionSummary(),
    val bossVariantModeId: String,
    val preferredBossVariantId: String?,
    val lastResult: RenderTextTokenSnapshot?,
    val scenarioId: ValidationScenarioId? = null,
    val scenarioEvidenceSummary: ValidationScenarioEvidenceSummary? = null,
    val packVisibilityComparison: ContentPackVisibilityComparisonSummary? = null,
)

fun ValidationSummarySnapshot.hasMeaningfulNextSeedRestart(): Boolean =
    seedCorpus.size > 1 && seed in seedCorpus
