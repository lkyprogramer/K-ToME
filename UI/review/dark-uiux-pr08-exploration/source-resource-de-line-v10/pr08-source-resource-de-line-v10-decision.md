# PR-08 Source Resource De-Line V10 Decision

Date: 2026-05-28

## Objective

Follow the V09 conclusion by testing source-resource removal of repeated
dark-line authority before adding a new room-scale resource key or renderer
contract.

This loop keeps the existing `r02-ui-demo-ruins-tiles` sheet, visual keys,
manifest schema and owner rows. It does not add new keys or runtime authority.

## Evidence

Evidence root:
`UI/review/dark-uiux-pr08-exploration/source-resource-de-line-v10/`

Generated candidates:

1. `r02-ui-demo-ruins-tiles-v10a-source-de-line.png`
2. `pr08-source-resource-de-line-v10a-contact-board.png`
3. `r02-ui-demo-ruins-tiles-v10b-edge-lift.png`
4. `pr08-source-resource-de-line-v10b-contact-board.png`

Runtime evidence:

1. `runtime-v10b-edge-lift/ui-demo-new-map-stage-crop.png`
2. `runtime-v10b-edge-lift/ui-demo-new-parity-1672x941.png`
3. `runtime-v10b-edge-lift/ui-demo-new-parity-1280x800.png`
4. `pr08-source-resource-de-line-v10b-runtime-comparison-board.png`
5. `pr08-source-resource-de-line-v10b-runtime-metrics.json`

## Candidate Verdict

Reject V10a before runtime. It reduced source wall edge darkness, but the
contact board showed side/corner/door pieces turning into rectangular collage
surfaces. That would create a new art-quality problem instead of removing the
old lattice problem.

Accept V10b as narrow forward polish only. It performs source edge-lift and
opacity normalization on the existing floor/wall family without new manifest
keys. Runtime review shows a small reduction in wall black-frame harshness and
dark-line authority, but the map still reads grid-first relative to
`UI/UI-demo-new.png`.

Do not update golden baselines from this state.

## Runtime Metrics

The local edge proxy is same-script comparison only.

| Candidate | Mean edge | P90 edge | Verdict |
| --- | ---: | ---: | --- |
| W05i baseline | 1.873 | 5 | prior accepted-forward baseline |
| V09 rejected | 1.866 | 5 | rejected renderer-side line bridge |
| V10b runtime | 1.850 | 5 | accepted-forward source polish |

## Key Hashes

Retained source/resource hashes:

1. `r02-ui-demo-ruins-tiles.png=8859e945db78e52ab184ab3305bfb014185e9d10103722160fa170953f01a9da`
2. `r02-ui-demo-ruins-tiles-contact.png=baa2d8359f8dcaebeb9573ebbd9423169dd4fc7da4a10bc07d13aa2de6cf5a17`
3. `tileset_ruins_ground_01.png=5b8b306f51c67de9ed608b7c0dd3c43c0e32abe8f9577065b52f110cfea69859`
4. `tileset_ruins_ground_01_variant_1.png=f6539cca008602a366e288ad3683ef77a99cd6d4de7685892a2db92795617652`
5. `tileset_ruins_ground_01_variant_2.png=c5fd62a67052f4e25c59b4fecd04e68549cd8067c5a858bdb01e95d2ac898b27`
6. `tileset_ruins_ground_01_variant_3.png=b954133886130f3fdda4f15bf92a49a23a5ff3ec448320fecf73dbaed34a3c92`
7. `tileset_ruins_wall_01.png=488e233e905d9b0b2bf9559d533a9f2bd3d7d7a5b57d9367be6b917840eea511`
8. `tileset_ruins_wall_01_crown.png=8afa0de2ec7213466d9d4b40258e46596851e25708869a89a0871007730abab1`
9. `tileset_ruins_wall_01_side.png=5e9d0d96c0a16cfad5d058854db50ba745797ebbed02cd4f7d4bd77d0d5f84c0`
10. `tileset_ruins_wall_01_corner.png=2a692a3e43c070943758ee442c418226bad6bf689617b2eb817205c8eed9a346`
11. `tileset_ruins_wall_01_door_contact.png=dc137347e66729efc95919969f4db633cfaeb5e57bfc8d7b9659ecff41ddba36`

Golden actual hashes from retained V10b expected failure:

1. `ui-demo-new-parity-1672x941=be6251ab76a189cee18b8e1b1858b627aea3f8b87687fc66132038580de7b04c`
2. `ui-demo-new-parity-1280x800=92e29e4eaa5271b83bc1c542d1495e4b7a94b3be6149a498762efb58f68711cb`
3. `ui-demo-new-map-stage-crop=51ad2d55186156e6296432235a8a4fc76a65db14a9f3ebd8a4393fb37452dbd6`

Retained evidence hashes:

1. `runtime-v10b-edge-lift/ui-demo-new-map-stage-crop.png=995128e604a2c6433087c34dd61da4435cf94279a29bf4c7b78b169df49f96c2`
2. `pr08-source-resource-de-line-v10b-runtime-comparison-board.png=608976182e3e290883a6c1e9ecda8d151eab493ecabd54cf70501d7e15949c8d`

## Commands Run

1. local Python/PIL candidate generation under the evidence folder.
2. `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
3. `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
4. `python3 scripts/generate_dark_final_full_inventory.py`
5. `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
6. `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`
7. `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`

## Result

PASS: sprite slicing, contact-sheet generation and final inventory.

PASS: PR-08 resource lint chain and filtered sprite-map report.

PASS: focused client tests plus `maintainabilityLint`.

EXPECTED FAIL: PR-02-1 golden hash gate failed because runtime visuals changed
and baselines remain intentionally unchanged.

## Next Direction

V10b is not director-grade closure. It proves that source-level edge authority
can move the crop slightly, but the map still lacks the room-scale authored
stone hierarchy visible in the reference.

The next cut should stop editing per-tile edges and instead route an explicit
non-grid-aligned room-scale material-breakup resource contract, or generate a
stronger floor/wall family where the room reads as large authored masonry before
the tile lattice is noticed.
