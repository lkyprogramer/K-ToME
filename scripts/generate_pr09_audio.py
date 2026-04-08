#!/usr/bin/env python3
"""Generate formal PR-09 sample content-pack raw audio assets."""

from __future__ import annotations

import argparse
import hashlib
import json
import math
import pathlib
import random
import struct
import wave
from dataclasses import dataclass
from datetime import datetime, timezone

from asset_pipeline_common import load_yaml


DEFAULT_SAMPLE_RATE = 44_100
MAX_PEAK = 0.92


@dataclass(frozen=True)
class AudioTarget:
    asset_id: str
    key: str
    source_path: str
    profile: str
    duration_seconds: float


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate PR-09 sample content-pack raw audio assets")
    parser.add_argument(
        "--plan",
        default="assets-src/audio/specs/phase4-pr09-audio-plan.yaml",
        help="YAML plan describing PR-09 audio assets to generate.",
    )
    parser.add_argument(
        "--raw-dir",
        default="assets-src/audio/raw",
        help="Root directory for raw audio assets.",
    )
    parser.add_argument(
        "--report",
        default="assets-src/audio/manifests/phase4-pr09-generation-report.jsonl",
        help="JSONL report path for generated raw audio assets.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite existing raw assets.",
    )
    return parser.parse_args()


def parse_targets(plan: dict) -> tuple[int, str, list[AudioTarget]]:
    pack_id = str(plan.get("packId", "")).strip()
    if not pack_id:
        raise ValueError("packId is required in the PR-09 audio plan.")

    sample_rate = int(plan.get("sampleRate", DEFAULT_SAMPLE_RATE))
    if sample_rate < 8_000:
        raise ValueError("sampleRate must be >= 8000.")

    raw_targets = plan.get("targets")
    if not isinstance(raw_targets, list) or not raw_targets:
        raise ValueError("targets must be a non-empty list.")

    targets: list[AudioTarget] = []
    for raw_target in raw_targets:
        if not isinstance(raw_target, dict):
            raise ValueError("each target must be a mapping.")
        asset_id = str(raw_target.get("id", "")).strip()
        key = str(raw_target.get("key", "")).strip()
        source_path = str(raw_target.get("sourcePath", "")).strip()
        profile = str(raw_target.get("profile", "")).strip()
        duration_seconds = float(raw_target.get("durationSeconds", 0.0))
        if not asset_id or not key or not source_path or not profile or duration_seconds <= 0.0:
            raise ValueError(f"invalid target definition: {raw_target}")
        targets.append(
            AudioTarget(
                asset_id=asset_id,
                key=key,
                source_path=source_path,
                profile=profile,
                duration_seconds=duration_seconds,
            ),
        )
    return sample_rate, pack_id, targets


def stable_seed(identifier: str) -> int:
    return int(hashlib.sha256(identifier.encode("utf-8")).hexdigest()[:16], 16)


def note(midi: float) -> float:
    return 440.0 * (2.0 ** ((midi - 69.0) / 12.0))


def waveform_value(kind: str, phase: float) -> float:
    if kind == "triangle":
        return (2.0 / math.pi) * math.asin(math.sin(phase))
    if kind == "saw":
        cycle = (phase / (2.0 * math.pi)) % 1.0
        return (2.0 * cycle) - 1.0
    if kind == "square":
        return 1.0 if math.sin(phase) >= 0.0 else -1.0
    return math.sin(phase)


def envelope(progress: float, duration: float, attack: float, release: float) -> float:
    if duration <= 0.0:
        return 0.0
    attack_scale = 1.0 if attack <= 0.0 else min(1.0, progress / attack)
    release_age = duration - progress
    release_scale = 1.0 if release <= 0.0 else min(1.0, max(0.0, release_age) / release)
    return max(0.0, min(attack_scale, release_scale))


