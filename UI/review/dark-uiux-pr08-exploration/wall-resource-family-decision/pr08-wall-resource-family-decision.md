# PR-08 Wall Resource Family Decision

> Date: 2026-05-28
> Status: `runtime-slice-d-grid-authority-not-final`
> Scope: wall crown/contact resource structure after wall-grid Slice A

## Direct Conclusion

The next runtime slice should not replace `tileset.ruins.wall_01` with another
single wall tile. The visual blocker is room enclosure language, so the resource
unit must be a small wall family:

1. `base`
2. `crown`
3. `side`
4. `corner`
5. `door_contact`

The first generated candidates prove the desired wall mass direction, but they
are not production-ready slices. They contain uneven scale, composite wall
objects and residual panel backgrounds from the source board.

The W02 clean-row pass removes the panel-background problem and is acceptable as
the next runtime A/B prototype seed. It is still not final director-grade
resource closure because the side/corner/contact pieces need runtime crop proof
against actors, loot and telegraphs.

Runtime Slice B has now executed W02 through the canonical resource path. The
engineering path is accepted, but visual closure is still rejected: W02 adds
clearer wall mass and crown/contact language, yet the map crop still reads as a
regular grid before it reads as a director-grade room space.

2026-05-28 follow-up: W03 orientation exploration, W04 full-tile wall family
work and Runtime Slice D have superseded W02 as the current comparison baseline.
The Slice D engineering path remains accepted, but the visual conclusion is
unchanged: wall/grid authority still dominates the first read. Floor V06e
regrain plus ground-first `tile_ground` bleed is accepted-forward separately as
floor polish, but it does not close the wall/grid blocker.

## Visual Thesis

Ruins walls should read as thick carved stone room enclosure first, with chipped
crown blocks, side occlusion and doorway contact shadows carrying the boundary.
The floor grid should become secondary material rhythm rather than the room
outline authority.

## Evidence Produced

Root:

`UI/review/dark-uiux-pr08-exploration/wall-resource-family-decision/generated-wall-family-candidates/`

Artifacts:

1. `pr08-wall-family-imagegen-source-board.png`
2. `pr08-wall-family-candidate-contact-board.png`
3. `pr08-wall-family-candidate-runtime-room-preview.png`
4. `slices/wall_conservative_*.png`
5. `slices/wall_material_depth_*.png`
6. `slices/wall_strong_style_*.png`
7. `build_wall_candidate_previews.py`

W02 clean-row artifacts:

1. `clean-wall-family-w02/pr08-wall-family-w02-imagegen-source-row.png`
2. `clean-wall-family-w02/pr08-wall-family-w02-contact-board.png`
3. `clean-wall-family-w02/pr08-wall-family-w02-runtime-room-preview.png`
4. `clean-wall-family-w02/slices/wall_w02_*.png`
5. `clean-wall-family-w02/build_clean_wall_candidate_preview.py`

Runtime Slice B artifacts:

1. `runtime-slice-b-w02/ui-demo-new-map-stage-crop.png`
2. `runtime-slice-b-w02/ui-demo-new-parity-1280x800.png`
3. `runtime-slice-b-w02/ui-demo-new-right-panel-grid.png`
4. `runtime-slice-b-w02/ui-demo-new-bottom-deck-no-command-hints.png`
5. `runtime-slice-b-w02/pr08-wall-family-w02-reference-slicea-runtime-comparison.png`
6. `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`

Runtime Slice D artifacts:

1. `runtime-slice-d-grid-authority/ui-demo-new-map-stage-crop.png`
2. `runtime-slice-d-grid-authority/ui-demo-new-parity-1672x941.png`
3. `runtime-slice-d-grid-authority/ui-demo-new-right-panel-grid.png`
4. `runtime-slice-d-grid-authority/ui-demo-new-bottom-deck-no-command-hints.png`
5. `runtime-slice-d-grid-authority/evidence-index.tsv`

Source-board hash:

`1ddbe93b2aea1c5b4d7c10682486fdcf452c005b13c9c28a66dc98beef88df3e`

Preview hashes:

1. contact board:
   `2ab97a33698547635d4d9a112aa510917fa47d99f79594a9616705ff2905a98a`
2. runtime room preview:
   `9fdf0e3f5ed60b3ad759d5cc73fb9df1c2d47ab808205e9c59049553e314a41f`

W02 hashes:

1. source row:
   `d2f0df51e8542974b133759892e4e0da050fef02967fed738f91ec979e3e342d`
2. contact board:
   `ebea9168b6d24b1ff30bac679552e292f9cb673e9c29c6548ed21e31b5ae59dd`
