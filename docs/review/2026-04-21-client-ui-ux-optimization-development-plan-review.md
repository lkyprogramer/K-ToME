# K-ToME 客户端 UI/UX 执行计划 深度 review

**被 review 对象**：`docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`  
**review 版本**：v1（开发细节层）  
**前提条件**：长期只有 1 人开发 / 测试 / 游玩；日志 / telemetry / 行为统计可以后置  
**review 关注点**：开发规划的**可执行细度**、**PR 拆分的依赖正确性**、**退出条件是否可验证**、**是否还有"开发时必须乱猜"的盲区**

---

## 0. 本 review 的视角与排除项

本 review **不再重复讨论**以下内容，因为它们已在前置设计文档或 review v2 中收口：

1. 设计哲学选型（机制透明派为主 + 空间即 UI 为辅 + 反疲劳为底线 + 微交互打磨层）。
2. 是否"照搬深渊纪事"。
3. 状态/telegraph/关键词 contract 是否应新建第二家族（执行计划已正确选择"扩既有"）。
4. review v2 里已被吸收的 12 条设计共识。

本 review 只聚焦：

1. 与当前代码真源不符的**直接事实错误**（必须改，否则执行时按错误前提展开会立刻返工）。
2. 文档已经描述了**方向**但**没有解答的关键子问题**，在实施时必然"乱猜"的盲点。
3. **PR 间依赖图**是否正确，以及是否存在隐藏的跨 PR 硬前置。
4. **退出条件**能否作为 PR 验收门槛（而不是"看起来完成了"）。
5. 在"单人开发游玩"前提下**可以安全简化**的约束。
6. 工作量估时与实际代码体量的对齐度。
7. 文档仍然缺失、但**实施阶段需要查的子清单**（应该写进文档、而不是让开发时靠记忆）。

---

## 1. 与真源不符的事实错误（P0 必改）

以下是 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md` 中与当前仓库真源直接冲突的描述。执行计划如果按这些错误描述启动 PR，会立刻撞墙。

### 1.1 `PendingTelegraphState` 并不存在

文档位置：§0 快照 第 8 条、§3 冻结合同 第 9 条、§17.1 对策 第 2 条、§对应问题 第 4 条，共出现 ≥4 次。

实际真源：

1. `client` 侧只有 `com.ktome.client.telegraph.TelegraphRenderer`（**`internal object`**，无状态）与 `TelegraphStyle`。
2. `game` 侧有 `com.ktome.game.telegraph.TelegraphRegistry`。
3. 没有任何类或字段叫 `PendingTelegraphState`。
4. Telegraph 的数据源是 `RenderSnapshot.overlays: List<OverlayRenderSnapshot>`。

影响：

1. "优先扩现有 `PendingTelegraphState`" 的对策在代码里没有着力点。
2. PR-08 `telegraph 三位一体` 的扩展边界被这一错误描述污染。

建议：

1. 把所有 `PendingTelegraphState` 改成 `OverlayRenderSnapshot`（真源字段）与 `TelegraphRenderer`（现有消费者）。
2. 把"扩现有 telegraph contract"的具体含义改写为：以 `OverlayRenderSnapshot` 为唯一 typed fact，在 `TileRenderer / TelegraphRenderer` 内部按同一 overlay 渲染到地图、目标卡、日志三处，禁止为此新增第二 snapshot 家族。

### 1.2 `TelegraphRenderer` 是 `object`，不是 stateful 可"扩现有 class"的组件

文档位置：§3 第 9 条把 `TelegraphRenderer` 和 `PendingTelegraphState` 放在同一个"扩现有"语境里。

实际真源：

1. `TelegraphRenderer` 只提供 `tileRows / asciiLines / alpha / fallbackColorHex`，所有方法无副作用。
2. 颜色硬编码在 `fallbackColorHex`（`#7B1FA2 / #E53935 / #F6C445 / #3A86FF`）和 `TileTextTone` / `AsciiTextTone` 枚举中。

影响：

1. PR-01 说"新增 UI 代码不再写入裸 hex 颜色"，但现存 `TelegraphRenderer.fallbackColorHex` 恰好是一组裸 hex——PR-01 到底要不要把这里也迁到 token？文档没声明，开发时会乱猜。
2. 如果 PR-06（状态层级与 telegraph 权重）要调整危险级别颜色，必须同时改 `TelegraphRenderer.fallbackColorHex` 和 `tileTone/asciiTone`——这属于**PR-01 的 token 迁移范围内**，不能留到 PR-06。

建议：

1. 在 PR-01 退出条件里明确列出"`TelegraphRenderer.fallbackColorHex / tileTone / asciiTone` 必须改用 `UiDesignTokens` 派生"。
2. 在 PR-06 前置依赖里显式声明"依赖 PR-01 已把 telegraph 颜色迁到 token"。

### 1.3 `OverlayState` 不是"modal 栈"

文档位置：§0 第 2 条说"`InputHandler` 已有 overlay mode 状态机，说明 modal / inspect / targeting 不是临时调试面"。

实际真源：

1. `OverlayState` 是扁平的 data class，字段包含 `mode: UiMode` 与各子 mode 的 selection 游标。
2. `InputHandler.reconcileMode(snapshot)` 是**从 snapshot 派生当前 mode 的函数**：
   - `snapshot.uiState.activeRouteSelection != null` → 强制进 `WORLD_MAP`
   - `snapshot.uiState.activeShop != null` → 强制进 `SHOP`
   - `hasPendingStatAllocation(snapshot)` → 强制进 `STAT_ASSIGN`
3. 没有 `push / pop` 语义，没有 `previousMode` 字段，关闭 `INVENTORY / LOADOUT_EDIT` 时当前实现是"重置 `mode = MAP`"而不是"返回上一层"。
4. `UiMode` 是**并列枚举**：`MAP / SHOP / WORLD_MAP / INVENTORY / LOADOUT_EDIT / TARGETING / INSPECT / VALIDATION / STAT_ASSIGN / TALENT_ASSIGN`，共 10 个。

影响：

1. PR-03 的 "引入 `ModalStack`" 不是"把现有 overlay mode 套个壳"，而是一次**结构性改造**：需要区分"被动 mode"（snapshot 驱动）与"主动 mode"（按键驱动、允许进栈）。
2. 文档完全没有给出"哪些 UiMode 可以进栈、哪些不能进栈"的划分。开发时必须乱猜。
3. 当前 `reconcileMode` 和 `ModalStack` 并存策略没定义——如果 `activeShop != null` 时玩家正好在 `INVENTORY`，应该怎么解决？文档没说。

建议：见本 review §2.1。

### 1.4 `UiMode` 实际枚举比文档 §5.2 输入语义表覆盖的多

文档位置：§5.2 输入语义表，列出 `地图态 / modal/drawer 态 / targeting/三层决策态` 三列。

实际真源：现存 10 个 `UiMode`：`MAP / SHOP / WORLD_MAP / INVENTORY / LOADOUT_EDIT / TARGETING / INSPECT / VALIDATION / STAT_ASSIGN / TALENT_ASSIGN`。

文档语义表的"modal/drawer"列默认代表 `INVENTORY / SHOP`，但没讲：

