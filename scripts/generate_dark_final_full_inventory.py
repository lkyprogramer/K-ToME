#!/usr/bin/env python3
"""Generate the PR-06 dark-v1 final-full expected inventory."""

from __future__ import annotations

import argparse
import json
import pathlib
from collections import defaultdict
from typing import Any, Callable

from dark_sprite_sheet_contract import (
    load_key_registry,
    load_manifest_entries,
    load_sheet_plan,
    sha256_file,
)

SCHEMA_VERSION = "dark-v1-final-full-inventory-v1"
GENERATOR_ID = "generate_dark_final_full_inventory.py:v1"
OBJECTIVE_MARKER_KEY = "icon.quest.objective_marker"
MANIFEST_SOURCED_FAMILIES = {
    "skill icon",
    "talent visual",
    "tree icon",
    "tree portrait",
    "status icon",
    "mutation icon",
    "damage type icon",
    "quest icon",
    "zone icon",
    "profession icon",
    "difficulty icon",
    "fallback/debug/hidden",
}


FamilyRule = tuple[str, Callable[[str, dict[str, Any]], bool], str, str, str]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate dark-v1 final-full inventory.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--registry", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/key-registry.yaml"))
    parser.add_argument("--manifest", type=pathlib.Path, default=pathlib.Path("assets-src/image/manifests/phase2-visual-manifest.json"))
    parser.add_argument("--runtime-manifest", type=pathlib.Path, default=pathlib.Path("client/src/main/resources/manifests/visual-manifest.json"))
    parser.add_argument("--screen-coverage-matrix", type=pathlib.Path, default=pathlib.Path("UI/pr/screen-coverage-matrix.md"))
    parser.add_argument("--handoff-inventory", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/dark-v1-pr06-handoff-inventory.json"))
    parser.add_argument("--out", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/dark-v1-final-full-inventory.json"))
    return parser.parse_args()


def repo_relative_path(path: pathlib.Path) -> str:
    try:
        return path.resolve().relative_to(pathlib.Path.cwd().resolve()).as_posix()
    except ValueError:
        return path.as_posix()


def exact_talent_key(key: str, suffix: str) -> bool:
    return key.startswith("talent.") and key.endswith(suffix)


def family_rules() -> list[FamilyRule]:
    return [
        (
            "skill icon",
            lambda key, _: key.startswith("icon.skill.") or exact_talent_key(key, ".icon"),
            "TalentAssetReferences / inscription active ability icon consumers",
            "ManifestResolveTest / talent golden / skill cooldown overlay focused test",
            "PR-06",
        ),
        (
            "talent visual",
            lambda key, _: exact_talent_key(key, ".visual"),
            "TalentAssetReferences",
            "ManifestResolveTest",
            "PR-06",
        ),
        (
            "tree icon",
            lambda key, _: key.startswith("icon.tree."),
            "TalentSidebarPresenter",
            "TalentSidebarPresenterTest / talent golden",
            "PR-06",
        ),
        (
            "tree portrait",
            lambda key, _: key.startswith("tree."),
            "Talent route or tree portrait consumer",
            "ManifestResolveTest / route or talent golden",
            "PR-06",
        ),
        (
            "status icon",
            lambda key, _: key.startswith("icon.status."),
            "StatusIconResolver",
            "StatusDefinitionsTest / StatusSchemaContractTest / StatusIconResolverTest",
            "PR-06",
        ),
        (
            "mutation icon",
            lambda key, _: key.startswith("icon.mutation."),
            "Actor mutation inspect or target-card consumer",
            "ManifestResolveTest / actor mutation consumer test",
            "PR-06",
        ),
        (
            "damage type icon",
            lambda key, _: key.startswith("icon.damage_type."),
            "damage tooltip, damage float, and resistance panel consumers",
            "ManifestResolveTest / damage tooltip focused tests",
            "PR-06",
        ),
        (
            "quest icon",
            lambda key, _: key.startswith("icon.quest."),
            "QuestSummaryIconResolver",
            "TileRenderModelTest.shellQuestSummaryCarriesGenericQuestMarkerForObjectiveLog",
            "PR-06",
        ),
        (
            "zone icon",
            lambda key, _: key.startswith("zone.") and key.endswith(".icon"),
            "route or inspect consumer",
            "ManifestResolveTest / route or inspect consumer test",
            "PR-06",
        ),
        (
            "profession icon",
            lambda key, _: key.startswith("icon.profession."),
            "ProfessionPlayerCreationOption.iconKey",
            "ProfessionSchemaTest / profession selection consumer test / ManifestResolveTest",
            "PR-06",
        ),
        (
            "difficulty icon",
            lambda key, _: key.startswith("difficulty.") and key.endswith(".icon"),
            "validation setup difficulty display",
            "ManifestResolveTest",
            "PR-06",
        ),
        (
            "fallback/debug/hidden",
            lambda key, entry: key == "missing_visual"
            or "hidden" in key
            or str(entry.get("category", "")).strip() == "debug",
            "manifest-only-with-reason",
            "fallback injection test / manual record",
            "UNKNOWN",
        ),
        (
            "actor visual",
            lambda key, _: key.startswith("actor."),
            "actor visual placements",
            "TileRendererCanvasTest / ManifestResolveTest",
            "UNKNOWN",
        ),
        (
            "monster icon",
            lambda key, _: key.startswith("icon.monster."),
            "actor card or inspect monster icon consumer",
            "ManifestResolveTest / actor inspect consumer test",
            "UNKNOWN",
        ),
        (
            "item icon",
            lambda key, _: key.startswith("item."),
            "inventory, equipment, loot and shop item consumers",
            "ManifestResolveTest / inventory equipment consumer tests",
            "UNKNOWN",
        ),
        (
            "affix icon",
            lambda key, _: key.startswith("affix."),
            "item affix tooltip or equipment detail consumer",
            "ManifestResolveTest / equipment tooltip consumer test",
            "UNKNOWN",
        ),
        (
            "material icon",
            lambda key, _: key.startswith("material."),
            "crafting or equipment material consumer",
            "ManifestResolveTest / material consumer test",
            "UNKNOWN",
        ),
        (
            "boss icon",
            lambda key, _: key.startswith("boss.") and key.endswith(".icon"),
            "boss route, codex, or encounter summary consumer",
            "ManifestResolveTest / boss consumer test",
            "UNKNOWN",
        ),
        (
            "boss visual",
            lambda key, _: key.startswith("boss.") and key.endswith(".visual"),
            "boss encounter visual consumer",
            "ManifestResolveTest / boss harness visual test",
            "UNKNOWN",
        ),
        (
            "zone visual",
            lambda key, _: key.startswith("zone.") and key.endswith(".visual"),
            "route, map, or biome visual consumer",
            "ManifestResolveTest / route visual consumer test",
            "UNKNOWN",
        ),
        (
            "portrait visual",
            lambda key, _: key.startswith("portrait."),
            "profession or actor portrait consumer",
            "ManifestResolveTest / portrait consumer test",
            "UNKNOWN",
        ),
        (
            "prop visual",
            lambda key, _: key.startswith("prop."),
            "map prop visual placements",
            "TileRendererCanvasTest / ManifestResolveTest",
            "UNKNOWN",
        ),
        (
            "tileset visual",
            lambda key, _: key.startswith("tileset.") or key.startswith("tile."),
            "map tile visual placements",
            "TileRendererCanvasTest / ManifestResolveTest",
            "UNKNOWN",
        ),
        (
            "ui chrome",
            lambda key, _: key.startswith("ui."),
            "shell, controls, HUD, setup, shop, and panel chrome consumers",
            "TileRendererCanvasTest / client UI focused tests",
            "UNKNOWN",
        ),
        (
            "vfx visual",
            lambda key, _: key.startswith("vfx."),
            "telegraph, terrain interaction, boss, or zone effect consumer",
            "ManifestResolveTest / VFX focused test",
            "UNKNOWN",
        ),
    ]


def family_for_key(key: str, registry_entry: dict[str, Any]) -> tuple[str, str, str, str] | None:
    for family, predicate, default_consumer, default_test, default_owner_pr in family_rules():
        if predicate(key, registry_entry):
            return family, default_consumer, default_test, default_owner_pr
    return None


def source_paths(args: argparse.Namespace) -> list[pathlib.Path]:
    paths = [
        args.manifest,
        args.runtime_manifest,
        args.registry,
        args.plan,
        args.screen_coverage_matrix,
        args.handoff_inventory,
        pathlib.Path("game/src/main/resources/data/talents/index.yaml"),
        pathlib.Path("game/src/main/resources/data/professions/index.yaml"),
        pathlib.Path("game/src/main/resources/data/visuals/index.yaml"),
        pathlib.Path("game/src/main/resources/data/races/index.yaml"),
    ]
    return sorted({path for path in paths}, key=lambda path: repo_relative_path(path))


def manifest_family_keys(manifest_by_key: dict[str, dict[str, Any]]) -> set[str]:
    keys: set[str] = {OBJECTIVE_MARKER_KEY}
    empty_entry: dict[str, Any] = {}
    for key in manifest_by_key:
        family_match = family_for_key(key, empty_entry)
        if family_match is not None and family_match[0] in MANIFEST_SOURCED_FAMILIES:
            keys.add(key)
    return keys


def build_inventory(args: argparse.Namespace) -> tuple[dict[str, Any], list[str]]:
    plans, _, plan_errors = load_sheet_plan(args.plan)
    registry_by_key, registry_errors = load_key_registry(args.registry)
    manifest_by_key = load_manifest_entries(args.manifest)
    runtime_manifest_by_key = load_manifest_entries(args.runtime_manifest)
    errors = plan_errors + registry_errors

    for path in source_paths(args):
        if not path.exists():
            errors.append(f"final-full inventory source path does not exist: {repo_relative_path(path)}.")

    expected_keys = sorted(set(registry_by_key) | manifest_family_keys(manifest_by_key) | manifest_family_keys(runtime_manifest_by_key))
    sheet_capacity_by_id = {plan.sheet_id: plan.capacity for plan in plans}
    used_count_by_sheet: dict[str, int] = defaultdict(int)
    for entry in registry_by_key.values():
        sheet_id = str(entry.get("sheetId", "")).strip()
        if sheet_id:
            used_count_by_sheet[sheet_id] += 1

    family_entries: dict[str, list[dict[str, Any]]] = defaultdict(list)
    family_sheet_ids: dict[str, set[str]] = defaultdict(set)
    for key in expected_keys:
        registry_entry = registry_by_key.get(key, {})
        family_match = family_for_key(key, registry_entry)
        if family_match is None:
            errors.append(f"final-full inventory key has no family rule: {key}.")
            continue
        family, default_consumer, default_test, default_owner_pr = family_match
        sheet_id = str(registry_entry.get("sheetId", "")).strip()
        if sheet_id:
            family_sheet_ids[family].add(sheet_id)
        key_entry: dict[str, Any] = {
            "key": key,
            "ownerPr": str(registry_entry.get("ownerPr", "")).strip() or default_owner_pr,
            "sheetId": sheet_id or "unregistered",
            "consumer": str(registry_entry.get("consumer", "")).strip() or default_consumer,
            "consumerTest": str(registry_entry.get("consumerTest", "")).strip() or default_test,
            "coverageExclusion": None,
            "historicalSheetIds": [sheet_id] if sheet_id else [],
        }
        family_entries[family].append(key_entry)

    source_digests = {
        repo_relative_path(path): sha256_file(path)
        for path in source_paths(args)
        if path.exists()
    }
    generated_from = sorted(source_digests)
    families: list[dict[str, Any]] = []
    for family in sorted(family_entries):
        keys = sorted(family_entries[family], key=lambda entry: entry["key"])
        family_sheet_set = family_sheet_ids.get(family, set())
        near_capacity_sheet_ids = sorted(
            sheet_id
            for sheet_id in family_sheet_set
            if sheet_capacity_by_id.get(sheet_id, 0) > 0
            and used_count_by_sheet.get(sheet_id, 0) / sheet_capacity_by_id[sheet_id] >= 0.8
        )
        family_payload: dict[str, Any] = {
            "family": family,
            "expectedCount": len(keys),
            "keys": keys,
        }
        if family_sheet_set:
            family_payload["historicalSheetIds"] = sorted(family_sheet_set)
        if near_capacity_sheet_ids:
            family_payload["nearCapacityWarn"] = True
            family_payload["nearCapacitySheetIds"] = near_capacity_sheet_ids
        families.append(family_payload)

    payload = {
        "schemaVersion": SCHEMA_VERSION,
        "schemaOwner": "PR-06",
        "generatedBy": GENERATOR_ID,
        "generatedFrom": generated_from,
        "sourceDigests": source_digests,
        "families": families,
    }
    return payload, errors


def main() -> int:
    args = parse_args()
    payload, errors = build_inventory(args)
    if errors:
        print("dark-final-full-inventory FAILED:")
        for error in errors:
            print(f"- {error}")
        return 1
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    key_count = sum(family["expectedCount"] for family in payload["families"])
    print(f"dark-final-full-inventory OK: expectedKeys={key_count}, out={args.out.as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
