# PR-08 Wall Adjacency And Grid-Layer Contract Proposal

> Date: 2026-05-28
> Status: `slice-a-room-compositor-executed-not-final`
> Scope: Dark UI/UX PR-08 map-stage room-space hierarchy

## Direct Conclusion

The next PR-08 map iteration should not continue floor U04 micro-tuning. The
runtime crop proves that floor variants are technically wired, but the first
read still comes from a constant lattice and weak wall thickness language.

The recommended next path is a bounded map-space contract:

1. wall crown, side shadow, corner and door-contact thickness must carry room
   enclosure
2. always-on floor grid authority must be reviewed and demoted to material seam
   or interaction affordance
3. target, cursor, path, telegraph and selection clarity remain explicit
   interaction layers

## Evidence

Inputs reviewed:

1. `UI/UI-demo-new.png`
2. `UI/goal/dark-uiux-pr08-director-grade-iteration-goal.md`
3. `UI/goal/dark-uiux-pr08-director-grade-iteration-log.md`
4. `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/runtime-u04/ui-demo-new-map-stage-crop.png`
5. `UI/review/dark-uiux-pr08-exploration/floor-variant-autotile-contract/runtime-u04/ui-demo-new-parity-1280x800.png`

U04 result:

1. Positive: floor variants are manifest-owned, deterministic and visible in
   runtime evidence.
2. Positive: the room floor now has more material variation than the previous
   single-cell resource path.
3. Still blocking: the eye reads a regular square lattice before it reads a
   carved dungeon room.
4. Still blocking: wall edges do not yet carry enough crown, side and contact
   thickness to replace the grid as the room boundary authority.

## Contract Boundary

Allowed in the next slice:

1. reorganize renderer layer ownership for default floor seams versus
   interaction grid affordances
2. add focused tests that prevent the default map from relying on full-room
   grid overlays
3. generate or replace wall-family resources only through sheet-plan,
   key-registry, owner-contract and canonical manifest authority
4. add runtime crops and expected-fail golden evidence without updating
   baselines

Not allowed in the next slice:

1. no `core` / `game` terrain snapshot change
2. no save, replay, profile, manifest schema or atlas schema change
3. no production Kotlin hard-coded owner-key inventory
4. no new full autotile schema unless a separate PR-08 contract explicitly
   accepts that scope
5. no baked text, digits, hotkeys or labels in generated resources

## Layer Decision

Use three explicit map readings instead of one constant grid:

| Reading | Owner | Default State |
| --- | --- | --- |
| Room material | floor/wall resources plus deterministic room-scale compositor | always visible |
| Room enclosure | wall crown, side shadow, contact and corner language | always visible |
| Tactical affordance | cursor, targetable cells, path, selection, telegraph | visible only when interaction requires it |

The default map crop should preserve readable stone seams, but those seams must
read as authored material construction. A full-room uniform grid should not be
the dominant first read.

## Implementation Slices

### Slice A - Layer Inventory And Guard Tests

1. Inventory current renderer calls that draw grid-aligned full-room floor
   lines, seam underpaint, wall contact bands and tactical affordance lines.
2. Classify each call as material, enclosure or interaction.
3. Add or update focused canvas tests so default MAP mode cannot regain a
   full-room high-authority lattice.
4. Keep cursor, target and telegraph tests green so interaction clarity remains
   protected.

### Slice B - Wall Crown/Contact Runtime Experiment

1. If existing `tileset.ruins.wall_01` can carry the experiment, adjust only its
   authored sheet/resource and compositor contact hierarchy.
2. If additional wall pieces are required, stop and add explicit
   sheet-plan/key-registry/owner-contract/manifest entries before runtime use.
3. Generate the same runtime crop set as U04 and compare against reference and
   U04 evidence.
4. Record accept/reject verdict in the iteration log before touching right panel
   or bottom deck chrome.

## Validation Plan

Focused validation:

```bash
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint
```

Resource validation, if wall resources are touched:

```bash
python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml
python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint assetLint styleLint manifestLint resourcePipelineLint
```

Director evidence:

```bash
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts"
```

The golden task is expected to fail until a later close slice intentionally
updates accepted baselines.

## Acceptance Criteria

1. Map-stage crop first reads as an authored room volume, not a debug or board
   grid.
2. Wall crown and contact language explains room enclosure without depending on
   a dark lattice.
