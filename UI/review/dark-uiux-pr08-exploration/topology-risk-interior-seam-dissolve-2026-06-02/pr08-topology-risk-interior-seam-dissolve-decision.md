# PR08 Topology-Risk Interior Seam Dissolve Decision

Date: 2026-06-02

## Verdict

`pr08-topology-risk-interior-seam-dissolve-v1-accepted-forward`

The topology-risk hybrid path now adds a structural internal seam dissolve
field for long visible shared seams. This is accepted-forward as risky-path
composition grammar: it reduces the tile/card-grid read inside L-like and
low-fill visible topology while preserving hidden notches, boundary wall
components, actor/loot ordering and existing topology-source authority.

This is not director-grade closure. It does not replace the dedicated topology
source, add new resources, update director hashes or claim all-map procedural
Direction A.

## Runtime Change

Changed production renderer:

```text
client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt
```

`RoomArtPlateRenderer.renderTopologyRiskHybrid` now calls
`drawTopologyRiskInteriorSeamDissolveFields` after source/material runs and
before AO, local light, boundary marks and wall components.

The new layer:

1. scans only visible points in topology-risk hybrid rooms;
2. draws horizontal dissolve fields only where row `y` and row `y + 1` are both
   visible for a run of at least `4` contiguous tiles;
3. draws vertical dissolve fields only where column `x` and column `x + 1` are
   both visible for a run of at least `4` contiguous tiles;
4. uses viewport-aware coordinates for vertical runs so screen-space y
   inversion does not shift fields off the visible topology;
5. does not cover hidden L-shaped notches or substitute for boundary wall
   components.

Focused test lock:

```text
client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt
```

The L-shaped topology-risk test now requires both horizontal and vertical
multi-cell seam dissolve fields and asserts they do not cover the hidden notch.

## Evidence

D8 generalization evidence root:

```text
UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/
```

Current D8 board:

```text
UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png
```

Current D8 board hash:

```text
695a213343cda62f5e0d872f2dbd21d912903c3e897f57ebe5a7e3f9e96fdeea
```

Current D8 evidence index:

```text
UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv
```

Current D8 evidence-index hash:

```text
5379445a0399bc7817eab47b81c63edb4b1cc480fb904971e102a34e7120aaea
```

## Validation

Fail-first focused command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
FAILED as expected: the existing topology-risk hybrid path did not draw the
required internal horizontal seam dissolve field.
```

Intermediate correction:

```text
After the first implementation, the horizontal seam assertion passed but the
vertical seam assertion failed. The production code used the smallest topology
y as the screen-space start; `TileMapViewport.tileRect` inverts map y, so the
vertical field was shifted. The fix uses the run's maximum y tile as the
screen-space start.
```

Focused pass:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 6s
```

Focused owner gate:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test \
  --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" \
  --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsFallbackTopologySourceBandsSubduedForNonRuinsRoomArtFamilies" \
  --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" \
  --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" \
  --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" \
  manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 8s
```

D8 generalization command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 21s
```

## Contract

1. No new visual key, source image, manifest schema or resource owner schema.
2. No core/game/save/replay/profile/localization/input/content-pack contract
   changed.
3. No second visibility authority or room topology authority introduced.
4. The layer is confined to client-only `TOPOLOGY_RISK_HYBRID_PRESENTATION`.
5. This is accepted-forward only; do not rebaseline director goldens from this
   packet.

## Next Contract

Continue risky-path composition work only if it changes the room-level
structure. Do not continue with small alpha, fog, one-off line-weight or
single-cell rectangle tuning. The next high-value cut should be a broader
topology-risk wall/floor composition grammar or dedicated topology-source
coverage for non-ruins families.
