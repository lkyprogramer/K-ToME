# PR08 V48 Hero Crest Resource Replacement Decision

## Verdict

Accepted-forward only.

V48 replaces the existing `ui.shell.hero_crest.placeholder` grey shield with a
formal red-enamel and gold-lion heraldic crest. The change goes through the
existing Dark UI resource authority chain instead of adding a renderer-only
overlay or a second hero-art key.

## Why This Shape

- V47 explicitly left the hero art itself as the next meaningful bottom-HUD gap.
- The reference `UI/UI-demo-new.png` uses an authored red/gold crest as a first
  read signal; the old grey shield remained a placeholder even after the V47
  material pillar.
- The existing `ui.shell.hero_crest.placeholder` key already has sheet-plan,
  key-registry, manifest and runtime consumption, so replacing its cell is the
  smallest authority-preserving resource move.

## Resource Chain

- Source generation:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-generated-source.png`
- Alpha extraction:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-alpha.png`
- Final 256 cell:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-cell256.png`
- Raw sheet cell:
  `assets-src/image/raw/sheets/dark-v1/r01b-ui-shell-chrome.png`
  row `2`, col `0`
- Runtime slice:
  `client/src/main/resources/dark-v1/ui/ui_shell_hero_crest_placeholder.png`
- Sprite report:
  `assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl`

The generated image used built-in `image_gen`, then local chroma-key alpha
extraction with the system imagegen helper. No new manifest key, resource
schema, runtime resolver path or hero state was added.

## Evidence

- Comparison board:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-comparison-board.png`
- Runtime metrics:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-runtime-metrics.json`
- V48 full screenshot:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-v48-full.png`
- V48 bottom-deck crop:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-v48-bottom-deck.png`
- Runtime crest copy:
  `UI/review/dark-uiux-pr08-exploration/hero-crest-resource-v48/pr08-hero-crest-resource-v48-runtime-hero-crest.png`

Key metrics:

- V48 vs V47 bottom-deck changed ratio:
  `0.03628896123470188`
- V48 vs V47 bottom-deck mean absolute RGB diff:
  `1.106585478587649`
- V48 vs V47 bottom-deck max absolute channel diff:
  `233`

## Validation

- RED then GREEN:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests 'com.ktome.client.assets.ManifestResolveTest.dark uiux hero crest keeps authored red enamel and gold heraldry'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :tools:darkKeyRegistryLint :tools:darkSpriteSheetLint :tools:spriteSheetMapLint :tools:resourcePipelineLint :tools:darkManifestCoveragePr02_1OwnerScope :tools:darkManifestCoveragePr08OwnerScope -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr08-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=r01-ui-chrome,r01b-ui-shell-chrome,r02-ui-demo-ruins-tiles`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr02 1 golden evidence hashes remain stable and writes canonical artifacts'`
- PASS:
  `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.DemoShellRendererTest --tests com.ktome.client.render.DemoShellLayoutTest :client:clientSmoke maintainabilityLint`

`clientSmoke` kept three existing environment-dependent subtests skipped.

## Rejected Alternatives

- Adding a second hero portrait key: rejected because the existing key already
  owns the hero-card crest consumer.
- Runtime color overlay over the old shield: rejected because it would keep the
  placeholder art and add renderer pressure.
- Another small hero-card rail/pillar pass: rejected because V47 already proved
  the art itself was the gap.

## Next

Do not continue by making another small hero crest or hero-card overlay tweak.
The next meaningful move should return to room-scale map structure or a broader
bottom HUD composition pass.
