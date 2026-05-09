# Dark UI/UX PR-01-1 Viewport Renderer Overlay Manual Record

## Scope

- PR: `dark-uiux-pr01-1-client-viewport-renderer-overlay`
- Source: `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`
- Implementation branch: `codex/dark-uiux-pr01-1-client-viewport-renderer-overlay`
- Resource generation: `none`; this PR uses primitive tooltip/modal chrome and existing manifest keys.
- Whitebox status: `computer-use-manual-pass-complete`; automated owner evidence, `clientSmoke`, `goldenScreenshot`, and the packaged-app Computer Use pass are complete.
- Packaged app manual pass: `build/whitebox/dark-uiux-pr01-1-manual-20260509/K-ToME-pr011-manual.app`
- Manual runtime home: `build/whitebox/dark-uiux-pr01-1-manual-20260509/runtime-home`
- Manual evidence root: `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence`
- Computer Use target app: `com.ktome.client.pr011manual`

## Verification Source

| requirementId | Must Test item | contract owner | test class | test method | artifact | status |
| --- | --- | --- | --- | --- | --- | --- |
| `UI01-1-M01` | player-centered deadzone viewport | `client` | `TileMapViewportTest` | `keepsTopLeftInsideDeadzone`, `keepsDeadzoneHalfOpenForEvenAndOddCells`, `keepsThresholdEqualJumpOnDeadzonePath`, `snapsWhenHorizontalJumpExceedsThreshold`, `snapsWhenVerticalJumpExceedsThreshold`, `snapsWhenDiagonalAnyAxisExceedsThreshold`, `snapsPlayerJumpWhileInspectFocusIsActive`, `snapsPlayerJumpWhileTargetingFocusIsActive` | `client/build/test-results/test/TEST-com.ktome.client.render.TileMapViewportTest.xml` | `PASS` |
| `UI01-1-M01` | focus projection feeds viewport identity and active cursor | `client` | `TileViewportFocusProjectionTest`, `TileRendererCanvasTest` | `playerModeUsesPlayerTile`, `inspectModeResolvesCursorFromOverlayOrModalState`, `targetingModeResolvesCursorFromOverlayOrModalState`, `viewportUsesShellMapBounds` | `client/build/test-results/test/TEST-com.ktome.client.render.TileViewportFocusProjectionTest.xml`, `client/build/test-results/test/TEST-com.ktome.client.render.TileRendererCanvasTest.xml` | `PASS` |
| `UI01-1-M02` | shell world size is fixed and decoupled from snapshot map dimensions | `client` | `FoundationViewportSupportTest` | `keepsWorldSizeFixedWhenSnapshotDimensionsChange`, `keepsViewportWorldSizeFixedAcrossResize`, `usesShellMapBoundsInsteadOfFullMapPixels` | `client/build/test-results/test/TEST-com.ktome.client.screen.FoundationViewportSupportTest.xml` | `PASS` |
| `UI01-1-M02` | small maps are centered without stretching and large maps are clamped | `client` | `TileMapViewportTest`, `TileRendererCanvasTest` | `centersSmallMapWithinBounds`, `centersSmallMapWithInnerPaddingAndZeroTopLeft`, `clampsBottomRightEdge`, `producesCellAlignedVisibleRange`, `usesIntegerTileToScreenForEveryLayer`, `changesIdentityWhenCellAlignedMapBoundsChanges`, `render canvas rejects mismatched snapshot and model map dimensions` | `client/build/test-results/test/TEST-com.ktome.client.render.TileMapViewportTest.xml`, `client/build/test-results/test/TEST-com.ktome.client.render.TileRendererCanvasTest.xml` | `PASS` |
| `UI01-1-M03` | `TileRenderer` only orchestrates typed map/shell/overlay frames | `client` | `TileRendererCanvasTest` | `recordsExplicitLayerFlushBoundaries`, `overlayFrameDoesNotCarryRawOverlayState`, `viewportUsesShellMapBounds`, `overlayDrawsAboveShellAndBottomHud` | `client/build/test-results/test/TEST-com.ktome.client.render.TileRendererCanvasTest.xml` | `PASS` |
| `UI01-1-M03` | map sublayer order has a typed owner | `client` | `TileLayerComposerTest` | `ordersMapSublayersForFogLootCursorAndCombatFeedback`, `drawsOnlyActiveProjectionCursor` | `client/build/test-results/test/TEST-com.ktome.client.render.TileLayerComposerTest.xml` | `PASS` |
| `UI01-1-M04` | overlay layer order and modal/passive tooltip arbitration | `client` | `TileOverlayLayerTest`, `TileRendererCanvasTest` | `selectsModalExplicitTooltipBeforePassiveTooltip`, `recordsSuppressedPassiveTooltipWhenModalActive`, `rendererDoesNotBranchOnModalFrameKind`, `rendererUsesSelectedTooltipFromModelOnly`, `productionTooltipPlacementUsesRightDownLeftUpCandidateOrder`, `overlayDrawsAboveShellAndBottomHud` | `client/build/test-results/test/TEST-com.ktome.client.render.TileOverlayLayerTest.xml`, `client/build/test-results/test/TEST-com.ktome.client.render.TileRendererCanvasTest.xml` | `PASS` |
| `UI01-1-M04` | modal stack and input close semantics do not regress | `client` | `InputHandlerTest`, `ModalStackTest` | `inventory mode opens item detail and uses escape as full close`, `combat decision frame walks action target and save semantics`, `framesAreBottomToTopForProjection` | `client/build/test-results/test/TEST-com.ktome.client.input.InputHandlerTest.xml`, `client/build/test-results/test/TEST-com.ktome.client.ui.layout.ModalStackTest.xml` | `PASS` |
| `UI01-1-M05` | map-first threshold, right-panel sections and modal safe bounds are programmatic | `client / docs` | `GameShellLayoutTest`, `TileRendererCanvasTest` | `keepsMapAreaAtLeastHalfShellUsableContentAt1280x800`, `computesShellUsableContentAreaExcludingBottomHud`, `producesModalSafeBoundsAboveFooterAndBottomLog`, `viewport layout matrix locks fixed shell and cell aligned map bounds`, `shell right panel keeps ground equipment inscriptions and backpack sections in order`, `modalUsesModalSafeBounds`, `tooltipAvoidsBottomLogReservedBounds` | `client/build/test-results/test/TEST-com.ktome.client.render.GameShellLayoutTest.xml`, `client/build/test-results/test/TEST-com.ktome.client.render.TileRendererCanvasTest.xml` | `PASS` |
| `UI01-1-M05` | `dark-uiux-pr01-1-*` golden evidence namespace | `client / docs` | `GoldenScreenshotHarnessTest` | `dark uiux pr01 1 golden evidence labels remain registered`, `dark uiux pr01 1 golden evidence hashes remain stable and writes canonical artifacts` | `client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml`, `client/build/reports/golden/dark-uiux-pr01-1/evidence-index.tsv` | `PASS` |
| `UI01-1-M06` | typed frame, diagnostics boundary, text metrics and hot-loop discipline | `client` | `TileRendererCanvasTest`, `maintainabilityLint` | `recordsExplicitLayerFlushBoundaries`, `overlayFrameDoesNotCarryRawOverlayState`, `renderDiagnosticsDoesNotExposeAggregateFrameState`, `shellFrameCarriesPaneFocusFactAndTextLayoutOnly`; task `maintainabilityLint` | `client/build/test-results/test/TEST-com.ktome.client.render.TileRendererCanvasTest.xml`, `build/reports/problems/problems-report.html` | `PASS` |
| `UI01-1-M07` | governance, contract lint and ASCII deletion | `docs / tools` | `ManifestResolveTest`, `acceptanceContractLint`, `contractLint` | `visual manifest v1 rejects removed ASCII entry fields`, `visual manifest v2 rejects removed ASCII entry fields`; tasks `acceptanceContractLint`, `contractLint` | `client/build/test-results/test/TEST-com.ktome.client.assets.ManifestResolveTest.xml`, `build/reports/problems/problems-report.html` | `PASS` |
| `UI01-1-M01` to `UI01-1-M07` | owner gates | `client / tools` | Gradle owner tasks | `:client:test`, `:client:clientSmoke`, `:client:goldenScreenshot`, `maintainabilityLint`, `verifyChanged` | `client/build/reports/tests/test/`, `client/build/reports/tests/clientSmoke/`, `client/build/reports/tests/goldenScreenshot/`, `client/build/reports/golden/dark-uiux-pr01-1/`, `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `PASS` |
| dirty worktree routed by `verifyChanged` | client UI evidence, dark UI/UX pipeline plus maintainability | `client / tools` | Gradle owner tasks | `:client:clientSmoke`, `:client:goldenScreenshot`, `:tools:darkKeyRegistryLint`, `:tools:darkSpriteSheetLint`, `:tools:spriteSheetMapLint`, `:tools:darkManifestCoveragePr00DryRun`, `:tools:scopeCoverageLint`, `:tools:maintainabilityLint` | `build/verification/verify-changed/task-paths.txt`, `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `PASS` |

