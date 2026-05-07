#!/usr/bin/env python3
"""Slice dark-v1 raw sprite sheets according to sheet-plan.yaml."""

from __future__ import annotations

import argparse
import pathlib

from dark_sprite_sheet_contract import DARK_RUNTIME_PREFIX, load_sheet_plan, print_errors
from verify_sprite_sheet_map import cell_rect


def load_pillow_image():
    try:
        from PIL import Image
    except ModuleNotFoundError as exc:
        if exc.name == "PIL":
            raise RuntimeError("Pillow is required when slice_spritesheet writes dark-v1 PNG outputs.") from exc
        raise
    return Image


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
    image_module = None
    for sheet in sheets:
        sheet_cells = [candidate for candidate in cells if candidate.sheet_id == sheet.sheet_id]
        dark_cells = [candidate for candidate in sheet_cells if candidate.output_name.startswith(DARK_RUNTIME_PREFIX)]
        skipped_pending += len(sheet_cells) - len(dark_cells)
        if not dark_cells:
            continue
        raw_path = raw_sheet_path(sheet, args.raw_root)
        if not raw_path.is_file():
            errors.append(f"missingRawSheet sheetId={sheet.sheet_id} expected={raw_path.as_posix()}.")
            continue
        if image_module is None:
            try:
                image_module = load_pillow_image()
            except RuntimeError as exc:
                errors.append(str(exc))
                break
        with image_module.open(raw_path) as image:
            rgba = image.convert("RGBA")
            for cell in dark_cells:
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
