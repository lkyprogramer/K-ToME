#!/usr/bin/env python3
"""Lint the Phase 2 visual manifests and planned generated assets."""

from __future__ import annotations

import argparse
import pathlib
import sys

from asset_pipeline_common import collect_assets, load_json, load_yaml, print_errors


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
        help="Path to the canonical phase2 visual manifest JSON",
    )
    parser.add_argument(
        "--runtime-manifest",
        default="client/src/main/resources/manifests/visual-manifest.json",
        help="Path to the bundled runtime visual manifest JSON",
    )
    parser.add_argument(
        "--runtime-root",
        default="client/src/main/resources",
        help="Root directory for bundled runtime assets referenced by the runtime manifest",
    )
    parser.add_argument(
        "--bundled-spec",
        default="assets-src/image/specs/phase2-runtime-visual-specs.json",
        help="Path to the bundled placeholder/debug visual spec catalog JSON",
    )
    parser.add_argument(
        "--asset-root",
        default="assets-src/image/raw/generated",
        help="Root directory for generated raw files when --require-files is enabled",
    )
    parser.add_argument(
        "--require-files",
        action="store_true",
        help="Fail if plan rawOutputPath entries do not exist under --asset-root",
    )
    return parser.parse_args()


def validate_entry(entry: dict, source_name: str, errors: list[str]) -> str:
    key = str(entry.get("key", "")).strip()
    if not key:
        errors.append(f"{source_name} entry key is required.")
    category = str(entry.get("category", "")).strip()
    raw_output_path = str(entry.get("rawOutputPath", "")).strip()
    footprint = str(entry.get("footprint", "")).strip()
    if not category:
        errors.append(f"{source_name} entry '{key or '<unknown>'}' must define category.")
    if not raw_output_path:
        errors.append(f"{source_name} entry '{key or '<unknown>'}' must define rawOutputPath.")
    if not footprint:
        errors.append(f"{source_name} entry '{key or '<unknown>'}' must define footprint.")
    pivot_x = entry.get("pivotX")
    pivot_y = entry.get("pivotY")
    if not isinstance(pivot_x, (int, float)) or not 0.0 <= float(pivot_x) <= 1.0:
        errors.append(f"{source_name} entry '{key or '<unknown>'}' pivotX must be between 0 and 1.")
    if not isinstance(pivot_y, (int, float)) or not 0.0 <= float(pivot_y) <= 1.0:
        errors.append(f"{source_name} entry '{key or '<unknown>'}' pivotY must be between 0 and 1.")
    return key


def compare_entries(
    source_by_key: dict[str, dict],
    runtime_by_key: dict[str, dict],
    runtime_root: pathlib.Path,
    errors: list[str],
) -> None:
    source_keys = set(source_by_key)
    runtime_keys = set(runtime_by_key)
    missing_runtime = sorted(source_keys - runtime_keys)
    extra_runtime = sorted(runtime_keys - source_keys)
    if missing_runtime:
        errors.append(f"runtime visual manifest is missing canonical keys: {', '.join(missing_runtime)}.")
    if extra_runtime:
        errors.append(f"runtime visual manifest has non-canonical keys: {', '.join(extra_runtime)}.")

    for key in sorted(source_keys & runtime_keys):
        source_entry = source_by_key[key]
        runtime_entry = runtime_by_key[key]
        for field_name in ("category", "rawOutputPath", "footprint", "pivotX", "pivotY", "tags", "asciiGlyph", "asciiColorHex"):
            if source_entry.get(field_name) != runtime_entry.get(field_name):
                errors.append(
                    f"runtime visual manifest field mismatch for '{key}' -> {field_name}: "
                    f"source='{source_entry.get(field_name)}' runtime='{runtime_entry.get(field_name)}'."
                )
        runtime_asset_path = runtime_root / str(runtime_entry.get("rawOutputPath", "")).strip()
        if not runtime_asset_path.is_file():
            errors.append(f"runtime visual asset is missing for '{key}': {runtime_asset_path}.")