Projection enum evidence:

- `TileViewportFocusSourceKind`: `PLAYER`, `OVERLAY_INSPECT`, `OVERLAY_TARGETING`, `MODAL_INSPECT`, `MODAL_TARGETING`, `MODAL_COMBAT_DECISION`, `VALIDATION_INSPECT`.
- `ValidationProjectionReason`: `VALIDATION_FIXTURE`, `MANUAL_VALIDATION_PROBE`, `DEBUG_WHITEBOX_PROJECTION`.
- Explicit validation coverage: `TileViewportFocusProjectionTest.validationCursorRequiresExplicitInspectProjection`.

## Commands Run

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
rg -n -i 'AsciiRenderer|AsciiRenderModel|tileset_foundation_ascii|\.ascii|asciiGlyph|asciiColorHex' client/src/main assets-src/image/specs assets-src/image/manifests client/src/main/resources examples/content-packs tools/src/main/resources game/src/main/resources/data/tilesets
./gradlew :client:compileTestKotlin
./gradlew :client:test --tests com.ktome.client.render.TileViewportFocusProjectionTest --tests com.ktome.client.render.TileMapViewportTest --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.screen.FoundationViewportSupportTest --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.TileOverlayLayerTest
./gradlew :client:test --tests com.ktome.client.input.InputHandlerTest --tests com.ktome.client.ui.layout.ModalStackTest --tests com.ktome.client.ui.layout.PaneFocusControllerTest
./gradlew maintainabilityLint
./gradlew :client:test
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew :client:goldenScreenshot
./gradlew localeLint contractLint
./gradlew :client:clientSmoke
./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest
./gradlew :client:test
./gradlew maintainabilityLint
./gradlew :client:test maintainabilityLint acceptanceContractLint localeLint contractLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew verifyChanged
./gradlew :client:test --tests 'com.ktome.client.render.TileMapViewportTest' --tests 'com.ktome.client.render.TileOverlayLayerTest' --tests 'com.ktome.client.render.TileRendererCanvasTest' --tests 'com.ktome.client.render.GameShellLayoutTest' --tests 'com.ktome.client.render.TileLayerComposerTest' --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest.dark uiux pr01 1 golden evidence labels remain registered'
./gradlew :client:test maintainabilityLint
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew verifyChanged
./gradlew :client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest'
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
./gradlew :tools:test --tests 'com.ktome.tools.verification.VerificationImpactAnalyzerTest' --tests 'com.ktome.tools.verification.ScopeCoverageLintTest' --tests 'com.ktome.tools.verification.VerifyChangedBuildContractTest'
./gradlew maintainabilityLint
./scripts/verify-bootstrap.sh
./gradlew :client:clientSmoke :client:goldenScreenshot
./gradlew verifyChanged
./gradlew :client:packageMacApp
scripts/capture-macos-app-window.sh --pid <pid> --app-name "K-ToME PR01-1 Manual" --out build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/<label>.png --max-width 1280 --max-height 900
./gradlew :client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'
./gradlew :client:goldenScreenshot
./gradlew :client:clientSmoke maintainabilityLint
./gradlew verifyChanged
git diff --check
```

Result:

- `acceptanceContractLint`: passed before implementation.
- Production ASCII scan: no matches; exit status `1`.
- `:client:compileTestKotlin`: passed.
- Focused viewport / projection / layout / layer / overlay lane: passed.
- Focused input / modal lane: passed.
- `maintainabilityLint`: passed.
- `:client:test`: passed.
- First `:client:clientSmoke :client:goldenScreenshot`: `:client:clientSmoke` reached before the failure; `:client:goldenScreenshot` failed because fixed shell / viewport layout intentionally changed screenshot hashes. The focused tests were already green, so expected hashes were updated to the newly generated fixed-shell outputs.
- Review-fix focused viewport / overlay / canvas / layout / layer / golden-label lane: passed.
- Rerun `:client:goldenScreenshot`: build passed.
- `localeLint contractLint`: passed.
- Rerun `:client:clientSmoke`: build passed.
- `:client:test --tests com.ktome.client.assets.ManifestResolveTest`: passed after removing the legacy ASCII manifest strip path.
- Final `:client:test`: passed.
- Final `maintainabilityLint`: passed.
- Final combined `:client:test maintainabilityLint acceptanceContractLint localeLint contractLint`: passed.
- Final combined `:client:clientSmoke :client:goldenScreenshot`: passed with `ClientSmokeHarnessTest tests=16 skipped=0 failures=0 errors=0` and `GoldenScreenshotHarnessTest tests=11 skipped=0 failures=0 errors=0`.
- Latest `:client:test maintainabilityLint`: passed after adding `shellFrameCarriesPaneFocusFactAndTextLayoutOnly`.
- Review-fix `:client:goldenScreenshot --tests 'com.ktome.client.golden.GoldenScreenshotHarnessTest'`: first run intentionally failed on pending hashes and wrote `client/build/reports/golden/dark-uiux-pr01-1/evidence-index.tsv`; rerun passed after freezing the generated hashes.
- Review-fix `:client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'`: passed after exact snapshot/model dimension fail-fast coverage was added.
- Review-fix `:tools:test --tests ...VerificationImpactAnalyzerTest ...ScopeCoverageLintTest ...VerifyChangedBuildContractTest`: passed after routing `client-ui-evidence` to `:client:clientSmoke` and `:client:goldenScreenshot`.
- Review-fix `maintainabilityLint`: passed.
- Review-fix `./scripts/verify-bootstrap.sh`: passed after client Gradle wiring changed.
- Review-fix `:client:clientSmoke :client:goldenScreenshot`: passed with `ClientSmokeHarnessTest tests=16 skipped=0 failures=0 errors=0` and `GoldenScreenshotHarnessTest tests=12 skipped=0 failures=0 errors=0`.
- Latest `verifyChanged`: passed; impact analysis selected `client-ui-evidence` with `:client:clientSmoke` and `:client:goldenScreenshot`, dark UI/UX pipeline owner tasks, maintainability and scope coverage full tasks.
- Manual `:client:packageMacApp`: passed on 2026-05-09; `client/build/release/K-ToME.app` was copied to `build/whitebox/dark-uiux-pr01-1-manual-20260509/K-ToME-pr011-manual.app` with a unique local bundle id so Computer Use would not bind a stale same-id app from an older whitebox run.
- Computer Use manual pass: passed on 2026-05-09 against `com.ktome.client.pr011manual`; CUA steps used `Return`, arrow keys, `x`, `Backspace`, `1`, `Esc`, `i`, `Return`, `c`, and `1024x768` window resize. All screenshot evidence was captured by target macOS window id with `window_owner=K-ToME PR01-1 Manual` and `window_pid=75750`.
- Post-review `:client:test --tests 'com.ktome.client.render.TileRendererCanvasTest'`: passed with `TileRendererCanvasTest tests=52 skipped=0 failures=0 errors=0`; covers the narrowed `TileRenderDiagnostics` contract and right-panel four-section order.
- Post-review `:client:goldenScreenshot`: first run failed on expected hash drift after the right-panel section contract changed; after validating the new shell image and updating canonical hashes, rerun passed with `GoldenScreenshotHarnessTest tests=12 skipped=0 failures=0 errors=0`.
- Post-review `:client:clientSmoke maintainabilityLint`: passed with `ClientSmokeHarnessTest tests=16 skipped=0 failures=0 errors=0`; `maintainabilityLint` passed.
- Post-review `verifyChanged`: passed; selected `:client:clientSmoke`, `:client:goldenScreenshot`, dark UI/UX pipeline tasks, `:tools:scopeCoverageLint`, and `:tools:maintainabilityLint`.
- Post-review `git diff --check`: passed.

