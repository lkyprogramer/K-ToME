# PR08 Random-Seed Generalization Decision

Date: 2026-06-02

## Verdict

`accepted-with-topology-contract`

The PR08 ruins full-room plate remains visually strong in rectangular and
near-rectangular crops, but the D8 board does not prove production-safe
procedural generalization. The current fixed plate route must be constrained by
a topology-aware contract before Direction A can be promoted beyond fixed proof
crops.

Follow-up implementation result: PR08 topology contract V1 is now wired in the
client presentation boundary. Plate-safe visible regions keep the authored
full-room plate; topology-risky visible regions now route through a hybrid
presentation path that keeps runtime tile material visible and adds
topology-following material fields plus true-boundary dark edge marks instead
of stretching one authored room composition into an unsafe bbox. This is
accepted-forward as a contract and first-quality fix, not final director-grade
closure for complex procedural rooms.

Follow-up implementation result: PR08 topology-risk decomposition V1 now reuses
the canonical wall-family resolver for risky shapes. The hybrid path draws
existing crown, side and door-contact components over the real visible topology
instead of relying only on rectangle edge marks. This is accepted-forward as
resource-backed decomposition progress, not final procedural closure.

Follow-up implementation result: PR08 topology-risk AO/local-light V1 now adds
visible-run ambient depth fields, side pressure and deterministic warm pools to
the risky path. This makes complex rooms read less like flat component stitching
while still avoiding unsafe full-room plate stretching. It is accepted-forward
only; topology masks or richer authored components remain required for final
procedural closure.

Follow-up implementation result: PR08 topology-risk plate-fragment
decomposition V1 now draws low-alpha fragments of the accepted room plate only
inside contiguous visible runs. The first 0.185 alpha attempt was rejected after
visual review as stripe-forward; the accepted V1 uses 0.125 alpha so the plate
acts as a restrained material source rather than a new dominant texture layer.

Follow-up implementation result: PR08 topology-risk material-breakup band V1 now
routes the existing formal `tileset.ruins.room_breakup_01` authored decal
through visible-topology bands. The first focused test exposed that the hybrid
path still kept the old single-bbox room-breakup draw; that legacy bbox is now
disabled for topology-risk hybrid rooms, so the authored material source no
longer covers hidden L-shaped gaps.

Follow-up implementation result: PR08 topology-risk source-cropped room plate
V1 now introduces a real client-only source-region draw contract. The risky
path samples the accepted room plate through visible topology bands and removes
the previous low-alpha plate-fragment and room-breakup band bridges from that
path. This is accepted-forward as the first true crop contract, not final
director-grade procedural closure.

Follow-up implementation result: PR08 dedicated topology source A V1 now routes
`tileset.ruins` topology-risk source-region bands through the formal visual key
`ui.map_stage.ruins.room_topology_source.pr08_demo` instead of reusing the
accepted full-room plate as the crop source. The source key is present in the
canonical/runtime visual manifests and resident preload path, and the focused
L-shape test proves the risky path uses the dedicated source while the
plate-safe path keeps the full-room plate. This is accepted-forward only; the
D8 risky rows are safer and more coherent, but still below the authored quality
of plate-safe rows.

Follow-up implementation result: PR08 dedicated topology source B V1 supersedes
A on the same formal visual key. B keeps the same source-region topology
contract but improves the source material itself: distributed wall mass,
threshold fragments, warm light pockets and readable stone mid-bands survive
the L-like, corridor-heavy and tall-cross crop masks better than A. B is
accepted-forward only because the D8 risky rows still do not match plate-safe
authored-room quality and the source has more small-tile texture that must be
watched in later passes.

Follow-up implementation result: PR08 dedicated topology source C V1 supersedes
B on the same formal visual key. C keeps the source-region topology contract
and improves the runtime risky rows with brighter, more continuous material in
corridor-heavy and horizontal crop bands. C is accepted-forward only: the
problem has moved closer to authored resource quality, but risky rows are still
more component-like than plate-safe rows.

Follow-up implementation result: PR08 topology source mask owner artifact V1
promotes the mask/crop diagnostic from one-off review evidence into a
repeatable `client` golden artifact. This closes the diagnostic ownership gap,
but does not change the D8 verdict: C remains accepted-forward only and source
D must still reduce the remaining risky-row component feel.

Follow-up implementation result: PR08 dedicated topology source D V1 supersedes
C on the same formal visual key after being generated as a more crop-safe
topology atlas. D passes the repeatable mask owner artifact, D8 runtime golden
evidence and focused manifest/resource/renderer gates. This improves the
source-art side of the contract but still does not claim final procedural
Direction A closure.

