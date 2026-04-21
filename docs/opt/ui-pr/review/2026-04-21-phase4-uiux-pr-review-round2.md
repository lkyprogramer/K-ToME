# Phase 4 UI/UX PR 级开发文档 Review（第二轮）

**被审对象**: `docs/opt/ui-pr/` 下 5 份合并后 PR 文档（`pr01` ~ `pr05`）、`README.md`、`manual-records/_template.md`  
**审阅日期**: `2026-04-21`  
**审阅者**: Claude（基于第一轮 review `2026-04-21-phase4-uiux-pr-review.md` 之后的文档增量）  
**结论**: 整体比第一轮有明显收敛，跨 PR deferred 收口表、golden label 所有权、人工白盒模板均已落地。但在 **token 面与迁移清单一致性、`UiErrorPayload` 跨 PR 字段映射、truth table Ctrl+S 行覆盖、`CombatDecisionFrame` push 触发条件、priority 公式、`ExplainPane` 与 `ModalCardModel` 的家族矛盾** 等 9 条 P0 项上仍然不具备"可直接发起 PR"的精度。建议按 §4 优先级清单至少解完 P0 再开工。

---

## 0. 与第一轮 review 的差异确认

第一轮提出 33 条具体问题 + 7 条通用问题。本轮确认已经解掉的主要项（不再重复列入问题清单）：

1. README 已建立 `跨 PR Deferred 与收口表`（`ITEM_COMPARE / COMBAT_DECISION / ExplainPane / BuildInfo.shortHash / UiEmptyState`）。
2. README 已建立 `Golden Label 所有权` 表和迁移规则。
3. README 已建立 `人工白盒记录` 指向 `manual-records/_template.md`，模板包含 6 类信息。
4. PR-01 §4.4 `BuildInfo.shortHash` 已明确在本 PR 直接落地，`unknown` 不再作为长期占位；失败回退加 warn。
5. PR-02 §4.1 deferred frame 临时行为已列 5 条；`Ctrl+S` 规则分 mode 明确；§4.3 加了 ExplainPane forward-compatible note。
6. PR-03 §4.2 `qualityTierId` 排序权重表已冻结；§4.4 `contextKeyValuePairs` 按 builder 插入顺序；§4.5 加载态预算 ~200ms + 500ms 软断言。
7. PR-04 §4.1 priority 矩阵已出，§4.6 认领三个 `AccessibilityToggle` 开关，§4.5 `keywordRegistryLint` 声明为本 PR gate。
8. PR-05 §2.1 单一 method 跳过后 `Backspace` 回 `ACTION`；§4.4 `ui.message.save.blocked-in-combat-decision`；§4.3.1 明确 Contract 扩张触发条件；§6.3.6 非目标核查列出。

以下问题是本轮新发现或第一轮仍未解干净的。

---

## 1. 通用 / 跨 PR 残留问题

### 1.1 tokens 面与首批迁移清单不一致

**发生于**: PR-01 §4.1 vs §5.3  
**问题**: §4.1 列的 `UiDesignTokens` 最小 token 面只有 `color.quality.* / color.accent.special.* / color.telegraph.*`，没有 `color.status.*`。但 §5.3 首批迁移清单要求 `TileRenderer.statusAccentColor(BUFF/DEBUFF/NEUTRAL)` 消费 `UiDesignTokens.color.status.buffAccent / debuffAccent / neutralAccent`，以及 `statusBadgeColor(...)` 消费 `UiDesignTokens.color.status.badge.*`。  
**后果**: 实施时要么 §4.1 token 面需要补全，要么 §5.3 迁移目标命名落空；两处一定要有一处改写。按 PR-01 "token 只属于 client" 原则，应当在 §4.1 增补 `color.status.buff/debuff/neutral/telegraph-badge/zone-badge` 等完整子面。  
**建议修订**: §4.1 在 1-12 之外补第 13 项 `color.status.buffAccent / debuffAccent / neutralAccent / badge.stack / badge.turns / badge.cap`，与 §5.3 对齐。

### 1.2 `UiErrorPayload` 跨 PR 字段映射悬空

**发生于**: PR-01 §4.5 vs PR-03 §4.4  
**问题**: PR-01 §4.5 列 payload 字段 `heading / detail / savePath / reasonCode / gameVersion / [ktome/<build-hash>]`（具体命名字段）；PR-03 §4.4 把 payload 形式化为 `heading / detail / contextKeyValuePairs / build-hash`（通用键值对）。两处字段形态不等价。  
**后果**: PR-01 实施时会以具体字段写 formatter；PR-03 落地时又要重做一次 builder。`contextKeyValuePairs` 的插入顺序约束只对 PR-03 有效，PR-01 的 4 个具体字段的顺序没被 PR-01 自己冻结，会在 PR-03 迁移时静默改顺序，导致现有测试/issue 模板失稳。  
**建议修订**: PR-01 §4.5 直接把"复制格式"语法改为 PR-03 的 `contextKeyValuePairs` 形态（`savePath / reasonCode / gameVersion` 作为 3 条 contextKeyValuePairs），并在 PR-01 就冻结插入顺序 `savePath -> reasonCode -> gameVersion`；PR-03 §4.4 补一行"PR-01 `Copy Error Detail` 的 3 条 context 必须保持此顺序，后续 PR 追加 context 必须 append 到末尾"。

### 1.3 truth table Ctrl+S 行覆盖不全

**发生于**: PR-02 §4.3  
**问题**: §4.3 truth table 10 个 mode 中，`INSPECT / SHOP / WORLD_MAP / STAT_ASSIGN / TALENT_ASSIGN` 行没有列 `Ctrl+S`；`Ctrl+S` 规则只显式列了 `MAP / INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN / INSPECT / TARGETING / COMBAT_DECISION`（把 TALENT_ASSIGN / INSPECT 列为"保存并保持"，但 truth table 对应行却缺 `Ctrl+S` 列值；SHOP / WORLD_MAP / STAT_ASSIGN / VALIDATION 的 Ctrl+S 行为未声明）。  
**后果**: `InputHandlerTest` 基于 truth table 断言时，SHOP / WORLD_MAP / STAT_ASSIGN / VALIDATION 的 Ctrl+S 行为落空；实现侧会随手选择 save 或 blocked，行为漂移。  
**建议修订**: truth table 10 行全部补 `Ctrl+S` 列；被动态接管期间（SHOP / WORLD_MAP / STAT_ASSIGN）是否允许保存必须在 §4.3 明确，VALIDATION 的 Ctrl+S 行为也要明确（大概率阻断 + toast `ui.message.save.blocked-in-validation`）。

