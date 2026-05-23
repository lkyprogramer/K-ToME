package com.ktome.client.render

import com.ktome.game.i18n.GameLocale
import com.ktome.game.i18n.LocalizationBundle
import com.ktome.game.validation.ValidationScenarioEvidenceSummary
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationScenarioEvidenceSummaryLinesTest {
    @Test
    fun compactsRepoRelativeEvidencePaths() {
        val lines =
            validationScenarioEvidenceSummaryLines(
                localizer(),
                evidenceSummary(
                    expectedEvidencePath =
                        "UI/manual-records/deep/path/for/pr06/that/keeps/the-tail/with/several/more/segments/than/the/overlay/can/render/dark-uiux-pr06-validation-overlay.md",
                ),
            )

        assertTrue(lines.any { line -> line.contains("dark-uiux-pr06-validation-overlay.md") }, lines.joinToString("\n"))
        assertTrue(lines.any { line -> line.contains("...") }, lines.joinToString("\n"))
    }

    @Test
    fun rejectsOrRedactsMachineAbsolutePaths() {
        val lines =
            validationScenarioEvidenceSummaryLines(
                localizer(),
                evidenceSummary(expectedEvidencePath = "/Users/local/private/dark-uiux-pr06-validation-overlay.md"),
            )

        assertFalse(lines.any { line -> line.contains("/Users/local/private") }, lines.joinToString("\n"))
        assertTrue(lines.any { line -> line.contains("<machine-path-redacted>") }, lines.joinToString("\n"))
    }

    private fun evidenceSummary(expectedEvidencePath: String): ValidationScenarioEvidenceSummary =
        ValidationScenarioEvidenceSummary(
            whiteboxRoot = "UI/manual-records/root",
            evidenceDir = "UI/manual-records/evidence",
            manualRecordPath = "UI/manual-records/manual.md",
            expectedEvidencePath = expectedEvidencePath,
            runbookPath = "UI/pr/runbook.md",
            appExecutableSha256Path = "build/reports/app.sha256",
            appExecutableSha256 = null,
        )

    private fun localizer() =
        LocalizationBundle.fromMaps(
            mapOf(
                GameLocale.EN_US to
                    mapOf(
                        "ui.validation.none" to "none",
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
                    ),
                GameLocale.ZH_CN to emptyMap(),
            ),
        ).translator(GameLocale.EN_US)
}
