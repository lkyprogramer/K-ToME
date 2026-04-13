#!/usr/bin/env python3

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parent.parent
SOURCE_PATH = REPO_ROOT / "docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline.json"
TARGET_PATH = REPO_ROOT / "docs/review/phase4/opt/baselines/2026-04-09-opt-pr01-terrain-metrics-baseline-unified.json"
LOG_PATH = REPO_ROOT / "docs/review/phase4/opt/baselines/2026-04-12-opt-pr01-terrain-metrics-baseline-unified.migration-log.json"


def main() -> None:
    source = json.loads(SOURCE_PATH.read_text())
    migrated = {
        "schemaVersion": 1,
        "baselineId": source["baselineId"],
        "domainId": "terrain",
        "mode": "RELATIVE_BASELINE",
        "metricDefinitionVersion": source["metricDefinitionVersion"],
        "approvedDebtKeys": [],
        "expectedFailureCodes": [],
        "ceilings": [],
        "expectedMetricRanges": [
            {
                "metricId": "terrainTaggedCombatExposureRate",
                "baselineValue": metric["baselineRate"],
                "targetRelativeIncrease": metric["targetRelativeIncrease"],
                "numerator": metric["numerator"],
                "denominator": metric["denominator"],
                "numeratorLabel": metric["numeratorLabel"],
                "denominatorLabel": metric["denominatorLabel"],
                "sourceValueText": metric["sourceValueText"],
                "normalizedFormula": metric["normalizedFormula"],
            }
            if metric["metricId"] == "terrainTaggedCombatExposureRate"
            else {
                "metricId": "terrainInteractionEncounterRate.aggregate",
                "baselineValue": metric["baselineRate"],
                "targetRelativeIncrease": metric["targetRelativeIncrease"],
                "numerator": metric["numerator"],
                "denominator": metric["denominator"],
                "numeratorLabel": metric["numeratorLabel"],
                "denominatorLabel": metric["denominatorLabel"],
                "sourceValueText": metric["sourceValueText"],
                "normalizedFormula": metric["normalizedFormula"],
            }
            for metric in source["metrics"]
        ],
        "sourceArtifactPath": source["sourceArtifactPath"],
        "sourceBuildId": source.get("sourceBuildId"),
        "sourceGeneratedAt": source.get("sourceGeneratedAt"),
        "notes": [
            *source.get("notes", []),
            f"Migrated from {SOURCE_PATH.relative_to(REPO_ROOT).as_posix()} by scripts/migrate_verification_baseline.py.",
        ],
        "metadata": {},
    }
    TARGET_PATH.write_text(json.dumps(migrated, indent=4) + "\n")
    log = {
        "migratedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "sourcePath": SOURCE_PATH.relative_to(REPO_ROOT).as_posix(),
        "targetPath": TARGET_PATH.relative_to(REPO_ROOT).as_posix(),
        "logVersion": 1,
        "baselineId": migrated["baselineId"],
        "domainId": migrated["domainId"],
        "mode": migrated["mode"],
        "schemaVersion": migrated["schemaVersion"],
        "metricIds": [metric["metricId"] for metric in migrated["expectedMetricRanges"]],
    }
    LOG_PATH.write_text(json.dumps(log, indent=4) + "\n")


if __name__ == "__main__":
    main()
