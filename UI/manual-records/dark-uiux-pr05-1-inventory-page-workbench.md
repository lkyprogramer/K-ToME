# Dark UI/UX PR05-1 Inventory Page Workbench Manual Record

## Status

PR05-1 runtime implementation, whitebox materialization, packaged launcher, and packaged-app window capture have been re-run after the stack/action/input/director-review fixes.

| Field | Value |
| --- | --- |
| ownerPr | `PR-05-1` |
| scenarioId | `dark-uiux-pr05-1-inventory-page-workbench` |
| seed | `20260521` |
| inputSequence | launch packaged whitebox app -> direct validation session -> `I` open inventory workbench -> `Right` / `Enter` compare selection -> `Right` / `Right` / `Enter` consumable/material-like selection -> `PgDn` empty page-two cell -> `Left` / `Left` / `Left` / `Enter` page-two item selection -> resize `1024x768` -> `Esc` return |
| viewport | `1280x800`, `1024x768` |
| screenshotPath | `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-workbench-open.png`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-compare-selection.png`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-consumable-selection.png`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-empty-cell-selection.png`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-pagination-page-two.png`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-min-window-1024x768.png`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-escape-return-map.png` |
| logPath | `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/app.log`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-page-workbench-app.log`, `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/pgrep-final-cleanup.txt` |
| expectedObservation | Full-screen inventory workbench with 9-slot visual equipment area, 6x4 stack-aware grid, selected item detail, typed compare/action rows, footer hints, live page 2 pagination, empty selected state, and no major overlap at the minimum window. |
| cleanupStatus | `packaged-app-killed`; `pgrep-final-cleanup.txt` is empty |
| residualRisk | Evidence artifacts depend on `build/whitebox/.../evidence` persistence. Re-run the runbook if cleaned. |
| result | `PASS` |

## Packaged CUA Evidence

| Step | Required observation | Runtime artifact status |
| --- | --- | --- |
| open workbench | `I` opens a full-page workbench. Left visual equipment sockets, center 6x4 backpack grid, right detail/compare/action pane, page/capacity and footer hints are visible. | `333a8ae7543e6c23c464df4c3d8a2ed22101e3b0c9da5233584090f3d037d94a` at `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-workbench-open.png` |
| compare selection | Moving focus and committing selection updates the selected equipment item and shows typed compare rows. | `f07c501dbe7852b6799d26087fc58a87b7fac6bb2aa8069971b61c61d31b5fb0` at `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-compare-selection.png` |
| consumable/material-like selection | Selecting a non-equipment item keeps the same workbench layout and does not show fake equip-only compare rows. | `700b4158b4f2c438d4f622cf6753b59e962b11da8732b8dbf1af50c320d86058` at `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-consumable-selection.png` |
| empty/focus stability path | Page 2 opens on an empty focused cell and shows empty selection/action state without placeholder item text. | `c3d773b130092654ba00a25b0e43fe681526f3894cd8a3aed89abc527cb8a026` at `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-empty-cell-selection.png` |
| live pagination | Page 2 remains visible after committing a real page-two item; focus coordinates and footer/page text remain stable. | `9cf1b2a007ade1d5215b5a8944ba1a9ef14954623faf089ba3cddf186a2b67b2` at `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-pagination-page-two.png` |
| min window | At `1024x768`, all three columns, grid, selected item detail and footer remain readable without major overlap. | `76e39cc82c66ecf9a94cde014f6b3585df4dd79b91890f23e2b7e0a33dad4a5b` at `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-min-window-1024x768.png` |
| escape return | `Esc` exits the inventory workbench and returns to the in-run map shell. | `250fe36522a7087c8f10a7b30c855a45b82fcad21c3b1709df6f87af2e92fb51` at `build/whitebox/dark-uiux-pr05-1-inventory-page-workbench/evidence/dark-uiux-pr05-1-inventory-escape-return-map.png` |

Every accepted screenshot has a `.metadata.txt` and `.sha256` sidecar. Metadata records `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_pid=38266`; min-window metadata records `window_bounds=80,38,1024,768`. All seven screenshot hashes are distinct.

## Golden Evidence

| Label | Hash | Artifact |
| --- | --- | --- |
| `dark-uiux-pr05-1-inventory-workbench` | `8a4911bd6370cd20864373e635f074719d001917dcc1991176be08341c8ed418` | `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-workbench.png` |
| `dark-uiux-pr05-1-inventory-compare` | `2838efc818b53b715f44ce70554d3f0871c25e25bb7dc3e1a381acc4fd2801f2` | `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-compare.png` |
| `dark-uiux-pr05-1-inventory-pagination` | `516dcfc22f6770ab6ee7cbf7521c7699635dab0f304fd8c024a3c3f1289b875b` | `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-pagination.png` |
| `dark-uiux-pr05-1-inventory-min-window` | `ae137977af504377972c967e0af87483382f647ba300eadd7ecf99ea40222e35` | `client/build/reports/golden/dark-uiux-pr05-1/dark-uiux-pr05-1-inventory-min-window.png` |

## Validation Runs

All Gradle commands were run serially after:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

| Gate | Result | Notes |
| --- | --- | --- |
| `./gradlew :client:test --tests com.ktome.client.render.InventoryWorkbenchPresenterTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.input.InputHandlerTest` | `PASS` | Covered typed stack/action target, material-variant stack identity, typed compare rows, compact viewport detail clamp, action/footer key visibility, empty selection, layout profiles, renderer workbench integration, and input pagination regressions. |
| `./gradlew :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest` | `PASS` | Covered typed PR05-1 scenario registration, expected evidence names, runbook materialization, launcher script, and copied-app cfg injection. |
| `./gradlew :client:clientSmoke localeLint contractLint maintainabilityLint verifyChanged` | `PASS` | Re-ran owner-routed client smoke, golden screenshot, contract, locale, keyword registry, boss harness, scope coverage, long-run routing and maintainability checks after director-review fixes. |
| `./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr05-1-inventory-page-workbench` | `PASS` | Generated PR05-1 launcher, runbook, expected evidence plan, app hash and evidence directories. |
| Manual white-box / CUA packaged validation | `PASS` | Script-launched packaged app opened the PR05-1 validation session directly; screenshots were captured from PID `38266`; final cleanup left no matching app process. |
