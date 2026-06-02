# PR-08 Room Enclosure And Wall Lattice V07 Decision

> Date: 2026-05-28
> Status: `w05i-accept-forward-not-final`
> Scope: wall/grid interaction after floor V06e

## Direct Conclusion

W05i `lr_strong_opaque` is accepted only as a narrow forward wall-resource
polish. It softens left/right wall-piece edge rhythm without changing alpha,
manifest schema, atlas schema, runtime snapshot contracts or golden baselines.

This is not director-grade closure. The runtime crop still reads grid-first
relative to `UI/UI-demo-new.png`; W05i only reduces a small part of the wall-run
seam problem.

## Candidate Verdict

| Candidate | Verdict | Reason |
| --- | --- | --- |
| V07a wall bleed 5 | rejected | stronger wall contact also enlarged repeated wall-tile vertical seams |
| V07 prototype center/collar planes | rejected | rectangular post-process treatments made the non-rectangular room read like blocky fog |
| W05a-c edge + mass bands | rejected | added obvious black resource bands in tiling preview |
| W05d-f lr candidates | rejected and removed | local generation bug wrote translucent pixels into opaque wall resources |
| W05g/h opaque lr candidates | rejected | safe but visually too subtle compared with W05i |
| W05i opaque lr strong | accept-forward | best current wall edge unification without alpha pollution or black-frame artifacts |

## Runtime Decision

The retained runtime state is:

1. W05i wall family content in `r02-ui-demo-ruins-tiles`.
2. No renderer wall-bleed change retained.
3. No new manifest key, schema, atlas contract, save/replay/profile field or
   `core` / `game` terrain snapshot change.
4. Golden baselines remain intentionally unchanged.

The next useful iteration should not be another wall edge-only polish. The
remaining gap needs either a stronger authored room-scale decal/AO resource
contract or a more structural wall/floor interaction model that reduces the
floor grid as first read while preserving actor, loot and telegraph clarity.

## Evidence

| Evidence | Path |
| --- | --- |
| V07 prototype board | `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/pr08-room-enclosure-lattice-v07-prototype-board.png` |
| rejected wall-bleed runtime board | `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/pr08-room-enclosure-lattice-v07a-runtime-comparison-board.png` |
| W05 initial tiling board | `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/wall-edge-unify-w05/pr08-wall-edge-unify-w05-tiling-board.png` |
| W05 opaque candidate board | `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/wall-edge-unify-w05/pr08-wall-edge-unify-w05g-i-run-board.png` |
| retained W05i runtime board | `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/pr08-room-enclosure-lattice-w05i-runtime-comparison-board.png` |
| retained W05i runtime archive | `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/runtime-w05i-wall-edge-unify/` |
| rejected V07a runtime archive | `UI/review/dark-uiux-pr08-exploration/room-enclosure-lattice-v07/runtime-v07a-wall-bleed5-rejected/` |

## Artifact Hashes

| Artifact | SHA-256 |
| --- | --- |
| retained W05i runtime comparison board | `ca9c2d6552af923a09526c0d6013ac0233a633cbb7b70b9ffd69afa04685482a` |
| W05 opaque candidate board | `825e76f30ea1bf04f1050780257566ba36b085beb63634dce6691a5cb24447fe` |
| rejected V07a runtime comparison board | `a4a2f1202ab8c63360e3260c9ce5c53259266e75c37ad08495c96c2be53a9a77` |
| retained W05i runtime map crop artifact | `c89d6142f7796fc1a9fab4d655e5868a08af18433a27525417cc6b8b9179b4ab` |
| retained W05i runtime full screen artifact | `ad22913bd4ccb399c899ce5084f1c2dd898faed8b77251c5f3272562ffa1d22d` |
| `tileset_ruins_wall_01.png` | `52081fed3a1478343e1f1e5a42e0c8cb113caa28e350c6a27d9afe8735f76212` |
| `tileset_ruins_wall_01_crown.png` | `d4edb9a81c8c3917ed7a2ca99d07646e375a1572c0ff7990378f7584a60496d7` |
| `tileset_ruins_wall_01_side.png` | `6893df8dc013e92e834a4161959223576e9ae92dd9db0fc231252959a2d4a83a` |
| `tileset_ruins_wall_01_corner.png` | `3cc0e3f3c1e0da7dc5d28f58d8138fe2f5a5863bdc0873c2870fcbe0318cd365` |
| `tileset_ruins_wall_01_door_contact.png` | `abc31dca258d47a9932941e5a41346949a4be92c00b6425d949a78b00aa7f430` |

## Validation

Commands run:

```bash
python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml
python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml
python3 scripts/generate_dark_final_full_inventory.py
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint
```

Result:

1. PASS: slicing, contact sheet generation and final full inventory.
2. PASS: resource lint chain and PR-08 filtered sprite-map report.
3. PASS: focused client tests plus `maintainabilityLint`.
4. EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals
   changed and baselines remain intentionally unchanged.

Focused golden actual hashes from the retained W05i expected failure:

```text
ui-demo-new-parity-1672x941=e523fadb506e92b0fc43f30658e7989665b8bd511abef1d33911b1ce7b2398c0
ui-demo-new-parity-1280x800=b796b84b515cc041bd9d5322fcfdfb0f5bfb1aa8c55824b9bbe0086018b3950d
ui-demo-new-right-panel-grid=b96f246cdb9ffb8a2af026d856f8efaaf5911a46ff0bde56d8693ab2451f0ae8
ui-demo-new-bottom-deck-no-command-hints=bd25097dd6ae08004c28d3964c09bba46f64ca8fc002630d548fdbc39a8e9453
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=0ad2681b3a07bda056083643637924f550cc9d21fb0d95143587374310ca7806
ui-demo-new-inventory-page-1=2be674b1b1cee578f3e38baaeb57b5d8affed53074e51ef233039c8b419172b8
ui-demo-new-inventory-page-2=b859958fb6082bb610ae43cb4103e47fe63de43b45f612e294c4d275f15f53e7
```
