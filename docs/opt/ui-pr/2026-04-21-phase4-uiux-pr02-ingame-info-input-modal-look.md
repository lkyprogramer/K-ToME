> 执行前必须先完整阅读并接受：
> `docs/2026-03-13-phase2-to-phase5-final-roadmap.md`
> `docs/phase4/roadmap.md`
> `docs/phase4/2026-03-13-phase4-verification-checklist.md`
> `docs/opt/2026-04-20-client-ui-ux-reference-driven-optimization-design.md`
> `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`
> `docs/opt/ui-pr/README.md`
> `docs/opt/ui-pr/2026-04-21-phase4-uiux-pr01-client-foundation-and-main-menu.md`

# Phase 4 UI/UX - PR-02 In-Game Info, Input, Modal, Look

**阶段**: `Phase 4 late-development / phase4-uiux-pr02`  
**优先级**: `P0`  
**合并来源**: 原计划 `PR-03 局内信息面 + 统一输入语义 + Look Mode 基础版`  
**前置条件**: `PR-01` 的 token、信息面骨架和首页帮助文案已落地。  
**硬依赖条款**:

1. `PR-01` 已提供 `UiDesignTokens.color.focus.ring` / focus ring width，本 PR 不新增第二套焦点色。
2. `PR-01` 首页帮助区中的键位说明必须以本 PR truth table 为准回写。
3. `docs/opt/ui-pr/README.md` 中 `ITEM_COMPARE / COMBAT_DECISION / ExplainPane` deferred 清单是本 PR 的退出边界。
4. `ValidationPreset.MAPGEN_DIFF` 当前已存在于 `com.ktome.game.validation.ValidationPreset` 枚举合同中，本 PR 复用 `seed=20260401`，不新建第二 preset。

**对应问题**: `InputHandler` 当前以扁平 `UiMode + OverlayState + reconcileMode(snapshot)` 管理 overlay。背包、装备、talent、inspect、targeting、shop、world map、stat assign 与 validation 的 owner 边界不清，`ESC / F / X / I / L / Backspace` 等键位历史语义重叠。后续 item、状态、说明和战斗面都依赖这一层先冻结。

---

## 1. 阶段目标

冻结局内信息面、统一输入语义、`ModalStack`、焦点恢复和基础 Look Mode。

完成标准：

1. `MAP` 是根态。
2. `WORLD_MAP / SHOP / STAT_ASSIGN` 是 snapshot 被动态，由 `reconcileMode(snapshot)` 接管。
3. `INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT / TARGETING` 是主动 modal frame，由 `ModalStack` 接管。
4. `VALIDATION` 继续由 validation owner 管理，不进入正式 modal stack。
5. `ESC` = 全退；`Backspace` = 退一层；`F` 只在 overlay/modal 内保留 legacy close alias，`MAP` 根态为 no-op。
6. `Tab / Shift+Tab` 在地图态切换世界/上下文/角色动作焦点，在 modal 内只循环当前 frame 焦点。
7. `UiMode.INSPECT` 升级为基础 Look Mode，不新增 `LOOK` 枚举。
8. `InputHandlerTest` 冻结后置 truth table。

## 2. 当前问题

1. `InputHandler.overlayCloseBindings` 当前以 `F` 为 close 主入口，`ESC` 没有统一全退语义。
2. `X` 在地图态进入 inspect，但在部分 overlay 中兼作下移。
3. `OverlayState` 同时承载 inventory、shop、route、targeting、inspect、validation 等局部游标，缺少 frame owner。
4. `reconcileMode(snapshot)` 会根据 route/shop/stat allocation 强制切换，但没有统一提示 active frame 被清空。
5. 当前 inspect 可移动光标，但还不是明确的 Look Mode 信息同步路径。

### 2.1 本 PR 必须冻结的口径

1. `ModalStack` 保存 client-local `ModalFrame(kind, localState)`，不直接保存原始 `UiMode`。
2. `ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION` 只作为 `ModalFrame.kind`，不新增 `UiMode`。
3. stack 深度最大为 `3`；`ITEM_COMPARE` deferred stub 也占一层深度。
4. 被动态接管时清空 active stack 回 `MAP`，不做隐藏栈恢复。
5. 被动态接管必须产生 `ui.message.force-switch.world-map / shop / stat-assign` toast 或日志提示。
6. `VALIDATION` 独立，`I / J / K / L` 不再作为 validation inspect cursor 移动。
7. `Ctrl+S` 是系统级保存；`COMBAT_DECISION` deferred frame 中先落 blocked stub，真实文案由 PR-05 补齐。

