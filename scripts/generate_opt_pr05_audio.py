#!/usr/bin/env python3
"""Generate formal OPT PR-05 hidden-content raw audio assets."""

from __future__ import annotations

import argparse
import pathlib
import random
from datetime import datetime, timezone

from asset_pipeline_common import load_yaml
from generate_pr07_audio import (
    DEFAULT_SAMPLE_RATE,
    AudioTarget,
    mix_chime,
    mix_click,
    mix_impact,
    mix_noise,
    mix_tone,
    normalize,
    note,
    stable_seed,
    write_report,
    write_wav,
)


DEFAULT_PACK_ID = "phase4-opt-pr05-audio-generation-v1"
PROFILE_DEFAULTS_BY_KEY: dict[str, tuple[str, float]] = {
    "audio.secret_zone.deep_iron_smuggler_stash": ("secret_zone_deep_iron_smuggler_stash", 5.1),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate OPT PR-05 hidden-content raw audio assets")
    parser.add_argument(
        "--plan",
        default="assets-src/audio/specs/phase4-opt-pr05-audio-plan.yaml",
        help="YAML plan describing OPT PR-05 audio assets to generate.",
    )
    parser.add_argument(
        "--raw-dir",
        default="assets-src/audio/raw",
        help="Root directory for raw audio assets.",
    )
    parser.add_argument(
        "--report",
        default="assets-src/audio/manifests/phase4-opt-pr05-generation-report.jsonl",
        help="JSONL report path for generated raw audio assets.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite existing raw assets.",
    )
    return parser.parse_args()


def parse_targets(plan: dict) -> tuple[int, str, list[AudioTarget]]:
    sample_rate = int(plan.get("sampleRate", DEFAULT_SAMPLE_RATE))
    if sample_rate < 8_000:
        raise ValueError("sampleRate must be >= 8000.")
    raw_entries = plan.get("entries")
    if not isinstance(raw_entries, list) or not raw_entries:
        raise ValueError("entries must be a non-empty list.")

    targets: list[AudioTarget] = []
    for raw_entry in raw_entries:
        if not isinstance(raw_entry, dict):
            raise ValueError("each entry must be a mapping.")
        key = str(raw_entry.get("key", "")).strip()
        event_id = str(raw_entry.get("eventId", "")).strip()
        source_path = str(raw_entry.get("sourcePath", "")).strip()
        if not key or not source_path:
            raise ValueError(f"invalid manifest entry definition: {raw_entry}")
        if event_id and event_id != key:
            raise ValueError(f"eventId must match key for OPT PR-05 audio generation: {raw_entry}")
        if key not in PROFILE_DEFAULTS_BY_KEY:
            raise ValueError(f"unsupported OPT PR-05 audio key: {key}")
        profile, duration_seconds = PROFILE_DEFAULTS_BY_KEY[key]
        targets.append(
            AudioTarget(
                asset_id=f"phase4_opt_pr05_{profile}",
                key=key,
                source_path=source_path,
                profile=profile,
                duration_seconds=duration_seconds,
            ),
        )
    return sample_rate, DEFAULT_PACK_ID, targets


def add_profile(samples: list[float], sample_rate: int, profile: str, rng: random.Random) -> None:
    if profile == "secret_zone_deep_iron_smuggler_stash":
        mix_noise(samples, sample_rate, 0.0, 5.1, 0.034, rng, attack=0.18, release=0.48, smooth=0.20)
        mix_tone(samples, sample_rate, 0.0, 5.1, 0.044, note(34.0), waveform="triangle", attack=0.30, release=0.90)
        mix_tone(samples, sample_rate, 0.44, 4.0, 0.018, note(51.0), note(46.0), waveform="saw", attack=0.08, release=0.42)
        for start_s, amplitude in ((0.56, 0.11), (1.74, 0.09), (3.08, 0.10), (4.12, 0.08)):
            mix_impact(samples, sample_rate, start_s, amplitude, rng, heavy=False)
        for start_s, midi in ((1.10, 69.0), (2.46, 72.0), (3.92, 67.0)):
            mix_click(samples, sample_rate, start_s, 0.05)
            mix_chime(samples, sample_rate, start_s + 0.03, 0.34, note(midi), 0.04)
        return
    raise ValueError(f"Unknown OPT PR-05 audio profile: {profile}")


def render_target(sample_rate: int, target: AudioTarget) -> list[float]:
    frame_count = max(1, int(target.duration_seconds * sample_rate))
    samples = [0.0] * frame_count
    rng = random.Random(stable_seed(f"{target.key}:{target.profile}"))
    add_profile(samples, sample_rate, target.profile, rng)
    return normalize(samples)


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

    print(f"Generated OPT PR-05 audio raw assets: {len(targets)} targets, report={report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
