# PR08 V39 Room Boundary Compression Decision

## Verdict

Accepted-forward only.

V39 adds a client-side room boundary compression pass after the existing room
silhouette pressure layer. The pass darkens the screen-top and screen-bottom
interior shoulders, side shoulders and compact corner shoulders so the visible
room reads slightly more like a thick stone chamber before grid detail.

It is not director-grade closure. The V39 map/reference resized mean absolute
RGB distance improves only slightly from V38 `15.575196621496431` to
`15.568107226598688`, and the visual board still shows the room shape as too
rectangular and grid-led compared with `UI/UI-demo-new.png`.

## Change Shape

- Owner module: `client`
- Change type: renderer-only structural room-boundary compression
- Stable contracts touched: no gameplay rules, core visibility,
  save/replay/profile/schema/content-pack, manifest key/schema, resource key or
  resource file changes
- Authority: no new manifest/resource authority; this is a private
  `TileRenderer` compositor pass over existing visible floor geometry

## Production

- Added `drawVisibleRoomBoundaryCompression` in `TileRenderer`.
- Invoked it after `drawVisibleRoomSilhouettePressure` and before outer room
  shadows.
- The pass derives its bounds from visible floor tile rects and draws:
  - a continuous dark screen-top interior shoulder
  - a lower warm screen-bottom shelf
  - asymmetric side shoulders
  - compact dark corner shoulders
  - two restrained worn lip highlights
- The pass is gated by visible floor area size and does not alter map data,
  terrain material selection, player state, event output, save/replay data or
  resource manifests.

## Tests

- Added focused render coverage:
  `TileRendererCanvasTest.render canvas compresses visible room boundary with thick interior stone shoulders`.
- RED proved the previous renderer had no continuous room-boundary compression
  shoulder matching the target dimensions and alpha.
- GREEN verifies the top, bottom, side and corner shoulder geometry, including
  that the V39 boundary compression shapes do not cover the player focal center.

## Evidence

| Artifact | Path |
| --- | --- |
| Comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-comparison-board.png` |
| Runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-runtime-metrics.json` |
| V39 map crop | `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-ui-demo-new-map-stage-crop.png` |
| V39 room closeup | `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-v39-room-closeup.png` |
| V39 vs V38 map diff | `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-v39-v38-map-stage-diff.png` |
| V39 vs V38 room diff | `UI/review/dark-uiux-pr08-exploration/runtime-room-boundary-compression-v39/pr08-room-boundary-compression-v39-v39-v38-room-closeup-diff.png` |

## Metrics

- V39 vs V38 map-stage changed ratio:
  `0.020001725030188027`
- V39 vs V38 map-stage mean absolute RGB diff:
  `0.025554884710482433`
- V39 vs V38 full-screen changed ratio:
  `0.009096343348468747`
- V39 vs V38 full-screen mean absolute RGB diff:
  `0.011519725613424925`
- V39 vs V38 room-closeup changed ratio:
  `0.05643462190050824`
- V39 vs V38 room-closeup mean absolute RGB diff:
  `0.07160069818779198`
- V38 resized map/reference mean absolute RGB diff:
  `15.575196621496431`
- V39 resized map/reference mean absolute RGB diff:
  `15.568107226598688`

## Commands Run

- RED and GREEN focused render test:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas compresses visible room boundary with thick interior stone shoulders'`
- Client owner verification:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.assets.ManifestResolveTest :client:clientSmoke maintainabilityLint`
- Focused golden evidence:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- Local Python/PIL evidence generation for V39 crop copies, room closeup, diff
  images, metrics JSON and comparison board.

## Result

- PASS: focused room-boundary compression render test.
- PASS: `TileRendererCanvasTest`, `ManifestResolveTest`,
  `:client:clientSmoke` and `maintainabilityLint`; three smoke subtests were
  skipped by existing environment assumptions.
- EXPECTED FAIL: PR02-1 golden hash gate failed because runtime map/full pixels
  changed and baselines remain intentionally unchanged.
- PASS: V39 runtime archive uses repo-relative paths in generated JSON/TSV.

## Director Notes

V39 is a useful structural correction, but it is still too conservative for
final art direction. The room now has slightly more perimeter mass, yet the
first read is still a rectangular grid room rather than the reference's
authored dungeon space.

Do not continue by only tuning V39 shoulder alpha, width or lip highlight
colors. The next map-stage move should change the room silhouette more
materially: wall/floor edge breakups, non-rectangular aperture pressure,
larger wall mass at corners, or a stronger map-stage composition decision that
changes how the room shape reads before grid detail.