1. `WORLD_MAP`（路线选择）—— 是 modal 还是"被动 mode"？
2. `LOADOUT_EDIT`（当前按 `L` 打开，按 `L / F / ESC` 关闭）—— `ESC` 在这里归哪一列？
3. `STAT_ASSIGN`（属性分配）—— `ESC` 会直接退出到 `MAP` 吗？是否允许玩家直接退出未完成的属性分配？
4. `TALENT_ASSIGN`（天赋分配）—— 同上。
5. `VALIDATION`（校验模式）—— 本身就不是玩家正式面，`ESC` 应该怎么处理？

影响：

1. PR-03 冻结输入语义时，如果语义表不覆盖这 5 个现有 mode，开发者要么"不动它们"（矛盾：文档说"一旦进入实现，就视为正式输入约定"），要么"按自己理解补"（矛盾：就是乱猜）。

建议：把 §5.2 语义表扩到 `UiMode × 核心键位` 的 10 × 11 完整表（或至少把上述 5 个 mode 明确归类到一列、或标注"不纳入本次冻结"）。

### 1.5 `MainMenuController` 初始焦点固定为 0，`hasSave` 从未进入 controller 构造

文档位置：§8.3 PR-02 任务拆解第 1 条"首启优先快速开始；有存档优先继续游戏"。

实际真源：

1. `MainMenuController.selectedIndex: Int = 0`（恒为 0，不受 `hasSave` 影响）。
2. `MainMenuScreen` 构造参数有 `continueEnabled: Boolean`，但只传给 `controller.pollAction(continueEnabled)` 和 `controller.entries(continueEnabled)`，**未传入 controller 构造器**。
3. 菜单项顺序固定为 `new_game / continue / validation_mode / exit`，`continue` 永远在 `selectedIndex = 1` 位置。

影响：

1. PR-02 "有存档优先继续游戏" 的实现需要在 controller 构造或 pollAction 前做初始焦点切换，**文档没声明这个接线改动**。
2. 如果改 `MainMenuController` 构造签名，会影响 `MainMenuControllerTest`——文档没在 §8.2 范围里列出测试文件。

建议：

1. 在 PR-02 §8.2 范围里追加 `client/src/test/kotlin/com/ktome/client/screen/MainMenuControllerTest.kt`。
2. 在 §8.3 拆解第 1 条末尾加一句："controller 构造新增 `initialFocusHasSave: Boolean` 或等价入参，PR 必须同时更新 `MainMenuScreen` 与 controller test。"

### 1.6 `ItemRenderSnapshot.iconKey` 仍然是 `nullable`

文档位置：§3 第 5 条"所有可装备物与 special template item 都必须能解析到正式 `iconKey`"。

实际真源：`ItemRenderSnapshot.iconKey: String? = null`（第 450 行）。

影响：

1. PR-04 退出条件第 1 条要求 `contract-lint` 覆盖可装备物 / special template item 的 `iconKey` 完整性，但 lint 存在**两种等价选择**：
   - 在 lint 层面补，保持字段 nullable（向后兼容）。
   - 改为 `iconKey: String`（强 nonnull），影响 schema 和所有 snapshot 构造点。
2. 文档没声明选哪条，开发时会乱猜，而这个选择会影响所有 `SessionSnapshotMapper` / content 构造逻辑。

建议：在 PR-04 §10.3 明确"本 PR 选择 lint 路径（保留 nullable）/ schema 路径（改 nonnull）"，并列出对 `SessionSnapshotMapper` 的影响面。

### 1.7 `StatusEffectRenderSnapshot` 已是 snapshot 正式字段

文档位置：§3 第 8 条、第 9 条把 keyword / telegraph 作为"优先扩现有"，但没把同等优先级给到 `StatusEffectRenderSnapshot`。

实际真源：

1. `StatusEffectRenderSnapshot(typeId, remainingTurns, nameKey?, iconKey?, stackCount, stackCap?, category)` 已完整。
2. `StatusEffectCategorySnapshot = BUFF / DEBUFF / NEUTRAL`，文档 §12.3 第 3 条说"状态继续复用现有 BUFF / DEBUFF / NEUTRAL"——这点文档是对的。
3. 但 `ZONE_EFFECT / TELEGRAPH` 作为 presentation group 需要**客户端自推**，推导来源未定义——到底从 `OverlayRenderSnapshot.shape / dangerLevel / sourceAbilityId` 哪个字段推，文档没说。

影响：PR-06 的 "`ZONE_EFFECT` 与 `TELEGRAPH` 优先作为 presentation group" 在实施时会因为派生来源不确定而乱猜。

建议：在 §12.3 第 3 条补一句"presentation group 的派生规则：`TELEGRAPH` = `OverlayRenderSnapshot.dangerLevel >= 3`；`ZONE_EFFECT` = `TerrainOverrideRenderSnapshot != null`；两者不相互排斥。"

---

## 2. 使开发必然"乱猜"的规划盲点（P0 必须补）

这一节列出的每一项都对应文档某条"做什么"描述，但缺了**怎么做的关键子问题**。单人开发前提下，每一项乱猜都会直接返工。

### 2.1 `ModalStack` 与现有 `reconcileMode(snapshot)` 派生状态的关系未定义

背景：§1.3 已说明 `reconcileMode` 是派生函数，`ModalStack` 是新引入概念。

未回答的子问题：

1. 栈元素是什么？是 `UiMode`？是 `UiMode + 子状态（inventorySelection / routeSelection ...）`？还是新的 `ModalFrame` 类？
2. 哪些 `UiMode` 可进栈？建议区分：
   - **被动 mode**（snapshot 强制派生）：`WORLD_MAP / SHOP / STAT_ASSIGN`——不进栈，由 `reconcileMode` 直接覆写栈顶。
   - **主动 mode**（按键显式推入）：`INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT / TARGETING`——入栈。
   - **根 mode**：`MAP`——永远是栈底，不可 pop。
   - **特殊 mode**：`VALIDATION`——开发态专用，应该单独说明是否进栈。
3. 栈与 `activeShop / activeRouteSelection` 的优先级：如果玩家按 `i` 进入 `INVENTORY` 后，`snapshot.activeShop` 非空了怎么办？当前 `reconcileMode` 会强制进 `SHOP`，栈状态如何保持一致？
4. 深度 3 上限如何"压平"？是 drop 最底层还是 drop 中间层？例子：`INVENTORY → ItemDetail → Compare`——若再按任意键想开第 4 层，UX 行为是什么？
5. 栈状态是 **client-local 持久** 还是仍从 snapshot 派生？`ModalStack` 是 `ui-token` 还是"业务 mode" 的一部分？

建议补充章节：新增 `§5.2a ModalStack 与 UiMode 迁移表`，给出上述 5 个问题的显式答案。

### 2.2 统一输入语义表漏了 5 个现有 mode（见 §1.4）

补充建议：把 §5.2 改写成完整 10 × 11 表格：

