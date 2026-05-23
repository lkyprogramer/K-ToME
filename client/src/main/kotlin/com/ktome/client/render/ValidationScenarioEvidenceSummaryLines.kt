package com.ktome.client.render

import com.ktome.game.i18n.Localizer
import com.ktome.game.validation.ValidationScenarioEvidenceSummary

internal fun validationScenarioEvidenceSummaryLines(
    localizer: Localizer,
    summary: ValidationScenarioEvidenceSummary,
): List<String> =
    buildList {
        add(localizer.text("ui.validation.phase4.v4.evidence_summary.whitebox_root", "value" to validationEvidencePathForDisplay(summary.whiteboxRoot)))
        add(localizer.text("ui.validation.phase4.v4.evidence_summary.evidence_dir", "value" to validationEvidencePathForDisplay(summary.evidenceDir)))
        add(localizer.text("ui.validation.phase4.v4.evidence_summary.report_summary", "value" to validationEvidencePathForDisplay(summary.expectedEvidencePath)))
        add(localizer.text("ui.validation.phase4.v4.evidence_summary.expected_evidence", "value" to validationEvidencePathForDisplay(summary.expectedEvidencePath)))
        add(localizer.text("ui.validation.phase4.v4.evidence_summary.runbook", "value" to validationEvidencePathForDisplay(summary.runbookPath)))
        add(localizer.text("ui.validation.phase4.v4.evidence_summary.manual_record", "value" to validationEvidencePathForDisplay(summary.manualRecordPath)))
        add(
            localizer.text(
                "ui.validation.phase4.v4.evidence_summary.app_hash_file",
                "value" to validationEvidencePathForDisplay(summary.appExecutableSha256Path),
            ),
        )
        add(
            localizer.text(
                "ui.validation.phase4.v4.evidence_summary.app_hash",
                "value" to (summary.appExecutableSha256 ?: localizer.text("ui.validation.none")),
            ),
        )
        if (summary.requiredLogEventKeys.isNotEmpty()) {
            add(
                localizer.text(
                    "ui.validation.phase4.v4.evidence_summary.required_events",
                    "value" to summary.requiredLogEventKeys.joinToString(", "),
                ),
            )
        }
        summary.scenarioNoteLabelKey?.let { noteKey ->
            add(
                localizer.text(
                    "ui.validation.phase4.v4.evidence_summary.scenario_note",
                    "value" to localizer.text(noteKey),
                ),
            )
        }
        add(
            localizer.text(
                "ui.validation.phase4.v4.evidence_summary.producer_freshness",
                "value" to localizer.text(summary.producerFreshnessLabelKey),
            ),
        )
    }
