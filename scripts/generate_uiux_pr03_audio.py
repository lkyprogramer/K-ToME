#!/usr/bin/env python3
"""Generate formal Phase 4 UI/UX PR-03 companion raw audio assets."""

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


DEFAULT_PACK_ID = "phase4-uiux-pr03-audio-generation-v1"
PROFILE_DEFAULTS_BY_KEY: dict[str, tuple[str, float]] = {
    "audio.ui.card_open": ("card_open", 0.42),
    "audio.ui.critical_error": ("critical_error", 0.82),
    "audio.shop.purchase_success": ("shop_purchase_success", 0.58),
    "audio.shop.purchase_failed": ("shop_purchase_failed", 0.46),
    "audio.item.high_value_pickup": ("high_value_pickup", 0.96),
    "audio.item.pickup.high_value": ("item_pickup_high_value", 0.86),
    "audio.item.pickup.unique": ("item_pickup_unique", 0.94),
    "audio.item.pickup.artifact": ("item_pickup_artifact", 1.06),
    "audio.item.equip.changed": ("item_equip_changed", 0.52),
    "audio.item.equip.rejected": ("item_equip_rejected", 0.42),
    "audio.item.compare.upgrade": ("item_compare_upgrade", 0.46),
    "audio.item.compare.sidegrade": ("item_compare_sidegrade", 0.40),
    "audio.item.compare.downgrade": ("item_compare_downgrade", 0.48),
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate Phase 4 UI/UX PR-03 raw audio assets")
    parser.add_argument(
        "--plan",
        default="assets-src/audio/specs/phase4-uiux-pr03-audio-plan.yaml",
        help="YAML plan describing PR-03 companion audio assets.",
    )
    parser.add_argument(
        "--raw-dir",
        default="assets-src/audio/raw",
        help="Root directory for raw audio assets.",
    )
    parser.add_argument(
        "--report",
        default="assets-src/audio/manifests/phase4-uiux-pr03-audio-generation-report.jsonl",
        help="JSONL report path for generated raw audio assets.",
    )
    parser.add_argument("--force", action="store_true", help="Overwrite existing raw assets.")
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
            raise ValueError(f"eventId must match key for PR-03 audio generation: {raw_entry}")
        if key not in PROFILE_DEFAULTS_BY_KEY:
            raise ValueError(f"unsupported PR-03 audio key: {key}")
        profile, duration_seconds = PROFILE_DEFAULTS_BY_KEY[key]
        targets.append(
            AudioTarget(
                asset_id=f"phase4_uiux_pr03_{profile}",
                key=key,
                source_path=source_path,
                profile=profile,
                duration_seconds=duration_seconds,
            ),
        )
    return sample_rate, DEFAULT_PACK_ID, targets


def add_profile(samples: list[float], sample_rate: int, profile: str, rng: random.Random) -> None:
    if profile == "card_open":
        mix_click(samples, sample_rate, 0.02, 0.035)
        mix_chime(samples, sample_rate, 0.06, 0.28, note(72.0), 0.055)
        return
    if profile == "critical_error":
        mix_noise(samples, sample_rate, 0.0, 0.70, 0.035, rng, attack=0.0, release=0.34, smooth=0.38)
        mix_tone(samples, sample_rate, 0.04, 0.58, 0.10, note(43.0), note(34.0), waveform="triangle", attack=0.01, release=0.34)
        mix_impact(samples, sample_rate, 0.12, 0.12, rng, heavy=True)
        return
    if profile == "shop_purchase_success":
        mix_click(samples, sample_rate, 0.01, 0.03)
        mix_chime(samples, sample_rate, 0.06, 0.38, note(76.0), 0.06)
        mix_chime(samples, sample_rate, 0.16, 0.32, note(81.0), 0.045)
        return
    if profile == "shop_purchase_failed":
        mix_click(samples, sample_rate, 0.01, 0.04)
        mix_tone(samples, sample_rate, 0.05, 0.24, 0.075, note(51.0), note(46.0), waveform="triangle", attack=0.0, release=0.18)
        return
    if profile == "high_value_pickup":
        mix_click(samples, sample_rate, 0.02, 0.04)
        mix_chime(samples, sample_rate, 0.08, 0.56, note(74.0), 0.06)
        mix_chime(samples, sample_rate, 0.24, 0.52, note(86.0), 0.035)
        mix_noise(samples, sample_rate, 0.0, 0.92, 0.012, rng, attack=0.12, release=0.46, smooth=0.18)
        return
    if profile == "item_pickup_high_value":
        mix_click(samples, sample_rate, 0.02, 0.035)
        mix_chime(samples, sample_rate, 0.08, 0.42, note(72.0), 0.052)
        mix_chime(samples, sample_rate, 0.22, 0.38, note(79.0), 0.034)
        return
    if profile == "item_pickup_unique":
        mix_click(samples, sample_rate, 0.02, 0.04)
        mix_chime(samples, sample_rate, 0.07, 0.48, note(76.0), 0.058)
        mix_chime(samples, sample_rate, 0.23, 0.44, note(83.0), 0.04)
        mix_noise(samples, sample_rate, 0.0, 0.78, 0.009, rng, attack=0.14, release=0.42, smooth=0.2)
        return
    if profile == "item_pickup_artifact":
        mix_click(samples, sample_rate, 0.02, 0.045)
        mix_impact(samples, sample_rate, 0.05, 0.06, rng, heavy=False)
        mix_chime(samples, sample_rate, 0.11, 0.56, note(79.0), 0.06)
        mix_chime(samples, sample_rate, 0.31, 0.58, note(91.0), 0.035)
        mix_noise(samples, sample_rate, 0.0, 0.96, 0.012, rng, attack=0.18, release=0.5, smooth=0.22)
        return
    if profile == "item_equip_changed":
        mix_click(samples, sample_rate, 0.02, 0.035)
        mix_chime(samples, sample_rate, 0.08, 0.30, note(69.0), 0.052)
        return
    if profile == "item_equip_rejected":
        mix_click(samples, sample_rate, 0.01, 0.035)
        mix_tone(samples, sample_rate, 0.05, 0.22, 0.06, note(49.0), note(45.0), waveform="triangle", attack=0.0, release=0.16)
        return
    if profile == "item_compare_upgrade":
        mix_chime(samples, sample_rate, 0.04, 0.28, note(74.0), 0.045)
        mix_chime(samples, sample_rate, 0.15, 0.24, note(81.0), 0.032)
        return
    if profile == "item_compare_sidegrade":
        mix_click(samples, sample_rate, 0.02, 0.03)
        mix_chime(samples, sample_rate, 0.10, 0.22, note(67.0), 0.028)
        return
    if profile == "item_compare_downgrade":
        mix_click(samples, sample_rate, 0.01, 0.03)
        mix_tone(samples, sample_rate, 0.07, 0.24, 0.045, note(57.0), note(53.0), waveform="triangle", attack=0.0, release=0.18)
        return
    raise ValueError(f"Unknown PR-03 audio profile: {profile}")


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

    print(f"Generated PR-03 companion audio raw assets: {len(targets)} targets, report={report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
