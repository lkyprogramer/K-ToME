#!/usr/bin/env python3
"""Lint the primary art style contract plus optional extra Gemini plans."""

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
    parser.add_argument(
        "--extra-plan",
        action="append",
        default=[],
        help="Additional Gemini image plan YAML paths that must also be covered by style lint.",
    )
    return parser.parse_args()


def validate_single_style(plan: dict, label: str, errors: list[str]) -> None:
    style_tag = str(plan.get("styleTag", "")).strip()
    if style_tag != EXPECTED_STYLE_TAG:
        errors.append(
            f"{label}: styleTag must be '{EXPECTED_STYLE_TAG}', got '{style_tag or '<empty>'}'."
        )

    style_prompt = str(plan.get("stylePrompt", "")).strip()
    if not style_prompt:
        errors.append(f"{label}: stylePrompt is required.")
    else:
        lowered_prompt = style_prompt.lower()
        for token in REQUIRED_STYLE_PROMPT_TOKENS:
            if token not in lowered_prompt:
                errors.append(f"{label}: stylePrompt must contain '{token}'.")

    defaults = plan.get("defaults")
    if not isinstance(defaults, dict):
        errors.append(f"{label}: defaults must be a mapping.")
    else:
        negative_constraints = normalize_list(defaults.get("negativeConstraints"))
        lowered_constraints = {item.lower() for item in negative_constraints}
        for token in REQUIRED_DEFAULT_NEGATIVE_CONSTRAINTS:
            if token.lower() not in lowered_constraints:
                errors.append(f"{label}: defaults.negativeConstraints must contain '{token}'.")

    art_style_bible = str(plan.get("artStyleBible", "")).strip()
    if not art_style_bible:
        errors.append(f"{label}: artStyleBible is required.")
        return

    bible_path = pathlib.Path(art_style_bible)
    if not bible_path.is_file():
        errors.append(f"{label}: artStyleBible file does not exist: {art_style_bible}.")
    else:
        bible_text = bible_path.read_text(encoding="utf-8")
        for token in REQUIRED_STYLE_BIBLE_TOKENS:
            if token not in bible_text:
                errors.append(f"{label}: artStyleBible must contain '{token}'.")

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


def validate_style(plan_paths: list[pathlib.Path]) -> list[str]:
    errors: list[str] = []
    for plan_path in plan_paths:
        validate_single_style(load_yaml(plan_path), str(plan_path), errors)
    return errors


def main() -> int:
    args = parse_args()
    plan_path = pathlib.Path(args.plan)
    plan_paths = [plan_path, *[pathlib.Path(path) for path in args.extra_plan]]
    errors = validate_style(plan_paths)
    if errors:
        return print_errors(errors)

    plan = load_yaml(plan_path)
    style_tag = plan.get("styleTag")
    asset_count = sum(len(collect_assets(load_yaml(path))) for path in plan_paths)
    print(f"style-lint OK: styleTag={style_tag}, assets={asset_count}, plans={plan_paths}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
