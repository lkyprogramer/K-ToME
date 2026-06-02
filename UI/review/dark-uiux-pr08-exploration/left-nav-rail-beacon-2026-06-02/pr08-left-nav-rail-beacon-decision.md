# PR08 Left Nav Rail Beacon Decision

> Date: 2026-06-02
> Decision: `pr08-left-nav-rail-beacon-accepted-forward`
> Scope: left permanent navigation rail after the bottom-action subject-material pass

## Decision

Accept the left nav rail beacon pass forward as a bounded shell polish slice.
The pass keeps layout, resource keys, manifests, input behavior, map
presentation and gameplay state unchanged.

The permanent left rail now gives every nav icon a larger display footprint and
a dark material socket. The selected item also gains a warm vertical beacon in
addition to the existing restrained cyan wash. This makes the rail read more
like a forged command spine and less like small icons pasted onto a tall strip.

This does not close final PR08, packaged recapture for the exact current crop,
bottom-strip final-read, non-ruins room families or golden rebaseline.

## Evidence

- `left-nav-rail-beacon-before-after-board.png`
- `dark-uiux-pr08-director-parity-1672x941.png`
- `dark-uiux-pr08-director-map-stage-crop.png`
- `dark-uiux-pr08-director-right-panel-crop.png`
- `dark-uiux-pr08-director-bottom-deck-crop.png`
- `evidence-index.tsv`

Updated evidence hashes:

```text
dark-uiux-pr08-director-parity-1672x941=110ebe6abe8d58804fe777f2629bf0cf62fce04c69394be42da597b51e2e5f2a
dark-uiux-pr08-director-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676
dark-uiux-pr08-director-right-panel-crop=337f719abdf81dd29cb6d1b7c0d21579b8f17e627dd9ba362db14233b4b790fa
dark-uiux-pr08-director-bottom-deck-crop=db555eb0443980c7465ef4dd4b7d2a562196715de124922dfec07ee986b4a30d
dark-uiux-pr08-director-telegraph-combat-crop=b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321
```

## Why Not Micro-Tuning

The left rail is a permanent first-screen control surface. The failure was not
that one color alpha was slightly off; the icon subjects did not carry enough
control weight and selected state relied too much on a faint wash. This pass
changes the component grammar by adding larger icon subject scale, per-button
material sockets and a selected-state beacon. It stays renderer-only because
the existing resources and layout are sufficient for this bounded shell slice.

## Rejected Alternatives

1. Packaged recapture first: rejected for this turn because it would verify the
   current shell packet without improving the visible permanent rail.
2. New nav icon resources: deferred because the existing icons become readable
   at the evidence size once the socket scale and selected beacon are fixed.
3. Layout resize: rejected because changing `DemoShellLayout` would affect map,
   modal and tooltip reservation contracts while the problem is component
   grammar inside the existing rail.
4. Bottom-strip final-read in the same patch: deferred to the next slice to keep
   this pass reviewable and avoid bundling two shell surfaces.

## Validation

Commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav rail reads as icon first forged command rail" --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas places shell rail and right panel inside their bounds" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Results:

1. Fail-first focused test failed on nav icon subject size before
   implementation.
2. Focused test passed after reducing nav icon inset and adding selected beacon
   plus material sockets.
3. Broader shell/right/bottom/nav focused renderer tests passed.
4. `maintainabilityLint` passed.
5. PR08 director golden evidence command passed and regenerated the packet.

## Remaining Risk

1. Packaged whitebox was not recaptured after this exact left-nav slice.
2. Bottom strip final-read remains the highest visible shell blocker.
3. Non-ruins room art families and final PR08 owner gate ladder remain open.
4. Golden rebaseline is still blocked until final PR08 closure is explicitly
   accepted.