| UiMode | ESC | Backspace | Tab | ? | i | x | Enter | F/. | 1-9 | Ctrl+S |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| MAP | 打开根帮助 | — | 焦点区切换 | 帮助 | 背包 | 检视 | 交互 | 等待 | hotbar | 保存 |
| SHOP | 关闭 Shop 回上一层 | 回上一层 | 焦点区切换 | 帮助 | 快捷关闭（保留旧行为） | — | 购买/出售 | — | 快速选 | 保存 |
| WORLD_MAP | 保留在 WORLD_MAP（被动强制） | — | 焦点切换 | 帮助 | — | — | 选择路线 | — | 直达路线 | 保存 |
| INVENTORY | 关闭回 MAP | 回上一层 | 焦点切换 | 帮助 | 关闭（对称 i） | — | 使用/装备 | — | 快速选 | 保存 |
| LOADOUT_EDIT | 关闭回 MAP | 回上一层 | 焦点切换 | 帮助 | — | — | 确认 | — | 快速选 slot | 保存 |
| TALENT_ASSIGN | 关闭回 MAP（需确认放弃） | 回上一层 | active/reserve 切换 | 帮助 | — | — | 确认分配 | — | 快速选 | 保存 |
| STAT_ASSIGN | 禁用 ESC（强制完成分配） | — | — | 帮助 | — | — | 确认 | — | 加点 | 保存 |
| INSPECT | 退出到 MAP | 退出到 MAP | — | 帮助 | — | 退出（对称 x） | 详情 | — | — | — |
| TARGETING | 取消（回 MAP） | 回上一层 | 候选目标切换 | 帮助 | — | — | 确认 | — | 目标编号 | — |
| VALIDATION | 退出 VALIDATION | — | 面板切换 | 帮助 | — | — | 确认 | — | — | — |

注：上表只是示范，真正内容用户自己定；但**必须写出来**才能冻结。

### 2.3 基础 Look Mode 与 `UiMode.INSPECT` 的"重构还是并存"没声明

背景：文档 §9.3 第 4 条说"把当前 `Inspect` 升级为基础 Look Mode"，但没说是**改现有 `UiMode.INSPECT` 的行为**，还是**新增 `UiMode.LOOK`**。

影响：

1. 如果改现有 INSPECT 行为：`InputHandler.pollInspectCommand` 内部重写，不改 `UiMode` 枚举，`OverlayState.inspectCursor` 继续复用。
2. 如果新增 LOOK：`UiMode` 枚举新增成员，`OverlayState` 需要 `lookCursor` 字段，`TileRenderModel` 需要同步消费。
3. 两种做法 blast radius 差 5 倍以上。

建议：PR-03 §9.3 第 4 条补充"选项：改现有 INSPECT（首选，blast 小）；理由：现有 `inspectCursor / defaultInspectCursor / pollInspectCommand` 已经做了 90% 的光标移动逻辑，只需加'世界面 / 目标卡 / 检视面同步当前光标'。"

### 2.4 `ESC / Backspace / Tab / ?` 与现有 `F / I / L / X` 的迁移冲突没定义

现有真源里的冲突键：

1. `F`：`overlayCloseBindings = listOf(Keys.F)`——**所有 overlay** 的通用关闭键。
2. `I`：MAP 态打开 `INVENTORY`；SHOP 态关闭 Shop（`isOverlayCloseBinding() || Keys.I`）。
3. `L`：MAP 态打开 `LOADOUT_EDIT`；LOADOUT_EDIT 态关闭；MAIN_MENU 态切换 locale。
4. `X`：MAP 态进 `INSPECT`；SHOP BUY 态下移；SHOP SELL 态下移；WORLD_MAP 态下移。

PR-03 §5.2 要把 `ESC` 升为"全退"、`Backspace` 升为"退一层"后，必须回答：

1. `F` 还承担 overlay 关闭职责吗？还是完全让位给 `ESC`？
2. `I` 在 SHOP 里还是"快捷关闭"吗？（建议保留，但需要在迁移表里显式列出。）
3. `L` 在 LOADOUT_EDIT 里是"再按 L 关闭"还是只能用 `ESC`？
4. `X` 在 SHOP / WORLD_MAP 里继续做下移还是让位给方向键？

当前文档**完全未触及这层迁移**，是 PR-03 实施时的重大盲区。

建议：新增 §5.2b `键位迁移决议表`，对每个现有冲突键给出三列决议：`保留 / 废弃 / 共存（兼容多义）`。

### 2.5 "战斗三层决策面" 到底是不是 ModalStack 的栈层？

背景：PR-08 §14.3 第 2 条说"新增战斗三层决策面：动作层 / 方式层 / 目标层"。

未回答：

1. 这三层是 `ModalStack` 的 3 个连续栈帧（`ActionFrame → StyleFrame → TargetFrame`）吗？
2. 还是一个单一 `CombatDecisionFrame` 内部的 3 个 `sub-phase`？
3. 如果是前者，`ModalStack` 深度 3 的限制刚好吃满，`INVENTORY` 等都不能嵌套——但 `INVENTORY → ItemDetail → Compare` 已经占用了 3 层预算，是否需要把战斗决策独立成一个栈轨道？
4. 如果是后者，与 §5.2 "modal 栈深度最大为 3" 的约束是独立的。

这两种设计实现 blast radius 差一个数量级。

建议：PR-08 §14.3 第 2 条明确"三层决策面是 `ModalStack` 上的 3 个连续帧 / 是单一帧的内部阶段机"。如果是前者，`ModalStack` 需要扩为 `maxCombatDepth = 4`（考虑同时允许玩家按 `?` 打开帮助）；如果是后者，`CombatDecisionPanel` 内部管理 3 sub-phase。

### 2.6 普通敌人 intent 的"已有 typed fact 在哪里"没指明

背景：PR-08 §14.3 第 5 条"如果已有 typed fact，就消费；不把先造一整套 AI plan snapshot 当成阻塞项"。

问题：

1. 当前 `RenderSnapshot.actors[i].aiTypeId: String?` 是规则层 AI 类型 id，不是"下一步意图"。
2. `OverlayRenderSnapshot` 只承载 scripted / boss telegraph，不承载普通怪的单步 intent。
3. 既然"已有 typed fact" 并不包含普通敌人 intent，§14.3 第 5 条等于"当前不做普通敌人 intent"，但文档用"如果有就消费"的措辞，让实施时会去翻 snapshot 找不存在的字段。

建议：把 §14.3 第 5 条改写为"本 PR 不消费普通敌人 intent；若未来规则层暴露单步 intent 字段（例如扩 `ActorRenderSnapshot.nextIntent`），再由独立 PR 接入。"—— 明确拒绝、而不是"条件性消费"。

### 2.7 地图掉落 Z-order：站在掉落格时如何"不被角色完全覆盖"

背景：PR-04 §10.3 第 6 条"如果玩家站在掉落格上，掉落提示仍必须可见"。

未回答：

1. 是把 item icon 画在角色图标侧边（偏移 offset X/Y）？
2. 还是让角色半透明，露出 item icon？
3. 还是在掉落格上加一个 tilted badge / corner marker？
4. 走 `TileRenderer` 里的哪一层绘制？当前渲染顺序是 terrain → overlay → actor → ??，item 的层级还没定义。

建议：§10.3 第 6 条补充具体方案，例如"item icon 绘制于 actor 图层下，通过 corner badge（右下 12×12 px）叠加在 actor 左下角；当 `items.size >= 2` 时 badge 显示 `N+`"。

### 2.8 `QualityPresentation` 中 `UNIQUE / ARTIFACT` 的视觉区分轴未定义

