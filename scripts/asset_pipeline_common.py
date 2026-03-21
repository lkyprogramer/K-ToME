#!/usr/bin/env python3
"""Shared helpers for the Phase 2 asset pipeline scripts."""

from __future__ import annotations

import json
import pathlib
from typing import Any

try:
    import yaml  # type: ignore
except Exception as exc:  # pragma: no cover
    raise RuntimeError("PyYAML is required for the Phase 2 asset pipeline scripts") from exc


EXPECTED_STYLE_TAG = "ktome-middle-fantasy-painterly-tile-v1"
REQUIRED_GATES = ("P2-B", "P2-C")
ALLOWED_CATEGORIES = {
    "tile_ground",
    "tile_wall",
    "tile_decal",
    "prop_interactable",
    "prop_environment",
    "actor_sprite",
    "portrait",
    "icon",
    "icon_skill",
    "icon_status",
    "icon_damage_type",
    "icon_item",
    "icon_quest",
    "ui_frame",
    "vfx_plate",
    "debug",
}
DISALLOWED_ASSET_FIELDS = {
    "provider",
    "providerName",
    "placeholderAllowed",
    "placeholderSurface",
    "generationStatus",
    "generationProvider",
}
REQUIRED_DEFAULT_NEGATIVE_CONSTRAINTS = (
    "no modern clothing",
    "no firearms",
    "no sci-fi materials",
    "no anime proportions",
    "no floating text, watermark or logo",
)
FORBIDDEN_STYLE_TOKENS = (
    "cyberpunk",
    "mecha",
    "spaceship",
    "laser rifle",
    "neon city",
    "anime idol",
    "futuristic ui",
)
EXPECTED_FOOTPRINT_BY_CATEGORY = {
    "tile_ground": "1x1",
    "tile_wall": "1x1",
    "tile_decal": "1x1",
    "prop_interactable": "1x1",
    "prop_environment": "1x1",
    "actor_sprite": "1x1",
    "portrait": "ui",
    "icon_skill": "ui",
    "icon_status": "ui",
    "icon_damage_type": "ui",
    "icon_item": "ui",
    "icon_quest": "ui",
    "ui_frame": "ui",
    "vfx_plate": "overlay",
}
PHASE2_REQUIRED_VISUAL_KEYS = {
    "P2-B": {
        "actor.vanguard",
        "actor.arcanist",
        "portrait.vanguard",
        "portrait.arcanist",
        "icon.profession.vanguard",
        "icon.profession.arcanist",
        "prop.armory_gate",
        "prop.supply_crate",
        "prop.alarm_bonfire",
        "prop.stairs.down",
        "prop.stairs.up",
        "icon.monster.bandit.captain",
        "icon.monster.bandit.sentry",
        "icon.monster.beast.rat",
        "icon.monster.undead.bone_archer",
        "icon.tree.vanguard_arms",
        "icon.tree.vanguard_shield",
        "icon.tree.vanguard_warcry",
        "icon.tree.arcanist_arcane",
        "icon.tree.arcanist_flame",
        "icon.tree.arcanist_frost",
        "tree.vanguard_arms",
        "tree.vanguard_shield",
        "tree.vanguard_warcry",
        "tree.arcanist_arcane",
        "tree.arcanist_flame",
        "tree.arcanist_frost",
        "item.basic_shield.icon",
        "item.healing_potion.icon",
        "item.mana_potion.icon",
        "item.arcane_staff.icon",
        "item.apprentice_robe.icon",
        "icon.quest.armory_key",
        "icon.skill.vanguard.power_strike",
        "icon.skill.arcanist.blink",
        "icon.damage_type.fire",
        "icon.damage_type.shadow",
        "vfx.boss.warning.sigil_01",
        "vfx.zone.effect.ward_seal_01",
    },
    "P2-C": {
        "actor.rogue",
        "actor.templar",
        "portrait.rogue",
        "portrait.templar",
        "icon.profession.rogue",
        "icon.profession.templar",
        "actor.boss.ashgate_warden",
        "actor.cultist.dungeon_lord",
        "boss.cultist.dungeon_lord.visual",
        "boss.cultist.dungeon_lord.icon",
        "zone.shattered_outpost.visual",
        "zone.shattered_outpost.icon",
        "zone.greenwood_fringe.visual",
        "zone.greenwood_fringe.icon",
        "zone.deep_iron_pit.visual",
        "zone.deep_iron_pit.icon",
        "zone.grey_gate_depths.visual",
        "zone.grey_gate_depths.icon",
        "prop.mine_furnace",
        "prop.ritual_altar",
        "item.battle_axe.icon",
        "item.leather_armor.icon",
        "item.long_sword.icon",
        "item.plate_armor.icon",
        "item.scroll_teleport.icon",
        "icon.monster.cultist.dungeon_lord",
        "icon.monster.orc.raider",
        "icon.quest.seal_key",
        "icon.tree.rogue_agility",
        "icon.tree.rogue_assassination",
        "icon.skill.rogue.shadowstep",
        "icon.tree.templar_grace",
        "icon.tree.templar_smite",
        "icon.skill.templar.divine_intervention",
        "tree.rogue_agility",
        "tree.rogue_assassination",
        "icon.tree.rogue_subtlety",
        "tree.templar_grace",
        "tree.templar_smite",
        "icon.tree.templar_faith",
        "tree.rogue_subtlety",
        "tree.templar_faith",
    },
}