## Evidence

Evidence root:

```text
UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/
```

Historical primary board:

```text
UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/dark-uiux-pr08-generalization-board.png
```

Historical board hash:

```text
    695a213343cda62f5e0d872f2dbd21d912903c3e897f57ebe5a7e3f9e96fdeea
```

Historical index:

```text
UI/review/dark-uiux-pr08-exploration/random-seed-generalization-2026-06-02/evidence-index.tsv
```

Current C runtime board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/runtime-c-d8/dark-uiux-pr08-generalization-board.png
```

Current C board hash:

```text
78b96c76a768ef892192a8f474c43a6582610e3016c2b803618e43f649604ab9
```

Current C evidence index:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/runtime-c-d8/evidence-index.tsv
```

Current C evidence index hash:

```text
eb5440ca776ddd0e825716f09c1bfb00b5eccc2d59c5742616ee566c60e5bddb
```

Current mask owner board:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/dark-uiux-pr08-topology-source-mask-board.png
```

Current mask owner board hash:

```text
8c0fb5155efc49ede5e95d5f9643227ee7525db053c6b97d775ce32a9f6cac96
```

Current mask owner evidence index:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-mask-owner-2026-06-02/evidence-index.tsv
```

Current mask owner evidence index hash:

```text
a66dfb8283a26403e6242cdefbf5e18e307b4f98b2f4167609148aa511b804a3
```

Captured probe set:

| Label | Seed | Floor | Topology | Hash |
| --- | ---: | ---: | --- | --- |
| `dark-uiux-pr08-generalization-ruins-proof-baseline` | `2026051102` | `1` | `13x9 / fill 786` | `d79563d35243fbbd79d185f06f3c9817312a3287193b4b3361a48cfd2d904270` |
| `dark-uiux-pr08-generalization-ruins-seed-2026060801` | `2026060801` | `1` | `14x13 / fill 868` | `6926211c5c8969a3e8cbe40eec0779bc2bdcb2fa4ca16e5941b06260a5883b8b` |
| `dark-uiux-pr08-generalization-ruins-seed-2026060802` | `2026060802` | `1` | `13x12 / fill 608` | `22a140eb257359ce53f344690756408f726d840441cad04602d4d4ff2a1f1d82` |
| `dark-uiux-pr08-generalization-ruins-seed-2026060803` | `2026060803` | `1` | `16x15 / fill 675` | `3ad0c035a05807cd3fa338a3e2d1c7515d154844b974b6c606b9d4930a0e97c7` |
| `dark-uiux-pr08-generalization-ruins-seed-2026060804-floor2` | `2026060804` | `2` | `13x12 / fill 647` | `0254226b8426774c31c83c72ce92def0e49b929cf1130090e35f48487c27a7a1` |
| `dark-uiux-pr08-generalization-ruins-seed-2026060805-floor2` | `2026060805` | `2` | `15x12 / fill 650` | `1e1d79afff068d95874e8d80430349ff75d936d4b8805205466349185508cbc0` |
| `dark-uiux-pr08-generalization-ruins-seed-2026060806` | `2026060806` | `1` | `9x16 / fill 888` | `06ee477ef6ff02f736f785ff90515f7ab0217f27d512a2fa2e3053d65f8a25fc` |
| `dark-uiux-pr08-generalization-ruins-seed-2026060807-floor2` | `2026060807` | `2` | `12x14 / fill 773` | `4a026a7eeed05e7d72f0908b2fa21d2c72ca6532a9e33e870b1c38d2a3daac27` |

## Director Review

The current plate keeps the desired dark-fantasy material language: heavy
walls, warm torch pools, rough slab rhythm and better first-read quality than
the old grid-first renderer. It still beats returning to per-cell floor/wall
material as the default visual direction.

The failure is not aesthetic polish. The failure is topology authority. The
renderer stretches one authored full-room composition into the axis-aligned
bounds of currently visible ruins material. When the visible region is
compact, broad or near rectangular, this is acceptable. When the visible region
becomes L-like, corridor-heavy or high-narrow, baked wall mass, torch placement
and dark cutouts no longer fully agree with the playable room topology.

After the topology contract and hybrid V1 pass, the risky low-fill and
extreme-aspect probes no longer use that unsafe full-room stretch. The board
now separates two states: plate-safe rooms keep the authored room source, while
topology-risky rooms keep deterministic runtime tile material and gain
topology-following fields / inner-edge mass that trace the actual visible room
shape. That is stronger than the plain fallback, but the hybrid rows still lack
the room-scale AO, local-light and mask/decomposition cohesion of the full
plate. Topology-risk decomposition V1 improves this by drawing canonical
wall-family crown, side and door-contact components, but it is still a
component bridge rather than a final authored-room resource system.

