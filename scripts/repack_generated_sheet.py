#!/usr/bin/env python3
"""Repack generated dark-v1 sheet subjects into exact sheet-plan cells."""

from __future__ import annotations

import argparse
import pathlib
import sys
from dataclasses import dataclass

from PIL import Image

from dark_sprite_sheet_contract import load_sheet_plan, print_errors


ALPHA_THRESHOLD = 16


@dataclass(frozen=True)
class Component:
    left: int
    top: int
    right: int
    bottom: int
    area: int

    @property
    def width(self) -> int:
        return self.right - self.left

    @property
    def height(self) -> int:
        return self.bottom - self.top

    @property
    def center_x(self) -> float:
        return (self.left + self.right) / 2

    @property
    def center_y(self) -> float:
        return (self.top + self.bottom) / 2


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Repack generated dark-v1 raw sheets into exact grid slots.")
    parser.add_argument("--plan", type=pathlib.Path, default=pathlib.Path("UI/sprite-sheets/sheet-plan.yaml"))
    parser.add_argument("--sheet-id", action="append", default=None)
    parser.add_argument("--raw-root", type=pathlib.Path, default=None)
    parser.add_argument("--overwrite", action="store_true")
    return parser.parse_args()


def raw_sheet_path(sheet, raw_root: pathlib.Path | None) -> pathlib.Path:
    if raw_root is None:
        return pathlib.Path(sheet.raw_sheet_path)
    return raw_root / f"{sheet.sheet_id}.png"


def find_components(image: Image.Image, min_area: int) -> list[Component]:
    width, height = image.size
    alpha = image.getchannel("A")
    alpha_pixels = alpha.load()
    visited = bytearray(width * height)
    components: list[Component] = []
    for start_y in range(height):
        for start_x in range(width):
            start_index = start_y * width + start_x
            if visited[start_index] or alpha_pixels[start_x, start_y] <= ALPHA_THRESHOLD:
                continue
            stack = [(start_x, start_y)]
            visited[start_index] = 1
            left = right = start_x
            top = bottom = start_y
            area = 0
            while stack:
                x, y = stack.pop()
                area += 1
                left = min(left, x)
                right = max(right, x)
                top = min(top, y)
                bottom = max(bottom, y)
                for next_x, next_y in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
                    if 0 <= next_x < width and 0 <= next_y < height:
                        index = next_y * width + next_x
                        if not visited[index] and alpha_pixels[next_x, next_y] > ALPHA_THRESHOLD:
                            visited[index] = 1
                            stack.append((next_x, next_y))
            if area >= min_area:
                components.append(Component(left, top, right + 1, bottom + 1, area))
    return components


