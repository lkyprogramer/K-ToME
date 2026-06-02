# PR-08 V28 Runtime Room Silhouette Pressure

## Verdict

Accepted-forward only.

V28 builds on V27 by moving from outside-stage aperture work into visible-room
edge composition. It adds a room-scale silhouette pressure pass inside
`MAP_ROOM_COMPOSITOR`, creating stronger top, lower, and right-edge cuts around
the enlarged room so the first read is less like a perfect rectangular grid
block.

This is not director-grade closure. The room is still more rectangular and
grid-forward than `UI/UI-demo-new.png`; V28 proves that compositor edge pressure
can help, but the remaining gap is now larger than another rectangle-bite pass
should solve.

## Scope

- Production:
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/`

No gameplay rules, map generation, core visibility, save, replay, profile,
schema, manifest schema, content-pack rule, resource file, owner-contract or
golden expected-hash change.

## Implementation

`renderWarmMapOverlay(...)` now calls `drawVisibleRoomSilhouettePressure(...)`
after visible-room corner breakup and before outer shadows.

The new pass:

1. draws a broad upper room-edge bite inside the visible room perimeter
2. draws an offset lower room-edge bite
3. draws a stronger right-side vertical pressure block
4. adds small worn-stone lip hints so the dark cuts still read as dungeon
   material
5. uses a viewport-safe player-center fallback when the player tile is not
   currently visible

The pass is presentation-only. It does not create a second map, visibility,
save, replay or content-pack authority.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-runtime-metrics.json` |
| evidence index | `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-evidence-index.tsv` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-ui-demo-new-parity-1672x941.png` |
| map-stage crop | `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-ui-demo-new-map-stage-crop.png` |
| right panel crop | `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-ui-demo-new-right-panel-grid.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-room-silhouette-pressure-v28/pr08-room-silhouette-pressure-v28-ui-demo-new-bottom-deck-no-command-hints.png` |

Key metrics versus V27:

| Surface | Mean abs RGB diff | Max RGB diff | Changed ratio |
| --- | ---: | ---: | ---: |
| map-stage crop | `0.21215379602476378` | `46` | `0.031062522760814983` |
| full screen 1672x941 | `0.09782230337945143` | `44` | `0.014257299065943285` |
| full screen 1280x800 | `0.10809277343750001` | `46` | `0.015826416015625` |
| right panel crop | `0.0` | `0` | `0.0` |
| bottom deck crop | `0.0` | `0` | `0.0` |

Reference-distance check:

- V27 resized map/reference mean abs diff: `17.150591621464486`
- V28 resized map/reference mean abs diff: `17.07226982027741`

Key artifact hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `b921449f983ed99c88c8dee3b3af9a6ce057e75913d4a049600492b7179e0edb` |
| runtime metrics | `94f37a9cff230b2ee3db25bf8b5ab0204584d542ff24db9bf9e967f4575cdc38` |
| full screen | `3192cc3709d6cfa5be57c979c50a2435cac177ecdccd5685b50ca615c4108cfc` |
| map-stage crop | `7ea39b9648aea259d2ca76d6aa92b169cf70b220758e19160a58d310f620fd1f` |
| right panel crop | `8de82d8abf81c34906ba60e5a8dd7545aa2e883b0c6574e100accd720df4c277` |
| bottom deck crop | `1d3ac3e0b3b67dea66a077aa2afa30f9b47b5bb4318e812f2894ea2534f9d02e` |

## Validation

RED, expected failure before production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks enlarged room grid edges with room scale silhouette pressure'
```

Failure evidence: missing broad upper room silhouette bite.

Strengthened RED after the first candidate was visually too weak:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks enlarged room grid edges with room scale silhouette pressure'
```

Failure evidence: the first candidate did not satisfy the stronger room-edge
cut contract.

Focused GREEN after production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.render canvas breaks enlarged room grid edges with room scale silhouette pressure'
```

Result: `BUILD SUCCESSFUL in 4s`.

Broader focused verification:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.screen.FoundationViewportSupportTest' maintainabilityLint
```

Result: `BUILD SUCCESSFUL in 6s`.

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
remain intentionally unchanged. Actual V28 hashes:

```text
ui-demo-new-parity-1672x941=5f7f8794cc2287d4c31aeadbb3d43993b97006fbeab05d534b52c118cac86311
ui-demo-new-parity-1280x800=cc1d0538e054c1eb1513110334ff9b9e4854b8c351eb77cf39217b7d20566c95
ui-demo-new-right-panel-grid=6b85d8516221c04247560fb315391ef5bf898d5bd2408426cf45c15117d357c2
ui-demo-new-bottom-deck-no-command-hints=e1376af7601496ff63112b7d3c5a9cdbe94551d73cd4b6529f5d9d4279220358
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=284b4e97a203b9ea243b7631379f82e5af365ee89bc651f2421edc909dc6143b
ui-demo-new-inventory-page-1=94911bda55a610dba9b8b4db41d4716a7a2428d70a89b8eee13331e987bae864
ui-demo-new-inventory-page-2=015e1b98335aa8a5bf4ae19a18e22a3f157d1446da560654d9e2f198bd914436
```

## Manual Review

V28 is accepted-forward because it creates a visible edge-pressure layer inside
the room, not just around the black stage. The room reads less like a pure
rectangle than V27, and the change remains isolated to the map stage: right
panel, bottom deck, nav and inventory crops are unchanged.

Remaining director blockers:

1. the map still depends too much on rectangular compositor bites
2. floor and wall resource density remains grid-first
3. corridor mouths and wall thickness still do not match the reference crop's
   organic room silhouette
4. another opacity-only or rectangle-bite pass would likely produce diminishing
   returns

## Next Action

Do not continue with room-edge opacity tuning or another rectangular compositor
bite. The next map move should switch technique:

1. generate or author a stronger room-scale floor/wall resource family for
   corridor mouths and broken wall thickness, or
2. change map-stage composition around room placement rather than adding another
   overlay pass, or
3. move to a right-panel/bottom-deck density slice if the map no longer gives a
   high-return improvement without resource work.
