# Dark UI/UX PR-04 Profession Tree UI — Deep Review Round 2

**评审对象**: `UI/pr/dark-uiux-pr04-profession-tree-ui.md` (780 行版本)
**对照**: 上一轮 review 提出的 P0/P1/P2/P3 项
**评审视角**: 资深 Roguelike / ToME 系统策划总监 + 客户端 presenter/renderer 落地负责人
**评审目的**: 在文档进入实现前，把所有"会让两个不同开发者写出不同代码"的模糊点收敛到 typed contract。

---

## TL;DR

- **方向修订彻底**：tree workbench model、tier/column/edge typed projection、preview block 模型、active slot modal item 模型、coexistence layout、data foundation gate —— 上一轮 P0/P1/P2 几乎全部落到合同里。**这次 review 不再有方向性问题。**
- **剩余问题全部是"工程实现层面的歧义"**：enum 之间矛盾、字段冗余、ordering 未固定、命名与含义不对齐、stop condition 与 acceptance matrix 未交叉锚定。任何一条不收敛，PR04 closure 时都会有 implementation 与 doc drift。
- **建议**：合入实现前先做一轮 contract patch（本文 §A / §B 必须做，§C 强烈建议，§D 可以收尾时改）；预计 0.5 day 内可改完。

---

## A. P0 · 会直接导致 renderer 自由发挥的契约矛盾（必改）

### A-1 `TalentTreeFocusLevel` enum 是互斥三态，但 §6.1 要求 tier row 和 node cell 同时高亮

文档现状：

```kotlin
enum class TalentTreeFocusLevel { NONE, TIER_ROW, NODE_CELL }
```

`TalentTreeNodeCell.focus: TalentTreeFocusLevel` —— 一个枚举字段只能取一个值。

但 §6.1 表格写：

> selected tier row 和 selected node cell 是两层 focus，**不能用同一 Boolean 混掉**

参考图也是双层：整个 tier II row 是半透明 cyan 矩形，被选中的 cell 在内部还有 cyan 实线。如果 cell.focus = NODE_CELL，那 tier row 谁负责标？如果 cell.focus = TIER_ROW，那 cell 自己怎么高亮？

**修复建议（二选一）**：

1. 改成 `Set<TalentTreeFocusLevel>`，允许 cell 同时携带 `{TIER_ROW, NODE_CELL}`。
2. 拆成两个字段：`tierRowFocused: Boolean` + `cellFocused: Boolean`，再把 enum 删掉。`TalentTreeTierLabel.focused: Boolean` 同步改名为 `rowFocused` 与 cell 字段一致。

无论哪种，必须新增 focused test：`NEW: TalentSidebarPresenterTest.selectedNodeCellAlsoFocusesItsTierRow`。

### A-2 `TalentTreeNodeCell.toneToken` 与 `hasPendingAllocation` 双重 pending 表达

§2.2 表格：

| pending allocation overlay | `TALENT_PENDING` | … | reuse base state icon, plus pending chrome |

但 cell data class 同时声明：

```kotlin
val toneToken: TalentTreeNodeToneToken,  // 包含 TALENT_PENDING
val hasPendingAllocation: Boolean,
```

两份 pending 表达 → presenter 要么写出 `toneToken=TALENT_PENDING, hasPendingAllocation=true`（冗余），要么写出 `toneToken=TALENT_LEARNABLE, hasPendingAllocation=true`（语义割裂），renderer 不知道该信哪一个。

**修复建议**：选一种 single source of truth。
- 推荐方案：删除 `hasPendingAllocation`，pending 完全由 `TALENT_PENDING` 携带；§2.2 表格第 5 行的 "reuse base state icon, plus pending chrome" 改成"renderer 收到 TALENT_PENDING 时按 base state icon 绘制 + 叠加 ember-gold ring"。
- 否则保留 boolean，把 enum 中的 `TALENT_PENDING` 删除，pending 只走 `hasPendingAllocation`。

二选一必须明文写死，否则 §UI04-M02a 测试无法断言"pending 不是 fifth state"。

### A-3 §6.2.1 "race tree 放在 profession tree 之后" 与 §2.3.1 "client 不排序" 直接冲突

- §2.3.1：`runtime tree order = RenderUiStateSnapshot.talentTrees order；client 不排序`
- §6.2.1：`Race tree 放在 profession tree sections 之后`

如果 snapshot 顺序是 `[profession1, race, profession2, profession3]`，client 是按 snapshot 还是强制 race 殿后？

**修复建议**：
- 推荐：删除 §6.2.1.1，统一遵循 §2.3.1 snapshot order。如果业务上必须 race 殿后，应该由 **snapshot owner（gameplay 层）** 排序，client 不干涉。
- 同时在 §2.4 race tree 口径里补一条："race tree 在 trees list 中的位置由 snapshot owner 决定；client 既不前置也不后置。"
- 同步删 §6.2 entire "Race Tree Placement" 子段或改成"Race tree 复用同一 section/cell/edge/preview contract，不做特殊位置约束"。

### A-4 `TalentSidebarLine` 残留状态未定义

