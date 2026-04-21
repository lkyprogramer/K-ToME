# K-ToME 客户端 UI/UX 优化执行计划 · 深度 Review（R2）

- **评审对象**: `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`（v2，全 1317 行）
- **上一轮 review**: `docs/review/2026-04-21-client-ui-ux-optimization-development-plan-review.md`（R1）
- **前提**: 当前及未来较长时间只有单人（lky）进行开发 / 测试 / 游玩；telemetry 与行为统计明确后置
- **Review 目标**: 在 R1 基础上，评估 v2 是否吸收了 R1 的 P0 建议；同时在"最细小的建议"尺度上进一步挑出会让后续开发继续乱猜的点，保证 PR-01 ~ PR-08 可以真正当作"执行手册"被 push 下去

---

## 0. Review 基准与总体判断

### 0.1 基准

本轮 review 再次以仓库真源为 ground truth，验证了以下关键文件/符号：

1. `core/src/main/kotlin/com/ktome/core/ai/AIRuntimeState.kt` → `PendingTelegraphState` **确实存在**（`data class`）
2. `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt` → `internal object TelegraphRenderer`，公开方法 `tileRows / asciiLines / alpha / fallbackColorHex`；私有 `tileTone / asciiTone` 返回枚举，不是 hex
3. `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt` → `UiMode` 仍是 10 枚举；`overlayCloseBindings = listOf(Keys.F)`；`waitBindings = [PERIOD, SPACE, NUMPAD_5]`（**不含 F**）；`Keys.I / Keys.L / Keys.X` 在 `TARGETING / INSPECT` 内部仍兼作 vim-style 方向键（line 537-540）
4. `build.gradle.kts` 已有 `--extra-plan` 接入模式（`assetLint / styleLint / audioLint / manifestLint`）
5. `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt` 使用 `saveFolderName` 作为 golden label 入口
6. 下述文件全部存在：`PlayerCreationPanel.kt / DescriptionPresenter.kt / CombatResolutionTrace.kt / AsciiRenderModel.kt / StatusIconResolver.kt / game/src/main/resources/data/items/index.yaml`

### 0.2 总体判断

v2 相对 v1（R1 的评审对象）已经吸收了大部分 R1 的 P0 建议，执行计划现在**基本可以当作开发手册使用**，但仍有若干细节会让实际落地时继续"乱猜"。本 review 把剩余问题按影响大小分层：

| 层级 | 含义 | 本轮数量 |
| --- | --- | --- |
| P0 | 事实错误 / 真源冲突 / 自相矛盾 → 若不修，PR 一定会写错 | 4 |
| P1 | 缺失的 truth table / 微决议 → 不修会让人停在那里拍脑袋 | 11 |
| P2 | 表达漂移、命名不统一、单人前提下偏严的条款 | 9 |
| 修订 | R1 报告自己的错误需要公开纠正 | 1 |

---

## 1. 纠正：R1 报告自己的错误（必读）

**R1 §1 曾判定 `PendingTelegraphState` 不存在，这是误判**。

真源位置：`core/src/main/kotlin/com/ktome/core/ai/AIRuntimeState.kt:9`

```kotlin
data class PendingTelegraphState(
    val telegraphSpecId: String,
    val sourceAbilityId: String,
    var remainingTurns: Int,
    val targetPoint: Point,
    val queuedAbilityId: String? = null,
    val resolvedDangerLevel: DangerLevel,
)
```

进一步 `Grep` 结果：该符号在 `FoundationGameSession.kt / SessionSnapshotMapper.kt / BossHarnessTest.kt / RenderSnapshotContractTest.kt` 等 9 个文件中被引用，是正式的运行时状态真源。

影响：

1. v2 文档 §0 / §3 / §17 / PR-08 引用的 `PendingTelegraphState -> OverlayRenderSnapshot -> TelegraphRenderer` 链路是**事实正确的**。
2. R1 报告里所有围绕"把 `PendingTelegraphState` 替换成 `OverlayRenderSnapshot + TelegraphRenderer`"的 P0 建议应作废。
3. v2 保留这一链路的描述（§3.9、§17.1 对策）不用再改。

