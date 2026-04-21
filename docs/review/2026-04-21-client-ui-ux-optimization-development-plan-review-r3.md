# K-ToME 客户端 UI/UX 优化执行计划 · 深度 Review（R3）

- **评审对象**: `docs/opt/2026-04-21-client-ui-ux-optimization-development-plan.md`（v3，全 1496 行）
- **上一轮 review**: `docs/review/2026-04-21-client-ui-ux-optimization-development-plan-review-r2.md`（R2，429 行）
- **前提**: 当前及较长时间内只有单人（lky）开发 / 测试 / 游玩；telemetry 与用户行为统计明确后置
- **Review 目标**:
  1. 复核 v3 是否完整吸收 R2 的 4 条 P0 + 11 条 P1 + 9 条 P2
  2. 对 v3 新增的 8 段小表 / 状态图 / truth table 做真源级逐行验证
  3. 在最细粒度上挑出仍会让后续 PR-01 ~ PR-08 实施时"停下来猜"的点
  4. 公开纠正 R2 报告自身继承到 v3 的一个真源引用错误（见 §1）

---

## 0. Review 基准与总体判断

### 0.1 基准（真源并行验证）

本轮 review 再次以仓库真源为 ground truth，重新验证了以下关键点：

1. `client/src/main/kotlin/com/ktome/client/input/InputHandler.kt`
   - `UiMode` 仍是 10 枚举（`MAP / SHOP / WORLD_MAP / INVENTORY / LOADOUT_EDIT / TARGETING / INSPECT / VALIDATION / STAT_ASSIGN / TALENT_ASSIGN`）
   - `overlayCloseBindings = listOf(Keys.F)` / `waitBindings = [PERIOD, SPACE, NUMPAD_5]` 仍成立（line 62 / 93）
   - **`pollInspectCommand` line 485-501 只用 `movementBindings`，不含 `I / J / K / L` 的 vim 映射**
   - **`pollTargetingCommand` line 609-635 只用 `movementBindings`，不含 `I / J / K / L` 的 vim 映射**
   - **`pollValidationCommand` line 537-540 是唯一用 `I / J / K / L` 作 inspect cursor vim 方向键的代码点**
   - `reconcileMode` 仍 private
   - `hasPendingStatAllocation` 真源：`InputHandler.kt:840` / `FoundationGameSession.kt:925`（`playerStatus.statPoints > 0`）
2. `client/src/main/kotlin/com/ktome/client/telegraph/TelegraphRenderer.kt`
   - `fallbackColorHex` 返回 4 个 hex：`#7B1FA2 / #E53935 / #F6C445 / #3A86FF`
   - `tileTone / asciiTone` 是 private，返回 `TileTextTone / AsciiTextTone` 枚举
3. `core/src/main/kotlin/com/ktome/core/ai/ThreatProfile.kt`
   - `DangerLevel = { LOW, MODERATE, HIGH, LETHAL }`
4. `game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt:3343`
   - `DangerLevel.overlaySeverity`: `LOW=1 / MODERATE=2 / HIGH=3 / LETHAL=4` → 与 `fallbackColorHex` 的 `>=4 / ==3 / ==2 / else` 分支完全对齐
5. `core/src/main/kotlin/com/ktome/core/snapshot/RenderSnapshot.kt:54`
   - `MapCellSnapshot.terrainOverride: TerrainOverrideRenderSnapshot?` 存在
6. `client/src/test/kotlin/com/ktome/client/golden/GoldenScreenshotHarnessTest.kt:256/290/328/388`
   - `withLwjgl3Context(width = 1280, height = 800)` → v3 §16.2 描述正确
7. `client/src/main/kotlin/com/ktome/client/screen/MainMenuController.kt:78`
   - `fun pollAction(hasSave: Boolean): MainMenuPollResult` → v3 §8.3 task 1 描述正确
8. `client/src/main/kotlin/com/ktome/client/input/ValidationCommandSource.kt:124`
   - `class ValidationCommandSource(...) : CommandSource by delegate`（`delegate = InputHandlerCommandSource(InputHandler(...))`）
9. `scripts/` 下真源：`generate_assets.sh` / `process_assets.py` / `sync_phase2_manifests.py` 均存在 → v3 §5.6 的命令引用正确

### 0.2 总体判断

v3 相对 v2 是一次**非常彻底的吸收**：R2 标记的 4 条 P0、11 条 P1、9 条 P2 **几乎全部进入了 v3 的正文**（见 §2 的吸收矩阵）。文档现在已经从 R2 结论里的"执行手册"升级成**带单元测试尺度 checklist 的执行手册**。

本轮 R3 的任务相应降为"在 1496 行里再抠细节"：

| 层级 | 含义 | 本轮数量 |
| --- | --- | --- |
| P0 | 事实错误 / 真源冲突 / 自相矛盾 → 不修 PR 一定写错 | 0 |
| P1 | 执行层面的精度盲点 → 不修 PR 会停 30 min 以上做选择 | 16 |
| P2 | 细微漂移 / 术语不对齐 / 单人前提下可松绑 | 15 |
| 修订 | R2 报告自身错误需要公开纠正（被 v3 继承） | 1 |

总体结论一句话：**v3 已经是一份可以不需要第三人解说就能 PR-01 开工的手册**；R3 提出的所有建议都是"让每个子表再少一次重新打开 InputHandler.kt 去数"的微优化。

---

## 1. 纠正：R2 报告自身错误（已被 v3 继承，需同步更正）

### 1.1 事实

**R2 §2 P0-2 曾断言**："`InputHandler.kt:537-540` 中 `Keys.I / Keys.L` 在 **`TARGETING / INSPECT`** 里兼作 vim 方向键"。这是对真源函数名的误判。

**真源实际情况**（以本次并行验证为准）：

