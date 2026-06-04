# Plan: Dark UI/UX PR-08 Pre-rendered Room Art Plate

**Generated**: 2026-05-31
**Regenerated**: 2026-06-03 as the lean Direction A execution plan
**Estimated Complexity**: High
**Status**: `hybrid-first-convergence`; current non-ruins packaged parity branch
is `rejected-backed-out`

## 0. Precheck Summary

This file is the current execution plan for Direction A. It is deliberately
short. Historical candidate batches, changelogs and packet-by-packet decisions
live in:

1. `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
2. `UI/review/dark-uiux-pr08-evidence-and-direction.md`
3. `UI/review/dark-uiux-pr08-exploration/*`

Do not append historical packet summaries here. Update this file only when the
current execution route, active blocker, gates or next slice changes.

Current route:

1. Plate-safe rooms: authored full-room plate remains valid.
2. Topology-risk rooms: hybrid-first presentation, not full-room stretch.
3. Non-ruins packaged parity: current topology-source-only / family-ghost route
   is rejected and backed out.
4. Next slice: tactical-grid acceptance / deferral A/B decision for
   topology-risk non-ruins before any new authored presentation implementation.

## 1. Visual Thesis

The target is not a prettier screenshot. The target is:

```text
The map reads as an authored dark-fantasy room before it reads as a tactical grid,
while gameplay markers and runtime text stay readable and runtime-owned.
```

Direction A uses authored room art where the procedural topology can safely
support it. D9 proved that this cannot be a pure full-room rollout. The current
plan is therefore hybrid-first:

1. Use full-room authored plates for `FULL_PLATE_SAFE`.
2. Use topology-aware hybrid composition for `TOPOLOGY_RISK_*`.
3. Keep tactical grid as restrained interaction feedback, not base material.
4. Keep visibility, targeting, telegraph, cursor, actors and loot above the
   authored presentation layer.

## 2. Current State

| Area | Current verdict | Meaning |
| --- | --- | --- |
| `tileset.ruins` proof slice | D3 accepted only for fixed proof slice | Valid resource/presentation route; not all-map closure |
| D9 ROI | `hybrid-first-convergence` | 14 plate-safe, 12 topology-risk rows from 26 playable samples |
| Ruins local polish | frozen | No more same-family alpha/seam/fog/source tuning without new blocker |
| Non-ruins topology sources | accepted-forward | Dedicated source keys exist; not packaged parity |
| Non-ruins wall-family variation | accepted-forward | Distinct wall role resources reduce D9 crop wall rhythm |
| Non-ruins packaged parity | rejected/backed-out | Topology-source-only and family-ghost routes fail first-read quality |
| Current next slice | A/B decision required | Clean tactical-grid acceptance / deferral versus another authored presentation-structure route |

The latest active blocker is not resource registration. It is first-read quality
for topology-risk non-ruins rooms in packaged / runtime crop evidence.

## 3. Architecture Target

Layer order target:

```text
base stage art
-> room authored presentation
-> semantic tile/wall anchors where needed
-> deterministic fog / visibility
-> actors / loot / markers / telegraph
-> cursor / focus / selection
-> UI chrome and runtime text
```

Ownership rules:

1. `core` and `game` remain untouched for PR-08 visual presentation work.
2. `client` owns presentation, rendering, input surfaces and visual overlay
   grammar.
3. `assets-src/image/specs/*`, canonical visual manifest and runtime visual
   manifest own formal image resources.
4. `UI/review/*` owns evidence and rejected/prototype artifacts, not runtime
   resource authority.
5. `UI/goal/*` owns current loop routing and handoff, not historical packet
   storage.

## 4. Active Keys And Resource Families

Current formal room plate / topology source keys:

1. `ui.map_stage.ruins.room_plate.pr08_demo`
2. `ui.map_stage.ruins.room_topology_source.pr08_demo`
3. `ui.map_stage.forest_edge.room_topology_source.pr08_demo`
4. `ui.map_stage.mine.room_topology_source.pr08_demo`
5. `ui.map_stage.shadow_depths.room_topology_source.pr08_demo`

Current non-ruins wall-family route:

1. `tileset.forest_edge.wall_01`
2. `tileset.mine.wall_01`
3. `tileset.shadow_depths.wall_01`

Each non-ruins wall family may resolve role pieces:

1. `base`
2. `crown`
3. `side`
4. `corner`
5. `door_contact`

Do not add parallel renderer-side lookup tables for these roles. Use the
existing manifest resolver / wall-family contract.

## 5. Accepted Direction Gates

| Gate | Current result | Constraint |
| --- | --- | --- |
| D0 candidate batch | historical | Do not reopen as routine image churn |
| D1 runtime prototype | route proved | Client-only presentation path is valid |
| D2 production resource route | valid | Formal keys/manifests are required |
| D3 map closure | ruins proof slice only | Does not generalize to non-ruins |
| D7 multi-map rollout | not closed | Needs per-family evidence |
| D8 procedural seed board | topology contract required | Full-room plates only for plate-safe |
| D9 real-procgen ROI | hybrid-first | Pure full-room rollout rejected |
| Packaged non-ruins parity | rejected/backed-out | Needs tactical-grid acceptance / deferral A/B decision before more authored presentation |

## 6. Next Slice

Goal:

```text
Decide whether topology-risk non-ruins rooms should use clean tactical-grid
acceptance / explicit deferral or one more authored presentation-structure route.
```

Recommended order:

1. Re-read the current baseline in the iteration log.
2. Pick one target evidence set for A/B judgement:
   - packaged forest-edge topology-risk crop
   - packaged shadow-depths topology-risk crop
   - D9 forest/shadow runtime-risk golden crops
3. State the active blocker in first-read language.
4. Compare clean tactical-grid acceptance / explicit deferral against at most
   one authored presentation-structure candidate.
5. Record `blocker-id=topology-risk-non-ruins-first-read`,
   `technique-family=layout-ab` or `presentation-structure`, and
   `failure-counter-impact`.
6. Implement only if the A/B decision keeps authored presentation alive and the
   chosen route has a clear rejection condition and rollback invariant.
7. Update the iteration log with actual commands and evidence.

The next slice is not allowed to be source-only texture iteration or another
same-family wall/alpha/ghost rescue.

## 7. Rejected Routes For The Next Slice

Do not continue:

1. topology-source-only packaged parity;
2. family-ghost recovery;
3. alpha `0.38` or other alpha rescue;
4. source-then-ghost layer-order rescue;
5. same-key source brightness / texture tuning;
6. extra seam, fog, line-weight or rectangle compositor micro-pass;
7. governance-only accepted-forward updates.

If a new probe looks like debug floor, chopped source strips, auxiliary rails or
placeholder topology bands, reject it immediately and restore the safer state.

## 8. Decision Standard

Every new packet must answer:

1. What is the active blocker?
2. What technique family is being used?
3. Why is this not micro-tuning?
4. What evidence would reject this route?
5. What is the rollback invariant?
6. If accepted-forward, is it:
   - `counts-as-progress`
   - `counts-as-failure`
   - `freeze-no-more-local-polish`

Product ROI and aesthetic stop-line decisions are agent-owned direction records
by default. Stop and ask only when a decision would cross a repository hard red
line such as schema/version/public contract, owner boundary, release gate or
destructive resource replacement.

## 9. Implementation Guardrails

Do not change:

1. `core`
2. `game`
3. save / replay / profile schema
4. manifest schema
5. content-pack boundary
6. gameplay rules

Do not introduce:

1. baked UI text or hotkeys in images
2. second visibility authority
3. second resource mapping table in renderer or tests
4. general-purpose renderer helpers without deleting real complexity
5. golden rebaseline before director evidence closes

Allowed with evidence:

1. client-only presentation model adjustments
2. topology-risk compositor structure changes
3. canonical resource / manifest additions through the existing pipeline
4. focused renderer tests
5. packaged whitebox recapture

## 10. Testing Strategy

Use the narrowest command that proves the changed contract.

Docs-only plan/governance update:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint --no-configuration-cache
git diff --check
```

Renderer / presentation implementation:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --no-configuration-cache
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.<targetedPr08Test>" --no-configuration-cache
./gradlew maintainabilityLint --no-configuration-cache
```

Resource / manifest implementation:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew resourcePipelineLint --no-configuration-cache
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --no-configuration-cache
```

Packaged parity:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=<scenario> --no-configuration-cache
```

Full close remains out of scope until a packet explicitly accepts final PR-08
closure:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew verifyChanged --no-configuration-cache
```

Run Gradle serially. Do not launch parallel Gradle jobs.

## 11. Evidence Outputs

A valid next packet should produce or update:

1. a decision file under `UI/review/dark-uiux-pr08-exploration/<packet>/`;
2. fixed crop or packaged crop evidence;
3. `evidence-index.tsv` when a packet creates multiple artifacts;
4. the iteration log;
5. only current-state fields in this plan if the route changes.

Do not copy full packet contents back into this plan.

## 12. Rollback Plan

If a route is rejected:

1. remove or back out the runtime path that produced the rejected first-read;
2. keep review artifacts under `UI/review/...` as rejected evidence;
3. restore the last safer renderer/resource path;
4. run the focused test that proves the rejected behavior no longer executes;
5. record the rejection and next valid route in the iteration log;
6. update this plan only if the active route or blocker changes.

For docs-only regeneration, rollback is simply restoring the previous document
versions from git. No runtime rollback is involved.