背景：PR-04 §10.3 第 2、3 条"建立 `QualityPresentation`：`NORMAL / MAGIC / RARE / UNIQUE / ARTIFACT`；品质色只作用于名称、边角标、对比摘要"。

未回答：

1. `NORMAL / MAGIC / RARE` 是 `RarityTier` 维度，`UNIQUE / ARTIFACT` 是 `SpecialTier` 维度——两个是**正交**的。如果一件 UNIQUE 装备同时是 RARE 底，UI 是显示哪个色？
2. 边角标是 `RarityTier + SpecialTier` 双重叠加，还是只取高优先级（`SpecialTier` 覆盖 `RarityTier`）？
3. 颜色建议（业界参照）：`NORMAL #C8C8C8 / MAGIC #4B6BE1 / RARE #E1C34B / UNIQUE #A05BFF / ARTIFACT #FF7A00`，但文档没给色值也没说"留空由 PR-01 token 决定"。

建议：§10.3 第 2 条补充具体映射表：

```
RarityTier.NORMAL  → nameColor=tokens.quality.normal  cornerGlyph=none
RarityTier.MAGIC   → nameColor=tokens.quality.magic   cornerGlyph=diamond
RarityTier.RARE    → nameColor=tokens.quality.rare    cornerGlyph=star
SpecialTier.UNIQUE   → 叠加 nameOutline=tokens.quality.unique
SpecialTier.ARTIFACT → 叠加 nameOutline=tokens.quality.artifact + glow
```

### 2.9 `content-ui-lint` 的抽象层次未定义

背景：PR-05 §11.3 第 4 条"新增 `content-ui-lint` 或等价约束"。

未回答：

1. lint 是独立 Gradle task（`contentUiLint`），还是扩已有 `contractLint / localeLint` 的子规则？
2. 覆盖面：新增 content（event / shop / reward）必须有 `iconKey`，UI 文案必须走 locale，卡片字段必填——这些**已有 lint** 是否已经覆盖？
3. 真源里 `contractLint / localeLint` 的任务注册在 `build.gradle.kts` 第 274-289 行，扩之的代价 vs 新建 lint 的代价需要比较。

建议：PR-05 §11.3 第 4 条改写为"首选扩 `contractLint`，新增 content-ui 子规则 `ContentUiLintCheck`；仅当规则需要跨模块 context（例：event → sharedModalCard）无法在 `tools/lint` 内表达时才新建 `contentUiLint` task。本 PR 落地时必须先确认 tools/lint 现有能力边界。"

### 2.10 golden 按 PR 切目录的命名格式没定义

背景：§16.2 第 1 条"golden 按 PR 切目录"，§2.1 第 2 条"golden 重拍范围失控"。

未回答：

1. 目录命名：`client/src/test/resources/golden/pr01/` 还是 `client/src/test/resources/golden/phase2-w5.0/` 还是 `client/src/test/resources/golden/phase2-uiux-pr01/`？
2. 现存 golden 是否要迁移？当前真源下 `client/src/test/resources/golden/` 的结构是什么？
3. 双语覆盖（§16.2 第 3 条）在 8 个 PR 下意味着 8 × 2 = 16 套 golden baseline——单人开发成本高（见 §5.1）。

建议：§16.2 明确"目录命名格式：`client/src/test/resources/golden/uiux-prNN/{locale}/*.png`"；同时允许在单人开发阶段只维护 `zh-CN` 一套，`en-US` 只在 PR 合并前做一次抽检（参见本 review §5.1）。

### 2.11 `ModalCardModel` 的字段集没定义

背景：PR-05 §11.2 第 4 条新增 `ui/card/ModalCardModel.kt`。

未回答：

1. `ModalCardModel` 覆盖 `event / shop / reward` 三种场景，其字段是否统一？
2. 必填字段建议：`titleKey / iconKey? / bodyTokens: List<RenderTextTokenSnapshot> / primaryActionKey? / secondaryActionKey?`——文档没给出。
3. 与 `ShopPanelSnapshot / ShopOfferSnapshot / RewardPresentationEntrySnapshot` 的派生关系没定义。

建议：§11.3 第 1 条补充 `ModalCardModel` 的字段建议，并说明"本模型只属于 client 表现层，通过 `fromShopOffer / fromRewardEntry / fromEventSnapshot` 工厂函数从正式 snapshot 派生"。

### 2.12 `InfoSurfaceLayout` 的具体形态没定义

背景：PR-01 §7.3 第 3 条"提供 `MapDominant / WideSplit / ModalOverlay` 三种布局骨架"。

未回答：

1. 这三种是**互斥枚举**，还是可以叠加？
2. `WideSplit` 的分栏比例 / 断点由谁决定？
3. 现有 `TileLayoutMetrics(mapOffsetY, worldWidth, sidebarX, sidebarWidth, bottomInset, panelGap, cardY, cardHeight, infoX, infoWidth, logX, logWidth, focusX, focusWidth, hotbarX, hotbarY, hotbarCardWidth, hotbarCardHeight, hotbarGap)` 已经是一个完整的 layout data class——`InfoSurfaceLayout` 是替代它、包装它、还是并存？
4. 如果替代，blast radius 大（TileRenderer 全量调用点要改）；如果包装，又多了一层抽象。

建议：PR-01 §7.3 第 3 条改写为"`InfoSurfaceLayout` 是 `TileLayoutMetrics` 的上游 strategy 接口，当前只提供 `MapDominant` 一种实现（等价当前布局）；`WideSplit / ModalOverlay` 只保留 sealed interface 的空实现，放到 PR-03 / PR-05 落地。"

### 2.13 错误态"Open Logs" 在单人环境意味什么

背景：§5.4 第 4 条"错误态必须给出返回路径：Retry / Back To Menu / Open Logs"。

单人开发前提下：

1. "Open Logs" 的目标受众就是开发者本人，相当于"打开 stdout tail / 打开 `gradle run` 日志窗口"。
2. 在 libgdx desktop 下没有"Open Logs" 按钮的直观等价物（不会调用系统 `logs.app`）。
3. 可能的落地：按钮只弹一个 modal 显示最近 N 条 `RenderLogEventSnapshot`？或者提示"请查看 `~/Library/Logs/K-ToME/` 下的日志文件"？

建议：在单人开发前提下，把"Open Logs"软化为"Copy Error Detail To Clipboard"（把 stack trace + 最近 10 条 log event 复制到剪贴板），这比"打开日志路径"更实用。

---

## 3. PR 间前置依赖图的修订

文档 §2.2 只说"Phase 2 基座 先于 Phase 3"，§6 PR 拆分总览说"当前 PR 未完成前不并行推进下一 PR"。但**没画跨 PR 的硬前置点**。下面列出容易遗漏的依赖：

### 3.1 PR-06 / PR-08 强依赖 PR-01 的 token

原因：

1. PR-06 状态 badge / telegraph 视觉分层必须用 token，否则 PR-06 里又会新增一批硬编码色。
2. PR-08 战斗三层决策面的 action/style/target affordance 颜色也必须在 token 体系里。
3. `TelegraphRenderer.fallbackColorHex` 的迁移属于 PR-01 范畴（见 §1.2）。

修订建议：PR-06 / PR-08 的 §前置 明示"依赖 PR-01 token 已落地"，并在退出条件里加一条"本 PR 未引入任何新的裸 hex 色"。

