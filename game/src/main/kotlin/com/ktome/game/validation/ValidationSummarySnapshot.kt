package com.ktome.game.validation

import com.ktome.core.snapshot.RenderTextTokenSnapshot

data class ValidationSummarySnapshot(
    val preset: ValidationPreset,
    val seed: Long,
    val seedCorpus: List<Long>,
    val zoneId: String,
    val floor: Int,
    val activePackIds: List<String>,
    val bossVariantModeId: String,
    val preferredBossVariantId: String?,
    val lastResult: RenderTextTokenSnapshot?,
)

fun ValidationSummarySnapshot.hasMeaningfulNextSeedRestart(): Boolean =
    seedCorpus.size > 1 && seed in seedCorpus