§2.2 写：

> 现有 `TalentSidebarLine` 可以保留为过渡 adapter 或 debug trace，但不能继续作为 PR04 renderer 的唯一 authority。

但 §0 acceptance matrix 仍有 `TalentSidebarPresenterTest.pr04NonNodeRowsDoNotCarryStateFields`，§5.1 列出 `TalentSidebarPresenter.kt modified`，但 `TalentSidebarPresenter` 输出类型在矩阵里是 `TalentTreePanelModel`。

**模糊点**：
- `TalentSidebarLine` 是 **删除** 还是 **保留**？
- 如果保留，由谁消费？renderer 不再消费，谁消费？
- `pr04NonNodeRowsDoNotCarryStateFields` 在新模型下断言什么？新模型已经分离 node cell / non-node 类型，不存在"non-node row 携带 state field"的可能。

**修复建议**：
1. §5.1 表中显式列 `Deleted` 或 `Kept as adapter only`：明确 `TalentSidebarLine` 的命运。
2. 若 Kept：§2.2 必须列 adapter 公开签名、调用者、移除 owner（PR06？PR07？）。
3. 若 Deleted：把 `pr04NonNodeRowsDoNotCarryStateFields` 改名为 `TalentTreePanelModelDoesNotMixNodeAndNonNodeStateFields` 并断言新 model。
4. 在 §9 "Removal / Iteration Plan" 表加一行 `legacy TalentSidebarLine` 的 exit rule。

### A-5 `tierIndex` 方向（自顶向下 vs 自底向上）未固定

§2.2：`tierIndex: Int`，§2.2.7：`tier labels are presenter-provided localized strings`。

§6.1 表："tier 从高到低或低到高必须全 PR 一致" —— **但没固定方向**。这是一个 binary 决策，留给实现者后果：
- 实现者 A 写 tier 1 = top（罗马 V 在底）
- 实现者 B 写 tier 1 = bottom（罗马 V 在顶，与参考图一致）
- golden screenshot 与 manual whitebox 截图就会无法跨实现复用。

**修复建议**：把方向写死。参考图与 PoE/Diablo/ToME 惯例一致是 **tier higher index displayed higher**（tier V 在顶，tier I 在底）。在 §6.1 表 `tier axis` 行写：

> `tierIndex == 1` 表示最低 tier，渲染在 section 底部；`tierIndex == N` 是最高 tier，渲染在 section 顶部。tier label 由 presenter 输出 localized 字符串（默认罗马 I..V，可被 locale override）。

并加 focused test：`NEW: TileRendererCanvasTest.darkUiuxPr04TierAxisHigherTierIsHigherOnScreen`。

### A-6 `columnIndex` 推导算法没合同

§6.1：

> 缺 column source 时 presenter 使用 **stable node-order policy** 并在 self-audit 记录

但 "stable node-order policy" 不是定义。两个开发者会写两套算法：
- 实现 A：`columnIndex = nodes.filter { it.tier == cell.tier }.indexOf(cell)`（按节点声明顺序）
- 实现 B：`columnIndex = nodes.filter { it.tier == cell.tier }.sortedBy { it.id }.indexOf(cell)`（按 id 排序）

两个 PR 的 golden screenshot 会布局完全不同。

**修复建议**：明确算法。推荐：

> 缺 typed `columnIndex` source 时，按以下确定性策略推导：取 `TalentTreeSnapshot.nodes` 中所有 `tierIndex == cell.tierIndex` 的节点，按它们在 nodes list 中出现的相对顺序，从 0 开始赋 columnIndex。该投影由 presenter 完成；presenter 必须把"derived"标记写入 self-audit。

并新增 test：`NEW: TalentSidebarPresenterTest.pr04ColumnIndexFollowsTypedSourceOrFallsBackToNodesListOrder`。

### A-7 `TalentTreeEdgeState.ACTIVE / SATISFIED / UNSATISFIED` 语义未定义

三态枚举，但 §6.1 只笼统说：
- unsatisfied = muted/locked tone
- satisfied = subdued active tone
- selected path = low-alpha cyan

但 `ACTIVE` vs `SATISFIED` 的语义区别是什么？文档既没说什么时候 edge 应该是 ACTIVE，也没说 ACTIVE 在视觉上画成什么样。renderer 会按 readme 直觉自定义。

**修复建议**：在 §2.2 增加三态判定表：

| State | Trigger | Renderer treatment |
| --- | --- | --- |
| `UNSATISFIED` | 前置节点 state ∈ {LOCKED, LEARNABLE} 且 prerequisite minRank 未达 | muted/locked tone line |
| `SATISFIED` | 前置节点 state ∈ {LEARNED_RESERVE, LEARNED_ACTIVE} 且 prerequisite minRank 已达，但 to-node 自身还在 LOCKED/LEARNABLE | subdued active tone line |
| `ACTIVE` | 前置 + 当前节点都是 LEARNED_RESERVE/LEARNED_ACTIVE | ember-gold tone line |