## Evidence

| Evidence | Path | Result | Notes |
| --- | --- | --- | --- |
| Focused client tests | `client/build/reports/tests/test/` | `PASS` | Covers viewport math, projection, typed frames, overlay arbitration, input/modal regressions. |
| Focused test XML | `client/build/test-results/test/` | `PASS` | Includes `TileMapViewportTest`, `TileViewportFocusProjectionTest`, `TileLayerComposerTest`, `TileOverlayLayerTest`, `TileRendererCanvasTest`, `FoundationViewportSupportTest`, `ManifestResolveTest`. |
| Golden gate | `client/build/reports/tests/goldenScreenshot/`, `client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml`, `client/build/reports/golden/dark-uiux-pr01-1/evidence-index.tsv` | `PASS` | `GoldenScreenshotHarnessTest tests=12 skipped=0 failures=0 errors=0`; includes `dark-uiux-pr01-1-*` evidence label registry, hash stability, screenshot artifact and side-by-side artifact generation. |
| Client smoke | `client/build/reports/tests/clientSmoke/`, `client/build/test-results/clientSmoke/TEST-com.ktome.client.ClientSmokeHarnessTest.xml` | `PASS` | `ClientSmokeHarnessTest tests=16 skipped=0 failures=0 errors=0`. |
| Governance gates | `build/reports/problems/problems-report.html` | `PASS` | `acceptanceContractLint`, `localeLint`, `contractLint`, `maintainabilityLint` passed. |
| VerifyChanged duration summary | `build/verification/verify-changed/task-paths.txt`, `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `PASS` | Latest plan includes `:client:clientSmoke`, `:client:goldenScreenshot`, dark UI/UX pipeline tasks and `:tools:maintainabilityLint`; client tasks were `UP-TO-DATE` because the owner gate had just passed. |
| Packaged app manual launch | `build/whitebox/dark-uiux-pr01-1-manual-20260509/K-ToME-pr011-manual.app`, `build/whitebox/dark-uiux-pr01-1-manual-20260509/app-executable.sha256`, `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/app.log` | `PASS` | Current app was copied under `build/whitebox` with bundle id `com.ktome.client.pr011manual` to avoid LaunchServices binding a stale `K-ToME-broken.app` with the same original bundle id. |
| Computer Use gameplay baseline | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-gameplay-baseline.png` | `PASS` | CUA `Return` from main menu reached live Tile dark UI; central map, left rail, right panel, bottom HUD and latest log were visible at 1280 width. SHA-256 `7096b9357556b8a054d64a7898b4c985ac044de8fb70f146a3a7c31ba474bff5`. |
| Computer Use edge clamp top-left | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-edge-top-left.png` | `PASS` | CUA arrow-key movement near the upper/left explored area kept map tiles aligned with no black negative-tile gutter or half-cell draw. SHA-256 `7a1cdb439f417644871b32555aa39197157b102efde00fbdebffbadc02ae6fe8`. |
| Computer Use edge clamp bottom-right | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-edge-bottom-right.png` | `PASS` | CUA arrow-key movement toward the right/down area kept actor, fog, and floor tiles aligned in the same map viewport transform. SHA-256 `9dfc7385a4035b9ea0ca6d13ef285d892b498e0b57870e6cc69458b4fd580252`. |
| Computer Use inspect modal / tooltip layer | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-inspect-tooltip-layer.png` | `PASS` | CUA `x` opened Inspect; modal sat above map/shell, retained readable zh-CN text, and `Backspace` returned to world focus. SHA-256 `6f31ce19dbdc862a356380c663f643a85ea749c6992085ddc57ab667653b7f1c`. |
| Computer Use combat decision modal | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-combat-decision-modal.png` | `PASS` | CUA `1` opened Combat Decision; modal/backdrop stayed above map and below no conflicting tooltip, while bottom latest-log area remained visible. SHA-256 `cf9d8c077117084fd20ed0ae8742dcb442b8b42e802576418a2d2bdd9363ecac`. |
| Computer Use item detail modal | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-item-modal-layer.png` | `PASS` | CUA `i`, `Return` opened Item Detail; item detail used overlay modal layer, did not cover latest bottom log, and right panel retained item summary. SHA-256 `91303e55b40fe0c209e71871a76f724f354d4159820e66ee8261044b22ec77e0`. |
| Computer Use item compare modal | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-modal-backdrop-stack.png` | `PASS` | CUA `c` from Item Detail opened Item Compare; backdrop stayed single-layer and footer hints remained readable. SHA-256 `1efc3ff80c1257f741d1ec1e494e18e1dc09999bbd925a145c9120a6bb546f87`. |
| Computer Use minimum window | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-shell-min-window.png` | `PASS` | Window was resized to `1024x768`; left rail, map viewport, right panel, bottom HUD and latest log remained partitioned without incoherent overlap. SHA-256 `db141432d23609b8a65905d569e8a2e106fcd6534979936970435bb9d6e4a9b6`. |

## Frame Ownership Self-Audit

| Frame / owner | Field list | Count | Immutable models | Referenced truth | Raw `OverlayState` | Duplicated cell metrics | Full aggregate model | Notes |
| --- | --- | ---: | --- | --- | --- | --- | --- | --- |
| `MapRenderFrame` | `model`, `layerPlan`, `layout`, `viewport` | 4 | `TileRenderModel`, `TileMapLayerPlan` | `TileMapViewport`, `GameShellLayout` through `TileLayoutMetrics` | `no` | `no` | `no` | Map pass consumes `TileMapViewport` as tile-to-screen truth; cell size is accessed through viewport. |
| `ShellRenderFrame` | `model`, `layout`, `textLayout`, `paneFocusAnchor` | 4 | `TileRenderModel`, `ShellTextLayout` | `GameShellLayout` through `TileLayoutMetrics`; pane focus fact only | `no` | `no` | `no` | Shell pass consumes precomputed text and pane focus fact; it no longer receives raw `OverlayState`. |
| `OverlayRenderFrame` | `overlayModel`, `shellContentBounds`, `modalSafeBounds`, `bottomLogReservedBounds`, `textMetrics` | 5 | `TileOverlayModel`, precomputed `TileTextLine` lists | `GameShellLayout` primary outputs | `no` | `no` | `no` | Overlay pass does not expose raw viewport, raw modal frames, `ModalStack.top()`, raw projection or raw overlay state. |
| `TileRenderDiagnostics` | `viewport`, `overlayFrame` | 2 | diagnostics only | `TileMapViewport`, `OverlayRenderFrame` | `no` | `no` | `no` | Test-only diagnostics returned by `renderToCanvas`; it no longer exposes `projection`, `layerPlan`, model slices or a total renderer frame aggregate. |

- Renderer pass owners: `TileMapRenderer`, `TileShellRenderer`, `TileOverlayRenderer`; `TileRenderer.renderToCanvas` performs model/layout/projection/viewport/model-frame orchestration and dispatch.
- `modalFrames order truth`: `OverlayState.modalFrames` is bottom-to-top; `ModalStackTest.framesAreBottomToTopForProjection` and `TileViewportFocusProjectionTest.scansModalFramesTopToBottomFromBottomFirstList` cover this.
- `anchor resolution owner`: `FrameTileOverlayAnchorResolver`; `WORLD_TILE` resolves through current `TileMapViewport.tileRect`, panel/modal/row anchors resolve through typed rects.
- `precomputed text layout fields`: `ShellTextLayout.playerName`, `summaryLines`, `targetTitle`, `targetLines`, `messageLines`, `leftRail`, `rightPanel`, `footerHints`, `hotbar`; plus `TileTooltipModel.titleLine`, `TileTooltipModel.bodyLines`, `TileModalModel.titleLine`, `TileModalModel.bodyLines`, `TileModalModel.footerHintLines`, `TileToastModel.line`, `TileDebugHintModel.line`.
- `temporary dual-write fields`: `OverlayState.targetingCursor`, `OverlayState.inspectCursor`, `ModalFrameLocalState.targetingCursor`, `ModalFrameLocalState.inspectCursor`.
- `current check owner`: `TileViewportFocusProjection`.
- `deletion condition`: remove overlay-level cursor fallback fields when modal-local cursor becomes the sole spatial cursor authority for inspect / targeting / combat decision.

## Overlay Conflict Evidence

| Conflict | Evidence | Result |
| --- | --- | --- |
| Modal explicit tooltip wins and passive tooltip is suppressed | `TileOverlayLayerTest.selectsModalExplicitTooltipBeforePassiveTooltip`, `TileOverlayLayerTest.recordsSuppressedPassiveTooltipWhenModalActive` | `PASS` |
| Passive tooltip is suppressed while modal is active | `TileOverlayLayerTest.activeModalSuppressesPassiveAnchoredTooltip`, `TileRendererCanvasTest.overlayDrawsAboveShellAndBottomHud` | `PASS` |
| Corner anchors flip inside shell bounds | `TileOverlayLayerTest.cornerAnchorsFlipInsideShellBounds` | `PASS` |
| Tooltip right/down/left/up candidate order uses production renderer | `TileOverlayLayerTest.productionTooltipPlacementUsesRightDownLeftUpCandidateOrder` | `PASS` |
| WORLD_TILE anchor is resolved by the viewport authority | `TileOverlayLayerTest.cornerAnchorsUseTileMapViewportResolvedWorldTileRect` | `PASS` |
| Anchor outside visible range is recorded instead of guessed | `TileOverlayLayerTest.recordsAnchorOutsideVisibleRangeSuppressionReason` | `PASS` |
| Missing presenter rect is recorded as anchor resolution failure | `TileOverlayLayerTest.recordsAnchorResolutionFailedWhenPresenterRectMissing` | `PASS` |
| Tooltip avoids bottom log reserved bounds | `TileRendererCanvasTest.tooltipAvoidsBottomLogReservedBounds` | `PASS` |
| Modal uses layout-owned safe bounds | `TileOverlayLayerTest.usesLayoutModalSafeBoundsWithoutWindowRecalculation`, `TileRendererCanvasTest.modalUsesModalSafeBounds` | `PASS` |

## ASCII Deletion Scan

Production scan command:

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|tileset_foundation_ascii|\.ascii|asciiGlyph|asciiColorHex' client/src/main assets-src/image/specs assets-src/image/manifests client/src/main/resources examples/content-packs tools/src/main/resources game/src/main/resources/data/tilesets
```