3. Player, enemies, loot, stairs, cursor, target, path and telegraph remain
   readable.
4. Right panel and bottom deck are not worsened by the map changes.
5. Goal and iteration log record the experiment, commands, crop paths and
   director verdict.

## Next Stop Condition

If this contract still fails to move the map from grid-first to room-first, stop
map compositor iteration and escalate to a formal wall resource family/autotile
resource decision. Do not continue by adding small rectangles, seam alpha tweaks
or another one-off floor patch.

## Slice A Runtime Result - 2026-05-28

Slice A has been executed as a runtime compositor hierarchy experiment.

Changed behavior:

1. the warm room compositor now flushes as `MAP_ROOM_COMPOSITOR` inside the map
   renderer, after terrain/cell material and before props, actors, loot,
   cursor, targeting, telegraph and combat feedback
2. `drawVisibleRoomSeamSoftener` was removed from the default room atmosphere
   path so the default map does not regain a full-room lattice through a late
   seam-softening layer
3. focused canvas coverage now asserts the room compositor stays below
   gameplay markers and tactical affordances

Runtime evidence:

1. `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/runtime-slice-a-room-compositor/ui-demo-new-map-stage-crop.png`
2. `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/runtime-slice-a-room-compositor/ui-demo-new-parity-1280x800.png`
3. `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/runtime-slice-a-room-compositor/ui-demo-new-right-panel-grid.png`
4. `UI/review/dark-uiux-pr08-exploration/wall-adjacency-grid-layer-contract/runtime-slice-a-room-compositor/ui-demo-new-bottom-deck-no-command-hints.png`

Validation:

1. PASS:
   `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`
2. EXPECTED FAIL:
   `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`

Director verdict:

1. accept Slice A as a layer-contract fix: room-scale warmth and material fields
   no longer paint over actors, loot, target affordances or cursor reads
2. do not accept Slice A as director-grade visual closure: the map-stage crop
   still reads grid-first, and wall crown/contact thickness remains too weak
   against `UI/UI-demo-new.png`
3. stop compositor-only map iteration here unless a new technique decision is
   made first

Next stop:

Proceed to a formal wall resource family/autotile/crown resource decision before
touching right panel or bottom deck chrome. The next candidate must make wall
crown, side shadow, corner and door-contact thickness the room enclosure
authority instead of relying on floor-grid suppression.

## Wall Resource Family Decision - 2026-05-28

The formal wall resource decision now lives at:

`UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/pr08-wall-resource-family-decision.md`

Decision:

1. do not replace `tileset.ruins.wall_01` with another single wall tile
2. move the next wall resource pass to a five-piece family: `base`, `crown`,
   `side`, `corner`, `door_contact`
3. use exact manifest keys plus manifest tags if the family enters runtime;
   do not add a manifest schema, atlas schema or region schema
4. treat the current generated board as direction evidence only, not as
   production-ready runtime slices

The generated direction board proves that thick crown/contact language can
carry room enclosure better than the current single wall tile. It does not yet
meet runtime resource quality because the slices retain uneven scale, composite
wall objects and residual panel backgrounds. The next pass should regenerate
clean isolated cells using the `material_depth` row as prompt seed before any
sheet-plan/key-registry/manifest mutation.

W02 update:

1. W02 generated a cleaner one-row five-piece wall family without the W01 panel
   background problem
2. W02 is acceptable as a rough runtime A/B prototype seed, not as final
   director-grade resource closure
3. any W02 runtime mutation must route through sheet-plan, key-registry,
   PR-08 owner contract, canonical manifest, runtime manifest sync and focused
   renderer tests
4. golden baselines remain expected-fail; W02 must be rejected if the runtime
   crop shows wall side/corner/contact scale competing with gameplay markers

Runtime Slice B update:

1. W02 has been routed through the canonical resource chain and focused tests
   passed
2. the engineering contract is accepted: wall-family resources are now
   manifest-owned and selected from existing map adjacency without changing
   `core` / `game` terrain snapshots
3. visual closure is rejected: W02 improves wall mass, but the runtime crop
   still reads grid-first and the single-orientation side/corner pieces repeat
   visibly
4. the next map decision should be an orientation-aware W03 wall family or a
   narrow renderer orientation contract, not right panel / bottom deck chrome
   and not another compositor-only pass