### 1.4 `ModalStack.push > 3` 的"fail fast 或按文档压平"二义

**发生于**: PR-02 §4.1 规则 1  
**问题**: 原文"`push` 超过深度 `3` 时 fail fast 或按文档压平，不能静默套娃。" fail fast（throw）和压平（替换 top / 丢弃新 push）语义相反，没给选择条件。  
**后果**: 实施时会走其中一种；测试如果不统一会出现 red/green 漂移。  
**建议修订**: 二选一。推荐"fail fast + throw IllegalStateException"配合 "client 代码保证每个入口都有合法 push 路径"。如果押"压平"，必须定义压平时是丢弃栈底还是栈顶、是否记录日志、是否 toast。

### 1.5 `ExplainPaneModel` 与 `ModalCardModel` 存在家族矛盾

**发生于**: PR-04 硬依赖条款 2 vs §3.1 / §4.3 / §4.4  
**问题**:  
- 硬依赖条款 2: "PR-03 的 ModalCardModel 是 ExplainPane 卡片结构来源，本 PR 不新增 inspect-only 卡片家族。"  
- §3.1 新增: `client/src/main/kotlin/com/ktome/client/ui/inspect/ExplainPaneModel.kt`  
- §4.3 第 4 条: "输出结构必须能被 ModalCardModel 或 ExplainPaneModel 消费"  

`ExplainPaneModel` 实际就是"inspect-only 卡片家族"。声明不新增家族但又新增 model 本身，自相矛盾。  
**后果**: 实施者只能选一种。选 A（ExplainPane 直接复用 ModalCardModel）时，§3.1 新增文件清单需要删 `ExplainPaneModel.kt`；选 B（保留 ExplainPaneModel）时，硬依赖条款 2 的表述需要改成"`ExplainPaneModel` 必须通过 `ModalCardModel` 可选字段组合实现，不新增 rewardLines / costLines 以外的字段族"。  
**建议修订**: 明确选 B，理由是 ExplainPane 字段面（例如 keyword chips、lore 段、关键词引用链）超出事件/商店卡片语义；但要在 §4.3 加一条"`ExplainPaneModel` 是 `ModalCardModel` 的 composition wrapper，不能持有 renderer 绕过 `ModalCardModel` 直接画文本的字段（如裸 `String` 说明段）"。

### 1.6 priority / danger 公式存在未定义量

**发生于**: PR-04 §4.1  
**问题**: priority 矩阵含 `previewTurnsInverse / dangerLevel / dangerWeight / remainingTurnsWeight / stackWeight`，但只有后两者给出了截断区间（`0..20` / `0..10`）。`previewTurnsInverse` 是"剩余多少回合开预警"还是"已过去多少预警回合"的反序？`dangerLevel` 的整数编码（`LOW=1 / MODERATE=2 / HIGH=3 / LETHAL=4`？）？`dangerWeight` 与 `dangerLevel` 的关系？  
**后果**: 实施者只能猜。`StatusPresentationModelTest` 的排序断言被迫跟着实现硬编码，回归时发现问题为时已晚。  
**建议修订**: 在 §4.1 下方加公式：  
- `dangerLevel: { LOW=1, MODERATE=2, HIGH=3, LETHAL=4 }`；  
- `previewTurnsInverse = max(0, previewTurnsCeiling - previewTurnsRemaining)`（`previewTurnsCeiling` 取 5）；  
- `dangerWeight = dangerLevel * 10`；  
- 同优先级 tiebreaker 为 `typeId` 字典序（stable）。

### 1.7 `CombatDecisionFrame` 的 push 触发条件未定义

**发生于**: PR-05 §4.4 第 1 条  
**问题**: 原文"InputHandler 在需要战斗决策时 push `COMBAT_DECISION` frame"——"需要战斗决策时"的触发信号没定义。是玩家按某个入口键（例如 `A` = attack / 1-9 = action slot）？是 snapshot 出现特定 fact（legalActions > 0 & hp threshold）？还是规则层发出 event？  
**后果**:  
1. 没有入口键，PR-05 与 PR-02 truth table 的耦合无法写定；  
2. 如果是规则层 event，则属于"被动态"一类，但 PR-02 §4.5 被动态只列了 `WORLD_MAP / SHOP / STAT_ASSIGN`，combat decision 的 active stack 清栈语义没覆盖；  
3. 目前玩家 `UiMode.TARGETING` 进入路径会被 combat decision 完全替代，但 PR-02 §4.3 truth table 第 5 行 `TARGETING` 仍存在，未声明在 PR-05 后是否废弃。  
**建议修订**: PR-05 §4.4 第 1 条扩成：  
- 入口：玩家在 `MAP` 按"攻击键"或"施法键"时，若 `legalActions >= 1` 则 push；否则 toast "当前无可用动作"；  
- 被动态联动：在 active modal 栈非空（INVENTORY / LOADOUT_EDIT / TALENT_ASSIGN 打开）时先要求 ESC/Backspace 退出，再允许进入 combat decision；  
- PR-02 truth table 第 5 行 `TARGETING` 由 `CombatDecisionFrame.TARGET` phase 替代，PR-05 §8 需新增一条"PR-02 truth table 第 5 行转为 sub-truth-table 归 CombatDecisionFrameTest 覆盖"。

### 1.8 `CombatDecisionFrame` 状态机未列 `Ctrl+S`

**发生于**: PR-05 §4.2 vs §4.4 第 5 条 vs §2.1 第 7 条  
**问题**: §2.1 与 §4.4 都写 `Ctrl+S` 阻断并 toast，但 §4.2 状态机的 `ACTION / METHOD / TARGET` 三格 phase 内键位列表只列了数字键 / Enter / Space / Backspace / ESC，没有 `Ctrl+S`。`Tab / Shift+Tab` 也只出现在下一张小表，没放入 ACTION/METHOD/TARGET 状态转移块。  
**后果**: `InputHandlerTest` 生成时会直接 copy 状态机，漏写 Ctrl+S 和 Tab 行为。  
**建议修订**: §4.2 状态机三格 phase 都补两行：  
- `Ctrl+S -> 保持当前 phase + toast ui.message.save.blocked-in-combat-decision`  
- `Tab / Shift+Tab -> 当前 phase 高亮循环`（已有下方小表，但状态机块里要 ref）。