（这条写在最前是为了防止后续开发者把 v2 文档"校准回" R1 的错误版本。）

---

## 2. P0 · 仍然存在的事实错误 / 真源冲突

### P0-1 · §5.2 键位表把 `F` 归类为"等待一回合"——与真源冲突

**问题**：§5.2 表格的第 9 行写 `F / . | 等待一回合 | 无 | 无`。

**真源**（`InputHandler.kt:62, 93`）：

```kotlin
private val overlayCloseBindings = listOf(Keys.F)
private val waitBindings = listOf(Keys.PERIOD, Keys.SPACE, Keys.NUMPAD_5)
```

`F` 是 **overlay 通用关闭键**，不是 wait 键。`.` `Space` `Numpad5` 才是 wait。

**v2 文档其它地方也承认了这一点**：§5.2b 表把 `F` 标为 "overlay 通用关闭 / 保留为 legacy close alias"——这与 §5.2 自相矛盾。

**建议**（最低改法）：

- §5.2 键位表第 9 行改为：

  | 键 | 地图态 | modal / drawer 态 | targeting / 三层决策态 | 原则 |
  | --- | --- | --- | --- | --- |
  | `. / Space / Numpad5` | 等待一回合 | 无 | 无 | 空等 |
  | `F`（过渡期） | 无（正式语义已迁移到 `ESC`） | 关闭当前 overlay（legacy alias） | 无 | 兼容别名；新帮助文案不再宣传 |

---

### P0-2 · §5.2b 键位迁移决议没有覆盖 `I / L` 当作 vim 方向键的现状

**问题**：`InputHandler.kt:537-540` 中存在如下映射（`TARGETING / INSPECT` 等面）：

```kotlin
input.isKeyJustPressed(Keys.I) -> Point(0, -1)   // 上移
input.isKeyJustPressed(Keys.L) -> Point(1, 0)    // 右移
```

v2 §5.2b 只谈了 `I = 背包开关`、`L = Loadout 开关`、`X = INSPECT 开关 / 废除下移职责`，**但没提 `I / L` 在光标移动场景里的 vim-binding**。

**影响**：PR-03 重写 `InputHandler` 时会在 `TARGETING / INSPECT` 里撞到"`I` 到底是上移还是打开背包"的二义冲突，开发时必然要停下来猜一次。

**建议**：§5.2b 表增加一行：

| 键位 | 当前语义 | 当前计划决议 | 备注 |
| --- | --- | --- | --- |
| `I / L` 在 TARGETING / INSPECT 内部兼作方向键 | 上移 / 右移（vim 风格） | **废除**；方向键统一回 `WASD / 方向键 / Numpad` | `I` 在这些面内不再承担开背包职责（被动态 frame 内不叠 frame）；`L` 同理 |

同时 §5.2b 末尾 `当前阶段明确废除的歧义` 列表增加一条：

> `I / L` 不再承担 `TARGETING / INSPECT` 中的方向键职责；方向键统一由 `WASD / 方向键 / Numpad` 承担。

---

### P0-3 · §5.2a ModalStack 示例 `Inventory -> Item Detail -> Compare` 与 UiMode 归属表不对齐

**问题**：§5.2 补充约束 1 写 `Inventory -> Item Detail -> Compare` 是栈深度 3 的典型；但 §5.2a 的 UiMode 归属表里没有 `ITEM_DETAIL` 或 `COMPARE` 这两个 frame kind。

**潜在理解分歧**：

- 理解 A：`Item Detail / Compare` 是 `INVENTORY` frame 内部的局部 sub-view，不 push 新 frame（ModalStack 深度始终 = 1）
- 理解 B：`Item Detail / Compare` 是独立 frame，push 后占 ModalStack 深度（= 3）

这两种理解直接决定 `Backspace` 的行为：

- 理解 A 下，`Backspace` 要在 `INVENTORY` 内部"回退局部视图"，不是"退一层 frame"
- 理解 B 下，`Backspace` 就是标准"pop 栈"

**建议**：§5.2a 明确选择**理解 B**（典型深度 3 的例子才有意义），并在 `ModalFrame.kind` 列表补充：