Result:

```text
productionScanStatus=1
productionHitCount=0
allowedNonClientRendererHits=none
negativeFixtureEvidence=ManifestResolveTest keeps asciiGlyph/asciiColorHex only in rejected-manifest fixtures outside the production scan path
```

- No client runtime, resource manifest, content-pack or tool fixture production matches.
- `ManifestResolveTest` intentionally keeps `asciiGlyph` / `asciiColorHex` only as negative fixtures proving v1 and v2 manifests now reject removed ASCII fields.

## Image Region Comparison

| Region | Evidence | Result | Notes |
| --- | --- | --- | --- |
| Central map dominance | `GameShellLayoutTest.keepsMapAreaAtLeastHalfShellUsableContentAt1280x800`, `TileRendererCanvasTest.viewportUsesShellMapBounds`, `dark-uiux-pr01-1-cua-gameplay-baseline.png` | `PASS` | Programmatic `mapBounds.width * mapBounds.height >= 0.5 * shellUsableContentArea`; live CUA baseline shows the map as first visual focus with actor / fog / loot / cursor readable. |
| Left rail actual entries | `dark-uiux-pr01-1-cua-gameplay-baseline.png`, `TileRenderModel.buildShell` | `PASS` | Actual baseline entries: `破碎前哨`, `层 1/2`, zone description, `任务`, `目标：突破前哨。`, `关键提示`, route/zone hint. The rail does not repeat full HP/resource/equipment state. |
| Right panel four-section order | `TileRendererCanvasTest.shell right panel keeps ground equipment inscriptions and backpack sections in order`, `:client:goldenScreenshot` | `PASS` | Post-fix right panel contract order is `Ground` / `Equipment` / `Inscriptions` / `Backpack`; empty sections render `(empty)` instead of disappearing, so missing ground loot or no inscriptions no longer drops a required section. |
| Bottom HUD actual positions | `dark-uiux-pr01-1-cua-gameplay-baseline.png`, `TileRendererCanvasTest.shell model keeps hp resource and xp in bottom hud instead of right panel` | `PASS` | HP/resource/XP gauges live in the left bottom HUD block; attack/defense summary lives in the bottom-right focus summary; hotbar cards sit bottom-left center; hotkey legend sits at the bottom edge; latest log occupies the center log panel. |
| Tooltip / modal / bottom log non-overlap | `TileRendererCanvasTest.tooltipAvoidsBottomLogReservedBounds`, `TileRendererCanvasTest.modalUsesModalSafeBounds`, `dark-uiux-pr01-1-cua-item-modal-layer.png` | `PASS` | Uses canvas geometry evidence and CUA item-detail screenshot; modal layer stays within `modalSafeBounds` and leaves bottom latest-log space readable. |
| Golden screenshot comparison | `:client:goldenScreenshot`, `client/build/reports/golden/dark-uiux-pr01-1/evidence-index.tsv` | `PASS` | `GoldenScreenshotHarnessTest tests=12 skipped=0 failures=0 errors=0`; writes all `dark-uiux-pr01-1-*` PNG artifacts. |
| Reference side-by-side artifact | `client/build/reports/golden/dark-uiux-pr01-1/dark-uiux-pr01-1-tome-layout-reference-side-by-side.png` | `PASS` | Generated by the golden owner test; it places `UI/UI-demo.png` beside the captured fixed-shell runtime frame for image-region comparison evidence. |
| Manual image whitebox | `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-gameplay-baseline.png`, `build/whitebox/dark-uiux-pr01-1-manual-20260509/evidence/dark-uiux-pr01-1-cua-shell-min-window.png` | `PASS` | Computer Use confirmed live partitioning at 1280 width and 1024x768. Visual material differences from `UI/UI-demo.png` remain PR-02 / PR-05 scope; post-fix right-panel four-section order is locked by focused test and refreshed golden evidence. |