若上游 typed prerequisite projection 还没准备好同时判断 from-side 和 to-side 状态，应该把 enum 收敛成两态 `{SATISFIED, UNSATISFIED}` 而不是留空白。

新增 test：`NEW: TalentSidebarPresenterTest.pr04EdgeStateReflectsBothEndpointStates`。

### A-8 `TalentPreviewBlock` ordering 与 §6.4.1 "NAME_RANK 固定在顶部" 矛盾

`blocks: List<TalentPreviewBlock>` 是 ordered list，应该由 presenter 排序。但 §6.4.1 又说 NAME_RANK 固定顶部。**问题**：
- 参考图实际顺序是：**HERO_ICON（最顶）→ NAME_RANK（icon 下方）→ RANK_TRANSITION → COST → PREREQUISITE/FAILED → RANK_PREVIEW → FLAVOR**
- 文档 enum 声明顺序却是 `NAME_RANK, HERO_ICON, RANK_TRANSITION, COST, PREREQUISITE, PREREQUISITE_FAILED, RANK_PREVIEW, FLAVOR`（NAME_RANK 在 HERO_ICON 之前）
- 文档自己冲突：声明顺序 ≠ "固定顶部" 描述

**修复建议**：把 §6.4 改写为：

> Presenter 必须按以下固定 ordering 输出 blocks：`HERO_ICON, NAME_RANK, RANK_TRANSITION, COST, PREREQUISITE, PREREQUISITE_FAILED, RANK_PREVIEW, FLAVOR`。每个 kind 至多出现一次。renderer 不重排。

并在 enum 注释里把声明顺序与 visual ordering 一致。

补 test：`NEW: TalentSidebarPresenterTest.pr04PreviewBlocksFollowFixedOrdering`。

### A-9 `PREREQUISITE` 与 `PREREQUISITE_FAILED` 共存条件未定义

参考图同时显示：
- "需学习前置: 不屈意志 (等级1)" —— 这是 PREREQUISITE
- "未满足前置条件" 红色 —— 这是 PREREQUISITE_FAILED

文档 §6.4 没说两者是否同时出现：
- PREREQUISITE always shown when has prereq？
- PREREQUISITE_FAILED only shown when unsatisfied？
- 还是 satisfied 时 PREREQUISITE shown、unsatisfied 时仅 PREREQUISITE_FAILED shown？

**修复建议**：明确规则。推荐：

> `PREREQUISITE` block 在 talent 存在 prerequisite 时始终展示（列出前置技能名 + 所需 rank）；`PREREQUISITE_FAILED` 仅在当前角色 state 未满足 prerequisite 时**追加**显示。两者同时出现时 ordering 为先 PREREQUISITE 后 PREREQUISITE_FAILED。

补 test：`NEW: TalentSidebarPresenterTest.pr04PreviewShowsPrerequisiteAndFailedTogetherWhenUnsatisfied`。

### A-10 `hotkeyDigit: Int` 与 `slot: Int` 在 `TalentActiveSlotStripItem` 重复

```kotlin
data class TalentActiveSlotStripItem(
    val slot: Int,
    val hotkeyDigit: Int,
    ...
)
```

§3.2.2：`slot order 固定 1..4` —— 即 `slot == hotkeyDigit` 永远成立。两个字段表达同一信息。

**修复建议**：删除 `hotkeyDigit`，保留 `slot`。renderer 直接用 `slot` 渲染数字角标。如果 future 需要 slot != hotkey（例如未来 8 槽 hotbar 但只显示 4 个），届时单独提 PR 加 `hotkeyDigit`。

---

## B. P1 · 字段语义/字段冗余/字段 nullability 不清（强烈建议改）

### B-1 `TalentTreeHeaderModel.ownerLabel: String?` 与 `professionName` 含义重叠

```kotlin
val professionName: String,
val ownerLabel: String?,
```

什么场景下 `ownerLabel` 不等于 `professionName`？文档没说。

**修复建议**：
- 如果是 race tree 的 owner 名（race name），应该重命名为 `raceName: String?`。
- 如果是某种"当前 focused tree owner" 文案，应该挪到 `TalentTreeSectionModel`。
- 如果暂时不用，**删掉**。

### B-2 `tree section header` 的 `ownerPointText: String?` 与 panel header 的 `professionPointText / racePointText` 关系不明

一个职业有 3 棵 profession tree。如果每棵 tree 的 `TalentTreeSectionModel.ownerPointText` 都填 "职业点 3"，会渲染 3 次相同 chip。

**修复建议**：
- 推荐：`ownerPointText` 只在 race tree section 中出现（显示种族点），profession tree section 不重复职业点。`TalentTreeHeaderModel.professionPointText` 是唯一职业点 surface。
- §6.1 表加一行 `section header.ownerPointText: 仅当 ownerType=RACE 时填值，否则 null`。
- 加 test：`NEW: TalentSidebarPresenterTest.pr04ProfessionSectionDoesNotDuplicateProfessionPointChip`。

### B-3 `ownerType: String` 应该是 typed enum

多个 model 都有 `ownerType: String`（SectionModel / NodeCell / SelectionIdentity）。String 容易拼写错（"PROFESSION" vs "Profession" vs "profession"），且无法被编译器约束。

