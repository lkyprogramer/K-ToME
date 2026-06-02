# PR08 Bottom Strip Final-Read Decision

**Date**: 2026-06-02
**Verdict**: `pr08-bottom-strip-final-read-accepted-forward`
**Scope**: `client` presentation-only bottom HUD pass

## Decision

Accept this slice forward as a bounded bottom-strip final-read improvement.

The fixed crop now gives the bottom HUD clearer first-read hierarchy without
changing layout, action semantics, log content, resource ownership, manifests
or map-stage presentation:

1. hero HP/resource gauges sit in individual forged troughs instead of reading
   as loose flat color strips;
2. the fourth empty action reserve socket is visually quieter than the three
   equipped action commands;
3. long route-hint log rows receive note-well material and a second-channel
   right brace so Chinese guidance is easier to scan at bottom-deck scale.

This is not final PR08 closure, a golden rebaseline, all-map closure or packaged
recapture approval.

## Root Cause Class

`information-hierarchy / state grammar`

The blocker was not a missing frame. The bottom strip had enough shell chrome,
but three local reads still competed with the reference-quality target:

1. HP and stamina bars carried too much raw red/green rectangle weight.
2. Empty action reserve chrome was close enough to filled action chrome that
   filled-vs-empty command weight was weaker than intended.
3. Long route hints were readable, but still closer to flat paragraph text than
   a dense operational event ledger.

## Chosen Route

Use runtime-owned material treatments inside existing bottom HUD geometry:

1. draw per-gauge troughs, lips and tone markers around existing semantic gauge
   bars;
2. reduce reserve-socket asset/interior alpha and shrink the empty socket
   interior;
3. add per-row note wells and right-side braces to log event plates.

## Rejected Alternatives

1. New image resources: rejected for this slice because the blocker is state
   hierarchy inside already accepted shell geometry, not missing asset
   authority.
2. Bottom layout rewrite: rejected because previous bottom-deck passes already
   established a shared console shelf and focused crop evidence points to
   final-read hierarchy, not panel placement.
3. Text/content edits: rejected because log copy and localization remain
   runtime-owned content; no Chinese text or labels are baked into resources.
4. Broad panel alpha retuning: rejected because it would not specifically fix
   gauge, empty-reserve and long-note scanability.

## Evidence

1. `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/bottom-strip-final-read-before-after-board.png`
2. `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/dark-uiux-pr08-director-bottom-deck-crop.png`
3. `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/dark-uiux-pr08-director-parity-1672x941.png`
4. `UI/review/dark-uiux-pr08-exploration/bottom-strip-final-read-2026-06-02/evidence-index.tsv`

Current hashes:

```text
dark-uiux-pr08-director-parity-1672x941=ff1ccb08a886097f20e9aa31152e035f3550c101d874cb72c7b37bb527637d32
dark-uiux-pr08-director-map-stage-crop=84d2844867a2c287cb1fa217fbb04fc9acc8b1c5418f475163a754c380302676
dark-uiux-pr08-director-right-panel-crop=337f719abdf81dd29cb6d1b7c0d21579b8f17e627dd9ba362db14233b4b790fa
dark-uiux-pr08-director-bottom-deck-crop=a416b96d21059212a6a7682f8163f0f065dcac3b545bad28b064814f8120cdfd
dark-uiux-pr08-director-telegraph-combat-crop=b759184f0836fc657d86e55b93b28e27a08dbb2e80f897eaf7da82ba3f579321
```

## Validation

Passed:

```bash
./gradlew :client:test --tests "com.ktome.client.render.DemoShellRendererTest.empty action reserve socket stays below equipped action hierarchy" --tests "com.ktome.client.render.DemoShellRendererTest.hero gauges sit in individual forged troughs instead of flat color strips" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" --no-configuration-cache
./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests "com.ktome.client.render.TileRendererCanvasTest.demo nav*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas places shell rail and right panel inside their bounds" --tests "com.ktome.client.render.TileRendererCanvasTest.right panel*" --tests "com.ktome.client.render.TileRendererCanvasTest.operation command matrix keeps caption hierarchy and non overlapping key chips" --tests "com.ktome.client.render.TileRendererCanvasTest.right operation hints sit on one forged command dock" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom hud*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom action*" --tests "com.ktome.client.render.TileRendererCanvasTest.bottom log*" --tests "com.ktome.client.render.TileRendererCanvasTest.render canvas keeps long chinese route hints readable in bottom log" maintainabilityLint --no-configuration-cache
./gradlew :client:goldenScreenshot --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr08 director runtime evidence writes canonical artifacts" --rerun-tasks --no-configuration-cache
```

Fail-first evidence:

1. the tightened reserve-socket alpha assertion failed against the previous
   `0.46` reserve asset alpha;
2. the new hero-gauge trough assertion failed before per-gauge troughs existed;
3. the new long-route note-well assertion failed before log row note wells were
   added.

## Rollback

Revert the bottom-strip renderer additions in
`client/src/main/kotlin/com/ktome/client/render/DemoShellRenderer.kt` and the
focused assertions in:

1. `client/src/test/kotlin/com/ktome/client/render/DemoShellRendererTest.kt`
2. `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`

No resource, manifest, schema, localization or gameplay rollback is required.

## Remaining Risk

1. Packaged whitebox was not recaptured after this exact bottom-strip slice.
2. Non-ruins room families remain outside the accepted `tileset.ruins` proof
   slice.
3. Final PR08 owner gate ladder, full `verifyChanged` and golden rebaseline
   remain open.
