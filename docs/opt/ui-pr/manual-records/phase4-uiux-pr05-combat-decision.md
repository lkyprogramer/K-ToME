# Phase 4 UI/UX Manual White-Box Record - PR-05 Combat Decision

**PR**: `phase4-uiux-pr05`
**记录人**: `Codex`
**复核人**: `Pending independent reviewer`
**日期**: `2026-04-23`
**记录时间**: `2026-04-23 19:49-20:06, 20:49-21:42 Asia/Shanghai`
**复核时间**: `Pending`
**结论**: `PACKAGED_CUA_FAST_ILLEGAL_MISSING_FACT_AND_REVIEW_FIX_PASS`

## 1. 环境

| 项 | 值 |
| --- | --- |
| Git branch | `codex/phase4-uiux-pr05-telegraph-combat-decision` |
| Git HEAD sha | `d33cbd8e` |
| OS / JVM | `macOS Darwin 24.6.0`; Gradle runs via SDKMAN `java 21.0.10-tem` |
| locale | `zh-CN` for PR-05 golden; `zh-CN` and `en-US` for full golden suite |
| 窗口尺寸 | `1280x800` |
| seed / validation preset | `seed=20260412`; boss/scripted telegraph fixture from `grey_gate_depths` |
| save slot / content pack | temp save under JUnit `@TempDir`; official content pack |
| 启动命令 | `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp`; `open -n client/build/release/K-ToME.app`; automated evidence still includes `./gradlew goldenScreenshot`, `./gradlew clientSmoke`, focused client/tool tests |
| JVM 参数 / feature flags | packaged app default JVM settings; runtime home isolated under `build/whitebox/phase4-uiux-pr05/runtime-home` |

## 2. 输入序列

