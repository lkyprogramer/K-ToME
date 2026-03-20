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
        "tileset.ruins.ground_01",
        "tileset.ruins.wall_01",
        "actor.vanguard",
        "actor.arcanist",
        "portrait.vanguard",
        "portrait.arcanist",
        "actor.bandit.captain",
        "item.basic_shield.icon",
        "item.arcane_staff.icon",
        "item.apprentice_robe.icon",
        "talent.vanguard.power_strike.icon",
        "talent.arcanist.fireball.icon",
        "vfx.boss.warning.sigil_01",
    },
    "P2-C": {
        "actor.rogue",
        "actor.templar",
        "portrait.rogue",
        "portrait.templar",
        "tileset.forest_edge.ground_01",
        "tileset.forest_edge.wall_01",
        "tileset.mine.ground_01",
        "tileset.mine.wall_01",
        "tileset.shadow_depths.ground_01",
        "tileset.shadow_depths.wall_01",
        "icon.skill.rogue.shadowstep",
        "icon.skill.templar.divine_intervention",
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
