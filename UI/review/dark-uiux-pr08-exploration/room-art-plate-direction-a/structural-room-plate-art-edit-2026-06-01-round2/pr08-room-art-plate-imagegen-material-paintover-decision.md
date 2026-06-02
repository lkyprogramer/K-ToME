# PR-08 Room Art Plate Imagegen Material Paintover Decision

Date: 2026-06-01

## Scope

This packet continues Direction A for the `tileset.ruins` map-stage blocker.
It keeps the existing runtime key
`ui.map_stage.ruins.room_plate.pr08_demo` and replaces only the same-key room
plate image. It does not change `core`, `game`, save/replay/profile schema,
manifest schema, visibility truth, gameplay markers, actors, loot or combat
rules.

## Method

The built-in image generation path produced one higher-quality ruins room art
source. That source was copied into this packet as:

`UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/source/imagegen-structural-room-plate-source-l.png`

The direct generated source was not promoted because it changed room geometry,
wall/door placement and authored extra environmental structure. The accepted
path uses it only as a material and fracture reference, then performs a
geometry-safe paintover on top of the existing K plate.

## Candidate Board

Primary evidence:

1. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/structural-room-plate-art-edit-round2-candidate-board.png`
2. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/structural-room-plate-art-edit-round2-opq-candidate-board.png`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-k-vs-round2-candidates-board.png`
4. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-k-vs-q-selected-board.png`

Candidate decisions:

1. L: rejected as production baseline. It improves source material but leaks
   generated right-edge room structure into the runtime crop.
2. M: rejected as production baseline. It suppresses grid read more strongly
   but becomes patchy and too dark on the right edge.
3. N: rejected as production baseline. It leaks generated prop/structure
   language into the playable crop.
4. O: rejected as selected baseline. It is geometry-safe but too conservative
   compared with K.
5. P: rejected as selected baseline. It is geometry-safe and strong, but it
   darkens the combat field too much relative to marker readability.
6. Q: selected. It keeps the existing room geometry, avoids generated prop
   leakage, adds warm readable degrid smoke and non-square fracture language,
   and preserves actor, loot and telegraph readability better than P/N.

## Selected Resource

Selected source candidate:

`UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/candidates/pr08-room-art-plate-structural-edit-q_warm-readable-degrid.png`

Promoted runtime resource:

`client/src/main/resources/dark-v1/ui/ui_map_stage_ruins_room_plate_pr08_demo.png`

Selected resource hash:

```text
e450669ec054d302fb62ac857dc5f3d7200ac8d297ac15b00b7f992de666f78e
```

The image authority spec now points to Q:

`assets-src/image/specs/phase4-uiux-pr08-room-art-plate-plan.yaml`

## Runtime Evidence

Selected runtime evidence:

1. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-q-warm-readable-degrid/ui-demo-new-map-stage-crop.png`
2. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-q-warm-readable-degrid/ui-demo-new-parity-1672x941.png`
3. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-q-warm-readable-degrid/ui-demo-new-parity-1280x800.png`
4. `UI/review/dark-uiux-pr08-exploration/room-art-plate-direction-a/structural-room-plate-art-edit-2026-06-01-round2/runtime-q-warm-readable-degrid/evidence-index.tsv`

Latest focused golden actual hashes from the expected-fail golden assertion:

```text
ui-demo-new-parity-1672x941=e1f42774ab4fb6000d18179246512b3b03d5167532c572d3c6278e7339206c59
ui-demo-new-parity-1280x800=86d0940e4ae2ab148b1990c83f8d511ea56a401cd8a6fee89440b570c7373787
ui-demo-new-map-stage-crop=0f748ee34e09bf545cf1a3b27864ed7f688c916eab089e838f0d5a40c6fcc873
```

## Validation

Commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts" --rerun-tasks --no-configuration-cache
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest.darkUiuxPr08DirectorFloorWallKeysResolveThroughExactEntries --tests "com.ktome.client.assets.ManifestResolveTest.session and warm cache preload resident textures when gdx is available" --tests "com.ktome.client.assets.ManifestResolveTest.non ruins tilesets do not preload pr08 ruins room presentation textures" --no-configuration-cache
./gradlew resourcePipelineLint --no-configuration-cache
```

Results:

1. Expected fail: focused golden hash gate, because PR-08 has not rebaselined
   PR-02-1 golden hashes.
2. Pass: PR-08 manifest/preload focused tests.
3. Pass: `resourcePipelineLint`.

## Director Verdict

Q is accepted forward as the current same-key room plate resource baseline.
D3 map-stage closure is still rejected.

Q is safer than L/M/N and stronger than O because it improves the center-right
combat field's non-grid material read without copying generated room geometry.
It is still not equal to `UI/UI-demo-new.png`: runtime cell surfaces remain
visible over the right combat field, so final PR-08 closure and golden
rebaseline remain blocked.

Next action: stop local paintover escalation for this exact crop unless a
substantially stronger full-room generated source can preserve topology. The
next high-return iteration should reassess the runtime interaction/visibility
surface stack at a product-design level, not continue small alpha or marker
micro-tuning.
