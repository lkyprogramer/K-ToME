# PR08 Topology-Risk Wall-Run Veil Decision

Date: 2026-06-03
Status: `pr08-topology-risk-wall-run-veil-v1-accepted-forward`

## Decision

Accept forward as a bounded topology-risk hybrid compositor improvement.

The packet reduces the repeated wall-card first read by:

1. dimming topology-risk hybrid runtime wall terrain to subdued tactical anchors;
2. adding horizontal and vertical run-level wall veils along visible topology boundaries;
3. skipping the PR08 wall-family relief repaint in topology-risk hybrid rooms;
4. drawing non-critical wall-family component pieces as sparse anchors.

It does not reach director-grade map-stage closure. The wall resources are still repeated 32px texture cards, so the next high-ROI slice should address wall-family resource variation or a stronger run-level wall-mass representation rather than another same-family alpha/seam tweak.

## Evidence

Visual packet:

- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-run-veil-2026-06-03/forest-edge-wall-run-veil-board.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-run-veil-2026-06-03/forest-edge-wall-run-veil-crop.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-run-veil-2026-06-03/shadow-depths-wall-run-veil-board.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-run-veil-2026-06-03/shadow-depths-wall-run-veil-crop.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-run-veil-2026-06-03/forest-edge-evidence-index.tsv`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-run-veil-2026-06-03/shadow-depths-evidence-index.tsv`

Final crop hashes:

- `forest_edge`: `f42006ed133fcdaf3cf8e8604592931a1ae67c2f6cd8595d31a0237f1ecf92a8`
- `shadow_depths`: `f3068cdff05cdb24bfd9758dd5b3857043a260cb8a5ecc408b47e2d5677fd6f0`

Auxiliary luma/edge metrics against the previous aperture-pressure packet:

| Crop | Previous mean luma | New mean luma | Previous edge mean | New edge mean |
| --- | ---: | ---: | ---: | ---: |
| `forest_edge` | 16.953 | 16.556 | 6.333 | 5.831 |
| `shadow_depths` | 20.677 | 20.317 | 8.808 | 8.234 |

The reduced edge mean matches the visual review: one repeated wall repaint layer is gone and long wall runs read less like stacked bright cards. The improvement is visible but not transformative.

## Validation

Commands run:

- RED: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --no-configuration-cache`
  - Failed before production filtering because topology-risk hybrid still drew the second wall-family relief repaint at alpha `0.4416`.
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --no-configuration-cache`
  - `BUILD SUCCESSFUL in 4s`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesPr08InteractionGrammarOverTopologyRiskHybridRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFramesNonRuinsTopologyRiskHybridWithBandScaleAperturePressure" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --no-configuration-cache`
  - `BUILD SUCCESSFUL in 1s`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache`
  - `BUILD SUCCESSFUL in 7s`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `BUILD SUCCESSFUL in 6s`
- PASS: `git diff --check`
  - no output
- PASS: repo-relative path scan over the updated PR08 docs and new evidence packet for local absolute-path patterns
  - no matches

## Director Verdict

Accepted-forward only.

Positive:

- wall-card edge noise drops in both D9 runtime crops;
- topology-risk hybrid keeps tactical wall anchors;
- wall runs now have run-level mass and no longer receive the second full-card repaint pass.

Still open:

- repeated 32px wall texture remains visible;
- no packaged recapture or exact full-width proof;
- no final owner gate, director hash rebaseline, all-map closure or final PR08 quality claim.

## Next Action

Use the next packet for a stronger wall-resource or wall-mass treatment:

1. generate or route higher-variance wall-family resources for non-ruins topology-risk crops, or
2. replace most topology-risk wall cards with deterministic run-level boundary mass while keeping sparse tactical anchors.

Do not spend the next packet on another same-family alpha, seam, fog, or micro-rectangle adjustment.
