#!/usr/bin/env python3
"""Validate repository-owned content pack runtime manifest schema versions."""

from __future__ import annotations

import argparse
import re
from pathlib import Path


SCHEMA_VERSION_PATTERN = re.compile(r"^schemaVersion:\s*(\d+)\s*$")
CONTENT_PACK_SCHEMA_VERSION_SOURCE = Path("game/src/main/kotlin/com/ktome/game/contentpack/ContentPackModels.kt")
CONTENT_PACK_SCHEMA_VERSION_PATTERN = re.compile(r"^\s*const\s+val\s+SCHEMA_VERSION:\s*Int\s*=\s*(\d+)\s*$")
RUNTIME_MANIFEST_ROOTS = [
    Path("examples/content-packs"),
    Path("tools/src/main/resources/fixtures/content-packs/packs"),
]


def runtime_manifest_paths(repo_root: Path) -> list[Path]:
    paths: list[Path] = []
    for root in (repo_root / manifest_root for manifest_root in RUNTIME_MANIFEST_ROOTS):
        if root.exists():
            paths.extend(path for path in root.rglob("manifest.yaml") if path.is_file())
    return sorted(paths)


def schema_version(manifest_path: Path) -> int | None:
    for line in manifest_path.read_text(encoding="utf-8").splitlines():
        match = SCHEMA_VERSION_PATTERN.match(line)
        if match:
            return int(match.group(1))
    return None


def expected_content_pack_schema_version(repo_root: Path) -> int:
    source_path = repo_root / CONTENT_PACK_SCHEMA_VERSION_SOURCE
    matches = [
        int(match.group(1))
        for line in source_path.read_text(encoding="utf-8").splitlines()
        if (match := CONTENT_PACK_SCHEMA_VERSION_PATTERN.match(line))
    ]
    if len(matches) != 1:
        raise RuntimeError(
            f"Expected exactly one ContentPackManifest.SCHEMA_VERSION declaration in "
            f"{CONTENT_PACK_SCHEMA_VERSION_SOURCE}; found {len(matches)}."
        )
    return matches[0]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo-root", default=".")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve()
    expected_schema_version = expected_content_pack_schema_version(repo_root)
    manifests = runtime_manifest_paths(repo_root)
    if not manifests:
        expected_roots = ", ".join(str(root) for root in RUNTIME_MANIFEST_ROOTS)
        print(f"No repository content pack runtime manifests found under: {expected_roots}.")
        return 1

    failures: list[str] = []
    for manifest in manifests:
        actual = schema_version(manifest)
        if actual != expected_schema_version:
            failures.append(f"{manifest.relative_to(repo_root)}: schemaVersion={actual!r}")

    if failures:
        print(f"Runtime content pack manifests must use schemaVersion={expected_schema_version}:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print(f"Validated {len(manifests)} runtime content pack manifests at schemaVersion={expected_schema_version}.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
