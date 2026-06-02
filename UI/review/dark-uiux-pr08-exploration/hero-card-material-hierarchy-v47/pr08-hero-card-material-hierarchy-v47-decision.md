# PR08 V47 Hero Card Material Hierarchy Decision

## Verdict

Accepted-forward only.

V47 gives the hero card a tall heraldic material pillar behind the crest. It
keeps current layout, crest asset, gauges and resource keys intact while making
the bottom-left hero block read less like a flat stat card.

## Why This Shape

- V46 explicitly moved the next useful bottom pass toward hero-card material
  hierarchy rather than more subtle rails.
- `UI/UI-demo-new.png` gives the hero area a stronger authored material block
  and portrait/crest anchor than the current small backing plate.
- The current renderer already owns hero-card interior composition, so this can
  be tested and shipped without widening resource or schema authority.

## Rejected Alternatives

- Generating a new hero portrait resource in this slice: rejected for V47
  because that needs the full image pipeline, manifest and owner gates; V47
  first locks the runtime material direction.
- Changing hero-card layout: rejected because the current problem is visual
  hierarchy, not a layout contract failure.
- Adding more small rail details: rejected because V46 already warned against
  stacking subtle rails.

## Evidence

- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-runtime-metrics.json`
- V47 bottom-deck crop:
  `UI/review/dark-uiux-pr08-exploration/hero-card-material-hierarchy-v47/pr08-hero-card-material-hierarchy-v47-v47-bottom-deck.png`

Key metrics:

- V47 vs V46 bottom-deck crop changed ratio:
  `0.03633116295894375`
- V47 vs V46 bottom-deck mean absolute RGB diff:
  `0.0600847048893712`

## Validation

- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.render.DemoShellRendererTest.hero card renders a tall heraldic material pillar behind the crest'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`

## Next

Do not continue with another tiny hero overlay. The next meaningful move should
either generate and pipe a stronger official hero portrait/crest resource
through the asset chain or return to room-scale map structure.