**修复建议**：引入 `enum class TalentTreeOwnerType { PROFESSION, RACE }`（或复用已有 typed enum），所有 String → enum。

加 test：`NEW: TalentSidebarPresenterTest.pr04OwnerTypeIsTypedNotString`。

### B-4 `selected` / `focused` / `focus` 字段命名混用

同一个 PR 里出现 4 种"被选中"表达：
- `TalentActiveSlotStripItem.selected: Boolean`
- `ActiveSlotChoiceModalItem.selected: Boolean`
- `TalentTreeTierLabel.focused: Boolean`
- `TalentTreeNodeCell.focus: TalentTreeFocusLevel`

`selected` 和 `focused` 含义到底相同还是不同？

**修复建议**：统一术语。推荐：

| 含义 | 命名 | 适用 |
| --- | --- | --- |
| 键盘焦点（光标当前位置） | `focused: Boolean` 或 `focus: enum` | 节点、tier、modal item |
| 已被点入 active loadout | `equipped: Boolean` | active slot item only |
| pending draft target | 已用 `state=PENDING_*` 表达 | strip / modal |

把 `selected` 全部 rename 为 `focused`（保留 boolean）或 `keyboardFocused`，并在 §2.2 表加一行说明。

### B-5 `ActiveSlotChoiceModalItem.hotkeyText: String` vs `slot: Int?` 与 cancel hint 分隔不清

- §3.3.5: `Esc is a footer/hint, not a selectable slot item`
- 但 §3.3 末尾 ordering 列表 #6 写 `cancel hint Esc`
- `ActiveSlotChoiceModalModel.cancelHintText: String` 是单独字段

那 `cancel hint` 是不是 `ActiveSlotChoiceModalItem`？还是仅 `cancelHintText` 文本？文档表述跳跃。

**修复建议**：明文规定：

> `ActiveSlotChoiceModalModel.items` 长度固定为 5：`SLOT_FILLED|SLOT_EMPTY|SLOT_REPLACE_TARGET × 4` + `RESERVE_ACTION × 1`。`Esc` 不是 item，由 `cancelHintText` 独立渲染为 footer。`§3.3` 末尾 ordering 改成：

```
items = [
  slot 1 (kind ∈ {SLOT_FILLED, SLOT_EMPTY, SLOT_REPLACE_TARGET}),
  slot 2 (同上),
  slot 3 (同上),
  slot 4 (同上),
  reserve (kind = RESERVE_ACTION, slot = null),
]
cancelHintText = "Esc 取消"  // 独立 footer，不算 item
```

补 test：`NEW: TalentSidebarPresenterTest.pr04ActiveSlotChoiceModalHasFiveItemsAndCancelIsFooter`。

### B-6 `TalentTreeSectionScrollModel.horizontalOffsetByTier: Map<Int, Int>` 滚动粒度未定

`Map<tierIndex, horizontalOffset>` —— 暗示每个 tier 可以独立横向滚动。但参考图里同 section 内所有 tier 是同步水平定位的。

如果保持 per-tier scroll：
- 玩家在 tier II 横向滚动后，tier I/III 是否同步？
- prerequisite edge 跨 tier 时，两端 column 位置不同，edge 怎么画？

**修复建议**：
- 推荐：改成 `horizontalOffset: Int`（section-wide 同步滚动）。如果 tier 内 column 数差异很大，用 deterministic compact column 而不是 per-tier 独立 scroll。
- 若坚持 per-tier，必须在 §6.3 加 "cross-tier edge 在 per-tier scroll 下的端点计算规则"，并加 test `NEW: TileRendererCanvasTest.darkUiuxPr04CrossTierEdgeFollowsBothTierScrollOffsets`。

### B-7 `TalentActiveSlotStripModel.reserveHintText` 与 modal `RESERVE_ACTION` row 在 modal 打开时的行为

§3.2 说 "ACTIVE_TALENT_SLOT_CHOICE modal 打开时复用同一 strip"。

modal 打开后 strip 是否还显示 `reserveHintText`？还是被 modal 的 `RESERVE_ACTION` row 取代？

**修复建议**：明文规定。推荐：

> 当 `activeSlotChoiceModal != null` 时，strip `reserveHintText` 必须为 `null`（避免与 modal 的 `RESERVE_ACTION` 重复）。modal 关闭后由 presenter 重新填充。

补 test：`NEW: TalentSidebarPresenterTest.pr04ReserveHintTextIsNullWhenActiveSlotChoiceModalIsOpen`。

### B-8 `rankText` 在节点 cell 和 RANK_TRANSITION block 双份存在

- `TalentTreeNodeCell.rankText: String`（"2/5" or "1->2/5"）
- `TalentPreviewBlock.kind = RANK_TRANSITION` 显示 "等级 2 → 等级 3"

两者格式不一致：cell 用 slash，preview 用 arrow。**presenter 内部是不是要拼两次？**

**修复建议**：拆字段。preview RANK_TRANSITION block 应该带结构化字段：

