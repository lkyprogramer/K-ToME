#!/usr/bin/env python3
"""Build PR-08 wall-family exploration slices and previews.

This script is intentionally scoped to UI/review evidence. It does not mutate
sheet-plan, manifests, runtime resources, or production Kotlin.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageStat


ROOT = Path(__file__).resolve().parents[5]
WORK_DIR = Path("UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates")
SOURCE = WORK_DIR / "pr08-wall-family-imagegen-source-board.png"
OUT_DIR = WORK_DIR / "slices"
CONTACT = WORK_DIR / "pr08-wall-family-candidate-contact-board.png"
ROOM_PREVIEW = WORK_DIR / "pr08-wall-family-candidate-runtime-room-preview.png"
CURRENT_WALL = Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_wall_01.png")
FLOOR_KEYS = [
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_1.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_2.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_3.png"),
]

PIECES = ["base", "crown", "side", "corner", "door_contact"]
ROWS = ["conservative", "material_depth", "strong_style"]


def crop_boxes(image: Image.Image) -> list[list[tuple[int, int, int, int]]]:
    width, height = image.size
    margin_x = int(width * 0.035)
    margin_y = int(height * 0.080)
    gap_x = int(width * 0.020)
    gap_y = int(height * 0.036)
    cell_w = int((width - margin_x * 2 - gap_x * 4) / 5)
    cell_h = int((height - margin_y * 2 - gap_y * 2) / 3)
    boxes: list[list[tuple[int, int, int, int]]] = []
    for row in range(3):
        row_boxes = []
        for col in range(5):
            left = margin_x + col * (cell_w + gap_x)
            top = margin_y + row * (cell_h + gap_y)
            row_boxes.append((left, top, left + cell_w, top + cell_h))
        boxes.append(row_boxes)
    return boxes


def tight_square(panel: Image.Image) -> Image.Image:
    rgba = panel.convert("RGBA")
    gray = rgba.convert("L")
    stat = ImageStat.Stat(gray.crop((0, 0, min(18, gray.width), min(18, gray.height))))
    bg = stat.mean[0]
    mask = Image.new("L", gray.size, 0)
    src = gray.load()
    dst = mask.load()
    for y in range(gray.height):
        for x in range(gray.width):
            value = src[x, y]
            if value > bg + 18 or value > 58:
                dst[x, y] = 255
    bbox = mask.getbbox()
    if bbox is None:
        bbox = (0, 0, rgba.width, rgba.height)
    left, top, right, bottom = bbox
    pad = 18
    left = max(0, left - pad)
    top = max(0, top - pad)
    right = min(rgba.width, right + pad)
    bottom = min(rgba.height, bottom + pad)
    crop = rgba.crop((left, top, right, bottom))
    size = max(crop.width, crop.height)
    square = Image.new("RGBA", (size, size), (5, 7, 10, 255))
    square.alpha_composite(crop, ((size - crop.width) // 2, (size - crop.height) // 2))
    return square.resize((128, 128), Image.Resampling.LANCZOS)


def save_slices(source: Image.Image) -> dict[str, dict[str, Image.Image]]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    slices: dict[str, dict[str, Image.Image]] = {}
    for row_name, row_boxes in zip(ROWS, crop_boxes(source), strict=True):
        slices[row_name] = {}
        for piece_name, box in zip(PIECES, row_boxes, strict=True):
            tile = tight_square(source.crop(box))
            output = OUT_DIR / f"wall_{row_name}_{piece_name}.png"
            tile.save(ROOT / output)
            slices[row_name][piece_name] = tile
    return slices


def draw_contact(slices: dict[str, dict[str, Image.Image]]) -> None:
    font = ImageFont.load_default()
    cell = 128
    label_h = 24
    gap = 12
    width = gap + 6 * (cell + gap)
    height = gap + 4 * (cell + label_h + gap)
    board = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    draw = ImageDraw.Draw(board)
    headers = ["current"] + PIECES
    for col, header in enumerate(headers):
        x = gap + col * (cell + gap)
        draw.text((x + 4, gap + 4), header, fill=(231, 225, 211, 255), font=font)
    current = Image.open(ROOT / CURRENT_WALL).convert("RGBA").resize((128, 128), Image.Resampling.LANCZOS)
    for row_index, row_name in enumerate(["current"] + ROWS):
        y = gap + (row_index + 1) * (cell + label_h + gap) - cell
        draw.text((gap + 4, y - label_h + 6), row_name, fill=(217, 154, 43, 255), font=font)
        if row_name == "current":
            board.alpha_composite(current, (gap, y))
            continue
        for col, piece_name in enumerate(PIECES, start=1):
            x = gap + col * (cell + gap)
            board.alpha_composite(slices[row_name][piece_name], (x, y))
    board.save(ROOT / CONTACT)


def floor_tile(index: int, size: int) -> Image.Image:
    path = FLOOR_KEYS[index % len(FLOOR_KEYS)]
    return Image.open(ROOT / path).convert("RGBA").resize((size, size), Image.Resampling.LANCZOS)


def render_room_row(row_name: str, pieces: dict[str, Image.Image], scale: int) -> Image.Image:
    room_cols = 9
    room_rows = 6
    pad = scale * 2
    width = pad * 2 + (room_cols + 2) * scale
    height = pad * 2 + (room_rows + 2) * scale
    canvas = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    draw.text((12, 10), row_name, fill=(217, 154, 43, 255), font=font)
    x0 = pad + scale
    y0 = pad + scale
    for y in range(room_rows):
        for x in range(room_cols):
            canvas.alpha_composite(floor_tile(x * 7 + y * 11, scale), (x0 + x * scale, y0 + y * scale))
    scaled = {key: value.resize((scale, scale), Image.Resampling.LANCZOS) for key, value in pieces.items()}
    for x in range(room_cols):
        canvas.alpha_composite(scaled["crown"], (x0 + x * scale, y0 - scale))
        if x not in {4}:
            canvas.alpha_composite(scaled["base"], (x0 + x * scale, y0 + room_rows * scale))
    for y in range(room_rows):
        canvas.alpha_composite(scaled["side"], (x0 - scale, y0 + y * scale))
        canvas.alpha_composite(scaled["side"], (x0 + room_cols * scale, y0 + y * scale))
    canvas.alpha_composite(scaled["corner"], (x0 - scale, y0 - scale))
    canvas.alpha_composite(scaled["corner"], (x0 + room_cols * scale, y0 - scale))
    canvas.alpha_composite(scaled["corner"], (x0 - scale, y0 + room_rows * scale))
    canvas.alpha_composite(scaled["corner"], (x0 + room_cols * scale, y0 + room_rows * scale))
    canvas.alpha_composite(scaled["door_contact"], (x0 + 4 * scale, y0 + room_rows * scale))
    draw.rectangle((x0, y0, x0 + room_cols * scale, y0 + room_rows * scale), outline=(28, 183, 200, 90), width=1)
    return canvas


def draw_room_preview(slices: dict[str, dict[str, Image.Image]]) -> None:
    scale = 48
    rows = [render_room_row(name, slices[name], scale) for name in ROWS]
    gap = 18
    width = max(row.width for row in rows)
    height = gap + sum(row.height for row in rows) + gap * (len(rows) - 1)
    board = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    y = gap
    for row in rows:
        board.alpha_composite(row, ((width - row.width) // 2, y))
        y += row.height + gap
    board.save(ROOT / ROOM_PREVIEW)


def main() -> int:
    source = Image.open(ROOT / SOURCE).convert("RGBA")
    slices = save_slices(source)
    draw_contact(slices)
    draw_room_preview(slices)
    print(f"wrote {OUT_DIR}")
    print(f"wrote {CONTACT}")
    print(f"wrote {ROOM_PREVIEW}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
