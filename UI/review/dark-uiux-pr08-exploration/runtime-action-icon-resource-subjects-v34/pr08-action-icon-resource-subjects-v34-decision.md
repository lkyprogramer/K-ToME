# PR-08 V34 Action Icon Resource Subjects

## Verdict

Accepted-forward only.

V34 follows the V33 renderer-scale pass and replaces the four real Vanguard
starter hotbar resources with subject-first transparent icons. The bottom action
slots no longer depend on baked circular medallion fills; the visible subjects
read as sword, shield, guard shield and charge spear inside the existing runtime
slot chrome.

This is not director-grade closure. The action strip is closer to the reference
because it now has authored subjects instead of circular badges, but the
reference still has cleaner rectangular slot silhouettes, stronger icon
composition and a more resolved bottom-deck material hierarchy.

## Scope

- Runtime resources:
  - `client/src/main/resources/dark-v1/icons/talent_vanguard_power_strike_icon.png`
  - `client/src/main/resources/dark-v1/icons/talent_vanguard_shield_bash_icon.png`
  - `client/src/main/resources/dark-v1/icons/talent_vanguard_guard_stance_icon.png`
  - `client/src/main/resources/dark-v1/icons/talent_vanguard_charge_icon.png`
- Source resources:
  - `assets-src/image/raw/sheets/dark-v1/r08-skills-vanguard-berserker.png`
  - `assets-src/image/contact-sheets/dark-v1/r08-skills-vanguard-berserker-contact.png`
- Reports:
  - `assets-src/image/manifests/dark-v1-pr00-sprite-map-report.jsonl`
  - `assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/assets/ManifestResolveTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/`

No gameplay rules, core visibility, save, replay, profile, schema, manifest key,
manifest schema, content-pack rule, action order or golden expected-hash change.

## Implementation

1. Kept `talent.vanguard.*.icon` as the runtime key authority.
2. Preserved transparent socket corners required by existing hotbar resources.
3. Added a focused alpha-coverage regression that rejects circular medallion
   fills for the four starter hotbar icons.
4. Reused original PR-06 metal/crack texture inside tighter subject masks, so
   the icons remain in the current dark fantasy material language.
5. Updated the r08 source sheet, contact sheet and sprite map hash reports to
   keep generated resource evidence in sync.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-runtime-metrics.json` |
| resource icon preview | `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-resource-icon-preview.png` |
| action crop | `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-v34-action-deck-crop.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-ui-demo-new-bottom-deck-no-command-hints.png` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-ui-demo-new-parity-1672x941.png` |
| action diff | `UI/review/dark-uiux-pr08-exploration/runtime-action-icon-resource-subjects-v34/pr08-action-icon-resource-subjects-v34-v34-v33-action-deck-diff.png` |

Key metrics versus V33:

| Surface | Mean abs RGB diff | Max RGB diff | Changed ratio |
| --- | ---: | ---: | ---: |
| action-deck crop | `2.8436076817558296` | `226/205/183` | `0.08986419753086419` |
| bottom deck crop | `1.0414732420972248` | `226/205/183` | `0.03291282329535178` |
| full screen 1672x941 | `0.15168612406293483` | `235/223/227` | `0.004802326497821212` |
| map-stage crop | `0.0` | `0/0/0` | `0.0` |
| right-panel crop | `0.0` | `0/0/0` | `0.0` |
| nav rail crop | `0.0` | `0/0/0` | `0.0` |

Subject alpha coverage:

| Key | Alpha coverage | Corner max alpha |
| --- | ---: | ---: |
| `talent.vanguard.power_strike.icon` | `0.223876953125` | `0` |
| `talent.vanguard.shield_bash.icon` | `0.3670654296875` | `0` |
| `talent.vanguard.guard_stance.icon` | `0.39764404296875` | `0` |
| `talent.vanguard.charge.icon` | `0.255859375` | `0` |

Directional reference-distance check:

- V33 resized action/reference mean abs diff: `17.994499910538558`
- V34 resized action/reference mean abs diff: `17.468430846305242`

This number is not accepted as a director-grade score because the crop shapes
and scene content differ.

Key artifact file hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `a91e61acd6a3ea2a68fa712fc98f86f2dcb6dfce3f33be095cba6f32d85f56a4` |
| resource icon preview | `05a6c8c323283f98d057700fdfa1723ce842e597ae32fa8cb51a2b5f97a131ea` |
| full screen | `b55796680024e4f51b157d8962d796d3c2835f0ebc8f1892c7c6d655d1c4f4d4` |
| bottom deck crop | `7148faaae9bb8935aa4876c94d83160762bec1bf17f1950ee19bde2bb400e729` |
| action crop | `b3572b8f8f1fb66bbd763c88731b73b39e7930ab94f0bcef12de03b9cc83bcdb` |
| action diff | `c56877a6bd91dc431f4ec4531cd9e405c1782c6845558a1d4f2c22cae93a42f6` |

## Validation

RED, expected failure before resource change:

```bash
./gradlew :client:test --tests 'com.ktome.client.assets.ManifestResolveTest.vanguard starter hotbar icons are subject first without circular medallion fill'
```

Failure evidence: `talent.vanguard.power_strike.icon` had
`alphaCoverage=0.4703369140625`, above the accepted subject-first range.

Focused GREEN after resource changes:

```bash
./gradlew :client:test --tests 'com.ktome.client.assets.ManifestResolveTest.vanguard starter hotbar icons keep transparent socket background' --tests 'com.ktome.client.assets.ManifestResolveTest.vanguard starter hotbar icons are subject first without circular medallion fill'
```

Result: `BUILD SUCCESSFUL in 3s`.

Resource gates:

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint resourcePipelineLint
./gradlew spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-2-sprite-map-report.jsonl
```

Result: both `BUILD SUCCESSFUL`.

Broader focused verification:

```bash
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint
```

Result: `BUILD SUCCESSFUL in 20s`. Three render/audio smoke subtests were
skipped by their existing environment assumptions.

Focused golden evidence:

```bash
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

Result: expected failure because PR02-1 hashes changed and golden baselines
remain intentionally unchanged. Actual V34 golden hashes:

```text
ui-demo-new-parity-1672x941=f54b9619d359aa82411f7d7073e2e4582e7ef27d8e8637bd3ce81ebd127f27a9
ui-demo-new-parity-1280x800=0674c3de7cd177a1ac64251d44581e8e817993f7106baa2ddaae75e6c7cfbb98
ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8
ui-demo-new-bottom-deck-no-command-hints=9a14399183d5593fd2df8e2afddd3c06b395e7f6d23862d0ac16a6abd1bcf620
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7
ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd
ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a
```

## Manual Review

V34 is accepted-forward because it executes the next action from V33: replace
the circular starter hotbar badge resources with subject-first authored icons
while keeping the existing action model and slot chrome.

Remaining director blockers:

1. slot rectangles and spacing still lag the cleaner reference action bar
2. bottom-deck log and hero areas still do not share one fully authored visual
   grammar
3. action icon subjects are stronger, but not yet at final illustration quality
4. map-stage director blockers remain unchanged from previous passes

## Next Action

Do not continue by only re-running the same icon mask pass. The next useful
bottom pass should be action-slot silhouette/chrome resource work or a stronger
bottom-deck composition pass. If the first-screen map returns as the dominant
gap, switch back to map-stage composition or authored wall/floor resource work.
