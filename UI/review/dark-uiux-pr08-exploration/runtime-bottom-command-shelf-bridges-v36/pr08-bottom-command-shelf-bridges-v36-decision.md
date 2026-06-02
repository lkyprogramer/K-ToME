# PR-08 V36 Bottom Command Shelf Bridges

## Verdict

Accepted-forward only.

V36 follows the V35 socket pass and targets the larger bottom-deck composition
gap. The renderer now draws wide forged bridge plates and small horizontal rungs
in the two hero/action/log gutters, so the bottom HUD reads more like one
assembled command shelf instead of three adjacent cards separated by empty
vertical cuts.

This is not director-grade closure. The pass improves physical cohesion in the
bottom strip, but the full comparison still shows the runtime bottom deck below
`UI/UI-demo-new.png` in overall authored composition, slot proportions and
hero/log/action unity.

## Scope

- Production:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/`

No gameplay rules, core visibility, save, replay, profile, schema, manifest key,
manifest schema, content-pack rule, action order, resource file or golden
expected-hash change.

## Implementation

1. Reused the existing bottom-deck layout as the only geometry authority.
2. Added `drawBottomHudBridgePlates` under `drawBottomHudCohesionRails`.
3. Drew one forged bridge plate in each hero/action and action/log gutter.
4. Added small brass rungs and restrained side edges to make each bridge read as
   a mechanical join, not a new content panel.
5. Kept all hero, action and log text/input/content ownership unchanged.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-runtime-metrics.json` |
| bridge closeup | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-v36-bottom-bridge-closeup.png` |
| action crop | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-v36-action-deck-crop.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-ui-demo-new-bottom-deck-no-command-hints.png` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-ui-demo-new-parity-1672x941.png` |
| action diff | `UI/review/dark-uiux-pr08-exploration/runtime-bottom-command-shelf-bridges-v36/pr08-bottom-command-shelf-bridges-v36-v36-v35-action-deck-diff.png` |

Key metrics versus V35:

| Surface | Mean abs RGB diff | Max RGB diff | Changed ratio |
| --- | ---: | ---: | ---: |
| action-deck crop | `0.15433196159122084` | `46/32/23` | `0.04869958847736625` |
| bottom deck crop | `0.1108337854946645` | `46/33/30` | `0.0350244167118828` |
| full screen 1672x941 | `0.016572843627279` | `45/30/31` | `0.005114875755711373` |
| map-stage crop | `0.0` | `0/0/0` | `0.0` |
| right-panel crop | `0.0` | `0/0/0` | `0.0` |
| nav rail crop | `0.0` | `0/0/0` | `0.0` |

Directional reference-distance check:

- V35 resized action/reference mean abs diff: `17.466652352835926`
- V36 resized action/reference mean abs diff: `17.41089282519234`

This number is not accepted as a director-grade score because the crop shapes
and scene content differ.

Key artifact file hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `45904dd14a17ee128b6fca5a63db9848d6dc59de421e290cde1d065ce101498d` |
| bridge closeup | `af1dfc46af2777b2880aa2e9ce8fc22fd7163b6d1a0ca989328769705c0ecdfd` |
| full screen | `3d5bb0c4c7e904c166e30afadc821d13c9f1ee184cfa2a5c8ec26829494b08ba` |
| bottom deck crop | `f9b22db39ff64fb45768d66feccf6d6bf69ae0a69cdc77ec2432f092be429907` |
| action crop | `3a3cc4488a4fdc605bf366171a996eb9468efc7ae390970087cb0c1d11326f14` |
| action diff | `8637fb573b888a7812c1f4a6d26fd9a7b2bf2d53fcd463a348f427dc71867636` |

## Validation

RED, expected failure before production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom hud uses inter panel bridge plates for one command shelf'
```

Failure evidence: inter-panel bridge plates were `0/2`, proving the test
targeted the missing bottom HUD composition join.

Focused GREEN after production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom hud uses inter panel bridge plates for one command shelf'
```

Result: `BUILD SUCCESSFUL in 4s`.

Broader focused verification:

```bash
./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.DemoShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest :client:clientSmoke maintainabilityLint
```

Result: `BUILD SUCCESSFUL in 19s`. Three render/audio smoke subtests were
skipped by their existing environment assumptions.

Focused golden evidence:

```bash
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'
```

Result: expected failure because PR02-1 hashes changed and golden baselines
remain intentionally unchanged. Actual V36 golden hashes:

```text
ui-demo-new-parity-1672x941=586c7fdd8cf3a57557edfff4905ba3c5dfaca9979e1b0d9d28d72df080c31ba9
ui-demo-new-parity-1280x800=aa0c249be84b5e2d81b634a8591cf67c32c49019870929c134ef9dc37236db6c
ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8
ui-demo-new-bottom-deck-no-command-hints=9a64b9de43f0d8526d48e2bd5e9705c266599d3002c9b924305f7c2bf5aacdcd
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7
ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd
ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a
```

## Manual Review

V36 is accepted-forward because it uses a different composition technique than
V35: broader inter-panel bridge plates instead of another socket treatment.

Remaining director blockers:

1. bottom HUD still needs a stronger authored composition across hero, action
   and log, not only bridge treatment in gutters
2. action slot proportions and icon illustration still lag the reference
3. log deck and hero card still feel like locally improved panels rather than a
   single final art-directed HUD object
4. map-stage director blockers remain unchanged

## Next Action

Do not continue by only adding more bridge plates or tuning bridge alpha. The
next useful pass should be a larger bottom-deck composition reset, authored
hero/log/action unification, or a return to map-stage composition/resource work
if the first-screen map again dominates the gap.
