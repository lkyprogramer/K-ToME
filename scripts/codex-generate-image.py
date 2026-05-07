#!/usr/bin/env python3
"""Generate one image through Codex CLI and copy the newest generated image."""

from __future__ import annotations

import argparse
import hashlib
import json
import pathlib
import shutil
import subprocess
import sys
import time
from datetime import datetime, timezone


DEFAULT_GENERATED_DIR = pathlib.Path.home() / ".codex" / "generated_images"
IMAGE_SUFFIXES = {".png", ".jpg", ".jpeg", ".webp"}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Run `codex exec` for an image prompt, select the newest image under "
            "~/.codex/generated_images, and copy it to the requested output path."
        )
    )
    parser.add_argument("prompt", help="Prompt passed to `codex exec`.")
    parser.add_argument(
        "--out",
        required=True,
        type=pathlib.Path,
        help="Destination image path. Parent directories are created automatically.",
    )
    parser.add_argument(
        "--generated-dir",
        type=pathlib.Path,
        default=DEFAULT_GENERATED_DIR,
        help="Codex generated images root. Defaults to ~/.codex/generated_images.",
    )
    parser.add_argument(
        "--overwrite",
        action="store_true",
        help="Allow replacing an existing output file.",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=int,
        default=300,
        help="Maximum time to wait for `codex exec` before failing. Defaults to 300.",
    )
    parser.add_argument(
        "--smoke-report",
        type=pathlib.Path,
        default=None,
        help="Optional JSON report path with selected source image, output path, and hash.",
    )
    parser.add_argument(
        "--skip-git-repo-check",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Pass --skip-git-repo-check to `codex exec` by default.",
    )
    return parser.parse_args()


def snapshot_directories(root: pathlib.Path) -> set[pathlib.Path]:
    if not root.is_dir():
        return set()
    return {path.resolve() for path in root.iterdir() if path.is_dir()}


def newest_directory(root: pathlib.Path, started_at_ns: int, before_dirs: set[pathlib.Path]) -> pathlib.Path:
    if not root.is_dir():
        raise FileNotFoundError(f"Generated image directory does not exist: {root}")
    candidates = [path for path in root.iterdir() if path.is_dir()]
    if not candidates:
        raise FileNotFoundError(f"No generated image folders found under: {root}")
    new_candidates = [path for path in candidates if path.resolve() not in before_dirs]
    if new_candidates:
        candidates = new_candidates
    else:
        candidates = [path for path in candidates if path.stat().st_mtime_ns >= started_at_ns]
    if not candidates:
        raise RuntimeError(
            "Codex CLI completed but no generated image folder created or touched during this run under "
            f"{root}."
        )
    candidates.sort(key=lambda path: (path.stat().st_mtime_ns, path.name), reverse=True)
    selected = candidates[0]
    return selected


def newest_image(folder: pathlib.Path) -> pathlib.Path:
    images = [
        path
        for path in folder.iterdir()
        if path.is_file() and path.suffix.lower() in IMAGE_SUFFIXES
    ]
    if not images:
        raise FileNotFoundError(f"No image files found in latest generated folder: {folder}")
    images.sort(key=lambda path: path.stat().st_mtime_ns, reverse=True)
    return images[0]


def sha256(path: pathlib.Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def write_smoke_report(
    path: pathlib.Path,
    prompt: str,
    source_folder: pathlib.Path,
    source_image: pathlib.Path,
    output: pathlib.Path,
    output_hash: str,
    started_at_ns: int,
    finished_at_ns: int,
) -> None:
    report = {
        "schemaVersion": "codex-image-smoke-v1",
        "generatedAt": datetime.now(timezone.utc).replace(microsecond=0).isoformat(),
        "promptHash": hashlib.sha256(prompt.encode("utf-8")).hexdigest(),
        "sourceFolder": source_folder.as_posix(),
        "sourceImage": source_image.as_posix(),
        "output": output.as_posix(),
        "sha256": output_hash,
        "startedAtNs": started_at_ns,
        "finishedAtNs": finished_at_ns,
    }
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> int:
    args = parse_args()
    output = args.out
    if output.exists() and not args.overwrite:
        raise FileExistsError(f"Output already exists; pass --overwrite to replace it: {output}")

    command = ["codex", "exec", args.prompt]
    if args.skip_git_repo_check:
        command.append("--skip-git-repo-check")

    generated_root = args.generated_dir.expanduser()
    before_dirs = snapshot_directories(generated_root)
    started_at_ns = time.time_ns()
    try:
        subprocess.run(command, check=True, stdin=subprocess.DEVNULL, timeout=args.timeout_seconds)
    except subprocess.TimeoutExpired as exc:
        raise TimeoutError(f"Codex CLI timed out after {args.timeout_seconds}s.") from exc
    finished_at_ns = time.time_ns()

    latest_folder = newest_directory(generated_root, started_at_ns, before_dirs)
    latest_image = newest_image(latest_folder)

    output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(latest_image, output)
    output_hash = sha256(output)
    if args.smoke_report:
        write_smoke_report(
            args.smoke_report,
            args.prompt,
            latest_folder,
            latest_image,
            output,
            output_hash,
            started_at_ns,
            finished_at_ns,
        )

    print(f"[codex-image] source_folder={latest_folder}")
    print(f"[codex-image] source_image={latest_image}")
    print(f"[codex-image] output={output}")
    print(f"[codex-image] sha256={output_hash}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"[codex-image] ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