After AO/local-light V1, the risky rows gain a warmer focal hierarchy and
stronger carved-room depth without reintroducing the unsafe full-plate bbox
stretch. This improves the director read of L-like and offset rooms, but the
grid rhythm and component boundaries still remain more visible than in the
plate-safe authored-room rows.

After plate-fragment decomposition V1, risky rows also inherit a restrained
amount of the accepted room plate's authored material. The important contract
change is not the texture itself; it is that the plate-derived material is now
decomposed by visible topology and does not cover hidden L-shaped gaps. This is
still a bridge technique, because true director-grade procedural closure needs
a source designed for fragmenting rather than repeatedly sampling a full-room
composition.

After material-breakup band V1, risky rows also consume the existing formal
`tileset.ruins.room_breakup_01` authored decal through visible-topology bands.
The old single-bbox room-breakup draw is disabled for topology-risk hybrid
rooms, so this resource no longer paints across hidden notches. This is a
better bridge than the earlier full-bbox material pass, but it is still not a
dedicated topology-aware room source.

After source-cropped room plate V1, risky rows draw source regions from the
accepted room plate according to topology bands. The important improvement is
contractual: `TileAssetSourceRegion` gives the client renderer a true crop
surface, and the L-shape focused test now proves the horizontal band, vertical
arm and hidden notch are distinct. Visually this is more cohesive than the
row-fragment and room-breakup bridge, but it still samples a full-room source
that was not authored as a topology-fragment atlas.

After dedicated topology source A V1, risky rows no longer sample the accepted
full-room plate. They draw the same topology bands from
`ui.map_stage.ruins.room_topology_source.pr08_demo`, which is authored as a
fragment-friendly ruins source with distributed walls, floor material and warm
light pockets. The mask diagnostic board shows the visible bands avoid hidden
notches and that candidate A is safer than full-room source reuse. The D8 board
improves topological fit, but the risky rows remain darker and less
director-grade than the plate-safe full-room rows, so this packet is
accepted-forward only.

After dedicated topology source B V1, the same runtime key carries a stronger
source. The A/B mask board shows B preserves the hidden-notch safety while
putting more usable wall, threshold, rubble and warm-light structure into each
crop band. Runtime D8 evidence improves source-derived room read in risky
rows, but it also reveals a remaining risk: B's small floor tile texture can
move the risky path back toward grid-first read if pushed too hard.

After dedicated topology source C V1, the same runtime key carries a brighter
source with better continuity through corridor-heavy and horizontal crop
bands. The B/C mask board shows C keeps hidden-notch safety while improving
mid-band material read. The forced D8 recapture confirms C is a better
same-key source than B, but not final closure: the risky rows still read more
like assembled components than plate-safe authored rooms.

## Root Cause Class

`room/material authority` plus `fixed-crop overfit`.

Current runtime shape:

```text
visible material cells -> axis-aligned bbox -> one full-room plate stretched to bbox
```

This is not enough for arbitrary procedural room shapes because the plate owns
composition details that should be constrained by topology.

## Rejected Alternatives

1. `accepted-generalizes`
   - Rejected because the D8 board shows visible composition mismatch risk in
     L-like, corridor-heavy and high-narrow crops.
2. `more full-room art candidate generation`
   - Rejected because a stronger single full-room painting would still be
     stretched by the same bbox contract.
3. `more alpha / seam / fog tuning`
   - Rejected because the blocker is topology fit, not local overlay weight.
4. `reject-direction-a-for-procgen`
   - Rejected because rectangular and near-rectangular samples still show that
     authored room art is the right quality direction.

## Required Next Contract

Direction A remains viable only after continuing from the V1 classification
route into one of these topology-aware presentation routes:

1. keep the stable visible-room topology model and strengthen the current
   topology-following hybrid material stack;
2. draw the authored plate through a topology mask or rect decomposition rather
   than a single bbox;
3. split the full-room plate into topology-following components: wall crown,
   side/corner/door-contact pieces, room-scale AO, material fields and local
   light pools;
4. keep the full plate only for plate-safe rectangular rooms and route complex
   shapes to a hybrid presentation stack.