### 1.9 `ValidationPreset` 硬依赖以源文件路径锚定

**发生于**: PR-02/03/04/05 硬依赖条款  
**问题**: 4 份 PR 都写"`ValidationPreset.XXX` 当前已存在于 `game/src/main/kotlin/com/ktome/game/validation/ValidationSessionOptions.kt`"。如果 validation 模块 refactor 或 preset 搬家，硬依赖描述会静默过期。  
**建议修订**: 硬依赖改为"引用接口层"：`ValidationPreset.MAPGEN_DIFF / LOOT_LAB / BOSS_VARIANT` 在 `com.ktome.game.validation.ValidationSessionOptions` 枚举中存在即可，禁止固定源文件路径。

### 1.10 跨 PR "locale key 临时形态 -> 正式形态" 的清理路径缺失

**发生于**: PR-02 §4.5 `ui.inspect.empty.tile` vs PR-03 §4.7 `ui.empty.inspect.title`  
**问题**: README §跨 PR deferred 表只写"PR-03 迁移到 `UiEmptyState`"，但没要求"迁移完成后删除 PR-02 遗留临时 key"。两个 key 长期共存会污染 locale 文件，且 locale lint 不会报错。  
**建议修订**: README §跨 PR deferred 表新增一列"临时 key 清理"，本条目填"PR-03 合入时必须删除 `ui.inspect.empty.tile` zh-CN/en-US 条目；`localeLint` 加 deprecated-key 清单断言"。

### 1.11 人工白盒模板缺少 enforcement 与双人签收

**发生于**: `manual-records/_template.md`  
**问题**: 模板给了结构，但：  
1. "结论 `PASS/FAIL/BLOCKED`"只要求一人签；  
2. 没规定 record 必须 check-in 到 repo 里与 PR 一并提交；  
3. 没规定"截图/录屏必须保存到 repo path 或 git-lfs，日志 excerpt 必须标注行号"；  
4. 没规定"skipped golden + 人工截图"情况下的最小证据（截图 hash、时间戳、locale、seed、窗口分辨率必须齐全）。  
**建议修订**: 模板新增：  
- §7 "双人签收（可选）"：`记录人 / 复核人` 两栏；  
- README 第 3 条加粗"人工白盒记录文件必须随 PR 一并提交到 `docs/opt/ui-pr/manual-records/phase4-uiux-prNN-*.md`"；  
- 模板 §1 加"git commit hash"字段（现在只有 "Git commit / branch"，需进一步要求 `HEAD` sha）。

### 1.12 `maintainabilityLint` 触发判定与实际文件数冲突

**发生于**: README 第 6 条 vs PR-01/02/03/04/05 §3.1  
**问题**: README 规定"新增/删除/移动 Kotlin 文件 `>= 5` 触发 maintainabilityLint"。实际各 PR 新增文件数：  
- PR-01: 6 新增 + 8 修改 → 已触发  
- PR-02: 6 新增 + 5 修改 → 已触发  
- PR-03: 6~8 新增 + 9 修改 → 已触发  
- PR-04: 4 新增 + 9 修改 → 未到 5 新增，但修改面广  
- PR-05: 3 新增（可能 5 含 contract 扩张）+ 5 修改 → 边界  

但 §6.2 中：  
- PR-01 / PR-03 把 maintainabilityLint 放在"若满足硬判定时"条件分支，而事实上 §3.1 已经触发 → 应无条件；  
- PR-02 / PR-04 / PR-05 已将 maintainabilityLint 放入默认命令；  

**建议修订**: PR-01 §6.2 第二段改成无条件命令；PR-03 §6.2 的 maintainabilityLint 也改为无条件；PR-04 看"修改公共 sealed / interface / DTO"第二维判定，目前改到了 `core/talent/KeywordRegistry.kt` 与 `DescriptionModel.kt`，已触发，保持无条件即可。

### 1.13 golden label 清理缺"PR-02 侧自检"

**发生于**: README §Golden Label 所有权 vs PR-02 §7  
**问题**: README 写"若曾生成 combat-decision stub label，PR-05 上线时必须删除或重录到 `phase4-uiux-pr05-*`"。但 PR-02 §7.1 没规定"PR-02 如果真的录了 stub，必须命名为 `phase4-uiux-pr02-combat-decision-stub-*`"。也就是说 PR-02 的 stub label 名字是未约定的，PR-05 合并时无法写"删除哪个 label"的自动化规则。  
**建议修订**: PR-02 §7.1 或 §8 中新增："若 `ITEM_COMPARE / COMBAT_DECISION` deferred frame 录入 golden label，label 名必须以 `phase4-uiux-pr02-item-compare-stub-` / `phase4-uiux-pr02-combat-decision-stub-` 为前缀。PR-03/PR-05 收口时须按此前缀删除或重录。"

---

## 2. PR-01 · Client Foundation and Main Menu

### 2.1 §4.2 MapDominant 分辨率基线面太窄

"保证现有 `1280x800` 基线不爆版" 是唯一基线声明。实际运行时若玩家窗口是 `1024x768` 或 `1920x1080`，MapDominant 布局策略的降级规则没定义。人工白盒 §6.3 也只固定 `1280x800`。

**建议**: §4.2 新增"最小支持窗口尺寸 `1024x768`，小于此尺寸时 MapDominant 允许 fallback 到 `ModalOverlay` 并 warn，但本 PR 不实施 fallback（PR-02 再考虑）"；且 golden harness 是否跑多个分辨率也需明确（当前 `GoldenScreenshotHarnessTest` 是单分辨率）。

### 2.2 §4.3 `ContinueUnavailableReasonCode` 缺 UNKNOWN fallback

4 个 reason code 不覆盖"未知异常"。如果 save 加载抛出没分类的异常（例如 `SecurityManagerDenied / DiskFull / NullPointerException`），UI 要么崩要么 degrade 成某个已知 code。未定义 fallback 会导致"生产环境吞异常变 `CORRUPTED`"的隐性契约。

