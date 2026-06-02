# PR08 V44 Runtime Right Empty Slot Quieting Decision

## Verdict

Accepted-forward only.

V44 removes the most obvious non-map UI artifact in the current right panel:
visual-only empty equipment sockets previously rendered high-alpha gold central
motifs that read like placeholder letters. The accepted cut keeps the existing
slot chrome and `TileDemoSlotModel.visualOnly` authority, but routes
visual-only empty sockets through a quieter dark socket branch.

## Why This Shape

- The reference direction in `UI/UI-demo-new.png` uses quiet dark empty sockets:
  empty slots support hierarchy instead of competing with equipped icons.
- V43i already showed that another small floor/wall resource pass would be a
  low-leverage move.
- This slice changes a visible UI/UX quality defect without touching gameplay,
  schema, manifest keys, resource keys or inventory authority.

## Rejected Alternatives

- Adding brighter slot ornaments: rejected because it would repeat the
  placeholder-glyph problem.
- Generating new slot assets: rejected for this pass because the existing slot
  chrome is sufficient to prove the UX direction, and new resources would widen
  the owner surface.
- Reworking all right-panel layout density: rejected because V44 only needed to
  address the fake-glyph defect and keep the diff reviewable.

## Evidence

- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-runtime-metrics.json`
- V44 right-panel crop:
  `UI/review/dark-uiux-pr08-exploration/runtime-right-empty-slot-quieting-v44/pr08-right-empty-slot-quieting-v44-v44-right-panel-grid.png`

Key metric:

- V44 vs V43i right-panel crop changed ratio:
  `0.032239365411436544`
- V44 vs V43i right-panel mean absolute RGB diff:
  `0.3675447466294747`

## Validation

- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.visual only empty equipment sockets stay quiet instead of drawing fake gold glyphs'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`

## Next

Do not continue by only tuning slot alpha or adding more slot ornaments. The
next meaningful move should be bottom-deck hero/action/log composition
unification, or a larger room-scale structural map pass that changes first read
before grid detail.