def mix_tone(
    samples: list[float],
    sample_rate: int,
    start_s: float,
    duration_s: float,
    amplitude: float,
    freq_start: float,
    freq_end: float | None = None,
    *,
    waveform: str = "sine",
    attack: float = 0.01,
    release: float = 0.08,
    vibrato_rate: float = 0.0,
    vibrato_depth: float = 0.0,
) -> None:
    start_index = max(0, int(start_s * sample_rate))
    frame_count = max(1, int(duration_s * sample_rate))
    phase = 0.0
    freq_end = freq_start if freq_end is None else freq_end
    for local_index in range(frame_count):
        buffer_index = start_index + local_index
        if buffer_index >= len(samples):
            break
        progress = local_index / sample_rate
        ratio = local_index / max(1, frame_count - 1)
        frequency = freq_start + (freq_end - freq_start) * ratio
        if vibrato_rate > 0.0 and vibrato_depth > 0.0:
            frequency += math.sin(2.0 * math.pi * vibrato_rate * progress) * vibrato_depth
        phase += 2.0 * math.pi * frequency / sample_rate
        samples[buffer_index] += waveform_value(waveform, phase) * amplitude * envelope(progress, duration_s, attack, release)


def mix_noise(
    samples: list[float],
    sample_rate: int,
    start_s: float,
    duration_s: float,
    amplitude: float,
    rng: random.Random,
    *,
    attack: float = 0.005,
    release: float = 0.08,
    smooth: float = 0.7,
) -> None:
    start_index = max(0, int(start_s * sample_rate))
    frame_count = max(1, int(duration_s * sample_rate))
    state = 0.0
    for local_index in range(frame_count):
        buffer_index = start_index + local_index
        if buffer_index >= len(samples):
            break
        progress = local_index / sample_rate
        noise = (rng.random() * 2.0) - 1.0
        state = (state * smooth) + (noise * (1.0 - smooth))
        samples[buffer_index] += state * amplitude * envelope(progress, duration_s, attack, release)


def mix_click(samples: list[float], sample_rate: int, start_s: float, amplitude: float) -> None:
    mix_tone(
        samples,
        sample_rate,
        start_s,
        0.028,
        amplitude,
        freq_start=1760.0,
        freq_end=960.0,
        waveform="triangle",
        attack=0.0,
        release=0.022,
    )


def mix_chime(
    samples: list[float],
    sample_rate: int,
    start_s: float,
    duration_s: float,
    base_frequency: float,
    amplitude: float,
) -> None:
    mix_tone(samples, sample_rate, start_s, duration_s, amplitude, base_frequency, waveform="sine", attack=0.005, release=duration_s * 0.8)
    mix_tone(samples, sample_rate, start_s, duration_s * 0.78, amplitude * 0.52, base_frequency * 2.0, waveform="triangle", attack=0.002, release=duration_s * 0.5)
    mix_tone(samples, sample_rate, start_s, duration_s * 0.52, amplitude * 0.22, base_frequency * 3.0, waveform="sine", attack=0.002, release=duration_s * 0.35)


def mix_whoosh(
    samples: list[float],
    sample_rate: int,
    start_s: float,
    duration_s: float,
    amplitude: float,
    rng: random.Random,
    *,
    bright: bool = False,
) -> None:
    mix_noise(samples, sample_rate, start_s, duration_s, amplitude * 0.38, rng, attack=0.0, release=duration_s * 0.6, smooth=0.44)
    mix_tone(
        samples,
        sample_rate,
        start_s,
        duration_s,
        amplitude * 0.20,
        freq_start=420.0 if bright else 220.0,
        freq_end=980.0 if bright else 520.0,
        waveform="saw",
        attack=0.0,
        release=duration_s * 0.55,
    )