**建议**: 新增 `UNKNOWN`，key `ui.menu.continue.unavailable.unknown`，copy payload 中 `reasonCode = UNKNOWN` 时必须额外带 `throwableClass / throwableMessage` 两项 context。

### 2.3 §4.4 `BuildInfo.shortHash` 失败回退的断言口径不具体

"测试不得把 `unknown` 当成唯一稳定期望；只断言字段存在、格式合法和失败回退可观测。"——方向对，但没给"合法格式"的正则与"可观测"的 assertion：  
- 合法格式：`unknown | [0-9a-f]{7,40}`？  
- 可观测：`WarningEvent` 进入日志收集器（现有 client 日志框架是什么？）还是 `System.err` 直接 print？

**建议**: §4.4 补一行 "`BuildInfoTest` 断言 `BuildInfo.shortHash.matches(Regex(\"unknown|[0-9a-f]{7,40}\"))`；失败回退时 `TestLogCollector` 或等价机制中存在 level=WARN 且 message 包含 `BuildInfo.shortHash resolution failed` 的条目。"

### 2.4 §4.6 LWJGL title 线程切换 owner 未定

"若运行时更新 LWJGL title 存在线程约束，必须回到 render/UI 线程执行" —— owner（`Gdx.app.postRunnable` / 自建 executor）未定，实施时会随手 `Gdx.app.postRunnable { ... }`，但项目中是否已有"主线程 executor"抽象没人认领。

**建议**: §4.6 补一句"`DesktopLauncherTitleFormatter` 提供纯 formatter；LWJGL title 切换由 `GameApp` 在 `render()` 入口消费 formatter 结果，不使用 `Gdx.app.postRunnable`。"（或声明正式 executor）。

### 2.5 §4.7 locale 清单缺 `ui.menu.action.validation`

首页存在 `ValidationMode` 次级入口（§4.3 "validation mode 可键盘到达，但不能默认聚焦"），但 locale 表没列文案 key。实施时会写死字符串或额外塞 key。

**建议**: 清单加 `ui.menu.action.validation`，示例文本 `验证模式`。

### 2.6 §3.1 新增 `MainMenuSummaryModel` 与 §4.3 字段不完全一致

§4.3 给出的 `MainMenuSummaryModel` data class 有 6 字段（`primaryAction / continueAvailability / buildCapabilities / helpLines / localeLabel`），但没有 `buildSummary` 字段名；而 §5.2 第 3 条又说 `MainMenuScreen.textSnapshot()` 暴露 "build summary"。"build summary" 与 `buildCapabilities` 是同一物还是不同字段？

**建议**: 统一命名，`buildCapabilities` 或 `buildSummary` 二选一；`BuildCapabilityLine` 的字段最小 shape（`labelKey / valueTextKey? / disabled: Boolean`）在 §4.3 或 §5.2 中列清。

---

## 3. PR-02 · In-Game Info, Input, Modal, Look

### 3.1 §4.4 `?` 在 ExplainPane 未落地前的 stub 行为未定义

§4.4 第 5 条说"`?` 在 inspect frame 内优先打开 ExplainPane sub-view；PR-04 会补完整说明"。但 PR-02 阶段 `?` 是 no-op 还是 placeholder？truth table 第 4 行 `INSPECT: ... / ?` 只列键名，未列行为。

**建议**: §4.3 truth table 第 4 行 `?` 的 PR-02 行为明确为"no-op + log debug，不产生 renderer 可见变化"；PR-04 上线后按该 PR 规则切入。

### 3.2 §4.2 PaneFocusController "恢复 vs reset" 优先级

§4.2 规则 4 "modal 关闭后恢复打开前地图锚点"  vs 规则 5 "被动态接管并清空 active stack 时，地图锚点重置为 `WORLD`"。场景：玩家 `CHARACTER_ACTION` 聚焦 → 打开 INVENTORY → 期间被动态接管（stat-assign）→ 退出 stat-assign 后。按规则 5 锚点应已 reset 为 WORLD；规则 4 的"打开前锚点"是 CHARACTER_ACTION——两规则冲突。

**建议**: §4.2 加一条明确优先级："被动态接管导致的 reset 优先于 modal 关闭后的锚点恢复。即一旦 reset 发生，保存的"打开前锚点"被清空。"

### 3.3 §4.1 deferred frame push 测试断言不够严

§4.1 deferred frame 临时行为 5 条，但测试 owner 用的是 "`ModalStackTest` 或 `InputHandlerTest`"。"或"意味着只要一处覆盖就行。建议改 AND："`ModalStackTest` 覆盖深度；`InputHandlerTest` 覆盖 `Ctrl+S` blocked stub 和 `pollCommand` no-op"，避免 owner 互相甩锅。

### 3.4 §4.4 Look Mode 空态字段跨 PR 的迁移规则

第 4 条说 PR-02 阶段 `RenderTextTokenSnapshot("ui.inspect.empty.tile")`，PR-03 必须迁移到 `UiEmptyState`。但 `UiEmptyState` 三字段（`title / detail / primaryCta?`），PR-02 的临时 token 只够填 `title`。PR-03 迁移时 `detail / primaryCta` 是什么来源？

**建议**: §4.4 加 PR-03 迁移的最小 shape："PR-03 迁移时 `title = ui.empty.inspect.title`，`detail = ui.empty.inspect.detail`（PR-03 §4.7 locale 清单应补），`primaryCta = null`。"

### 3.5 §7.1 deferred stub label 命名

见 §1.13。PR-02 应约束 deferred frame 若入 golden，label 必须含 `-stub-` 子段。

### 3.6 §6.2 命令顺序

`./gradlew :client:test --tests ...` → `./gradlew :client:test --tests "GoldenScreenshotHarnessTest"` → `./gradlew localeLint contractLint maintainabilityLint`。lint 在最后，意味着 lint 失败已经浪费完整测试时间。虽然串行需求可能就是这样，但建议注释"lint 放最后是为了先跑行为 test；如果 CI 资源紧张可考虑 lint 前置"。

---

## 4. PR-03 · Item, Content Presentation, UI States

### 4.1 §4.1 `QualityPresentation` 表的 snapshot 字段命名歧义

