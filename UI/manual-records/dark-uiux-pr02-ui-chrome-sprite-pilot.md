# Dark UI/UX PR-02 UI Chrome Sprite Pilot Manual Record

## Summary

| field | value |
| --- | --- |
| recordStatus | `HUMAN_CUA_PASS_AFTER_LAYOUT_FIT_FIX` |
| recordedAt | `2026-05-10 11:47:14 CST` |
| layoutFitFixRecordedAt | `2026-05-10 13:07:47 CST` |
| layoutFitFinalRecordedAt | `2026-05-10 15:50:05 CST` |
| promptIndexPath | `UI/sprite-sheets/prompts/dark-v1/prompt-index.json` |
| spriteMapReportPath | `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` |
| coverageReportPath | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` |
| verifyChangedDurationSummaryPath | `build/verification/verify-changed/full-task-duration-summary.md` |
| goldenEvidenceIndexPath | `client/build/reports/golden/dark-uiux-pr02/evidence-index.tsv` |
| goldenLabels | `dark-uiux-pr02-round1-chrome`, `dark-uiux-pr02-hud-icons-pilot`, `dark-uiux-pr02-standalone-screen-chrome` |
| canonicalManifestPath | `assets-src/image/manifests/phase2-visual-manifest.json` |
| runtimeManifestPath | `client/src/main/resources/manifests/visual-manifest.json` |
| codexSmokeReports | `build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-chrome.json`, `build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-controls.json`, `build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-hud-icons.json` |
| postProcessScript | `scripts/repack_generated_sheet.py` |
| manualReviewer | `Codex Computer Use packaged-app whitebox` |
| reviewedAt | `2026-05-10 15:50:05 CST` |
| manualNotRunReason | `N/A; post-fix packaged target-window evidence was captured for shell/HUD, inventory/modal, validation overlay, and a standalone/main-menu surface. Computer Use accessibility discovery still could not read the GLFW window, so interaction was completed by direct CGEvent key posting to the packaged app pid before target-window screenshots were captured.` |

## Prompt And Raw Sheet Evidence

| sheetId | promptPath | rawSheetPath | rawSheetHash | contactSheetPath | smokeReportPath |
| --- | --- | --- | --- | --- | --- |
| `r01-ui-chrome` | `UI/sprite-sheets/prompts/dark-v1/001-r01-ui-chrome.prompt.txt` | `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png` | `478712c3761da4c8a2836daecc4c76832f83e0b249f32b45e21035730ca83371` | `assets-src/image/contact-sheets/dark-v1/r01-ui-chrome-contact.png` | `build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-chrome.json` |
| `r01-ui-controls` | `UI/sprite-sheets/prompts/dark-v1/002-r01-ui-controls.prompt.txt` | `assets-src/image/raw/sheets/dark-v1/r01-ui-controls.png` | `6a0a35f452c4e25272bc5dd3006d7ea2d3b93030e45907bed58c150991cf1622` | `assets-src/image/contact-sheets/dark-v1/r01-ui-controls-contact.png` | `build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-controls.json` |
| `r01-ui-hud-icons` | `UI/sprite-sheets/prompts/dark-v1/003-r01-ui-hud-icons.prompt.txt` | `assets-src/image/raw/sheets/dark-v1/r01-ui-hud-icons.png` | `b294bc0eaf5e6ea6d989cc99a5fb99f2e952ab8167fa8f4579edcead12f4e2e4` | `assets-src/image/contact-sheets/dark-v1/r01-ui-hud-icons-contact.png` | `build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-hud-icons.json` |

Raw sheets were generated through `scripts/codex-generate-image.py` from the three PR-02 prompt files. The script now runs `codex exec` through a PTY, uses a read-only isolated workdir, copies the newest Codex generated image, normalizes the prompt-declared canvas size, and removes the generated checkerboard background before writing the canonical raw PNG.

The generated subjects were then repacked with `scripts/repack_generated_sheet.py --overwrite` so every subject aligns with the immutable `sheet-plan.yaml` grid contract. This keeps the document rule intact: generated image drift is corrected by regenerating or post-processing the raw sheet, not by changing row/col ownership to fit a bad image. Transient Codex source folders are intentionally not copied into this record because committed manual records must remain repo-relative.

Follow-up fix on `2026-05-10`: `ui.hud.quest_marker.icon`, `ui.hud.log_marker.icon`, and `ui.hud.warning.icon` were visually inspected after user feedback. The previous HUD sheet had edge fragments in the last two cells, so `r01-ui-hud-icons` was regenerated with a stricter promptBase requiring isolated centered icons, wide transparent padding, no overlap, and no cropped edge decorations. A second follow-up found the regenerated source image was `1774x887`; `scripts/codex-generate-image.py` now preserves aspect ratio when normalizing to the prompt canvas instead of stretching wide source images into a square. `scripts/repack_generated_sheet.py` now skips oversized splitting when the generated alpha component count already matches the direct cell count, preventing large but isolated icons from being mis-split.

## Manifest Diff Summary

| field | value |
| --- | --- |
| ownerContract | `UI/sprite-sheets/owner-contracts/pr02-owner-keys.yaml` contains `42` required direct keys. |
| keyRegistry | `UI/sprite-sheets/key-registry.yaml` contains `42` `ownerPr: PR-02` entries. |
| runtimePngCount | `client/src/main/resources/dark-v1/ui/` contains `42` PR-02 sliced PNGs. |
| canonicalManifestDiff | `assets-src/image/manifests/phase2-visual-manifest.json` contains `42` `ui.*` entries with `rawOutputPath` under `dark-v1/ui/`. |
| runtimeManifestDiff | `./gradlew syncPhase2Manifests` synced the canonical visual manifest into `client/src/main/resources/manifests/visual-manifest.json`. |
| spriteMapSummary | `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` contains `42` mapped direct cells with repo-relative raw sheet and output paths. |
| contactSheetQa | `CODEX_VISUAL_CHECKED`; `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` contains `42` rows with `qaStatus=CODEX_VISUAL_CHECKED`, `reviewer=Codex contact-sheet visual inspection`, `reviewedAt=2026-05-10T11:31:17+08:00`. |

## Coverage Artifact Excerpt

| field | value |
| --- | --- |
| schemaVersion | `dark-v1-manifest-coverage-v1` |
| scopeMode | `owner-scope` |
| ownerPr | `PR-02` |
| status | `PASS` |
| requiredOwnerSheetIds | `["r01-ui-chrome", "r01-ui-controls", "r01-ui-hud-icons"]` |
| ownerSheetIds | `["r01-ui-chrome", "r01-ui-controls", "r01-ui-hud-icons"]` |
| requiredOwnerKeyCountBySheet | `{"r01-ui-chrome": 15, "r01-ui-controls": 19, "r01-ui-hud-icons": 8}` |
| ownerExpectedKeyCountBySheet | `{"r01-ui-chrome": 15, "r01-ui-controls": 19, "r01-ui-hud-icons": 8}` |
| ownerMissingKeys | `[]` |
| ownerMissingRequiredKeys | `[]` |
| ownerUnexpectedKeys | `[]` |
| ownerPendingKeys | `[]` |
| ownerOldStyleKeys | `[]` |
| allowedOwnerFallbackKeys | `[]` |
| scopeExternalPendingKeys | `[]` |

`requiredOwnerKeys`, `ownerExpectedKeys`, and `ownerCoveredKeys` are identical:

```text
ui.combat.action.icon
ui.combat.invalid.icon
ui.combat.lock.icon
ui.combat.method.icon
ui.combat.target.icon
ui.control.back.icon
ui.control.backpack.icon
ui.control.confirm.icon
ui.control.copy.icon
ui.control.equipment.icon
ui.frame.modal.body
ui.frame.panel.body
ui.frame.panel.corner_bl
ui.frame.panel.corner_br
ui.frame.panel.corner_tl
ui.frame.panel.corner_tr
ui.frame.panel.edge_bottom
ui.frame.panel.edge_left
ui.frame.panel.edge_right
ui.frame.panel.edge_top
ui.frame.panel.focus
ui.frame.slot.empty
ui.frame.slot.equipped
ui.frame.slot.selected
ui.frame.tooltip.body
ui.hud.gold.icon
ui.hud.hp.icon
ui.hud.key.icon
ui.hud.log_marker.icon
ui.hud.quest_marker.icon
ui.hud.stamina.icon
ui.hud.warning.icon
ui.hud.xp.icon
ui.screen.error.marker
ui.screen.loading.marker
ui.screen.outcome.defeat_marker
ui.screen.outcome.victory_marker
ui.screen.validation.badge
ui.state.active.icon
ui.state.learnable.icon
ui.state.locked.icon
ui.state.reserve.icon
```

## Automated Evidence

| command | result | note |
| --- | --- | --- |
| `./gradlew acceptanceContractLint` | `PASS` | Preflight governance contract check before implementation. |
| `python3 scripts/generate_sheet_prompt.py` | `PASS` | Generated `3` PR-02 prompt files and `prompt-index.json`. |
| `python3 scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/001-r01-ui-chrome.prompt.txt)" --out assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png --smoke-report build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-chrome.json --timeout-seconds 300 --overwrite` | `PASS` | Generated and normalized `r01-ui-chrome`. |
| `python3 scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/002-r01-ui-controls.prompt.txt)" --out assets-src/image/raw/sheets/dark-v1/r01-ui-controls.png --smoke-report build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-controls.json --timeout-seconds 300 --overwrite` | `PASS` | Generated and normalized `r01-ui-controls`. |
| `python3 scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/003-r01-ui-hud-icons.prompt.txt)" --out assets-src/image/raw/sheets/dark-v1/r01-ui-hud-icons.png --smoke-report build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-hud-icons.json --timeout-seconds 300 --overwrite` | `PASS` | Regenerated and normalized `r01-ui-hud-icons` after the first generated sheet produced residual edge fragments in `quest/log/warning` cells. |
| `python3 scripts/repack_generated_sheet.py --overwrite` | `PASS` | Repacked generated subjects into the sheet-plan cells without changing formal row/col contracts. |
| `python3 scripts/render_contact_sheet.py --overwrite` | `PASS` | Wrote `3` contact sheets with row/col/key labels. |
| `python3 scripts/slice_spritesheet.py --overwrite` | `PASS` | Wrote `42` runtime PNGs, `skippedPending=0`. |
| `python3 scripts/verify_sprite_sheet_map.py --check map --report assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` | `PASS` | Wrote/validated the PR-02 sprite map report. |
| `python3 -m py_compile scripts/codex-generate-image.py scripts/repack_generated_sheet.py` | `PASS` | Python syntax check for the image-generation and repack scripts. |
| `./gradlew :tools:test --tests com.ktome.tools.darkuiux.DarkSpriteSheetPipelineScriptTest` | `PASS` | Covers Codex image script PTY/read-only invocation, canvas normalization, dark owner sheet-plan precedence, and formal owner sheets without alias cells. |
| `./gradlew syncPhase2Manifests manifestLint assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint -Pktome.darkUiux.requireFullGrid=true -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-owner-keys.yaml -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` | `PASS` | Resource gate for registry, manifest, full grid, sliced PNGs, and sprite-map report. |
| `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest --tests com.ktome.client.screen.MainMenuScreenTextTest` | `PASS` | Covers all 42 owner key exact resolves, TileRenderer panel/slot/modal/HUD asset draws, standalone chrome consumption/fallback, and main menu text contract. |
| `unset JAVA_TOOL_OPTIONS && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.render.InfoSurfaceLayoutTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest --tests com.ktome.client.screen.MainMenuScreenTextTest --tests com.ktome.client.assets.ManifestResolveTest` | `PASS` | Layout-fit rerun after replacing stretched panel body rendering with PR-02 corner/edge/body frame drawing, CJK-aware bounded shell text, and a shorter hotbar that gives the bottom HUD cards enough vertical space. |
| `./gradlew darkManifestCoverageLint --rerun-tasks -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-owner-keys.yaml` | `PASS` | Owner-scope expected keys = `42`; missing, pending, old-style, fallback, and external pending lists are empty. |
| `unset JAVA_TOOL_OPTIONS && source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:goldenScreenshot` | `PASS` | Rerun passed after golden hashes were rebaselined to the final chrome/text layout fit. |
| `./gradlew :client:goldenScreenshot --tests "*dark uiux pr02*"` | `PASS` | Wrote PR-02 golden artifacts and `client/build/reports/golden/dark-uiux-pr02/evidence-index.tsv`; final hashes: standalone `11cd74c8765a1f8017b72042381480379cbb89989e8b75351df5e0baf663ea8b`, round1 `34ee37a2d921677f925edfef667760581d7909089732a5046b85a5e88372ebbb`, HUD `9dc97b55cfc4980e6fba1477b7c242ae8405f614adb56b997a011512a1df716f`. |
| `./gradlew :tools:spriteSheetMapLint :tools:darkManifestCoveragePr02OwnerScope -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` | `PASS` | PR-02 sprite map lint preserved `42` `CODEX_VISUAL_CHECKED` rows; fixed owner-scope coverage task reported `expectedKeys=42`. |
| `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-02 -Pktome.darkUiux.ownerContract=UI/sprite-sheets/owner-contracts/pr02-owner-keys.yaml` | `PASS` | Root owner-scope close gate reported `expectedKeys=42`. |
| `./gradlew maintainabilityLint verifyChanged` | `PASS` | `verifyChanged` selected `8` tasks and now routes dark UI coverage through `:tools:darkManifestCoveragePr02OwnerScope`; duration summary written to `build/verification/verify-changed/full-task-duration-summary.md`; `totalDurationMillis=71080`. |
| `unset JAVA_TOOL_OPTIONS && ./gradlew :game:test --tests com.ktome.game.validation.ValidationScenarioRegistryTest :client:test --tests com.ktome.client.screen.ValidationScenarioBootstrapTest --tests com.ktome.client.input.ValidationCommandSourceTest :tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest` | `PASS` | Confirms the PR-02 fast whitebox scenario is registered in game/client/tools, materializes expected evidence names, and keeps the launch script quoting valid. |
| `unset JAVA_TOOL_OPTIONS && source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:clientSmoke :client:goldenScreenshot maintainabilityLint verifyChanged` | `PASS` | Owner gate after frame/text layout fit changes. `verifyChanged` selected `:client:clientSmoke`, `:client:goldenScreenshot`, dark UI owner gates, keyword/contract preflights, and `maintainabilityLint`. |
| `unset JAVA_TOOL_OPTIONS && source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr02-ui-chrome-sprite-pilot` | `PASS` | Packaged app and Phase4 v4 whitebox materialization regenerated after the launcher/window-position and bounded text changes. |

