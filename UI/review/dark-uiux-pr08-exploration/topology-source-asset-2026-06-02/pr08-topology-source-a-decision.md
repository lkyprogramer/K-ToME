# PR08 Topology Source A Decision

Date: 2026-06-02

## Verdict

`pr08-ruins-dedicated-topology-source-a-v1-accepted-forward`

Dedicated topology source A is accepted-forward for the `tileset.ruins`
topology-risk path. It is a formal single-image runtime resource keyed by
`ui.map_stage.ruins.room_topology_source.pr08_demo`, while the mask/crop board
remains review-only evidence.

Supersession note: source B V1 now supersedes source A on the same runtime key.
Keep this file as the A V1 evidence record, not as the current source contract.

This packet does not close final Direction A, all-map quality, non-ruins
topology coverage or the procedural-room director-grade gate. It only removes
the most unsafe V1 source-crop behavior: sampling the accepted full-room plate
for L-like, corridor-heavy or tall topology-risk bands.

## Inputs

Generation brief:

```text
Top-down dark-fantasy ruins topology source, 4:3 orthographic room-fragment
painting, distributed stone wall segments, broken door thresholds, cracked
floor material, warm torch pools, dark corner falloff, no actors, no UI, no
letters, no numbers, no hotkeys, no baked gameplay markers, no single centered
room composition.
```

Candidate source:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02/ui_map_stage_ruins_topology_source_pr08_candidate_a.png
```

Candidate source hash:

```text
49c4cbebc691c8ddfb04d592b0c8450ff1a3b065a0842b4f12e98968e756f1b8
```

Promoted runtime resource:

```text
client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png
```

Runtime resource hash:

```text
49c4cbebc691c8ddfb04d592b0c8450ff1a3b065a0842b4f12e98968e756f1b8
```

Formal visual key:

```text
ui.map_stage.ruins.room_topology_source.pr08_demo
```

## Diagnostic Evidence

Mask/crop diagnostic board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02/pr08-topology-source-crop-mask-diagnostic-board.png
```

Diagnostic board hash:

```text
10c2723f35d909552473ce2d2cd34b1dc02a9978c981eb8c49651bff79e70142
```

The board compares the previous full-room source reuse against candidate A on
three representative topology-risk shapes: L-like, corridor-heavy offset and
tall cross. Cyan bands show source crop rectangles, green cells show visible
material cells and red cells show hidden cells inside the axis-aligned bounds.

The board proves the important safety property: the source bands follow visible
topology and do not cover the hidden notch cells that made single-bbox
full-room sampling unsafe.

D8 runtime board after source alpha `0.36`:

```text
UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png
```

D8 board hash:

```text
695a213343cda62f5e0d872f2dbd21d912903c3e897f57ebe5a7e3f9e96fdeea
```

## Implementation Contract

1. Plate-safe rooms keep `ui.map_stage.ruins.room_plate.pr08_demo`.
2. `tileset.ruins` topology-risk rooms require
   `ui.map_stage.ruins.room_topology_source.pr08_demo` for source-region bands.
3. Non-ruins risky rooms keep their prior fallback unless they receive their
   own topology source family.
4. The manifest schema is unchanged; this is a normal `ui_frame` single-image
   resource.
5. The mask diagnostic board is not a manifest asset and must not become a
   runtime dependency.

## Director Review

Candidate A is better than reusing the accepted full-room plate as a crop
source because it is fragment-friendly: wall mass, thresholds and warm light
pockets are distributed around the source instead of assuming one centered
room composition. The risky D8 rows now have a safer topology fit and avoid the
old hidden-notch leak.

It is still accepted-forward only. The risky rows remain darker and more
bottom-layer than the plate-safe full-room rows, so the visual result is not
yet director-grade procedural closure. The next quality move is source quality
and diagnostic formalization, not more seam, alpha or fog tuning.

## Validation

Commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 7s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 20s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 24s
```

## Next Contract

Promote mask/crop diagnostics into a repeatable owner/golden artifact or
generate a stronger topology source before using this path as evidence for
final procedural Direction A closure.
