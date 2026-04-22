# Phase 4 UI/UX Manual White-Box Record

**PR**: `phase4-uiux-pr03`
**记录人**: `Codex`
**复核人**: `Codex / Computer Use packaged-app verification`
**日期**: `2026-04-22`
**记录时间**: `2026-04-22 21:46:55 CST`
**复核时间**: `2026-04-22 22:52:13 CST`
**结论**: `PASS - packaged-app Computer Use white-box evidence plus automation owner evidence`

## 1. 环境

| 项 | 值 |
| --- | --- |
| Git branch | `codex/phase4-uiux-pr03-item-content-ui-states` |
| Git HEAD sha | `9ebb0946` |
| OS / JVM | `Darwin 24.6.0 arm64 / Temurin 21.0.10` |
| locale | `zh-CN` packaged-app live pass; `zh-CN / en-US` golden coverage |
| 窗口尺寸 | `1280x800` packaged app window; `1280x800` golden screenshot harness |
| seed / validation preset | `LOOT_LAB seed=20260413` packaged-app live validation pass; `20260409 underground_river rogue` PR03 item inspect fixture |
| save slot / content pack | `build/whitebox/phase4-uiux-pr03-item-content-states-cua/runtime-home-packaged-open`; isolated JUnit temp save directories |
| 启动命令 | `./gradlew :client:packageMacApp`; packaged app launched from `client/build/release/K-ToME.app`; `./gradlew goldenScreenshot`; `./gradlew clientSmoke`; targeted `:client:test` selectors |
| JVM 参数 / feature flags | SDKMAN `java=21.0.10-tem`, `kotlin=2.2.21` |
| Computer Use target | `com.ktome.client`, repo packaged app PID `29432`; broken-manifest isolated packaged app PID `32145` |

## 2. 输入序列

| # | 起始状态 / mode | 输入 | 预期行为 | 实际行为 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 1 | PR03 inspect fixture / `INVENTORY` | golden harness selects MAGIC affix item | quality color and corner glyph are visible in item row | `GoldenScreenshotHarnessTest.opt pr03 inspect...` hash updated and repassed | `PASS` |
| 2 | PR03 inspect fixture / `INVENTORY` | golden harness selects UNIQUE item | rarity stays primary color; special tier accent is represented by item presentation contract | PR03 inspect golden hashes repassed for `en-US` and `zh-CN` | `PASS` |
| 3 | PR03 inspect fixture / `INVENTORY` | golden harness selects ARTIFACT item | ARTIFACT is treated as special accent, not a fourth rarity | PR03 inspect golden hashes repassed for `en-US` and `zh-CN` | `PASS` |
| 4 | map render model / `MAP` | canvas and golden render single/stacked ground loot | item icon, head item, badge `9+`, actor-corner placement are visible | `TileRendererCanvasTest` passed; `GoldenScreenshotHarnessTest` includes PR03 ground-loot hashes | `PASS` |
| 5 | route / shop / reward cards | card conversion and sidebar rendering | route, shop offer/sell, and recent reward rows consume shared `ModalCardModel`; route card does not invent unowned zone icon keys | `RoutePreviewTextTest`, `ModalCardModelTest`, `clientSmoke` passed | `PASS` |
| 6 | loading state | smoke timing artifact | loading transition is measured around real `AssetContractCoordinator.prepareSession -> warmCache` state and clears within 500ms | `client/build/reports/client-smoke/loading-timing.jsonl` records `1ms / 500ms`, `loadingCleared=true` | `PASS` |
| 7 | packaged app / main menu | Computer Use connects to repo packaged app, not installed app | CUA target PID `29432` matches `client/build/release/K-ToME.app/Contents/MacOS/K-ToME` | `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-packaged-main-menu.png`; `pgrep-packaged-open-after-launch.txt` | `PASS` |
| 8 | packaged app / validation setup | select `LOOT_LAB`, seed `20260413`, start validation session | setup screen shows `预设: 掉落实验室`, `Seed: 20260413`; session loads into map | `phase4-uiux-pr03-packaged-validation-loot-lab-setup.png`; `phase4-uiux-pr03-packaged-loot-lab-loaded-empty-ground.png` | `PASS` |
| 9 | packaged app / validation overlay | open overlay and trigger reward/item actions | overlay shows preset/seed context; `Validation reward` and `Validation item` update recent reward/ground item state | `phase4-uiux-pr03-packaged-validation-overlay-loot-lab.png`; `phase4-uiux-pr03-packaged-card-shared-reward-validation.png`; `phase4-uiux-pr03-packaged-quality-variants-map-after-validation-item.png` | `PASS` |
| 10 | packaged app / inventory + ground loot | inspect quality card, drop items, verify player-standing markers | inventory card shows quality/equipment fields; ground panel and map cell show single and two-item stack markers while player stands on the cell | `phase4-uiux-pr03-packaged-inventory-quality-card.png`; `phase4-uiux-pr03-packaged-ground-loot-stack-player-standing.png`; `phase4-uiux-pr03-packaged-ground-loot-single-player-standing.png` | `PASS` |

## 3. 视觉与可读性检查

