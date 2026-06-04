# PR08 Non-Ruins Wall-Family Variation Decision

## Verdict

`pr08-non-ruins-wall-family-variation-v1-accepted-forward`

The non-ruins topology-risk hybrid path no longer resolves every wall-family component role back to the full square base wall tile. `forest_edge`, `mine` and `shadow_depths` now provide distinct `base`, `crown`, `side`, `corner` and `door_contact` wall-family pieces through the existing `terrain_wall_family:*` and `terrain_wall_piece:*` manifest contract.

This is a resource/manifest-level wall-family variation packet, not a renderer rule rewrite. It reduces repeated square wall-card rhythm in the D9 runtime crops while keeping the existing topology-risk wall anchor and boundary component contract.

## Evidence

- Wall piece board: `UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-variation-2026-06-03/non-ruins-wall-family-variation-board.png`
- Runtime comparison: `UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-variation-2026-06-03/topology-source-v3-vs-wall-family-runtime-board.png`
- Runtime metrics: `UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-variation-2026-06-03/runtime-crop-metrics.tsv`
- Final forest-edge crop: `UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-variation-2026-06-03/forest-edge-wall-family-variation-crop.png`
- Final shadow-depths crop: `UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-variation-2026-06-03/shadow-depths-wall-family-variation-crop.png`

## Result

- Forest-edge D9 crop hash: `f9c13929e8429caa62d2bbd6e80557eeb51d8b8da95b304022517aeb4dc97e3b`
- Shadow-depths D9 crop hash: `293c813de4527cfacc1d85e11fbfa4c451d3084281a13d4f7fb72747ab8d4199`
- Edge metric moved from `1.359 -> 1.156` for forest-edge after wall-family variation.
- Edge metric moved from `2.034 -> 1.686` for shadow-depths after wall-family variation.

## Commands

- FAIL-FIRST: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08NonRuinsWallFamiliesResolveDistinctTopologyRiskPieces" --no-configuration-cache`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08NonRuinsWallFamiliesResolveDistinctTopologyRiskPieces" --no-configuration-cache`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --no-configuration-cache`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache`; latest rerun reports `visualAssets=1086`

## Limits

This is accepted-forward only. It does not close packaged parity, exact full-width proof, all-map quality, director hash rebaseline or final PR08 quality. The next high-ROI step should use packaged/non-ruins scenario parity or a broader map-stage presentation pass rather than another source-only texture pass.