表第 4/5 行用了混合判断 "`specialTemplateId != null + UNIQUE`"，但 `UNIQUE` 是 `SpecialTier` 枚举值。snapshot 里到底有没有 `specialTier: SpecialTier?` 字段？还是 `specialTemplateId` 本身就已经编码了 `SpecialTier`（snapshot 里只暴露 id，客户端查 template 推 tier）？  
§2.1 第 1 条"正式品质合同只有 NORMAL/MAGIC/RARE 与 UNIQUE/ARTIFACT"——两组枚举。但 `ItemRenderSnapshot` 的确切字段未列。

**建议**: §4.1 前加一行"假设 snapshot 暴露 `qualityTierId: String(NORMAL/MAGIC/RARE) + specialTier: SpecialTier?(UNIQUE/ARTIFACT) + specialTemplateId: String?`；本 PR 禁止新增 snapshot 字段，若上述字段缺一，必须回到 core 扩 contract，记入 §3.2 非目标之外的单独"上游修订点"。"

### 4.2 §4.2 GroundLootMarker 排序第 1 条"所有 special 优先"

"`specialTemplateId != null`" 排第一，意味着 NORMAL + special 优先于 RARE 非 special。从玩家视角，`RARE 非 special` 和 `NORMAL special` 谁该当 head icon 值得讨论。设计意图未述。

**建议**: §4.2 补一句"设计意图：special 稀有度更高，且 special accent 在地图 marker 上比 rarity 主色更显眼；若后续 UX 反馈 rarity 应优先，规则层改动在后续 PR 内。"

### 4.3 §4.3 `ModalCardAction` 枚举语义未定义

7 个枚举名列出，但触发条件与副作用未定义：  
- `Confirm` vs `EnterRoute` 在路线卡片上是否等价？  
- `ReadMore` 在哪些面用？与 ExplainPane 的关系？  
- `Buy / Sell` 是否要求 cost/reward 非空？  

**建议**: §4.3 后附表："| action | 触发键 | 适用面 | 副作用 |"列 7 项。

### 4.4 §4.5 "软断言"定义不明

"`ClientSmokeHarnessTest` 至少增加 `loading screen transitions within 500ms` 软断言。" 软断言在 JVM + Gradle 生态里没有标准语义：是 `soft-assert` 库、warn-not-fail、统计报告、stderr log？  
**建议**: §4.5 明确"软断言 = 记录到 `build/reports/client-smoke/loading-timing.jsonl`，不 fail test；连续 3 次 over 500ms 则下一次 CI 中置为 hard fail"，或简化为"测量并 log；PR description 附测量值，不作为 fail 条件"。

### 4.5 §4.6 lint 阻塞规则 3 "ModalCardModel 必填字段缺失"

Kotlin data class 的非 nullable 字段由编译期保证，lint 场景不存在"必填字段缺失"。这条阻塞规则所指的应是"locale key 引用的 token 不可解析 / icon key 不可解析 / 卡片没给 primaryAction 以外的有效 action"等运行时/内容层面问题。

**建议**: 阻塞规则 3 改为"`ModalCardModel` 的 `title / primaryAction` 不能为 empty token 或 `Close` / `Cancel` 以外无可走路径的状态"（即"卡片无有效下一步"）。

### 4.6 §4.5 `UiLoadingState.allowsCancel` cancel action 未定义

`allowsCancel: Boolean` 但 cancel 触发走什么 `ModalCardAction`？哪个 locale key？没定义的话每个加载态会各自实现。

**建议**: §4.5 加"`allowsCancel == true` 时必须配套 `ModalCardAction.Cancel`，locale key `ui.loading.cancel`。"

### 4.7 §6.2 maintainabilityLint 无条件化

见 §1.12。§3.1 文件数已触发硬判定，§6.2 应无条件。

### 4.8 §7.3 `Resource Fallback Audit` 模板缺失

"未生成正式资源时，PR description 必须包含 `Resource Fallback Audit`。" 模板不存在。

**建议**: 在 `docs/opt/ui-pr/` 下新增 `resource-fallback-audit-template.md`（或 inline 到 §7.3），列最小字段：`asset key / 请求面 / fallback 类型(text / placeholder icon) / 迁移计划 / 预期正式 PR`。

### 4.9 §4.7 locale 清单缺 `ui.empty.inspect.detail` 等 detail key

§4.7 7 个 key 都是 title/action，detail 完全没列。`UiEmptyState` 必需 `detail` 字段，运行时必须有 key。§4.7 下面说"允许实现时补充更细粒度 detail key"——但补什么 key、由谁检查 `localeLint`，没写规则。

**建议**: 要求补 `ui.empty.inventory.detail / ui.empty.shop.detail / ui.empty.inspect.detail / ui.empty.log.detail / ui.loading.cancel`，进入 `localeLint` 覆盖。

---

## 5. PR-04 · Status, Description, Readability

### 5.1 §4.1 priority 公式

见 §1.6。量符号未定义是 P0。

### 5.2 §4.1 `StatusPresentationGroup.TELEGRAPH` 投影改为强制

"推荐实现为 `TelegraphPresentationModel.toStatusPresentation()` 投影" 只是"推荐"。PR-04 先于 PR-05 落地，`TelegraphPresentationModel` 此时尚不存在。实施者可能先在 PR-04 建一个 compact 版 telegraph model，PR-05 再补完整版——这就是两真源。

**建议**:  
- 方案 A：PR-04 推迟 `StatusPresentationGroup.TELEGRAPH` 到 PR-05 再一起做；PR-04 的 StatusPresentationModel 只做 `BUFF / DEBUFF / NEUTRAL / ZONE_EFFECT` 四组，telegraph 暂走现有 `TelegraphRenderer`。  
- 方案 B：PR-04 内先写 `TelegraphPresentationModel` 的最小 shape（最小字段面与 PR-05 完整面向后兼容），并在 PR-04 §4.1 文档里冻结"PR-05 只 append 字段、不改已定义字段名"。  
推荐方案 B，但硬依赖条款必须说明。

### 5.3 §4.2 色盲 + ReduceMotion 组合的 fallback 未定