```kotlin
data class RankTransition(
    val currentRank: Int,
    val previewRank: Int,
    val maxRank: Int,
    val localizedArrow: String,  // 默认 "→"，可被 locale override
)
```

或者把 `RANK_TRANSITION` block 的 `primaryText` 由 presenter 输出最终字符串，但 §6.4 写明格式规范："`{currentRankLabel} {arrow} {previewRankLabel}`，其中 rankLabel 来自 locale `talent.rank.label.${n}`"。

避免 renderer 自己拼 arrow。

### B-9 `TalentPreviewBlock.iconKey: String?` 在非 HERO_ICON block 的用途未定义

每个 block 都有 `iconKey: String?`。HERO_ICON 用是清楚的；其他 block 呢？COST block 是否要画"金币图标"？PREREQUISITE 是否要画"锁链图标"？

**修复建议**：在 §6.4 加一张表：

| Block kind | `iconKey` 用途 |
| --- | --- |
| `HERO_ICON` | 必填，talent 大图 |
| `NAME_RANK` | 必为 null |
| `RANK_TRANSITION` | 必为 null |
| `COST` | 可选，资源类型 icon（职业点 / 种族点） |
| `PREREQUISITE` | 必为 null |
| `PREREQUISITE_FAILED` | 必为 null |
| `RANK_PREVIEW` | 必为 null |
| `FLAVOR` | 必为 null |

并加 test 断言违反规则即失败。

### B-10 `FLAVOR` block 的 "locale source 可用" 判定标准

§6.4.7：`FLAVOR 只在已有 desc/flavor locale source 可用时展示；不得为 UI 氛围临时写死中文句子`。

但 presenter 怎么判断 "可用"？
- locale key 存在？
- locale key 返回非空字符串？
- talent metadata 有 `flavorKey` 字段？

**修复建议**：明确判定接口。推荐：

> Presenter 只在 `LocaleSource.tryGetText("talent.${talentId}.flavor")` 返回非 null 且非 empty 时输出 FLAVOR block；否则不在 blocks list 中包含 FLAVOR。renderer 不允许 fall back 到任何"通用 flavor"。

补 test：`NEW: TalentSidebarPresenterTest.pr04FlavorBlockOmittedWhenLocaleKeyMissingOrEmpty`。

---

## C. P2 · acceptance matrix / gate / 命名一致性问题

### C-1 `pr04ManualLabelMapping` lint 被两条 requirement 共用

- `UI04-M09b-pr04-manual-label-capture` fastCheck = `acceptanceContractLint.pr04ManualLabelMapping`
- `UI04-M10b-min-window-manual-record` fastCheck = `acceptanceContractLint.pr04ManualLabelMapping`

同一 lint 同时验证两件事，失败时 owner 不知道是 label mapping 错还是 min-window record 错。

**修复建议**：
- 推荐拆分：`pr04ManualLabelMapping` 仅验证 §8.2 dark label mapping 表的字段完整性；新增 `pr04MinWindowManualRecord` 单独验证 min-window viewport / log overlap 字段。
- 或在 lint 实现里加 subRuleId，并在 §0 fastCheck 列写 `acceptanceContractLint.pr04ManualLabelMapping#labelMapping` / `#minWindow`。

### C-2 `Phase2ContentCoverageTest` 的 owner 与 task path 不一致

- requirement `UI04-M00-tree-foundation-audit` owner = `game`
- fastCheck = `:tools:contractLint --tests com.ktome.tools.lint.Phase2ContentCoverageTest`

owner `game` 但 task 在 `:tools` —— ownerGate 触发时谁负责修？

**修复建议**：明确 owner 二级。
- 如果数据是 game 负责，schema audit 是 tools 负责，分成两条 requirement：`UI04-M00a-tree-data` (owner=game) + `UI04-M00b-tree-coverage-audit` (owner=tools)。
- 或在 requirement 里加 `secondaryOwner: tools`，并说明排错路径。

### C-3 `UI04-M02b-reference-direction-boundary` 的 fastCheck 是 `acceptanceContractLint` 但没具体 sub-rule

requirement 写 `acceptanceContractLint`，artifact 是文档本身。**这条 requirement 在 PR04 实际不会失败**——只要文档存在它就过。

**修复建议**：
- 推荐删除此 requirement，把"参考图非合同"的声明留在 §0.0 散文里，不进 acceptance matrix。acceptance matrix 应该全是 typed/可执行 check。
- 或把它升级成 `acceptanceContractLint.pr04ReferenceImageBoundary` 子规则，断言文档明文写"reference image is non-canonical"。

### C-4 `dark-uiux-pr04-coexistence-with-equipment-and-inventory` 既是 golden 也是 manual

- §0 Canonical Artifact `client golden` 列出此 label
- §8.2 manual record 也列出此 label

是 deterministic canvas test 生成的 golden hash，还是 packaged app 截图？源不同。

**修复建议**：拆名。建议：
- `dark-uiux-pr04-coexistence-with-equipment-and-inventory.golden` → renderer canvas test
- `dark-uiux-pr04-coexistence-with-equipment-and-inventory.manual.png` → packaged app capture

