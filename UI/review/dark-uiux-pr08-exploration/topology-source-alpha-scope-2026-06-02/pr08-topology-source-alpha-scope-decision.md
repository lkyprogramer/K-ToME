# PR08 Topology Source Alpha Scope Decision

Date: 2026-06-02

## Verdict

`pr08-topology-risk-source-alpha-scope-v1-accepted-forward`

The `0.42` topology-source band alpha is accepted only for families with a
formal dedicated topology source. Fallback families that still crop their
accepted full room plate must keep the older subdued `0.36` source-band alpha.

This prevents the ruins dedicated-source readability pass from silently
amplifying non-ruins family fallback plates before they have their own
topology-source assets.

## Runtime Change

Changed production source model:

```text
client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt
```

`RoomArtPlateSource` now carries an internal `topologySourceBandAlpha` selected
by `RoomArtPlateCatalog`:

1. dedicated topology source key exists: `0.42`
2. fallback to the accepted family room plate: `0.36`

Focused test lock:

```text
client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt
```

The fail-first test used forest-edge and shadow-depths L-shaped topology-risk
snapshots to prove non-ruins fallback source bands were incorrectly rendering at
`0.42`. After the production change, the same fallback bands render at
`0.36`, while the ruins dedicated topology source still renders at `0.42`.

## Evidence

D8 evidence root:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/d8/
```

D8 board hash:

```text
252b492f232fda07d310c639a8a9e519cf305f5cfddd82d393459df5d6c010f4
```

D8 evidence index hash:

```text
d067dbe2f43f53709c11f3027759f1abd64061f2d101dbd3df7e839280263029
```

Director recapture root:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/director-recapture/
```

Director recapture evidence-index hash:

```text
14da736d2bd251f7aae1f95cd139470e6bc6899ffaeaae07f2c263559dace6c0
```

Current director recapture hashes:

```text
dark-uiux-pr08-director-parity-1672x941=4caa515be022105c6a0c75f58c6abeb11fba73e043dbf98678458bb90437c692
dark-uiux-pr08-director-map-stage-crop=a4a8b0ca3256b61f833bdd0b3d533e58622f802abe000afe3e15f166a542427c
dark-uiux-pr08-director-right-panel-crop=201645bf13f9824fd95289063b92ea8adc722f9d0f344aca75902a2e99c26c47
dark-uiux-pr08-director-bottom-deck-crop=a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd
dark-uiux-pr08-director-forest-map-stage-crop=db845a05ebc50a4f5d0d5706e3f75662747c720880e68e4ba88a3555fb4da203
dark-uiux-pr08-director-mine-map-stage-crop=7233f0dc91505c77e7cfe4b1cbb8d02ed6d92c71d11b9e1911e33b1ecffbb3b4
dark-uiux-pr08-director-shadow-depths-map-stage-crop=bd328b7ab5d062e9db06ec91715c5adfd08d4462f86e8f69f389be5fec4dca2c
dark-uiux-pr08-director-telegraph-combat-crop=2e13535fea38cef3e7a624d3659685e756588d7df3ae4907e2dd7ab03ac51b57
```

Director read:

1. The ruins D8 board remains stable after scoping because ruins has the formal
   dedicated topology source.
2. The fixed director recapture still reports hash drift and is not rebaselined
   in this packet. It is evidence for current state and impact, not a pass.
3. The non-ruins fallback test protects against a concrete regression that was
   visible in exploratory forest/shadow crops: without a dedicated source,
   higher alpha exposes the family plate's square rhythm too strongly.

## Contract

1. No new visual key, manifest schema, source image or resource owner schema.
2. No core, game, save, replay, profile, localization, input or content-pack
   contract changed.
3. No second visibility authority or map rule state introduced.
4. The alpha distinction is internal to the existing client presentation source
   model.

## Validation

Fail-first non-ruins fallback command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" --no-configuration-cache
```

```text
FAILED as expected: forest-edge fallback source bands rendered at alpha=0.42
instead of the expected 0.35..0.37 range.
```

Focused alpha-boundary command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 6s
```

D8 generalization command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 20s
```

Focused owner gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 8s
```

Director recapture command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --no-configuration-cache
```

```text
FAILED by expected-hash mismatch after writing current artifacts. This packet
does not accept a director golden rebaseline.
```

## Next Contract

Do not expand the `0.42` dedicated-source alpha to fallback family plates. The
next map slice should focus on a stronger structural topology source for
non-ruins families or a more explicit risky-path composition grammar that
reduces grid/card rhythm without relying on higher source alpha.

Follow-up completed in:

```text
UI/review/dark-uiux-pr08-exploration/topology-risk-interior-seam-dissolve-2026-06-02/pr08-topology-risk-interior-seam-dissolve-decision.md
```

That packet chose the composition-grammar branch and accepted a long-run
internal shared-seam dissolve layer for topology-risk hybrid rooms.
