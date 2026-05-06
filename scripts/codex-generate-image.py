#!/usr/bin/env python3
"""Generate one image through Codex CLI and copy the newest generated image."""

from __future__ import annotations

import argparse
import hashlib
import pathlib
import shutil
import subprocess
import sys
import time


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
        "--skip-git-repo-check",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Pass --skip-git-repo-check to `codex exec` by default.",
    )
    return parser.parse_args()


def newest_directory(root: pathlib.Path, started_at_ns: int) -> pathlib.Path:
    if not root.is_dir():
        raise FileNotFoundError(f"Generated image directory does not exist: {root}")
    candidates = [path for path in root.iterdir() if path.is_dir()]
    if not candidates:
        raise FileNotFoundError(f"No generated image folders found under: {root}")
    candidates.sort(key=lambda path: path.stat().st_mtime_ns, reverse=True)
    selected = candidates[0]
    if selected.stat().st_mtime_ns < started_at_ns:
        raise RuntimeError(
            "Codex CLI completed but no newer generated image folder was found under "
            f"{root}. Latest folder: {selected}"
        )
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


def main() -> int:
    args = parse_args()
    output = args.out
    if output.exists() and not args.overwrite:
        raise FileExistsError(f"Output already exists; pass --overwrite to replace it: {output}")

    command = ["codex", "exec", args.prompt]
    if args.skip_git_repo_check:
        command.append("--skip-git-repo-check")

    started_at_ns = time.time_ns()
    subprocess.run(command, check=True, stdin=subprocess.DEVNULL)

    latest_folder = newest_directory(args.generated_dir.expanduser(), started_at_ns)
    latest_image = newest_image(latest_folder)

    output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(latest_image, output)

    print(f"[codex-image] source_folder={latest_folder}")
    print(f"[codex-image] source_image={latest_image}")
    print(f"[codex-image] output={output}")
    print(f"[codex-image] sha256={sha256(output)}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"[codex-image] ERROR: {exc}", file=sys.stderr)
        raise SystemExit(1)
