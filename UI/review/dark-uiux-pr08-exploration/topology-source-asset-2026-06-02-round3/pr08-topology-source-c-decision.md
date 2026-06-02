# PR08 Topology Source C Decision

Date: 2026-06-02

## Verdict

`pr08-ruins-dedicated-topology-source-c-v1-accepted-forward`

Follow-up owner artifact added on 2026-06-02:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/pr08-topology-source-mask-owner-decision.md
```

Superseded on 2026-06-02 by dedicated topology source D V1:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-d-decision.md
```

Topology source C supersedes source B as the current same-key runtime
prototype for `ui.map_stage.ruins.room_topology_source.pr08_demo`.

This is a source-quality iteration, not a new resource contract. The visual
key, manifest schema, runtime path and presentation boundary stay unchanged.
C is accepted-forward because the B/C diagnostic and forced D8 runtime
recapture show brighter, more continuous material in corridor-heavy and
horizontal topology crop bands. It still does not close final procedural
Direction A quality.

## Source

Candidate C:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/ui_map_stage_ruins_topology_source_pr08_candidate_c.png
```

Candidate C hash:

```text
da6b3b9150e93f78a28d08fb92668a96f228333020326ff4b46a082c4a6c92a2
```

Promoted runtime resource:

```text
client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png
```

Runtime resource hash:

```text
da6b3b9150e93f78a28d08fb92668a96f228333020326ff4b46a082c4a6c92a2
```

Formal visual key:

```text
ui.map_stage.ruins.room_topology_source.pr08_demo
```

## Prompt

```text
Top-down orthographic dark fantasy ruined dungeon topology-source painting for
crop bands. Fragment-friendly, no single centered room, distributed wall
ribbons, broken thresholds, rubble shoulders, warm torch pools, larger
irregular worn slab fields, readable mid-band material at low runtime alpha,
no actors, no loot, no UI, no text, no numbers, no cursor, no gameplay
markers, no grid overlay.
```

The generation returned one new candidate plus two images matching existing
A/B hashes. The new candidate was selected as C; the existing-hash outputs were
not promoted.

## B/C Diagnostic

B/C mask diagnostic board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/pr08-topology-source-c-vs-b-crop-mask-diagnostic-board.png
```

Diagnostic board hash:

```text
7b2ffdc688278522e6d6f5ef9b397d30b25265088443fdf3e0858f50949dc39e
```

The board compares B and C on the same L-like, corridor-heavy offset and tall
cross topology masks. Cyan lines show source crop bands, green cells are
visible cells and red cells are hidden cells inside the topology bounds.

Director read:

1. C keeps the same hidden-notch safety as B because it uses the same
   source-region band contract.
2. C improves the corridor-heavy and horizontal crop bands with brighter,
   more continuous floor/wall/light material.
3. C does not fully solve the small-grid and component-row read; it is a better
   source candidate, not final closure.

## Runtime Evidence

C D8 evidence root:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/runtime-c-d8/
```

C D8 board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/runtime-c-d8/dark-uiux-pr08-generalization-board.png
```

C D8 board hash:

```text
78b96c76a768ef892192a8f474c43a6582610e3016c2b803618e43f649604ab9
```

C D8 evidence index hash:

```text
eb5440ca776ddd0e825716f09c1bfb00b5eccc2d59c5742616ee566c60e5bddb
```

Runtime read:

1. Plate-safe rows still keep the stronger authored full-room plate.
2. Topology-risk rows are brighter than B in corridor-heavy and horizontal
   bands and carry more visible wall/floor/light continuity at low alpha.
3. Risky rows remain more component-like than plate-safe rows, so C is not
   final director-grade procedural closure.

## Contract

1. Same visual key: `ui.map_stage.ruins.room_topology_source.pr08_demo`.
2. Same runtime file:
   `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`.
3. Same manifest schema and category: `ui_frame`.
4. No gameplay, save, replay, profile, content pack, localization or input
   contract changes.
5. The B/C diagnostic board is review evidence only; it is not a manifest
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

Forced D8 recapture command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache --rerun-tasks
```

```text
BUILD SUCCESSFUL in 44s
27 actionable tasks: 27 executed
```

The forced recapture was required because the first pass completed successfully
but the previously copied `UI/review/.../random-seed-generalization-2026-06-02`
board was stale. The authoritative fresh artifacts came from
`client/build/reports/golden/dark-uiux-pr08-generalization/` and were copied
into `runtime-c-d8`.

## Next Contract

Source D has now been generated and accepted-forward on the same key. Continue
from the D decision packet when judging whether the remaining risky-row gap is
source-art driven or compositor/source-alpha driven.
