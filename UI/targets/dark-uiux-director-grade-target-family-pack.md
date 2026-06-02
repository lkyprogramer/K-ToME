# Dark UI/UX PR-08 Target Family Pack

> Date: 2026-05-26
> Updated: 2026-05-27
> Status: `accepted-for-direction`
> Scope: follow-up artifact for `UI/manual-records/dark-uiux-director-grade-target-comp.md`

This file defines the next required PR-08 visual target pack after the monolithic target-comp attempts.

It is design evidence only. It is not a manifest, sheet plan, key registry, runtime source or golden baseline.

## Problem Statement

The subtractive spike selected `resource-gap` as the primary root cause, and target comp attempt 3 established the strongest composition direction without obvious baked text. However, the 32px tile-truth sample failed because full-screen crops mix tile material with light, poison, props, wall-edge darkness and scene-specific compositor content.

Therefore PR-08 must split the next target from a full-screen image into resource families before runtime integration.

## Required Visual Files

| File | Purpose | Status |
| --- | --- | --- |
| `dark-uiux-director-grade-target-family-floor-32px.png` | Ruins floor family target: clean 32px tile variants | accepted for direction |
| `dark-uiux-director-grade-target-family-wall-32px.png` | Ruins wall family target: clean 32px wall variants | accepted for direction |
| `dark-uiux-director-grade-target-family-floor-wall-repeat.png` | Contact sheet proving floor/wall repetition and seam behavior | accepted for direction |
| `dark-uiux-director-grade-target-family-map-compositor.png` | Separate darkness/light/telegraph composition reference without becoming resource authority | accepted for direction |
| `dark-uiux-director-grade-target-family-ui-chrome.png` | Panel body, dividers, slot chrome and bottom deck target reference | accepted for direction |
| `dark-uiux-director-grade-target-family-prompt.txt` | Prompt or manual design notes used for the family pack | recorded |

## Artifact Hashes

| File | sha256 |
| --- | --- |
| `dark-uiux-director-grade-target-family-floor-32px.png` | `a9b6a32717bdbfc3fb94ebb64db8bffd2a689e405c7039b2f6ebf7d25dbe8619` |
| `dark-uiux-director-grade-target-family-wall-32px.png` | `fb7688695a7f0f79ccf27db60be3174b3a2ff323844e39372e0c1917ec3c1e26` |
| `dark-uiux-director-grade-target-family-floor-wall-repeat.png` | `80aa56bded38e2358f978bf8f90abe24353629fe6012d72097cd4453617ff73d` |
| `dark-uiux-director-grade-target-family-map-compositor.png` | `d5497eea250474a5a08f4aa07f45e9c6d2d260499abfc110d6505d727de3116e` |
| `dark-uiux-director-grade-target-family-ui-chrome.png` | `f47f8f00fa76bc5b905b5307a81947627fb8ed8c75808a4143ddec01b2494d63` |
| `dark-uiux-director-grade-target-family-prompt.txt` | `92267b29733ee952e1d13718940becac3a87c4849c6a6fd1c4a8e52a7e12d7a9` |

## Direction Verdict

The family pack is accepted as PR-08 direction evidence.

This acceptance means:

1. PR-08 should proceed as resource-family-first reset, not monolithic full-screen paintover.
2. `tileset.ruins.ground_01` and `tileset.ruins.wall_01` may be superseded under `ownerPr=PR-08` after resource readiness and owner coverage are wired.
3. Map darkness, warm light, selection and telegraph should stay in deterministic compositor layers, not baked into floor or wall resources.
4. Right panel, slot chrome and bottom deck should be handled as reusable `ui_frame`-style chrome, with runtime-owned text and icons kept separate.

This acceptance does not mean the generated target PNGs are final runtime resources. They remain design evidence.

## Acceptance Rules

The family pack can be marked `accepted` only when all required rows pass:

| Resource family | Acceptance rule | Rejection trigger |
| --- | --- | --- |
| Ruins floor | At least 8 clean 32px variants; no actor, prop, telegraph, fog, poison or baked lighting; 8x4 repeat does not form obvious checkerboard seams | Needs full-screen crop, oversized paintover, or compositor content to look correct |
| Ruins wall | At least 8 clean 32px variants; wall mass reads thicker than grid edge; no torch, prop, actor, text or fog baked in | Reads as floor border only, includes scene props, or requires region/atlas schema |
| Map compositor | Demonstrates deterministic darkness, warm light and telegraph overlays as separate layers over clean tile resources | Hides player/enemy/loot/telegraph, or requires full-screen map art |
| UI chrome | Shows panel body, divider, slot and bottom deck treatments that can map to existing `ui_frame` keys or explicit PR-08 supersession rows | Requires baked UI labels, numbers, log text, manifest keys or new schema |

## Acceptance Review

| Resource family | Result | Notes |
| --- | --- | --- |
| Ruins floor | accepted for direction | Clean 8x4 32px candidate sheet with no text, props, actors or baked telegraphs. Final runtime assets still need manual/asset-pipeline polish because visible cell border rhythm can become repetitive. |
| Ruins wall | accepted for direction | Clean 8x4 wall sheet with stronger blocking mass than floor and no text/props. Repetition is usable for planning; final assets should reduce over-busy rubble where it hurts readability. |
| Floor/wall repeat | accepted for direction | Built from 32px crops of the family sheets, not from full-screen comp crops. It proves the next PR-08 path can be validated as resource families. |
| Map compositor | accepted for direction | Separates black field, warm light, selection rings, hazard and attack telegraph over visible tile resources; does not make a full-screen map paintover the authority. |
| UI chrome | accepted for direction | Provides reusable panel, divider, slot, deck and blank log surface treatments with no labels, numbers, item icons or character portraits baked in. |

## Runtime Stop Rule

This family pack is accepted for direction, but it is still not implementation approval by itself.

Do not modify these runtime/resource surfaces from PR-08 until PR-08 owner coverage, resource readiness, focused renderer tests and rollback notes are prepared for the affected rows:

1. `TileRenderer.kt`
2. `DemoShellRenderer.kt`
3. `UI/sprite-sheets/sheet-plan.yaml`
4. `UI/sprite-sheets/key-registry.yaml`
5. visual/audio/runtime manifests
6. golden screenshot hashes

## Active Direction From Attempt 3

Use `UI/targets/dark-uiux-director-grade-target-1672x941-attempt3.png` as the mood/layout reference:

1. Orthographic dark dungeon map with separate actor, prop, telegraph and light layers.
2. Forged iron, worn stone, old leather and weathered brass UI chrome.
3. Blank runtime-owned log surface with no baked text.
4. Bottom HUD and right panel as one cohesive character console.

Do not use attempt 3 as a crop source for final resources.
