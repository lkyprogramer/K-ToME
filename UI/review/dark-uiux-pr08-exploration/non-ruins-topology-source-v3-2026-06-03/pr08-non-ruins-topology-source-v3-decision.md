# PR08 Non-Ruins Topology Source V3 Decision

## Verdict

`pr08-non-ruins-topology-source-v3-limited-forward`

The V3 `forest_edge`, `mine` and `shadow_depths` topology-source atlases are better source resources than V2: they use broader non-grid material flow, stronger mid-band light pools and clearer family-specific material language. They are kept as the current same-key runtime topology-source PNGs.

The runtime D9 crop evidence shows source-only replacement is not the main remaining quality lever. The forest-edge crop edge metric moves `1.339 -> 1.359` versus the wall-mass-slab packet, and shadow-depths remains `2.034 -> 2.034`; therefore V3 is limited-forward, not a director-grade runtime jump.

## Evidence

- Source board: `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v3-2026-06-03/non-ruins-topology-source-v3-source-board.png`
- Source metrics: `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v3-2026-06-03/source-metrics.tsv`
- Runtime comparison: `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v3-2026-06-03/wall-mass-slab-vs-topology-source-v3-runtime-board.png`
- Runtime metrics: `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v3-2026-06-03/runtime-crop-metrics.tsv`

## Commands

- PASS: `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache`

## Follow-Up

Do not continue same-family topology-source brightness or texture-only iteration as the next move. The evidence says the repeated runtime wall-card read is the higher-ROI blocker.