The latest implementation slice has moved the `tileset.ruins` topology-risk
path from V1 marks, component overlays, local-light fields and source-cropped
full-room reuse to dedicated topology source D V1 plus a repeatable mask owner
artifact. The immediate D follow-up has now been accepted as a bounded
risky-path source-alpha pass, raising dedicated topology-source crop bands from
`0.36` to `0.42`. The source-alpha scope guard now keeps fallback family
room-plate source bands at `0.36` until those families have dedicated
topology-source assets. The next accepted-forward composition grammar slice is
topology-risk interior seam dissolve V1: long-run internal shared-seam fields
are drawn only where adjacent visible topology is actually connected, with a
focused L-shape test guarding horizontal/vertical coverage and hidden notches.
The next contract should continue only with broader topology-risk wall/floor
composition grammar, dedicated topology-source coverage for non-ruins
families, or packaged/director recapture before claiming final Direction A
closure from procedural-room evidence.

## Why Not Micro-Tuning

Changing alpha, seams, line weight, fog, draw order or same-key texture detail
cannot fix a mismatch between baked wall/light composition and procedural room
shape. The owning layer is the presentation model and topology contract.

## Why Not Fixed-Crop Overfit

The original proof slice remains valid only for its fixed crop. The D8 board
expands evidence to eight seed/floor/profession combinations and shows that
the current full-room plate is not universally stable under procedural
topology variance. This packet is therefore a bounded generalization probe,
not a closure packet.

## Rollback

If the topology contract path becomes noisy, remove the focused topology test
and revert the `RoomArtPlateTopologyContract` wiring. If the material-breakup
band route becomes visually noisy, restore the old warm-overlay room-breakup
draw for topology-risk hybrid rooms only after adding a topology-risk artifact
that proves it does not cover hidden gaps. Existing PR08 director golden
evidence and runtime room art plate resources are not changed by this packet.
If dedicated topology source A becomes visually noisy, remove
`ui.map_stage.ruins.room_topology_source.pr08_demo` from the manifest/preload
route and let topology-risk rooms fall back to the previous accepted-forward
source-crop behavior while keeping the mask diagnostic evidence for the next
source iteration.

## Validation

Command run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache
```

Result:

```text
BUILD SUCCESSFUL in 25s
```

Warnings were limited to existing deprecated `getFrameBufferPixmap` call
sites in the golden harness.

Topology contract V1 commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 8s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 19s
```

Topology-risk AO/local-light V1 commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 8s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 20s
```

Dedicated topology source A V1 commands run:

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

The first dedicated-source board used source alpha `0.28` and was visually
judged too dim for the topology-risk rows. The runtime draw alpha was raised
to `0.36` and the focused renderer + D8 golden proof was rerun:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 24s
```

Dedicated topology source B V1 command run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 22s
```

B evidence root:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round2/runtime-b-d8/
```

Dedicated topology source C V1 command run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 22s
```

Dedicated topology source C V1 forced recapture command run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries" --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache --rerun-tasks
```

```text
BUILD SUCCESSFUL in 44s
27 actionable tasks: 27 executed
```

C evidence root:

```text
UI/review/dark-uiux-pr08-exploration/topology-source-asset-2026-06-02-round3/runtime-c-d8/
```

Topology-risk source-cropped room plate V1 commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD FAILED in 12s
```

The failed focused run showed two source-cropped room-plate bands with correct
source regions, but the assertion still used the old local-light anchor instead
of representative source-crop band cells.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD FAILED in 4s
```

The second focused run used an incorrect vertical representative point after
viewport y mapping. The assertion was corrected to a visible vertical-arm cell.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 4s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 19s
```

Final forced rerun after document/evidence reconciliation:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache --rerun-tasks
```

```text
BUILD SUCCESSFUL in 46s
24 actionable tasks: 24 executed
```

Topology-risk plate-fragment decomposition V1 commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 8s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 21s
```

Topology-risk hybrid V1 commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 9s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 20s
```

Topology-risk decomposition V1 commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 13s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 19s
```

Topology-risk material-breakup band V1 commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD FAILED in 8s
```

The failed focused run exposed that the topology-risk hybrid path still kept
the old single-bbox `tileset.ruins.room_breakup_01` draw at high alpha. The
runtime branch was narrowed so topology-risk hybrid rooms use the new
visible-band material-breakup route instead.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 9s
```

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsPr08RoomArtPlateBelowActorsAndMarkersForRuinsPrototype" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasFallsBackFromPr08FullRoomPlateWhenVisibleTopologyIsLShaped" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasSuppressesPr08LegacyRoomDecorativePassesWhenRoomArtPlateIsActive" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 random seed generalization probe writes board artifacts" maintainabilityLint --no-configuration-cache
```

```text
BUILD SUCCESSFUL in 20s
```
