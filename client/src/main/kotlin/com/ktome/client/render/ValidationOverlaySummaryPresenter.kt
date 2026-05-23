package com.ktome.client.render

import com.ktome.client.bossVariantModeLabelKey
import com.ktome.client.input.ValidationOverlayPanelState
import com.ktome.core.snapshot.RenderTextArgumentSnapshot
import com.ktome.core.snapshot.RenderTextTokenSnapshot
import com.ktome.game.contentpack.ContentPackVisibilityComparisonSummary
import com.ktome.game.contentpack.ContentPackVisibilityStateSummary
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.Localizer
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal enum class ValidationOverlayDisplayMode {
    COMPACT,
    DETAILED,
}

internal object ValidationOverlaySummaryPresenter {
    private const val MAX_VISUAL_COLUMNS = 96
    private const val CJK_MAX_CHARS = 144
    private const val DETAILED_MAX_ROWS = 32

    fun present(
        localizer: Localizer,
        panel: ValidationOverlayPanelState,
        displayMode: ValidationOverlayDisplayMode = ValidationOverlayDisplayMode.COMPACT,
        visibleOverlayRows: Int = 48,
    ): List<TileTextRow> {
        val rows = buildRows(localizer, panel, displayMode).map { row -> row.copy(text = clampLine(localizer, row.text)) }
        val maxRows =
            when (displayMode) {
                ValidationOverlayDisplayMode.COMPACT -> max(6, min(12, floor(visibleOverlayRows * 0.25).toInt()))
                ValidationOverlayDisplayMode.DETAILED -> DETAILED_MAX_ROWS
            }
        if (rows.size <= maxRows) {
            return rows
        }
        val visibleRows = rows.take(maxRows - 1)
        val hiddenCount = rows.size - visibleRows.size
        return visibleRows +
            TileTextRow(
                text = localizer.text("ui.validation.fold.more", "count" to hiddenCount),
                tone = TileTextTone.LIGHT_GRAY,
            )
    }

    fun compactPath(value: String): String {
        val normalized = value.replace('\\', '/').trim()
        if (normalized.startsWith("/Users/") || normalized.startsWith("/tmp/") || Regex("^[A-Za-z]:/").containsMatchIn(normalized)) {
            return "<machine-path-redacted>/${normalized.substringAfterLast('/')}"
        }
        return normalized
            .removePrefix("./")
            .let { path ->
                if (visualColumns(path) <= MAX_VISUAL_COLUMNS) {
                    path
                } else {
                    keepTail(path)
                }
            }
    }