1. `INVENTORY`
2. `LOADOUT_EDIT`
3. `TALENT_ASSIGN`
4. `INSPECT`
5. `TARGETING`
6. `ITEM_DETAIL`（PR-03 新增，从 INVENTORY / SHOP push）
7. `ITEM_COMPARE`（PR-03 可选后置）
8. `COMBAT_DECISION`（PR-08 新增）

同时明确 `ITEM_DETAIL / COMPARE` 不引入新 `UiMode` 枚举，只作为 `ModalFrame.kind`——这和 §5.2a 已有的 "ModalStack 不直接保存 `UiMode`" 口径一致。

---

### P0-4 · §5.2 `Tab` 行 "在世界面 / 上下文面 / 角色动作面之间切换焦点"——当前真源里没有正式焦点链

**问题**：PR-03 §9.3 task 1 承诺"形式化四面（世界 / 上下文 / 角色-动作 / modal）"，但 v2 文档没说清"四面焦点"是新语义还是已有的。实际真源里 `InputHandler` 不维护"面级焦点"，`Tab` 也没有对应 binding。

**影响**：PR-03 落地时会面对一个没有起点的新特性，却被塞在"统一输入语义冻结"里。

**建议**：

1. §5.2 `Tab` 行补脚注：`PR-03 新增语义，当前真源未实现`
2. PR-03 §9.3 增加一条独立 task：`新增 PaneFocusController，维护「世界面 / 上下文面 / 角色动作面」三个焦点锚点；Tab = 循环下一个锚点，Shift+Tab = 反向`
3. 明确 `modal` 打开时，`Tab` 的作用范围变成 modal 内部，而不是再循环三个地图面锚点——这一点目前已经隐含，但应当写死

---

## 3. P1 · 仍然会让人"乱猜"的 11 个细节盲点

### P1-1 · §5.2a `VALIDATION` 行 owner 写"validation owner"，没有具体类名

**建议**：写死 owner 名称（例如 `ValidationSessionController` 或当前实际持有 `validationCursor / validationPanel` 的类）。PR-03 就不用再去翻 git blame。

### P1-2 · QualityPresentation 的具体颜色/边角标映射表没冻结

**背景**：§10.3 task 2 说"建立 `QualityPresentation`"，§5.3 说"品质名称色 / 品质边角标 / 与正式品质合同的映射"，但**具体映射没给**。PR-04 落地时必然还要拍一次色号。

**建议**：在 §10.3 补表（建议值仅供参考，最终由 PR-04 验证）：

| RarityTier | 名称 color token | 边角 glyph | SpecialTier accent |
| --- | --- | --- | --- |
| NORMAL | `color.quality.normal` | 无 | 无 |
| MAGIC | `color.quality.magic` | `◆` | 无 |
| RARE | `color.quality.rare` | `◆◆` | 无 |
| — | — | — | `UNIQUE` → 金色外描边；`ARTIFACT` → 紫色外描边 |

并同时在 §7.3 (PR-01) 要求 `UiDesignTokens` 首发就必须提供 `color.quality.*` 这组 token，否则 PR-04 会回头改 PR-01。

### P1-3 · §10.3 task 4 `items.firstOrNull()?.iconKey` 没定义多件掉落的排序规则

**问题**：多件掉落同格时，"首件"以什么为准？（品质优先？新掉落优先？字典序？）——这个决定将直接影响 "同格 5 件，4 件 normal 1 件 unique" 时显示哪个 icon。

**建议**：PR-04 §10.3 task 4 补具体排序：

> 多件掉落按 `SpecialTier.isNotEmpty` > `RarityTier` 倒序 > `iconKey` 字典序选 head；数量 badge 用 `items.size`，上限 `9+`。

### P1-4 · §5.4 "被动态接管时直接清空 active stack 回 MAP 基线"——缺少打断提示

**问题**：玩家正在 `INVENTORY` 看装备，session 突然让 `activeShop != null` 强制切 `SHOP`，会有一次 "我点的不见了" 的突兀体验。

**建议**：§5.4 增加一小条：

