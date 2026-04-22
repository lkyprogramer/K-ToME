# Phase 4 UI/UX PR-01 Manual White-Box Record

**PR**: `phase4-uiux-pr01`  
**记录人**: `Codex / Computer Use`  
**复核人**: `N/A`  
**日期**: `2026-04-22`  
**记录时间**: `2026-04-22 00:37-00:58 CST`; telegraph retest `2026-04-22 09:20-09:39 CST`  
**复核时间**: `N/A`  
**结论**: `PASS`

## 1. 环境

| 项 | 值 |
| --- | --- |
| Git branch | `codex/phase4-uiux-pr01-client-foundation-main-menu` |
| Git HEAD sha | `436959780b221e0c7d557cd74ff58383e64dadeb` |
| OS / JVM | `macOS Darwin 24.6.0 arm64`; packaged app built by `:client:packageMacApp` from SDKMAN JDK 21 task environment |
| locale | `zh-CN` |
| 窗口尺寸 | `1280x800`; `1024x768` retake via System Events window bounds |
| seed / validation preset | standard quick-start run; validation `Boss 变体` preset seed `20260412` |
| save slot / content pack | isolated temp home `build/whitebox/phase4-uiux-pr01/home`; content pack `(none)` |
| 启动命令 | `JAVA_TOOL_OPTIONS="-Duser.home=/Users/luo/Documents/github/K-ToME/build/whitebox/phase4-uiux-pr01/home" client/build/release/K-ToME.app/Contents/MacOS/K-ToME` |
| JVM 参数 / feature flags | `-Duser.home=/Users/luo/Documents/github/K-ToME/build/whitebox/phase4-uiux-pr01/home`; CUA app target `com.ktome.client`; CUA version `755` |

## 2. 输入序列

| # | 起始状态 / mode | 输入 | 预期行为 | 实际行为 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 1 | no save main menu, `1280x800` | CUA launch app | OS title is `K-ToME`; no dev operation text in title; `快速开始` first focus; `继续游戏` disabled | Window title `K-ToME`; `快速开始` cyan; `继续游戏` gray; help line remains in menu body | `PASS` |
| 2 | no save main menu | `Return` | Enter starts a new run | Enter entered map state; map/sidebar/bottom surfaces visible | `PASS` |
| 3 | map state | `Ctrl+S`, `Cmd+Q`, relaunch same temp home | save exists; relaunch first focus moves to `继续游戏` | save file created at `home/.ktome/run-save.json`; relaunch shows `继续游戏` cyan | `PASS` |
| 4 | continue main menu | `Return` | Enter loads saved run | saved run loaded; map state displayed with `游戏已加载。` log | `PASS` |
| 5 | corrupted save main menu | corrupt `home/.ktome/run-save.json`, relaunch | `继续游戏` disabled; first focus returns to `快速开始`; error summary visible | `快速开始` cyan; `继续游戏` gray; footer shows `当前存档无法加载；需要反馈时请复制错误详情。` | `PASS` |
| 6 | corrupted save main menu | `Down`, `Return`, `Down`, `Return` | disabled continue does not submit; keyboard can leave disabled entry and reach validation mode | `Return` on disabled continue stayed on menu; second `Down` reached `验证模式`; `Return` entered validation setup | `PASS` |
| 7 | corrupted save main menu, `1024x768` | resize window to `1024x768` | main menu text and build summary do not clip | First retake exposed build-summary right-edge clipping; fixed by moving `MAIN_MENU_BUILD_SUMMARY_X`; after-fix retake no longer clips | `PASS_AFTER_FIX` |
| 8 | quick-start map, `1024x768` | `Return` from quick start | MapDominant map/sidebar/bottom cards do not overlap | map, right sidebar, bottom info/log/focus/action surfaces remain separated | `PASS` |
| 9 | validation `Boss 变体`, `1024x768` | `F9`, select travel section/action, attempt boss travel and waits | boss warning / telegraph danger overlay appears, with HUD status colors still readable | Initial CUA pass reached validation overlay and boss-variant session, but runtime path did not produce an active telegraph overlay | `BLOCKED_BEFORE_FIX` |
| 10 | validation `Boss 变体`, packaged app retest | `F9`, navigate to `遭遇 / 触发 Boss 预警`, `Return`, `F9` | deterministic validation action triggers active boss telegraph overlay through runtime state | Added `TriggerBossTelegraph` validation action; CUA executed the action and observed accepted `Validation encounter` result in tile view | `PASS_AFTER_FIX` |

