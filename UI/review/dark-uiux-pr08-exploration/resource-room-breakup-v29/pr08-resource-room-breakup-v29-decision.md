# PR08 Resource Room Breakup V29 Decision

## Verdict

Accepted-forward only. V29 replaces the existing `tileset.ruins.room_breakup_01`
resource with a fragmented, semi-transparent painterly stone overlay and adds a
resource-quality regression so the previous flat/vector-like room decal cannot
quietly return.

## Why

V28 showed diminishing returns from rectangular compositor bites. The next
high-return gap was floor/wall resource density and corridor-mouth material
quality, not another runtime opacity pass. Reusing the existing PR-08 owner key
keeps manifest authority stable while improving the visible room surface.

## Evidence

- Runtime archive:
  `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/`
- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/pr08-resource-room-breakup-v29-comparison-board.png`
- Metrics:
  `UI/review/dark-uiux-pr08-exploration/resource-room-breakup-v29/pr08-resource-room-breakup-v29-runtime-metrics.json`

Key metrics:

- resource alpha coverage: `0.604248046875`
- resource unique color count: `5646`
- V29 vs V28 map-stage changed ratio: `0.29979491307764555`
- V29 vs V28 map-stage mean abs diff: `2.0848548738491814`
- V28 reference-distance mean abs diff: `17.126598208524204`
- V29 reference-distance mean abs diff: `15.950576128137797`
- right panel and bottom deck changed ratio: `0.0`

## Validation

- PASS:
  `./gradlew resourcePipelineLint :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture' :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomMaterialBreakupAsSingleRoomScaleAsset' --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers' maintainabilityLint`
- PASS:
  `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' :client:clientSmoke`
- EXPECTED FAIL:
  `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`

The golden failure is expected because PR-02-1 baseline hashes are intentionally
not updated during this iterative director-grade exploration.

## Next

Do not keep tuning this same `room_breakup_01` texture. Move to map-stage
composition/corridor mouth architecture, or take a right/bottom density pass if
map-only deltas shrink again.