"色盲/高对比场景下必须靠形状、描边、badge 或节奏区分"中提到"节奏"。但 §4.6 `reduceMotion` 开启时不能用节奏。组合（`colorBlindSafe=on & reduceMotion=on`）下必须留 "形状/描边/badge" 三选。没显式组合表。

**建议**: §4.2 加矩阵：

| colorBlindSafe | reduceMotion | 可用区分手段 |
| --- | --- | --- |
| off | off | color + shape + motion + badge |
| on | off | shape + motion + badge |
| off | on | color + shape + badge |
| on | on | shape + badge (no color-only, no motion-only) |

### 5.4 §4.4 "上一 inspect sub-view" 语义不清

§4.4 第 4 条"`Backspace` 关闭 ExplainPane 或回上一 inspect sub-view"。inspect 内有多个 sub-view 吗（lore / keyword expand / ...）？如果有，sub-view 是否有自己的迷你 stack？

**建议**: PR-04 当前只做 ExplainPane 一种 sub-view；§4.4 第 4 条简化为"`Backspace` 关闭 ExplainPane sub-view"，删掉"回上一 sub-view"的暗示。如果未来有多 sub-view，再单独 PR 定义。

### 5.5 §4.5 `keywordRegistryLint` 实现成本与 classpath 依赖

lint 要同时访问 `core/talent/KeywordRegistry` 与 `client/ui/talent/DescriptionPresenter` 的消费点——tools/lint 模块是否已有对 client/core 的 classpath / 反射入口？没人评估。实施时可能发现 classpath 隔离导致只能做"扫描字符串引用"而不是"编译期符号解析"，回到半可靠状态。

**建议**: §4.5 补一段"lint 实现策略：优先尝试 KSP / KotlinPoet 编译期符号扫描；若 tools 侧未启用 KSP，退化为对 `DescriptionPresenter` 源码的 regex 扫描并在扫描覆盖面不全时 WARN（而非 ERROR）。实施时若发现 KSP 不可用，列 TODO 到 `docs/opt/ui-pr/follow-ups.md`。"

### 5.6 §4.6 Accessibility toggle 默认值、切换时机未定

3 个开关没默认值；运行期切换是否需要 re-render / restart 未定；持久化归谁（save？settings.json？debug flag？）未定。

**建议**: §4.6 补：  
- 默认全 `off`；  
- 切换入口暂为启动参数 `-Dktome.ui.a11y.highContrast=true` 等；  
- 运行期切换：当前 PR 允许"切换后下一次 `render()` 生效"；不要求全局 invalidate；  
- 不持久化到 save；PR-04 明确非目标。

### 5.7 §4.3 扩展入口与 §6.1 必测行为口径不一致

§4.3 第 5 条"combat action 入口本 PR 只要求 pure unit test 覆盖"；§6.1 第 4 条"`DescriptionPresenter` 服务 talent、item、shop、inspect、action"。"服务 action" 在 PR-04 是 pure unit 还是端到端？两处语气不一致。

**建议**: §6.1 第 4 条改为"`DescriptionPresenter` 服务 talent、item、shop、inspect；combat action 分支由 `DescriptionPresenterTest` 中的 pure unit case 覆盖，端到端由 PR-05 补"。

### 5.8 §4.7 locale 缺 badge 模板 key

badge 文本 `remainingTurns / xN / N/cap` 是否走 locale？如果 `×3` 和 `3/5` 是固定格式（数字+字面 glyph），跨 locale 是否需要 `ui.status.badge.stack / .turns / .cap` 三个模板 key？当前 §4.7 未列。

**建议**: 若决定不 locale 化，在 §4.1 badge 规则下补"badge 内容不走 locale，由 pure formatter 生成，形如 `x3` / `3/5` / `4t`"；若 locale 化，则 §4.7 补三个 key。

---

## 6. PR-05 · Telegraph and Combat Decision Surface

### 6.1 §4.4 push 触发条件

见 §1.7。P0。

### 6.2 §4.2 状态机 Ctrl+S / Tab 行为缺

见 §1.8。P0。

### 6.3 §4.2 ACTION phase "0 legal target" 的拒选

状态机"若 `action.methodOptions.size == 0` 则拒绝选中并保持 ACTION"只处理 `methodOptions = 0` 的情形。"`action` 有 method 但 `legalTargets = 0`" 的情形被推到 TARGET phase 才 toast。UX 上这种 action 应当在 ACTION phase 就显示 disabled / no-legal-target badge，而不是让玩家选完再被 toast 打回来。

**建议**: §4.2 ACTION phase 补"若 `legalTargetCountPreview(action) == 0`，action 显示 disabled；按下数字键/Enter 时保持 ACTION 并 toast `ui.message.combat.no-legal-target`"；`legalTargetCountPreview` 是 `ActionHintModel.legalTargetCount` 的前置计算版本。

### 6.4 §4.2 数字键越界

ACTION / METHOD / TARGET phase 内"数字键 N -> 选择第 N 个"。若 N > list.size，behavior？

**建议**: 补"若 `N > list.size` 或 `N > 9`，no-op + 无 toast（避免误按频繁提示）"。

### 6.5 §4.2 TARGET cursor 在非法格的 hover 反馈

"若尝试确认非法位置 -> 保持 TARGET + toast `ui.message.combat.illegal-target`"——cursor 移到非法位置是否立即高亮红色 / 显示 illegal badge？只到按下 Enter 才反馈会造成试错成本。

**建议**: §4.2 TARGET phase 补"cursor 悬停非法格时显示 `illegal` 红色边框（消费 `UiDesignTokens.color.telegraph.lethal` 或专用 token），不产生 toast；确认时才 toast。"

### 6.6 §4.3 `TelegraphLinkageHint` 最小 shape 未定

`telegraphLinkage: TelegraphLinkageHint?` —— 该类型在文档里没有字段定义。

**建议**: §4.3 后附一个 data class `TelegraphLinkageHint(val telegraphId: String, val dangerLevel: DangerLevel, val previewTurnsRemaining: Int)`，或至少声明"由 `TelegraphPresentationModel` 直接复用最小字段面，禁止 client 再建第二套"。

### 6.7 §5.4 tests 缺 direct unit test

