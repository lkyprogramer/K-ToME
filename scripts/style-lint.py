#!/usr/bin/env python3
"""Lint the Phase 2 art style contract and prompt bindings."""

from __future__ import annotations

import argparse
import pathlib
import sys

from asset_pipeline_common import (
    EXPECTED_STYLE_TAG,
    FORBIDDEN_STYLE_TOKENS,
    REQUIRED_DEFAULT_NEGATIVE_CONSTRAINTS,
    collect_assets,
    load_yaml,
    normalize_list,
    print_errors,
)


REQUIRED_STYLE_PROMPT_TOKENS = (
    "high fantasy",
    "middle-earth inspired",
    "painterly",
    "never anime",
    "never sci-fi",
)
REQUIRED_STYLE_BIBLE_TOKENS = (
    "K-ToME 美术风格圣经",
    "ktome-middle-fantasy-painterly-tile-v1",
    "中土",
    "高幻想",
    "48x48",
    "所有图片生成",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Lint the Phase 2 art style bindings")
    parser.add_argument(
        "--plan",
        default="assets-src/image/specs/phase2-asset-plan.yaml",
        help="Path to the phase2 image asset plan YAML",
    )
    return parser.parse_args()


def validate_style(plan_path: pathlib.Path) -> list[str]:
    plan = load_yaml(plan_path)
    errors: list[str] = []

    style_tag = str(plan.get("styleTag", "")).strip()
    if style_tag != EXPECTED_STYLE_TAG:
        errors.append(
            f"styleTag must be '{EXPECTED_STYLE_TAG}', got '{style_tag or '<empty>'}'."
        )

    style_prompt = str(plan.get("stylePrompt", "")).strip()
    if not style_prompt:
        errors.append("stylePrompt is required.")
    else:
        lowered_prompt = style_prompt.lower()
        for token in REQUIRED_STYLE_PROMPT_TOKENS:
            if token not in lowered_prompt:
                errors.append(f"stylePrompt must contain '{token}'.")

    defaults = plan.get("defaults")
    if not isinstance(defaults, dict):
        errors.append("defaults must be a mapping.")
    else:
        negative_constraints = normalize_list(defaults.get("negativeConstraints"))
        lowered_constraints = {item.lower() for item in negative_constraints}
        for token in REQUIRED_DEFAULT_NEGATIVE_CONSTRAINTS:
            if token.lower() not in lowered_constraints:
                errors.append(f"defaults.negativeConstraints must contain '{token}'.")

    art_style_bible = str(plan.get("artStyleBible", "")).strip()
    if not art_style_bible:
        errors.append("artStyleBible is required.")
        return errors

    bible_path = pathlib.Path(art_style_bible)
    if not bible_path.is_file():
        errors.append(f"artStyleBible file does not exist: {art_style_bible}.")
    else:
        bible_text = bible_path.read_text(encoding="utf-8")
        for token in REQUIRED_STYLE_BIBLE_TOKENS:
            if token not in bible_text:
                errors.append(f"artStyleBible must contain '{token}'.")

    for asset in collect_assets(plan):
        gate_id = asset["_gateId"]
        asset_id = str(asset.get("id", "")).strip()
        combined_text = "\n".join(
            [
                str(asset.get("subject", "")),
                "\n".join(normalize_list(asset.get("constraints"))),
                "\n".join(normalize_list(asset.get("materialTags"))),
                "\n".join(normalize_list(asset.get("moodTags"))),
            ]
        ).lower()
        for token in FORBIDDEN_STYLE_TOKENS:
            if token in combined_text:
                errors.append(
                    f"[{gate_id}] asset '{asset_id}' contains forbidden style token '{token}'."
                )
        if asset.get("geminiKeyRequired") is not True:
            errors.append(f"[{gate_id}] asset '{asset_id}' must set geminiKeyRequired: true.")
        if "styleTag" in asset or "artStyleBible" in asset:
            errors.append(f"[{gate_id}] asset '{asset_id}' may not override root style binding.")

    return errors


def main() -> int:
    args = parse_args()
    plan_path = pathlib.Path(args.plan)
    errors = validate_style(plan_path)
    if errors:
        return print_errors(errors)

    plan = load_yaml(plan_path)
    style_tag = plan.get("styleTag")
    asset_count = len(collect_assets(plan))
    print(f"style-lint OK: styleTag={style_tag}, assets={asset_count}, plan={plan_path}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