```kotlin
// client/src/main/kotlin/com/ktome/client/input/InputHandler.kt

private fun pollInspectCommand(snapshot: RenderSnapshot): PlayerCommand? {   // line 485
    if (isOverlayCloseBinding() || input.isKeyJustPressed(Keys.X)) {
        clearInspect(); return null
    }
    val cursor = inspectCursor ?: defaultInspectCursor(snapshot)
    val movement = movementBindings.entries.firstOrNull { (k, _) -> input.isKeyJustPressed(k) }?.value
    // ... 只用 movementBindings（WASD / 方向键 / Numpad），没有 I/J/K/L vim 映射
}

private fun pollTargetingCommand(snapshot: RenderSnapshot): PlayerCommand? { // line 609
    if (isOverlayCloseBinding()) { clearTargeting(); return null }
    val cursor = targetingCursor ?: playerPosition(snapshot)
    val movement = movementBindings.entries.firstOrNull { (k, _) -> input.isKeyJustPressed(k) }?.value
    // ... 同上，也不含 I/J/K/L 的 vim 映射
}

private fun pollValidationCommand(snapshot: RenderSnapshot): PlayerCommand? { // line 503
    // ... validationCursor.moveSection/moveAction
    val inspectionDelta =                                                      // line 535-542
        when {
            input.isKeyJustPressed(Keys.I) -> Point(0, -1)   // 上移 inspect cursor
            input.isKeyJustPressed(Keys.K) -> Point(0, 1)
            input.isKeyJustPressed(Keys.J) -> Point(-1, 0)
            input.isKeyJustPressed(Keys.L) -> Point(1, 0)
            else -> null
        }
    // ... 仅在 mode == UiMode.VALIDATION 的代码路径下执行
}
```

**结论**：`I / J / K / L` 的 vim 方向键**只存在于 `VALIDATION` overlay 内部**，服务于 validation 调试态下的 inspect cursor 移动。`INSPECT` 和 `TARGETING` 两个 mode 的光标移动都只走 `movementBindings`（`WASD / 方向键 / HOME/END/PAGE_UP/PAGE_DOWN / NUMPAD_1..9`），没有 `I / L` 的二义性。

### 1.2 v3 里被继承的错位

- v3 §5.2b 第 6 行："`I / L` 在 `TARGETING / INSPECT` 内部兼作方向键" → 位置错，应为 `VALIDATION`
- v3 §5.2b "当前阶段明确废除的歧义" 第 4 条：同样错把 `INSPECT / TARGETING` 写成 vim 方向键的场所
- v3 §9.3.1 truth table `INSPECT | I / L | 无 | 当前阶段明确废除 vim 方向键歧义` → 对 INSPECT 而言这条是**无意义的**（本来就没有），不过写出来也不是错；关键是 VALIDATION overlay 下是否要保留这个 vim 方向键没有对应 truth table 行。

### 1.3 最低改法

- §5.2b 第 6 行改为："`I / J / K / L` 在 `VALIDATION` overlay 内部兼作 inspect cursor 的 vim 方向键 | 废除；VALIDATION 内部光标移动同样统一回 `WASD / 方向键 / Numpad` | 避免 validation 调试态与正常 `I` = 背包快捷入口之间的键源分裂"
- §5.2b "当前阶段明确废除的歧义" 第 4 条改为："`I / J / K / L` 不再承担 `VALIDATION` overlay 的 inspect cursor 移动职责"
- §9.3.1 truth table 增加：`VALIDATION | I / J / K / L | 无 | 原 vim 方向键废除；移动光标统一回 movementBindings`

（这个纠错必须写在 R3 最前，否则 v3 一旦按 R2 的错误描述落地，PR-03 在 `pollInspectCommand / pollTargetingCommand` 里会去删一段不存在的代码。）

---

## 2. v3 对 R2 的吸收矩阵

| R2 建议 | 吸收状态 | v3 对应位置 | 残余问题 |
| --- | --- | --- | --- |
| P0-1 `F` 行纠错 | ✅ | §5.2 键位表 `F`（过渡期）行 + 脚注 2 | 见 §3 P1-3（第 3 列对 TARGETING 下 F 的描述仍不准确） |
| P0-2 `I / L` 方向键废除 | ✅（继承 R2 错误，见 §1） | §5.2b 第 6 行 + 废除条款第 4 条 + §9.3.1 INSPECT I/L 行 | 见 §1 |
| P0-3 `ModalFrame.kind` 扩充 | ✅ | §5.2a 第 1.6/1.7/1.8 项 | 见 §3 P1-4（ITEM_COMPARE 的 deferred stub 对深度 3 限制的影响没写） |
| P0-4 `Tab` 四面焦点链 | ✅ | §5.2 脚注 1 + §9.3 task 1（PaneFocusController） | 见 §3 P1-1（Tab 在各 mode 的 truth table 仍缺） |
| P1-1 VALIDATION owner | ✅ | §5.2a `VALIDATION` 行 owner = `ValidationCommandSource` | 见 §3 P1-14（owner 表达精度：实际是 delegate 包装 InputHandler） |
| P1-2 QualityPresentation 映射表 | ✅ | §10.3 task 2 5 行表 + §7.3 task 1 的 `color.quality.*` 首发要求 | 见 §3 P1-8（5 行表容易被误读成 rarity 5 档） |
| P1-3 多件掉落排序 | ✅ | §10.3 task 4 | 干净落地，无残余 |
| P1-4 force-switch toast | ✅ | §5.4 第 5 条 | 无残余 |
| P1-5 token 替换点（fallbackColorHex / Tone 映射） | ✅ | §7.3 task 2 前 2 个 bullet | 无残余 |
| P1-6 `DesktopLauncher.kt` 归属 | ✅ | §7.3 task 4 | 见 §4 P2-11（标题保留"应用名"还是"seed + locale"没定） |
| P1-7 `ItemIconKeyCoverageRule` | ✅ | §10.3 task 8 独立条目 | 无残余 |
| P1-8 InputHandler truth table | ✅ | §9.3.1 | 见 §3 P1-1/P1-2/P1-6/P1-7（缺 Tab / TALENT_ASSIGN / LOADOUT_EDIT / WORLD_MAP / STAT_ASSIGN 行） |
| P1-9 CombatDecisionFrame 状态图 | ✅ | §14.3.1 | 见 §3 P1-10/P1-11/P1-12（无可选方式触发条件 / 无合法目标 / Tab 在 phase 内循环的语义） |
| P1-10 `< 100ms` 软化 | ✅ | §5.5 性能基线第 3 条 | 无残余 |
| P1-11 UiErrorPayload | ✅ | §5.4 第 4 条 + §11.3 task 2 | 见 §3 P1-13（三个字段的类型与 `<build-hash>` 取值源未定） |
| P2-1 `L / X` 两行补到 §5.2 | ✅ | §5.2 第 6/7 行 | 无残余 |
| P2-2 `I` 行 modal 态描述 | ✅ | §5.2 `I` 行第 2 列 | 无残余 |
| P2-3 `Ctrl+S` 脚注 | ✅ | §5.2 脚注 1 | 见 §4 P2-14（targeting 态"禁用"含义不明） |
| P2-4 双语 golden 松绑 | ✅ | §16.2 第 4/5 条 | 见 §4 P2-13（遗留 phase2/phase3 golden label 如何处理没说） |
| P2-5 manifest 合流口径 | ✅ | §5.6 图片/音频管线标准各第 4 条 | 无残余 |
| P2-6 加载态量化软化 | ✅ | §5.4 第 3 条 + §5.5 性能基线 | 见 §4 P2-12（§11.3 task 3 第 3 bullet 与 §5.4 重复） |
| P2-7 `contractLint / contentUiLint` 命名统一 | ✅ | 全文一致 | 无残余 |
| P2-8 ExplainPane 归属 | ✅ | §13.3 task 3 | 见 §3 P1-9（`?` 键在 INSPECT frame 下与通用帮助冲突） |
| P2-9 孤儿 key 反向条款 | ✅ | §16.5 第 9 条 | 无残余 |
| R2 §5.1 `PR-01→PR-02 / PR-05→PR-07` 弱前置 | ✅ | §6.1 弱前置 2 条 | 见 §4 P2-1（PR-05→PR-07 在串行前提下语义冗余） |
| R2 §5.2 估时复核 `17-22 人日` | ✅ | 头部 + §6.1 | 无残余 |

