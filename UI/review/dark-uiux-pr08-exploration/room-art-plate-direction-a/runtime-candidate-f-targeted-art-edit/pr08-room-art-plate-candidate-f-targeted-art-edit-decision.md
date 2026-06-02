# PR-08 Candidate F Targeted Art Edit Runtime Packet Decision

> Date: 2026-06-01
> Status: `candidate-f-targeted-art-edit-d3-not-closed`
> Scope: PR-08 Direction A `tileset.ruins` room art plate resource edit after placement/framing packet

## Direct Decision

Do not accept D3 map-stage closure yet.

Targeted Candidate F art edit D is accepted as forward progress and is now the
current same-key room plate resource foundation. It keeps the existing
`ui.map_stage.ruins.room_plate.pr08_demo` route, preserves the 1448x1086 plate
size, avoids baked gameplay objects/text, and focuses the edit on the
center-right combat field plus outer aperture shoulders.

The edit does not close D3. The right combat field is darker and less flat than
the placement/framing packet, but the runtime interaction square grammar still
organizes the first read too strongly.

## Root Cause Class

`resource-local floor rhythm + runtime interaction grid grammar`

The resource edit reduced the local floor-board problem, but the remaining
blocker now sits mostly in how the interaction layer presents target/range
states over the art plate.

## Resource/Edit Changes

1. Rejected the built-in image-generation edit output because it changed the
   room layout anchors and introduced statue/prop-like content.
2. Produced deterministic local Candidate F edit candidates A-E under
   `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/targeted-candidate-f-art-edit-2026-06-01/candidates/`.
3. Selected Candidate D: it darkens and varies the center-right combat field,
   adds larger diagonal worn-stone rhythm, and strengthens the right/lower
   aperture shoulders without swallowing actors, loot, cursor or props.
4. Rejected Candidate E as too dark for the current runtime overlay stack.
5. Replaced only the existing runtime PNG:
   `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`.
6. Updated the source authority spec to point to the selected edited candidate.

## Changed Files

1. `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
2. `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/targeted-candidate-f-art-edit-2026-06-01/candidates/*`
4. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/*`
5. PR-08 goal / plan / log / evidence documents

## Evidence

| Evidence | Path |
| --- | --- |
| Golden status | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/golden-status.txt` |
| Evidence index | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/evidence-index.tsv` |
| Selected source edit | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/pr08-room-art-plate-f-targeted-edit-d.png` |
| Map A/B board | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/placement-vs-targeted-art-edit-map-board.png` |
| Full 1672x941 parity | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/ui-demo-new-parity-1672x941.png` |
| Full 1280x800 parity | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/ui-demo-new-parity-1280x800.png` |
| Map-stage crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-targeted-art-edit/ui-demo-new-map-stage-crop.png` |

## Golden Actual Hashes

These are the actual hashes reported by the focused golden command. They are
not golden baseline updates.

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `f0fd5d3b6b4af98492359d73a69f53b130f18e2474bbb371e848f6fd0f668f88` |
| `ui-demo-new-parity-1280x800` | `c057e18ce94d0ebc6b8388b2ce39ea6a1560f546e4813322d78e59a15eca47f9` |
| `ui-demo-new-right-panel-grid` | `36e9b06dc1aba326458cd9c907e02062130d7fb5321007f13483fd18e96ccfcc` |
| `ui-demo-new-bottom-deck-no-command-hints` | `6bf2380371c23586c38737409e70fb487c903b82b89ed989fc80c0f8e837a142` |
| `ui-demo-new-nav-rail-crop` | `c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6` |
| `ui-demo-new-map-stage-crop` | `781eda1ab95dccd6400307db6e1358cd48f7d56cbbd7d17b34a9f45f70b6263b` |
| `ui-demo-new-inventory-page-1` | `021a21d9494e20ad4374d4209e2e66d38b7e0280d55f1845ca01b33377c039cf` |
| `ui-demo-new-inventory-page-2` | `3f93fa09695715e8ab612fbd28d62675e82f1a8a6b8dc0e1ba8430ce8a3a7a8f` |

## Commands Run

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint resourcePipelineLint
```

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test \
  --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries \
  --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" \
  --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures" \
  --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback
```

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew maintainabilityLint
```

EXPECTED FAIL:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

## Director Read

Accepted forward:

1. The center-right floor no longer reads as the same bright open field as the
   placement/framing packet.
2. Right/lower aperture shoulders read heavier and less flat.
3. Runtime markers remain readable, including actor, enemy, scroll, selection
   frame and fire marker.
4. The route stayed within the single-image resource authority and did not
   change manifest schema, visual key, raw output path or non-ruins fallback.

Not accepted for D3 closure:

1. The active interaction/range area still resolves as a square tactical board
   at first glance.
2. Further same-resource darkening starts to reduce room clarity before it
   removes the grid-first read; Candidate E showed that limit.
3. The next blocker is interaction overlay grammar over the art plate, not a
   missing broad room-plate art family.

## Why Not Micro-Tuning

This pass changed the underlying room plate pixels through the existing
single-image resource route. It did not tune renderer alpha, add fog rectangles,
change draw order, rebaseline golden hashes, or introduce a second resource
mapping table. The result shows the remaining issue is no longer best owned by
more same-image darkening.

## Rejected Alternatives

1. Do not integrate the built-in generated edit output: it changed room layout
   anchors and introduced statue/prop-like content.
2. Do not integrate Candidate E: it darkens the right field more, but starts
   flattening playable-room clarity without closing D3.
3. Do not create a new visual key, atlas schema or sprite-sheet owner route for
   this targeted edit.
4. Do not rebaseline PR-02-1 golden hashes from this packet.
5. Do not apply the ruins room plate to non-ruins tilesets.

## Rollback Path

Restore the previous Candidate F source image from
`UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/candidates/pr08-room-art-plate-f-continuous-stone-field.png`
to `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`,
then point `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`
back to that source candidate and rerun the same resource and focused client
gates.

## Next Action

Run a structural interaction-overlay grammar pass for the active art-plate
path. The goal is to keep hover/selection/targeting readable while preventing
range and default tactical surfaces from forming a first-read square board over
the authored room plate.
