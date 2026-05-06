package com.ktome.game.contentpack

data class ContentPackOverlaySummary(
    val packId: String,
    val namespace: String,
    val opCounts: Map<String, Int>,
)

data class ContentPackKeyResolutionSummary(
    val resolvedVisualKeys: Int = 0,
    val resolvedAudioKeys: Int = 0,
    val resolvedLocaleKeys: Int = 0,
    val overriddenKeys: Int = 0,
    val warningVisualKeys: List<String> = emptyList(),
    val warningAudioKeys: List<String> = emptyList(),
    val warningLocaleKeys: List<String> = emptyList(),
) {
    val warningCount: Int
        get() = warningVisualKeys.size + warningAudioKeys.size + warningLocaleKeys.size
}

data class ContentPackVisibilityStateSummary(
    val activePackIds: List<String>,
    val activePackSummaries: List<ContentPackOverlaySummary> = emptyList(),
    val touchedContentIds: List<String> = emptyList(),
    val keyResolutionSummary: ContentPackKeyResolutionSummary = ContentPackKeyResolutionSummary(),
)

data class ContentPackVisibilityComparisonSummary(
    val noPackState: ContentPackVisibilityStateSummary = ContentPackVisibilityStateSummary(activePackIds = emptyList()),
    val activeSamplePackState: ContentPackVisibilityStateSummary,
)
