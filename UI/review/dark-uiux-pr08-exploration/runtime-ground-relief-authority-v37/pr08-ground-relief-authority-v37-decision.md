# PR-08 V37 Ground Relief Authority

## Verdict

Accepted-forward only.

V37 removes the PR-08 ground-family per-tile relief repaint from
`MAP_ROOM_COMPOSITOR`. The ground family remains the base terrain authority; the
room compositor should no longer stamp the same floor resource once per visible
floor tile after room-scale material, atmosphere and hidden-stage passes.

This is not director-grade closure. It is an authority cleanup and a map-stage
noise reduction step. The room still reads grid-first relative to
`UI/UI-demo-new.png`.

## Scope

- owner module: `client`
- production file:
  `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- test file:
  `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- evidence root:
  `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/`
- stable contracts touched: no gameplay rules, core visibility,
  save/replay/profile/schema/content-pack, manifest key/schema, resource file or
  golden expected-hash changes

## Change

- Removed `drawPr08GroundFamilyReliefRepaint` from `renderWarmMapOverlay`.
- Removed the now-unused `isPr08RuinsGroundFamily` predicate.
- Replaced the old test expectation that required a `0.34` alpha per-tile
  ground repaint with an assertion that the room compositor keeps PR-08 ground
  resources out of repeated relief restamps.
- Kept PR-08 wall-family relief repaint unchanged.
- Kept actor, loot marker, telegraph and cursor layering unchanged.

## Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-comparison-board.png` |
| V37 map crop | `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-ui-demo-new-map-stage-crop.png` |
| V37 full screen | `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-ui-demo-new-parity-1672x941.png` |
| V37/V36 map diff | `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-v37-v36-map-stage-diff.png` |
| V37 room closeup | `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-v37-room-closeup.png` |
| metrics | `UI/review/dark-uiux-pr08-exploration/runtime-ground-relief-authority-v37/pr08-ground-relief-authority-v37-runtime-metrics.json` |

## Metrics

- PR-08 ground-family compositor restamps:
  - V36 RED evidence: `60`
  - V37 GREEN evidence: `0`
- V37 vs V36 map-stage crop:
  - `changedRatio=0.24177112682805282`
  - `meanAbsRgbDiff=0.5827528926200652`
  - `maxAbsRgbDiff=[31,25,20]`
- V37 vs V36 full-screen crop:
  - `changedRatio=0.11094481718013514`
  - `meanAbsRgbDiff=0.2676077360099117`
- map/reference resized mean absolute RGB diff:
  - V36: `15.910307374823503`
  - V37: `15.49837559657294`
- sampled room dark-line contrast:
  - V36 mean: `4.290575236343592`
  - V37 mean: `4.702899353810177`
  - interpretation: V37 reduces repeated ground texture restamp noise, but the
    remaining floor/wall lattice still reads too strongly.

## Golden Actual Hashes

- `ui-demo-new-parity-1672x941=fc0c4eb700c1d46ce130d9204c3a23acc3e1d5c3b865e851f93c4353a5176ce7`
- `ui-demo-new-parity-1280x800=5cbc2d4fd0531eb979698eefcdc269a10033f7a22b629956965a578396f74645`
- `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
- `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
- `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
- `ui-demo-new-map-stage-crop=6e8a067678d8b31f158708dab61ab35c57d0ba7989775dea932f74b6cf22dcd3`
- `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
- `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`

## Validation

Commands were run with SDKMAN environment initialized.

- RED, expected failure on repeated ground-family restamps:
  `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps'`
  - result: failed as expected with `repeatedRestamps=60, base=60`
- GREEN focused V37 test:
  `./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.renderCanvasKeepsPr08GroundFamilyOutOfRoomReliefRestamps'`
  - result: `BUILD SUCCESSFUL`
- broader focused client verification, smoke and anti-bloat lint:
  `./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
  - result: `BUILD SUCCESSFUL`
  - note: three render/audio smoke subtests were skipped by existing
    environment assumptions
- focused golden evidence:
  `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
  - result: expected failure because map/full-screen pixels changed and
    baselines remain intentionally unchanged

## Director Review

V37 is directionally useful because it removes a second floor-authority pass
that was reintroducing repeated tile texture over room-scale compositor work.
It also moves the map crop closer to the retained reference-distance metric.

It is not enough because the remaining floor/wall construction still exposes a
regular square lattice as the first room read. Do not continue by only toggling
ground repaint alpha or adding another per-grid mask. The next map pass should
use a stronger non-grid-aligned room material resource, a wall/floor structural
resource decision, or a larger map-stage composition move that changes room
silhouette and wall thickness rather than only repaint order.
