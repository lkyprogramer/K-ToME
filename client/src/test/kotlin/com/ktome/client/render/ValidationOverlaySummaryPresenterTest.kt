package com.ktome.client.render

import com.ktome.client.input.ValidationOverlayActionState
import com.ktome.client.input.ValidationOverlayPanelState
import com.ktome.client.input.ValidationOverlaySectionState
import com.ktome.core.map.Point
import com.ktome.game.contentpack.ContentPackKeyResolutionSummary
import com.ktome.game.contentpack.ContentPackOverlaySummary
import com.ktome.game.contentpack.ContentPackVisibilityComparisonSummary
import com.ktome.game.contentpack.ContentPackVisibilityStateSummary
import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.i18n.Localizer
import com.ktome.game.validation.ValidationPhase4Guide
import com.ktome.game.validation.ValidationPreset
import com.ktome.game.validation.ValidationScenarioEvidenceSummary
import com.ktome.game.validation.ValidationSummarySnapshot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationOverlaySummaryPresenterTest {
    @Test
    fun boundsRowsAndLineLength() {
        val rows =
            ValidationOverlaySummaryPresenter.present(
                localizer = localizer(),
                panel = panel(longLists = true),
                displayMode = ValidationOverlayDisplayMode.COMPACT,
                visibleOverlayRows = 48,
            )

        assertTrue(rows.size <= 12)
        assertTrue(rows.all { row -> row.text.length <= 100 }, rows.joinToString("\n") { it.text })
        assertTrue(rows.last().text.startsWith("+"), rows.joinToString("\n") { it.text })
    }

    @Test
    fun keepsEvidencePathsRepoRelative() {
        val redacted = ValidationOverlaySummaryPresenter.compactPath("/Users/local/build/private/evidence/manual.md")
        val compacted =
            ValidationOverlaySummaryPresenter.compactPath(
                "UI/manual-records/some/deep/path/that/is/intentionally/long/dark-uiux-pr06-validation-overlay-record.md",
            )

        assertTrue(redacted.startsWith("<machine-path-redacted>/"), redacted)
        assertTrue(redacted.endsWith("manual.md"), redacted)
        assertTrue(compacted.endsWith("dark-uiux-pr06-validation-overlay-record.md"), compacted)
        assertFalse(compacted.startsWith("/"), compacted)
    }

    @Test
    fun keepsCompactedPathsOnSegmentBoundaries() {
        val compacted =
            ValidationOverlaySummaryPresenter.compactPath(
                "UI/manual-records/中文目录/" +
                    (1..30).joinToString("/") { index -> "segment-$index" } +
                    "/dark-uiux-pr06-validation-overlay-record.md",
            )

        assertTrue(compacted.startsWith(".../"), compacted)
        assertTrue(compacted.endsWith("dark-uiux-pr06-validation-overlay-record.md"), compacted)
        assertFalse(compacted.startsWith("...文"), compacted)
    }

    @Test
    fun compactModeKeepsWarningsAndEvidenceBeforeFoldForEnglish() {
        val rows =
            ValidationOverlaySummaryPresenter.present(
                localizer = localizer(),
                panel = panel(longLists = true),
                displayMode = ValidationOverlayDisplayMode.COMPACT,
                visibleOverlayRows = 24,
            )

        assertCompactWarningAndEvidenceRows(rows, localizer())
    }

    @Test
    fun compactModeKeepsWarningsAndEvidenceBeforeFoldForChinese() {
        val rows =
            ValidationOverlaySummaryPresenter.present(
                localizer = localizer(GameLocale.ZH_CN),
                panel = panel(longLists = true),
                displayMode = ValidationOverlayDisplayMode.COMPACT,
                visibleOverlayRows = 24,
            )

        assertCompactWarningAndEvidenceRows(rows, localizer(GameLocale.ZH_CN))
    }

    @Test
    fun compactModePrioritizesSelectedValidationActions() {
        val rows =
            ValidationOverlaySummaryPresenter.present(
                localizer = localizer(),
                panel = panelWithSelectedActions(),
                displayMode = ValidationOverlayDisplayMode.COMPACT,
                visibleOverlayRows = 48,
            )
        val text = rows.joinToString("\n") { row -> row.text }

        assertEquals("Controls", rows[0].text, text)
        assertEquals("Fast actions", rows[1].text, text)
        assertTrue(rows.take(4).any { row -> row.text.contains("Prepare shop") }, text)
        assertTrue(rows.take(6).any { row -> row.text == "Evidence" }, text)
        assertTrue(rows.take(8).any { row -> row.text.contains("Root: UI/manual-records/root") }, text)
    }

    @Test
    fun usesColumnAwareBudgetForCjkText() {
        val rows =
            ValidationOverlaySummaryPresenter.present(
                localizer = localizer(GameLocale.ZH_CN),
                panel = panel(longLists = true),
                displayMode = ValidationOverlayDisplayMode.COMPACT,
                visibleOverlayRows = 48,
            )

        assertTrue(rows.all { row -> row.text.length <= 147 }, rows.joinToString("\n") { it.text })
    }

    @Test
    fun detailedModePreservesPackVisibilityComparisonRows() {
        val rows =
            ValidationOverlaySummaryPresenter.present(
                localizer = localizer(),
                panel = panel(longLists = false, includePackVisibilityComparison = true),
                displayMode = ValidationOverlayDisplayMode.DETAILED,
                visibleOverlayRows = 48,
            )
        val text = rows.joinToString("\n") { row -> row.text }

        assertTrue(text.contains("No-pack state: active=n/a, ops=empty, touched=empty"), text)
        assertTrue(
            rows.any { row ->
                row.text.startsWith("Active sample pack state: active=sample.flooded_relics, ops=sample.flooded_relics:ADD=5")
            },
            text,
        )
    }

    @Test
    fun compactPathKeepsCjkFileNameWhenDroppingDeepSegments() {
        val compacted =
            ValidationOverlaySummaryPresenter.compactPath(
                "UI/manual-records/" +
                    (1..30).joinToString("/") { index -> "segment-$index" } +
                    "/深色界面验证记录.md",
            )

        assertTrue(compacted.startsWith(".../"), compacted)
        assertTrue(compacted.endsWith("深色界面验证记录.md"), compacted)
        assertFalse(compacted.startsWith("...色"), compacted)
    }

    private fun panel(
        longLists: Boolean,
        includePackVisibilityComparison: Boolean = false,
    ): ValidationOverlayPanelState =
        ValidationOverlayPanelState(
            summary =
                ValidationSummarySnapshot(
                    preset = ValidationPreset.CONTENT_PACK,
                    seed = 42L,
                    seedCorpus = listOf(42L, 43L),
                    zoneId = "test",
                    floor = 2,
                    activePackIds = if (longLists) (10 downTo 1).map { "pack-$it" } else listOf("pack-a"),
                    touchedContentIds = if (longLists) (1..10).map { "content-$it" } else listOf("content-a"),
                    packKeyResolutionSummary =
                        ContentPackKeyResolutionSummary(
                            warningVisualKeys = listOf("visual.missing"),
                            warningAudioKeys = listOf("audio.missing"),
                            warningLocaleKeys = listOf("locale.missing"),
                        ),
                    bossVariantModeId = "default",
                    preferredBossVariantId = null,
                    lastResult = null,
                    scenarioEvidenceSummary =
                        ValidationScenarioEvidenceSummary(
                            whiteboxRoot = "UI/manual-records/root",
                            evidenceDir = "UI/manual-records/evidence",
                            manualRecordPath = "UI/manual-records/manual.md",
                            expectedEvidencePath = "build/reports/verification/evidence.tsv",
                            runbookPath = "UI/pr/runbook.md",
                            appExecutableSha256Path = "build/reports/app.sha256",
                            appExecutableSha256 = null,
                        ),
                    packVisibilityComparison =
                        if (includePackVisibilityComparison) {
                            ContentPackVisibilityComparisonSummary(
                                noPackState = ContentPackVisibilityStateSummary(activePackIds = emptyList()),
                                activeSamplePackState =
                                    ContentPackVisibilityStateSummary(
                                        activePackIds = listOf("sample.flooded_relics"),
                                        activePackSummaries =
                                            listOf(
                                                ContentPackOverlaySummary(
                                                    packId = "sample.flooded_relics",
                                                    namespace = "sample",
                                                    opCounts = mapOf("ADD" to 5),
                                                ),
                                            ),
                                        touchedContentIds = listOf("item.sample"),
                                    ),
                            )
                        } else {
                            null
                        },
                ),
            zoneNameKey = "zone.test",
            inspectCursor = Point(1, 2),
            phase4Guide =
                ValidationPhase4Guide(
                    targetLabelKeys = (1..5).map { "target.$it" },
                    quickPathLabelKeys = (1..5).map { "quick.$it" },
                    evidenceLabelKeys = (1..5).map { "evidence.$it" },
                ),
            scenarioContext = null,
            sections = emptyList(),
        )

    private fun panelWithSelectedActions(): ValidationOverlayPanelState =
        panel(longLists = true).copy(
            sections =
                listOf(
                    ValidationOverlaySectionState(
                        titleKey = "section.fast",
                        selected = true,
                        actions =
                            listOf(
                                ValidationOverlayActionState(labelKey = "action.evidence", selected = false),
                                ValidationOverlayActionState(labelKey = "action.shop", selected = true),
                                ValidationOverlayActionState(labelKey = "action.route", selected = false),
                            ),
                    ),
                    ValidationOverlaySectionState(
                        titleKey = "section.other",
                        selected = false,
                        actions = listOf(ValidationOverlayActionState(labelKey = "action.hidden", selected = false)),
                    ),
                ),
        )

    private fun localizer(locale: GameLocale = GameLocale.EN_US) =
        LocalizationBundle.fromMaps(
            mapOf(
                GameLocale.EN_US to localeMap(),
                GameLocale.ZH_CN to localeMap("验证", "预设", "种子", "区域", "层数", "证据"),
            ),
        ).translator(locale)

    private fun localeMap(
        summary: String = "Summary",
        preset: String = "Preset: {value}",
        seed: String = "Seed: {value}",
        zone: String = "Zone: {value}",
        floor: String = "Floor: {value}",
        evidence: String = "Evidence",
    ): Map<String, String> =
        mapOf(
            "ui.validation.overlay.summary" to summary,
            "ui.validation.preset.content_pack" to "Content pack",
            "ui.validation.entry.preset" to preset,
            "ui.validation.entry.seed" to seed,
            "ui.validation.entry.seed_corpus" to "Seed corpus: {value}",
            "ui.validation.entry.zone" to zone,
            "ui.validation.entry.floor" to floor,
            "ui.validation.active_packs" to "Active: {value}",
            "ui.validation.pack.namespaces" to "Namespaces: {value}",
            "ui.validation.pack.overlay_ops" to "Ops: {value}",
            "ui.validation.pack.touched_ids" to "Touched: {value}",
            "ui.validation.pack.key_resolution" to "Keys: {visual}/{audio}/{locale}/{overrides}/{warnings}",
            "ui.validation.pack.key_warnings" to "Warnings: visual={visual}, audio={audio}, locale={locale}",
            "ui.validation.pack.no_pack_state" to "No-pack state: active={active}, ops={ops}, touched={touched}",
            "ui.validation.pack.active_sample_state" to "Active sample pack state: active={active}, ops={ops}, touched={touched}",
            "ui.validation.empty" to "empty",
            "ui.validation.not_available" to "n/a",
            "ui.validation.none" to "none",
            "ui.validation.entry.boss_variant_mode" to "Boss mode: {value}",
            "ui.validation.boss_variant.default" to "default",
            "ui.validation.entry.preferred_variant" to "Preferred: {value}",
            "ui.inspect.cursor" to "Cursor {x},{y}",
            "ui.validation.overlay.last_result" to "Last: {value}",
            "ui.validation.phase4.targets" to "Targets",
            "ui.validation.phase4.quick_paths" to "Quick",
            "ui.validation.phase4.evidence" to "Evidence paths",
            "ui.validation.phase4.v4.evidence_summary" to evidence,
            "ui.validation.phase4.v4.evidence_summary.whitebox_root" to "Root: {value}",
            "ui.validation.phase4.v4.evidence_summary.evidence_dir" to "Dir: {value}",
            "ui.validation.phase4.v4.evidence_summary.report_summary" to "Report: {value}",
            "ui.validation.phase4.v4.evidence_summary.expected_evidence" to "Expected: {value}",
            "ui.validation.phase4.v4.evidence_summary.runbook" to "Runbook: {value}",
            "ui.validation.phase4.v4.evidence_summary.manual_record" to "Manual: {value}",
            "ui.validation.phase4.v4.evidence_summary.app_hash_file" to "Hash file: {value}",
            "ui.validation.phase4.v4.evidence_summary.app_hash" to "Hash: {value}",
            "ui.validation.phase4.v4.evidence_summary.producer_freshness" to "Freshness: {value}",
            "validation.phase4.v4.evidence.producer_freshness.not_applicable" to "n/a",
            "ui.controls.validation" to "Controls",
            "ui.validation.fold.more" to "+{count} more",
            "zone.test" to "Test zone",
            "section.fast" to "Fast actions",
            "section.other" to "Other actions",
            "action.evidence" to "Show evidence",
            "action.shop" to "Prepare shop",
            "action.route" to "Prepare route",
            "action.hidden" to "Hidden action",
        ) + (1..5).flatMap { index ->
            listOf(
                "target.$index" to "Target $index",
                "quick.$index" to "Quick $index",
                "evidence.$index" to "Evidence $index",
            )
        }.toMap()

    private fun assertCompactWarningAndEvidenceRows(
        rows: List<TileTextRow>,
        localizer: Localizer,
    ) {
        val evidenceTitle = localizer.text("ui.validation.phase4.v4.evidence_summary")
        assertEquals(TileTextTone.WHITE, rows[0].tone, rows.joinToString("\n") { it.text })
        assertTrue(rows[0].text.isNotBlank(), rows.joinToString("\n") { it.text })
        assertEquals(evidenceTitle, rows[1].text, rows.joinToString("\n") { it.text })
        assertEquals(TileTextTone.GOLD, rows[1].tone, rows.joinToString("\n") { it.text })
    }
}
