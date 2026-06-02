# PR08 Topology Source B Decision

Date: 2026-06-02

## Verdict

`pr08-ruins-dedicated-topology-source-b-v1-accepted-forward`

Superseded on 2026-06-02 by dedicated topology source C V1:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/pr08-topology-source-c-decision.md
```

Topology source B previously superseded source A as the same-key runtime prototype
for `ui.map_stage.ruins.room_topology_source.pr08_demo`.

This is a source-quality iteration, not a new resource contract. The visual key,
manifest schema, runtime path and presentation boundary stay unchanged. B is
accepted-forward because it gives topology-risk bands stronger distributed wall
mass, threshold fragments, warm light pockets and readable mid-band stone
material. It still does not close final procedural Direction A quality.

## Source

Candidate B:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/ui_map_stage_ruins_topology_source_pr08_candidate_b.png
```

Candidate B hash:

```text
f3ead79aeeb39f3af73d75352f48a597f92f03baf7f179feacea356a251bad5c
```

Promoted runtime resource:

```text
client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png
```

Runtime resource hash:

```text
f3ead79aeeb39f3af73d75352f48a597f92f03baf7f179feacea356a251bad5c
```

Formal visual key:

```text
ui.map_stage.ruins.room_topology_source.pr08_demo
```

## Prompt

```text
Top-down orthographic dark fantasy ruins topology source painting for
source-region cropping into L-shaped and corridor-heavy visible room bands.
Fragment-friendly, no single centered room, distributed broken wall mass,
thresholds, rubble shoulders, readable worn-stone mid-bands, warm torch pools,
dark edge pressure, no actors, no loot, no UI, no text, no numbers, no cursor,
no gameplay markers, no grid overlay.
```

## A/B Diagnostic

A/B mask diagnostic board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/pr08-topology-source-b-vs-a-crop-mask-diagnostic-board.png
```

Diagnostic board hash:

```text
0ca943c943aacadd270a43f44a3be47fd121a5aaefc7f2f59c259c34f5831ce9
```

The board compares A and B on the same L-like, corridor-heavy offset and tall
cross topology masks. Cyan lines show source crop bands, green cells are
visible cells and red cells are hidden cells inside the topology bounds.

Director read:

1. B preserves A's hidden-notch safety because it uses the same source-region
   band contract.
2. B carries more visible room structure inside arbitrary crops: wall fragments,
   stair/threshold hints, rubble shoulders and warm light pools.
3. B is less bottom-layer than A, but also has more small-tile floor texture.
   That texture must not be allowed to become the new tactical-grid first read.

## Runtime Evidence

B D8 evidence root:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/runtime-b-d8/
```

B D8 board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/runtime-b-d8/dark-uiux-pr08-generalization-board.png
```

B D8 board hash:

```text
a2ec17b6111814e247a8117c49293663a08802530eaceeaacc9a21e5a0457295
```

B D8 evidence index hash:

```text
f5fe55edd0948e8676e696da30bc3f85d5463b963bc881eec949c35d4477a6f4
```

Runtime read:

1. Plate-safe rows still keep the stronger authored full-room plate.
2. Topology-risk rows now show more source-derived masonry and light structure
   than A.
3. The risky rows remain darker and more component-like than plate-safe rows,
   so B is not final director-grade closure.

## Contract

1. Same visual key: `ui.map_stage.ruins.room_topology_source.pr08_demo`.
2. Same runtime file:
   `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`.
3. Same manifest schema and category: `ui_frame`.
4. No gameplay, save, replay, profile, content pack, localization or input
   contract changes.
5. The A/B diagnostic board is review evidence only; it is not a manifest
   resource and must not become runtime truth.

## Validation

Command run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 22s
```

The same command was rerun after spec/log/decision writeback:

```text
BUILD SUCCESSFUL in 1s
27 actionable tasks: 4 executed, 23 up-to-date
```

SDKMAN printed an internet reachability warning during the rerun, but it still
selected `java 21.0.10-tem` and `kotlin 2.2.21`; Gradle completed successfully.

## Next Contract

Promote mask/crop diagnostics into an owner/golden artifact, or generate a
source C that keeps B's distributed wall/light structure while reducing its
small-tile floor-grid texture.