> 被动态首次接管时，必须在日志区或 toast 区给出一行 `ui.message.force-switch.*` 提示（哪条被动态接管了，原因 key）。不做打断回退，但让玩家知道"是规则，不是 bug"。

### P1-5 · §7.3 task 2 PR-01 硬编码色替换点写成 `TelegraphRenderer.fallbackColorHex / tileTone / asciiTone`

**问题**：`tileTone / asciiTone` 返回枚举（`TileTextTone / AsciiTextTone`）而不是 hex，不是"硬编码色"替换对象。真正的硬编码 hex 只有 `fallbackColorHex` 一处。

**建议**：

- §7.3 task 2 改为：
  1. 替换 `TelegraphRenderer.fallbackColorHex` 返回的 4 个 hex 为 token 引用（`token.color.telegraph.low/moderate/high/lethal`）
  2. `TileTextTone / AsciiTextTone` 的 mapping（Tone → 实际颜色）收口到 `UiDesignTokens`，而不是继续散落在 `TileRenderer / AsciiRenderer` 的各自 `when` 分支里
- 否则 PR-01 可能把 `tileTone` 直接硬拆成 token 引用，破坏 Tone 枚举本身的信息层级抽象

### P1-6 · §7.2 范围列出 `DesktopLauncher.kt`，但 §7.3 任务没对应项

**建议**：§7.3 task 4 "去掉窗口标题里的开发态操作说明"明确指向 `DesktopLauncher.kt` 的 `setTitle(...)` 调用（或真源中 title 来源），避免 PR-01 在 screen 层和 launcher 层之间再分析一次。

### P1-7 · PR-04 §10.3 task 7 "ItemRenderSnapshot.iconKey 当前保持 nullable"——`contract-lint` 覆盖路径描述不具体

**问题**：§10.4 退出条件 1 写 `contract-lint 能覆盖可装备物 / special template item 的 iconKey 完整性`，但 iconKey 保持 nullable 的情况下，这种覆盖只能走"内容层级 coverage"而非"schema 层级 nonnull"。当前 `contractLint` 的覆盖路径是什么没说清。

**建议**：PR-04 §10.3 补一个明确 task：

> 在 `tools/lint/ContractLint` 新增 `ItemIconKeyCoverageRule`：扫描 `game/src/main/resources/data/items/index.yaml` 里的所有 `equipable == true` 或 `specialTemplateId != null` 的 item，要求 `iconKey` 非空且能在 `visual-manifest.json` 中解析；缺失视为 error。

### P1-8 · PR-03 §9.3 没有给出"每个 UiMode 下每个按键的 truth table"

**问题**：§5.2 是全局语义表，§5.2b 是迁移决议表，但 PR-03 落地时最需要的是**单元测试尺度**的"在 X mode 下按 Y 键应发生 Z"列表。这个表缺失，`InputHandlerTest` 一定会写得千奇百怪。

**建议**：PR-03 §9.3 后面补 `§9.3.1 InputHandler 后置 truth table`（至少覆盖下列单元）：

| UiMode | 键 | 期望行为 | 备注 |
| --- | --- | --- | --- |
| MAP | ESC | 无（根态） | 不再显示"操作说明" |
| MAP | Backspace | 无 | 根态没有"上一层" |
| MAP | I | push INVENTORY frame | |
| MAP | L | push LOADOUT_EDIT frame | |
| MAP | X | push INSPECT frame（升级后的 Look Mode） | |
| MAP | . / Space / Numpad5 | wait 一回合 | |
| MAP | F | 无（legacy close 键在根态不触发） | |
| MAP | 1-9 | 使用 active talent slot N | |
| INVENTORY | ESC | 全退到 MAP | |
| INVENTORY | Backspace | 若 stack 深度 > 1，pop；否则 close | |
| INVENTORY | I | close INVENTORY（与 MAP 对称） | |
| INVENTORY | F | close INVENTORY（legacy alias） | |
| INSPECT | WASD / 方向键 / Numpad | 移动光标 | |
| INSPECT | I / L | 无（P0-2 废除 vim 映射） | |
| INSPECT | X | close INSPECT（与 MAP 对称） | |
| INSPECT | ESC | 全退到 MAP | |
| TARGETING | ESC | 全退到 MAP | |
| TARGETING | Backspace | 回 CombatDecision 上一层（若已接 PR-08），否则 close | |
| SHOP | I | close SHOP（与"商店对称关闭"保留） | |
| SHOP | X | 无（废除 SHOP 下移职责） | |

