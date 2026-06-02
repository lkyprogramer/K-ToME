# PR08 Bottom Action Subject Material Decision

> Date: 2026-06-01
> Decision: `pr08-bottom-action-subject-material-accepted-forward`
> Scope: filled bottom action subjects after the bottom command-shelf pass

## Decision

Accept the bottom-action subject-material pass forward as a bounded action-slot
quality improvement. The pass keeps layout, resources, map presentation, action
semantics and log content unchanged.

Filled action icons now draw larger and sit on dark material pedestals with warm
crown lips. This makes the action deck read more like authored equipment
subjects inside command sockets rather than small generic button glyphs. It
does not close final PR08, packaged recapture for this exact crop, non-ruins
room families or golden rebaseline.

## Evidence

- `bottom-action-subject-material-before-after-board.png`
- `dark-uiux-pr08-director-bottom-deck-crop.png`
- `dark-uiux-pr08-director-parity-1672x941.png`
- `dark-uiux-pr08-director-right-panel-crop.png`
- `dark-uiux-pr08-director-map-stage-crop.png`
- `evidence-index.tsv`

Updated evidence hashes:

```text
dark-uiux-pr08-director-parity-1672x941=1509ffde2f893139c1c2265a70ef9e27bb94c65e44e30b9aa8d8beb0376adde3
dark-uiux-pr08-director-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676
dark-uiux-pr08-director-right-panel-crop=337f719abdf81dd29cb6d1b7c0d21579b8f17e627dd9ba362db14233b4b790fa
dark-uiux-pr08-director-bottom-deck-crop=db555eb0443980c7465ef4dd4b7d2a562196715de124922dfec07ee986b4a30d
dark-uiux-pr08-director-telegraph-combat-crop=b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321
```

## Why Not Micro-Tuning

The previous bottom shelf pass made the container read more coherently, but the
filled action subjects still lacked the reference image's object confidence.
This pass changes the action-slot subject hierarchy: icon subjects become
larger and receive an authored material pedestal. It is not a color-only,
padding-only or frame-alpha adjustment.

## Rejected Alternatives

1. Packaged recapture first: rejected for this turn because it would only verify
   the existing packet and would not improve the still-visible action-subject
   weakness.
2. New action icon resources: deferred because this renderer-only pass can
   improve subject scale and grounding without introducing a new owner/resource
   chain. A resource-quality pass may still be needed later if crop review shows
   the existing icon subjects remain below the final bar.
3. Layout resize: rejected because the issue is action subject hierarchy inside
   existing sockets, not overlap or reserved-bound failure.

## Validation

Commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action subjects sit on large material pedestals" --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Results:

1. Fail-first focused test confirmed the action icon subjects were too small and
   lacked large material pedestals.
2. Focused test passed after implementation.
3. Broader bottom focused renderer tests passed.
4. `maintainabilityLint` passed.
5. PR08 director golden evidence command passed and regenerated the packet.

## Remaining Risk

1. Packaged whitebox was not recaptured after this exact bottom-action slice.
2. Existing action icon resources may still be below final director-grade
   resource richness even with stronger runtime presentation.
3. Non-ruins room art families and final PR08 owner gate ladder remain open.
4. Golden rebaseline is still blocked until final PR08 closure is explicitly
   accepted.
