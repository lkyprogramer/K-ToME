#!/usr/bin/env python3
"""Post-process Gemini raw image assets into runtime-ready PNGs.

Pipeline:
raw/generated -> background removal -> trim/crop -> resize/pad -> optimized PNG
"""

from __future__ import annotations

import argparse
import json
import pathlib
import shutil
from collections import deque
from dataclasses import dataclass
from datetime import datetime, timezone

from PIL import Image

from asset_pipeline_common import collect_assets, load_yaml


SEED_BRIGHTNESS_THRESHOLD = 150
PIXEL_BRIGHTNESS_THRESHOLD = 145
SEED_SPREAD_THRESHOLD = 100
PIXEL_SPREAD_THRESHOLD = 110
COLOR_DISTANCE_THRESHOLD = 115
ALPHA_THRESHOLD = 8

CANVAS_BY_CATEGORY = {
    "tile_ground": 256,
    "tile_wall": 256,
    "tile_decal": 256,
    "prop_interactable": 256,
    "prop_environment": 256,
    "actor_sprite": 256,
    "portrait": 256,
    "icon": 160,
    "icon_skill": 160,
    "icon_status": 160,
    "icon_damage_type": 160,
    "icon_item": 160,
    "icon_quest": 160,
    "ui_frame": 256,
    "vfx_plate": 192,
}

PADDING_BY_CATEGORY = {
    "tile_ground": 0.03,
    "tile_wall": 0.03,
    "tile_decal": 0.03,
    "prop_interactable": 0.06,
    "prop_environment": 0.06,
    "actor_sprite": 0.06,
    "portrait": 0.05,
    "icon": 0.1,
    "icon_skill": 0.1,
    "icon_status": 0.1,
    "icon_damage_type": 0.1,
    "icon_item": 0.1,
    "icon_quest": 0.1,
    "ui_frame": 0.05,
    "vfx_plate": 0.08,
}

BOTTOM_ALIGNED_CATEGORIES = {"prop_interactable", "prop_environment", "actor_sprite"}


@dataclass(frozen=True)
class AssetSpec:
    gate_id: str
    asset_id: str
    category: str
    output_name: str
    visual_key: str


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Post-process generated Phase 2 image assets")
    parser.add_argument(
        "--plan",
        default="assets-src/image/specs/phase2-asset-plan.yaml",
        help="Path to the YAML asset plan",
    )
    parser.add_argument(
        "--raw-dir",
        default="assets-src/image/raw/generated",
        help="Directory containing raw generated PNGs",
    )
    parser.add_argument(
        "--processed-dir",
        default="assets-src/image/processed",
        help="Directory for processed PNGs",
    )
    parser.add_argument(
        "--runtime-dir",
        default="client/src/main/resources",
        help="Runtime resource root to sync processed PNGs into",
    )
    parser.add_argument(
        "--report",
        default="assets-src/image/manifests/phase2-processing-report.jsonl",
        help="JSONL report path for processed assets",
    )
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="Skip assets whose processed output already exists.",
    )
    return parser.parse_args()


def parse_specs(plan_path: pathlib.Path) -> list[AssetSpec]:
    plan = load_yaml(plan_path)
    return [
        AssetSpec(
            gate_id=asset["_gateId"],
            asset_id=str(asset["id"]),
            category=str(asset["category"]),
            output_name=str(asset["outputName"]),
            visual_key=str(asset["visualKey"]),
        )
        for asset in collect_assets(plan)
    ]


def rgb_distance(left: tuple[int, int, int], right: tuple[int, int, int]) -> int:
    return abs(left[0] - right[0]) + abs(left[1] - right[1]) + abs(left[2] - right[2])


def is_light_neutral(pixel: tuple[int, int, int]) -> bool:
    avg = sum(pixel) / 3.0
    spread = max(pixel) - min(pixel)
    return avg >= SEED_BRIGHTNESS_THRESHOLD and spread <= SEED_SPREAD_THRESHOLD


def build_seed_palette(image: Image.Image) -> list[tuple[int, int, int]]:
    width, height = image.size
    palette: list[tuple[int, int, int]] = []
    border_points = [(x, 0) for x in range(width)] + [(x, height - 1) for x in range(width)]
    border_points += [(0, y) for y in range(height)] + [(width - 1, y) for y in range(height)]
    for point in border_points:
        pixel = image.getpixel(point)
        avg = sum(pixel) / 3.0
        spread = max(pixel) - min(pixel)
        if avg < SEED_BRIGHTNESS_THRESHOLD or spread > SEED_SPREAD_THRESHOLD:
            continue
        if not any(rgb_distance(pixel, existing) <= 24 for existing in palette):
            palette.append(pixel)
        if len(palette) >= 24:
            break
    if not palette:
        palette.append((255, 255, 255))
    return palette


def is_background_pixel(
    pixel: tuple[int, int, int],
    seed_palette: list[tuple[int, int, int]],
) -> bool:
    avg = sum(pixel) / 3.0
    spread = max(pixel) - min(pixel)
    if avg < PIXEL_BRIGHTNESS_THRESHOLD or spread > PIXEL_SPREAD_THRESHOLD:
        return False
    return min(rgb_distance(pixel, seed) for seed in seed_palette) <= COLOR_DISTANCE_THRESHOLD


