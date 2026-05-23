#!/usr/bin/env python3
"""Generate deterministic random QA samples for dark-v1 sprite sheets."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
from types import SimpleNamespace
from typing import Any

from dark_sprite_sheet_contract import (
    DARK_CONTACT_SHEET_DIR,
    DARK_RAW_SHEET_DIR,
    load_sheet_plan,
    print_errors,
    repo_relative_error,
    sha256_file,
    write_json,
)


SCHEMA_VERSION = "dark-art-random-qa-v1"
DEFAULT_SEED = "dark-uiux-pr06-art-random-qa-v1"
DEFAULT_SHEET_IDS = (
    "r07-items-base",
    "r07-items-unique-artifact",
    "r07-items-affix-material",
    "r08-skills-vanguard-berserker",
    "r08-skills-templar-rogue",
    "r08-skills-arcanist-spellblade",
    "r09-status-damage",
    "r09-quest-zone-profession",
    "r09-fallback-debug",
    "r09-rejected-polish",
)
MANDATORY_KEYS = {
    "icon.skill.vanguard.power_strike",
    "icon.skill.vanguard.shield_bash",
    "icon.skill.vanguard.guard_stance",
    "icon.skill.templar.holy_light",
    "icon.skill.templar.holy_shield",
    "icon.status.guard_stance_buff",
    "icon.status.burn",
    "icon.status.cursed",
    "icon.damage_type.fire",
    "icon.damage_type.physical",
    "icon.quest.objective_marker",
    "icon.quest.armory_key",
    "icon.profession.vanguard",
    "icon.tree.vanguard_shield",
    "zone.shattered_outpost.icon",
    "item.long_sword.icon",
    "item.basic_shield.icon",
    "item.chain_mail.icon",
    "item.healing_potion.icon",
    "item.mana_potion.icon",
    "item.energy_tonic.icon",
    "item.scroll_teleport.icon",
    "item.consecrated_oil.icon",
    "item.unique.greenwood_watcher_blade.icon",
    "item.unique.furnace_plate.icon",
    "material.iron.icon",
    "material.steel.icon",
    "affix.sharp.icon",
    "affix.sturdy.icon",
    "ui.shop.price.affordable",
    "ui.shop.inscription.marker",
    "missing_visual",
    "tile.hidden",
}
FAIL_CRITERIA = (
    "subject unreadable at required size",
    "same-family icons collapse into a shared generic shape",
    "clean vector sticker look",
    "overbright neon or glassmorphism",
    "cross-cell bleed",
    "text, watermark, or label inside asset",
    "outline collapses below 32px",
)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate deterministic dark-v1 art random QA sample records.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument(
        "--prompt-index",
        type=pathlib.Path,
        default=pathlib.Path("UI/sprite-sheets/prompts/dark-v1/prompt-index.json"),
    )
    parser.add_argument("--seed", default=DEFAULT_SEED)
    parser.add_argument("--sheet-ids", default=",".join(DEFAULT_SHEET_IDS))
    parser.add_argument(
        "--out",
        type=pathlib.Path,
        default=pathlib.Path("UI/manual-records/dark-uiux-pr06-art-random-qa.json"),
    )
    parser.add_argument(
        "--sample-root",
        type=pathlib.Path,
        default=pathlib.Path("build/reports/verification/dark-uiux/random-qa"),
    )
    parser.add_argument("--raw-root", type=pathlib.Path, default=pathlib.Path(DARK_RAW_SHEET_DIR))
    parser.add_argument("--contact-root", type=pathlib.Path, default=pathlib.Path(DARK_CONTACT_SHEET_DIR))
    parser.add_argument("--skip-images", action="store_true")
    parser.add_argument("--overwrite", action="store_true")
    return parser.parse_args()


def parse_csv(value: str) -> list[str]:
    return [item.strip() for item in value.split(",") if item.strip()]


def repo_relative(path: pathlib.Path) -> str:
    try:
        return path.relative_to(pathlib.Path.cwd()).as_posix()
    except ValueError:
        return path.as_posix()


def load_prompt_paths(index_path: pathlib.Path) -> tuple[dict[str, str], list[str]]:
    if not index_path.is_file():
        return {}, [f"prompt index is missing: {index_path.as_posix()}."]
    try:
        payload = json.loads(index_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {}, [f"prompt index is not valid JSON: {exc}."]
    prompts = payload.get("prompts")
    if not isinstance(prompts, list):
        return {}, ["prompt index must contain a prompts list."]
    by_sheet_id: dict[str, str] = {}
    errors: list[str] = []
    for index, row in enumerate(prompts):
        if not isinstance(row, dict):
            errors.append(f"prompt-index.prompts[{index}] must be a mapping.")
            continue
        sheet_id = str(row.get("sheetId", "")).strip()
        prompt_path = str(row.get("promptPath", "")).strip()
        if not sheet_id or not prompt_path:
            errors.append(f"prompt-index.prompts[{index}] must define sheetId and promptPath.")
            continue
        path_error = repo_relative_error(prompt_path, "promptPath", f"prompt-index.prompts[{index}]")
        if path_error:
            errors.append(path_error)
        by_sheet_id[sheet_id] = prompt_path
    return by_sheet_id, errors


def cell_rect(sheet, cell) -> dict[str, int]:
    left = cell.col * sheet.grid["cellWidth"]
    top = cell.row * sheet.grid["cellHeight"]
    return {
        "x": left,
        "y": top,
        "width": sheet.grid["cellWidth"],
        "height": sheet.grid["cellHeight"],
    }


def sample_count(total: int) -> int:
    if total < 4:
        return total
    return min(12, max(4, math.ceil(total * 0.15)))


def stable_sample_rank(seed: str, sheet_id: str, target_key: str) -> str:
    payload = f"{seed}|{sheet_id}|{target_key}".encode("utf-8")
    return hashlib.sha256(payload).hexdigest()


def required_sizes(target_key: str, sheet_id: str) -> list[int]:
    if target_key.startswith("icon.profession."):
        return [128, 48, 24]
    if target_key.startswith("icon.skill.") or target_key.startswith("talent."):
        return [16, 24, 32, 48]
    if (
        target_key.startswith("icon.status.")
        or target_key.startswith("icon.damage_type.")
        or target_key.startswith("icon.mutation.")
    ):
        return [16, 24, 32]
    if (
        target_key.startswith("icon.quest.")
        or target_key.startswith("icon.tree.")
        or target_key.startswith("zone.")
        or target_key.startswith("difficulty.")
    ):
        return [12, 16, 24, 32]
    if target_key.startswith("item."):
        return [24, 32, 48, 64]
    if target_key.startswith("material.") or target_key.startswith("affix."):
        return [16, 24, 32, 48]
    if target_key.startswith("ui.shop."):
        return [16, 24, 32]
    if sheet_id == "r09-fallback-debug" or target_key in {"missing_visual", "tile.hidden"}:
        return [16, 24, 32, 48]
    if target_key.startswith("rejected.polish."):
        return [32, 48, 128]
    return [16, 24, 32]


def select_samples(seed: str, sheet_id: str, cells) -> list[dict[str, Any]]:
    ordered_cells = sorted(cells, key=lambda item: item.target_key)
    mandatory = [cell for cell in ordered_cells if cell.target_key in MANDATORY_KEYS]
    mandatory_keys = {cell.target_key for cell in mandatory}
    random_pool = [cell for cell in ordered_cells if cell.target_key not in mandatory_keys]
    random_cells = sorted(
        random_pool,
        key=lambda item: (stable_sample_rank(seed, sheet_id, item.target_key), item.target_key),
    )[: sample_count(len(ordered_cells))]
    selected: list[dict[str, Any]] = []
    for cell in mandatory:
        selected.append({"cell": cell, "selection": "mandatory"})
    for cell in random_cells:
        selected.append({"cell": cell, "selection": "random"})
    return selected


def render_sample_sheet(raw_path: pathlib.Path, output_path: pathlib.Path, samples: list[dict[str, Any]], sheet) -> str | None:
    try:
        from PIL import Image, ImageDraw
    except ModuleNotFoundError as exc:
        if exc.name == "PIL":
            return "Pillow is required to render sample sheets; rerun without --skip-images after installing PIL."
        raise

    with Image.open(raw_path).convert("RGBA") as raw_image:
        tile_width = 168
        tile_height = 210
        columns = 4
        rows = max(1, math.ceil(len(samples) / columns))
        canvas = Image.new("RGBA", (columns * tile_width, rows * tile_height), (12, 14, 18, 255))
        draw = ImageDraw.Draw(canvas)
        for index, sample in enumerate(samples):
            cell = sample["cell"]
            rect = cell_rect(sheet, cell)
            crop = raw_image.crop((rect["x"], rect["y"], rect["x"] + rect["width"], rect["y"] + rect["height"]))
            x = (index % columns) * tile_width
            y = (index // columns) * tile_height
            draw.text((x + 6, y + 6), f"{sample['selection']} {cell.row},{cell.col}", fill=(230, 232, 236, 255))
            draw.text((x + 6, y + 22), cell.target_key[:26], fill=(190, 196, 205, 255))
            offset_x = x + 8
            offset_y = y + 48
            for size in required_sizes(cell.target_key, sheet.sheet_id):
                preview = crop.resize((size, size), Image.Resampling.LANCZOS)
                canvas.alpha_composite(preview, (offset_x, offset_y))
                draw.rectangle((offset_x, offset_y, offset_x + size, offset_y + size), outline=(70, 78, 90, 255))
                draw.text((offset_x, offset_y + size + 3), f"{size}px", fill=(160, 166, 176, 255))
                offset_x += max(size + 14, 38)
                if offset_x > x + tile_width - 36:
                    offset_x = x + 8
                    offset_y += 64
        output_path.parent.mkdir(parents=True, exist_ok=True)
        canvas.save(output_path)
    return None


def load_existing_decisions(out_path: pathlib.Path) -> dict[tuple[str, str, str], dict[str, Any]]:
    if not out_path.is_file():
        return {}
    try:
        payload = json.loads(out_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError:
        return {}
    if not isinstance(payload, dict):
        return {}
    decisions: dict[tuple[str, str, str], dict[str, Any]] = {}
    for sheet in payload.get("sheets", []):
        if not isinstance(sheet, dict):
            continue
        sheet_id = str(sheet.get("sheetId", "")).strip()
        raw_hash = sheet.get("rawSheetHash")
        for sample in sheet.get("samples", []):
            if not isinstance(sample, dict):
                continue
            target_key = str(sample.get("targetKey", "")).strip()
            selection = str(sample.get("selection", "")).strip()
            if not sheet_id or not target_key or not selection:
                continue
            decisions[(sheet_id, target_key, selection)] = {"rawSheetHash": raw_hash, "sample": sample}
    return decisions


def preserve_existing_decision(
    sample_record: dict[str, Any],
    existing_decisions: dict[tuple[str, str, str], dict[str, Any]],
    sheet_id: str,
    raw_sheet_hash: str | None,
) -> dict[str, Any]:
    previous = existing_decisions.get((sheet_id, sample_record["targetKey"], sample_record["selection"]))
    if previous is None or previous.get("rawSheetHash") != raw_sheet_hash:
        return sample_record
    previous_sample = previous.get("sample")
    if not isinstance(previous_sample, dict):
        return sample_record
    stable_fields = ("row", "col", "category", "outputName", "cellRect", "requiredSizesPx")
    if any(previous_sample.get(field) != sample_record.get(field) for field in stable_fields):
        return sample_record
    previous_decision = str(previous_sample.get("qaDecision", "")).strip()
    if not previous_decision or previous_decision == "MANUAL_REVIEW_REQUIRED":
        return sample_record
    for field in ("qaDecision", "rejectReason", "reviewer", "reviewedAt"):
        if field in previous_sample:
            sample_record[field] = previous_sample.get(field)
    return sample_record


def main() -> int:
    args = parse_args()
    if args.out.exists() and not args.overwrite:
        return print_errors("dark-art-random-qa", [f"output already exists, pass --overwrite: {args.out.as_posix()}."])

    sheets, cells, errors = load_sheet_plan(args.plan)
    prompt_paths, prompt_errors = load_prompt_paths(args.prompt_index)
    errors.extend(prompt_errors)
    selected_sheet_ids = parse_csv(args.sheet_ids)
    if not selected_sheet_ids:
        errors.append("--sheet-ids must contain at least one sheet id.")
    sheet_by_id = {sheet.sheet_id: sheet for sheet in sheets}
    missing_sheets = [sheet_id for sheet_id in selected_sheet_ids if sheet_id not in sheet_by_id]
    if missing_sheets:
        errors.append("requested sheet ids are absent from sheet-plan.yaml: " + ", ".join(missing_sheets) + ".")
    if errors:
        return print_errors("dark-art-random-qa", errors)

    cells_by_sheet: dict[str, list[Any]] = {}
    for cell in cells:
        cells_by_sheet.setdefault(cell.sheet_id, []).append(cell)

    existing_decisions = load_existing_decisions(args.out)
    sheet_records: list[dict[str, Any]] = []
    render_errors: list[str] = []
    for sheet_id in selected_sheet_ids:
        sheet = sheet_by_id[sheet_id]
        sheet_cells = sorted(cells_by_sheet.get(sheet_id, []), key=lambda item: item.target_key)
        selected = select_samples(args.seed, sheet_id, sheet_cells)
        if sheet_id == "r09-rejected-polish" and not selected:
            selected.append(
                {
                    "cell": SimpleNamespace(
                        target_key="rejected.polish.reserved_slot_0_0",
                        row=0,
                        col=0,
                        category="reserved",
                        output_name=None,
                    ),
                    "selection": "mandatory_reserved_polish",
                }
            )
        raw_path = args.raw_root / f"{sheet_id}.png"
        sample_sheet_path = args.sample_root / f"{sheet_id}-random-qa.png"
        render_note = None
        if not args.skip_images and raw_path.is_file() and selected:
            render_note = render_sample_sheet(raw_path, sample_sheet_path, selected, sheet)
            if render_note:
                render_errors.append(f"{sheet_id}: {render_note}")
        raw_sheet_hash = sha256_file(raw_path) if raw_path.is_file() else None
        samples: list[dict[str, Any]] = []
        for sample in selected:
            cell = sample["cell"]
            sample_record = {
                "targetKey": cell.target_key,
                "row": cell.row,
                "col": cell.col,
                "category": cell.category,
                "outputName": cell.output_name,
                "selection": sample["selection"],
                "cellRect": cell_rect(sheet, cell),
                "requiredSizesPx": required_sizes(cell.target_key, sheet_id),
                "qaDecision": "MANUAL_REVIEW_REQUIRED",
                "rejectReason": None,
            }
            samples.append(
                preserve_existing_decision(sample_record, existing_decisions, sheet_id, raw_sheet_hash)
            )
        sheet_records.append(
            {
                "sheetId": sheet_id,
                "round": sheet.round_id,
                "playerVisibleCells": len(sheet_cells),
                "randomSampleRuleCount": sample_count(len(sheet_cells)),
                "mandatoryKeysPresent": sorted(cell.target_key for cell in sheet_cells if cell.target_key in MANDATORY_KEYS),
                "sampledKeys": [sample["targetKey"] for sample in samples],
                "promptPath": prompt_paths.get(sheet_id),
                "rawSheetPath": repo_relative(raw_path),
                "rawSheetHash": raw_sheet_hash,
                "contactSheetPath": repo_relative(args.contact_root / f"{sheet_id}-contact.png"),
                "sampleSheetPath": repo_relative(sample_sheet_path) if sample_sheet_path.is_file() else None,
                "sampleSheetRender": "SKIPPED" if args.skip_images else ("WRITTEN" if sample_sheet_path.is_file() else "RAW_MISSING"),
                "samples": samples,
                "qaGate": "BLOCK_RUNTIME_SLICE_IF_ANY_PLAYER_VISIBLE_SAMPLE_FAILS",
            }
        )

    payload = {
        "schemaVersion": SCHEMA_VERSION,
        "seed": args.seed,
        "planPath": args.plan.as_posix(),
        "promptIndexPath": args.prompt_index.as_posix(),
        "sampleRule": {
            "randomCount": "max(4, ceil(playerVisibleCells * 15%)), capped at 12; if fewer than 4 cells, inspect all",
            "mandatorySamples": "added on top of random selection",
        },
        "failCriteria": list(FAIL_CRITERIA),
        "sheets": sheet_records,
    }
    write_json(args.out, payload)
    if render_errors:
        return print_errors("dark-art-random-qa", render_errors)
    print(f"dark-art-random-qa OK: sheets={len(sheet_records)}, out={args.out.as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