### 3.2 PR-08 强依赖 PR-03 的 ModalStack

原因（见 §2.5）：

1. 战斗三层决策面若作为 `ModalStack` 的连续栈帧，PR-03 的 `ModalStack` 能力必须先到位。
2. 若作为单一 frame 内部阶段机，仍然依赖 PR-03 的焦点恢复规则。

修订建议：PR-08 §前置 明示"依赖 PR-03 ModalStack 与 ESC / Backspace 语义已冻结"。

### 3.3 PR-04 与 PR-03 的 Look Mode Z-order 未对齐

原因（见 §2.7）：

1. PR-04 新增的地面掉落 marker 必须被 PR-03 的 Look Mode 光标正确叠加。
2. Look Mode 光标在掉落格上时，目标卡应该显示"掉落物"而非"角色"——两边都要改 `TileRenderModel`。

修订建议：PR-04 §前置 明示"依赖 PR-03 Look Mode 基础版已能把光标位置写入 `OverlayState.inspectCursor`"；若 PR-03 先于 PR-04 完成，Look Mode 不必等 marker 渲染完就能上线。

### 3.4 PR-07 动态说明依赖 PR-03 的 ModalStack

原因：

1. 解释型检视面板作为 modal 打开后，应该能按 `Backspace` 回到上一层而不是直接退到 MAP。
2. 关键词 chip 点击后打开 "keyword detail modal" 也依赖 ModalStack。

修订建议：PR-07 §前置 明示依赖 PR-03。

### 3.5 修订后的 PR 依赖图

```
PR-01 (token + InfoSurfaceLayout)
  └─> PR-02 (首页)
  └─> PR-03 (ModalStack + 输入语义 + Look Mode)
       ├─> PR-04 (iconKey + 品质 + 地面掉落)
       ├─> PR-06 (状态 + telegraph 视觉权重)     [还依赖 PR-01 token]
       ├─> PR-07 (动态说明 + 解释型检视)
       └─> PR-08 (telegraph 三位一体 + 战斗三层) [还依赖 PR-01 token, PR-04 iconKey]
  └─> PR-05 (内容扩张卡片 + 错误/空态)            [还依赖 PR-04 iconKey 覆盖]
```

**关键路径**：`PR-01 → PR-03 → PR-08`。这三个 PR 是整条执行链的骨架，任何延期都会放大下游返工成本。建议用户把 PR-03 优先级**提高到与 PR-01 并列的关键 PR**（文档 §6 固定规则第 3 条只说"PR-01 ~ PR-04 是 Phase 2 地基链"，没区分关键性）。

---

## 4. 每个 PR 的退出条件从"定性"升级到"可验证"

文档每个 PR 的 §X.4 退出条件都是**定性描述**，例如"design token 与布局骨架冻结""首焦点路径固定且可 smoke"。单人开发环境下定性验收很容易自欺，**必须量化到"写得出 assert" 的程度**。下面给出每个 PR 的量化建议。

### 4.1 PR-01

现有退出条件：`TileRenderer / StatusHudRenderer / MainMenu 至少 3 个面开始消费 token`。

问题："至少 3 个面"含糊；"消费 token"无断言。

建议量化：

1. `UiDesignTokens` 至少包含：8 色（前景/背景/三档对比）、5 字号档（micro/small/body/title/hero）、4 间距档（xs/s/m/l）、3 动效档（instant/short/long）、2 圆角档（none/default）、2 描边档（thin/thick）。
2. `TileRenderer` 中 `color = Color(...)`、`font.color = Color.XXX` 的直接调用点全部替换为 `tokens.xxx`；grep 断言 `:client:main` 下 `Color\.(GOLD|RED|CYAN|MAGENTA|LIGHT_GRAY|GRAY|DARK_GRAY|SALMON)` 命中数为 **0**（或明确列出豁免名单）。
3. `TelegraphRenderer.fallbackColorHex` 迁移到 `tokens.telegraph.{lethal, high, moderate, low}`。
4. `MainMenuScreen.Color.GOLD / LIGHT_GRAY / GRAY / SALMON / CYAN` 全量迁移。
5. 窗口标题 grep 断言 `Gdx.app.graphics.setTitle` / `Lwjgl3ApplicationConfiguration.setTitle` 不包含 `操作说明 / ESC / F / Ctrl+`。

### 4.2 PR-02

现有退出条件："首焦点路径固定且可 smoke"。

量化：

1. `MainMenuControllerTest` 新增 case：`init(hasSave=true)` 后 `selectedIndex == 1`；`init(hasSave=false)` 后 `selectedIndex == 0`。
2. `MainMenuScreenTextTest` 新增 case：首屏至少显示 `快速开始 / 继续游戏 / 当前 locale / 关键键位` 四个文本 key。
3. `ClientSmokeHarnessTest` 新增 case：`hasSave=true` 时按 Enter 直接进入 continueGame 路径；`hasSave=false` 时按 Enter 进入 startNewGame 路径。

### 4.3 PR-03

现有退出条件：`ESC / Backspace 不冲突；modal 关闭可以回原地图光标；Tab 焦点链不越界；基础 Look Mode 可自由移动；LogPresentationModel 具备分类/重要度/空态/回退`。

量化：

1. `InputHandlerTest` 新增 case：`INVENTORY → Inspect (非官方子 modal, 若引入) → ESC` 回 `MAP`；`INVENTORY → Backspace` 回 `MAP`（因为当前栈深为 1，退一层等于退出）。
2. 栈深测试：连续推入 `INVENTORY → Item Detail → Compare` 后按 `Backspace` 逐层回退；按 `ESC` 直接回 `MAP`。
3. 光标恢复测试：`MAP(cursor=(5,3)) → INVENTORY → ESC → MAP`，断言 cursor 仍为 `(5,3)`。
4. `UiMode.INSPECT` 基础 Look Mode 测试：光标移动时 `OverlayState.inspectCursor` 同步变化，`TargetCardModel` / `InspectPanelModel` 的 render input 随光标切换。
5. 空态：`ClientSmokeHarnessTest` 新增 `inventory 为空时的 UI 文案是本地化 key 'ui.inventory.empty'`。

### 4.4 PR-04

现有退出条件：`contract-lint 覆盖 iconKey 完整性；品质档位可稳定区分；地图不再只靠 sidebar 看到掉落；区分同格多件与 stack`。

量化：

1. `contractLint` 新增规则 `ItemIconKeyRequiredCheck`：遍历 `game/src/main/resources/data/items/` 与 `game/src/main/resources/data/special/`，断言所有条目的 `iconKey != null`；预期通过阈值 100%。
2. `QualityPresentation` 单元测试：`NORMAL / MAGIC / RARE / UNIQUE / ARTIFACT × {nameColor, cornerGlyph, outline}` 的 5 × 3 = 15 断言。
3. `TileRendererCanvasTest` 新增 case：`MapCell(items=[item1])` 渲染 item icon；`MapCell(items=[item1, item2, ..., item10])` 渲染带 `9+` badge；`MapCell(items=[item1], actorAtSamePos=true)` 渲染角色 + 掉落 corner badge。
4. grep 断言 `client` 下不存在 `qualityTierId == "LEGENDARY" / "MYTHIC" / "SET"`；若存在则 lint fail。