新增文件 `CombatDecisionFrame.kt / CombatDecisionPanel.kt / ActionHintModel.kt` 没有对应 `CombatDecisionFrameTest / CombatDecisionPanelTest / ActionHintModelTest`。测试面全挂在 `InputHandlerTest / TileRendererCanvasTest / ClientSmokeHarnessTest / GoldenScreenshotHarnessTest`，粒度过粗。

**建议**: §5.4 补：  
- `CombatDecisionFrameTest`：phase state machine 转移覆盖（8 条 §4.2 状态机行）；  
- `CombatDecisionPanelTest`：render 输出对 `ActionHintModel` 各字段的可见性；  
- `ActionHintModelBuilderTest`：从 snapshot 构建的纯单测。

### 6.8 §6.3.6 非目标核查缺自动化

"目标卡中不出现 `下一步:` / `预测:` / `即将:` 等前缀；`AsciiRenderModel / TileRenderModel` 中搜索 `aiTypeId` 的消费点只用于类型标签，不作为 intent"——人工核查易遗漏。

**建议**: §5.4 tests 补 `AiIntentLeakRuleTest`（在 tools/lint 或 client test 内），扫描  
- locale 文件是否含 `下一步 / 预测 / 即将 / next action / predicted`（仅 boss telegraph allow-list）；  
- `TileRenderModel / AsciiRenderModel` 中 `aiTypeId` 读取点是否 annotate 为 `@TypeLabelOnly`。

### 6.9 PR-02 truth table TARGETING 行的 deprecation

见 §1.7 后半。PR-02 §4.3 truth table 第 5 行 `TARGETING` 在 PR-05 落地后由 `CombatDecisionFrame.TARGET` 替代。这个关系需要在 PR-05 §8 或 §4.4 显式说明。

**建议**: §8 出口门禁新增一条"PR-02 truth table 第 5 行 `TARGETING` 由 `CombatDecisionFrameTest` 中 TARGET phase tests 接管；`InputHandlerTest.TARGETING` 原用例保留但 re-target 到 frame phase 断言。"

### 6.10 §5.4 GoldenScreenshotHarnessTest 标签规则

"增加 `phase4-uiux-pr05-*` label" 但未列清单。参考 §6.3 证据列 5 项 label，应直接写进 §5.4 tests 下"golden label 清单"子段。

---

## 7. 跨 PR 交叉风险（新一轮识别）

| # | 风险 | 涉及 PR | 影响 |
| --- | --- | --- | --- |
| 1 | `UiErrorPayload` 字段 shape 在 PR-01 vs PR-03 不一致 | PR-01 §4.5 / PR-03 §4.4 | P0 Copy Error Detail 格式会在 PR-03 重做 |
| 2 | `StatusPresentationGroup.TELEGRAPH` 与 `TelegraphPresentationModel` 的建模顺序冲突（PR-04 先落） | PR-04 §4.1 / PR-05 §4.1 | 高 容易出双真源 |
| 3 | `ExplainPane + Backspace` 与 `INSPECT + Backspace` 优先级需 PR-02 truth table 修订 | PR-02 §4.3 / PR-04 §4.4 | 已在 PR-02 forward-compatible note 覆盖 |
| 4 | `CombatDecisionFrame` push 与 PR-02 被动态清栈规则的交互 | PR-02 §4.5 / PR-05 §4.4 | P0 清栈/force-switch 语义缺 |
| 5 | `ValidationPreset` 硬依赖锚点是源文件路径而非接口层 | PR-02/03/04/05 硬依赖条款 | 中 validation 模块 refactor 后过期 |
| 6 | `ValidationPreset.BOSS_VARIANT` 同 seed `20260412` 同时被 PR-04 与 PR-05 复用 | PR-04 §6.3 / PR-05 §6.3 | 中 PR-04 白盒记录在 PR-05 合入后会失效 |
| 7 | PR-02 `ui.inspect.empty.tile` 临时 key 在 PR-03 合入后未被清理 | PR-02 §4.5 / PR-03 §4.7 | 中 locale 文件污染 |
| 8 | PR-02 deferred stub golden label 命名未约束 | PR-02 §7.1 / PR-05 §8 | 中 PR-05 清理 stub label 缺自动化 |
| 9 | `ExplainPaneModel` 新增违反"不新增 inspect-only 卡片家族" | PR-04 硬依赖条款 2 / §3.1 | 高 家族判定口径自相矛盾 |
| 10 | PR-04 `combat action` 分支 pure unit + PR-05 端到端的并存 owner | PR-04 §4.3 / PR-05 §5.4 | 低 测试面容易被删 |
| 11 | `maintainabilityLint` 触发判定与 PR-01/PR-03 §6.2 条件分支冲突 | README §6 / PR-01 §6.2 / PR-03 §6.2 | 中 实际已触发但仍写在条件分支 |
| 12 | `?` 键在 ExplainPane 落地前的 stub 行为未写 | PR-02 §4.3 第 4 行 | 低 行为漂移 |

---

## 8. 优先级建议

### P0（合并前必须修）

1. **PR-01 §4.1 / §5.3** token 面不完整，与首批迁移清单不一致。  
2. **PR-01 §4.5 vs PR-03 §4.4** `UiErrorPayload` 字段 shape 跨 PR 不一致。  
3. **PR-02 §4.1** "fail fast 或按文档压平" 二义。  
4. **PR-02 §4.3** truth table 10 行的 Ctrl+S 列缺项。  
5. **PR-03 §6.2 / PR-01 §6.2** maintainabilityLint 条件分支与实际文件数冲突。  
6. **PR-04 硬依赖条款 2 vs §3.1** `ExplainPaneModel` 家族矛盾。  
7. **PR-04 §4.1** priority 公式未定义（`dangerLevel / previewTurnsInverse / dangerWeight`）。  
8. **PR-05 §4.4 第 1 条** `CombatDecisionFrame` push 触发条件未定。  
9. **PR-05 §4.2** 状态机未覆盖 `Ctrl+S / Tab / Shift+Tab`。  

### P1（合并前应修）

