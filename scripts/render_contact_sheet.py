#!/usr/bin/env python3
"""Render labeled contact sheets for dark-v1 raw sprite sheets."""

from __future__ import annotations

import argparse
import pathlib

from PIL import Image, ImageDraw, ImageFont

from dark_sprite_sheet_contract import load_sheet_plan, print_errors
from verify_sprite_sheet_map import cell_rect


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Render dark-v1 contact sheets with row/col/key labels.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--output-root", type=pathlib.Path, default=pathlib.Path("assets-src/image/contact-sheets/dark-v1"))
    parser.add_argument("--overwrite", action="store_true")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    sheets, cells, errors = load_sheet_plan(args.plan)
    if errors:
        return print_errors("render-contact-sheet", errors)
    font = ImageFont.load_default()
    written = 0
    for sheet in sheets:
        raw_path = pathlib.Path(sheet.raw_sheet_path)
        if not raw_path.is_file():
            errors.append(f"missingRawSheet sheetId={sheet.sheet_id} expected={sheet.raw_sheet_path}.")
            continue
        output_path = args.output_root / f"{sheet.sheet_id}-contact.png"
        if output_path.exists() and not args.overwrite:
            errors.append(f"Contact sheet already exists; pass --overwrite to replace it: {output_path.as_posix()}.")
            continue
        with Image.open(raw_path) as image:
            rgba = image.convert("RGBA")
            draw = ImageDraw.Draw(rgba)
            for cell in [candidate for candidate in cells if candidate.sheet_id == sheet.sheet_id]:
                left, top, right, bottom = cell_rect(sheet, cell)
                draw.rectangle((left, top, right - 1, bottom - 1), outline=(28, 183, 200, 255), width=3)
                label = f"{cell.row},{cell.col} {cell.target_key}"
                draw.rectangle((left + 4, top + 4, min(right - 4, left + 250), top + 22), fill=(5, 7, 10, 220))
                draw.text((left + 8, top + 7), label, fill=(231, 225, 211, 255), font=font)
            output_path.parent.mkdir(parents=True, exist_ok=True)
            rgba.save(output_path)
            written += 1
    if errors:
        return print_errors("render-contact-sheet", errors)
    print(f"render-contact-sheet OK: written={written}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
