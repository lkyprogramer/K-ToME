#!/usr/bin/env python3
"""Generate stable dark-v1 sheet prompts from UI/sprite-sheets/sheet-plan.yaml."""

from __future__ import annotations

import argparse
import json
import pathlib

from dark_sprite_sheet_contract import STYLE_TAG, load_sheet_plan, print_errors, sha256_text, write_json


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate dark-v1 sprite sheet prompts from the sheet plan.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--output-dir", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/prompts/dark-v1"))
    return parser.parse_args()


def prompt_id(order: int, sheet_id: str) -> str:
    return f"{order:03d}-{sheet_id}"


def parse_prompt_id(value: str) -> tuple[int, str] | None:
    order_text, separator, sheet_id = value.partition("-")
    if not separator or not order_text.isdigit() or not sheet_id:
        return None
    return int(order_text), sheet_id


def load_existing_prompt_ids(output_dir: pathlib.Path) -> tuple[dict[str, str], int, list[str]]:
    index_path = output_dir / "prompt-index.json"
    if not index_path.exists():
        return {}, 0, []

    try:
        payload = json.loads(index_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        return {}, 0, [f"existing prompt-index.json is not valid JSON: {exc}."]

    prompts = payload.get("prompts")
    if not isinstance(prompts, list):
        return {}, 0, ["existing prompt-index.json must define a prompts list."]

    by_sheet_id: dict[str, str] = {}
    seen_prompt_ids: set[str] = set()
    seen_orders: set[int] = set()
    max_order = 0
    errors: list[str] = []
    for index, entry in enumerate(prompts):
        owner = f"prompt-index.prompts[{index}]"
        if not isinstance(entry, dict):
            errors.append(f"{owner} must be a mapping.")
            continue
        sheet_id = str(entry.get("sheetId", "")).strip()
        existing_prompt_id = str(entry.get("promptId", "")).strip()
        parsed = parse_prompt_id(existing_prompt_id)
        if not sheet_id:
            errors.append(f"{owner} sheetId is required.")
            continue
        if parsed is None:
            errors.append(f"{owner} promptId must use 001-sheet-id format, got '{existing_prompt_id or '<missing>'}'.")
            continue
        order, prompt_sheet_id = parsed
        if prompt_sheet_id != sheet_id:
            errors.append(f"{owner} promptId suffix '{prompt_sheet_id}' must match sheetId '{sheet_id}'.")
        if sheet_id in by_sheet_id:
            errors.append(f"existing prompt-index.json has duplicate sheetId '{sheet_id}'.")
        if existing_prompt_id in seen_prompt_ids:
            errors.append(f"existing prompt-index.json has duplicate promptId '{existing_prompt_id}'.")
        if order in seen_orders:
            errors.append(f"existing prompt-index.json has duplicate prompt order '{order:03d}'.")
        by_sheet_id[sheet_id] = existing_prompt_id
        seen_prompt_ids.add(existing_prompt_id)
        seen_orders.add(order)
        max_order = max(max_order, order)
    return by_sheet_id, max_order, errors


def assign_prompt_ids(output_dir: pathlib.Path, sheets) -> tuple[dict[str, str], list[str]]:
    existing_by_sheet_id, max_order, errors = load_existing_prompt_ids(output_dir)
    plan_sheet_ids = {sheet.sheet_id for sheet in sheets}
    orphaned_sheet_ids = sorted(set(existing_by_sheet_id) - plan_sheet_ids)
    if orphaned_sheet_ids:
        errors.append(
            "existing prompt-index.json references sheetIds absent from sheet-plan.yaml: "
            + ", ".join(orphaned_sheet_ids)
            + ". Keep the sheet or document the migration before regenerating prompts."
        )
    if errors:
        return {}, errors

    assigned = {
        sheet_id: prompt_id_value
        for sheet_id, prompt_id_value in existing_by_sheet_id.items()
        if sheet_id in plan_sheet_ids
    }
    for sheet in sorted(sheets, key=lambda item: (item.round_id, item.sheet_id)):
        if sheet.sheet_id in assigned:
            continue
        max_order += 1
        assigned[sheet.sheet_id] = prompt_id(max_order, sheet.sheet_id)
    return assigned, []


def prompt_sort_key(prompt_id_value: str) -> int:
    parsed = parse_prompt_id(prompt_id_value)
    return parsed[0] if parsed else 0


def render_prompt(prompt_id_value: str, sheet, cells) -> str:
    canvas_width, canvas_height = sheet.canvas_size
    lines = [
        f"Prompt ID: {prompt_id_value}",
        f"Sheet ID: {sheet.sheet_id}",
        f"Expected output file: {sheet.raw_sheet_path}",
        f"Canvas: {canvas_width}x{canvas_height}",
        f"Grid: {sheet.grid['columns']} columns x {sheet.grid['rows']} rows",
        f"Cell: {sheet.grid['cellWidth']}x{sheet.grid['cellHeight']}",
        f"Style tag: {STYLE_TAG}",
        "",
        f"Create a {canvas_width}x{canvas_height} transparent-background sprite sheet for K-ToME.",
        f"Style tag: {STYLE_TAG}.",
        f"Grid: {sheet.grid['columns']} columns x {sheet.grid['rows']} rows, each cell is {sheet.grid['cellWidth']}x{sheet.grid['cellHeight']}.",
        "Every listed non-reserved cell contains exactly one centered subject, no text, no numbers, no labels, no watermark, no merged cells.",
        "Only render the cells listed below; leave every unlisted grid slot transparent and empty.",
        "Visual style: dark fantasy roguelike, charcoal black panels, forged iron, worn stone, ember highlights, restrained cyan edge glow, readable at 32px.",
        "Cell list:",
    ]
    for cell in sorted(cells, key=lambda item: (item.row, item.col)):
        alias_suffix = f" Alias of targetKey {cell.alias_of}." if cell.alias_of else ""
        lines.append(f"row {cell.row} col {cell.col} targetKey {cell.target_key}: {cell.subject}.{alias_suffix}")
    lines.extend(
        [
            "Avoid: sci-fi HUD, neon cyberpunk, anime, chibi, glossy plastic, bright glassmorphism, modern icons, franchise symbols.",
            "",
        ]
    )
    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    sheets, cells, errors = load_sheet_plan(args.plan)
    if errors:
        return print_errors("generate-sheet-prompt", errors)
    prompt_ids, prompt_errors = assign_prompt_ids(args.output_dir, sheets)
    if prompt_errors:
        return print_errors("generate-sheet-prompt", prompt_errors)

    args.output_dir.mkdir(parents=True, exist_ok=True)
    index: list[dict] = []
    for sheet in sorted(sheets, key=lambda item: prompt_sort_key(prompt_ids[item.sheet_id])):
        sheet_cells = [cell for cell in cells if cell.sheet_id == sheet.sheet_id]
        pid = prompt_ids[sheet.sheet_id]
        content = render_prompt(pid, sheet, sheet_cells)
        prompt_path = args.output_dir / f"{pid}.prompt.txt"
        prompt_path.write_text(content, encoding="utf-8")
        index.append(
            {
                "promptId": pid,
                "promptPath": prompt_path.as_posix(),
                "promptHash": sha256_text(content),
                "sheetId": sheet.sheet_id,
                "rawSheetPath": sheet.raw_sheet_path,
                "round": sheet.round_id,
                "grid": sheet.grid,
                "cellCount": len(sheet_cells),
                "cellCountScope": "non-reserved-cells",
                "gridCapacity": sheet.capacity,
            }
        )

    write_json(args.output_dir / "prompt-index.json", {"schemaVersion": "dark-prompt-index-v1", "prompts": index})
    print(f"generate-sheet-prompt OK: prompts={len(index)}, outputDir={args.output_dir.as_posix()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
