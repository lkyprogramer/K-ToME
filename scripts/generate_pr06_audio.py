#!/usr/bin/env python3
"""Generate deterministic PR-06 raw audio assets for the official slice."""

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
    parser = argparse.ArgumentParser(description="Generate PR-06 raw audio assets")
    parser.add_argument(
        "--plan",
        default="assets-src/audio/specs/pr06-audio-refresh-plan.yaml",
        help="YAML plan describing PR-06 audio assets to generate.",
    )
    parser.add_argument(
        "--raw-dir",
        default="assets-src/audio/raw",
        help="Root directory for raw audio assets.",
    )
    parser.add_argument(
        "--report",
        default="assets-src/audio/manifests/pr06-audio-refresh-report.jsonl",
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
        raise ValueError("packId is required in the PR-06 audio plan.")

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
        0.03,
        amplitude,
        freq_start=1_800.0,
        freq_end=900.0,
        waveform="triangle",
        attack=0.0,
        release=0.025,
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
    mix_tone(samples, sample_rate, start_s, duration_s * 0.8, amplitude * 0.5, base_frequency * 2.0, waveform="triangle", attack=0.002, release=duration_s * 0.6)
    mix_tone(samples, sample_rate, start_s, duration_s * 0.6, amplitude * 0.25, base_frequency * 3.0, waveform="sine", attack=0.002, release=duration_s * 0.4)


def mix_impact(
    samples: list[float],
    sample_rate: int,
    start_s: float,
    amplitude: float,
    rng: random.Random,
    *,
    heavy: bool,
) -> None:
    duration_s = 0.22 if heavy else 0.14
    mix_tone(
        samples,
        sample_rate,
        start_s,
        duration_s,
        amplitude * (0.65 if heavy else 0.45),
        freq_start=120.0 if heavy else 180.0,
        freq_end=68.0 if heavy else 120.0,
        waveform="triangle",
        attack=0.0,
        release=duration_s * 0.9,
    )
    mix_noise(
        samples,
        sample_rate,
        start_s,
        duration_s * 0.75,
        amplitude * (0.55 if heavy else 0.35),
        rng,
        attack=0.0,
        release=duration_s * 0.5,
        smooth=0.2,
    )
    mix_click(samples, sample_rate, start_s, amplitude * 0.18)


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
    mix_noise(samples, sample_rate, start_s, duration_s, amplitude * 0.4, rng, attack=0.0, release=duration_s * 0.65, smooth=0.45)
    mix_tone(
        samples,
        sample_rate,
        start_s,
        duration_s,
        amplitude * 0.22,
        freq_start=420.0 if bright else 220.0,
        freq_end=980.0 if bright else 520.0,
        waveform="saw",
        attack=0.0,
        release=duration_s * 0.55,
    )


def mix_shimmer(
    samples: list[float],
    sample_rate: int,
    start_s: float,
    duration_s: float,
    base_frequency: float,
    amplitude: float,
) -> None:
    mix_tone(samples, sample_rate, start_s, duration_s, amplitude, base_frequency, waveform="triangle", attack=0.03, release=duration_s * 0.6, vibrato_rate=6.0, vibrato_depth=3.0)
    mix_tone(samples, sample_rate, start_s + 0.02, duration_s * 0.9, amplitude * 0.55, base_frequency * 1.5, waveform="sine", attack=0.02, release=duration_s * 0.55)
    mix_tone(samples, sample_rate, start_s + 0.05, duration_s * 0.7, amplitude * 0.3, base_frequency * 2.0, waveform="sine", attack=0.01, release=duration_s * 0.4)


def add_profile(samples: list[float], sample_rate: int, profile: str, rng: random.Random) -> None:
    if profile == "profession_vanguard":
        for start_s, midi_note in ((0.0, 45), (0.34, 48), (0.68, 52)):
            mix_tone(samples, sample_rate, start_s, 0.34, 0.24, note(midi_note), waveform="triangle", attack=0.02, release=0.18)
            mix_impact(samples, sample_rate, start_s, 0.22, rng, heavy=True)
        mix_tone(samples, sample_rate, 0.14, 0.95, 0.08, note(57), waveform="saw", attack=0.06, release=0.45)
        return
    if profile == "profession_arcanist":
        for start_s, midi_note in ((0.0, 69), (0.18, 73), (0.36, 76), (0.56, 81)):
            mix_chime(samples, sample_rate, start_s, 0.42, note(midi_note), 0.12)
        mix_shimmer(samples, sample_rate, 0.18, 1.05, note(57), 0.1)
        return
    if profile == "tree_vanguard_arms":
        mix_whoosh(samples, sample_rate, 0.0, 0.28, 0.3, rng)
        mix_impact(samples, sample_rate, 0.22, 0.32, rng, heavy=True)
        return
    if profile == "tree_vanguard_shield":
        mix_impact(samples, sample_rate, 0.08, 0.28, rng, heavy=True)
        mix_tone(samples, sample_rate, 0.0, 0.62, 0.08, note(43), waveform="triangle", attack=0.03, release=0.35)
        return
    if profile == "tree_vanguard_warcry":
        mix_tone(samples, sample_rate, 0.0, 0.5, 0.18, note(52), note(59), waveform="saw", attack=0.02, release=0.25)
        mix_noise(samples, sample_rate, 0.08, 0.32, 0.07, rng, attack=0.0, release=0.18, smooth=0.12)
        return
    if profile == "tree_arcanist_flame":
        mix_tone(samples, sample_rate, 0.0, 0.48, 0.16, note(76), note(88), waveform="saw", attack=0.01, release=0.22)
        mix_noise(samples, sample_rate, 0.04, 0.44, 0.06, rng, attack=0.0, release=0.18, smooth=0.35)
        return
    if profile == "tree_arcanist_frost":
        mix_chime(samples, sample_rate, 0.0, 0.5, note(88), 0.12)
        mix_tone(samples, sample_rate, 0.08, 0.54, 0.08, note(76), note(62), waveform="triangle", attack=0.02, release=0.3)
        return
    if profile == "tree_arcanist_arcane":
        mix_shimmer(samples, sample_rate, 0.0, 0.7, note(65), 0.14)
        mix_click(samples, sample_rate, 0.18, 0.08)
        mix_click(samples, sample_rate, 0.34, 0.07)
        return
    if profile == "item_short_sword":
        mix_click(samples, sample_rate, 0.0, 0.2)
        mix_tone(samples, sample_rate, 0.01, 0.18, 0.16, 1_020.0, 560.0, waveform="triangle", attack=0.0, release=0.14)
        mix_noise(samples, sample_rate, 0.0, 0.12, 0.05, rng, attack=0.0, release=0.08, smooth=0.2)
        return
    if profile == "item_basic_shield":
        mix_impact(samples, sample_rate, 0.03, 0.3, rng, heavy=True)
        mix_tone(samples, sample_rate, 0.02, 0.26, 0.08, 420.0, 260.0, waveform="triangle", attack=0.0, release=0.22)
        return
    if profile == "item_chain_mail":
        for start_s, amp in ((0.0, 0.1), (0.08, 0.12), (0.18, 0.09), (0.28, 0.07)):
            mix_click(samples, sample_rate, start_s, amp)
            mix_noise(samples, sample_rate, start_s, 0.08, amp * 0.35, rng, attack=0.0, release=0.05, smooth=0.3)
        return
    if profile == "item_healing_potion":
        mix_click(samples, sample_rate, 0.0, 0.11)
        mix_chime(samples, sample_rate, 0.05, 0.4, note(74), 0.1)
        mix_tone(samples, sample_rate, 0.1, 0.34, 0.08, note(62), note(69), waveform="sine", attack=0.02, release=0.22)
        return
    if profile == "item_mana_potion":
        mix_click(samples, sample_rate, 0.0, 0.09)
        mix_shimmer(samples, sample_rate, 0.04, 0.45, note(69), 0.09)
        mix_tone(samples, sample_rate, 0.08, 0.36, 0.07, note(64), note(76), waveform="triangle", attack=0.03, release=0.2)
        return
    if profile == "item_arcane_staff":
        mix_tone(samples, sample_rate, 0.0, 0.52, 0.08, note(45), waveform="triangle", attack=0.04, release=0.3)
        mix_shimmer(samples, sample_rate, 0.08, 0.4, note(72), 0.11)
        return
    if profile == "item_apprentice_robe":
        mix_noise(samples, sample_rate, 0.0, 0.28, 0.06, rng, attack=0.0, release=0.2, smooth=0.55)
        mix_chime(samples, sample_rate, 0.12, 0.24, note(81), 0.05)
        return
    if profile == "talent_power_strike":
        mix_whoosh(samples, sample_rate, 0.0, 0.22, 0.24, rng)
        mix_impact(samples, sample_rate, 0.2, 0.34, rng, heavy=True)
        return
    if profile == "talent_sweeping_strike":
        mix_whoosh(samples, sample_rate, 0.0, 0.36, 0.28, rng)
        mix_impact(samples, sample_rate, 0.24, 0.18, rng, heavy=False)
        mix_impact(samples, sample_rate, 0.34, 0.18, rng, heavy=False)
        return
    if profile == "talent_shield_bash":
        mix_impact(samples, sample_rate, 0.05, 0.36, rng, heavy=True)
        mix_tone(samples, sample_rate, 0.08, 0.2, 0.09, 360.0, 180.0, waveform="triangle", attack=0.0, release=0.18)
        return
    if profile == "talent_guard_stance":
        mix_tone(samples, sample_rate, 0.0, 0.7, 0.11, note(43), waveform="triangle", attack=0.05, release=0.32)
        mix_shimmer(samples, sample_rate, 0.08, 0.55, note(55), 0.05)
        return
    if profile == "talent_war_cry":
        mix_tone(samples, sample_rate, 0.0, 0.46, 0.18, note(52), note(60), waveform="saw", attack=0.01, release=0.22)
        mix_noise(samples, sample_rate, 0.06, 0.32, 0.08, rng, attack=0.0, release=0.18, smooth=0.08)
        return
    if profile == "talent_intimidation":
        mix_tone(samples, sample_rate, 0.0, 0.52, 0.14, note(60), note(46), waveform="triangle", attack=0.01, release=0.28)
        mix_noise(samples, sample_rate, 0.12, 0.26, 0.07, rng, attack=0.0, release=0.18, smooth=0.18)
        return
    if profile == "talent_sunder_armor":
        mix_whoosh(samples, sample_rate, 0.0, 0.18, 0.16, rng)
        mix_noise(samples, sample_rate, 0.12, 0.2, 0.09, rng, attack=0.0, release=0.14, smooth=0.12)
        mix_click(samples, sample_rate, 0.14, 0.14)
        mix_impact(samples, sample_rate, 0.18, 0.18, rng, heavy=False)
        return
    if profile == "talent_unyielding":
        mix_tone(samples, sample_rate, 0.0, 0.72, 0.1, note(48), note(60), waveform="triangle", attack=0.04, release=0.35)
        mix_shimmer(samples, sample_rate, 0.16, 0.48, note(64), 0.08)
        return
    if profile == "talent_fireball":
        mix_click(samples, sample_rate, 0.0, 0.08)
        mix_tone(samples, sample_rate, 0.02, 0.26, 0.14, 520.0, 900.0, waveform="saw", attack=0.0, release=0.16)
        mix_noise(samples, sample_rate, 0.14, 0.22, 0.08, rng, attack=0.0, release=0.1, smooth=0.22)
        mix_impact(samples, sample_rate, 0.16, 0.18, rng, heavy=False)
        return
    if profile == "talent_flame_wall":
        mix_noise(samples, sample_rate, 0.0, 0.58, 0.09, rng, attack=0.0, release=0.2, smooth=0.42)
        mix_tone(samples, sample_rate, 0.0, 0.5, 0.1, 340.0, 760.0, waveform="saw", attack=0.02, release=0.16)
        return
    if profile == "talent_ice_bolt":
        mix_chime(samples, sample_rate, 0.0, 0.22, note(88), 0.12)
        mix_tone(samples, sample_rate, 0.04, 0.18, 0.1, 1_000.0, 560.0, waveform="triangle", attack=0.0, release=0.12)
        return
    if profile == "talent_frost_nova":
        mix_shimmer(samples, sample_rate, 0.0, 0.62, note(62), 0.09)
        mix_tone(samples, sample_rate, 0.12, 0.44, 0.12, note(84), note(60), waveform="triangle", attack=0.02, release=0.26)
        return
    if profile == "talent_arcane_shield":
        mix_shimmer(samples, sample_rate, 0.0, 0.68, note(67), 0.12)
        mix_tone(samples, sample_rate, 0.08, 0.48, 0.06, note(55), waveform="triangle", attack=0.04, release=0.28)
        return
    if profile == "talent_blink":
        mix_noise(samples, sample_rate, 0.0, 0.12, 0.05, rng, attack=0.0, release=0.05, smooth=0.18)
        mix_tone(samples, sample_rate, 0.0, 0.16, 0.12, 880.0, 1_460.0, waveform="triangle", attack=0.0, release=0.05)
        mix_tone(samples, sample_rate, 0.22, 0.18, 0.1, 1_240.0, 720.0, waveform="triangle", attack=0.0, release=0.08)
        return
    if profile == "talent_mana_surge":
        for start_s, midi_note in ((0.0, 69), (0.09, 73), (0.18, 76), (0.28, 81)):
            mix_chime(samples, sample_rate, start_s, 0.3, note(midi_note), 0.09)
        mix_tone(samples, sample_rate, 0.0, 0.62, 0.08, note(48), note(69), waveform="triangle", attack=0.03, release=0.26)
        return
    if profile == "talent_ice_prison":
        mix_chime(samples, sample_rate, 0.0, 0.18, note(83), 0.09)
        mix_tone(samples, sample_rate, 0.04, 0.42, 0.12, 640.0, 220.0, waveform="triangle", attack=0.0, release=0.2)
        mix_click(samples, sample_rate, 0.18, 0.14)
        return
    if profile == "monster_bandit_captain":
        mix_tone(samples, sample_rate, 0.0, 0.18, 0.16, 980.0, 740.0, waveform="square", attack=0.0, release=0.08)
        mix_noise(samples, sample_rate, 0.16, 0.18, 0.05, rng, attack=0.0, release=0.1, smooth=0.18)
        return
    if profile == "monster_beast_rat":
        for start_s, amplitude in ((0.0, 0.07), (0.07, 0.06), (0.15, 0.05)):
            mix_click(samples, sample_rate, start_s, amplitude)
            mix_tone(
                samples,
                sample_rate,
                start_s,
                0.08,
                amplitude,
                1_520.0,
                1_060.0,
                waveform="square",
                attack=0.0,
                release=0.05,
            )
        mix_noise(samples, sample_rate, 0.0, 0.24, 0.03, rng, attack=0.0, release=0.12, smooth=0.14)
        return
    if profile == "monster_cultist_dungeon_lord":
        mix_tone(samples, sample_rate, 0.0, 0.54, 0.13, note(45), note(38), waveform="triangle", attack=0.03, release=0.28)
        mix_shimmer(samples, sample_rate, 0.1, 0.34, note(62), 0.05)
        mix_noise(samples, sample_rate, 0.06, 0.26, 0.04, rng, attack=0.0, release=0.14, smooth=0.24)
        return
    if profile == "monster_orc_raider":
        mix_whoosh(samples, sample_rate, 0.0, 0.2, 0.18, rng)
        mix_tone(samples, sample_rate, 0.04, 0.26, 0.14, note(50), note(41), waveform="saw", attack=0.0, release=0.16)
        mix_impact(samples, sample_rate, 0.18, 0.22, rng, heavy=False)
        return
    if profile == "monster_undead_bone_archer":
        for start_s, amplitude in ((0.0, 0.06), (0.08, 0.07), (0.18, 0.05)):
            mix_click(samples, sample_rate, start_s, amplitude)
            mix_noise(samples, sample_rate, start_s, 0.05, amplitude * 0.3, rng, attack=0.0, release=0.03, smooth=0.22)
        mix_tone(samples, sample_rate, 0.05, 0.26, 0.09, 920.0, 540.0, waveform="triangle", attack=0.0, release=0.16)
        mix_shimmer(samples, sample_rate, 0.14, 0.18, note(81), 0.04)
        return
    if profile == "boss_bandit_captain":
        mix_impact(samples, sample_rate, 0.0, 0.18, rng, heavy=True)
        mix_tone(samples, sample_rate, 0.08, 0.52, 0.16, note(45), note(52), waveform="saw", attack=0.02, release=0.26)
        mix_tone(samples, sample_rate, 0.2, 0.36, 0.08, note(57), waveform="triangle", attack=0.02, release=0.22)
        return
    if profile == "boss_warning":
        mix_click(samples, sample_rate, 0.0, 0.1)
        mix_tone(samples, sample_rate, 0.0, 0.4, 0.18, note(76), note(64), waveform="triangle", attack=0.0, release=0.18)
        mix_noise(samples, sample_rate, 0.1, 0.12, 0.03, rng, attack=0.0, release=0.05, smooth=0.12)
        return
    if profile == "zone_shattered_outpost":
        mix_noise(samples, sample_rate, 0.0, 5.2, 0.05, rng, attack=0.3, release=0.6, smooth=0.82)
        mix_tone(samples, sample_rate, 0.0, 5.2, 0.03, 46.0, waveform="triangle", attack=0.5, release=0.8)
        for start_s, base_frequency in ((0.74, 420.0), (2.16, 330.0), (3.72, 510.0)):
            mix_click(samples, sample_rate, start_s, 0.06)
            mix_tone(samples, sample_rate, start_s, 0.28, 0.04, base_frequency, base_frequency * 0.75, waveform="triangle", attack=0.0, release=0.2)
        return
    if profile == "interactable_open":
        mix_click(samples, sample_rate, 0.0, 0.12)
        mix_click(samples, sample_rate, 0.08, 0.1)
        mix_tone(samples, sample_rate, 0.12, 0.28, 0.1, 260.0, 170.0, waveform="triangle", attack=0.01, release=0.22)
        mix_noise(samples, sample_rate, 0.12, 0.22, 0.04, rng, attack=0.0, release=0.12, smooth=0.4)
        return
    if profile == "interactable_stairs":
        for start_s, base_frequency in ((0.0, 540.0), (0.16, 420.0), (0.32, 320.0)):
            mix_click(samples, sample_rate, start_s, 0.08)
            mix_tone(samples, sample_rate, start_s, 0.18, 0.06, base_frequency, base_frequency * 0.72, waveform="triangle", attack=0.0, release=0.14)
        return
    raise ValueError(f"Unknown PR-06 audio profile: {profile}")


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

    print(f"Generated PR-06 audio raw assets: {len(targets)} targets, report={report_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