## Whitebox And Contact Sheet QA

| label | status | replacement evidence | residual risk |
| --- | --- | --- | --- |
| `dark-uiux-pr02-round1-chrome` | `HUMAN_CUA_PASS_AFTER_LAYOUT_FIT_FIX` | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-shell-hud-frame-fit.png`, `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-inventory-modal-frame-fit.png`, `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-round1-chrome.png`, `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets`, owner-scope coverage | No residual chrome/text overlap observed in packaged shell/HUD or inventory modal evidence. |
| `dark-uiux-pr02-hud-icons-pilot` | `HUMAN_CUA_PASS_AFTER_LAYOUT_FIT_FIX` | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-shell-hud-frame-fit.png`, `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-hud-icons-pilot.png`, direct local inspection of `assets-src/image/contact-sheets/dark-v1/r01-ui-hud-icons-contact.png`, `TileRendererCanvasTest.darkUiuxPr02DrawsPanelSlotModalAndHudAssets`, owner-scope coverage | HUD icons resolve and fit inside the post-fix bottom HUD card layout. |
| `dark-uiux-pr02-standalone-screen-chrome` | `HUMAN_CUA_PASS_AFTER_LAYOUT_FIT_FIX` | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-runtime-error-loading-fit.png`, `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-validation-overlay-frame-fit.png`, `client/build/reports/golden/dark-uiux-pr02/dark-uiux-pr02-standalone-screen-chrome.png`, `StandaloneScreenLayoutTest.darkUiuxPr02StandaloneChromeConsumesManifestKeys`, `StandaloneScreenLayoutTest.darkUiuxPr02RuntimeLoadingAndErrorChromeConsumesManifestMarkers`, `MainMenuScreenTextTest`, `clientSmoke`, `goldenScreenshot` | The standalone/main-menu surface is captured under the legacy `runtime-error-loading-fit` filename; runtime loading/error/outcome still share the same bounded standalone layout tests and golden/client gates. |
| `dark-uiux-pr02-contact-sheet-qa` | `HUMAN_CUA_PASS` | Direct visual inspection of `assets-src/image/contact-sheets/dark-v1/r01-ui-chrome-contact.png`, `assets-src/image/contact-sheets/dark-v1/r01-ui-controls-contact.png`, and `assets-src/image/contact-sheets/dark-v1/r01-ui-hud-icons-contact.png`; `spriteSheetMapLint`; `assets-src/image/manifests/dark-v1-pr02-sprite-map-report.jsonl` has `42` `CODEX_VISUAL_CHECKED` rows. | 未观察到文字、水印、串格、跨格；reserved empty regions remain intentionally blank. |

## Layout Fit Finding And Fix

| field | value |
| --- | --- |
| initialFindingStatus | `HUMAN_CUA_LIMITED_WITH_LAYOUT_FINDING` |
| findingSource | User-provided packaged-app screenshot in the PR-02 whitebox review plus the prior packaged evidence paths listed above. |
| findingSummary | PR-02 assets were generated and resolved, but the actual UI still used fixed-coordinate text and stretched panel body chrome, causing left/right rail text overflow, bottom HUD text/bar overlap, and panel decorations that did not match content bounds. |
| fixScope | `client` only; no `core`, `game`, save, replay, schema, or manifest owner-key contract changes. |
| implementedFix | Added frame-based PR-02 chrome drawing with corner/edge/body assets, CJK-aware bounded text width estimation and wrap/truncate, chrome content insets for shell/tooltip/modal/standalone text, tooltip height/content-fit correction, main-menu creation panel/footer slot fit, and a shorter hotbar that gives bottom HUD cards enough vertical space. |
| focusedAfterEvidence | `unset JAVA_TOOL_OPTIONS && source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:test --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.render.InfoSurfaceLayoutTest --tests com.ktome.client.screen.StandaloneScreenLayoutTest --tests com.ktome.client.screen.MainMenuScreenTextTest --tests com.ktome.client.assets.ManifestResolveTest` returned `BUILD SUCCESSFUL`. |
| postFixPackagedCuaStatus | `HUMAN_CUA_PASS_AFTER_LAYOUT_FIT_FIX` |

Packaged app / CUA evidence:

| field | value |
| --- | --- |
| appBundle | `client/build/release/K-ToME.app` |
| appExecutable | `client/build/release/K-ToME.app/Contents/MacOS/K-ToME` |
| scenarioAppPid | `78140` |
| standaloneAppPid | `77722` |
| runtimeErrorAppPid | `N/A; a normal standalone/main-menu surface was captured as the PR-02 touched standalone surface in this run.` |
| runtimeHome | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/runtime-home` |
| computerUseTarget | `com.ktome.client` |
| launchNote | `launch-packaged-app.sh` now starts the current packaged app after `DesktopLauncher` sets an explicit window position; this avoids the libGDX primary-monitor centering NPE seen before the fix. |
| mapHudScreenshot | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-shell-hud-frame-fit.png`, SHA-256 `f9ff41868cc76f2658687ecb05dc15d9f3aed83484f3ed3c0c9c2a56bb0f6af0` |
| inventoryModalScreenshot | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-inventory-modal-frame-fit.png`, SHA-256 `cefe217f3b563b1f32ed8c946fe7feb1e2ef6a0ad78dfc56d749bb5664847fe0` |
| validationOverlayScreenshot | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-validation-overlay-frame-fit.png`, SHA-256 `15ee47c95788e57c96eab606000fc32eb9d1691fce027f9888190f683eb34c47` |
| standaloneScreenshot | `build/whitebox/dark-uiux-pr02-ui-chrome-sprite-pilot/evidence/dark-uiux-pr02-runtime-error-loading-fit.png`, SHA-256 `0a5c786bfa45dc9953b48ca8f34d59a2286d58cd3c4e6e83669dc719707f2612` |
| screenshotMetadata | `*.png.metadata.txt` sidecars show `capture_mode=macos-window-id`, `window_owner=K-ToME`, `window_bounds=80,38,1280,828`, and matching packaged app pids for the accepted post-fix screenshots. |
| computerUseLimitation | `get_app_state(app=com.ktome.client)` returned `cgWindowNotFound` and coordinate click timed out against the GLFW window, so the interaction path used direct CGEvent key posting to the packaged app pid before target-window screenshots. The limitation affects the input mechanism only; the captured windows are packaged-app evidence with pid/window metadata. |
| packagedLaunchGate | `unset JAVA_TOOL_OPTIONS && source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr02-ui-chrome-sprite-pilot` returned `BUILD SUCCESSFUL`. |

Post-fix manual result: shell/HUD, inventory/modal, validation overlay, and a standalone/main-menu surface were re-captured from the current packaged app and no longer show the original global chrome/text mismatch. Automated bounded-layout assertions, PR-02 golden screenshots, `clientSmoke`, `maintainabilityLint`, and `verifyChanged` also passed after the final fit fix.

## Document Self-Audit

| requirement | status | evidence |
| --- | --- | --- |
| Generate Round 1 sheets `r01-ui-chrome`, `r01-ui-controls`, `r01-ui-hud-icons` | `一致` | Three raw PNGs exist under `assets-src/image/raw/sheets/dark-v1/`; hashes are listed above. |
| Keep owner contract, key registry, sheet plan, sliced PNGs, manifest, and coverage in one batch | `一致` | Owner contract keys = `42`; registry PR-02 entries = `42`; runtime PNGs = `42`; owner-scope coverage = `PASS`. |
| Do not alter row/col contracts to accommodate generated image drift | `一致` | `scripts/repack_generated_sheet.py` aligns generated subjects to existing `sheet-plan.yaml`; sheet-plan ownership remains the authority. |
| Client consumes panel, slot, modal, HUD, standalone shared chrome, and runtime loading/error markers through manifest-resolved assets | `一致` | `TileRendererCanvasTest`, `StandaloneScreenLayoutTest`, `ManifestResolveTest`, `clientSmoke`, PR-02 golden labels, and `goldenScreenshot` passed. |
| Manual white-box / packaged visual review | `一致` | Post-fix packaged target-window evidence captured `dark-uiux-pr02-shell-hud-frame-fit.png`, `dark-uiux-pr02-inventory-modal-frame-fit.png`, `dark-uiux-pr02-validation-overlay-frame-fit.png`, and `dark-uiux-pr02-runtime-error-loading-fit.png`; interaction used direct CGEvent key posting because Computer Use accessibility discovery could not read the GLFW window. |
