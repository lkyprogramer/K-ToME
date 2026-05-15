#!/usr/bin/env python3
"""Lint the Phase 2 visual manifests and planned generated assets."""

from __future__ import annotations

import argparse
import pathlib
import sys

from asset_pipeline_common import (
    PHASE2_REQUIRED_VISUAL_KEYS,
    collect_assets,
    flatten_required_keys,
    load_json,
    load_yaml,
    print_errors,
    split_phase2_fallback_budget,
)
from dark_sprite_sheet_contract import load_key_registry, load_sheet_plan


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Lint the Phase 2 visual manifest")
    parser.add_argument(
        "--plan",
        default="assets-src/image/specs/phase2-asset-plan.yaml",
        help="Path to the phase2 image asset plan YAML",
    )
    parser.add_argument(
        "--extra-plan",
        action="append",
        default=[],
        help="Additional asset plan YAML paths whose visual keys must also be covered by the manifest.",
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
    parser.add_argument(
        "--dark-key-registry",
        default=None,
        help="Optional dark-v1 key registry YAML whose keys count as upstream visual coverage.",
    )
    parser.add_argument(
        "--dark-sheet-plan",
        default=None,
        help="Optional dark-v1 sheet-plan YAML whose cell outputName values must match canonical manifest rawOutputPath.",
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
        for field_name in ("category", "rawOutputPath", "footprint", "pivotX", "pivotY", "tags", "tintColorHex"):
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
    plan_paths: list[pathlib.Path],
    manifest_path: pathlib.Path,
    runtime_manifest_path: pathlib.Path,
    runtime_root: pathlib.Path,
    bundled_spec_path: pathlib.Path,
    asset_root: pathlib.Path,
    require_files: bool,
    dark_key_registry_path: pathlib.Path | None,
    dark_sheet_plan_path: pathlib.Path | None,
) -> list[str]:
    plans = [load_yaml(path) for path in plan_paths]
    plan = plans[0]
    manifest = load_json(manifest_path)
    runtime_manifest = load_json(runtime_manifest_path)
    errors: list[str] = []
    bundled_by_key = load_bundled_specs(bundled_spec_path, errors)

    manifest_version = manifest.get("manifestVersion")
    if not isinstance(manifest_version, int) or manifest_version < 1:
        errors.append("manifestVersion must be an integer >= 1.")
    if runtime_manifest.get("manifestVersion") != manifest_version:
        errors.append("runtime visual manifest manifestVersion must match canonical visual manifest.")

    plan_style_tags = {str(candidate.get("styleTag", "")).strip() for candidate in plans}
    if "" in plan_style_tags:
        errors.append("All visual asset plans must define styleTag.")
    if len(plan_style_tags - {""}) > 1:
        errors.append(f"All visual asset plans must share the same styleTag, got {sorted(plan_style_tags - {''})}.")
    plan_style_tag = next(iter(plan_style_tags - {""}), "")
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

    required_visual_keys = flatten_required_keys(PHASE2_REQUIRED_VISUAL_KEYS)
    for key in sorted(required_visual_keys):
        source_entry = source_by_key.get(key)
        if source_entry is None:
            errors.append(f"canonical visual manifest is missing required formal-path key '{key}'.")
            continue
        source_path = str(source_entry.get("rawOutputPath", "")).strip()
        if source_path == "debug/missing_visual.png":
            errors.append(f"required formal-path visual key '{key}' must not use missing_visual in canonical manifest.")
        runtime_entry = runtime_by_key.get(key)
        if runtime_entry is None:
            errors.append(f"runtime visual manifest is missing required formal-path key '{key}'.")
            continue
        runtime_path = str(runtime_entry.get("rawOutputPath", "")).strip()
        if runtime_path == "debug/missing_visual.png":
            errors.append(f"required formal-path visual key '{key}' must not use missing_visual in runtime manifest.")

    compare_entries(source_by_key, runtime_by_key, runtime_root, errors)

    dark_bridge_requested = bool(dark_key_registry_path or dark_sheet_plan_path)
    dark_plan_errors: list[str] = []
    dark_registry_errors: list[str] = []
    dark_registry_by_key: dict[str, dict] = {}
    dark_cell_by_key: dict[str, object] = {}
    dark_registry_only_alias_keys: set[str] = set()
    if dark_bridge_requested and dark_key_registry_path and dark_sheet_plan_path:
        _, dark_cells, dark_plan_errors = load_sheet_plan(dark_sheet_plan_path)
        dark_registry_by_key, dark_registry_errors = load_key_registry(dark_key_registry_path)
        if not dark_plan_errors and not dark_registry_errors:
            dark_cell_by_key = {cell.target_key: cell for cell in dark_cells}
            dark_registry_only_alias_keys = {
                key
                for key, entry in dark_registry_by_key.items()
                if key not in dark_cell_by_key and str(entry.get("aliasOf", "")).strip()
            }
    dark_bridge_keys = set(dark_cell_by_key) | dark_registry_only_alias_keys

    plan_by_key: dict[str, dict] = {}
    for candidate_plan in plans:
        for asset in collect_assets(candidate_plan):
            visual_key = str(asset.get("visualKey", "")).strip()
            output_name = str(asset.get("outputName", "")).strip()
            category = str(asset.get("category", "")).strip()
            if visual_key in dark_cell_by_key:
                continue
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
    for key in sorted((set(source_by_key) & set(bundled_by_key)) - dark_bridge_keys):
        source_entry = source_by_key[key]
        bundled_entry = bundled_by_key[key]
        for field_name in ("category", "rawOutputPath", "footprint", "pivotX", "pivotY", "tags", "tintColorHex"):
            if bundled_entry.get(field_name) != source_entry.get(field_name):
                errors.append(
                    f"canonical visual manifest field mismatch for bundled spec '{key}' -> {field_name}: "
                    f"spec='{bundled_entry.get(field_name)}' source='{source_entry.get(field_name)}'."
                )

    if dark_bridge_requested:
        if not dark_key_registry_path or not dark_sheet_plan_path:
            errors.append("--dark-key-registry and --dark-sheet-plan must be provided together.")
        else:
            errors += [f"dark sheet plan bridge: {error}" for error in dark_plan_errors]
            errors += [f"dark key registry bridge: {error}" for error in dark_registry_errors]
            if dark_plan_errors or dark_registry_errors:
                errors.append("dark manifest bridge skipped because dark sheet plan or key registry failed validation.")
            else:
                missing_dark_registry_keys = sorted(set(dark_cell_by_key) - set(dark_registry_by_key))
                if missing_dark_registry_keys:
                    errors.append(
                        "dark sheet plan keys missing from dark key registry: "
                        + ", ".join(missing_dark_registry_keys)
                        + "."
                    )
                for key, cell in sorted(dark_cell_by_key.items()):
                    source_entry = source_by_key.get(key)
                    if source_entry is None:
                        errors.append(f"canonical visual manifest is missing dark sheet-plan targetKey '{key}'.")
                        continue
                    if str(source_entry.get("category", "")).strip() != cell.category:
                        errors.append(
                            f"canonical visual manifest category mismatch for dark key '{key}': "
                            f"sheet-plan='{cell.category}' source='{source_entry.get('category')}'."
                        )
                    if str(source_entry.get("rawOutputPath", "")).strip() != cell.output_name:
                        errors.append(
                            f"canonical visual manifest rawOutputPath mismatch for dark key '{key}': "
                            f"sheet-plan='{cell.output_name}' source='{source_entry.get('rawOutputPath')}'."
                        )
                expected_spec_keys |= set(dark_cell_by_key)
                registry_only_aliases = {
                    key: dark_registry_by_key[key]
                    for key in sorted(dark_registry_only_alias_keys)
                }
                for key, registry_entry in sorted(registry_only_aliases.items()):
                    source_entry = source_by_key.get(key)
                    if source_entry is None:
                        errors.append(f"canonical visual manifest is missing dark registry-only alias key '{key}'.")
                        continue
                    alias_of = str(registry_entry.get("aliasOf", "")).strip()
                    alias_source_entry = source_by_key.get(alias_of)
                    if alias_source_entry is None:
                        errors.append(f"dark registry-only alias '{key}' points to missing canonical key '{alias_of}'.")
                        continue
                    if str(source_entry.get("rawOutputPath", "")).strip() != str(alias_source_entry.get("rawOutputPath", "")).strip():
                        errors.append(
                            f"canonical visual manifest rawOutputPath mismatch for registry-only alias '{key}': "
                            f"alias='{source_entry.get('rawOutputPath')}' target='{alias_source_entry.get('rawOutputPath')}'."
                        )
                expected_spec_keys |= set(registry_only_aliases)

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

    return errors


def main() -> int:
    args = parse_args()
    errors = validate_manifest(
        [pathlib.Path(args.plan), *[pathlib.Path(path) for path in args.extra_plan]],
        pathlib.Path(args.manifest),
        pathlib.Path(args.runtime_manifest),
        pathlib.Path(args.runtime_root),
        pathlib.Path(args.bundled_spec),
        pathlib.Path(args.asset_root),
        args.require_files,
        pathlib.Path(args.dark_key_registry) if args.dark_key_registry else None,
        pathlib.Path(args.dark_sheet_plan) if args.dark_sheet_plan else None,
    )
    if errors:
        return print_errors(errors)

    manifest = load_json(pathlib.Path(args.manifest))
    required_visual_keys = flatten_required_keys(PHASE2_REQUIRED_VISUAL_KEYS)
    source_by_key = {
        str(entry.get("key", "")).strip(): entry
        for entry in manifest["entries"]
        if isinstance(entry, dict) and str(entry.get("key", "")).strip()
    }
    required_fallback_keys, placeholder_budget_keys = split_phase2_fallback_budget(
        entries_by_key=source_by_key,
        required_keys=required_visual_keys,
        path_field="rawOutputPath",
        fallback_value="debug/missing_visual.png",
    )
    print(
        "manifest-lint OK: "
        f"entries={len(manifest['entries'])}, styleTag={manifest['styleTag']}, manifest={args.manifest}, "
        f"runtimeManifest={args.runtime_manifest}, requiredFormalPathKeys={len(required_visual_keys)}, "
        f"requiredMissingVisual={len(required_fallback_keys)}, phase2PlaceholderBudget={len(placeholder_budget_keys)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
