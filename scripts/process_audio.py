#!/usr/bin/env python3
"""Build the Phase 2 audio runtime bundle from raw sources."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
import random
import shutil
import struct
import subprocess
import wave
from dataclasses import dataclass
from datetime import datetime, timezone

from asset_pipeline_common import load_json, load_yaml


SUPPORTED_RAW_SUFFIXES = (".wav", ".ogg", ".mp3", ".flac", ".aif", ".aiff", ".m4a")
TARGET_SAMPLE_RATE = 44_100
TARGET_CHANNELS = 1
REPO_ROOT = pathlib.Path(__file__).resolve().parents[1]


def display_path(path: pathlib.Path) -> str:
    resolved = path.resolve()
    try:
        return resolved.relative_to(REPO_ROOT).as_posix()
    except ValueError:
        return str(resolved)


@dataclass(frozen=True)
class AudioAsset:
    source_path: str
    cue_family: str
    event_ids: tuple[str, ...]
    keys: tuple[str, ...]


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Process Phase 2 audio into runtime-ready OGG files")
    parser.add_argument(
        "--runtime-manifest",
        default="client/src/main/resources/manifests/audio-manifest.json",
        help="Bundled runtime audio manifest JSON",
    )
    parser.add_argument(
        "--raw-dir",
        default="assets-src/audio/raw",
        help="Directory containing raw imported audio",
    )
    parser.add_argument(
        "--cleaned-dir",
        default="assets-src/audio/cleaned",
        help="Directory for cleaned audio outputs",
    )
    parser.add_argument(
        "--runtime-root",
        default="client/src/main/resources",
        help="Runtime resource root to sync processed audio into",
    )
    parser.add_argument(
        "--report",
        default="assets-src/audio/manifests/phase2-processing-report.jsonl",
        help="JSONL report path for processed audio assets",
    )
    parser.add_argument(
        "--filter-plan",
        help="Optional audio plan YAML used to restrict processing to a subset of manifest entries.",
    )
    parser.add_argument(
        "--skip-existing",
        action="store_true",
        help="Skip assets whose cleaned output already exists.",
    )
    parser.add_argument(
        "--bootstrap-missing",
        action="store_true",
        help="Generate deterministic placeholder raw audio when a source file is missing.",
    )
    return parser.parse_args()


def require_tool(name: str) -> str:
    path = shutil.which(name)
    if path is None:
        raise RuntimeError(f"Required tool '{name}' was not found on PATH.")
    return path


def select_primary_cue_family(cue_families: list[str]) -> str:
    family_set = set(cue_families)
    if family_set == {"ambience", "zone"}:
        return "ambience"
    if "talent" in family_set:
        return "talent"
    if "profession" in family_set:
        return "profession"
    if "resource" in family_set:
        return "resource"
    if "damage" in family_set:
        return "damage"
    return cue_families[0]


def parse_manifest(manifest_path: pathlib.Path) -> list[AudioAsset]:
    payload = load_json(manifest_path)
    entries = payload.get("entries")
    if not isinstance(entries, list) or not entries:
        raise ValueError(f"Audio manifest entries must be a non-empty list: {manifest_path}")

    grouped: dict[str, dict[str, object]] = {}
    for entry in entries:
        if not isinstance(entry, dict):
            raise ValueError(f"Audio manifest entry must be a mapping: {manifest_path}")
        source_path = str(entry.get("sourcePath", "")).strip()
        cue_family = str(entry.get("cueFamily", "")).strip()
        event_id = str(entry.get("eventId", "")).strip()
        key = str(entry.get("key", "")).strip()
        if not source_path or not cue_family or not event_id or not key:
            raise ValueError(f"Audio manifest entry must define key/cueFamily/eventId/sourcePath: {entry}")
        bucket = grouped.setdefault(
            source_path,
            {"cueFamilies": set(), "eventIds": [], "keys": []},
        )
        cast_families = bucket["cueFamilies"]
        assert isinstance(cast_families, set)
        cast_families.add(cue_family)
        bucket["eventIds"].append(event_id)
        bucket["keys"].append(key)

    assets: list[AudioAsset] = []
    for source_path, bucket in sorted(grouped.items()):
        cue_families = sorted(str(value) for value in bucket["cueFamilies"])
        assets.append(
            AudioAsset(
                source_path=source_path,
                cue_family=select_primary_cue_family(cue_families),
                event_ids=tuple(sorted(set(bucket["eventIds"]))),
                keys=tuple(sorted(set(bucket["keys"]))),
            ),
        )
    return assets


def load_plan_source_paths(plan_path: pathlib.Path) -> set[str]:
    payload = load_yaml(plan_path)
    entries = payload.get("entries")
    if not isinstance(entries, list) or not entries:
        raise ValueError(f"Audio plan entries must be a non-empty list: {plan_path}")

    source_paths: set[str] = set()
    for entry in entries:
        if not isinstance(entry, dict):
            raise ValueError(f"Audio plan entry must be a mapping: {plan_path}")
        source_path = str(entry.get("sourcePath", "")).strip()
        key = str(entry.get("key", "")).strip()
        if not source_path or not key:
            raise ValueError(f"Audio plan entry must define key/sourcePath: {entry}")
        source_paths.add(source_path)
    return source_paths


def filter_assets_by_plan(
    assets: list[AudioAsset],
    plan_path: pathlib.Path,
) -> list[AudioAsset]:
    planned_source_paths = load_plan_source_paths(plan_path)
    filtered_assets = [asset for asset in assets if asset.source_path in planned_source_paths]
    matched_source_paths = {asset.source_path for asset in filtered_assets}
    missing_source_paths = sorted(planned_source_paths - matched_source_paths)
    if missing_source_paths:
        raise ValueError(
            "Audio plan references sourcePath values missing from the runtime manifest: "
            + ", ".join(missing_source_paths),
        )
    return filtered_assets


def raw_candidates(raw_dir: pathlib.Path, source_path: str) -> list[pathlib.Path]:
    relative = pathlib.Path(source_path)
    stem = relative.with_suffix("")
    return [raw_dir / relative] + [raw_dir / stem.with_suffix(suffix) for suffix in SUPPORTED_RAW_SUFFIXES if suffix != relative.suffix]


def resolve_input_path(raw_dir: pathlib.Path, runtime_root: pathlib.Path, source_path: str) -> pathlib.Path | None:
    for candidate in raw_candidates(raw_dir, source_path):
        if candidate.is_file():
            return candidate
    runtime_path = runtime_root / source_path
    if runtime_path.is_file():
        return runtime_path
    return None


def stable_seed(identifier: str) -> int:
    return int(hashlib.sha256(identifier.encode("utf-8")).hexdigest()[:16], 16)


def generate_placeholder_wav(output_path: pathlib.Path, cue_family: str, identifier: str) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    rng = random.Random(stable_seed(identifier))
    normalized_id = identifier.lower()
    if cue_family == "ambience":
        duration_s = 5.2 if "shattered_outpost" in normalized_id else 4.0
    elif cue_family == "music":
        duration_s = 8.0 if "menu" in normalized_id else 6.0
    elif cue_family == "interactable":
        duration_s = 0.45
    elif cue_family == "profession":
        duration_s = 1.35 if "vanguard" in normalized_id or "arcanist" in normalized_id else 1.2
    elif cue_family == "monster":
        duration_s = 0.95 if "bandit" in normalized_id else 0.8
    elif cue_family == "ui":
        duration_s = 0.52 + (stable_seed(identifier) % 5) * 0.06
    elif cue_family == "silence":
        duration_s = 0.1
    else:
        duration_s = 0.8

    frame_count = int(TARGET_SAMPLE_RATE * duration_s)
    samples = bytearray()

    base_frequency = 180 + (stable_seed(identifier) % 220)
    secondary_frequency = max(80, base_frequency // 2)

    for index in range(frame_count):
        t = index / TARGET_SAMPLE_RATE
        if cue_family == "ambience":
            envelope = min(1.0, t / 0.8, (duration_s - t) / 0.8)
            if "shattered_outpost" in normalized_id:
                wind = (
                    math.sin(2.0 * math.pi * 46 * t) * 0.08
                    + math.sin(2.0 * math.pi * 91 * t) * 0.05
                )
                whistle = math.sin(2.0 * math.pi * (312 + math.sin(2.0 * math.pi * 0.16 * t) * 18) * t) * 0.035
                rubble = (rng.random() * 2.0 - 1.0) * (0.018 + max(0.0, math.sin(2.0 * math.pi * 0.42 * t)) * 0.02)
                value = (wind + whistle + rubble) * envelope
            else:
                noise = (rng.random() * 2.0 - 1.0) * 0.03
                value = (
                    math.sin(2.0 * math.pi * base_frequency * t) * 0.08
                    + math.sin(2.0 * math.pi * secondary_frequency * t) * 0.05
                    + math.sin(2.0 * math.pi * 0.35 * t) * 0.04
                    + noise
                ) * envelope
        elif cue_family == "music":
            envelope = min(1.0, t / 1.0, (duration_s - t) / 1.2)
            if "menu" in normalized_id:
                drone = math.sin(2.0 * math.pi * secondary_frequency * t) * 0.07
                lead = (
                    math.sin(2.0 * math.pi * base_frequency * t) * 0.08
                    + math.sin(2.0 * math.pi * (base_frequency * 1.26) * t) * 0.06
                )
                chime = math.sin(2.0 * math.pi * (base_frequency * 2.02) * t + math.sin(2.0 * math.pi * 0.28 * t)) * 0.03
                pulse = math.sin(2.0 * math.pi * 0.125 * t) * 0.025
                value = (drone + lead + chime + pulse) * envelope
            else:
                chord = (
                    math.sin(2.0 * math.pi * base_frequency * t) * 0.10
                    + math.sin(2.0 * math.pi * (base_frequency * 1.25) * t) * 0.07
                    + math.sin(2.0 * math.pi * (base_frequency * 1.5) * t) * 0.05
                )
                pulse = math.sin(2.0 * math.pi * 0.25 * t) * 0.03
                value = (chord + pulse) * envelope
        elif cue_family == "interactable":
            pulse_times = (0.0, 0.12, 0.24)
            value = 0.0
            for pulse_index, pulse_start in enumerate(pulse_times):
                age = t - pulse_start
                if 0.0 <= age <= 0.09:
                    frequency = 900 - pulse_index * 120
                    decay = math.exp(-age * 28.0)
                    value += math.sin(2.0 * math.pi * frequency * age) * 0.28 * decay
        elif cue_family == "ui":
            pulse_start = 0.02
            age = t - pulse_start
            if 0.0 <= age <= 0.12:
                frequency = base_frequency + 920
                value = math.sin(2.0 * math.pi * frequency * age) * 0.24 * math.exp(-age * 24.0)
            else:
                value = 0.0
        elif cue_family == "footstep":
            value = 0.0
            for pulse_start in (0.0, 0.18):
                age = t - pulse_start
                if 0.0 <= age <= 0.09:
                    noise = (rng.random() * 2.0 - 1.0) * 0.12
                    decay = math.exp(-age * 32.0)
                    value += (math.sin(2.0 * math.pi * 120 * age) * 0.12 + noise) * decay
        elif cue_family == "melee":
            age = t
            if 0.0 <= age <= 0.2:
                slash = math.sin(2.0 * math.pi * 420 * age) * 0.18
                impact = math.sin(2.0 * math.pi * 90 * age) * 0.14
                value = (slash + impact) * math.exp(-age * 18.0)
            else:
                value = 0.0
        elif cue_family == "spell":
            envelope = min(1.0, t / 0.08, max(0.0, (duration_s - t) / 0.22))
            value = (
                math.sin(2.0 * math.pi * (base_frequency + 220) * t) * 0.10
                + math.sin(2.0 * math.pi * (base_frequency + 510) * t) * 0.08
                + math.sin(2.0 * math.pi * 6.0 * t) * 0.05
            ) * envelope
        elif cue_family == "monster":
            envelope = min(1.0, t / 0.05, max(0.0, (duration_s - t) / 0.18))
            if "bandit" in normalized_id:
                value = 0.0
                for pulse_index, pulse_start in enumerate((0.0, 0.18)):
                    age = t - pulse_start
                    if 0.0 <= age <= 0.16:
                        pitch = 780 - pulse_index * 90
                        whistle = math.sin(2.0 * math.pi * pitch * age) * 0.16
                        rasp = (rng.random() * 2.0 - 1.0) * 0.03
                        value += (whistle + rasp) * math.exp(-age * 12.0)
                value *= envelope
            else:
                growl = math.sin(2.0 * math.pi * 72 * t) * 0.20
                rasp = (rng.random() * 2.0 - 1.0) * 0.06
                value = (growl + rasp) * envelope
        elif cue_family == "profession":
            if "vanguard" in normalized_id:
                segment = duration_s / 3.0
                note_index = min(2, int(t / segment))
                frequency = 176 + note_index * 58
                local_t = t - segment * note_index
                envelope = min(1.0, local_t / 0.04, max(0.0, (segment - local_t) / 0.12))
                overtone = math.sin(2.0 * math.pi * (frequency * 2.0) * local_t) * 0.06
                value = (math.sin(2.0 * math.pi * frequency * local_t) * 0.22 + overtone) * envelope
            elif "arcanist" in normalized_id:
                segment = duration_s / 3.0
                note_index = min(2, int(t / segment))
                frequency = 262 + note_index * 86
                local_t = t - segment * note_index
                shimmer = math.sin(2.0 * math.pi * (frequency * 1.5) * local_t + math.sin(2.0 * math.pi * 5.0 * local_t)) * 0.08
                envelope = min(1.0, local_t / 0.06, max(0.0, (segment - local_t) / 0.18))
                value = (math.sin(2.0 * math.pi * frequency * local_t) * 0.18 + shimmer) * envelope
            else:
                segment = duration_s / 3.0
                note_index = min(2, int(t / segment))
                frequency = base_frequency + note_index * 110
                local_t = t - segment * note_index
                envelope = min(1.0, local_t / 0.05, max(0.0, (segment - local_t) / 0.15))
                value = math.sin(2.0 * math.pi * frequency * local_t) * 0.24 * envelope
        elif cue_family == "silence":
            value = 0.0
        else:
            envelope = min(1.0, t / 0.04, max(0.0, (duration_s - t) / 0.12))
            value = math.sin(2.0 * math.pi * base_frequency * t) * 0.18 * envelope

        clamped = max(-0.95, min(0.95, value))
        samples.extend(struct.pack("<h", int(clamped * 32767)))

    with wave.open(str(output_path), "wb") as handle:
        handle.setnchannels(TARGET_CHANNELS)
        handle.setsampwidth(2)
        handle.setframerate(TARGET_SAMPLE_RATE)
        handle.writeframes(bytes(samples))


def process_audio(
    ffmpeg_path: str,
    input_path: pathlib.Path,
    output_path: pathlib.Path,
) -> None:
    output_path.parent.mkdir(parents=True, exist_ok=True)
    command = [
        ffmpeg_path,
        "-y",
        "-hide_banner",
        "-loglevel",
        "error",
        "-i",
        str(input_path),
        "-vn",
        "-ac",
        str(TARGET_CHANNELS),
        "-ar",
        str(TARGET_SAMPLE_RATE),
        "-af",
        "highpass=f=40,lowpass=f=14000,alimiter=limit=0.92",
        "-c:a",
        "libvorbis",
        "-q:a",
        "4",
        str(output_path),
    ]
    subprocess.run(command, check=True)


def probe_audio(ffprobe_path: str, path: pathlib.Path) -> dict[str, object]:
    command = [
        ffprobe_path,
        "-v",
        "error",
        "-show_entries",
        "stream=codec_name,sample_rate,channels",
        "-show_entries",
        "format=duration,size",
        "-of",
        "json",
        str(path),
    ]
    completed = subprocess.run(command, check=True, capture_output=True, text=True)
    payload = json.loads(completed.stdout)
    stream = (payload.get("streams") or [{}])[0]
    format_payload = payload.get("format") or {}
    return {
        "codec": stream.get("codec_name"),
        "sampleRate": int(stream.get("sample_rate", 0) or 0),
        "channels": int(stream.get("channels", 0) or 0),
        "durationSeconds": float(format_payload.get("duration", 0.0) or 0.0),
        "sizeBytes": int(format_payload.get("size", 0) or 0),
    }


def write_report(report_path: pathlib.Path, records: list[dict[str, object]]) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    with report_path.open("w", encoding="utf-8") as handle:
        for record in records:
            handle.write(json.dumps(record, ensure_ascii=False) + "\n")


def main() -> int:
    args = parse_args()
    ffmpeg_path = require_tool("ffmpeg")
    ffprobe_path = require_tool("ffprobe")

    manifest_path = pathlib.Path(args.runtime_manifest).resolve()
    filter_plan_path = pathlib.Path(args.filter_plan).resolve() if args.filter_plan else None
    raw_dir = pathlib.Path(args.raw_dir).resolve()
    cleaned_dir = pathlib.Path(args.cleaned_dir).resolve()
    runtime_root = pathlib.Path(args.runtime_root).resolve()
    report_path = pathlib.Path(args.report).resolve()

    raw_dir.mkdir(parents=True, exist_ok=True)
    cleaned_dir.mkdir(parents=True, exist_ok=True)
    records: list[dict[str, object]] = []

    assets = parse_manifest(manifest_path)
    if filter_plan_path is not None:
        assets = filter_assets_by_plan(assets, filter_plan_path)
        print(f"[filter] {filter_plan_path} -> {len(assets)} manifest entries")
    for asset in assets:
        runtime_path = runtime_root / asset.source_path
        cleaned_path = cleaned_dir / asset.source_path
        relative = pathlib.Path(asset.source_path)
        raw_default_path = raw_dir / relative.with_suffix(".wav")
        raw_path = resolve_input_path(raw_dir, runtime_root, asset.source_path)

        if raw_path is None and args.bootstrap_missing:
            generate_placeholder_wav(raw_default_path, asset.cue_family, asset.source_path)
            raw_path = raw_default_path

        if raw_path is None:
            print(f"ERROR: missing raw source for {asset.source_path}")
            return 1

        if args.skip_existing and cleaned_path.is_file():
            runtime_path.parent.mkdir(parents=True, exist_ok=True)
            if not runtime_path.is_file():
                shutil.copy2(cleaned_path, runtime_path)
            probe = probe_audio(ffprobe_path, cleaned_path)
            records.append(
                {
                    "processedAt": datetime.now(timezone.utc).isoformat(),
                    "status": "skipped",
                    "sourcePath": asset.source_path,
                    "cueFamily": asset.cue_family,
                    "eventIds": list(asset.event_ids),
                    "keys": list(asset.keys),
                    "rawPath": display_path(raw_path) if raw_path is not None else None,
                    "cleanedPath": display_path(cleaned_path),
                    "runtimePath": display_path(runtime_path),
                    "rawBytes": raw_path.stat().st_size if raw_path is not None and raw_path.is_file() else None,
                    "processedBytes": probe["sizeBytes"],
                    "codec": probe["codec"],
                    "sampleRate": probe["sampleRate"],
                    "channels": probe["channels"],
                    "durationSeconds": probe["durationSeconds"],
                },
            )
            print(f"[skip] {asset.source_path} -> {cleaned_path}")
            continue

        source_bytes = raw_path.stat().st_size
        process_audio(ffmpeg_path, raw_path, cleaned_path)
        runtime_path.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(cleaned_path, runtime_path)
        probe = probe_audio(ffprobe_path, cleaned_path)

        records.append(
            {
                "processedAt": datetime.now(timezone.utc).isoformat(),
                "status": "processed",
                "sourcePath": asset.source_path,
                "cueFamily": asset.cue_family,
                "eventIds": list(asset.event_ids),
                "keys": list(asset.keys),
                "rawPath": display_path(raw_path),
                "cleanedPath": display_path(cleaned_path),
                "runtimePath": display_path(runtime_path),
                "rawBytes": source_bytes,
                "processedBytes": probe["sizeBytes"],
                "codec": probe["codec"],
                "sampleRate": probe["sampleRate"],
                "channels": probe["channels"],
                "durationSeconds": probe["durationSeconds"],
            },
        )
        print(
            f"[processed] {asset.source_path} "
            f"({source_bytes}B -> {probe['sizeBytes']}B, {probe['durationSeconds']:.2f}s, "
            f"{probe['sampleRate']}Hz/{probe['channels']}ch)"
        )

    write_report(report_path, records)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
