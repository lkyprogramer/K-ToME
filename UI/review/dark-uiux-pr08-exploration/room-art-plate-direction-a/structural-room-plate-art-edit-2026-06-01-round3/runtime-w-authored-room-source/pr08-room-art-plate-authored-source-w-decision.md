# PR-08 Room Art Plate Authored Source W Decision

## Decision

Candidate W is accepted as the current same-key
`ui.map_stage.ruins.room_plate.pr08_demo` resource foundation.

D3 map-stage closure is accepted for the `tileset.ruins` proof slice only. W
changes the fixed runtime crop from fog/grid-first to authored-room-first:
thick walls, local torch pools, rough stone breakup and dark room mass now lead
the first read while actors, loot, enemy marker, cursor and runtime affordances
remain legible above the plate.

This is not final PR-08 closure, all-map closure or golden rebaseline approval.
Forest, mine and shadow-depth tilesets still use their fallback paths until
their own room art families have evidence.

## Root Cause Class

`authored-room source quality`.

Candidate U proved the integration path but still depended on a tactical field
read. The remaining blocker was not line weight or fog alpha; it was that the
source plate did not carry enough room-scale wall mass, material hierarchy and
local authored light to outrank the tactical lattice.

## Evidence

1. `runtime-u-vs-w-authored-source-board.png`
2. `ui_map_stage_ruins_room_plate_pr08_demo.png`
3. `ui-demo-new-map-stage-crop.png`
4. `ui-demo-new-parity-1672x941.png`
5. `ui-demo-new-parity-1280x800.png`
6. `evidence-index.tsv`
7. `golden-status.txt`

Selected hashes:

```text
resource=ccbb9a6d923b201fef03ce1013d1f7ae453f373d1ea9b1b47532bb989b66c742
runtime-u-vs-w-authored-source-board=45741d8228cad73de49fa82f1bc30894a97c83dbdef210e079fbfd37effef4fe
ui-demo-new-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676
ui-demo-new-parity-1672x941=dff87e79ca72445c3d77e1509799569c44daaa46d70b86c2f52cf806addc2180
ui-demo-new-parity-1280x800=24214c574ab650b830d02f4a6903500943efeb40e513e417dfb016439f6dc9b8
```

## Rejected Alternatives

1. Candidate U remains a valid rollback point, but it reads too fog/grid-first
   in the fixed crop and does not reach D3 map-stage closure quality.
2. More per-cell fog, seam, line-weight, marker or rectangle tuning is rejected
   as the main route because recent S/T/U/V evidence already showed diminishing
   returns.
3. Treating the visible tactical grid as the accepted product constraint is
   deferred. W proves a higher-quality authored room source can improve the
   first read without changing runtime authority.

## Why Not Micro-tuning

W changes the underlying room source: wall mass, slab rhythm, local warm light,
dark aperture and authored material hierarchy. The improvement is not from
alpha, draw order, fog rectangles, seam width or marker surface tuning. Runtime
overlays remain subordinate integration layers above the room art plate.

## Rollback

Restore the previous U resource from:

```text
UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-u-combat-field-surface-integration/ui_map_stage_ruins_room_plate_pr08_demo.png
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

Also run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.*Pr08*" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08*" --no-configuration-cache
./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache
./gradlew assetLint styleLint manifestLint --no-configuration-cache
git diff --check
```

Result: passed. A machine-local path scan over the touched W packet,
goal/log/spec and evidence documents also returned no repo artifact references
to local home paths or the transient generated-image cache.
