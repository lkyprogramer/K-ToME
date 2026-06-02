# PR-08 V24 Runtime Wall-Family Relief Repaint

## Verdict

Accepted-forward only.

V24 improves the visible-room wall read by repainting the existing PR-08
`tileset.ruins.wall_01` family once inside `MAP_ROOM_COMPOSITOR`, after the
room atmosphere and wall mass passes. It is not director-grade closure: the
map-stage first read still trails `UI/UI-demo-new.png` in room scale, floor
stone rhythm, wall thickness and dark aperture richness.

Do not continue with wall relief opacity tuning. The next map pass must be
larger than a wall repaint parameter tweak.

## Scope

- Production:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/`

No gameplay, save, replay, profile, schema, manifest schema, content-pack rule,
resource file, owner-contract or golden expected-hash change.

## Implementation

`drawPr08WallFamilyReliefRepaint` filters the already-built
`frame.layerPlan.terrainBase` placements:

1. visible only: `placement.alpha >= 0.99f`
2. runtime category: `tile_wall`
3. resolved key belongs to `tileset.ruins.wall_01` or its piece keys

It then calls the normal `drawPlacement` path with `alphaScale = 0.46f`. The
new test verifies that the relief pass reuses the exact terrain placement
coordinates, dimensions and flip flags, so it does not become a second wall
selection authority.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-runtime-metrics.json` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-ui-demo-new-parity-1672x941.png` |
| map-stage crop | `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-ui-demo-new-map-stage-crop.png` |
| right panel crop | `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-ui-demo-new-right-panel-grid.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-wall-family-relief-repaint-v24/pr08-wall-family-relief-repaint-v24-ui-demo-new-bottom-deck-no-command-hints.png` |

Key metrics versus V23:

| Surface | Mean abs RGB diff | Max RGB diff | Changed pixels | Changed ratio |
| --- | ---: | ---: | ---: | ---: |
| map-stage crop | `0.17965838013276345` | `41` | `122365 / 2086920` | `0.058634255266133826` |
| full screen | `0.0832129428125429` | `49` | `170846 / 6293408` | `0.02714681774961992` |

Key artifact hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `d8ccc91a1550881553304f0812724525c7d370bd589f3e9d8681c46a71ce8bb7` |
| runtime metrics | `fa37e6961c525041ba3404439ce017a80865cc56c2d38cecf4f772113c082119` |
| full screen | `bb213db5032848374f7a8375224751c3a83d7d91b07f474583a5f5500d0574b0` |
| map-stage crop | `44039ee8642d34e0c12c3027e905dc5f9b76e6cfed24e5626786d8bd7cfdd8fc` |
| right panel crop | `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277` |
| bottom deck crop | `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e` |

## Validation

RED, expected failure before renderer change:

```bash
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere
```

Failure evidence: `relief=0, base=36`.

GREEN after renderer change:

```bash
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08WallFamilyAsRoomReliefAfterAtmosphere
```

Result: `BUILD SUCCESSFUL in 3s`.

Broader focused verification:

```bash
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest maintainabilityLint
```

Result: `BUILD SUCCESSFUL in 5s`.

Focused golden evidence:

```bash
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

Result: expected failure because PR02-1 hashes changed and golden baselines
remain intentionally unchanged. Actual V24 hashes:

```text
ui-demo-new-parity-1672x941=db47301298606fea0eefd8457881614bfa23b02520f425923c3645a603b6f787
ui-demo-new-parity-1280x800=1a8da7ea024378fb2e8da0a4c474f517a989026b07bb3f77a7497571e8876d31
ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2
ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=59438665b9b63ae0035d5dc29c6a219970907f5423def0c9539b4b5a3accb858
ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864
ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436
```

## Manual Review

V24 is visibly positive but narrow. The top and bottom wall runs keep more
authored masonry after atmosphere, and side walls are less washed out.

Remaining director blockers:

1. visible room still reads grid-first compared with the reference crop
2. map stage still has too much empty black area relative to the authored room
3. floor stone rhythm is still softer and less hand-authored than the reference
4. wall thickness and dark aperture pressure still need a stronger structural pass

## Next Action

Do not tune `alphaScale = 0.46f` as the next move. Choose one larger cut:

1. resource-backed floor-family visibility pass with explicit marker/actor
   readability proof, or
2. map-stage composition change that increases visible room authority and dark
   aperture richness without changing gameplay visibility, or
3. right/bottom icon-detail pass if the next slice deliberately leaves map work.