## Deadzone Still / Scroll Evidence

| Scenario | Evidence | `viewportTopLeft` | `mapBounds` | `deadzoneStart` | `deadzoneEndExclusive` | Result |
| --- | --- | --- | --- | --- | --- | --- |
| Still inside half-open deadzone | `TileMapViewportTest.keepsTopLeftInsideDeadzone`, `dark-uiux-pr01-1-viewport-deadzone-still.png` | `(3, 3) -> (3, 3)` in the focused 60x40 / 10x10 visible-range fixture | `RectInt(0, 0, 320, 320)` | `x=3`, `y=4` | `x=7`, `y=7` | `PASS`; one-cell move stays inside `[start, endExclusive)` and does not scroll. |
| Scroll after crossing deadzone | `TileMapViewportTest.snapsWhenHorizontalJumpExceedsThreshold`, `dark-uiux-pr01-1-viewport-deadzone-scroll.png` | recentered after crossing the horizontal deadzone threshold | `RectInt(0, 0, 320, 320)` in the owner fixture; golden uses runtime shell `mapBounds` | `x=3`, `y=4` | `x=7`, `y=7` | `PASS`; crossing beyond the half-open interval changes viewport on whole-cell boundaries only. |

## Findings

1. `FIXED`: Initial golden hash run showed broad expected-hash drift after moving from snapshot-size-driven world size to fixed shell viewport. Focused viewport / canvas tests were green, so golden expected hashes were updated to the new fixed-shell output and `:client:goldenScreenshot` was rerun.
2. `FIXED`: Client visual manifest loader still stripped legacy v1 `asciiGlyph` / `asciiColorHex` fields. The strip path was removed and `ManifestResolveTest` now proves removed ASCII fields are rejected for v1 and v2 manifests.
3. `FIXED`: Initial implementation had typed frames but renderer pass ownership was still too implicit. `TileMapRenderer`, `TileShellRenderer` and `TileOverlayRenderer` now own map/shell/overlay pass dispatch behind `TileRenderer.renderToCanvas`.
4. `FIXED`: Review found player jump snap was skipped in `INSPECT` / `TARGETING`. `TileMapViewport` now snaps any player jump that exceeds the axis deadzone threshold, and `TileMapViewportTest` covers inspect and targeting modes.
5. `FIXED`: Review found `TileTextMetrics` existed but renderer draw code still owned duplicate wrapping/truncation helpers. `ShellTextLayout` now precomputes shell text, panels, messages and hotbar labels before shell rendering, and renderer-private duplicate text helpers were removed.
6. `FIXED`: Review found overlay focused tests used a fake probe. `TileOverlayLayerTest` now calls production `TileRenderer.renderOverlayFrame`, and asserts the right/down/left/up tooltip placement candidate order from rendered rects.
7. `FIXED`: Additional suggestions were absorbed: production render path now requires exact model/snapshot map dimensions; layer flush order is asserted as a full sequence; `GameShellLayoutTest` covers the 3x3 viewport shell/map matrix; `GoldenScreenshotHarnessTest` registers and hashes the canonical `dark-uiux-pr01-1-*` evidence labels.
8. `FIXED`: `verifyChanged` now has a `client-ui-evidence` owner domain, routes client renderer/UI/test changes to `:client:clientSmoke` and `:client:goldenScreenshot`, and protects that routing with impact/scope/build-contract tests.
9. `FIXED_FOR_MANUAL`: Computer Use initially resolved `com.ktome.client` to an older `K-ToME-broken.app` leftover from a previous whitebox run. The stale process was terminated, the current packaged app was copied under `build/whitebox` with unique local bundle id `com.ktome.client.pr011manual`, and all final CUA evidence was taken from that unique bundle.
10. `FIXED`: Review found the `renderToCanvas` diagnostics return still looked like a total frame aggregate. `TileRenderFrameSummary` was replaced with narrow `TileRenderDiagnostics`, exposing only `viewport` and `overlayFrame`; `TileRendererCanvasTest.renderDiagnosticsDoesNotExposeAggregateFrameState` locks this boundary.
11. `FIXED`: Review found the Image Region record and right-panel evidence were too coarse. The right panel now keeps the required `Ground` / `Equipment` / `Inscriptions` / `Backpack` section order even when a section is empty, and the manual record now lists left rail entries, bottom HUD positions, ASCII hit count and deadzone metadata.