    private fun buildRows(
        localizer: Localizer,
        panel: ValidationOverlayPanelState,
        displayMode: ValidationOverlayDisplayMode,
    ): List<TileTextRow> {
        val summary = panel.summary
        val warningRows =
            listOf(
                TileTextRow(
                    ValidationPackSummaryText.keyWarnings(localizer, summary.packKeyResolutionSummary),
                    TileTextTone.WHITE,
                ),
            )
        val evidenceRows =
            summary.scenarioEvidenceSummary?.let { evidenceSummary ->
                listOf(TileTextRow(localizer.text("ui.validation.phase4.v4.evidence_summary"), TileTextTone.GOLD)) +
                    validationScenarioEvidenceSummaryLines(localizer, evidenceSummary).map { line ->
                        TileTextRow("  $line", TileTextTone.LIGHT_GRAY)
                    }
            }.orEmpty()
        val primaryRows =
            listOf(
                TileTextRow(localizer.text("ui.validation.overlay.summary"), TileTextTone.GOLD),
                TileTextRow(localizer.text("ui.validation.entry.preset", "value" to localizer.text(summary.preset.titleKey)), TileTextTone.WHITE),
                TileTextRow(localizer.text("ui.validation.entry.seed", "value" to summary.seed), TileTextTone.WHITE),
            ) +
                if (summary.seedCorpus.size > 1) {
                    listOf(
                        TileTextRow(
                            localizer.text(
                                "ui.validation.entry.seed_corpus",
                                "value" to summary.seedCorpus.sorted().joinToString(", "),
                            ),
                            TileTextTone.WHITE,
                        ),
                    )
                } else {
                    emptyList()
                } +
                listOf(
                    TileTextRow(localizer.text("ui.validation.entry.zone", "value" to localizer.text(panel.zoneNameKey)), TileTextTone.WHITE),
                    TileTextRow(localizer.text("ui.validation.entry.floor", "value" to summary.floor), TileTextTone.WHITE),
                    TileTextRow(
                        localizer.text(
                            "ui.validation.active_packs",
                            "value" to ValidationPackSummaryText.activePackIds(localizer, summary.activePackIds),
                        ),
                        TileTextTone.WHITE,
                    ),
                    TileTextRow(
                        localizer.text(
                            "ui.validation.pack.namespaces",
                            "value" to ValidationPackSummaryText.namespaces(localizer, summary.activePackSummaries),
                        ),
                        TileTextTone.WHITE,
                    ),
                    TileTextRow(
                        localizer.text(
                            "ui.validation.pack.overlay_ops",
                            "value" to ValidationPackSummaryText.overlayOps(localizer, summary.activePackSummaries),
                        ),
                        TileTextTone.WHITE,
                    ),
                    TileTextRow(
                        localizer.text(
                            "ui.validation.pack.touched_ids",
                            "value" to ValidationPackSummaryText.touchedContentIds(localizer, summary.touchedContentIds),
                        ),
                        TileTextTone.WHITE,
                    ),
                    TileTextRow(
                        localizer.text(
                            "ui.validation.pack.key_resolution",
                            "visual" to summary.packKeyResolutionSummary.resolvedVisualKeys,
                            "audio" to summary.packKeyResolutionSummary.resolvedAudioKeys,
                            "locale" to summary.packKeyResolutionSummary.resolvedLocaleKeys,
                            "overrides" to summary.packKeyResolutionSummary.overriddenKeys,
                            "warnings" to summary.packKeyResolutionSummary.warningCount,
                        ),
                        TileTextTone.WHITE,
                    ),
                ) +
                packVisibilityComparisonRows(localizer, summary.packVisibilityComparison) +
                listOf(
                    TileTextRow(
                        localizer.text(
                            "ui.validation.entry.boss_variant_mode",
                            "value" to localizer.text(bossVariantModeLabelKey(summary.bossVariantModeId)),
                        ),
                        TileTextTone.WHITE,
                    ),
                    TileTextRow(
                        localizer.text(
                            "ui.validation.entry.preferred_variant",
                            "value" to (summary.preferredBossVariantId ?: localizer.text("ui.validation.none")),
                        ),
                        TileTextTone.WHITE,
                    ),
                    TileTextRow(localizer.text("ui.inspect.cursor", "x" to panel.inspectCursor.x, "y" to panel.inspectCursor.y), TileTextTone.WHITE),
                    TileTextRow(
                        localizer.text(
                            "ui.validation.overlay.last_result",
                            "value" to (summary.lastResult?.let { token -> renderTextToken(localizer, token) } ?: localizer.text("ui.validation.none")),
                        ),
                        TileTextTone.LIGHT_GRAY,
                    ),
                )
        val guideRows =
            listOf(TileTextRow(localizer.text("ui.validation.phase4.targets"), TileTextTone.GOLD)) +
                panel.phase4Guide.targetLabelKeys.map { labelKey -> TileTextRow("  ${localizer.text(labelKey)}", TileTextTone.WHITE) } +
                listOf(TileTextRow(localizer.text("ui.validation.phase4.quick_paths"), TileTextTone.GOLD)) +
                panel.phase4Guide.quickPathLabelKeys.map { labelKey -> TileTextRow("  ${localizer.text(labelKey)}", TileTextTone.LIGHT_GRAY) } +
                listOf(TileTextRow(localizer.text("ui.validation.phase4.evidence"), TileTextTone.GOLD)) +
                panel.phase4Guide.evidenceLabelKeys.map { labelKey -> TileTextRow("  ${localizer.text(labelKey)}", TileTextTone.LIGHT_GRAY) } +
                panel.scenarioContext?.requiredEvidenceKeys.orEmpty().map { evidencePath ->
                    TileTextRow("  ${compactPath(evidencePath)}", TileTextTone.LIGHT_GRAY)
                }
        val controlRows =
            listOf(TileTextRow(localizer.text("ui.controls.validation"), TileTextTone.LIGHT_GRAY)) +
                panel.sections.flatMap { section ->
                    listOf(
                        TileTextRow(
                            text = localizer.text(section.titleKey),
                            tone = if (section.selected) TileTextTone.GOLD else TileTextTone.WHITE,
                            selected = section.selected,
                        ),
                    ) +
                        section.actions.map { action ->
                            TileTextRow(
                                text = "  ${localizer.text(action.labelKey)}",
                                tone = if (action.selected) TileTextTone.CYAN else TileTextTone.LIGHT_GRAY,
                                selected = action.selected,
                            )
                        }
                }
        return when (displayMode) {
            ValidationOverlayDisplayMode.COMPACT -> warningRows + evidenceRows + primaryRows + guideRows + controlRows
            ValidationOverlayDisplayMode.DETAILED -> evidenceRows + warningRows + primaryRows + guideRows + controlRows
        }
    }