3. runtime room preview:
   `0307b9d420310e09a377b7ac31f92fcf6905d708815d8ac9815155befb0bb692`

Runtime Slice B hashes:

1. map crop:
   `a6f1ec4d63772c6cc1e2bae6fbdbb98055b3f9925efbbd8c76ce13e868b7d2f6`
2. full screen 1280:
   `e91a7f0361ca061e8d29ba603b1e66c9f64a0484d9cd45d0769df583d12d1bc5`
3. right panel:
   `417dc27fb77b5f7dd633cab0ca3e377dc82894c17bee9b4bc6676d841a6ff79f`
4. bottom deck:
   `b5bf7bd1840e2f34bc2f0e7ebbb1e4c39cd92f1bbd170e157b5eb297a093f4cf`
5. comparison board:
   `06e5a2731e4dbd15ada7cd96440a2f6a3dd34a9c22129ec3644301d0008ce94a`
6. PR-08 filtered sprite-map report:
   `9119015960be25a8e073aaf748d84bb5ce82e494d7cb9f46d01b40c629fec674`

## Candidate Verdict

| Candidate | Verdict | Reason |
| --- | --- | --- |
| conservative | `direction-useful` | clear crown and side mass, but too close to a repeated brick-wall panel |
| material_depth | `best-next-prompt-seed` | strongest balance of crown readability, chipped contact language and restrained contrast |
| strong_style | `too-heavy-as-is` | strong room enclosure, but bulky corners and doorway detail would likely compete with actors at runtime size |

Overall verdict:

1. accept `material_depth` as the next prompt seed
2. reject direct slicing into runtime resources
3. reject single-tile wall replacement as the next implementation unit

W02 follow-up verdict:

1. accept W02 as a rough runtime A/B prototype candidate
2. reject W02 as final production closure before a real map-stage crop
3. watch side/corner/contact scale and actor readability in the next runtime
   crop before updating any golden baseline

Runtime Slice B verdict:

1. accept the resource-authority implementation path: W02 now flows through
   sheet-plan, key-registry, PR-08 owner contract, canonical manifest, runtime
   manifest sync, preload and focused render-model tests
2. reject W02 as director-grade visual closure: wall mass is clearer, but grid
   authority remains too strong and the single-orientation side/corner pieces
   repeat visibly in the room crop
3. do not update golden baselines
4. next map step should either generate a cleaner orientation-aware W03 wall
   family or add a narrow renderer-side orientation contract before moving to
   right panel / bottom deck chrome

Runtime Slice D follow-up verdict:

1. accept Slice D as the current wall/grid engineering baseline
2. reject Slice D as director-grade visual closure because the coarse lattice
   still dominates first read
3. keep V06 floor polish separate from wall-family closure; it is not proof
   that the wall/grid blocker is solved
4. next map step should target wall/grid interaction or room-enclosure
   authority before right panel / bottom deck chrome work resumes

## Proposed Runtime Contract For The Next Slice

Use exact manifest keys without adding a new manifest schema:

1. `tileset.ruins.wall_01`
2. `tileset.ruins.wall_01.crown`
3. `tileset.ruins.wall_01.side`
4. `tileset.ruins.wall_01.corner`
5. `tileset.ruins.wall_01.door_contact`

Use manifest tags to describe family membership and piece role:

1. `terrain_wall_family:tileset.ruins.wall_01`
2. `terrain_wall_piece:base`
3. `terrain_wall_piece:crown`
4. `terrain_wall_piece:side`
5. `terrain_wall_piece:corner`
6. `terrain_wall_piece:door_contact`

Runtime selection should stay in `client` presentation/render code and derive
piece choice from existing map adjacency only. Do not change `core` / `game`
terrain snapshots, save/replay/profile schema, atlas schema or region schema.

## Alternatives Considered

1. Single replacement for `tileset.ruins.wall_01`
   - rejected because it cannot express crown, side, corner and doorway contact
     at the same time
2. Full wall autotile schema
   - rejected for PR-08 because it expands the schema surface beyond the current
     resource-family-first reset
3. Renderer-only wall shadow/crown paint
   - rejected as the primary path because Slice A already proved compositor
     hierarchy alone does not move the map out of grid-first read
4. Exact-key wall family with manifest tags
   - recommended because it matches the U04 floor variant precedent and avoids a
     new schema while giving the renderer real enclosure resources

## Next Action

Use Runtime Slice D plus floor V06e as the comparison baseline before touching
right panel or bottom deck chrome. The next wall/grid iteration must reduce the
coarse lattice first read in the runtime crop. W02 remains useful as the first
canonical runtime A/B proof that wall-family ownership can move room enclosure
out of renderer-only paint, but it is no longer the current crop baseline.
