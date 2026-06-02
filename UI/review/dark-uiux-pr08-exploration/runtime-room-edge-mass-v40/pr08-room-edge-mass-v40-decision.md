# PR08 V40 Asymmetric Room Edge Mass Decision

## Verdict

Accepted-forward only.

V40 adds a stronger client-side room edge mass pass after V39 boundary
compression. The pass adds larger asymmetric masonry blocks along the visible
floor perimeter so the room starts to read less like a clean rectangular grid
and more like a carved stone chamber.

It is a real improvement over V39, but it is not director-grade closure. The
V40 map/reference resized mean absolute RGB distance improves from V39
`15.568107226598688` to `15.524223257240335`, yet the comparison board still
shows a rectangular tactical room with visible grid rhythm rather than the
reference's fully authored dungeon silhouette.

## Change Shape

- Owner module: `client`
- Change type: renderer-only room edge silhouette/composition pass
- Stable contracts touched: no gameplay rules, core visibility,
  save/replay/profile/schema/content-pack, manifest key/schema, resource key or
  resource file changes
- Authority: no new resource, manifest, map, terrain or gameplay authority; the
  pass derives geometry from visible floor tile bounds inside `TileRenderer`

## Production

- Added `drawVisibleRoomAsymmetricEdgeMass` in `TileRenderer`.
- Invoked it after `drawVisibleRoomBoundaryCompression` and before outer room
  shadows.
- The pass draws:
  - a broad upper-left masonry mass
  - an offset lower-right plinth
  - a thicker left side buttress
  - an asymmetric right side buttress
  - restrained worn-stone lip highlights for material identity
- The pass is size-gated and does not alter map data, cell materials,
  manifests, resource files, actors, loot, targeting affordances or persistence.

## Tests

- Added focused render coverage:
  `TileRendererCanvasTest.render canvas offsets visible room edges with asymmetric masonry mass`.
- RED proved the previous renderer had no broad offset masonry mass on the
  upper-left room edge.
- GREEN verifies the four V40 mass regions and asserts that the new edge mass
  shapes do not cover the player focal center.

## Evidence

| Artifact | Path |
| --- | --- |
| Comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-comparison-board.png` |
| Runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-runtime-metrics.json` |
| V40 map crop | `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-ui-demo-new-map-stage-crop.png` |
| V40 room closeup | `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-v40-room-closeup.png` |
| V40 vs V39 map diff | `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-v40-v39-map-stage-diff.png` |
| V40 vs V39 room diff | `UI/review/dark-uiux-pr08-exploration/runtime-room-edge-mass-v40/pr08-room-edge-mass-v40-v40-v39-room-closeup-diff.png` |

## Metrics

- V40 vs V39 map-stage changed ratio:
  `0.01898826979472141`
- V40 vs V39 map-stage mean absolute RGB diff:
  `0.05474910394265233`
- V40 vs V39 full-screen changed ratio:
  `0.008689886306433652`
- V40 vs V39 full-screen mean absolute RGB diff:
  `0.02508418332324871`
- V40 vs V39 room-closeup changed ratio:
  `0.023429847528107194`
- V40 vs V39 room-closeup mean absolute RGB diff:
  `0.05266440782381026`
- V39 resized map/reference mean absolute RGB diff:
  `15.568107226598688`
- V40 resized map/reference mean absolute RGB diff:
  `15.524223257240335`

## Commands Run

- RED and GREEN focused render test:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas offsets visible room edges with asymmetric masonry mass'`
- Client owner verification:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke maintainabilityLint`
- Focused golden evidence:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- Local Python/PIL evidence generation for V40 crop copies, room closeup, diff
  images, metrics JSON and comparison board.

## Result

- PASS: focused asymmetric room edge mass render test.
- PASS: `TileRendererCanvasTest`, `ManifestResolveTest`,
  `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
  skipped by existing environment assumptions.
- PASS: PR02-1 golden hash gate after rebaselining
  `GoldenScreenshotHarnessTest` expected hashes to the accepted V40 artifacts.
- PASS: V40 runtime archive uses repo-relative paths in generated JSON/TSV.

## Director Notes

V40 is stronger than V39 and should be kept as accepted-forward. It makes the
room perimeter feel more like uneven stone mass and moves the reference metric
in the right direction.

It still does not close the goal. The remaining gap is now less about adding
more dark rectangles to the same room and more about the larger map-stage
composition: the map still shows a compact rectangular tactical room centered
inside a large framed stage. The next move should test a larger composition
decision, such as increasing room/world scale within the stage, changing the
visible-room aperture proportion, or introducing authored wall/floor resource
forms that reshape the room silhouette before compositor overlays.
