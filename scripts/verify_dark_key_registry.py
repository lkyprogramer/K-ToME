#!/usr/bin/env python3
"""Validate the dark-v1 key registry against the sheet plan and visual manifest."""

from __future__ import annotations

import argparse
import pathlib
from typing import Any

from dark_sprite_sheet_contract import (
    load_key_registry,
    load_manifest_entries,
    load_sheet_plan,
    OWNER_PR_PATTERN,
    OWNER_PR_PATTERN_TEXT,
    print_errors,
    write_json,
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Validate dark-v1 key registry ownership and fallbacks.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--registry", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/key-registry.yaml"))
    parser.add_argument("--manifest", type=pathlib.Path, default=pathlib.Path("assets-src/image/manifests/phase2-visual-manifest.json"))
    parser.add_argument("--report", type=pathlib.Path, default=None)
    return parser.parse_args()


def detect_alias_cycles(registry_by_key: dict[str, dict[str, Any]]) -> list[str]:
    def normalize_cycle(cycle: list[str]) -> tuple[str, ...]:
        first_index = min(range(len(cycle)), key=lambda index: cycle[index])
        return tuple(cycle[first_index:] + cycle[:first_index])

    errors: list[str] = []
    reported_cycles: set[tuple[str, ...]] = set()
    globally_visited: set[str] = set()
    for target_key in sorted(registry_by_key):
        if target_key in globally_visited:
            continue
        seen: list[str] = []
        current = target_key
        while current:
            if current in seen:
                cycle = seen[seen.index(current) :]
                normalized_cycle = normalize_cycle(cycle)
                if normalized_cycle not in reported_cycles:
                    errors.append(f"alias cycle detected: {' -> '.join(cycle + [cycle[0]])}.")
                    reported_cycles.add(normalized_cycle)
                globally_visited.update(cycle)
                break
            seen.append(current)
            current = str(registry_by_key.get(current, {}).get("aliasOf", "")).strip()
        else:
            globally_visited.update(seen)
    return errors


def validate_registry(
    plan_path: pathlib.Path,
    registry_path: pathlib.Path,
    manifest_path: pathlib.Path,
) -> tuple[list[str], dict[str, Any]]:
    _, cells, plan_errors = load_sheet_plan(plan_path)
    registry_by_key, registry_errors = load_key_registry(registry_path)
    manifest_by_key = load_manifest_entries(manifest_path)
    errors = plan_errors + registry_errors
    cells_by_key = {cell.target_key: cell for cell in cells}

    missing_registry_keys = sorted(set(cells_by_key) - set(registry_by_key))
    if missing_registry_keys:
        errors.append(f"sheet-plan targetKeys missing from key registry: {', '.join(missing_registry_keys)}.")
    extra_registry_keys = sorted(
        target_key
        for target_key in set(registry_by_key) - set(cells_by_key)
        if not str(registry_by_key[target_key].get("aliasOf", "")).strip()
    )
    if extra_registry_keys:
        errors.append(f"key registry targetKeys absent from sheet plan: {', '.join(extra_registry_keys)}.")

    for target_key, entry in sorted(registry_by_key.items()):
        cell = cells_by_key.get(target_key)
        owner_pr = str(entry.get("ownerPr", "")).strip()
        sheet_id = str(entry.get("sheetId", "")).strip()
        category = str(entry.get("category", "")).strip()
        fallback_key = str(entry.get("fallbackKey", "")).strip()
        consumer = str(entry.get("consumer", "")).strip()
        consumer_test = str(entry.get("consumerTest", "")).strip()
        alias_of = str(entry.get("aliasOf", "")).strip()

        if not owner_pr:
            errors.append(f"{target_key} ownerPr is required.")
        elif not OWNER_PR_PATTERN.fullmatch(owner_pr):
            errors.append(f"{target_key} ownerPr must match {OWNER_PR_PATTERN_TEXT}, got '{owner_pr}'.")
        if not fallback_key:
            errors.append(f"{target_key} fallbackKey is required.")
        elif fallback_key not in manifest_by_key:
            errors.append(f"{target_key} fallbackKey '{fallback_key}' does not exist in canonical manifest.")
        elif fallback_key == target_key:
            errors.append(f"{target_key} fallbackKey must not point to itself.")
        if not consumer:
            errors.append(f"{target_key} consumer is required.")
        if not consumer_test:
            errors.append(f"{target_key} consumerTest is required.")
        if alias_of and alias_of not in registry_by_key:
            errors.append(f"{target_key} aliasOf target '{alias_of}' is missing from key registry.")
        if cell is None:
            if alias_of:
                alias_target = registry_by_key.get(alias_of)
                if alias_target is not None:
                    alias_sheet_id = str(alias_target.get("sheetId", "")).strip()
                    alias_category = str(alias_target.get("category", "")).strip()
                    if sheet_id != alias_sheet_id:
                        errors.append(
                            f"{target_key} registry-only alias sheetId mismatch: "
                            f"registry={sheet_id} aliasOf={alias_of} sheetId={alias_target.get('sheetId')}."
                        )
                    if category != alias_category:
                        errors.append(
                            f"{target_key} registry-only alias category mismatch: "
                            f"registry={category} aliasOf={alias_of} category={alias_target.get('category')}."
                        )
            continue
        if sheet_id != cell.sheet_id:
            errors.append(f"{target_key} sheetId mismatch: registry={sheet_id} sheet-plan={cell.sheet_id}.")
        if category != cell.category:
            errors.append(f"{target_key} category mismatch: registry={category} sheet-plan={cell.category}.")
        if cell.alias_of and alias_of != cell.alias_of:
            errors.append(f"{target_key} aliasOf mismatch: registry={alias_of or '<none>'} sheet-plan={cell.alias_of}.")

    errors += detect_alias_cycles(registry_by_key)
    report = {
        "schemaVersion": "dark-key-registry-lint-v1",
        "registryPath": registry_path.as_posix(),
        "sheetPlanPath": plan_path.as_posix(),
        "entryCount": len(registry_by_key),
        "targetKeys": sorted(registry_by_key),
    }
    return errors, report


def main() -> int:
    args = parse_args()
    errors, report = validate_registry(args.plan, args.registry, args.manifest)
    if args.report:
        report["status"] = "FAIL" if errors else "PASS"
        report["errors"] = errors
        write_json(args.report, report)
    if errors:
        return print_errors("dark-key-registry-lint", errors)
    print(f"dark-key-registry-lint OK: entries={report['entryCount']}, registry={args.registry.as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
