# PR-08 Room Art Plate Combat Surface Integration U Decision

## Decision

Candidate U is accepted forward as the current same-key
`ui.map_stage.ruins.room_plate.pr08_demo` resource foundation.

D3 map-stage closure is still rejected. U improves the right combat field over
S/T without V's over-soft continuous fog plane, but the runtime crop still has
visible tactical read-grid weight and is not yet final `UI/UI-demo-new.png`
quality.

## Root Cause Class

`room-scale combat-field surface integration`.

The blocker is no longer a missing texture route or single marker-card issue.
The remaining first-read problem is that the right combat area still organizes
itself as tactical lattice before authored dungeon material.

## Evidence

1. `runtime-s-t-u-v-map-stage-board.png`
2. `ui-demo-new-map-stage-crop.png`
3. `ui-demo-new-parity-1672x941.png`
4. `source-t-vs-u-combat-field-surface-integration-board.png`
5. `evidence-index.tsv`

Selected hashes:

```text
resource=2d1f7e38054df9a899cbf1ec414f79e3fd39a0bcae1f0e462811790b1e302f01
ui-demo-new-map-stage-crop=c66735a05fbe99b883fc80adfd621215f4e0b73328034ea00c6684f1b174c895
ui-demo-new-parity-1672x941=fbe5d8fb3818663ff865d0104d57048d5a09d3b8a4dead2554dcf1466da13aba
ui-demo-new-parity-1280x800=9f6a3dc2c2d5c9282b59922be59000abf9afa3fb91ed2e8e580cbecb2c174e49
```

## Rejected Alternatives

1. Candidate S remains readable but leaves the right combat lattice too
   square-board-first.
2. Candidate T demotes local line work but does not change the broad combat
   field enough versus S.
3. Candidate V creates the strongest continuous combat plane, but it over-washes
   authored stone detail and makes the field feel like a soft fog layer rather
   than thick dungeon material.

## Why Not Micro-tuning

The selected route changes the source family for the right combat field. It is
not an alpha, seam, line-weight, fog-rectangle or draw-order tweak. Runtime
numeric tuning remains subordinate to preserving actors, loot, cursor, telegraph
and visibility authority above the room art plate.

## Rollback

Restore the previous S resource from:

```text
UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-s-stronger-surface-cohesion/ui_map_stage_ruins_room_plate_pr08_demo.png
```

No schema, core/game rule, save/replay, tileset rollout or manifest schema
change is required for rollback.

## Validation

Run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Result: expected failure because PR-08 evidence generation intentionally does
not rebaseline PR-02-1 golden hashes. The command wrote the selected runtime
crop and hashes recorded above.
