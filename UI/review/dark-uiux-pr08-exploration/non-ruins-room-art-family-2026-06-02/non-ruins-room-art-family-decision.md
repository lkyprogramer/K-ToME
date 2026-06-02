# PR08 Non-Ruins Room-Art Family Decision

Date: 2026-06-02

## Decision

Accepted-forward for the first non-ruins room-art family rollout proof.

This packet promotes three family-specific client-only prototype plates into
the existing single-image visual-manifest route:

1. `tileset.forest_edge` -> `ui.map_stage.forest_edge.room_plate.pr08_demo`
2. `tileset.mine` -> `ui.map_stage.mine.room_plate.pr08_demo`
3. `tileset.shadow_depths` -> `ui.map_stage.shadow_depths.room_plate.pr08_demo`

The forest-edge and mine results are strong enough to keep as the next family
rollout foundation. The initial shadow-depths plate proved the catalog,
manifest, preload and golden evidence route, but its runtime crop was compressed
by the room shape and right interaction field. The follow-up V3 shadow-depths
crop-fit plate supersedes that initial source as the current accepted-forward
shadow-depths resource foundation. The later V4 right-mass composition
supersedes V3 as the current accepted-forward shadow-depths resource: it adds a
secondary right-side wall return, broken slab lane and restrained violet seams
so the packaged fixed crop no longer reads as an empty grid-first right half.
It still must not be used as all-map or final packaged closure.

## Why This Route

The remaining PR08 map risk after the accepted ruins proof was family overfit:
using one ruins plate everywhere would create a false all-map quality claim.
The selected route adds typed room-art families keyed by the active tileset
materials, so each map family owns its own visual language while unsupported
tilesets still fall back to the tile renderer.

Why not micro-tuning: alpha, grid, seam or fog changes would not solve the
family-overfit problem. The root cause is missing non-ruins art authority, so
the fix belongs in the room-art catalog, visual manifest and family-specific
bitmap resources.

## Generated Art Sources

The candidates were generated with the built-in `image_gen` tool and then
reviewed as bitmap room plates. Generated originals are not a production
contract; the repo-relative review and runtime resources below are the
traceable artifacts for this packet.

Prompt intent:

1. Forest-edge: dark fantasy forest ruin room, mossy stone, roots, amber light,
   authored floor mass, no characters or UI text.
2. Mine: subterranean mine chamber, timber supports, ore glints, dark stone,
   warm lantern pools, no characters or UI text.
3. Shadow-depths: cold violet shadow chamber, black stone, occult depth,
   restrained glow, no characters or UI text.
4. Shadow-depths V2/V3 crop-fit attempts: reduce centered arena symmetry,
   quiet the lower-right play surface, and bias wall mass toward the top/left
   so the fixed `grey_gate_depths` crop reads as environment art under runtime
   overlays.
5. Shadow-depths V4 right-mass composition: preserve the V3 tall crop fit,
   then push a secondary occult wall return, buttress mass and larger broken
   slab rhythm into the right 35%-70% band so runtime grid is no longer the
   dominant right-half structure.

Review assets:

1. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_forest_edge_room_plate_pr08_demo.png`
2. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_mine_room_plate_pr08_demo.png`
3. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_shadow_depths_room_plate_pr08_demo.png`
4. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_shadow_depths_room_plate_pr08_demo_v2.png`
5. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_shadow_depths_room_plate_pr08_demo_v3.png`
6. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/ui_map_stage_shadow_depths_room_plate_pr08_demo_v4-imagegen.png`
7. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/non-ruins-room-art-family-board.png`
8. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/non-ruins-family-runtime-crop-board.png`
9. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/shadow-depths-v1-v2-v3-runtime-crop-board.png`
10. `UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/shadow-depths-v3-v4-runtime-crop-board.png`

Runtime resources:

1. `client/src/main/resources/dark-v1/ui/ui_map_stage_forest_edge_room_plate_pr08_demo.png`
2. `client/src/main/resources/dark-v1/ui/ui_map_stage_mine_room_plate_pr08_demo.png`
3. `client/src/main/resources/dark-v1/ui/ui_map_stage_shadow_depths_room_plate_pr08_demo.png`

The runtime shadow-depths file now uses the V4 right-mass source:

```text
c61f5f7c6f9924acbae795a606b28b7ec697e463ea31a57d120bdf70421a2c46
```

## Runtime And Manifest Integration

Changed contracts stay inside client presentation and resource authority:

1. `DarkUiMapVisualKeys` now exposes typed room-art families and resolves a
   family only when visible map cells contain that family's ground or wall
   material.
2. `RoomArtPlateCatalog` resolves the active family's plate key instead of
   reusing the ruins key.
3. `ClientAssetLoadStrategy` preloads the active family's own plate and keeps
   ruins-only material breakup scoped to `tileset.ruins`.
4. Canonical and runtime visual manifests carry the three new `ui_frame`
   entries with `room_art_plate` tags.
5. The image spec records repo-relative source candidates for the three
   non-ruins plates.

No schema, atlas, sprite-sheet owner contract, gameplay rule, content-pack rule
or save/replay contract changed.

## Golden Evidence

New PR08 director labels:

1. `dark-uiux-pr08-director-forest-map-stage-crop`
2. `dark-uiux-pr08-director-mine-map-stage-crop`
3. `dark-uiux-pr08-director-shadow-depths-map-stage-crop`

Crop artifacts:

1. `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-forest-map-stage-crop.png`
2. `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-mine-map-stage-crop.png`
3. `client/build/reports/golden/dark-uiux-pr08-director/dark-uiux-pr08-director-shadow-depths-map-stage-crop.png`

Evidence hashes are recorded in
`UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/evidence-index.tsv`.

## Packaged Scenario Wiring

The three non-ruins family crops now have sibling packaged validation scenario
ids:

1. `dark-uiux-pr08-director-forest-map-stage`
2. `dark-uiux-pr08-director-mine-map-stage`
3. `dark-uiux-pr08-director-shadow-depths-map-stage`

These scenarios are wired through the typed registry, client presentation
catalog, whitebox materialization catalog, `phase4-v4-scenarios.yaml`, i18n
guide text and CLI tests. Their runtime seed/zone/profession values match the
existing PR08 family golden evidence, and the golden family crops now derive
their runtime parameters and crop labels from the registry scenario evidence
contract to avoid a second authority.

`preparePhase4V4Whitebox` materializes all three scenarios at `1280x800`. The
generated runbooks and expected-evidence packets are recorded in
`evidence-index.tsv`.

The first packaged visual capture pass has now run for all three scenarios.
Full-window, map-stage, right-panel and bottom-deck crops were captured from
the packaged app and are recorded in `UI/manual-records/dark-uiux-pr08-non-ruins-packaged-parity.md`.
The fixed-crop board is:

`UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/packaged-non-ruins-family-map-stage-crop-board.png`

After the V4 right-mass pass, the current fixed-crop board is:

`UI/review/dark-uiux-pr08-exploration/non-ruins-room-art-family-2026-06-02/packaged-non-ruins-family-map-stage-crop-board-v4-shadow.png`

Forest-edge and mine remain accepted-forward as packaged core crop evidence.
Shadow-depths V4 is also accepted-forward as packaged core crop evidence: it
fixes the immediate right-half emptiness blocker by adding secondary room mass
and larger slab rhythm under the runtime grid. It is still weaker than
forest/mine as a final all-map proof because the fixed room shape remains
narrow and grid-readable in the lower-right play lane.

The `show-evidence-summary` action is now captured and logged for all three
packaged scenarios. Forest-edge used the normal packaged interaction path. Mine
and shadow-depths used the constrained
`KTOME_VALIDATION_STARTUP_SURFACE=evidence-summary` startup route after the
local Mac keyboard/Computer Use route proved unreliable for those transient
packaged bundles. This is a summary-surface capture path only; it does not
create a generic arbitrary validation-action injection path.

## Validation

Commands actually run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 8s`, 15 actionable tasks, 6 executed and
9 up-to-date.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 36s`, 15 actionable tasks, 15 executed.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew assetLint styleLint manifestLint resourcePipelineLint :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" maintainabilityLint --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 7s`, 28 actionable tasks, 10 executed and
18 up-to-date. `manifestLint` reported `entries=644`.

After V3 replaced the shadow-depths runtime resource, this command was run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew assetLint styleLint manifestLint resourcePipelineLint :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" maintainabilityLint --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 20s`, 29 actionable tasks, 10 executed and
19 up-to-date. `manifestLint` reported `entries=644`.

