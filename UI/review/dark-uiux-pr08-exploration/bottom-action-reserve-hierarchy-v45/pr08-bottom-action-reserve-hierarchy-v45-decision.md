# PR08 V45 Bottom Action Reserve Hierarchy Decision

## Verdict

Accepted-forward only.

V45 lowers the fourth bottom action socket when it has no action entry. The
slot still communicates reserve capacity, but it no longer reads like an
available action or competes with actions `1-3`.

## Why This Shape

- `UI/UI-demo-new.png` gives available actions the strongest read in the bottom
  command area; empty capacity does not compete with active commands.
- The V44 endpoint identified bottom-deck composition as one of the next useful
  non-map moves.
- This slice preserves the active slot count and existing action layout while
  correcting hierarchy at the point where the current golden was visibly noisy.

## Rejected Alternatives

- Removing the fourth slot: rejected because active slot capacity is existing
  UI contract, not a V45 visual decision.
- Adding a new generated action frame asset: rejected because the current issue
  is hierarchy, and can be validated through renderer behavior without widening
  the resource owner surface.
- Reworking the whole bottom HUD in this slice: rejected because V45 needed a
  narrow RED/GREEN step before attempting a larger hero/action/log composition
  reset.

## Evidence

- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-runtime-metrics.json`
- V45 bottom-deck crop:
  `UI/review/dark-uiux-pr08-exploration/bottom-action-reserve-hierarchy-v45/pr08-bottom-action-reserve-hierarchy-v45-v45-bottom-deck.png`

Key metrics:

- V45 vs V44 bottom-deck crop changed ratio:
  `0.042176101766443604`
- V45 vs V44 bottom-deck mean absolute RGB diff:
  `0.17322602037740398`

## Validation

- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.empty action reserve socket stays below equipped action hierarchy'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`

## Next

Do not continue with another one-slot hierarchy tweak. The next meaningful move
should either consolidate hero/action/log into a stronger authored bottom
command console or return to room-scale map structure.
