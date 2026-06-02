# PR08 Bottom Deck Command Shelf Decision

> Date: 2026-06-01
> Decision: `pr08-bottom-deck-command-shelf-accepted-forward`
> Scope: bottom HUD command shelf after right-panel command-surface pass

## Decision

Accept the bottom-deck command-shelf pass forward as a bounded shell hierarchy
improvement. The pass keeps layout, resources, map presentation, action
semantics and log content unchanged.

The accepted route adds forged seam lock plates between hero/action/log and
subdues individual bottom child frames under the shared console shell. The
bottom HUD now reads less like three adjacent cards and more like one forged
command shelf. It does not close final PR08, packaged recapture for this exact
crop, non-ruins room families or golden rebaseline.

## Evidence

- `bottom-deck-command-shelf-before-after-board.png`
- `dark-uiux-pr08-director-bottom-deck-crop.png`
- `dark-uiux-pr08-director-parity-1672x941.png`
- `dark-uiux-pr08-director-right-panel-crop.png`
- `dark-uiux-pr08-director-map-stage-crop.png`
- `evidence-index.tsv`

Updated evidence hashes:

```text
dark-uiux-pr08-director-parity-1672x941=deb78736aa7804cd100e03fec3500fc1d3e5b70ce8fbe5ca1d2c92c74a5da1d0
dark-uiux-pr08-director-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676
dark-uiux-pr08-director-right-panel-crop=337f719abdf81dd29cb6d1b7c0d21579b8f17e627dd9ba362db14233b4b790fa
dark-uiux-pr08-director-bottom-deck-crop=d465f7118466bd257ed9b25cf7baae17143efdec39fc77ec8630cca2e2000403
dark-uiux-pr08-director-telegraph-combat-crop=b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321
```

## Why Not Micro-Tuning

This pass changes the bottom HUD information hierarchy, not isolated color or
spacing. The prior crop still read as three card frames even after shared rails;
the root cause was competing child-frame authority at the hero/action/log seams.
The chosen route moves visual ownership to the shared console shell and seam
lock plates, which is a shell composition correction inside the existing
presentation owner.

## Rejected Alternatives

1. Layout resize: rejected because `bottomDeckLayout` affects modal and tooltip
   reserved bounds, and the current issue is hierarchy rather than overlap.
2. New bottom HUD image resource: rejected for this slice because the shared
   shell structure can be improved through existing chrome/rendering without a
   new resource authority route.
3. Final rebaseline now: rejected because packaged recapture, all-map/non-ruins
   strategy, full owner gate ladder and final director review remain open.

## Validation

Commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest.bottom\ hud\ masks\ inter\ panel\ seams\ with\ forged\ lock\ plates --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest.bottom\ hud\ subdues\ individual\ panel\ frames\ under\ the\ shared\ console\ shell --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Results:

1. Fail-first seam-lock test confirmed the missing seam lock plates.
2. Fail-first child-frame hierarchy test confirmed individual bottom frames were
   still too dominant.
3. Both focused tests passed after implementation.
4. Broader bottom focused renderer tests passed.
5. `maintainabilityLint` passed.
6. PR08 director golden evidence command passed and regenerated the packet.

## Remaining Risk

1. Packaged whitebox was not recaptured after this exact bottom-deck slice.
2. Bottom shelf material richness is improved but still not a final director
   close against `UI/UI-demo-new.png`.
3. Non-ruins room art families and final PR08 owner gate ladder remain open.
4. Golden rebaseline is still blocked until final PR08 closure is explicitly
   accepted.
