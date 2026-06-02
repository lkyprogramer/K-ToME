# PR-08 Room Material Authority V12

## Goal

Make the V11 room-scale material-breakup resource visible enough to challenge
the repeated square lattice without adding a new resource key, schema, manifest
format, owner row or renderer-side second authority.

## Change

Accepted production wiring:

1. Kept `tileset.ruins.room_breakup_01` as the only room material asset.
2. Moved the room-breakup draw later inside `MAP_ROOM_COMPOSITOR`, after room
   contact/story layers and before torch/player light.
3. Raised the room-breakup asset alpha from `0.48` to `0.64`.

No resource, manifest, save/replay/profile, `core`, `game`, content-pack or
golden baseline change was introduced.

## Evidence

Artifacts:

| Artifact | Path |
| --- | --- |
| Runtime archive | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/runtime-v12-material-authority/` |
| Runtime comparison board | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/pr08-room-material-authority-v12-runtime-comparison-board.png` |
| Runtime metrics | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v12/pr08-room-material-authority-v12-runtime-metrics.json` |

Hashes:

| Item | SHA-256 |
| --- | --- |
| V12 runtime map crop artifact | `9f4b758c0456a01610e8b2c34dad376b99fc6e4727469c52bdb536ecfdd6e7c5` |
| V12 runtime comparison board | `46b5c2fef9eb48d463612cb47e789ab0e753876a952b43c99b829ec97697ebc1` |

Golden content hash from the retained expected failure:

`ui-demo-new-map-stage-crop=d946119b95206f3ad3f371b9a086f22f0bea03d96aefef32f96e04054542a394`

## Validation

Passed:

1. `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`

Expected failure:

1. `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`

The golden failure is retained as evidence because PR-02-1 baselines were not
updated.

Not run:

1. PR-08 resource lint chain, because V12 did not change resources, manifests,
   owner contracts, sheet plans or generated inventory.
2. `verifyChanged` and packaged whitebox, because this is not PR-08 closure.

## Verdict

Accept V12 forward only as a partial material-authority boost.

It improves room-level material presence versus V11 and preserves marker
readability. It still fails director-grade closure: the map first read remains
too square-lattice-first and the reference crop still has stronger authored
room enclosure, wall thickness, local dark-field structure and floor detail.

## Next

Do not continue alpha-only or draw-order tuning on the same room-breakup asset.
The next useful cut should demote or remove the repeated dark lattice at its
source/renderer authority boundary, or replace the current room material family
with stronger authored material that reads before the tile lattice without
turning into fog wash.