After V4 replaced the shadow-depths runtime resource, this command was run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew assetLint styleLint manifestLint resourcePipelineLint :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDrawsAcceptedNonRuinsRoomArtPlateFamiliesWithoutRuinsPlateReuse" --tests "com.ktome.client.render.TileRendererCanvasTest.renderCanvasDoesNotDrawPr08RoomArtPlateForUnsupportedTilesets" :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" maintainabilityLint --no-configuration-cache
```

Result: failed first because only the expected PR08 director golden hashes for
`dark-uiux-pr08-director-shadow-depths-map-stage-crop` and
`dark-uiux-pr08-director-telegraph-combat-crop` were stale. After visual review
and bounded rebaseline to the actual V4 hashes, the same command passed.
`BUILD SUCCESSFUL in 24s`, 29 actionable tasks, 8 executed and 21 up-to-date.

After the packaged sibling scenario wiring was added, these commands were run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:test --tests com.ktome.client.input.ValidationCommandSourceTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 13s`, 25 actionable tasks, 16 executed
and 9 up-to-date.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew localeLint assetLint styleLint manifestLint resourcePipelineLint maintainabilityLint --no-configuration-cache
```

Result: passed after adding explicit room-plate locale labels and static
summary-note keys. `BUILD SUCCESSFUL in 10s`, 27 actionable tasks, 15 executed
and 12 up-to-date.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 14s`, 15 actionable tasks, 3 executed and
12 up-to-date.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-forest-map-stage --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-mine-map-stage --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache
```

Result: all three passed and generated runbook/expected-evidence scaffolding.
The forest run completed in 7s; mine and shadow-depths reused the packaged app
and completed in under 1s each.

After the V4 whitebox text contract replaced the V3 wording, this command was
run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew localeLint :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:packageMacApp preparePhase4V4Whitebox maintainabilityLint -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 10s`, 28 actionable tasks, 10 executed and
18 up-to-date. The regenerated runbook and expected-evidence packet now name
the V4 right-mass room-art plate.

After the packaged evidence-summary startup surface was added, these commands
were run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.screen.ValidationScenarioBootstrapTest --tests "com.ktome.client.GameAppLifecycleTest.validation startup surface opens evidence summary through typed scenario action" :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest maintainabilityLint --no-configuration-cache
```

Result: passed after the startup overlay mode was wired through the validation
command source. The final run reported `BUILD SUCCESSFUL in 17s`; warnings were
limited to the existing deprecated `getFrameBufferPixmap` call sites.
After a whitespace-only Kotlin continuation cleanup and the no-startup-surface
MAP-mode regression test were added, the same command plus that extra
`GameAppLifecycleTest` filter was rerun and reported `BUILD SUCCESSFUL in 7s`,
26 actionable tasks, 3 executed and 23 up-to-date.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-mine-map-stage --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache
```

Result: passed. The mine command reported `BUILD SUCCESSFUL in 7s`; the
shadow-depths command reported `BUILD SUCCESSFUL in 884ms`.

Packaged summary captures were then taken with
`KTOME_VALIDATION_STARTUP_SURFACE=evidence-summary`:

1. Mine summary screenshot:
   `build/whitebox/dark-uiux-pr08-director-mine-map-stage/evidence/dark-uiux-pr08-director-mine-evidence-summary.png`,
   hash `7fa559037e2199507c8e08b915125336e81e87547598bf8a2e25bcf21fcb56f4`.
2. Shadow-depths summary screenshot:
   `build/whitebox/dark-uiux-pr08-director-shadow-depths-map-stage/evidence/dark-uiux-pr08-director-shadow-depths-evidence-summary.png`,
   hash `ba1c14950b967ebe0f6ae893d7aea16d7580a92bc258b02d10e15083464f2e68`.
3. Both app logs contain `evidence_summary_opened` for their scenario action.

## Remaining Risk

1. This is not all-map closure.
2. This includes packaged whitebox scenario wiring, materialization smoke, and
   packaged core crop plus evidence-summary capture for forest-edge, mine and
   shadow-depths.
3. The shadow-depths runtime crop is improved by V4 and now has packaged core
   crop evidence that resolves the immediate empty-right-half blocker. It still
   needs broader topology coverage before final all-map acceptance.
4. The proof covers selected representative zones only; it does not prove every
   future forest, mine or shadow-depths room topology.
5. The worktree contains broader PR08 changes, so this packet validates the
   current dirty-tree state rather than a minimal isolated patch set.

## Next Action

Use this packet as the non-ruins family route foundation, then either:

1. continue shadow-depths topology-specific art/resource work only if the next
   fixed-crop or broader topology evidence shows V4 still fails another
   material blocker, or
2. return to the director-grade goal and choose a higher-return first-screen
   surface only if fixed-crop evidence shows map family closure is no longer
   the dominant blocker.
