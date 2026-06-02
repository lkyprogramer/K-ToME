#!/usr/bin/env python3
"""Build W03 orientation-aware wall-family candidate previews.

This script is evidence-only. It does not mutate runtime resources, manifests,
or sprite sheet contracts.
"""

from __future__ import annotations

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[5]
WORK_DIR = Path("UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/orientation-wall-family-w03")
SOURCE = WORK_DIR / "pr08-wall-family-w03-imagegen-source-board.png"
OUT_DIR = WORK_DIR / "slices"
CONTACT = WORK_DIR / "pr08-wall-family-w03-contact-board.png"
ORIENTATION_PREVIEW = WORK_DIR / "pr08-wall-family-w03-orientation-room-preview.png"
COMPARISON = WORK_DIR / "pr08-wall-family-w03-w02-preview-comparison.png"
W02_RUNTIME_CROP = Path(
    "UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/runtime-slice-b-w02/ui-demo-new-map-stage-crop.png",
)
REFERENCE = Path("UI/UI-demo-new.png")
FLOOR_KEYS = [
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_1.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_2.png"),
    Path("client/src/main/resources/dark-v1/tiles/tileset_ruins_ground_01_variant_3.png"),
]
PIECES = ["base", "crown", "side", "corner", "door_contact"]
CANDIDATES = ["w03_a", "w03_b", "w03_c"]

# The generated board is evidence, not a production sheet. These boxes isolate
# the source subjects while keeping enough dark margin for fair runtime-size QA.
CELL_BOXES = [
    [(45, 35, 330, 300), (390, 30, 665, 305), (715, 30, 885, 300), (955, 30, 1235, 310), (1275, 30, 1585, 300)],
    [(45, 340, 330, 625), (390, 340, 665, 625), (715, 340, 885, 625), (955, 340, 1235, 635), (1275, 340, 1585, 625)],
    [(45, 645, 330, 940), (390, 645, 665, 940), (715, 645, 885, 940), (955, 645, 1235, 950), (1275, 645, 1585, 940)],
]


def trim_subject(panel: Image.Image) -> Image.Image:
    rgba = panel.convert("RGBA")
    gray = rgba.convert("L")
    width, height = gray.size
    border_values = []
    for x in range(width):
        for y in range(13):
            border_values.append(gray.getpixel((x, y)))
            border_values.append(gray.getpixel((x, height - 1 - y)))
    for y in range(height):
        for x in range(13):
            border_values.append(gray.getpixel((x, y)))
            border_values.append(gray.getpixel((width - 1 - x, y)))
    threshold = 28
    if border_values:
        threshold = max(28, int(sum(border_values) / len(border_values)) + 18)
    mask = gray.point(lambda value: 255 if value > threshold else 0)
    bbox = mask.getbbox()
    if bbox is None:
        return rgba
    left, top, right, bottom = bbox
    pad = 18
    return rgba.crop((max(0, left - pad), max(0, top - pad), min(width, right + pad), min(height, bottom + pad)))