## Removal / Iteration Plan Follow-up

- Removed the fake overlay renderer probe from focused overlay tests; tooltip placement evidence now routes through production `TileOverlayRenderer` via `TileRenderer.renderOverlayFrame`.
- Removed duplicate renderer-private text wrapping/truncation helpers; `TileTextMetrics` is the only text metric owner and `ShellTextLayout` is the shell draw-pass input.
- Removed the total diagnostics aggregate shape from `renderToCanvas`; tests now receive `TileRenderDiagnostics` with only `viewport` and `overlayFrame`, so `projection` and `layerPlan` stay inside orchestration.
- Kept the planned cursor-authority iteration explicit: `OverlayState.targetingCursor` / `inspectCursor` and modal-local cursor fields are still temporary dual-write inputs until modal-local cursor becomes the sole spatial cursor authority.
- Absorbed Additional Suggestions into executable checks: right-panel four-section order, 3x3 layout matrix, exact production map-dimension fail-fast, full layer flush sequence, production tooltip placement test, `dark-uiux-pr01-1-*` evidence hash/artifact generation, and `verifyChanged` client UI evidence routing.

## Residual Risk

- The live manual map reached through quick start is a compact starting layout, so long-corridor deadzone still/scroll, four-corner tooltip flip, targeting cursor viewport, overlay conflict fixture and full side-by-side ToME comparison remain covered by `client/build/reports/golden/dark-uiux-pr01-1/evidence-index.tsv` and focused owner tests rather than only by the live CUA path.
- The packaged-app runtime reported an existing progress-profile load failure in the main menu and explicitly did not write new meta progress for this run. This did not block starting a new run or validating viewport / overlay rendering.
- The final Computer Use bundle id is a build-only local copy (`com.ktome.client.pr011manual`) created to avoid stale LaunchServices resolution; the executable and resources come from `client/build/release/K-ToME.app`.
- Overlay chrome remains primitive by design; PR-02 resource work may replace paint sources but must not change this PR's viewport / renderer / overlay model contracts.