或在 §0 / §8.2 显式标注 evidence type，避免后续 PR-07 final index 重复收集。

### C-5 golden label 命名仍用 "talent-sidebar-start" 但已迁移到 "tree workbench"

`dark-uiux-pr04-talent-sidebar-start` 命名暗示 sidebar，但实际 UI 是 panel workbench。新读者会困惑。

**修复建议**：
- 若 PR04 现阶段保留旧名以维护 traceability，§9 Removal Plan 加一条 `rename to dark-uiux-pr04-talent-workbench-start` 列入 PR06 rebaseline owner。
- 或本 PR 直接 rename，所有引用一次性更新。

### C-6 `UI04-M10d-overflow-and-input-bounds` 与 §6.5 "mouse=deferred" 路径冲突

- requirement fastCheck = `InputHandlerTest.talentTreeMouseClickSelectsNodeWhenHitRegionExists`
- §6.5.4 允许"如果当前 hit region 基础设施不足，PR04 必须在 self-audit 写 `mouse=deferred`"

如果 mouse=deferred，那个 InputHandlerTest 怎么执行？

**修复建议**：把测试逻辑分支化：
- `NEW: InputHandlerTest.talentTreeMouseClickSelectsNodeWhenHitRegionExists` —— 仅在 PR04 实现 hit region 时运行
- `NEW: InputHandlerTest.talentTreeKeyboardOnlyWhenMouseDeferred` —— mouse deferred 时验证 keyboard 路径完整
- §0 acceptance matrix 用 conditional fastCheck："`InputHandlerTest.talentTreeMouseClickSelectsNodeWhenHitRegionExists` if mouse implemented; else `InputHandlerTest.talentTreeKeyboardOnlyWhenMouseDeferred`"

### C-7 §4 stop condition 与 §0 `UI04-M00` 没显式互锚

§4.3 "release playable 职业缺树..." 是 stop condition。§0 `UI04-M00-tree-foundation-audit` 是 requirement。**两者关系**：
- stop condition 是 requirement 的 prerequisite？
- 还是 requirement 失败触发 stop condition？

**修复建议**：§4 stop condition 第 3 条末尾补 "对应 acceptance requirement `UI04-M00-tree-foundation-audit`"，把 §0 与 §4 显式锚定。

### C-8 §0 测试命名两种风格混用

- backtick 风格: `\`pr01 scenario generates profession tree evidence names from the typed registry\``
- camelCase: `pr04BuildsTierColumnNodeCellsAndPrerequisiteEdges`

实现者新增测试时不知道该用哪种。建议：

- 旧 EXISTING tests 保留原名（backtick 也好 camelCase 也好）
- 所有 NEW 测试统一 camelCase，因为 §0 矩阵新增的全是 camelCase

§0 表格底部加一行 footnote："PR04 新增测试统一使用 camelCase；EXISTING 测试名保留原仓库风格。"

### C-9 `:tools:test` vs `:tools:contractLint` 在 §8.1 Gate ladder 里分开了，但 §0 数据 foundation gate 把它放在同一行

§0：
```
:game:test ... :tools:contractLint --tests Phase2ContentCoverageTest
```

§8.1：
```bash
./gradlew :game:test ... :tools:contractLint --tests Phase2ContentCoverageTest
```

OK 一致。但 §8.1 Gate ladder 第 3 行又写 `:tools:test --tests com.ktome.tools.whitebox.Phase4V4WhiteboxScenarioCliTest` —— 这是 `:tools:test` 不是 `:tools:contractLint`。

两个 task 都跑还是只跑一个？**修复建议**：在 §0 Gate Budget 第 1 行明确 `Phase2ContentCoverageTest` 通过 `:tools:contractLint`，第 3 行 fast lane 跑 `Phase4V4WhiteboxScenarioCliTest` 通过 `:tools:test`，并在 §8.1 给出完整顺序，避免 owner 误解为重复跑。

---

## D. P3 · 文档可读性与一致性细节（收尾改）

### D-1 §0 acceptance matrix `UI04-M03` rename 痕迹

requirement id 是 `UI04-M03-render-tree-model`，source 写 `§5.1 TileRenderModel / TileRenderer tree workbench model`，但 §5.1 实际表格列名仍是"row model"风格 row 描述。可考虑把 §5.1 描述更新为 "tree workbench model" 与 matrix 对齐。

### D-2 §2.6 表 "tree shape 为 6/5/5" 与 "derived tier distribution 为 T1=6 / T2=6 / T3=4" 数字不匹配

`6/5/5 = 16`，`6+6+4 = 16`。OK 数字对得上，但 tier distribution 是按 tier 维度横切的，和 tree shape 是按 tree 维度纵切的。读者可能误读为"每棵树 T1=6 / T2=6 / T3=4"，实际是"全 16 node 中 T1 共 6 个、T2 共 6 个、T3 共 4 个"。

**修复建议**：表头改成 "tree shape (nodes per tree)" / "tier distribution (nodes per tier, summed across trees)"，避免歧义。

