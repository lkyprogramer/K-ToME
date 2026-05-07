#!/usr/bin/env python3
"""Slice dark-v1 raw sprite sheets according to sheet-plan.yaml."""

from __future__ import annotations

import argparse
import pathlib

from PIL import Image

from dark_sprite_sheet_contract import DARK_RUNTIME_PREFIX, load_sheet_plan, print_errors
from verify_sprite_sheet_map import cell_rect


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Slice dark-v1 raw sprite sheets into runtime PNGs.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--raw-root", type=pathlib.Path, default=None)
    parser.add_argument("--runtime-root", type=pathlib.Path, default=pathlib.Path("client/src/main/resources"))
    parser.add_argument("--overwrite", action="store_true")
    return parser.parse_args()


def raw_sheet_path(sheet, raw_root: pathlib.Path | None) -> pathlib.Path:
    if raw_root is None:
        return pathlib.Path(sheet.raw_sheet_path)
    return raw_root / f"{sheet.sheet_id}.png"


def main() -> int:
    args = parse_args()
    sheets, cells, errors = load_sheet_plan(args.plan)
    if errors:
        return print_errors("slice-spritesheet", errors)
    written = 0
    skipped_pending = 0
    for sheet in sheets:
        raw_path = raw_sheet_path(sheet, args.raw_root)
        if not raw_path.is_file():
            errors.append(f"missingRawSheet sheetId={sheet.sheet_id} expected={raw_path.as_posix()}.")
            continue
        with Image.open(raw_path) as image:
            rgba = image.convert("RGBA")
            for cell in [candidate for candidate in cells if candidate.sheet_id == sheet.sheet_id]:
                if not cell.output_name.startswith(DARK_RUNTIME_PREFIX):
                    skipped_pending += 1
                    continue
                output_path = args.runtime_root / cell.output_name
                if output_path.exists() and not args.overwrite:
                    errors.append(f"Output already exists; pass --overwrite to replace it: {output_path.as_posix()}.")
                    continue
                output_path.parent.mkdir(parents=True, exist_ok=True)
                rgba.crop(cell_rect(sheet, cell)).save(output_path)
                written += 1
    if errors:
        return print_errors("slice-spritesheet", errors)
    print(f"slice-spritesheet OK: written={written}, skippedPending={skipped_pending}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
