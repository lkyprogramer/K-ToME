# PR-08 V27 Runtime Aperture Depth

## Verdict

Accepted-forward only.

V27 builds on V26's 40px runtime map scale by adding a stage-scale aperture
depth pass after hidden fog, plus shorter focal worn-masonry lip segments around
the visible room. The result gives the enlarged map a clearer carved-dungeon
frame instead of leaving the room floating in a flat black field.

This is not director-grade closure. The room silhouette is still too
rectangular and grid-forward versus `UI/UI-demo-new.png`; V27 is a bounded
aperture-depth improvement, not the final map-art pass.

## Scope

- Production:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/`

No gameplay rules, map generation, core visibility, save, replay, profile,
schema, manifest schema, content-pack rule, resource file, owner-contract or
golden expected-hash change.

## Implementation

`drawHiddenStageApertureMasonry(...)` now delegates a final
`drawDirectorScaleApertureDepth(...)` pass. The pass:

1. draws heavy upper/lower aperture shelves outside `visibleClip`
2. draws left/right dark pylons outside `visibleClip`
3. adds short worn-masonry lip highlights around the focal room area
4. falls back to `visibleClip` center when the player tile is outside the
   current viewport, which is valid in validation overlay smoke

The pass remains presentation-only. It does not create a second visibility,
mapgen, save, replay or content-pack authority.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-runtime-metrics.json` |
| evidence index | `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-evidence-index.tsv` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-ui-demo-new-parity-1672x941.png` |
| map-stage crop | `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-ui-demo-new-map-stage-crop.png` |
| right panel crop | `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-ui-demo-new-right-panel-grid.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-aperture-depth-v27/pr08-aperture-depth-v27-ui-demo-new-bottom-deck-no-command-hints.png` |

Key metrics versus V26:

| Surface | Mean abs RGB diff | Max RGB diff | Changed ratio |
| --- | ---: | ---: | ---: |
| map-stage crop | `0.11129463515611522` | `27` | `0.042738102083453126` |
| full screen 1672x941 | `0.050649240178506354` | `27` | `0.019462904677402134` |
| full screen 1280x800 | `0.056704833984375` | `27` | `0.021775146484375` |
| right panel crop | `0.0` | `0` | `0.0` |
| bottom deck crop | `0.0` | `0` | `0.0` |

Key artifact hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `dda23903bddb5ef41592a25c3e63e433296f41116dd0d408d9147416a401687a` |
| runtime metrics | `84733d083bef96c23dc7679e1b43a247579f0e01722ee3bda795efc32280e97f` |
| full screen | `bd6f98619179542c390179e0fe7deb3ea29c0af55510d3fd7ed7fa7ec63225b6` |
| map-stage crop | `9f2322d33cdc4ea6c0449f2e23ae2a16d1fe9aa8b0b1db5e0d66dc5f1e89b7be` |
| right panel crop | `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277` |
| bottom deck crop | `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e` |

## Validation

RED, expected failure before production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas frames enlarged room with stage scale aperture depth'
```

Failure evidence: missing heavy upper aperture lintel.

Second RED, expected failure before readable masonry lips:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas outlines enlarged room aperture with worn masonry lips'
```

Failure evidence: missing readable upper worn-stone catchlight.

Focused GREEN after production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas frames enlarged room with stage scale aperture depth' --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas outlines enlarged room aperture with worn masonry lips'
```

Result: `BUILD SUCCESSFUL in 4s`.

Broader focused verification:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' maintainabilityLint
```

Result: `BUILD SUCCESSFUL in 5s`.

Runtime smoke:

```bash
./gradlew :client:clientSmoke
```

First run caught a real V27 bug: validation overlay can render with
`playerTile` outside the current viewport. Final result after the fallback fix:
`BUILD SUCCESSFUL in 18s`. Three render/audio smoke subtests were skipped by
their existing environment assumptions.

Focused golden evidence:

```bash
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

Result: expected failure because PR02-1 hashes changed and golden baselines
remain intentionally unchanged. Actual V27 hashes:

```text
ui-demo-new-parity-1672x941=a1d8870792891d3ff66895284efb23287d930721769f865d90b40dfcb98ae8c5
ui-demo-new-parity-1280x800=9836b2728b1e87ab35b0d1b42137037cfb031ffde6e30ca66584b6ab8c628abc
ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2
ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=e6dcae8b77f337daad88b6238d25cd75f6e86693193f72b4cfa9f9a567f4bbcc
ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864
ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436
```

## Manual Review

V27 is accepted-forward because it moves the enlarged V26 map away from a flat
black-stage read. The visible aperture now has top and side material cues, and
the right panel, bottom deck, nav and inventory crops remain isolated.

Remaining director blockers:

1. the visible room is still too rectangular compared with the reference
2. floor/wall density still reads more grid-first than room-first
3. the black stage is better framed but still visually dominant
4. the next map pass needs a larger room-silhouette or resource-backed change,
   not another aperture alpha or lip-opacity tweak

## Next Action

Do not continue with hidden-stage alpha-only or lip-opacity tuning. The next
map move should be one larger structural cut:

1. generated floor/wall resource density for room-scale silhouette and corridor
   mouths, or
2. room-shape compositor work that breaks rectangular room edges more
   decisively without touching gameplay visibility, or
3. a map-stage composition pass that changes how the room occupies the black
   stage.