### D-3 §6.6 Legend 项与 enum 五值不对齐

§6.6 legend："`可学习 / 锁定 / 已激活 / 预留 / 当前选择 / pending`" —— 6 项。

`TalentTreeNodeToneToken` 5 值（含 PENDING）。`当前选择` 不是 tone token，是 focus。

**修复建议**：legend item model 加 `kind: TalentTreeLegendItemKind { STATE, FOCUS, PENDING }`，明确每项对应什么：

```kotlin
enum class TalentTreeLegendItemKind { STATE_TONE, FOCUS, PENDING_OVERLAY }
data class TalentTreeLegendItem(
    val kind: TalentTreeLegendItemKind,
    val iconKey: String?,
    val label: String,
    val toneToken: TalentTreeNodeToneToken?,  // 仅 STATE_TONE / PENDING_OVERLAY 时填值
)
```

补 test：`NEW: TalentSidebarPresenterTest.pr04LegendHasFiveToneTokensPlusFocusEntry`。

### D-4 §2.2 表格 "selected tone" 在 `toneToken` 行已经说不改 state identity，但又有 `TALENT_PENDING` token

`TALENT_PENDING` 在 enum 内但 §2.2 第 4 条说"pending is a display overlay only, **不增加第五 TalentNodeStateSnapshot**"。token enum 中 PENDING 算不算"实际上是第五 tone"？

**修复建议**：把 `TALENT_PENDING` 从 `TalentTreeNodeToneToken` 中分离，独立成 `TalentTreeNodePendingChrome` 或 boolean overlay，与 4 个 state tone 解耦。这也解决了 A-2 的问题。

### D-5 §3.1 表 backspace 路径"if implementation already routes it through root modal close" 太宽泛

"实现已经路由"是事实陈述还是规范？**应该规范**。建议：

> Backspace 不被 `ACTIVE_TALENT_SLOT_CHOICE` 主动消费；如果上游 modal stack 中存在 root-level handler 把 Backspace 映射为 modal close，则 PR04 不阻断该路径，并必须在 `InputHandlerTest.talentSlotChoiceIgnoresNonContractKeys` 中显式断言"Backspace 不产生 `PlayerCommand`，但允许触发 modal stack close（即 PR04 不消费但不阻断）"。

### D-6 §5.1 文件数量与 maintainabilityLint 阈值关系

§0 Gate Budget 写："新增 presentation model、renderer shared row layout 或 **5 个以上 Kotlin 文件**"才触发 maintainabilityLint。

§5.1 实际表格里：
- `TalentTreePanelModel.kt` (新增)
- `TalentTreeSelectionIdentity.kt` (新增)
- 其他都是 modified

新增 2 个文件，不达 5 个阈值？还是 `TalentTreePanelModel.kt` 内会含 6+ 个 data class（按现行 Kotlin 风格通常会被拆文件）？

**修复建议**：在 §5.1 表后加一段说明：

> `TalentTreePanelModel.kt` 文件内允许包含本文档列出的所有相关 data class（HeaderModel, SectionModel, NodeCell, EdgeProjection, TierLabel, PreviewModel, PreviewBlock, LegendModel, LegendItem, ActiveSlotChoiceModalModel, ActiveSlotChoiceModalItem, ActiveSlotStripModel, ActiveSlotStripItem 等）。如实现选择按 model family 拆分到多个文件并超过 5 个新文件，必须跑 `maintainabilityLint` 并在 self-audit 记录文件数。

### D-7 §10 self-audit 表"tier / edge source" 行表达不闭合

> typed tier and prerequisite edge source exists, **or edge drawing is explicitly deferred without renderer inference**

"explicitly deferred" 的载体是什么？manual record？PR description？acceptance matrix？

**修复建议**：把 deferred 路径锚定到 §9 Removal Plan 表中已有的 "missing typed prerequisite edge projection" 行，并在 self-audit 加 "if deferred: cite §9 exit rule entry"。

### D-8 §0 `UI04-M10c-right-companion-coexistence` 与 §UI04-M10a/b 的 owner 不一致

- M10a owner = client (rendering)
- M10b owner = docs (manual record)
- M10c owner = client (rendering)

按照 §UI04-M09a/b 的拆分模式（whitebox materialization owner=tools, manual capture owner=docs），coexistence 也应该有 manual 对应。但 M10c 只有 client owner 一条。

**修复建议**：增加 `UI04-M10c-2-right-companion-manual-evidence` owner = docs，artifact 是 `UI/manual-records/dark-uiux-pr04-profession-tree-ui.md` 中的 coexistence 截图行。否则人工 evidence 没有 owner。

### D-9 §2.2 表 "selected tone" 描述与现行实现的语义差异未声明

旧 PR04（review round 1 前）`TalentSidebarLine.selected: Boolean` 是单字段。新 PR04 把"选中"语义拆到 `focus: TalentTreeFocusLevel`。

**修复建议**：在 §9 Removal Plan 加一行：

| `TalentSidebarLine.selected: Boolean` 单字段 | yes as legacy only | PR04 | presenter test asserts new focus enum | new code path uses `focus: TalentTreeFocusLevel`; legacy line adapter may keep boolean for migration |

