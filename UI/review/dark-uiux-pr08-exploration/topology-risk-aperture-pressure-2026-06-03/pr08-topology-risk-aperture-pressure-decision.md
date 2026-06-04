# PR08 Topology-Risk Aperture Pressure V1 Decision

> Date: 2026-06-03
> Verdict: `pr08-topology-risk-aperture-pressure-v1-accepted-forward`
> Scope: D9 topology-risk map-stage runtime crops for `tileset.forest_edge` and `tileset.shadow_depths`

## Direct Conclusion

Accepted-forward.

The previous non-ruins topology-source V2 packet improved source richness but
barely moved the D9 runtime crop. This packet therefore moves to the planned
map-stage aperture/framing slice: topology-risk hybrid rooms now receive
band-scale aperture pressure, worn-stone catchlight lips and a very low-alpha
material lift after the dedicated topology-source bands.

This improves room-scale framing for the D9 low-fill and disconnected runtime
crops without changing resource keys, manifest schema, gameplay state,
visibility truth, `core`, `game`, save, replay or content-pack contracts.

It is not director-grade closure. The updated crops still show visible wall
block repetition and do not match the first-read richness of `UI/UI-demo-new.png`.

## Changed Runtime Contract

`RoomArtPlatePresentation` keeps the existing topology-risk hybrid order:

```text
mantle fields
material runs
interior seam dissolve
ambient depth
dedicated topology source cropped bands
band-scale aperture pressure
local light pools
boundary marks
wall components
```

The new aperture pass is clipped to true topology bands, not the room bbox, so
L-shaped hidden notches remain uncovered.

## Evidence

| Evidence | Path |
| --- | --- |
| Forest-edge D9 board | `UI/review/dark-uiux-pr08-exploration/topology-risk-aperture-pressure-2026-06-03/forest-edge-aperture-pressure-board.png` |
| Forest-edge D9 crop | `UI/review/dark-uiux-pr08-exploration/topology-risk-aperture-pressure-2026-06-03/forest-edge-aperture-pressure-crop.png` |
| Forest-edge evidence index | `UI/review/dark-uiux-pr08-exploration/topology-risk-aperture-pressure-2026-06-03/forest-edge-evidence-index.tsv` |
| Shadow-depths D9 board | `UI/review/dark-uiux-pr08-exploration/topology-risk-aperture-pressure-2026-06-03/shadow-depths-aperture-pressure-board.png` |
| Shadow-depths D9 crop | `UI/review/dark-uiux-pr08-exploration/topology-risk-aperture-pressure-2026-06-03/shadow-depths-aperture-pressure-crop.png` |
| Shadow-depths evidence index | `UI/review/dark-uiux-pr08-exploration/topology-risk-aperture-pressure-2026-06-03/shadow-depths-evidence-index.tsv` |

Runtime evidence-index hashes:

| Crop | Hash | Topology |
| --- | --- | --- |
| `forest_edge` seed `2026060908` | `f8b075977e4732582f646c8bc4f0a5f21e8870c2a8081dc629db93f5a0717154` | `TOPOLOGY_RISK_LOW_FILL`, `14x11`, `93` visible cells, `fillPermille=603` |
| `shadow_depths` seed `2026060920` | `de64b54c586371093833912cb1c290008cfaeb5fcfa070358ccb6530bf518636` | `TOPOLOGY_RISK_DISCONNECTED`, `16x12`, `129` visible cells, `fillPermille=671`, `connectedComponents=2` |

Auxiliary whole-crop luma metrics:

| Image | Mean luma | `luma > 45` | `luma > 70` | `luma > 90` |
| --- | ---: | ---: | ---: | ---: |
| `UI/UI-demo-new.png` | `19.3535` | `0.100322` | `0.052188` | `0.039575` |
| forest-edge aperture crop | `17.3042` | `0.074394` | `0.018198` | `0.007374` |
| forest-edge V2 previous crop | `17.2567` | `0.074073` | `0.018196` | `0.007374` |
| shadow-depths aperture crop | `20.9992` | `0.087408` | `0.023999` | `0.007592` |
| shadow-depths V2 previous crop | `20.9790` | `0.087223` | `0.023997` | `0.007592` |

The metric change is small but non-regressive against the V2 runtime crops. The
visual value is mainly room-scale framing, not brightness recovery.

## Commands Run

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFramesNonRuinsTopologyRiskHybridWithBandScaleAperturePressure" --no-configuration-cache
```

Result: PASS, `BUILD SUCCESSFUL in 7s`.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesPr08InteractionGrammarOverTopologyRiskHybridRoomArtPlate" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFramesNonRuinsTopologyRiskHybridWithBandScaleAperturePressure" --no-configuration-cache
```

Result: PASS, `BUILD SUCCESSFUL in 1s`.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache
```

Result: PASS, `BUILD SUCCESSFUL in 7s`.

## Director Verdict

Accepted-forward as
`pr08-topology-risk-aperture-pressure-v1-accepted-forward`.

This packet executes the planned map-stage aperture/framing slice and improves
the room boundary read for D9 topology-risk crops. It is intentionally local:
no golden rebaseline, no packaged parity claim, no exact full-width proof, no
all-map quality claim and no final PR08 director-grade closure.

The next higher-ROI slice should address the remaining repeated wall-block /
tile-card read with a larger topology-risk composition change or runtime-scale
visibility treatment, not another same-family topology-source art regeneration.