| 检查项 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| ground loot marker | map cell item count is visible without opening sidebar | live packaged app covered single and two-item stack while player stands on the cell; marker canvas/golden owner evidence covers `9+` badge | `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-packaged-ground-loot-single-player-standing.png`; `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-packaged-ground-loot-stack-player-standing.png`; `client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml` | `PASS` |
| quality variants | `NORMAL / MAGIC / RARE` plus `UNIQUE / ARTIFACT` accent are distinct contracts | `QualityPresentationTest` and PR03 inspect golden passed | `client/build/test-results/clientSmoke/TEST-com.ktome.client.ClientSmokeHarnessTest.xml`; `client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml` | `PASS` |
| shared card model | route/shop/reward do not create local-only card rows | packaged app live pass covered reward card presentation; source/tests cover `ModalCardModel.routePreview`, `shopOffer`, `shopSellEntry`, and `rewardPresentation`; route cards keep `iconKey=null` until snapshot owns route icon keys | `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-packaged-card-shared-reward-validation.png`; `client/src/test/kotlin/com/ktome/client/ui/card/ModalCardModelTest.kt`; `client/src/test/kotlin/com/ktome/client/render/RoutePreviewTextTest.kt` | `PASS` |
| empty states | inventory/shop/inspect/log use `UiEmptyState` token pairs | deprecated `ui.inspect.empty.tile` removed; Tile/ASCII message lines render `UiEmptyState.log` when logEvents is empty | `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`; `tools/build/test-results/localeLint/TEST-com.ktome.tools.lint.LocaleLintTest.xml` | `PASS` |

## 4. 错误 / 空态 / 回退检查

| 场景 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| save/continue error payload | copied payload preserves heading/detail/context/build hash order | `UiErrorPayloadTest` and continue formatter use `UiErrorPayload` | `client/src/test/kotlin/com/ktome/client/ui/state/UiErrorPayloadTest.kt` | `PASS` |
| required error actions | error state exposes `Retry / Back To Menu / Copy Error Detail` | isolated broken-manifest packaged app shows `R - 重试`, `Esc - 返回主菜单`, `C - 复制错误详情`; copied payload includes `stage: manifest-load` and invalid visual manifest message | `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-packaged-error-manifest-actions.png`; `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-error-copy-detail-payload.txt`; `client/src/test/kotlin/com/ktome/client/GameAppLifecycleTest.kt` | `PASS` |
| loading cancel invariant | `allowsCancel == true` iff cancel action is `CANCEL` | `UiLoadingStateTest` covers generic/cancellable states; `ClientSmokeHarnessTest` covers real warm-cache loading clear | `client/src/test/kotlin/com/ktome/client/ui/state/UiLoadingStateTest.kt`; `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt` | `PASS` |
| item icon fallback | official item and special template icon keys cannot silently fallback | `ItemIconKeyCoverageRule` is wired into `contractLint` | `tools/build/test-results/contractLint/TEST-com.ktome.tools.lint.ItemIconKeyCoverageRuleTest.xml` | `PASS` |

## 5. 证据路径

| 类型 | 路径或说明 |
| --- | --- |
| golden hash | `client/build/test-results/goldenScreenshot/TEST-com.ktome.client.golden.GoldenScreenshotHarnessTest.xml` |
| smoke artifact | `build/reports/harness/client-smoke.md`; `build/reports/harness/client-smoke.json` |
| loading timing | `client/build/reports/client-smoke/loading-timing.jsonl` contains `{"state":"ui.loading.generic","transitionMs":1,"budgetMs":500,"loadingCleared":true}` |
| contract lint | `tools/build/test-results/contractLint/TEST-com.ktome.tools.lint.ItemIconKeyCoverageRuleTest.xml` |
| locale lint | `tools/build/test-results/localeLint/TEST-com.ktome.tools.lint.LocaleLintTest.xml` |
| Computer Use report | `docs/opt/ui-pr/manual-records/phase4-uiux-pr03-cua-whitebox-report.md`; local evidence copy at `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/cua-whitebox-report.md` |
| packaged app CUA screenshots | `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-packaged-*.png`; each accepted screenshot has `.metadata.txt` and `.sha256` sidecars |
| copied payload | `build/whitebox/phase4-uiux-pr03-item-content-states-cua/evidence/phase4-uiux-pr03-error-copy-detail-payload.txt` |
| excluded screenshots | earlier `/Applications/K-ToME.app` auto-launch screenshots in the same evidence directory are not signoff evidence; accepted records use the `phase4-uiux-pr03-packaged-*` prefix |

## 6. Resource Fallback Audit

| key | 请求面 | fallback-visualKey / fallback-audioCueId | fallback 行为 | 失效风险等级 | 补交付 unblock task | 关闭 PR / owner | 是否开放正式玩家路径 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `N/A` | `item / card / state` | `N/A` | No new fallback path was introduced. Existing item/icon/audio manifest entries are consumed through exact manifest keys, and `ItemIconKeyCoverageRule` blocks missing or unresolved official item icons. | `low` | `N/A` | `phase4-uiux-pr03` | `yes` |

## 7. 签收结论

1. 未通过项：`N/A`
2. 需要回归的项：
   - Live CUA directly covered single and two-item ground-loot stacks; `9+` stack remains covered by canvas/golden owner evidence.
   - Live CUA directly covered reward and inventory item card surfaces; shop card convergence remains covered by source/tests because this live route did not enter a shop room.
3. 可进入下一 PR：`yes`

## 8. 双人签收

| 角色 | 姓名 / ID | 结论 | 备注 |
| --- | --- | --- | --- |
| 记录人 | `Codex` | `PASS` | `2026-04-22 21:46:55 CST`; source/tests/golden/smoke evidence recorded |
| 复核人 | `Codex / Computer Use packaged-app verification` | `PASS` | `2026-04-22 22:52:13 CST`; repo packaged app CUA screenshots, manifest error copy payload, and target PID evidence recorded |
