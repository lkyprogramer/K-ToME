#!/usr/bin/env python3
"""Generate formal OPT PR-03 equipment raw audio assets."""

from __future__ import annotations

import argparse
import pathlib
import random
from datetime import datetime, timezone

from asset_pipeline_common import load_yaml
from generate_opt_pr02_audio import (
    DEFAULT_SAMPLE_RATE,
    AudioTarget,
    mix_chime,
    mix_click,
    mix_impact,
    mix_noise,
    mix_tone,
    mix_whoosh,
    normalize,
    note,
    stable_seed,
    write_report,
    write_wav,
)


DEFAULT_PACK_ID = "phase4-opt-pr03-audio-generation-v1"
PROFILE_DEFAULTS_BY_KEY: dict[str, tuple[str, float]] = {
    "audio.item.unique.thornpath_crook": ("unique_thornpath_crook", 0.86),
    "audio.item.unique.heartwood_talisman": ("unique_heartwood_talisman", 0.90),
    "audio.item.unique.quenchbreaker_maul": ("unique_quenchbreaker_maul", 0.98),
    "audio.item.unique.cinderveil_plate": ("unique_cinderveil_plate", 0.94),
    "audio.item.unique.floodglass_rapier": ("unique_floodglass_rapier", 0.82),
    "audio.item.unique.deepcurrent_lens": ("unique_deepcurrent_lens", 0.88),
    "audio.item.unique.vesper_chainmail": ("unique_vesper_chainmail", 0.93),
    "audio.item.unique.nullwake_blade": ("unique_nullwake_blade", 0.84),
    "audio.item.artifact.heartroot_gambit": ("artifact_heartroot_gambit", 1.04),
    "audio.item.artifact.slag_tyrant_seal": ("artifact_slag_tyrant_seal", 1.06),
    "audio.item.artifact.deepcurrent_crown": ("artifact_deepcurrent_crown", 1.02),
    "audio.item.artifact.vesper_prism": ("artifact_vesper_prism", 1.00),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate OPT PR-03 equipment raw audio assets")
    parser.add_argument(
        "--plan",
        default="assets-src/audio/specs/phase4-opt-pr03-audio-plan.yaml",
        help="YAML plan describing OPT PR-03 audio assets to generate. Supports the formal audio plan and a generation plan.",
    )
    parser.add_argument(
        "--raw-dir",
        default="assets-src/audio/raw",
        help="Root directory for raw audio assets.",
    )
    parser.add_argument(
        "--report",
        default="assets-src/audio/manifests/phase4-opt-pr03-generation-report.jsonl",
        help="JSONL report path for generated raw audio assets.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="Overwrite existing raw assets.",
    )
    return parser.parse_args()


def parse_targets(plan: dict) -> tuple[int, str, list[AudioTarget]]:
    if "targets" in plan:
        return parse_generation_targets(plan)
    if "entries" in plan:
        return parse_manifest_targets(plan)
    raise ValueError("OPT PR-03 audio plan must declare either 'targets' or 'entries'.")


def parse_generation_targets(plan: dict) -> tuple[int, str, list[AudioTarget]]:
    pack_id = str(plan.get("packId", "")).strip()
    if not pack_id:
        raise ValueError("packId is required in the OPT PR-03 audio generation plan.")
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


def parse_manifest_targets(plan: dict) -> tuple[int, str, list[AudioTarget]]:
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
            raise ValueError(f"eventId must match key for OPT PR-03 audio generation: {raw_entry}")
        if key not in PROFILE_DEFAULTS_BY_KEY:
            raise ValueError(f"unsupported OPT PR-03 audio key: {key}")
        profile, duration_seconds = PROFILE_DEFAULTS_BY_KEY[key]
        targets.append(
            AudioTarget(
                asset_id=f"phase4_opt_pr03_{profile}",
                key=key,
                source_path=source_path,
                profile=profile,
                duration_seconds=duration_seconds,
            ),
        )
    return sample_rate, DEFAULT_PACK_ID, targets


def add_profile(samples: list[float], sample_rate: int, profile: str, rng: random.Random) -> None:
    if profile == "unique_thornpath_crook":
        mix_click(samples, sample_rate, 0.0, 0.06)
        mix_tone(samples, sample_rate, 0.02, 0.40, 0.09, note(67.0), note(61.0), waveform="triangle", attack=0.0, release=0.20)
        mix_noise(samples, sample_rate, 0.0, 0.26, 0.03, rng, attack=0.0, release=0.08, smooth=0.42)
        return
    if profile == "unique_heartwood_talisman":
        mix_chime(samples, sample_rate, 0.00, 0.46, note(64.0), 0.08)
        mix_tone(samples, sample_rate, 0.10, 0.50, 0.05, note(48.0), note(43.0), waveform="sine", attack=0.02, release=0.22)
        return
    if profile == "unique_quenchbreaker_maul":
        mix_impact(samples, sample_rate, 0.00, 0.24, rng, heavy=True)
        mix_noise(samples, sample_rate, 0.10, 0.32, 0.05, rng, attack=0.01, release=0.10, smooth=0.20)
        mix_tone(samples, sample_rate, 0.08, 0.56, 0.08, note(38.0), note(31.0), waveform="triangle", attack=0.01, release=0.26)
        return
    if profile == "unique_cinderveil_plate":
        mix_noise(samples, sample_rate, 0.00, 0.56, 0.05, rng, attack=0.04, release=0.18, smooth=0.12)
        mix_chime(samples, sample_rate, 0.14, 0.34, note(70.0), 0.05)
        mix_tone(samples, sample_rate, 0.06, 0.48, 0.05, note(45.0), note(40.0), waveform="saw", attack=0.02, release=0.18)
        return
    if profile == "unique_floodglass_rapier":
        mix_click(samples, sample_rate, 0.00, 0.05)
        mix_whoosh(samples, sample_rate, 0.04, 0.26, 0.12, rng, bright=True)
        mix_chime(samples, sample_rate, 0.14, 0.30, note(79.0), 0.05)
        return
    if profile == "unique_deepcurrent_lens":
        mix_chime(samples, sample_rate, 0.00, 0.40, note(72.0), 0.07)
        mix_tone(samples, sample_rate, 0.10, 0.48, 0.05, note(55.0), note(50.0), waveform="triangle", attack=0.02, release=0.20)
        mix_noise(samples, sample_rate, 0.08, 0.26, 0.02, rng, attack=0.01, release=0.08, smooth=0.55)
        return
    if profile == "unique_vesper_chainmail":
        mix_impact(samples, sample_rate, 0.00, 0.16, rng, heavy=False)
        mix_chime(samples, sample_rate, 0.08, 0.44, note(62.0), 0.06)
        mix_tone(samples, sample_rate, 0.12, 0.54, 0.06, note(43.0), note(38.0), waveform="triangle", attack=0.02, release=0.24)
        return
    if profile == "unique_nullwake_blade":
        mix_click(samples, sample_rate, 0.00, 0.07)
        mix_whoosh(samples, sample_rate, 0.02, 0.24, 0.10, rng, bright=False)
        mix_tone(samples, sample_rate, 0.10, 0.42, 0.06, note(58.0), note(50.0), waveform="saw", attack=0.01, release=0.18)
        return
    if profile == "artifact_heartroot_gambit":
        mix_click(samples, sample_rate, 0.00, 0.08)
        mix_chime(samples, sample_rate, 0.06, 0.54, note(68.0), 0.09)
        mix_tone(samples, sample_rate, 0.12, 0.60, 0.07, note(45.0), note(52.0), waveform="triangle", attack=0.02, release=0.28)
        mix_noise(samples, sample_rate, 0.00, 0.18, 0.02, rng, attack=0.0, release=0.08, smooth=0.48)
        return
    if profile == "artifact_slag_tyrant_seal":
        mix_impact(samples, sample_rate, 0.00, 0.24, rng, heavy=True)
        mix_impact(samples, sample_rate, 0.20, 0.12, rng, heavy=False)
        mix_tone(samples, sample_rate, 0.08, 0.62, 0.09, note(34.0), note(28.0), waveform="triangle", attack=0.0, release=0.30)
        return
    if profile == "artifact_deepcurrent_crown":
        mix_chime(samples, sample_rate, 0.00, 0.58, note(74.0), 0.08)
        mix_chime(samples, sample_rate, 0.16, 0.36, note(81.0), 0.05)
        mix_whoosh(samples, sample_rate, 0.04, 0.34, 0.08, rng, bright=True)
        mix_tone(samples, sample_rate, 0.08, 0.54, 0.05, note(50.0), note(45.0), waveform="triangle", attack=0.02, release=0.22)
        return
    if profile == "artifact_vesper_prism":
        mix_click(samples, sample_rate, 0.00, 0.08)
        mix_chime(samples, sample_rate, 0.06, 0.52, note(65.0), 0.08)
        mix_tone(samples, sample_rate, 0.12, 0.54, 0.06, note(53.0), note(46.0), waveform="saw", attack=0.02, release=0.24)
        mix_whoosh(samples, sample_rate, 0.20, 0.24, 0.06, rng, bright=False)
        return
    raise ValueError(f"Unknown OPT PR-03 audio profile: {profile}")


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

    print(f"Generated OPT PR-03 audio raw assets: {len(targets)} targets, report={report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