**吸收率：26/26 全吸收。** R2 提出的 P0/P1/P2 + 依赖矩阵 + 估时 100% 进入 v3 正文；唯一"偏差"是 §1 里指出的 R2 自身真源误判被忠实地抄了进去。

---

## 3. P1 · v3 仍残留的执行精度盲点（16 条）

### P1-1 · §9.3.1 truth table 缺 `Tab / Shift+Tab` 在各 mode 的行为

**问题**：§9.3 task 1 引入了 `PaneFocusController`，§5.2 `Tab` 行也写了"世界/上下文/角色动作面"；但 §9.3.1 truth table 只有一行 "任意 modal | Tab / Shift+Tab | 只在当前 frame 内循环焦点"。
- `MAP` 下 Tab 到底是 "世界 → 上下文 → 角色动作" 还是 "日志 → 目标卡 → 背包快捷"？
- `INSPECT` 下 Tab 是否进一步在 inspect 内部的 `ExplainPane / Target info / Keyword chips` 之间循环？
- `TARGETING` 下 Tab 的候选循环对象（候选敌人？候选方向？）在 §14.3.1 里和 truth table 里都没对齐。

**建议**：§9.3.1 补：

| UiMode / Frame | 键 | 期望行为 | 备注 |
| --- | --- | --- | --- |
| `MAP` | `Tab` | 下一个 PaneFocus 锚点：`世界 → 上下文 → 角色动作 → 世界` | `PaneFocusController` 持有 3 个 anchor |
| `MAP` | `Shift+Tab` | 反向循环 | |
| `INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT` | `Tab` | 当前 frame 内焦点锚点循环（例如 INVENTORY 的 `list → detail → compare`） | `modal` 打开后 Tab 不再穿透 `MAP` 锚点 |
| `TARGETING` | `Tab` | 候选合法 target 之间循环；若无候选则 no-op | 与 `CombatDecisionFrame.TARGET` phase 的 Tab 语义一致 |

### P1-2 · §9.3.1 truth table 缺 `TALENT_ASSIGN / LOADOUT_EDIT / WORLD_MAP / STAT_ASSIGN` 行

**问题**：v3 把 truth table 写成"最小行为面"，但实际 `PR-03` 直接命中的所有 mode 都应该有条目。真源里：
- `TALENT_ASSIGN`: `Keys.T` 关闭 + `Keys.BACKSPACE` rollback + `Keys.R` respec + 数字键 / `Keys.E` 等
- `LOADOUT_EDIT`: `Keys.L` 关闭 + 数字键选 slot + 方向键选 reserve
- `WORLD_MAP`: `Keys.ENTER/SPACE/E` 确认路线
- `STAT_ASSIGN`: `Keys.NUM_1..NUM_4` 分配 STR/DEX/CON/WIL

**建议**：`§9.3.1` 至少补：

| UiMode / Frame | 键 | 期望行为 | 备注 |
| --- | --- | --- | --- |
| `TALENT_ASSIGN` | `T` | close | 对称开关（真源 `pollTalentAssignCommand` line 651） |
| `TALENT_ASSIGN` | `ESC` | 直接回 `MAP` | |
| `TALENT_ASSIGN` | `Backspace` | `PlayerCommand.RollbackTalentDraft` | 真源 line 665 |
| `TALENT_ASSIGN` | `R` | `PlayerCommand.RespecTalentTree`（若可 respec） | 真源 line 669-673 |
| `LOADOUT_EDIT` | `L` | close | 真源 line 446 |
| `LOADOUT_EDIT` | `ESC` | 直接回 `MAP` | |
| `LOADOUT_EDIT` | `1-4` | 选 slot 1..4 | 真源 line 452-455 |
| `WORLD_MAP` | `Enter / Space / E` | 确认当前 route | 真源 line 435-441 |
| `STAT_ASSIGN` | `1-4` | AssignStat STR/DEX/CON/WIL | 真源 line 645-648 |