## 3. 范围与非目标

### 3.1 范围

1. 修改：
   - `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
   - `client/src/main/kotlin/com/ktome/client/input/ValidationCommandSource.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt`
   - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/FoundationGameScreen.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuScreen.kt`
   - `client/src/main/kotlin/com/ktome/client/screen/MainMenuSummaryModel.kt`
   - `game/src/main/resources/i18n/zh-CN.json`
   - `game/src/main/resources/i18n/en-US.json`
2. 新增：
   - `client/src/main/kotlin/com/ktome/client/ui/layout/ModalStack.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/layout/PaneFocusController.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/LogPresentationModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/PlayerCardModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/TargetCardModel.kt`
   - `client/src/main/kotlin/com/ktome/client/ui/panel/ActionPanelModel.kt`
3. 更新测试：
   - `client/src/test/kotlin/com/ktome/client/input/InputHandlerTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/layout/ModalStackTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ui/layout/PaneFocusControllerTest.kt`
   - `client/src/test/kotlin/com/ktome/client/render/TileRendererCanvasTest.kt`
   - `client/src/test/kotlin/com/ktome/client/ClientSmokeHarnessTest.kt`
   - `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt`

### 3.2 非目标

1. 不实现完整 advanced tooltip。
2. 不实现 PR-05 的三层战斗决策面，只预留 `COMBAT_DECISION` frame 接入点。
3. 不改 `RenderSnapshot` schema。
4. 不新增普通敌人 intent。
5. 不把 validation overlay 改成正式玩家面。
6. 不新增批量图片或音频资源。

## 4. 技术方案

### 4.1 `ModalStack`

建议结构：

```kotlin
enum class ModalFrameKind {
    INVENTORY,
    LOADOUT_EDIT,
    TALENT_ASSIGN,
    INSPECT,
    TARGETING,
    ITEM_DETAIL,
    ITEM_COMPARE,
    COMBAT_DECISION,
}
```

规则：

1. `push` 超过深度 `3` 时 fail fast，抛出 `IllegalStateException`；client 入口必须保证合法 push，不做自动压平或静默丢弃。
2. `Backspace` 深度 `> 1` 时 pop；深度 `== 1` 时 close 到 `MAP`。
3. `ESC` 清空 stack 到 `MAP`。
4. `ITEM_COMPARE / COMBAT_DECISION` 可先 no-op，但必须由 `ModalStackTest` 覆盖深度和 pop，由 `InputHandlerTest` 覆盖业务键 no-op 与 `Ctrl+S` blocked stub。
5. stack 不保存 `WORLD_MAP / SHOP / STAT_ASSIGN / VALIDATION`。

deferred frame 的临时行为固定为：

1. `push` 后真实占用一层 stack 深度。
2. `render` 返回空视图或当前 frame 的 explicit placeholder，不绘制玩家可见正式功能。
3. `pollCommand` 对业务键返回 no-op；`ESC` 全退，`Backspace` pop。
4. `COMBAT_DECISION + Ctrl+S` 返回 blocked stub，不执行保存；PR-05 补 `ui.message.save.blocked-in-combat-decision` 真实 toast。
5. PR-03/PR-05 收口前不得删除这些 stub 测试。

### 4.2 `PaneFocusController`

地图态焦点锚点：

1. `WORLD`
2. `CONTEXT`
3. `CHARACTER_ACTION`

行为：

1. `Tab` 正向循环。
2. `Shift+Tab` 反向循环。
3. modal 打开后暂停地图锚点，`Tab` 只在 frame 内循环。
4. modal 关闭后恢复打开前地图锚点。
5. 被动态接管并清空 active stack 时，地图锚点重置为 `WORLD`。
6. 被动态接管导致的 reset 优先于 modal 关闭后的锚点恢复；一旦 reset 发生，保存的“打开前锚点”必须清空。

### 4.3 输入 truth table

`InputHandlerTest` 至少覆盖：

