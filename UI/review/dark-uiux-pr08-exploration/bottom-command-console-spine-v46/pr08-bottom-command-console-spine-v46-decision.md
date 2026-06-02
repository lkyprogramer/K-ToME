# PR08 V46 Bottom Command Console Spine Decision

## Verdict

Accepted-forward only.

V46 adds a shared bottom command spine across hero, action deck and log. This
keeps the current layout and resource keys intact, but pushes the bottom HUD
toward one authored operation surface instead of three independent framed cards.

## Why This Shape

- V45 explicitly ruled out another one-slot hierarchy tweak.
- `UI/UI-demo-new.png` reads as one bottom command surface with several
  functional regions, not as unrelated panels.
- The current renderer already owns bottom HUD cohesion rails, so this can be
  done without adding layout, schema, manifest, resource or action authority.

## Rejected Alternatives

- Rebuilding the full bottom layout in this slice: rejected because it would
  mix composition, layout and content hierarchy without a tight RED/GREEN
  anchor.
- Adding a new generated bottom-frame asset: rejected because this pass only
  proves the shared-console direction through runtime composition.
- Adding more per-slot ornaments: rejected because V45 already closed the
  reserve-slot hierarchy issue and warned against more single-slot tweaks.

## Evidence

- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-runtime-metrics.json`
- V46 bottom-deck crop:
  `UI/review/dark-uiux-pr08-exploration/bottom-command-console-spine-v46/pr08-bottom-command-console-spine-v46-v46-bottom-deck.png`

Key metrics:

- V46 vs V45 bottom-deck crop changed ratio:
  `0.06664255139567131`
- V46 vs V45 bottom-deck mean absolute RGB diff:
  `0.24963927573802777`

## Validation

- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.bottom hud renders a shared command spine across hero action and log'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`

## Next

Do not continue by stacking more subtle rails. The next meaningful move should
be a larger hero-card material hierarchy reset or a room-scale map structural
pass.