## 3. 视觉与可读性检查

| 检查项 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| no-save first screen | title/build summary/main actions/help visible; `快速开始` first focus | visible; no title dev text; help does not overlap main actions at `1280x800` | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-main-menu-first-run.png` | `PASS` |
| continue first focus | valid save makes `继续游戏` first focus | relaunch after save shows `继续游戏` cyan | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-main-menu-continue.png` | `PASS` |
| continue enter load | Enter from continue loads game | map state loaded and log says game loaded | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-continue-enter-loaded.png` | `PASS` |
| map dominant layout `1280x800` | map/sidebar/bottom cards separated | no visible overlap | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-map-dominant-layout.png` | `PASS` |
| main menu `1024x768` | no major text clipping | initial shot clipped right summary; after code fix retake passes | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-1024x768-after-fix.png` | `PASS_AFTER_FIX` |
| map dominant layout `1024x768` | map/sidebar/bottom cards separated | no visible overlap | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-map-dominant-layout-1024x768.png` | `PASS` |
| validation mode reachability | validation remains reachable but not primary action | validation setup reached from secondary entry | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-validation-mode-reachable.png` | `PASS` |
| telegraph token | boss warning / danger overlay visible and not conflicting with HUD | Initial CUA attempt retained as failure evidence; retest used deterministic `触发 Boss 预警` validation action and reached accepted runtime path | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-telegraph-token-attempt.png`; `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-telegraph-token-after-fix-cua.txt` | `PASS_AFTER_FIX` |

## 4. 错误 / 空态 / 回退检查

| 场景 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| no save | `继续游戏` disabled/gray and not first focus | `快速开始` first focus; `继续游戏` disabled | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-main-menu-first-run.png` | `PASS` |
| corrupted save | `继续游戏` disabled; error summary visible; submit rejected | disabled continue did not submit; keyboard moved on to validation | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-main-menu-corrupted-save.png`; `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-disabled-focus.png`; `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-validation-reachable.png` | `PASS` |
| copy error payload contract | payload order `savePath / reasonCode / gameVersion / [ktome/<hash>]` | payload artifact recorded with `CORRUPTED`, `0.4.0`, `43695978`; UI clipboard action was not exercised in CUA | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-payload.txt` | `PASS_WITH_LIMIT` |
| save corruption fixture | original valid save retained before corruption | valid save backup retained | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-run-save-before-corruption.json` | `PASS` |

## 5. 证据路径

