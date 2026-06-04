# PR08 D9 Real-Procgen ROI Decision

Date: 2026-06-03

## Verdict

`pr08-d9-real-procgen-roi-hybrid-first-freeze-v1-accepted`

D9 is now materialized as an automated real-procgen distribution probe over 26
playable session starts from the foundation route. The distribution is mixed:
14 samples are `FULL_PLATE_SAFE`, while 12 samples are topology-risk rows
(`TOPOLOGY_RISK_LOW_FILL` or `TOPOLOGY_RISK_DISCONNECTED`).

The ROI decision is `hybrid-first-convergence`: keep full-room authored plates
for `FULL_PLATE_SAFE` rooms, but do not pursue broad all-room/all-tileset
Direction A as a pure full-room plate rollout. Topology-risk rows need the
hybrid path and family-specific topology-source coverage before any broader
closure claim.

Ruins same-family local polish is frozen unless a new D9, packaged, or director
artifact proves a fresh ruins-specific blocker. The accepted `tileset.ruins`
proof slice remains valid; this packet rejects further ruins alpha, seam, fog,
line-weight, same-key source, or single-cell rectangle tuning as the next PR08
move.

## Evidence

Canonical review packet:

```text
UI/review/dark-uiux-pr08-exploration/d9-real-procgen-roi-2026-06-03/
```

Automated owner output:

```text
client/build/reports/golden/dark-uiux-pr08-d9-real-procgen/
```

Evidence files:

```text
UI/review/dark-uiux-pr08-exploration/d9-real-procgen-roi-2026-06-03/evidence-index.tsv
UI/review/dark-uiux-pr08-exploration/d9-real-procgen-roi-2026-06-03/summary.md
```

Artifact hashes from the validated build output:

```text
evidence-index.tsv e29f5c9d31998ad13c242a4d7f750a5b0659d8dc94f1ee44bc179123f8329bfc
summary.md d39fc71a2dc604565fd2e454c0676909467fd318dd1e95fab357f49074bae9c4
```

## Distribution

| Metric | Value |
| --- | ---: |
| Samples | 26 |
| `FULL_PLATE_SAFE` | 14 |
| `TOPOLOGY_RISK_LOW_FILL` | 10 |
| `TOPOLOGY_RISK_DISCONNECTED` | 2 |
| `TOPOLOGY_RISK_EXTREME_ASPECT` | 0 |

| Tileset | Samples | Safe | Risk |
| --- | ---: | ---: | ---: |
| `tileset.ruins` | 4 | 2 | 2 |
| `tileset.forest_edge` | 4 | 1 | 3 |
| `tileset.mine` | 4 | 2 | 2 |
| `tileset.shadow_depths` | 14 | 9 | 5 |

## Decision Rationale

The full-room plate direction still has clear value: a majority of the sample
set is plate-safe, and all current room-art families produced at least one
safe sample. Stopping Direction A would throw away the already-proven authored
room read.

Pure full rollout is not justified. Every sampled tileset family also produced
topology-risk rows, and `tileset.forest_edge` is risk-heavy in this sample
set. The next implementation should therefore extend the hybrid-first path,
not widen the full-room plate contract.

No numeric hit-rate threshold is introduced by this packet. The decision is
structural: a mixed population means full plates remain useful for safe rooms,
but the rollout authority must be hybrid-first because real playable procgen
regularly produces risky visible topology.

## Contract

1. D9 reuses `RoomArtPlateTopologyContract`; it does not implement a second
   topology classifier.
2. No `core`, `game`, save, replay, profile, combat, pathing, visibility,
   content-pack, manifest schema, or resource-key contract changed.
3. No new PNG resource, visual key, localization key, or runtime asset path is
   introduced by this packet.
4. The D9 output is evidence for ROI/freeze only. It does not update director
   golden hashes, package whitebox evidence, or all-map closure status.

## Next Contract

The next high-ROI map-stage implementation is non-ruins topology coverage,
starting with `tileset.forest_edge` because D9 shows 3 risky rows out of 4 and
the current production catalog still lacks a formal non-ruins dedicated
topology source route. Do not resume ruins same-key polish before a new
artifact proves a regression.

## Validation

Command run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.golden.Pr08RoomArtPlateD9RealProcgenProbeTest.dark uiux pr08 d9 real procgen roi probe writes distribution artifacts" maintainabilityLint --no-configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 7s
```
