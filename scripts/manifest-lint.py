#!/usr/bin/env python3
"""Lint the Phase 2 visual manifest against the asset plan."""

from __future__ import annotations

import argparse
import pathlib
import sys
from collections import Counter

from asset_pipeline_common import (
    EXPECTED_FOOTPRINT_BY_CATEGORY,
    collect_assets,
    load_json,
    load_yaml,
    print_errors,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Lint the Phase 2 visual manifest")
    parser.add_argument(
        "--plan",
        default="assets-src/image/specs/phase2-asset-plan.yaml",
        help="Path to the phase2 image asset plan YAML",
    )
    parser.add_argument(
        "--manifest",
        default="assets-src/image/manifests/phase2-visual-manifest.json",
        help="Path to the phase2 visual manifest JSON",
    )
    parser.add_argument(
        "--asset-root",
        default="assets-src/image/raw/generated",
        help="Root directory for generated raw files when --require-files is enabled",
    )
    parser.add_argument(
        "--require-files",
        action="store_true",
        help="Fail if manifest rawOutputPath entries do not exist under --asset-root",
    )
    return parser.parse_args()


def validate_manifest(
    plan_path: pathlib.Path,
    manifest_path: pathlib.Path,
    asset_root: pathlib.Path,
    require_files: bool,
) -> list[str]:
    plan = load_yaml(plan_path)
    manifest = load_json(manifest_path)
    errors: list[str] = []

    manifest_version = manifest.get("manifestVersion")
    if not isinstance(manifest_version, int) or manifest_version < 1:
        errors.append("manifestVersion must be an integer >= 1.")

    plan_style_tag = str(plan.get("styleTag", "")).strip()
    manifest_style_tag = str(manifest.get("styleTag", "")).strip()
    if manifest_style_tag != plan_style_tag:
        errors.append(
            f"manifest styleTag '{manifest_style_tag}' does not match plan styleTag '{plan_style_tag}'."
        )

    entries = manifest.get("entries")
    if not isinstance(entries, list) or not entries:
        errors.append("entries must be a non-empty list.")
        return errors

    entry_id_counter = Counter()
    visual_key_counter = Counter()
    raw_path_counter = Counter()
    plan_assets = {str(asset.get("id")): asset for asset in collect_assets(plan)}
    manifest_entries: dict[str, dict] = {}

    for entry in entries:
        if not isinstance(entry, dict):
            errors.append("manifest entries must be mappings.")
            continue
        source_asset_id = str(entry.get("sourceAssetId", "")).strip()
        visual_key = str(entry.get("visualKey", "")).strip()
        category = str(entry.get("category", "")).strip()
        raw_output_path = str(entry.get("rawOutputPath", "")).strip()
        footprint = str(entry.get("footprint", "")).strip()
        pivot_x = entry.get("pivotX")
        pivot_y = entry.get("pivotY")
        tags = entry.get("tags")

        entry_id_counter[source_asset_id] += 1
        visual_key_counter[visual_key] += 1
        raw_path_counter[raw_output_path] += 1
        manifest_entries[source_asset_id] = entry

        if not source_asset_id:
            errors.append("manifest entry sourceAssetId is required.")
        if not visual_key:
            errors.append(f"[{source_asset_id or '<unknown>'}] visualKey is required.")
        if not raw_output_path.endswith(".png"):
            errors.append(f"[{source_asset_id}] rawOutputPath must end with .png.")
        if category not in EXPECTED_FOOTPRINT_BY_CATEGORY:
            errors.append(f"[{source_asset_id}] unsupported category '{category}'.")

        expected_footprint = EXPECTED_FOOTPRINT_BY_CATEGORY.get(category)
        if expected_footprint and footprint != expected_footprint:
            errors.append(
                f"[{source_asset_id}] footprint must be '{expected_footprint}', got '{footprint}'."
            )

        if not isinstance(pivot_x, (int, float)) or not 0.0 <= float(pivot_x) <= 1.0:
            errors.append(f"[{source_asset_id}] pivotX must be between 0 and 1.")
        if not isinstance(pivot_y, (int, float)) or not 0.0 <= float(pivot_y) <= 1.0:
            errors.append(f"[{source_asset_id}] pivotY must be between 0 and 1.")
        if not isinstance(tags, list) or not tags or not all(isinstance(tag, str) and tag for tag in tags):
            errors.append(f"[{source_asset_id}] tags must be a non-empty string list.")

        plan_asset = plan_assets.get(source_asset_id)
        if not plan_asset:
            errors.append(f"manifest entry '{source_asset_id}' does not exist in asset plan.")
            continue

        expected_gate_tag = str(plan_asset["_gateId"]).lower()
        if isinstance(tags, list):
            tag_set = set(tags)
            if "phase2" not in tag_set:
                errors.append(f"[{source_asset_id}] tags must contain 'phase2'.")
            if expected_gate_tag not in tag_set:
                errors.append(f"[{source_asset_id}] tags must contain '{expected_gate_tag}'.")

        expected_visual_key = str(plan_asset.get("visualKey", "")).strip()
        expected_category = str(plan_asset.get("category", "")).strip()
        expected_raw_path = str(plan_asset.get("outputName", "")).strip()
        if visual_key != expected_visual_key:
            errors.append(
                f"[{source_asset_id}] visualKey mismatch: manifest='{visual_key}', plan='{expected_visual_key}'."
            )
        if category != expected_category:
            errors.append(
                f"[{source_asset_id}] category mismatch: manifest='{category}', plan='{expected_category}'."
            )
        if raw_output_path != expected_raw_path:
            errors.append(
                f"[{source_asset_id}] rawOutputPath mismatch: manifest='{raw_output_path}', plan='{expected_raw_path}'."
            )

        if require_files:
            candidate = asset_root / raw_output_path
            if not candidate.is_file():
                errors.append(f"[{source_asset_id}] generated raw file not found: {candidate}.")

    for key, count in entry_id_counter.items():
        if key and count > 1:
            errors.append(f"Duplicate sourceAssetId in manifest: '{key}'.")
    for key, count in visual_key_counter.items():
        if key and count > 1:
            errors.append(f"Duplicate visualKey in manifest: '{key}'.")
    for key, count in raw_path_counter.items():
        if key and count > 1:
            errors.append(f"Duplicate rawOutputPath in manifest: '{key}'.")

    missing_entries = sorted(set(plan_assets) - set(manifest_entries))
    extra_entries = sorted(set(manifest_entries) - set(plan_assets))
    if missing_entries:
        errors.append(f"Manifest missing plan asset ids: {', '.join(missing_entries)}.")
    if extra_entries:
        errors.append(f"Manifest has extra asset ids not in plan: {', '.join(extra_entries)}.")

    return errors


def main() -> int:
    args = parse_args()
    plan_path = pathlib.Path(args.plan)
    manifest_path = pathlib.Path(args.manifest)
    asset_root = pathlib.Path(args.asset_root)
    errors = validate_manifest(plan_path, manifest_path, asset_root, args.require_files)
    if errors:
        return print_errors(errors)

    manifest = load_json(manifest_path)
    print(
        "manifest-lint OK: "
        f"entries={len(manifest['entries'])}, styleTag={manifest['styleTag']}, manifest={manifest_path}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
