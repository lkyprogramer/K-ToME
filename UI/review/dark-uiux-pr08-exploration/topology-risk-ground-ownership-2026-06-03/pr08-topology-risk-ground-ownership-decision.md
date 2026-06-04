# PR08 Topology-Risk Ground Ownership Decision

## Verdict

`pr08-topology-risk-ground-ownership-v1-accepted-forward`

## Scope

This packet addresses the weakest recurring first-read issue seen in the D9
`forest_edge` low-fill and `shadow_depths` disconnected runtime boards: risky
rooms still read as a tactical ground-tile checkerboard before they read as an
authored room surface.

## Change

1. `TOPOLOGY_RISK_HYBRID_PRESENTATION` now shares PR08 ground-material ownership
   with full art-plate presentation: ground tiles and floor cell-material passes
   are suppressed, while wall tiles, actors, markers and runtime overlays remain
   runtime-owned.
2. `RoomArtPlateRenderer` adds a tileset-aware topology band mantle beneath
   dedicated topology source bands.
3. Dedicated topology source bands now render after hybrid seam/AO fields at
   `0.50` alpha, keeping the same visual keys and source-region contract.

## Evidence

| Artifact | Hash |
| --- | --- |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-ground-ownership-2026-06-03/forest-edge-ground-ownership-crop.png` | `618eb919b9bb609c0872c6cc19324ceb86de1ba6c8a67252c0c8008c6ff15cdd` |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-ground-ownership-2026-06-03/forest-edge-ground-ownership-board.png` | `51ed99f6c57f2f757a2c2694d343b6e442f0c85071329e9852a7136d4a6cfb4c` |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-ground-ownership-2026-06-03/shadow-depths-ground-ownership-crop.png` | `c27317c2a6368b4f4a35e670d83cb8915e201b6a794b90887a5ed65eb2aa15a0` |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-ground-ownership-2026-06-03/shadow-depths-ground-ownership-board.png` | `d9cf8a932454a95a416f09c8d2f7de8d3362c74c9c5135ab8699b4addb8eb4a5` |

## Runtime Observations

1. Forest-edge seed `2026060908` remains `TOPOLOGY_RISK_LOW_FILL` with bounds
   `14x11`, `93` visible cells, `fillPermille=603`, and one connected component.
2. Shadow-depths seed `2026060920` remains `TOPOLOGY_RISK_DISCONNECTED` with
   bounds `16x12`, `129` visible cells, `fillPermille=671`, and two connected
   components.
3. In both runtime crops, floor-grid repetition is demoted by topology source
   ownership while wall tiles and wall-family components keep tactical boundary
   readability.

## Validation

1. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache`
   passed.
2. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" --no-configuration-cache`
   passed.

## Not Closed

This packet does not claim packaged parity, exact full-width proof, final
director golden rebaseline, all-map closure or final PR08 quality. The next
slice should use the updated runtime boards to decide whether topology-risk
interaction grammar or a stronger generated topology source is the higher-ROI
follow-up.