def load_bundled_specs(path: pathlib.Path, errors: list[str]) -> dict[str, dict]:
    payload = load_json(path)
    entries = payload.get("entries")
    if not isinstance(entries, list):
        errors.append("bundled visual spec catalog entries must be a list.")
        return {}

    bundled_by_key: dict[str, dict] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            errors.append("bundled visual spec catalog entries must be mappings.")
            continue
        key = validate_entry(entry, "bundled visual spec catalog", errors)
        if key in bundled_by_key:
            errors.append(f"Duplicate bundled visual spec key '{key}'.")
        bundled_by_key[key] = entry
    return bundled_by_key


def validate_manifest(
    plan_path: pathlib.Path,
    manifest_path: pathlib.Path,
    runtime_manifest_path: pathlib.Path,
    runtime_root: pathlib.Path,
    bundled_spec_path: pathlib.Path,
    asset_root: pathlib.Path,
    require_files: bool,
) -> list[str]:
    plan = load_yaml(plan_path)
    manifest = load_json(manifest_path)
    runtime_manifest = load_json(runtime_manifest_path)
    errors: list[str] = []
    bundled_by_key = load_bundled_specs(bundled_spec_path, errors)

    manifest_version = manifest.get("manifestVersion")
    if not isinstance(manifest_version, int) or manifest_version < 1:
        errors.append("manifestVersion must be an integer >= 1.")
    if runtime_manifest.get("manifestVersion") != manifest_version:
        errors.append("runtime visual manifest manifestVersion must match canonical visual manifest.")

    plan_style_tag = str(plan.get("styleTag", "")).strip()
    manifest_style_tag = str(manifest.get("styleTag", "")).strip()
    runtime_style_tag = str(runtime_manifest.get("styleTag", "")).strip()
    if manifest_style_tag != plan_style_tag:
        errors.append(
            f"canonical visual manifest styleTag '{manifest_style_tag}' does not match plan styleTag '{plan_style_tag}'."
        )
    if runtime_style_tag != plan_style_tag:
        errors.append(
            f"runtime visual manifest styleTag '{runtime_style_tag}' does not match plan styleTag '{plan_style_tag}'."
        )

    manifest_fallback_key = str(manifest.get("fallbackKey", "")).strip()
    runtime_fallback_key = str(runtime_manifest.get("fallbackKey", "")).strip()
    if not manifest_fallback_key:
        errors.append("canonical visual manifest fallbackKey is required.")
    if runtime_fallback_key != manifest_fallback_key:
        errors.append("runtime visual manifest fallbackKey must match canonical visual manifest.")

    source_prefix_rules = manifest.get("prefixRules", [])
    runtime_prefix_rules = runtime_manifest.get("prefixRules", [])
    if source_prefix_rules != runtime_prefix_rules:
        errors.append("runtime visual manifest prefixRules must exactly match canonical visual manifest.")

    source_entries = manifest.get("entries")
    runtime_entries = runtime_manifest.get("entries")
    if not isinstance(source_entries, list) or not source_entries:
        errors.append("canonical visual manifest entries must be a non-empty list.")
        return errors
    if not isinstance(runtime_entries, list) or not runtime_entries:
        errors.append("runtime visual manifest entries must be a non-empty list.")
        return errors

    source_by_key: dict[str, dict] = {}
    runtime_by_key: dict[str, dict] = {}
    for entry in source_entries:
        if not isinstance(entry, dict):
            errors.append("canonical visual manifest entries must be mappings.")
            continue
        key = validate_entry(entry, "canonical visual manifest", errors)
        if key in source_by_key:
            errors.append(f"Duplicate canonical visual manifest key '{key}'.")
        source_by_key[key] = entry
    for entry in runtime_entries:
        if not isinstance(entry, dict):
            errors.append("runtime visual manifest entries must be mappings.")
            continue
        key = validate_entry(entry, "runtime visual manifest", errors)
        if key in runtime_by_key:
            errors.append(f"Duplicate runtime visual manifest key '{key}'.")
        runtime_by_key[key] = entry

    if manifest_fallback_key and manifest_fallback_key not in source_by_key:
        errors.append(f"canonical visual manifest is missing fallback entry '{manifest_fallback_key}'.")
    if manifest_fallback_key and manifest_fallback_key in runtime_by_key:
        fallback_asset_path = runtime_root / str(runtime_by_key[manifest_fallback_key].get("rawOutputPath", "")).strip()
        if not fallback_asset_path.is_file():
            errors.append(f"runtime visual fallback asset is missing: {fallback_asset_path}.")

    compare_entries(source_by_key, runtime_by_key, runtime_root, errors)

    plan_by_key: dict[str, dict] = {}
    for asset in collect_assets(plan):
        visual_key = str(asset.get("visualKey", "")).strip()
        output_name = str(asset.get("outputName", "")).strip()
        category = str(asset.get("category", "")).strip()
        plan_by_key[visual_key] = asset
        source_entry = source_by_key.get(visual_key)
        if source_entry is None:
            errors.append(f"canonical visual manifest is missing plan visualKey '{visual_key}'.")
            continue
        if str(source_entry.get("category", "")).strip() != category:
            errors.append(
                f"canonical visual manifest category mismatch for '{visual_key}': "
                f"plan='{category}' source='{source_entry.get('category')}'."
            )
        if str(source_entry.get("rawOutputPath", "")).strip() != output_name:
            errors.append(
                f"canonical visual manifest rawOutputPath mismatch for '{visual_key}': "
                f"plan='{output_name}' source='{source_entry.get('rawOutputPath')}'."
            )
        if require_files:
            candidate = asset_root / output_name
            if not candidate.is_file():
                errors.append(f"generated raw file not found for plan visualKey '{visual_key}': {candidate}.")

    expected_spec_keys = set(plan_by_key) | set(bundled_by_key)
    missing_spec_keys = sorted(source_by_key.keys() - expected_spec_keys)
    extra_spec_keys = sorted(expected_spec_keys - source_by_key.keys())
    if missing_spec_keys:
        errors.append(
            "canonical visual manifest keys without upstream spec coverage: "
            + ", ".join(missing_spec_keys)
            + "."
        )
    if extra_spec_keys:
        errors.append(
            "visual spec coverage defines keys absent from the canonical visual manifest: "
            + ", ".join(extra_spec_keys)
            + "."
        )

    for key in sorted(set(source_by_key) & set(bundled_by_key)):
        source_entry = source_by_key[key]
        bundled_entry = bundled_by_key[key]
        for field_name in ("category", "rawOutputPath", "footprint", "pivotX", "pivotY", "tags", "asciiGlyph", "asciiColorHex"):
            if bundled_entry.get(field_name) != source_entry.get(field_name):
                errors.append(
                    f"canonical visual manifest field mismatch for bundled spec '{key}' -> {field_name}: "
                    f"spec='{bundled_entry.get(field_name)}' source='{source_entry.get(field_name)}'."
                )

    return errors


def main() -> int:
    args = parse_args()
    errors = validate_manifest(
        pathlib.Path(args.plan),
        pathlib.Path(args.manifest),
        pathlib.Path(args.runtime_manifest),
        pathlib.Path(args.runtime_root),
        pathlib.Path(args.bundled_spec),
        pathlib.Path(args.asset_root),
        args.require_files,
    )
    if errors:
        return print_errors(errors)

    manifest = load_json(pathlib.Path(args.manifest))
    print(
        "manifest-lint OK: "
        f"entries={len(manifest['entries'])}, styleTag={manifest['styleTag']}, manifest={args.manifest}, "
        f"runtimeManifest={args.runtime_manifest}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
