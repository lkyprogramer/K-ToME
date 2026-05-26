# Dark UI/UX PR07 Final All-Screens Evidence Index

## Summary

| Field | Value |
| --- | --- |
| result | `FAIL_UI_DIRECTOR_REVIEW_2026_05_24` |
| date | 2026-05-24 |
| source matrix | `UI/pr/screen-coverage-matrix.md` |
| source PR doc | `UI/pr/dark-uiux-pr07-golden-whitebox-polish.md` |
| scenario id | `dark-uiux-pr07-final-ui` |
| packaged manual record | `UI/manual-records/dark-uiux-pr07-packaged-app.md` |
| doc implementation audit | `UI/review/dark-uiux-final-doc-implementation-audit.md` |

This file is the required `dark-uiux-pr07-final-all-screens` evidence index. It binds each Required or Conditional screen from `UI/pr/screen-coverage-matrix.md` to owner evidence, focused checks, coverage evidence, and the repo-relative packaged-app evidence slot. It now records a real packaged app whitebox run on 2026-05-24. The current packaged app was rebuilt/materialized and launched as pid `61858`; Computer Use was attempted but could not bind or drive the libGDX window, so current persisted evidence uses the documented target-window screenshot fallback. The result remains a blocking visual-quality failure against the `UI/UI-demo-new.png` reference bar. The latest packaged shell screenshot was recaptured after equipment slot scaling, prop staging, ritual/bonfire atmosphere, cooler map materials, collapsed-room corner masking, pre-actor terrain glaze, terrain tile bleed, and validation-only non-rectangular stage-room preference; it is visibly improved but still not director-level final quality. The earlier packaged `F9` / `V` conclusion was rechecked after a compact validation-panel polish: PR07 fast actions were reachable, and the selected action plus evidence summary path were visible in packaged evidence. The validation overlay is no longer the primary blocker, but it still needs a clean final re-drive once CUA/window binding is available; the shell/background/panel quality remains below final acceptance.

## Shared Evidence