### P1-3 · §5.2 `F` 行第 3 列对 TARGETING 的描述不准确

**问题**：v3 §5.2 `F`（过渡期）第 3 列写"无"；但真源 `pollTargetingCommand` line 611 `isOverlayCloseBinding()` → 在 TARGETING 下按 `F` 会 `clearTargeting`。也就是说 `F` 在 TARGETING 下实际 = 关闭 targeting，等价于 ESC。

**建议**：§5.2 `F`（过渡期）第 3 列改为："关闭 targeting（legacy alias，等价 ESC）"。
否则 PR-03 的 `InputHandlerTest` 断言 "TARGETING 下 F 不触发" 会和真源行为冲突，要么改真源要么改 truth。

### P1-4 · §5.2a ModalStack 深度 3 与 `ITEM_COMPARE` 的 deferred stub 矛盾

**问题**：§5.2a 补充约束 2 说"ITEM_COMPARE 当前阶段允许先以 deferred stub 预留，不要求 `PR-03` 首批做满；但栈语义必须先冻结"。但 §5.2 的"Inventory → Item Detail → Compare"深度 3 示例假设 `ITEM_COMPARE` 是一个**已 push 的真实 frame**。

如果 `ITEM_COMPARE` 是 deferred stub，那这个深度 3 示例就要么：
- 改为"Inventory → Item Detail"深度 2（当前能真正做到的最大值）
- 或者承认 deferred stub 已经占一层栈深度（即使内部为空）

**建议**：§5.2a 在补充约束 2 后加一句："deferred stub 仍占一层栈深度（即不允许在 `ITEM_COMPARE` 未实现时把'Inventory → Item Detail → Compare'改写成深度 2）；`PR-03` 预留的 `COMPARE` frame kind 即使内部为空壳，也必须参与深度 = 3 的计数。"

### P1-5 · §9.3.1 `MAP | 1-9` 没说明 inscription 与 talent 的分流优先级

**真源**：`pollMapCommand` line 338-361：先 `hotkeyInscription`（line 338-348），再 `hotkeySlot`（line 350-361）。即 **inscription 优先级高于 talent**。若同一数字键同时绑定 inscription 和 talent，inscription 命中先返回。

**建议**：§9.3.1 `MAP | 1-9` 行 "期望行为" 改为："**优先**使用 inscription hotkey；若未命中，则使用 active talent slot"；"备注" 加一行"分流优先级冻结为 inscription > talent，与 `pollMapCommand` 真源一致"。

### P1-6 · §9.3.1 truth table 未区分 `MAP | X` 的前置条件

**问题**：v3 §9.3.1 "MAP | X | push INSPECT frame | 直接升级后的 Look Mode"。但真源 `InputHandler.kt:138-142`：

```kotlin
if (mode == UiMode.MAP && input.isKeyJustPressed(Keys.X)) {
    mode = UiMode.INSPECT
    inspectCursor = defaultInspectCursor(snapshot)
    return null
}
```

这个检查在 `reconcileMode` **之前**运行（line 134-142 是 pre-reconcile shortcut）。若 snapshot 随后强制 `activeShop != null`，`reconcileMode` 会把 mode 改回 `SHOP`，导致 X 实质失效。PR-03 重写时是否保留这种"pre-reconcile shortcut"模式？

**建议**：§9.3.1 `MAP | X` 行补脚注："当前真源在 `reconcileMode` 之前做 pre-check；PR-03 落地后必须明确声明 `push INSPECT` 是否允许被被动态 snapshot（`activeShop / activeRouteSelection`）立即抢回。"

### P1-7 · §9.3.1 缺 `TARGETING | . / Space / Numpad5` 行

**问题**：真源 `pollTargetingCommand` line 636-637 里 `input.isKeyJustPressed(Keys.ENTER) || input.isKeyJustPressed(Keys.SPACE)` 把 **`Space` 当作"确认目标"**。而 v3 §5.2 `. / Space / Numpad5` 行第 3 列写"无"，自相矛盾。

**建议**：
- §5.2 `. / Space / Numpad5` 行拆成两行：
  - `.` 与 `Numpad5`：MAP 态"等待一回合"；其他态"无"
  - `Space`：MAP 态"等待一回合"；modal / targeting 态"确认（等价 `Enter`）"
- §9.3.1 truth table 补 `TARGETING | Space | 确认当前目标 | 真源 line 637`
- 同理 §9.3.1 补 `INVENTORY | Space | 激活当前物品 | 真源 line 594-599`

### P1-8 · §10.3 QualityPresentation 映射表易被误读成 5 档 rarity

**问题**：v3 §10.3 task 2 把表格写成 5 行并排：

```text
| NORMAL   | color.quality.normal / 无角标        |
| MAGIC    | color.quality.magic / ◆               |
| RARE     | color.quality.rare / ◆◆              |
| UNIQUE   | accent.special.unique                 |
| ARTIFACT | accent.special.artifact               |
```

5 行并排视觉上是一个一维梯度。但 §3 合同第 3 条明确写："`RarityTier = NORMAL / MAGIC / RARE`" + "`SpecialTier = UNIQUE / ARTIFACT`"，这是**两维正交**。`MAGIC + UNIQUE`、`RARE + ARTIFACT` 都是合法组合。

**建议**：把 §10.3 task 2 的映射表拆成两个独立小表，并明示"两维正交叠加"：

```
RarityTier 轴（主色与基础角标）:
| NORMAL | color.quality.normal       | 无角标 |
| MAGIC  | color.quality.magic        | ◆     |
| RARE   | color.quality.rare         | ◆◆   |

SpecialTier 轴（可叠加的 client-local accent，不改变 rarity 主色）:
| UNIQUE   | accent.special.unique   | 外描边 / 角标 halo     |
| ARTIFACT | accent.special.artifact | 外描边 / 角标 halo     |

表现合成规则:
1. rarity 主色始终渲染
2. 若 specialTemplateId != null，叠加对应 accent（外描边 / halo）
3. 组合示例: "RARE + UNIQUE" = 稀有主色 ◆◆ + unique 外描边
```