truth table 不需要穷举所有键，只需明确"在这个 mode 里，这些键是否有语义"。空项就写"无"——明示就是开发期可以直接落 `InputHandlerTest` 的输入。

### P1-9 · PR-08 战斗三层 "CombatDecisionFrame 内部 phase 机" 没有 phase 状态图

**问题**：§14.3 task 2 只说"内部 phase 机"，但 phase 转移规则（什么条件下从"动作层"进到"方式层"？Backspace 触发什么？ESC 触发什么？选中 target 后是否自动关闭 frame？）没定义。PR-08 落地时这些都是 1-2 小时的小决策，但不写会反复改。

**建议**：§14.3 补 `§14.3.1 CombatDecisionFrame phase 状态图`：

```
[enter frame] ──> ACTION ──(Enter 确认动作)──> METHOD
                     │
                     └─(无可选方式: 单一方式)──> TARGET
METHOD ──(Enter 确认方式)──> TARGET
TARGET ──(Enter 确认目标)──> [resolve + pop frame]

任意 phase:
  Backspace ──> 回上一 phase（ACTION phase 下 Backspace == ESC）
  ESC       ──> pop frame 直接回 MAP
```

### P1-10 · §5.5 性能 "modal 打开到首个稳定可交互帧 < 100ms"——单人前提下无测量手段

**问题**：v2 仍保留这个量化约束，但单人开发时没有 P95 统计框架（v2 §4.2 也明确说 "不做用户行为 telemetry"），这条 exit criterion 实际无法验收。

**建议**：§5.5 改为：

> 单人开发阶段以"本机可感知流畅"为准：modal 打开无可感知卡顿；如果出现可感知延迟，手动写一次 `jmh` 或 `measureNanoTime` profile，不进入常态 golden。

### P1-11 · §5.4 错误态 `Copy Error Detail` 没定义 "error detail" 源

**问题**：错误态要"复制错误详情"，但具体字段（stack trace？JSON error envelope？save 版本 hash？manifest mismatch diff？）没定义。PR-05 一定会乱定义格式。

**建议**：§5.4 补：

> 错误态 payload 固定为纯文本 `UiErrorPayload(heading, detail, contextKeyValuePairs)`；`Copy Error Detail` 复制 UTF-8 编码的格式化文本，包含 heading 一行、detail 全文、`key: value` 表，行末加 `[ktome/<build-hash>]`。

---

## 4. P2 · 表达漂移 / 单人前提下偏严的条款（小粒度建议）

这些不阻塞开发，但修掉可以减少后续 review 成本。

### P2-1 · §5.2 键位表没列 `L / X`，但 §5.2b 明确保留

**建议**：§5.2 增加两行：

| 键 | 地图态 | modal / drawer 态 | targeting / 三层决策态 | 原则 |
| --- | --- | --- | --- | --- |
| `L` | 打开 LOADOUT_EDIT | 无（LOADOUT_EDIT 内按 L 关闭） | 无 | 装备编辑对称开关 |
| `X` | 打开 INSPECT（升级为 Look Mode） | 无 | 无 | 检视模式对称开关 |

### P2-2 · §5.2 `i` 列 "modal 态: 无"——与 §5.2b 保留的 "SHOP 下按 I 关闭商店" 不一致

**建议**：§5.2 `i` 行第二列改为 "在 `INVENTORY` / `SHOP` 等对称可开关的 modal 中用于 close"。

### P2-3 · §5.2 `Ctrl+S` 行 "modal 态保存并保持当前上下文"——当前真源未实现该快捷键

**建议**：`Ctrl+S` 行加脚注：`PR-03 新增；当前真源由 GameApp/菜单入口承担保存`。避免让实现者误以为已有对应 binding。