| Evidence type | Path / command | Current status |
| --- | --- | --- |
| final-full coverage source record | `UI/manual-records/dark-uiux-pr06-overview-screenshot.md` | Historical PR06 record says expected=487, covered=487, missing=0, oldStyle=0, pending/rejected=0. |
| final-full command for this PR | `./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full -Pktome.darkUiux.expectedInventory=UI/sprite-sheets/dark-v1-final-full-inventory.json -Pktome.darkUiux.artRandomQaRecord=UI/manual-records/dark-uiux-pr06-art-random-qa.json -Pktome.darkUiux.packagedSentinelEvidence=UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md` | `PASS`; not a substitute for PR07 packaged whitebox. |
| packaged app manual record | `UI/manual-records/dark-uiux-pr07-packaged-app.md` | `FAIL_UI_DIRECTOR_REVIEW_2026_05_24`. |
| packaged evidence dir | `build/whitebox/dark-uiux-pr07-final-ui/evidence` | 2026-05-24 screenshots captured with metadata and sha256 sidecars for menu, character creation, continue unavailable, validation setup, validation overlay, shell, inventory, shop, replacement modal, talent, active slot modal, status, combat/inspect, route, stat assign, reward/frontstage, map-layer placeholder, and controlled UiErrorScreen. Latest shell recapture: `dark-uiux-pr07-ui-demo-new-shell-polish7-truecolor.png`, target pid `61858`, sha256 `e4bded8b9c6cb3346a7395ad43ad3a62ddb94d0577a675661cbde11d36744a4e`. |
| packaged contact sheet | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-20260524-contact-sheet.png` | Side-by-side review sheet including `UI/UI-demo-new.png` and current packaged captures. |
| shell authority | `UI/manual-records/ui-demo-new-visual-parity.md` and `ui-demo-new-*` labels | Explicitly used for homepage/shell references; no fallback to old `dark-uiux-pr02-1-demo-*` labels. |

## Screen Matrix Index

| Matrix surface | Status | Owner evidence / golden labels | Focused checks | Coverage artifact | Packaged evidence slot |
| --- | --- | --- | --- | --- | --- |
| 首页 / 主菜单 | `covered-with-exception` | `dark-uiux-pr01-home-main-menu`; `ui-demo-new-visual-parity.md`; `ui-demo-new-parity-1672x941` / `ui-demo-new-parity-1280x800` | `MainMenuFocusPolicyTest`, `MainMenuControllerTest`, `MainMenuScreenTextTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-main-menu.png` captured or not reached in this run; see packaged record |
| 角色创建 / 职业选择 | `covered-with-exception` | `dark-uiux-pr01-home-new-run`; PR06 profession icon / missing visual coverage | `MainMenuScreenTextTest`, `ProfessionSchemaTest.playerCreationProfessionOptionsExposeCanonicalProfessionIcons` | final-full coverage record; `UI-FOLLOWUP-race-icon-family-contract` remains owner boundary | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-character-creation.png` captured or not reached in this run; see packaged record |
| 继续游戏异常 / 复制详情 | `covered-with-exception` | `dark-uiux-pr01-continue-unavailable` | `ContinueUnavailablePayloadFormatterTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-continue-unavailable.png` captured or not reached in this run; see packaged record |
| 验证模式入口 | `covered-with-exception` | `dark-uiux-pr01-validation-entry`; `ui-demo-new-visual-parity.md` | `GameAppLifecycleTest` validation entry case | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-main-menu.png` captured or not reached in this run; see packaged record |
| 验证模式 Setup 页 | `covered-with-exception` | `dark-uiux-pr07-validation-setup`; `ui-demo-new-visual-parity.md`; `ui-demo-new-validation-setup` | `ValidationSetupControllerTest`, `ClientSmokeHarnessTest` validation setup smoke | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-validation-setup.png` captured or not reached in this run; see packaged record |
| 验证模式运行时 overlay | `covered-with-exception` | `dark-uiux-pr07-validation-overlay`; `dark-uiux-pr06-validation-overlay-compact` | validation overlay smoke/golden; `dark-uiux-pr07-final-ui` registry parity tests | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-validation-overlay.png` captured or not reached in this run; see packaged record |
| 局内主 shell | `covered-with-exception` | `dark-uiux-pr01-shell-1280x800`, `dark-uiux-pr01-shell-min-window`; `ui-demo-new-parity-1280x800`, `ui-demo-new-right-panel-grid`, `ui-demo-new-bottom-deck-no-command-hints`, `ui-demo-new-nav-rail-crop`, `ui-demo-new-map-stage-crop` | `DemoShellLayoutTest`, `DemoShellRendererTest`, `GameShellLayoutTest`, `TileRendererCanvasTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-demo-new-shell.png` captured or not reached in this run; see packaged record |
| Loading / runtime error state | `covered-with-exception` | `dark-uiux-pr07-runtime-loading`, `dark-uiux-pr07-runtime-error` | `UiLoadingStateTest`, `UiErrorPayloadTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-runtime-loading.png` + `dark-uiux-pr07-runtime-error.png` captured or not reached in this run; see packaged record |
| 全局错误页 | `covered-with-exception` | `dark-uiux-pr07-ui-error-screen`; asset failure injection record | startup error path through `ValidationScenarioBootstrapTest` and `UiErrorPayloadTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-error-screen.png` captured or not reached in this run; see packaged record |
| 胜利结算页 | `covered-with-exception` | `dark-uiux-pr07-outcome-victory`; golden outcome set | `OutcomeSummaryPresenterTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-outcome-victory.png` captured or not reached in this run; see packaged record |
| 失败结算页 | `covered-with-exception` | `dark-uiux-pr07-outcome-defeat`; golden outcome set | `OutcomeSummaryPresenterTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-outcome-defeat.png` captured or not reached in this run; see packaged record |
| 装备面板 | `covered-with-exception` | `dark-uiux-pr03-equipment-slots`; `dark-uiux-pr05-1-inventory-workbench`; `dark-uiux-pr05-1-inventory-compare` | `EquipmentInventoryPresenterTest`, `InventoryWorkbenchPresenterTest`, `TileRendererCanvasTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inventory-workbench.png` captured or not reached in this run; see packaged record |
| 背包 grid | `covered-with-exception` | `dark-uiux-pr03-inventory-empty`, `dark-uiux-pr03-inventory-stacked`; `dark-uiux-pr05-1-inventory-workbench`, `dark-uiux-pr05-1-inventory-pagination`, `dark-uiux-pr05-1-inventory-min-window` | `InventoryWorkbenchPresenterTest`, `TileRendererCanvasTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inventory-workbench.png` captured or not reached in this run; see packaged record |
| 铭文商店 / Shop buy-sell | `covered-with-exception` | `dark-uiux-pr03-inscription-shop` | `InputHandlerTest` shop cases, `DescriptionPresenterTest` shop item lines | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inscription-shop.png` captured or not reached in this run; see packaged record |
| 铭文满槽替换 modal | `covered-with-exception` | `dark-uiux-pr03-shop-full-slot-replace` | `InputHandlerTest` replacement prompt cases | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-shop-replacement-modal.png` captured or not reached in this run; see packaged record |
| 天赋分配面板 | `covered-with-exception` | `dark-uiux-pr04-talent-assign-panel-start`, `dark-uiux-pr04-talent-assign-min-window-log-visible`, `dark-uiux-pr04-right-companion-coexistence`, `dark-uiux-pr04-01-*`, `dark-uiux-pr06-talent-icon-rebaseline` | `TalentSidebarPresenterTest`, PR04-01 passive focused tests | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-talent-assign.png` captured or not reached in this run; see packaged record |
| 主动槽选择 modal | `covered-with-exception` | `dark-uiux-pr04-active-slot-choice` | `InputHandlerTest` active slot cases | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-active-slot-modal.png` captured or not reached in this run; see packaged record |
| 技能 / actor status HUD / 任务摘要 | `covered-with-exception` | `dark-uiux-pr06-status-quest-skill-overview`; `dark-uiux-pr06-talent-icon-rebaseline` | `StatusDefinitionsTest`, `StatusSchemaContractTest`, `StatusPresentationModelTest`, `StatusIconResolverTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-status-quest-skill-overview.png` captured or not reached in this run; see packaged record |
| 战斗选择 / 行动提示 / telegraph | `covered-with-exception` | `dark-uiux-pr07-combat-decision`; PR05 telegraph actor/map manual/golden | combat focused tests; telegraph actor/map anchored checks | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-combat-decision.png` captured or not reached in this run; see packaged record |
| Look / Inspect / Explain pane | `covered-with-exception` | `dark-uiux-pr07-look-inspect` | actor mutation inspect/target-card focused tests; Explain/Description focused tests | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-look-inspect.png` captured or not reached in this run; see packaged record |
| 世界路线 / route selection | `covered-with-exception` | `dark-uiux-pr07-world-route-selection` | `RoutePreviewTextTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-world-route-selection.png` captured or not reached in this run; see packaged record |
| 属性分配 / stat assign | `covered-with-exception` | `dark-uiux-pr07-stat-assign` | stat assign manual/focused owner checks | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-stat-assign.png` captured or not reached in this run; see packaged record |
| 被动 / reward / frontstage 选择 | `covered-with-exception` | `dark-uiux-pr07-reward-frontstage` | reward focused tests/manual | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-reward-frontstage.png` captured or not reached in this run; see packaged record |
| 地图 / tile / actor / portrait / VFX | `covered-with-exception` | `dark-uiux-pr05-map-layer-stack`, `dark-uiux-pr05-actor-boss-telegraph` | `TileRendererCanvasTest`, PR05 map/actor checks | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-map-layer-telegraph.png` captured or not reached in this run; see packaged record |
| 设置 / 无障碍 | `covered-with-exception` | `dark-uiux-pr07-accessibility-settings` | `AccessibilityToggleTest` | final-full coverage record | `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-accessibility-settings.png` captured or not reached in this run; see packaged record |
| Desktop title / launcher visible text | `covered-with-exception` | desktop launcher title manual metadata; no raw local path in committed records | `DesktopLauncherTitleFormatterTest` | final-full coverage record | packaged screenshot metadata under `build/whitebox/dark-uiux-pr07-final-ui/evidence/*.metadata.txt` captured or not reached in this run; see packaged record |

