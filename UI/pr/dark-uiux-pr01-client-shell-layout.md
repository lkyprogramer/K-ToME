# Dark UI/UX PR-01 Client Shell And Entry Screens Layout

**阶段**: `dark-uiux-pr01-client-shell-layout`
**优先级**: `P0`
**工作量**: `L`
**前置条件**: PR-00 完成。
**资源生成结论**: 不生成正式资源，全部使用 primitive 绘制或现有 manifest key。

## 0. 开发治理与验收矩阵

本 PR 继承 [development-governance.md](./development-governance.md)。执行前先跑 `acceptanceContractLint`，再跑 client focused tests、client evidence 和最终 `verifyChanged`。通用验证阶梯见 [docs/verification/README.md](../../docs/verification/README.md)，AI / agent 红线见 [docs/rule/ai-change-governance.md](../../docs/rule/ai-change-governance.md)。

### Acceptance Matrix

| requirementId | source | owner | fastCheck | ownerGate | artifact | whitebox |
| --- | --- | --- | --- | --- | --- | --- |
| `UI01-M01` | §3 shell layout / renderer 拆分 | `client` | `GameShellLayoutTest`, `TileRendererCanvasTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/` | `required` |
| `UI01-M02` | §3 dark UI token / fixed dimensions | `client` | `AsciiRenderModelTest`, layout focused tests | `goldenScreenshot` | `client/build/reports/golden/` | `required` |
| `UI01-M03` | §5 必测行为 | `client` | PR focused client tests | `clientSmoke` | `build/reports/tests/` | `N/A` |
| `UI01-M04` | §6 验证命令 / governance | `docs` / `tools` | `acceptanceContractLint` | `maintainabilityLint`, `verifyChanged` | `build/verification/verify-changed/full-task-duration-summary.{json,md}` | `N/A` |
| `UI01-M05` | §3 首页 / 主菜单 / 验证入口 | `client` | `MainMenuFocusPolicyTest`, `MainMenuControllerTest`, `MainMenuScreenTextTest` | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/` | `required` |
| `UI01-M06` | §3 standalone screen token 与 validation setup | `client` | `ValidationSetupControllerTest`, `GameAppLifecycleTest` validation cases | `clientSmoke`, `goldenScreenshot` | `client/build/reports/golden/` | `required` |

### Gate Budget

预计重型任务：`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-01 改 client shell layout、首页/主菜单、验证入口、standalone screen token、renderer 拆分和 presentation token。

### Canonical Artifact

canonical evidence 固定为 client golden output、client smoke report、focused test report、`UI/manual-records/dark-uiux-pr01-shell.md` 和 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。debug-only screenshot metadata 或本机窗口坐标不得成为合同。

### Failure Rule

layout / golden 失败时先补 focused layout test 或修 renderer 尺寸约束；首页/验证 setup 失败时先修 `MainMenu*` / `ValidationSetup*` presentation 和 focus policy；不得用人工观察替代 `clientSmoke`、`goldenScreenshot`。

## 1. 阶段目标

1. 建立新 UI 的入口骨架：首页/主菜单、角色创建、继续游戏状态、验证模式入口必须先统一 token、焦点和布局。
2. 建立局内结构骨架：左侧导航栏、中央地图、右侧玩家面板、底部 HUD。
3. 引入或收敛暗黑 UI token：背景、边框、slot、bar、状态色、spacing、固定尺寸、standalone screen header/body/footer。
4. 拆分 `TileRenderer` 的 map、left rail、right panel、bottom HUD、tooltip/modal 绘制职责。
5. 删除重复状态栏，快捷键提示迁移到底部紧凑 footer。
6. 明确 ASCII 只作为 debug/fallback path，Tile dark UI 和 standalone dark screen 是正式玩家路径。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt` | 扩展 dark UI token |
| `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt` | 首页/主菜单暗黑 layout、焦点态、禁用态、帮助文案和角色创建区域 |
| `client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt` | 保持主菜单 action/focus 语义，补验证入口与 disabled state 验收 |
| `client/src/main/kotlin/com/ktome/client/screen/MainMenuFocusPolicy.kt` | 保持键盘优先焦点顺序和 continue/validation fallback 行为 |
| `client/src/main/kotlin/com/ktome/client/ui/creation/PlayerCreationPanel.kt` | 角色创建/职业选择使用 dark token、固定区域和长文本约束 |
| `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupScreen.kt` | 验证模式 setup 页迁移到 dark standalone screen token |
| `client/src/main/kotlin/com/ktome/client/screen/ValidationSetupController.kt` | 保持 scenario 选择和 start/back input 语义 |
| `client/src/main/kotlin/com/ktome/client/screen/GameOverScreen.kt` / `VictoryScreen.kt` / `UiErrorScreen.kt` | 本 PR 建立 shared standalone screen token；PR-07 最终补 golden/白盒 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 拆分绘制入口和布局函数 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 增加 shell 所需 presentation model |
| `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt` | 保持 ASCII fallback 语义一致 |
| `client/src/test/kotlin/com/ktome/client/render/GameShellLayoutTest.kt` | 新增或更新 bounds 不重叠、最小窗口、底部 HUD 单一状态测试 |
| `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | 覆盖文本不越界、tooltip/modal 层级、slot 固定尺寸 |
| `client/src/test/kotlin/com/ktome/client/render/AsciiRenderModelTest.kt` | 确认 ASCII fallback 日志和状态语义未丢失 |
| `client/src/test/kotlin/com/ktome/client/screen/MainMenuFocusPolicyTest.kt` | 覆盖主菜单初始焦点、continue unavailable、validation fallback |
| `client/src/test/kotlin/com/ktome/client/screen/MainMenuControllerTest.kt` | 覆盖 quick start、continue、validation mode、复制错误详情 |
| `client/src/test/kotlin/com/ktome/client/screen/MainMenuScreenTextTest.kt` | 覆盖主菜单文案、footer/help 位置、长文本不挤压 |
| `client/src/test/kotlin/com/ktome/client/screen/ValidationSetupControllerTest.kt` | 覆盖验证 setup 列表、start/back/selection |
| `client/src/test/kotlin/com/ktome/client/GameAppLifecycleTest.kt` | 覆盖首页 -> 验证 setup -> session 生命周期 |