### P2-4 · §16.2 "双语至少覆盖 zh-CN, en-US"——单人前提下偏严

**建议**：单人阶段改为：

> 金 golden 只需主语言（`zh-CN` 或实际开发者使用语言）强制抽检；另一语言以 `localeLint` + `DescriptionPresenterTest` 文字层覆盖为主，不做图层 golden，等玩家规模扩大再补。

### P2-5 · §5.6 "phase2-visual-manifest.json / phase2-audio-manifest.json"——PR-06~PR-08 是 Phase 3

**问题**：v2 §5.6 把 canonical manifest 写死成 `phase2-*`，但 PR-06 / PR-07 / PR-08 的 image plan 命名已经用 `phase3-uiux-prNN-*`。两者是否继续合流到同一个 `phase2-visual-manifest.json`？

**建议**：§5.6 明确 "当前阶段所有 uiux plan 仍合并到 `assets-src/image/manifests/phase2-visual-manifest.json` 与 `.../phase2-audio-manifest.json`，不引入 phase3 级 canonical manifest；除非整仓显式决议 `Phase 3` 的资源要分离"，或者反过来明确 "PR-06 起切到 phase3-visual-manifest.json"。二选一，不要歧义。

### P2-6 · §5.4 "加载态必须是短时过渡，不能成为长期遮罩"——"短时"没有量化

**建议**：单人阶段改为 "不超过一帧可见循环 (~200ms)"，或 "不允许在加载态下阻塞玩家输入超过 1 回合"。量化与否不强求，但要可验证。

### P2-7 · 命名小漂移：`contract-lint` / `contractLint`

**问题**：§10.4 退出条件 1 写 `contract-lint`，§10.5 推荐验证里写 `./gradlew contractLint`；同样 §11.3 task 4 写 `contentUiLint` 而 §11.2 写 `content-ui-lint`。

**建议**：全文统一用 gradle task 名 `contractLint / contentUiLint`，把 kebab-case 作为散文名仅出现在标题级描述。避免 review 时出现"这俩是不是同一个东西"的第二次确认。

### P2-8 · §13.3 PR-07 "解释型检视" 归属到哪个 frame 没说清

**问题**：PR-03 已把 `INSPECT` 升级为 Look Mode；PR-07 要把说明/关键词/构筑标签"放到同一个解释型检视里"——这个检视是 `INSPECT frame` 自身的附加 sub-pane，还是新增 `EXPLAIN` frame？

**建议**：§13.3 task 3 改为：

> 在 `INSPECT frame` 内部增加 `ExplainPane` sub-view（不新增 UiMode / ModalFrame.kind）；按 `?` 打开详情层而非 push 新 frame。

### P2-9 · §16.5 "新资源 key 已被 ClientSmokeHarnessTest 和命中的 golden 场景消费到"——缺反向条款

**建议**：补一条 "在 smoke/golden 中还没消费的 key，必须在同一 PR 内删除或标记为 deferred；禁止留'已 lint 但无测试引用'的孤儿 key"。否则 manifest 会慢慢长出一堆只为 lint 通过而存在的 stub key。

---

## 5. PR 间依赖与估时的再评估

### 5.1 依赖矩阵补充

v2 §6.1 已有硬前置，但漏了两条实际存在的弱前置：

1. **PR-01 → PR-02**：MainMenu 色值要走 token，否则 PR-02 又要自己拍 `Color.GOLD / LIGHT_GRAY` 一次
2. **PR-05 → PR-07**：共享卡片模型（`ModalCardModel`）冻结前，`DescriptionPresenter` 改造时会重复定义卡片 DOM

**建议**：§6.1 增加：

- `PR-01 -> PR-02`（弱）
- `PR-05 -> PR-07`（弱）

### 5.2 估时评估（v2 与 R1 建议对齐后的复核）