## PR07 Packaged Whitebox Result

The packaged app launches and many surfaces are inspectable, but the final UI does not meet the requested UI director / art director bar. The main failure is visual parity: compared with `UI/UI-demo-new.png`, the current packaged shell still has weaker dungeon atmosphere, more repetitive map composition, lower-richness right panel content, and lower overall material/lighting finish. The latest shell pass improved equipment slot scale, item icon prominence, prop staging, occult/torch glow, room edge masking, pre-actor terrain glaze, terrain tile bleed, and validation-only non-rectangular stage-room preference, but it did not close the gap.

The functional whitebox blocker is now narrower but not closed: prior packaged evidence proved `F9` / `V` can reach the `PHASE4_V4_FAST` action path, and PR07 fast actions can prepare evidence summary, shop, replacement modal, active slot modal, route selection, stat assign, and reward/frontstage. The overlay screenshot shows the selected action and evidence summary path, so the former hidden-action-list issue is reduced to a follow-up recheck for the full action set. In the current pid `61858` session, Computer Use could list the running app but could not bind or drive the window, so only target-window screenshot evidence was refreshed. Victory/defeat, runtime loading/error, and settings/accessibility remain not accepted in this pass because the shell/background quality already fails the director-level gate.

Captured partial evidence includes:

1. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-main-menu.png`
2. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-demo-new-shell.png`
3. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inventory-workbench.png`
4. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-talent-assign.png`
5. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-look-inspect.png`
6. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-combat-decision.png`
7. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-reward-frontstage.png`
8. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-map-layer-telegraph.png`
9. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-ui-error-screen.png`
10. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-inscription-shop.png`
11. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-shop-replacement-modal.png`
12. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-active-slot-modal.png`
13. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-world-route-selection.png`
14. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-stat-assign.png`
15. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-validation-overlay.png`
16. `build/whitebox/dark-uiux-pr07-final-ui/evidence/dark-uiux-pr07-20260524-contact-sheet.png`

The following PR07-required surfaces still need a clean dedicated pass after the blocking art-direction issues are fixed: victory/defeat, runtime loading/error, and accessibility/settings. The PR07 validation overlay selected-action/evidence path is reviewable in prior packaged evidence, but the full action list should be rechecked with the remaining surface pass and with a working Computer Use binding or explicitly accepted target-window fallback. The current shell/background/UI quality remains blocking even for surfaces that were captured.

## Exception Policy

Rows that still say `covered-with-exception` retain owner-evidence mapping only; they are not final PR07 acceptance. The packaged run has now executed enough real UI surfaces to fail visual-quality review, and it proved the PR07 fast validation path is reachable with selected-action/evidence summary visible. These rows should be upgraded to `covered` only after a later packaged run captures the remaining surfaces and raises the shell/background/UI quality to the `UI/UI-demo-new.png` bar.

## Fallback / Missing Visual / Debug Resource Status

| Surface | Status | Evidence |
| --- | --- | --- |
| player-visible `missing_visual` sentinel | `covered-with-exception` | `darkManifestCoverageLint` final-full passed with PR06 final-full inventory inputs; the 2026-05-24 packaged sweep did not show ASCII fallback in captured surfaces but remains blocked by visual-quality and remaining surface / CUA re-drive limitations. |
| debug/history resource fallback | `covered-with-exception` | `UI/manual-records/dark-uiux-pr06-packaged-sentinel-audit.md` covers the PR06 packaged sentinel audit; PR07 captured fresh package evidence for several surfaces but not all required fallback/debug surfaces. |
| client ASCII fallback / debug renderer | `covered` | Not used as a coverage category in this index; shell authority stays with `ui-demo-new-*` labels and current Tile/golden surfaces. |
| raw machine path leakage | `covered` | PR07 committed records use repo-relative paths or explicit placeholder-style future paths only. |
