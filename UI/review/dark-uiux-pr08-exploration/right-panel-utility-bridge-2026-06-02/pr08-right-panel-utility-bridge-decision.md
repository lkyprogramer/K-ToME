# PR08 Right Panel Utility Bridge Decision

> Date: 2026-06-02
> Verdict: `pr08-right-panel-utility-bridge-accepted-forward`
> Scope: bounded right-panel shell polish after bottom-strip final-read

## Decision

Accept the right-panel utility bridge pass forward only.

The right panel's backpack, page readout and operation hints now read more like
one lower-right utility surface: the backpack page row sits in a forged cradle,
and paired short posts connect it to the operation command dock. This addresses
the fixed-crop issue where the backpack pager and operation hint area still
looked like separate stacked cards after the previous command-surface pass.

## Kept Contracts

1. No `core` or `game` changes.
2. No layout capacity, input, command text, localization, item semantics,
   resource key, manifest or schema changes.
3. The change stays inside `DemoShellRenderer` and existing
   `DemoRightPanelLayout` geometry.
4. The operation hint key chips remain runtime text and retain caption
   hierarchy.

## Evidence

Artifacts:

1. `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/right-panel-utility-bridge-before-after-board.png`
2. `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-parity-1672x941.png`
3. `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-right-panel-crop.png`
4. `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-map-stage-crop.png`
5. `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/dark-uiux-pr08-director-bottom-deck-crop.png`
6. `UI/review/dark-uiux-pr08-exploration/right-panel-utility-bridge-2026-06-02/evidence-index.tsv`

Current PR08 evidence hashes:

| Label | Hash |
| --- | --- |
| `dark-uiux-pr08-director-parity-1672x941` | `e7f65d0f2f205e238909ec1cd9f54387a9eaf365912e1da5710c9df9e72ed936` |
| `dark-uiux-pr08-director-map-stage-crop` | `84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676` |
| `dark-uiux-pr08-director-right-panel-crop` | `201645bf13f9824fd95289063b92ea8adc722f9d0f344aca75902a2e99c26c47` |
| `dark-uiux-pr08-director-bottom-deck-crop` | `a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd` |
| `dark-uiux-pr08-director-telegraph-combat-crop` | `b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321` |

## Commands

All Gradle commands were run after:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env
```

Commands actually run:

```bash
./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.right panel backpack pager and operation hints share a utility bridge" --no-configuration-cache
./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas places shell rail and right panel inside their bounds" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Result:

1. FAIL-FIRST: the new focused test failed before implementation because no
   paired bridge posts connected backpack pager and operation hints.
2. PASS: the focused test passed after adding the forged cradle and bridge
   posts.
3. PASS: broader shell/right/bottom/nav focused tests and `maintainabilityLint`
   passed.
4. PASS: PR08 director golden regenerated the canonical evidence artifacts.

## Not Closed

This pass does not approve final PR08 closure, golden rebaseline, all-map room
families or packaged recapture for this exact crop. The next closure-oriented
step should be packaged recapture for the current shell packet before any final
rebaseline decision.
