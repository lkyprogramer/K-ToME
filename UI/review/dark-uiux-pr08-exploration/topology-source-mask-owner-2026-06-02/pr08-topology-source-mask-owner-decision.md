# PR08 Topology Source Mask Owner Decision

Date: 2026-06-02

## Verdict

`pr08-topology-source-mask-owner-artifact-v1-accepted-forward`

The PR08 topology source mask diagnostic is now a repeatable `client`
golden owner artifact instead of a review-only one-off board.

This is a verification-boundary improvement, not a runtime rendering or
resource-quality closure. The board reads the current promoted runtime source
`ui_map_stage_ruins_room_topology_source_pr08_demo.png`, renders it through
fixed L-like, corridor-heavy offset and tall-cross crop masks, and writes a
repo-relative evidence index with the source hash. It does not become runtime
truth and does not change the manifest or presentation contract.

## Artifact

Owner board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/dark-uiux-pr08-topology-source-mask-board.png
```

Owner board hash:

```text
8c0fb5155efc49ede5e95d5f9643227ee7525db053c6b97d775ce32a9f6cac96
```

Evidence index:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/evidence-index.tsv
```

Evidence index hash:

```text
a66dfb8283a26403e6242cdefbf5e18e307b4f98b2f4167609148aa511b804a3
```

Source resource hash:

```text
da6b3b9150e93f78a28d08fb92668a96f228333020326ff4b46a082c4a6c92a2
```

## Runtime Contract

1. Same runtime visual key remains active:
   `ui.map_stage.ruins.room_topology_source.pr08_demo`.
2. Same runtime source path remains active:
   `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`.
3. The mask board is generated under
   `client/build/reports/golden/dark-uiux-pr08-topology-source-mask/`.
4. The checked-in review evidence is copied from that generated owner output.
5. No `core`, `game`, manifest schema, save, replay, profile or gameplay
   contract changed.

## Director Read

The owner board confirms the C source is usable across the three canonical
topology-risk masks and keeps hidden bbox cells visually separate from visible
crop bands. It also makes the remaining quality gap easier to review:

1. L-like and corridor-heavy bands retain usable warm-light and wall/floor
   continuity.
2. Tall-cross bands still read more component-like than plate-safe full-room
   rows.
3. This artifact proves repeatable reviewability, not final director-grade
   procedural closure.

## Validation

Focused owner artifact command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 topology source mask diagnostic writes owner artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 4s
```

D8 generalization command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 19s
```

Focused manifest/renderer/lint command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 2s
```

Rejected validation shape:

```text
The combined command that ran the new mask owner golden and D8 generalization
inside one :client:goldenScreenshot invocation was terminated with exit 143
after the first mask test had already passed and the D8 pass stalled. The D8
test then passed when run as its own Gradle invocation, so the accepted
validation shape is split golden invocations.
```

## Next Contract

This owner artifact was reused to generate and judge source D:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-d-decision.md
```

Continue from the D decision packet when deciding whether the remaining
risky-row gap is source-art driven or compositor/source-alpha driven.
