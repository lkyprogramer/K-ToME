#!/usr/bin/env python3
"""Lint the Phase 2 audio plan and canonical/runtime manifests."""

from __future__ import annotations

import argparse
import pathlib
import sys

from asset_pipeline_common import (
    PHASE2_REQUIRED_AUDIO_KEYS,
    flatten_required_keys,
    load_json,
    load_yaml,
    print_errors,
    split_phase2_fallback_budget,
)


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
    "audio.ui.cancel": "audio/ui/cancel.ogg",
    "audio.ui.hover": "audio/ui/hover.ogg",
    "audio.footstep.default": "audio/footstep/default.ogg",
    "audio.interactable.open": "audio/interactable/open.ogg",
    "audio.melee.default": "audio/melee/default.ogg",
    "audio.melee.light": "audio/melee/light.ogg",
    "audio.spell.default": "audio/spell/default.ogg",
    "audio.spell.basic": "audio/spell/basic.ogg",
    "audio.monster.default": "audio/monster/default.ogg",
    "audio.monster.bandit.sentry": "audio/monster/bandit_alert.ogg",
    "audio.monster.beast.rat": "audio/monster/beast_rat.ogg",
    "audio.monster.cultist.dungeon_lord": "audio/monster/cultist_dungeon_lord.ogg",
    "audio.monster.orc.raider": "audio/monster/orc_raider.ogg",
    "audio.monster.undead.bone_archer": "audio/monster/undead_bone_archer.ogg",
    "audio.interactable.stairs": "audio/interactable/stairs.ogg",
    "audio.boss.warning": "audio/boss/warning.ogg",
    "audio.boss.cultist.dungeon_lord": "audio/boss/cultist_dungeon_lord.ogg",
    "ambient.shattered_outpost": "audio/ambient/shattered_outpost.ogg",
    "ambient.greenwood_fringe": "audio/ambient/greenwood_fringe.ogg",
    "ambient.deep_iron_pit": "audio/ambient/deep_iron_pit.ogg",
    "ambient.grey_gate_depths": "audio/ambient/grey_gate_depths.ogg",
    "audio.music.menu": "audio/music/menu.ogg",
    "audio.profession.vanguard": "audio/professions/vanguard.ogg",
    "audio.profession.arcanist": "audio/professions/arcanist.ogg",
    "audio.item.basic_shield": "audio/item/basic_shield.ogg",
    "audio.item.arcane_staff": "audio/item/arcane_staff.ogg",
    "audio.item.apprentice_robe": "audio/item/apprentice_robe.ogg",
    "audio.item.mana_potion": "audio/item/mana_potion.ogg",
    "audio.talent.power_strike": "audio/talent/power_strike.ogg",
    "audio.talent.fireball": "audio/talent/fireball.ogg",
    "audio.tree.vanguard_arms": "audio/tree/vanguard_arms.ogg",
    "audio.tree.arcanist_flame": "audio/tree/arcanist_flame.ogg",
    "audio.boss.bandit_captain": "audio/boss/bandit_captain.ogg",
    "audio.zone.shattered_outpost": "audio/ambient/shattered_outpost.ogg",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Lint the Phase 2 audio manifest")
    parser.add_argument(
        "--plan",
        default="assets-src/audio/specs/phase2-audio-plan.yaml",
        help="Path to the phase2 audio plan YAML",
    )
    parser.add_argument(
        "--extra-plan",
        action="append",
        default=[],
        help="Additional audio plan YAML paths whose entries must also be covered by the manifest.",
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
    parser.add_argument(
        "--content-index",
        default="game/src/main/resources/data/audio/index.yaml",
        help="Path to the gameplay content audio index YAML that must stay aligned with formal-path audio keys.",
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


def placeholder_tagged_keys(entries_by_key: dict[str, dict]) -> list[str]:
    return sorted(
        key
        for key, entry in entries_by_key.items()
        if "placeholder" in {str(tag).strip() for tag in entry.get("tags", []) if str(tag).strip()}
    )


def load_content_audio_keys(path: pathlib.Path, errors: list[str]) -> set[str]:
    payload = load_yaml(path)
    profiles = payload.get("audioProfiles")
    if not isinstance(profiles, list):
        errors.append("content audio index audioProfiles must be a list.")
        return set()

    keys: set[str] = set()
    for entry in profiles:
        if not isinstance(entry, dict):
            errors.append("content audio index entries must be mappings.")
            continue
        key = str(entry.get("id", "")).strip()
        if not key:
            errors.append("content audio index entry id is required.")
            continue
        if key in keys:
            errors.append(f"Duplicate content audio index id '{key}'.")
        keys.add(key)
    return keys


def validate(
    plan_paths: list[pathlib.Path],
    manifest_path: pathlib.Path,
    runtime_manifest_path: pathlib.Path,
    runtime_root: pathlib.Path,
    bundled_spec_path: pathlib.Path,
    content_index_path: pathlib.Path,
) -> list[str]:
    plans = [load_yaml(path) for path in plan_paths]
    plan = plans[0]
    manifest = load_json(manifest_path)
    runtime_manifest = load_json(runtime_manifest_path)
    errors: list[str] = []
    bundled_by_key = load_bundled_specs(bundled_spec_path, errors)
    content_audio_keys = load_content_audio_keys(content_index_path, errors)

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

    plan_entries = list(plan.get("entries") or [])
    for extra_plan in plans[1:]:
        extra_entries = extra_plan.get("entries")
        if extra_entries is None:
            continue
        if not isinstance(extra_entries, list):
            errors.append("extra audio plan entries must be a list when provided.")
            continue
        plan_entries.extend(extra_entries)
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
    manifest_placeholder_keys = placeholder_tagged_keys(manifest_by_key)
    if manifest_placeholder_keys:
        errors.append(
            "canonical audio manifest must not contain placeholder-tagged entries: "
            + ", ".join(manifest_placeholder_keys)
            + "."
        )
    runtime_placeholder_keys = placeholder_tagged_keys(runtime_by_key)
    if runtime_placeholder_keys:
        errors.append(
            "runtime audio manifest must not contain placeholder-tagged entries: "
            + ", ".join(runtime_placeholder_keys)
            + "."
        )
    bundled_placeholder_keys = placeholder_tagged_keys(bundled_by_key)
    if bundled_placeholder_keys:
        errors.append(
            "bundled audio spec catalog must not contain placeholder-tagged entries: "
            + ", ".join(bundled_placeholder_keys)
            + "."
        )

    missing_runtime = sorted(manifest_keys - runtime_keys)
    extra_runtime = sorted(runtime_keys - manifest_keys)
    if missing_runtime:
        errors.append(f"runtime audio manifest is missing canonical keys: {', '.join(missing_runtime)}.")
    if extra_runtime:
        errors.append(f"runtime audio manifest has non-canonical keys: {', '.join(extra_runtime)}.")

    missing_manifest_from_content = sorted(content_audio_keys - manifest_keys)
    if missing_manifest_from_content:
        errors.append(
            "content audio index defines ids absent from the canonical audio manifest: "
            + ", ".join(missing_manifest_from_content)
            + "."
        )

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

    required_audio_keys = flatten_required_keys(PHASE2_REQUIRED_AUDIO_KEYS)
    for key in sorted(required_audio_keys):
        plan_entry = next(
            (entry for entry in plan_entries if isinstance(entry, dict) and str(entry.get("key", "")).strip() == key),
            None,
        )
        if plan_entry is None:
            errors.append(f"audio plan is missing required formal-path key '{key}'.")
            continue
        plan_source_path = str(plan_entry.get("sourcePath", "")).strip()
        if plan_source_path == "audio/fallback/silence.ogg":
            errors.append(f"required formal-path audio key '{key}' must not use silence in audio plan.")

        manifest_entry = manifest_by_key.get(key)
        if manifest_entry is None:
            errors.append(f"canonical audio manifest is missing required formal-path key '{key}'.")
            continue
        manifest_source_path = str(manifest_entry.get("sourcePath", "")).strip()
        if manifest_source_path == "audio/fallback/silence.ogg":
            errors.append(f"required formal-path audio key '{key}' must not use silence in canonical audio manifest.")

        runtime_entry = runtime_by_key.get(key)
        if runtime_entry is None:
            errors.append(f"runtime audio manifest is missing required formal-path key '{key}'.")
            continue
        runtime_source_path = str(runtime_entry.get("sourcePath", "")).strip()
        if runtime_source_path == "audio/fallback/silence.ogg":
            errors.append(f"required formal-path audio key '{key}' must not use silence in runtime audio manifest.")

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

    formal_content_audio_prefixes = (
        "audio.mutation.",
        "audio.terrain.",
        "audio.boss.variant.",
    )
    required_content_audio_keys = sorted(
        {
            str(entry.get("key", "")).strip()
            for entry in plan_entries
            if isinstance(entry, dict)
            and any(str(entry.get("key", "")).strip().startswith(prefix) for prefix in formal_content_audio_prefixes)
        }
    )
    missing_content_audio_keys = [key for key in required_content_audio_keys if key not in content_audio_keys]
    if missing_content_audio_keys:
        errors.append(
            "content audio index is missing formal-path gameplay audio keys: "
            + ", ".join(missing_content_audio_keys)
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
        [pathlib.Path(args.plan), *[pathlib.Path(path) for path in args.extra_plan]],
        pathlib.Path(args.manifest),
        pathlib.Path(args.runtime_manifest),
        pathlib.Path(args.runtime_root),
        pathlib.Path(args.bundled_spec),
        pathlib.Path(args.content_index),
    )
    if errors:
        return print_errors(errors)

    manifest = load_json(pathlib.Path(args.manifest))
    manifest_by_key = {
        str(entry.get("key", "")).strip(): entry
        for entry in manifest["entries"]
        if isinstance(entry, dict) and str(entry.get("key", "")).strip()
    }
    required_audio_keys = flatten_required_keys(PHASE2_REQUIRED_AUDIO_KEYS)
    required_fallback_keys, silence_budget_keys = split_phase2_fallback_budget(
        entries_by_key=manifest_by_key,
        required_keys=required_audio_keys,
        path_field="sourcePath",
        fallback_value="audio/fallback/silence.ogg",
    )
    print(
        "audio-lint OK: "
        f"plan={args.plan}, manifest={args.manifest}, runtimeManifest={args.runtime_manifest}, "
        f"requiredCueFamilies={sorted(REQUIRED_CUE_FAMILIES)}, requiredFormalPathKeys={len(required_audio_keys)}, "
        f"requiredSilence={len(required_fallback_keys)}, phase2SilenceBudget={len(silence_budget_keys)}"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
