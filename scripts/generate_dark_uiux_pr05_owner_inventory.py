#!/usr/bin/env python3
"""Materialize the PR-05 dark UI/UX owner key inventory."""

from __future__ import annotations

import argparse
import json
import pathlib
from dataclasses import dataclass
from typing import Any

from asset_pipeline_common import load_json, load_yaml


SCHEMA_VERSION = "dark-uiux-pr05-owner-key-inventory-v1"
OWNER_PR = "PR-05"
REQUIRED_OWNER_SHEET_IDS = [
    "r02-tiles-ground",
    "r02-tiles-wall",
    "r02-tiles-decal",
    "r03-props-interactable",
    "r03-props-environment",
    "r03-vfx-telegraph",
    "r04-actors-player",
    "r04-actors-humanoid",
    "r04-actors-monster",
    "r04-actors-boss",
    "r05-bestiary-humanoid-icons",
    "r05-bestiary-creature-icons",
    "r05-boss-icons",
    "r06-portraits-classes",
    "r06-portraits-trees",
    "r06-portraits-zones",
]

EXCLUDED_UPSTREAM_KEYS = {
    "tileset.ruins.ground_01",
    "tileset.ruins.wall_01",
    "actor.vanguard",
    "prop.stairs.down",
}

SHEET_CONSUMER_TESTS = {
    "r02-tiles-ground": "ManifestResolveTest.darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries",
    "r02-tiles-wall": "ManifestResolveTest.darkUiuxPr05TilesetOwnerKeysResolveThroughExactEntries",
    "r02-tiles-decal": "TileLayerComposerTest.composesTerrainPropsVfxTelegraphBeforeActors",
    "r03-props-interactable": "TileRendererCanvasTest.rendersPr05InteractablePropsWithDarkManifestEntries",
    "r03-props-environment": "TileRendererCanvasTest.rendersPr05InteractablePropsWithDarkManifestEntries",
    "r03-vfx-telegraph": "TileLayerComposerTest.keepsBossTelegraphAboveOrdinaryVfx",
    "r04-actors-player": "TileRendererCanvasTest.keepsPr05ActorSpritesReadableOnDarkMap",
    "r04-actors-humanoid": "TileRendererCanvasTest.keepsPr05ActorSpritesReadableOnDarkMap",
    "r04-actors-monster": "TileRendererCanvasTest.keepsPr05ActorSpritesReadableOnDarkMap",
    "r04-actors-boss": "TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell",
    "r05-bestiary-humanoid-icons": "ManifestResolveTest.darkUiuxPr05BestiaryIconsResolveThroughExactEntries",
    "r05-bestiary-creature-icons": "ManifestResolveTest.darkUiuxPr05BestiaryIconsResolveThroughExactEntries",
    "r05-boss-icons": "ManifestResolveTest.darkUiuxPr05BestiaryIconsResolveThroughExactEntries",
    "r06-portraits-classes": "ManifestResolveTest.darkUiuxPr05PortraitKeysResolveThroughExactEntries",
    "r06-portraits-trees": "ManifestResolveTest.darkUiuxPr05PortraitKeysResolveThroughExactEntries",
    "r06-portraits-zones": "ManifestResolveTest.darkUiuxPr05ZoneVisualKeysResolveThroughExactEntries",
}


@dataclass(frozen=True)
class SourceRule:
    sheet_id: str
    keys: tuple[str, ...] = ()
    prefixes: tuple[str, ...] = ()
    category: str | None = None
    consumer: str = ""
    source_decision: str = ""