def fit_square(image: Image.Image, side: int = 128) -> Image.Image:
    subject = trim_subject(image)
    max_subject = 116
    scale = min(max_subject / subject.width, max_subject / subject.height)
    resized = subject.resize((max(1, round(subject.width * scale)), max(1, round(subject.height * scale))), Image.Resampling.LANCZOS)
    square = Image.new("RGBA", (side, side), (5, 7, 10, 255))
    square.alpha_composite(resized, ((side - resized.width) // 2, (side - resized.height) // 2))
    return square


def save_slices(source: Image.Image) -> dict[str, dict[str, Image.Image]]:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    result: dict[str, dict[str, Image.Image]] = {}
    for row, candidate in enumerate(CANDIDATES):
        pieces: dict[str, Image.Image] = {}
        for col, piece in enumerate(PIECES):
            tile = fit_square(source.crop(CELL_BOXES[row][col]))
            tile.save(ROOT / OUT_DIR / f"wall_{candidate}_{piece}.png")
            pieces[piece] = tile
        result[candidate] = pieces
    return result


def draw_contact(slices: dict[str, dict[str, Image.Image]]) -> None:
    font = ImageFont.load_default()
    cell = 128
    gap = 14
    label_h = 24
    row_label_w = 70
    width = row_label_w + gap + len(PIECES) * (cell + gap)
    height = gap * 2 + label_h + len(CANDIDATES) * (cell + gap)
    board = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    draw = ImageDraw.Draw(board)
    for col, piece in enumerate(PIECES):
        x = row_label_w + gap + col * (cell + gap)
        draw.text((x + 4, gap + 5), piece, fill=(231, 225, 211, 255), font=font)
    for row, candidate in enumerate(CANDIDATES):
        y = gap + label_h + row * (cell + gap)
        draw.text((gap, y + cell // 2 - 5), candidate, fill=(231, 225, 211, 255), font=font)
        for col, piece in enumerate(PIECES):
            x = row_label_w + gap + col * (cell + gap)
            board.alpha_composite(slices[candidate][piece], (x, y))
    board.save(ROOT / CONTACT)


def floor_tile(index: int, size: int) -> Image.Image:
    return Image.open(ROOT / FLOOR_KEYS[index % len(FLOOR_KEYS)]).convert("RGBA").resize((size, size), Image.Resampling.LANCZOS)


def draw_candidate_room(slices: dict[str, Image.Image], label: str) -> Image.Image:
    scale = 48
    room_cols = 8
    room_rows = 5
    label_h = 24
    pad = scale
    width = (room_cols + 2) * scale + pad * 2
    height = (room_rows + 2) * scale + pad * 2 + label_h
    canvas = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    draw = ImageDraw.Draw(canvas)
    font = ImageFont.load_default()
    draw.text((pad, 6), label, fill=(231, 225, 211, 255), font=font)
    scaled = {key: value.resize((scale, scale), Image.Resampling.LANCZOS) for key, value in slices.items()}
    side_west = scaled["side"]
    side_east = ImageOps.mirror(scaled["side"])
    corner_nw = scaled["corner"]
    corner_ne = ImageOps.mirror(scaled["corner"])
    corner_sw = ImageOps.flip(scaled["corner"])
    corner_se = ImageOps.flip(ImageOps.mirror(scaled["corner"]))
    x0 = pad + scale
    y0 = pad + scale + label_h
    for y in range(room_rows):
        for x in range(room_cols):
            canvas.alpha_composite(floor_tile(x * 7 + y * 13, scale), (x0 + x * scale, y0 + y * scale))
    for x in range(room_cols):
        canvas.alpha_composite(scaled["crown"], (x0 + x * scale, y0 - scale))
        if x != room_cols // 2:
            canvas.alpha_composite(scaled["base"], (x0 + x * scale, y0 + room_rows * scale))
    for y in range(room_rows):
        canvas.alpha_composite(side_west, (x0 - scale, y0 + y * scale))
        canvas.alpha_composite(side_east, (x0 + room_cols * scale, y0 + y * scale))
    canvas.alpha_composite(corner_nw, (x0 - scale, y0 - scale))
    canvas.alpha_composite(corner_ne, (x0 + room_cols * scale, y0 - scale))
    canvas.alpha_composite(corner_sw, (x0 - scale, y0 + room_rows * scale))
    canvas.alpha_composite(corner_se, (x0 + room_cols * scale, y0 + room_rows * scale))
    canvas.alpha_composite(scaled["door_contact"], (x0 + room_cols // 2 * scale, y0 + room_rows * scale))
    draw.rectangle((x0, y0, x0 + room_cols * scale, y0 + room_rows * scale), outline=(28, 183, 200, 65), width=1)
    return canvas


def draw_orientation_preview(slices: dict[str, dict[str, Image.Image]]) -> None:
    previews = [draw_candidate_room(slices[candidate], candidate) for candidate in CANDIDATES]
    gap = 18
    width = sum(preview.width for preview in previews) + gap * (len(previews) + 1)
    height = max(preview.height for preview in previews) + gap * 2
    board = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    x = gap
    for preview in previews:
        board.alpha_composite(preview, (x, gap))
        x += preview.width + gap
    board.save(ROOT / ORIENTATION_PREVIEW)


def letterbox(image: Image.Image, size: tuple[int, int]) -> Image.Image:
    return ImageOps.contain(image.convert("RGBA"), size, Image.Resampling.LANCZOS)


def draw_comparison() -> None:
    font = ImageFont.load_default()
    preview_label = CANDIDATES[0].split("_", maxsplit=1)[0].upper()
    w02 = letterbox(Image.open(ROOT / W02_RUNTIME_CROP), (520, 380))
    reference = letterbox(Image.open(ROOT / REFERENCE), (520, 380))
    w03 = letterbox(Image.open(ROOT / ORIENTATION_PREVIEW), (1040, 380))
    gap = 18
    label_h = 24
    width = gap * 3 + 1040
    height = label_h * 2 + gap * 4 + 380 * 2
    board = Image.new("RGBA", (width, height), (5, 7, 10, 255))
    draw = ImageDraw.Draw(board)
    draw.text((gap, gap), "reference UI-demo-new", fill=(231, 225, 211, 255), font=font)
    board.alpha_composite(reference, (gap, gap + label_h))
    draw.text((gap * 2 + 520, gap), "W02 runtime map crop", fill=(231, 225, 211, 255), font=font)
    board.alpha_composite(w02, (gap * 2 + 520, gap + label_h))
    y = gap * 3 + label_h + 380
    draw.text((gap, y), f"{preview_label} orientation mirror/flip room preview", fill=(231, 225, 211, 255), font=font)
    board.alpha_composite(w03, (gap, y + label_h))
    board.save(ROOT / COMPARISON)


def main() -> int:
    source = Image.open(ROOT / SOURCE).convert("RGBA")
    slices = save_slices(source)
    draw_contact(slices)
    draw_orientation_preview(slices)
    draw_comparison()
    print(f"wrote {OUT_DIR}")
    print(f"wrote {CONTACT}")
    print(f"wrote {ORIENTATION_PREVIEW}")
    print(f"wrote {COMPARISON}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
