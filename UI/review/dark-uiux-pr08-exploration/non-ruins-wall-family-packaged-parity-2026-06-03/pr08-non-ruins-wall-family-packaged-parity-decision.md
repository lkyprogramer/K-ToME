# PR08 Non-Ruins Wall-Family Packaged Parity Decision

Date: 2026-06-03

## Decision

Rejected and backed out.

The topology-source-only packaged route is rejected for the current
non-ruins topology-risk crops. It preserved the mine crop, but forest-edge and
shadow-depths lost the previous V4 room richness and became source-strip /
grid-first.

The follow-up family-ghost recovery is also rejected. It was technically safer
than unsafe full-room stretching because it clipped the family plate to visible
topology bands, but it still made the runtime look like chopped source strips,
debug floor and auxiliary wall rails. The runtime compositor has been restored
to the more conservative dedicated-topology-source path: non-ruins
topology-risk rooms must not sample the accepted full-room family plate through
fragmented topology bands.

## Visual Verdict

The family-ghost board is now rejected evidence:

`UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-packaged-parity-2026-06-03/packaged-non-ruins-wall-family-family-ghost-v1-board.png`

Hash: `19a40a9e61fa60b8c1cace69745364b3cd571541c88b793cfdf929bc9c01d0b2`.

Forest-edge, mine and shadow-depths do not constitute accepted-forward visual
progress in this board. The screenshot evidence shows the same root problem:
pre-rendered room resources are being split into topology bands and then
stacked with wall-mass, wall-run and grid-like support layers. This is
technically bounded but visually wrong.

Therefore this packet counts as a failed packaged parity attempt and a
direction change trigger, not as progress toward director-grade closure.

## Implementation

Backout contract:

1. `RoomArtPlateRenderer.renderTopologyRiskHybrid` no longer draws
   non-ruins family ghost bands.
2. `TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies`
   now proves that non-ruins topology-risk rooms crop their dedicated topology
   source and do not sample the accepted full-room family plate through
   chopped topology bands.

No gameplay, core, schema, save/replay, content pack, manifest schema or
resource key contract changed.

## Evidence

Detailed hashes are recorded in:

`UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-packaged-parity-2026-06-03/evidence-index.tsv`

Rejected family-ghost packaged map hashes:

1. Forest-edge map crop:
   `60c95a8d5ee7e13fb8b8e95b85cbcf2ec897ed8702ed8f69b8d519972afae574`
2. Mine map crop:
   `dcc8920a762ce554be4cd8c54afeed859ee06b8aad04a5fd7d454682aef4fef3`
3. Shadow-depths clean map crop:
   `3c5f1f494c434f7624dd00f5811ef7b76ec958e40e24aac8adc45b590ebc132c`

Rejected topology-source-only board:

`UI/review/dark-uiux-pr08-exploration/non-ruins-wall-family-packaged-parity-2026-06-03/packaged-non-ruins-wall-family-topology-source-only-rejected-board.png`

Hash: `30661acf94ffb51800c7f6319165de33a897a8c7afb83f31734db7175fc9feac`.

## Validation

Commands actually run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --no-configuration-cache
```

Result: fail-first for the now-rejected ghost implementation. The test expected
two low-alpha family ghost bands and found zero.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --no-configuration-cache
```

Result: passed after the temporary ghost implementation. `BUILD SUCCESSFUL in
8s`. This pass is now historical evidence only; the implementation was later
backed out.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFramesNonRuinsTopologyRiskHybridWithBandScaleAperturePressure" --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 3s`.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 8s`.

After visual rejection and runtime backout, this focused command was rerun:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 10s`. The current assertion forbids
non-ruins topology-risk rooms from sampling the accepted full-room family plate
through chopped topology bands.

The focused manifest/renderer regression after backout also passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasUsesDedicatedTopologySourceBandsForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSubduesTopologyRiskWallCardsIntoRunLevelVeils" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFramesNonRuinsTopologyRiskHybridWithBandScaleAperturePressure" --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 4s`.

The D9 owner golden rerun after backout also passed:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ForestEdgeD9RiskyRuntimeCropWritesTopologySourceEvidence" --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.darkUiuxPr08ShadowDepthsD9DisconnectedRuntimeCropWritesTopologySourceEvidence" --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 8s`.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew resourcePipelineLint maintainabilityLint --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 2s`, `resource-pipeline-authority OK:
visualAssets=1086, audioAssets=330`.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 8s`. This confirms the packaged app and
whitebox preparation now correspond to the backed-out runtime state.

```bash
git diff --check
```

Result: passed with no output.

Absolute-path hygiene scan over the updated PR08 goal, plan, log, manual
record and packaged parity evidence files passed with no matches.

Packaged capture commands for the three non-ruins scenarios were run after
re-materialization. The shadow-depths final clean recapture used
`scripts/capture-macos-app-window.sh --bundle-id com.ktome.client --app-name K-ToME --pid 60343 --raw`
after moving the mouse outside the map with a temporary CGEvent helper, then
fixed crop derivation was regenerated from that same full-window capture.

## Remaining Risk

1. Forest-edge and shadow-depths remain below the previous V4 packaged richness
   in topology-risk fixed crops.
2. The backed-out family ghost proves that alpha/source/ghost-band tuning is
   not the right next lever.
3. This packet does not rebaseline director golden hashes and does not claim
   final PR08 closure.

## Next Action

Move to a broader map-stage presentation slice for non-ruins topology-risk
rooms. Do not continue source-only topology PNG iteration, alpha tuning, or
another governance-only accepted-forward update. The next useful packet should
either produce a visibly stronger topology-aware room composition or record a
freeze/escalation decision from packaged/D9 evidence.