### 4.5 PR-05

量化：

1. `contentUiLint`（或扩 `contractLint`）新增 3 条 check：
   - `EventCardFieldCheck`：event content 必有 `titleKey / bodyKey`。
   - `ShopOfferIconCheck`：每个 `ShopOfferSnapshot` 渲染时能解析到 `iconKey`。
   - `RewardEntryLocalizationCheck`：`RewardPresentationEntrySnapshot.itemDisplayName` 的 `key` 在 locale 文件里能找到。
2. 空态测试：背包/商店/日志空时分别显示 `ui.inventory.empty / ui.shop.empty / ui.log.empty`；不接受 `"Empty"` 硬编码。
3. 错误态测试：`ClientSmokeHarnessTest` 新增 manifest 版本错配场景，断言 UI 显示 `ui.error.manifest_mismatch` 且提供 `Retry / Back` 两个按钮。

### 4.6 PR-06

量化：

1. `StatusHudRendererTest`（若不存在则新增）：`BUFF / DEBUFF / NEUTRAL × {iconKey, badge, category color}` 断言。
2. `TelegraphRenderer` 测试：`dangerLevel ∈ [1, 4]` × `zh-CN / en-US` × `tileRows / asciiLines` 的断言网格。
3. presentation group 派生规则测试：`OverlayRenderSnapshot(dangerLevel=4)` 归 `TELEGRAPH`；`MapCellSnapshot(terrainOverride != null)` 归 `ZONE_EFFECT`。
4. 高对比度 / 色盲回退：tokens 层提供 `tokens.dangerLevel.high.shape = triangle`（除颜色外的形状区分），断言在 `tokens.palette.colorblind` 模式下 telegraph icon 的形状标识不变。

### 4.7 PR-07

量化：

1. `DescriptionPresenterTest` 新增 case：keyword 在 talent desc / inventory desc / shop desc 三个消费面都能 parse 成 keyword chip。
2. `KeywordRegistry` 容量断言：所有 keyword id 在至少一个真源面板能触发渲染。
3. 不新增第二 KeywordDictionary：grep 断言 `class KeywordDictionary` / `object KeywordDictionary` 不存在。

### 4.8 PR-08

量化：

1. `ClientSmokeHarnessTest` 新增 case：打开一个高 danger overlay 后，地图 overlay / 目标卡 / log 三处的 `dangerLevel` 匹配、`sourceAbilityId` 匹配、`warningMessage.key` 匹配。
2. 战斗三层决策：`UseTalent(requiresTarget=true)` 流程：按 `Enter` → 进入 Action 层；按 `Tab` 在 `Style` 选项间切换（若适用）；按方向键选目标；按 `Enter` 确认；断言最终 `PlayerCommand.UseTalent(slot, target=(x,y))` 被提交。
3. `ESC` 在战斗三层决策面各阶段的行为：Action 层 ESC = 回 MAP；Style 层 ESC = 回 MAP（或回 Action 层，需明确）；Target 层 ESC = 回 MAP。**这个选择 §2.5 还没解决，必须在 PR-08 开工前定下来。**

---

## 5. 基于"单人开发测试游玩"前提可以简化的条目

文档整体没有区分"对外发布期约束" vs "单人开发期约束"。单人前提下以下条目可以软化：

### 5.1 golden 双语覆盖可以降为主语言 + 抽检

文档 §16.2 第 3 条"双语至少覆盖 zh-CN / en-US"。

单人开发下：

1. 用户自己用 zh-CN 游玩，en-US 只是保留 key 可用。
2. 8 个 PR × 2 语言 = 16 套 golden baseline，每次改 UI 都要重拍，成本高。

建议：

1. 主 golden 只维护 `zh-CN`。
2. `en-US` 只在每个 PR 合并前跑一次 `localeLint + 抽样 smoke`（不生成 golden png），确认 key 未缺失、排版不破。
3. 等到真正有"第二名玩家"或外部测试时再补 `en-US` 全量 golden。

### 5.2 `content-ui-lint` 可以先始终保持 warning

文档 §11.6 最小切片"先落 PR checklist；再把 lint 从 warning 升为 error"。

单人开发下：开发者本人就是 lint 的唯一接收者，warning → error 的 gate 意义不大（自己不会无视自己的 warning）。

建议：lint 保持 warning，只在 `./gradlew verify` 的 summary 里列出。真正升 error 的时机可以推到首次外部合作前。

### 5.3 性能基线 P95 < 16.6ms 降为"本机可感知流畅"

文档 §5.5 性能基线。

单人开发下：没有 CI 多机型性能 gate，所谓 P95 预算只有开发者本机的"感受"。

建议：

1. 性能基线从"P95 < 16.6ms 硬阈值"改为"开发者本机在 `density=max + boss 战` 场景下无明显掉帧"。
2. 每个 UI-affecting PR 合并前**手动**跑一次 boss 战 + 满 overlay 场景，记录主观判断即可。
3. 真正的 P95 监控留到有第二位玩家/真实玩家时再上。

### 5.4 a11y 某些条目可以降级

文档 §5.5 a11y 基线：`键盘路径完整 / 焦点可见 / 高对比度色盲回退 / 字号档位 / Reduce Motion`。

单人开发下，按"开发者本人是否需要"分类：

1. **必须保留**（对开发者本人也有价值）：键盘路径完整、焦点可见、字号档位（开发者会调节自己的屏幕 DPI）、Reduce Motion。
2. **可以降级为 non-blocking**：高对比度、色盲回退。只有开发者本人确实有色盲需求时才保留为 blocking。

建议：§5.5 第 3 条"高对比度 / 色盲回退"改为"作为 token-ready feature 实现，但不作为 PR 退出条件的 blocking 项；当开发者或第二位玩家实际需要时再激活。"

### 5.5 telemetry / 行为统计继续明确后置

文档 §3 第 12 条、§4.2 第 5 条、§15 都已明确后置 —— **这点保留现状**。

补充建议：把"后置"写得更具体：

1. 当前只保留 `ClientSmokeHarnessTest / GoldenScreenshotHarnessTest / RenderSnapshotAssetAudit` 产生的 fail-fast artifact。
2. 不新增 `UxEvent / MetricCollector / TelemetryPipeline` 类型。
3. 现有 `docs/phase4/*` 的统计收集规划不触发本 UI/UX 计划的 PR。

### 5.6 资源 plan 命名约束可以软化

文档 §5.6 要求每个触发资源的 PR 都按 `phase?-uiux-prNN-gemini-plan.yaml` 命名。

单人开发下：

1. 命名约定的价值是**未来自己回看能找到**——这个价值对单人也成立。
2. 但"每个 PR 只要碰资源就强制新增 plan" 的约束过严；多个 PR 共享同一 plan（例：PR-04 + PR-05 共用一个 icon companion plan）在单人环境下是合理的。

建议：§5.6 第 1 条补充"允许相邻 PR 共享同一 plan（如 `phase2-uiux-pr04-05-gemini-plan.yaml`），只要命名显式声明覆盖范围。"

---

## 6. 工作量重估

文档 §6 估时：