### P1-9 · §5.2 `?` 与 §13.3 `ExplainPane` 的 `?` 触发点冲突

**问题**：
- §5.2 `?` 行：`modal 态 → 打开当前面板帮助` / `targeting 态 → 打开当前动作解释`
- §13.3 task 3：INSPECT frame 里的 `ExplainPane` 由 `?` 打开（"按 ? 打开详情层"）

当玩家在 `INSPECT frame` 里按 `?` 时：到底打开"当前面板帮助"（一般 help overlay），还是"ExplainPane"（对当前光标对象的解释）？这两个语义不兼容，PR-07 落地时必然回头补决议。

**建议**：§13.3 task 3 改为："`INSPECT frame` 下按 `?` **优先**打开 `ExplainPane`（对当前光标对象的解释）；若 `ExplainPane` 已打开，则 `?` 切回一般 help overlay（panel help）。`ExplainPane` 不新增 UiMode 也不占 ModalStack 深度，仅替换 INSPECT frame 的右侧信息面。"

### P1-10 · §14.3.1 "若该动作无可选方式 → TARGET" 触发条件含糊

**问题**：phase 状态图写"若该动作无可选方式 → TARGET"，但"无可选方式"的判定源没写。是看 `ActionDefinition.methods` 静态定义？还是看 snapshot 的 `uiState.availableMethods.size == 1`？

**建议**：§14.3.1 补："触发条件为 `snapshot.uiState.combatDecision?.currentAction?.methodOptions.size <= 1`（以 snapshot 为准，不是静态定义）；若等于 0，则视为该动作不合法，`ACTION phase` 拒绝选中并保持焦点在 action list。"

### P1-11 · §14.3.1 未定义 "`TARGET phase` 下无合法目标" 场景

**问题**：若敌人全死或目标全离开视野，`TARGET phase` 下玩家按 Enter 确认时应该：
- 拒绝并在日志区给 `ui.message.combat.no-legal-target`？
- 自动降级到 `METHOD phase`？
- 完全 pop frame 回 MAP？

**建议**：§14.3.1 补："若 `TARGET phase` 下 `snapshot.uiState.combatDecision.legalTargets.isEmpty()`，`Enter` = 拒绝提交 + `ui.message.combat.no-legal-target` toast；`Backspace` 行为不变（仍回 METHOD）。玩家必须手动 Backspace / ESC 退出，不自动降级。"

### P1-12 · §14.3.1 "Tab / Shift+Tab / 数字键 / 方向键" 在每个 phase 的语义未拆分

**问题**：v3 §14.3.1 补充规则 1："phase 内部切换优先消费 `Tab / Shift+Tab / 数字键 / 方向键`"；但：
- `ACTION phase` 里数字键 = 选第 N 个 action；`Tab` = 高亮下一个 action；方向键 = 在 action 列表里上下移动？
- `METHOD phase` 同理；
- `TARGET phase` 里数字键 = 选第 N 个合法 target；`Tab` = 高亮下一个合法 target；方向键 = 自由移动 targeting cursor 还是跳跃到下一个合法 target？

这三套键位在三个 phase 下的语义不能用一句话覆盖。

**建议**：§14.3.1 在补充规则 1 之后补小表：

| phase | 数字键 | Tab | 方向键 |
| --- | --- | --- | --- |
| `ACTION` | 选第 N 个 action | 下一个 action（循环） | 上下滚 action list |
| `METHOD` | 选第 N 个 method | 下一个 method（循环） | 上下滚 method list |
| `TARGET` | 跳到第 N 个合法 target | 下一个合法 target（循环） | 自由移动 targeting cursor（保持 vanilla 语义） |

### P1-13 · §5.4 `UiErrorPayload` 字段类型与 `<build-hash>` 源未定义

**问题**：
- `heading / detail / contextKeyValuePairs` 三个字段是：
  - i18n key（`String` + 运行时解析）
  - 还是已解析文本（`String`，玩家 locale）
  - 还是 `TextToken`（与 `RenderTextTokenSnapshot` 一致）？
- `[ktome/<build-hash>]` 的 `build-hash` 从 `BuildConfig.gitSha` / `manifest.version` / `build.gradle.kts` 注入哪个？

**建议**：§5.4 第 4 条补：
> `UiErrorPayload.heading: String` 和 `detail: String` 是玩家当前 locale 已解析的纯文本（来自 `localizer.text(key, *args)`），不保留 i18n key。
> `contextKeyValuePairs: List<Pair<String, String>>`，key 为 debug 标签（英文，不做 locale），value 为解析后的文本。
> `<build-hash>` 取 `BuildConfig.GIT_SHORT_SHA`（由 `build.gradle.kts` 在 `processResources` 阶段注入；若注入失败，回退为 `unknown`）。

### P1-14 · §5.2a `VALIDATION` 行 owner 表达与真源实际层级不匹配

**问题**：v3 写 "VALIDATION | 开发态 | `ValidationCommandSource`"，但真源 `ValidationCommandSource.kt:124` 是 `class ValidationCommandSource(...) : CommandSource by delegate`，其 `delegate = InputHandlerCommandSource(InputHandler(...))`。真正处理 `VALIDATION` mode 的 polling 是 **内嵌的 `InputHandler.pollValidationCommand`**；`ValidationCommandSource` 只负责：
1. 创建 InputHandler 时传入 `validationOverlayAvailability = ENABLED`
2. 在 `overlayState()` 里 enrich `validationPanel`（line 143）

**建议**：§5.2a `VALIDATION` 行 owner 改为："`ValidationCommandSource`（delegate 包装 `InputHandler.pollValidationCommand`；enrich `ValidationOverlayPanelState`）"。这样 PR-03 重写时就不会误以为 validation 的键位处理也要搬离 InputHandler。