    private fun packVisibilityComparisonRows(
        localizer: Localizer,
        comparison: ContentPackVisibilityComparisonSummary?,
    ): List<TileTextRow> {
        comparison ?: return emptyList()
        return listOf(
            TileTextRow(
                text = visibilityStateText(localizer, "ui.validation.pack.no_pack_state", comparison.noPackState),
                tone = TileTextTone.WHITE,
            ),
            TileTextRow(
                text = visibilityStateText(localizer, "ui.validation.pack.active_sample_state", comparison.activeSamplePackState),
                tone = TileTextTone.WHITE,
            ),
        )
    }

    private fun visibilityStateText(
        localizer: Localizer,
        key: String,
        state: ContentPackVisibilityStateSummary,
    ): String =
        localizer.text(
            key,
            "active" to ValidationPackSummaryText.activePackIds(localizer, state.activePackIds),
            "ops" to ValidationPackSummaryText.overlayOps(localizer, state.activePackSummaries),
            "touched" to ValidationPackSummaryText.touchedContentIds(localizer, state.touchedContentIds),
        )

    private fun renderTextToken(
        localizer: Localizer,
        token: RenderTextTokenSnapshot,
    ): String =
        localizer.text(
            token.key,
            *token.arguments.map { argument -> argument.name to resolveArgument(localizer, argument) }.toTypedArray(),
        )

    private fun resolveArgument(
        localizer: Localizer,
        argument: RenderTextArgumentSnapshot,
    ): String =
        argument.value
            ?: argument.valueKey?.let(localizer::text)
            ?: argument.valueToken?.let { token -> renderTextToken(localizer, token) }
            ?: ""

    private fun clampLine(
        localizer: Localizer,
        value: String,
    ): String {
        val limit = if (localizer.locale == GameLocale.ZH_CN) CJK_MAX_CHARS else MAX_VISUAL_COLUMNS
        if (visualColumns(value) <= limit) {
            return value
        }
        return keepHead(value, limit)
    }

    private fun keepHead(
        value: String,
        limit: Int,
    ): String {
        val marker = "..."
        var head = value
        while (head.isNotEmpty() && visualColumns(head + marker) > limit) {
            head = head.dropLast(1)
        }
        return head + marker
    }

    private fun keepTail(
        value: String,
        limit: Int = MAX_VISUAL_COLUMNS,
    ): String {
        val marker = "..."
        if (visualColumns(marker + value) <= limit) {
            return marker + value
        }
        val segments = value.split('/').filter(String::isNotBlank)
        if (segments.size > 1) {
            for (index in segments.indices) {
                val candidate = marker + "/" + segments.drop(index).joinToString("/")
                if (visualColumns(candidate) <= limit) {
                    return candidate
                }
            }
            val lastSegment = segments.last()
            if (visualColumns(marker + lastSegment) <= limit) {
                return marker + lastSegment
            }
        }
        var tail = segments.lastOrNull() ?: value
        while (tail.isNotEmpty() && visualColumns(marker + tail) > limit) {
            tail = tail.drop(1)
        }
        return marker + tail
    }

    private fun visualColumns(value: String): Int =
        value.sumOf { char ->
            when (Character.getType(char)) {
                Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt(),
                Character.ENCLOSING_MARK.toInt(),
                -> 0
                else -> if (isWide(char)) 2 else 1
            }
        }

    private fun isWide(char: Char): Boolean =
        char.code in 0x1100..0x115F ||
            char.code in 0x2E80..0xA4CF ||
            char.code in 0xAC00..0xD7A3 ||
            char.code in 0xF900..0xFAFF ||
            char.code in 0xFE10..0xFE6F ||
            char.code in 0xFF00..0xFF60
}

internal fun validationEvidencePathForDisplay(value: String): String =
    ValidationOverlaySummaryPresenter.compactPath(value)
