# PR-08 V35 Action Slot Silhouette Chrome

## Verdict

Accepted-forward only.

V35 follows the V34 resource-icon pass and targets the remaining slot silhouette
gap. Filled bottom action slots now draw a low-alpha rectangular hollow socket
stage behind the icon subject, with restrained brass lips and paired iron side
jambs. The action icon area reads less like a small square pad floating above
labels and more like a crafted command bay inside the shared bottom console.

This is not director-grade closure. The visible delta is intentionally subtle
and the comparison board still shows weaker bottom-deck composition than
`UI/UI-demo-new.png`. The pass improves material hierarchy but does not solve
overall action strip proportions, log/hero/action cohesion, or final
illustration quality.

## Scope

- Production:
  - `client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt`
- Tests:
  - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
- Evidence:
  - `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/`

No gameplay rules, core visibility, save, replay, profile, schema, manifest key,
manifest schema, content-pack rule, action order, resource file or golden
expected-hash change.

## Implementation

1. Kept `ActionPanelEntryModel.icon` as the only action-icon authority.
2. Added `drawActionSlotSilhouetteChrome` inside the existing bottom action
   renderer for filled slots only.
3. Drew a low-alpha rectangular socket stage behind the icon subject.
4. Added two brass lips plus paired iron side jambs to make the icon area read
   as a command recess.
5. Kept labels on the shared command plinth and did not add full-height
   independent slot wells.

## Runtime Evidence

| Artifact | Path |
| --- | --- |
| comparison board | `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-comparison-board.png` |
| runtime metrics | `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-runtime-metrics.json` |
| slot closeup | `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-v35-slot-closeup.png` |
| action crop | `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-v35-action-deck-crop.png` |
| bottom deck crop | `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-ui-demo-new-bottom-deck-no-command-hints.png` |
| full screen | `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-ui-demo-new-parity-1672x941.png` |
| action diff | `UI/review/dark-uiux-pr08-exploration/runtime-action-slot-silhouette-chrome-v35/pr08-action-slot-silhouette-chrome-v35-v35-v34-action-deck-diff.png` |

Key metrics versus V34:

| Surface | Mean abs RGB diff | Max RGB diff | Changed ratio |
| --- | ---: | ---: | ---: |
| action-deck crop | `0.04759122085048011` | `4/3/2` | `0.036905349794238686` |
| bottom deck crop | see metrics JSON | see metrics JSON | see metrics JSON |
| map-stage crop | `0.0` | `0/0/0` | `0.0` |
| right-panel crop | `0.0` | `0/0/0` | `0.0` |
| nav rail crop | `0.0` | `0/0/0` | `0.0` |

Directional reference-distance check:

- V34 resized action/reference mean abs diff: `17.468430846305242`
- V35 resized action/reference mean abs diff: `17.466652352835926`

This number is not accepted as a director-grade score because the crop shapes
and scene content differ.

Key artifact file hashes:

| Artifact | sha256 |
| --- | --- |
| comparison board | `33c429e7304e2566ea1c69612a691a7de57fc2f46f87c66470715505501bfe4b` |
| slot closeup | `87c558abc3526060ede732da75c7db0f03a0e26815a7f0958f99f305356bc46b` |
| full screen | `ba96f87a0bc8de949bfceeea683b8266c08c3e71df4e4bc7bd39d6100f560e6c` |
| bottom deck crop | `a4e225962e494fc62f3a6a9eda14f5fdf77461ab1d3a0ea68e8a564b7a51ca76` |
| action crop | `0ed591b36100ef661c43ad13f2b2faab58c32bc85c067a928cb804e66cacd2f3` |
| action diff | `f540b3320c29ad39d486b0c2aed87922faa77eb270afcc00feb8d2a9f0a2015b` |

## Validation

RED, expected failure before production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom action slots use rectangular hollow command sockets'
```

Failure evidence: filled action slots had `0/4` wide rectangular hollow socket
backings, proving the test targeted the missing slot silhouette rather than an
incidental render detail.

Focused GREEN after production change:

```bash
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest.bottom action slots use rectangular hollow command sockets'
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
remain intentionally unchanged. Actual V35 golden hashes:

```text
ui-demo-new-parity-1672x941=d3ee84c1c31c1a165e6a140013988e236316d97bc2e98f39171aea45462e355b
ui-demo-new-parity-1280x800=98f7df053ce9bf19bc6c90912d4c1b608f77cc042793446675c4530e0f091da4
ui-demo-new-right-panel-grid=4fc73bb307d123267bef293e925fd0f1449b4dd3437b92ffc315d3f9ada6c5f8
ui-demo-new-bottom-deck-no-command-hints=beaf0b35e8230dac1bc2c6981ac61cb3126febcc43a096b91bd777160a79d170
ui-demo-new-nav-rail-crop=c253dc7d8e1aca962a973415973c6a1d9cc739afd704d943a0c28e678f3419d6
ui-demo-new-map-stage-crop=8a21f9a42b0045398a44324580798afed351e22e6d1484f3187055c6874a79d7
ui-demo-new-inventory-page-1=23905f61e56675bd4736eb0a9f47a200012d9899584e1be5c1b613c84952b8bd
ui-demo-new-inventory-page-2=63bbaecf273cd14cbdea001954c8d4a50691e29207140bf3067332e18d9cc94a
```

## Manual Review

V35 is accepted-forward because it addresses the specific V34 blocker:
action slots needed a cleaner rectangular hollow command socket, not just
larger or better icon subjects.

Remaining director blockers:

1. bottom deck still has three-panel composition rather than a fully authored
   unified command strip
2. action slot proportions and spacing are still less resolved than the
   reference
3. log and hero card material language still compete with the action deck
4. map-stage director blockers remain unchanged

## Next Action

Do not keep tuning these socket alphas. The next useful pass should be a larger
bottom-deck composition move, authored log/hero/action unification, or a return
to map-stage composition/resource work if the first-screen map becomes the
dominant blocker again.
