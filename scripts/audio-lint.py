#!/usr/bin/env python3
"""Lint the Phase 2 audio plan and canonical/runtime manifests."""

from __future__ import annotations

import argparse
import pathlib
import sys

from asset_pipeline_common import load_json, load_yaml, print_errors


REQUIRED_CUE_FAMILIES = {
    "silence",
    "ui",
    "footstep",
    "melee",
    "spell",
    "monster",
    "interactable",
    "ambience",
    "music",
}

REQUIRED_AUDIBLE_KEY_PATHS = {
    "audio.ui.confirm": "audio/ui/confirm.ogg",
    "audio.footstep.default": "audio/footstep/default.ogg",
    "audio.melee.default": "audio/melee/default.ogg",
    "audio.spell.default": "audio/spell/default.ogg",
    "audio.monster.default": "audio/monster/default.ogg",
    "audio.interactable.stairs": "audio/interactable/stairs.ogg",
    "ambient.shattered_outpost": "audio/ambient/shattered_outpost.ogg",
    "ambient.greenwood_fringe": "audio/ambient/greenwood_fringe.ogg",
    "ambient.deep_iron_pit": "audio/ambient/deep_iron_pit.ogg",
    "ambient.grey_gate_depths": "audio/ambient/grey_gate_depths.ogg",
    "audio.music.menu": "audio/music/menu.ogg",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Lint the Phase 2 audio manifest")
    parser.add_argument(
        "--plan",
        default="assets-src/audio/specs/phase2-audio-plan.yaml",
        help="Path to the phase2 audio plan YAML",
    )
    parser.add_argument(
        "--manifest",
        default="assets-src/audio/manifests/phase2-audio-manifest.json",
        help="Path to the phase2 audio manifest JSON",
    )
    parser.add_argument(
        "--runtime-manifest",
        default="client/src/main/resources/manifests/audio-manifest.json",
        help="Path to the bundled runtime audio manifest JSON",
    )
    parser.add_argument(
        "--runtime-root",
        default="client/src/main/resources",
        help="Root directory for bundled runtime assets referenced by the runtime manifest",
    )
    parser.add_argument(
        "--bundled-spec",
        default="assets-src/audio/specs/phase2-runtime-audio-specs.json",
        help="Path to the bundled placeholder/debug audio spec catalog JSON",
    )
    return parser.parse_args()


def load_bundled_specs(path: pathlib.Path, errors: list[str]) -> dict[str, dict]:
    payload = load_json(path)
    entries = payload.get("entries")
    if not isinstance(entries, list):
        errors.append("bundled audio spec catalog entries must be a list.")
        return {}

    bundled_by_key: dict[str, dict] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            errors.append("bundled audio spec catalog entries must be mappings.")
            continue
        key = str(entry.get("key", "")).strip()
        if key in bundled_by_key:
            errors.append(f"Duplicate bundled audio spec key '{key}'.")
        bundled_by_key[key] = entry
        if not key:
            errors.append("bundled audio spec catalog entry key is required.")
        if not str(entry.get("cueFamily", "")).strip():
            errors.append(f"bundled audio spec catalog entry '{key}' must define cueFamily.")
        if not str(entry.get("eventId", "")).strip():
            errors.append(f"bundled audio spec catalog entry '{key}' must define eventId.")
        if not str(entry.get("sourcePath", "")).strip():
            errors.append(f"bundled audio spec catalog entry '{key}' must define sourcePath.")
    return bundled_by_key


def validate(
    plan_path: pathlib.Path,
    manifest_path: pathlib.Path,
    runtime_manifest_path: pathlib.Path,
    runtime_root: pathlib.Path,
    bundled_spec_path: pathlib.Path,
) -> list[str]:
    plan = load_yaml(plan_path)
    manifest = load_json(manifest_path)
    runtime_manifest = load_json(runtime_manifest_path)
    errors: list[str] = []
    bundled_by_key = load_bundled_specs(bundled_spec_path, errors)

    if plan.get("manifestVersion") != manifest.get("manifestVersion"):
        errors.append("plan manifestVersion must match audio manifest manifestVersion.")
    if manifest.get("manifestVersion") != runtime_manifest.get("manifestVersion"):
        errors.append("canonical audio manifest manifestVersion must match runtime audio manifest manifestVersion.")

    fallback_key = str(plan.get("fallbackKey", "")).strip()
    if not fallback_key:
        errors.append("fallbackKey is required in the audio plan.")
    if fallback_key != str(manifest.get("fallbackKey", "")).strip():
        errors.append("plan fallbackKey must match audio manifest fallbackKey.")
    if fallback_key != str(runtime_manifest.get("fallbackKey", "")).strip():
        errors.append("canonical audio fallbackKey must match runtime audio manifest fallbackKey.")

    if manifest.get("prefixRules", []) != runtime_manifest.get("prefixRules", []):
        errors.append("canonical and runtime audio prefixRules must match exactly.")

    cue_families = plan.get("cueFamilies")
    if not isinstance(cue_families, list) or not cue_families:
        errors.append("cueFamilies must be a non-empty list.")
        return errors

    cue_family_ids: set[str] = set()
    for family in cue_families:
        if not isinstance(family, dict):
            errors.append("cueFamilies entries must be mappings.")
            continue
        family_id = str(family.get("id", "")).strip()
        cue_family_ids.add(family_id)
        for field_name in ("provider", "source", "license"):
            if not str(family.get(field_name, "")).strip():
                errors.append(f"cue family '{family_id or '<unknown>'}' must define {field_name}.")

    missing_families = sorted(REQUIRED_CUE_FAMILIES - cue_family_ids)
    if missing_families:
        errors.append(f"Missing required cue families: {', '.join(missing_families)}.")

    plan_entries = plan.get("entries")
    manifest_entries = manifest.get("entries")
    runtime_entries = runtime_manifest.get("entries")
    if not isinstance(plan_entries, list) or not plan_entries:
        errors.append("entries must be a non-empty list in the audio plan.")
        return errors
    if not isinstance(manifest_entries, list) or not manifest_entries:
        errors.append("entries must be a non-empty list in the audio manifest.")
        return errors
    if not isinstance(runtime_entries, list) or not runtime_entries:
        errors.append("entries must be a non-empty list in the runtime audio manifest.")
        return errors

    manifest_by_key = {}
    for entry in manifest_entries:
        if not isinstance(entry, dict):
            errors.append("audio manifest entries must be mappings.")
            continue
        key = str(entry.get("key", "")).strip()
        manifest_by_key[key] = entry
        if not key:
            errors.append("audio manifest entry key is required.")
        if str(entry.get("cueFamily", "")).strip() not in cue_family_ids:
            errors.append(f"audio manifest entry '{key}' references unknown cue family.")
        if not str(entry.get("eventId", "")).strip():
            errors.append(f"audio manifest entry '{key}' must define eventId.")
        if not str(entry.get("sourcePath", "")).strip():
            errors.append(f"audio manifest entry '{key}' must define sourcePath.")

    runtime_by_key = {}
    for entry in runtime_entries:
        if not isinstance(entry, dict):
            errors.append("runtime audio manifest entries must be mappings.")
            continue
        key = str(entry.get("key", "")).strip()
        runtime_by_key[key] = entry
        if not key:
            errors.append("runtime audio manifest entry key is required.")
        if not str(entry.get("cueFamily", "")).strip():
            errors.append(f"runtime audio manifest entry '{key}' must define cueFamily.")
        if not str(entry.get("eventId", "")).strip():
            errors.append(f"runtime audio manifest entry '{key}' must define eventId.")
        if not str(entry.get("sourcePath", "")).strip():
            errors.append(f"runtime audio manifest entry '{key}' must define sourcePath.")

    manifest_keys = set(manifest_by_key)
    runtime_keys = set(runtime_by_key)
    missing_runtime = sorted(manifest_keys - runtime_keys)
    extra_runtime = sorted(runtime_keys - manifest_keys)
    if missing_runtime:
        errors.append(f"runtime audio manifest is missing canonical keys: {', '.join(missing_runtime)}.")
    if extra_runtime:
        errors.append(f"runtime audio manifest has non-canonical keys: {', '.join(extra_runtime)}.")

    for key in sorted(manifest_keys & runtime_keys):
        manifest_entry = manifest_by_key[key]
        runtime_entry = runtime_by_key[key]
        for field_name in ("cueFamily", "eventId", "sourcePath", "tags"):
            if manifest_entry.get(field_name) != runtime_entry.get(field_name):
                errors.append(
                    f"runtime audio manifest field mismatch for '{key}' -> {field_name}: "
                    f"source='{manifest_entry.get(field_name)}' runtime='{runtime_entry.get(field_name)}'."
                )

    family_has_non_fallback: dict[str, bool] = {family: False for family in REQUIRED_CUE_FAMILIES if family != "silence"}
    for entry in manifest_entries:
        if not isinstance(entry, dict):
            continue
        family = str(entry.get("cueFamily", "")).strip()
        source_path = str(entry.get("sourcePath", "")).strip()
        if family in family_has_non_fallback and source_path and source_path != "audio/fallback/silence.ogg":
            family_has_non_fallback[family] = True

    missing_audible_families = sorted(family for family, present in family_has_non_fallback.items() if not present)
    if missing_audible_families:
        errors.append(
            "Required cue families must provide at least one non-fallback asset: "
            + ", ".join(missing_audible_families)
            + "."
        )

    for key, expected_path in REQUIRED_AUDIBLE_KEY_PATHS.items():
        manifest_entry = manifest_by_key.get(key)
        if manifest_entry is None:
            errors.append(f"canonical audio manifest is missing required audible key '{key}'.")
            continue
        source_path = str(manifest_entry.get("sourcePath", "")).strip()
        if source_path != expected_path:
            errors.append(
                f"required audible key '{key}' must resolve to '{expected_path}', got '{source_path}'."
            )
        if source_path == "audio/fallback/silence.ogg":
            errors.append(f"required audible key '{key}' must not use the silence fallback.")

    for entry in plan_entries:
        if not isinstance(entry, dict):
            errors.append("audio plan entries must be mappings.")
            continue
        key = str(entry.get("key", "")).strip()
        cue_family = str(entry.get("cueFamily", "")).strip()
        if not key:
            errors.append("audio plan entry key is required.")
            continue
        if cue_family not in cue_family_ids:
            errors.append(f"audio plan entry '{key}' references unknown cue family '{cue_family}'.")
        manifest_entry = manifest_by_key.get(key)
        if manifest_entry is None:
            errors.append(f"audio manifest is missing plan key '{key}'.")
            continue
        for field_name in ("cueFamily", "eventId", "sourcePath"):
            if str(entry.get(field_name, "")).strip() != str(manifest_entry.get(field_name, "")).strip():
                errors.append(
                    f"audio manifest field mismatch for '{key}' -> {field_name}: "
                    f"plan='{entry.get(field_name, '')}' manifest='{manifest_entry.get(field_name, '')}'."
                )
        runtime_entry = runtime_by_key.get(key)
        if runtime_entry is None:
            errors.append(f"runtime audio manifest is missing plan key '{key}'.")
            continue
        for field_name in ("cueFamily", "eventId", "sourcePath"):
            if str(manifest_entry.get(field_name, "")).strip() != str(runtime_entry.get(field_name, "")).strip():
                errors.append(
                    f"runtime audio manifest field mismatch for '{key}' -> {field_name}: "
                    f"assets-src='{manifest_entry.get(field_name, '')}' runtime='{runtime_entry.get(field_name, '')}'."
                )

    for key, expected_path in REQUIRED_AUDIBLE_KEY_PATHS.items():
        plan_entry = next(
            (entry for entry in plan_entries if isinstance(entry, dict) and str(entry.get("key", "")).strip() == key),
            None,
        )
        if plan_entry is None:
            errors.append(f"audio plan is missing required audible key '{key}'.")
            continue
        plan_source_path = str(plan_entry.get("sourcePath", "")).strip()
        if plan_source_path != expected_path:
            errors.append(
                f"audio plan required audible key '{key}' must point to '{expected_path}', got '{plan_source_path}'."
            )

    overlapping_spec_keys = sorted({str(entry.get("key", "")).strip() for entry in plan_entries if isinstance(entry, dict)} & set(bundled_by_key))
    if overlapping_spec_keys:
        errors.append(
            "bundled audio spec catalog duplicates audio plan keys: "
            + ", ".join(overlapping_spec_keys)
            + "."
        )

    expected_spec_keys = {str(entry.get("key", "")).strip() for entry in plan_entries if isinstance(entry, dict)} | set(bundled_by_key)
    missing_spec_keys = sorted(manifest_keys - expected_spec_keys)
    extra_spec_keys = sorted(expected_spec_keys - manifest_keys)
    if missing_spec_keys:
        errors.append(
            "canonical audio manifest keys without upstream spec coverage: "
            + ", ".join(missing_spec_keys)
            + "."
        )
    if extra_spec_keys:
        errors.append(
            "audio spec coverage defines keys absent from the canonical audio manifest: "
            + ", ".join(extra_spec_keys)
            + "."
        )

    for key in sorted(manifest_keys & set(bundled_by_key)):
        bundled_entry = bundled_by_key[key]
        manifest_entry = manifest_by_key[key]
        if str(bundled_entry.get("cueFamily", "")).strip() not in cue_family_ids:
            errors.append(f"bundled audio spec catalog entry '{key}' references unknown cue family.")
        for field_name in ("cueFamily", "eventId", "sourcePath", "tags"):
            if bundled_entry.get(field_name) != manifest_entry.get(field_name):
                errors.append(
                    f"canonical audio manifest field mismatch for bundled spec '{key}' -> {field_name}: "
                    f"spec='{bundled_entry.get(field_name)}' manifest='{manifest_entry.get(field_name)}'."
                )

    runtime_fallback_entry = runtime_by_key.get(fallback_key)
    if runtime_fallback_entry is None:
        errors.append(f"runtime audio manifest is missing fallback entry '{fallback_key}'.")

    required_runtime_paths = sorted(
        {
            str(entry.get("sourcePath", "")).strip()
            for entry in runtime_entries
            if isinstance(entry, dict) and str(entry.get("sourcePath", "")).strip()
        },
    )
    for source_path in required_runtime_paths:
        full_path = runtime_root / source_path
        if not full_path.is_file():
            errors.append(f"runtime audio asset is missing: {full_path}.")

    return errors


def main() -> int:
    args = parse_args()
    errors = validate(
        pathlib.Path(args.plan),
        pathlib.Path(args.manifest),
        pathlib.Path(args.runtime_manifest),
        pathlib.Path(args.runtime_root),
        pathlib.Path(args.bundled_spec),
    )
    if errors:
        return print_errors(errors)

    print(
        "audio-lint OK: "
        f"plan={args.plan}, manifest={args.manifest}, runtimeManifest={args.runtime_manifest}, "
        f"requiredCueFamilies={sorted(REQUIRED_CUE_FAMILIES)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