| # | 起始状态 / mode | 输入 | 预期行为 | 实际行为 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 1 | `MAP` with boss/scripted telegraph | fixed boss telegraph fixture | 地图 overlay、目标卡、日志前缀共享 danger/turn/source/icon 语义 | `GoldenScreenshotHarnessTest.phase4 uiux pr05 telegraph and combat decision hashes remain stable` 覆盖 telegraph triple surface hash `7447dbb6b60ced5dc9f690ae91b7a0d91dcac5424969329e0f4a58a3678c0d50` | PASS |
| 2 | `COMBAT_DECISION/ACTION` | `Enter` / `Space` / 数字键 action confirm | 选择 action；单 method 进入 TARGET；disabled action 不提交 | `InputHandlerTest` 和 `CombatDecisionFrameTest` 覆盖 command construction、disabled reason、no target feedback；golden action hash `47c966778debf1985741421a19f7090b8a51cff073cf00a9d7b7637326008905` | PASS |
| 3 | `COMBAT_DECISION/METHOD` | `Enter` / `Space` / 数字键 method confirm | 进入 TARGET，`skippedMethod=false` | `InputHandlerTest` phase assertions and method golden hash `116391420e20d09ebfbc629e80f92d2c179d1680b4adb190b23f84357a692ddb` | PASS |
| 4 | `COMBAT_DECISION/TARGET` | `Tab` / arrows / `Enter` / `Space` | 合法 target resolve + pop frame；非法 cursor hover 显示 illegal 边框；非法 cursor confirm toast | `InputHandlerTest` target phase assertions; target hash `e106c50c09013ee1c29b64578a78dad04dc3251720d204a26abe8c80d08b635d`; illegal target hash `7de9840fe785624823ed4e0212d427a1f98ba8e462affb0817ea31047ec873c9` | PASS |
| 5 | any combat decision phase | `Backspace`, `ESC`, `Ctrl+S` | `Backspace` 按 phase 回退，`ESC` 返回 `MAP`，`Ctrl+S` toast `ui.message.save.blocked-in-combat-decision` | `InputHandlerTest` 覆盖 skipped method backtrack、frame pop、save block toast | PASS |
| 6 | packaged app main menu | `Down -> Enter` | 进入 Validation Mode setup | CUA 进入 validation setup，随后切到 `Boss 变体` preset | PASS |
| 7 | validation setup | `Right x4`, `Down x11`, `Enter` | 启动 `Boss 变体` preset，seed `20260412` | packaged app 启动到 `grey_gate_depths` / `Boss 变体` 会话 | PASS |
| 8 | validation overlay | `F9`, `Travel -> 进入 Boss 旁`, `Encounter -> 触发 Boss telegraph`, `F9` | 快速到达 boss/scripted telegraph 场景 | 目标窗口证据显示 boss telegraph 风险、目标卡摘要与日志前缀同屏出现 | PASS |
| 9 | `MAP` with telegraph | `Enter` | 打开 `COMBAT_DECISION/ACTION` | packaged app 进入战斗决策 ACTION 列表，正式 action phase icon/rows 可见 | PASS |
| 10 | `COMBAT_DECISION/ACTION` | `Enter`, `Backspace` | 单一 method action 跳到 TARGET，`Backspace` 回 ACTION | packaged app 进入 TARGET 后回到 ACTION；该 live scene 未触发 METHOD phase | PARTIAL |
| 11 | `COMBAT_DECISION/TARGET` | `Ctrl+S` | 阻断保存并保留 combat decision | packaged app 保持 TARGET；toast 文案受截图时机影响未稳定捕获，自动化 `InputHandlerTest` 覆盖 toast key | PARTIAL |
| 12 | validation overlay PR05 fast surface | `F9`, `Down`, `4`, `Enter` | 打开非法目标 fixture；红色非法目标 cursor 可见；确认不提交、不改变窗口状态 | packaged app pid `54365` 打开 `COMBAT_DECISION/TARGET` 非法目标面；确认前后窗口截图 hash 均为 `2c8969a74c2e9acc7f819751a093ea6270e36213dae08c2ff08fde0a330d2d8e` | PASS |
| 13 | validation overlay PR05 fast surface | `F9`, `Down`, `5` | 打开缺失事实 fixture；缺字段显示为缺事实文案，不退化成 ready/0/no-risk | packaged app pid `54365` 打开 `COMBAT_DECISION/ACTION` 缺事实面，行文案显示 `规则层未来暴露` | PASS |

## 3. 视觉与可读性检查

