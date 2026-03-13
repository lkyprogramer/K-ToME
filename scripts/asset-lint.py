#!/usr/bin/env python3
"""Lint the Phase 2 image asset plan."""

from __future__ import annotations

import argparse
import pathlib
import re
import sys
from collections import Counter

from asset_pipeline_common import (
    ALLOWED_CATEGORIES,
    DISALLOWED_ASSET_FIELDS,
    EXPECTED_STYLE_TAG,
    PHASE2_REQUIRED_VISUAL_KEYS,
    REQUIRED_GATES,
    canonical_gate_prefix,
    collect_assets,
    grouped_assets,
    load_yaml,
    normalize_list,
    print_errors,
)


ASSET_ID_RE = re.compile(r"^phase2_[a-z0-9_]+$")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Lint the Phase 2 image asset plan")
    parser.add_argument(
        "--plan",
        default="assets-src/image/specs/phase2-asset-plan.yaml",
        help="Path to the phase2 image asset plan YAML",
    )
    return parser.parse_args()


def validate_plan(plan_path: pathlib.Path) -> list[str]:
    plan = load_yaml(plan_path)
    errors: list[str] = []

    if str(plan.get("styleTag", "")).strip() != EXPECTED_STYLE_TAG:
        errors.append(
            f"styleTag must be '{EXPECTED_STYLE_TAG}', got '{plan.get('styleTag', '')}'."
        )
    if not str(plan.get("artStyleBible", "")).strip():
        errors.append("artStyleBible is required.")
    if not str(plan.get("stylePrompt", "")).strip():
        errors.append("stylePrompt is required.")

    defaults = plan.get("defaults")
    if not isinstance(defaults, dict):
        errors.append("defaults must be a mapping.")
    else:
        if not str(defaults.get("imageAspectRatio", "")).strip():
            errors.append("defaults.imageAspectRatio is required.")
        if not str(defaults.get("imageSize", "")).strip():
            errors.append("defaults.imageSize is required.")
        if not normalize_list(defaults.get("negativeConstraints")):
            errors.append("defaults.negativeConstraints must be a non-empty list.")

    grouped = grouped_assets(plan)
    missing_gates = [gate for gate in REQUIRED_GATES if gate not in grouped]
    extra_gates = [gate for gate in grouped if gate not in REQUIRED_GATES]
    if missing_gates:
        errors.append(f"Missing required phase2AssetGates: {', '.join(missing_gates)}.")
    if extra_gates:
        errors.append(
            "Phase 2 asset plan may only define P2-B and P2-C gates; "
            f"unexpected gates: {', '.join(sorted(extra_gates))}."
        )

    assets = collect_assets(plan)
    if not assets:
        errors.append("phase2AssetGates must contain at least one asset.")
        return errors

    id_counter = Counter()
    visual_key_counter = Counter()
    output_name_counter = Counter()

    for asset in assets:
        gate_id = asset["_gateId"]
        asset_id = str(asset.get("id", "")).strip()
        category = str(asset.get("category", "")).strip()
        visual_key = str(asset.get("visualKey", "")).strip()
        output_name = str(asset.get("outputName", "")).strip()
        subject = str(asset.get("subject", "")).strip()
        constraints = normalize_list(asset.get("constraints"))
        material_tags = normalize_list(asset.get("materialTags"))
        mood_tags = normalize_list(asset.get("moodTags"))

        id_counter[asset_id] += 1
        visual_key_counter[visual_key] += 1
        output_name_counter[output_name] += 1

        if not ASSET_ID_RE.match(asset_id):
            errors.append(f"[{gate_id}] invalid asset id '{asset_id}'.")
        if category not in ALLOWED_CATEGORIES:
            errors.append(f"[{gate_id}] asset '{asset_id}' has unsupported category '{category}'.")
        if "." not in visual_key:
            errors.append(f"[{gate_id}] asset '{asset_id}' visualKey must be dot-delimited.")
        if not output_name.endswith(".png"):
            errors.append(f"[{gate_id}] asset '{asset_id}' outputName must end with .png.")
        if output_name.startswith("/") or output_name.startswith("\\"):
            errors.append(f"[{gate_id}] asset '{asset_id}' outputName must be a relative path.")
        expected_prefix = canonical_gate_prefix(gate_id)
        if output_name and not output_name.startswith(expected_prefix):
            errors.append(
                f"[{gate_id}] asset '{asset_id}' outputName must start with '{expected_prefix}'."
            )
        if not subject:
            errors.append(f"[{gate_id}] asset '{asset_id}' subject is required.")
        if not constraints:
            errors.append(f"[{gate_id}] asset '{asset_id}' must define at least one constraint.")
        if not material_tags and not mood_tags:
            errors.append(
                f"[{gate_id}] asset '{asset_id}' must define materialTags or moodTags."
            )
        if asset.get("geminiKeyRequired") is not True:
            errors.append(f"[{gate_id}] asset '{asset_id}' must set geminiKeyRequired: true.")
        if "styleTag" in asset or "stylePrompt" in asset or "artStyleBible" in asset:
            errors.append(
                f"[{gate_id}] asset '{asset_id}' may not override styleTag/stylePrompt/artStyleBible."
            )
        for field_name in DISALLOWED_ASSET_FIELDS:
            if field_name in asset:
                errors.append(
                    f"[{gate_id}] asset '{asset_id}' may not define '{field_name}' in Phase 2."
                )

        if category.startswith("tile_") or category.startswith("prop_"):
            if not str(asset.get("biome", "")).strip():
                errors.append(f"[{gate_id}] asset '{asset_id}' must define biome.")
        if category == "actor_sprite":
            if not str(asset.get("profession", "")).strip() and not str(
                asset.get("faction", "")
            ).strip():
                errors.append(
                    f"[{gate_id}] actor asset '{asset_id}' must define profession or faction."
                )
        if category == "portrait":
            if not str(asset.get("profession", "")).strip() and not str(
                asset.get("faction", "")
            ).strip():
                errors.append(
                    f"[{gate_id}] portrait asset '{asset_id}' must define profession or faction."
                )
        if category == "icon_skill" and not str(asset.get("profession", "")).strip():
            errors.append(f"[{gate_id}] skill icon '{asset_id}' must define profession.")

    for key, count in id_counter.items():
        if key and count > 1:
            errors.append(f"Duplicate asset id: '{key}'.")
    for key, count in visual_key_counter.items():
        if key and count > 1:
            errors.append(f"Duplicate visualKey: '{key}'.")
    for key, count in output_name_counter.items():
        if key and count > 1:
            errors.append(f"Duplicate outputName: '{key}'.")

    for gate_id in REQUIRED_GATES:
        gate_assets = grouped.get(gate_id, [])
        if gate_assets and len(gate_assets) < 20:
            errors.append(f"[{gate_id}] must contain at least 20 assets, got {len(gate_assets)}.")
        gate_visual_keys = {str(asset.get("visualKey", "")).strip() for asset in gate_assets}
        missing_visual_keys = sorted(PHASE2_REQUIRED_VISUAL_KEYS[gate_id] - gate_visual_keys)
        if missing_visual_keys:
            errors.append(
                f"[{gate_id}] missing required visualKeys: {', '.join(missing_visual_keys)}."
            )

    return errors


def main() -> int:
    args = parse_args()
    plan_path = pathlib.Path(args.plan)
    errors = validate_plan(plan_path)
    if errors:
        return print_errors(errors)

    plan = load_yaml(plan_path)
    grouped = grouped_assets(plan)
    counts = {gate_id: len(assets) for gate_id, assets in sorted(grouped.items())}
    total = sum(counts.values())
    print(f"asset-lint OK: total={total}, gates={counts}, plan={plan_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
