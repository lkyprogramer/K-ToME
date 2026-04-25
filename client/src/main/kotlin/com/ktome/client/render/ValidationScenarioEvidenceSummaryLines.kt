package com.ktome.client.render

import com.ktome.game.i18n.Localizer
import com.ktome.game.validation.ValidationScenarioEvidenceSummary

internal fun validationScenarioEvidenceSummaryLines(
    localizer: Localizer,
    summary: ValidationScenarioEvidenceSummary,
): List<String> =
    listOf(
        localizer.text("ui.validation.phase4.v4.evidence_summary.whitebox_root", "value" to summary.whiteboxRoot),
        localizer.text("ui.validation.phase4.v4.evidence_summary.evidence_dir", "value" to summary.evidenceDir),
        localizer.text("ui.validation.phase4.v4.evidence_summary.report_summary", "value" to summary.expectedEvidencePath),
        localizer.text("ui.validation.phase4.v4.evidence_summary.expected_evidence", "value" to summary.expectedEvidencePath),
        localizer.text("ui.validation.phase4.v4.evidence_summary.runbook", "value" to summary.runbookPath),
        localizer.text("ui.validation.phase4.v4.evidence_summary.manual_record", "value" to summary.manualRecordPath),
        localizer.text("ui.validation.phase4.v4.evidence_summary.app_hash_file", "value" to summary.appExecutableSha256Path),
        localizer.text(
            "ui.validation.phase4.v4.evidence_summary.app_hash",
            "value" to (summary.appExecutableSha256 ?: localizer.text("ui.validation.none")),
        ),
        localizer.text(
            "ui.validation.phase4.v4.evidence_summary.producer_freshness",
            "value" to localizer.text(summary.producerFreshnessLabelKey),
        ),
    )
