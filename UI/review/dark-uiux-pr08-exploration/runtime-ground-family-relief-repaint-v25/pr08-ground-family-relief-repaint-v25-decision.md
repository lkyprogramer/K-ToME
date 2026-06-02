# PR-08 V25 Runtime Ground-Family Relief Repaint

## Verdict

Accepted-forward only.

V25 repaints the existing PR-08 `tileset.ruins.ground_01` family once inside
`MAP_ROOM_COMPOSITOR`, after the global map atmosphere and before actors,
loot markers and telegraphs. It makes the authored floor-resource rhythm more
visible than V24, but it still does not close the director-grade target: the
map-stage first read remains too small, grid-forward and surrounded by too much
empty black stage compared with `UI/UI-demo-new.png`.

Do not continue by tuning only the new ground relief opacity. The next map pass
needs a stronger map-stage composition, viewport scale or aperture-depth change,
or a resource-generation pass that materially changes room scale and silhouette.

## Scope

- Production:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/`

No gameplay, save, replay, profile, schema, manifest schema, content-pack rule,
resource file, owner-contract or golden expected-hash change.

## Implementation

`drawPr08GroundFamilyReliefRepaint` filters the already-built
`frame.layerPlan.terrainBase` placements:

1. visible only: `placement.alpha >= 0.99f`
2. runtime category: `tile_ground`
3. resolved key belongs to `tileset.ruins.ground_01` or its variants

It then calls the normal `drawPlacement` path with `alphaScale = 0.34f`. The
new focused canvas test verifies that the relief pass reuses the exact terrain
placement coordinates, dimensions and flip flags, and that it stays below actor
and ground-loot marker layers.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-runtime-metrics.json` |
| evidence index | `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-evidence-index.tsv` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-ui-demo-new-parity-1672x941.png` |
| map-stage crop | `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-ui-demo-new-map-stage-crop.png` |
| right panel crop | `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-ui-demo-new-right-panel-grid.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-ground-family-relief-repaint-v25/pr08-ground-family-relief-repaint-v25-ui-demo-new-bottom-deck-no-command-hints.png` |

Key metrics versus V24:

| Surface | Mean abs RGB diff | Max RGB diff | Changed pixels | Changed ratio |
| --- | ---: | ---: | ---: | ---: |
| map-stage crop | `0.3052632587736952` | `21` | `317529 / 2086920` | `0.1521519751595653` |
| full screen | `0.14275995878015008` | `24` | `438030 / 6293408` | `0.06960139879696342` |

Key artifact hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `bf3ed4d0a43bf701c7574b92c67ee81f026da9263b488334fb1af240ce8839bf` |
| runtime metrics | `c417c3ce2f4379e537c54590115f3d9fa37540167b0e52ba433827f293f7a525` |
| full screen | `251ef0f6005ede9e4fceeafe37bce9425bf996577e6b6579650c1dfd782f6dac` |
| map-stage crop | `c552622c4aa2a121608ec44699a47ba47d21d3a645434f662dc971f15b6e9378` |
| right panel crop | `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277` |
| bottom deck crop | `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e` |

## Validation

RED, expected failure before renderer change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers'
```

Failure evidence: `relief=0, base=60`.

GREEN after renderer change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers'
```

Result: `BUILD SUCCESSFUL in 5s`.

Broader focused verification:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.TileRenderModelTerrainVariantTest' maintainabilityLint
```

Result: `BUILD SUCCESSFUL in 5s`.

Focused golden evidence:

```bash
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

Result: expected failure because PR02-1 hashes changed and golden baselines
remain intentionally unchanged. Actual V25 hashes:

```text
ui-demo-new-parity-1672x941=461a28e25c012c76596b88809344aabcf8af16df64db1bdd7ccefc3c9fd1ab2a
ui-demo-new-parity-1280x800=73eb4cc7205a468bea78c63e1231396b6695ba60a32983029ebd7ba3017c2777
ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2
ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=c12b30c31bfe03a5bde2166fdcbe968b92f23ce51482efbc2a1e6dc73842d46a
ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864
ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436
```

## Manual Review

The V25 board is visibly positive versus V24: the room floor keeps more of the
authored PR-08 stone family after atmosphere, and actor, loot marker, telegraph,
right panel and bottom deck readability are not covered.

Remaining director blockers:

1. visible room still reads too small inside the stage
2. empty black map-stage area still dominates the crop
3. floor still reads as repeated tile grid before it reads as hand-authored room
4. wall thickness, non-rectangular silhouette and dark aperture pressure remain
   weaker than the reference

## Next Action

Do not tune `alphaScale = 0.34f` as the next move. Choose one larger cut:

1. map-stage composition or viewport-scale pass that increases visible-room
   authority without changing gameplay visibility, or
2. aperture-depth pass that gives the black stage a stronger carved-room read,
   or
3. generated floor/wall resource pass only if it materially changes silhouette,
   room-scale density or floor/wall family structure.