1. `MAP`: `ESC / Backspace / Tab / Shift+Tab / I / L / X / . / Numpad5 / Space / F / 1-9 / Ctrl+S`
2. `INVENTORY`: `ESC / Backspace / Tab / I / F / Space / Enter / E / X / Ctrl+S`
3. `LOADOUT_EDIT`: `L / ESC / Tab / 1-4 / X / Ctrl+S`
4. `INSPECT`: movement / `I J K L` no-op / `X` close / `ESC` / `?` / `Tab` / `Ctrl+S`
5. `TARGETING`: `ESC / F / Backspace / Space / Enter / Tab / Ctrl+S`
6. `SHOP`: `I` close / `X` no-op / `Ctrl+S`
7. `WORLD_MAP`: `Enter / Space / E` confirm / `X` no-op / `Ctrl+S`
8. `STAT_ASSIGN`: `1-4` / `Ctrl+S`
9. `TALENT_ASSIGN`: `T / ESC / Tab / Backspace / R / X / Ctrl+S`
10. `VALIDATION`: `ESC / F9 / WASD / arrows / I J K L / Enter / Space / E / Ctrl+S`

行为矩阵：

| mode | key / group | mode remains | mode changes | stack changes | command emitted | toast/log | save allowed |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `MAP` | `ESC / Backspace / F` | yes | no | none | no-op | none | N/A |
| `MAP` | `I / L / X` | no | `INVENTORY / LOADOUT_EDIT / INSPECT` | push active frame | open frame | none | N/A |
| `MAP` | `. / Numpad5 / Space` | yes | no | none | wait turn | none | N/A |
| `INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT` | `ESC` | no | `MAP` | clear stack | close all | none | N/A |
| `INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT` | `Backspace` | depends on depth | previous frame or `MAP` | pop one frame | close current | none | N/A |
| `SHOP / WORLD_MAP / STAT_ASSIGN` | owner keys | yes until owner resolves | delegated passive owner | no active stack mutation | delegated owner command | delegated owner feedback only | follows `Ctrl+S` rules |
| `VALIDATION` | `WASD / arrows / Enter / Space / E / F9` | delegated validation owner | delegated validation owner | no active stack mutation | validation command | validation-owned; cannot open player inspect | no |
| `VALIDATION` | `I / J / K / L` | yes | no | none | no-op | debug log optional | no |
| `TARGETING` | target confirm keys | delegated targeting owner | delegated targeting owner | target cursor only; no modal stack mutation | target command | targeting-owned; cannot save | no |

delegation contract：

1. `SHOP / WORLD_MAP / STAT_ASSIGN` 不创建 active stack，不允许 client-local close all，`Ctrl+S` 必须遵循本节统一 save gate。
2. `VALIDATION` 不打开玩家 Look/Inspect，不保存正式 run，不吞掉 `ESC / F9` 关闭语义。
3. `TARGETING` 只修改 target cursor / target command，不保存半确认目标，不绕过 `ESC / Backspace / Ctrl+S` 的全局语义。

`Ctrl+S` 规则：

1. `MAP / INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT / SHOP / WORLD_MAP / STAT_ASSIGN` 保存并保持当前 mode、modal stack、被动态和焦点锚点不变。
2. `TARGETING` 不保存，返回阻断反馈，避免保存半确认目标的瞬态上下文。
3. `VALIDATION` 不保存，toast `ui.message.save.blocked-in-validation`，避免把验证 overlay 的调试态写入正式 save。
4. `COMBAT_DECISION` deferred frame 先落 blocked stub；PR-05 补真实 toast 和 locale key。

Forward-compatible note：PR-04 落地 `ExplainPane` 后，`INSPECT + Backspace` 的优先级修订为：若 `ExplainPane` 已打开，先关闭 `ExplainPane`；否则按本 PR 冻结行为回 `MAP`。

`TARGETING` 临时状态：PR-05 落地 `CombatDecisionFrame` 后，本表 `TARGETING` 行只保留为兼容壳；正式断言迁移到 `CombatDecisionFrame.TARGET`，删除条件是旧直接 targeting 入口完全消失并由 PR-05 的 `CombatDecisionFrameTest` 覆盖。

### 4.4 Look Mode 基础版

要求：

1. `UiMode.INSPECT` 直接升级，不新增 `LOOK`。
2. 光标可用 `WASD / arrows / numpad` 在地图内移动。
3. 当前格 actor、terrain、prop、items、telegraph、status 同步到上下文面/目标卡/inspect 面。
4. 空地显示空态，不显示 `null` 或裸 key；PR-02 阶段允许临时用 `RenderTextTokenSnapshot("ui.inspect.empty.tile")`，PR-03 必须迁移到 `UiEmptyState`。
5. `?` 在 inspect frame 内预留 `ExplainPane` sub-view；PR-02 阶段为 no-op + debug log，不产生 renderer 可见变化，PR-04 再补完整说明与 Backspace 优先级。
6. PR-03 迁移到 `UiEmptyState` 时，最小 shape 固定为：`title = ui.empty.inspect.title`，`detail = ui.empty.inspect.detail`，`primaryCta = null`。