def add_profile(samples: list[float], sample_rate: int, profile: str, rng: random.Random) -> None:
    if profile == "zone_flooded_reliquary":
        mix_noise(samples, sample_rate, 0.0, 5.4, 0.045, rng, attack=0.28, release=0.72, smooth=0.74)
        mix_tone(samples, sample_rate, 0.0, 5.4, 0.026, note(40.0), waveform="triangle", attack=0.7, release=1.0)
        mix_tone(samples, sample_rate, 0.34, 4.6, 0.022, note(67.0), note(74.0), waveform="sine", attack=0.3, release=0.6, vibrato_rate=4.2, vibrato_depth=4.0)
        for start_s, midi in ((0.88, 79.0), (2.30, 84.0), (4.08, 76.0)):
            mix_chime(samples, sample_rate, start_s, 0.62, note(midi), 0.07)
        return
    if profile == "interactable_reliquary_node":
        mix_click(samples, sample_rate, 0.0, 0.10)
        mix_whoosh(samples, sample_rate, 0.02, 0.36, 0.16, rng, bright=True)
        mix_chime(samples, sample_rate, 0.08, 0.46, note(74.0), 0.14)
        mix_chime(samples, sample_rate, 0.18, 0.34, note(81.0), 0.08)
        return
    if profile == "interactable_return_bridge":
        for start_s, midi in ((0.0, 69.0), (0.15, 64.0), (0.30, 60.0)):
            mix_click(samples, sample_rate, start_s, 0.07)
            mix_tone(samples, sample_rate, start_s, 0.22, 0.08, note(midi), note(midi - 5.0), waveform="triangle", attack=0.0, release=0.18)
        mix_whoosh(samples, sample_rate, 0.0, 0.58, 0.10, rng)
        return
    if profile == "item_floodtide_lantern":
        mix_click(samples, sample_rate, 0.0, 0.06)
        mix_chime(samples, sample_rate, 0.06, 0.52, note(76.0), 0.09)
        mix_tone(samples, sample_rate, 0.12, 0.78, 0.03, note(52.0), waveform="triangle", attack=0.08, release=0.28)
        return
    if profile == "item_tideglass_echo":
        mix_whoosh(samples, sample_rate, 0.0, 0.42, 0.10, rng, bright=True)
        mix_tone(samples, sample_rate, 0.04, 0.86, 0.05, note(62.0), note(74.0), waveform="saw", attack=0.06, release=0.34)
        mix_chime(samples, sample_rate, 0.22, 0.54, note(86.0), 0.08)
        return
    raise ValueError(f"Unknown PR-09 audio profile: {profile}")


def normalize(samples: list[float]) -> list[float]:
    peak = max((abs(value) for value in samples), default=0.0)
    if peak <= 0.0:
        return samples
    scale = min(1.0, MAX_PEAK / peak)
    return [value * scale for value in samples]


def render_target(sample_rate: int, target: AudioTarget) -> list[float]:
    frame_count = max(1, int(target.duration_seconds * sample_rate))
    samples = [0.0] * frame_count
    rng = random.Random(stable_seed(f"{target.key}:{target.profile}"))
    add_profile(samples, sample_rate, target.profile, rng)
    return normalize(samples)


def write_wav(path: pathlib.Path, sample_rate: int, samples: list[float]) -> int:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as handle:
        handle.setnchannels(1)
        handle.setsampwidth(2)
        handle.setframerate(sample_rate)
        pcm = bytearray()
        for value in samples:
            clamped = max(-1.0, min(1.0, value))
            pcm.extend(struct.pack("<h", int(clamped * 32_767)))
        handle.writeframes(bytes(pcm))
    return path.stat().st_size


def write_report(report_path: pathlib.Path, record: dict[str, object]) -> None:
    report_path.parent.mkdir(parents=True, exist_ok=True)
    with report_path.open("a", encoding="utf-8") as handle:
        handle.write(json.dumps(record, ensure_ascii=True) + "\n")


def main() -> int:
    args = parse_args()
    plan = load_yaml(pathlib.Path(args.plan))
    sample_rate, pack_id, targets = parse_targets(plan)
    raw_dir = pathlib.Path(args.raw_dir).resolve()
    report_path = pathlib.Path(args.report).resolve()

    if report_path.exists():
        report_path.unlink()

    for target in targets:
        raw_path = (raw_dir / pathlib.Path(target.source_path)).with_suffix(".wav")
        if raw_path.exists() and not args.force:
            write_report(
                report_path,
                {
                    "generatedAt": datetime.now(timezone.utc).isoformat(),
                    "status": "skipped",
                    "packId": pack_id,
                    "assetId": target.asset_id,
                    "key": target.key,
                    "profile": target.profile,
                    "rawPath": str(raw_path),
                },
            )
            continue

        samples = render_target(sample_rate, target)
        byte_size = write_wav(raw_path, sample_rate, samples)
        write_report(
            report_path,
            {
                "generatedAt": datetime.now(timezone.utc).isoformat(),
                "status": "generated",
                "packId": pack_id,
                "assetId": target.asset_id,
                "key": target.key,
                "profile": target.profile,
                "sampleRate": sample_rate,
                "durationSeconds": target.duration_seconds,
                "rawPath": str(raw_path),
                "rawBytes": byte_size,
            },
        )

    print(f"Generated PR-09 audio raw assets: {len(targets)} targets, report={report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