def remove_background(image: Image.Image) -> Image.Image:
    rgb = image.convert("RGB")
    rgba = image.convert("RGBA")
    width, height = rgba.size
    seed_palette = build_seed_palette(rgb)
    visited = bytearray(width * height)
    alpha = rgba.getchannel("A")
    alpha_pixels = alpha.load()
    rgb_pixels = rgb.load()
    queue: deque[tuple[int, int]] = deque()

    def enqueue(x: int, y: int) -> None:
        index = y * width + x
        if visited[index]:
            return
        pixel = rgb_pixels[x, y]
        if not is_background_pixel(pixel, seed_palette):
            return
        visited[index] = 1
        queue.append((x, y))

    for x in range(width):
        enqueue(x, 0)
        enqueue(x, height - 1)
    for y in range(height):
        enqueue(0, y)
        enqueue(width - 1, y)

    while queue:
        x, y = queue.popleft()
        alpha_pixels[x, y] = 0
        for next_x, next_y in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= next_x < width and 0 <= next_y < height:
                enqueue(next_x, next_y)

    rgba.putalpha(alpha)
    return rgba


def crop_to_content(image: Image.Image, category: str) -> Image.Image:
    alpha = image.getchannel("A")
    bbox = alpha.point(lambda value: 255 if value > ALPHA_THRESHOLD else 0).getbbox()
    if bbox is None:
        return image

    width, height = image.size
    pad_ratio = PADDING_BY_CATEGORY.get(category, 0.05)
    pad_x = max(1, int((bbox[2] - bbox[0]) * pad_ratio))
    pad_y = max(1, int((bbox[3] - bbox[1]) * pad_ratio))
    padded_box = (
        max(0, bbox[0] - pad_x),
        max(0, bbox[1] - pad_y),
        min(width, bbox[2] + pad_x),
        min(height, bbox[3] + pad_y),
    )
    return image.crop(padded_box)


def resize_to_canvas(image: Image.Image, category: str) -> Image.Image:
    canvas_size = CANVAS_BY_CATEGORY.get(category, 256)
    result = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    width, height = image.size
    scale = min(canvas_size / width, canvas_size / height)
    target_size = (
        max(1, int(round(width * scale))),
        max(1, int(round(height * scale))),
    )
    resized = image.resize(target_size, Image.Resampling.LANCZOS)

    offset_x = (canvas_size - target_size[0]) // 2
    if category in BOTTOM_ALIGNED_CATEGORIES:
        offset_y = canvas_size - target_size[1]
    else:
        offset_y = (canvas_size - target_size[1]) // 2
    result.alpha_composite(resized, (offset_x, max(0, offset_y)))
    return result


def process_image(
    source_path: pathlib.Path,
    category: str,
) -> Image.Image:
    raw = Image.open(source_path).convert("RGBA")
    with_alpha = remove_background(raw)
    cropped = crop_to_content(with_alpha, category)
    return resize_to_canvas(cropped, category)


def write_report(report_path: pathlib.Path, records: list[dict[str, object]]) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    with report_path.open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")


def main() -> int:
    args = parse_args()
    plan_path = pathlib.Path(args.plan).resolve()
    raw_dir = pathlib.Path(args.raw_dir).resolve()
    processed_dir = pathlib.Path(args.processed_dir).resolve()
    runtime_dir = pathlib.Path(args.runtime_dir).resolve()
    report_path = pathlib.Path(args.report).resolve()

    specs = parse_specs(plan_path)
    processed_dir.mkdir(parents=True, exist_ok=True)
    records: list[dict[str, object]] = []

    for spec in specs:
        raw_path = raw_dir / spec.output_name
        if not raw_path.is_file():
            print(f"ERROR: missing raw asset for {spec.asset_id}: {raw_path}")
            return 1

        processed_path = processed_dir / spec.output_name
        runtime_path = runtime_dir / spec.output_name
        processed_path.parent.mkdir(parents=True, exist_ok=True)
        runtime_path.parent.mkdir(parents=True, exist_ok=True)

        if args.skip_existing and processed_path.is_file():
            if not runtime_path.is_file():
                shutil.copy2(processed_path, runtime_path)
            with Image.open(processed_path) as existing_image:
                records.append(
                    {
                        "processedAt": datetime.now(timezone.utc).isoformat(),
                        "status": "skipped",
                        "gateId": spec.gate_id,
                        "assetId": spec.asset_id,
                        "category": spec.category,
                        "visualKey": spec.visual_key,
                        "rawPath": str(raw_path),
                        "processedPath": str(processed_path),
                        "runtimePath": str(runtime_path),
                        "rawBytes": raw_path.stat().st_size,
                        "processedBytes": processed_path.stat().st_size,
                        "processedSize": list(existing_image.size),
                    },
                )
            print(f"[skip] {spec.asset_id} -> {processed_path}")
            continue

        source_bytes = raw_path.stat().st_size
        processed = process_image(raw_path, spec.category)
        processed.save(processed_path, optimize=True, compress_level=9)
        shutil.copy2(processed_path, runtime_path)
        processed_bytes = processed_path.stat().st_size

        records.append(
            {
                "processedAt": datetime.now(timezone.utc).isoformat(),
                "status": "processed",
                "gateId": spec.gate_id,
                "assetId": spec.asset_id,
                "category": spec.category,
                "visualKey": spec.visual_key,
                "rawPath": str(raw_path),
                "processedPath": str(processed_path),
                "runtimePath": str(runtime_path),
                "rawBytes": source_bytes,
                "processedBytes": processed_bytes,
                "processedSize": list(processed.size),
            },
        )
        print(
            f"[processed] {spec.asset_id} -> {processed_path} "
            f"({source_bytes}B -> {processed_bytes}B, {processed.size[0]}x{processed.size[1]})"
        )

    write_report(report_path, records)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
