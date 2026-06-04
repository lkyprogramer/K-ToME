# PR08 Topology-Risk Interaction Grammar Decision

## Verdict

`pr08-topology-risk-interaction-grammar-v1-accepted-forward`

## Scope

This packet follows `pr08-topology-risk-ground-ownership-v1`. Once
`TOPOLOGY_RISK_HYBRID_PRESENTATION` lets the authored topology source own the
floor plane, legacy square interaction feedback became the next mismatch:
sprite overlays, target highlights, player indicators, loot markers, cursor and
fog still used the tile-first grammar unless the compositor was a full room
plate.

## Change

1. `RoomCompositorStrategy` now exposes explicit ground-material and
   interaction-grammar ownership properties.
2. `TOPOLOGY_RISK_HYBRID_PRESENTATION` now uses the same restrained art-plate
   interaction grammar as `ART_PLATE_PRESENTATION`.
3. Legacy interaction fallback remains limited to
   `LEGACY_TILE_DECORATION`.

## Evidence

| Artifact | Hash |
| --- | --- |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-interaction-grammar-2026-06-03/forest-edge-interaction-grammar-crop.png` | `24bd8f48a2251b1c1d703f2dd5046edf2564288e465c0d5fb190051ae31cf678` |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-interaction-grammar-2026-06-03/forest-edge-interaction-grammar-board.png` | `0a9d1d6c8a87aa8d42431f6600d7834e289754dbc18398e7f1492d9bd19e683a` |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-interaction-grammar-2026-06-03/shadow-depths-interaction-grammar-crop.png` | `1ac205dca47ea14644eb2657b9a69e87adc38816781fbe524673f4770b2ae9ab` |
| `UI/review/dark-uiux-pr08-exploration/topology-risk-interaction-grammar-2026-06-03/shadow-depths-interaction-grammar-board.png` | `0597404726bca9b9d9ab4207554e870435922b939ca3e5efd7531b2ca8e70c5e` |

## Runtime Observations

1. Forest-edge seed `2026060908` remains `TOPOLOGY_RISK_LOW_FILL` with bounds
   `14x11`, `93` visible cells, `fillPermille=603`, and one connected component.
2. Shadow-depths seed `2026060920` remains `TOPOLOGY_RISK_DISCONNECTED` with
   bounds `16x12`, `129` visible cells, `fillPermille=671`, and two connected
   components.
3. Both runtime crop hashes changed after the interaction grammar extension.
   The visible effect is strongest around runtime fog/aperture treatment and
   marker/cursor surfaces: the hybrid room reads less like a tile UI fallback
   pasted over an authored source.

## Focused Contract

`TileRendererCanvasTest.renderCanvasUsesPr08InteractionGrammarOverTopologyRiskHybridRoomArtPlate`
proves a topology-risk L-shaped room now:

1. uses dedicated topology-source bands without stretching the full plate;
2. reduces sprite overlay alpha and adds restrained overlay marks;
3. avoids broad target tile fills and broad cursor outlines;
4. uses restrained target, player, cursor and loot marker grammar.

## Validation

1. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesPr08InteractionGrammarOverTopologyRiskHybridRoomArtPlate" --no-configuration-cache`
   passed.
2. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesPr08InteractionGrammarOverTopologyRiskHybridRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08SpriteOverlayGrammarOverRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08TargetingGrammarOverRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesRestrainedPr08MarkerSurfaceGrammarOverRoomArtPlate" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache`
   passed.

## Not Closed

This packet does not claim packaged parity, final director golden rebaseline,
all-map closure or final PR08 quality. It also does not regenerate topology
source art. The next high-ROI slice should compare the updated interaction
runtime boards against the reference quality bar and decide whether the weaker
remaining read is source-art richness, map-stage aperture framing, or packaged
scenario parity.