| PR | 文档估时 | review 建议估时 | 差异说明 |
| --- | --- | --- | --- |
| PR-01 | 1.5 人日 | 1.5 ~ 2 人日 | 若包含 `TelegraphRenderer.fallbackColorHex` 迁移 + `MainMenuScreen` 所有 `Color.XXX` 迁移，工作量增加 |
| PR-02 | 1 人日 | 1 ~ 1.5 人日 | `MainMenuController` 构造签名改动 + 测试同步 |
| PR-03 | 2 ~ 2.5 人日 | **3 ~ 4 人日** | 最被低估：ModalStack 引入 + 输入语义冻结 + 现有 `F/I/L/X` 键迁移决议 + Look Mode 基础版 + `InputHandler` 35.7K 重构 + `InputHandlerTest` 同步。这是关键路径上最容易延期的 PR |
| PR-04 | 1.5 ~ 2 人日 | 2 ~ 2.5 人日 | 图标补齐数量取决于当前 `items/index.yaml` 缺口，需要先盘点；contractLint 规则扩写 |
| PR-05 | 1.5 人日 | 1.5 ~ 2 人日 | content-ui-lint 落地位置不确定（见 §2.9）会影响估时 |
| PR-06 | 1.5 ~ 2 人日 | 1.5 ~ 2 人日 | 符合实际 |
| PR-07 | 1.5 ~ 2 人日 | 1.5 ~ 2 人日 | 符合实际 |
| PR-08 | 2 ~ 3 人日 | 2.5 ~ 4 人日 | 若战斗三层作为 ModalStack 栈帧（见 §2.5），需要栈深扩容 + 焦点恢复规则补充；若作为 frame 内部阶段机，仍需 Action/Style/Target 三层 affordance 建模 |

**合计**：文档 12 ~ 16 人日 → review 估计 **15 ~ 21 人日**。

说明：单人开发连续投入时，净工作量 × 1.3 ~ 1.5 大约是日历周期。文档"按 8 个串行 PR"意味着最快约 3 个工作周、最慢约 5 个工作周完成。这与"短期里程碑"的概念匹配。

---

## 7. 应补进文档的章节（减少开发时"查真源"往返）

下面列出的表格应该直接落在 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md` 里，否则开发者每次都要重新翻代码才能继续。

### 7.1 `UiMode × ModalStack 归属表`（建议新增 §5.2a）

| UiMode | 类型 | 进栈 | `reconcileMode` 覆盖规则 |
| --- | --- | --- | --- |
| MAP | 根 | 不进栈（栈底） | 默认落点 |
| SHOP | 被动 | 不进栈 | `activeShop != null` 时强制 |
| WORLD_MAP | 被动 | 不进栈 | `activeRouteSelection != null` 时强制 |
| STAT_ASSIGN | 被动 | 不进栈 | `hasPendingStatAllocation` 时强制 |
| INVENTORY | 主动 | 进栈 | 开 = push；关 = pop |
| LOADOUT_EDIT | 主动 | 进栈 | 同上 |
| TALENT_ASSIGN | 主动 | 进栈 | 同上（注意：当前已有 allocation 规则，需要确认 ESC 取消语义） |
| INSPECT（Look Mode） | 主动 | 进栈 | 同上 |
| TARGETING | 主动 | 进栈 | 若与 PR-08 战斗三层合并则单独定义 |
| VALIDATION | 开发态 | 独立 | 不参与 ModalStack |

### 7.2 `键位迁移决议表`（建议新增 §5.2b）

| 现有键位 | 现有语义 | PR-03 后语义 | 迁移策略 |
| --- | --- | --- | --- |
| `F` | overlay 通用关闭 | 让位给 ESC | 保留 F 作为 alias（兼容期），未来废弃 |
| `I`（SHOP 态） | 关闭 Shop | 与 ESC 并存 | 保留，作为"对称快捷关闭" |
| `L`（LOADOUT_EDIT 态） | 关闭 Loadout | 与 ESC 并存 | 保留，作为"对称快捷关闭" |
| `X`（WORLD_MAP 态） | 下移选择 | 让位给方向键 / DOWN | 废弃 X，仅保留 DOWN / S |
| `X`（SHOP BUY 态） | 下移选择 | 同上 | 废弃 |
| `X`（SHOP SELL 态） | 下移选择 | 同上 | 废弃 |
| `X`（MAP 态） | 进 INSPECT | 进 Look Mode（改造现有 INSPECT） | 保留 |

### 7.3 `现有硬编码颜色 → token 替换清单`（建议新增 §7.3a）

用户自己跑一次：

```bash
rg --type kotlin -n 'Color\.(GOLD|RED|CYAN|MAGENTA|LIGHT_GRAY|GRAY|DARK_GRAY|SALMON|WHITE|BLACK)' client/src/main/
rg --type kotlin -n '#[0-9A-Fa-f]{6}' client/src/main/
```

把结果贴成 `替换清单` 表格，作为 PR-01 的退出条件附件。

### 7.4 `golden 目录规范`（建议在 §16.2 内补）

```
client/src/test/resources/golden/
  uiux-pr01-token/
    zh-CN/
      main_menu.png
      in_game_map.png
      ...
  uiux-pr03-modal/
    zh-CN/
      inventory_open.png
      inspect_mode.png
  ...