10. **PR-01 §4.3** `ContinueUnavailableReasonCode` 缺 `UNKNOWN` fallback。  
11. **PR-01 §4.7** locale 清单缺 `ui.menu.action.validation`。  
12. **PR-02 §4.4** `?` 在 ExplainPane 未落地前 stub 行为未定。  
13. **PR-02 §4.2** PaneFocusController "恢复 vs reset" 优先级未定。  
14. **PR-03 §4.1** `QualityPresentation` 表的 snapshot 字段命名歧义。  
15. **PR-03 §4.3** `ModalCardAction` 7 项语义未定义。  
16. **PR-03 §4.5** "软断言"语义不明。  
17. **PR-04 §4.1** `StatusPresentationGroup.TELEGRAPH` 与 PR-05 建模顺序冲突。  
18. **PR-04 §4.2** 色盲 + reduceMotion 组合 fallback 矩阵缺。  
19. **PR-05 §4.2** ACTION phase "0 legal target" 拒选未定义。  
20. **PR-05 §4.2** TARGET cursor 非法格的 hover 反馈未定义。  
21. **PR-05 §5.4** 缺 `CombatDecisionFrameTest / CombatDecisionPanelTest / ActionHintModelBuilderTest` direct unit test。  
22. **README §跨 PR deferred** 缺"临时 locale key 清理"列。  
23. **README §Golden Label 所有权 / PR-02 §7.1** deferred stub label 命名约束缺。  
24. **PR-04 §4.5** `keywordRegistryLint` 跨模块实现策略未定。  
25. **PR-04 §4.6** AccessibilityToggle 默认值、切换时机、持久化未定。  

### P2（有时间再修）

26. **PR-01 §4.2** 最小支持窗口尺寸声明。  
27. **PR-01 §4.4** `BuildInfo.shortHash` 合法格式正则与失败可观测断言。  
28. **PR-01 §4.6** LWJGL title 线程切换 owner。  
29. **PR-02 §6.2** lint vs test 顺序说明。  
30. **PR-03 §4.2** GroundLootMarker 排序"special 优先"设计意图补注。  
31. **PR-03 §7.3** `Resource Fallback Audit` 模板落地。  
32. **PR-03 §4.7 + PR-03 §4.5** `UiEmptyState.detail` / `UiLoadingState.cancel` 的 locale key。  
33. **PR-04 §4.7** badge 文本是否走 locale 的决议。  
34. **PR-04 §4.4** "上一 inspect sub-view" 语义简化。  
35. **PR-05 §6.3.6** 非目标核查 lint 自动化。  
36. **PR-05 §4.3** `TelegraphLinkageHint` 最小 shape。  
37. **PR-05 §5.4** golden label 清单直写文档。  
38. **manual-records/_template.md** 双人签收 / commit sha / 截图保存路径 enforcement。  
39. **README** ValidationPreset 硬依赖改为接口层引用。  
40. **README** golden label 自动化清理 hook。  

---

## 9. 源计划映射完整性检查（第二轮）

第一轮已确认 5 份合并后 PR 覆盖源 §7~§14 Exit 条件，无整条丢失。第二轮重点抽查增量文档对源计划的"合并后条款颗粒度"：

| 源 PR | 源 Exit 条目 | 当前合并后落点 | 颗粒度是否达到源要求 |
| --- | --- | --- | --- |
| 原 PR-01 Token | `TelegraphRenderer` danger 色不再裸 hex | PR-01 §8.2 | 达 |
| 原 PR-02 首页 | 无 save / 有 save / 不可 save 三态自动化 + 人工 | PR-01 §8.3 / §6.3 | 达 |
| 原 PR-03 输入 | `I / F / X / Tab / Ctrl+S` 语义冻结 | PR-02 §4.3 truth table | 达但 Ctrl+S 行覆盖不全（§1.3） |
| 原 PR-04 图标 | 官方可装备物 iconKey 非空可解析 | PR-03 §4.6 `ItemIconKeyCoverageRule` | 达 |
| 原 PR-04 地面掉落 | 单件/多件/9+ marker | PR-03 §4.2 / §6.1 | 达 |
| 原 PR-05 共享卡片 | 事件/商店/奖励共享 | PR-03 §4.3 | 达但 ModalCardAction 语义不足（§4.3） |
| 原 PR-05 错误/空/加载 | 3 态下一步引导 | PR-03 §4.5 / §6.1 | 达但 detail key 缺（§4.9） |
| 原 PR-06 状态 badge | 回合/stack/cap/分类 | PR-04 §4.1 | 达但 priority 公式不定（§5.1） |
| 原 PR-06 telegraph 权重 | 高风险权重高于普通状态 | PR-04 §4.2 | 达但 a11y 组合缺（§5.3） |
| 原 PR-07 动态说明 | 关键词入 item/shop/inspect/action | PR-04 §4.3 | 达但 action 分支 owner 断层（§5.7） |
| 原 PR-07 ExplainPane | 不占 stack、不新 UiMode | PR-04 §4.4 | 达但 `ExplainPaneModel` 家族矛盾（§1.5） |
| 原 PR-08 telegraph 三位一体 | 地图 / 目标卡 / 日志同语义 | PR-05 §4.1 | 达 |
| 原 PR-08 战斗三层 | ACTION -> METHOD -> TARGET | PR-05 §4.2 | 达但状态机键位覆盖不全（§6.2, §6.3, §6.4, §6.5） |
| 原 PR-08 Ctrl+S 阻断 | 战斗决策期间阻断 + toast | PR-05 §4.4 | 达 |

**结论**: 0 条源 Exit 条目整条丢失，但合并后条款的颗粒度在 `priority 公式 / push 触发条件 / 状态机键位 / payload 字段映射 / token 面完整性` 5 个面上低于源计划的隐含精度。

---

## 10. 给作者的下一步建议

1. **第 8 节 P0 9 条** 必须在开工 PR-01 之前直接在文档里落字；不要带着 P0 开工，否则 PR-01 的 token/payload 决策会污染 PR-03。  
2. P1 25 项中，`§3.7 / §4.1 / §4.3 / §5.3 / §6.3 / §6.4 / §6.5` 属于"状态机 / 公式 / 枚举语义"类，迟修会让 `InputHandlerTest` 和 golden 漂移，建议一并解。  
3. P2 多数是文档/流程 enforcement，可放到每份 PR 内部落地时就地修；但 `README` 的跨 PR 清理列、golden label 命名约束建议先做，避免 PR-02 开工后留 stub 后悔。  
4. 本轮 review 不建议引入新"回头拆 PR"的动作；5 份 PR 的合并理由与 README 的执行纪律仍然站得住脚。
