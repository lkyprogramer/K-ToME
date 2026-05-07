#!/usr/bin/env python3
"""Write and validate dark-v1 manifest coverage artifacts."""

from __future__ import annotations

import argparse
import pathlib
from datetime import datetime, timezone
from typing import Any

from dark_sprite_sheet_contract import (
    DARK_RUNTIME_PREFIX,
    PENDING_RAW_OUTPUT,
    STYLE_TAG,
    load_key_registry,
    load_manifest_entries,
    load_sheet_plan,
    print_errors,
    write_json,
)


def raw_output_path(entry: dict[str, Any] | None) -> str:
    if entry is None:
        return ""
    return str(entry.get("rawOutputPath", "")).strip()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate dark-v1 manifest coverage by mode.")
    parser.add_argument("--coverage-mode", choices=("pr00-dry-run", "owner-scope", "final-full"), default="final-full")
    parser.add_argument("--owner-pr", default="")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--registry", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/key-registry.yaml"))
    parser.add_argument("--manifest", type=pathlib.Path, default=pathlib.Path("assets-src/image/manifests/phase2-visual-manifest.json"))
    parser.add_argument("--runtime-manifest", type=pathlib.Path, default=pathlib.Path("client/src/main/resources/manifests/visual-manifest.json"))
    parser.add_argument("--report", type=pathlib.Path, default=pathlib.Path("build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json"))
    return parser.parse_args()


def is_dark_output(entry: dict[str, Any] | None) -> bool:
    return raw_output_path(entry).startswith(DARK_RUNTIME_PREFIX)


def is_pending_output(entry: dict[str, Any] | None) -> bool:
    if entry is None:
        return True
    return raw_output_path(entry) in ("", PENDING_RAW_OUTPUT)


def is_old_style_output(entry: dict[str, Any] | None) -> bool:
    raw_path = raw_output_path(entry)
    return raw_path not in ("", PENDING_RAW_OUTPUT) and not raw_path.startswith(DARK_RUNTIME_PREFIX)


def build_coverage(args: argparse.Namespace) -> tuple[dict[str, Any], list[str]]:
    _, cells, plan_errors = load_sheet_plan(args.plan)
    registry_by_key, registry_errors = load_key_registry(args.registry)
    manifest_by_key = load_manifest_entries(args.manifest)
    runtime_by_key = load_manifest_entries(args.runtime_manifest)
    errors = plan_errors + registry_errors

    if args.coverage_mode == "owner-scope" and not args.owner_pr:
        errors.append("owner-scope coverage requires --owner-pr.")
    if args.coverage_mode != "owner-scope" and args.owner_pr:
        errors.append(f"{args.coverage_mode} coverage must not set --owner-pr.")

    registry_keys = sorted(registry_by_key)
    if args.coverage_mode == "owner-scope":
        expected_keys = sorted(
            target_key
            for target_key, entry in registry_by_key.items()
            if str(entry.get("ownerPr", "")).strip() == args.owner_pr
        )
    else:
        expected_keys = registry_keys

    missing_keys = sorted(key for key in expected_keys if key not in manifest_by_key or key not in runtime_by_key)
    pending_keys = sorted(key for key in expected_keys if is_pending_output(manifest_by_key.get(key)))
    old_style_keys = sorted(key for key in expected_keys if is_old_style_output(manifest_by_key.get(key)))
    covered_keys = sorted(key for key in expected_keys if is_dark_output(manifest_by_key.get(key)))

    if args.coverage_mode == "owner-scope" and missing_keys:
        errors.append(f"owner-scope missing keys for {args.owner_pr}: {', '.join(missing_keys)}.")
    if args.coverage_mode == "final-full":
        if missing_keys:
            errors.append(f"final-full missing keys: {', '.join(missing_keys)}.")
        if pending_keys:
            errors.append(f"final-full pendingOrRejectedPlayerVisibleCells are not empty: {', '.join(pending_keys)}.")
        if old_style_keys:
            errors.append(f"final-full oldStylePlayerVisibleKeys are not empty: {', '.join(old_style_keys)}.")

    common: dict[str, Any] = {
        "schemaVersion": "dark-v1-manifest-coverage-v1",
        "styleTag": STYLE_TAG,
        "scopeMode": args.coverage_mode,
        "ownerPr": args.owner_pr or None,
        "expectedKeySetSource": args.registry.as_posix(),
        "strictOldStyleResidue": args.coverage_mode == "final-full",
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "sourceManifestPath": args.manifest.as_posix(),
        "runtimeManifestPath": args.runtime_manifest.as_posix(),
        "keyRegistryPath": args.registry.as_posix(),
        "sheetPlanPath": args.plan.as_posix(),
    }
    if args.coverage_mode == "owner-scope":
        owner_sheet_ids = sorted(
            {
                str(registry_by_key[key].get("sheetId", "")).strip()
                for key in expected_keys
            }
        )
        common.update(
            {
                "ownerSheetIds": owner_sheet_ids,
                "ownerExpectedKeys": expected_keys,
                "ownerCoveredKeys": covered_keys,
                "ownerMissingKeys": missing_keys,
                "scopeExternalPendingKeys": sorted(
                    key for key in set(registry_keys) - set(expected_keys) if is_pending_output(manifest_by_key.get(key))
                ),
                "allowedOwnerFallbackKeys": pending_keys,
            }
        )
    else:
        common.update(
            {
                "expectedKeySet": expected_keys,
                "coveredKeySet": covered_keys,
                "missingKeys": missing_keys,
                "oldStylePlayerVisibleKeys": old_style_keys if args.coverage_mode == "final-full" else [],
                "pendingOrRejectedPlayerVisibleCells": pending_keys,
                "fallbackKeyUsage": {
                    key: str(registry_by_key.get(key, {}).get("fallbackKey", "")).strip()
                    for key in expected_keys
                    if is_pending_output(manifest_by_key.get(key))
                },
                "allowedFallbackKeys": ["missing_visual"] if args.coverage_mode == "pr00-dry-run" else [],
                "allowedCoverageExclusions": [],
                "sourceSheetIds": sorted({cell.sheet_id for cell in cells}),
            }
        )
    common["status"] = "FAIL" if errors else "PASS"
    common["errors"] = errors
    return common, errors


def main() -> int:
    args = parse_args()
    coverage, errors = build_coverage(args)
    write_json(args.report, coverage)
    if errors:
        return print_errors("dark-manifest-coverage-lint", errors)
    print(
        "dark-manifest-coverage-lint OK: "
        f"mode={args.coverage_mode}, expectedKeys={len(coverage.get('expectedKeySet', coverage.get('ownerExpectedKeys', [])))}, report={args.report.as_posix()}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
