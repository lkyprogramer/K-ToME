# Dark UI/UX PR05-1 Inventory Page Workbench Manual Record

## Status

This file is a pending manual record template for the PR05-1 inventory workbench contract. It does not claim that the runtime workbench, golden labels, screenshots or packaged-app manual validation already exist.

| Field | Value |
| --- | --- |
| ownerPr | `PR-05-1` |
| evidenceLabel | `dark-uiux-pr05-1-inventory-workbench`, `dark-uiux-pr05-1-inventory-compare`, `dark-uiux-pr05-1-inventory-pagination`, `dark-uiux-pr05-1-inventory-min-window` |
| scenarioId | `dark-uiux-pr05-1-inventory-page-workbench` |
| seed | `PENDING_IMPLEMENTATION` |
| inputSequence | `open -> move focus -> enter inspect/select -> equip/use attempt -> drop disabled/enabled -> page -> escape` |
| viewport | `1672x941`, `1280x800`, `1024x768` |
| screenshotPath | `PENDING_IMPLEMENTATION` |
| logPath | `PENDING_IMPLEMENTATION` |
| expectedObservation | Full-screen inventory workbench with 9-slot visual equipment area, 6x4 stack-aware grid, selected item detail, typed compare/action rows, footer hints and no overlap. |
| cleanupStatus | `not-run` |
| residualRisk | Runtime PR05-1 implementation has not produced the required golden/manual evidence yet. |
| result | `PENDING_IMPLEMENTATION` |

## Required Final Evidence

| evidenceLabel | Required observation | Runtime artifact status |
| --- | --- | --- |
| `dark-uiux-pr05-1-inventory-workbench` | Three-column workbench, 6x4 grid, stack quantity badge, footer and low-light shell context. | `PENDING_IMPLEMENTATION` |
| `dark-uiux-pr05-1-inventory-compare` | Equippable selected item shows typed detail rows, current equipped target cue and typed compare rows. | `PENDING_IMPLEMENTATION` |
| `dark-uiux-pr05-1-inventory-pagination` | Inventory with more than 24 visible entries shows page count, stable focus and capacity text. | `PENDING_IMPLEMENTATION` |
| `dark-uiux-pr05-1-inventory-min-window` | Compact viewport keeps text, footer, focus ring and detail pane readable without overlap. | `PENDING_IMPLEMENTATION` |

## Validation Runs

All final Gradle commands must run serially after:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

| Gate | Result | Notes |
| --- | --- | --- |
| `./gradlew acceptanceContractLint` | `PENDING_IMPLEMENTATION` | Must pass after doc/reference/manual template changes. |
| `./gradlew :client:test --tests com.ktome.client.render.InventoryWorkbenchPresenterTest --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.render.TileRendererCanvasTest` | `PENDING_IMPLEMENTATION` | Must cover workbench presentation, focus, pagination, hover preview, typed rows and stack quantity badge. |
| `./gradlew :client:clientSmoke :client:goldenScreenshot` | `PENDING_IMPLEMENTATION` | Must register and write the four PR05-1 labels listed above. |
| `./gradlew localeLint contractLint maintainabilityLint verifyChanged` | `PENDING_IMPLEMENTATION` | Must prove localized footer/action tokens, typed presentation ownership and changed-domain routing. |

## Finalization Rule

Before PR05-1 can close, replace every `PENDING_IMPLEMENTATION` and `not-run` value in this file with real repo-relative artifact paths, hashes and pass/fail results. The four required PR05-1 labels and manual screenshots cannot be skipped; skipped-with-reason is allowed only for non-blocking supplementary records.
