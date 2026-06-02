#!/usr/bin/env python3
"""Build the W02 clean wall-family candidate preview.

This script is evidence-only and does not mutate runtime resources.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[5]
WORK_DIR = Path("UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/clean-wall-family-w02")
SOURCE = WORK_DIR / "pr08-wall-family-w02-imagegen-source-row.png"
OUT_DIR = WORK_DIR / "slices"
CONTACT = WORK_DIR / "pr08-wall-family-w02-contact-board.png"
ROOM_PREVIEW = WORK_DIR / "pr08-wall-family-w02-runtime-room-preview.png"
FLOOR_KEYS = [
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_1.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_2.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_3.png"),
]
PIECES = ["base", "crown", "side", "corner", "door_contact"]


def segment_boxes(image: Image.Image) -> list[tuple[int, int, int, int]]:
    width, height = image.size
    if width == 2048 and height == 1024:
        return [
            (45, 128, 415, 560),
            (455, 178, 850, 560),
            (900, 130, 1210, 560),
            (1260, 130, 1625, 585),
            (1678, 215, 2035, 560),
        ]
    usable_top = int(height * 0.12)
    usable_bottom = int(height * 0.63)
    return [
        (
            int(width * col / 5),
            usable_top,
            int(width * (col + 1) / 5),
            usable_bottom,
        )
        for col in range(5)
    ]


def tight_square(panel: Image.Image) -> Image.Image:
    rgba = panel.convert("RGBA")
    gray = rgba.convert("L")
    mask = Image.new("L", gray.size, 0)
    src = gray.load()
    dst = mask.load()
    for y in range(gray.height):
        for x in range(gray.width):
            value = src[x, y]
            if value > 22:
                dst[x, y] = 255
    bbox = mask.getbbox() or (0, 0, rgba.width, rgba.height)
    left, top, right, bottom = bbox
    pad = 20
    left = max(0, left - pad)
    top = max(0, top - pad)
    right = min(rgba.width, right + pad)
    bottom = min(rgba.height, bottom + pad)
    crop = rgba.crop((left, top, right, bottom))
    size = max(crop.width, crop.height)
    square = Image.new("RGBA", (size, size), (5, 7, 10, 255))
    square.alpha_composite(crop, ((size - crop.width) // 2, (size - crop.height) // 2))
    return square.resize((128, 128), Image.Resampling.LANCZOS)


def save_slices(source: Image.Image) -> dict[str, Image.Image]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    slices = {}
    for piece_name, box in zip(PIECES, segment_boxes(source), strict=True):
        tile = tight_square(source.crop(box))
        tile.save(ROOT / OUT_DIR / f"wall_w02_{piece_name}.png")
        slices[piece_name] = tile
    return slices


def draw_contact(slices: dict[str, Image.Image]) -> None:
    font = ImageFont.load_default()
    cell = 128
    gap = 14
    label_h = 24
    width = gap + len(PIECES) * (cell + gap)
    height = gap * 2 + label_h + cell
    board = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    draw = ImageDraw.Draw(board)
    for col, piece_name in enumerate(PIECES):
        x = gap + col * (cell + gap)
        draw.text((x + 4, gap + 5), piece_name, fill=(231, 225, 211, 255), font=font)
        board.alpha_composite(slices[piece_name], (x, gap + label_h))
    board.save(ROOT / CONTACT)


def floor_tile(index: int, size: int) -> Image.Image:
    return Image.open(ROOT / FLOOR_KEYS[index % len(FLOOR_KEYS)]).convert("RGBA").resize((size, size), Image.Resampling.LANCZOS)


def draw_room_preview(slices: dict[str, Image.Image]) -> None:
    scale = 56
    room_cols = 10
    room_rows = 6
    pad = scale * 2
    width = pad * 2 + (room_cols + 2) * scale
    height = pad * 2 + (room_rows + 2) * scale
    canvas = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    draw = ImageDraw.Draw(canvas)
    scaled = {key: value.resize((scale, scale), Image.Resampling.LANCZOS) for key, value in slices.items()}
    x0 = pad + scale
    y0 = pad + scale
    for y in range(room_rows):
        for x in range(room_cols):
            canvas.alpha_composite(floor_tile(x * 5 + y * 13, scale), (x0 + x * scale, y0 + y * scale))
    for x in range(room_cols):
        canvas.alpha_composite(scaled["crown"], (x0 + x * scale, y0 - scale))
        if x != room_cols // 2:
            canvas.alpha_composite(scaled["base"], (x0 + x * scale, y0 + room_rows * scale))
    for y in range(room_rows):
        canvas.alpha_composite(scaled["side"], (x0 - scale, y0 + y * scale))
        canvas.alpha_composite(scaled["side"], (x0 + room_cols * scale, y0 + y * scale))
    for x, y in [
        (x0 - scale, y0 - scale),
        (x0 + room_cols * scale, y0 - scale),
        (x0 - scale, y0 + room_rows * scale),
        (x0 + room_cols * scale, y0 + room_rows * scale),
    ]:
        canvas.alpha_composite(scaled["corner"], (x, y))
    canvas.alpha_composite(scaled["door_contact"], (x0 + room_cols // 2 * scale, y0 + room_rows * scale))
    draw.rectangle((x0, y0, x0 + room_cols * scale, y0 + room_rows * scale), outline=(28, 183, 200, 75), width=1)
    canvas.save(ROOT / ROOM_PREVIEW)


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
