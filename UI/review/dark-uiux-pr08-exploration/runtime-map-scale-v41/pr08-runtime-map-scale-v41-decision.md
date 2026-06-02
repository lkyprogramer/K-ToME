# PR08 V41 Runtime Map Scale Decision

## Verdict

Accepted-forward only.

V41 changes the Foundation runtime map from a distant overview board into a
larger first-read tactical surface by increasing the runtime tile scale to
`42f`. This is a composition/aperture pass, not a gameplay, schema, content,
manifest, resource-key or persistence change.

The accepted candidate is `42f`. A `44f` intermediate probe was rejected after
visual review because it pushed the room too close to the frame and reduced the
stage breathing room; it was not kept as accepted evidence.

## Scope

- Owner module: `client`
- Production:
  - `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
- Golden/test support:
  - `client/src/test/kotlin/com/ktome/client/screen/FoundationViewportSupportTest.kt`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
- Stable contracts touched:
  - No `core` or `game` rule change
  - No save/replay/profile/schema/content-pack change
  - No manifest key/schema/resource file change
  - No second rendering authority or alternate map state

## Design Reasoning

V40 improved perimeter mass, but the first read still looked like a compact
rectangular tactical room sitting inside a large framed stage. V41 changes the
layout proportion before tuning texture detail again.

The final `42f` scale keeps the room legible and materially larger while
preserving enough dark stage around the map. The stage crop moves closer to
the reference in measured RGB distance:

- V40 resized map/reference mean absolute RGB diff: `15.501569326917759`
- V41 resized map/reference mean absolute RGB diff: `15.069590488634477`
- V41 vs V40 map-stage changed ratio: `0.5414409752170662`
- V41 vs V40 map-stage mean absolute RGB diff: `5.72072991138456`
- V41 vs V40 full-screen changed ratio: `0.24834700054406134`
- V41 vs V40 room-closeup changed ratio: `0.9548390574464808`

## Evidence

- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-runtime-metrics.json`
- Evidence index:
  `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-evidence-index.tsv`
- Map-stage crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-ui-demo-new-map-stage-crop.png`
- Room closeup:
  `UI/review/dark-uiux-pr08-exploration/runtime-map-scale-v41/pr08-runtime-map-scale-v41-v41-room-closeup.png`

## PR02-1 Golden Hashes

- `ui-demo-new-parity-1672x941=22a380bfbe2aefeec40a969e50c7d15fcf54549065e2460b0d36ff3efe35f97a`
- `ui-demo-new-parity-1280x800=03df3a3869707f74bfaf0f91c3874d8c67095e97e164a9816e8e981fd0db6c1e`
- `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
- `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
- `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
- `ui-demo-new-map-stage-crop=39155e93b614bd11c08dcd2a056fa400683d7b7bf3d90f188d01ac73f119132f`
- `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
- `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`

## Validation

- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.screen.FoundationViewportSupportTest.usesDirectorGradePresentationCellScaleForFoundationRuntimeShell'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.screen.FoundationViewportSupportTest --tests com.ktome.client.render.TileRendererCanvasTest :client:clientSmoke maintainabilityLint`

`clientSmoke` reported three existing skipped subtests under its current
environment assumptions; the Gradle build itself completed successfully.

## Director Notes

V41 is a meaningful first-read improvement and should remain. It is still not
director-grade closure: the map now has better scale, but the room silhouette
is still rectangular and grid-led compared with `UI/UI-demo-new.png`.

Do not continue by only increasing tile scale again. The next pass should
target one of:

- stronger authored wall/floor resource families
- non-rectangular aperture pressure
- room silhouette breakups that alter the first read before grid detail
- bottom-deck composition only if the next full-screen review says the map is
  no longer the dominant blocker