### 4.5 被动态强制接管

被动态：

1. `activeRouteSelection != null` -> `WORLD_MAP`
2. `activeShop != null` -> `SHOP`
3. `hasPendingStatAllocation(snapshot)` -> `STAT_ASSIGN`

规则：

1. 当前存在 active modal stack 时先清空。
2. 切换到被动态。
3. 生成 `ui.message.force-switch.*` 提示。
4. 不保存 hidden stack。

固定 locale key：

| key | 所属面 | 示例文本 |
| --- | --- | --- |
| `ui.message.force-switch.world-map` | 被动态接管 | `路线选择已接管当前界面。` |
| `ui.message.force-switch.shop` | 被动态接管 | `商店已接管当前界面。` |
| `ui.message.force-switch.stat-assign` | 被动态接管 | `有待分配属性点，已切到属性分配。` |
| `ui.inspect.empty.tile` | Look Mode 空态 | `这里没有可检视目标。` |
| `ui.message.save.blocked-in-validation` | validation save block | `验证模式中不能保存。` |

## 5. 推荐改动面

### 5.1 `client/input`

1. `mode` 与 `ModalStack` 联动，`overlayState()` 从 stack 派生 mode/游标。
2. `pollCommand(...)` 先做 snapshot 被动态 reconcile，再分发 owner。
3. `F` 只作为 legacy close alias。
4. `X` 从非 inspect 下移职责中移除。
5. `Ctrl+S` 在 map/modal 保存并保持上下文；targeting/combat decision 按文档阻断或预留阻断点。

### 5.2 `client/render`

1. 输出 `LogPresentationModel`、`PlayerCardModel`、`TargetCardModel`、`ActionPanelModel`。
2. 对地图三类焦点锚点绘制可见 focus ring，颜色/宽度消费 `UiDesignTokens.color.focus.ring` / focus ring width。
3. `INSPECT` 对象摘要同步到上下文/目标卡。
4. 空态文案走 locale：空背包、空商店、无可检视目标、无日志。

### 5.3 `client/screen`

1. `FoundationGameScreen` 继续使用 `CommandSource`，不新增第二输入通道。
2. 若已有 `onSnapshotUpdated(...)`，优先复用来接收 force-switch 提示。

## 6. 测试与自证

### 6.1 必测行为

1. `ESC` 和 `Backspace` 不再语义重叠。
2. `F / I / L / X` 的兼容/废弃决议固化。
3. `Tab` 焦点链不会越界、卡死或穿透错误 owner。
4. modal 关闭后恢复地图光标和焦点区。
5. 被动态接管时清空 active stack，并有提示。
6. Look Mode 可自由移动并同步对象信息。
7. 首页帮助区 `ui.menu.help.primary-keys` 与本 PR truth table 一致。

### 6.2 自动化命令

所有 Gradle 命令必须串行执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:test --tests "com.ktome.client.input.InputHandlerTest" --tests "com.ktome.client.render.TileRendererCanvasTest" --tests "com.ktome.client.ClientSmokeHarnessTest"
./gradlew :client:test --tests "com.ktome.client.ui.layout.ModalStackTest" --tests "com.ktome.client.ui.layout.PaneFocusControllerTest"
./gradlew :client:test --tests "com.ktome.client.golden.GoldenScreenshotHarnessTest"
./gradlew localeLint contractLint maintainabilityLint
./gradlew clientSmoke goldenScreenshot verifyChanged
```

若 §7 触发 image/audio plan 或 manifest/`--extra-plan` 接线，必须补：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew assetLint styleLint manifestLint audioLint
./scripts/verify-bootstrap.sh
```

### 6.3 人工白盒验证流程

固定环境：

1. `locale`: `zh-CN`
2. 窗口尺寸：`1280x800`
3. validation preset：`MAPGEN_DIFF` seed `20260401`
4. 记录文件建议：`docs/opt/ui-pr/manual-records/phase4-uiux-pr02-input-look.md`

流程：

