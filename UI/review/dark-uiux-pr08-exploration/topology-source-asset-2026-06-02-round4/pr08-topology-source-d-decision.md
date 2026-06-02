# PR08 Topology Source D Decision

Date: 2026-06-02

## Verdict

`pr08-ruins-dedicated-topology-source-d-v1-accepted-forward`

Topology source D supersedes source C as the current same-key runtime
prototype for `ui.map_stage.ruins.room_topology_source.pr08_demo`.

This is a source-quality iteration, not a new resource contract. The visual
key, manifest schema, runtime path and presentation boundary stay unchanged.
D is accepted-forward because it passes the repeatable mask owner artifact,
D8 runtime golden evidence and focused manifest/resource/renderer gates while
improving the crop-safe material distribution over C. It still does not close
final procedural Direction A quality.

## Source

Candidate D:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/ui_map_stage_ruins_topology_source_pr08_candidate_d.png
```

Candidate D hash:

```text
5879b51e5c2a3fad2c86316bdbdf2c9364a6d45cf64d4daba45a0700c6f2a906
```

Promoted runtime resource:

```text
client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png
```

Runtime resource hash:

```text
5879b51e5c2a3fad2c86316bdbdf2c9364a6d45cf64d4daba45a0700c6f2a906
```

Formal visual key:

```text
ui.map_stage.ruins.room_topology_source.pr08_demo
```

## Prompt

```text
Use case: stylized-concept
Asset type: K-ToME PR08 dark UI/UX runtime topology-source atlas image, 4:3, target 1448x1086 PNG-like raster.
Primary request: Create a top-down orthographic dark fantasy ruined dungeon topology-source painting for crop-band rendering. It must be a crop-safe material atlas for procedural visible topology masks, not one centered complete room. Quality should match the visible K-ToME reference UI screenshot and improve upon the current Source C by reducing dark rectangular dead zones and component-row striping when cropped into L-like, corridor-heavy, and tall-cross masks.
Style tags: ktome-middle-fantasy-painterly-tile-v1 and ktome-dark-fantasy-sprite-ui-v1.
Scene/backdrop: ancient ruined stone dungeon floor and broken wall fragments seen from directly above, low-saturation charcoal and cold stone, warm torch/ember pools, dark edge pressure.
Subject: distributed crop-safe wall ribbons, broken masonry shoulders, corridor mouths, small parapet turns, rubble pockets, worn irregular slab fields, subtle moss/dirt staining, and several modest warm light pools spread across the image so every horizontal and vertical crop band contains usable material.
Composition requirements: no single centered room, no complete fixed perimeter, no large empty black rectangles, no big blank fog fields, no obvious repeated grid overlay, no actors, no items, no loot, no UI, no text, no numbers, no cursor, no gameplay markers. Keep the whole image useful for arbitrary source-region crops: top, middle, bottom, left, right, and narrow vertical bands should all contain coherent floor/wall/light material. Use heavier readable material through the center vertical and center horizontal corridors to support tall-cross masks. Keep edges dark but still materially readable.
Rendering: painterly concept-art finish, orthographic tile-friendly, worn stone and blackened masonry, warm torch highlights balanced with cool shadow, restrained detail at runtime alpha, strong authored-room quality, no photoreal collage, no anime, no sci-fi, no neon.
```

The built-in image generation path produced one accepted D candidate. The
generated source was copied into the repository review evidence path and then
promoted to the same runtime prototype path.

## C/D Diagnostic

C/D mask owner comparison board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-c-vs-d-mask-owner-board.png
```

C/D mask owner board hash:

```text
74246ead9ff9a740e10870802fdcd396e2ae7c1fde0b218c2a814fb5503b41ef
```

D mask owner board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-d-mask-owner-board.png
```

D mask owner board hash:

```text
cfb46cf84728209e98f13b2b1a0d587873b37f3141620d86009115026e66571a
```

D mask owner evidence index hash:

```text
2412afb89aafe5284e836784f3757450239b519e0dd60e973e88e4998a826e89
```

Director read:

1. D keeps the same hidden-notch safety because it uses the same source-region
   band contract and the same runtime visual key.
2. D improves crop-band material distribution over C: L-like and
   corridor-heavy bands have more evenly readable floor/wall fragments, and
   the tall-cross vertical band receives stronger center-cross stone material.
3. D is still a bounded source-quality iteration. It reduces the risky-row
   component feel but does not make every topology-risk row read like the
   plate-safe authored full-room rows.

## Runtime Evidence

D D8 evidence root:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/runtime-d-d8/
```

D D8 board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/runtime-d-d8/dark-uiux-pr08-generalization-board.png
```

D D8 board hash:

```text
5bccb74b686b562d87ac10f499b78df496f123d8cd6520ed6e298b16d8843da4
```

D D8 evidence index hash:

```text
1d5a7ff6263ac687b3b7eb672e9b6880e2032855231d9b4790157905c930c4aa
```

C/D D8 comparison board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round4/pr08-topology-source-c-vs-d-d8-board.png
```

C/D D8 comparison board hash:

```text
99158dec3b7d123cfe4cc6099d47ad63c9d78e513c155f888cfdfbbd3eb7f608
```

Runtime read:

1. Plate-safe rows remain unchanged in contract and continue using the stronger
   authored full-room plate path.
2. Topology-risk rows retain runtime tile material, semantic overlays and actor
   readability while receiving a stronger same-key topology source.
3. D improves source distribution but low-alpha risky rows can still read as
   too subdued beside plate-safe authored-room rows. Further closure should
   target compositor/source alpha balance or a stronger runtime-specific
   source if fixed-crop evidence proves the remaining gap is still art-source
   driven.

## Contract

1. Same visual key: `ui.map_stage.ruins.room_topology_source.pr08_demo`.
2. Same runtime file:
   `client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_topology_source_pr08_demo.png`.
3. Same manifest schema and category: `ui_frame`.
4. No gameplay, save, replay, profile, content pack, localization or input
   contract changes.
5. The checked-in comparison boards are review evidence only; the runtime truth
   remains the canonical visual manifest plus runtime resource path.

## Validation

Focused mask owner command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 topology source mask diagnostic writes owner artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 8s
```

D8 generalization command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 20s
```

Focused manifest/renderer/lint command:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 8s
```

## Next Contract

Do not claim final procedural-room closure from D alone. The D follow-up was to
use the D D8 board and current director runtime evidence to decide whether the
remaining quality gap was still source-art driven or whether the risky-path
compositor needed a bounded alpha/contrast/runtime-read pass.

Follow-up resolved in:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-alpha-2026-06-02/pr08-topology-source-alpha-decision.md
```

The immediate D follow-up was accepted as a bounded risky-path source-alpha
pass: dedicated topology-source crop bands now render at `0.42` instead of
`0.36` without changing the visual key, runtime source asset or manifest
schema.