```

### 7.5 `Look Mode 与 INSPECT 的 cutover 表`（建议新增 §9.3a）

| 能力 | 现有 INSPECT | PR-03 基础 Look Mode | 实现路径 |
| --- | --- | --- | --- |
| 自由光标移动 | ✓ | ✓ | 复用 `inspectCursor` |
| 光标位置 → 目标卡同步 | 部分 | 必须完整 | 扩 `TargetCardModel.fromInspectCursor` |
| 光标位置 → 世界面高亮 | ✗ | 必须 | PR-03 新增 |
| 多层 tooltip | ✗ | 仍然 ✗ | 留给 Look Mode 完整版 |
| 按 `?` 打开当前格详情 modal | ✗ | 必须 | PR-03 新增 |

### 7.6 `ItemRenderSnapshot.iconKey 盘点表`（PR-04 前置）

用户先跑：

```bash
# 盘点 game 侧 content 中还有多少 item / special 没定义 iconKey
rg -n 'iconKey' game/src/main/resources/data/items/
rg -n 'iconKey' game/src/main/resources/data/special/
```

把"当前 N 个 item 没 iconKey"的数字填入文档，作为 PR-04 范围估算基准。

---

## 8. 风险补充

文档 §17 列了 5 条风险。以下是补充：

### 8.1 `InputHandler` 35.7K 的 blast radius

风险：PR-03 要动 `InputHandler.reconcileMode / pollMapCommand / pollShopCommand / pollInventoryCommand / pollLoadoutCommand / pollTargetingCommand / pollInspectCommand / pollWorldMapCommand / pollStatAssignCommand / pollTalentAssignCommand`——10 个 poll 函数全量回归。

对策：

1. PR-03 实施前先做一次 `InputHandlerTest` 覆盖率盘点，确认哪些 poll 函数 test-underpinned。
2. 分批迁移：先落统一语义表 + ModalStack 空壳，再改 INVENTORY / LOADOUT_EDIT 两个"典型主动 mode"，剩余 mode 在同 PR 末期收尾。
3. 保留 `git diff` 单独提交的能力，遇到回归能快速 bisect。

### 8.2 `TileRenderer` 29.8K + `TileRenderModel` 63K 在 PR-03 内承担过多

风险：PR-03 §9.2 列了 `TileRenderer / TileRenderModel / InputHandler / FoundationGameScreen` 四个大文件。加上 5 个新建 model 文件，单个 PR 的 review 负担过重。

对策：

1. PR-03 内部再切两个 commit：C1 = ModalStack + 输入语义；C2 = 信息面 model + Look Mode 同步。
2. 若发现 `TileRenderModel` 63K 中与 ModalStack 无关的部分会被连带改动，考虑把 Log/Player/Target/Action card model 抽取拆到 **PR-03.5** 的小 PR（文档现在是 8 个 PR，可以扩成 9 个）。

### 8.3 `CombatDecisionPanel` 应不应该折回 PR-03 同期完成

风险：PR-08 的战斗三层决策面依赖 ModalStack（§3.2）。如果 PR-03 完成后 PR-04/05/06/07 各自独立推进了几周，到 PR-08 时 `ModalStack` 可能已被其他 PR 的特例污染。

对策：

1. 方案 A：PR-03 落地时**同时**搭 `CombatDecisionPanel` 空壳（只有 sealed interface + 空 implementation），保证 ModalStack 从一开始就考虑"战斗三层"栈帧的承载能力。
2. 方案 B：接受污染风险，PR-08 开始时若发现 ModalStack 不足就扩容。
3. 单人开发下，方案 A 成本略高但能避免 PR-08 阶段返工 PR-03——推荐 A。

### 8.4 `RenderSnapshotAssetAudit` 的 audit scope 可能抗拒"可容忍缺失"

文档 §5.4 第 1 条"`iconKey` / `visualKey` 缺失时显示统一 fallback"。

真源：`RenderSnapshotAssetAudit.audit(snapshot)` 会遍历 snapshot 触发硬 fail。

风险：PR-04 在图标还没全量补齐前，本地 `FoundationGameScreen.render` 会 crash。单人开发如果没接 fallback 路径，会出现"每补一个 iconKey 才能测一次"的退化体验。

对策：

1. PR-01 退出条件之一：`RenderSnapshotAssetAudit` 必须有"告警模式"（只 log 缺失、不 throw），供 PR-04 迁移期使用。
2. PR-04 完成后再切回 "硬 fail 模式"。

---

## 9. 文档级小问题清单（P2）

以下是文档层面的措辞 / 数值 / 可读性问题，单人开发下不阻塞实施，但建议一次性清理。

1. §0 第 8 条写 "`PendingTelegraphState`" 与 "`TelegraphRegistry`" 并列——前者不存在（见 §1.1）。
2. §3 第 6 条"不能被解释为正式 item stack quantity"——措辞正确，但可以追加一句"未来若需要 stack，必须先扩 `ItemRenderSnapshot.stackCount` 并通过 contractLint 引导"。
3. §5.1 PR 映射矩阵表格里 "a11y / 音效 / 性能基线" 横跨 8 个 PR 全部打勾——含义等于"每个 PR 都要做"，但实际上 a11y 色盲回退等条目可以按本 review §5.4 降级，建议拆列。
4. §5.6 第 6 条"当前阶段的音效补充坚持最小必要 cue 原则"——可以加一句"开发期自听音效即可判断，不需要做 dB 级别客观指标"。
5. §6 PR 拆分总览表格中 "建议估时" 写在表格外，建议合并进表格增加可读性。
6. §7.5 推荐验证命令块里 `./gradlew :client:test --tests "..."` 的 `...` 部分未展开——建议直接写具体测试类名，开发时不用回查文档。
7. §11.3 第 4 条"新增 `content-ui-lint` 或等价约束"—— "或等价约束"在 PR 退出条件里是模糊语义，建议去掉模棱两可措辞（见 §2.9）。
8. §13.3 第 5 条"`Advanced Tooltip` 仍然留在长期候选，不前置到本 PR"——建议改为"本 PR 完成后，若开发者实际感到说明密度不足，再启动 Advanced Tooltip 的独立规划"，把触发条件也写进 §15 候选表。
9. §14.3 第 5 条措辞"当前 PR 只要求'如果已有 typed fact，就消费'"——见 §2.6，建议改为明确拒绝。
10. §16.2 第 2 条"默认主分辨率固定为 1600×900"——可以加"（开发者本机分辨率可配置，golden 以此为基准，不代表游戏内部强绑定）"。
11. §18 "一句话原则"引用：单人开发下可以追加"即使规划完美，也以玩家本人实际游玩体验为最终验收"。

---

## 10. 结论与下一步

### 10.1 review 结论

1. 本执行计划的**方向与原则完全对**：扩现有 contract 不新造平行家族、Phase 2 基座先于 Phase 3、telemetry 后置、资源管线走现有 `--extra-plan` 接线。
2. 但**执行细节离"开发时不乱猜"还有距离**，集中在 3 处：
   - 真源事实错误（§1，7 条）。
   - 开发必然乱猜的规划盲点（§2，13 条）。
   - PR 间依赖未画显（§3，4 条硬前置）。
3. 在"长期单人开发游玩"前提下，有 6 处可以安全简化（§5）。
4. 工作量整体偏乐观，尤其 PR-03 应从 2 ~ 2.5 人日调到 **3 ~ 4 人日**（§6）。

### 10.2 下一步建议（P0 级，必须先于 PR-01 开工）

1. **修 §1 的 7 条事实错误**（尤其 `PendingTelegraphState` → `OverlayRenderSnapshot + TelegraphRenderer`）。
2. **新增 §5.2a ModalStack 归属表 + §5.2b 键位迁移决议表**（§2.1、§2.2、§2.4、§7.1、§7.2）。
3. **明确 PR-03 Look Mode 改造路径**："改现有 INSPECT" 还是"新增 LOOK"（§2.3）。
4. **明确 PR-08 战斗三层决策面的栈形态**："3 个栈帧" 还是 "单一 frame 内的阶段机"（§2.5）。

### 10.3 下一步建议（P1 级，可与 PR-01 并行完成）

1. 补 §10.3 `QualityPresentation` 具体颜色/图形映射表（§2.8、§7.3）。
2. 补 §10.3 地图掉落 Z-order 规则（§2.7）。
3. 补 §12.3 `ZONE_EFFECT / TELEGRAPH` presentation group 派生规则（§1.7）。
4. 补 §16.2 golden 目录命名规范（§2.10、§7.4）。
5. 补"item iconKey 盘点表"作为 PR-04 前置（§7.6）。

### 10.4 下一步建议（P2 级，任何时间）

1. §9 小问题清单里的 11 条文档级小问题。
2. 决定单人开发期是否执行本 review §5 的 6 条简化。

### 10.5 一句话收口

> **先让规划里每个子问题都有显式答案，再开工。** 单人开发前提下没有"别人帮忙兜底"的余量，乱猜 1 次 = 返工 1 次 = 日历时间损失远大于修文档的 2 小时。

---

*本 review 基于当前仓库真源代码与 `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md` 的完整阅读；未覆盖部分（例如实际 golden baseline 差异、scripts/asset_pipeline_common.py 的细节约束）需要在具体 PR 开工前做二次盘点。*
