# PR-08 Room Material Authority V13

## Goal

Replace the V12 `tileset.ruins.room_breakup_01` art with a stronger
room-scale material overlay while keeping the same PR-08 visual key, sheet cell,
manifest entry and compositor draw path. The candidate must make the floor read
less like a repeated square lattice without becoming a fog wash or a baked
full-screen paintover.

## Change

Accepted production wiring:

1. Kept `tileset.ruins.room_breakup_01` as the only room material-breakup key.
2. Replaced its `r02-ui-demo-ruins-tiles` source cell with V13 M04
   `broken-field`.
3. Re-sliced runtime PNGs, regenerated the r02 contact sheet, final-full
   inventory and PR-08 sprite-map report.
4. Kept the V12 renderer draw path unchanged: one room-scale asset in
   `MAP_ROOM_COMPOSITOR`, below actors, loot, telegraph, selection and cursor.

No manifest key, manifest schema, atlas schema, save/replay/profile, `core`,
`game`, content-pack or golden baseline change was introduced.

## Candidate Notes

| Candidate | Verdict | Reason |
| --- | --- | --- |
| M01 woven-slab | rejected | useful stone mass probe, but weaker first-read material than M04 |
| M02 ash-masonry | rejected | safer than M03, but too close to broad fog bands at floor-repeat scale |
| M03 fractured-rake | rejected | strong material read, but bright diagonal cracks started reading as overlay ribbons in runtime |
| M04 broken-field | accepted-forward | strongest balance of room-scale stone fields, muted cracks and marker readability |

## Evidence

Artifacts:

| Artifact | Path |
| --- | --- |
| Candidate board | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-breakup-v13-candidate-board.png` |
| Runtime archive | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/runtime-v13-m04-broken-field/` |
| Runtime comparison board | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-material-authority-v13-runtime-comparison-board.png` |
| Runtime metrics | `UI/review/dark-uiux-pr08-exploration/room-material-authority-v13/pr08-room-material-authority-v13-runtime-metrics.json` |

Hashes:

| Item | SHA-256 |
| --- | --- |
| V13 M04 runtime room-breakup PNG | `54e230d753d06a84e08df0eb91e2df9605416c1fcc115bc1eaa92ee6414e4967` |
| retained r02 raw sheet | `496adf577bea24aff14e4b60209c208db27e6700ced2184a7e251905abc6b804` |
| retained r02 contact sheet | `e9c91ce264fa750814b70675d3b0b2dcf34284f4ad67afa27003dc08a86a4360` |
| PR-08 sprite-map report | `d3cd925806cc687c6dbaf679b0c31eae8bb5efb1f5c34252a118801728d39080` |
| V13 runtime map crop artifact | `8a313cbeee0b7631727d6ab03d974d20ec431006259a99a972bb5427f40f126a` |
| V13 runtime comparison board | `44a57457fc3b2f303f4e1534e75e3c2f15353eadd6c8ac97b9c21edc5a27c6b4` |

Golden content hash from the retained expected failure:

`ui-demo-new-map-stage-crop=f6657c666e7a3c5cab9cc9e4aaea2966ef6b819d1d3e76581a0cfb80620cbb8a`

## Validation

Passed:

1. `python3 scripts/slice_spritesheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
2. `python3 scripts/render_contact_sheet.py --overwrite --plan UI/sprite-sheets/sheet-plan.yaml`
3. `python3 scripts/generate_dark_final_full_inventory.py`
4. `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoveragePr08OwnerScope assetLint styleLint manifestLint resourcePipelineLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
5. `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellRendererTest maintainabilityLint`

Expected failure:

1. `./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts' --rerun-tasks`

The golden failure is retained as evidence because PR-02-1 baselines were not
updated.

Not run:

1. `verifyChanged` and packaged whitebox, because this is not PR-08 closure.

## Verdict

Accept V13 M04 forward as a stronger room material resource, not as
director-grade closure.

It improves the room-scale material read relative to V12 without adding a new
contract or hiding gameplay markers. The crop still fails the director rubric:
the visible room continues to read as a coarse square lattice before it reads as
a carved, authored dungeon room. The reference crop still has materially better
wall thickness, local darkness, floor detail and room enclosure.

## Next

Do not continue tuning `tileset.ruins.room_breakup_01` through more alpha,
draw-order or same-shape overlay variants. The next cut should either:

1. directly demote the tactical/grid-line authority at the renderer/source
   boundary while keeping markers readable, or
2. stop map iteration temporarily and start the right-panel/bottom-deck chrome
   sprint if the map blocker is no longer the highest-leverage first-screen
   gap.