1. 启动：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew :client:run
```

2. 地图态：
   - `Tab / Shift+Tab` 在世界、上下文、角色动作三锚点循环
   - `ESC / Backspace` 根态 no-op
   - `. / Numpad5 / Space` 等待一回合
   - `1-9` 优先 inscription，未命中再 talent
3. modal 栈：
   - `I` 打开 inventory
   - 进入 item detail，再进入 compare deferred stub
   - `Backspace` 逐层 pop
   - `ESC` 直接回 `MAP`
   - 关闭后地图焦点锚点和光标恢复
4. Loadout / Talent：
   - `L` 打开/关闭 loadout
   - talent assign 中 `Backspace` 仍是 draft rollback
   - `X` 不再在列表里下移
5. Look Mode：
   - `X` 从地图进入 inspect
   - 移动到空地、敌人、地形、prop、掉落物、telegraph cell
   - 检查世界面、上下文面、目标/检视卡同步
   - `I / J / K / L` no-op
6. 被动态接管：
   - 打开 inventory 或 inspect
   - 触发 shop、world map 或 stat assign
   - 确认 active stack 清空并出现 `ui.message.force-switch.*`
7. validation overlay：
   - `WASD / arrows` 切 section/action
   - `I / J / K / L` no-op
   - `ESC / F9` 关闭后地图移动恢复
8. 保存证据：
   - 输入序列表：`mode / keys / expected / actual`
   - `phase4-uiux-pr02-modal-stack`
   - `phase4-uiux-pr02-look-mode`
   - `phase4-uiux-pr02-force-switch`

通过标准：

1. truth table 中每个 owner 至少被手动抽样覆盖。
2. 任一模式下没有“按键被吞但无反馈”的高风险路径。
3. Look Mode 和 targeting 不互相污染 cursor。

### 6.4 统一验证框架关系

本 PR 不新增 Phase 4 white-box domain，但人工白盒记录是正式验收面。`GoldenScreenshotHarnessTest` 不能替代输入语义验证；若 golden 被跳过，必须保留手工截图或输入记录。

## 7. 资源生成计划

### 7.1 图片

默认不新增 image plan。只有 `Inspect / Look Mode / Help Overlay` 缺少稳定正式图标时，才允许新增：

```text
assets-src/image/specs/phase4-uiux-pr02-gemini-plan.yaml
assets-src/image/manifests/phase4-uiux-pr02-generation-report.jsonl
assets-src/image/manifests/phase4-uiux-pr02-processing-report.jsonl
```

禁止为了 panel 装饰、drawer 花边或 Look Mode 氛围批量补图。

### 7.2 音频

默认复用 `audio.ui.*`。只有 modal 打开/关闭/错误与通用 confirm/cancel 无法区分时，才允许新增：

```text
assets-src/audio/specs/phase4-uiux-pr02-audio-plan.yaml
assets-src/audio/manifests/phase4-uiux-pr02-processing-report.jsonl
```

### 7.3 约束

1. 若新增 plan，必须同步 `build.gradle.kts --extra-plan`。
2. 新 key 必须被 smoke/golden 消费。
3. deferred `ITEM_COMPARE` stub 不视为孤儿 key，但必须有测试说明。
4. 若 deferred frame 录入 golden label，`ITEM_COMPARE` 必须使用 `phase4-uiux-pr02-item-compare-stub-*` 前缀，`COMBAT_DECISION` 必须使用 `phase4-uiux-pr02-combat-decision-stub-*` 前缀。
5. 未生成正式资源但使用 fallback 时，PR description 必须包含 [Resource Fallback Audit](resource-fallback-audit-template.md)。

## 8. 出口门禁

1. `ModalStack`、`PaneFocusController`、基础 Look Mode 已落地。
2. `InputHandlerTest` 覆盖 truth table 最小行为面。
3. `ESC / Backspace / Tab / F / I / L / X / Ctrl+S` 语义冻结。
4. 被动态接管有提示且不保留 hidden stack。
5. `LogPresentationModel` 至少具备分类、重要度、空态和回退文案字段，并被 smoke 或 renderer test 覆盖。
6. `ITEM_COMPARE / COMBAT_DECISION` deferred frame 的占栈、空视图、no-op、pop 行为有测试。
7. 若 deferred stub 进入 golden，label 前缀符合 README 的 stub 命名规则，便于 PR-03/PR-05 自动清理或重录。
8. smoke/golden 命中新增或更新场景。
9. 人工白盒记录包含输入序列、force-switch、Look Mode 和 validation overlay 证据。
10. 没有新增第二 keyword、telegraph、intent、snapshot 或 validation owner。
11. `ui.menu.help.primary-keys` 已按最终 truth table 回写，新增 locale key 已进入 `zh-CN/en-US` 并由 `localeLint` 覆盖。