PHASE2_REQUIRED_AUDIO_KEYS = {
    "P2-B": {
        "audio.profession.vanguard",
        "audio.profession.arcanist",
        "audio.tree.vanguard_arms",
        "audio.tree.arcanist_arcane",
        "ambient.shattered_outpost",
        "audio.zone.shattered_outpost",
        "audio.boss.warning",
        "audio.boss.bandit_captain",
        "audio.interactable.open",
        "audio.interactable.stairs",
        "audio.ui.confirm",
        "audio.ui.cancel",
        "audio.ui.hover",
        "audio.ui.level_up",
        "audio.ui.talent_unlock",
        "audio.objective.progress",
        "audio.route.transition",
        "audio.route.complete",
        "audio.talent.power_strike",
        "audio.talent.fireball",
        "audio.item.basic_shield",
        "audio.item.healing_potion",
        "audio.item.mana_potion",
        "audio.item.arcane_staff",
    },
    "P2-C": {
        "audio.profession.rogue",
        "audio.profession.templar",
        "audio.tree.rogue_subtlety",
        "audio.tree.templar_faith",
        "ambient.greenwood_fringe",
        "ambient.deep_iron_pit",
        "ambient.grey_gate_depths",
        "audio.zone.greenwood_fringe",
        "audio.zone.deep_iron_pit",
        "audio.zone.grey_gate_depths",
        "audio.boss.cultist.dungeon_lord",
        "audio.talent.shadowstep",
        "audio.talent.divine_intervention",
        "audio.item.battle_axe",
        "audio.item.leather_armor",
        "audio.item.long_sword",
        "audio.item.plate_armor",
        "audio.item.scroll_teleport",
    },
}


def load_yaml(path: pathlib.Path) -> dict[str, Any]:
    if not path.is_file():
        raise FileNotFoundError(f"YAML file not found: {path}")
    payload = yaml.safe_load(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"YAML root must be a mapping: {path}")
    return payload


def load_json(path: pathlib.Path) -> dict[str, Any]:
    if not path.is_file():
        raise FileNotFoundError(f"JSON file not found: {path}")
    payload = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(payload, dict):
        raise ValueError(f"JSON root must be a mapping: {path}")
    return payload


def normalize_list(value: Any) -> list[str]:
    if value is None:
        return []
    if isinstance(value, list):
        return [str(item).strip() for item in value if str(item).strip()]
    if isinstance(value, str) and value.strip():
        return [value.strip()]
    return []


def flatten_required_keys(required_keys_by_gate: dict[str, set[str]]) -> set[str]:
    flattened: set[str] = set()
    for keys in required_keys_by_gate.values():
        flattened |= set(keys)
    return flattened


def split_phase2_fallback_budget(
    entries_by_key: dict[str, dict[str, Any]],
    required_keys: set[str],
    path_field: str,
    fallback_value: str,
) -> tuple[list[str], list[str]]:
    required_fallback_keys: list[str] = []
    budget_fallback_keys: list[str] = []

    for key, entry in entries_by_key.items():
        tags = set(normalize_list(entry.get("tags")))
        if "phase2" not in tags:
            continue
        resolved_path = str(entry.get(path_field, "")).strip()
        if resolved_path != fallback_value:
            continue
        if key in required_keys:
            required_fallback_keys.append(key)
        else:
            budget_fallback_keys.append(key)

    return sorted(required_fallback_keys), sorted(budget_fallback_keys)


def collect_assets(plan: dict[str, Any]) -> list[dict[str, Any]]:
    gate_map = plan.get("phase2AssetGates")
    if not isinstance(gate_map, dict):
        raise ValueError("phase2AssetGates must be a mapping")

    assets: list[dict[str, Any]] = []
    for gate_id, gate_payload in gate_map.items():
        if not isinstance(gate_payload, dict):
            raise ValueError(f"Gate {gate_id} must be a mapping")
        gate_description = str(gate_payload.get("description", "")).strip()
        gate_assets = gate_payload.get("assets")
        if not isinstance(gate_assets, list):
            raise ValueError(f"Gate {gate_id} assets must be a list")
        for asset in gate_assets:
            if not isinstance(asset, dict):
                raise ValueError(f"Gate {gate_id} contains a non-mapping asset")
            enriched = dict(asset)
            enriched["_gateId"] = str(gate_id)
            enriched["_gateDescription"] = gate_description
            assets.append(enriched)
    return assets


def grouped_assets(plan: dict[str, Any]) -> dict[str, list[dict[str, Any]]]:
    grouped: dict[str, list[dict[str, Any]]] = {}
    for asset in collect_assets(plan):
        grouped.setdefault(asset["_gateId"], []).append(asset)
    return grouped


def canonical_gate_prefix(gate_id: str) -> str:
    return f"phase2/{gate_id.lower()}/"


def print_errors(errors: list[str]) -> int:
    if not errors:
        return 0
    for error in errors:
        print(f"ERROR: {error}")
    return 1
