# PR08 V42 Runtime Aperture Corner Shelves Decision

## Verdict

Accepted-forward only.

V42 keeps V41's `42f` runtime scale and adds restrained runtime-only aperture
corner shelves around the enlarged visible room. The goal is to reduce the
remaining clean rectangular read without changing gameplay, map data,
resources, manifests or persistence.

## Scope

- Owner module: `client`
- Production:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- Test/golden support:
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
  - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`
- Stable contracts touched:
  - No `core` or `game` rule change
  - No save/replay/profile/schema/content-pack change
  - No manifest key/schema/resource file change
  - No resource generation or asset authority change

## Design Reasoning

V41 fixed the map scale, but the enlarged room still retained a rectangular,
grid-led silhouette. V42 adds two related renderer details:

- hidden-stage diagonal aperture shelves where enough hidden space surrounds
  the visible room
- visible-room interior corner shelves gated to runtime scale `>= 40f`, so the
  canonical V41/V42 first screen changes even when the room reaches the stage
  aperture

The change is intentionally small. It improves the measured map/reference
distance without reopening broad map composition:

- V42 vs V41 map-stage changed ratio: `0.01817463055603473`
- V42 vs V41 map-stage mean absolute RGB diff: `0.06548853493825006`
- V42 vs V41 full-screen changed ratio: `0.008391478829912187`
- V41 resized map/reference mean absolute RGB diff: `14.975958190369429`
- V42 resized map/reference mean absolute RGB diff: `14.942269628597481`

## Evidence

- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-runtime-metrics.json`
- Evidence index:
  `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-evidence-index.tsv`
- Map-stage crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-ui-demo-new-map-stage-crop.png`
- Room closeup:
  `UI/review/dark-uiux-pr08-exploration/runtime-aperture-corner-shelves-v42/pr08-aperture-corner-shelves-v42-v42-room-closeup.png`

## PR02-1 Golden Hashes

- `ui-demo-new-parity-1672x941=f4f5f29fdb78f6c65208b58c60a26654b419d8316f72f287a9dbd46bc46f725f`
- `ui-demo-new-parity-1280x800=d6e4f64eef1075f7cf1ce8b78a2955608a7a8921c94f8530250e51427ecd064f`
- `ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8`
- `ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd`
- `ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6`
- `ui-demo-new-map-stage-crop=03d2f81a14c71c628f6c0b64e163674dd65fe4766b098af2366f4560746eed84`
- `ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd`
- `ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a`

## Validation

- RED then PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas gives director scaled runtime room diagonal aperture corner shelves'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`

`clientSmoke` reported three existing skipped subtests under its current
environment assumptions; the Gradle build itself completed successfully.

## Director Notes

V42 is worth keeping, but it is not a closure candidate. It is a small
silhouette refinement after V41's larger scale move. The first screen still
needs a more authored wall/floor resource family or a stronger room-shape
composition pass to approach `UI/UI-demo-new.png`.

Do not continue by stacking more small dark shelves. The next meaningful move
should be resource-family or floor/wall silhouette work that changes the
material read before grid detail.
