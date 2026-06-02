# PR-08 Room-Scale Material Breakup V11

## Goal

Route the V10 blocker away from per-tile edge editing by adding a narrow,
resource-owned, non-grid-aligned room material layer for the ruins map. The
candidate must preserve marker readability, stay inside the existing visual
manifest model, and avoid becoming a second visibility or terrain authority.

## Change

Accepted production wiring:

1. Added `tileset.ruins.room_breakup_01` as a PR-08 `tile_decal` owner key in
   the existing `r02-ui-demo-ruins-tiles` sheet.
2. Added exact manifest coverage and owner-contract coverage for the new key.
3. Added a client visual-key constant and session preload for ruins snapshots.
4. Added a single room-scale draw in `MAP_ROOM_COMPOSITOR`, after the current
   room atmosphere pass and before aperture / warm overlay / gameplay marker
   layers.
5. Added focused renderer and manifest tests proving the key resolves exactly
   and is drawn as one large room asset, not as per-cell lattice repair.

No schema, save/replay/profile, `core`, `game`, content-pack, or manifest
format changes were introduced.

## Evidence

Artifacts:

| Artifact | Path |
| --- | --- |
| Candidate cell | `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/tileset_ruins_room_breakup_01_candidate.png` |
| Floor probe | `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/room_breakup_on_floor_probe.png` |
| Runtime archive | `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/runtime-v11-room-breakup/` |
| Runtime comparison board | `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/pr08-room-scale-material-breakup-v11-runtime-comparison-board.png` |
| Runtime metrics | `UI/review/dark-uiux-pr08-exploration/room-scale-material-breakup-v11/pr08-room-scale-material-breakup-v11-runtime-metrics.json` |

Hashes:

| Item | SHA-256 |
| --- | --- |
| retained r02 raw sheet | `8d316c96d351c7f28b33ebfd8b6146a281eb20762da4de9b0fb876d6e033c397` |
| retained r02 contact sheet | `693ccff4be5f668fae43a0c1e685524a6fd641745427e0182e72d8813ce4c0b2` |
| generated room breakup tile | `040f56dacdbe25fb3e0e655a97818c99be57e0def7fe81064824f4552e856378` |
| V11 runtime map crop artifact | `0785053b1b98fe8f7e13e4405e0a85fbc01942b6e4ed03f8bcdd90bd90040bd0` |
| V11 runtime comparison board | `417adf2d9d598089331947e0b9c6df21989a0beb81528bc4d4e79b2bfe67fede` |

Golden content hash from the retained expected failure:

`ui-demo-new-map-stage-crop=8d5acb1f488dc2e89f880fc3614b903190c228f7cf46de725b05820a0f718601`

## Validation

Passed:

1. `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
2. `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
3. `python3 scripts/generate_dark_final_full_inventory.py`
4. `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
5. `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`

Expected failure:

1. `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`

The golden failure is retained as evidence because PR-02-1 baselines were not
updated.

## Verdict

Accept V11 forward as infrastructure plus partial visual improvement.

It establishes the missing narrow contract for a room-scale, resource-owned
material field and confirms the runtime can render it as one large non-grid
asset below gameplay markers. It does not close PR-08 director-grade quality:
the map still reads too much as a regular square lattice before it reads as an
authored room.

## Next

Do not return to per-tile edge edits. The next productive cut should either:

1. make the room-scale material family carry stronger masonry authority without
   becoming fog wash, or
2. remove/demote the repeated dark lattice at its renderer/source boundary so
   the V11 resource can become the first-read material layer.
