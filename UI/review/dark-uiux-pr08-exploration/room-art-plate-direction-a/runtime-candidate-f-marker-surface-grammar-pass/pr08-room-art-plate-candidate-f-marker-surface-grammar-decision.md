# PR-08 Candidate F Marker-Surface Grammar Decision

Date: 2026-06-01

## Verdict

Accepted forward, but D3 map-stage closure remains rejected.

This pass removes the strongest square-card marker surfaces over the active
`tileset.ruins` room art plate: the player no longer uses a full-tile gold box,
loot no longer uses a filled icon backing card, and the active cursor no longer
uses a complete tile outline. The pass keeps runtime-owned actor, loot, player,
targeting and cursor semantics visible through smaller contact shadows, short
rarity rails and corner marks.

## Scope

Changed runtime behavior:

1. Art-plate player indicator uses a compact foot shelf and corner marks instead
   of a full square frame.
2. Art-plate ground loot uses a small floor contact, rarity rail, count badge
   and optional special-accent tick instead of a filled square backing card.
3. Art-plate active cursor uses broken corner marks and a short lower pin
   instead of a full tile outline.
4. Legacy non-art-plate player, loot and cursor grammar is unchanged.

Unchanged contracts:

1. No `core` or `game` rule, save, replay, visibility, pathing, combat or schema
   change.
2. Actors, loot, player indicator, target state, telegraphs and combat feedback
   remain runtime-owned.
3. The current same-key room art plate resource remains
   `ui.map_stage.ruins.room_plate.pr08_demo`.
4. Non-ruins tilesets keep the existing renderer fallback.
5. Golden PR-02-1 baselines are not rebaselined.

## Evidence

Artifacts:

1. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/structural-overlay-vs-marker-surface-map-board.png`
2. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/ui-demo-new-map-stage-crop.png`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/ui-demo-new-parity-1672x941.png`
4. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/ui-demo-new-parity-1280x800.png`
5. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/evidence-index.tsv`
6. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-marker-surface-grammar-pass/golden-status.txt`

Latest focused golden map hash:

```text
ui-demo-new-map-stage-crop=124ba9a1938e51856f81695e2920c047d0aaa5a607534bdbb8c4af519a04335f
```

Previous structural-overlay map hash:

```text
ui-demo-new-map-stage-crop=d48c1f6b0f7dbc767218e081555ecb02c1d9b325a1cf92ff11b306c051ff867d
```

## Validation

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08MarkerSurfaceGrammarOverRoomArtPlate --no-configuration-cache
```

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvas*Pr08RoomArtPlate*" --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForNonRuinsTilesets --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasAddsPr08ApertureShoulderForHiddenVoidsInsideRoomPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08IdleGridHintsBelowInteractionWeight --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasMergesPr08FogVeilsIntoRoomScaleBandsOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasLetsPr08RoomArtPlateOwnGroundMaterialInsteadOfBaseTileSquares --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08SpriteOverlayGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08MarkerSurfaceGrammarOverRoomArtPlate --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08RoomMaterialBreakupStableWhenPlayerMovesInsideSameRoom --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFloorMaterialForNonArtPlateFallback --tests com.ktome.client.render.TileRendererCanvasTest.keepsBossTelegraphReadableWhenActorOccupiesCell --no-configuration-cache
```

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew maintainabilityLint --no-configuration-cache
```

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas draws ground loot marker with count badge and rarity glyph" --tests com.ktome.client.render.TileRendererCanvasTest.drawsGroundLootMarkerAboveTerrainAndBelowBlockingActorBadge --tests "com.ktome.client.render.TileRendererCanvasTest.combat decision target cursor marks illegal hover without relying on toast" --no-configuration-cache
```

Expected failure:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Reason: PR-08 evidence is intentionally archived without updating the old
PR-02-1 golden baseline.

## Director Read

Accepted:

1. The white loot backing card is gone from the map-stage first read.
2. The purple/yellow weapon marker no longer reads as a full square UI card.
3. The player state remains visible without asserting a full tile ownership box.
4. Targeting, loot count, rarity and actor readability remain visible.

Rejected for closure:

1. The right combat field still carries visible tile topology from target/range
   and visibility surfaces.
2. This is a marker grammar improvement, not a full match to the authored-room
   quality bar in `UI/UI-demo-new.png`.

Next stop condition:

1. If one more runtime pass is attempted, it must target interaction topology at
   the multi-cell/range/visibility surface level, not another marker alpha
   tweak.
2. If that still fails the authored-room first-read test, stop renderer
   micro-iteration and perform a structural room-plate edit around a less
   grid-shaped combat field.
