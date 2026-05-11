#!/usr/bin/env python3
"""Generate one image through Codex CLI and copy the newest generated image."""

from __future__ import annotations

import argparse
import errno
import hashlib
import json
import os
import pathlib
import select
import shutil
import subprocess
import sys
import tempfile
import time
from datetime import datetime, timezone
from typing import Iterable


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
    parser.add_argument(
        "--pty",
        action=argparse.BooleanOptionalAction,
        default=True,
        help="Run `codex exec` inside a pseudo-terminal on POSIX systems. Defaults to true.",
    )
    parser.add_argument(
        "--sandbox",
        choices=("read-only", "workspace-write", "danger-full-access"),
        default="read-only",
        help="Sandbox mode passed to the nested `codex exec`. Defaults to read-only.",
    )
    parser.add_argument(
        "--codex-workdir",
        type=pathlib.Path,
        default=None,
        help="Working directory for nested `codex exec`. Defaults to an isolated temporary directory.",
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


def parse_canvas_size(prompt: str) -> tuple[int, int] | None:
    for line in prompt.splitlines():
        label, separator, value = line.partition(":")
        if separator and label.strip() == "Canvas":
            width_text, size_separator, height_text = value.strip().lower().partition("x")
            if size_separator and width_text.isdigit() and height_text.isdigit():
                return int(width_text), int(height_text)
    return None


def copy_or_normalize_image(source: pathlib.Path, output: pathlib.Path, prompt: str) -> None:
    canvas_size = parse_canvas_size(prompt)
    if canvas_size is None:
        shutil.copy2(source, output)
        return

    image_module = load_pillow_image()
    with image_module.open(source) as image:
        normalized = remove_generated_checkerboard(image.convert("RGBA"))
        if normalized.size != canvas_size:
            normalized = fit_image_to_canvas(normalized, canvas_size, image_module)
        normalized.save(output)


def load_pillow_image():
    try:
        from PIL import Image
    except ModuleNotFoundError as exc:
        if exc.name == "PIL":
            raise RuntimeError("Pillow is required to normalize Codex image output to the prompt Canvas size.") from exc
        raise
    return Image


def fit_image_to_canvas(image, canvas_size: tuple[int, int], image_module):
    canvas_width, canvas_height = canvas_size
    source_width, source_height = image.size
    scale = min(canvas_width / source_width, canvas_height / source_height)
    fitted_size = (
        max(1, round(source_width * scale)),
        max(1, round(source_height * scale)),
    )
    if fitted_size != image.size:
        image = image.resize(fitted_size, image_module.Resampling.LANCZOS)
    canvas = image_module.new("RGBA", canvas_size, (0, 0, 0, 0))
    canvas.alpha_composite(image, ((canvas_width - image.width) // 2, (canvas_height - image.height) // 2))
    return canvas


def remove_generated_checkerboard(image):
    pixels = image.load()
    width, height = image.size
    light_neutral_count = 0
    sample_count = 0
    for x, y in sample_points(width, height):
        red, green, blue, alpha = pixels[x, y]
        if alpha == 255:
            sample_count += 1
            if is_generated_checker_pixel(red, green, blue):
                light_neutral_count += 1

    if sample_count == 0 or light_neutral_count / sample_count < 0.12:
        return image

    for y in range(height):
        for x in range(width):
            red, green, blue, alpha = pixels[x, y]
            if alpha == 255 and is_generated_checker_pixel(red, green, blue):
                pixels[x, y] = (red, green, blue, 0)
    return image


def sample_points(width: int, height: int) -> Iterable[tuple[int, int]]:
    step_x = max(1, width // 32)
    step_y = max(1, height // 32)
    for y in range(0, height, step_y):
        for x in range(0, width, step_x):
            yield x, y


def is_generated_checker_pixel(red: int, green: int, blue: int) -> bool:
    return min(red, green, blue) >= 225 and max(red, green, blue) - min(red, green, blue) <= 14


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


def run_codex_command(command: list[str], timeout_seconds: int, use_pty: bool) -> None:
    if use_pty and os.name == "posix":
        run_codex_command_with_pty(command, timeout_seconds)
        return

    subprocess.run(command, check=True, stdin=subprocess.DEVNULL, timeout=timeout_seconds)


def run_codex_command_with_pty(command: list[str], timeout_seconds: int) -> None:
    import pty

    master_fd, slave_fd = pty.openpty()
    output = bytearray()
    process = subprocess.Popen(
        command,
        stdin=slave_fd,
        stdout=slave_fd,
        stderr=slave_fd,
        close_fds=True,
    )
    os.close(slave_fd)
    deadline = time.monotonic() + timeout_seconds
    try:
        while True:
            remaining = deadline - time.monotonic()
            if remaining <= 0:
                process.kill()
                process.wait()
                tail = output[-4096:].decode("utf-8", errors="replace")
                raise TimeoutError(f"Codex CLI timed out after {timeout_seconds}s. Output tail:\n{tail}")

            readable, _, _ = select.select([master_fd], [], [], min(0.25, remaining))
            if readable:
                chunk = read_pty_chunk(master_fd)
                if chunk:
                    output.extend(chunk)
                    sys.stdout.buffer.write(chunk)
                    sys.stdout.buffer.flush()
                elif process.poll() is not None:
                    break

            if process.poll() is not None:
                drain_pty(master_fd, output)
                break

        exit_code = process.wait()
        if exit_code != 0:
            raise subprocess.CalledProcessError(exit_code, command)
    finally:
        os.close(master_fd)


def read_pty_chunk(master_fd: int) -> bytes:
    try:
        return os.read(master_fd, 4096)
    except OSError as exc:
        if exc.errno == errno.EIO:
            return b""
        raise


def drain_pty(master_fd: int, output: bytearray) -> None:
    while True:
        readable, _, _ = select.select([master_fd], [], [], 0)
        if not readable:
            return
        chunk = read_pty_chunk(master_fd)
        if not chunk:
            return
        output.extend(chunk)
        sys.stdout.buffer.write(chunk)
        sys.stdout.buffer.flush()


def build_image_only_prompt(prompt: str) -> str:
    return "\n".join(
        [
            "Generate exactly one image from the prompt below using image generation.",
            "Do not inspect, read, create, edit, or delete repository or workspace files.",
            "Do not run shell commands. Do not transform or post-process the image.",
            "Do not save the image to the requested output path; this wrapper script will normalize and copy the newest generated image after Codex exits.",
            "Return only after the generated image artifact exists.",
            "",
            "<image_prompt>",
            prompt.strip(),
            "</image_prompt>",
        ]
    )


def codex_workdir_context(path: pathlib.Path | None):
    if path is not None:
        path.mkdir(parents=True, exist_ok=True)
        return StaticWorkdir(path)
    return tempfile.TemporaryDirectory(prefix="codex-image-")


class StaticWorkdir:
    def __init__(self, path: pathlib.Path) -> None:
        self.path = path

    def __enter__(self) -> str:
        return str(self.path)

    def __exit__(self, exc_type, exc, traceback) -> None:
        return None


def main() -> int:
    args = parse_args()
    output = args.out
    if output.exists() and not args.overwrite:
        raise FileExistsError(f"Output already exists; pass --overwrite to replace it: {output}")

    generated_root = args.generated_dir.expanduser()
    before_dirs = snapshot_directories(generated_root)
    started_at_ns = time.time_ns()
    try:
        with codex_workdir_context(args.codex_workdir) as codex_workdir:
            command = ["codex", "exec", "--cd", codex_workdir, "--sandbox", args.sandbox]
            if args.skip_git_repo_check:
                command.append("--skip-git-repo-check")
            command.append(build_image_only_prompt(args.prompt))
            run_codex_command(command, args.timeout_seconds, args.pty)
    except subprocess.TimeoutExpired as exc:
        raise TimeoutError(f"Codex CLI timed out after {args.timeout_seconds}s.") from exc
    finished_at_ns = time.time_ns()

    latest_folder = newest_directory(generated_root, started_at_ns, before_dirs)
    latest_image = newest_image(latest_folder)

    output.parent.mkdir(parents=True, exist_ok=True)
    copy_or_normalize_image(latest_image, output, args.prompt)
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