| PR | v2 估时 | R2 复核 | 说明 |
| --- | --- | --- | --- |
| PR-01 | 1.5 人日 | **2 人日** | 吸收 P1-2 / P1-5 / P1-6 后略扩 |
| PR-02 | 1 人日 | 1 人日 | 保持 |
| PR-03 | 3-4 人日 | **4-5 人日** | 吸收 P0-1/P0-2/P0-4/P1-8/P1-9 后，InputHandler 改造 + truth table + 四面焦点锚点不止 3-4 人日 |
| PR-04 | 2-2.5 人日 | **2.5-3 人日** | 吸收 P1-2 / P1-3 / P1-7 |
| PR-05 | 1.5 人日 | 1.5-2 人日 | P1-11 使 `UiErrorPayload` 定型略扩 |
| PR-06 | 1.5-2 人日 | 1.5-2 人日 | 保持 |
| PR-07 | 1.5-2 人日 | 1.5-2 人日 | P2-8 归 INSPECT frame 后更轻 |
| PR-08 | 2.5-4 人日 | 2.5-4 人日 | 保持；P1-9 状态图补上后反而更省 |

**总量**：v2 总估 15-21 人日 → R2 复核为 **17-22 人日**。仍在 Phase 2→3 横跨区间内，单人开发 3-4 周可以吃掉。

---

## 6. 强烈建议立即补的小表格（总览）

若一次性吸收所有 P0 + P1，建议在 v2 文档中新增如下表格：

1. **§5.2 键位表** 增加 `L / X` 两行，修正 `F` 行（P0-1 / P2-1）
2. **§5.2a ModalFrame.kind 列表** 增加 `ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION`（P0-3）
3. **§5.2b 键位迁移决议表** 增加 `I / L 作方向键` 一行 + 废除歧义条款（P0-2）
4. **§7.3 `UiDesignTokens` 首发要求** 包含 `color.quality.*`、`color.telegraph.*`（P1-2 / P1-5）
5. **§9.3.1 InputHandler truth table**（P1-8）
6. **§10.3 QualityPresentation 映射表**（P1-2）
7. **§10.3 多件掉落排序规则**（P1-3）
8. **§14.3.1 CombatDecisionFrame phase 状态图**（P1-9）

这 8 个表格合起来 < 200 行文档体量，但会把"开发时乱猜"的概率压到接近 0。

---

## 7. 结论

### 7.1 总体

v2 相对 v1 已经是一个**合格的执行手册**。R1 提出的 P0 级结构性建议大部分都吸收了：

- ModalStack 归属表、键位迁移决议表、Look Mode 改造路径、CombatDecisionFrame 单 frame + phase 机、资源管线标准 —— 全部已进入文档正文。

### 7.2 仍建议先做的 P0 修正（按优先级）

1. **P0-1**：§5.2 键位表 `F` 行纠错（1 行改动，但影响所有 InputHandler 改造预期）
2. **P0-2**：§5.2b 补 `I / L 方向键` 废除行（防止 PR-03 第一天撞墙）
3. **P0-3**：§5.2a ModalFrame.kind 列表补 `ITEM_DETAIL / ITEM_COMPARE / COMBAT_DECISION`
4. **P0-4**：§5.2 `Tab` 行补脚注 + PR-03 增加 `PaneFocusController` task

### 7.3 强烈建议吸收的 P1（任何一个不补，PR-03 / PR-04 都会卡 1-2 小时）

- P1-2（QualityPresentation 映射表）
- P1-3（多件掉落排序规则）
- P1-8（InputHandler truth table）
- P1-9（CombatDecisionFrame 状态图）

### 7.4 单人开发前提下建议松绑

- P2-4（双语 golden 降为主语言）
- P1-10（< 100ms 改为"本机可感知流畅"）
- P2-6（加载态量化条件软化）

### 7.5 下一步

如果采纳上述建议，合理的落地方式是：

1. 直接在 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md` 的 §5 / §7 / §9 / §10 / §14 原地加表（约 8 段小表）；
2. 不用新开 v3 文档，在 v2 同文件内做 in-place 更新即可；
3. 更新完成后，PR-01 可以直接开始。

**一句话**：v2 已经从"设计说明书"升级成"执行手册"，只差 8 段小表就能从"执行手册"升级成"单元测试尺度的 checklist"。单人开发阶段，这 8 段小表是 ROI 最高的收尾投资。
