# PR08 Topology Source Alpha Decision

Date: 2026-06-02

## Verdict

`pr08-topology-risk-source-alpha-v1-accepted-forward`

The remaining Source D D8 gap is compositor/source-alpha driven for this slice,
not another immediate source-art regeneration.

This pass raises the dedicated topology-source crop-band alpha from `0.36` to
`0.42` inside the `RoomCompositorStrategy.ART_PLATE_PRESENTATION`
topology-risk path. The visual key, runtime resource, manifest schema, source
region contract, gameplay overlays and marker layer order stay unchanged.

## Runtime Change

Changed production draw point:

```text
client/src/main/kotlin/com/ktome/client/render/RoomArtPlatePresentation.kt
```

Focused test lock:

```text
client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt
```

The fail-first test first required source bands to render in the new
`0.41..0.43` range while production still rendered `0.36`, proving the
assertion was live. Production was then changed to `0.42`.

## Evidence

Alpha D8 board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-alpha-d8-board.png
```

Alpha D8 board hash:

```text
252b492f232fda07d310c639a8a9e519cf305f5cfddd82d393459df5d6c010f4
```

Alpha D8 evidence index hash:

```text
d067dbe2f43f53709c11f3027759f1abd64061f2d101dbd3df7e839280263029
```

Source D versus alpha D8 board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-d-vs-alpha-d8-board.png
```

Source D versus alpha D8 board hash:

```text
9559355a260dddb6b62f8690c5829e69df2bf6d19bb0b975c500f37e8944b741
```

Source D versus alpha D8 diff:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-d-vs-alpha-d8-diff.png
```

Source D versus alpha D8 diff hash:

```text
5b331d9325354c5c3a880cf529b0d93615b05733105d83d85bf29880830421ae
```

Pixel comparison metric:

```text
MAE 16.0333 (0.000244653)
```

Director read:

1. The D8 topology-risk rows gain a small but visible material-readability
   lift in dark source bands.
2. The change remains below the actor, loot, telegraph, cursor and selection
   layers; it does not turn source material into gameplay authority.
3. The pass is accepted-forward because it improves risky-row first read
   without a new resource key, schema, source image or renderer branch.

## Contract

1. Same topology source key:
   `ui.map_stage.ruins.room_topology_source.pr08_demo`.
2. Same runtime source file:
   `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`.
3. No core, game, save, replay, profile, localization, input or content-pack
   contract changed.
4. No manifest schema, resource owner schema or production asset path changed.
5. No second visibility authority or rule state was introduced.

## Validation

Fail-first focused renderer command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
FAILED as expected: topology source bands still rendered at alpha=0.36 and no
draws matched the new 0.41..0.43 assertion.
```

Focused renderer rerun after production alpha change:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
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

Focused manifest/renderer/lint command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 7s
```

## Next Contract

Do not claim procedural-room or all-map closure from this alpha pass. The next
loop should use the alpha D8 board and current director runtime evidence to
decide whether topology-risk rows now need a stronger structural source asset,
a more explicit risky-path composition grammar, or a packaged/director
recapture before family expansion.

Follow-up resolved in:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-alpha-scope-2026-06-02/pr08-topology-source-alpha-scope-decision.md
```

The stronger `0.42` alpha is now explicitly scoped to formal dedicated topology
sources. Fallback family room-plate crops remain at `0.36` until those families
have accepted dedicated topology-source assets.