### P1-15 · §9.3.1 truth table 缺 VALIDATION mode 条目

**问题**：§9.3.1 完全没写 VALIDATION mode 下的任何键位。即使 VALIDATION 是开发态，只要它在 `UiMode` 枚举里存在，PR-03 重写就会面对它。

**建议**：§9.3.1 补：

| UiMode / Frame | 键 | 期望行为 | 备注 |
| --- | --- | --- | --- |
| `VALIDATION` | `ESC / F9` | close validation overlay | 真源 `pollValidationCommand` line 504 |
| `VALIDATION` | `WASD / 方向键` | 切 section / action（纵横分轴） | 真源 line 509-532 |
| `VALIDATION` | `I / J / K / L` | 无（§1 指出的废除项） | PR-03 清理后 |
| `VALIDATION` | `Enter / Space / E` | 提交当前 validation action | 真源 line 552-568 |

### P1-16 · §7.3 task 1 token 命名空间只给了 `color.*` 示例，其他类别完全没示例

**问题**：v3 §7.3 task 1 给了：
- 颜色 token：`color.quality.*` / `color.telegraph.*`（示例齐全）
- 字号/数字 token、间距 token、动效 token：**没有任何示例 key**
- a11y token（focus ring / high contrast text / disabled alpha）：**同样没有示例 key**

PR-01 落地时必然要拍一组命名空间。拍坏了整个 `UiDesignTokens` 会被二次重写。

**建议**：§7.3 task 1 补一个**建议命名空间**（不强制，但冻结一份默认）：

```
color.quality.normal / color.quality.magic / color.quality.rare
color.telegraph.low / color.telegraph.moderate / color.telegraph.high / color.telegraph.lethal
color.text.primary / color.text.secondary / color.text.disabled
color.accent.special.unique / color.accent.special.artifact

spacing.1 / spacing.2 / spacing.4 / spacing.8        // 4dp 基线倍数
typography.body.size / typography.body.line
typography.title.size / typography.caption.size
motion.fast.ms / motion.medium.ms / motion.slow.ms

focus.ring.color / focus.ring.width
alpha.disabled / alpha.overlay.dim / alpha.overlay.glass
radius.sm / radius.md / radius.lg
stroke.thin / stroke.medium / stroke.thick
```

---

## 4. P2 · 更细粒度的漂移与可松绑条款（15 条）

### P2-1 · §6.1 `PR-05 -> PR-07` 弱前置条款在串行前提下语义冗余

**问题**：§6 已强制串行（"当前 PR 未完成前，不并行推进下一 PR"），因此 PR-05 物理上必然早于 PR-07；"弱前置"语义被串行覆盖。

**建议**：把这条弱前置改写为一条**共享模型冻结条款**：
> "PR-07 的 `ExplainPane` 必须复用 PR-05 冻结的 `ModalCardModel` DOM，禁止自造第二套 inspect-specific 卡片模型。若发现共用不可行，暂停 PR-07 并回到 PR-05 扩展 `ModalCardModel`。"

### P2-2 · §5.2 `F`（过渡期）行脚注位置

**问题**：§5.2 表下脚注 2 "`F` 在根地图态不再承担等待或帮助语义"放在附注第 2 条，但真正触发行为变更的是同表第 9 行；若直接把"legacy alias"加到该行备注列里，读者不需要再扫到脚注。

**建议**：§5.2 `F`（过渡期）行新增一个"备注"列，或者把脚注 2 的内容直接写进行内"原则"列里。

### P2-3 · §5.2b "X 废除非 inspect 下移职责" 真源比 v3 写的更广

**问题**：v3 §5.2b `X` 行写"保留 MAP→INSPECT；**移除非 inspect 的下移职责**"。但真源里 `Keys.X` 作为"下移"不仅出现在 SHOP / WORLD_MAP，还出现在：
- `pollInventoryCommand` line 589
- `pollValidationCommand` line 513
- `pollLoadoutCommand` line 468
- `pollTalentAssignCommand` line 682

**建议**：§5.2b `X` 行备注补："废除范围：`SHOP / WORLD_MAP / INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / VALIDATION` 中所有把 `Keys.X` 当作下移的分支；PR-03 清理时统一用 `DOWN / S / NUMPAD_2`。"

### P2-4 · §10.3 task 6 corner marker / badge / corner glyph 术语不统一

**问题**：v3 里出现三个相近术语：
- §10.3 task 3（品质 table）："边角标 glyph"
- §10.3 task 6（地面掉落）："corner marker / badge"
- §10.3 task 2 map 表："无角标 / ◆ / ◆◆"

这些都在指同一层"位于 tile 角落的小装饰图形"，但三种命名容易让人反复确认是不是同一个东西。

**建议**：全文统一为 "corner glyph"（rarity 轴）/ "corner badge"（数量 / special accent 叠加标识）两个术语，分别承担不同职能。

### P2-5 · §14.3.1 "Backspace → ESC 等价" 表达含糊

**问题**：phase 状态图写 "ACTION phase Backspace -> ESC 等价"。"ESC 等价"作为行为描述略含糊——是"pop frame 回 MAP"还是"和 ESC 走同一 code path"？

**建议**：直接写 "ACTION phase Backspace -> pop frame 回 MAP（与 ESC 同效）"。

### P2-6 · §14.3.1 "数字确认动作" 表达层级不一致

**问题**：phase 状态图 `ACTION` 分支写"Enter / 数字确认动作 → METHOD"，可能被误解为"按数字键就同时完成选择+确认"。而实际设计应该是：数字键 = 选中 + 高亮，Enter 才是确认（避免误触）。

**建议**：改为：
```
ACTION
  数字键 N -> 选中第 N 个 action（高亮，不 transition）
  Enter -> 确认当前高亮 action -> METHOD（或若无可选方式 -> TARGET）
  Backspace / ESC -> pop frame 回 MAP
```

（与 §5.2 `1-9 | 对应动作/方式/目标编号` 的 "直达"定位略冲突——若 PR-08 决定"数字键直达"，那么这里也要同步写清。）