## 3. 实现任务

1. 定义 `GameShellLayout` 或等价内部 layout helper，输出 `leftRailBounds`、`mapBounds`、`rightPanelBounds`、`bottomHudBounds`。
2. 左侧栏只展示当前 dungeon、floor、quest summary、critical hint，不承载规则状态。
3. 右侧栏只展示玩家状态、装备 slot、背包摘要、资源计数，不再重复底部生命/耐力。
4. 底部 HUD 统一展示生命/耐力/经验、日志、快捷栏、快捷键提示。
5. Tooltip 和 modal 使用同一 overlay 层级，不能遮住底部日志的最新关键反馈。
6. `UiDesignTokens` 必须预留 talent 四态 tone：locked、learnable、reserve、active；PR-04 只消费，不重新发明颜色。
7. 为 standalone screens 定义共享 dark layout primitive 或等价函数：background、title/header、primary action stack、secondary panel、footer/help、disabled detail area。
8. 首页必须显示 `Quick Start / Continue / Validation Mode` 的统一 action stack；continue unavailable 时保留可聚焦禁用态和复制详情入口。
9. 角色创建区域必须使用固定宽度/高度策略，职业、种族、区域、摘要和长说明不能推挤底部 footer。
10. 验证模式入口不能被降级成 debug-only 文本；它是主菜单的一等入口，但文案必须清楚标记 validation mode。
11. `ValidationSetupScreen` 必须移除旧硬编码临时色彩，接入 dark token；scenario/preset 列表和 active pack summary 在最小窗口下必须可读。
12. `GameOverScreen`、`VictoryScreen`、`UiErrorScreen` 在本 PR 先接入 standalone screen token 和布局约束；PR-07 负责最终 golden/packaged evidence。
13. ASCII fallback 只保留语义完整性；不得用 ASCII 截图证明 Tile dark UI 已完成。

## 4. 非目标

1. 不替换 item、skill、actor、tile 资源。
2. 不实现职业树 UI 细节。
3. 不引入鼠标点击移动、D-pad 或新输入语义。
4. 不改 `core`、`game` 规则模型。
5. 不改验证 scenario 规则、content pack 选择规则或 save/load 规则。
6. 不在 PR-01 生成正式 sprite sheet；standalone screen 资源 key 由 PR-02 接入。

## 5. 必测行为

1. `1280x800` 下四个区域不重叠，且底部只出现一套生命/耐力/经验状态。
2. 最小支持窗口下中文文本不越界；长 dungeon/quest 名称必须截断或换行。
3. 地图视口仍以玩家附近内容为主，不因新增左右栏导致玩家偏出可视中心。
4. ASCII fallback 不因为 Tile shell 改造丢失关键日志或状态信息。
5. 首页第一屏不是 marketing/说明页，而是可操作的主菜单；默认焦点落在当前最合理主操作。
6. Continue available / absent / unavailable 三种状态都有稳定视觉和焦点行为；复制错误详情不阻塞新开局或验证模式。
7. 角色创建长职业名、长区域说明、中文 locale、英文 locale 都不能遮挡 action stack 或 footer。
8. 验证模式 setup 页能用键盘选择 preset/scenario、返回主菜单、启动默认 preset。
9. Loading/error/outcome standalone screen 不保留旧红/绿/白字临时风格；PR-07 最终补 evidence 前，本 PR 至少完成 token 接入和布局约束。
10. 桌面标题和可见版本/locale 文案不包含本机绝对路径。

## 6. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.AsciiRenderModelTest :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
```

首页/验证入口 focused lane：

```bash
./gradlew :client:test --tests com.ktome.client.screen.MainMenuFocusPolicyTest --tests com.ktome.client.screen.MainMenuControllerTest --tests com.ktome.client.screen.MainMenuScreenTextTest --tests com.ktome.client.screen.ValidationSetupControllerTest --tests com.ktome.client.GameAppLifecycleTest
```

如果 `GameShellLayoutTest` 不存在，本 PR 必须新增或在等价测试中覆盖 bounds 不重叠、最小窗口不溢出、底部 HUD 文本截断。若新增 shared standalone screen helper，必须补 focused test 或 golden 覆盖首页、验证 setup、error/outcome 基础布局。

## 7. 人工白盒

1. 桌面端打开首页，确认主菜单、角色创建/职业选择、continue 状态、验证模式入口、语言/帮助/footer 均为暗黑统一风格。
2. 在 continue unavailable 场景确认 disabled 态可读、复制详情入口可聚焦、不遮挡主操作。
3. 从首页进入验证模式 setup，确认 scenario/preset 列表、active pack summary、start/back 操作和中文文本可读。
4. 桌面端进入局内主界面，确认左栏、地图、右栏、底部 HUD 均可见。
5. 确认右下角旧重复状态栏不存在。
6. 快捷键提示按分组显示，不挤压日志首行。
7. 切换窗口宽度，确认文本和 slot 不互相覆盖。
8. 必填证据：`dark-uiux-pr01-home-main-menu`、`dark-uiux-pr01-home-new-run`、`dark-uiux-pr01-continue-unavailable`、`dark-uiux-pr01-validation-entry`、`dark-uiux-pr01-shell-1280x800`、`dark-uiux-pr01-shell-min-window`。
