# PR08 Non-Ruins Topology Source V2 Decision

## Verdict

`pr08-non-ruins-topology-source-v2-limited-accepted-forward`

## Scope

This packet follows `pr08-topology-risk-interaction-grammar-v1`. The updated
D9 runtime-risk boards showed that hybrid interaction feedback no longer falls
back to legacy tile-first squares, but the actual map-stage crops still read too
flat and dark because the non-ruins dedicated topology sources carried very
little visible material richness.

## Change

1. The `forest_edge`, `mine` and `shadow_depths` topology-source PNGs are
   replaced in-place on their existing runtime paths.
2. `assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml` now points
   those three source candidates to this V2 evidence directory.
3. `RoomArtPlateCatalog` keeps the frozen ruins dedicated topology-source alpha
   at `0.50`, while non-ruins dedicated topology sources now render at `0.62` so
   the richer V2 source material is not fully buried in the runtime stack.

No visual key, manifest schema, gameplay model, save/replay/profile data,
content-pack rule or `core/game` contract changes in this packet.

## Source Evidence

| Family | V1 `brightOver70` | V2 `brightOver70` | V2 hash |
| --- | ---: | ---: | --- |
| `forest_edge` | `0.005484` | `0.053702` | `b4d845b549688434db3cb8f1be9d22cf753727e1ba40ebd604afa8d9da728a67` |
| `mine` | `0.001727` | `0.032493` | `7335e931483630be5fb61fd008df6d4480c4df9cc0db313208a51fbc1fe9ae3e` |
| `shadow_depths` | `0.000077` | `0.020147` | `12368e284c314ee32cda27683615dbd9b8f7b0396f6af6e1600c538beeed7ba1` |

Board:

`UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v2-2026-06-03/non-ruins-topology-source-v2-board.png`

## Runtime Evidence

| Runtime crop | Hash | Topology source hash |
| --- | --- | --- |
| `forest_edge` D9 risky seed `2026060908` | `09c8717c650156ddce0d0b0fdfa79570080b7599ec783beb2e62f20848e1a908` | `b4d845b549688434db3cb8f1be9d22cf753727e1ba40ebd604afa8d9da728a67` |
| `shadow_depths` D9 disconnected seed `2026060920` | `c645c229ca0a79fc5ee653ccb7d2a2d81833f2f4b731d1e9d504c5e2f8d812be` | `12368e284c314ee32cda27683615dbd9b8f7b0396f6af6e1600c538beeed7ba1` |

Runtime artifacts:

1. `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v2-2026-06-03/forest-edge-topology-source-v2-board.png`
2. `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v2-2026-06-03/forest-edge-topology-source-v2-crop.png`
3. `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v2-2026-06-03/shadow-depths-topology-source-v2-board.png`
4. `UI/review/dark-uiux-pr08-exploration/non-ruins-topology-source-v2-2026-06-03/shadow-depths-topology-source-v2-crop.png`

## Runtime Observation

The source assets themselves are materially stronger and remain crop-safe after
the overbright full-perimeter candidate was rejected during generation. The
actual runtime crops changed, but whole-crop brightness metrics remained almost
flat:

| Crop | `midOver45` | `brightOver70` |
| --- | ---: | ---: |
| `forest_edge` V1 interaction crop | `0.1051` | `0.0236` |
| `forest_edge` V2 exposed crop | `0.1056` | `0.0236` |
| `shadow_depths` V1 interaction crop | `0.1084` | `0.0291` |
| `shadow_depths` V2 exposed crop | `0.1085` | `0.0291` |
| `UI/UI-demo-new.png` reference | `0.1166` | `0.0651` |

This means the resource change is valid but limited: the stronger source is not
the dominant remaining blocker. The next higher-ROI map-stage packet should
address aperture/framing or runtime scale/visibility pressure instead of
continuing to regenerate non-ruins topology-source art.

## Focused Contract

`TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies`
now proves non-ruins topology-risk rooms crop their dedicated source bands at
the non-ruins exposure alpha, while the ruins topology-risk tests still preserve
the frozen `0.50` alpha path.

## Validation

1. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --no-configuration-cache`
   passed.
2. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesPr08InteractionGrammarOverTopologyRiskHybridRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --no-configuration-cache`
   passed.
3. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache`
   passed after the final non-ruins exposure change.

## Not Closed

This packet does not close packaged parity, all-map quality, director golden
rebaseline, exact full-width proof or final PR08 quality. It also should not
trigger another same-family source-art pass by itself; the evidence points to
map-stage aperture/framing as the next better target.
