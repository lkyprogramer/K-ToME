#!/usr/bin/env python3
"""Sync canonical Phase 2 manifests from assets-src into runtime resources."""

from __future__ import annotations

import json
import pathlib
import shutil


MANIFEST_PAIRS = (
    ("assets-src/image/manifests/phase2-visual-manifest.json", "client/src/main/resources/manifests/visual-manifest.json"),
    ("assets-src/audio/manifests/phase2-audio-manifest.json", "client/src/main/resources/manifests/audio-manifest.json"),
)


def sync_file(source: pathlib.Path, target: pathlib.Path) -> None:
    payload = json.loads(source.read_text(encoding="utf-8"))
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    shutil.copystat(source, target, follow_symlinks=True)
    print(f"[synced] {source} -> {target}")


def main() -> int:
    root = pathlib.Path(__file__).resolve().parent.parent
    for source_relative, target_relative in MANIFEST_PAIRS:
        source = root / source_relative
        target = root / target_relative
        if not source.is_file():
            raise FileNotFoundError(f"Canonical manifest not found: {source}")
        sync_file(source, target)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