### P2-7 · §16.2 遗留 golden label 迁移口径未定

**问题**：v3 §16.2 第 2 条"命中本计划的 UI 变更场景，golden label / save-folder-name 统一使用 `phase4-uiux-prNN-*` 前缀"。但仓库里现有 golden label 可能已经是 `phase2-*` / `phase3-*` 前缀（从 `GoldenScreenshotHarnessTest.kt` 看已经有多个固定分辨率场景）。

**建议**：§16.2 补一条："遗留 `phase2-* / phase3-*` golden label 保持不动；只要求本计划**新增或命中重录**的 label 使用 `phase4-uiux-prNN-*` 前缀；若重录命中某个遗留 label，必须同步 rename。"

### P2-8 · §11.3 task 3 "不允许长期遮罩掩盖真实输入阻塞" 与 §5.4 重复

**问题**：§5.4 第 3 条已经写了 "加载态必须是短时过渡，不允许在加载态下阻塞玩家输入超过一次明显交互节拍"；§11.3 task 3 第 3 个 bullet 又写 "不允许用长期遮罩掩盖真实输入阻塞"，含义完全相同。

**建议**：删 §11.3 task 3 第 3 bullet，改为引用 §5.4："规则同 §5.4 加载态条款。"

### P2-9 · §7.3 task 4 "窗口标题改回会话信息或纯应用名" 二选一未决

**问题**：v3 留了"二选一"，单人开发前提下应该选具体一套，否则 PR-01 会拍脑袋。

**建议**：§7.3 task 4 改为：
> "窗口标题格式固定为 `K-ToME · <locale> · <seed>[· <save-slot>]`（seed 与 slot 存在时展示；不再展示键位说明或 '[VALIDATION]' 等开发态 marker）。若在 release 构建里 seed / slot 不可用，回退为纯 `K-ToME`。"

### P2-10 · §5.2a deferred stub 不说清是否触发 lint

**问题**：§5.2a 补充约束 2 说"ITEM_COMPARE 允许先以 deferred stub 预留"。但 §16.5 第 9 条说"禁止留'已 lint 但无测试引用'的孤儿 key"。deferred stub 是否算孤儿？

**建议**：§5.2a 补充约束 2 末尾加："deferred stub 不被视为 §16.5 的孤儿 key；但 stub frame kind 必须在 `ModalStack` 里有一个专门的 `ModalFrame.kind == ITEM_COMPARE` 分支（返回 no-op frame），并在 `ModalStackTest` 里冻结其深度语义。"

### P2-11 · §5.2 `.` / `Space` / `Numpad5` 合并成一行会掩盖语义不对称

**问题**：见 P1-7，`Space` 在 modal/targeting 态是 confirm alias，而 `.` / `Numpad5` 不是。把三个键合并成一行的风险是读者默认"三者行为完全等价"。

**建议**：§5.2 拆成两行：`.` / `Numpad5` 一行（严格 wait），`Space` 一行（MAP=wait / 其他=confirm alias）。

### P2-12 · §13.3 task 4 "新增 keyword 一致性 lint" 未命名

**问题**：§13.3 task 4 说 "优先围绕现有 `KeywordRegistry` 做一致性校验"，但没给 lint 的 gradle task 命名建议，PR-07 落地时还要自己拍。

**建议**：§13.3 task 4 结尾加："建议 gradle task 名 `keywordRegistryLint`，owner 归 `tools/lint/`；核心规则：`DescriptionPresenter` 实际消费的 keyword id 必须在 `KeywordRegistry` 中存在；反向地，`KeywordRegistry` 定义的 keyword id 必须至少被一个 `DescriptionModel` 引用。"

### P2-13 · §8.3 task 4 "新老玩家差异化首屏" 漏了 "存档损坏" 第三态

**问题**：§8.3 只列"首启态 / 有存档态"两个 state。但单人开发期极容易出现"存档因 schema migration 失败 / manifest 版本漂移而加载失败"的第三态（见 §5.4 错误态要求 save 恢复失败时给出返回路径）。MainMenu 也应该能识别这种态。

**建议**：§8.3 task 4 补 "存档损坏 / 无法加载 态"：
> "若 `saveDirectory` 下存在 save 文件但加载失败，MainMenu 首屏应把 `继续游戏` 置灰并附上 `ui.menu.continue.corrupted.detail` 文案；玩家可以按 F3（或等价键）进入 error payload copy 流程（与 §5.4 复用 `Copy Error Detail`）。"

### P2-14 · §5.2 `Ctrl+S` 行 "targeting / 三层决策态 | 禁用" 含义未定

**问题**："禁用"可能有两种解释：
- A. 按 Ctrl+S 无响应（no-op）
- B. 按 Ctrl+S 会 block + 给出 "当前 phase 不允许保存" 提示

**建议**：§5.2 `Ctrl+S` 行第 3 列改为："阻断并 toast `ui.message.save.blocked-in-combat-decision`（不 no-op 以避免玩家怀疑自己按错键）"。

### P2-15 · §16.5 第 8 条 "哪些 key 仍走 fallback" 报告无结构要求

**问题**：§16.5 第 8 条说"若资源因外部前置条件暂时未生成，PR 中必须明确：哪些 key 仍走 fallback / 为什么当前 fallback 仍可用 / 真正开放该 UI 路径前还缺哪一步资源交付"；但报告格式（自由文本？yaml？表格？）未定。

**建议**：§16.5 第 8 条补："报告格式固定为 PR description 里一个名为 `Resource Fallback Audit` 的 markdown 小表（列：`key / fallback-visualKey 或 fallback-audioCueId / 失效风险等级（low/medium/high）/ 补交付 unblock task`）。表若为空也要写'全部真实资源已落地'。"

---

## 5. PR 间依赖与估时的再复核

### 5.1 依赖矩阵

v3 §6.1 的硬前置 6 条 + 弱前置 2 条 **结构完整**。见 §4 P2-1，`PR-05 -> PR-07` 弱前置建议改为"共享卡片模型冻结条款"。

