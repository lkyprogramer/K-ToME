# PR08 Right Panel Command Surface Decision

> Date: 2026-06-01
> Decision: `pr08-right-panel-command-surface-accepted-forward`
> Scope: right-panel command surface after Candidate W ruins map proof and packaged telegraph parity

## Decision

Accept the right-panel command-surface pass forward as the next shell-quality
slice. The map-stage proof slice remains Candidate W for `tileset.ruins`; this
packet changes the right column hierarchy only.

The accepted route adds visible dark material wells behind all right-panel
sections and forged wells behind equipment sockets. It makes the right side read
as an authored command surface rather than sparse independent rows. It does not
close final PR08, bottom-deck cohesion, non-ruins room families, packaged
recapture for this exact crop or golden rebaseline.

## Evidence

- `right-panel-command-surface-before-after-board.png`
- `dark-uiux-pr08-director-right-panel-crop.png`
- `dark-uiux-pr08-director-parity-1672x941.png`
- `dark-uiux-pr08-director-bottom-deck-crop.png`
- `dark-uiux-pr08-director-map-stage-crop.png`
- `evidence-index.tsv`

Updated evidence hashes:

```text
dark-uiux-pr08-director-parity-1672x941=0fa3c7b9c92a98d072a84ddd10b2c8886d8c4f690b8089992aef4e8f6a53c76c
dark-uiux-pr08-director-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676
dark-uiux-pr08-director-right-panel-crop=337f719abdf81dd29cb6d1b7c0d21579b8f17e627dd9ba362db14233b4b790fa
dark-uiux-pr08-director-bottom-deck-crop=6bf2380371c23586c38737409e70fb487c903b82b89ed989fc80c0f8e837a142
dark-uiux-pr08-director-telegraph-combat-crop=b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321
```

## Why Not Micro-Tuning

This pass does not tune map alpha, seams, grid weight or room-plate texture
details. The packaged telegraph path already removed the blocking runtime
evidence gap for the ruins proof slice, and the fixed crop shows the next
visible director-quality issue in the shell: the right panel lacked material
grounding and command-surface density. The change is therefore an information
hierarchy and chrome-density correction at the shell owner layer.

## Rejected Alternatives

1. More ruins map-stage alpha/marker tuning: rejected because Candidate W is
   already accepted for the `tileset.ruins` proof slice and further map
   micro-tuning would not address the visible right-column weakness.
2. Final golden rebaseline now: rejected because bottom deck, non-ruins
   families, packaged recapture for this exact crop and full owner gate ladder
   remain open.
3. New baked right-panel image resource: rejected for this slice because the
   current renderer/chrome path can express the material hierarchy without
   adding a new owner/resource contract.

## Validation

Commands run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest.right\ panel\ sections\ use\ readable\ forged\ wells\ behind\ sockets\ and\ utility\ rows --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" maintainabilityLint --no-configuration-cache
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Results:

1. Fail-first focused test confirmed the missing section wells.
2. Focused right-panel test passed after implementation.
3. Broader right-panel / bottom-deck focused renderer tests passed.
4. `maintainabilityLint` passed.
5. PR08 director golden evidence command passed and regenerated the packet.

## Remaining Risk

1. Packaged whitebox was not recaptured after this exact right-panel slice.
2. Bottom deck still reads less cohesive than the reference quality bar.
3. Non-ruins room art families and final PR08 owner gate ladder remain open.
4. Golden rebaseline is still blocked until final PR08 closure is explicitly
   accepted.