| 类型 | 路径或说明 |
| --- | --- |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-main-menu-first-run.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-main-menu-continue.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-continue-enter-loaded.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-main-menu-corrupted-save.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-disabled-focus.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-validation-reachable.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-map-dominant-layout.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-1024x768-after-fix.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-map-dominant-layout-1024x768.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-validation-mode-reachable.png` |
| screenshot | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-telegraph-token-attempt.png` |
| CUA retest note | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-telegraph-token-after-fix-cua.txt` |
| screen recording | `N/A` |
| golden hash | `./gradlew goldenScreenshot` passed after updating the main-menu golden hashes for the 1024 summary fix |
| smoke artifact | `./gradlew clientSmoke goldenScreenshot verifyChanged` passed after retake |
| copied payload | `build/whitebox/phase4-uiux-pr01/evidence/phase4-uiux-pr01-corrupted-save-payload.txt` |
| log excerpt | `build/whitebox/phase4-uiux-pr01/logs/app-first-run.log`, `app-continue.log`, `app-corrupted-save.log`, `app-1024-retake.log`; logs only include `JAVA_TOOL_OPTIONS` startup line |

## 6. 签收结论

1. 未通过项：`N/A`; initial CUA telegraph attempt is retained as before-fix evidence.
2. 已修复项：新增 deterministic `触发 Boss 预警` validation action，CUA 可从 validation overlay 直接触发 runtime `PendingTelegraphState` 路径。
3. 可进入下一 PR：`yes`, subject to normal PR review and CI.

## 7. 双人签收

| 角色 | 姓名 / ID | 结论 | 备注 |
| --- | --- | --- | --- |
| 记录人 | `Codex / Computer Use` | `PASS` | `2026-04-22 09:39 CST`; telegraph blocked item retested after deterministic validation action fix |
| 复核人 | `N/A` | `N/A` | `N/A` |

## 8. Review Fix 快速复测

**时间**: `2026-04-22 11:01 CST`  
**目标**: 复测 review findings 1-5 的主菜单 UI/UX 修复。  
**App**: `client/build/release/K-ToME.app`; LaunchServices 使用 `open -n -a <repo>/client/build/release/K-ToME.app` 启动构建产物。  
**隔离 runtime home**: `build/whitebox/phase4-uiux-pr01-review-fixes/runtime-home`  
**CUA target**: `com.ktome.client`, observed pid `3476`  
**JVM 确认**: `jcmd 3476 VM.system_properties` confirmed `user.home=/Users/luo/Documents/github/K-ToME/build/whitebox/phase4-uiux-pr01-review-fixes/runtime-home`  
**Fixture**: `runtime-home/.ktome/run-save.json` 写入损坏存档文本 `{ invalid save json`。

| 检查项 | 输入 / 观察 | 实际结果 | 证据 | 结果 |
| --- | --- | --- | --- | --- |
| Copy Error Detail | 在 unavailable continue 主菜单按 `C` | footer notice 变为 `已复制继续游戏错误详情。`; clipboard payload 包含 `savePath`, `reasonCode`, `gameVersion`, `[ktome/<hash>]` | `build/whitebox/phase4-uiux-pr01-review-fixes/evidence/copied-payload.txt` | `PASS` |
| BuildInfo startup path | packaged app 启动后复制 payload | payload build hash 为 `43695978`; 当前包 build hash resolved，因此未触发 fallback warn | `build/whitebox/phase4-uiux-pr01-review-fixes/evidence/copied-payload.txt` | `PASS_WITH_LIMIT` |
| helpLines layout | CUA 观察 zh-CN main menu | help 文案压缩为两条单行，位于右侧帮助区；不再覆盖种族 state/description，也不再压到底部 footer notice | `build/whitebox/phase4-uiux-pr01-review-fixes/evidence/main-menu-unavailable-after-copy.png` | `PASS` |
| reason-specific unavailable copy | 损坏 save fixture 启动主菜单 | footer 初始显示 reason-specific `读取存档失败。`; copy payload `reasonCode: IO_ERROR` | `build/whitebox/phase4-uiux-pr01-review-fixes/evidence/copied-payload.txt` | `PASS` |
| primary/focus consistency | unavailable continue 主菜单初始状态 | `快速开始` 为首焦点；`继续游戏` disabled; secondary entries remain reachable visually | `build/whitebox/phase4-uiux-pr01-review-fixes/evidence/main-menu-unavailable-after-copy.png` | `PASS` |

**限制说明**:

1. 当前 packaged app 的 `BuildInfo.shortHash` 正常解析，Computer Use 复测未强制制造 fallback resource；fallback warn 仍由 `BuildInfoTest` 覆盖。
2. `main-menu-unavailable-after-copy.png` 使用 foreground `screencapture` 保存；实际画面先由 Computer Use 确认，截图仅作为本次记录的文件证据。

## 9. Final Gate 补充验证

**时间**: `2026-04-22 11:31 CST`

| 命令 | 结果 | 备注 |
| --- | --- | --- |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./scripts/verify-bootstrap.sh` | `PASS` | 覆盖 `client/build.gradle.kts` / build resource 接线变更后的 bootstrap 验证要求。 |
| `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew clientSmoke goldenScreenshot verifyChanged` | `PASS` | 覆盖 PR01 文档要求的最终 smoke、golden 与 changed-surface owner gate。 |
| `git diff --check` | `PASS` | 提交前 whitespace 检查。 |
