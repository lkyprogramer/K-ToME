#!/usr/bin/env python3
"""Build W04 full-tile wall-family candidate previews.

This wrapper reuses the W03 evidence-only preview builder with W04 paths and
candidate names. It does not mutate runtime resources, manifests, or sprite
sheet contracts.
"""

from __future__ import annotations

import importlib.util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[5]
W03_SCRIPT = (
    ROOT /
    "UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/"
    "orientation-wall-family-w03/build_orientation_wall_candidate_preview.py"
)


def load_w03_builder():
    spec = importlib.util.spec_from_file_location("w03_wall_preview_builder", W03_SCRIPT)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"Unable to load W03 preview builder: {W03_SCRIPT}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def main() -> int:
    builder = load_w03_builder()
    work_dir = Path("UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/full-tile-wall-family-w04")
    builder.WORK_DIR = work_dir
    builder.SOURCE = work_dir / "pr08-wall-family-w04-imagegen-source-board.png"
    builder.OUT_DIR = work_dir / "slices"
    builder.CONTACT = work_dir / "pr08-wall-family-w04-contact-board.png"
    builder.ORIENTATION_PREVIEW = work_dir / "pr08-wall-family-w04-orientation-room-preview.png"
    builder.COMPARISON = work_dir / "pr08-wall-family-w04-w02-preview-comparison.png"
    builder.CANDIDATES = ["w04_a", "w04_b", "w04_c"]
    return builder.main()


if __name__ == "__main__":
    raise SystemExit(main())
