# PR-08 V26 Runtime Map Presentation Scale

## Verdict

Accepted-forward only.

V26 changes the Foundation runtime presentation cell from `32px` to `40px`.
This trades distant tactical overview for larger, more legible map tiles in the
director-grade UI target. The map room now carries more first-screen weight than
V25 and moves closer to the reference crop's central-room authority.

This is not director-grade closure. The map stage still has too much empty black
area, the room silhouette remains rectangular, and wall/aperture depth still
lags behind `UI/UI-demo-new.png`.

## Scope

- Production:
  - `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/screen/FoundationViewportSupportTest.kt`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/`

No gameplay rules, map generation, core visibility, save, replay, profile,
schema, manifest schema, content-pack rule, resource file, owner-contract or
golden expected-hash change.

## Implementation

The runtime `FoundationGameScreen` renderer now passes `cellWidth = 40f` and
`cellHeight = 40f` to `TileRenderer`. `TileMapViewport` continues using the
same snapshot and focus contract; only the number of screen-visible cells
changes because each cell occupies more presentation space.

The golden helper was updated to use the same `40f` layout metrics for PR-02-1
evidence cropping and diagnostics.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-runtime-metrics.json` |
| evidence index | `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-evidence-index.tsv` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-ui-demo-new-parity-1672x941.png` |
| map-stage crop | `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-ui-demo-new-map-stage-crop.png` |
| right panel crop | `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-ui-demo-new-right-panel-grid.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-map-presentation-scale-v26/pr08-map-presentation-scale-v26-ui-demo-new-bottom-deck-no-command-hints.png` |

Key metrics versus V25:

| Surface | Mean abs RGB diff | Max RGB diff | Changed pixels | Changed ratio |
| --- | ---: | ---: | ---: | ---: |
| map-stage crop | `10.215827311700178` | `226` | `1219395 / 2086920` | `0.5843036628140993` |
| full screen | `4.687473146505042` | `226` | `1688699 / 6293408` | `0.26832822534308914` |
| right panel crop | `0.0` | `0` | `0 / 1147200` | `0.0` |
| bottom deck crop | `0.0` | `0` | `0 / 663480` | `0.0` |

Key artifact hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `5ee8ab824d39cbc6742ba13b994c7ec8434966cd832b4bbf11c3da1623e80d41` |
| runtime metrics | `a8c39524e4a00bbbf4abf111f6a672ed813d238d96fe41cb125d1c2f0f8fb60c` |
| full screen | `6779e2cf6ab483533f77b0e79e96e937d18160472241fd704c211f57ca079f99` |
| map-stage crop | `322b36600d01b41d0fce5349edbf919503233991a086880e315f044a25285ed1` |
| right panel crop | `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277` |
| bottom deck crop | `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e` |

## Validation

RED, expected failure before production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell'
```

Failure evidence: runtime source still used 32px presentation cells.

GREEN after production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell'
```

Result: `BUILD SUCCESSFUL in 4s`.

Broader focused verification:

```bash
./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest' --tests 'com.ktome.client.render.GameShellLayoutTest' --tests 'com.ktome.client.render.InfoSurfaceLayoutTest' --tests 'com.ktome.client.render.DemoShellLayoutTest' --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasRepaintsPr08GroundFamilyAsRoomReliefWithoutCoveringActorsOrLootMarkers' maintainabilityLint
```

Result: `BUILD SUCCESSFUL in 5s`.

Runtime smoke:

```bash
./gradlew :client:clientSmoke
```

Result: `BUILD SUCCESSFUL in 15s`. Three render/audio smoke subtests were
skipped by their existing environment assumptions.

Focused golden evidence:

```bash
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
```

Result: expected failure because PR02-1 hashes changed and golden baselines
remain intentionally unchanged. Actual V26 hashes:

```text
ui-demo-new-parity-1672x941=8daf37fa172eb68d0e91709ab75c26e78a757875acc6d22e121556b277cceb2e
ui-demo-new-parity-1280x800=fc35bc89d6bb80e2dca0cd9693b90240652a30538c1259d5cf04eb79f9f88bdb
ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2
ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=47ee6906ca1a6a7c8e750ad72a9ac2453ca9f34db32b72ab5fe02c0274e1964e
ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864
ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436
```

## Manual Review

V26 is a larger and more useful movement than V24/V25 opacity/resource
readability changes. The visible room no longer feels as miniature inside the
map stage, and the first read has more in-world weight. Right panel, bottom
deck, inventory and nav evidence remain unchanged versus V25.

Remaining director blockers:

1. black stage area still dominates the crop
2. room silhouette is still too rectangular and map-grid-forward
3. wall thickness and corridor mouth depth still lack the reference crop's
   carved-room pressure
4. larger tiles make resource fidelity more exposed, so the next visual pass
   should improve aperture/silhouette or resource density rather than only
   changing scale again

## Next Action

Do not keep changing presentation cell size as the next move. Choose one larger
cut that builds on the new scale:

1. aperture-depth pass that reduces empty black stage and frames the enlarged
   room as carved masonry, or
2. generated floor/wall resource pass that materially improves wall thickness,
   corridor mouths and room-scale silhouette at 40px, or
3. map-stage composition pass that redistributes room focus without touching
   gameplay snapshot semantics.
