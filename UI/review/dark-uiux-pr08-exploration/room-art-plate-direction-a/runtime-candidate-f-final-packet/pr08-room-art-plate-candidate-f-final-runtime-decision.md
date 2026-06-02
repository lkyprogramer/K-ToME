# PR-08 Candidate F Final Runtime Packet Decision

> Date: 2026-05-31
> Status: `candidate-f-retained-d3-not-closed`
> Scope: PR-08 Direction A `tileset.ruins` room art plate runtime packet after bounded composition pass

## Direct Decision

Do not accept D3 map-stage closure yet.

Keep Candidate F as the current `ui.map_stage.ruins.room_plate.pr08_demo`
resource foundation, because it is materially stronger than Candidate C and the
V2 alternatives. The blocker has moved from broad `resource-gap` to runtime
presentation composition: default non-interaction grid weight, room-plate
placement / clip fit, and black-field framing.

This decision rejects another open-ended renderer micro-tuning loop. The next
move, if the PR-08 loop continues on the map, must be a bounded
placement/framing pass or a targeted Candidate F art edit after that pass proves
insufficient.

## Resource Route Proof

| Field | Value |
| --- | --- |
| visualKey | `ui.map_stage.ruins.room_plate.pr08_demo` |
| runtime path | `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png` |
| source candidate | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/room-art-plate-v2-generated/candidates/pr08-room-art-plate-f-continuous-stone-field.png` |
| source SHA-256 | `21e520fe073a80e6f34f32badd86ec3a5ed181c10eddcbe42a0cb11b497579af` |
| runtime resource SHA-256 | `21e520fe073a80e6f34f32badd86ec3a5ed181c10eddcbe42a0cb11b497579af` |
| dimensions | `1448x1086` |
| canonical spec | `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml` |
| canonical manifest | `assets-src/image/manifests/phase2-visual-manifest.json` |
| runtime manifest | `client/src/main/resources/manifests/visual-manifest.json` |

## Runtime Evidence

| Evidence | Path |
| --- | --- |
| Golden status | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/golden-status.txt` |
| Evidence index | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/evidence-index.tsv` |
| Full 1672x941 parity | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-parity-1672x941.png` |
| Full 1280x800 parity | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-parity-1280x800.png` |
| Map-stage crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-map-stage-crop.png` |
| Right panel crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-right-panel-grid.png` |
| Bottom deck crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-bottom-deck-no-command-hints.png` |
| Nav rail crop | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-nav-rail-crop.png` |
| Inventory page 1 | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-inventory-page-1.png` |
| Inventory page 2 | `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-final-packet/ui-demo-new-inventory-page-2.png` |

## Golden Actual Hashes

These are the actual hashes reported by the focused golden command. They are
not golden baseline updates.

| Label | Hash |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `a948abb581328b8f176d352961a96d167b8623e3d666509be04bb20553a9d43b` |
| `ui-demo-new-parity-1280x800` | `6abb44b14429099ceec2d6ac9baa91870b785590bc1087878eeae96769f9d5a2` |
| `ui-demo-new-right-panel-grid` | `36e9b06dc1aba326458cd9c907e02062130d7fb5321007f13483fd18e96ccfcc` |
| `ui-demo-new-bottom-deck-no-command-hints` | `6bf2380371c23586c38737409e70fb487c903b82b89ed989fc80c0f8e837a142` |
| `ui-demo-new-nav-rail-crop` | `c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6` |
| `ui-demo-new-map-stage-crop` | `1ffb0314bcf9c9907daab737a35ae0813295aed0aad3c2cc78491dd6c63cc749` |
| `ui-demo-new-inventory-page-1` | `021a21d9494e20ad4374d4209e2e66d38b7e0280d55f1845ca01b33377c039cf` |
| `ui-demo-new-inventory-page-2` | `3f93fa09695715e8ab612fbd28d62675e82f1a8a6b8dc0e1ba8430ce8a3a7a8f` |

## Implementation Result

The bounded composition pass did four things:

1. Unified the PR-08 ruins room presentation predicate behind
   `DarkUiMapVisualKeys.supportsRuinsRoomPresentation`.
2. Stopped non-ruins tilesets from preloading or rendering PR-08 ruins room
   presentation resources even when test data contains ruins visual keys.
3. Made legacy `tileset.ruins.room_breakup_01` placement stable across player
   movement by deriving its drift from room geometry instead of `playerTile`.
4. Restored floor-material fallback for non-art-plate paths and reduced the
   art-plate seam softener alpha so the default static grid reads less loudly.

## Director Read

Accepted forward:

1. Candidate F remains the best current room plate foundation.
2. The bounded composition pass reduced static grid dominance versus the
   previous Candidate F packet.
3. Runtime markers, actors, loot, telegraph, cursor and tactical overlays remain
   above the room compositor.
4. The resource route and focused resource gates now have current validation
   evidence after Candidate F.

Not accepted for D3 closure:

1. The map still reads partly as a tactical board before an authored room,
   especially in the right-side combat area.
2. The full plate is still fitted to a topology AABB, so room art, black-field
   framing and actual playable shape are not yet fully harmonized.
3. The large black field around the room creates a framed combat-zone read
   rather than the more integrated dungeon aperture shown in the reference.

## Why Not Micro-Tuning

The pass deliberately fixes ownership and placement stability first. It changes
the presentation boundary and static grid budget, not just an isolated alpha
number. Further closure should not add more same-shaped seam rectangles; it
should either improve room plate placement/framing or use a targeted Candidate F
art edit if the current placement model cannot satisfy the authored-room read.

## Commands Run

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures"
```

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test \
  --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom \
  --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback \
  --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" \
  --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures"
```

PASS:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew manifestLint resourcePipelineLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope maintainabilityLint
```

EXPECTED FAIL:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

The golden command failed only because PR-08 has not rebaselined the PR-02-1
hashes; it wrote the archived evidence in this packet.

## Rollback Path

1. Restore `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`
   from the previous Candidate C or earlier Candidate F packet if the resource
   itself is rejected.
2. Remove the Candidate F final packet records if the evidence is superseded by
   a later packet.
3. Re-enable the legacy room compositor path by making
   `RoomArtPlateCatalog.resolve` return `null` for `tileset.ruins`, leaving
   non-ruins fallback unchanged.

## Remaining Blocks

1. D3 map-stage closure is not accepted.
2. No golden baseline update is authorized.
3. All-map closure is still blocked; only `tileset.ruins` has a room plate.
4. A later bounded pass should address plate placement/framing before another
   resource generation round.
