# PR-08 Candidate F Topology Grammar Decision

**Date**: 2026-06-01
**Packet**: `runtime-candidate-f-topology-grammar-pass`
**Verdict**: accepted-forward for interaction grammar; D3 map-stage closure is
still rejected

## Root Cause

The marker-surface pass removed the strongest single-cell card shapes, but the
targeting path still had a second grid source: when `targetHighlights` existed,
the art-plate readability pass drew seam hints across every visible floor
adjacency. That made target/range mode read as a whole-room tactical board
instead of a local interaction state over an authored room.

The fixed PR-02-1 golden map crop does not enter targeting mode, so this pass
does not change the default `ui-demo-new-map-stage-crop` hash. The unchanged
hash is evidence that the remaining default crop blocker is now structural room
plate / visibility-field composition, not this targeting-only code path.

## Route

Implemented one bounded client-only topology grammar pass:

1. Replaced the art-plate targeting grid softener with target-local topology
   hints derived only from existing legal `targetHighlights`.
2. Changed legal target rendering from per-cell veil + corner marks to row
   range bands, legal-target boundary marks and tiny connectors between
   adjacent legal targets.
3. Preserved illegal target and active cursor readability.
4. Kept legacy non-art-plate targeting grammar unchanged.

No `core`, `game`, save, replay, profile, terrain, visibility, pathing,
combat, schema, manifest or content-pack contract changed.

## Why Not Micro-Tuning

This pass deliberately did not adjust marker alpha, corner length, fog alpha or
same-resource darkness. The problem was not a single overlay being too bright;
it was a room-wide topology source becoming a second tactical-grid authority in
targeting mode. The fix removes that source instead of tuning its opacity.

## Rejected Alternatives

1. Keep the all-floor grid softener and lower alpha again: rejected because it
   leaves the whole-room adjacency graph in place.
2. Add another renderer flag for target mode: rejected as option-sprawl; the
   existing art-plate branch and target highlight data already define the
   owner path.
3. Add a separate visibility or targeting model: rejected because it would risk
   a second runtime truth.
4. Rebaseline the golden crop: rejected because the default crop is still not a
   director-grade closure and this packet does not justify changing PR-02-1
   baseline hashes.

## Evidence

Artifacts:

1. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/marker-surface-vs-topology-grammar-map-board.png`
2. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/ui-demo-new-map-stage-crop.png`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/ui-demo-new-parity-1672x941.png`
4. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/ui-demo-new-parity-1280x800.png`
5. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/evidence-index.tsv`
6. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/runtime-candidate-f-topology-grammar-pass/golden-status.txt`

Actual map-stage hash:

```text
ui-demo-new-map-stage-crop=124ba9a1938e51856f81695e2920c047d0aaa5a607534bdbb8c4af519a04335f
```

The hash matches the previous marker-surface packet. The topology change is
covered by focused canvas tests, while the default runtime crop remains
unchanged and therefore cannot support D3 closure.

## Validation

Passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate --no-configuration-cache
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

Expected fail:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

The golden command wrote current artifacts and failed only because PR-02-1
baseline hashes were not updated.

## Rollback

Rollback is local to:

1. `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
2. `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`

Remove the target-local range bands, boundary marks and connector assertions,
then restore the previous art-plate grid softener call if the targeting
interaction proves less readable.

## Director Verdict

Accepted forward:

1. Targeting mode no longer has a code path that converts all visible floor
   adjacency into whole-room grid seams.
2. Legal target topology is now bounded to the actual highlighted target set.
3. Legacy renderer and non-ruins fallback behavior remain unchanged.

Not accepted:

1. D3 map-stage closure.
2. Golden rebaseline.
3. All-map closure.

Next action:

Stop renderer micro-iteration for this blocker. Because the default crop is
unchanged, the remaining map-stage issue must move to a structural room-plate
art edit focused on the center-right combat field's baked square rhythm,
material clumps, light breakup and enclosing silhouette.