def split_oversized_components(components: list[Component], sheet) -> list[Component]:
    cell_width = sheet.grid["cellWidth"]
    cell_height = sheet.grid["cellHeight"]
    rows = sheet.grid["rows"]
    columns = sheet.grid["columns"]
    pieces: list[Component] = []
    for component in components:
        split_columns = component.width > cell_width * 1.5
        split_rows = component.height > cell_height * 1.5
        should_split = split_columns or split_rows
        if not should_split:
            pieces.append(component)
            continue
        if split_columns:
            col_start = max(0, component.left // cell_width)
            col_end = min(columns - 1, (component.right - 1) // cell_width)
        else:
            col_start = col_end = min(columns - 1, max(0, int(component.center_x // cell_width)))
        if split_rows:
            row_start = max(0, component.top // cell_height)
            row_end = min(rows - 1, (component.bottom - 1) // cell_height)
        else:
            row_start = row_end = min(rows - 1, max(0, int(component.center_y // cell_height)))
        for row in range(row_start, row_end + 1):
            for col in range(col_start, col_end + 1):
                left = max(component.left, col * cell_width)
                top = max(component.top, row * cell_height)
                right = min(component.right, (col + 1) * cell_width)
                bottom = min(component.bottom, (row + 1) * cell_height)
                if right > left and bottom > top:
                    area = (right - left) * (bottom - top)
                    if area >= min(cell_width, cell_height):
                        pieces.append(Component(left, top, right, bottom, area))
    return pieces


def ordered_pieces(components: list[Component], sheet) -> list[Component]:
    threshold = sheet.grid["cellHeight"] * 0.75
    components = sorted(components, key=lambda component: component.center_y)
    rows: list[list[Component]] = []
    for component in components:
        if rows and abs(component.center_y - average_center_y(rows[-1])) <= threshold:
            rows[-1].append(component)
        else:
            rows.append([component])
    ordered: list[Component] = []
    for row in rows:
        ordered.extend(sorted(row, key=lambda component: component.center_x))
    return ordered


def normalize_piece_count(pieces: list[Component], expected_count: int, sheet) -> list[Component]:
    if len(pieces) <= expected_count:
        return pieces

    cell_width = sheet.grid["cellWidth"]
    cell_height = sheet.grid["cellHeight"]
    merge_distance = max(cell_width, cell_height) * 1.4
    anchors = sorted(pieces, key=lambda component: component.area, reverse=True)[:expected_count]
    extras = sorted(pieces, key=lambda component: component.area, reverse=True)[expected_count:]
    merged_count = 0
    unmerged_extras: list[Component] = []
    for extra in extras:
        nearest_index = min(
            range(len(anchors)),
            key=lambda index: squared_distance(extra, anchors[index]),
        )
        anchor = anchors[nearest_index]
        if extra.area <= anchor.area * 0.35 and squared_distance(extra, anchor) <= merge_distance * merge_distance:
            anchors[nearest_index] = merge_components(anchor, extra)
            merged_count += 1
        else:
            unmerged_extras.append(extra)
    if merged_count > 0:
        print(
            f"[repack-generated-sheet] WARN {sheet.sheet_id} merged {merged_count} small alpha components "
            f"to match expected_count={expected_count}.",
            file=sys.stderr,
        )
    if unmerged_extras:
        return ordered_pieces(anchors + unmerged_extras, sheet)
    return ordered_pieces(anchors, sheet)


def squared_distance(left: Component, right: Component) -> float:
    delta_x = left.center_x - right.center_x
    delta_y = left.center_y - right.center_y
    return delta_x * delta_x + delta_y * delta_y


def merge_components(left: Component, right: Component) -> Component:
    return Component(
        min(left.left, right.left),
        min(left.top, right.top),
        max(left.right, right.right),
        max(left.bottom, right.bottom),
        left.area + right.area,
    )


def average_center_y(row: list[Component]) -> float:
    return sum(component.center_y for component in row) / len(row)


def cell_padding(cell, sheet) -> int:
    if cell.category.startswith("tile_"):
        return 0
    return max(6, min(sheet.grid["cellWidth"], sheet.grid["cellHeight"]) // 12)


def repack_sheet(sheet, cells, raw_path: pathlib.Path, overwrite: bool) -> list[str]:
    if raw_path.exists() and not overwrite:
        return [f"Raw sheet already exists; pass --overwrite to repack: {raw_path.as_posix()}."]
    with Image.open(raw_path) as source_image:
        source = source_image.convert("RGBA")
    expected_size = sheet.canvas_size
    if source.size != expected_size:
        return [f"{sheet.sheet_id} raw sheet size must be {expected_size}, got {source.size}."]

    direct_cells = sorted(
        [cell for cell in cells if cell.sheet_id == sheet.sheet_id and not cell.reserved],
        key=lambda cell: (cell.row, cell.col),
    )
    min_area = max(64, min(sheet.grid["cellWidth"], sheet.grid["cellHeight"]) * 3)
    components = find_components(source, min_area)
    if len(components) == len(direct_cells):
        pieces = ordered_pieces(components, sheet)
    else:
        pieces = ordered_pieces(split_oversized_components(components, sheet), sheet)
    if len(pieces) > len(direct_cells):
        pieces = normalize_piece_count(pieces, len(direct_cells), sheet)
    if len(pieces) < len(direct_cells):
        grid_output = repack_from_existing_grid(source, sheet, direct_cells)
        if grid_output is not None:
            print(
                f"[repack-generated-sheet] WARN {sheet.sheet_id} used fixed-grid fallback "
                f"after detecting {len(pieces)} subjects for expected_count={len(direct_cells)}.",
                file=sys.stderr,
            )
            grid_output.save(raw_path)
            return []
    if len(pieces) != len(direct_cells):
        return [
            f"{sheet.sheet_id} generated subject count mismatch: expected={len(direct_cells)} actual={len(pieces)}. "
            "Regenerate the raw sheet or adjust repack detection."
        ]

    output = Image.new("RGBA", expected_size, (0, 0, 0, 0))
    cell_width = sheet.grid["cellWidth"]
    cell_height = sheet.grid["cellHeight"]
    for cell, piece in zip(direct_cells, pieces):
        padding = cell_padding(cell, sheet)
        crop = source.crop(expand_box(piece, source.size, 2))
        crop = keep_largest_alpha_component(crop)
        crop = trim_alpha(crop)
        max_width = cell_width - padding * 2
        max_height = cell_height - padding * 2
        scale = min(max_width / crop.width, max_height / crop.height, 1.0)
        if scale < 1.0:
            crop = crop.resize(
                (max(1, round(crop.width * scale)), max(1, round(crop.height * scale))),
                Image.Resampling.LANCZOS,
            )
        x = cell.col * cell_width + (cell_width - crop.width) // 2
        y = cell.row * cell_height + (cell_height - crop.height) // 2
        output.alpha_composite(crop, (x, y))
    output.save(raw_path)
    return []


def repack_from_existing_grid(source: Image.Image, sheet, direct_cells) -> Image.Image | None:
    output = Image.new("RGBA", sheet.canvas_size, (0, 0, 0, 0))
    cell_width = sheet.grid["cellWidth"]
    cell_height = sheet.grid["cellHeight"]
    for cell in direct_cells:
        padding = cell_padding(cell, sheet)
        crop = source.crop(cell_box(sheet, cell))
        crop = keep_largest_alpha_component(crop)
        crop = trim_alpha(crop)
        if crop.getchannel("A").getbbox() is None:
            return None
        max_width = cell_width - padding * 2
        max_height = cell_height - padding * 2
        scale = min(max_width / crop.width, max_height / crop.height, 1.0)
        if scale < 1.0:
            crop = crop.resize(
                (max(1, round(crop.width * scale)), max(1, round(crop.height * scale))),
                Image.Resampling.LANCZOS,
            )
        x = cell.col * cell_width + (cell_width - crop.width) // 2
        y = cell.row * cell_height + (cell_height - crop.height) // 2
        output.alpha_composite(crop, (x, y))
    return output


def cell_box(sheet, cell) -> tuple[int, int, int, int]:
    cell_width = sheet.grid["cellWidth"]
    cell_height = sheet.grid["cellHeight"]
    left = cell.col * cell_width
    top = cell.row * cell_height
    return (left, top, left + cell_width, top + cell_height)


def expand_box(component: Component, image_size: tuple[int, int], padding: int) -> tuple[int, int, int, int]:
    width, height = image_size
    return (
        max(0, component.left - padding),
        max(0, component.top - padding),
        min(width, component.right + padding),
        min(height, component.bottom + padding),
    )


def trim_alpha(image: Image.Image) -> Image.Image:
    bbox = image.getchannel("A").getbbox()
    if bbox is None:
        return image
    return image.crop(bbox)


def keep_largest_alpha_component(image: Image.Image) -> Image.Image:
    components = find_components(image, 16)
    if len(components) <= 1:
        return image
    largest = max(components, key=lambda component: component.area)
    output = Image.new("RGBA", image.size, (0, 0, 0, 0))
    output.alpha_composite(
        image.crop((largest.left, largest.top, largest.right, largest.bottom)),
        (largest.left, largest.top),
    )
    return output


def main() -> int:
    args = parse_args()
    sheets, cells, errors = load_sheet_plan(args.plan)
    if errors:
        return print_errors("repack-generated-sheet", errors)
    selected_sheet_ids = set(args.sheet_id or [sheet.sheet_id for sheet in sheets])
    for sheet in sheets:
        if sheet.sheet_id not in selected_sheet_ids:
            continue
        errors.extend(repack_sheet(sheet, cells, raw_sheet_path(sheet, args.raw_root), args.overwrite))
    if errors:
        return print_errors("repack-generated-sheet", errors)
    print(f"repack-generated-sheet OK: sheets={len(selected_sheet_ids)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