SOURCE_RULES = [
    SourceRule(
        sheet_id="r02-tiles-ground",
        keys=("tileset.forest_edge.ground_01", "tileset.mine.ground_01", "tileset.shadow_depths.ground_01"),
        category="tile_ground",
        consumer="FoundationGameSession.terrainVisualKey -> TileRenderModelBuilder",
        source_decision="runtime terrain key emitted as ${zone.tilesetKey}.ground_01; ruins remains PR-02-2",
    ),
    SourceRule(
        sheet_id="r02-tiles-wall",
        keys=("tileset.forest_edge.wall_01", "tileset.mine.wall_01", "tileset.shadow_depths.wall_01"),
        category="tile_wall",
        consumer="FoundationGameSession.terrainVisualKey -> TileRenderModelBuilder",
        source_decision="runtime terrain key emitted as ${zone.tilesetKey}.wall_01; ruins remains PR-02-2",
    ),
    SourceRule(
        sheet_id="r02-tiles-decal",
        keys=(
            "vfx.terrain.interaction.ice",
            "vfx.terrain.interaction.oil",
            "vfx.terrain.interaction.oil_burning",
            "vfx.terrain.interaction.water",
        ),
        category="tile_decal",
        consumer="FoundationGameSession.terrainVisualKey -> overlay terrain renderer",
        source_decision="terrain interaction keys are emitted as map decals and remain tile_decal",
    ),
    SourceRule(
        sheet_id="r03-props-interactable",
        keys=(
            "prop.alarm_bonfire",
            "prop.armory_gate",
            "prop.crystal_resonance_node",
            "prop.heart_ward_focus",
            "prop.hidden_entrance.return_bridge",
            "prop.hidden_entrance.revealed",
            "prop.ritual_altar",
            "prop.river_ferry_anchor",
            "prop.stairs.up",
            "prop.supply_crate",
            "prop.temple_ward_reliquary",
        ),
        category="prop_interactable",
        consumer="map prop visual placements",
        source_decision="interactable map prop keys from canonical manifest; prop.stairs.down remains PR-02-2",
    ),
    SourceRule(
        sheet_id="r03-props-environment",
        keys=("prop.mine_furnace",),
        category="prop_environment",
        consumer="map prop visual placements",
        source_decision="environmental map prop key from canonical manifest",
    ),
    SourceRule(
        sheet_id="r03-vfx-telegraph",
        keys=(
            "vfx.boss.warning.sigil_01",
            "vfx.telegraph.warning.sigil_01",
            "vfx.boss.variant.abyssal_eclipse",
            "vfx.boss.variant.grey_crown",
            "vfx.boss.variant.molten_glass",
        ),
        category="vfx_plate",
        consumer="overlay visual placements and boss telegraph renderer",
        source_decision="boss and telegraph warning keys are PR-05 vfx_plate overlays",
    ),
    SourceRule(
        sheet_id="r03-vfx-telegraph",
        keys=(
            "vfx.zone.effect.crystal_shard_01",
            "vfx.zone.effect.current_lane_01",
            "vfx.zone.effect.void_pressure_01",
            "vfx.zone.effect.ward_seal_01",
        ),
        category="tile_decal",
        consumer="zone effect overlay visual placements",
        source_decision="zone effect keys remain tile_decal map plates",
    ),
    SourceRule(
        sheet_id="r04-actors-player",
        keys=("actor.player", "actor.arcanist", "actor.rogue", "actor.templar"),
        category="actor_sprite",
        consumer="snapshot.actors[].visualKey",
        source_decision="release playable actors; actor.vanguard remains PR-02-2",
    ),
    SourceRule(
        sheet_id="r04-actors-humanoid",
        prefixes=("actor.bandit.", "actor.cultist.", "actor.orc.", "actor.warded_ruin."),
        category="actor_sprite",
        consumer="actor visual placements",
        source_decision="expanded humanoid actor family prefixes from canonical manifest",
    ),
    SourceRule(
        sheet_id="r04-actors-monster",
        prefixes=("actor.beast.", "actor.undead.", "actor.abyssal.", "actor.crystal.", "actor.forge.", "actor.river."),
        category="actor_sprite",
        consumer="actor visual placements",
        source_decision="expanded monster actor family prefixes from canonical manifest",
    ),
    SourceRule(
        sheet_id="r04-actors-boss",
        keys=(
            "actor.boss.ashgate_warden",
            "boss.abyssal.guardian.visual",
            "boss.cultist.dungeon_lord.visual",
            "boss.orc.molten_giant.visual",
        ),
        category="actor_sprite",
        consumer="boss actor visual placements and boss data visualKey",
        source_decision="explicit boss actor and boss visual keys from PR-05 contract",
    ),
    SourceRule(
        sheet_id="r05-bestiary-humanoid-icons",
        prefixes=("icon.monster.bandit.", "icon.monster.cultist.", "icon.monster.orc.", "icon.monster.warded_ruin."),
        category="icon",
        consumer="bestiary / codex icon resolver",
        source_decision="expanded humanoid bestiary icon prefixes from canonical manifest",
    ),
    SourceRule(
        sheet_id="r05-bestiary-creature-icons",
        prefixes=(
            "icon.monster.beast.",
            "icon.monster.undead.",
            "icon.monster.abyssal.",
            "icon.monster.crystal.",
            "icon.monster.forge.",
            "icon.monster.river.",
        ),
        category="icon",
        consumer="bestiary / codex icon resolver",
        source_decision="expanded creature bestiary icon prefixes from canonical manifest",
    ),
    SourceRule(
        sheet_id="r05-boss-icons",
        keys=("boss.abyssal.guardian.icon", "boss.cultist.dungeon_lord.icon", "boss.orc.molten_giant.icon"),
        category="icon",
        consumer="boss / bestiary icon resolver and boss data iconKey",
        source_decision="explicit boss icon keys from PR-05 contract",
    ),
    SourceRule(
        sheet_id="r06-portraits-classes",
        keys=("portrait.arcanist", "portrait.rogue", "portrait.templar", "portrait.vanguard"),
        category="portrait",
        consumer="profession / class portrait UI",
        source_decision="release profession portrait keys",
    ),
    SourceRule(
        sheet_id="r06-portraits-trees",
        prefixes=("tree.",),
        category="portrait",
        consumer="talent tree UI",
        source_decision="expanded current canonical tree portrait keys",
    ),
    SourceRule(
        sheet_id="r06-portraits-zones",
        prefixes=("zone.",),
        category=None,
        consumer="zone / route / hidden content UI",
        source_decision="expanded zone visual keys; category audit keeps current canonical category unless PR-05 explicitly changes it",
    ),
]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate the PR-05 dark UI/UX owner inventory.")
    parser.add_argument("--manifest", type=pathlib.Path, default=pathlib.Path("assets-src/image/manifests/phase2-visual-manifest.json"))
    parser.add_argument("--sheet-plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--key-registry", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/key-registry.yaml"))
    parser.add_argument("--out", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/pr05-owner-key-inventory.json"))
    parser.add_argument("--summary", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/pr05-owner-key-inventory.md"))
    return parser.parse_args()


def manifest_entries(path: pathlib.Path) -> dict[str, dict[str, Any]]:
    payload = load_json(path)
    entries = payload.get("entries")
    if not isinstance(entries, list):
        raise ValueError(f"manifest entries must be a list: {path}")
    return {str(entry["key"]): entry for entry in entries if isinstance(entry, dict) and "key" in entry}


def load_existing_owner_keys(sheet_plan_path: pathlib.Path, key_registry_path: pathlib.Path) -> set[str]:
    owner_keys: set[str] = set()
    # Existing upstream ownership is represented by the key registry. The sheet
    # plan can already contain PR-05 cells on regeneration, so it must not be
    # treated as an exclusion source.
    load_yaml(sheet_plan_path)
    registry_payload = load_yaml(key_registry_path)
    for entry in registry_payload.get("entries", []) or []:
        if not isinstance(entry, dict):
            continue
        if str(entry.get("ownerPr", "")).strip() in {"PR-02", "PR-02-1", "PR-02-2", "PR-03"}:
            target_key = str(entry.get("targetKey", "")).strip()
            if target_key:
                owner_keys.add(target_key)
    return owner_keys


def keys_for_rule(rule: SourceRule, entries_by_key: dict[str, dict[str, Any]]) -> list[str]:
    explicit = [key for key in rule.keys if key in entries_by_key]
    prefixed = [
        key
        for key in sorted(entries_by_key)
        if any(key.startswith(prefix) for prefix in rule.prefixes)
    ]
    if rule.sheet_id == "r06-portraits-zones":
        prefixed = [key for key in prefixed if key.endswith(".visual")]
    return sorted(dict.fromkeys(explicit + prefixed))


def sanitize_output_name(target_key: str) -> str:
    return target_key.replace(".", "_").replace("-", "_")


def output_name(sheet_id: str, category: str, target_key: str) -> str:
    stem = sanitize_output_name(target_key)
    if sheet_id.startswith("r02-tiles"):
        directory = "tiles"
    elif sheet_id.startswith("r03-props"):
        directory = "props"
    elif sheet_id == "r03-vfx-telegraph":
        directory = "vfx"
    elif sheet_id.startswith("r04-actors"):
        directory = "actors"
    elif sheet_id.startswith("r05-"):
        directory = "icons"
    elif sheet_id.startswith("r06-"):
        directory = "portraits"
    else:
        directory = "misc"
    return f"dark-v1/{directory}/{stem}.png"


def is_dark_runtime_output(entry: dict[str, Any] | None) -> bool:
    if entry is None:
        return False
    return str(entry.get("rawOutputPath", "")).strip().startswith("dark-v1/")


def category_for(rule: SourceRule, target_key: str, entries_by_key: dict[str, dict[str, Any]]) -> str:
    if rule.category is not None:
        return rule.category
    return str(entries_by_key[target_key].get("category", "")).strip()


def materialize_entries(
    entries_by_key: dict[str, dict[str, Any]],
    existing_owner_keys: set[str],
) -> list[dict[str, Any]]:
    result: list[dict[str, Any]] = []
    occupied_keys: set[str] = set()
    next_slot_by_sheet: dict[str, int] = {}
    for rule in SOURCE_RULES:
        columns = 4 if rule.sheet_id.startswith("r06-") or rule.sheet_id in {"r04-actors-player", "r04-actors-boss"} else 8
        next_slot = next_slot_by_sheet.get(rule.sheet_id, 0)
        for target_key in keys_for_rule(rule, entries_by_key):
            if target_key in EXCLUDED_UPSTREAM_KEYS or target_key in existing_owner_keys:
                continue
            if target_key in occupied_keys:
                continue
            row = next_slot // columns
            col = next_slot % columns
            category = category_for(rule, target_key, entries_by_key)
            result.append(
                {
                    "targetKey": target_key,
                    "sheetId": rule.sheet_id,
                    "row": row,
                    "col": col,
                    "category": category,
                    "outputName": output_name(rule.sheet_id, category, target_key),
                    "fallbackKey": "missing_visual",
                    "consumer": rule.consumer,
                    "consumerTest": SHEET_CONSUMER_TESTS[rule.sheet_id],
                    "aliasOf": None,
                    "playerVisible": True,
                    "ownerPr": OWNER_PR,
                    "sourceManifestPath": "assets-src/image/manifests/phase2-visual-manifest.json",
                    "sourceDecision": rule.source_decision,
                }
            )
            occupied_keys.add(target_key)
            next_slot += 1
        next_slot_by_sheet[rule.sheet_id] = next_slot
    return result


def validate_inventory(entries: list[dict[str, Any]]) -> list[str]:
    errors: list[str] = []
    by_sheet = {sheet_id: [] for sheet_id in REQUIRED_OWNER_SHEET_IDS}
    for entry in entries:
        sheet_id = entry["sheetId"]
        if sheet_id not in by_sheet:
            errors.append(f"{entry['targetKey']} uses unknown PR-05 sheetId {sheet_id}.")
        else:
            by_sheet[sheet_id].append(entry)
    for sheet_id, sheet_entries in by_sheet.items():
        if not sheet_entries:
            errors.append(f"{sheet_id} has no PR-05 owner keys.")
    for sheet_id in ("r04-actors-player", "r04-actors-boss", "r06-portraits-classes", "r06-portraits-trees", "r06-portraits-zones"):
        if len(by_sheet[sheet_id]) > 16:
            errors.append(f"{sheet_id} exceeds large-sheet capacity: {len(by_sheet[sheet_id])} > 16.")
    for sheet_id, sheet_entries in by_sheet.items():
        if sheet_id not in {"r04-actors-player", "r04-actors-boss", "r06-portraits-classes", "r06-portraits-trees", "r06-portraits-zones"}:
            if len(sheet_entries) > 64:
                errors.append(f"{sheet_id} exceeds icon/tile-sheet capacity: {len(sheet_entries)} > 64.")
    return errors


def write_summary(path: pathlib.Path, payload: dict[str, Any]) -> None:
    lines = [
        "# PR-05 Dark UI/UX Owner Key Inventory",
        "",
        f"- schemaVersion: `{payload['schemaVersion']}`",
        f"- ownerPr: `{payload['ownerPr']}`",
        f"- requiredOwnerSheetCount: `{payload['requiredOwnerSheetCount']}`",
        f"- ownerKeyCount: `{len(payload['entries'])}`",
        f"- closeGate: `{len(payload['ownerCoveredKeys'])}/{len(payload['ownerExpectedKeys'])}`",
        f"- allowedOwnerFallbackKeys: `{len(payload['allowedOwnerFallbackKeys'])}`",
        f"- oldStyleOwnerKeys: `{len(payload['oldStyleOwnerKeys'])}`",
        f"- pendingOwnerKeys: `{len(payload['pendingOwnerKeys'])}`",
        "",
        "## Sheet Summary",
        "",
        "| sheetId | keyCount | categories |",
        "| --- | ---: | --- |",
    ]
    for sheet_id in payload["requiredOwnerSheetIds"]:
        sheet_entries = [entry for entry in payload["entries"] if entry["sheetId"] == sheet_id]
        categories = ", ".join(sorted({entry["category"] for entry in sheet_entries}))
        lines.append(f"| `{sheet_id}` | {len(sheet_entries)} | `{categories}` |")
    lines.extend(["", "## Entries", "", "| sheetId | row | col | targetKey | category | outputName |", "| --- | ---: | ---: | --- | --- | --- |"])
    for entry in payload["entries"]:
        lines.append(
            f"| `{entry['sheetId']}` | {entry['row']} | {entry['col']} | "
            f"`{entry['targetKey']}` | `{entry['category']}` | `{entry['outputName']}` |"
        )
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    entries_by_key = manifest_entries(args.manifest)
    existing_owner_keys = load_existing_owner_keys(args.sheet_plan, args.key_registry)
    entries = materialize_entries(entries_by_key, existing_owner_keys)
    errors = validate_inventory(entries)
    owner_expected_keys = sorted(entry["targetKey"] for entry in entries)
    owner_covered_keys = sorted(
        key
        for key in owner_expected_keys
        if is_dark_runtime_output(entries_by_key.get(key))
    )
    old_style_owner_keys = sorted(set(owner_expected_keys) - set(owner_covered_keys))
    pending_owner_keys: list[str] = []
    if old_style_owner_keys:
        errors.append(f"PR-05 owner keys without dark-v1 output: {', '.join(old_style_owner_keys)}.")
    if errors:
        for error in errors:
            print(f"[pr05-owner-inventory] {error}")
        return 1
    payload = {
        "schemaVersion": SCHEMA_VERSION,
        "ownerPr": OWNER_PR,
        "generatedFrom": [
            args.manifest.as_posix(),
            args.sheet_plan.as_posix(),
            args.key_registry.as_posix(),
        ],
        "requiredOwnerSheetCount": len(REQUIRED_OWNER_SHEET_IDS),
        "requiredOwnerSheetIds": REQUIRED_OWNER_SHEET_IDS,
        "ownerExpectedKeys": owner_expected_keys,
        "ownerCoveredKeys": owner_covered_keys,
        "allowedOwnerFallbackKeys": [],
        "oldStyleOwnerKeys": old_style_owner_keys,
        "pendingOwnerKeys": pending_owner_keys,
        "entries": entries,
    }
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(payload, indent=2, sort_keys=False) + "\n", encoding="utf-8")
    write_summary(args.summary, payload)
    print(f"pr05-owner-inventory OK: entries={len(entries)}, out={args.out.as_posix()}, summary={args.summary.as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
