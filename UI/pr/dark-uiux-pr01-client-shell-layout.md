# Dark UI/UX PR-01 Client Shell Layout

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

### Gate Budget

预计重型任务：`:client:clientSmoke`、`:client:goldenScreenshot`、`maintainabilityLint`、`verifyChanged`。触发原因是 PR-01 改 client shell layout、renderer 拆分和 presentation token。

### Canonical Artifact

canonical evidence 固定为 client golden output、client smoke report、focused test report 和 `build/verification/verify-changed/full-task-duration-summary.{json,md}`。debug-only screenshot metadata 或本机窗口坐标不得成为合同。

### Failure Rule

layout / golden 失败时先补 focused layout test 或修 renderer 尺寸约束；不得用人工观察替代 `clientSmoke`、`goldenScreenshot`。

## 1. 阶段目标

1. 建立新 UI 的结构骨架：左侧导航栏、中央地图、右侧玩家面板、底部 HUD。
2. 引入或收敛暗黑 UI token：背景、边框、slot、bar、状态色、spacing、固定尺寸。
3. 拆分 `TileRenderer` 的 map、left rail、right panel、bottom HUD、tooltip/modal 绘制职责。
4. 删除重复状态栏，快捷键提示迁移到底部紧凑 footer。

## 2. 影响范围

| 区域 | 预期改动 |
| --- | --- |
| `client/src/main/kotlin/com/ktome/client/ui/token/UiDesignTokens.kt` | 扩展 dark UI token |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt` | 拆分绘制入口和布局函数 |
| `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt` | 增加 shell 所需 presentation model |
| `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt` | 保持 ASCII fallback 语义一致 |
| `client/src/test/kotlin/com/ktome/client/render/GameShellLayoutTest.kt` | 新增或更新 bounds 不重叠、最小窗口、底部 HUD 单一状态测试 |
| `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt` | 覆盖文本不越界、tooltip/modal 层级、slot 固定尺寸 |
| `client/src/test/kotlin/com/ktome/client/render/AsciiRenderModelTest.kt` | 确认 ASCII fallback 日志和状态语义未丢失 |

## 3. 实现任务

1. 定义 `GameShellLayout` 或等价内部 layout helper，输出 `leftRailBounds`、`mapBounds`、`rightPanelBounds`、`bottomHudBounds`。
2. 左侧栏只展示当前 dungeon、floor、quest summary、critical hint，不承载规则状态。
3. 右侧栏只展示玩家状态、装备 slot、背包摘要、资源计数，不再重复底部生命/耐力。
4. 底部 HUD 统一展示生命/耐力/经验、日志、快捷栏、快捷键提示。
5. Tooltip 和 modal 使用同一 overlay 层级，不能遮住底部日志的最新关键反馈。
6. `UiDesignTokens` 必须预留 talent 四态 tone：locked、learnable、reserve、active；PR-04 只消费，不重新发明颜色。

## 4. 非目标

1. 不替换 item、skill、actor、tile 资源。
2. 不实现职业树 UI 细节。
3. 不引入鼠标点击移动、D-pad 或新输入语义。
4. 不改 `core`、`game` 规则模型。

## 5. 必测行为

1. `1280x800` 下四个区域不重叠，且底部只出现一套生命/耐力/经验状态。
2. 最小支持窗口下中文文本不越界；长 dungeon/quest 名称必须截断或换行。
3. 地图视口仍以玩家附近内容为主，不因新增左右栏导致玩家偏出可视中心。
4. ASCII fallback 不因为 Tile shell 改造丢失关键日志或状态信息。

## 6. 验证

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests com.ktome.client.render.GameShellLayoutTest --tests com.ktome.client.render.TileRendererCanvasTest --tests com.ktome.client.render.AsciiRenderModelTest :client:clientSmoke :client:goldenScreenshot localeLint contractLint maintainabilityLint verifyChanged
```

如果 `GameShellLayoutTest` 不存在，本 PR 必须新增或在等价测试中覆盖 bounds 不重叠、最小窗口不溢出、底部 HUD 文本截断。

## 7. 人工白盒

1. 桌面端打开主界面，确认左栏、地图、右栏、底部 HUD 均可见。
2. 确认右下角旧重复状态栏不存在。
3. 快捷键提示按分组显示，不挤压日志首行。
4. 切换窗口宽度，确认文本和 slot 不互相覆盖。
