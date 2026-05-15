# Dark UI/UX PR03 Fallback Key Injection Record

## Status

- result: `PASS_AUTOMATED_EVIDENCE`
- last-updated: `2026-05-15`
- scenarioId: `dark-uiux-pr03-fallback-key-injection`
- officialRuntimeManifestMutation: `not performed`
- injectedMissingKey: `item.missing.debug.icon`
- fallbackKey: `ui.empty.inventory.icon`
- sourceManifestPath: `assets-src/image/manifests/phase2-visual-manifest.json`
- runtimeManifestPath: `client/src/main/resources/manifests/visual-manifest.json`
- evidenceLabel: `dark-uiux-pr03-inventory-empty`
- reportPath: `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json`
- screenshotPath: `N/A - resolver/presenter evidence used; official runtime manifest was not mutated`
- residualRisk: `No packaged-app injected-key screenshot was produced; this keeps the player runtime manifest clean and relies on focused resolver/presenter coverage for the missing-key path.`

## Automated Evidence

- `EquipmentInventoryPresenterTest` covers missing item icon fallback to `ui.empty.inventory.icon` and asserts resolver evidence for `item.missing.debug.icon` and `item.missing.debug.visual`.
- `ManifestResolveTest` covers exact PR03 manifest entries.
- `darkManifestCoverageLint` ran in owner-scope mode for PR-03 sheets: `r07-items-base`, `r07-items-unique-artifact`, `r07-items-affix-material`.

## Validation Command

```bash
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.EquipmentInventoryPresenterTest darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-03 -Pktome.darkUiux.requiredOwnerSheetIds=r07-items-base,r07-items-unique-artifact,r07-items-affix-material
```

Result: `PASS`.

## Boundary

This record does not prove the visual appearance of an injected missing key inside a packaged app. The PR03 manual white-box record covers the packaged inventory surface, while this file records the controlled resolver/presenter fallback path without polluting the official runtime manifest.