| 检查项 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| telegraph 三位一体 | map overlay、target card、log prefix 使用同一 `TelegraphPresentationModel` 语义 | `TileRendererCanvasTest.target card and log prefix reuse the same telegraph presentation` 通过 | `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | PASS |
| packaged telegraph 三位一体 | `BOSS_VARIANT seed=20260412` 下 map overlay、target card、log prefix 同屏可见 | CUA packaged app 证据显示 `地牢守护者` telegraph、目标卡 `1t` danger 摘要与日志 boss warning 同屏存在 | `build/whitebox/phase4-uiux-pr05/evidence/03-telegraph-triple-surface-packaged.png` | PASS |
| combat decision panel | ACTION/METHOD/TARGET 显示正式 phase icon 和 confirm cue key | `CombatDecisionPanelTest`、`TileRendererCanvasTest.combat decision panel consumes formal phase icons...` 通过 | `client/src/test/kotlin/com/ktome/client/ui/combat/CombatDecisionPanelTest.kt` | PASS |
| packaged combat decision ACTION/TARGET | packaged app 可进入 ACTION 和 TARGET，正式 rows 与目标 cursor 可见 | ACTION 与 TARGET 均已通过 CUA 截图留证；METHOD 需依赖自动化/golden 或新增 fast validation fixture | `build/whitebox/phase4-uiux-pr05/evidence/04-combat-decision-action-packaged.png`, `build/whitebox/phase4-uiux-pr05/evidence/05-combat-decision-target-packaged.png` | PARTIAL |
| missing typed fact | 缺字段显示 `missingFactReason`，不渲染为 0/ready/no risk | `ActionHintModelBuilderTest` 覆盖 missing action、zero target、real zero cost 差异 | `client/src/test/kotlin/com/ktome/client/ui/combat/ActionHintModelBuilderTest.kt` | PASS |
| packaged missing typed fact | packaged app PR05 fast fixture 显示缺事实文案，不把缺字段渲染为可用动作 | CUA 目标窗口证据显示 `白袍 手动 / 规则层未来暴露` | `build/whitebox/phase4-uiux-pr05/evidence/15-pr05-fast-missing-fact-window.png` | PASS |
| 普通敌人 intent 非目标 | 不新增 `AIPlanSnapshot`，不从 `aiTypeId` 推断 next action | `AiIntentLeakRuleTest` 通过；`TileRenderModel` 只消费 telegraph overlay | `tools/src/test/kotlin/com/ktome/tools/lint/AiIntentLeakRuleTest.kt` | PASS |

## 4. 错误 / 空态 / 回退检查

| 场景 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| 无合法目标 | 保持当前 phase，toast `ui.message.combat.no-legal-target` | `InputHandlerTest` / `CombatDecisionFrameTest` 覆盖 | `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt` | PASS |
| 非法目标 | hover 不播错误音，确认时 toast `ui.message.combat.illegal-target` | `InputHandlerTest` 覆盖确认失败；`AudioRouterTest` 覆盖 target lock 只在真实 target command 播放；packaged CUA fast fixture 显示红色非法目标 cursor，`Enter` 后窗口 hash 不变 | `client/src/test/kotlin/com/ktome/client/audio/AudioRouterTest.kt`; `build/whitebox/phase4-uiux-pr05/evidence/13-pr05-fast-illegal-target-window.png`; `build/whitebox/phase4-uiux-pr05/evidence/14-pr05-fast-illegal-target-confirm-window.png` | PASS |
| disabled action | ACTION phase 显示 disabled reason，提交不发 command | `CombatDecisionFrameTest` 覆盖 resource/no-target disabled reason | `client/src/test/kotlin/com/ktome/client/ui/combat/CombatDecisionFrameTest.kt` | PASS |
| PR-02 stub 清理 | 不再保留 `ui.modal.combat_decision.stub` locale 文案 | `localeLint` 通过 | `game/src/main/resources/i18n/*.json` | PASS |
| stale bundle-id routing | CUA 目标必须绑定本轮 packaged app，而不是同 bundle id 的旧窗口 | 初次 CUA 连接命中旧 `K-ToME-broken.app`，该证据仅保留为 setup failure；通过结束旧进程并 `open -n client/build/release/K-ToME.app` 后，窗口 metadata 绑定 pid `18553` | `build/whitebox/phase4-uiux-pr05/evidence/00-resource-contract-error.png`, `build/whitebox/phase4-uiux-pr05/evidence/01-validation-setup-boss-variant.png.metadata.txt` | FIXED |

## 5. Combat Affordance Resource Audit

| 类别 | visualKey | audioCueId | manifest path | consumer test | smoke / golden evidence |
| --- | --- | --- | --- | --- | --- |
| action | `ui.combat.action.icon` | `audio.combat.action.confirm` | `client/src/main/resources/manifests/visual-manifest.json`; `client/src/main/resources/manifests/audio-manifest.json` | `CombatDecisionPanelTest`, `CombatAffordanceResourceAuditRuleTest` | PR-05 golden action hash `47c966778debf1985741421a19f7090b8a51cff073cf00a9d7b7637326008905` |
| method | `ui.combat.method.icon` | `audio.combat.method.confirm` | same runtime manifests | `CombatDecisionPanelTest`, `AudioRouterTest` phase transition coverage | PR-05 golden method hash `116391420e20d09ebfbc629e80f92d2c179d1680b4adb190b23f84357a692ddb` |
| target | `ui.combat.target.icon` | `audio.combat.target.confirm` | same runtime manifests | `CombatDecisionPanelTest`, `InputHandlerTest` target phase coverage | PR-05 golden target hash `e106c50c09013ee1c29b64578a78dad04dc3251720d204a26abe8c80d08b635d` |
| lock | `ui.combat.lock.icon` | `audio.combat.target.lock` | same runtime manifests | `AudioRouterTest.targeted combat commands emit target lock before action feedback` | PR-05 target/illegal golden hashes |
| invalid | `ui.combat.invalid.icon` | `audio.combat.invalid.submit` | same runtime manifests | `CombatAffordanceResourceAuditRuleTest`, `InputHandlerTest` no/illegal target coverage | PR-05 disabled/illegal golden hashes |

## 6. 证据路径

| 类型 | 路径或说明 |
| --- | --- |
| screenshot | Packaged CUA PNG evidence under `build/whitebox/phase4-uiux-pr05/evidence/`: `01-validation-setup-boss-variant.png` sha256 `9addf8e6ff8059d18334428c59ebfeaca74df0d8d0a2e83a1cdf0b2ce9a0795c`; `02-boss-variant-map-start.png` sha256 `45d14976fbbe3db634bacce83e13495b1c7bec4da42b3f93b4e4f49a1e24fb9c`; `03-telegraph-triple-surface-packaged.png` sha256 `94fafb83549a8f73c7562c692e0a3f4e15f45cc84a2bc5d173961080919772f8`; `04-combat-decision-action-packaged.png` sha256 `5642559177f9be2145427a340843ba8958568662041733dd633d5931057a562c`; `05-combat-decision-target-packaged.png` sha256 `4bba2d647d9727f2ef5c6bef224958f79d7c210f3868b5c7827218138b125c9e`; `06-combat-decision-save-block-target.png` sha256 `2287c74ecc8734fb035be8567b6990f08b88cc3d2c6e761ec20327a83247b70d`; `10-pr05-fast-disabled-resource-window.png` sha256 `b309484a61bd3cd83484527eb35c35d43672e70d2e3b27533c62ec0002a01ebb`; `11-pr05-fast-no-legal-target-window.png` sha256 `1ff1e475cd6be458a51be9b0ce722ece048edd5c66113fabf847ea8fb9b7641e`; `13-pr05-fast-illegal-target-window.png` sha256 `2c8969a74c2e9acc7f819751a093ea6270e36213dae08c2ff08fde0a330d2d8e`; `14-pr05-fast-illegal-target-confirm-window.png` sha256 `2c8969a74c2e9acc7f819751a093ea6270e36213dae08c2ff08fde0a330d2d8e`; `15-pr05-fast-missing-fact-window.png` sha256 `8eee169c2c7cc451049a308ee9eb76109e473e58c93c03ab7a486cd6d01d109a`. `12-invalid-stale-resource-window.png` is excluded because metadata points to an old PR03 broken app process. |
| screen recording | `N/A` |
| golden hash | `./gradlew goldenScreenshot` passed |
| smoke artifact | `./gradlew clientSmoke` passed |
| copied payload | `N/A` |
| log excerpt | `build/whitebox/phase4-uiux-pr05/evidence/app.log` records packaged app launch with isolated `JAVA_TOOL_OPTIONS`; each accepted PNG has `.metadata.txt` and `.sha256` sidecars with `capture_mode=macos-window-id`, `window_owner=K-ToME`, and a current packaged app `window_pid` (`36379` for disabled/no-legal, `54365` for illegal/missing-fact) |

## 6.1 Packaged CUA 快速验证模式评估

结论：需要优化，但不是为了本次阻塞式修 bug，而是为了后续 PR05 快速回归的稳定性。

当前可复用能力：

1. `ValidationPreset.BOSS_VARIANT` 已固定 `seed=20260412`，能从 packaged app 进入 `grey_gate_depths`。
2. Validation overlay 已有 `Travel -> Boss` 与 `Encounter -> trigger boss telegraph`，可以快速触发 telegraph 三位一体。
3. `Enter` 可以从 MAP 打开真实 `CombatDecisionFrame`，ACTION 与 TARGET 可 live 验证。

当前缺口：

1. METHOD fast surface 曾通过 CUA 观察，但早期截图不是 `scripts/capture-macos-app-window.sh` 产物；本轮按用户要求不重抓 METHOD。
2. CUA 仍依赖少量手动菜单移动，容易受 section/action cursor 偏移影响。
3. 同 bundle id 的旧白盒 app 会污染 Computer Use 路由；执行前必须先确认 `window_pid` 对应 `client/build/release/K-ToME.app`。本轮已排除 `12-invalid-stale-resource-window.png`。

建议最小优化：

1. 保留当前 validation-only PR05 fast surface，并继续扩展为固定入口脚本，materialize telegraph + multi-method action + disabled/no-target/illegal-target/missing-fact fixtures。
2. 将该入口只挂在 `ValidationPreset.BOSS_VARIANT` 或新 PR05 preset 下，不进入正式 gameplay authority。
3. 为 CUA manual record 固定脚本：`open -n client/build/release/K-ToME.app -> Validation PR05 -> capture action/method/target/boundary`，避免后续白盒靠人工逐项找状态。

## 7. 签收结论

1. 已补通过项：packaged CUA fast path 覆盖 `disabled-resource / no-legal-target / illegal-target / missing-fact`，其中本轮补跑并留证 `illegal-target` 与 `missing-fact`。
2. 剩余证据说明：METHOD fast surface 曾通过 CUA 观察，但早期截图不是窗口脚本产物；本轮按用户要求不重抓 METHOD。
3. 可进入下一 PR：`conditional yes`，前提是接受 METHOD 由 automated/golden 与人工观察记录覆盖；若要求所有 packaged CUA 证据都必须来自窗口脚本，则 METHOD 仍需补一张脚本截图。

## 7.1 Claude Review Follow-Up

输入报告：`tmp/uiux5-review.md`，已按 repo/source truth 逐项核实。

已修复并验证的有效问题：

1. `ActionHintModelBuilder` 不再把 `snapshot.uiState.targetablePositions` 当作 per-action legal target count；targeted action 的 legal target summary 现在显示 missing fact，真正的 no-legal-target 保留在 TARGET phase 防御路径。
2. `CombatDecisionFrame.disabledReasonKey` 不再用全局 target list 空值禁用 ACTION phase action，避免 client 侧伪造规则判断。
3. inscription action 的 cost/range 不再硬编码为 `0 / ui.resource.none / 0..0`；snapshot 未暴露的字段以 missing fact 呈现。
4. `ClientSmokeHarnessTest` 新增 PR05 validation fast surface smoke，覆盖 packaged/manual 同源的 PR05 client-only combat decision 入口。
5. PR05 golden hash 改为 label-keyed map，失败时能定位到 `action / method / target / disabled / illegal` 具体 surface；本轮同步了 missing-fact 文案导致的 PR05 action hash。

未在本轮扩张的事项：

1. 真实 gameplay 的 multi-method action contract 仍未扩张；当前 METHOD 由 validation-only fixture、InputHandlerTest 与 golden 覆盖，避免为 PR05 在 client 侧发明第二套 method authority。
2. 人工双人复核仍为 `Pending independent reviewer`，本轮只完成 Codex packaged CUA 与自动化验证。

## 8. 双人签收

| 角色 | 姓名 / ID | 结论 | 备注 |
| --- | --- | --- | --- |
| 记录人 | `Codex` | `FAST_ILLEGAL_MISSING_FACT_AND_REVIEW_FIX_PASS` | `2026-04-23 21:39-21:42, 22:00-22:35 Asia/Shanghai`; packaged CUA evidence captured with `scripts/capture-macos-app-window.sh`; Claude review fix verified by focused tests and goldenScreenshot |
| 复核人 | `Pending independent reviewer` | `AWAITING_REVIEW` | `Manual white-box dual sign-off not completed in this automation-only pass` |
