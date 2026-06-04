# PR08 Topology-Risk Wall-Mass Slab Decision

Date: 2026-06-03
Status: `pr08-topology-risk-wall-mass-slab-v1-accepted-forward`

## Decision

Accept forward as a bounded topology-risk hybrid wall-mass improvement.

This packet changes the topology-risk hybrid path from drawing every runtime
wall tile as a visible card toward a wall-mass-first composition:

1. topology-risk hybrid wall terrain now draws only sparse deterministic
   tactical anchors;
2. horizontal and vertical boundary runs receive heavier wall-mass slabs before
   the lighter wall-run veils;
3. the existing wall-run veil and sparse component anchors remain above the
   authored topology source.

It does not reach director-grade map-stage closure. The D9 runtime crops still
carry square wall/source rhythm, especially in `shadow_depths`, so the next
high-ROI slice should be resource-level topology-source or wall-family
variation rather than another alpha, seam, fog or rectangle pass.

## Evidence

Visual packet:

- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-mass-slab-2026-06-03/forest-edge-wall-mass-slab-board.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-mass-slab-2026-06-03/forest-edge-wall-mass-slab-crop.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-mass-slab-2026-06-03/shadow-depths-wall-mass-slab-board.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-mass-slab-2026-06-03/shadow-depths-wall-mass-slab-crop.png`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-mass-slab-2026-06-03/forest-edge-evidence-index.tsv`
- `UI/review/dark-uiux-pr08-exploration/topology-risk-wall-mass-slab-2026-06-03/shadow-depths-evidence-index.tsv`

Final crop hashes:

- `forest_edge`: `5b8d8a22592fd863618f6c5cabbbd0d4946f6eb391ad6911d714531cef7cca3c`
- `shadow_depths`: `9b3e8acd80a3174246812707af0162927c641778137c6d1a8031c70582b7733d`

Auxiliary luma/edge metrics against the previous wall-run veil packet:

| Crop | Previous mean luma | New mean luma | Previous edge mean | New edge mean |
| --- | ---: | ---: | ---: | ---: |
| `forest_edge` | 16.556 | 16.165 | 5.831 | 5.666 |
| `shadow_depths` | 20.317 | 19.925 | 8.234 | 8.022 |

The reduced edge mean matches the implementation intent: repeated wall-card
edge pressure is lower. The visual improvement is still not transformative
because the topology-source and wall visual language continue to expose square
blocks.

## Validation

Commands run:

- RED: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --no-configuration-cache`
  - Failed on the new sparse-anchor assertion: current topology-risk hybrid
    drew `20` per-cell wall cards for `20` visible wall cells.
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --no-configuration-cache`
  - `BUILD SUCCESSFUL in 5s`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesPr08InteractionGrammarOverTopologyRiskHybridRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFramesNonRuinsTopologyRiskHybridWithBandScaleAperturePressure" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --no-configuration-cache`
  - `BUILD SUCCESSFUL in 1s`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache`
  - `BUILD SUCCESSFUL in 7s`
- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache`
  - `BUILD SUCCESSFUL in 6s`

## Director Verdict

Accepted-forward only.

Positive:

- focused proof now locks sparse runtime wall-card anchors;
- both D9 runtime crops show lower edge mean than the previous wall-run packet;
- wall boundary runs have heavier mass before lighter veil and component
  anchors.

Still open:

- repeated square wall/source rhythm remains visible;
- `shadow_depths` remains below director-grade first-read quality;
- no packaged recapture, exact full-width proof, final owner gate, director hash
  rebaseline, all-map closure or final PR08 quality claim.

## Next Action

Use the next packet for resource-level topology-source or wall-family
variation:

1. generate or route less square, higher-variation topology-source wall material
   for non-ruins D9 risky crops; or
2. introduce wall-family variation through the existing resource authority
   route, then prove it with the same D9 forest/shadow crops.

Do not spend the next packet on another same-family alpha, seam, fog or
micro-rectangle adjustment.
