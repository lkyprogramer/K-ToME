# PR08 V38 Room Breakup Resource Decision

## Verdict

Accepted-forward only.

V38 replaces `tileset.ruins.room_breakup_01` with a lower-alpha,
room-scale, off-grid stone breakup resource and adds a focused resource quality
test for that contract. It improves the resource authority itself, but it does
not close the director-grade map gap: the V38 map/reference resized mean
absolute RGB distance is `15.575196621496431`, slightly worse than V37
`15.49837559657294`.

## Change Shape

- Owner module: `client`, `tools`
- Change type: dark-v1 room material resource update plus resource-quality test
- Stable contracts touched: no gameplay rules, core visibility,
  save/replay/profile/schema/content-pack, manifest key/schema, or resource key
  changes
- Resource key preserved: `tileset.ruins.room_breakup_01`

## Production

- Replaced
  `client/src/main/resources/dark-v1/tiles/tileset_ruins_room_breakup_01.png`
  with a lower-alpha off-grid stone texture.
- Updated the matching raw sheet cell in
  `assets-src/image/raw/sheets/dark-v1/r02-ui-demo-ruins-tiles.png`.
- Updated the matching contact sheet cell in
  `assets-src/image/contact-sheets/dark-v1/r02-ui-demo-ruins-tiles-contact.png`.
- Refreshed the r02 raw-sheet hash and `tileset.ruins.room_breakup_01`
  cell/output hashes in the PR00, PR02-2 and PR08 sprite map reports.

## Tests

- Added focused resource assertions to
  `DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture`.
- RED proved the previous resource failed the new room-scale plate requirement:
  `largestMaterialIslandLongSpan=94`, `largestMaterialIslandShortSpan=40`,
  `largestMaterialIslandAxisDistance=9.767090614655844`.
- GREEN final resource metrics:
  - alpha coverage: `0.4998779296875`
  - max alpha: `114`
  - unique color count: `5371`
  - mean weighted edge: `2.321037265637058`
  - largest material island: `128x78`
  - largest island axis distance: `14.510029721154714`

## Evidence

| Artifact | Path |
| --- | --- |
| Comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-comparison-board.png` |
| Runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-runtime-metrics.json` |
| V38 map crop | `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-ui-demo-new-map-stage-crop.png` |
| V38 room closeup | `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-v38-room-closeup.png` |
| V38 resource crop | `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-tileset-ruins-room-breakup-01.png` |
| V38 vs V37 map diff | `UI/review/dark-uiux-pr08-exploration/runtime-room-breakup-resource-v38/pr08-room-breakup-resource-v38-v38-v37-map-stage-diff.png` |

## Metrics

- V38 vs V37 map-stage crop changed ratio:
  `0.24689494566154907`
- V38 vs V37 map-stage mean absolute RGB diff:
  `0.7000175697519151`
- V38 vs V37 full-screen changed ratio:
  `0.11324500175421648`
- V38 vs V37 full-screen mean absolute RGB diff:
  `0.3213815683542738`
- V37 resized map/reference mean absolute RGB diff:
  `15.49837559657294`
- V38 resized map/reference mean absolute RGB diff:
  `15.575196621496431`

## Commands Run

- RED, expected failure on the previous resource:
  `./gradlew :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture'`
- GREEN and resource/client verification:
  `./gradlew resourcePipelineLint spriteSheetMapLint :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 room material breakup resource keeps painterly negative space and micro texture' :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest :client:clientSmoke maintainabilityLint`
- Focused golden evidence:
  `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- Local Python/PIL resource generation, contact-sheet update, report hash refresh
  and evidence board generation.

## Result

- PASS: focused room material resource test after final V38 resource.
- PASS: `resourcePipelineLint`, `spriteSheetMapLint`,
  `ManifestResolveTest`, `TileRendererCanvasTest`, `:client:clientSmoke` and
  `maintainabilityLint`; three smoke subtests were skipped by existing
  environment assumptions.
- EXPECTED FAIL: PR02-1 golden hash gate failed because runtime map/full pixels
  changed and baselines remain intentionally unchanged.

## Director Notes

V38 should not continue as another alpha-tuning pass on the same
`room_breakup_01` key. The resource now has a valid off-grid room-scale
material contract, but the map still reads too rectangular and too grid-led
relative to `UI/UI-demo-new.png`. The next map move should be structural:
wall/floor silhouette, room boundary thickness, or a larger map-stage
composition decision, not another isolated breakup texture adjustment.
