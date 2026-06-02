# PR-08 Candidate F Structural Overlay Grammar Decision

Date: 2026-06-01

## Verdict

Accepted forward, but D3 map-stage closure remains rejected.

This pass materially reduces the default tactical-board read over the active `tileset.ruins` room art plate by removing idle full-room grid hints, letting the room plate own ground material, reducing art-plate explored fog opacity, and converting art-plate overlay/target feedback into restrained runtime marks. It does not update golden baselines and does not claim all-map closure.

## Scope

Changed runtime behavior:

1. The active art-plate path no longer draws `tile_ground` base terrain over the room plate.
2. Idle MAP mode no longer paints global art-plate grid softener lines.
3. Art-plate fog merges matching horizontal runs vertically where possible and uses a lighter explored veil.
4. Art-plate vfx/telegraph overlays draw the original asset at lower alpha plus small corner/center marks instead of using only full-strength square plates.
5. Target highlights keep the restrained art-plate corner grammar from the previous in-flight pass.

Unchanged contracts:

1. No `core` or `game` rule, save, replay, visibility, pathing, combat or schema change.
2. Actors, loot, player indicator, target state, telegraphs and combat feedback remain runtime-owned.
3. Non-ruins tilesets keep the existing renderer fallback.
4. Golden PR-02-1 baselines are not rebaselined.

## Evidence

Artifacts:

1. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/ui-demo-new-map-stage-crop.png`
2. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/ui-demo-new-parity-1672x941.png`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/ui-demo-new-parity-1280x800.png`
4. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/ui-demo-new-right-panel-grid.png`
5. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/evidence-index.tsv`
6. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-structural-overlay-grammar-pass/golden-status.txt`

Latest focused golden map hash:

```text
ui-demo-new-map-stage-crop=d48c1f6b0f7dbc767218e081555ecb02c1d9b325a1cf92ff11b306c051ff867d
```

Previous targeted-art-edit map hash:

```text
ui-demo-new-map-stage-crop=781eda1ab95dccd6400307db6e1358cd48f7d56cbbd7d17b34a9f45f70b6263b
```

## Validation

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasMergesPr08FogVeilsIntoRoomScaleBandsOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasLetsPr08RoomArtPlateOwnGroundMaterialInsteadOfBaseTileSquares --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08SpriteOverlayGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests com.ktome.client.render.TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell --no-configuration-cache
```

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew maintainabilityLint --no-configuration-cache
```

Expected failure:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Reason: PR-08 evidence is intentionally archived without updating the old PR-02-1 golden baseline.

## Director Read

Accepted:

1. The authored room plate is more visible in hidden/explored shoulders.
2. Default MAP mode no longer has a renderer-authored all-room grid.
3. Explored regions read less like opaque tactical masks.
4. Telegraph and targeting semantics are still runtime-visible.

Rejected for closure:

1. The right combat field still reads partly as a square tactical board because loot/player/marker surfaces and visibility topology remain tile-shaped.
2. The result is an integration improvement, not a final UI-director/art-director first-read match to `UI/UI-demo-new.png`.

Next stop condition:

1. Run a marker-surface grammar pass for art-plate mode: keep actor, loot, player and target readability, but stop item/selection marker backing surfaces from becoming filled square cards over the environment.
2. If that still fails, stop renderer micro-iteration and regenerate or structurally edit the room plate around a less grid-shaped combat field.