### 5.2 估时

v3 的 `17-22 人日` 复核：R2 已将估时从 v2 的 `15-21` 推到 `17-22`；v3 如果吸收本轮 P1-1/P1-2/P1-6/P1-7/P1-13/P1-15/P1-16 后，PR-03 / PR-04 / PR-08 的实际工作量仍在该区间内，不建议再上推。

| PR | v3 估时 | R3 复核 | 说明 |
| --- | --- | --- | --- |
| PR-01 | 2 | **2-2.5** | 吸收 P1-16 token 命名空间首发后略扩 |
| PR-02 | 1 | 1 | 保持 |
| PR-03 | 4-5 | 4.5-5.5 | 吸收 P1-1/P1-2/P1-6/P1-7/P1-15 后 truth table 行数从 ~20 扩到 ~40 |
| PR-04 | 2.5-3 | 2.5-3 | 保持（P1-8 只要求把一个表拆成两个） |
| PR-05 | 1.5-2 | 1.5-2 | 保持 |
| PR-06 | 1.5-2 | 1.5-2 | 保持 |
| PR-07 | 1.5-2 | 1.5-2 | 保持 |
| PR-08 | 2.5-4 | 3-4 | 吸收 P1-10/P1-11/P1-12 后的 phase 语义细化略扩下限 |

**新总区间：`17.5-22.5 人日`**（中位数 ≈ 20 人日，单人开发 3-4 周）

---

## 6. 建议本轮优先吸收的条目（ROI 最高）

如果 lky 只想在 v3 基础上再做一次窄 delta 更新（<100 行文档体量），建议优先吸收：

1. **§1**（R2 继承的 I/J/K/L → VALIDATION 纠正）—— 防止 PR-03 去删一段根本不存在的代码
2. **P1-1/P1-2/P1-15**（Tab / TALENT_ASSIGN / LOADOUT_EDIT / WORLD_MAP / STAT_ASSIGN / VALIDATION 的 truth table 行）—— 把 §9.3.1 从 "最小覆盖" 升级成 "真正的 InputHandlerTest 冻结面"
3. **P1-3/P1-7**（`F` / `Space` 在 TARGETING / modal 的准确行为）—— 修掉两个 §5.2 与真源冲突的小错
4. **P1-5**（`MAP | 1-9` inscription vs talent 分流优先级）—— 一行备注避免一次真源翻查
5. **P1-8**（QualityPresentation 拆成 rarity + special accent 两维表）—— 防止 PR-04 把 UNIQUE/ARTIFACT 当第 4/5 档 rarity
6. **P1-9**（`?` 键在 INSPECT frame 下优先 ExplainPane）—— 防止 PR-03 与 PR-07 的 `?` 行为互覆盖
7. **P1-10/P1-11/P1-12**（CombatDecisionFrame 的"无可选方式触发条件 / 无合法目标 / Tab 各 phase 循环对象"）—— PR-08 落地时这 3 个问题迟早会出现
8. **P1-13**（`UiErrorPayload` 三字段类型与 `<build-hash>` 取值源）—— 防止 PR-05 把 `heading` 当 i18n key 还是当文本纠结
9. **P1-16**（非 color 类 token 示例命名空间冻结）—— 防止 PR-01 拍一份会被 PR-06 重命名的 token 命名

剩下的 P2 条目都是"修掉让文档更干净"，不修也不阻塞开发。

---

## 7. 结论

### 7.1 总体

**v3 已经是一份可直接用于 PR-01 开工的执行手册。** R2 的 26 条建议（4 P0 + 11 P1 + 9 P2 + 2 依赖项）几乎 100% 吸收；v3 对 §5.2 / §5.2a / §5.2b / §5.4 / §5.5 / §5.6 / §7.3 / §9.3 / §10.3 / §11.3 / §13.3 / §14.3 / §16.2 / §16.5 都做了结构性充实。

R3 找到的问题可以分成两档：

- **必须修（P1 + §1 纠错）**：共 17 条，几乎全是"表格缺一两行 / 某个名词没给示例 / 某个 phase 转移条件没定义" 级别的执行层精度盲点。
- **可以不修（P2）**：共 15 条，都是术语漂移 / 文字冗余 / 单人前提可松绑条款。

### 7.2 v3 相对 v2 的主要进步（值得肯定）

1. `§9.3.1 InputHandler truth table` 已经把 "vs v2 开发者需要自己推断 InputHandlerTest 怎么写" 的部分收口
2. `§14.3.1 CombatDecisionFrame phase 状态图` 是整份文档最精致的部分之一
3. `§5.2a` 的 UiMode owner 归属表 + `ModalFrame.kind` 列表冻结，几乎完全实现了 R2 期待的"ModalStack 与 UiMode 语义解耦"
4. `§10.3 QualityPresentation 表 + ItemIconKeyCoverageRule 独立 task` 把"品质合同 vs iconKey 覆盖 vs 地图掉落排序"三件事一次性冻结
5. `§5.4 UiErrorPayload + force-switch toast` 把错误态 / 被动态切换这两个 UX 边缘 case 显式化

### 7.3 下一步建议

1. **单人开发前提下的最优路径**：直接在 v3 同文件做 in-place 吸收（只吸收 §6 列出的 9 条优先项），不开 v4 文档；约 60-80 行文档 delta。
2. PR-01 可以**在吸收 §6 优先项之后**直接开工；不吸收也能开工，但 PR-03 的 `InputHandlerTest` 和 PR-04 的 `QualityPresentation` 会在执行时至少停 2 次补表。
3. R3 本身不建议再次触发"写 v4 全文" —— 文档体量已经在 1500 行边缘，继续扩不如沉到代码里。

### 7.4 一句话总结

**v3 已经把能在文档层抓到的精度都抓回来了；R3 抠的只是"每段小表再少一次重新打开 InputHandler.kt 去数行数" 级别的微优化**。从"开发手册"升级到"冻结的执行合同"，差的不是文字，是代码 commit。
