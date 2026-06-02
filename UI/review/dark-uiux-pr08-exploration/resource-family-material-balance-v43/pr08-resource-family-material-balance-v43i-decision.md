# PR-08 Resource Family Material Balance V43i Decision

> Date: 2026-05-29
> Status: `accept-forward-not-final`
> Scope: `tileset.ruins.ground_01` floor family, `tileset.ruins.wall_01` wall family, PR02-1 golden evidence

## Direct Conclusion

V43i is accepted as a narrow resource-quality step, not director-grade closure.

The retained change keeps the V42 floor/wall forms, then applies only minimal
edge-value and microtexture correction:

1. floor outer-ring luminance no longer creates the previous bright repeated
   frame;
2. floor outer 2px retains enough microtexture to avoid a low-detail tile
   border;
3. wall-family pieces gain subtle painterly color variation without changing
   manifest keys, schema, terrain authority or runtime layer ordering.

## Candidate Verdict

| Candidate | Verdict | Reason |
| --- | --- | --- |
| V43a | rejected | Resource metrics passed, but runtime showed long bright cracks and wall striping that amplified grid read. |
| V43c | rejected | The regenerated cells became too vector-like; floor cracks and wall blocks read as hard symbols rather than hand-authored stone. |
| V43f | rejected | Tests passed after regrain, but runtime repeated crack/dot motifs every cell and worsened the first read. |
| V43i | accepted-forward | Keeps the stronger V42 material identity while fixing measurable floor edge value drift and wall color-complexity gaps. |

## Evidence

| Evidence | Path |
| --- | --- |
| Decision | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-decision.md` |
| Comparison board | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-comparison-board.png` |
| Runtime metrics | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-runtime-metrics.json` |
| Map-stage crop | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-ui-demo-new-map-stage-crop.png` |
| Full-screen 1672x941 | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-ui-demo-new-parity-1672x941.png` |
| Full-screen 1280x800 | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-ui-demo-new-parity-1280x800.png` |
| Contact sheet | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-r02-contact.png` |
| Raw sheet archive | `UI/review/dark-uiux-pr08-exploration/resource-family-material-balance-v43/pr08-resource-family-material-balance-v43i-r02-raw.png` |

## Metrics

| Metric | Value |
| --- | --- |
| V43i vs V42 map changed ratio | `0.13658118183734883` |
| V43i vs V42 map mean absolute RGB diff | `0.14303039886532556` |
| V42 resized map/reference mean absolute RGB diff | `23.694325609028123` |
| V43i resized map/reference mean absolute RGB diff | `23.661567126032903` |

## Golden Hashes

| Artifact | SHA-256 |
| --- | --- |
| `ui-demo-new-parity-1672x941` | `c11e36a97750e71ef478e43e8de3e3f521876c77a577928db5bf49e55e062768` |
| `ui-demo-new-parity-1280x800` | `b90cabf9a34b5f437912990f379509e15f3343562917d0eb0bbb3cf07ed04cd9` |
| `ui-demo-new-map-stage-crop` | `dadb4615be9c7fc23214b12d28df8e489534ff9f9b3764c631133e4674925b3b` |
| `ui-demo-new-right-panel-grid` | `4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8` |
| `ui-demo-new-bottom-deck-no-command-hints` | `9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd` |
| `ui-demo-new-nav-rail-crop` | `c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6` |
| `ui-demo-new-inventory-page-1` | `23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd` |
| `ui-demo-new-inventory-page-2` | `63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a` |

## Validation

1. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:test --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 ruins floor family keeps material detail without bright tile-edge seams' --tests 'com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest.pr08 ruins wall family keeps dense masonry material in every authored piece'`
2. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
3. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:darkSpriteSheetLint :tools:spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles :tools:resourcePipelineLint :tools:darkManifestCoveragePr08OwnerScope`
4. `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRenderModelTerrainVariantTest maintainabilityLint`

All four validation commands passed.

## Remaining Gap

V43i still does not close the director-grade gap. The map remains more
grid-led and less room-authored than `UI/UI-demo-new.png`. The next meaningful
move should not be another single-cell floor/wall reroll; it should target
room-scale composition, stronger wall/floor structural authority, or UI density
outside the map if map resource iteration keeps producing small deltas.