### D-10 §8.2 `app executable hash` 字段在 macOS-only 路径

`build/whitebox/phase4-v4-pr01/app-executable.sha256` —— 但 K-ToME 在 Linux/Windows 也能跑。如果 owner 在非 Mac 上做 manual whitebox，sha256 怎么计算？

**修复建议**：在 §8.2 字段表"app executable hash"行补 platform note：

> macOS: `.app/Contents/MacOS/<executable>` sha256；Linux: `bin/<executable>` sha256；Windows: `<executable>.exe` sha256。manual record 必须记录 platform，sha256 校验只在同 platform 内可比。

### D-11 §3.3 表的 `Esc 取消` 在 modal items list 中出现的歧义

§3.3 末尾文字 "Modal ordering fixed: 1. slot 1, 2. slot 2, 3. slot 3, 4. slot 4, 5. reserve action R, **6. cancel hint Esc**" —— 但 enum 没有 ESC 这个 kind。

**修复建议**（与 B-5 联动）：把"6. cancel hint Esc"移出 items list，独立写在 ordering 列表之后：

> Items list 长度 5；底部 Esc cancel hint 由 `cancelHintText` 字段单独渲染，**不在 items list 内**。

### D-12 §0 fastCheck 列写 `EXISTING:` 与 `NEW:` 前缀

部分 fastCheck 写了 `EXISTING:` 前缀（如 `EXISTING: InputHandlerTest.\`talent assign respec follows focused tree owner\``），部分没写。

**修复建议**：所有 EXISTING 测试统一加 `EXISTING:` 前缀；所有未写前缀的默认是 `NEW:`。审计时一眼可见新增 vs 复用。

---

## E. 推荐的契约 patch 顺序

按风险与成本排序，推荐如下落地顺序：

1. **A-1 / A-2 / A-3 / A-4** —— 直接矛盾的契约项，必须在 implement 前修。
2. **A-5 / A-6 / A-7** —— 决定 layout/edge 实现细节，否则两个开发者会写出不同 golden。
3. **A-8 / A-9 / A-10** —— preview block / strip 模型字段冗余/ordering。
4. **B-1 ~ B-10** —— 字段语义清理，逐条对照修。
5. **C-1 ~ C-9** —— acceptance matrix / gate 命名一致性，建议在 acceptanceContractLint 落地前修。
6. **D-1 ~ D-12** —— 文档可读性收尾。

预期工作量：A 一两小时；B+C 半天；D 一两小时。**完整 patch 后 PR04 可以进入实现阶段**。

---

## F. 与上一轮的差异

| 上一轮 Finding | Round 2 状态 |
| --- | --- |
| P0-1 layout model 缺失 | 已解决（新增 `TalentTreePanelModel` 家族） |
| P0-2 第五状态（EXCLUDED） | 已正确处理（不新增 client 态、留给上游） |
| P0-3 reserve 列表入口 | 部分解决（`R` 作为 modal 第五行；完整 reserve list modal 留给未来 PR） |
| P1-1 tier label / legend / section header | 已解决（typed model） |
| P1-2 preview block 模型 | 已解决（typed enum + ordering） |
| P1-3 active slot strip 字段 | 部分解决（仍有 `hotkeyDigit/slot` 冗余 → 见 A-10） |
| P1-4 modal item typed list | 已解决（含 cancel 与 reserve 边界仍有 B-5 歧义） |
| P1-5 dual focus | 已加入字段，但 enum 单值无法表达双层 → 见 A-1 |
| P2-1 overflow/scroll | 已加 §6.3 + scroll model（per-tier scroll 含义未清 → 见 B-6） |
| P2-2 mouse 输入 | 已加 §6.5 boundary + `mouse=deferred` 路径（与 acceptance matrix 测试条件交互未明 → 见 C-6） |
| P2-3 右伴随面板共存 | 已解决（§6.7 + `UI04-M10c`） |
| P2-4 race tree 视觉位置 | 已加 §6.2（与 §2.3.1 冲突 → 见 A-3） |
| P2-5 pending 视觉 | 已加 `TALENT_PENDING` token（与 `hasPendingAllocation` boolean 冗余 → 见 A-2） |
| 拆 PR04a/04b 建议 | 未采纳；文档仍是单 PR，但已通过 §2.6 data foundation gate 把 schema/data 责任前置 — **可接受**，因为 gate 已强制先验证数据合同 |

---

## G. 一句话给执行者

> 上一轮把"职业树是什么样"想清楚了。这一轮把"工程上怎么 typed 化它"基本写完了。**还差最后一公里：把 enum 单值能不能表达双层 focus、`hotkeyDigit/slot/selected/focused` 命名是不是同一回事、preview block 是不是 ordered fixed、race tree 在哪里、tier 朝上还是朝下 —— 这些 binary 决策一旦留模糊，PR06 rebaseline 和 PR07 final audit 都会被迫返工。建议照 §A/§B 列表逐条修死，0.5 day 即可清完。**
